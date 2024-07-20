package de.feelix.sierra.utilities.update;

import de.feelix.sierra.Sierra;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * The UpdateChecker class is responsible for checking the latest release version of a given repository on GitHub.
 */
@Getter
public class UpdateChecker {

    private static final String VERSION_START_TAG = "\"tag_name\":\"";
    private static final String GITHUB_API_BASE_URL = "https://api.github.com/repos/";
    private static final String GITHUB_API_RELEASES = "/releases/latest";
    public static final String UNKNOWN_VERSION = "UNKNOWN";

    private String latestReleaseVersion = UNKNOWN_VERSION;

    /**
     * Refreshes the new version by retrieving the latest release version from the GitHub API.
     * If an error occurs while retrieving the version, it logs the error and returns.
     */
    public void refreshNewVersion() {
        try {
            URL url = new URL(GITHUB_API_BASE_URL + "Interlink-Media/Sierra" + GITHUB_API_RELEASES);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                String jsonResponse = responseBuilder.toString();
                int startIndex = jsonResponse.indexOf(VERSION_START_TAG);
                if (startIndex != -1) {
                    startIndex += VERSION_START_TAG.length();
                    int endIndex = jsonResponse.indexOf("\"", startIndex);
                    if (endIndex != -1) {
                        this.latestReleaseVersion = jsonResponse.substring(startIndex, endIndex);
                    }
                }
            }
        } catch (IOException e) {
            Sierra.getPlugin().getLogger().severe("Unable to fetch latest version: " + e.getMessage());
        }
    }

    /**
     * Starts the scheduler for checking for updates asynchronously.
     * The scheduler runs the refreshNewVersion method every 1100 ticks (55 seconds) with an initial delay of 1100
     * ticks (55 seconds).
     */
    public void startScheduler() {
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(Sierra.getPlugin(), o -> refreshNewVersion(),
                                                          30100, 30100);
    }
}
