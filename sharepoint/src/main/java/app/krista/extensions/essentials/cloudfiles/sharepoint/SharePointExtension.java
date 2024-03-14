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
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.List;
import com.microsoft.graph.models.Subscription;
import com.microsoft.graph.requests.DriveItemDeltaCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.GroupCollectionPage;
import com.microsoft.graph.requests.ListCollectionPage;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Field.Text(value = Constants.CLIENT_ID)
@Field.Text(value = Constants.CLIENT_SECRET, isSecured = true)
@Field.Text(value = Constants.TENANT_ID, isSecured = true)
@Field.Text(value = Constants.EMAIL)
@Extension(version = "2.0.0-rc8")
@StaticResource(path = "docs", file = "docs")
public class SharePointExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharePointExtension.class);
    private static final String WHEN_FILE_IS_CHANGE_REQ_ID = "localDomainRequest_d6ab2648-df6b-4429-aeef-a56c75fde191";
    private static final String SOURCE = "Source";

    private final String routingUrl;
    private final SharePointAttributes sharePointAttributes;
    private final GraphServiceClientProviderFactory clientProviderFactory;
    private final SharePointRequestAuthenticator requestAuthenticator;
    private final SearchFileExecutor searchFileExecutor;

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

    private static String getListIdFromSite(String siteId, String listName, GraphServiceClient<Request> graphClient) {
        try {
            ListCollectionPage listCollectionPage = graphClient.sites(siteId).lists().buildRequest().get();
            while (listCollectionPage != null && !listCollectionPage.getCurrentPage().isEmpty()) {
                for (List list : listCollectionPage.getCurrentPage()) {
                    if (list.name != null && list.name.equals(listName)) {
                        return list.id;
                    }
                }
                if (listCollectionPage.getNextPage() != null) {
                    listCollectionPage = listCollectionPage.getNextPage().buildRequest().get();
                } else {
                    break;
                }
            }

        } catch (GraphServiceException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
        }
        return null;
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
        if (waitForEventListener.getRequestId().equals(WHEN_FILE_IS_CHANGE_REQ_ID)) {
            Map<String, Object> payload = new HashMap<>();
            String path = waitForEventListener.getRequestInputs().get(SOURCE).toString();
            searchFileExecutor.getFileInfo(path, payload, true);
            DriveItem driveItem = payload.containsKey(Constants.SOURCE_DRIVE_ITEM) ? (DriveItem) payload.get(Constants.SOURCE_DRIVE_ITEM) : null;
            if (driveItem != null) {
                GraphServiceClient<Request> provider = clientProviderFactory.create(sharePointAttributes).getUserRequestBuilder(null, null);
                String siteId = driveItem.parentReference.siteId;
                if (siteId == null && driveItem.name != null && driveItem.name.equals("root")) {
                    String siteName = FileUtil.extractSecondLastFolder(driveItem.webUrl != null ? driveItem.webUrl : "");
                    siteId = getSiteIdBySiteName(siteName, provider);
                }
                String listName = FileUtil.getLastPathElement(payload.get(Constants.SOURCE_ROOT_PATH).toString());
                String listId = getListIdFromSite(siteId, listName, provider);
                if (listId != null) {
                    Subscription subscription = SharePointSubscription.createSubscription(routingUrl, clientProviderFactory.create(sharePointAttributes), siteId, listId);
                    setFileResourceMetaData(path, driveItem, subscription);
                } else {
                    throw new IllegalStateException(Constants.INVALID_INPUT);
                }
            } else {
                throw new IllegalStateException(Constants.INVALID_INPUT);
            }

        }
    }

    private void setFileResourceMetaData(String path, DriveItem driveItem, Subscription subscription) {
        if (subscription != null) {
            FileChangeResourceUtil.addSubscription(path, subscription.id);
            DriveItemDeltaCollectionPage deltaCollectionPage = getDeltaCollectionPage(driveItem);
            FileChangeResourceUtil.addFileMetaData(subscription.id, deltaCollectionPage);
        }
    }

    private DriveItemDeltaCollectionPage getDeltaCollectionPage(DriveItem driveItem) {
        try {
            return clientProviderFactory.create(sharePointAttributes).
                    getUserRequestBuilder(null, null).drives(driveItem.parentReference.driveId)
                    .items(driveItem.id).delta().buildRequest().get();
        } catch (GraphServiceException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
        }
        return null;
    }

    private String getSiteIdBySiteName(String siteName, GraphServiceClient<Request> provider) {
        try {
            GroupCollectionPage groups = provider.groups().buildRequest().get();
            while (groups != null && !groups.getCurrentPage().isEmpty()) {
                for (Group group : groups.getCurrentPage()) {
                    if (group.mailNickname != null && group.mailNickname.equals(siteName) && group.proxyAddresses != null) {
                        for (String content : group.proxyAddresses) {
                            if (content.startsWith("SPO")) {
                                return FileUtil.extractSiteID(content);
                            }
                        }
                    }
                }
                if (groups.getNextPage() != null) {
                    groups = groups.getNextPage().buildRequest().get();
                } else {
                    break;
                }
            }
        } catch (Exception cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
        }
        return " ";
    }

    /**
     * For Example In this request the user can de register own event listener that was used to implement wait for event request
     *
     * @param waitForEventListener
     */
    @InvokerRequest(InvokerRequest.Type.UNREGISTER_EVENT_LISTENER)
    public void onUnRegisterEventListener(WaitForEventListener waitForEventListener) {

        if (waitForEventListener.getRequestId().equals(WHEN_FILE_IS_CHANGE_REQ_ID)) {
            String path = waitForEventListener.getRequestInputs().get(SOURCE).toString();
            String subscriptionId = FileChangeResourceUtil.getSubscriptionByPath(path);

            if (subscriptionId != null) {
                SharePointSubscription.deleteSubscription(clientProviderFactory.create(sharePointAttributes), subscriptionId);
                FileChangeResourceUtil.removeSubscriptionByID(subscriptionId);
                FileChangeResourceUtil.removePathFromSubscriptionStore(path);
            }
        }
    }

}
