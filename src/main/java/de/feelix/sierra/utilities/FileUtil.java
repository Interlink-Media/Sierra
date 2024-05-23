package de.feelix.sierra.utilities;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * FileUtil is a utility class for working with files.
 */
public class FileUtil {

    /**
     * This method saves an InputStream to a file specified by the file path.
     *
     * @param inputStream The InputStream to save.
     * @param filePath    The file path where the InputStream should be saved.
     * @throws IOException if there is an error saving the InputStream to file.
     */
    public static void saveInputStreamToFile(InputStream inputStream, String filePath) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(filePath);

        byte[] buffer = new byte[1024];
        int length;

        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        inputStream.close();
        outputStream.close();
    }
}
