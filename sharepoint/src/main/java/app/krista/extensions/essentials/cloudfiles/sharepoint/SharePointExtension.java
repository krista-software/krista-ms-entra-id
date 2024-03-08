package app.krista.extensions.essentials.cloudfiles.sharepoint;

import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.event.WaitForEventListener;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.Extension;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.InvokerRequest;
import app.krista.extension.impl.anno.StaticResource;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.SearchFileExecutor;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.SharePointSubscription;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileChangeResourceUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.stores.SharePointAttributeStore;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.microsoft.graph.http.DeltaCollectionPage;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.DriveCollectionPage;
import com.microsoft.graph.requests.DriveItemDeltaCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.ListCollectionPage;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Field.Text(value = Constants.CLIENT_ID)
@Field.Text(value = Constants.CLIENT_SECRET, isSecured = true)
@Field.Text(value = Constants.TENANT_ID, isSecured = true)
@Field.Text(value = Constants.EMAIL)
@Extension(version = "2.0.0-rc5")
@StaticResource(path = "docs", file = "docs")
public class SharePointExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharePointExtension.class);
    private final String routingUrl;
    private final SharePointAttributes sharePointAttributes;
    private final GraphServiceClientProviderFactory clientProviderFactory;
    private final SharePointRequestAuthenticator requestAuthenticator;
    private final SearchFileExecutor searchFileExecutor;
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Inject
    public SharePointExtension(Invoker invoker, SharePointAttributes sharePointAttributes,
                               GraphServiceClientProviderFactory clientProviderFactory,
                               SharePointAttributeStore sharePointAttributeStore, SearchFileExecutor searchFileExecutor) {
        this(invoker, sharePointAttributes, clientProviderFactory,
                new SharePointRequestAuthenticator(sharePointAttributeStore, sharePointAttributes), searchFileExecutor);
    }

    public SharePointExtension(Invoker invoker, SharePointAttributes sharePointAttributes,
                               GraphServiceClientProviderFactory clientProviderFactory,
                               SharePointRequestAuthenticator requestAuthenticator, SearchFileExecutor searchFileExecutor) {
        this.routingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        this.sharePointAttributes = sharePointAttributes;
        this.clientProviderFactory = clientProviderFactory;
        this.requestAuthenticator = requestAuthenticator;
        this.searchFileExecutor = searchFileExecutor;
    }

    @InvokerRequest(InvokerRequest.Type.AUTHENTICATOR)
    public RequestAuthenticator getRequestAuthenticator() {
        return requestAuthenticator;
    }

    @InvokerRequest(InvokerRequest.Type.CUSTOM_TABS)
    public Map<String, String> customTabs() {
        return Map.of("Documentation", "static/docs");
    }

    @InvokerRequest(InvokerRequest.Type.VALIDATE_ATTRIBUTES)
    public void validateConnection(Map<String, Object> connectionAttributes) {
        SharePointAttributes attributes = SharePointAttributes.create(routingUrl, connectionAttributes);
        if (attributes.getMailId() != null && !attributes.getMailId().isBlank()) {
            clientProviderFactory.create(attributes)
                    .getGraphServiceClientForAdmin()
                    .me().drive().root().children().buildRequest().get();
        }
//        executorService.submit(() -> {
//            SharePointSubscription.createSharePointSubscriptionForAllSites(routingUrl, clientProviderFactory.create(attributes));
//        });
    }

    @InvokerRequest(InvokerRequest.Type.TEST_CONNECTION)
    public void testConnection() {
        if (sharePointAttributes.getMailId() != null && !sharePointAttributes.getMailId().isBlank()) {
            clientProviderFactory.create(sharePointAttributes)
                    .getGraphServiceClientForAdmin()
                    .me().drive().root().children().buildRequest().get();
        }
    }

    @InvokerRequest(InvokerRequest.Type.INVOKER_UPDATED)
    public void attributesUpdated(Map<String, Object> oldAttributes, Map<String, Object> newAttributes) {
        sharePointAttributes.update(newAttributes);
    }

    /**
     * For Example In this request the user can register own event listener to implement wait for event request
     *
     * @param waitForEventListener
     */
    @InvokerRequest(InvokerRequest.Type.REGISTER_EVENT_LISTENER)
    public void onRegisterEventListener(WaitForEventListener waitForEventListener) {
        if(waitForEventListener.getRequestId().equals("localDomainRequest_d6ab2648-df6b-4429-aeef-a56c75fde191")) {
            Map<String, Object> payload = new HashMap<>();
            if (waitForEventListener != null) {
                String path = waitForEventListener.getRequestInputs().get("Source").toString();
                //Check if File Exists
                searchFileExecutor.getFileInfo(path, payload, true);
                DriveItem driveItem = payload.containsKey(Constants.SOURCE_DRIVE_ITEM) ? (DriveItem) payload.get(Constants.SOURCE_DRIVE_ITEM) : null;
                if (driveItem != null) {
                    GraphServiceClient<Request> provider = clientProviderFactory.create(sharePointAttributes).getUserRequestBuilder(null, null);
                    String siteId = driveItem.parentReference.siteId;
                    String listName = FileUtil.getlastPathElement(payload.get(Constants.SOURCE_ROOT_PATH).toString());
                    String listId = getListFromSite(siteId, listName, provider);
                    if (listId != null) {
                        //Create Subscription
                        Subscription subscription = SharePointSubscription.createSubscription(routingUrl, clientProviderFactory.create(sharePointAttributes), siteId, listId);
                        FileChangeResourceUtil.addSubscription(path, subscription.id);
                        DriveItemDeltaCollectionPage deltaCollectionPage = clientProviderFactory.create(sharePointAttributes).getUserRequestBuilder(null, null).drives(driveItem.parentReference.driveId)
                                .items(driveItem.id).delta().buildRequest().get();
                        FileChangeResourceUtil.addFileMetaData(subscription.id, deltaCollectionPage);
                        //Now add metaData
                    } else {
                        throw new IllegalStateException("Invalid Input");
                    }

                } else {
                    throw new IllegalStateException("Invalid Input");
                }
            }
            //create subscription based on path and store delta collection with subscription ID in Map
            //store that path and subscription ID in Map
        }
        //
    }

    /**
     * For Example In this request the user can de register own event listener that was used to implement wait for event request
     *
     * @param waitForEventListener
     */
    @InvokerRequest(InvokerRequest.Type.UNREGISTER_EVENT_LISTENER)
    public void onUnRegisterEventListener(WaitForEventListener waitForEventListener) {
        //Delete subscription
        //
    }


    private static String getListFromSite(String siteId, String listName, GraphServiceClient<Request> graphClient) {
        try {
            ListCollectionPage listCollectionPage = graphClient.sites(siteId).lists().buildRequest().get();
            while(listCollectionPage != null && !listCollectionPage.getCurrentPage().isEmpty()){
                for(List list : listCollectionPage.getCurrentPage()){
                    if(list.name != null && list.name.equals(listName)){
                        return list.id;
                    }
                }
                if(listCollectionPage.getNextPage() != null){
                    listCollectionPage = listCollectionPage.getNextPage().buildRequest().get();
                }else{
                    break;
                }
            }

        } catch (GraphServiceException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
        }
        return null;
    }

}
