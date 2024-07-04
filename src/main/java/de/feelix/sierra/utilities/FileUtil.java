package de.feelix.sierra.utilities;

import lombok.experimental.UtilityClass;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * FileUtil is a utility class for working with files.
 */
@UtilityClass
public class FileUtil {

    /**
     * This method saves the content of an InputStream to a file specified by the file path.
     *
     * @param inputStream The InputStream containing the content to be saved.
     * @param filePath    The path of the file to save the content to.
     * @throws IOException If an I/O error occurs while reading the InputStream or writing to the file.
     */
    public static void saveInputStreamToFile(InputStream inputStream, String filePath) throws IOException {
        try (InputStream in = inputStream;
             FileOutputStream out = new FileOutputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int    length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }
}
