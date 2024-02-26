package app.krista.extensions.essentials.cloudfiles.sharepoint.util;

import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import com.microsoft.graph.requests.DriveItemRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class with static methods for common file-related operations, string validation, and file creation.
 */

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

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

    public static String getParentPath(String relativePath) {
        int lastSlashIndex = relativePath.lastIndexOf(File.separator);
        if (lastSlashIndex != -1) {
            String path = relativePath.substring(0, lastSlashIndex);
            path = path.contains("shared/") ? path.replace("shared", "") : path;
            return path;
        } else {
            throw new IllegalArgumentException(Constants.INVALID_PATH);
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
            String permission = (String) user.get(Constants.PERMISSIONS);
            if (permission.equalsIgnoreCase(targetPermission)) {
                usersWithPermission.add((String) user.get(Constants.USER));
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

    public static boolean hasFileExtension(String path) {
        Path filePath = Paths.get(path);
        return filePath.getFileName() != null && filePath.getFileName().toString().contains(".");
    }

    public static String getExtractBaseUrl(String URL) {
        URI uri = URI.create(URL);

        // Extract the subdomain using host() and substring() methods
        String subdomain = uri.getHost();
        int index = subdomain.indexOf("-");
        if (index != -1) {
            subdomain = subdomain.substring(0, index); // Remove any trailing parts
        }
        return subdomain;
    }

    public static DriveItemRequestBuilder getUserRequestBuilderWithItemId(GraphServiceClientProvider provider, String driveId, String itemId) {
        return driveId != null
                ? provider.getUserRequestBuilder(null, null).drives(driveId).items(itemId)
                : provider.getUserRequestBuilder(null, null).drive().items(itemId);
    }

    public static String getRelativeFilePath(String filePath) {
        int rootIndex = filePath.indexOf("/root:");
        String pathAfterRoot = null;
        if (rootIndex != -1) {
            pathAfterRoot = filePath.substring(rootIndex + "/root:".length());

            while (pathAfterRoot.startsWith("/")) {
                pathAfterRoot = pathAfterRoot.substring(1);
            }
            return pathAfterRoot;
        } else {
            return pathAfterRoot;
        }
    }

    public static boolean isPersonalDrive(String target) {
        return target.startsWith(Constants.DRIVE_ROOT) || target.startsWith("/" + Constants.DRIVE_ROOT);
    }

    public static DriveItemRequestBuilder getRootUserRequestBuilder(GraphServiceClientProvider provider) {
        return provider.getUserRequestBuilder(null, null).drive().root();
    }

    public static boolean checkDriveIDContains(String relativePath) {
        return relativePath.matches(".*/drives/b![^/]+/root:/.*") || relativePath.matches(".*drives/b![^/]+/root:/.*");
    }

    public static List<String> getDriveIDAndPath(String relativePath) {

        String[] stream = relativePath.trim().split("/");

        String path = Arrays.stream(stream, 4, stream.length)
                .collect(Collectors.joining("/"));

        return List.of(stream[2], path);
    }

    public static DriveItemRequestBuilder getSharedUserRequestBuilder(GraphServiceClientProvider provider, String driveId) {
        return provider.getUserRequestBuilder(null, null).drives(driveId).root();
    }

    public static void failedToReadTextFile(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.FAILED_TO_READ_TEXTFILE);
        payload.put(Constants.SUCCESS, false);
    }

    public static java.io.File fromFile(app.krista.model.base.File file) {
        if (file == null) {
            throw new IllegalArgumentException(Constants.NO_FILE_TO_PARSE);
        }
        try {
            return MediaClient.create().download(MediaClient.createMediaUrl(file.getMediaId()), file.getFileName());
        } catch (IOException cause) {
            LOGGER.error(" {} : {} " + Constants.ERROR_MESSAGE, cause.getMessage());
            throw new IllegalArgumentException(cause);
        }
    }
}