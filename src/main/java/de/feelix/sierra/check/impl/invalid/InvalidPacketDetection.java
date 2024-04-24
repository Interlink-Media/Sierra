package de.feelix.sierra.check.impl.invalid;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetExperience;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.packet.OutgoingProcessor;
import de.feelix.sierra.manager.storage.MenuType;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.*;
import de.feelix.sierra.utilities.types.BannerType;
import de.feelix.sierra.utilities.types.ShulkerBoxType;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.PunishType;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The InvalidPacketDetection class is responsible for detecting and handling invalid packets received from players.
 * It extends several super classes: SierraDetection, IngoingProcessor, and OutgoingProcessor.
 * <p>
 * Class Fields:
 * - type: The type of the lectern packet event
 * - lecternId: The ID of the lectern
 * - lastSlot: The last slot clicked by the player
 * - lastBookUse: The time stamp of the last book use
 * - containerType: The type of the container packet event
 * - containerId: The ID of the container
 * - keepAliveMap: A map to store keep alive packets
 * <p>
 * Class Methods:
 * <p>
 * public InvalidPacketDetection(PlayerData playerData)
 * - Constructor method to create a new instance of InvalidPacketDetection
 * - Parameters:
 * - playerData: The PlayerData object containing the player information
 *
 * @Override public void handle(PacketReceiveEvent event, PlayerData playerData)
 * - Override method to handle incoming packet events
 * - Parameters:
 * - event: The PacketReceiveEvent object representing the received packet event
 * - playerData: The PlayerData object containing the player information
 * <p>
 * private void checkForInvalidSlot(PacketReceiveEvent event, WrapperPlayClientClickWindow wrapper)
 * - Private method to check for invalid slots in the packet event
 * - Parameters:
 * - event: The PacketReceiveEvent object representing the received packet event
 * - wrapper: The WrapperPlayClientClickWindow object representing the packet wrapper
 * <p>
 * private void checkButtonClickPosition(PacketReceiveEvent event, WrapperPlayClientClickWindow wrapper)
 * - Private method to perform Grim check for button click position
 * - Parameters:
 * - event: The PacketReceiveEvent object representing the received packet event
 * - wrapper: The WrapperPlayClientClickWindow object representing the packet wrapper
 * <p>
 * private void checkIfItemIsAvailable(PacketReceiveEvent event, ItemStack itemStack)
 * - Private method to check if the item is available
 * - Parameters:
 * - event: The PacketReceiveEvent object representing the received packet event
 * - itemStack: The ItemStack object representing the item being checked
 * <p>
 * private void checkForInvalidShulker(PacketReceiveEvent event, ItemStack itemStack)
 * - Private method to check for invalid shulker packets
 * - Parameters:
 * - event: The PacketReceiveEvent object representing the received packet event
 * - itemStack: The ItemStack object representing the item being checked
 * <p>
 * private void checkForInvalidContainer(PacketReceiveEvent event, ItemStack itemStack)
 * - Private method to check for invalid container packets
 * - Parameters:
 * - event: The PacketReceiveEvent object representing the received packet event
 * - itemStack: The ItemStack object representing the item being checked
 * <p>
 * private void checkNbtTags(PacketReceiveEvent event, ItemStack itemStack)
 * - Private method to check for invalid NBT tags in packets
 * - Parameters:
 * - event: The PacketReceiveEvent object representing the received packet event
 * - itemStack: The ItemStack object representing the item being checked
 * <p>
 * private void checkForInvalidBanner(PacketReceiveEvent event, ItemStack itemStack)
 * - Private method to check for invalid banner packets
 * - Parameters:
 * - event: The PacketReceiveEvent object representing the received packet event
 * - itemStack: The ItemStack object representing the item being checked
 * @Override public void handle(PacketSendEvent event, PlayerData playerData)
 * - Override method to handle outgoing packet events
 * - Parameters:
 * - event: The PacketSendEvent object representing the outgoing packet event
 * - playerData: The PlayerData object containing the player information
 * <p>
 * private boolean isSupportedVersion()
 * - Private method to check if the server version is supported
 * - Returns:
 * - true if the server version is supported, false otherwise
 * <p>
 * private boolean isSupportedVersion(User user)
 * - Private method to check if the server version is supported
 * - Parameters:
 * - user: The User object representing the player
 * - Returns:
 * - true if the server version is supported, false otherwise
 */
@SierraCheckData(checkType = CheckType.INVALID)
public class InvalidPacketDetection extends SierraDetection implements IngoingProcessor, OutgoingProcessor {

    /**
     * Constructs a new InvalidPacketDetection instance for a given player.
     *
     * @param playerData the data of the player who triggered the detection
     */
    public InvalidPacketDetection(PlayerData playerData) {
        super(playerData);
    }

    // https://github.com/PaperMC/Paper/commit/ea2c81e4b9232447f9896af2aac4cd0bf62386fd
    // https://wiki.vg/Inventory
    // https://github.com/GrimAnticheat/Grim/blob/2.0/src/main/java/ac/grim/grimac/checks/impl/crash/CrashD.java
    private MenuType type = MenuType.UNKNOWN;

    /**
     * The variable lecternId is a private integer that represents the ID of a lectern.
     *
     * @since 1.0
     */
    private int lecternId = -1;

