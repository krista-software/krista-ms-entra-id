package app.krista.extensions.essentials.cloudfiles.sharepoint.util;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.models.SearchHitsContainer;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseUtil {

    private ResponseUtil() {
    }

    public static Map<String, Object> fileEntityResponse(File fileEntity, boolean Success, String message,
                                                         Map<String, Object> payload) {
        payload.put(Constants.FILE, fileEntity);
        payload.put(Constants.MESSAGE, message);
        payload.put(Constants.SUCCESS, Success);
        return payload;
    }

    public static void listOfFileEntityResponse(List<File> fileEntity, boolean Success, String message,
                                                FreeForm payload) {
        payload.put(Constants.FILES, "[ Entity(File) ]", fileEntity);
        payload.put(Constants.MESSAGE, "Text", message);
        payload.put(Constants.SUCCESS, "Switch", Success);
    }

    public static Map<String, Object> successResponse(boolean Success, String message, Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, message);
        payload.put(Constants.SUCCESS, Success);
        return payload;
    }

    public static void getInfoResponse(int files, int folders, long size, String oldestFile, long oldestFileCreateDate,
                                       String newestFile, long newestFileCreteDate, String lastModifiedFileId,
                                       String lastModifiedFileName, long lastModifiedFileCreateDate, boolean Success,
                                       String message, FreeForm payload) {
        payload.put("Files", "Number", files);
        payload.put("Folders", "Number", folders);
        payload.put("Size", "Number", size);
        payload.put("Oldest File", "Text", oldestFile);
        payload.put("Oldest File Create Date", "Date", oldestFileCreateDate);
        payload.put("Newest File", "Text", newestFile);
        payload.put("Newest File Date", "Date", newestFileCreteDate);
        payload.put("Last Modified File Id", "Text", lastModifiedFileId);
        payload.put("Last Modified File Name", "Text", lastModifiedFileName);
        payload.put("Last Modified File Create Date", "Date", lastModifiedFileCreateDate);
        payload.put(Constants.MESSAGE, "Text", message);
        payload.put(Constants.SUCCESS, "Switch", Success);
    }

    public static FreeForm getInfoResponse(boolean Success, String message, FreeForm payload) {
        payload.put(Constants.MESSAGE, "Text", message);
        payload.put(Constants.SUCCESS, "Switch", Success);
        return payload;
    }

    public static void addFileSize(long size, FreeForm payload) {
        payload.put("Size", "Number", size);
    }

    public static void getFileEventData(FreeForm eventData, Map<String, Object> payload) {
        payload.put("Files", eventData.get("Files"));
        payload.put("Folders", eventData.get("Folders"));
        payload.put("Size", eventData.get("Size"));
        payload.put("Oldest File", eventData.get("Oldest File"));
        payload.put("Oldest File Create Date", eventData.get("Oldest File Create Date"));
        payload.put("Newest File", eventData.get("Newest File"));
        payload.put("Newest File Date", eventData.get("Newest File Date"));
        payload.put("Last Modified File Id", eventData.get("Last Modified File Id"));
        payload.put("Last Modified File Name", eventData.get("Last Modified File Name"));
        payload.put("Last Modified File Create Date", eventData.get("Last Modified File Create Date"));
        payload.put(Constants.MESSAGE, eventData.get(Constants.MESSAGE));
        payload.put(Constants.SUCCESS, eventData.get(Constants.SUCCESS));
    }

    public static boolean moreResultsAvailable(SearchEntityQueryCollectionPage page) {
        List<SearchHitsContainer> hitsContainers = page.getCurrentPage().get(0).hitsContainers;
        return hitsContainers != null && !Objects.requireNonNull(hitsContainers).isEmpty() && Boolean.TRUE.equals(hitsContainers.get(0).moreResultsAvailable);
    }


    public static boolean isValidEmail(String email) {
        String EMAIL_REGEX =
                "/^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$/";

        Pattern EMAIL_PATTERN =
                Pattern.compile(EMAIL_REGEX);
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }

}
