package de.feelix.sierra.check.impl.book;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CrashDetails;
import de.feelix.sierra.utilities.FieldReader;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.PunishType;
import org.bukkit.ChatColor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// PaperMC
// net.minecraft.server.network.ServerGamePacketListenerImpl#handleEditBook
@SierraCheckData(checkType = CheckType.BOOK)
public class MassiveBook extends SierraDetection implements IngoingProcessor {

    private String lastContent      = "";
    private int    lastContentCount = 0;

    public static final String[] MOJANG_CRASH_TRANSLATIONS = {"translation.test.invalid", "translation.test.invalid2"};

    private static final String key = "wveb54yn4y6y6hy6hb54yb5436by5346y3b4yb343yb453by45b34y5by34yb543yb54y5 "
                                      + "h3y4h97,i567yb64t5vr2c43rc434v432tvt4tvybn4n6n57u6u57m6m6678mi68,867,79o,"
                                      + "o97o,"
                                      +
                                      "978iun7yb65453v4tyv34t4t3c2cc423rc334tcvtvt43tv45tvt5t5v43tv5345tv43tv5355vt5t3tv5t533v5t45tv43vt4355t54fwveb54yn4y6y6hy6hb54yb5436by5346y3b4yb343yb453by45b34y5by34yb543yb54y5 h3y4h97,i567yb64t5vr2c43rc434v432tvt4tvybn4n6n57u6u57m6m6678mi68,867,79o,o97o,978iun7yb65453v4tyv34t4t3c2cc423rc334tcvtvt43tv45tvt5t5v43tv5345tv43tv5355vt5t3tv5t533v5t45tv43vt4355t54fwveb54yn4y6y6hy6hb54yb5436by5346y3b4yb343yb453by45b34y5by34yb543yb54y5 h3y4h97,i567yb64t5";

    public MassiveBook(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void handle(PacketReceiveEvent event, PlayerData data) {

        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-book-crasher", true)) {
            return;
        }

        boolean blockBooks = Sierra.getPlugin().getSierraConfigEngine().config()
            .getBoolean("disable-books-completely", false);

        List<String> pageList = new ArrayList<>();

        if (event.getPacketType() == PacketType.Play.Client.EDIT_BOOK) {

            if (blockBooks) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Used book while disabled (edit book)")
                    .punishType(PunishType.BAN)
                    .build());
                return;
            }

