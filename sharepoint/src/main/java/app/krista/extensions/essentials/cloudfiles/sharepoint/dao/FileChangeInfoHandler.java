package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;


import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileChangeResourceUtil;
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
import java.util.stream.Collectors;

@Service
public class FileChangeInfoHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileChangeInfoHandler.class);
    private final GraphServiceClientProvider provider;
    private final String routingUrl;
    private final FileSearchUtilityService fileSearchUtilityService;

    @Inject
    public FileChangeInfoHandler(GraphServiceClientProviderFactory factory, Invoker invoker, FileSearchUtilityService fileSearchUtilityService) {
        this(factory.create(), invoker, fileSearchUtilityService);
    }

    public FileChangeInfoHandler(GraphServiceClientProvider provider, Invoker invoker, FileSearchUtilityService fileSearchUtilityService) {
        this.provider = provider;
        this.routingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE).contains("https://extension.local.eng.krista.app") ? invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE).replace("https://extension.local.eng.krista.app", "http://localhost:8765") : invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        this.fileSearchUtilityService = fileSearchUtilityService;
    }

    public Map<String, Object> getChangeFileEvent(String subscriptionId) {
        Map<String, Object> payload = new HashMap<>();
        try {
            DriveItemDeltaCollectionPage oldDeltaCollectionPage = FileChangeResourceUtil.getFileChangeResource(subscriptionId);
            if(oldDeltaCollectionPage == null){
                payload.put(Constants.MESSAGE, Constants.EMPTY);
                return payload;
            }
            String driveID = extractDriveID(oldDeltaCollectionPage);
            String itemId = extractItemId(oldDeltaCollectionPage);
            processDeltaLinkItems(oldDeltaCollectionPage, payload);
            FileChangeResourceUtil.addFileMetaData(subscriptionId, createFileChangeResource(driveID,itemId));
            return payload;

        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.info(cause.getMessage(), cause);
            ResponseUtil.errorResponse(false, Constants.FILE_NOT_FOUND, payload);
        }
        return payload;
    }

    private DriveItemDeltaCollectionPage createFileChangeResource(String driveID, String itemId) {
        try {
            return provider.getUserRequestBuilder(null, null).drives(driveID).items(itemId).delta().buildRequest().get();
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.info(cause.getMessage(), cause);
            throw new RuntimeException("Error Occurred During Subscription");
        }
    }

    private void processDeltaLinkItems(DriveItemDeltaCollectionPage oldDeltaCollectionPage, Map<String, Object> payload) {
        try {
            payload.put(Constants.EVENT, Constants.NO_CHANGE);
            String token = getDeltaLinkToken(oldDeltaCollectionPage);
            String driveID = extractDriveID(oldDeltaCollectionPage);
            String itemId = extractItemId(oldDeltaCollectionPage);
            DriveItemDeltaCollectionPage trackChanges = getDeltaCollectionWithToken(token,driveID,itemId);
            compareWithChanges(trackChanges, payload,driveID,itemId);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.info(cause.getMessage(), cause);
            payload.put(Constants.EVENT, Constants.NO_CHANGE);
        }
    }

    @Nullable
    private static String extractDriveID(DriveItemDeltaCollectionPage oldDeltaCollectionPage) {
        if(oldDeltaCollectionPage.getCurrentPage()!= null
                && !oldDeltaCollectionPage.getCurrentPage().isEmpty()
        && oldDeltaCollectionPage.getCurrentPage().get(0) != null
                && oldDeltaCollectionPage.getCurrentPage().get(0).parentReference != null
        && oldDeltaCollectionPage.getCurrentPage().get(0).parentReference.driveId != null)
        {
           return Objects.requireNonNull(oldDeltaCollectionPage.getCurrentPage().get(0).parentReference).driveId;
        }
        else {
            return  " ";
        }
    }

    @Nullable
    private static String extractItemId(DriveItemDeltaCollectionPage oldDeltaCollectionPage) {
        if(oldDeltaCollectionPage.getCurrentPage()!= null
                && !oldDeltaCollectionPage.getCurrentPage().isEmpty()
                && oldDeltaCollectionPage.getCurrentPage().get(0) != null
                && oldDeltaCollectionPage.getCurrentPage().get(0).id != null)
        {
            return Objects.requireNonNull(oldDeltaCollectionPage.getCurrentPage().get(0).id);
        }
        else {
            return  " ";
        }
    }

    private void compareWithChanges(DriveItemDeltaCollectionPage trackChange, Map<String, Object> payload, String driveID, String itemId) {
        while (trackChange != null && !trackChange.getCurrentPage().isEmpty()) {
            DriveItemDeltaCollectionPage nextDelta = getDeltaCollectionWithToken(getDeltaLinkToken(trackChange), driveID, itemId);
            if(nextDelta == null){
                DriveItem driveItem = trackChange.getCurrentPage().get(trackChange.getCurrentPage().size()-1);
                if(driveItem.lastModifiedDateTime != null && driveItem.createdDateTime != null
                && driveItem.lastModifiedDateTime.equals(driveItem.createdDateTime)){
                    ResponseUtil.addCreatedFile(driveItem, payload,provider.getUserRequestBuilder(null,null),fileSearchUtilityService);
                }else{
                    ResponseUtil.addUpdatedFile(driveItem, payload,provider.getUserRequestBuilder(null,null),fileSearchUtilityService);
                }
            }
            if (nextDelta != null) {
                trackChange = nextDelta;
            } else {
                break;
            }
        }
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
            LOGGER.info(cause.getMessage(), cause);
        }
        return null;
    }

    private String getDeltaLinkToken(DriveItemDeltaCollectionPage deltaCollectionPage) {
        while (deltaCollectionPage != null && !deltaCollectionPage.getCurrentPage().isEmpty()) {
            if(deltaCollectionPage.deltaLink() != null){
                return  Arrays.stream(Objects.requireNonNull(deltaCollectionPage.deltaLink())
                        .split("token")).collect(Collectors.toList()).get(1).substring(1);

            } if (deltaCollectionPage.getNextPage() != null) {
                deltaCollectionPage = deltaCollectionPage.getNextPage().buildRequest().get();
            } else {
                break;
            }
        }
        return "";
    }


}
