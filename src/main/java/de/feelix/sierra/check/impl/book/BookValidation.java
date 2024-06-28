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
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.check.violation.Violation;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierra.utilities.FieldReader;
import de.feelix.sierra.utilities.Triple;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.PunishType;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;

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

        boolean blockBooks = Sierra.getPlugin().getSierraConfigEngine().config().getBoolean(
            "disable-books-completely", false);
        List<String> pageList = new ArrayList<>();

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

        Triple<String, PunishType, List<Debug<?>>> invalid = validatePages(pageList);
        if (invalid != null) {

            this.violation(event, Violation.builder()
                .description(invalid.getFirst())
                .punishType(invalid.getSecond())
                .debugs(invalid.getThird())
                .points(1)
                .build());
        }
    }

    private void handleEditBook(PacketReceiveEvent event, PlayerData data, boolean blockBooks, List<String> pageList) {
        if (blockBooks) {
            this.violation(event, Violation.builder()
                .description("used book while disabled")
                .punishType(PunishType.BAN)
                .points(1)
                .debugs(Collections.emptyList())
                .build());
            return;
        }

        WrapperPlayClientEditBook wrapper = CastUtil.getSupplier(
            () -> new WrapperPlayClientEditBook(event), data::exceptionDisconnect);
        pageList.addAll(wrapper.getPages());
    }

    private void handlePluginMessage(PacketReceiveEvent event, PlayerData data, boolean blockBooks,
                                     List<String> pageList) {
        WrapperPlayClientPluginMessage wrapper = CastUtil.getSupplier(
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

            try {
                ItemStack wrappedItemStack = universalWrapper.readItemStack();

                checkGeneral(event, blockBooks, pageList, wrappedItemStack);
            } catch (IndexOutOfBoundsException exception) {
                this.violation(event, Violation.builder()
                    .description("send invalid payload")
                    .punishType(PunishType.KICK)
                    .points(1)
                    .debugs(Collections.singletonList(new Debug<>("Exception", exception.getMessage())))
                    .build());
            }
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    private void handlePickItem(PacketReceiveEvent event, PlayerData data, boolean blockBooks, List<String> pageList) {
        WrapperPlayClientPickItem wrapper = CastUtil.getSupplier(
            () -> new WrapperPlayClientPickItem(event), data::exceptionDisconnect);

        Object buffer = null;
        try {
            buffer = UnpooledByteBufAllocationHelper.buffer();
            ByteBufHelper.writeBytes(buffer, wrapper.getBuffer());
            PacketWrapper<?> universalWrapper = PacketWrapper.createUniversalPacketWrapper(buffer);

            ItemStack wrappedItemStack = universalWrapper.readItemStack();

            if ((wrappedItemStack.getType() == ItemTypes.WRITTEN_BOOK
                 || wrappedItemStack.getType() == ItemTypes.WRITTEN_BOOK) && blockBooks) {

                this.violation(event, Violation.builder()
                    .description("used book while disabled")
                    .punishType(PunishType.KICK)
                    .points(1)
                    .debugs(Collections.emptyList())
                    .build());
            }

            checkForInvalidAuthor(event, pageList, wrappedItemStack);
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    private void handleBlockPlacement(PacketReceiveEvent event, PlayerData data, boolean blockBooks,
                                      List<String> pageList) {
        WrapperPlayClientPlayerBlockPlacement wrapper = CastUtil.getSupplier(
            () -> new WrapperPlayClientPlayerBlockPlacement(event), data::exceptionDisconnect);

        if (wrapper.getItemStack().isPresent()) {
            ItemStack itemStack = wrapper.getItemStack().get();
            checkGeneral(event, blockBooks, pageList, itemStack);
        }
    }

    private void handleCreativeInventoryAction(PacketReceiveEvent event, PlayerData data, boolean blockBooks,
                                               List<String> pageList) {
        if (getPlayerData() != null && getPlayerData().getGameMode() != GameMode.CREATIVE) {
            this.violation(event, Violation.builder()
                .description("spoofed his game-mode")
                .punishType(PunishType.BAN)
                .points(1)
                .debugs(Arrays.asList(
                    new Debug<>("GameMode", getPlayerData().getGameMode().name()),
                    new Debug<>("Fake", "CREATIVE")
                ))
                .build());
            return;
        }

        WrapperPlayClientCreativeInventoryAction wrapper = CastUtil.getSupplier(
            () -> new WrapperPlayClientCreativeInventoryAction(event), data::exceptionDisconnect);

        int slot = wrapper.getSlot();

        if ((slot >= 100 || slot < -1) && slot != -999) {
            this.violation(event, Violation.builder()
                .description("clicked invalid slot")
                .punishType(PunishType.KICK)
                .points(1)
                .debugs(Arrays.asList(new Debug<>("Slot", slot), new Debug<>("Tag", "CREATIVE")))
                .build());
            return;
        }

        ItemStack itemStack = wrapper.getItemStack();
        checkGeneral(event, blockBooks, pageList, itemStack);
    }

    private void checkGeneral(PacketReceiveEvent event, boolean blockBooks, List<String> pageList,
                              ItemStack itemStack) {
        if (checkForInvalidAuthor(event, blockBooks, itemStack)) return;

        if (invalidTitleOrAuthor(itemStack)) {
            removeTags(itemStack);
            this.violation(event, Violation.builder()
                .description("used invalid book")
                .punishType(PunishType.BAN)
                .points(1)
                .debugs(Collections.singletonList(new Debug<>("Tag", "Author")))
                .build());
        }

        pageList.addAll(getPages(itemStack));
    }

    private boolean checkForInvalidAuthor(PacketReceiveEvent event, boolean blockBooks, ItemStack itemStack) {
        if ((itemStack.getType() == ItemTypes.WRITTEN_BOOK || itemStack.getType() == ItemTypes.WRITTEN_BOOK)
            && blockBooks) {
            this.violation(event, Violation.builder()
                .description("used book while disabled")
                .punishType(PunishType.BAN)
                .points(1)
                .debugs(Collections.emptyList())
                .build());
        }

        return itemStack.getType() != ItemTypes.WRITABLE_BOOK && itemStack.getType() != ItemTypes.WRITTEN_BOOK;
    }

    private void handleClickWindow(PacketReceiveEvent event, PlayerData data, boolean blockBooks,
                                   List<String> pageList) {
        WrapperPlayClientClickWindow wrapper = CastUtil.getSupplier(
            () -> new WrapperPlayClientClickWindow(event), data::exceptionDisconnect);

        if (wrapper == null) return;

        if (wrapper.getCarriedItemStack() != null) {
            ItemStack itemStack = wrapper.getCarriedItemStack();
            if ((itemStack.getType() == ItemTypes.WRITTEN_BOOK || itemStack.getType() == ItemTypes.WRITTEN_BOOK)
                && blockBooks) {
                this.violation(event, Violation.builder()
                    .description("used book while disabled")
                    .punishType(PunishType.BAN)
                    .points(1)
                    .debugs(Collections.emptyList())
                    .build());
            }

            checkForInvalidAuthor(event, pageList, itemStack);
        }
    }

    private void checkForInvalidAuthor(PacketReceiveEvent event, List<String> pageList, ItemStack itemStack) {
        if (itemStack.getType() != ItemTypes.WRITABLE_BOOK && itemStack.getType() != ItemTypes.WRITTEN_BOOK) {
            return;
        }

        if (invalidTitleOrAuthor(itemStack)) {
            removeTags(itemStack);
            this.violation(event, Violation.builder()
                .description("used invalid book")
                .punishType(PunishType.BAN)
                .points(1)
                .debugs(Collections.singletonList(new Debug<>("Tag", "Author")))
                .build());
        }

        pageList.addAll(getPages(itemStack));
    }

    private void removeTags(ItemStack carriedItemStack) {
        Objects.requireNonNull(carriedItemStack.getNBT()).removeTag("pages");
        Objects.requireNonNull(carriedItemStack.getNBT()).removeTag("author");
        Objects.requireNonNull(carriedItemStack.getNBT()).removeTag("title");
    }

    private Triple<String, PunishType, List<Debug<?>>> validatePages(List<String> pageList) {
        long totalBytes   = 0;
        long allowedBytes = 2560;

        if (pageList.size() > 50) return new Triple<>("interacted with an invalid item", PunishType.KICK,
                                                      Collections.singletonList(new Debug<>("Pages", pageList.size()))
        );

        for (String pageContent : pageList) {
            Triple<String, PunishType, List<Debug<?>>> duplicatedContent = isDuplicatedContent(pageContent);
            if (duplicatedContent != null) return duplicatedContent;

            String strippedContent = ChatColor.stripColor(pageContent.replaceAll("\\+", ""));
            //noinspection ConstantValue
            if (strippedContent == null || strippedContent.equals("null")) {
                return new Triple<>(
                    "interacted with an invalid item", PunishType.KICK,
                    Collections.singletonList(new Debug<>("Tag", "Color Strip"))
                );
            }

            Triple<String, PunishType, List<Debug<?>>> invalidColor = isInvalidColor(strippedContent);
            if (invalidColor != null) return invalidColor;

            Triple<String, PunishType, List<Debug<?>>> extraFrequency = isExtraFrequency(pageContent);
            if (extraFrequency != null) return extraFrequency;

            Triple<String, PunishType, List<Debug<?>>> fieldIsReadable = checkFieldReadable(pageContent);
            if (fieldIsReadable != null) return fieldIsReadable;

            String noSpaces = pageContent.replace(" ", "");
            if (noSpaces.startsWith("{\"translate\"")) {
                for (String crashTranslation : MOJANG_CRASH_TRANSLATIONS) {
                    String translationJson = String.format("{\"translate\":\"%s\"}", crashTranslation);
                    if (pageContent.equalsIgnoreCase(translationJson)) {
                        return new Triple<>(
                            "interacted with an invalid item", PunishType.KICK,
                            Collections.singletonList(new Debug<>("Tag", "Mojang crash translations"))
                        );
                    }
                }
                continue;
            }

            Triple<String, PunishType, List<Debug<?>>> invalidChars = tooManyInvalidChars(pageContent);
            if (invalidChars != null) return invalidChars;

            int contentLength = pageContent.getBytes(
                StandardCharsets.UTF_8).length;
            Triple<String, PunishType, List<Debug<?>>> invalidPageSize = isInvalidPageSize(contentLength);
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
            return new Triple<>(
                "interacted with too large book", PunishType.KICK,
                Arrays.asList(new Debug<>("Total", totalBytes), new Debug<>("Max", allowedBytes))
            );
        }

        return null;
    }

    private static @Nullable Triple<String, PunishType, List<Debug<?>>> isInvalidPageSize(int contentLength) {
        if (contentLength > 256 * 4) {
            return new Triple<>(
                "interacted with an invalid item", PunishType.KICK,
                Collections.singletonList(new Debug<>("Tag", "Page byte size"))
            );
        }
        return null;
    }

    private static @Nullable Triple<String, PunishType, List<Debug<?>>> tooManyInvalidChars(String pageContent) {
        int oversizedChars = 0;
        for (int charIndex = 0; charIndex < pageContent.length(); charIndex++) {
            char currentChar = pageContent.charAt(charIndex);
            if (String.valueOf(currentChar).getBytes().length > 1) {
                oversizedChars++;
                if (oversizedChars > 15) {
                    return new Triple<>(
                        "interacted with an invalid item", PunishType.KICK,
                        Collections.singletonList(new Debug<>("Tag", "Big characters"))
                    );
                }
            }
        }
        return null;
    }

    private static @Nullable Triple<String, PunishType, List<Debug<?>>> checkFieldReadable(String pageContent) {
        if (FieldReader.isReadable(pageContent) && !pageContent.isEmpty()) {
            return new Triple<>(
                "interacted with an invalid item", PunishType.KICK,
                Collections.singletonList(new Debug<>("Tag", "Not readable"))
            );
        }
        return null;
    }

    private static @Nullable Triple<String, PunishType, List<Debug<?>>> isExtraFrequency(String pageContent) {
        if (pageContent.split("extra").length > 8.0) {
            return new Triple<>(
                "interacted with an invalid item", PunishType.KICK,
                Collections.singletonList(new Debug<>("Tag", "Extra frequency"))
            );
        }
        return null;
    }

    private static @Nullable Triple<String, PunishType, List<Debug<?>>> isInvalidColor(String strippedContent) {
        if (strippedContent.length() > 256.0) {
            return new Triple<>(
                "interacted with an invalid item", PunishType.KICK,
                Collections.singletonList(new Debug<>("Tag", "Color Code"))
            );
        }
        return null;
    }

    private @Nullable Triple<String, PunishType, List<Debug<?>>> isDuplicatedContent(String pageContent) {
        if (pageContent.equalsIgnoreCase(lastContent)) {
            if (lastContentCount++ > 4) {
                return new Triple<>(
                    "interacted with an invalid item", PunishType.KICK,
                    Arrays.asList(new Debug<>("Tag", "Many equal pages"), new Debug<>("Count", lastContentCount))
                );
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
