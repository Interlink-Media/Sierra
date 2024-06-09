package de.feelix.sierra.check.impl.book;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierra.utilities.FieldReader;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.PunishType;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SierraCheckData(checkType = CheckType.BOOK_VALIDATION)
public class BookValidation extends SierraDetection implements IngoingProcessor {

    private              String   lastContent               = "";
    private              int      lastContentCount          = 0;
    private static final String[] MOJANG_CRASH_TRANSLATIONS = {"translation.test.invalid", "translation.test.invalid2"};

    public BookValidation(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void handle(PacketReceiveEvent event, PlayerData data) {
        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-book-crasher", true)) {
            return;
        }

        boolean      blockBooks = Sierra.getPlugin()
            .getSierraConfigEngine()
            .config()
            .getBoolean("disable-books-completely", false);
        List<String> pageList   = new ArrayList<>();

        PacketTypeCommon packetType = event.getPacketType();
        if (packetType.equals(PacketType.Play.Client.EDIT_BOOK)) {
            handleEditBook(event, data, blockBooks, pageList);
        } else if (packetType.equals(PacketType.Play.Client.PLUGIN_MESSAGE)) {
            handlePluginMessage(event, data, blockBooks, pageList);
        } else if (packetType.equals(PacketType.Play.Client.PICK_ITEM)) {
            handlePickItem(event, data, blockBooks, pageList);
        } else if (packetType.equals(PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT)) {
            handleBlockPlacement(event, data, blockBooks, pageList);
        } else if (packetType.equals(PacketType.Play.Client.CREATIVE_INVENTORY_ACTION)) {
            handleCreativeInventoryAction(event, data, blockBooks, pageList);
        } else if (packetType.equals(PacketType.Play.Client.CLICK_WINDOW)) {
            handleClickWindow(event, data, blockBooks, pageList);
        }

        Pair<String, PunishType> invalid = validatePages(pageList);
        if (invalid != null) {
            violation(
                event, ViolationDocument.builder()
                    .debugInformation(invalid.getFirst())
                    .punishType(invalid.getSecond())
                    .build());
        }
    }

    private void handleEditBook(PacketReceiveEvent event, PlayerData data, boolean blockBooks, List<String> pageList) {
        if (blockBooks) {
            violation(
                event, ViolationDocument.builder()
                    .debugInformation("Used book while disabled (edit book)")
                    .punishType(PunishType.BAN)
                    .build());
            return;
        }

        WrapperPlayClientEditBook wrapper = CastUtil.getSupplierValue(
            () -> new WrapperPlayClientEditBook(event), data::exceptionDisconnect);
        pageList.addAll(wrapper.getPages());
    }

    private void handlePluginMessage(PacketReceiveEvent event, PlayerData data, boolean blockBooks,
                                     List<String> pageList) {
        WrapperPlayClientPluginMessage wrapper = CastUtil.getSupplierValue(
            () -> new WrapperPlayClientPluginMessage(event), data::exceptionDisconnect);

        if (wrapper.getChannelName().contains("MC|BEdit") || wrapper.getChannelName().contains("MC|BSign")) {
            processPluginMessage(event, blockBooks, pageList, wrapper);
        }
    }

