package app.krista.extensions.essentials.cloudfiles.sharepoint.util;

import com.microsoft.graph.models.DriveItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utility class for common operations related to Microsoft Graph Drive and File handling.
 */

public class DriveUtil {

    public static final String YYYY_MM_DD_T_HH_MM = "yyyy-MM-dd'T'HH:mm";
    public static final String YYYY_MM_DD = "yyyy-MM-dd";

    private DriveUtil() {
    }

    public static File getFileFromInputStream(InputStream stream, String fileName) throws IOException {
        if (stream == null) {
            throw new IllegalArgumentException(Constants.FAILED_GET_FILE_FROM_INPUT_STREAM_INPUT_STREAM_EMPTY);
        }
        stringValidator(fileName, Constants.FAILED_GET_FILE_FROM_INPUT_STREAM_FILE_NAME_EMPTY);
        File outputFile = new File(fileName);
        Files.copy(stream, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return outputFile;
    }

    public static void stringValidator(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static void listValidator(List files, String errorMessage) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static byte[] getByteArray(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return fis.readAllBytes();
        }
    }

    public static DriveItem getDriveItemForItem(String folderName) {
        stringValidator(folderName, Constants.FAILED_TO_GET_DRIVE_ITEM_FOLDER_NAME_IS_EMPTY);
        DriveItem driveItem = new DriveItem();
        driveItem.name = folderName;
        driveItem.folder = new com.microsoft.graph.models.Folder();
        return driveItem;
    }

    public static String validatePath(String fullUrl) {
        try {
            String escapedUrl = fullUrl.replace(" ", "%20");  // Escape spaces
            URI uri = new URI(escapedUrl);
            return uri.getPath();
        } catch (URISyntaxException e) {
            return null;
        }
    }


    public static String removeLastSegment(String url) {
        if (url != null && !url.isEmpty()) {
            int lastSlashIndex = url.lastIndexOf(File.separator);
            if (lastSlashIndex >= 0 && lastSlashIndex < url.length() - 1) {
                return url.substring(0, lastSlashIndex);
            }
        }
        return url;
    }

    public static String getRootUrl(String filePath) {
        if (filePath != null) {
            int domainEndIndex = filePath.indexOf(File.separator, 8);
            int fourthSlashIndex = nthIndexOf(filePath, File.separator.charAt(0), domainEndIndex, 4);

            if (fourthSlashIndex != -1) {
                return filePath.substring(0, fourthSlashIndex);
            }
        }
        return null;
    }

    private static int nthIndexOf(String str, char c, int startIndex, int n) {
        int index = startIndex - 1;
        for (int i = 0; i < n; i++) {
            index = str.indexOf(c, index + 1);
            if (index == -1) {
                break;
            }
        }
        return index;
    }

    public static String removeFirstSlash(String inputPath) {
        if (inputPath != null && inputPath.startsWith(File.separator)) {
            return inputPath.substring(1);
        }
        return inputPath;
    }

    public static String doubleToDateConversion(String inputString) {
        double inputDouble = Double.parseDouble(inputString);

        // Convert double to Date
        Date date = new Date((long) inputDouble);

        // Format the date as "dd-MM-yyyy"
        SimpleDateFormat dateFormat = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM);
        return dateFormat.format(date);
    }

    public static String getModifiedDate(long from) {
        Date date = new Date(from);
        DateFormat dateFormat = new SimpleDateFormat(YYYY_MM_DD);
        dateFormat.setTimeZone(TimeZone.getTimeZone("IST"));
        return dateFormat.format(date);
    }

    public static String getTokenExtractor(String url) {
        String token;
        try {
            URI uri = new URI(url);
            String query = uri.getQuery();

            Map<String, String> queryParams = new HashMap<>();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] parts = pair.split("=");
                    String key = parts[0];
                    String value = parts.length > 1 ? parts[1] : null;
                    queryParams.put(key, value);
                }
            }

            token = queryParams.get("token");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException();
        }
        return token;
    }

}