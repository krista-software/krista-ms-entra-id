package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.ManagementUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DrivePermissionImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * DAO class for managing folders in Microsoft Graph using the Graph API.
 * Provides methods to retrieve a list of folders, validate folder existence,
 * and handle exceptions and response messages.
 *
 * @author Vaibhav Choudhary
 */

@Service
public class FolderManagementDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(FolderManagementDao.class);
    private final GraphServiceClientProvider provider;
    private final ManagementUtilityService managementUtilityService;
    private final FileSearchUtilityService fileSearchUtilityService;

    @Inject
    public FolderManagementDao(GraphServiceClientProviderFactory factory,ManagementUtilityService managementUtilityService,
                               FileSearchUtilityService fileSearchUtilityService) {
        this.provider = factory.create();
        this.managementUtilityService = managementUtilityService;
        this.fileSearchUtilityService = fileSearchUtilityService;
    }

    /**
     * Retrieves a list of folders at the specified path, optionally filtered by search criteria, handling path validation, folder existence checks, and potential errors.
     * @return A FreeForm object containing the list of folders or error messages.
     */
    public FreeForm getListOfFolders(String relativePath, String search, boolean useEmail, String accountID) {
        FreeForm payload = new FreeForm();
        try {
            List<FileService> files = new ArrayList<>();
            if (relativePath == null || relativePath.isEmpty()) {
                ResponseUtil.getInfoResponse(false, Constants.PROVIDE_VALID_SOURCE_PATH, payload);
            }
            relativePath = managementUtilityService.getUpdatedFilePath(relativePath);
            DriveItem driveItem = checkIfFileExist(relativePath, payload, useEmail, accountID);
            if (payload.get(Constants.SUCCESS) == Boolean.TRUE && driveItem != null && driveItem.parentReference != null) {
                relativePath = DriveUtil.getRelativePath(relativePath);
                getRootFilesByDriveIDPath(driveItem.parentReference.driveId, relativePath, files, useEmail, accountID, search, payload);
            } else {
                ResponseUtil.getInfoResponse(false, Constants.PROVIDE_VALID_SOURCE_PATH, payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage())
                    .contains("itemNotFound") ? Constants.FILE_NOT_FOUND : cause.getMessage(), payload);
        }
        return payload;
    }

    public void getRootFilesByDriveIDPath(String driveID, String relativePath, List<FileService> files,
                                          boolean useEmail, String accountID, String search, FreeForm payload) {
        try {
            GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(useEmail, accountID);
            DriveItem driveItem = graphClient.drives(driveID).root().itemWithPath(relativePath).buildRequest().get();
            if (driveItem != null && driveItem.id != null && driveItem.folder != null) {
                DriveItemCollectionPage page = graphClient.drives(driveID).root().itemWithPath(relativePath).children()
                        .buildRequest().get();
                while (page != null && !page.getCurrentPage().isEmpty()) {
                    for (DriveItem driveInPage : page.getCurrentPage()) {
                        if (search != null && !search.isEmpty() && driveInPage.folder != null &&
                                driveInPage.name != null && driveInPage.name.contains(search)) {
                            files.add(new DrivePermissionImpl(driveInPage, graphClient, fileSearchUtilityService));
                        }
                        if (search != null && search.isBlank() && driveInPage.folder != null) {
                            files.add(new DrivePermissionImpl(driveInPage, graphClient, fileSearchUtilityService));
                        }
                    }
                    if (page.getNextPage() != null) {
                        page = page.getNextPage().buildRequest().get();
                    } else {
                        ResponseUtil.listOfFileEntityResponse(files.stream().map(CatalogTypes::fromFile)
                                .collect(Collectors.toList()), true, " ", payload);
                        break;
                    }
                }
            } else {
                ResponseUtil.getInfoResponse(false, Constants.PROVIDE_VALID_SOURCE_PATH, payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage())
                    .contains("itemNotFound") ? "File Not Found" : cause.getMessage(), payload);
        }
    }


    private DriveItem checkIfFileExist(String sourcePath, FreeForm payload, boolean useEmail, String accountID) {
        DriveItem driveItem = new DriveItem();
        try {
            if (sourcePath != null) {
                GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(useEmail, accountID);
                driveItem = managementUtilityService.getFileByAbsolutePath(sourcePath, graphClient);
                if (driveItem != null && driveItem.parentReference != null && driveItem.parentReference.driveId != null) {
                    ResponseUtil.addFileSize(driveItem.size != null ? driveItem.size : 0L, payload);
                    ResponseUtil.getInfoResponse(true, Constants.FILE_FOUND, payload);
                } else {
                    ResponseUtil.getInfoResponse(false, Constants.INVALID_FOLDER_PATH, payload);
                }
            } else {
                ResponseUtil.getInfoResponse(false, Constants.INVALID_FOLDER_PATH, payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage())
                    .contains("itemNotFound") ? Constants.FILE_NOT_FOUND : cause.getMessage(), payload);
            return new DriveItem();
        }
        return driveItem;
    }

}
