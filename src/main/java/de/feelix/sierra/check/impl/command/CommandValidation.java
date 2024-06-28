package de.feelix.sierra.check.impl.command;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.check.violation.Violation;
import de.feelix.sierra.manager.config.SierraConfigEngine;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.PunishType;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SierraCheckData(checkType = CheckType.COMMAND_VALIDATION)
public class CommandValidation extends SierraDetection implements IngoingProcessor {

    private static final Pattern PLUGIN_EXCLUSION  = Pattern.compile("/(\\S+:)");
    private static final Pattern EXPLOIT_PATTERN   = Pattern.compile("\\$\\{.+}");
    public static final  Pattern WORLDEDIT_PATTERN = Pattern.compile("for\\(.*?\\)\\{.*?}");
    private static final Pattern MVC_PATTERN       = Pattern.compile("/mv \\((\\w\\?\\{\\d+})\\)%");
    private static final Pattern EXPLOIT_PATTERN2  = Pattern.compile("\\$\\{.*}");

    private double count                = 0;
    private String lastCommand          = "";
    private long   sentLastMessageTwice = 0;
    private long   lastEntry            = 0;
    private int    commandSpamBuffer    = 0;

    public CommandValidation(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {
        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("block-disallowed-commands", true)) {
            return;
        }

        PacketTypeCommon packetType = event.getPacketType();
        if (packetType.equals(PacketType.Play.Client.UPDATE_COMMAND_BLOCK)) {
            WrapperPlayClientUpdateCommandBlock commandBlockWrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientUpdateCommandBlock(event), playerData::exceptionDisconnect);
            checkDisallowedCommand(event, commandBlockWrapper.getCommand().toLowerCase().replaceAll("\\s+", " "));
        } else if (packetType.equals(PacketType.Play.Client.CHAT_MESSAGE)) {
            WrapperPlayClientChatMessage chatMessageWrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientChatMessage(event), playerData::exceptionDisconnect);
            handleChatMessage(event, chatMessageWrapper.getMessage().toLowerCase().replaceAll("\\s+", " "));
        } else if (packetType.equals(PacketType.Play.Client.NAME_ITEM)) {
            WrapperPlayClientNameItem nameItemWrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientNameItem(event), playerData::exceptionDisconnect);
            checkForLog4J(event, nameItemWrapper.getItemName().toLowerCase().replaceAll("\\s+", " "));
        } else if (packetType.equals(PacketType.Play.Client.CHAT_COMMAND)) {
            WrapperPlayClientChatCommand chatCommandWrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientChatCommand(event), playerData::exceptionDisconnect);
            handleChatMessage(event, chatCommandWrapper.getCommand().toLowerCase().replaceAll("\\s+", " "));
        }
    }

    private void handleChatMessage(PacketReceiveEvent event, String message) {

        if (isInvalidMultiverseCommand(message)) {
            this.violation(event, Violation.builder()
                .description("used an forbidden command")
                .punishType(PunishType.MITIGATE)
                .points(3)
                .debugs(Collections.singletonList(new Debug<>("Command", message)))
                .build());
        }

        checkForDoubleCommands(event, message);
        checkDisallowedCommand(event, message);
        checkForLog4J(event, message);
        checkForPluginExploits(event, message);
    }

    private void checkForPluginExploits(PacketReceiveEvent event, String command) {
        command = command.replace("minecraft:", "").replace("/", "");

        if (System.currentTimeMillis() - lastEntry < 1000) {
            commandSpamBuffer++;
            if (commandSpamBuffer > 5) {
                this.violation(event, Violation.builder()
                    .description("is using commands too frequent")
                    .punishType(commandSpamBuffer > 50 ? PunishType.KICK : PunishType.MITIGATE)
                    .points(3)
                    .debugs(Collections.singletonList(new Debug<>("Delay", (System.currentTimeMillis() - lastEntry))))
                    .build());
            }
        } else {
            commandSpamBuffer = 0;
        }

        for (String placeholder : Arrays.asList("[pos]", "[time]")) {
            int count = countOccurrences(command, placeholder);
            if (count > 3) {
                this.violation(event, Violation.builder()
                    .description("is using tags too frequent")
                    .punishType(violations() > 100 ? PunishType.KICK : PunishType.MITIGATE)
                    .points(3)
                    .debugs(Collections.singletonList(new Debug<>("Count", count)))
                    .build());
            }
        }

        for (String string : command.split(" ")) {
            if (string.length() > 80) {

                this.violation(event, Violation.builder()
                    .description("is using an invalid command")
                    .punishType(PunishType.MITIGATE)
                    .debugs(Arrays.asList(new Debug<>("Length", string.length()), new Debug<>("Max", 80)))
                    .build());
            }
        }
        lastEntry = System.currentTimeMillis();
    }

    private int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx   = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    private void checkDisallowedCommand(PacketReceiveEvent event, String commandLine) {
        for (String disallowedCommand : Sierra.getPlugin()
            .getSierraConfigEngine()
            .config()
            .getStringList("disallowed-commands")) {
            if (commandLine.contains(disallowedCommand)) {
                if (playerHasNoPermission()) {
                    this.violation(event, Violation.builder()
                        .description("is using an invalid command")
                        .punishType(PunishType.MITIGATE)
                        .debugs(Collections.singletonList(new Debug<>("Command", commandLine)))
                        .build());
                }
            }
        }

        if (WORLDEDIT_PATTERN.matcher(commandLine).find()) {
            this.violation(event, Violation.builder()
                .description("is using an invalid command")
                .punishType(PunishType.KICK)
                .debugs(Collections.singletonList(new Debug<>("Command", commandLine)))
                .build());
        }

        if (MVC_PATTERN.matcher(commandLine).find()) {
            this.violation(event, Violation.builder()
                .description("is using an invalid command")
                .punishType(PunishType.KICK)
                .debugs(Collections.singletonList(new Debug<>("Command", commandLine)))
                .build());
        }
    }

    private void checkForLog4J(PacketReceiveEvent event, String message) {
        if (message.contains("${jndi:ldap") || message.contains("${jndi") || message.contains("ldap")) {
            this.violation(event, Violation.builder()
                .description("is using an invalid command")
                .punishType(PunishType.KICK)
                .debugs(Collections.singletonList(new Debug<>("Command", message)))
                .build());
        }

        if (EXPLOIT_PATTERN.matcher(message).matches() || EXPLOIT_PATTERN2.matcher(message).matches()) {
            this.violation(event, Violation.builder()
                .description("is using an invalid command")
                .punishType(PunishType.KICK)
                .debugs(Collections.singletonList(new Debug<>("Command", message)))
                .build());
        }
    }

    private void checkForDoubleCommands(PacketReceiveEvent event, String message) {
        for (String disallowedCommand : Sierra.getPlugin()
            .getSierraConfigEngine()
            .config()
            .getStringList("disallowed-commands")) {
            if (message.contains(disallowedCommand)) {
                if (playerHasNoPermission()) {
                    this.violation(event, Violation.builder()
                        .description("is using an invalid command")
                        .punishType(PunishType.MITIGATE)
                        .debugs(Collections.singletonList(new Debug<>("Command", message)))
                        .build());
                }
            }
            String pluginCommand = replaceGroup(PLUGIN_EXCLUSION.pattern(), message);
            if (pluginCommand.contains(disallowedCommand)) {
                if (playerHasNoPermission()) {
                    this.violation(event, Violation.builder()
                        .description("is using an invalid command")
                        .punishType(PunishType.MITIGATE)
                        .debugs(Collections.singletonList(new Debug<>("Command", pluginCommand)))
                        .build());
                }
            }
        }

        if (lastCommand.equalsIgnoreCase(message)) {
            if (System.currentTimeMillis() - sentLastMessageTwice < 1000 && count++ > 5) {
                this.violation(event, Violation.builder()
                    .description("is using commands too frequent")
                    .punishType(PunishType.MITIGATE)
                    .debugs(Collections.singletonList(new Debug<>("Repeat", count)))
                    .build());
            }
            sentLastMessageTwice = System.currentTimeMillis();
        } else {
            count = 0;
        }
        lastCommand = message;
    }

    /**
     * This method checks if the player has the necessary permission to perform an action.
     * It retrieves the enable-bypass-permission configuration option from the sierra.yml file
     * and checks if the player has the bypass permission.
     *
     * @return true if the player does not have the necessary permission, false otherwise
     * @see Sierra#getPlugin()
     * @see Sierra#getSierraConfigEngine()
     * @see SierraConfigEngine#config()
     * @see PlayerData#hasBypassPermission()
     */
    private boolean playerHasNoPermission() {
        return !Sierra.getPlugin().getSierraConfigEngine().config()
            .getBoolean("enable-bypass-permission", false) || !getPlayerData().hasBypassPermission();
    }

    private boolean isInvalidMultiverseCommand(String testString) {
        return testString.contains("mvh") && testString.contains(".+.+.+.+") && testString.endsWith(")%");
    }

    private String replaceGroup(String regex, String source) {
        Matcher m = Pattern.compile(regex).matcher(source);
        if (!m.find()) return source;
        return new StringBuilder(source).replace(m.start(1), m.end(1), "").toString();
    }
}
