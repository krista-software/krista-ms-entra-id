package app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util;

import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Utility service class for file-related search operations using Microsoft Graph API.
 * This class provides methods to retrieve drive metadata, extract information from URLs,
 * and obtain details such as absolute path, parent folder, and file extension.
 *
 * @author Vaibhav Choudhary
 */

@Service
public class FileSearchUtilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSearchUtilityService.class);

    private FileSearchUtilityService() {
    }

    public static String extractParentFolderNameFromUrl(String webUrl) {
        String[] parts = webUrl.split("/");
        if (parts.length >= 2) {
            String parentFolderName = parts[parts.length - 2];
            return decodeUrlComponent(parentFolderName);
        } else {
            return null;
        }
    }

    private static String decodeUrlComponent(String encodedComponent) {
        try {
            return URLDecoder.decode(encodedComponent, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException cause) {
            return encodedComponent;
        }
    }

    public static String getAbsolutePath(DriveItem driveMetaData, DriveItem driveItem) {
        if (driveItem != null && driveItem.webUrl != null && !driveItem.webUrl.contains(".aspx")) {
            return driveItem.webUrl;
        } else if (driveMetaData != null && driveMetaData.webUrl != null && driveItem != null) {
            if (driveMetaData.webUrl.contains(driveMetaData.name)) {
                return driveMetaData.webUrl;
            }
            return driveMetaData.webUrl + (driveItem.name != null ? "/" + driveItem.name : "");
        }
        return null;
    }

    public static String getParentFolder(DriveItem driveMetaData, DriveItem driveItem) {
        if (driveMetaData != null && driveMetaData.parentReference != null
                && driveMetaData.parentReference.name != null) {
            return driveMetaData.parentReference.name;
        } else {
            String path = getAbsolutePath(driveMetaData, driveItem);
            if (path != null && !path.contains(".aspx")) {
                return extractParentFolderNameFromUrl(path);
            } else {
                return null;
            }
        }
    }

    public static String getExtension(DriveItem driveMetaData, DriveItem driveItem) {
        if (driveMetaData != null && driveMetaData.folder == null) {
            return FileUtil.getBaseName(driveItem.name != null ? driveItem.name : null).get(1);
        } else {
            return null;
        }
    }

    public static String extractLastNameFromPath(String path) {
        if (path != null && !path.isEmpty()) {
            String[] pathSegments = path.split("/");
            return pathSegments[pathSegments.length - 1];
        } else {
            return null;
        }
    }

    public DriveItem getDriveMetaData(DriveItem driveItem, GraphServiceClient<Request> graphClient) {
        DriveItem driveMetaData = null;
        if (driveItem != null && driveItem.parentReference != null && driveItem.parentReference.driveId != null) {

            try {
                driveMetaData = graphClient.drives(driveItem.parentReference.driveId).
                        items(driveItem.id).buildRequest().expand(Constants.QUERY_PERMISSIONS).get();
            } catch (GraphServiceException | IllegalArgumentException cause) {
                LOGGER.error("Error while fetching Drive Item {} {} :", cause, cause.getMessage());
            }
        }
        return driveMetaData;
    }

}

