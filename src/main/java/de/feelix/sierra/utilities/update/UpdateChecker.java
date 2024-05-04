package de.feelix.sierra.utilities.update;

import de.feelix.sierra.Sierra;
import io.github.retrooper.packetevents.util.FoliaCompatUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * The UpdateChecker class is responsible for checking the latest release version of a given repository on GitHub.
 */
public class UpdateChecker {

    /**
     * The VERSION_START_TAG is a constant variable that represents the start tag for the version name in the JSON
     * response from the GitHub API.
     */
    private static final String VERSION_START_TAG = "\"tag_name\":\"";

    /**
     * The GITHUB_API_BASE_URL variable represents the base URL for the GitHub API.
     * It is a private constant string variable with the value "<a href="https://api.github.com/repos/">...</a>".
     * The base URL is used to construct the URL for making API requests to GitHub repositories.
     */
    private static final String GITHUB_API_BASE_URL = "https://api.github.com/repos/";

    /**
     * The GITHUB_API_RELEASES variable represents the endpoint for retrieving the latest release of a GitHub
     * repository.
     * It is a private constant string variable with the value "/releases/latest".
     */
    private static final String GITHUB_API_RELEASES = "/releases/latest";

    /**
     * The latestReleaseVersion variable stores the latest release version of the Sierra plugin.
     * It is a private field of type String.
     * The initial value is "UNKNOWN".
     */
    private String latestReleaseVersion = "UNKNOWN";

    /**
     * Represents an unknown version.
     */
    public static final String UNKNOWN_VERSION = "UNKNOWN";

    /**
     * Retrieves the latest release version of a given repository on GitHub.
     *
     * @return the latest release version as a string, or an empty string if an error occurred
     */
    public String getLatestReleaseVersion() {
        if (!latestReleaseVersion.equalsIgnoreCase(UNKNOWN_VERSION)) {
            return latestReleaseVersion;
        }
        refreshNewVersion();
        return this.latestReleaseVersion;
    }

    /**
     * Refreshes the new version by retrieving the latest release version from the GitHub API.
     * If an error occurs while retrieving the version, it logs the error and returns.
     */
    private void refreshNewVersion() {
        String jsonResponse;
        try {
            jsonResponse = getJsonResponseFromGithub();
        } catch (IOException e) {
            logError(e);
            return;
        }
        String versionFromJson = getVersionFromJson(jsonResponse);

        if (versionFromJson != null) {
            this.latestReleaseVersion = versionFromJson;
        }
    }

    /**
     * Logs a severe error message along with the exception stack trace.
     *
     * @param e The exception you want to log.
     */
    private void logError(Exception e) {
        Sierra.getPlugin().getLogger().severe("Unable to fetch latest version: " + e.getMessage());
    }

    /**
     * Retrieves the version number from a JSON response string.
     *
     * @param jsonResponse the JSON response string
     * @return the version number as a string, or null if it cannot be found
     */
    private String getVersionFromJson(String jsonResponse) {
        int startIndex = jsonResponse.indexOf(VERSION_START_TAG);
        if (startIndex != -1) {
            startIndex += VERSION_START_TAG.length();
            int endIndex = jsonResponse.indexOf("\"", startIndex);
            if (endIndex != -1) {
                return jsonResponse.substring(startIndex, endIndex);
            }
        }
        return null;
    }

    /**
     * Starts the scheduler for checking for updates asynchronously.
     * The scheduler runs the refreshNewVersion method every 1100 ticks (55 seconds) with an initial delay of 1100
     * ticks (55 seconds).
     */
    public void startScheduler() {
        FoliaCompatUtil.runTaskTimerAsync(Sierra.getPlugin(),
                                          o -> refreshNewVersion(), 5100, 5100
        );
    }

    /**
     * Retrieves the JSON response from the GitHub API for a given repository.
     *
     * @return the JSON response as a string
     * @throws IOException if an error occurs while making the API request
     */
    private String getJsonResponseFromGithub() throws IOException {
        URL               url        = new URL(GITHUB_API_BASE_URL + "Interlink-Media/Sierra" + GITHUB_API_RELEASES);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String jsonResponse = readResponse(reader);

        reader.close();

        return jsonResponse;
    }

    /**
     * Reads the response from a BufferedReader and returns it as a String.
     *
     * @param reader the BufferedReader to read the response from
     * @return the response as a String
     * @throws IOException if an error occurs while reading the response
     */
    private String readResponse(BufferedReader reader) throws IOException {
        StringBuilder responseBuilder = new StringBuilder();
        String        line;
        while ((line = reader.readLine()) != null) {
            responseBuilder.append(line);
        }
        return responseBuilder.toString();
    }
}