            WrapperPlayClientEditBook wrapper = new WrapperPlayClientEditBook(event);
            pageList.addAll(wrapper.getPages());

        } else if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {

            WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);

            // Make sure it's a book payload
            if (wrapper.getChannelName().contains("MC|BEdit") || wrapper.getChannelName().contains("MC|BSign")) {
                Object buffer = null;
                try {
                    buffer = UnpooledByteBufAllocationHelper.buffer();
                    ByteBufHelper.writeBytes(buffer, wrapper.getData());
                    PacketWrapper<?> universalWrapper = PacketWrapper.createUniversalPacketWrapper(buffer);

                    ItemStack wrappedItemStack = universalWrapper.readItemStack();

                    if (wrappedItemStack.getType() == ItemTypes.WRITTEN_BOOK
                        || wrappedItemStack.getType() == ItemTypes.WRITTEN_BOOK && blockBooks) {
                        violation(event, ViolationDocument.builder()
                            .debugInformation("Used book while disabled (plugin message)")
                            .punishType(PunishType.BAN)
                            .build());
                    }

                    if ((wrappedItemStack.getType() != ItemTypes.WRITABLE_BOOK
                         || wrappedItemStack.getType() != ItemTypes.WRITTEN_BOOK)) {
                        return;
                    }

                    if (invalidTitleOrAuthor(wrappedItemStack)) {
                        removeTags(wrappedItemStack);
                        violation(event, ViolationDocument.builder()
                            .debugInformation("Invalid author in payload")
                            .punishType(PunishType.BAN)
                            .build());
                    }

                    pageList.addAll(this.getPages(wrappedItemStack));
                } finally {
                    ByteBufHelper.release(buffer);
                }
            }

        } else if (event.getPacketType() == PacketType.Play.Client.PICK_ITEM) {

            WrapperPlayClientPickItem wrapper = new WrapperPlayClientPickItem(event);

            Object buffer = null;
            try {
                buffer = UnpooledByteBufAllocationHelper.buffer();
                ByteBufHelper.writeBytes(buffer, wrapper.getBuffer());
                PacketWrapper<?> universalWrapper = PacketWrapper.createUniversalPacketWrapper(buffer);

                ItemStack wrappedItemStack = universalWrapper.readItemStack();

                if ((wrappedItemStack.getType() == ItemTypes.WRITTEN_BOOK
                     || wrappedItemStack.getType() == ItemTypes.WRITTEN_BOOK) && blockBooks) {
                    violation(event, ViolationDocument.builder()
                        .debugInformation("Used book while disabled (pick item)")
                        .punishType(PunishType.BAN)
                        .build());
                }

                if (wrappedItemStack.getType() != ItemTypes.WRITABLE_BOOK
                    || wrappedItemStack.getType() != ItemTypes.WRITTEN_BOOK) {
                    return;
                }

                if (invalidTitleOrAuthor(wrappedItemStack)) {
                    removeTags(wrappedItemStack);
                    violation(event, ViolationDocument.builder()
                        .debugInformation("Invalid author in pick item")
                        .punishType(PunishType.BAN)
                        .build());
                }

                pageList.addAll(this.getPages(wrappedItemStack));
            } finally {
                ByteBufHelper.release(buffer);
            }

        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);

            if (wrapper.getItemStack().isPresent()) {

                if ((wrapper.getItemStack().get().getType() == ItemTypes.WRITTEN_BOOK
                     || wrapper.getItemStack().get().getType() == ItemTypes.WRITTEN_BOOK) && blockBooks) {
                    violation(event, ViolationDocument.builder()
                        .debugInformation("Used book while disabled (block place)")
                        .punishType(PunishType.BAN)
                        .build());
                }

                if ((wrapper.getItemStack().get().getType() != ItemTypes.WRITABLE_BOOK
                     || wrapper.getItemStack().get().getType() != ItemTypes.WRITTEN_BOOK)) {
                    return;
                }

                if (invalidTitleOrAuthor(wrapper.getItemStack().get())) {
                    removeTags(wrapper.getItemStack().get());
                    violation(event, ViolationDocument.builder()
                        .debugInformation("Invalid author in block place")
                        .punishType(PunishType.BAN)
                        .build());
                }

                pageList.addAll(this.getPages(wrapper.getItemStack().get()));
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {

            if (getPlayerData() != null && getPlayerData().getGameMode() != GameMode.CREATIVE) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Spoofed creative game-mode")
                    .punishType(PunishType.BAN)
                    .build());
                return;
            }

            WrapperPlayClientCreativeInventoryAction wrapper = new WrapperPlayClientCreativeInventoryAction(event);

            int slot = wrapper.getSlot();
            if (slot >= 100 || slot < -1) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Invalid creative slot at: " + slot)
                    .punishType(PunishType.KICK)
                    .build());
                return;
            }

            if ((wrapper.getItemStack().getType() == ItemTypes.WRITTEN_BOOK
                 || wrapper.getItemStack().getType() == ItemTypes.WRITTEN_BOOK) && blockBooks) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Used book while disabled (creative)")
                    .punishType(PunishType.BAN)
                    .build());
            }

            if ((wrapper.getItemStack().getType() != ItemTypes.WRITABLE_BOOK
                 || wrapper.getItemStack().getType() != ItemTypes.WRITTEN_BOOK)) {
                return;
            }

            if (invalidTitleOrAuthor(wrapper.getItemStack())) {
                removeTags(wrapper.getItemStack());
                violation(event, ViolationDocument.builder()
                    .debugInformation("Invalid author in creative inv")
                    .punishType(PunishType.BAN)
                    .build());
            }
            pageList.addAll(this.getPages(wrapper.getItemStack()));

        } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow wrapper = new WrapperPlayClientClickWindow(event);
            if (wrapper.getCarriedItemStack() != null) {

                if ((wrapper.getCarriedItemStack().getType() == ItemTypes.WRITTEN_BOOK
                     || wrapper.getCarriedItemStack().getType() == ItemTypes.WRITTEN_BOOK)
                    && blockBooks) {

                    violation(event, ViolationDocument.builder()
                        .debugInformation("Used book while disabled (click window)")
                        .punishType(PunishType.BAN)
                        .build());
                }

                if ((wrapper.getCarriedItemStack().getType() != ItemTypes.WRITABLE_BOOK
                     || wrapper.getCarriedItemStack().getType() != ItemTypes.WRITTEN_BOOK)) {
                    return;
                }

                if (invalidTitleOrAuthor(wrapper.getCarriedItemStack())) {
                    removeTags(wrapper.getCarriedItemStack());
                    violation(event, ViolationDocument.builder()
                        .debugInformation("Invalid author in click window")
                        .punishType(PunishType.BAN)
                        .build());
                }
                pageList.addAll(this.getPages(wrapper.getCarriedItemStack()));
            }
        } else {
            return;
        }

        CrashDetails invalid = validatePages(pageList);
        if (invalid != null) {
            violation(event, ViolationDocument.builder()
                .debugInformation(invalid.getDetails())
                .punishType(PunishType.BAN)
                .build());
        }
    }

    private void removeTags(ItemStack carriedItemStack) {
        Objects.requireNonNull(carriedItemStack.getNBT()).removeTag("pages");
        Objects.requireNonNull(carriedItemStack.getNBT()).removeTag("author");
        Objects.requireNonNull(carriedItemStack.getNBT()).removeTag("title");
    }

    private CrashDetails validatePages(List<String> pageList) {

        long   totalBytes            = 0;
        double maxPageSizeMultiplier = 0.98;
        int    maxPageSize           = 2560;
        long   allowedBytes          = maxPageSize;

        if (pageList.size() > 50) {
            return new CrashDetails("Too many pages", PunishType.KICK);
        }

        double  maxColorLength    = 256.0;

        for (String pageContent : pageList) {

            if ((pageContent.contains(key) || pageContent.equalsIgnoreCase(key))) {
                return new CrashDetails("Contains invalid key", PunishType.BAN);
            }

            if (pageContent.equalsIgnoreCase(lastContent)) {
                if (lastContentCount++ > 4) {
                    return new CrashDetails("Too many equal pages", PunishType.KICK);
                }
            } else {
                lastContentCount = 0;
            }

            lastContent = pageContent;

            String strippedContent = ChatColor.stripColor(pageContent.replaceAll("\\+", ""));
            //noinspection ConstantValue
            if (strippedContent == null || strippedContent.equals("null")) {
                return new CrashDetails("Contains invalid color code", PunishType.BAN);
            }

            if (strippedContent.length() > maxColorLength) {
                return new CrashDetails("Invalid color code signature", PunishType.BAN);
            }

            if (pageContent.split("extra").length > 8.0) {
                return new CrashDetails("Invalid extra frequency", PunishType.BAN);
            }

            if (FieldReader.isNotReadable(pageContent) && !pageContent.isEmpty()) {
                return new CrashDetails("Field is not readable", PunishType.BAN);
            }

            String noSpaces = pageContent.replace(" ", "");
            if (noSpaces.startsWith("{\"translate\"")) {
                for (String crashTranslation : MOJANG_CRASH_TRANSLATIONS) {
                    String translationJson = String.format("{\"translate\":\"%s\"}", crashTranslation);
                    if (pageContent.equalsIgnoreCase(translationJson)) {
                        return new CrashDetails("Mojang crash translation", PunishType.KICK);
                    }
                }
                continue;
            }


            int oversizedChars = 0;
            for (int charIndex = 0; charIndex < pageContent.length(); charIndex++) {
                char currentChar = pageContent.charAt(charIndex);
                if (String.valueOf(currentChar).getBytes().length > 1) {
                    oversizedChars++;
                    if (oversizedChars > 15) {
                        return new CrashDetails("Too many big characters", PunishType.KICK);
                    }
                }
            }

            int contentLength = pageContent.getBytes(StandardCharsets.UTF_8).length;

            if (contentLength > 256 * 4) {
                return new CrashDetails("Invalid page size", PunishType.BAN);
            }

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

            allowedBytes += (long) ((maxPageSize * Math.min(1, Math.max(0.1D, (double) length / 255D)))
                                    * maxPageSizeMultiplier);

            if (multiBytes > 1) {
                // Penalize multi-byte characters
                allowedBytes -= multiBytes;
            }
        }

        // Check if the book size is too large
        if (totalBytes > allowedBytes) {
            return new CrashDetails("Book size is too large", PunishType.BAN);
        }

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
