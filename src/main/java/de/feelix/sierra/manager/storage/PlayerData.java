package de.feelix.sierra.manager.storage;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.Location;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.CheckManager;
import de.feelix.sierra.manager.storage.alert.AbstractAlertSetting;
import de.feelix.sierra.manager.storage.logger.SierraLogger;
import de.feelix.sierra.manager.storage.processor.*;
import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierra.utilities.message.ConfigValue;
import de.feelix.sierraapi.timing.TimingHandler;
import de.feelix.sierraapi.user.settings.AlertSettings;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import io.github.retrooper.packetevents.util.viaversion.ViaVersionUtil;
import lombok.Data;
import de.feelix.sierraapi.check.CheckRepository;
import de.feelix.sierraapi.user.impl.SierraUser;
import de.feelix.sierraapi.violation.MitigationStrategy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


@Data
public class PlayerData implements SierraUser {

    private Object bukkitPlayer;
    private User user;
    private GameMode gameMode;
    private Location lastLocation;
    private ClientVersion clientVersion;

    private String brand = "vanilla";
    private String locale = "unset";
    private final Set<String> channels = new HashSet<>();
    private final long joinTime = System.currentTimeMillis();

    private boolean receivedPunishment = false;
    private boolean exempt = false;
    private boolean nameChecked = false;
    private boolean bypassPermission = false;

    private double bytesSent = 0;

    private final AlertSettings alertSettings = new AbstractAlertSetting();
    private final AlertSettings mitigationSettings = new AbstractAlertSetting();

    private SierraLogger sierraLogger;
    private final CheckManager checkManager = new CheckManager(this);
    private final BrandProcessor brandProcessor = new BrandProcessor(this);
    private final GameModeProcessor gameModeProcessor = new GameModeProcessor(this);
    private final PingProcessor pingProcessor = new PingProcessor(this);
    private final TeleportProcessor teleportProcessor = new TeleportProcessor(this);
    private final TransactionProcessor transactionProcessor = new TransactionProcessor(this);
    private final TimingHandler timingProcessor = new TimingProcessor(this);

    public PlayerData(User user) {
        this.user = user;
        this.clientVersion = user.getClientVersion();
        this.sierraLogger = new SierraLogger("INVALID");
    }

    public void pollData(Player bukkitPlayer) {
        this.bukkitPlayer = bukkitPlayer;
        bypassPermission = bukkitPlayer.hasPermission("sierra.bypass");
        if (this.sierraLogger.getPlayerName().equalsIgnoreCase("INVALID")) {
            this.sierraLogger.close();
            sierraLogger = new SierraLogger(bukkitPlayer.getName());
        }
        sendTransaction();
    }

    public ClientVersion getClientVersion() {
        // Use ViaVersion cause its an early injector
        if (ViaVersionUtil.getViaVersionAccessor() != null) {
            this.clientVersion = ClientVersion.getById(ViaVersionUtil.getViaVersionAccessor().getProtocolVersion(user));
            return this.clientVersion;
        }

        if (this.clientVersion == null) {
            // First use players own sent client version
            this.clientVersion= user.getClientVersion();

            // If players version is still null use server version as client version
            if (this.clientVersion == null) {
                this.clientVersion = ClientVersion.getById(
                    PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion());
            }
        }
        return this.clientVersion;
    }

    public void sendTransaction() {
        this.transactionProcessor.sendTransaction(true);
    }

    public void exceptionDisconnect(Exception exception) {

        // To prevent future console spam cause thread is not fast enough
        if (receivedPunishment) return;

        Sierra.getPlugin().getLogger().warning("We disconnect " + this.username() + " for security purpose");
        Sierra.getPlugin().getLogger().warning("Exception: " + exception.getMessage());
        punish(MitigationStrategy.KICK);
    }

    public void punish(MitigationStrategy mitigationStrategy) {
        setReceivedPunishment(true);
        if (mitigationStrategy == MitigationStrategy.BAN && Sierra.getPlugin().getPunishmentConfig().isBan()) {
            ban();
        }
        kick();
    }

    private void ban() {
        SierraDataManager.increaseBanValue();
        FoliaScheduler.getGlobalRegionScheduler().run(Sierra.getPlugin(), o -> Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(),
            new ConfigValue("punish-command", "ban {username} Crashing", false)
                .replace("{username}", this.user.getName())
                .message()
        ));
    }

    public void addRealTimeTask(int transaction, Runnable runnable) {
        transactionProcessor.addRealTimeTask(transaction, false, runnable);
    }

    public int getTicksExisted() {
        return FormatUtils.convertMillisToTicks(System.currentTimeMillis() - this.joinTime);
    }

    public boolean hasBypassPermission() {
        return this.bypassPermission;
    }

    public void cancelEvent(ProtocolPacketEvent event) {
        event.cleanUp();
        event.setCancelled(true);
    }

    @Override
    public String username() {
        return user != null ? user.getName() : "Undefined";
    }

    @Override
    public String brand() {
        return brand;
    }

    @Override
    public String locale() {
        return locale;
    }

    @Override
    public int entityId() {
        return user != null ? user.getEntityId() : -1;
    }

    @Override
    public int ping() {
        return (int) this.getPingProcessor().getPing();
    }

    @Override
    public int ticksExisted() {
        return this.getTicksExisted();
    }

    @Override
    public UUID uuid() {
        return user != null ? user.getUUID() : UUID.randomUUID();
    }

    @Override
    public long existSince() {
        return System.currentTimeMillis() - joinTime;
    }

    @Override
    public String version() {
        return getClientVersion().name().replace("V_", "").replace("_", ".");
    }

    @Override
    public boolean kick() {
        if (user != null) {
            SierraDataManager.increaseKickValue();
            user.closeConnection();
            return true;
        }
        return false;
    }

    @Override
    public boolean isExempt() {
        return this.exempt;
    }

    @Override
    public GameMode gameMode() {
        return this.getGameMode();
    }

    @Override
    public boolean setExempt(boolean b) {
        this.exempt = b;
        return this.exempt;
    }

    @Override
    public AlertSettings alertSettings() {
        return alertSettings;
    }

    @Override
    public AlertSettings mitigationSettings() {
        return mitigationSettings;
    }

    @Override
    public CheckRepository checkRepository() {
        return this.checkManager;
    }

    @Override
    public TimingHandler timingHandler() {
        return this.getTimingProcessor();
    }
}
