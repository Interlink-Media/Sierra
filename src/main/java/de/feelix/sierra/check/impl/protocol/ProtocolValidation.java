package de.feelix.sierra.check.impl.protocol;

import com.cryptomorin.xseries.XMaterial;
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
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierra.utilities.FieldReader;
import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierra.utilities.attributes.AttributeMapper;
import de.feelix.sierra.utilities.types.BannerType;
import de.feelix.sierra.utilities.types.ShulkerBoxType;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.PunishType;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.github.retrooper.packetevents.protocol.nbt.NBTType.*;

@SierraCheckData(checkType = CheckType.PROTOCOL_VALIDATION)
public class ProtocolValidation extends SierraDetection implements IngoingProcessor, OutgoingProcessor {

    private              MenuType      type               = MenuType.UNKNOWN;
    private              int           lecternId          = -1;
    private              int           lastSlot           = -1;
    private              long          lastBookUse        = 0L;
    private static final Pattern       EXPLOIT_PATTERN    = Pattern.compile("\\$\\{.+}");
    private              int           containerType      = -1;
    private              int           containerId        = -1;
    private static final int           MAX_BYTE_SIZE      = 262144;
    private static final String        WURSTCLIENT_URL    = "www.wurstclient.net";
    private static final int           MAX_BANNER_LAYERS  = 15;
    private static final int           MAX_PATTERN_LENGTH = 50;
    private static final int           MIN_VALID_COLOR    = 0;
    private static final int           MAX_SIGN_LENGTH    = 45;
    private              boolean       hasOpenAnvil       = false;
    private final        AtomicInteger listContent        = new AtomicInteger(0);
    private static final int           MAX_VALID_COLOR    = 255;

    public ProtocolValidation(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {
        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-protocol-packet", true)) {
            return;
        }

        if (event.getConnectionState() != ConnectionState.PLAY) return;
        if (playerData.getClientVersion() == null) {
            playerData.setClientVersion(event.getUser().getClientVersion());
        }

        int capacity = ByteBufHelper.capacity(event.getByteBuf());
        int maxBytes = 64000 * (playerData.getClientVersion().isOlderThan(ClientVersion.V_1_8) ? 2 : 1);
        if (capacity > maxBytes) {
            violation(event, createViolation("Bytes: " + capacity + " Max Bytes: " + maxBytes, PunishType.KICK));
        }

        int readableBytes     = ByteBufHelper.readableBytes(event.getByteBuf());
        int maxBytesPerSecond = 64000 * (playerData.getClientVersion().isOlderThan(ClientVersion.V_1_8) ? 2 : 1);
        playerData.setBytesSent(playerData.getBytesSent() + readableBytes);

        if (playerData.getBytesSent() > maxBytesPerSecond) {
            violation(
                event,
                createViolation(
                    "Bytes Sent: " + playerData.getBytesSent() + " Max Bytes/s: " + maxBytesPerSecond,
                    PunishType.KICK
                )
            );
        }

        handleClientSettings(event, playerData);
        handleCreativeInventoryAction(event, playerData);
        handleEntityAction(event);
        handleSpectate(event);
        handleClickWindowButton(event);
        handleChatMessage(event);
        handleHeldItemChange(event);
        handleTabComplete(event, playerData);
        handleUpdateSign(event, playerData);
        handlePluginMessage(event, playerData);
        handlePlayerBlockPlacement(event, playerData);
        handleSteerVehicle(event);
        handleInteractEntity(event);
        handleNameItem(event);
        handlePlayerDigging(event, playerData);
        handleUseItem(event, playerData);
        handleClickWindow(event, playerData);
        handleCloseWindow();
    }

    private void handleClientSettings(PacketReceiveEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_SETTINGS) {
            WrapperPlayClientSettings wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientSettings(event), playerData::exceptionDisconnect);

            if (wrapper == null) return;

