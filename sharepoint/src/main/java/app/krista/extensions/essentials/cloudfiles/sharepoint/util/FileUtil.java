package app.krista.extensions.essentials.cloudfiles.sharepoint.util;

import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import com.microsoft.graph.requests.DriveItemRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class with static methods for common file-related operations, string validation, and file creation.
 */

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    private static final String DD_MM_YYYY_HH_MM_A = "dd/MM/yyyy, hh:mm a";

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
            return relativePath.substring(0, lastSlashIndex);
        } else {
            return null;
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
            throw new IllegalStateException(cause);
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

    public static String getExtractBaseUrl(String url) {
        URI uri = URI.create(url);

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

    public static String getRootRelativeFilePath(String filePath) {
        int rootIndex = filePath.indexOf("/root:");
        String pathAfterRoot = null;
        if (rootIndex != -1) {
            pathAfterRoot = filePath.substring(rootIndex + "/root:".length());

            while (pathAfterRoot.startsWith("/")) {
                pathAfterRoot = pathAfterRoot.substring(1);
            }
        }
        return pathAfterRoot;
    }

    public static String validateTextInput(String input) {
        return input == null ? "" : input;
    }

    public static String relativeFilePath(String fullPath) {
        String[] str = fullPath.split("/");

        if (str.length < 4) {
            return null;
        }

        StringBuilder drivePathBuilder = new StringBuilder("/");
        for (int i = 3; i < str.length; i++) {
            drivePathBuilder.append(str[i]);
            if (i < str.length - 1) {
                drivePathBuilder.append("/");
            }
        }

        return drivePathBuilder.toString();
    }


    public static boolean isEqualDate(Long entityFieldValue, Long fieldValue) {
        long epochTime1 = entityFieldValue; // First epoch time
        long epochTime2 = fieldValue; // Second epoch time

        // Convert epoch times to Date objects
        Date date1 = new Date(epochTime1);
        Date date2 = new Date(epochTime2);

        // Format dates to compare only date components (day, month, year)
        SimpleDateFormat dateFormat = new SimpleDateFormat(DD_MM_YYYY_HH_MM_A);

        String formattedDate1 = dateFormat.format(date1);
        String formattedDate2 = dateFormat.format(date2);
        return formattedDate1.equals(formattedDate2);
    }

    public static boolean isPathContainsInput(String path, String input) {
        // Split path and input into tokens
        String[] pathTokens = path.split("/");
        String[] inputTokens = input.split("/");

        // Remove leading and trailing empty tokens
        pathTokens = removeEmptyTokens(pathTokens);
        inputTokens = removeEmptyTokens(inputTokens);

        // Check if the number of tokens in path is at least equal to the number of tokens in input
        if (pathTokens.length < inputTokens.length) {
            return false;
        }

        // Compare tokens
        for (int i = 0; i < inputTokens.length; i++) {
            if (!pathTokens[i].equals(inputTokens[i])) {
                return false;
            }
        }

        return true;
    }

    private static String[] removeEmptyTokens(String[] tokens) {
        return Arrays.stream(tokens)
                .filter(token -> !token.isEmpty())
                .toArray(String[]::new);
    }

    public static DriveItemRequestBuilder getRootUserRequestBuilder(GraphServiceClientProvider provider, String driveID) {
        return getUserRequestBuilder(provider).drives(driveID).root();
    }

    public static GraphServiceClient<Request> getUserRequestBuilder(GraphServiceClientProvider provider) {
        return provider.getUserRequestBuilder(null, null);
    }

    public static String driveIdPath(String fullPath) {
        try {
            String[] str = fullPath.split("/");
            return "/" + str[1] + "/" + (str[2]);
        } catch (RuntimeException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
            return Constants.CONTENT;
        }
    }

    public static String getLastPathElement(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    public static void failedToReadTextFile(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.FAILED_TO_READ_TEXTFILE);
        payload.put(Constants.SUCCESS, false);
    }

    public static String preparePath(String filePath) {
        if (filePath != null) {
            return filePath.replace("%20", " ");
        }
        return null;
    }

    public static String extractSecondLastFolder(String path) {
        String[] parts = path.replaceFirst("^(https?://)", "").split("/");
        int length = parts.length;
        if (length > 2) {
            return parts[length - 2];
        }
        return "";
    }

    public static String extractSiteID(String input) {
        Pattern pattern = Pattern.compile("SPO:SPO_([^@]+)@");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}