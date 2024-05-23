package de.feelix.sierra.check.impl.protocol;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
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
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetExperience;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.impl.command.CommandValidation;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.packet.OutgoingProcessor;
import de.feelix.sierra.manager.storage.menu.MenuType;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.*;
import de.feelix.sierra.utilities.attributes.AttributeMapper;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * The InvalidPacketDetection class is responsible for detecting and handling protocol packets received from players.
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
 * - Private method to check for protocol slots in the packet event
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
 * - Private method to check for protocol shulker packets
 * - Parameters:
 * - event: The PacketReceiveEvent object representing the received packet event
 * - itemStack: The ItemStack object representing the item being checked
 * <p>
 * private void checkForInvalidContainer(PacketReceiveEvent event, ItemStack itemStack)
 * - Private method to check for protocol container packets
 * - Parameters:
 * - event: The PacketReceiveEvent object representing the received packet event
 * - itemStack: The ItemStack object representing the item being checked
 * <p>
 * private void checkNbtTags(PacketReceiveEvent event, ItemStack itemStack)
 * - Private method to check for protocol NBT tags in packets
 * - Parameters:
 * - event: The PacketReceiveEvent object representing the received packet event
 * - itemStack: The ItemStack object representing the item being checked
 * <p>
 * private void checkForInvalidBanner(PacketReceiveEvent event, ItemStack itemStack)
 * - Private method to check for protocol banner packets
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
@SierraCheckData(checkType = CheckType.PROTOCOL_VALIDATION)
public class ProtocolValidation extends SierraDetection implements IngoingProcessor, OutgoingProcessor {

    /**
     * Constructs a new InvalidPacketDetection instance for a given player.
     *
     * @param playerData the data of the player who triggered the detection
     */
    public ProtocolValidation(PlayerData playerData) {
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
     * A regular expression pattern used to detect exploit patterns in strings.
     */
    private static final Pattern EXPLOIT_PATTERN = Pattern.compile("\\$\\{.+}");

    /**
     * Represents the type of container.
     * <p>
     * The containerType variable is an integer that represents the type of container.
     * It is used to differentiate between different types of containers, such as inventories or chests.
     * The default value is -1, indicating an protocol container type.
     * </p>
     * <p>
     * This variable is used in the class InvalidPacketDetection of the SierraCheckData project.
     * </p>
     */
    private int containerType = -1;

    /**
     * Represents the ID of the container.
     * The container ID is used to identify a specific container instance.
     * A negative value (-1) indicates that the container ID is protocol or uninitialized.
     */
    private int containerId = -1;

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
     * The maximum number of layers allowed for a banner.
     */
    private static final int MAX_BANNER_LAYERS = 15;

    /**
     * The maximum length of a pattern.
     */
    private static final int MAX_PATTERN_LENGTH = 50;

    /**
     * The minimum valid color value.
     */
    private static final int MIN_VALID_COLOR = 0;

    /**
     * The maximum length of a sign in characters.
     */
    private static final int MAX_SIGN_LENGTH = 45;

    /**
     * Represents a content of a list.
     *
     * <p>
     * This variable is an instance of {@link AtomicInteger}.
     * It provides atomic operations on an integer value that can be used
     * for synchronization and concurrency control.
     * </p>
     *
     * <p>
     * This variable is marked as <code>private</code> and <code>final</code>,
     * meaning it can only be accessed within the class that declares it,
     * and its value cannot be changed after initialization.
     * </p>
     *
     * @see AtomicInteger
     * @since Replace with the version number
     */
    private final AtomicInteger listContent = new AtomicInteger(0);

    /**
     * The maximum valid color value.
     * <p>
     * This variable represents the maximum value of a valid color component in the
     * RGB color model. The valid color component values range from 0 to this
     * maximum value (inclusive).
     * <p>
     * The value of this variable is set to 255, which corresponds to the maximum
     * valid color value in an 8-bit RGB color model (0-255).
     */
    private static final int MAX_VALID_COLOR = 255;

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
            .getBoolean("prevent-protocol-packet", true)) {
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

            WrapperPlayClientSettings wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientSettings(event),
                playerData::exceptionDisconnect
            );

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

