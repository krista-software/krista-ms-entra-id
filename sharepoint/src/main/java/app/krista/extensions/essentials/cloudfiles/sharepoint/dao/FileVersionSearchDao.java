package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.ManagementUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemVersion;
import com.microsoft.graph.requests.DriveItemVersionCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileVersionSearchDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileVersionSearchDao.class);
    private final GraphServiceClientProvider clientProvider;
    private final ManagementUtilityService managementUtilityService;


    @Inject
    public FileVersionSearchDao(GraphServiceClientProviderFactory factory, ManagementUtilityService managementUtilityService) {
        this.clientProvider = factory.create();
        this.managementUtilityService = managementUtilityService;
    }


    public Map<String, Object> getVersionHistory(String sourcePath, String search, Long since) {
        Map<String, Object> payload = new HashMap<>();
        try {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(null, null);
            if (search != null && !search.isBlank()) {
                sourcePath = sourcePath + "/" + search;
            }
            DriveItem driveItem = managementUtilityService.getFileByAbsolutePath(sourcePath, graphClient);

            if (driveItem == null || driveItem.parentReference == null || driveItem.parentReference.driveId == null) {
                ResponseUtil.successResponse(false, Constants.PROVIDE_VALID_SOURCE_PATH, payload);
                return payload;
            }

            getVersionHistoryByDriveItem(driveItem, payload, since, graphClient);
        } catch (Exception cause) {
            handleVersionHistoryException(cause, payload);
        }
        return payload;
    }

    private void getVersionHistoryByDriveItem(DriveItem driveItem, Map<String, Object> payload, Long since,
                                              GraphServiceClient<Request> graphClient) {
        try {
            DriveItemVersionCollectionPage versions = graphClient.me().drives(driveItem.parentReference.driveId)
                    .items(driveItem.id).versions().buildRequest().get();

            List<Map<String, Object>> obj1 = new ArrayList<>();
            if (versions != null && versions.getCurrentPage().size() > 1) {
                if (since != null) {
                    for (DriveItemVersion version : versions.getCurrentPage()) {
                        OffsetDateTime offsetDateTime = version.lastModifiedDateTime;
                        long offsetDateTimeAsLong = offsetDateTime.toInstant().toEpochMilli();

                        if (offsetDateTimeAsLong >= since &&  !version.id.equals("1.0")) {
                            ResponseUtil.successResponse(true, Constants.FOUND_VERSION_HISTORY_FOR_FILE, payload);
                            Long date = version.lastModifiedDateTime != null ? Timestamp.valueOf(version.lastModifiedDateTime
                                    .toLocalDateTime()).getTime() : null;

                            Map<String, Object> obj = new HashMap<>();
                            obj.put("Path", driveItem.webUrl);
                            obj.put("Date", date);
                            obj.put("Update", "Updated");
                            obj1.add(obj);
                        }
                    }
                } else {
                    for (DriveItemVersion version : versions.getCurrentPage()) {
                        if(version != null && !version.id.equals("1.0")) {
                            ResponseUtil.successResponse(true, Constants.FOUND_VERSION_HISTORY_FOR_FILE, payload);
                            Long date = version.lastModifiedDateTime != null ? Timestamp.valueOf(version.lastModifiedDateTime
                                    .toLocalDateTime()).getTime() : null;

                            Map<String, Object> obj = new HashMap<>();
                            obj.put("Path", driveItem.webUrl);
                            obj.put("Date", date);
                            obj.put("Update", "Updated");
                            obj1.add(obj);
                        }
                    }
                }
                payload.put("Result", obj1);
            } else if (versions != null && versions.getCurrentPage().size() == 1) {
                if (since != null) {
                    for (DriveItemVersion version : versions.getCurrentPage()) {
                        OffsetDateTime offsetDateTime = version.lastModifiedDateTime;
                        long offsetDateTimeAsLong = offsetDateTime.toInstant().toEpochMilli();

                        if (offsetDateTimeAsLong >= since &&  version.id.equals("1.0")) {
                            ResponseUtil.successResponse(true, Constants.FOUND_VERSION_HISTORY_FOR_FILE, payload);
                            Long date = version.lastModifiedDateTime != null ? Timestamp.valueOf(version.lastModifiedDateTime
                                    .toLocalDateTime()).getTime() : null;

                            Map<String, Object> obj = new HashMap<>();
                            obj.put("Path", driveItem.webUrl);
                            obj.put("Date", date);
                            obj.put("Update", "Created");
                            obj1.add(obj);
                        }
                    }
                } else {
                    for (DriveItemVersion version : versions.getCurrentPage()) {
                        if(version != null && version.id.equals("1.0")) {
                            ResponseUtil.successResponse(true, Constants.FOUND_VERSION_HISTORY_FOR_FILE, payload);
                            Long date = version.lastModifiedDateTime != null ? Timestamp.valueOf(version.lastModifiedDateTime
                                    .toLocalDateTime()).getTime() : null;

                            Map<String, Object> obj = new HashMap<>();
                            obj.put("Path", driveItem.webUrl);
                            obj.put("Date", date);
                            obj.put("Update", "Created");
                            obj1.add(obj);
                        }
                    }
                }
                payload.put("Result", obj1);
            } else {
                ResponseUtil.successResponse(false, Constants.THERE_IS_NO_VERSION_HISTORY, payload);
            }

        } catch (Exception cause) {
            throw new IllegalArgumentException(" " + cause.getMessage());
        }
    }

    private void handleVersionHistoryException(Exception cause, Map<String, Object> payload) {
        LOGGER.error(Constants.ERROR_MESSAGE + " : " + cause.getMessage());
        payload.put(Constants.MESSAGE, Constants.ERROR_GETTING_VERSION_HISTORY);
        payload.put(Constants.SUCCESS, false);
    }


}
