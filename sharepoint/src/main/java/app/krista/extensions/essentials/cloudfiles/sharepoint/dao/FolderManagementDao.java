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
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
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
                relativePath = getRelativePath(relativePath);
                getRootFilesByDriveIDPath(driveItem.parentReference.driveId, relativePath, files, useEmail, accountID, search, payload);
            } else {
                ResponseUtil.getInfoResponse(false, Constants.INVALID_FOLDER_PATH, payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(cause.getMessage());
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage())
                    .contains("itemNotFound") ? Constants.INVALID_FOLDER_PATH : cause.getMessage(), payload);
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
                ResponseUtil.getInfoResponse(false, Constants.INVALID_FOLDER_PATH, payload);
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


    public static String getRelativePath(String fullUrl) {
        try {
            String escapedUrl = escapeSpaces(fullUrl);
            URI uri = new URI(escapedUrl);
            String path = uri.getPath();

            String[] pathSegments = getPathSegments(path);

            int startIndex = findDocumentsIndex(pathSegments);
            if (startIndex != -1) {
                startIndex++;
                return buildDynamicPath(pathSegments, startIndex);
            } else {
                return null;
            }
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static String escapeSpaces(String url) {
        return url.replace(" ", "%20");
    }

    private static String[] getPathSegments(String path) {
        return path.split(java.io.File.separator.equals("\\") ? "\\\\" : java.io.File.separator);
    }

    private static int findDocumentsIndex(String[] pathSegments) {
        for (int i = pathSegments.length - 1; i >= 0; i--) {
            if (pathSegments[i].equalsIgnoreCase(Constants.DOCUMENTS) ||
                    pathSegments[i].equalsIgnoreCase(Constants.SHARED_DOCUMENTS)) {
                return i;
            }
        }
        return -1;
    }

    private static String buildDynamicPath(String[] pathSegments, int startIndex) {
        StringBuilder dynamicPath = new StringBuilder(java.io.File.separator);
        for (int i = startIndex; i < pathSegments.length; i++) {
            dynamicPath.append(pathSegments[i]);
            if (i < pathSegments.length - 1) {
                dynamicPath.append(File.separator);
            }
        }
        return dynamicPath.toString();
    }



}
