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

// PaperMC
// net.minecraft.server.network.ServerGamePacketListenerImpl#handleEditBook
@SierraCheckData(checkType = CheckType.BOOK_VALIDATION)
public class BookValidation extends SierraDetection implements IngoingProcessor {

    /**
     * The lastContent variable stores the last content string.
     */
    private String lastContent = "";

    /**
     * Represents the count of the last content.
     * <p>
     * This variable keeps track of the count of the last content processed,
     * which can be used for various purposes in the application.
     * <p>
     * Initial value: 0
     */
    private int lastContentCount = 0;

    /**
     * The MOJANG_CRASH_TRANSLATIONS variable represents an array of translations for Mojang crash messages.
     * It is a public static final String[] variable.
     * <p>
     * Usage:
     * String[] translations = MOJANG_CRASH_TRANSLATIONS;
     * <p>
     * Example:
     * translations[0] = "translation.test.invalid";
     * translations[1] = "translation.test.invalid2";
     */
    private static final String[] MOJANG_CRASH_TRANSLATIONS = {"translation.test.invalid", "translation.test.invalid2"};

    /**
     * Represents a massive book.
     * Inherits from the SierraDetection class.
     *
     * @param playerData The PlayerData associated with the book.
     */
    public BookValidation(PlayerData playerData) {
        super(playerData);
    }

    /**
     * Handles the PacketReceiveEvent and performs various checks on the event and player data.
     * If the "prevent-book-crasher" configuration option is set to true, this method will prevent book crashers.
     *
     * @param event The PacketReceiveEvent to handle
     * @param data  The PlayerData associated with the event
     */
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