    /**
     * Represents the index of the last slot in a collection or array.
     * The initial value is set to -1 indicating that no slots have been assigned yet.
     * <p>
     * This variable is private and can only be accessed within the current class.
     *
     * @since 1.0
     */
    private int lastSlot = -1;

    /**
     * The variable represents the timestamp of the last usage of a book.
     *
     * <p>This variable is updated to the current timestamp whenever a book is used.
     * It provides the ability to keep track of the last time a book was accessed or utilized.</p>
     *
     * <p>Note that the variable is only accessible within the current class.</p>
     *
     * @since (initial release)
     */
    private long lastBookUse = 0L;

    /**
     * Represents the type of container.
     * <p>
     * The containerType variable is an integer that represents the type of container.
     * It is used to differentiate between different types of containers, such as inventories or chests.
     * The default value is -1, indicating an invalid container type.
     * </p>
     * <p>
     * This variable is used in the class InvalidPacketDetection of the SierraCheckData project.
     * </p>
     */
    private int containerType = -1;

    /**
     * Represents the ID of the container.
     * The container ID is used to identify a specific container instance.
     * A negative value (-1) indicates that the container ID is invalid or uninitialized.
     */
    private int containerId = -1;

    /**
     * Represents a map of keep alive timestamps.
     * <p>
     * This map is used to store pairs of timestamps indicating the last time a keep alive packet was sent and received.
     * The map follows a first-in-first-out (FIFO) order, meaning the oldest pair is always removed first when the
     * capacity of the map is reached.
     * </p>
     * <p>
     * The map uses a LinkedList implementation for efficient insertion and removal of elements.
     * </p>
     *
     * @since 1.0
     */
    private final Queue<Pair<Long, Long>> keepAliveMap = new LinkedList<>();

    /**
     * Represents the maximum byte size for a variable.
     *
     * <p>
     * The MAX_BYTE_SIZE variable is a constant that defines the maximum byte size
     * that a variable can have.
     * </p>
     *
     * <p>
     * It is used to restrict the size of a data object or string to prevent
     * excessive memory usage or performance issues.
     * </p>
     *
     * @since 1.0
     */
    private static final int MAX_BYTE_SIZE = 262144;

    /**
     * The WURSTCLIENT_URL constant represents the URL of the Wurst Client website.
     * It is a private static final variable.
     * <p>
     * Example usage:
     * String url = InvalidPacketDetection.WURSTCLIENT_URL;
     */
    private static final String WURSTCLIENT_URL = "www.wurstclient.net";

