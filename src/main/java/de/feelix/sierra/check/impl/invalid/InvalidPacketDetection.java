package de.feelix.sierra.check.impl.invalid;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
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
import de.feelix.sierra.utilities.FieldReader;
import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierra.utilities.NBTDetector;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.PunishType;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

@SierraCheckData(checkType = CheckType.INVALID)
public class InvalidPacketDetection extends SierraDetection implements IngoingProcessor, OutgoingProcessor {

    public InvalidPacketDetection(PlayerData playerData) {
        super(playerData);
    }

    // https://github.com/PaperMC/Paper/commit/ea2c81e4b9232447f9896af2aac4cd0bf62386fd
    // https://wiki.vg/Inventory
    // https://github.com/GrimAnticheat/Grim/blob/2.0/src/main/java/ac/grim/grimac/checks/impl/crash/CrashD.java
    private MenuType type      = MenuType.UNKNOWN;
    private int      lecternId = -1;

    private int  lastSlot      = -1;
    private long lastBookUse   = 0L;
    private int  containerType = -1;
    private int  containerId   = -1;

    private final Queue<Pair<Long, Long>> keepAliveMap = new LinkedList<>();

    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {

        if (!Sierra.getPlugin()
            .getSierraConfigEngine()
            .config()
            .getBoolean("prevent-invalid-packet", true)) {
            return;
        }

        if (event.getConnectionState() != ConnectionState.PLAY) return;

        if (playerData.clientVersion == null) {
            playerData.clientVersion = event.getUser().getClientVersion();
        }

        //https://netty.io/4.1/api/io/netty/buffer/ByteBuf.html//Sequential Access Indexing
        int capacity = ByteBufHelper.capacity(event.getByteBuf());
        int maxBytes = 64000 * (playerData.clientVersion.isOlderThan(ClientVersion.V_1_8) ? 2 : 1);
        if (capacity > maxBytes) {
            violation(event, ViolationDocument.builder()
                .debugInformation("Bytes: " + capacity + " Max Bytes: " + maxBytes)
                .punishType(PunishType.KICK)
                .build());
        }
        //https://netty.io/4.1/api/io/netty/buffer/ByteBuf.html//Sequential Access Indexing
        int readableBytes     = ByteBufHelper.readableBytes(event.getByteBuf());
        int maxBytesPerSecond = 64000 * (playerData.clientVersion.isOlderThan(ClientVersion.V_1_8) ? 2 : 1);
        if ((playerData.bytesSent += readableBytes) > maxBytesPerSecond) {
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

        } else if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {

            WrapperPlayClientCreativeInventoryAction wrapper   = new WrapperPlayClientCreativeInventoryAction(event);
            ItemStack                                itemStack = wrapper.getItemStack();
            checkForInvalidBanner(event, itemStack);
            checkIfItemIsAvailable(event, itemStack);
            checkForInvalidContainer(event, itemStack);
            checkForInvalidShulker(event, itemStack);
            checkNbtTags(event, itemStack);

        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {

            WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);

            wrapper.getItemStack().ifPresent(itemStack -> {
                if (itemStack.getType() == ItemTypes.WRITABLE_BOOK
                    || itemStack.getType() == ItemTypes.WRITTEN_BOOK
                    || itemStack.getType() == ItemTypes.BOOK) {
                    this.lastBookUse = System.currentTimeMillis();
                }
            });

        } else if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {

            WrapperPlayClientEntityAction wrapper = new WrapperPlayClientEntityAction(event);
            if (wrapper.getEntityId() < 0) {
                violation(event, ViolationDocument.builder()
                    .punishType(PunishType.BAN)
                    .debugInformation("Invalid entity action: " + wrapper.getEntityId())
                    .build());
            }

            if (wrapper.getJumpBoost() < 0 || wrapper.getJumpBoost() > 100 || wrapper.getEntityId() != event.getUser()
                .getEntityId()
                || (wrapper.getAction() != WrapperPlayClientEntityAction.Action.START_JUMPING_WITH_HORSE
                    && wrapper.getJumpBoost() != 0)) {

                violation(event, ViolationDocument.builder()
                    .debugInformation(
                        "boost=" + wrapper.getJumpBoost() + ", action=" + wrapper.getAction() + ", entity="
                        + wrapper.getEntityId())
                    .punishType(PunishType.KICK)
                    .build());
            }

        } else if (event.getPacketType() == PacketType.Play.Client.CLIENT_SETTINGS) {

            WrapperPlayClientSettings wrapper = new WrapperPlayClientSettings(event);
            if (wrapper.getLocale() == null) {
                violation(event, ViolationDocument.builder()
                    .punishType(PunishType.KICK)
                    .debugInformation("Locale is null in settings")
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

            if(payload.isEmpty()) {
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
                checkIfItemIsAvailable(event, itemStack);
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

        } else if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
            WrapperPlayClientKeepAlive wrapper = new WrapperPlayClientKeepAlive(event);

            if (wrapper.getId() < 0) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Invalid id: " + wrapper.getId())
                    .punishType(PunishType.BAN)
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

            if (isSupportedVersion()) {
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
            checkIfItemIsAvailable(event, carriedItemStack);
            checkForInvalidContainer(event, carriedItemStack);
            checkForInvalidShulker(event, carriedItemStack);
            checkForInvalidBanner(event, carriedItemStack);
            checkNbtTags(event, carriedItemStack);

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

    private void checkIfItemIsAvailable(PacketReceiveEvent event, ItemStack itemStack) {
        ItemType itemStackType = itemStack.getType();

        long millis = System.currentTimeMillis();

        Player player = (Player) event.getPlayer();

        AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }

        for (org.bukkit.inventory.ItemStack content : player.getInventory().getContents()) {
            if (SpigotConversionUtil.fromBukkitItemStack(content).getType() == itemStackType) {
                atomicBoolean.set(true);
                break;
            }
        }

        for (org.bukkit.inventory.ItemStack content : player.getOpenInventory().getTopInventory()) {
            if (SpigotConversionUtil.fromBukkitItemStack(content).getType() == itemStackType) {
                atomicBoolean.set(true);
                break;
            }
        }
        for (org.bukkit.inventory.ItemStack content : player.getOpenInventory().getBottomInventory()) {
            if (SpigotConversionUtil.fromBukkitItemStack(content).getType() == itemStackType) {
                atomicBoolean.set(true);
                break;
            }
        }


        if (!atomicBoolean.get()) {

            long delay = System.currentTimeMillis() - millis;

            String format = "Interacted with: %s, but its not in his inventory. (Took: %dms)";

            Sierra.getPlugin().getLogger().log(Level.INFO, String.format(format, itemStackType.getName(), delay));

            String msg = "This is an experimental check."
                         + " If there are any errors with this, please report them on the Discord";
            Sierra.getPlugin().getLogger().log(Level.INFO, msg);

            violation(event, ViolationDocument.builder()
                .debugInformation("Interacted with: " + itemStackType.getName() + ", took: " + delay + "ms")
                .punishType(PunishType.KICK)
                .build());
        }
    }

    private void checkForInvalidShulker(PacketReceiveEvent event, ItemStack itemStack) {
        if (itemStack.getType() == ItemTypes.BLACK_SHULKER_BOX
            || itemStack.getType() == ItemTypes.WHITE_SHULKER_BOX
            || itemStack.getType() == ItemTypes.RED_SHULKER_BOX
            || itemStack.getType() == ItemTypes.GREEN_SHULKER_BOX
            || itemStack.getType() == ItemTypes.BLUE_SHULKER_BOX
            || itemStack.getType() == ItemTypes.LIGHT_BLUE_SHULKER_BOX
            || itemStack.getType() == ItemTypes.YELLOW_SHULKER_BOX
            || itemStack.getType() == ItemTypes.LIME_SHULKER_BOX
            || itemStack.getType() == ItemTypes.BROWN_SHULKER_BOX
            || itemStack.getType() == ItemTypes.PINK_SHULKER_BOX
            || itemStack.getType() == ItemTypes.ORANGE_SHULKER_BOX
            || itemStack.getType() == ItemTypes.GRAY_SHULKER_BOX
            || itemStack.getType() == ItemTypes.LIGHT_GRAY_SHULKER_BOX
            || itemStack.getType() == ItemTypes.CYAN_SHULKER_BOX
            || itemStack.getType() == ItemTypes.PURPLE_SHULKER_BOX
            || itemStack.getType() == ItemTypes.MAGENTA_SHULKER_BOX) {

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

    private void checkForInvalidContainer(PacketReceiveEvent event, ItemStack itemStack) {
        if (itemStack.getType() == ItemTypes.CHEST
            || itemStack.getType() == ItemTypes.HOPPER
            || itemStack.getType() == ItemTypes.HOPPER_MINECART
            || itemStack.getType() == ItemTypes.CHEST_MINECART) {

            if (itemStack.getNBT() != null) {
                String string = FormatUtils.mapToString(itemStack.getNBT().getTags());
                if (string.getBytes(StandardCharsets.UTF_8).length > 262144) {
                    violation(event, ViolationDocument.builder()
                        .debugInformation("Invalid container size")
                        .punishType(PunishType.KICK)
                        .build());
                }
                if (string.contains("www.wurstclient.net")) {
                    violation(event, ViolationDocument.builder()
                        .debugInformation("Wurstclient container")
                        .punishType(PunishType.BAN)
                        .build());
                }
            }
        }
    }

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

    private void checkForInvalidBanner(PacketReceiveEvent event, ItemStack itemStack) {

        if (itemStack.getType() == ItemTypes.BLACK_BANNER || itemStack.getType() == ItemTypes.WHITE_BANNER
            || itemStack.getType() == ItemTypes.RED_BANNER || itemStack.getType() == ItemTypes.GREEN_BANNER
            || itemStack.getType() == ItemTypes.BLUE_BANNER || itemStack.getType() == ItemTypes.LIGHT_BLUE_BANNER
            || itemStack.getType() == ItemTypes.YELLOW_BANNER || itemStack.getType() == ItemTypes.LIME_BANNER
            || itemStack.getType() == ItemTypes.BROWN_BANNER || itemStack.getType() == ItemTypes.PINK_BANNER
            || itemStack.getType() == ItemTypes.ORANGE_BANNER || itemStack.getType() == ItemTypes.GRAY_BANNER
            || itemStack.getType() == ItemTypes.LIGHT_GRAY_BANNER || itemStack.getType() == ItemTypes.CYAN_BANNER
            || itemStack.getType() == ItemTypes.PURPLE_BANNER || itemStack.getType() == ItemTypes.MAGENTA_BANNER) {

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
        } else if (event.getPacketType() == PacketType.Play.Server.KEEP_ALIVE) {

            WrapperPlayServerKeepAlive packet = new WrapperPlayServerKeepAlive(event);
            keepAliveMap.add(new Pair<>(packet.getId(), System.nanoTime()));

        } else if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {

            WrapperPlayServerOpenWindow window = new WrapperPlayServerOpenWindow(event);

            if (isSupportedVersion()) {
                this.type = MenuType.getMenuType(window.getType());
                if (type == MenuType.LECTERN) lecternId = window.getContainerId();
            }
            this.containerType = window.getType();
            this.containerId = window.getContainerId();
        }
    }

    private boolean isSupportedVersion() {
        return PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14);
    }

    private boolean isSupportedVersion(User user) {
        return user.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19) && PacketEvents.getAPI()
            .getServerManager()
            .getVersion()
            .isNewerThanOrEquals(
                ServerVersion.V_1_19);
    }
}