            if (EXPLOIT_PATTERN.matcher(wrapper.getLocale()).matches()) {
                violation(event, ViolationDocument.builder()
                    .punishType(PunishType.MITIGATE)
                    .debugInformation("Invalid locale: " + wrapper.getLocale())
                    .build());
            }

        } else if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {

            WrapperPlayClientCreativeInventoryAction wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientCreativeInventoryAction(event),
                playerData::exceptionDisconnect
            );

            ItemStack itemStack = wrapper.getItemStack();

            checkGenericNBTLimit(event, itemStack);
            checkAttributes(event, itemStack);
            checkInvalidNbt(event, itemStack);
            checkForInvalidBanner(event, itemStack);
            checkForInvalidArmorStand(event, itemStack);
            checkForInvalidContainer(event, itemStack);
            checkForInvalidShulker(event, itemStack);
            checkNbtTags(event, itemStack);

        } else if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {

            WrapperPlayClientEntityAction wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientEntityAction(event),
                playerData::exceptionDisconnect
            );

            if (wrapper.getEntityId() < 0) {
                violation(event, ViolationDocument.builder()
                    .punishType(PunishType.BAN)
                    .debugInformation("Invalid entity action: " + wrapper.getEntityId())
                    .build());
            }

            if (wrapper.getJumpBoost() < 0 || wrapper.getJumpBoost() > 100) {

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

            WrapperPlayClientClickWindowButton wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientClickWindowButton(event),
                playerData::exceptionDisconnect
            );

            if (wrapper.getButtonId() < 0 || wrapper.getWindowId() < 0) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Invalid click slot: " + wrapper.getWindowId() + "/" + wrapper.getButtonId())
                    .punishType(PunishType.BAN)
                    .build());
            }

        } else if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {

            WrapperPlayClientChatMessage wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientChatMessage(event),
                playerData::exceptionDisconnect
            );

            if (wrapper.getMessage().contains("${")) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Send log4j exploit")
                    .punishType(PunishType.KICK)
                    .build());
            }

        } else if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {

            WrapperPlayClientHeldItemChange wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientHeldItemChange(event),
                playerData::exceptionDisconnect
            );

            int slot = wrapper.getSlot();

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
                    .punishType(PunishType.MITIGATE)
                    .build());
            }

            this.lastSlot = slot;


        } else if (event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) {

            WrapperPlayClientTabComplete wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientTabComplete(event),
                playerData::exceptionDisconnect
            );

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
            if (new NBTDetector().find(text) || CommandValidation.WORLDEDIT_PATTERN.matcher(text).matches()) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Text: " + wrapper.getText())
                    .punishType(PunishType.KICK)
                    .build());
            }

            if ((text.equals("/") || text.trim().isEmpty()) && isSupportedServerVersion(ServerVersion.V_1_13)) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Trimmed empty tab to zero")
                    .punishType(PunishType.KICK)
                    .build());
            }

            // Papers patch
            final int index;
            if (text.length() > 64 && ((index = text.indexOf(' ')) == -1 || index >= 64)) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("(protocol) length=" + length)
                    .punishType(PunishType.KICK)
                    .build());
            }

        } else if (event.getPacketType() == PacketType.Play.Client.UPDATE_SIGN) {


            WrapperPlayClientUpdateSign wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientUpdateSign(event), playerData::exceptionDisconnect);

            if (wrapper == null) return;

            for (String textLine : wrapper.getTextLines()) {

                if (textLine.toLowerCase().contains("run_command")) {
                    violation(
                        event,
                        createViolation("Sign contains json command", PunishType.KICK)
                    );
                }

                if (textLine.length() > MAX_SIGN_LENGTH) {
                    violation(
                        event,
                        createViolation("Sign length: " + textLine.length(), PunishType.BAN)
                    );
                }
            }

        } else if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {

            WrapperPlayClientPluginMessage wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientPluginMessage(event),
                playerData::exceptionDisconnect
            );

            String channelName = wrapper.getChannelName();

            if (channelName.equalsIgnoreCase("MC|ItemName") && !playerData.isHasOpenAnvil()) {
                violation(event, createViolation("Send anvil name, without anvil", PunishType.KICK));
            }

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
                    .debugInformation("Send protocol channel in plugin message")
                    .punishType(PunishType.KICK)
                    .build());
            }

            String payload = new String(wrapper.getData(), StandardCharsets.UTF_8);

            if (channelName.equalsIgnoreCase("MC|PickItem")
                || channelName.equalsIgnoreCase("MC|TrSel")) {

                if (payload.equalsIgnoreCase("N")) {
                    violation(event, ViolationDocument.builder()
                        .debugInformation("Console Spammer of Liquid")
                        .punishType(this.violations() > 3 ? PunishType.BAN : PunishType.MITIGATE)
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

            WrapperPlayClientPlayerBlockPlacement wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientPlayerBlockPlacement(event),
                playerData::exceptionDisconnect
            );

            if (wrapper.getSequence() < 0 && isSupportedVersion(ServerVersion.V_1_19, event.getUser(),
                                                                ClientVersion.V_1_19
            )) {
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

                checkGenericNBTLimit(event, itemStack);
                checkAttributes(event, itemStack);
                checkInvalidNbt(event, itemStack);
                checkForInvalidBanner(event, itemStack);
                checkForInvalidArmorStand(event, itemStack);
                checkForInvalidContainer(event, itemStack);
                checkForInvalidShulker(event, itemStack);
                checkNbtTags(event, itemStack);
            }

        } else if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {

            WrapperPlayClientSteerVehicle wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientSteerVehicle(event),
                playerData::exceptionDisconnect
            );

            float forward  = wrapper.getForward();
            float sideways = wrapper.getSideways();

            if (forward > 0.98f || forward < -0.98f) {
                violation(event, ViolationDocument.builder()
                    .punishType(PunishType.KICK)
                    .debugInformation(String.format("forward: %.2f", forward))
                    .build());
            }

            if (sideways > 0.98f || sideways < -0.98f) {
                violation(event, ViolationDocument.builder()
                    .punishType(PunishType.KICK)
                    .debugInformation(String.format("sideways: %.2f", sideways))
                    .build());
            }
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {

            WrapperPlayClientInteractEntity wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientInteractEntity(event),
                playerData::exceptionDisconnect
            );

            int entityId = event.getUser().getEntityId();

            if (wrapper.getEntityId() < 0 || entityId == wrapper.getEntityId()) {
                violation(event, ViolationDocument.builder()
                    .punishType(PunishType.BAN)
                    .debugInformation("Id at " + wrapper.getEntityId() + ", player id: " + entityId)
                    .build());
            }
        } else if (event.getPacketType() == PacketType.Play.Client.NAME_ITEM) {

            WrapperPlayClientNameItem wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientNameItem(event),
                playerData::exceptionDisconnect
            );

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

            // General minecraft limit
            if (length > 50) {
                violation(event, ViolationDocument.builder()
                    .punishType(PunishType.KICK)
                    .debugInformation("Name longer than 50: " + length)
                    .build());
            }

        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING
                   && isSupportedVersion(ServerVersion.V_1_19, event.getUser(), ClientVersion.V_1_19)) {

            WrapperPlayClientPlayerDigging dig = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientPlayerDigging(event),
                playerData::exceptionDisconnect
            );

            if (dig.getSequence() < 0 && isSupportedVersion(
                ServerVersion.V_1_19, event.getUser(), ClientVersion.V_1_19)) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Invalid sequence in dig")
                    .punishType(PunishType.BAN)
                    .build());
            }

        } else if (event.getPacketType() == PacketType.Play.Client.USE_ITEM && isSupportedVersion(
            ServerVersion.V_1_19, event.getUser(), ClientVersion.V_1_19)) {

            WrapperPlayClientUseItem use = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientUseItem(event),
                playerData::exceptionDisconnect
            );

            if (use.getSequence() < 0) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Invalid sequence in use item")
                    .punishType(PunishType.BAN)
                    .build());
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {

            WrapperPlayClientClickWindow wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientClickWindow(event),
                playerData::exceptionDisconnect
            );

            if (isSupportedServerVersion(ServerVersion.V_1_14)) {
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

            checkGenericNBTLimit(event, carriedItemStack);
            checkAttributes(event, carriedItemStack);
            checkInvalidNbt(event, carriedItemStack);
            checkForInvalidContainer(event, carriedItemStack);
            checkForInvalidShulker(event, carriedItemStack);
            checkForInvalidBanner(event, carriedItemStack);
            checkForInvalidArmorStand(event, carriedItemStack);
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
        } else if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {

            // Reset this state to prevent false positives
            playerData.setHasOpenAnvil(false);
        }
    }

    /**
     * Checks the attributes of an ItemStack and handles violations accordingly.
     *
     * @param event     The PacketReceiveEvent that triggered the check.
     * @param itemStack The ItemStack to check for attribute modifiers.
     */
    private void checkAttributes(ProtocolPacketEvent<Object> event, ItemStack itemStack) {
        if (hasAttributeModifiers(itemStack)) {
            List<NBTCompound> tags           = getAttributeModifiers(itemStack);
            boolean           vanillaMapping = useVanillaAttributeMapping();
            for (NBTCompound tag : tags) {
                AttributeMapper attributeMapper = getAttributeMapper(tag);
                if (attributeMapper != null) {
                    handleAttributeViolation(event, vanillaMapping, attributeMapper, tag);
                }
            }
        }
    }

    /**
     * Retrieves the AttributeMapper corresponding to the given tag. The AttributeMapper is used to
     * handle violations based on attribute modifiers in an ItemStack.
     *
     * @param tag The NBTCompound tag containing the "AttributeName" key.
     * @return The AttributeMapper corresponding to the given tag, or null if no matching AttributeMapper is found.
     */
    private AttributeMapper getAttributeMapper(NBTCompound tag) {
        //noinspection DataFlowIssue
        return AttributeMapper.getAttributeMapper(tag.getStringTagOrNull("AttributeName").getValue());
    }

    /**
     * Checks if an ItemStack has attribute modifiers.
     *
     * @param itemStack The ItemStack to check for attribute modifiers.
     * @return true if the ItemStack has attribute modifiers, false otherwise.
     */
    private boolean hasAttributeModifiers(ItemStack itemStack) {
        return itemStack.getNBT() != null && itemStack.getNBT().getCompoundListTagOrNull("AttributeModifiers") != null;
    }

    /**
     * Retrieves the list of attribute modifiers from an ItemStack.
     *
     * @param itemStack The ItemStack to retrieve attribute modifiers from.
     * @return The list of attribute modifiers as a List of NBTCompounds.
     */
    private List<NBTCompound> getAttributeModifiers(ItemStack itemStack) {
        //noinspection DataFlowIssue
        NBTList<NBTCompound> tagOrNull = itemStack.getNBT().getCompoundListTagOrNull("AttributeModifiers");
        //noinspection DataFlowIssue
        return tagOrNull.getTags();
    }

    /**
     * Checks the length of the NBT data in the given ItemStack against the
     * defined limit. If the length exceeds the limit, a violation is generated
     * and the appropriate action is taken.
     *
     * @param event     The packet receive event
     * @param itemStack The ItemStack to check for NBT data
     */
    public void checkGenericNBTLimit(PacketReceiveEvent event, ItemStack itemStack) {

        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("generic-nbt-limit", true)) {
            return;
        }

        if (itemStack.getNBT() == null) return;

        int length = FormatUtils.mapToString(itemStack.getNBT().getTags()).length();

        int limit = getPlayerData().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16) ? 30000 : 25000;

        if (length > limit) {
            violation(event, ViolationDocument.builder()
                .debugInformation("length=" + length + ", limit=" + limit)
                .punishType(PunishType.MITIGATE)
                .build());
        }
    }

    /**
     * Handles an attribute violation in a packet receive event.
     *
     * @param event           The ProtocolPacketEvent<Object> event to handle.
     * @param vanillaMapping  A flag indicating whether vanilla attribute mapping is used.
     * @param attributeMapper The AttributeMapper to handle violations based on attribute modifiers.
     * @param tag             The NBTCompound tag containing the attribute information.
     */
    private void handleAttributeViolation(ProtocolPacketEvent<Object> event, boolean vanillaMapping,
                                          AttributeMapper attributeMapper, NBTCompound tag) {

        //noinspection DataFlowIssue
        double amount = tag.getNumberTagOrNull("Amount").getAsDouble();

        if (isAmountInvalid(vanillaMapping, attributeMapper, amount)) {
            violation(
                event,
                createViolation("Invalid attribute modifier. Amount: " + amount, PunishType.KICK)
            );
        } else if (!vanillaMapping && isSierraModifierInvalid(amount)) {
            violation(
                event,
                createViolation("Sierra attribute modifier. Amount: " + amount, PunishType.KICK)
            );
        } else if (FormatUtils.checkDoublePrecision(amount)) {
            violation(event, createViolation("Double is to precisely", PunishType.KICK));
        }
    }

    /**
     * Determines whether the plugin should use vanilla attribute mapping or not.
     *
     * @return true if the plugin should use vanilla attribute mapping, false otherwise.
     */
    private boolean useVanillaAttributeMapping() {
        return Sierra.getPlugin()
            .getSierraConfigEngine()
            .config()
            .getBoolean("use-vanilla-attribute-mapping", true);
    }

    /**
     * Checks if the amount is protocol based on the specified vanilla mapping flag, attribute mapper, and amount.
     *
     * @param vanillaMapping  A flag indicating whether vanilla attribute mapping is used.
     * @param attributeMapper The AttributeMapper to handle violations based on attribute modifiers.
     * @param amount          The amount to check.
     * @return true if the amount is protocol, false otherwise.
     */
    private boolean isAmountInvalid(boolean vanillaMapping, AttributeMapper attributeMapper, double amount) {
        return vanillaMapping && (amount > attributeMapper.getMax() || amount < attributeMapper.getMin());
    }

    /**
     * Checks if the given amount is considered protocol for a Sierra attribute modifier.
     *
     * @param amount The amount to check.
     * @return true if the amount is protocol, false otherwise.
     */
    private boolean isSierraModifierInvalid(double amount) {
        return Math.abs(amount) > 5.000;
    }

    /**
     * Checks the given ItemStack for protocol NBT tags and handles violations accordingly.
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
                    //noinspection DataFlowIssue
                    if (tag.getStringTagOrNull("id").getValue().equalsIgnoreCase("minecraft:air")) {
                        violation(event, createViolation("Invalid item: air", PunishType.MITIGATE));
                    } else //noinspection DataFlowIssue
                        if (tag.getStringTagOrNull("id").getValue().equalsIgnoreCase("minecraft:bundle")) {
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

        if (customModelData != null && isSupportedServerVersion(ServerVersion.V_1_14)) {

            int asInt = customModelData.getAsInt();

            //noinspection ConditionCoveredByFurtherCondition
            if (asInt == Integer.MIN_VALUE || asInt == Integer.MAX_VALUE || asInt < 0) {
                violation(event, createViolation("Invalid custom model data: " + asInt, PunishType.MITIGATE));
            }
        }
    }

    /**
     * Create a violation document with the given debug information and punish type.
     *
     * @param debugInformation A string representing the debug information related to the violation.
     * @param punishType       The type of punishment associated with the violation.
     * @return A ViolationDocument object containing the debug information and punish type.
     */
    public ViolationDocument createViolation(String debugInformation, PunishType punishType) {
        return ViolationDocument.builder()
            .debugInformation(debugInformation)
            .punishType(punishType)
            .build();
    }

    /**
     * Checks for an protocol slot and handles violations.
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
        int max = isSupportedServerVersion(ServerVersion.V_1_10) ? 127 : openInventory.countSlots();

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
     * Checks for an protocol shulker and handles violations.
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
     * Checks for an protocol container in the given ItemStack and performs necessary actions.
     *
     * @param event     The PacketReceiveEvent.
     * @param itemStack The ItemStack to check.
     */
    private void checkForInvalidContainer(PacketReceiveEvent event, ItemStack itemStack) {
        if (isContainerItem(itemStack)) {
            if (itemStack.getNBT() != null) {
                String string = FormatUtils.mapToString(itemStack.getNBT().getTags());
                checkForInvalidSizeAndPresence(event, string);
            }
        }
    }

    /**
     * Checks if the given ItemStack is an protocol item.
     *
     * @param itemStack The ItemStack to check.
     * @return true if the item is protocol, false otherwise.
     */
    private boolean isContainerItem(ItemStack itemStack) {
        return itemStack.getType() == ItemTypes.CHEST
               || itemStack.getType() == ItemTypes.HOPPER
               || itemStack.getType() == ItemTypes.HOPPER_MINECART
               || itemStack.getType() == ItemTypes.CHEST_MINECART;
    }

    /**
     * Checks for an protocol size and presence of Wurstclient in the given string.
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
        if (itemStack.getNBT() != null) {
            itemStack.getNBT().getTags().forEach((s, nbt) -> {
                if (nbt.getType() == NBTType.LIST) {
                    checkList(s, event, itemStack);
                } else if (nbt.getType() == NBTType.INT_ARRAY) {
                    checkIntArray(s, event, itemStack);
                } else if (nbt.getType() == NBTType.LONG_ARRAY) {
                    checkLongArray(s, event, itemStack);
                } else if (nbt.getType() == NBTType.BYTE_ARRAY) {
                    checkByteArray(s, event, itemStack);
                }
            });
        }
        this.listContent.set(0);
    }

    /**
     * Checks a given list of NBT tags and performs violation actions based on certain conditions.
     *
     * @param s         The name of the NBT tag list to check.
     * @param event     The PacketReceiveEvent associated with the check.
     * @param itemStack The ItemStack to extract the NBT tags from.
     */
    private void checkList(String s, PacketReceiveEvent event, ItemStack itemStack) {

        if (itemStack.getNBT() == null) return;

        listContent.set(listContent.get() + 1);
        if (itemStack.getNBT().getCompoundListTagOrNull(s) != null) {
            NBTList<NBTCompound> tagOrNull = itemStack.getNBT().getCompoundListTagOrNull(s);
            //noinspection DataFlowIssue
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
        if (listContent.get() > 10) {
            violation(event, ViolationDocument.builder()
                .debugInformation("Too many nbt lists")
                .punishType(PunishType.KICK)
                .build());
        }
    }

    /**
     * Checks the validity of an integer array stored in an ItemStack's NBT data.
     * If the integer array is protocol, it triggers a violation.
     *
     * @param key       the key used to retrieve the integer array from the ItemStack's NBT data
     * @param event     the PacketReceiveEvent object containing event information
     * @param itemStack the ItemStack object to check for an integer array
     */
    private void checkIntArray(String key, PacketReceiveEvent event, ItemStack itemStack) {

        if (itemStack.getNBT() == null) return;

        NBTList<NBTIntArray> tagListOfTypeOrNull = itemStack.getNBT()
            .getTagListOfTypeOrNull(key, NBTIntArray.class);

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
    }

    /**
     * Checks the validity of a long array stored in an ItemStack's NBT data.
     *
     * @param key       the key of the long array in the ItemStack's NBT data
     * @param event     the PacketReceiveEvent associated with the check
     * @param itemStack the ItemStack containing the NBT data to be checked
     */
    private void checkLongArray(String key, PacketReceiveEvent event, ItemStack itemStack) {

        if (itemStack.getNBT() == null) return;

        NBTList<NBTLongArray> tagListOfTypeOrNull = itemStack.getNBT()
            .getTagListOfTypeOrNull(key, NBTLongArray.class);

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
    }

    /**
     * Checks the given byte array in the provided `itemStack` for violations.
     *
     * @param key       the key to retrieve the byte array from the NBT tag of the `itemStack`
     * @param event     the PacketReceiveEvent associated with the check
     * @param itemStack the ItemStack to check for violations
     */
    private void checkByteArray(String key, PacketReceiveEvent event, ItemStack itemStack) {

        if (itemStack.getNBT() == null) return;

        NBTList<NBTByteArray> tagListOfTypeOrNull = itemStack.getNBT()
            .getTagListOfTypeOrNull(key, NBTByteArray.class);

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

    /**
     * Checks for any protocol properties in the provided Armor Stand item stack and sends an event if found.
     *
     * @param event     The packet receive event.
     * @param itemStack The item stack to check.
     */
    private void checkForInvalidArmorStand(PacketReceiveEvent event, ItemStack itemStack) {
        if (itemStack.getType() != ItemTypes.ARMOR_STAND || itemStack.getNBT() == null) return;
        NBTCompound entityTag = itemStack.getNBT().getCompoundTagOrNull("EntityTag");
        if (entityTag == null) return;

        checkInvalidPoses(event, entityTag);
        checkInvalidCustomName(event, entityTag);
        checkInvalidSkullOwner(event, entityTag);
        checkInvalidRotation(event, entityTag);
    }

    /**
     * Checks for protocol poses in the given entity tag and raises events accordingly.
     *
     * @param event     The PacketReceiveEvent object associated with the entity tag.
     * @param entityTag The NBTCompound object representing the entity tag.
     */
    private void checkInvalidPoses(PacketReceiveEvent event, NBTCompound entityTag) {
        NBTCompound pose = entityTag.getCompoundTagOrNull("Pose");
        if (pose != null) {
            invalidPoseAngles(event, pose, "Head");
            invalidPoseAngles(event, pose, "Body");
            invalidPoseAngles(event, pose, "LeftArm");
            invalidPoseAngles(event, pose, "RightArm");
            invalidPoseAngles(event, pose, "LeftLeg");
            invalidPoseAngles(event, pose, "RightLeg");
        }
    }

    /**
     * Checks if the custom name of an entity is protocol.
     * If the custom name exceeds the character limit of 70, it is considered protocol.
     *
     * @param event     The packet receive event.
     * @param entityTag The NBT compound tag of the entity.
     */
    private void checkInvalidCustomName(PacketReceiveEvent event, NBTCompound entityTag) {
        NBTString customName = entityTag.getStringTagOrNull("CustomName");
        if (customName != null && customName.getValue().length() > 70) {
            violation(
                event, createViolation(
                    "Invalid armor stand name length: " + customName.getValue().length(),
                    PunishType.MITIGATE
                ));
        }
    }

    /**
     * Checks for protocol skull owners in the equipment of the entity.
     *
     * @param event     the PacketReceiveEvent that triggered the check
     * @param entityTag the NBTCompound representing the tag of the entity
     */
    private void checkInvalidSkullOwner(PacketReceiveEvent event, NBTCompound entityTag) {
        NBTList<NBTCompound> equipment = entityTag.getCompoundListTagOrNull("Equipment");
        if (equipment != null) {
            for (NBTCompound tag : equipment.getTags()) {
                checkSkullOwner(event, tag);
            }
        }
    }

    /**
     * Checks for protocol rotation of an armor stand entity.
     *
     * @param event     The packet receive event associated with the armor stand entity.
     * @param entityTag The NBT compound tag containing the rotation information.
     */
    private void checkInvalidRotation(PacketReceiveEvent event, NBTCompound entityTag) {
        NBTList<NBTNumber> rotation = entityTag.getNumberListTagOrNull("Rotation");
        if (rotation != null) {
            for (NBTNumber tag : rotation.getTags()) {
                float armorStandRotation = tag.getAsFloat();
                if (armorStandRotation < 0 || armorStandRotation > 360) {
                    violation(
                        event, createViolation(
                            "Invalid armor stand rotation: " + armorStandRotation,
                            PunishType.KICK
                        ));
                }
            }
        }
    }

    /**
     * Check the owner of a skull item and take appropriate action if the owner's name is protocol.
     *
     * @param event The PacketReceiveEvent that triggered the method.
     * @param item  The NBTCompound representing the skull item.
     */
    private void checkSkullOwner(PacketReceiveEvent event, NBTCompound item) {
        if ("skull".equals(item.getStringTagValueOrNull("id"))) {
            NBTCompound tag = item.getCompoundTagOrNull("tag");
            if (tag != null) {
                NBTString skullOwner = tag.getStringTagOrNull("SkullOwner");
                if (skullOwner != null) {
                    String name = skullOwner.getValue();
                    if (name.length() < 3 || name.length() > 16 || !name.matches("^[a-zA-Z0-9_\\-.]{3,16}$")) {
                        violation(event, createViolation(
                            "Invalid skull owner name: " + name,
                            PunishType.KICK
                        ));
                    }
                }
            }
        }
    }

    /**
     * Checks if the given pose angles are valid for the specified limb.
     *
     * @param event The PacketReceiveEvent associated with the pose angles check.
     * @param pose  The NBTCompound representing the pose.
     * @param limb  The name of the limb for which to check the pose angles.
     * @throws IllegalStateException if the pose angles for the limb are protocol.
     */
    private void invalidPoseAngles(PacketReceiveEvent event, NBTCompound pose,
                                   String limb) throws IllegalStateException {
        NBTList<NBTNumber> angles = pose.getTagListOfTypeOrNull(limb, NBTNumber.class);
        if (angles != null) {
            for (NBTNumber tag : angles.getTags()) {
                double value = tag.getAsDouble();
                if (value < -360.0 || value > 360.0) {
                    violation(
                        event, createViolation(
                            String.format("Invalid rotation, limb: %s[%s]", limb, value),
                            PunishType.KICK
                        ));
                    return;
                }
            }
        }
    }

    /**
     * Checks for an protocol banner and handles violations.
     *
     * @param event     The PacketReceiveEvent.
     * @param itemStack The ItemStack to check.
     * @since 1.0.0
     */
    private void checkForInvalidBanner(PacketReceiveEvent event, ItemStack itemStack) {
        if (!isBanner(itemStack) || itemStack.getNBT() == null) {
            return;
        }
        NBTCompound blockEntityTag = itemStack.getNBT().getCompoundTagOrNull("BlockEntityTag");
        if (blockEntityTag == null) {
            return;
        }
        NBTList<NBTCompound> tagOrNull = blockEntityTag.getCompoundListTagOrNull("Patterns");
        if (tagOrNull == null) {
            return;
        }
        List<NBTCompound> tags = tagOrNull.getTags();
        if (tags.size() > MAX_BANNER_LAYERS) {
            createViolation(event, "Too many banner layers");
            return;
        }
        for (NBTCompound tag : tags) {
            validatePattern(event, tag.getStringTagOrNull("Pattern"));
            validateColor(event, tag.getNumberTagOrNull("Color"));
        }
    }

    /**
     * Validates the given pattern for a packet receive event.
     *
     * @param event   the packet receive event to validate the pattern for
     * @param pattern the pattern to validate
     */
    private void validatePattern(PacketReceiveEvent event, NBTString pattern) {
        if (pattern == null || pattern.getValue() == null) {
            createViolation(event, "Banner pattern is null");
            return;
        }
        if (pattern.getValue().length() > MAX_PATTERN_LENGTH) {
            createViolation(event, "Banner pattern is too long: " + pattern.getValue().length());
        }
    }

    /**
     * Validates the given color for a banner.
     * If the color is null or protocol, it creates a violation event.
     *
     * @param event the PacketReceiveEvent object representing the event
     * @param color the NBTNumber object representing the color of the banner
     */
    private void validateColor(PacketReceiveEvent event, NBTNumber color) {
        if (color == null) {
            createViolation(event, "Banner color is null");
            return;
        }
        try {
            int rgb = color.getAsInt();
            if (rgb < MIN_VALID_COLOR || rgb > MAX_VALID_COLOR) {
                createViolation(event, "Banner color is protocol: " + rgb);
            }
        } catch (Exception exception) {
            createViolation(event, "BANNER: " + exception.getMessage());
        }
    }

    /**
     * Creates a violation for the given event and debug information.
     *
     * @param event            the PacketReceiveEvent associated with the violation
     * @param debugInformation the debug information for the violation
     */
    private void createViolation(PacketReceiveEvent event, String debugInformation) {
        violation(event, ViolationDocument.builder()
            .debugInformation(debugInformation)
            .punishType(PunishType.KICK)
            .build());
    }

    /**
     * Checks if the given ItemStack is a banner.
     *
     * @param itemStack the ItemStack to check
     * @return true if the ItemStack is a banner, false otherwise
     */
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
            .getBoolean("prevent-protocol-packet", true)) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_EXPERIENCE) {
            WrapperPlayServerSetExperience wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayServerSetExperience(event),
                playerData::exceptionDisconnect
            );

            if (wrapper.getLevel() < 0 || wrapper.getExperienceBar() < 0 || wrapper.getTotalExperience() < 0) {

                violation(event, ViolationDocument.builder()
                    .debugInformation(String.format("Stats at %d/%s/%d", wrapper.getLevel(),
                                                    wrapper.getExperienceBar(),
                                                    wrapper.getTotalExperience()
                    ))
                    .punishType(PunishType.KICK)
                    .build());
            }
        } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {

            WrapperPlayServerWindowItems wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayServerWindowItems(event), playerData::exceptionDisconnect);

            for (ItemStack item : wrapper.getItems()) {
                if (item.getNBT() == null) continue;
                checkAttributes(event, item);
            }

        } else if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {

            WrapperPlayServerOpenWindow window = CastUtil.getSupplierValue(
                () -> new WrapperPlayServerOpenWindow(event),
                playerData::exceptionDisconnect
            );

            if (PacketEvents.getAPI()
                .getServerManager()
                .getVersion()
                .isNewerThanOrEquals(ServerVersion.V_1_14)) {
                if (window.getType() == MenuType.ANVIL.getId()) {
                    playerData.setHasOpenAnvil(true);
                }
            } else {
                String legacyType = window.getLegacyType();
                if (legacyType.contains("anvil")) {
                    playerData.setHasOpenAnvil(true);
                }
            }

            if (isSupportedServerVersion(ServerVersion.V_1_14)) {
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
    private boolean isSupportedServerVersion(ServerVersion serverVersion) {
        return PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(serverVersion);
    }

    /**
     * Checks if the specified user's client version and the server version are supported.
     *
     * @param user The user to check.
     * @return {@code true} if both the client version and server version are equal to or newer than version 1.19,
     * {@code false} otherwise.
     */
    @SuppressWarnings("SameParameterValue")
    private boolean isSupportedVersion(ServerVersion serverVersion, User user, ClientVersion clientVersion) {
        return user.getClientVersion().isNewerThanOrEquals(clientVersion)
               && isSupportedServerVersion(serverVersion);
    }
}
