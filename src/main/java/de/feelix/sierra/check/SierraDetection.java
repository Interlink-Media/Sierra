package de.feelix.sierra.check;

import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.events.EventManager;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.config.SierraConfigEngine;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.impl.SierraCheck;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.events.AsyncUserDetectionEvent;
import de.feelix.sierraapi.violation.PunishType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The SierraDetection class is used to detect violations in player data.
 */
@Getter
public class SierraDetection implements SierraCheck {

    /**
     * The `playerData` variable represents the player's data associated with the `SierraDetection` class.
     */
    private final PlayerData playerData;

    /**
     * The rawCheckType variable represents the type of check being performed.
     * It is an instance of the CheckType enumeration, which contains different types of checks.
     * Each check type has a unique identifier and a friendly name that describes its purpose or function.
     * <p>
     * The rawCheckType variable is declared as private and final, meaning its value cannot be modified once set.
     * It is a constant that determines the type of check being performed by the SierraDetection class.
     * <p>
     * For example, a rawCheckType value of CheckType.SPAM represents a check for packet spam,
     * and a value of CheckType.SIGN represents a check for sign crashing.
     */
    private final CheckType rawCheckType;

    /**
     * The friendlyName variable is a private field of type String.
     * It represents the friendly name associated with a player.
     * The friendly name is a user-friendly identifier that can be used
     * to refer to the player in a more informal and approachable manner.
     * Examples of friendly names can include nicknames, aliases, or abbreviated names.
     * <p>
     * Please note that the friendlyName field is used in the SierraDetection class,
     * which is responsible for detecting violations in player data.
     * The friendlyName field is accessed by various methods in the SierraDetection class
     * to handle violation events, log information to the console, and alert staff members about violations.
     * <p>
     * Example usage:
     * ```java
     * SierraDetection sierraDetection = new SierraDetection();
     * sierraDetection.setFriendlyName("JohnDoe");
     * String friendlyName = sierraDetection.getFriendlyName();
     * System.out.println("Hello, " + friendlyName + "!");
     * ```
     *
     * @see SierraDetection
     */
    private String friendlyName;

    /**
     * The ID of the check used to detect violations in player data.
     */
    private int checkId;

    /**
     * Represents the number of violations found by a check for violations in player data.
     */
    private int violations = 0;

    /**
     * The SierraDetection class is used to detect violations in player data.
     *
     * @param playerData The PlayerData object containing the player's data
     */
    public SierraDetection(PlayerData playerData) {
        // Initialize member variables
        this.playerData = playerData;
        this.rawCheckType = initializeCheckType();
        if (this.rawCheckType != null) {
            this.friendlyName = rawCheckType.getFriendlyName();
            this.checkId = rawCheckType.getId();
        }
    }

    /**
     * Initializes the CheckType from an annotation.
     *
     * @return The initialized CheckType or null if the class does not have the SierraCheckData annotation.
     */
    // Private method to initialize CheckType from annotation
    private CheckType initializeCheckType() {
        // Check if the class has the SierraCheckData annotation
        if (this.getClass().isAnnotationPresent(SierraCheckData.class)) {
            // Retrieve annotation data and return the CheckType
            SierraCheckData checkData = this.getClass().getAnnotation(SierraCheckData.class);
            return checkData.checkType();
        }
        return null;
    }

    /**
     * Handle violation event.
     *
     * @param event             The ProtocolPacketEvent that triggered the violation
     * @param violationDocument The ViolationDocument containing information about the violation
     */
    // Handle violation event
    public void violation(ProtocolPacketEvent<Object> event, ViolationDocument violationDocument) {

        // Cancel the packet event
        event.setCancelled(true);

        if(playerData.isBlocked()) return;

        // Safety check for player data in violation document
        if (violationDocument.getPlayerData() == null) violationDocument.setPlayerData(playerData);

        if (violationDocument.getPunishType() == null) violationDocument.setPunishType(PunishType.MITIGATE);

        if (violationDocument.getDebugInformation() == null || violationDocument.getDebugInformation().isEmpty()) {
            violationDocument.setDebugInformation("No debug available");
        }

        // Update violation count
        this.violations++;

        // Asynchronously call user detection event
        throwDetectionEvent(violationDocument);

        // Log to console, alert staff, create history, and potentially punish
        User user = event.getUser();
        consoleLog(user, violationDocument);
        alert(user, violationDocument.getDebugInformation(), violationDocument.punishType());

        if (violationDocument.punishType() != PunishType.MITIGATE) {
            Sierra.getPlugin()
                .getDataManager()
                .createPunishmentHistory(
                    playerData.username(), violationDocument.punishType(), playerData.getPingProcessor().getPing(),
                    violationDocument.debugInformation()
                );

            playerData.punish(violationDocument.punishType());
        }
    }

