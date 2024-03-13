package app.krista.extensions.essentials.cloudfiles.sharepoint.util;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DrivePermissionImpl;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.SearchHitsContainer;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.Request;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.*;

public class ResponseUtil {

    private ResponseUtil() {
    }

    public static void fileEntityResponse(File fileEntity, boolean success, String message, Map<String, Object> payload) {
        payload.put(Constants.FILE, fileEntity);
        payload.put(Constants.MESSAGE, message);
        payload.put(Constants.SUCCESS, success);
    }

    public static void addUpdatedFile(DriveItem driveItem, Map<String, Object> payload, GraphServiceClient<Request> graphClient, FileSearchUtilityService fileSearchUtilityService, String sitePath) {
        app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File fileEntity = File.fromFile(new DrivePermissionImpl(driveItem, graphClient, fileSearchUtilityService,sitePath));
        payload.put(Constants.FILE, fileEntity);
        payload.put(Constants.EVENT, Constants.UPDATED);
    }
    public static void addCreatedFile(DriveItem driveItem, Map<String, Object> payload, GraphServiceClient<Request> graphClient, FileSearchUtilityService fileSearchUtilityService, String sitePath) {
        app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File fileEntity = File.fromFile(new DrivePermissionImpl(driveItem, graphClient, fileSearchUtilityService,sitePath));
        payload.put(Constants.FILE, fileEntity);
        payload.put(Constants.EVENT, Constants.CREATED);
    }

    public static void addEventToFreeForm(Map<String, Object> payload, FreeForm form) {
        form.put(Constants.FILE, "Entity(File)", payload.get(Constants.FILE));
        form.put(Constants.EVENT, Constants.TEXT, payload.get(Constants.EVENT));
    }

    public static void listOfFileEntityResponse(List<File> fileEntity, boolean success, String message, FreeForm payload) {
        payload.put(Constants.FILES, "[ Entity(File) ]", fileEntity);
        payload.put(Constants.MESSAGE, Constants.TEXT, message);
        payload.put(Constants.SUCCESS, Constants.SWITCH, success);
    }


    public static void successResponse(boolean success, String message, Map<String, Object> payload) {
        errorResponse(success, message, payload);
    }

    public static void getInfoResponse(int files, int folders, long size, String oldestFile, long oldestFileCreateDate, String newestFile, long newestFileCreteDate, String lastModifiedFileId, String lastModifiedFileName, long lastModifiedFileCreateDate, boolean success, String message, FreeForm payload) {
        payload.put(Constants.FILES, Constants.NUMBER, files);
        payload.put(Constants.FOLDERS, Constants.NUMBER, folders);
        payload.put(Constants.SIZE, Constants.NUMBER, size);
        payload.put(Constants.OLDEST_FILE, Constants.TEXT, oldestFile);
        payload.put(Constants.OLDEST_FILE_CREATE_DATE, Constants.DATE, oldestFileCreateDate);
        payload.put(Constants.NEWEST_FILE, Constants.TEXT, newestFile);
        payload.put(Constants.NEWEST_FILE_DATE, Constants.DATE, newestFileCreteDate);
        payload.put(Constants.LAST_MODIFIED_FILE_ID, Constants.TEXT, lastModifiedFileId);
        payload.put(Constants.LAST_MODIFIED_FILE_NAME, Constants.TEXT, lastModifiedFileName);
        payload.put(Constants.LAST_MODIFIED_FILE_CREATE_DATE, Constants.DATE, lastModifiedFileCreateDate);
        payload.put(Constants.MESSAGE, Constants.TEXT, message);
        payload.put(Constants.SUCCESS, Constants.SWITCH, success);
    }

    public static FreeForm getInfoResponse(boolean success, String message, FreeForm payload) {
        payload.put(Constants.MESSAGE, "Text", message);
        payload.put(Constants.SUCCESS, "Switch", success);
        return payload;
    }

    public static void addFileSize(long size, FreeForm payload) {
        payload.put("Size", "Number", size);
    }

    public static void changesSinceResponse(boolean success, String message, List<Map<String, Object>> result, Map<String, Object> payload) {
        payload.put(Constants.RESULT, result);
        payload.put(Constants.MESSAGE, message);
        payload.put(Constants.SUCCESS, success);
    }

