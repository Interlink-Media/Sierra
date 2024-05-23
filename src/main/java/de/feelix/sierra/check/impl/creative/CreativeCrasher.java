package de.feelix.sierra.check.impl.creative;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.impl.creative.impl.*;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.PunishType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class represents an item detection runner that handles the detection
 * and processing of items for Sierra plugin.
 * It extends the SierraDetection class and implements the IngoingProcessor interface.
 */
@SierraCheckData(checkType = CheckType.CREATIVE)
public class CreativeCrasher extends SierraDetection implements IngoingProcessor {

    /**
     * List of ItemCheck instances used to perform item checks.
     *
     * <p>
     * The checks list stores instances of classes that implement the ItemCheck interface. These classes are
     * responsible for handling specific item checks.
     * </p>
     *
     * <p>
     * The checks list is initialized as an empty ArrayList and can be modified by adding or removing ItemCheck
     * instances.
     * </p>
     *
     * @see ItemCheck
     */
    private final List<ItemCheck> checks = new ArrayList<>();

    /**
     * The maximum number of recursion levels allowed.
     */
    private static final int MAX_RECURSIONS = 30;

    /**
     * The variable ITEMS_KEY is a private static final String that represents the key for accessing the "Items" field.
     * <p>
     * The "Items" field is used for storing a collection of items in a data structure, and this key is used to retrieve
     * the value associated with this field.
     * <p>
     * Example usage:
     * String itemsKey = ITEMS_KEY;
     *
     * @since 1.0
     */
    private static final String ITEMS_KEY = "Items";

    /**
     * Represents the key used to identify a tag.
     * This key is used in various operations where tags are processed.
     */
    private static final String TAG_KEY = "tag";

    /**
     * Represents the key used for the block entity tag in the Sierra system.
     * The block entity tag stores additional data for a specific block entity in Minecraft.
     */
    private static final String BLOCK_ENTITY_TAG_KEY = "BlockEntityTag";

    /**
     * The MAX_ITEMS variable represents the maximum number of items allowed.
     * <p>
     * It is used in the ItemDetectionRunner class to set a limit on the number of items that can be processed.
     * <p>
     * The MAX_ITEMS variable is a private static final integer with a value of 54.
     * <p>
     * This variable is important for preventing potential crashes or exploits caused by an excessive number of items.
     * By setting a reasonable maximum value for the number of items, it helps ensure the stability and security of
     * the application.
     * <p>
     * This variable is not intended to be modified during runtime and is marked as final to prevent any accidental
     * changes.
     */
    private static final int MAX_ITEMS = 54;

    /**
     * The ItemDetectionRunner class is responsible for running item detection checks on player data.
     *
     * @param playerData The PlayerData object containing the player's data
     */
    public CreativeCrasher(PlayerData playerData) {
        super(playerData);

        this.addCreativeChecks(
            new CreativeMap(),
            new CreativeClientBookCrash(),
            new PotionLimit(),
            new BooksProtocol(),
            new CreativeAnvil(),
            new FireworkSize(),
            new InvalidPlainNbt()
        );

        if (Sierra.getPlugin().getSierraConfigEngine().config().getInt("max-enchantment-level", 5) != -1) {
            this.addCreativeChecks(new EnchantLimit());
        }

        // Important check. Always import
        this.addCreativeChecks(new CreativeSkull());
    }


    /**
     * Handles the packet receive event and performs item detection checks on the player data.
     *
     * @param event      The packet receive event
     * @param playerData The player data object
     */
    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {

        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-creative-crasher", true)) {
            return;
        }

        if (playerData == null) return;

        ItemStack itemStack = null;

        if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            if (playerData.getGameMode() != GameMode.CREATIVE) {
                return;
            }

