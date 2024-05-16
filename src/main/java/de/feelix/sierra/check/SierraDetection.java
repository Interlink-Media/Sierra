package de.feelix.sierra.check;

import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.config.SierraConfigEngine;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.impl.SierraCheck;
import io.github.retrooper.packetevents.util.FoliaCompatUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.events.impl.AsyncUserDetectionEvent;
import de.feelix.sierraapi.violation.PunishType;
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

        if (playerData.isReceivedPunishment()) return;

        // Update violation count
        this.violations++;

        // Asynchronously call user detection event
        throwDetectionEvent(violationDocument);

        // Log to console, alert staff, create history, and potentially punish
        User user = event.getUser();
        consoleLog(user, violationDocument);
        alert(user, violationDocument);

        if (violationDocument.punishType() != PunishType.MITIGATE) {
            Sierra.getPlugin()
                .getSierraDataManager()
                .createPunishmentHistory(
                    playerData.username(), violationDocument.punishType(), playerData.getPingProcessor().getPing(),
                    violationDocument.debugInformation()
                );

            Sierra.getPlugin()
                .getSierraDiscordGateway()
                .sendAlert(playerData, this.checkType(), violationDocument, this.violations());

            playerData.punish(violationDocument.punishType());
        }
    }

    /**
     * Throws a detection event asynchronously.
     *
     * @param violationDocument The ViolationDocument containing information about the violation
     */
    private void throwDetectionEvent(ViolationDocument violationDocument) {
        FoliaCompatUtil.runTaskAsync(
            Sierra.getPlugin(),
            () -> Sierra.getPlugin()
                .getEventBus()
                .publish(new AsyncUserDetectionEvent(violationDocument, playerData, checkType(), this.violations))
        );
    }

    /**
     * Logs a message to the console.
     *
     * @param user              The User object representing the player.
     * @param violationDocument The ViolationDocument containing information about the violation.
     */
    protected void consoleLog(User user, ViolationDocument violationDocument) {

        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("log-violation-to-console", true)) {
            return;
        }

        if (violationDocument.getPunishType() == PunishType.MITIGATE) return;

        logToConsole(createGeneralMessage(user, violationDocument.getPunishType()));
        logToConsole(createGeneralInformation(violationDocument));
        logToConsole(createGeneralCheck());
    }

    /**
     * Creates a general message for a given user and punish type.
     * The message is formatted as "Player [username] got [friendlyMessage] sending an invalid packet".
     *
     * @param user       The User object representing the player.
     * @param punishType The PunishType enum representing the type of punishment.
     * @return A string representing the general message.
     */
    private String createGeneralMessage(User user, PunishType punishType) {
        return "Player " + user.getName() + " got " + punishType.friendlyMessage() + " sending an invalid packet";
    }

    /**
     * Creates general information for a violation document.
     *
     * @param violationDocument The ViolationDocument object containing information about the violation.
     * @return A string representing the general information.
     */
    private String createGeneralInformation(ViolationDocument violationDocument) {
        return String.format(
            "Debug information: %s", violationDocument.getDebugInformation().isEmpty()
                ? "No debug available" : FormatUtils.shortenString(violationDocument.getDebugInformation()));
    }

    /**
     * Generates a general check message for a violation.
     *
     * @return A string representing the general check information.
     */
    private String createGeneralCheck() {
        return String.format("Check Information: %s/%d - VL: %d", this.friendlyName, this.checkId, this.violations);
    }

    /**
     * Logs a message to the console.
     *
     * @param message The message to be logged.
     */
    private void logToConsole(String message) {
        Logger logger = Sierra.getPlugin().getLogger();
        logger.log(Level.INFO, message);
    }


    /**
     * Sends an alert message to staff members with information about the violation.
     *
     * @param user              The User object representing the player.
     * @param violationDocument The ViolationDocument containing information about the violation.
     */
    protected void alert(User user, ViolationDocument violationDocument) {
        SierraConfigEngine sierraConfig = Sierra.getPlugin().getSierraConfigEngine();

        PunishType punishType = violationDocument.getPunishType();
        String     staffAlert = formatStaffAlertMessage(user, punishType, sierraConfig);
        String     username = this.playerData.getUser().getName();
        String     clientVersion = this.playerData.getUser().getClientVersion().getReleaseName();

        StringBuilder content = new StringBuilder()
            .append(" §7Username: §c")
            .append(username)
            .append("\n")
            .append(" §7Version: §c")
            .append(clientVersion)
            .append("\n")
            .append(" §7Brand: §c")
            .append(playerData.getBrand())
            .append("\n")
            .append(" §7Exist since: §c")
            .append(this.playerData.getTicksExisted())
            .append(" ticks\n")
            .append(" §7Game mode: §c")
            .append(this.playerData.getGameMode().name())
            .append("\n")
            .append(" §7Tag: §c")
            .append(this.friendlyName.toLowerCase())
            .append("\n")
            .append(" §7Debug info: §c")
            .append(FormatUtils.shortenString(violationDocument.getDebugInformation()))
            .append("\n")
            .append("\n")
            .append(ChatColor.translateAlternateColorCodes(
                '&',
                getAlertNote(sierraConfig)
            ));

        String command = getPunishmentCommand(sierraConfig, username);

        if (punishType == PunishType.MITIGATE) {
            for (PlayerData playerData : Sierra.getPlugin().getSierraDataManager().getPlayerData().values()) {
                if (playerData.getMitigationSettings().enabled()) {
                    playerData.getUser().sendMessage(
                        LegacyComponentSerializer.legacy('&')
                            .deserialize(staffAlert)
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, command))
                            .hoverEvent(HoverEvent.showText(Component.text(content.toString()))));
                }
            }
        } else {
            for (PlayerData playerData : Sierra.getPlugin().getSierraDataManager().getPlayerData().values()) {
                if (playerData.getAlertSettings().enabled()) {
                    playerData.getUser().sendMessage(
                        LegacyComponentSerializer.legacy('&')
                            .deserialize(staffAlert)
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, command))
                            .hoverEvent(HoverEvent.showText(Component.text(content.toString()))));
                }
            }
        }
    }

    /**
     * Retrieves the alert note from the Sierra configuration.
     *
     * @param sierraConfig The SierraConfigEngine object for accessing the configuration options.
     * @return The alert note from the configuration.
     */
    private String getAlertNote(SierraConfigEngine sierraConfig) {
        return sierraConfig.config().getString("layout.detection-message.alert-command-note", "&fClick to teleport");
    }

    /**
     * Retrieves the punishment command for a given user from the Sierra configuration.
     *
     * @param sierraConfig The SierraConfigEngine object for accessing the configuration options.
     * @param username     The username of the user.
     * @return The punishment command for the user.
     */
    private String getPunishmentCommand(SierraConfigEngine sierraConfig, String username) {
        return sierraConfig.config()
            .getString("layout.detection-message.alert-command", "/tp {username}")
            .replace("{username}", username);
    }

    /**
     * Formats the staff alert message with the given user, punish type, and SierraConfigEngine.
     *
     * @param user         The User object representing the player.
     * @param punishType   The PunishType enum representing the type of punishment.
     * @param sierraConfig The SierraConfigEngine object for accessing configuration options.
     * @return The formatted staff alert message.
     */
    private String formatStaffAlertMessage(User user, PunishType punishType, SierraConfigEngine sierraConfig) {
        String staffAlertTemplate = sierraConfig.config().getString(
            "layout.detection-message.staff-alert",
            "{prefix} &c{username} &8┃ &f{mitigation} &c{checkname} &8┃ &cx{violations}"
        );
        return staffAlertTemplate
            .replace("{prefix}", Sierra.PREFIX)
            .replace("{username}", user.getName())
            .replace("{mitigation}", punishType.friendlyMessage())
            .replace("{checkname}", this.friendlyName)
            .replace("{violations}", String.valueOf(violations));
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
     * Sets the number of violations found by this check.
     *
     * @param violations The number of violations to set.
     * @return The number of violations after setting.
     */
    @Override
    public double setViolations(double violations) {
        this.violations = (int) violations;
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
