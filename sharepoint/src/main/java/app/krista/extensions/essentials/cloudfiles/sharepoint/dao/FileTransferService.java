package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.FileImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.RestClient;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import com.google.gson.JsonObject;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemRequestBuilder;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    private static final String MICROSOFT_GRAPH_CONFLICT_BEHAVIOR_REPLACE = "?@microsoft.graph.conflictBehavior=replace";
    private final GraphServiceClientProvider provider;
    private final FileSearchUtilityService fileSearchUtilityService;
    private final SearchFileExecutor searchFileExecutor;

    @Inject
    public FileTransferService(GraphServiceClientProviderFactory factory,
                               FileSearchUtilityService fileSearchUtilityService, SearchFileExecutor searchFileExecutor) {
        this.provider = factory.create();
        this.fileSearchUtilityService = fileSearchUtilityService;
        this.searchFileExecutor = searchFileExecutor;
    }

    /**
     * Copies a file from the source path to the target path, handling potential replacements, folder creation, and errors.
     *
     * @return A Map containing status and result details, including success/failure indicators and error messages.
     */
    public Map<String, Object> copyFile(String source, String target, Boolean replace, boolean createFolder) {
        Map<String, Object> payload = new HashMap<>();
        try {
            searchFileExecutor.getFileInfo(source, payload, true);
            if (payload.get(Constants.SUCCESS) != null && payload.get(Constants.SUCCESS).equals(Boolean.TRUE)) {
                if (payload.get(Constants.IS_SOURCE_FILE_EXIST) != null && payload.get(Constants.IS_SOURCE_FILE_EXIST).equals(Boolean.TRUE)) {
                    DriveItem sourceDriveItem = (DriveItem) payload.get(Constants.SOURCE_DRIVE_ITEM);
                    Map<String, Object> targetPayload = new HashMap<>();

                    String targetRelativeFilePath = FileUtil.relativeFilePath(target);
                    String targetDriveIdPath = FileUtil.driveIdPath(target);
                    String updatedTarget = searchFileExecutor.getUpdatedFilePath(targetDriveIdPath);
                    searchFileExecutor.setPayloadIfInitialPathValidOrNot(updatedTarget, targetPayload, null, null);
                    String targetDriveId = targetPayload.get(Constants.DRIVE_ID) != null ? targetPayload.get(Constants.DRIVE_ID).toString() : null;
                    if(targetDriveId == null){
                        payload.put(Constants.MESSAGE,Constants.PLEASE_PROVIDE_VALID_TARGET_PATH);
                        payload.put(Constants.SUCCESS,false);
                        return payload;
                    }
                    if (createFolder) {
                        createFolderInPath(targetDriveId, targetRelativeFilePath);
                    }
                    searchFileExecutor.getFileInfo(target, payload, false);
                    if (payload.get(Constants.IS_TARGET_FILE_EXIST) != null && payload.get(Constants.IS_TARGET_FILE_EXIST).equals(Boolean.TRUE)) {
                        DriveItem targetDriveItem = (DriveItem) payload.get(Constants.TARGET_DRIVE_ITEM);
                        executeCopy(sourceDriveItem, targetDriveItem, replace, payload);
                        return payload;

                    } else {
                        ResponseUtil.errorResponse(false, Constants.PLEASE_PROVIDE_VALID_TARGET_PATH, payload);
                        return payload;
                    }

                }
            }
        } catch (GraphServiceException | IOException | IllegalArgumentException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage(), cause);
            ResponseUtil.errorResponse(false, Objects.requireNonNull(cause.getMessage())
                    .contains(Constants.ITEM_NOT_FOUND)
                    ? Constants.FILE_NOT_FOUND
                    : Constants.ERROR_OCCURRED_DURING_COPYING_FILE, payload);
        }
        return payload;
    }

    /**
     * Moves a file from the source path to the target path, handling path validation, file existence checks, target folder validation, and potential errors.
     *
     * @return A Map containing status and result details, including success/failure indicators and error messages.
     */
    public Map<String, Object> moveFile(String source, String target) {
        Map<String, Object> payload = new HashMap<>();
        try {
            searchFileExecutor.getFileInfo(source, payload, true);
            if (payload.get(Constants.SUCCESS) != null && payload.get(Constants.SUCCESS).equals(Boolean.TRUE)) {
                if (payload.get(Constants.IS_SOURCE_FILE_EXIST) != null && payload.get(Constants.IS_SOURCE_FILE_EXIST).equals(Boolean.TRUE)) {
                    DriveItem sourceDriveItem = (DriveItem) payload.get(Constants.SOURCE_DRIVE_ITEM);
                    Map<String, Object> targetPayload = new HashMap<>();
                    searchFileExecutor.getFileInfo(target, targetPayload, false);
                    if (targetPayload.get(Constants.IS_TARGET_FILE_EXIST) != null && targetPayload.get(Constants.IS_TARGET_FILE_EXIST).equals(Boolean.TRUE)) {
                        DriveItem targetDriveItem = (DriveItem) targetPayload.get(Constants.TARGET_DRIVE_ITEM);
                        payload.put(Constants.TARGET_ROOT_PATH,targetPayload.get(Constants.TARGET_ROOT_PATH));
                        executeMove(sourceDriveItem, targetDriveItem, payload);
                        return payload;

                    } else {
                        ResponseUtil.errorResponse(false, Constants.PLEASE_PROVIDE_VALID_TARGET_PATH, payload);
                        return payload;
                    }

                }
            }
        } catch (GraphServiceException | IOException | IllegalArgumentException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage(), cause);
            ResponseUtil.errorResponse(false, Objects.requireNonNull(cause.getMessage())
                    .contains(Constants.ITEM_NOT_FOUND)
                    ? Constants.FILE_NOT_FOUND
                    : Constants.ERROR_OCCURRED_DURING_COPYING_FILE, payload);
        }
        return payload;
    }

    private RestClient getRestClient(String driveId) {
        return provider.getRestClient(driveId);
    }

    public RequestBody createRequestBody(String folderId, String driveId, String name) {
        String payload = getParentReferenceJson(folderId, driveId, name);
        MediaType jsonMediaType = MediaType.parse(Constants.APPLICATION_JSON);
        return RequestBody.create(payload, jsonMediaType);
    }

    private String getParentReferenceJson(String folderId, String driveId, String name) {
        JsonObject jsonObject = new JsonObject();
        JsonObject parentReference = new JsonObject();
        parentReference.addProperty("driveId", driveId);
        parentReference.addProperty(Constants.ID, folderId);
        jsonObject.addProperty("name", name);
        jsonObject.add(Constants.PARENT_REFERENCE, parentReference);
        return jsonObject.toString();
    }


    private void prepareCopyResponse(Response response, String driveId, Map<String, Object> payload) throws IOException {
        if (response != null && response.code() == 202) {
            try (Response statusResponse = getRestClient(null).get(response.header("Location"))) {
                if (statusResponse.code() == 200) {
                    final ResponseBody statusResponseBody = statusResponse.body();
                    if (statusResponseBody != null) {
                        String responseBodyString = statusResponseBody.string(); // Read the response body as a string
                        JsonObject jsonObject = Constants.GSON.fromJson(responseBodyString, JsonObject.class);

                        if (jsonObject.isJsonObject()) {
                            getCopyPayLoad(driveId, payload, jsonObject);
                        }
                    }
                }
            }
        } else {
            ResponseUtil.errorResponse(false, Constants.ERROR_DURING_SERVICE, payload);
        }
    }

    private void getCopyPayLoad(String driveId, Map<String, Object> payload, JsonObject jsonObject) {
        String status = jsonObject.getAsJsonPrimitive("status").getAsString();
        if ("completed".equalsIgnoreCase(status)) {
            DriveItem driveItem = FileUtil
                    .getUserRequestBuilderWithItemId(provider
                            , driveId
                            , jsonObject.getAsJsonPrimitive("resourceId").getAsString().trim())
                    .buildRequest().expand(Constants.PERMISSIONS).get();
            String sitePath = payload.get(Constants.TARGET_ROOT_PATH).toString();
            ResponseUtil.fileEntityResponse(File
                            .fromFile(new FileImpl(driveItem, provider.getUserRequestBuilder(null, null), sitePath, fileSearchUtilityService))
                    , true, Constants.FILE_HAS_BEEN_COPIED, payload);
        } else if ("inProgress".equalsIgnoreCase(status)) {
            ResponseUtil.errorResponse(true, Constants.COPY_IN_PROGRESS, payload);
        } else if ("failed".equalsIgnoreCase(status)) {
            ResponseUtil.errorResponse(false, Constants.FILE_ALREADY_EXIST, payload);
        }
    }

    private void prepareMovedResponse(Response moveResponse, String driveId, Map<String, Object> payload) {
        if (moveResponse != null && moveResponse.code() == 200) {
            try {
                String itemID = moveResponse.request().url()
                        .pathSegments()
                        .get(moveResponse.request().url().pathSegments().size() - 1);
                DriveItem driveItem = FileUtil.getUserRequestBuilderWithItemId(provider, driveId, itemID)
                        .buildRequest().expand(Constants.PERMISSIONS).get();
                String sitePath = payload.get(Constants.TARGET_ROOT_PATH).toString();
                ResponseUtil.fileEntityResponse(File
                                .fromFile(new FileImpl(driveItem, provider.getUserRequestBuilder(null, null), sitePath, fileSearchUtilityService))
                        , true, Constants.FILE_HAS_BEEN_MOVED, payload);
            } catch (GraphServiceException | IllegalArgumentException cause) {
                LOGGER.info(cause.getMessage(), cause);
                ResponseUtil.errorResponse(false, Objects.requireNonNull(cause.getMessage())
                        .contains(Constants.ITEM_NOT_FOUND)
                        ? Constants.FILE_NOT_FOUND : Constants.ERROR_OCCURRED_DURING_MOVING_FILE, payload);
            }
        } else if (moveResponse != null && moveResponse.code() == 409) {
            ResponseUtil.errorResponse(false, Constants.FILE_ALREADY_EXIST, payload);
        } else {
            ResponseUtil.errorResponse(false, Constants.ERROR_DURING_SERVICE, payload);
        }
    }

    private void createFolderInPath(String targetDriveId, String targetRelativeFilePath) {
        try {
            String validatedPath = targetRelativeFilePath.startsWith("/") ? targetRelativeFilePath.replaceFirst("/", "") : targetRelativeFilePath;
            String[] stream = validatedPath.split("/");
            StringBuilder relativePathBuilder = new StringBuilder();
            for (String folderName : stream) {
                Map<String, Object> check = new HashMap<>();
                relativePathBuilder.append("/").append(folderName);
                searchFileExecutor.fetchFileDetailsByDriveID(targetDriveId, relativePathBuilder.toString(), false, check, null, null);
                if (!(boolean) check.get(Constants.IS_TARGET_FILE_EXIST)) {
                    String parentPath = FileUtil.getParentPath(relativePathBuilder.toString());
                    DriveItemRequestBuilder builder = FileUtil.getRootUserRequestBuilder(provider, targetDriveId);
                    if (parentPath != null && !parentPath.isEmpty()) {
                        builder = builder.itemWithPath(FileUtil.getParentPath(relativePathBuilder.toString()));
                    }
                    builder.children().buildRequest().post(DriveUtil.getDriveItemForItem(folderName));
                }
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage(), cause);
        }
    }

    private void executeMove(DriveItem source, DriveItem target, Map<String, Object> payload) throws IOException {
        if (source != null) {
            String endpoint = source.id;
            Response moveResponse = getRestClient(source.parentReference.driveId).
                    executeHttpMethods(endpoint, Constants.PATCH_METHOD,
                            createRequestBody(target.id, target.parentReference.driveId, source.name));
            prepareMovedResponse(moveResponse, target.parentReference.driveId, payload);
        }
    }

    private void executeCopy(DriveItem source, DriveItem target, Boolean replace, Map<String, Object> payload) throws IOException {
        if (source != null) {

            String endpoint = source.id + "/copy";
            if (Boolean.TRUE.equals(replace)) {
                endpoint += MICROSOFT_GRAPH_CONFLICT_BEHAVIOR_REPLACE;
            }
            Response copyResponse = getRestClient(source.parentReference.driveId)
                    .executeHttpMethods(endpoint, Constants.POST_METHOD, createRequestBody(target.id, target.parentReference.driveId, source.name));
            prepareCopyResponse(copyResponse, target.parentReference.driveId, payload);
        }
    }

}