            adjustViewDistance(wrapper, event);
            checkLocale(wrapper, event);
        }
    }

    private void adjustViewDistance(WrapperPlayClientSettings wrapper, PacketReceiveEvent event) {
        if (wrapper.getViewDistance() < 2) {
            Logger logger = Sierra.getPlugin().getLogger();
            logger.log(
                Level.INFO, String.format("Adjusting %s's view distance from %d to 2", event.getUser().getName(),
                                          wrapper.getViewDistance()
                ));
            wrapper.setViewDistance(2);
        }
    }

    private void checkLocale(WrapperPlayClientSettings wrapper, PacketReceiveEvent event) {
        if (wrapper.getLocale() == null) {
            violation(event, createViolation("Locale is null in settings", PunishType.KICK));
        }
        if (EXPLOIT_PATTERN.matcher(wrapper.getLocale()).matches()) {
            violation(event, createViolation("Invalid locale: " + wrapper.getLocale(), PunishType.MITIGATE));
        }
    }

    private void handleCreativeInventoryAction(PacketReceiveEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            WrapperPlayClientCreativeInventoryAction wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientCreativeInventoryAction(event), playerData::exceptionDisconnect);
            ItemStack itemStack = wrapper.getItemStack();
            checkItemStack(event, itemStack);
        }
    }

    private void handleEntityAction(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientEntityAction(event), getPlayerData()::exceptionDisconnect);
            checkEntityAction(wrapper, event);
        }
    }

    private void checkEntityAction(WrapperPlayClientEntityAction wrapper, PacketReceiveEvent event) {
        if (wrapper.getEntityId() < 0) {
            violation(event, createViolation("Invalid entity action: " + wrapper.getEntityId(), PunishType.BAN));
        }
        if (wrapper.getJumpBoost() < 0 || wrapper.getJumpBoost() > 100) {
            violation(event, createViolation("boost: " + wrapper.getJumpBoost(), PunishType.KICK));
        }
    }

    private void handleSpectate(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.SPECTATE
            && getPlayerData().getGameMode() != GameMode.SPECTATOR) {
            violation(event, createViolation("Spoofed spectator state", PunishType.BAN));
        }
    }

    private void handleClickWindowButton(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW_BUTTON) {
            WrapperPlayClientClickWindowButton wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientClickWindowButton(event), getPlayerData()::exceptionDisconnect);
            if (wrapper.getButtonId() < 0 || wrapper.getWindowId() < 0) {
                violation(
                    event, createViolation(
                        "Invalid click slot: " + wrapper.getWindowId() + "/" + wrapper.getButtonId(),
                        PunishType.BAN
                    ));
            }
        }
    }

    private void handleChatMessage(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
            WrapperPlayClientChatMessage wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientChatMessage(event), getPlayerData()::exceptionDisconnect);
            if (wrapper.getMessage().contains("${")) {
                violation(event, createViolation("Send log4j exploit", PunishType.KICK));
            }
        }
    }

    private void handleHeldItemChange(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            WrapperPlayClientHeldItemChange wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientHeldItemChange(event), getPlayerData()::exceptionDisconnect);
            checkHeldItemChange(wrapper, event);
        }
    }

    private void checkHeldItemChange(WrapperPlayClientHeldItemChange wrapper, PacketReceiveEvent event) {
        int slot = wrapper.getSlot();
        if (slot > 36 || slot < 0) {
            violation(event, createViolation("Invalid slot at (HOTBAR): " + slot, PunishType.BAN));
        }
        Player player = (Player) event.getPlayer();
        if (player == null) return;
        int length = player.getInventory().getContents().length;
        if (!(wrapper.getSlot() >= 0 && wrapper.getSlot() < length)) {
            violation(
                event, createViolation("Invalid slot at: " + wrapper.getSlot() + ", max: " + length, PunishType.BAN));
        }
        if (slot == lastSlot) {
            violation(event, createViolation("Selected slot twice: " + wrapper.getSlot(), PunishType.MITIGATE));
        }
        this.lastSlot = slot;
    }

    private void handleTabComplete(PacketReceiveEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) {
            WrapperPlayClientTabComplete wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientTabComplete(event), playerData::exceptionDisconnect);
            checkTabComplete(wrapper, event);
        }
    }

    private void checkTabComplete(WrapperPlayClientTabComplete wrapper, PacketReceiveEvent event) {
        if (wrapper.getText() == null) {
            violation(event, createViolation("Send packet with null value", PunishType.BAN));
        }
        String text   = wrapper.getText();
        int    length = text.length();
        if (CommandValidation.WORLDEDIT_PATTERN.matcher(text).matches()) {
            violation(event, createViolation("WorldEdit Pat/Tab: " + text, PunishType.MITIGATE));
        }
        if (areBracketsTooFrequent(text, 15) || CommandValidation.WORLDEDIT_PATTERN.matcher(text).matches()) {
            violation(event, createViolation("Text: " + wrapper.getText(), PunishType.KICK));
        }
        if (length > 256) {
            violation(event, createViolation("(length) length=" + length, PunishType.KICK));
        }
        if ((text.equals("/") || text.trim().isEmpty()) && isSupportedServerVersion(ServerVersion.V_1_13)) {
            violation(event, createViolation("Trimmed empty tab to zero", PunishType.KICK));
        }
        int index;
        if (text.length() > 64 && ((index = text.indexOf(' ')) == -1 || index >= 64)) {
            violation(event, createViolation("(protocol) length=" + length, PunishType.KICK));
        }
    }

    private void handleUpdateSign(PacketReceiveEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Client.UPDATE_SIGN) {
            WrapperPlayClientUpdateSign wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientUpdateSign(event), playerData::exceptionDisconnect);
            if (wrapper == null) return;
            checkUpdateSign(wrapper, event, playerData);
        }
    }

    private void checkUpdateSign(WrapperPlayClientUpdateSign wrapper, PacketReceiveEvent event, PlayerData playerData) {
        double distanceFromLastLocation = wrapper.getBlockPosition()
            .toVector3d()
            .distanceSquared(playerData.getLastLocation().getPosition());
        if (distanceFromLastLocation > 64) {
            violation(
                event, createViolation(
                    String.format("Sign is too far away: %.2f", distanceFromLastLocation),
                    PunishType.KICK
                ));
        }
        for (String textLine : wrapper.getTextLines()) {
            if (textLine.toLowerCase().contains("run_command")) {
                violation(event, createViolation("Sign contains json command", PunishType.KICK));
            }
            if (textLine.length() > MAX_SIGN_LENGTH) {
                violation(event, createViolation("Sign length: " + textLine.length(), PunishType.BAN));
            }
        }
    }

    private void handlePluginMessage(PacketReceiveEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientPluginMessage(event), playerData::exceptionDisconnect);
            checkPluginMessage(wrapper, event, playerData);
        }
    }

    private void checkPluginMessage(WrapperPlayClientPluginMessage wrapper, PacketReceiveEvent event,
                                    PlayerData playerData) {
        String channelName = wrapper.getChannelName();
        if (channelName.equalsIgnoreCase("MC|ItemName") && !hasOpenAnvil) {
            violation(event, createViolation("Send anvil name, without anvil", PunishType.KICK));
        }
        if (isBookChannel(channelName) && System.currentTimeMillis() - this.lastBookUse > 60000L) {
            violation(event, createViolation("Send book sign, without book use", PunishType.MITIGATE));
        }
        if (channelName.contains("${")) {
            violation(event, createViolation("Send protocol channel in plugin message", PunishType.KICK));
        }
        checkPayload(wrapper, event, playerData);
    }

    private boolean isBookChannel(String channelName) {
        return channelName.equals("MC|BEdit") || channelName.equals("MC|BSign") || channelName.equals("minecraft:bedit")
               || channelName.equals("minecraft:bsign");
    }

    private void checkPayload(WrapperPlayClientPluginMessage wrapper, PacketReceiveEvent event, PlayerData playerData) {
        String payload = new String(wrapper.getData(), StandardCharsets.UTF_8);
        if (payload.equalsIgnoreCase("N")) {
            violation(
                event, createViolation(
                    "Console Spammer of Liquid",
                    this.violations() > 3 ? PunishType.BAN : PunishType.MITIGATE
                ));
        }
        checkBookInHand(event, payload);
        handleChannels(event, payload, playerData, wrapper.getChannelName());
    }

    private void checkBookInHand(PacketReceiveEvent event, String payload) {
        if (payload.contains("MC|BEdit") || payload.contains("MC|BSign") || payload.contains("MC|BOpen")) {
            Player player = (Player) event.getPlayer();
            //noinspection deprecation
            XMaterial xMaterial = XMaterial.matchXMaterial(player.getItemInHand());
            if (xMaterial != XMaterial.WRITABLE_BOOK && xMaterial != XMaterial.WRITTEN_BOOK
                && xMaterial != XMaterial.BOOK) {
                violation(event, createViolation("No book in hand", PunishType.KICK));
            }
        }
    }

    private void handleChannels(PacketReceiveEvent event, String payload, PlayerData playerData, String channelName) {
        String[] channels = payload.split("\0");
        if (channelName.equals("REGISTER")) {
            handleRegisterChannels(event, playerData, channels);
        } else if (channelName.equals("UNREGISTER")) {
            handleUnregisterChannels(playerData, channels);
        }
    }

    private void handleRegisterChannels(PacketReceiveEvent event, PlayerData playerData, String[] channels) {
        if (playerData.getChannels().size() + channels.length > 124 || channels.length > 124) {
            violation(event, createViolation("Invalid channel length: " + channels.length, PunishType.BAN));
        } else {
            for (String channel : channels) {
                playerData.getChannels().add(channel);
            }
        }
    }

    private void handleUnregisterChannels(PlayerData playerData, String[] channels) {
        for (String channel : channels) {
            playerData.getChannels().remove(channel);
        }
    }

    private void handlePlayerBlockPlacement(PacketReceiveEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientPlayerBlockPlacement(event), playerData::exceptionDisconnect);

            if (wrapper == null) return;

            checkBlockPlacement(wrapper, event);
        }
    }

    private void checkBlockPlacement(WrapperPlayClientPlayerBlockPlacement wrapper, PacketReceiveEvent event) {

        if (wrapper.getSequence() < 0 && isSupportedVersion(
            ServerVersion.V_1_19, event.getUser(), ClientVersion.V_1_19)) {
            violation(event, createViolation("Invalid sequence in block place", PunishType.BAN));
        }
        if (wrapper.getItemStack().isPresent()) {
            ItemStack itemStack = wrapper.getItemStack().get();
            checkBookUse(itemStack);
            checkItemStack(event, itemStack);
        }
    }

    private void checkBookUse(ItemStack itemStack) {
        if (itemStack.getType() == ItemTypes.WRITABLE_BOOK || itemStack.getType() == ItemTypes.WRITTEN_BOOK
            || itemStack.getType() == ItemTypes.BOOK) {
            this.lastBookUse = System.currentTimeMillis();
        }
    }

    private void handleSteerVehicle(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
            WrapperPlayClientSteerVehicle wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientSteerVehicle(event), getPlayerData()::exceptionDisconnect);
            checkSteerVehicle(wrapper, event);
        }
    }

    private void checkSteerVehicle(WrapperPlayClientSteerVehicle wrapper, PacketReceiveEvent event) {
        float forward  = wrapper.getForward();
        float sideways = wrapper.getSideways();
        if (forward > 0.98f || forward < -0.98f) {
            violation(event, createViolation(String.format("forward: %.2f", forward), PunishType.KICK));
        }
        if (sideways > 0.98f || sideways < -0.98f) {
            violation(event, createViolation(String.format("sideways: %.2f", sideways), PunishType.KICK));
        }
    }

    private void handleInteractEntity(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientInteractEntity(event), getPlayerData()::exceptionDisconnect);
            checkInteractEntity(wrapper, event);
        }
    }

    private void checkInteractEntity(WrapperPlayClientInteractEntity wrapper, PacketReceiveEvent event) {
        int entityId = event.getUser().getEntityId();
        if (wrapper.getEntityId() < 0 || entityId == wrapper.getEntityId()) {
            violation(
                event, createViolation("Id at " + wrapper.getEntityId() + ", player id: " + entityId, PunishType.KICK));
        }
    }

    private void handleNameItem(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.NAME_ITEM) {
            WrapperPlayClientNameItem wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientNameItem(event), getPlayerData()::exceptionDisconnect);
            checkNameItem(wrapper, event);
        }
    }

    private void checkNameItem(WrapperPlayClientNameItem wrapper, PacketReceiveEvent event) {
        if (wrapper.getItemName().contains("${")) {
            violation(event, createViolation("Send log4j exploit in item name", PunishType.KICK));
        }
        int length = wrapper.getItemName().length();
        if (length > 0 && FieldReader.isReadable(wrapper.getItemName())) {
            violation(event, createViolation("Name is not readable: " + wrapper.getItemName(), PunishType.KICK));
        }
        if (length > 50) {
            violation(event, createViolation("Name longer than 50: " + length, PunishType.KICK));
        }
    }

    private void handlePlayerDigging(PacketReceiveEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING && isSupportedVersion(
            ServerVersion.V_1_19, event.getUser(), ClientVersion.V_1_19)) {
            WrapperPlayClientPlayerDigging dig = CastUtil.getSupplier(
                () -> new WrapperPlayClientPlayerDigging(event), playerData::exceptionDisconnect);
            checkPlayerDigging(dig, event);
        }
    }

    private void checkPlayerDigging(WrapperPlayClientPlayerDigging dig, PacketReceiveEvent event) {
        if (dig.getSequence() < 0 && isSupportedVersion(ServerVersion.V_1_19, event.getUser(), ClientVersion.V_1_19)) {
            violation(event, createViolation("Invalid sequence in dig", PunishType.BAN));
        }
    }

    private void handleUseItem(PacketReceiveEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM && isSupportedVersion(
            ServerVersion.V_1_19, event.getUser(), ClientVersion.V_1_19)) {
            WrapperPlayClientUseItem use = CastUtil.getSupplier(
                () -> new WrapperPlayClientUseItem(event), playerData::exceptionDisconnect);
            if (use.getSequence() < 0) {
                violation(event, createViolation("Invalid sequence in use item", PunishType.BAN));
            }
        }
    }

    private void handleClickWindow(PacketReceiveEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientClickWindow(event), playerData::exceptionDisconnect);

            if (wrapper == null) return;

            checkClickWindow(wrapper, event);
        }
    }

    private void checkClickWindow(WrapperPlayClientClickWindow wrapper, PacketReceiveEvent event) {
        if (isSupportedServerVersion(ServerVersion.V_1_14)) {
            int clickType = wrapper.getWindowClickType().ordinal();
            int button    = wrapper.getButton();
            int windowId  = wrapper.getWindowId();
            if (type == MenuType.LECTERN && windowId > 0 && windowId == lecternId) {
                violation(event, createViolation("clickType=" + clickType + ", button=" + button, PunishType.KICK));
            }
        }

        checkButtonClickPosition(event, wrapper);
        ItemStack carriedItemStack = wrapper.getCarriedItemStack();
        checkItemStack(event, carriedItemStack);
        checkForInvalidSlot(event, wrapper);
        checkInvalidClick(wrapper, event);
    }

    private void handleCloseWindow() {
        hasOpenAnvil = false;
    }

    private void checkItemStack(PacketReceiveEvent event, ItemStack itemStack) {
        if (itemStack == null || itemStack.getNBT() == null) return;
        checkGenericBookPages(event, itemStack);
        checkGenericNBTLimit(event, itemStack);
        checkLanguageExploit(event, itemStack);
        checkAttributes(event, itemStack);
        checkInvalidNbt(event, itemStack);
        checkForInvalidBanner(event, itemStack);
        checkForInvalidArmorStand(event, itemStack);
        checkForInvalidContainer(event, itemStack);
        checkForInvalidShulker(event, itemStack);
        checkNbtTags(event, itemStack);
    }

    private void checkGenericBookPages(PacketReceiveEvent event, ItemStack itemStack) {
        //noinspection DataFlowIssue
        NBTList<NBTString> pages = itemStack.getNBT().getStringListTagOrNull("pages");
        if (pages == null) return;
        if (pages.getTags().size() > 50) {
            violation(event, createViolation("Too many pages, pv", PunishType.KICK));
        }
        int totalLength = pages.getTags().stream().mapToInt(tag -> tag.getValue().length()).sum();
        if (totalLength > 12800) {
            violation(event, createViolation("Reached general book limit", PunishType.KICK));
        }
    }

    private void checkLanguageExploit(PacketReceiveEvent event, ItemStack itemStack) {
        //noinspection DataFlowIssue
        String mapped = FormatUtils.mapToString(itemStack.getNBT().getTags());
        if (mapped.contains("translate") || mapped.contains("options.snooper.desc")
            || FormatUtils.countOccurrences(mapped, "translate") > 20) {
            itemStack.setNBT(new NBTCompound());
            violation(event, createViolation("Contains translate request", PunishType.MITIGATE));
        }
    }

    private void checkAttributes(ProtocolPacketEvent<Object> event, ItemStack itemStack) {
        if (!hasAttributeModifiers(itemStack)) return;
        List<NBTCompound> tags           = getAttributeModifiers(itemStack);
        boolean           vanillaMapping = useVanillaAttributeMapping();
        for (NBTCompound tag : tags) {
            AttributeMapper attributeMapper = getAttributeMapper(tag);
            if (attributeMapper != null) {
                handleAttributeViolation(event, vanillaMapping, attributeMapper, tag);
            }
        }
    }

    private AttributeMapper getAttributeMapper(NBTCompound tag) {
        //noinspection DataFlowIssue
        return AttributeMapper.getAttributeMapper(tag.getStringTagOrNull("AttributeName").getValue());
    }

    private boolean hasAttributeModifiers(ItemStack itemStack) {
        return itemStack.getNBT() != null && itemStack.getNBT().getCompoundListTagOrNull("AttributeModifiers") != null;
    }

    private List<NBTCompound> getAttributeModifiers(ItemStack itemStack) {
        //noinspection DataFlowIssue
        return itemStack.getNBT().getCompoundListTagOrNull("AttributeModifiers").getTags();
    }

    public void checkGenericNBTLimit(PacketReceiveEvent event, ItemStack itemStack) {
        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("generic-nbt-limit", true)) {
            return;
        }
        //noinspection DataFlowIssue
        int length = FormatUtils.mapToString(itemStack.getNBT().getTags()).length();
        int limit  = getPlayerData().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16) ? 30000 : 25000;
        if (length > limit) {
            violation(event, createViolation("length=" + length + ", limit=" + limit, PunishType.MITIGATE));
        }
    }

    private void handleAttributeViolation(ProtocolPacketEvent<Object> event, boolean vanillaMapping,
                                          AttributeMapper attributeMapper, NBTCompound tag) {
        //noinspection DataFlowIssue
        double amount = tag.getNumberTagOrNull("Amount").getAsDouble();
        if (isAmountInvalid(vanillaMapping, attributeMapper, amount)) {
            violation(event, createViolation("Invalid attribute modifier. Amount: " + amount, PunishType.KICK));
        } else if (!vanillaMapping && isSierraModifierInvalid(amount)) {
            violation(event, createViolation("Sierra attribute modifier. Amount: " + amount, PunishType.KICK));
        } else if (FormatUtils.checkDoublePrecision(amount)) {
            violation(event, createViolation("Double is to precisely", PunishType.KICK));
        }
    }

    private boolean useVanillaAttributeMapping() {
        return Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("use-vanilla-attribute-mapping", true);
    }

    private boolean isAmountInvalid(boolean vanillaMapping, AttributeMapper attributeMapper, double amount) {
        return vanillaMapping && (amount > attributeMapper.getMax() || amount < attributeMapper.getMin());
    }

    private boolean isSierraModifierInvalid(double amount) {
        return Math.abs(amount) > 5.000;
    }

    private void checkInvalidNbt(PacketReceiveEvent event, ItemStack itemStack) {
        if (itemStack == null || itemStack.getNBT() == null) return;
        NBTCompound          nbt   = itemStack.getNBT();
        NBTList<NBTCompound> items = nbt.getTagListOfTypeOrNull("Items", NBTCompound.class);
        if (items != null) {
            if (items.size() > 64) {
                violation(event, createViolation("Too big items list", PunishType.MITIGATE));
            }
            for (NBTCompound tag : items.getTags()) {
                checkItemTag(tag, event);
            }
        }
        checkProjectileTags(nbt, event);
        checkCustomModelData(nbt, event);
    }

    private void checkItemTag(NBTCompound tag, PacketReceiveEvent event) {
        if (tag.getStringTagOrNull("id") != null) {
            //noinspection DataFlowIssue
            String value = tag.getStringTagOrNull("id").getValue();
            if (value.equalsIgnoreCase("minecraft:air") || value.equalsIgnoreCase("minecraft:bundle")) {
                violation(event, createViolation("Invalid item: " + value, PunishType.MITIGATE));
            }
        }
    }

    private void checkProjectileTags(NBTCompound nbt, PacketReceiveEvent event) {
        NBTList<NBTCompound> chargedProjectiles = nbt.getTagListOfTypeOrNull("ChargedProjectiles", NBTCompound.class);
        if (chargedProjectiles != null) {
            for (NBTCompound tag : chargedProjectiles.getTags()) {
                NBTCompound tag1 = tag.getCompoundTagOrNull("tag");
                //noinspection DataFlowIssue
                if (tag1 != null && tag1.getStringTagOrNull("Potion") != null && tag1.getStringTagOrNull("Potion")
                    .getValue()
                    .endsWith("empty")) {
                    violation(event, createViolation("Invalid projectile: empty", PunishType.MITIGATE));
                }
            }
        }
    }

    private void checkCustomModelData(NBTCompound nbt, PacketReceiveEvent event) {
        NBTInt customModelData = nbt.getTagOfTypeOrNull("CustomModelData", NBTInt.class);
        if (customModelData != null && isSupportedServerVersion(ServerVersion.V_1_14)) {
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

    private void checkForInvalidSlot(PacketReceiveEvent event, WrapperPlayClientClickWindow wrapper) {

        int slot = wrapper.getSlot();
        // 89 - Biggest inv is the LARGE_CHEST: https://wiki.vg/Inventory
        int     max     = 89;
        boolean invalid = slot > max;

        if (slot < 0) {
            if (slot != -999 && slot != -1) { // Minecraft used -id's
                invalid = true;
            }
        }

        if (invalid) {
            violation(event, createViolation("Invalid slot " + slot + ", max: " + max,
                                             PunishType.MITIGATE));
        }
    }

    public boolean areBracketsTooFrequent(String input, int threshold) {
        return input.chars().filter(c -> c == '[' || c == ']').count() > threshold;
    }

    private void checkButtonClickPosition(PacketReceiveEvent event, WrapperPlayClientClickWindow wrapper) {
        int     clickType = wrapper.getWindowClickType().ordinal();
        int     button    = wrapper.getButton();
        boolean flag      = isInvalidButtonClick(clickType, button);
        if (flag) {
            violation(
                event, createViolation(
                    "clickType=" + clickType + " button=" + button + (wrapper.getWindowId() == containerId
                        ? " container=" + containerType
                        : ""), PunishType.MITIGATE));
        }
    }

    private boolean isInvalidButtonClick(int clickType, int button) {
        switch (clickType) {
            case 0:
            case 1:
            case 4:
                return button != 0 && button != 1;
            case 2:
                return (button > 8 || button < 0) && button != 40;
            case 3:
                return button != 2;
            case 5:
                return button == 3 || button == 7 || button > 10 || button < 0;
            case 6:
                return button != 0;
            default:
                return false;
        }
    }

    private void checkForInvalidShulker(PacketReceiveEvent event, ItemStack itemStack) {
        if (isShulkerBox(itemStack)) {
            //noinspection DataFlowIssue
            String string = FormatUtils.mapToString(itemStack.getNBT().getTags());
            if (string.getBytes(StandardCharsets.UTF_8).length > 10000) {
                violation(event, createViolation("Invalid shulker size", PunishType.KICK));
            }
        }
    }

    private boolean isShulkerBox(ItemStack itemStack) {
        try {
            ShulkerBoxType.valueOf(itemStack.getType().toString());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void checkForInvalidContainer(PacketReceiveEvent event, ItemStack itemStack) {
        if (isContainerItem(itemStack)) {
            //noinspection DataFlowIssue
            String string = FormatUtils.mapToString(itemStack.getNBT().getTags());
            checkForInvalidSizeAndPresence(event, string);
        }
    }

    private boolean isContainerItem(ItemStack itemStack) {
        return itemStack.getType() == ItemTypes.CHEST || itemStack.getType() == ItemTypes.HOPPER
               || itemStack.getType() == ItemTypes.HOPPER_MINECART || itemStack.getType() == ItemTypes.CHEST_MINECART;
    }

    private void checkForInvalidSizeAndPresence(PacketReceiveEvent event, String string) {
        if (string.getBytes(StandardCharsets.UTF_8).length > MAX_BYTE_SIZE) {
            violation(event, createViolation("Invalid container size", PunishType.KICK));
        }
        if (string.contains(WURSTCLIENT_URL)) {
            violation(event, createViolation("Wurstclient container", PunishType.BAN));
        }
    }

    private void checkNbtTags(PacketReceiveEvent event, ItemStack itemStack) {
        if (itemStack.getNBT() != null) {
            itemStack.getNBT().getTags().forEach((s, nbt) -> {
                NBTType<?> nbtType = nbt.getType();
                if (nbtType.equals(LIST)) {
                    checkList(s, event, itemStack);
                } else if (nbtType.equals(INT_ARRAY)) {
                    checkIntArray(s, event, itemStack);
                } else if (nbtType.equals(LONG_ARRAY)) {
                    checkLongArray(s, event, itemStack);
                } else if (nbtType.equals(BYTE_ARRAY)) {
                    checkByteArray(s, event, itemStack);
                }
            });
        }
        this.listContent.set(0);
    }

    private void checkList(String s, PacketReceiveEvent event, ItemStack itemStack) {
        //noinspection DataFlowIssue
        NBTList<NBTCompound> tagOrNull = itemStack.getNBT().getCompoundListTagOrNull(s);
        if (tagOrNull != null) {
            if (tagOrNull.getTags().size() > 50) {
                violation(event, createViolation("Too big nbt list size", PunishType.KICK));
            }
            for (NBTCompound tag : tagOrNull.getTags()) {
                if (tag == null || tag.toString().equalsIgnoreCase("null") || tag.toString().length() > 900) {
                    violation(event, createViolation("Invalid tag in nbt list", PunishType.KICK));
                }
            }
        }
        if (listContent.incrementAndGet() > 10) {
            violation(event, createViolation("Too many nbt lists", PunishType.KICK));
        }
    }

    private void checkIntArray(String key, PacketReceiveEvent event, ItemStack itemStack) {
        //noinspection DataFlowIssue
        NBTList<NBTIntArray> tagListOfTypeOrNull = itemStack.getNBT().getTagListOfTypeOrNull(key, NBTIntArray.class);
        if (tagListOfTypeOrNull != null) {
            if (tagListOfTypeOrNull.size() > 50) {
                violation(event, createViolation("Too big int array size", PunishType.KICK));
            }
            for (NBTIntArray tag : tagListOfTypeOrNull.getTags()) {
                if (tag.getValue().length > 150) {
                    violation(event, createViolation("Invalid integer length", PunishType.KICK));
                }
                for (int i : tag.getValue()) {
                    if (i == Integer.MAX_VALUE || i == Integer.MIN_VALUE) {
                        violation(event, createViolation("Integer size out of bounds", PunishType.KICK));
                    }
                }
            }
        }
    }

    private void checkLongArray(String key, PacketReceiveEvent event, ItemStack itemStack) {
        //noinspection DataFlowIssue
        NBTList<NBTLongArray> tagListOfTypeOrNull = itemStack.getNBT().getTagListOfTypeOrNull(key, NBTLongArray.class);
        if (tagListOfTypeOrNull != null) {
            if (tagListOfTypeOrNull.size() > 50) {
                violation(event, createViolation("Too big long array size", PunishType.KICK));
            }
            for (NBTLongArray tag : tagListOfTypeOrNull.getTags()) {
                if (tag.getValue().length > 150) {
                    violation(event, createViolation("Invalid long length", PunishType.KICK));
                }
                for (long i : tag.getValue()) {
                    if (i == Long.MAX_VALUE || i == Long.MIN_VALUE) {
                        violation(event, createViolation("Long size out of bounds", PunishType.KICK));
                    }
                }
            }
        }
    }

    private void checkByteArray(String key, PacketReceiveEvent event, ItemStack itemStack) {
        //noinspection DataFlowIssue
        NBTList<NBTByteArray> tagListOfTypeOrNull = itemStack.getNBT().getTagListOfTypeOrNull(key, NBTByteArray.class);
        if (tagListOfTypeOrNull != null) {
            if (tagListOfTypeOrNull.size() > 50) {
                violation(event, createViolation("Too big byte array size", PunishType.KICK));
            }
            for (NBTByteArray tag : tagListOfTypeOrNull.getTags()) {
                if (tag.getValue().length > 150) {
                    violation(event, createViolation("Invalid byte length", PunishType.KICK));
                }
                for (byte i : tag.getValue()) {
                    if (i == Byte.MAX_VALUE || i == Byte.MIN_VALUE) {
                        violation(event, createViolation("Byte size out of bounds", PunishType.KICK));
                    }
                }
            }
        }
    }

    private void checkForInvalidArmorStand(PacketReceiveEvent event, ItemStack itemStack) {
        if (itemStack.getType() != ItemTypes.ARMOR_STAND || itemStack.getNBT() == null) return;
        NBTCompound entityTag = itemStack.getNBT().getCompoundTagOrNull("EntityTag");
        if (entityTag == null) return;
        checkInvalidPoses(event, entityTag);
        checkInvalidCustomName(event, entityTag);
        checkInvalidSkullOwner(event, entityTag);
        checkInvalidRotation(event, entityTag);
    }

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

    private void checkInvalidSkullOwner(PacketReceiveEvent event, NBTCompound entityTag) {
        NBTList<NBTCompound> equipment = entityTag.getCompoundListTagOrNull("Equipment");
        if (equipment != null) {
            for (NBTCompound tag : equipment.getTags()) {
                checkSkullOwner(event, tag);
            }
        }
    }

    private void checkInvalidRotation(PacketReceiveEvent event, NBTCompound entityTag) {
        NBTList<NBTNumber> rotation = entityTag.getNumberListTagOrNull("Rotation");
        if (rotation != null) {
            for (NBTNumber tag : rotation.getTags()) {
                float armorStandRotation = tag.getAsFloat();
                if (armorStandRotation < 0 || armorStandRotation > 360) {
                    violation(
                        event, createViolation("Invalid armor stand rotation: " + armorStandRotation, PunishType.KICK));
                }
            }
        }
    }

    private void checkSkullOwner(PacketReceiveEvent event, NBTCompound item) {
        if ("skull".equals(item.getStringTagValueOrNull("id"))) {
            NBTCompound tag = item.getCompoundTagOrNull("tag");
            if (tag != null) {
                NBTString skullOwner = tag.getStringTagOrNull("SkullOwner");
                if (skullOwner != null) {
                    String name = skullOwner.getValue();
                    if (name.length() < 3 || name.length() > 16 || !name.matches("^[a-zA-Z0-9_\\-.]{3,16}$")) {
                        violation(event, createViolation("Invalid skull owner name: " + name, PunishType.KICK));
                    }
                }
            }
        }
    }

    private void invalidPoseAngles(PacketReceiveEvent event, NBTCompound pose, String limb) {
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

    private void validatePattern(PacketReceiveEvent event, NBTString pattern) {
        if (pattern == null || pattern.getValue() == null) {
            createViolation(event, "Banner pattern is null");
            return;
        }
        if (pattern.getValue().length() > MAX_PATTERN_LENGTH) {
            createViolation(event, "Banner pattern is too long: " + pattern.getValue().length());
        }
    }

    private void createViolation(PacketReceiveEvent event, String message) {
        violation(event, createViolation(message, PunishType.MITIGATE));
    }

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

    private boolean isBanner(ItemStack itemStack) {
        try {
            BannerType.valueOf(itemStack.getType().toString());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void handle(PacketSendEvent event, PlayerData playerData) {
        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-protocol-packet", true)) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_EXPERIENCE) {
            WrapperPlayServerSetExperience wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayServerSetExperience(event), playerData::exceptionDisconnect);
            checkSetExperience(wrapper, event);
        } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayServerWindowItems(event), playerData::exceptionDisconnect);
            checkWindowItems(wrapper, event);
        } else if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            WrapperPlayServerOpenWindow window = CastUtil.getSupplier(
                () -> new WrapperPlayServerOpenWindow(event), playerData::exceptionDisconnect);
            checkOpenWindow(window);
        }
    }

    private void checkSetExperience(WrapperPlayServerSetExperience wrapper, PacketSendEvent event) {
        if (wrapper.getLevel() < 0 || wrapper.getExperienceBar() < 0 || wrapper.getTotalExperience() < 0) {
            violation(
                event, createViolation(
                    String.format("Stats at %d/%s/%d", wrapper.getLevel(), wrapper.getExperienceBar(),
                                  wrapper.getTotalExperience()
                    ), PunishType.KICK));
        }
    }

    private void checkWindowItems(WrapperPlayServerWindowItems wrapper, PacketSendEvent event) {
        for (ItemStack item : wrapper.getItems()) {
            if (item.getNBT() == null) continue;
            checkAttributes(event, item);
        }
    }

    private void checkOpenWindow(WrapperPlayServerOpenWindow window) {

        System.out.println("Window Type: " + window.getType());
        System.out.println("Legacy Type: " + window.getLegacyType());
        System.out.println("Container Id: " + window.getContainerId());

        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14)) {
            if (window.getType() == MenuType.ANVIL.getId()) {
                hasOpenAnvil = true;
            }
        } else {
            if (window.getLegacyType().contains("anvil")) {
                hasOpenAnvil = true;
            }
        }
        if (isSupportedServerVersion(ServerVersion.V_1_14)) {
            this.type = MenuType.getMenuType(window.getType());
            if (type == MenuType.LECTERN) lecternId = window.getContainerId();
        }
        this.containerType = window.getType();
        this.containerId = window.getContainerId();
    }

    private boolean isSupportedServerVersion(ServerVersion serverVersion) {
        return PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(serverVersion);
    }

    private boolean isSupportedVersion(@SuppressWarnings("SameParameterValue") ServerVersion serverVersion, User user,
                                       @SuppressWarnings("SameParameterValue") ClientVersion clientVersion) {
        return user.getClientVersion().isNewerThanOrEquals(clientVersion) && isSupportedServerVersion(serverVersion);
    }

    private void checkInvalidClick(WrapperPlayClientClickWindow wrapper, PacketReceiveEvent event) {
        int clickType = wrapper.getWindowClickType().ordinal();
        int button    = wrapper.getButton();
        int windowId  = wrapper.getWindowId();
        int slot      = wrapper.getSlot();

        if (button < 0 || windowId < 0) {
            violation(
                event, createViolation(
                    "Button: " + button + ", window: " + windowId + ", slot: " + slot,
                    PunishType.KICK
                ));
        }
        if ((clickType == 1 || clickType == 2) && windowId >= 0 && button < 0) {
            violation(event, createViolation("clickType=" + clickType + ", button=" + button, PunishType.KICK));
        } else if (windowId >= 0 && clickType == 2 && slot < 0) {
            violation(
                event, createViolation(
                    "clickType=" + clickType + ", button=" + button + ", slot=" + slot,
                    PunishType.KICK
                ));
        }

        if (slot > 127 || slot < -999) {
            violation(event, createViolation("Invalid slot at " + slot, PunishType.KICK));
        }
    }
}
