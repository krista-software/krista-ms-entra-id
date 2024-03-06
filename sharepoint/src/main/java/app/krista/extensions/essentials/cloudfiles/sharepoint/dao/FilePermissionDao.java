package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DriveSharedPermissionImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.models.DriveItem;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FilePermissionDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePermissionDao.class);
    private final GraphServiceClientProvider clientProvider;
    private final SharePointAttributes sharePointAttributes;
    private final FileSearchUtilityService fileSearchUtilityService;
    private final SearchFileExecutor searchFileExecutor;

    @Inject
    public FilePermissionDao(GraphServiceClientProviderFactory clientProviderFactory,
                             SharePointAttributes sharePointAttributes, FileSearchUtilityService fileSearchUtilityService, SearchFileExecutor searchFileExecutor) {
        this.clientProvider = clientProviderFactory.create();
        this.sharePointAttributes = sharePointAttributes;
        this.fileSearchUtilityService = fileSearchUtilityService;
        this.searchFileExecutor = searchFileExecutor;
    }

    /**
     * Retrieves shared permissions for a file at the specified path, handling path validation, potential errors, and returning shared permission details.
     *
     * @return A FreeForm object containing shared permission information or error messages.
     */
    public FreeForm getSharedPermission(String filePath, boolean useEmail, String accountID) {
        LOGGER.info("Searching for files with filePath: {} ", filePath);
        FreeForm payload = new FreeForm();
        Map<String, Object> mapPayload = new HashMap<>();
        List<DriveSharedPermissionImpl> resultPage = new ArrayList<>();
        searchFileExecutor.getFileInfo(filePath, mapPayload, true, useEmail, accountID);
        DriveItem driveItem = mapPayload.containsKey(Constants.SOURCE_DRIVE_ITEM) ? (DriveItem) mapPayload.get(Constants.SOURCE_DRIVE_ITEM) : null;
        if (driveItem != null && driveItem.name != null && driveItem.name.equals("root")) {
            payload.put(Constants.MESSAGE, Constants.TEXT, Constants.PLEASE_PROVIDE_VALID_SOURCE_PATH);
            return payload;
        }
        if (driveItem != null) {
            resultPage.add(new DriveSharedPermissionImpl(driveItem, clientProvider.getUserRequestBuilder(useEmail, accountID), sharePointAttributes, fileSearchUtilityService));
            ResponseUtil.listOfFileEntityResponse(resultPage.stream().map(app.krista.extensions.essentials.cloudfiles.
                            sharepoint.catalog.entities.File::fromFile)
                    .collect(Collectors.toList()), true, Constants.FILE_FOUND, payload);
        } else {
            payload.put(Constants.MESSAGE, Constants.TEXT, mapPayload.get(Constants.MESSAGE));
        }
        return payload;
    }
}