    public static Map<String, Object> createResultContent(String path, long date, String update) {
        Map<String, Object> resultContent = new LinkedHashMap<>();
        resultContent.put(Constants.PATH, path);
        resultContent.put(Constants.DATE, date);
        resultContent.put(Constants.UPDATE, update);
        return resultContent;
    }

    public static void getFileEventData(FreeForm eventData, Map<String, Object> payload) {
        payload.put(Constants.FILES, eventData.get(Constants.FILES));
        payload.put(Constants.FOLDERS, eventData.get(Constants.FOLDERS));
        payload.put(Constants.SIZE, eventData.get(Constants.SIZE));
        payload.put(Constants.OLDEST_FILE, eventData.get(Constants.OLDEST_FILE));
        payload.put(Constants.OLDEST_FILE_CREATE_DATE, eventData.get(Constants.OLDEST_FILE_CREATE_DATE));
        payload.put(Constants.NEWEST_FILE, eventData.get(Constants.NEWEST_FILE));
        payload.put(Constants.NEWEST_FILE_DATE, eventData.get(Constants.NEWEST_FILE_DATE));
        payload.put(Constants.LAST_MODIFIED_FILE_ID, eventData.get(Constants.LAST_MODIFIED_FILE_ID));
        payload.put(Constants.LAST_MODIFIED_FILE_NAME, eventData.get(Constants.LAST_MODIFIED_FILE_NAME));
        payload.put(Constants.LAST_MODIFIED_FILE_CREATE_DATE, eventData.get(Constants.LAST_MODIFIED_FILE_CREATE_DATE));
        payload.put(Constants.MESSAGE, eventData.get(Constants.MESSAGE));
        payload.put(Constants.SUCCESS, eventData.get(Constants.SUCCESS));
    }

    public static boolean moreResultsAvailable(SearchEntityQueryCollectionPage page) {
        List<SearchHitsContainer> hitsContainers = page.getCurrentPage().get(0).hitsContainers;
        return hitsContainers != null && !Objects.requireNonNull(hitsContainers).isEmpty() && Boolean.TRUE.equals(hitsContainers.get(0).moreResultsAvailable);
    }


    public static boolean isValidEmail(String email) {
        return EmailValidator.getInstance().isValid(email);
    }

    public static Map<String, Object> nonSuccessResponce(Map<String, Object> payload) {
        payload.put(Constants.FILE, new File());
        payload.put(Constants.SUCCESS, false);
        payload.put(Constants.MESSAGE, Constants.GROUP_NAME_EXIST);
        return payload;
    }

    public static void errorResponse(boolean success, String message, Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, message);
        payload.put(Constants.SUCCESS, success);
    }

    public static Map<String, Object> buildPathValidationResult(boolean success, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(Constants.MESSAGE, message);
        payload.put(Constants.SUCCESS, success);
        return payload;
    }

    public static void addDriveItemMessage(boolean isFileExist, boolean success, String message, Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, message);
        payload.put(Constants.IS_FILE_EXIST, isFileExist);
        payload.put(Constants.SUCCESS, success);
    }

    public static void addSourceDriveItem(boolean isFileExist, DriveItem driveItem, Map<String, Object> payload) {
        payload.put(Constants.SOURCE_DRIVE_ITEM, driveItem);
        payload.put(Constants.IS_SOURCE_FILE_EXIST, isFileExist);
    }

    public static void addTargetDriveItem(boolean isFileExist, DriveItem driveItem, Map<String, Object> payload) {
        payload.put(Constants.TARGET_DRIVE_ITEM, driveItem);
        payload.put(Constants.IS_TARGET_FILE_EXIST, isFileExist);
    }

    public static void addDriveItem(boolean isFileExist, DriveItem driveItem, Map<String, Object> payload) {
        payload.put(Constants.DRIVE_ITEM, driveItem);
        payload.put(Constants.IS_FILE_EXIST, isFileExist);
    }

    public static Map<String,Object> preparePayloadResponse(File file, boolean success, String message) {
        Map<String,Object> payload = new HashMap<>();
        payload.put(Constants.FILE,file);
        payload.put(Constants.SUCCESS,success);
        payload.put(Constants.MESSAGE,message);
        return payload;
    }
}