            WrapperPlayClientCreativeInventoryAction wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientCreativeInventoryAction(event), playerData::exceptionDisconnect);

            itemStack = wrapper.getItemStack();
        } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {

            WrapperPlayClientClickWindow wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientClickWindow(event),
                playerData::exceptionDisconnect
            );

            if (wrapper.getCarriedItemStack() == null) {
                return;
            }

            itemStack = wrapper.getCarriedItemStack();
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {

            WrapperPlayClientPlayerBlockPlacement wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientPlayerBlockPlacement(event),
                playerData::exceptionDisconnect
            );

            if (!wrapper.getItemStack().isPresent()) {
                return;
            }

            itemStack = wrapper.getItemStack().get();
        }

        if (itemStack == null) return;

        NBTCompound compound = itemStack.getNBT();
        //when the compound has block entity tag, do recursion to find nested/hidden items
        if (compound != null && compound.getTags().containsKey("BlockEntityTag")) {
            NBTCompound blockEntityTag = compound.getCompoundTagOrNull("BlockEntityTag");
            //reset recursion count to prevent false kicks
            playerData.setRecursionCount(0);
            recursion(event, playerData, itemStack, blockEntityTag);
        } else if (compound != null) {
            //if this gets called, it's not a container, so we don't need to do recursion
            for (ItemCheck check : checks) {
                //Maybe add a check result class, so that we can have more detailed verbose output...
                Pair<String, PunishType> crashDetails = check.handleCheck(event, itemStack, compound, playerData);
                if (crashDetails != null) {
                    violation(event, ViolationDocument.builder()
                        .debugInformation(crashDetails.getFirst())
                        .punishType(crashDetails.getSecond())
                        .build());
                }
            }
        }
    }

    /**
     * Performs a recursive operation on the given event, player data, clicked item, and block entity tag.
     *
     * @param event          The packet receive event
     * @param data           The player data object
     * @param clickedItem    The clicked item
     * @param blockEntityTag The NBT compound representing the block entity tag
     */
    private void recursion(PacketReceiveEvent event, PlayerData data, ItemStack clickedItem,
                           NBTCompound blockEntityTag) {

        if (exceededRecursionMax(data)) {
            sendViolation(event, data.getRecursionCount(), PunishType.BAN);
            return;
        }

        if (!blockEntityTag.getTags().containsKey(ITEMS_KEY)) return;

        NBTList<NBTCompound> items = blockEntityTag.getCompoundListTagOrNull(ITEMS_KEY);

        if (items == null) return;

        if (exceededMaxItems(items)) {
            sendViolation(event, items.size(), PunishType.BAN);
            return;
        }

        processItems(event, data, clickedItem, items);
    }

    /**
     * Checks if the recursion count has exceeded the maximum limit.
     *
     * @param data The PlayerData object containing the player's data
     * @return true if the recursion count has exceeded the maximum limit, false otherwise
     */
    private boolean exceededRecursionMax(PlayerData data) {
        int newCount = data.getRecursionCount() + 1;
        data.setRecursionCount(newCount);
        return newCount > MAX_RECURSIONS;
    }

    /**
     * Checks if the number of items exceeds the maximum allowed limit.
     *
     * @param items The list of NBT compounds representing the items
     * @return true if the number of items exceeds the maximum limit, false otherwise
     */
    private boolean exceededMaxItems(NBTList<NBTCompound> items) {
        return items.size() > MAX_ITEMS;
    }

    /**
     * Sends a violation based on the provided event, information, and punishment type.
     *
     * @param event      The PacketReceiveEvent triggering the violation
     * @param info       The information related to the violation
     * @param punishType The punishment type to be applied
     */
    private void sendViolation(PacketReceiveEvent event, int info,
                               @SuppressWarnings("SameParameterValue") PunishType punishType) {
        violation(event, ViolationDocument.builder()
            .debugInformation("Violation Info: " + info)
            .punishType(punishType)
            .build());
    }

    /**
     * Processes the items in the given list by checking for specific tags and performing actions accordingly.
     *
     * @param event       The packet receive event.
     * @param data        The player data object.
     * @param clickedItem The clicked item.
     * @param items       The list of NBT compounds representing the items.
     */
    private void processItems(PacketReceiveEvent event, PlayerData data, ItemStack clickedItem,
                              NBTList<NBTCompound> items) {
        for (int i = 0; i < items.size(); i++) {
            NBTCompound item = items.getTag(i);
            if (item.getTags().containsKey(TAG_KEY)) {
                if (processTaggedItem(event, data, clickedItem, item)) {
                    return;
                }
            } else if (callDefaultChecks(event, data, clickedItem, item)) {
                return;
            }
        }
    }

    /**
     * Processes a tagged item by performing various checks and actions based on the item's tags.
     *
     * @param event       The packet receive event triggering the processing of the tagged item.
     * @param data        The player data object.
     * @param clickedItem The clicked item.
     * @param item        The NBT compound representing the tagged item.
     * @return true if any default checks were triggered and handled, otherwise false.
     */
    private boolean processTaggedItem(PacketReceiveEvent event, PlayerData data, ItemStack clickedItem,
                                      NBTCompound item) {
        NBTCompound tag = item.getCompoundTagOrNull(TAG_KEY);

        if (tag == null) return false;

        if (callDefaultChecks(event, data, clickedItem, tag)) return true;

        if (tag.getTags().containsKey(BLOCK_ENTITY_TAG_KEY)) {
            NBTCompound recursionBlockEntityTag = tag.getCompoundTagOrNull(BLOCK_ENTITY_TAG_KEY);
            recursion(event, data, clickedItem, recursionBlockEntityTag);
        }
        return false;
    }

    /**
     * Calls the default checks for item detection.
     *
     * @param event       The packet receive event
     * @param data        The player data object
     * @param clickedItem The clicked item
     * @param tag         The NBT compound representing the item's tag
     * @return true if any default checks were triggered and handled, otherwise false
     */
    private boolean callDefaultChecks(PacketReceiveEvent event, PlayerData data, ItemStack clickedItem,
                                      NBTCompound tag) {
        for (ItemCheck check : checks) {
            Pair<String, PunishType> crashDetails = check.handleCheck(event, clickedItem, tag, data);
            if (crashDetails != null) {
                sendViolationDocument(event, data, crashDetails);
                return true;
            }
        }
        return false;
    }

    /**
     * Sends a violation document based on the provided event, player data, and crash details.
     *
     * @param event        The PacketReceiveEvent triggering the violation
     * @param data         The player data object containing the player's data
     * @param crashDetails The crash details associated with the violation
     */
    private void sendViolationDocument(PacketReceiveEvent event, PlayerData data,
                                       Pair<String, PunishType> crashDetails) {
        violation(event, buildViolationDocument(data, crashDetails));
    }

    /**
     * Builds a ViolationDocument object with the provided player data and crash details.
     *
     * @param data         The PlayerData object containing the player's data.
     * @param crashDetails The CrashDetails object containing crash details associated with the violation.
     * @return A ViolationDocument object with the provided player data and crash details.
     */
    private ViolationDocument buildViolationDocument(PlayerData data, Pair<String, PunishType> crashDetails) {
        return ViolationDocument.builder()
            .debugInformation(crashDetails.getFirst() + " | R: " + data.getRecursionCount())
            .punishType(PunishType.BAN)
            .build();
    }

    /**
     * Adds the specified item checks to the list of creative checks.
     *
     * @param checks The item checks to be added
     */
    private void addCreativeChecks(ItemCheck... checks) {
        this.checks.addAll(Arrays.asList(checks));
    }
}
