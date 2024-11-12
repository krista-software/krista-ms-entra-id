package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileChangeResourceUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileHierarchyBuilder;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemDeltaParameterSet;
import com.microsoft.graph.requests.DriveItemDeltaCollectionPage;
import org.jetbrains.annotations.Nullable;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class FileChangeInfoHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileChangeInfoHandler.class);
    private final GraphServiceClientProvider provider;
    private final FileSearchUtilityService fileSearchUtilityService;

    @Inject
    public FileChangeInfoHandler(GraphServiceClientProviderFactory factory, FileSearchUtilityService fileSearchUtilityService) {
        this(factory.create(), fileSearchUtilityService);
    }

    public FileChangeInfoHandler(GraphServiceClientProvider provider, FileSearchUtilityService fileSearchUtilityService) {
        this.provider = provider;
        this.fileSearchUtilityService = fileSearchUtilityService;
    }

    public List<Map<String, Object>> getChangeFileEvent(String subscriptionId) {
        Map<String, Object> payload = new HashMap<>();
        try {
            DriveItemDeltaCollectionPage oldDeltaCollectionPage = FileChangeResourceUtil.getFileChangeResource(subscriptionId);
            if(oldDeltaCollectionPage == null){
                payload.put(Constants.MESSAGE, Constants.EMPTY);
                return List.of(payload);
            }
            String driveID = extractDriveID(oldDeltaCollectionPage);
            String itemId = extractItemId(oldDeltaCollectionPage);
            String monitoredPath = FileChangeResourceUtil.getPathBySubscription(subscriptionId);
            if(monitoredPath == null){
                payload.put(Constants.MESSAGE, Constants.EMPTY);
                return List.of(payload);
            }
            String sitePath = FileUtil.driveIdPath(monitoredPath);
            List<Map<String, Object>> payloadList = processDeltaLinkItems(oldDeltaCollectionPage, payload,sitePath);
            FileChangeResourceUtil.addFileMetaData(subscriptionId, createFileChangeResource(driveID,itemId));
            return payloadList;

        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error("Error occurred while processing file change event : {}",cause.getMessage(), cause);
            ResponseUtil.errorResponse(false, Constants.FILE_NOT_FOUND, payload);
        }
        return List.of(payload);
    }

    private DriveItemDeltaCollectionPage createFileChangeResource(String driveID, String itemId) {
        try {
            return provider.getUserRequestBuilder(null, null).drives(driveID).items(itemId).delta().buildRequest().get();
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error("Error occurred during subscription : {}" ,cause.getMessage(), cause);
            throw new RuntimeException();
        }
    }

    private  List<Map<String, Object>> processDeltaLinkItems(DriveItemDeltaCollectionPage oldDeltaCollectionPage, Map<String, Object> payload, String sitePath) {
        try {
            payload.put(Constants.EVENT, Constants.EMPTY);
            String token = getDeltaLinkToken(oldDeltaCollectionPage);
            String driveID = extractDriveID(oldDeltaCollectionPage);
            String itemId = extractItemId(oldDeltaCollectionPage);

            DriveItemDeltaCollectionPage trackChanges = getDeltaCollectionWithToken(token,driveID,itemId);
            List<DriveItem> trackDriveItems = getListFromTrack(trackChanges,driveID,itemId);
            List<DriveItem> changedDriveItems = FileHierarchyBuilder.buildHierarchy(trackDriveItems);
            return prepareEvent(changedDriveItems,payload,sitePath);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.info("Error occurred while processing delta items : {} ",cause.getMessage(), cause);
            payload.put(Constants.EVENT, Constants.EMPTY);
        }
        return List.of(payload);
    }

    private List<Map<String, Object>> prepareEvent(List<DriveItem> changedDriveItems, Map<String, Object> payload, String sitePath) {
        List<Map<String, Object>> eventMap = new ArrayList<>();
        if(!changedDriveItems.isEmpty()) {
            for (DriveItem driveItem  : changedDriveItems) {
                if(driveItem.name == null){
                    continue;
                }
                if (driveItem.lastModifiedDateTime != null && driveItem.lastModifiedDateTime.equals(driveItem.createdDateTime)) {
                    eventMap.add(ResponseUtil.addCreatedFile(driveItem, provider.getUserRequestBuilder(null, null), fileSearchUtilityService, sitePath));
                } else {
                    eventMap.add(ResponseUtil.addUpdatedFile(driveItem, provider.getUserRequestBuilder(null, null), fileSearchUtilityService, sitePath));
                }
            }
        }
        return !eventMap.isEmpty() ? eventMap : List.of(payload);
    }

    private List<DriveItem> getListFromTrack(DriveItemDeltaCollectionPage trackChange, String driveID, String itemId) {
        List<DriveItem> driveItemList = new ArrayList<>();
        while (trackChange != null && !trackChange.getCurrentPage().isEmpty()) {
            driveItemList.addAll(trackChange.getCurrentPage());
            DriveItemDeltaCollectionPage nextDelta = getDeltaCollectionWithToken(getDeltaLinkToken(trackChange), driveID, itemId);
            if (nextDelta != null) {
                trackChange = nextDelta;
            } else {
                return driveItemList;
            }
        }
        return driveItemList;
    }

    @Nullable
    private static String extractDriveID(DriveItemDeltaCollectionPage oldDeltaCollectionPage) {
        oldDeltaCollectionPage.getCurrentPage();
        if(!oldDeltaCollectionPage.getCurrentPage().isEmpty() && oldDeltaCollectionPage.getCurrentPage().get(0) != null && oldDeltaCollectionPage.getCurrentPage().get(0).parentReference != null && Objects.requireNonNull(oldDeltaCollectionPage.getCurrentPage().get(0).parentReference).driveId != null)
        {
           return Objects.requireNonNull(oldDeltaCollectionPage.getCurrentPage().get(0).parentReference).driveId;
        }
        return  null;
    }

    @Nullable
    private static String extractItemId(DriveItemDeltaCollectionPage oldDeltaCollectionPage) {
        oldDeltaCollectionPage.getCurrentPage();
        if(!oldDeltaCollectionPage.getCurrentPage().isEmpty() && oldDeltaCollectionPage.getCurrentPage().get(0) != null && oldDeltaCollectionPage.getCurrentPage().get(0).id != null)
        {
            return Objects.requireNonNull(oldDeltaCollectionPage.getCurrentPage().get(0).id);
        }
        return  null;
    }

    private DriveItemDeltaCollectionPage getDeltaCollectionWithToken(String token, String driveID, String itemId) {
        try {
            return provider.getUserRequestBuilder(null, null).drives(driveID).items(itemId)
                    .delta(DriveItemDeltaParameterSet
                            .newBuilder()
                            .withToken(token)
                            .build())
                    .buildRequest()
                    .get();
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.info("Track change delta collection has reached last page");
        }
        return null;
    }

    private String getDeltaLinkToken(DriveItemDeltaCollectionPage deltaCollectionPage) {
        while (deltaCollectionPage != null && !deltaCollectionPage.getCurrentPage().isEmpty()) {
            if(deltaCollectionPage.deltaLink() != null){
                return  Arrays.stream(Objects.requireNonNull(deltaCollectionPage.deltaLink())
                        .split("token")).collect(Collectors.toList()).get(1).substring(1);

            }
             if (deltaCollectionPage.getNextPage() != null) {
                deltaCollectionPage = deltaCollectionPage.getNextPage().buildRequest().get();
            } else {
                break;
            }
        }
        return "";
    }

}
