package de.feelix.sierra.check;

import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.config.SierraConfigEngine;
import de.feelix.sierra.manager.storage.HistoryDocument;
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
import de.feelix.sierraapi.events.AsyncHistoryCreateEvent;
import de.feelix.sierraapi.events.AsyncUserDetectionEvent;
import de.feelix.sierraapi.violation.PunishType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public class SierraDetection implements SierraCheck {

    // Member Variables
    private final PlayerData playerData;
    private final CheckType  rawCheckType;

    private String friendlyName;
    private int    checkId;
    private int    violations = 0;

    // Constructor
    public SierraDetection(PlayerData playerData) {
        // Initialize member variables
        this.playerData = playerData;
        this.rawCheckType = initializeCheckType();
        if (this.rawCheckType != null) {
            this.friendlyName = rawCheckType.getFriendlyName();
            this.checkId = rawCheckType.getId();
        }
    }

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

    // Handle violation event
    public void violation(ProtocolPacketEvent<Object> event, ViolationDocument violationDocument) {

        // Cancel the packet event
        event.setCancelled(true);

        // Safety check for player data in violation document
        if (violationDocument.getPlayerData() == null) violationDocument.setPlayerData(playerData);

        if (violationDocument.getPunishType() == null) violationDocument.setPunishType(PunishType.MITIGATE);

        if (violationDocument.getDebugInformation() == null || violationDocument.getDebugInformation().isEmpty()) {
            violationDocument.setDebugInformation("No debug available");
        }

        // Asynchronously call user detection event
        Bukkit.getScheduler().runTaskAsynchronously(
            Sierra.getPlugin(),
            () -> Bukkit.getPluginManager().callEvent(new AsyncUserDetectionEvent(violationDocument, playerData))
        );

        // Update violation count
        this.violations++;

        // Log to console, alert staff, create history, and potentially punish
        User user = event.getUser();
        consoleLog(user, violationDocument);
        alert(user, violationDocument.getDebugInformation(), violationDocument.punishType());
        createHistory(playerData, violationDocument);
        playerData.punish(violationDocument.punishType());
    }

    // Create history document asynchronously
    private void createHistory(PlayerData playerData, ViolationDocument violationDocument) {
        HistoryDocument document = new HistoryDocument(
            playerData.getUser().getName(),
            violationDocument.getDebugInformation(),
            violationDocument.punishType()
        );

        Bukkit.getScheduler().runTaskAsynchronously(
            Sierra.getPlugin(),
            () -> Bukkit.getPluginManager().callEvent(new AsyncHistoryCreateEvent(document))
        );

        Sierra.getPlugin().getDataManager().getHistories().add(document);
    }

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
        int ticks = FormatUtils.convertMillisToTicks(System.currentTimeMillis() - this.playerData.getJoinTime());

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

    // Implement interface method for violation count
    @Override
    public double violations() {
        return this.violations;
    }

    // Implement interface method for check type
    @Override
    public CheckType checkType() {
        return this.rawCheckType;
    }
}
