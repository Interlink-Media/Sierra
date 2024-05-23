package de.feelix.sierraloader;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.feelix.sierraloader.exception.SierraLoaderException;
import de.feelix.sierraloader.storage.Resource;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The SierraLoader class is a Bukkit plugin that loads and unloads a custom plugin from a remote repository.
 */
@SuppressWarnings("unused")
public class SierraLoader extends JavaPlugin {

    /**
     * The resource variable represents a resource that can be written to a file.
     * It is an instance of the Resource class.
     *
     * @since version 1.0.0
     */
    private final Resource resource = new Resource("sierra");

    /**
     * Represents a plugin.
     */
    private Plugin plugin;

    /**
     * The onEnable method is called when the plugin is enabled.
     * It is responsible for initializing and setting up the plugin.
     */
    @Override
    public void onEnable() {
        getLogger().info("Starting Sierra Loader...");
        getLogger().info("Download latest version of sierra...");
        try {
            try (InputStream in = formURL().openStream()) {
                resource.write(in);
            }
        } catch (IOException e) {
            getLogger().warning("Error occurred while downloading JAR: " + e.getMessage());
        }
        getLogger().info("Latest version of sierra downloaded! Booting...");
        loadJar();
    }

    /**
     * The onDisable method is called when the plugin is disabled.
     * It is responsible for unloading and disabling the SierraLoader plugin.
     */
    @Override
    public void onDisable() {
        getLogger().info("Unloading sierra...");
        if (plugin != null) {
            getServer().getPluginManager().disablePlugin(plugin);
            plugin = null;
        }
    }

    /**
     * Loads the JAR file and enables the plugin.
     *
     * @throws SierraLoaderException if there is an error loading the JAR file
     */
    private void loadJar() {
        try {
            Path tempFile = createTempJarFile();
            if (loadJarToPlugin(tempFile.toFile())) {
                plugin.onLoad();
                getServer().getPluginManager().enablePlugin(plugin);
            }
        } catch (InvalidPluginException | IOException | InvalidDescriptionException e) {
            throw new SierraLoaderException("Failed to load jar", e);
        }
    }

    /**
     * Creates a temporary JAR file by copying the contents of the original file and returns its path.
     *
     * @return the path of the temporary JAR file
     * @throws IOException if an I/O error occurs
     */
    private Path createTempJarFile() throws IOException {
        File originalFile = resource.createOrGetFile();
        Path tempFile     = Files.createTempFile("Sierra-", ".jar");
        copy(originalFile, tempFile.toFile());
        return tempFile;
    }

    /**
     * Loads a JAR file into the plugin.
     *
     * @param jarFile the JAR file to load
     * @return true if the JAR file was loaded successfully, false otherwise
     * @throws InvalidPluginException      if the plugin is protocol
     * @throws InvalidDescriptionException if the plugin description is protocol
     */
    private boolean loadJarToPlugin(File jarFile) throws InvalidPluginException, InvalidDescriptionException {
        plugin = getServer().getPluginManager().loadPlugin(jarFile);
        return plugin != null;
    }

    /**
     * Copies the contents of a source file to a destination file.
     *
     * @param source          the source file to copy
     * @param destinationFile the destination file
     * @throws IOException if an I/O error occurs
     */
    private void copy(File source, File destinationFile) throws IOException {
        try (InputStream is = Files.newInputStream(source.toPath());
             OutputStream os = Files.newOutputStream(destinationFile.toPath())) {
            byte[] buffer = new byte[1024];
            int    length;
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
        }
    }

    /**
     * Forms a URL using the latest release download URL retrieved from the GitHub API.
     *
     * @return the URL formed using the latest release download URL
     * @throws MalformedURLException if the URL string is not a valid URL
     * @throws RuntimeException      if the latest release download URL is null
     */
    private URL formURL() throws MalformedURLException {
        String latestReleaseDownloadUrl = getLatestReleaseDownloadUrl("Interlink-Media", "Sierra");
        if (latestReleaseDownloadUrl == null) {
            throw new RuntimeException("The latest release download URL cannot be null");
        }
        return new URL(latestReleaseDownloadUrl);
    }

    /**
     * Retrieves the latest release download URL for a given GitHub repository.
     *
     * @param repoOwner the owner of the repository
     * @param repoName  the name of the repository
     * @return the latest release download URL as a String, or null if there was an error
     */
    public static String getLatestReleaseDownloadUrl(String repoOwner, String repoName) {
        try {
            return getDownloadURLFromResponse(executeGETRequest(
                String.format("https://api.github.com/repos/%s/%s/releases/latest", repoOwner, repoName)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Executes a GET request to the specified API URL and returns the response as a StringBuilder.
     *
     * @param apiUrl the URL of the API endpoint to send the GET request to
     * @return a StringBuilder containing the response from the GET request
     * @throws IOException if an I/O error occurs during the GET request
     */
    private static StringBuilder executeGETRequest(String apiUrl) throws IOException {
        URL               url  = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
        }

        StringBuilder responseBuilder;
        try (BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())))) {
            responseBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                responseBuilder.append(line);
            }
        }
        conn.disconnect();
        return responseBuilder;
    }

    /**
     * Retrieves the download URL from the given response.
     *
     * @param responseBuilder the StringBuilder containing the response
     * @return the download URL as a String
     * @throws RuntimeException if no assets are found in the latest release
     */
    private static String getDownloadURLFromResponse(StringBuilder responseBuilder) {
        JsonObject jsonResponse = new Gson().fromJson(responseBuilder.toString(), JsonObject.class);
        JsonArray  assets       = jsonResponse.getAsJsonArray("assets");

        //noinspection SizeReplaceableByIsEmpty
        if (assets == null || assets.size() == 0) return "";

        for (JsonElement asset : assets) {
            String browserDownloadUrl = asset.getAsJsonObject().get("browser_download_url").getAsString();
            if (!browserDownloadUrl.contains("Sierra-")) {
                continue;
            }
            return browserDownloadUrl;
        }
        return "";
    }
}
