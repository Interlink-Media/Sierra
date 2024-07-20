package de.feelix.sierra.manager.storage.logger;

import de.feelix.sierra.Sierra;
import de.feelix.sierraapi.exceptions.SierraException;
import lombok.Data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
public class SierraLogger {

    private BufferedWriter writer;
    private String playerName;

    public SierraLogger(String playerName) {
        this.playerName = playerName;

        File sierraLogs = new File("plugins/Sierra/logs/");

        if (!sierraLogs.exists()) {
            if (!sierraLogs.mkdirs()) {
                throw new SierraException("Failed to create directory " + sierraLogs.getAbsolutePath());
            }
        }

        File pluginDir = new File("plugins/Sierra/logs/" + playerName);
        if (!pluginDir.exists()) {
            if (!pluginDir.mkdirs()) {
                throw new RuntimeException("Failed to create plugin directory: " + pluginDir);
            }
        }
        try {
            writer = new BufferedWriter(new FileWriter(new File(pluginDir, "logs.sierra"), true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void log(LogTag tag, String message) {

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logMessage = String.format("[%s] %s: %s", timestamp, tag.name(), message);

        try {
            writer.write(logMessage);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            Sierra.getPlugin().getLogger().warning("Unable to write log: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
