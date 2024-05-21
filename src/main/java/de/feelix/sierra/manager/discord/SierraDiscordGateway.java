package de.feelix.sierra.manager.discord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierraapi.check.CheckType;
import lombok.Data;
import org.bukkit.configuration.file.YamlConfiguration;

import java.awt.*;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The DiscordGateway class represents a Discord gateway for sending alerts and messages through a webhook.
 * It uses a webhook URL to interact with Discord and perform various actions related to Discord integration.
 */
// General class taken from: https://github.com/GrimAnticheat/Grim/blob/2.0/src/main/java/ac/grim/grimac/manager/DiscordManager.java
@Data
public class SierraDiscordGateway {

    /**
     * The WebhookClient class represents a client object used to interact with a Discord webhook.
     * It is responsible for sending alerts and messages through the webhook.
     * <p>
     * This class is a private static variable defined in the DiscordGateway class.
     * The DiscordGateway class is part of the GrimAnticheat project and is used for Discord integration.
     * <p>
     * Example usage:
     * <p>
     * // Create a new instance of WebhookClient
     * client = WebhookClient.withId(webhookId, webhookToken);
     * <p>
     * // Set the timeout for requests in milliseconds (optional)
     * client.setTimeout(timeout);
     * <p>
     * // Send an alert using the client object
     * client.sendAlert(playerData, violationDocument, violations);
     */
    private WebhookClient client;

    /**
     * The WEBHOOK_PATTERN variable represents a regular expression pattern for matching Discord webhook URLs.
     * <p>
     * The pattern matches the following format for webhook URLs:
     * - Either "http://" or "https://"
     * - Optional subdomain followed by a dot
     * - Main domain name followed by a dot and top-level domain
     * - "/api" followed by optional version number (e.g., "/v1") and "/webhooks/"
     * - Group 1: Webhook ID consisting of one or more digits
     * - Literal "/"
     * - Group 2: Webhook Token consisting of one or more alphabet characters, digits, underscores, or hyphens
     * - Optional additional path segment
     * <p>
     * Example usage:
     * <p>
     * String url = "<a href="https://discord.com/api/webhooks/123456789012345678/webhook_token">...</a>";
     * Matcher matcher = WEBHOOK_PATTERN.matcher(url);
     * if (matcher.matches()) {
     * String webhookId = matcher.group(1);
     * String webhookToken = matcher.group(2);
     * // Perform further processing based on webhook ID and webhook token
     * }
     */
    private final Pattern WEBHOOK_PATTERN = Pattern.compile(
        "(?:https?://)?(?:\\w+\\.)?\\w+\\.\\w+/api(?:/v\\d+)?/webhooks/(\\d+)/([\\w-]+)(?:/(?:\\w+)?)?");

    /**
     * Configures the Discord gateway for sending alerts and messages through a webhook.
     * The Discord gateway uses a webhook URL to interact with Discord.
     * It sets up the gateway, sets the timeout for requests, and enables the gateway if the necessary configurations
     * are
     * present.
     *
     * @throws IllegalArgumentException if the webhook URL is not in the correct format
     */
    public void setup() {
        YamlConfiguration config = Sierra.getPlugin().getSierraConfigEngine().config();

        if (!config.getBoolean("discord-webhook", false)) return;

        String url = config.getString("discord-webhook-url", "");

        if (url.isEmpty()) {
            Sierra.getPlugin().getLogger().severe("Discord is enabled, but webhook url is empty!");
            return;
        }

        Matcher matcher = WEBHOOK_PATTERN.matcher(url);

        if (!matcher.matches()) throw new IllegalArgumentException("Failed to parse webhook URL");

        client = WebhookClient.withId(Long.parseUnsignedLong(matcher.group(1)), matcher.group(2));
        client.setTimeout(15000); // Requests expire after 15 seconds

        Sierra.getPlugin().getLogger().info("Discord gateway is enabled");
    }

    /**
     * Sends an alert with the given player data, check type, violation document, and violations count to a Discord channel via webhook.
     *
     * @param playerData        the player data containing information about the player
     * @param checkType         the type of check that triggered the alert
     * @param violationDocument the violation document associated with the alert
     * @param violations        the number of violations recorded by the check
     */
    public void sendAlert(PlayerData playerData, CheckType checkType, ViolationDocument violationDocument,
                          double violations) {
        if (client != null) {

            WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
                .setAuthor(new WebhookEmbed.EmbedAuthor("Sierra AntiCrash",
                                                        "https://i.imgur.com/8ptl4C8.png",
                                                        "https://github.com/Interlink-Media/Sierra"
                ))
                .setImageUrl("https://i.stack.imgur.com/Fzh0w.png") // Constant width
                .setThumbnailUrl("https://crafthead.net/helm/" + playerData.uuid())
                .setColor(new Color(255, 30, 30).getRGB())
                .setTitle(new WebhookEmbed.EmbedTitle("**Sierra Report**", null))
                .setDescription("This is a report about player `" + playerData.username()+"`")

                .addField(new WebhookEmbed.EmbedField(false, "Check",
                                                      checkType.getFriendlyName() + "/" + violationDocument.punishType()
                                                          .historyMessage()
                ))
                .addField(new WebhookEmbed.EmbedField(false, "Debug information", violationDocument.debugInformation()))
                .addField(new WebhookEmbed.EmbedField(false, "Violations", String.valueOf(violations)))

                .setTimestamp(Instant.now())
                .setFooter(new WebhookEmbed.EmbedFooter(
                    "https://github.com/Interlink-Media/Sierra",
                    "https://i.imgur.com/8ptl4C8.png"
                ));

            try {
                client.send(embed.build());
            } catch (Exception exception) {
                Sierra.getPlugin().getLogger().severe("Unable to send webhook: " + exception.getMessage());
            }
        }
    }
}