    private void processPluginMessage(PacketReceiveEvent event, boolean blockBooks,
                                      List<String> pageList, WrapperPlayClientPluginMessage wrapper) {
        Object buffer = null;
        try {
            buffer = UnpooledByteBufAllocationHelper.buffer();
            ByteBufHelper.writeBytes(buffer, wrapper.getData());
            PacketWrapper<?> universalWrapper = PacketWrapper.createUniversalPacketWrapper(buffer);

            ItemStack wrappedItemStack = universalWrapper.readItemStack();

            if ((wrappedItemStack.getType() == ItemTypes.WRITTEN_BOOK
                 || wrappedItemStack.getType() == ItemTypes.WRITTEN_BOOK) && blockBooks) {
                violation(
                    event, ViolationDocument.builder()
                        .debugInformation("Used book while disabled (plugin message)")
                        .punishType(PunishType.BAN)
                        .build());
            }

            if (wrappedItemStack.getType() != ItemTypes.WRITABLE_BOOK
                && wrappedItemStack.getType() != ItemTypes.WRITTEN_BOOK) {
                return;
            }

            if (invalidTitleOrAuthor(wrappedItemStack)) {
                removeTags(wrappedItemStack);
                violation(
                    event, ViolationDocument.builder()
                        .debugInformation("Invalid author in payload")
                        .punishType(PunishType.BAN)
                        .build());
            }

            pageList.addAll(getPages(wrappedItemStack));
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    private void handlePickItem(PacketReceiveEvent event, PlayerData data, boolean blockBooks, List<String> pageList) {
        WrapperPlayClientPickItem wrapper = CastUtil.getSupplierValue(
            () -> new WrapperPlayClientPickItem(event), data::exceptionDisconnect);

        Object buffer = null;
        try {
            buffer = UnpooledByteBufAllocationHelper.buffer();
            ByteBufHelper.writeBytes(buffer, wrapper.getBuffer());
            PacketWrapper<?> universalWrapper = PacketWrapper.createUniversalPacketWrapper(buffer);

            ItemStack wrappedItemStack = universalWrapper.readItemStack();

            if ((wrappedItemStack.getType() == ItemTypes.WRITTEN_BOOK
                 || wrappedItemStack.getType() == ItemTypes.WRITTEN_BOOK) && blockBooks) {
                violation(
                    event, ViolationDocument.builder()
                        .debugInformation("Used book while disabled (pick item)")
                        .punishType(PunishType.BAN)
                        .build());
            }

            if (wrappedItemStack.getType() != ItemTypes.WRITABLE_BOOK
                && wrappedItemStack.getType() != ItemTypes.WRITTEN_BOOK) {
                return;
            }

            if (invalidTitleOrAuthor(wrappedItemStack)) {
                removeTags(wrappedItemStack);
                violation(
                    event, ViolationDocument.builder()
                        .debugInformation("Invalid author in pick item")
                        .punishType(PunishType.BAN)
                        .build());
            }

            pageList.addAll(getPages(wrappedItemStack));
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    private void handleBlockPlacement(PacketReceiveEvent event, PlayerData data, boolean blockBooks,
                                      List<String> pageList) {
        WrapperPlayClientPlayerBlockPlacement wrapper = CastUtil.getSupplierValue(
            () -> new WrapperPlayClientPlayerBlockPlacement(event), data::exceptionDisconnect);

        if (wrapper.getItemStack().isPresent()) {
            ItemStack itemStack = wrapper.getItemStack().get();
            if ((itemStack.getType() == ItemTypes.WRITTEN_BOOK || itemStack.getType() == ItemTypes.WRITTEN_BOOK)
                && blockBooks) {
                violation(
                    event, ViolationDocument.builder()
                        .debugInformation("Used book while disabled (block place)")
                        .punishType(PunishType.BAN)
                        .build());
            }

            if (itemStack.getType() != ItemTypes.WRITABLE_BOOK && itemStack.getType() != ItemTypes.WRITTEN_BOOK) {
                return;
            }

            if (invalidTitleOrAuthor(itemStack)) {
                removeTags(itemStack);
                violation(
                    event, ViolationDocument.builder()
                        .debugInformation("Invalid author in block place")
                        .punishType(PunishType.BAN)
                        .build());
            }

            pageList.addAll(getPages(itemStack));
        }
    }

    private void handleCreativeInventoryAction(PacketReceiveEvent event, PlayerData data, boolean blockBooks,
                                               List<String> pageList) {
        if (getPlayerData() != null && getPlayerData().getGameMode() != GameMode.CREATIVE) {
            violation(
                event, ViolationDocument.builder()
                    .debugInformation("Spoofed creative game-mode")
                    .punishType(PunishType.BAN)
                    .build());
            return;
        }

        WrapperPlayClientCreativeInventoryAction wrapper = CastUtil.getSupplierValue(
            () -> new WrapperPlayClientCreativeInventoryAction(event), data::exceptionDisconnect);

        int slot = wrapper.getSlot();
        if (slot >= 100 || slot < -1) {
            violation(
                event, ViolationDocument.builder()
                    .debugInformation("Invalid creative slot at: " + slot)
                    .punishType(PunishType.KICK)
                    .build());
            return;
        }

        ItemStack itemStack = wrapper.getItemStack();
        if ((itemStack.getType() == ItemTypes.WRITTEN_BOOK || itemStack.getType() == ItemTypes.WRITTEN_BOOK)
            && blockBooks) {
            violation(
                event, ViolationDocument.builder()
                    .debugInformation("Used book while disabled (creative)")
                    .punishType(PunishType.BAN)
                    .build());
        }

        if (itemStack.getType() != ItemTypes.WRITABLE_BOOK && itemStack.getType() != ItemTypes.WRITTEN_BOOK) {
            return;
        }

        if (invalidTitleOrAuthor(itemStack)) {
            removeTags(itemStack);
            violation(
                event, ViolationDocument.builder()
                    .debugInformation("Invalid author in creative inv")
                    .punishType(PunishType.BAN)
                    .build());
        }

        pageList.addAll(getPages(itemStack));
    }

    private void handleClickWindow(PacketReceiveEvent event, PlayerData data, boolean blockBooks,
                                   List<String> pageList) {
        WrapperPlayClientClickWindow wrapper = CastUtil.getSupplierValue(
            () -> new WrapperPlayClientClickWindow(event), data::exceptionDisconnect);

        if(wrapper == null) return;

        if (wrapper.getCarriedItemStack() != null) {
            ItemStack itemStack = wrapper.getCarriedItemStack();
            if ((itemStack.getType() == ItemTypes.WRITTEN_BOOK || itemStack.getType() == ItemTypes.WRITTEN_BOOK)
                && blockBooks) {
                violation(
                    event, ViolationDocument.builder()
                        .debugInformation("Used book while disabled (click window)")
                        .punishType(PunishType.BAN)
                        .build());
            }

            if (itemStack.getType() != ItemTypes.WRITABLE_BOOK && itemStack.getType() != ItemTypes.WRITTEN_BOOK) {
                return;
            }

            if (invalidTitleOrAuthor(itemStack)) {
                removeTags(itemStack);
                violation(
                    event, ViolationDocument.builder()
                        .debugInformation("Invalid author in click window")
                        .punishType(PunishType.BAN)
                        .build());
            }

            pageList.addAll(getPages(itemStack));
        }
    }

    private void removeTags(ItemStack carriedItemStack) {
        Objects.requireNonNull(carriedItemStack.getNBT()).removeTag("pages");
        Objects.requireNonNull(carriedItemStack.getNBT()).removeTag("author");
        Objects.requireNonNull(carriedItemStack.getNBT()).removeTag("title");
    }

    private Pair<String, PunishType> validatePages(List<String> pageList) {
        long totalBytes   = 0;
        long allowedBytes = 2560;

        if (pageList.size() > 50) return new Pair<>("Too many pages", PunishType.KICK);

        for (String pageContent : pageList) {
            Pair<String, PunishType> duplicatedContent = isDuplicatedContent(pageContent);
            if (duplicatedContent != null) return duplicatedContent;

            String strippedContent = ChatColor.stripColor(pageContent.replaceAll("\\+", ""));
            //noinspection ConstantValue
            if (strippedContent == null || strippedContent.equals("null")) {
                return new Pair<>("Contains protocol color code", PunishType.BAN);
            }

            Pair<String, PunishType> invalidColor = isInvalidColor(strippedContent);
            if (invalidColor != null) return invalidColor;

            Pair<String, PunishType> extraFrequency = isExtraFrequency(pageContent);
            if (extraFrequency != null) return extraFrequency;

            Pair<String, PunishType> fieldIsReadable = checkFieldReadable(pageContent);
            if (fieldIsReadable != null) return fieldIsReadable;

            String noSpaces = pageContent.replace(" ", "");
            if (noSpaces.startsWith("{\"translate\"")) {
                for (String crashTranslation : MOJANG_CRASH_TRANSLATIONS) {
                    String translationJson = String.format("{\"translate\":\"%s\"}", crashTranslation);
                    if (pageContent.equalsIgnoreCase(translationJson)) {
                        return new Pair<>("Mojang crash translation", PunishType.KICK);
                    }
                }
                continue;
            }

            Pair<String, PunishType> invalidChars = tooManyInvalidChars(pageContent);
            if (invalidChars != null) return invalidChars;

            int                      contentLength   = pageContent.getBytes(StandardCharsets.UTF_8).length;
            Pair<String, PunishType> invalidPageSize = isInvalidPageSize(contentLength);
            if (invalidPageSize != null) return invalidPageSize;

            totalBytes += contentLength;
            int length     = pageContent.length();
            int multiBytes = 0;
            if (contentLength != length) {
                for (char c : pageContent.toCharArray()) {
                    if (c > 127) {
                        multiBytes++;
                    }
                }
            }

            allowedBytes += (long) ((2560 * Math.min(1, Math.max(0.1D, (double) length / 255D))) * 0.98);

            if (multiBytes > 1) {
                allowedBytes -= multiBytes;
            }
        }

        if (totalBytes > allowedBytes) {
            return new Pair<>("Book size is too large", PunishType.BAN);
        }

        return null;
    }

    private static @Nullable Pair<String, PunishType> isInvalidPageSize(int contentLength) {
        if (contentLength > 256 * 4) {
            return new Pair<>("Invalid page size", PunishType.BAN);
        }
        return null;
    }

    private static @Nullable Pair<String, PunishType> tooManyInvalidChars(String pageContent) {
        int oversizedChars = 0;
        for (int charIndex = 0; charIndex < pageContent.length(); charIndex++) {
            char currentChar = pageContent.charAt(charIndex);
            if (String.valueOf(currentChar).getBytes().length > 1) {
                oversizedChars++;
                if (oversizedChars > 15) {
                    return new Pair<>("Too many big characters", PunishType.KICK);
                }
            }
        }
        return null;
    }

    private static @Nullable Pair<String, PunishType> checkFieldReadable(String pageContent) {
        if (FieldReader.isReadable(pageContent) && !pageContent.isEmpty()) {
            return new Pair<>("Field is not readable", PunishType.KICK);
        }
        return null;
    }

    private static @Nullable Pair<String, PunishType> isExtraFrequency(String pageContent) {
        if (pageContent.split("extra").length > 8.0) {
            return new Pair<>("Invalid extra frequency", PunishType.BAN);
        }
        return null;
    }

    private static @Nullable Pair<String, PunishType> isInvalidColor(String strippedContent) {
        if (strippedContent.length() > 256.0) {
            return new Pair<>("Invalid color code signature", PunishType.KICK);
        }
        return null;
    }

    private @Nullable Pair<String, PunishType> isDuplicatedContent(String pageContent) {
        if (pageContent.equalsIgnoreCase(lastContent)) {
            if (lastContentCount++ > 4) {
                return new Pair<>("Too many equal pages", PunishType.KICK);
            }
        } else {
            lastContentCount = 0;
        }

        lastContent = pageContent;
        return null;
    }

    private boolean invalidTitleOrAuthor(ItemStack itemStack) {
        if (itemStack.getNBT() != null) {
            String title = itemStack.getNBT().getStringTagValueOrNull("title");
            if (title != null && title.length() > 100) {
                return true;
            }

            String author = itemStack.getNBT().getStringTagValueOrNull("author");
            return author != null && author.length() > 16;
        }
        return false;
    }

    private List<String> getPages(ItemStack itemStack) {
        List<String> pageList = new ArrayList<>();
        if (itemStack.getNBT() != null) {
            NBTList<NBTString> nbtList = itemStack.getNBT().getStringListTagOrNull("pages");
            if (nbtList != null) {
                for (NBTString tag : nbtList.getTags()) {
                    pageList.add(tag.getValue());
                }
            }
        }
        return pageList;
    }
}
