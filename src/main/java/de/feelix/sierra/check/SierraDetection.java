package de.feelix.sierra.check;

import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierra.utilities.message.ConfigValue;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.impl.SierraCheck;
import de.feelix.sierraapi.events.impl.AsyncUserDetectionEvent;
import io.github.retrooper.packetevents.adventure.serializer.legacy.LegacyComponentSerializer;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.MitigationStrategy;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * The SierraDetection class is used to detect violations in player data.
 */
@Getter
public class SierraDetection implements SierraCheck {

    private final PlayerData playerData;
    private final CheckType  rawCheckType;

    private String friendlyName;
    private int    checkId;
    private int    violations = 0;

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
     * Dispatches a ProtocolPacketEvent with a ViolationDocument.
     *
     * @param event             The ProtocolPacketEvent to be dispatched.
     * @param violationDocument The ViolationDocument containing information about the violation.
     */
    public void dispatch(ProtocolPacketEvent<Object> event, ViolationDocument violationDocument) {

        // Cancel the packet event
        event.setCancelled(true);
        event.cleanUp();

        if (playerData.isReceivedPunishment()) return;

        // Update violation count
        this.violations++;

        // If i forget anything in calling to prevent null pointers
        correctViolation(violationDocument);

        // Asynchronously call user detection event
        throwDetectionEvent(violationDocument);

        // Log to console, alert staff, create history, and potentially punish
        User user = event.getUser();
        consoleLog(user, violationDocument);
        alert(user, violationDocument);

        if (violationDocument.getMitigationStrategy() != MitigationStrategy.MITIGATE) {

            Sierra.getPlugin().getSierraDataManager().addKick(this.checkType());

            Sierra            plugin            = Sierra.getPlugin();
            SierraDataManager sierraDataManager = plugin.getSierraDataManager();

            sierraDataManager
                .createPunishmentHistory(
                    playerData.username(), playerData.version(), violationDocument.getMitigationStrategy(),
                    playerData.getPingProcessor().getPing(),
                    FormatUtils.chainDebugs(true, violationDocument.getDebugs())
                );

            blockAddressIfEnabled(violationDocument);
            playerData.punish(violationDocument.getMitigationStrategy());
        }
    }

    /**
     * Corrects a violation in the provided ViolationDocument.
     *
     * @param violationDocument The ViolationDocument containing information about the violation.
     */
    private void correctViolation(ViolationDocument violationDocument) {
        if (violationDocument.getDebugs() == null) violationDocument.setDebugs(Collections.emptyList());

        if (violationDocument.getDescription() == null) violationDocument.setDescription("No description provided");

        if (violationDocument.getMitigationStrategy() == null)
            violationDocument.setMitigationStrategy(MitigationStrategy.MITIGATE);
    }

    /**
     * Block the player's address if the punishment type is set to BAN, the ban feature is enabled in the
     * punishment configuration, and the "block-connections-after-ban" property is set to true in the Sierra
     * configuration.
     *
     * @param violationDocument The ViolationDocument object containing information about the violation.
     */
    private void blockAddressIfEnabled(ViolationDocument violationDocument) {
        if (violationDocument.getMitigationStrategy() == MitigationStrategy.BAN && Sierra.getPlugin()
            .getPunishmentConfig()
            .isBan()
            && Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("block-connections-after-ban", true)) {
            Sierra.getPlugin()
                .getAddressStorage()
                .addIPAddress(this.playerData.getUser().getAddress().getAddress().getHostAddress());
        }
    }

