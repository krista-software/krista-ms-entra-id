package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSearchUtilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSearchUtilityService.class);

    public FileSearchUtilityService() {
    }

    public static DriveItem getDriveMetaData(DriveItem driveItem, GraphServiceClientProvider clientProvider) {
        DriveItem driveMetaData = null;
        if (driveItem.parentReference.driveId != null && driveItem.parentReference.id != null) {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(true, null);

            driveMetaData = graphClient.drives(driveItem.parentReference.driveId).
                    items(driveItem.parentReference.id).buildRequest().expand("permissions").get();
        }
        return driveMetaData;
    }

    public static String extractParentFolderNameFromUrl(DriveItem driveItem) {
        String[] parts = driveItem.webUrl.split("/");
        String lastPart = parts[parts.length - 1];
        String decodedName = lastPart.replace("%20", " ");
        return decodedName;
    }

}