            WrapperPlayClientEditBook wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientEditBook(event),
                data::exceptionDisconnect
            );

            pageList.addAll(wrapper.getPages());

        } else if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {

            WrapperPlayClientPluginMessage wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientPluginMessage(event),
                data::exceptionDisconnect
            );

            // Make sure it's a book payload
            if (wrapper.getChannelName().contains("MC|BEdit") || wrapper.getChannelName().contains("MC|BSign")) {
                Object buffer = null;
                try {
                    buffer = UnpooledByteBufAllocationHelper.buffer();
                    ByteBufHelper.writeBytes(buffer, wrapper.getData());
                    PacketWrapper<?> universalWrapper = PacketWrapper.createUniversalPacketWrapper(buffer);

                    ItemStack wrappedItemStack = universalWrapper.readItemStack();

                    if ((wrappedItemStack.getType() == ItemTypes.WRITTEN_BOOK
                         || wrappedItemStack.getType() == ItemTypes.WRITTEN_BOOK) && blockBooks) {
                        violation(event, ViolationDocument.builder()
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

            WrapperPlayClientPickItem wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientPickItem(event),
                data::exceptionDisconnect
            );

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
                    && wrappedItemStack.getType() != ItemTypes.WRITTEN_BOOK) {
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
            WrapperPlayClientPlayerBlockPlacement wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientPlayerBlockPlacement(event),
                data::exceptionDisconnect
            );

            if (wrapper.getItemStack().isPresent()) {

                if ((wrapper.getItemStack().get().getType() == ItemTypes.WRITTEN_BOOK
                     || wrapper.getItemStack().get().getType() == ItemTypes.WRITTEN_BOOK) && blockBooks) {
                    violation(event, ViolationDocument.builder()
                        .debugInformation("Used book while disabled (block place)")
                        .punishType(PunishType.BAN)
                        .build());
                }

                if (wrapper.getItemStack().get().getType() != ItemTypes.WRITABLE_BOOK
                    && wrapper.getItemStack().get().getType() != ItemTypes.WRITTEN_BOOK) {
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

            WrapperPlayClientCreativeInventoryAction wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientCreativeInventoryAction(event),
                data::exceptionDisconnect
            );

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

            if (wrapper.getItemStack().getType() != ItemTypes.WRITABLE_BOOK
                && wrapper.getItemStack().getType() != ItemTypes.WRITTEN_BOOK) {
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

            WrapperPlayClientClickWindow wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientClickWindow(event),
                data::exceptionDisconnect
            );

            if (wrapper.getCarriedItemStack() != null) {

                if ((wrapper.getCarriedItemStack().getType() == ItemTypes.WRITTEN_BOOK
                     || wrapper.getCarriedItemStack().getType() == ItemTypes.WRITTEN_BOOK)
                    && blockBooks) {

                    violation(event, ViolationDocument.builder()
                        .debugInformation("Used book while disabled (click window)")
                        .punishType(PunishType.BAN)
                        .build());
                }

                if (wrapper.getCarriedItemStack().getType() != ItemTypes.WRITABLE_BOOK
                    && wrapper.getCarriedItemStack().getType() != ItemTypes.WRITTEN_BOOK) {
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
        }

        Pair<String, PunishType> invalid = validatePages(pageList);
        if (invalid != null) {
            violation(event, ViolationDocument.builder()
                .debugInformation(invalid.getFirst())
                .punishType(PunishType.BAN)
                .build());
        }
    }

    /**
     * Removes specific tags from an ItemStack's NBT.
     *
     * @param carriedItemStack The ItemStack from which to remove the tags.
     */
    private void removeTags(ItemStack carriedItemStack) {
        Objects.requireNonNull(carriedItemStack.getNBT()).removeTag("pages");
        Objects.requireNonNull(carriedItemStack.getNBT()).removeTag("author");
        Objects.requireNonNull(carriedItemStack.getNBT()).removeTag("title");
    }

    /**
     * Validates the list of pages.
     *
     * @param pageList The list of pages to validate.
     * @return The CrashDetails object if there is an issue with the pages, null otherwise.
     * @see Pair
     */
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
                return new Pair<>("Contains invalid color code", PunishType.BAN);
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

            int contentLength = pageContent.getBytes(StandardCharsets.UTF_8).length;

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

            allowedBytes += (long) ((2560 * Math.min(1, Math.max(0.1D, (double) length / 255D)))
                                    * 0.98);

            if (multiBytes > 1) {
                // Penalize multi-byte characters
                allowedBytes -= multiBytes;
            }
        }

        // Check if the book size is too large
        if (totalBytes > allowedBytes) {
            return new Pair<>("Book size is too large", PunishType.BAN);
        }

        return null;
    }

    /**
     * Determines if the given page size is invalid.
     *
     * @param contentLength The length of the page content.
     * @return The CrashDetails object if the page size is invalid, null otherwise.
     * @see Pair
     */
    private static @Nullable Pair<String, PunishType>  isInvalidPageSize(int contentLength) {
        if (contentLength > 256 * 4) {
            return new Pair<>("Invalid page size", PunishType.BAN);
        }
        return null;
    }

    /**
     * Determines if the given page content contains too many oversized characters.
     *
     * @param pageContent The content of the page to check.
     * @return The CrashDetails object if there are too many oversized characters, null otherwise.
     */
    private static @Nullable Pair<String, PunishType>  tooManyInvalidChars(String pageContent) {
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

    /**
     * Checks if the given page content is readable.
     *
     * @param pageContent The content of the page to check.
     * @return A CrashDetails object if the field is not readable, null otherwise.
     * @see Pair
     */
    private static @Nullable Pair<String, PunishType>  checkFieldReadable(String pageContent) {
        if (FieldReader.isReadable(pageContent) && !pageContent.isEmpty()) {
            return new Pair<>("Field is not readable", PunishType.KICK);
        }
        return null;
    }

    /**
     * Checks if the given page content has an extra frequency.
     *
     * @param pageContent The content of the page to check.
     * @return A CrashDetails object if the page content has an extra frequency, null otherwise.
     * @see Pair
     */
    private static @Nullable Pair<String, PunishType>  isExtraFrequency(String pageContent) {
        if (pageContent.split("extra").length > 8.0) {
            return new Pair<>("Invalid extra frequency", PunishType.BAN);
        }
        return null;
    }

    /**
     * Checks if the given strippedContent is an invalid color code signature.
     *
     * @param strippedContent The stripped content to check.
     * @return A CrashDetails object if the color code signature is invalid, null otherwise.
     * @see Pair
     */
    private static @Nullable Pair<String, PunishType>  isInvalidColor(String strippedContent) {
        if (strippedContent.length() > 256.0) {
            return new Pair<>("Invalid color code signature", PunishType.BAN);
        }
        return null;
    }

    /**
     * Checks if the given page content is duplicated.
     *
     * @param pageContent The content of the page to check.
     * @return A CrashDetails object if the page content is duplicated too many times, null otherwise.
     * @see Pair
     */
    private @Nullable Pair<String, PunishType>  isDuplicatedContent(String pageContent) {
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


    /**
     * Checks if the title or author of the given ItemStack is invalid.
     *
     * @param itemStack The ItemStack to check.
     * @return {@code true} if the title or author is invalid, {@code false} otherwise.
     */
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

    /**
     * Retrieves the list of pages stored in the given ItemStack.
     *
     * @param itemStack The ItemStack to retrieve pages from.
     * @return A list of strings representing the pages extracted from the ItemStack.
     */
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