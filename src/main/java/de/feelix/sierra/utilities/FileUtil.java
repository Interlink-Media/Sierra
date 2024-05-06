package de.feelix.sierra.utilities;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {

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
