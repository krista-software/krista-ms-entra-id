package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * @author Vaibhav Choudhary
 */

public class FileSearchUtilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSearchUtilityService.class);

    public FileSearchUtilityService() {
    }

    public static DriveItem getDriveMetaData(DriveItem driveItem, GraphServiceClientProvider clientProvider) {
        DriveItem driveMetaData = null;
        if (driveItem.parentReference.driveId != null && driveItem.parentReference.id != null) {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(true, null);

            try {
                driveMetaData = graphClient.drives(driveItem.parentReference.driveId).
                        items(driveItem.parentReference.id).buildRequest().expand(Constants.QUERY_PERMISSIONS).get();
            } catch (Exception cause) {
                return driveMetaData;
            }
        }
        return driveMetaData;
    }

    public static String extractParentFolderNameFromUrl(String webUrl) {
        String[] parts = webUrl.split("/");
        if (parts.length >= 2) {
            // Use parts.length - 3 to get the second-to-last part
            String parentFolderName = parts[parts.length - 2];
            // Decode the name if needed
            return decodeUrlComponent(parentFolderName);
        } else {
            return null; // Not enough parts to extract the parent folder name
        }
    }

    private static String decodeUrlComponent(String encodedComponent) {
        try {
            return URLDecoder.decode(encodedComponent, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            // Handle decoding exception if needed
            e.printStackTrace();
            return encodedComponent;
        }
    }

}