    /**
     * Throws a detection event asynchronously.
     *
     * @param violationDocument The ViolationDocument containing information about the violation
     */
    private void throwDetectionEvent(ViolationDocument violationDocument) {

        FoliaScheduler.getAsyncScheduler().runNow(
            Sierra.getPlugin(),
            o -> Sierra.getPlugin()
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

        if (violationDocument.getMitigationStrategy() == MitigationStrategy.MITIGATE) return;

        logToConsole(createGeneralMessage(user, violationDocument.getMitigationStrategy()));
        logToConsole(createGeneralInformation(violationDocument));
        logToConsole(createGeneralCheck());
    }

    private String createGeneralMessage(User user, MitigationStrategy mitigationStrategy) {
        return "Player " + user.getName() + " got " + mitigationStrategy.friendlyMessage()
               + " sending an protocol packet";
    }

    /**
     * Creates general information for a violation document.
     *
     * @param violationDocument The ViolationDocument object containing information about the violation.
     * @return A string representing the general information.
     */
    private String createGeneralInformation(ViolationDocument violationDocument) {
        return String.format(
            "Debug information: %s", violationDocument.getDebugs().isEmpty()
                ? "No debug available"
                : FormatUtils.shortenString(violationDocument.debugInformation()));
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
        logger.info(message);
    }


    /**
     * Sends an alert message to staff members with information about the violation.
     *
     * @param user              The User object representing the player.
     * @param violationDocument The ViolationDocument containing information about the violation.
     */
    protected void alert(User user, ViolationDocument violationDocument) {

        MitigationStrategy mitigationStrategy = violationDocument.getMitigationStrategy();
        String staffAlert = formatStaffAlertMessage(
            user, mitigationStrategy, violationDocument.getDescription());
        String username      = this.playerData.getUser().getName();
        String clientVersion = this.playerData.getUser().getClientVersion().getReleaseName();

        String content = new ConfigValue(
            "layout.detection-message.alert-content",
            " &7Username: &b{username}{n} &7Version: &b{clientVersion}{n} &7Brand: &b{brand}{n} &7Exist since: "
            + "&b{ticksExisted}{n} &7Game mode: &b{gameMode}{n} &7Tag: &b{tags}{n} &7Description: &b{description}{n} "
            + "&7Debug info: &b{debugInfo}{n}{n} "
            + "{alertNote}",
            true
        )
            .replace("{username}", username)
            .replace("{clientVersion}", clientVersion)
            .replace("{brand}", this.playerData.brand())
            .replace("{ticksExisted}", this.playerData.ticksExisted() + " ticks")
            .replace("{gameMode}", this.playerData.gameMode().name())
            .replace("{description}", violationDocument.getDescription())
            .replace("{tags}", this.friendlyName.toLowerCase())
            .replace("{debugInfo}", FormatUtils.shortenString(violationDocument.debugInformation()))
            .replace("{alertNote}", getAlertNote())
            .stripped().colorize().replacePrefix().message();

        String command = getPunishmentCommand(username);

        Collection<PlayerData> playerDataList = Sierra.getPlugin().getSierraDataManager().getPlayerData().values();

        if (mitigationStrategy == MitigationStrategy.MITIGATE) {
            for (PlayerData playerData : playerDataList) {
                if (playerData.getMitigationSettings().enabled()) {
                    playerData.getUser().sendMessage(
                        LegacyComponentSerializer.legacy('&')
                            .deserialize(staffAlert)
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, command))
                            .hoverEvent(HoverEvent.showText(Component.text(content))));
                }
            }
        } else {
            for (PlayerData playerData : playerDataList) {
                if (playerData.getAlertSettings().enabled()) {
                    playerData.getUser().sendMessage(
                        LegacyComponentSerializer.legacy('&')
                            .deserialize(staffAlert)
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, command))
                            .hoverEvent(HoverEvent.showText(Component.text(content))));
                }
            }
        }
    }

    /**
     * Retrieves the alert note from the Sierra configuration.
     *
     * @return The alert note from the configuration.
     */
    private String getAlertNote() {
        return new ConfigValue(
            "layout.detection-message.alert-command-note",
            "&fClick to teleport", true
        ).colorize().replacePrefix().message();
    }

    /**
     * Retrieves the punishment command for a given user from the Sierra configuration.
     *
     * @param username The username of the user.
     * @return The punishment command for the user.
     */
    private String getPunishmentCommand(String username) {
        return new ConfigValue(
            "layout.detection-message.alert-command",
            "/tp {username}", true
        ).replace("{username}", username).message();
    }

    private String formatStaffAlertMessage(User user, MitigationStrategy mitigationStrategy, String description) {

        return new ConfigValue(
            "layout.detection-message.staff-alert",
            "{prefix} &b{username} &8┃ &f{mitigation} &b{checkname} &8┃ &3x{violations}", true
        ).colorize().replacePrefix()
            .replace("{username}", user.getName())
            .replace("{mitigation}", mitigationStrategy.friendlyMessage())
            .replace("{description}", description)
            .replace("{checkname}", this.friendlyName)
            .replace("{violations}", String.valueOf(violations)).message();
    }

    /**
     * Returns the number of violations detected.
     *
     * @return The number of violations detected.
     */
    @Override
    public double violations() {
        return this.violations;
    }

    /**
     * Sets the number of violations found by this check.
     *
     * @param violations The number of violations to set.
     */
    @Override
    public void setViolations(double violations) {
        this.violations = (int) violations;
    }

    /**
     * Retrieves the check type of the current instance.
     *
     * @return The check type of the instance.
     */
    @Override
    public CheckType checkType() {
        return this.rawCheckType;
    }
}
