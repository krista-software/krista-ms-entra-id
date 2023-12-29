package app.krista.extensions.essentials.cloudfiles.sharepoint.util;

import com.microsoft.graph.models.DriveItem;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

public class FileUtil {

    private FileUtil() {
    }

    public static List<String> getBaseName(String fileName) {
        if (fileName != null) {
            List<String> result = new ArrayList<>();
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                result.add(fileName.substring(0, dotIndex));
                result.add(fileName.substring(dotIndex + 1));
            } else {
                result.add(fileName);
                result.add("");
            }
            return result;
        }
        return List.of();
    }

    public static String getFilePath(String filePath, DriveItem driveItem) {
        String fileName = driveItem.name;
        return filePath + "/" + fileName;
    }

    public static void stringValidator(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static String getParentPath(String relativePath) {
        int lastSlashIndex = relativePath.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            String path = relativePath.substring(0, lastSlashIndex);
            path = path.contains("shared/") ? path.replace("shared", "") : path;
            return path;
        } else {
            throw new IllegalArgumentException("Invalid Path");
        }
    }

    public static String readFile(InputStream fileContent) {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileContent))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            return stringBuilder.toString();
        } catch (IOException cause) {
            throw new RuntimeException(cause);
        }
    }

    public static List<String> getUsersWithPermission(List<Map<String, Object>> share, String targetPermission) {
        List<String> usersWithPermission = new ArrayList<>();
        for (Map<String, Object> user : share) {
            String permission = (String) user.get("Permissions");
            if (permission.equalsIgnoreCase(targetPermission)) {
                usersWithPermission.add((String) user.get("User"));
            }
        }
        return usersWithPermission;
    }

    public static File createTxtFile(String fileName, String fileContent) throws IOException {
        File file = new File(fileName);
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(fileContent);
        }
        return file;
    }

}