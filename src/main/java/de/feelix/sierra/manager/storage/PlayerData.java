package de.feelix.sierra.manager.storage;

import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.CheckManager;
import lombok.Getter;
import lombok.Setter;
import de.feelix.sierraapi.check.CheckRepository;
import de.feelix.sierraapi.user.impl.SierraUser;
import de.feelix.sierraapi.violation.PunishType;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class PlayerData implements SierraUser {

    private       User        user;
    private final Set<String> channels = new HashSet<>();

    public int lastBookEditTick;
    public int lastDropItemTick;
    public int lastCraftRequestTick;
    public int dropCount;
    public int recursionCount;
    public int bytesSent;
    public int openWindowType;
    public int openWindowContainer;

    public double packetCount;
    public double packetAllowance = 1000;

    public long joinTime = System.currentTimeMillis();

    public  boolean blocked       = false;
    public  boolean receiveAlerts = false;
    private boolean exempt        = false;
    private boolean hasBrand      = false;

    private String brand = "vanilla";

    public       GameMode     gameMode;
    public final CheckManager checkManager = new CheckManager(this);


    /**
     * The PlayerData function is a constructor that takes in a User object and sets the user variable to it.
     *
     * @param user user Set the user field in this class
     */
    public PlayerData(User user) {
        this.user = user;
    }

    @Override
    public String username() {
        return user.getName();
    }

    @Override
    public int entityId() {
        return user.getEntityId();
    }

    @Override
    public UUID uuid() {
        return user.getUUID();
    }

    @Override
    public long existSince() {
        return System.currentTimeMillis() - joinTime;
    }

    @Override
    public String version() {
        return user.getClientVersion().name();
    }

    @Override
    public boolean kick() {
        if (user != null) {
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
    public boolean setExempt(boolean b) {
        this.exempt = b;
        return this.exempt;
    }

    @Override
    public boolean isAlerts() {
        return this.receiveAlerts;
    }

    @Override
    public boolean setAlerts(boolean b) {
        this.receiveAlerts = b;
        return this.receiveAlerts;
    }

    /**
     * The punish function is used to punish a player for crashing.
     *
     * @param punishType punishType Determine what type of punishment the player should receive
     */
    public void punish(PunishType punishType) {
        if (punishType == PunishType.BAN) {
            setBlocked(true);
            kick();
            if (Sierra.getPlugin().getPunishmentConfig().isBan()) {
                ban();
            }
        } else if (punishType == PunishType.KICK && Sierra.getPlugin().getPunishmentConfig().isKick()) {
            setBlocked(true);
            kick();
        }
    }

    private void ban() {
        Bukkit.getScheduler()
            .runTask(
                Sierra.getPlugin(), () -> Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    Sierra.getPlugin()
                        .getSierraConfigEngine()
                        .config()
                        .getString("punish-command", "ban {username} Crashing")
                        .replace("{username}", this.user.getName())
                ));
    }

    @Override
    public CheckRepository checkRepository() {
        return this.checkManager;
    }
}
