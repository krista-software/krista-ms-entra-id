package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.CopyFileUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.ManagementUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.FileImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.RestClient;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.google.gson.JsonObject;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The FileTransferService class provides methods for copying and moving files within a Microsoft Graph-driven system.
 * It utilizes the GraphServiceClientProvider for interacting with the Microsoft Graph API.
 * This class also collaborates with several utility services such as OperationsManagementDao,
 * ManagementUtilityService, and CopyFileUtilityService to achieve its functionality.
 *
 * @author Vaibhav Choudhary
 */

@Service
public class FileTransferService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileTransferService.class);
    private final GraphServiceClientProvider provider;
    private final OperationsManagementDao operationsManagementDao;
    private final ManagementUtilityService managementUtilityService;
    private final CopyFileUtilityService copyFileUtilityService;
    private final FileSearchUtilityService fileSearchUtilityService;

    @Inject
    public FileTransferService(GraphServiceClientProviderFactory factory,
                               OperationsManagementDao operationsManagementDao,
                               ManagementUtilityService managementUtilityService,
                               CopyFileUtilityService copyFileUtilityService,
                               FileSearchUtilityService fileSearchUtilityService) {
        this.provider = factory.create();
        this.operationsManagementDao = operationsManagementDao;
        this.managementUtilityService = managementUtilityService;
        this.copyFileUtilityService = copyFileUtilityService;
        this.fileSearchUtilityService = fileSearchUtilityService;
    }

    /**
     * Copies a file from the source path to the target path, handling potential replacements, folder creation, and errors.
     * @return A Map containing status and result details, including success/failure indicators and error messages.
     */
    public Map<String, Object> copyFile(String source, String target, Boolean replace, Boolean createFolder) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (validateReplaceAndCreateFolderParameters(replace, createFolder, payload)) {
                copyFileUtilityService.validateSourceAndTargetPaths(source, target, payload);
                if ((boolean) payload.get(Constants.SUCCESS)) {
                    source = managementUtilityService.getUpdatedFilePath(source);
                    DriveItem driveItemSource = managementUtilityService.checkIfFileExist(source, payload);

                    if ((boolean) payload.get(Constants.SUCCESS)) {
                        target = managementUtilityService.getUpdatedFilePath(target);
                        DriveItem driveItemTarget = createOrCheckTargetItem(target, createFolder, payload, replace);

                        if ((boolean) payload.get(Constants.IS_FOLDER_EXIST)) {
                            buildCopyFileRequest(driveItemSource, replace, driveItemTarget, payload, target, source);
                        }
                    }
                }
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            copyFileUtilityService.handleCopyFileException(cause, payload);
        }
        return payload;
    }

    private void buildCopyFileRequest(DriveItem driveItemSource, Boolean replace, DriveItem driveItemTarget,
                                      Map<String, Object> payload, String target, String source) {
        try {
            String endpoint = driveItemSource.id + "/copy";
            endpoint = addConflictBehaviorQueryParam(endpoint, replace);

            Response copyResponse = getRestClient(driveItemTarget.parentReference.driveId)
                    .executeHttpMethods(endpoint, Constants.POST_METHOD, createRequestBody(driveItemTarget.id));

            processResponse(copyResponse, payload, driveItemSource, target, source);
        } catch (GraphServiceException | IOException | IllegalArgumentException cause) {
            copyFileUtilityService.handleCopyFileException(cause, payload);
        }
    }

    private boolean validateReplaceAndCreateFolderParameters(Boolean replace, Boolean createFolder,
                                                             Map<String, Object> payload) {
        if (replace != null && createFolder != null && replace && createFolder) {
            payload.put(Constants.MESSAGE, Constants.PLEASE_SPECIFY_REPLACE_OR_CREATE);
            payload.put(Constants.SUCCESS, false);
            return false;
        }
        return true;
    }

    private DriveItem createOrCheckTargetItem(String target, Boolean createFolder,
                                              Map<String, Object> payload, Boolean replace) {
        DriveItem driveItemTarget;
        if (createFolder) {
            driveItemTarget = createFolderInPath(target, payload);
        } else {
            driveItemTarget = checkIfTargetFolderExist(target, payload);
        }

        if (!(boolean) payload.get(Constants.IS_FOLDER_EXIST)) {
            payload.put(Constants.MESSAGE, Constants.PROVIDE_VALID_TARGET_PATH);
            payload.put(Constants.SUCCESS, false);
        }
        return driveItemTarget;
    }

    private String addConflictBehaviorQueryParam(String endpoint, Boolean replace) {
        return replace ? endpoint + "?@microsoft.graph.conflictBehavior=replace" : endpoint;
    }

    private DriveItem checkIfTargetFolderExist(String target, Map<String, Object> response) {
        if (target != null) {
            try {
                GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(null, null);
                DriveItem driveItem = managementUtilityService.getFileByAbsolutePath(target, graphClient);
                boolean isFolderExist = driveItem != null && driveItem.parentReference != null;
                response.put(Constants.IS_FOLDER_EXIST, isFolderExist);
                return isFolderExist ? driveItem : new DriveItem();
            } catch (GraphServiceException | IllegalArgumentException cause) {
                copyFileUtilityService.handleFolderExistenceCheckException(cause, response);
                return new DriveItem();
            }
        } else {
            response.put(Constants.IS_FOLDER_EXIST, false);
            return new DriveItem();
        }
    }

    private RestClient getRestClient(String driveId) {
        return provider.getRestClient(driveId);
    }

    public RequestBody createRequestBody(String folderId) {
        String payload = getParentReferenceJson(folderId);
        MediaType jsonMediaType = MediaType.parse(Constants.APPLICATION_JSON);
        return RequestBody.create(jsonMediaType, payload);
    }

    private String getParentReferenceJson(String folderId) {
        JsonObject jsonObject = new JsonObject();
        JsonObject parentReference = new JsonObject();
        parentReference.addProperty(Constants.ID, folderId);
        jsonObject.add(Constants.PARENT_REFERENCE, parentReference);
        return jsonObject.toString();
    }

    private void processResponse(Response statusResponse, Map<String, Object> payload, DriveItem driveItemSource, String target, String source) throws IOException {
        if (statusResponse.code() == Constants.HTTP_STATUS_OK || statusResponse.code() == Constants.HTTP_STATUS_ACCEPTED) {
            GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(null, null);

            DriveItem driveItem = graphClient.drives(driveItemSource.parentReference.driveId).items(driveItemSource.id)
                    .buildRequest().get();

            payload.put(Constants.FILE, CatalogTypes.fromFile(new FileImpl(driveItem, graphClient, source, target, fileSearchUtilityService)));
            payload.put(Constants.MESSAGE, Constants.FILE_COPIED_SUCCESSFULLY);
            payload.put(Constants.SUCCESS, true);
        } else if (statusResponse.code() == Constants.HTTP_STATUS_CONFLICT) {
            payload.put(Constants.MESSAGE, Constants.FILE_WITH_THIS_NAME_ALREADY_EXISTS);
            payload.put(Constants.SUCCESS, false);
        } else {
            payload.put(Constants.MESSAGE, Constants.THERE_IS_SOME_ISSUE_WITH_FILE);
            payload.put(Constants.SUCCESS, false);
        }
    }

    /**
     * Moves a file from the source path to the target path, handling path validation, file existence checks, target folder validation, and potential errors.
     * @return A Map containing status and result details, including success/failure indicators and error messages.
     */
    public Map<String, Object> moveFile(String source, String target) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (copyFileUtilityService.validateSourceAndTargetPathsForMove(source, target, payload)) {
                source = managementUtilityService.getUpdatedFilePath(source);
                DriveItem driveItemSource = managementUtilityService.checkIfFileExist(source, payload);

                if ((boolean) payload.get(Constants.SUCCESS)) {
                    target = managementUtilityService.getUpdatedFilePath(target);
                    DriveItem driveItemTarget = validateTargetFolderAndGetDriveItem(target, payload);

                    if ((boolean) payload.get(Constants.IS_FOLDER_EXIST)) {
                        moveFileToTarget(driveItemSource, driveItemTarget, target, source, payload);
                    } else {
                        copyFileUtilityService.handleInvalidTargetPath(payload);
                    }
                } else {
                    copyFileUtilityService.handleInvalidSourcePath(payload);
                }
            }
        } catch (GraphServiceException | IOException | IllegalArgumentException cause) {
            copyFileUtilityService.handleMoveFileException(cause, payload);
        }
        return payload;
    }

    private void moveFileToTarget(DriveItem driveItemSource, DriveItem driveItemTarget, String target, String source,
                                  Map<String, Object> payload) throws IOException {
        String endpoint = buildMoveEndpoint(driveItemSource, driveItemTarget);

        Response moveResponse = getRestClient(driveItemTarget.parentReference.driveId).
                executeHttpMethods(endpoint, Constants.PATCH_METHOD,
                        createRequestBody(driveItemTarget.id));

        processResponse(moveResponse, payload, driveItemSource, target, source);
    }


    private DriveItem validateTargetFolderAndGetDriveItem(String target, Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        DriveItem driveItemTarget = checkIfTargetFolderExist(target, response);
        payload.put(Constants.IS_FOLDER_EXIST, response.get(Constants.IS_FOLDER_EXIST));

        return driveItemTarget;
    }

    private String buildMoveEndpoint(DriveItem driveItemSource, DriveItem driveItemTarget) {
        String endpoint = driveItemSource.id + "?@microsoft.graph.conflictBehavior=replace";
        return endpoint;
    }

    private DriveItem createFolderInPath(String target, Map<String, Object> payload) {
        DriveItem driveItem = new DriveItem();
        try {
            copyFileUtilityService.validateTargetPath(target, payload);

            if ((boolean) payload.get(Constants.SUCCESS)) {
                String absolutePathBuilder = DriveUtil.getRootUrl(target);
                DriveItem driveItemTarget = copyFileUtilityService.checkRootTargetFolderExist(absolutePathBuilder, payload);

                if ((boolean) payload.get(Constants.IS_FOLDER_EXIST)) {
                    target = DriveUtil.removeFirstSlash(DriveUtil.getRelativePath(target));
                    driveItem = createFoldersInTargetPath(target, absolutePathBuilder, driveItemTarget, payload);
                } else {
                    copyFileUtilityService.handleInvalidTargetPath(payload);
                    return new DriveItem();
                }
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            copyFileUtilityService.handleInvalidTargetPath(payload);
        }
        return driveItem;
    }

    private DriveItem createFoldersInTargetPath(String target, String absolutePathBuilder, DriveItem driveItemTarget,
                                                Map<String, Object> payload) {
        DriveItem driveItem = new DriveItem();
        String[] folders = target.split("/");

        StringBuilder relativePathBuilder = new StringBuilder();
        for (String folderName : folders) {
            Map<String, Object> check = new HashMap<>();
            absolutePathBuilder = absolutePathBuilder + "/" + folderName;
            relativePathBuilder.append("/").append(folderName);

            // Check if the folder exists
            driveItem = checkIfTargetFolderExist(absolutePathBuilder.toString(), check);

            if (!(boolean) check.get(Constants.IS_FOLDER_EXIST)) {
                driveItem = createDriveItemForFolder(folderName, absolutePathBuilder, relativePathBuilder.toString(),
                        driveItemTarget, payload);
                return driveItem;
            }
        }
        return driveItem;
    }

    private DriveItem createDriveItemForFolder(String folderName, String absolutePathBuilder, String relativePath,
                                               DriveItem driveItemTarget, Map<String, Object> payload) {
        DriveItem driveItem;
        driveItem = provider.getUserRequestBuilder(null, null)
                .drives(driveItemTarget.parentReference.driveId).root()
                .itemWithPath(FileUtil.getParentPath(relativePath))
                .children().buildRequest().post(DriveUtil.getDriveItemForItem(folderName));

        payload.put("isFileExist", true);
        return driveItem;
    }

}