    /**
     * Handles a PacketReceiveEvent and performs various checks and actions based on the packet type and player data.
     *
     * @param event      The PacketReceiveEvent to handle.
     * @param playerData The PlayerData associated with the player.
     */
    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {

        if (!Sierra.getPlugin()
            .getSierraConfigEngine()
            .config()
            .getBoolean("prevent-invalid-packet", true)) {
            return;
        }

        if (event.getConnectionState() != ConnectionState.PLAY) return;

        if (playerData.getClientVersion() == null) {
            playerData.setClientVersion(event.getUser().getClientVersion());
        }

        //https://netty.io/4.1/api/io/netty/buffer/ByteBuf.html//Sequential Access Indexing
        int capacity = ByteBufHelper.capacity(event.getByteBuf());
        int maxBytes = 64000 * (playerData.getClientVersion().isOlderThan(ClientVersion.V_1_8) ? 2 : 1);
        if (capacity > maxBytes) {
            violation(event, ViolationDocument.builder()
                .debugInformation("Bytes: " + capacity + " Max Bytes: " + maxBytes)
                .punishType(PunishType.KICK)
                .build());
        }
        //https://netty.io/4.1/api/io/netty/buffer/ByteBuf.html//Sequential Access Indexing
        int readableBytes     = ByteBufHelper.readableBytes(event.getByteBuf());
        int maxBytesPerSecond = 64000 * (playerData.getClientVersion().isOlderThan(ClientVersion.V_1_8) ? 2 : 1);
        playerData.setBytesSent(playerData.getBytesSent() + readableBytes);
        if (playerData.getBytesSent() > maxBytesPerSecond) {
            violation(event, ViolationDocument.builder()
                .debugInformation("Bytes Sent: " + playerData.getBytesSent() + " Max Bytes/s: " + maxBytesPerSecond)
                .punishType(PunishType.KICK)
                .build());
        }
        // https://github.com/PaperMC/Paper/commit/e3997543203bc1d86b58b6f1e751b0593228ca7b
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_SETTINGS) {

            WrapperPlayClientSettings wrapper = new WrapperPlayClientSettings(event);

            if (wrapper.getViewDistance() < 2) {
                String format = "Adjusting %s's view distance from %d to 2";
                Logger logger = Sierra.getPlugin().getLogger();
                logger.log(Level.INFO, String.format(format, event.getUser().getName(), wrapper.getViewDistance()));
                wrapper.setViewDistance(2);
            }

            if (wrapper.getLocale() == null) {
                violation(event, ViolationDocument.builder()
                    .punishType(PunishType.KICK)
                    .debugInformation("Locale is null in settings")
                    .build());
            }

        } else if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {

            WrapperPlayClientCreativeInventoryAction wrapper   = new WrapperPlayClientCreativeInventoryAction(event);
            ItemStack                                itemStack = wrapper.getItemStack();
            checkInvalidNbt(event, itemStack);
            checkForInvalidBanner(event, itemStack);
            checkForInvalidContainer(event, itemStack);
            checkForInvalidShulker(event, itemStack);
            checkNbtTags(event, itemStack);

        } else if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {

            WrapperPlayClientEntityAction wrapper = new WrapperPlayClientEntityAction(event);
            if (wrapper.getEntityId() < 0) {
                violation(event, ViolationDocument.builder()
                    .punishType(PunishType.BAN)
                    .debugInformation("Invalid entity action: " + wrapper.getEntityId())
                    .build());
            }

            if (wrapper.getJumpBoost() < 0
                || wrapper.getJumpBoost() > 100) {

                violation(event, ViolationDocument.builder()
                    .debugInformation("boost: " + wrapper.getJumpBoost())
                    .punishType(PunishType.KICK)
                    .build());
            }

        } else if (event.getPacketType() == PacketType.Play.Client.SPECTATE) {

            if (getPlayerData().getGameMode() != GameMode.SPECTATOR) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Spoofed spectator state")
                    .punishType(PunishType.BAN)
                    .build());
            }

        } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW_BUTTON) {

            WrapperPlayClientClickWindowButton wrapper = new WrapperPlayClientClickWindowButton(event);
            if (wrapper.getButtonId() < 0 || wrapper.getWindowId() < 0) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Invalid click slot: " + wrapper.getWindowId() + "/" + wrapper.getButtonId())
                    .punishType(PunishType.BAN)
                    .build());
            }

        } else if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {

            WrapperPlayClientChatMessage wrapper = new WrapperPlayClientChatMessage(event);
            if (wrapper.getMessage().contains("${")) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Send log4j exploit")
                    .punishType(PunishType.KICK)
                    .build());
            }

        } else if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {

            WrapperPlayClientHeldItemChange wrapper = new WrapperPlayClientHeldItemChange(event);
            int                             slot    = wrapper.getSlot();

            if (slot > 36 || slot < 0) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Invalid slot at (HOTBAR): " + slot)
                    .punishType(PunishType.BAN)
                    .build());
            }
            Player player = (Player) event.getPlayer();

            if (player == null) return;

            int length = player.getInventory().getContents().length;

            if (!(wrapper.getSlot() >= 0 && wrapper.getSlot() < length)) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Invalid slot at: " + wrapper.getSlot() + ", max: " + length)
                    .punishType(PunishType.BAN)
                    .build());
            }

            if (slot == lastSlot) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Selected slot twice: " + wrapper.getSlot())
                    .punishType(PunishType.KICK)
                    .build());
            }

            this.lastSlot = slot;

        } else if (event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) {

            WrapperPlayClientTabComplete wrapper = new WrapperPlayClientTabComplete(event);

            if (wrapper.getText() == null) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Send packet with null value")
                    .punishType(PunishType.BAN)
                    .build());
            }

            String    text   = wrapper.getText();
            final int length = text.length();

            // general length limit
            if (length > 256) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("(length) length=" + length)
                    .punishType(PunishType.KICK)
                    .build());
            }

            // Detecting liquid bounce completion exploit
            if (new NBTDetector().find(text)) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Text: " + wrapper.getText())
                    .punishType(PunishType.KICK)
                    .build());
            }

            if ((text.equals("/") || text.trim().isEmpty()) && PacketEvents.getAPI()
                .getServerManager()
                .getVersion()
                .isNewerThanOrEquals(ServerVersion.V_1_13)) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Trimmed empty tab to zero")
                    .punishType(PunishType.KICK)
                    .build());
            }

            // Papers patch
            final int index;
            if (text.length() > 64 && ((index = text.indexOf(' ')) == -1 || index >= 64)) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("(invalid) length=" + length)
                    .punishType(PunishType.KICK)
                    .build());
            }

        } else if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);

            String channelName = wrapper.getChannelName();

            if (channelName.equals("MC|BEdit")
                || channelName.equals("MC|BSign")
                || channelName.equals("minecraft:bedit")
                || channelName.equals("minecraft:bsign")) {

                if (System.currentTimeMillis() - this.lastBookUse > 60000L) {
                    violation(event, ViolationDocument.builder()
                        .debugInformation("Send book sign, without book use")
                        .punishType(PunishType.MITIGATE)
                        .build());
                }
            }

            if (channelName.contains("${")) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Send invalid channel in plugin message")
                    .punishType(PunishType.KICK)
                    .build());
            }

            if (wrapper.getData().length == 0) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Send empty data")
                    .punishType(PunishType.KICK)
                    .build());
            }

            String payload = new String(wrapper.getData(), StandardCharsets.UTF_8);

            if (payload.isEmpty()) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("String is empty")
                    .punishType(PunishType.KICK)
                    .build());
            }

            if (channelName.equalsIgnoreCase("MC|PickItem")
                || channelName.equalsIgnoreCase("MC|TrSel")) {

                if (payload.equalsIgnoreCase("N")) {
                    if (this.violations() > 3) {
                        violation(event, ViolationDocument.builder()
                            .debugInformation("Console Spammer of Liquid #2")
                            .punishType(PunishType.BAN)
                            .build());
                    }
                    violation(event, ViolationDocument.builder()
                        .debugInformation("Console Spammer of Liquid #1")
                        .punishType(PunishType.MITIGATE)
                        .build());
                }
            }

            if (channelName.contains("MC|BEdit") || channelName.contains("MC|BSign")) {
                Player player = (Player) event.getPlayer();
                //noinspection deprecation
                ItemStack itemStack = SpigotConversionUtil.fromBukkitItemStack(player.getItemInHand());

                if (itemStack == null) return;

                if (itemStack.getType() != ItemTypes.BOOK
                    && itemStack.getType() != ItemTypes.WRITABLE_BOOK
                    && itemStack.getType() != ItemTypes.WRITTEN_BOOK) {
                    violation(event, ViolationDocument.builder()
                        .debugInformation("No book in hand")
                        .punishType(PunishType.KICK)
                        .build());
                }
            }

            String[] channels = payload.split("\0");

            if (channelName.equals("REGISTER")) {
                if (playerData.getChannels().size() + channels.length > 124 || channels.length > 124) {
                    violation(event, ViolationDocument.builder()
                        .debugInformation("Invalid channel length: " + channels.length)
                        .punishType(PunishType.BAN)
                        .build());
                } else {
                    for (String channel : channels) {
                        playerData.getChannels().add(channel);
                    }
                }
            } else if (channelName.equals("UNREGISTER")) {
                for (String channel : channels) {
                    playerData.getChannels().remove(channel);
                }
            }

        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {

            WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);

            if (wrapper.getSequence() < 0 && isSupportedVersion(event.getUser())) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Invalid sequence in block place")
                    .punishType(PunishType.BAN)
                    .build());
            }

            if (wrapper.getItemStack().isPresent()) {

                ItemStack itemStack = wrapper.getItemStack().get();

                if (itemStack.getType() == ItemTypes.WRITABLE_BOOK
                    || itemStack.getType() == ItemTypes.WRITTEN_BOOK
                    || itemStack.getType() == ItemTypes.BOOK) {
                    this.lastBookUse = System.currentTimeMillis();
                }

                checkInvalidNbt(event, itemStack);
                checkForInvalidBanner(event, itemStack);
                checkForInvalidContainer(event, itemStack);
                checkForInvalidShulker(event, itemStack);
                checkNbtTags(event, itemStack);
            }

        } else if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
            WrapperPlayClientSteerVehicle wrapper = new WrapperPlayClientSteerVehicle(event);

            float forward  = wrapper.getForward();
            float sideways = wrapper.getSideways();

            if (forward > 0.98f || forward < -0.98f) {
                violation(event, ViolationDocument.builder()
                    .punishType(PunishType.BAN)
                    .debugInformation(String.format("forward: %.2f", forward))
                    .build());
            }

            if (sideways > 0.98f || sideways < -0.98f) {
                violation(event, ViolationDocument.builder()
                    .punishType(PunishType.BAN)
                    .debugInformation(String.format("sideways: %.2f", sideways))
                    .build());
            }
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {

            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

            if (wrapper.getEntityId() < 0 || event.getUser().getEntityId() == wrapper.getEntityId()) {
                violation(event, ViolationDocument.builder()
                    .punishType(PunishType.BAN)
                    .debugInformation("Entity id at " + wrapper.getEntityId())
                    .build());

            }
        } else if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {

            // Check original by https://github.com/GrimAnticheat/Grim/blob/2.0/src/main/java/ac/grim/grimac/checks/impl/badpackets/BadPacketsO.java
            WrapperPlayClientKeepAlive packet = new WrapperPlayClientKeepAlive(event);

            long    id    = packet.getId();
            boolean hasID = false;

            for (Pair<Long, Long> iterator : keepAliveMap) {
                if (iterator.getFirst() == id) {
                    hasID = true;
                    break;
                }
            }

            if (!hasID) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Unexpected id: " + id)
                    .punishType(PunishType.MITIGATE)
                    .build());
            } else { // Found the ID, remove stuff until we get to it (to stop very slow memory leaks)
                Pair<Long, Long> data;
                do {
                    data = keepAliveMap.poll();
                    if (data == null) break;
                } while (data.getSecond() != id);
            }

        } else if (event.getPacketType() == PacketType.Play.Client.NAME_ITEM) {
            WrapperPlayClientNameItem wrapper = new WrapperPlayClientNameItem(event);

            if (wrapper.getItemName().contains("${")) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Send log4j exploit in item name")
                    .punishType(PunishType.KICK)
                    .build());
            }

            int length = wrapper.getItemName().length();

            if (length > 0 && FieldReader.isReadable(wrapper.getItemName())) {
                violation(event, ViolationDocument.builder()
                    .punishType(PunishType.KICK)
                    .debugInformation("Name is not readable: " + wrapper.getItemName())
                    .build());
            }

            if (length > 50) {
                violation(event, ViolationDocument.builder()
                    .punishType(PunishType.MITIGATE)
                    .debugInformation("Name longer than 50: " + length)
                    .build());
            }

        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING
                   && isSupportedVersion(event.getUser())) {

            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);
            if (dig.getSequence() < 0 && isSupportedVersion(event.getUser())) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Invalid sequence in dig")
                    .punishType(PunishType.BAN)
                    .build());
            }

        } else if (event.getPacketType() == PacketType.Play.Client.USE_ITEM && isSupportedVersion(
            event.getUser())) {
            WrapperPlayClientUseItem use = new WrapperPlayClientUseItem(event);
            if (use.getSequence() < 0 && isSupportedVersion(event.getUser())) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Invalid sequence in use item")
                    .punishType(PunishType.BAN)
                    .build());
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {

            WrapperPlayClientClickWindow wrapper = new WrapperPlayClientClickWindow(event);

            if (isSupportedVersion(ServerVersion.V_1_14)) {
                int clickType = wrapper.getWindowClickType().ordinal();
                int button    = wrapper.getButton();
                int windowId  = wrapper.getWindowId();

                if (type == MenuType.LECTERN && windowId > 0 && windowId == lecternId) {
                    violation(event, ViolationDocument.builder()
                        .punishType(PunishType.KICK)
                        .debugInformation("clickType=" + clickType + ", button=" + button)
                        .build());
                }
            }

            checkButtonClickPosition(event, wrapper);

            ItemStack carriedItemStack = wrapper.getCarriedItemStack();
            checkInvalidNbt(event, carriedItemStack);
            checkForInvalidContainer(event, carriedItemStack);
            checkForInvalidShulker(event, carriedItemStack);
            checkForInvalidBanner(event, carriedItemStack);
            checkNbtTags(event, carriedItemStack);
            checkForInvalidSlot(event, wrapper);

            int clickType = wrapper.getWindowClickType().ordinal();
            int button    = wrapper.getButton();
            int windowId  = wrapper.getWindowId();
            int slot      = wrapper.getSlot();

            // ref: https://github.com/CCBlueX/LiquidBounce/blob/nextgen/src/main/kotlin/net/ccbluex/liquidbounce/features/module/modules/exploit/servercrasher/exploits/PaperWindowExploit.kt
            // patch in paper: https://github.com/PaperMC/Paper/commit/8493340be4fa69fa9369719272e5dff1b7a2f455
            if (button < 0 || windowId < 0) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Button: " + button + ", window: " + windowId + ", slot: " + slot)
                    .punishType(PunishType.KICK)
                    .build());
            }
            if ((clickType == 1 || clickType == 2) && windowId >= 0 && button < 0) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("clickType=" + clickType + ", button=" + button)
                    .punishType(PunishType.KICK)
                    .build());
            } else if (windowId >= 0 && clickType == 2 && slot < 0) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("clickType=" + clickType + ", button=" + button + ", slot=" + slot)
                    .punishType(PunishType.KICK)
                    .build());
            }

            if (slot > 127 || slot < -999) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Invalid slot at " + slot)
                    .punishType(PunishType.KICK)
                    .build());
            }
        }
    }

    /**
     * Checks the given ItemStack for invalid NBT tags and handles violations accordingly.
     *
     * @param event     The PacketReceiveEvent that triggered the check.
     * @param itemStack The ItemStack to check for NBT tags.
     */
    private void checkInvalidNbt(PacketReceiveEvent event, ItemStack itemStack) {

        if (itemStack == null || itemStack.getNBT() == null) return;

        NBTCompound nbt = itemStack.getNBT();

        NBTList<NBTCompound> items = nbt.getTagListOfTypeOrNull("Items", NBTCompound.class);
        if (items != null) {
            if (items.size() > 64) {
                violation(event, createViolation("Too big items list", PunishType.MITIGATE));
            }

            for (NBTCompound tag : items.getTags()) {
                if (tag.getStringTagOrNull("id") != null) {
                    if (tag.getStringTagOrNull("id").getValue().equalsIgnoreCase("minecraft:air")) {
                        violation(event, createViolation("Invalid item: air", PunishType.MITIGATE));
                    } else if (tag.getStringTagOrNull("id").getValue().equalsIgnoreCase("minecraft:bundle")) {
                        violation(event, createViolation("Invalid item: bundle", PunishType.MITIGATE));
                    }
                }
            }
        }

        NBTList<NBTCompound> chargedProjectiles = nbt.getTagListOfTypeOrNull("ChargedProjectiles", NBTCompound.class);

        if (chargedProjectiles != null) {
            for (NBTCompound tag : chargedProjectiles.getTags()) {
                NBTCompound tag1 = tag.getCompoundTagOrNull("tag");
                if (tag1 != null) {
                    NBTString potion = tag1.getStringTagOrNull("Potion");
                    if (potion != null) {
                        if (potion.getValue().endsWith("empty")) {
                            violation(event, createViolation(
                                "Invalid projectile: empty",
                                PunishType.MITIGATE
                            ));
                        }
                    }
                }
            }
        }

        NBTInt customModelData = nbt.getTagOfTypeOrNull("CustomModelData", NBTInt.class);
        if (customModelData != null && PacketEvents.getAPI()
            .getServerManager()
            .getVersion()
            .isNewerThanOrEquals(ServerVersion.V_1_14)) {

            int asInt = customModelData.getAsInt();

            //noinspection ConditionCoveredByFurtherCondition
            if (asInt == Integer.MIN_VALUE || asInt == Integer.MAX_VALUE || asInt < 0) {
                violation(event, createViolation("Invalid custom model data: " + asInt, PunishType.MITIGATE));
            }
        }
    }

    public ViolationDocument createViolation(String debugInformation, PunishType punishType) {
        return ViolationDocument.builder()
            .debugInformation(debugInformation)
            .punishType(punishType)
            .build();
    }

    /**
     * Checks for an invalid slot and handles violations.
     *
     * @param event   The PacketReceiveEvent.
     * @param wrapper The WrapperPlayClientClickWindow.
     */
    private void checkForInvalidSlot(PacketReceiveEvent event, WrapperPlayClientClickWindow wrapper) {

        int slot = wrapper.getSlot();

        if (slot < 0 && slot != -999 && slot != -1) {
            violation(event, ViolationDocument.builder()
                .debugInformation("Invalid slot " + slot)
                .punishType(PunishType.KICK)
                .build());
        }

        Player player = (Player) event.getPlayer();

        if (player == null) return;

        InventoryView openInventory = player.getOpenInventory();

        // Idea by someone
        int max = isSupportedVersion(ServerVersion.V_1_10) ? 127 : openInventory.countSlots();

        if (openInventory.getBottomInventory().getType() == InventoryType.PLAYER
            && openInventory.getTopInventory().getType() == InventoryType.CRAFTING) {

            max += 4;
        }

        if (slot > max) {
            violation(event, ViolationDocument.builder()
                .debugInformation("Slot: " + slot + ", max: " + max)
                .punishType(PunishType.KICK)
                .build());
        }
    }

    /**
     * Checks the position of the button click and handles violations.
     *
     * @param event   The PacketReceiveEvent.
     * @param wrapper The WrapperPlayClientClickWindow.
     */
    // Grim check (https://github.com/GrimAnticheat/Grim/blob/2.0/src/main/java/ac/grim/grimac/checks/impl/badpackets/BadPacketsP.java)
    private void checkButtonClickPosition(PacketReceiveEvent event, WrapperPlayClientClickWindow wrapper) {
        int clickType = wrapper.getWindowClickType().ordinal();
        int button    = wrapper.getButton();

        boolean flag = false;

        switch (clickType) {
            case 0:
            case 1:
            case 4:
                if (button != 0 && button != 1) flag = true;
                break;
            case 2:
                if ((button > 8 || button < 0) && button != 40) flag = true;
                break;
            case 3:
                if (button != 2) flag = true;
                break;
            case 5:
                if (button == 3 || button == 7 || button > 10 || button < 0) flag = true;
                break;
            case 6:
                if (button != 0) flag = true;
                break;
        }

        //Allowing this to false flag to debug and find issues faster
        if (flag) {
            violation(event, ViolationDocument.builder()
                .debugInformation("clickType=" + clickType + " button=" + button + (wrapper.getWindowId() == containerId
                    ? " container=" + containerType : ""))
                .punishType(PunishType.MITIGATE)
                .build());
        }
    }

    /**
     * Checks for an invalid shulker and handles violations.
     *
     * @param event     The PacketReceiveEvent.
     * @param itemStack The ItemStack to check.
     */
    private void checkForInvalidShulker(PacketReceiveEvent event, ItemStack itemStack) {
        if (isShulkerBox(itemStack)) {
            if (itemStack.getNBT() != null) {

                String string = FormatUtils.mapToString(itemStack.getNBT().getTags());

                if (string.getBytes(StandardCharsets.UTF_8).length > 10000) {
                    violation(event, ViolationDocument.builder()
                        .debugInformation("Invalid shulker size")
                        .punishType(PunishType.KICK)
                        .build());
                }
            }
        }
    }

    /**
     * Checks if the given ItemStack represents a shulker box.
     *
     * @param itemStack The ItemStack to check.
     * @return True if the ItemStack is a shulker box, false otherwise.
     */
    private boolean isShulkerBox(ItemStack itemStack) {
        try {
            ShulkerBoxType.valueOf(itemStack.getType().toString());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Checks for an invalid container in the given ItemStack and performs necessary actions.
     *
     * @param event     The PacketReceiveEvent.
     * @param itemStack The ItemStack to check.
     */
    private void checkForInvalidContainer(PacketReceiveEvent event, ItemStack itemStack) {
        if (isInvalidItem(itemStack)) {
            if (itemStack.getNBT() != null) {
                String string = FormatUtils.mapToString(itemStack.getNBT().getTags());
                checkForInvalidSizeAndPresence(event, string);
            }
        }
    }

    /**
     * Checks if the given ItemStack is an invalid item.
     *
     * @param itemStack The ItemStack to check.
     * @return true if the item is invalid, false otherwise.
     */
    private boolean isInvalidItem(ItemStack itemStack) {
        return itemStack.getType() == ItemTypes.CHEST
               || itemStack.getType() == ItemTypes.HOPPER
               || itemStack.getType() == ItemTypes.HOPPER_MINECART
               || itemStack.getType() == ItemTypes.CHEST_MINECART;
    }

    /**
     * Checks for an invalid size and presence of Wurstclient in the given string.
     * If the byte size of the string exceeds the maximum byte size, a violation is triggered with a kick punish type.
     * If the string contains the URL of Wurstclient, a violation is triggered with a ban punish type.
     *
     * @param event  The PacketReceiveEvent.
     * @param string The string to check.
     */
    private void checkForInvalidSizeAndPresence(PacketReceiveEvent event, String string) {
        if (string.getBytes(StandardCharsets.UTF_8).length > MAX_BYTE_SIZE) {
            violation(event, ViolationDocument.builder()
                .debugInformation("Invalid container size")
                .punishType(PunishType.KICK)
                .build());
        }
        if (string.contains(WURSTCLIENT_URL)) {
            violation(event, ViolationDocument.builder()
                .debugInformation("Wurstclient container")
                .punishType(PunishType.BAN)
                .build());
        }
    }

    /**
     * Check the NBT tags of an ItemStack for various violations and handle them accordingly.
     *
     * @param event     The PacketReceiveEvent that triggered the check.
     * @param itemStack The ItemStack to check for NBT tags.
     */
    private void checkNbtTags(PacketReceiveEvent event, ItemStack itemStack) {
        AtomicInteger listContent = new AtomicInteger(0);
        if (itemStack.getNBT() != null) {
            itemStack.getNBT().getTags().forEach((s, nbt) -> {
                if (nbt.getType() == NBTType.LIST) {
                    listContent.set(listContent.get() + 1);
                    if (itemStack.getNBT().getCompoundListTagOrNull(s) != null) {
                        NBTList<NBTCompound> tagOrNull = itemStack.getNBT().getCompoundListTagOrNull(s);
                        if (tagOrNull.getTags().size() > 50) {
                            violation(event, ViolationDocument.builder()
                                .debugInformation("Too big nbt list size")
                                .punishType(PunishType.KICK)
                                .build());
                        }
                        for (NBTCompound tag : tagOrNull.getTags()) {
                            if (tag == null || tag.toString().equalsIgnoreCase("null")) {
                                violation(event, ViolationDocument.builder()
                                    .debugInformation("Null Tag in tag-list")
                                    .punishType(PunishType.KICK)
                                    .build());
                            }
                            if (tag != null && tag.toString().length() > 900) {
                                violation(event, ViolationDocument.builder()
                                    .debugInformation("Too long tag string length")
                                    .punishType(PunishType.KICK)
                                    .build());
                            }
                        }
                    }

                } else if (nbt.getType() == NBTType.INT_ARRAY) {

                    NBTList<NBTIntArray> tagListOfTypeOrNull = itemStack.getNBT()
                        .getTagListOfTypeOrNull(s, NBTIntArray.class);

                    if (tagListOfTypeOrNull != null) {
                        if (tagListOfTypeOrNull.size() > 50) {
                            violation(event, ViolationDocument.builder()
                                .debugInformation("Too big int array size")
                                .punishType(PunishType.KICK)
                                .build());
                        }

                        for (NBTIntArray tag : tagListOfTypeOrNull.getTags()) {
                            if (tag.getValue().length > 150) {
                                violation(event, ViolationDocument.builder()
                                    .debugInformation("Invalid integer length")
                                    .punishType(PunishType.KICK)
                                    .build());
                            }
                            for (int i : tag.getValue()) {
                                if (i == Integer.MAX_VALUE || i == Integer.MIN_VALUE) {
                                    violation(event, ViolationDocument.builder()
                                        .debugInformation("Integer size out of bounds")
                                        .punishType(PunishType.KICK)
                                        .build());
                                }
                            }
                        }
                    }
                } else if (nbt.getType() == NBTType.LONG_ARRAY) {

                    NBTList<NBTLongArray> tagListOfTypeOrNull = itemStack.getNBT()
                        .getTagListOfTypeOrNull(s, NBTLongArray.class);

                    if (tagListOfTypeOrNull != null) {
                        if (tagListOfTypeOrNull.size() > 50) {
                            violation(event, ViolationDocument.builder()
                                .debugInformation("Too big long array size")
                                .punishType(PunishType.KICK)
                                .build());
                        }

                        for (NBTLongArray tag : tagListOfTypeOrNull.getTags()) {
                            if (tag.getValue().length > 150) {
                                violation(event, ViolationDocument.builder()
                                    .debugInformation("Invalid long length")
                                    .punishType(PunishType.KICK)
                                    .build());
                            }
                            for (long i : tag.getValue()) {
                                if (i == Long.MAX_VALUE || i == Long.MIN_VALUE) {
                                    violation(event, ViolationDocument.builder()
                                        .debugInformation("Long size out of bounds")
                                        .punishType(PunishType.KICK)
                                        .build());
                                }
                            }
                        }
                    }

                } else if (nbt.getType() == NBTType.BYTE_ARRAY) {

                    NBTList<NBTByteArray> tagListOfTypeOrNull = itemStack.getNBT()
                        .getTagListOfTypeOrNull(s, NBTByteArray.class);

                    if (tagListOfTypeOrNull != null) {
                        if (tagListOfTypeOrNull.size() > 50) {
                            violation(event, ViolationDocument.builder()
                                .debugInformation("Too big byte array size")
                                .punishType(PunishType.KICK)
                                .build());
                        }

                        for (NBTByteArray tag : tagListOfTypeOrNull.getTags()) {
                            if (tag.getValue().length > 150) {
                                violation(event, ViolationDocument.builder()
                                    .debugInformation("Invalid byte length")
                                    .punishType(PunishType.KICK)
                                    .build());
                            }
                            for (byte i : tag.getValue()) {
                                if (i == Byte.MAX_VALUE || i == Byte.MIN_VALUE) {
                                    violation(event, ViolationDocument.builder()
                                        .debugInformation("Byte size out of bounds")
                                        .punishType(PunishType.KICK)
                                        .build());
                                }
                            }
                        }
                    }
                }
            });
        }
        if (listContent.get() > 10) {
            violation(event, ViolationDocument.builder()
                .debugInformation("Too many nbt lists")
                .punishType(PunishType.KICK)
                .build());
        }
    }

    /**
     * Checks for an invalid banner and handles violations.
     *
     * @param event     The PacketReceiveEvent.
     * @param itemStack The ItemStack to check.
     * @since 1.0.0
     */
    private void checkForInvalidBanner(PacketReceiveEvent event, ItemStack itemStack) {

        if (isBanner(itemStack)) {

            if (itemStack.getNBT() != null) {

                if (itemStack.getNBT().getCompoundTagOrNull("BlockEntityTag") != null) {
                    NBTList<NBTCompound> tagOrNull = itemStack.getNBT()
                        .getCompoundTagOrNull("BlockEntityTag")
                        .getCompoundListTagOrNull("Patterns");

                    if (tagOrNull != null) {
                        List<NBTCompound> tags = tagOrNull.getTags();

                        if (tags.size() > 15) {
                            violation(event, ViolationDocument.builder()
                                .debugInformation("Too many banner layers")
                                .punishType(PunishType.KICK)
                                .build());
                        }

                        for (NBTCompound tag : tags) {
                            NBTString pattern = tag.getStringTagOrNull("Pattern");

                            if (pattern == null || pattern.getValue() == null) {
                                violation(event, ViolationDocument.builder()
                                    .debugInformation("Banner pattern is null")
                                    .punishType(PunishType.KICK)
                                    .build());
                                return;
                            }

                            if (pattern.getValue().length() > 50) {
                                violation(event, ViolationDocument.builder()
                                    .debugInformation("Banner pattern is too long: " + pattern.getValue().length())
                                    .punishType(PunishType.KICK)
                                    .build());
                                return;
                            }

                            NBTNumber color = tag.getNumberTagOrNull("Color");
                            if (color == null) {
                                violation(event, ViolationDocument.builder()
                                    .debugInformation("Banner color is null")
                                    .punishType(PunishType.KICK)
                                    .build());
                                return;
                            }

                            int rgb = 0;
                            try {
                                rgb = color.getAsInt();
                            } catch (Exception exception) {
                                violation(event, ViolationDocument.builder()
                                    .debugInformation("BANNER: " + exception.getMessage())
                                    .punishType(PunishType.KICK)
                                    .build());
                            }

                            if (rgb < 0 || rgb > 255) {
                                violation(event, ViolationDocument.builder()
                                    .debugInformation("Banner color is invalid: " + rgb)
                                    .punishType(PunishType.KICK)
                                    .build());
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isBanner(ItemStack itemStack) {
        try {
            BannerType.valueOf(itemStack.getType().toString());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Handles a PacketSendEvent and performs various checks and actions based on the packet type and player data.
     *
     * @param event      The PacketSendEvent to handle.
     * @param playerData The PlayerData associated with the player.
     */
    @Override
    public void handle(PacketSendEvent event, PlayerData playerData) {

        if (!Sierra.getPlugin()
            .getSierraConfigEngine()
            .config()
            .getBoolean("prevent-invalid-packet", true)) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_EXPERIENCE) {
            WrapperPlayServerSetExperience wrapper = new WrapperPlayServerSetExperience(event);

            if (wrapper.getLevel() < 0 || wrapper.getExperienceBar() < 0 || wrapper.getTotalExperience() < 0) {

                violation(event, ViolationDocument.builder()
                    .debugInformation(String.format("Stats at %d/%s/%d", wrapper.getLevel(),
                                                    wrapper.getExperienceBar(),
                                                    wrapper.getTotalExperience()
                    ))
                    .punishType(PunishType.BAN)
                    .build());
            }
        } else if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {

            playerData.setSkipInvCheckTime(System.currentTimeMillis());

        } else if (event.getPacketType() == PacketType.Play.Server.KEEP_ALIVE) {

            WrapperPlayServerKeepAlive packet = new WrapperPlayServerKeepAlive(event);
            keepAliveMap.add(new Pair<>(packet.getId(), System.nanoTime()));

        } else if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {

            WrapperPlayServerOpenWindow window = new WrapperPlayServerOpenWindow(event);

            if (isSupportedVersion(ServerVersion.V_1_14)) {
                this.type = MenuType.getMenuType(window.getType());
                if (type == MenuType.LECTERN) lecternId = window.getContainerId();
            }
            this.containerType = window.getType();
            this.containerId = window.getContainerId();
        }
    }

    /**
     * Checks if the specified user's client version and the server version are supported.
     *
     * @param serverVersion The ServerVersion to check against.
     * @return {@code true} if both the client version and server version are equal to or newer than the specified
     * ServerVersion,
     * {@code false} otherwise.
     */
    private boolean isSupportedVersion(ServerVersion serverVersion) {
        return PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(serverVersion);
    }

    /**
     * Checks if the specified user's client version and the server version are supported.
     *
     * @param user The user to check.
     * @return {@code true} if both the client version and server version are equal to or newer than version 1.19,
     * {@code false} otherwise.
     */
    private boolean isSupportedVersion(User user) {
        return user.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19) && isSupportedVersion(
            ServerVersion.V_1_19);
    }
}
