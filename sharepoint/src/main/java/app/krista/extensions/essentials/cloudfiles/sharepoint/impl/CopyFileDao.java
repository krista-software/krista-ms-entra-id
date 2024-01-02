package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.RestClient;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.google.gson.JsonObject;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import okhttp3.*;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

/**
 * This class provides operations for copying and moving files in SharePoint.
 *
 * ***************************************************************
 * WARNING: DO NOT CHANGE ANYTHING IN THIS CLASS.
 * ***************************************************************
 *
 * @author Vaibhav Choudhary
 */

@Service
public class CopyFileDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyFileDao.class);
    private final GraphServiceClientProvider provider;
    private final OperationsManagementDao operationsManagementDao;
    private final ManagementUtilityService managementUtilityService;
    private final CopyFileUtilityService copyFileUtilityService;

    @Inject
    public CopyFileDao(GraphServiceClientProviderFactory factory, OperationsManagementDao operationsManagementDao,
                       ManagementUtilityService managementUtilityService,CopyFileUtilityService copyFileUtilityService) {
        this(factory.create(), operationsManagementDao, managementUtilityService, copyFileUtilityService);
    }

    public CopyFileDao(GraphServiceClientProvider provider, OperationsManagementDao operationsManagementDao,
                       ManagementUtilityService managementUtilityService, CopyFileUtilityService copyFileUtilityService) {
        this.provider = provider;
        this.operationsManagementDao = operationsManagementDao;
        this.managementUtilityService = managementUtilityService;
        this.copyFileUtilityService = copyFileUtilityService;
    }

    public Map<String, Object> copyFile(String source, String target, Boolean replace, Boolean createFolder) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (validateReplaceAndCreateFolderParameters(replace, createFolder, payload)) {
                copyFileUtilityService.validateSourceAndTargetPaths(source, target, payload);

                if ((boolean) payload.get(Constants.SUCCESS)) {
                    DriveItem driveItemSource = managementUtilityService.checkIfFileExist(source, payload);

                    if ((boolean) payload.get(Constants.SUCCESS)) {
                        DriveItem driveItemTarget = createOrCheckTargetItem(target, createFolder, payload, replace);

                        if ((boolean) payload.get(Constants.IS_FOLDER_EXIST)) {
                            String endpoint = driveItemSource.id + "/copy";
                            endpoint = addConflictBehaviorQueryParam(endpoint, replace);

                            Response copyResponse = getRestClient(driveItemTarget.parentReference.driveId)
                                    .executeHttpMethods(endpoint, Constants.POST_METHOD, createRequestBody(driveItemTarget.id));

                            processResponse(copyResponse, payload, driveItemSource);
                        }
                    }
                }
            }
        } catch (GraphServiceException | IOException | IllegalArgumentException cause) {
            copyFileUtilityService.handleCopyFileException(cause, payload);
        }

        return payload;
    }

    private boolean validateReplaceAndCreateFolderParameters(Boolean replace, Boolean createFolder, Map<String, Object> payload) {
        if (replace != null && createFolder != null && replace && createFolder) {
            payload.put(Constants.MESSAGE, Constants.PLEASE_SPECIFY_REPLACE_OR_CREATE);
            payload.put(Constants.SUCCESS, false);
            return false;
        }
        return true;
    }

    private DriveItem createOrCheckTargetItem(String target, Boolean createFolder, Map<String, Object> payload, Boolean replace) {
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

    public DriveItem checkIfTargetFolderExist(String target, Map<String, Object> response) {
        if (target != null) {
            try {
                DriveItem driveItem = managementUtilityService.getFileByAbsolutePath(target);
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

    public DriveItem checkTargetFolderExist(String target, Map<String, Object> response) {
        if (target != null) {
            try {
                DriveItem driveItem = managementUtilityService.getFileByAbsolutePath(target);
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

    private void processResponse(Response statusResponse, Map<String, Object> payload, DriveItem driveItemSource) throws IOException {
        if (statusResponse.code() == 200 || statusResponse.code() == 202) {
            DriveItem driveItem = provider.getUserRequestBuilder(null, null)
                    .drives(driveItemSource.parentReference.driveId).items(driveItemSource.id)
                    .buildRequest().get();

            payload.put(Constants.FILE, CatalogTypes.fromFile(new FileImpl(driveItem, null, null)));
            payload.put(Constants.MESSAGE, Constants.FILE_COPIED_SUCCESSFULLY);
            payload.put(Constants.SUCCESS, true);
        } else if (statusResponse.code() == 409) {
            payload.put(Constants.MESSAGE, "A file with this name already exists. Please choose a different file name.");
            payload.put(Constants.SUCCESS, false);
        } else {
            payload.put(Constants.MESSAGE, "There is some issue with File , Please choose a different file name.");
            payload.put(Constants.SUCCESS, false);
        }
    }

    public Map<String, Object> moveFile(String source, String target) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (copyFileUtilityService.validateSourceAndTargetPathsForMove(source, target, payload)) {
                DriveItem driveItemSource = managementUtilityService.checkIfFileExist(source, payload);

                if ((boolean) payload.get(Constants.SUCCESS)) {
                    DriveItem driveItemTarget = validateTargetFolderAndGetDriveItem(target, payload);

                    if ((boolean) payload.get(Constants.IS_FOLDER_EXIST)) {
                        String endpoint = buildMoveEndpoint(driveItemSource, driveItemTarget);

                        Response moveResponse = getRestClient(driveItemTarget.parentReference.driveId).
                                executeHttpMethods(endpoint, Constants.PATCH_METHOD,
                                        createRequestBody(driveItemTarget.id));
                        processResponse(moveResponse, payload, driveItemSource);
                        return payload;
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
                    String[] folders = target.split("/");

                    StringBuilder relativePathBuilder = new StringBuilder();
                    for (String folderName : folders) {
                        Map<String, Object> check = new HashMap<>();
                        absolutePathBuilder = absolutePathBuilder + "/" + folderName;
                        relativePathBuilder.append("/").append(folderName);

                        // Check if the folder exists
                        driveItem = checkTargetFolderExist(absolutePathBuilder.toString(), check);

                        if (!(boolean) check.get(Constants.IS_FOLDER_EXIST)) {
                            driveItem = provider.getUserRequestBuilder(null, null).drives(driveItemTarget.parentReference.driveId).root().itemWithPath(FileUtil.getParentPath(relativePathBuilder.toString())).children().buildRequest().post(DriveUtil.getDriveItemForItem(folderName));
                            payload.put("isFileExist", true);
                            return driveItem;
                        }
                    }
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

}