    /**
     * Throws a detection event asynchronously.
     *
     * @param violationDocument The ViolationDocument containing information about the violation
     */
    private void throwDetectionEvent(ViolationDocument violationDocument) {
        Bukkit.getScheduler().runTaskAsynchronously(
            Sierra.getPlugin(),
            () -> EventManager.callEvent(
                new AsyncUserDetectionEvent(violationDocument, playerData, checkType(), this.violations))
        );
    }

    /**
     * Log information to console.
     *
     * @param user              The User object representing the player
     * @param violationDocument The ViolationDocument containing information about the violation
     */
    // Log information to console
    protected void consoleLog(User user, ViolationDocument violationDocument) {
        String generalMessage = "Player " + user.getName() + " was prevented from sending an invalid packet";
        String generalInformation = String.format(
            "Debug information: %s", violationDocument.getDebugInformation().isEmpty()
                ? "No debug available" : FormatUtils.shortenString(violationDocument.getDebugInformation()));
        String generalCheck = String.format("Check Information: %s/%d - VL: %d",
                                            this.friendlyName, this.checkId, this.violations
        );

        Logger logger = Sierra.getPlugin().getLogger();

        logger.log(Level.INFO, generalMessage);
        logger.log(Level.INFO, generalInformation);
        logger.log(Level.INFO, generalCheck);
    }

    /**
     * Alert staff members about the violation.
     *
     * @param user       The User object representing the player
     * @param details    Additional details about the violation
     * @param punishType The type of punishment to be applied (MITIGATE, KICK, or BAN)
     */
    // Alert staff members about the violation
    protected void alert(User user, String details, PunishType punishType) {

        SierraConfigEngine sierraConfig = Sierra.getPlugin().getSierraConfigEngine();

        // Format and send staff alert message to online users
        String staffAlert = sierraConfig
            .config()
            .getString(
                "layout.detection-message.staff-alert",
                "{prefix} &c{username} &8┃ &f{mitigation} &c{checkname} &8┃ &cx{violations}"
            );

        staffAlert = staffAlert.replace("{prefix}", Sierra.PREFIX);
        staffAlert = staffAlert.replace("{username}", user.getName());
        staffAlert = staffAlert.replace("{mitigation}", punishType.friendlyMessage());
        staffAlert = staffAlert.replace("{checkname}", this.friendlyName);
        staffAlert = staffAlert.replace("{violations}", String.valueOf(violations));
        staffAlert = ChatColor.translateAlternateColorCodes('&', staffAlert);

        String username = this.playerData.getUser().getName();

        String clientVersion = this.playerData.getUser().getClientVersion().getReleaseName();
        int ticks = FormatUtils.convertMillisToTicks(
            System.currentTimeMillis() - this.playerData.getJoinTime());

        String content =
            " §7Username: §c" + username + "\n" +
            " §7Version: §c" + clientVersion + "\n" +
            " §7Brand: §c" + playerData.getBrand() + "\n" +
            " §7Exist since: §c" + ticks + " ticks\n" +
            " §7Game mode: §c" + this.playerData.getGameMode().name() + "\n" +
            " §7Tag: §c" + this.friendlyName.toLowerCase() + "\n" +
            " §7Debug info: §c" + FormatUtils.shortenString(details) + "\n"
            + "\n"
            + ChatColor.translateAlternateColorCodes('&', sierraConfig.config()
                .getString("layout.detection-message.alert-command-note", "&fClick to teleport"));

        String command = sierraConfig.config().getString(
            "layout.detection-message.alert-command",
            "/tp {username}"
        );
        command = command.replace("{username}", username);

        for (PlayerData playerData : Sierra.getPlugin().getDataManager().getPlayerData().values()) {
            if (playerData.isReceiveAlerts()) {
                playerData.getUser().sendMessage(
                    LegacyComponentSerializer.legacy('&')
                        .deserialize(staffAlert)
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .hoverEvent(HoverEvent.showText(Component.text(content))));
            }
        }
    }

    /**
     * Returns the number of violations detected.
     *
     * @return The number of violations detected.
     */
    // Implement interface method for violation count
    @Override
    public double violations() {
        return this.violations;
    }

    /**
     * Retrieves the check type of the current instance.
     *
     * @return The check type of the instance.
     */
    // Implement interface method for check type
    @Override
    public CheckType checkType() {
        return this.rawCheckType;
    }
}
