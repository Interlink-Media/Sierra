package de.feelix.sierra.check.impl.command;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.PunishType;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SierraCheckData(checkType = CheckType.COMMAND)
public class BlockedCommand extends SierraDetection implements IngoingProcessor {

    /**
     * Regular expression pattern for excluding plugins in the BlockedCommand class.
     * It is used to match a plugin prefix (e.g., "plugin:") followed by a non-whitespace character.
     * The pattern is compiled and stored as a private static final field.
     */
    private static final Pattern PLUGIN_EXCLUSION = java.util.regex.Pattern.compile("/(\\S+:)");

    /**
     * Regular expression pattern used to match exploit patterns in strings.
     * An exploit pattern is defined as any string that starts with "${" and ends with "}".
     * This pattern can be used to search for such patterns and take appropriate action.
     */
    private static final Pattern EXPLOIT_PATTERN = java.util.regex.Pattern.compile("\\$\\{.+}");

    /**
     * Represents a regular expression pattern for detecting a specific code pattern in a string.
     * <p>
     * The pattern is defined as follows:
     * - The string must contain the word "for" followed by an open parenthesis "("
     * - The pattern can match any characters (including new lines) in a non-greedy way, denoted by ".*?"
     * - The string must then contain a closing parenthesis ")" followed by an opening curly brace "{"
     * - The pattern can again match any characters (including new lines) in a non-greedy way, denoted by ".*?"
     * - The string must end with a closing curly brace "}"
     * <p>
     * This pattern is typically used to detect a for loop with its body in a given string.
     * <p>
     * Example usage:
     * <p>
     * ```java
     * String code = "for (...) {\n   ...\n}";
     * Matcher matcher = pattern.matcher(code);
     * if (matcher.find()) {
     *     // Code pattern matched
     *     ...
     * }
     * ```
     */
    private static final Pattern WORLDEDIT_PATTERN = Pattern.compile("for\\(.*?\\)\\{.*?}");

    /**
     * Regular expression pattern used to match a specific command pattern in the format "/mv ({letter?{number}})%".
     * The pattern is used to detect blocked commands.
     */
    private static final Pattern MVC_PATTERN = java.util.regex.Pattern.compile("/mv \\((\\w\\?\\{\\d+})\\)%");

    /**
     * The EXPLOIT_PATTERN2 variable is a regular expression pattern that matches a specific pattern in a string.
     * It is used to search for occurrences of "${...}" in a string, where "..." represents any characters.
     * <p>
     * Usage:
     * <p>
     * ```java
     * private static final Pattern EXPLOIT_PATTERN2 = Pattern.compile("\\$\\{.*}");
     * ```
     * <p>
     * The EXPLOIT_PATTERN2 variable is of type java.util.regex.Pattern.
     * <p>
     * Example usage:
     * <p>
     * ```java
     * String message = "This is a ${placeholder}.";
     * if (EXPLOIT_PATTERN2.matcher(message).find()) {
     * // Do something if the pattern is found in the message
     * ...
     * } else {
     * // Do something if the pattern is not found in the message
     * ...
     * }
     * ```
     */
    private static final Pattern EXPLOIT_PATTERN2 = Pattern.compile("\\$\\{.*}");

    private double count                = 0;
    private String lastCommand          = "";
    private long   sentLastMessageTwice = 0;

    /**
     * BlockedCommand is a class representing a specific action to be taken when a blocked command is detected.
     *
     * @param playerData The PlayerData associated with the player
     */
    public BlockedCommand(PlayerData playerData) {
        super(playerData);
    }

    /**
     * Handles incoming packets and performs specific actions based on the packet type.
     *
     * @param event      the PacketReceiveEvent that triggered the handling
     * @param playerData the PlayerData associated with the player
     */
    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {

        if (!Sierra.getPlugin()
            .getSierraConfigEngine()
            .config()
            .getBoolean("block-disallowed-commands", true)) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.UPDATE_COMMAND_BLOCK) {

            WrapperPlayClientUpdateCommandBlock wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientUpdateCommandBlock(event),
                playerData::exceptionDisconnect
            );

