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
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.packet.OutgoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.manager.storage.menu.MenuType;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierra.utilities.FieldReader;
import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierra.utilities.attributes.AttributeMapper;
import de.feelix.sierra.utilities.types.BannerType;
import de.feelix.sierra.utilities.types.ShulkerBoxType;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.violation.MitigationStrategy;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
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
    private              boolean       hasOpenAnvil       = false;
    private static final Pattern       EXPLOIT_PATTERN    = Pattern.compile("\\$\\{.+}");
    private              int           containerType      = -1;
    private              int           containerId        = -1;
    private static final int           MAX_BYTE_SIZE      = 262144;
    private static final String        WURSTCLIENT_URL    = "www.wurstclient.net";
    private static final int           MAX_BANNER_LAYERS  = 15;
    private static final int           MAX_PATTERN_LENGTH = 50;
    private static final int           MIN_VALID_COLOR    = 0;
    private static final int           MAX_SIGN_LENGTH    = 45;
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
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.KICK)
                .description("sends too big packet")
                .debugs(Arrays.asList(new Debug<>("Bytes", capacity), new Debug<>("Max Bytes", maxBytes)))
                .build());
        }

        int readableBytes     = ByteBufHelper.readableBytes(event.getByteBuf());
        int maxBytesPerSecond = 64000 * (playerData.getClientVersion().isOlderThan(ClientVersion.V_1_8) ? 2 : 1);
        playerData.setBytesSent(playerData.getBytesSent() + readableBytes);

        if (playerData.getBytesSent() > maxBytesPerSecond) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.KICK)
                .description("sends too big packet in a second")
                .debugs(Arrays.asList(
                    new Debug<>("Bytes", playerData.getBytesSent()),
                    new Debug<>("Max Bytes", maxBytesPerSecond)
                ))
                .build());
        }

        handleAnvilInventory(event);
        handleClientSettings(event, playerData);
        handleCreativeInventoryAction(event, playerData);
        handleEntityAction(event);
        handleSpectate(event);
        handleClickWindowButton(event);
        handleChatMessage(event);
        handleHeldItemChange(event);
        handleTabComplete(event, playerData);
        handleUpdateSign(event, playerData);
        handlePlayerBlockPlacement(event, playerData);
        handleSteerVehicle(event);
        handleInteractEntity(event);
        handleNameItem(event);
        handlePlayerDigging(event, playerData);
        handleUseItem(event, playerData);
        handleClickWindow(event, playerData);
        handlePluginMessage(event, playerData);
    }

    private void handleAnvilInventory(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);

            String channelName = wrapper.getChannelName();

            if (channelName.equalsIgnoreCase("MC|ItemName") && !hasOpenAnvil) {

                dispatch(event, ViolationDocument.builder()
                    .mitigationStrategy(MitigationStrategy.KICK)
                    .description("send anvil payload with closed inventory")
                    .debugs(Collections.emptyList())
                    .build());
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            if (hasOpenAnvil) hasOpenAnvil = false;
        }
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
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.KICK)
                .description("send null value in packet")
                .debugs(Collections.singletonList(new Debug<>("Tag", "ClientSettings")))
                .build());
        }
        if (EXPLOIT_PATTERN.matcher(wrapper.getLocale()).matches()) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .description("send exploit in locale")
                .debugs(Collections.singletonList(new Debug<>("Locale", wrapper.getLocale())))
                .build());
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

            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.BAN)
                .description("send invalid entity id")
                .debugs(Collections.singletonList(new Debug<>("Id", wrapper.getEntityId())))
                .build());
        }
        if (wrapper.getJumpBoost() < 0 || wrapper.getJumpBoost() > 100) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.KICK)
                .description("send invalid jump boost")
                .debugs(Collections.singletonList(new Debug<>("Boost", wrapper.getJumpBoost())))
                .build());
        }
    }

    private void handleSpectate(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.SPECTATE
            && getPlayerData().getGameMode() != GameMode.SPECTATOR) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.BAN)
                .description("spoofed his game-mode")
                .debugs(Collections.singletonList(new Debug<>("GameMode", GameMode.SPECTATOR.name())))
                .build());
        }
    }

    private void handleClickWindowButton(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW_BUTTON) {
            WrapperPlayClientClickWindowButton wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientClickWindowButton(event), getPlayerData()::exceptionDisconnect);
            if (wrapper.getButtonId() < 0 || wrapper.getWindowId() < 0) {

                dispatch(event, ViolationDocument.builder()
                    .mitigationStrategy(MitigationStrategy.BAN)
                    .description("clicked on invalid button")
                    .debugs(Arrays.asList(
                        new Debug<>("WindowId", wrapper.getWindowId()),
                        new Debug<>("ButtonId", wrapper.getButtonId())
                    ))
                    .build());
            }
        }
    }

    private void handleChatMessage(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
            WrapperPlayClientChatMessage wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientChatMessage(event), getPlayerData()::exceptionDisconnect);
            if (wrapper.getMessage().contains("${")) {

                dispatch(event, ViolationDocument.builder()
                    .mitigationStrategy(MitigationStrategy.KICK)
                    .description("send log4j exploit")
                    .debugs(Collections.singletonList(new Debug<>("Message", wrapper.getMessage())))
                    .build());
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
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.BAN)
                .description("send invalid hot-bar slot")
                .debugs(Collections.singletonList(new Debug<>("Slot", slot)))
                .build());
        }
        Player player = (Player) event.getPlayer();
        if (player == null) return;
        int length = player.getInventory().getContents().length;
        if (!(wrapper.getSlot() >= 0 && wrapper.getSlot() < length)) {

            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.BAN)
                .description("send invalid hot-bar slot")
                .debugs(Arrays.asList(new Debug<>("Slot", slot), new Debug<>("Max", length)))
                .build());
        }
        if (slot == lastSlot) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .description("send hot-bar slot twice")
                .debugs(Arrays.asList(new Debug<>("Slot", slot), new Debug<>("Last", lastSlot)))
                .build());
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
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.BAN)
                .description("send null value in tab-complete")
                .debugs(Collections.singletonList(new Debug<>("Tag", "Null")))
                .build());
        }
        String text   = wrapper.getText();
        int    length = text.length();
        if (CommandValidation.WORLDEDIT_PATTERN.matcher(text).matches()) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .description("send invalid tab-complete")
                .debugs(Arrays.asList(new Debug<>("Tag", "WorldEdit"), new Debug<>("Text", text)))
                .build());
        }
        if (areBracketsTooFrequent(text, 15) || CommandValidation.WORLDEDIT_PATTERN.matcher(text).matches()) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.KICK)
                .description("send invalid tab-complete")
                .debugs(Arrays.asList(new Debug<>("Tag", "WorldEdit-Brackets"), new Debug<>("Text", text)))
                .build());
        }
        if (length > 256) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .description("send invalid tab-complete")
                .debugs(Arrays.asList(new Debug<>("Tag", "Length"), new Debug<>("Length", length)))
                .build());
        }
        if ((text.equals("/") || text.trim().isEmpty()) && isSupportedServerVersion(ServerVersion.V_1_13)) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .description("send invalid tab-complete")
                .debugs(Collections.singletonList(new Debug<>("Tag", "Trim")))
                .build());
        }
        int index;
        if (text.length() > 64 && ((index = text.indexOf(' ')) == -1 || index >= 64)) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.KICK)
                .description("send invalid tab-complete")
                .debugs(Collections.singletonList(new Debug<>("Length", length)))
                .build());
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
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.KICK)
                .description("send sign update out of distance")
                .debugs(Collections.singletonList(new Debug<>("Distance", distanceFromLastLocation)))
                .build());
        }
        for (String textLine : wrapper.getTextLines()) {
            if (textLine.toLowerCase().contains("run_command")) {
                dispatch(event, ViolationDocument.builder()
                    .mitigationStrategy(MitigationStrategy.KICK)
                    .description("send raw json in sign update")
                    .debugs(Collections.emptyList())
                    .build());
            }
            if (textLine.length() > MAX_SIGN_LENGTH) {
                dispatch(event, ViolationDocument.builder()
                    .mitigationStrategy(MitigationStrategy.BAN)
                    .description("send to big sign-update")
                    .debugs(Collections.singletonList(new Debug<>("Length", textLine.length())))
                    .build());
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

        if (isBookChannel(channelName) && System.currentTimeMillis() - this.lastBookUse > 60000L) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .description("send book sign without book use")
                .debugs(Collections.emptyList())
                .build());
        }
        if (channelName.contains("${")) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.KICK)
                .description("send protocol channel in plugin message")
                .debugs(Collections.singletonList(new Debug<>("Channel", channelName)))
                .build());
        }
        checkPayload(wrapper, event, playerData);
    }

    private boolean isBookChannel(String channelName) {
        return channelName.equalsIgnoreCase("MC|BEdit")
               || channelName.equalsIgnoreCase("MC|BSign")
               || channelName.equalsIgnoreCase("minecraft:bedit")
               || channelName.equalsIgnoreCase("minecraft:bsign");
    }

    private void checkPayload(WrapperPlayClientPluginMessage wrapper, PacketReceiveEvent event, PlayerData playerData) {
        String payload = new String(wrapper.getData(), StandardCharsets.UTF_8);
        if (payload.equalsIgnoreCase("N")) {

            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(this.violations() > 3 ? MitigationStrategy.BAN : MitigationStrategy.MITIGATE)
                .description("send liquid-bounce console spammer")
                .debugs(Collections.emptyList())
                .build());
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

                dispatch(event, ViolationDocument.builder()
                    .mitigationStrategy(MitigationStrategy.KICK)
                    .description("send book packet without book in hand")
                    .debugs(Collections.emptyList())
                    .build());
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
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.BAN)
                .description("send to big channel in payload")
                .debugs(Collections.singletonList(new Debug<>("Length", channels.length)))
                .build());
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
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.BAN)
                .description("send invalid sequence in block place")
                .debugs(Collections.singletonList(new Debug<>("Sequence", wrapper.getSequence())))
                .build());
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
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.KICK)
                .description("send invalid steer vehicle")
                .debugs(Collections.singletonList(new Debug<>("Forward", forward)))
                .build());
        }
        if (sideways > 0.98f || sideways < -0.98f) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.KICK)
                .description("send invalid steer vehicle")
                .debugs(Collections.singletonList(new Debug<>("Sideways", sideways)))
                .build());
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

            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.KICK)
                .description("send invalid interact")
                .debugs(Arrays.asList(
                    new Debug<>("Id", wrapper.getEntityId()),
                    new Debug<>("Own", entityId == wrapper.getEntityId())
                ))
                .build());
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
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.KICK)
                .description("send log4j exploit in item name")
                .debugs(Collections.singletonList(new Debug<>("Message", wrapper.getItemName())))
                .build());
        }
        int length = wrapper.getItemName().length();
        if (length > 0 && FieldReader.isReadable(wrapper.getItemName())) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.KICK)
                .description("send unreadable item name")
                .debugs(Collections.singletonList(new Debug<>("Name", wrapper.getItemName())))
                .build());
        }
        if (length > 50) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.KICK)
                .description("send too big item name")
                .debugs(Collections.singletonList(new Debug<>("Length", length)))
                .build());
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
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.BAN)
                .description("send invalid dig sequence")
                .debugs(Collections.emptyList())
                .build());
        }
    }

    private void handleUseItem(PacketReceiveEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM && isSupportedVersion(
            ServerVersion.V_1_19, event.getUser(), ClientVersion.V_1_19)) {
            WrapperPlayClientUseItem use = CastUtil.getSupplier(
                () -> new WrapperPlayClientUseItem(event), playerData::exceptionDisconnect);
            if (use.getSequence() < 0) {
                dispatch(event, ViolationDocument.builder()
                    .mitigationStrategy(MitigationStrategy.BAN)
                    .description("send invalid use item sequence")
                    .debugs(Collections.emptyList())
                    .build());
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
                dispatch(event, ViolationDocument.builder()
                    .mitigationStrategy(MitigationStrategy.KICK)
                    .description("send invalid window click")
                    .debugs(Arrays.asList(new Debug<>("ClickType", clickType), new Debug<>("Button", button)))
                    .build());
            }
        }

        checkButtonClickPosition(event, wrapper);
        ItemStack carriedItemStack = wrapper.getCarriedItemStack();
        checkItemStack(event, carriedItemStack);
        checkForInvalidSlot(event, wrapper);
        checkInvalidClick(wrapper, event);
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

        if (itemStack == null || itemStack.getNBT() == null) return;

        NBTList<NBTString> pages = itemStack.getNBT().getStringListTagOrNull("pages");

        if (pages == null) return;

        if (pages.getTags().size() > 50) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.KICK)
                .description("send book with too many pages")
                .debugs(Collections.singletonList(new Debug<>("Pages", pages.getTags().size())))
                .build());
        }
        int totalLength = pages.getTags().stream().mapToInt(tag -> tag.getValue().length()).sum();
        if (totalLength > 12800) {
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.KICK)
                .description("send book with too big content")
                .debugs(Arrays.asList(new Debug<>("Length", totalLength), new Debug<>("Max", 12800)))
                .build());
        }
    }

    private void checkLanguageExploit(PacketReceiveEvent event, ItemStack itemStack) {

        if (itemStack == null || itemStack.getNBT() == null) return;

        String mapped = FormatUtils.mapToString(itemStack.getNBT().getTags());

        if (mapped.contains("translate") || mapped.contains("options.snooper.desc")
            || FormatUtils.countOccurrences(mapped, "translate") > 20) {

            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .description("send raw translate request")
                .debugs(Arrays.asList(
                    new Debug<>("Contains", mapped.contains("translate")),
                    new Debug<>("Snooper", mapped.contains("options.snooper.desc")),
                    new Debug<>("Count", FormatUtils.countOccurrences(mapped, "translate")),
                    new Debug<>("Max Count", 20)
                )).build());
            itemStack.setNBT(new NBTCompound());
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

        if (itemStack == null || itemStack.getNBT() == null) return;

        int length = FormatUtils.mapToString(itemStack.getNBT().getTags()).length();
        int limit  = getPlayerData().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16) ? 30000 : 25000;
        if (length > limit) {

            dispatch(event, ViolationDocument.builder()
                .description("send item-stack with too big nbt tag")
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .debugs(Arrays.asList(new Debug<>("Length", length), new Debug<>("Limit", limit)))
                .build());
        }
    }

    private void handleAttributeViolation(ProtocolPacketEvent<Object> event, boolean vanillaMapping,
                                          AttributeMapper attributeMapper, NBTCompound tag) {

        if (tag == null) return;

        NBTNumber numberTagOrNull = tag.getNumberTagOrNull("Amount");

        if (numberTagOrNull == null) return;

        double amount = numberTagOrNull.getAsDouble();

        if (isAmountInvalid(vanillaMapping, attributeMapper, amount)) {

            dispatch(event, ViolationDocument.builder()
                .description("send invalid attribute modifier")
                .mitigationStrategy(MitigationStrategy.KICK)
                .debugs(Collections.singletonList(new Debug<>("Amount", amount)))
                .build());

        } else if (!vanillaMapping && isSierraModifierInvalid(amount)) {
            dispatch(event, ViolationDocument.builder()
                .description("send invalid sierra-attribute modifier")
                .mitigationStrategy(MitigationStrategy.KICK)
                .debugs(Collections.singletonList(new Debug<>("Amount", amount)))
                .build());
        } else if (FormatUtils.checkDoublePrecision(amount)) {
            dispatch(event, ViolationDocument.builder()
                .description("send invalid attribute modifier")
                .mitigationStrategy(MitigationStrategy.KICK)
                .debugs(Collections.singletonList(new Debug<>("Tag", "Precision")))
                .build());
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
                dispatch(event, ViolationDocument.builder()
                    .description("send to big item-list")
                    .mitigationStrategy(MitigationStrategy.MITIGATE)
                    .debugs(Collections.singletonList(new Debug<>("Size", items.size())))
                    .build());
            }
            for (NBTCompound tag : items.getTags()) {
                checkItemTag(tag, event);
            }
        }
        checkProjectileTags(nbt, event);
        checkCustomModelData(nbt, event);
    }

    private void checkItemTag(NBTCompound tag, PacketReceiveEvent event) {
        NBTString id = tag.getStringTagOrNull("id");

        if (id != null) {
            String value = id.getValue();
            if (value.equalsIgnoreCase("minecraft:air")
                || value.equalsIgnoreCase("minecraft:bundle")) {

                dispatch(event, ViolationDocument.builder()
                    .description("send invalid item-stack id")
                    .mitigationStrategy(MitigationStrategy.MITIGATE)
                    .debugs(Collections.singletonList(new Debug<>("Id", value)))
                    .build());
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

                    dispatch(event, ViolationDocument.builder()
                        .description("send invalid projectile tag")
                        .mitigationStrategy(MitigationStrategy.MITIGATE)
                        .debugs(Collections.singletonList(new Debug<>("Tag", "empty")))
                        .build());
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
                if (!SierraDataManager.skipModelCheck) {
                    dispatch(event, ViolationDocument.builder()
                        .description("send invalid custom-model data")
                        .mitigationStrategy(MitigationStrategy.MITIGATE)
                        .debugs(Collections.singletonList(new Debug<>("Data", asInt)))
                        .build());
                }
            }
        }
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
            dispatch(event, ViolationDocument.builder()
                .description("send invalid slot packet")
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .debugs(Arrays.asList(new Debug<>("Slot", slot), new Debug<>("Max", max)))
                .build());
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

            dispatch(event, ViolationDocument.builder()
                .description("send invalid button click position")
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .debugs(Arrays.asList(new Debug<>("ClickType", clickType), new Debug<>("Button", button),
                                      new Debug<>("Container", (wrapper.getWindowId() == containerId
                                          ? containerType
                                          : "Empty"))
                ))
                .build());
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

        if (itemStack == null || itemStack.getNBT() == null) return;

        if (isShulkerBox(itemStack)) {
            String string = FormatUtils.mapToString(itemStack.getNBT().getTags());
            int    length = string.getBytes(StandardCharsets.UTF_8).length;
            if (length > 10000) {

                dispatch(event, ViolationDocument.builder()
                    .description("send to big shulker box")
                    .mitigationStrategy(MitigationStrategy.KICK)
                    .debugs(Arrays.asList(new Debug<>("Size", length), new Debug<>("Max", 10000)))
                    .build());
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
            dispatch(event, ViolationDocument.builder()
                .description("send to big container")
                .mitigationStrategy(MitigationStrategy.KICK)
                .debugs(Arrays.asList(
                    new Debug<>("Size", string.getBytes(StandardCharsets.UTF_8).length),
                    new Debug<>("Max", MAX_BYTE_SIZE)
                ))
                .build());
        }
        if (string.contains(WURSTCLIENT_URL)) {
            dispatch(event, ViolationDocument.builder()
                .description("send invalid container")
                .mitigationStrategy(MitigationStrategy.BAN)
                .debugs(Collections.singletonList(new Debug<>("Tag", "WurstClient")))
                .build());
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
                dispatch(event, ViolationDocument.builder()
                    .description("send invalid nbt list size")
                    .mitigationStrategy(MitigationStrategy.KICK)
                    .debugs(Arrays.asList(new Debug<>("Size", tagOrNull.getTags().size()), new Debug<>("Max", 50)))
                    .build());
            }
            for (NBTCompound tag : tagOrNull.getTags()) {
                if (tag == null || tag.toString().equalsIgnoreCase("null") || tag.toString().length() > 900) {

                    dispatch(event, ViolationDocument.builder()
                        .description("send invalid nbt list")
                        .mitigationStrategy(MitigationStrategy.KICK)
                        .debugs(Arrays.asList(
                            new Debug<>("Size", tagOrNull.getTags().size()),
                            new Debug<>("Tag", "Null/Length")
                        ))
                        .build());
                }
            }
        }
        if (listContent.incrementAndGet() > 10) {
            dispatch(event, ViolationDocument.builder()
                .description("send too many invalid nbt list")
                .mitigationStrategy(MitigationStrategy.KICK)
                .debugs(Collections.singletonList(new Debug<>("Content", listContent.get())))
                .build());
        }
    }

    private void checkIntArray(String key, PacketReceiveEvent event, ItemStack itemStack) {
        //noinspection DataFlowIssue
        NBTList<NBTIntArray> tagListOfTypeOrNull = itemStack.getNBT().getTagListOfTypeOrNull(key, NBTIntArray.class);
        if (tagListOfTypeOrNull != null) {
            if (tagListOfTypeOrNull.size() > 50) {
                dispatch(event, ViolationDocument.builder()
                    .mitigationStrategy(MitigationStrategy.KICK)
                    .description("send invalid int array")
                    .debugs(Collections.singletonList(new Debug<>("Tag", "Size")))
                    .build());
            }
            for (NBTIntArray tag : tagListOfTypeOrNull.getTags()) {
                if (tag.getValue().length > 150) {
                    dispatch(event, ViolationDocument.builder()
                        .mitigationStrategy(MitigationStrategy.KICK)
                        .description("send invalid int array")
                        .debugs(Collections.singletonList(new Debug<>("Tag", "Length")))
                        .build());
                }
                for (int i : tag.getValue()) {
                    if (i == Integer.MAX_VALUE || i == Integer.MIN_VALUE) {
                        dispatch(event, ViolationDocument.builder()
                            .mitigationStrategy(MitigationStrategy.KICK)
                            .description("send invalid int array")
                            .debugs(Collections.singletonList(new Debug<>("Tag", "MAX")))
                            .build());
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
                dispatch(event, ViolationDocument.builder()
                    .mitigationStrategy(MitigationStrategy.KICK)
                    .description("send invalid long array")
                    .debugs(Collections.singletonList(new Debug<>("Tag", "Size")))
                    .build());
            }
            for (NBTLongArray tag : tagListOfTypeOrNull.getTags()) {
                if (tag.getValue().length > 150) {
                    dispatch(event, ViolationDocument.builder()
                        .mitigationStrategy(MitigationStrategy.KICK)
                        .description("send invalid long array")
                        .debugs(Collections.singletonList(new Debug<>("Tag", "Length")))
                        .build());
                }
                for (long i : tag.getValue()) {
                    if (i == Long.MAX_VALUE || i == Long.MIN_VALUE) {
                        dispatch(event, ViolationDocument.builder()
                            .mitigationStrategy(MitigationStrategy.KICK)
                            .description("send invalid long array")
                            .debugs(Collections.singletonList(new Debug<>("Tag", "Max")))
                            .build());
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
                dispatch(event, ViolationDocument.builder()
                    .mitigationStrategy(MitigationStrategy.KICK)
                    .description("send invalid byte array")
                    .debugs(Collections.singletonList(new Debug<>("Tag", "Size")))
                    .build());
            }
            for (NBTByteArray tag : tagListOfTypeOrNull.getTags()) {
                if (tag.getValue().length > 150) {
                    dispatch(event, ViolationDocument.builder()
                        .mitigationStrategy(MitigationStrategy.KICK)
                        .description("send invalid byte array")
                        .debugs(Collections.singletonList(new Debug<>("Tag", "Length")))
                        .build());
                }
                for (byte i : tag.getValue()) {
                    if (i == Byte.MAX_VALUE || i == Byte.MIN_VALUE) {
                        dispatch(event, ViolationDocument.builder()
                            .mitigationStrategy(MitigationStrategy.KICK)
                            .description("send invalid byte array")
                            .debugs(Collections.singletonList(new Debug<>("Tag", "Max")))
                            .build());
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
            dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .description("send invalid armor stand name")
                .debugs(Collections.singletonList(new Debug<>("Length", customName.getValue().length())))
                .build());
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
                    dispatch(event, ViolationDocument.builder()
                        .mitigationStrategy(MitigationStrategy.KICK)
                        .description("send invalid armor stand rotation")
                        .debugs(Collections.singletonList(new Debug<>("Rotation", armorStandRotation)))
                        .build());
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
                        dispatch(event, ViolationDocument.builder()
                            .mitigationStrategy(MitigationStrategy.KICK)
                            .description("send invalid skull name")
                            .debugs(Collections.singletonList(new Debug<>("Name", name)))
                            .build());
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
                    dispatch(event, ViolationDocument.builder()
                        .mitigationStrategy(MitigationStrategy.KICK)
                        .description("send invalid armor stand limb rotation")
                        .debugs(Arrays.asList(new Debug<>("Rotation", value), new Debug<>("Limb", limb)))
                        .build());
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
            dispatch(event, ViolationDocument.builder()
                .description("send invalid banner layers")
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .debugs(Arrays.asList(new Debug<>("Size", tags.size()), new Debug<>("Max", MAX_BANNER_LAYERS)))
                .build());
            return;
        }
        for (NBTCompound tag : tags) {
            validatePattern(event, tag.getStringTagOrNull("Pattern"));
            validateColor(event, tag.getNumberTagOrNull("Color"));
        }
    }

    private void validatePattern(PacketReceiveEvent event, NBTString pattern) {
        if (pattern == null || pattern.getValue() == null) {
            dispatch(event, ViolationDocument.builder()
                .description("send invalid banner pattern")
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .debugs(Collections.singletonList(new Debug<>("Tag", "Null")))
                .build());
            return;
        }
        if (pattern.getValue().length() > MAX_PATTERN_LENGTH) {
            dispatch(event, ViolationDocument.builder()
                .description("send invalid banner pattern length")
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .debugs(Arrays.asList(
                    new Debug<>("Length", pattern.getValue().length()),
                    new Debug<>("Max", MAX_PATTERN_LENGTH)
                ))
                .build());
        }
    }

    private void validateColor(PacketReceiveEvent event, NBTNumber color) {
        if (color == null) {
            dispatch(event, ViolationDocument.builder()
                .description("send invalid banner color")
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .debugs(Collections.singletonList(
                    new Debug<>("Tag", "null")
                ))
                .build());
            return;
        }
        try {
            int rgb = color.getAsInt();
            if (rgb < MIN_VALID_COLOR || rgb > MAX_VALID_COLOR) {
                dispatch(event, ViolationDocument.builder()
                    .description("send invalid banner color")
                    .mitigationStrategy(MitigationStrategy.MITIGATE)
                    .debugs(Collections.singletonList(
                        new Debug<>("Color", rgb)
                    ))
                    .build());
            }
        } catch (Exception exception) {
            dispatch(event, ViolationDocument.builder()
                .description("send invalid banner color")
                .mitigationStrategy(MitigationStrategy.MITIGATE)
                .debugs(Collections.singletonList(
                    new Debug<>("Exception", exception.getMessage())
                ))
                .build());
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
        if (wrapper.getLevel() < 0 || (wrapper.getExperienceBar() < 0 && !Sierra.getPlugin()
            .getSierraConfigEngine()
            .config()
            .getBoolean("skip-negative-experience-check", false)) || wrapper.getTotalExperience() < 0) {

            dispatch(event, ViolationDocument.builder()
                .description("send invalid experience request")
                .mitigationStrategy(MitigationStrategy.KICK)
                .debugs(Arrays.asList(
                    new Debug<>("Level", wrapper.getLevel()),
                    new Debug<>("Experience Bar", wrapper.getExperienceBar()),
                    new Debug<>("Total", wrapper.getTotalExperience())
                ))
                .build());
        }
    }

    private void checkWindowItems(WrapperPlayServerWindowItems wrapper, PacketSendEvent event) {
        for (ItemStack item : wrapper.getItems()) {
            if (item.getNBT() == null) continue;
            checkAttributes(event, item);
        }
    }

    private void checkOpenWindow(WrapperPlayServerOpenWindow window) {

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

            dispatch(event, ViolationDocument.builder()
                .description("send invalid window click")
                .mitigationStrategy(MitigationStrategy.KICK)
                .debugs(Arrays.asList(new Debug<>("Button", button), new Debug<>("Window", windowId),
                                      new Debug<>("Slot", slot)
                ))
                .build());
        }
        if ((clickType == 1 || clickType == 2) && windowId >= 0 && button < 0) {
            dispatch(event, ViolationDocument.builder()
                .description("send invalid window click")
                .mitigationStrategy(MitigationStrategy.KICK)
                .debugs(Arrays.asList(
                    new Debug<>("Button", button),
                    new Debug<>("Window", windowId),
                    new Debug<>("ClickType", clickType),
                    new Debug<>("Slot", slot)
                ))
                .build());
        } else if (windowId >= 0 && clickType == 2 && slot < 0) {
            dispatch(event, ViolationDocument.builder()
                .description("send invalid window click")
                .mitigationStrategy(MitigationStrategy.KICK)
                .debugs(Arrays.asList(
                    new Debug<>("Button", button),
                    new Debug<>("Window", windowId),
                    new Debug<>("ClickType", clickType),
                    new Debug<>("Slot", slot)
                ))
                .build());
        }
    }
}
