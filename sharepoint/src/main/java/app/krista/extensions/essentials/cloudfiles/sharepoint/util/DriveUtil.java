package app.krista.extensions.essentials.cloudfiles.sharepoint.util;

import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.ItemReference;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;


public final class DriveUtil {

    private DriveUtil() {
    }

    public static File getFileFromInputStream(InputStream stream, String fileName) throws IOException {
        if (stream == null) {
            throw new IllegalArgumentException("Failed get file from input stream. Input stream is empty.");
        }
        stringValidator(fileName, "Failed get file from input stream. File name is empty.");
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
        try (FileInputStream fl = new FileInputStream(file)) {
            byte[] arr = new byte[(int) file.length()];
            while (fl.read(arr) > 0) {
            }
            return arr;
        }
    }

    public static DriveItem getDriveItemForItem(String folderName) {
        stringValidator(folderName, "Failed to get drive item. Folder name is empty.");
        DriveItem driveItem = new DriveItem();
        driveItem.name = folderName;
        driveItem.folder = new com.microsoft.graph.models.Folder();
        return driveItem;
    }

    public static String getRelativePath(String fullUrl) {
        try {
            String escapedUrl = fullUrl.replace(" ", "%20");  // Escape spaces
            URI uri = new URI(escapedUrl);
            String path = uri.getPath();

            String[] pathSegments = path.split("/");
            int sitesIndex = indexOfSegment(pathSegments, "sites");
            if (sitesIndex != -1 && sitesIndex + 3 < pathSegments.length) {
                StringBuilder dynamicPath = new StringBuilder("/");
                for (int i = sitesIndex + 3; i < pathSegments.length; i++) {
                    dynamicPath.append(pathSegments[i]);
                    if (i < pathSegments.length - 1) {
                        dynamicPath.append("/");
                    }
                }
                return dynamicPath.toString();
            } else {
                return null;
            }
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static int indexOfSegment(String[] pathSegments, String segment) {
        for (int i = 0; i < pathSegments.length; i++) {
            if (segment.equals(pathSegments[i])) {
                return i;
            }
        }
        return -1;
    }

    public static String removeLastSegment(String url) {
        if (url != null && !url.isEmpty()) {
            int lastSlashIndex = url.lastIndexOf('/');
            if (lastSlashIndex >= 0 && lastSlashIndex < url.length() - 1) {
                return url.substring(0, lastSlashIndex);
            }
        }
        return url;
    }

    public static String getRootUrl(String filePath) {
        if (filePath != null) {
            int domainEndIndex = filePath.indexOf('/', 8);
            int fourthSlashIndex = nthIndexOf(filePath, '/', domainEndIndex, 4);

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
        if (inputPath != null && inputPath.startsWith("/")) {
            return inputPath.substring(1);
        }
        return inputPath;
    }

}