            checkDisallowedCommand(event, wrapper.getCommand());

        } else if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {

            WrapperPlayClientChatMessage wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientChatMessage(event),
                playerData::exceptionDisconnect
            );

            String message = wrapper.getMessage().toLowerCase().replaceAll("\\s+", " ");
            checkForDoubleCommands(event, message);
            checkDisallowedCommand(event, message);
            checkForLog4J(event, message);
        } else if (event.getPacketType() == PacketType.Play.Client.NAME_ITEM) {

            WrapperPlayClientNameItem wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientNameItem(event),
                playerData::exceptionDisconnect
            );
            checkForLog4J(event, wrapper.getItemName());
        } else if (event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND) {

            WrapperPlayClientChatCommand wrapper = CastUtil.getSupplierValue(
                () -> new WrapperPlayClientChatCommand(event),
                playerData::exceptionDisconnect
            );

            String message = wrapper.getCommand().toLowerCase().replaceAll("\\s+", " ");
            checkForDoubleCommands(event, message);
            checkDisallowedCommand(event, wrapper.getCommand());
            checkForLog4J(event, message);
        }
    }

    /**
     * Checks the given command for disallowed commands and triggers a violation if found.
     *
     * @param event   the PacketReceiveEvent that triggered the check
     * @param wrapper the WrapperPlayClientUpdateCommandBlock containing the command to be checked
     */
    private void checkDisallowedCommand(PacketReceiveEvent event, String wrapper) {
        String string = wrapper.toLowerCase().replaceAll("\\s+", " ");

        for (String disallowedCommand : Sierra.getPlugin()
            .getSierraConfigEngine().config().getStringList("disallowed-commands")) {
            if (string.contains(disallowedCommand)) {
                if (playerHasPermission(event)) {
                    violation(event, ViolationDocument.builder()
                        .debugInformation(string)
                        .punishType(PunishType.MITIGATE)
                        .build());
                }
            }
        }

        if (WORLDEDIT_PATTERN.matcher(string).find()) {
            violation(event, ViolationDocument.builder()
                .punishType(PunishType.KICK)
                .debugInformation("WorldEdit Pattern: " + string)
                .build());
        }

        if (MVC_PATTERN.matcher(string).find()) {
            violation(event, ViolationDocument.builder()
                .punishType(PunishType.KICK)
                .debugInformation("MVC Pattern: " + string)
                .build());
        }
    }

    /**
     * Checks the given message for Log4J related vulnerabilities and triggers a violation if found.
     *
     * @param event   the PacketReceiveEvent that triggered the check
     * @param message the message to be checked
     */
    private void checkForLog4J(PacketReceiveEvent event, String message) {
        message = message.toLowerCase();

        if (message.contains("${jndi:ldap") || message.contains("${jndi") || message.contains("ldap")) {
            violation(event, ViolationDocument.builder()
                .debugInformation("Ldap: " + message)
                .punishType(PunishType.MITIGATE)
                .build());
        }

        if (EXPLOIT_PATTERN.matcher(message).matches() || EXPLOIT_PATTERN2.matcher(message).matches()) {
            violation(event, ViolationDocument.builder()
                .debugInformation("Pattern 4J: " + message)
                .punishType(PunishType.MITIGATE)
                .build());
        }
    }

    /**
     * Checks for double commands in the given message and handles violations if necessary.
     *
     * @param event   the packet receive event
     * @param message the message to check for double commands
     */
    private void checkForDoubleCommands(PacketReceiveEvent event, String message) {
        for (String disallowedCommand : Sierra.getPlugin()
            .getSierraConfigEngine().config().getStringList("disallowed-commands")) {
            if (message.contains(disallowedCommand)) {
                if (playerHasPermission(event)) {
                    violation(event, ViolationDocument.builder()
                        .debugInformation(message)
                        .punishType(PunishType.MITIGATE)
                        .build());
                }
            }
            String pluginCommand = replaceGroup(PLUGIN_EXCLUSION.pattern(), message);
            if (pluginCommand.contains(disallowedCommand)) {
                if (playerHasPermission(event)) {
                    violation(event, ViolationDocument.builder()
                        .debugInformation(pluginCommand)
                        .punishType(PunishType.MITIGATE)
                        .build());
                }
            }
        }

        if (this.lastCommand.equalsIgnoreCase(message)) {
            if (System.currentTimeMillis() - this.sentLastMessageTwice < 1000 && this.count++ > 5) {
                violation(event, ViolationDocument.builder()
                    .punishType(PunishType.MITIGATE)
                    .debugInformation(String.format("Typed same command %s times", this.count))
                    .build());
            }
            this.sentLastMessageTwice = System.currentTimeMillis();
        } else {
            this.count = 0;
        }
        this.lastCommand = message;
    }

    /**
     * Checks if a player has permission to perform a certain action.
     *
     * @param event the packet receive event
     * @return true if the player has permission, false otherwise
     */
    private boolean playerHasPermission(PacketReceiveEvent event) {
        Player player = (Player) event.getPlayer();

        if (player == null) {
            return true;
        }

        return !player.isOp();
    }

    /**
     * Replaces the first occurrence of a captured group in the source string that matches the given regular expression.
     *
     * @param regex  the regular expression pattern to match
     * @param source the string in which to replace the captured group
     * @return the resulting string after replacing the captured group, or the source string if the pattern is not found
     */
    private String replaceGroup(String regex, String source) {

        Matcher m = Pattern.compile(regex).matcher(source);
        for (int i = 0; i < 1; i++)
            if (!m.find()) return source; // pattern not met, may also throw an exception here
        return new StringBuilder(source).replace(m.start(1), m.end(1), "").toString();
    }
}


