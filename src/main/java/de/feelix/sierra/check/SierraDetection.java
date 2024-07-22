package de.feelix.sierra.check;

import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.config.SierraConfigEngine;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.manager.storage.SierraDataManager;
import de.feelix.sierra.manager.storage.logger.LogTag;
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
    private final CheckType rawCheckType;
    private String friendlyName;
    private long lastDetectionTime = 0;
    private int checkId;
    private int violations = 0;

    /**
     * Initializes a new SierraDetection instance with the provided player data.
     *
     * @param playerData The PlayerData object containing the player's data.
     */
    public SierraDetection(PlayerData playerData) {
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
    private CheckType initializeCheckType() {
        if (this.getClass().isAnnotationPresent(SierraCheckData.class)) {
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
        event.setCancelled(true);
        event.cleanUp();
        this.lastDetectionTime = System.currentTimeMillis();

        playerData.getSierraLogger().log(LogTag.DETECTION, violationDocument.toString());

        if (playerData.isReceivedPunishment()) return;

        this.violations++;
        correctViolation(violationDocument);
        throwDetectionEvent(violationDocument);

        User user = event.getUser();
        logViolation(user, violationDocument);
        alertStaff(user, violationDocument);

        if (violationDocument.getMitigationStrategy().mitigationOrdinal()
            >= MitigationStrategy.KICK.mitigationOrdinal()) {
            handlePunishment(violationDocument);
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
     * Throws a detection event asynchronously.
     *
     * @param violationDocument The ViolationDocument containing information about the violation.
     */
    private void throwDetectionEvent(ViolationDocument violationDocument) {
        FoliaScheduler.getAsyncScheduler().runNow(Sierra.getPlugin(), o -> Sierra.getPlugin()
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
    private void logViolation(User user, ViolationDocument violationDocument) {
        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("log-violation-to-console", true)
            || violationDocument.getMitigationStrategy() == MitigationStrategy.MITIGATE) {
            return;
        }

        Logger logger = Sierra.getPlugin().getLogger();
        logger.info(createGeneralMessage(user, violationDocument.getMitigationStrategy()));
        logger.info(createGeneralInformation(violationDocument));
        logger.info(createGeneralCheck());
    }

    private String createGeneralMessage(User user, MitigationStrategy mitigationStrategy) {
        return String.format(
            "Player %s got %s sending a protocol packet", user.getName(), mitigationStrategy.friendlyMessage());
    }

    private String createGeneralInformation(ViolationDocument violationDocument) {
        return String.format(
            "Debug information: %s", violationDocument.getDebugs().isEmpty()
                ? "No debug available"
                : violationDocument.debugInformation());
    }

    private String createGeneralCheck() {
        return String.format("Check Information: %s/%d - VL: %d", this.friendlyName, this.checkId, this.violations);
    }

    /**
     * Sends an alert message to staff members with information about the violation.
     *
     * @param user              The User object representing the player.
     * @param violationDocument The ViolationDocument containing information about the violation.
     */
    private void alertStaff(User user, ViolationDocument violationDocument) {
        String staffAlert = formatStaffAlertMessage(
            user, violationDocument.getMitigationStrategy(), violationDocument.getDescription());
        String content = formatAlertContent(user, violationDocument);

        Collection<PlayerData> playerDataList = Sierra.getPlugin().getSierraDataManager().getPlayerData().values();
        playerDataList.forEach(playerData -> {
            if (shouldAlertPlayer(playerData, violationDocument.getMitigationStrategy())) {
                playerData.getUser().sendMessage(
                    LegacyComponentSerializer.legacy('&')
                        .deserialize(staffAlert)
                        .clickEvent(
                            ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, getPunishmentCommand(user.getName())))
                        .hoverEvent(HoverEvent.showText(Component.text(content)))
                );
            }
        });
    }

    private boolean shouldAlertPlayer(PlayerData playerData, MitigationStrategy mitigationStrategy) {
        return (mitigationStrategy == MitigationStrategy.MITIGATE && playerData.getMitigationSettings().enabled()) ||
               (mitigationStrategy != MitigationStrategy.MITIGATE && playerData.getAlertSettings().enabled());
    }

    private String formatStaffAlertMessage(User user, MitigationStrategy mitigationStrategy, String description) {
        return new ConfigValue(
            "layout.detection-message.staff-alert",
            "{prefix} &b{username} &8┃ &f{mitigation} &b{checkname} &8┃ &3x{violations}", true
        )
            .colorize().replacePrefix()
            .replace("{username}", user.getName())
            .replace("{mitigation}", mitigationStrategy.friendlyMessage())
            .replace("{description}", description)
            .replace("{checkname}", this.friendlyName)
            .replace("{violations}", String.valueOf(violations)).message();
    }

    private String formatAlertContent(User user, ViolationDocument violationDocument) {
        return new ConfigValue(
            "layout.detection-message.alert-content",
            " &7Username: &b{username}{n} &7Version: &b{clientVersion}{n} &7Brand: &b{brand}{n} &7Exist since: "
            + "&b{ticksExisted}{n} &7Game mode: &b{gameMode}{n} &7Tag: &b{tags}{n} &7Description: &b{description}{n} "
            + "&7Debug info: &b{debugInfo}{n}{n} {alertNote}",
            true
        )
            .replace("{username}", user.getName())
            .replace("{clientVersion}", playerData.getUser().getClientVersion().getReleaseName()
                .replace("V_", "")
                .replace("_", "."))
            .replace("{brand}", playerData.brand())
            .replace("{ticksExisted}", playerData.ticksExisted() + " ticks")
            .replace("{gameMode}", playerData.gameMode().name())
            .replace("{description}", violationDocument.getDescription())
            .replace("{tags}", this.friendlyName.toLowerCase())
            .replace("{debugInfo}", FormatUtils.shortenString(violationDocument.debugInformation()))
            .replace("{alertNote}", getAlertNote())
            .stripped().colorize().replacePrefix().message();
    }

    private String getPunishmentCommand(String username) {
        return new ConfigValue("layout.detection-message.alert-command", "/tp {username}", true).replace(
            "{username}", username).message();
    }

    private String getAlertNote() {
        return new ConfigValue("layout.detection-message.alert-command-note", "&fClick to teleport", true).colorize()
            .replacePrefix()
            .message();
    }

    private void handlePunishment(ViolationDocument violationDocument) {
        Sierra plugin = Sierra.getPlugin();
        SierraDataManager sierraDataManager = plugin.getSierraDataManager();

        sierraDataManager.addKick(this.checkType());
        sierraDataManager.createPunishmentHistory(playerData.username(), playerData.version(),
                                                  violationDocument.getMitigationStrategy(),
                                                  playerData.getPingProcessor().getPing(),
                                                  FormatUtils.chainDebugs(violationDocument.getDebugs())
        );

        blockAddressIfEnabled(violationDocument);
        playerData.punish(violationDocument.getMitigationStrategy());
    }

    private void blockAddressIfEnabled(ViolationDocument violation) {
        boolean punishmentSetting = Sierra.getPlugin().getPunishmentConfig().isBan();
        boolean blockConnections = Sierra.getPlugin().getSierraConfigEngine().config()
            .getBoolean("block-connections-after-ban", true);

        if (violation.getMitigationStrategy() == MitigationStrategy.BAN && punishmentSetting && blockConnections) {
            Sierra.getPlugin().getAddressStorage()
                .addIPAddress(this.playerData.getUser().getAddress().getAddress().getHostAddress());
        }
    }

    /**
     * Retrieves the SierraConfigEngine instance from the Sierra plugin.
     *
     * @return The SierraConfigEngine instance.
     */
    public SierraConfigEngine configEngine() {
        return Sierra.getPlugin().getSierraConfigEngine();
    }

    @Override
    public double violations() {
        return this.violations;
    }

    @Override
    public long lastDetection() {
        return this.lastDetectionTime;
    }

    @Override
    public void setViolations(double violations) {
        this.violations = (int) violations;
    }

    @Override
    public CheckType checkType() {
        return this.rawCheckType;
    }
}
