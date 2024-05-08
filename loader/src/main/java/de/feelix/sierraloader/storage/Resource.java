package de.feelix.sierraloader.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Locale;

/**
 * The Resource class represents a resource that can be written to a file.
 */
public class Resource {

    /**
     * Represents the path to the Windows directory for a resource file.
     */
    private static final String WINDOWS_DIRECTORY_PATH = "/Sierra/Bootstrap/";

    /**
     * Represents the directory path for another operating system.
     */
    private static final String OTHER_OS_DIRECTORY_PATH = "/.sierra/bootstrap/";

    /**
     * Represents the property for the operating system name.
     */
    private static final String OS_NAME_PROPERTY = "os.name";

    /**
     * The variable `name` represents the name of a resource.
     *
     * @since version 1.0.0
     */
    private final String name;

    /**
     * Represents a resource that can be written to a file.
     */
    public Resource(String name) {
        this.name = name;
    }

    /**
     * Writes the contents of the given InputStream to a file.
     *
     * @param inputStream the InputStream containing the data to write
     */
    public void write(InputStream inputStream) {
        File file = createOrGetFile();
        handleFileInputStream(file, inputStream);
    }

    /**
     * Creates a new file or retrieves an existing file with the given name.
     * The file is created in the appropriate directory based on the operating system.
     *
     * @return the created or existing file
     * @throws IllegalStateException if the directory cannot be created
     */
    public File createOrGetFile() {
        String operatingSystem = System.getProperty(OS_NAME_PROPERTY).toLowerCase(Locale.ROOT);
        String directoryPath;
        if (operatingSystem.contains("win")) {
            directoryPath = System.getenv("APPDATA") + WINDOWS_DIRECTORY_PATH;
        } else {
            directoryPath = System.getProperty("user.home") + OTHER_OS_DIRECTORY_PATH;
        }
        File workDirectory = new File(directoryPath);
        if (!workDirectory.exists()) {
            if (!workDirectory.mkdirs()) {
                throw new IllegalStateException("Unable to create directory " + workDirectory.getAbsolutePath());
            }
        }
        return new File(workDirectory, name + ".sierra");
    }

    /**
     * Handles the InputStream by creating or updating a specified File.
     *
     * @param file        the destination File
     * @param inputStream the InputStream to transfer to the File
     */
    private void handleFileInputStream(File file, InputStream inputStream) {
        if (file.exists()) {
            if (!file.delete()) {
                throw new IllegalStateException("Unable to delete file " + file.getAbsolutePath());
            }
        }
        try {
            if (!file.createNewFile()) {
                throw new IllegalStateException("Unable to create file " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        transferInputStreamToFile(file, inputStream);
    }

    /**
     * Transfers the contents of an InputStream to a specified File.
     *
     * @param file        the destination File
     * @param inputStream the InputStream containing the data to transfer
     */
    private void transferInputStreamToFile(File file, InputStream inputStream) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            ReadableByteChannel byteChannel = Channels.newChannel(inputStream);
            fileOutputStream.getChannel().transferFrom(byteChannel, 0, Long.MAX_VALUE);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
