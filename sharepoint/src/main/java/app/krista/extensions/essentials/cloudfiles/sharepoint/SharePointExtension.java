package app.krista.extensions.essentials.cloudfiles.sharepoint;

import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.Extension;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.InvokerRequest;
import app.krista.extension.impl.anno.StaticResource;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.HandleFileIsChanged;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.SharePointSubscription;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.stores.SharePointAttributeStore;

import javax.inject.Inject;
import java.util.Map;

@Field.Text(value = SharePointAttributes.CLIENT_ID)
@Field.Text(value = SharePointAttributes.CLIENT_SECRET, isSecured = true)
@Field.Text(value = SharePointAttributes.TENANT_ID, isSecured = true)
@Field.Text(value = SharePointAttributes.EMAIL)
@Field.Boolean(value = SharePointAttributes.ALLOW_GROUP_ALERT, required = false)
@Extension(version = "2.0.0-rc1")
@StaticResource(path = "docs", file = "docs")
public class SharePointExtension {

    private final String routingUrl;
    private final SharePointAttributes sharePointAttributes;
    private final GraphServiceClientProviderFactory clientProviderFactory;
    private final SharePointRequestAuthenticator requestAuthenticator;
    private final HandleFileIsChanged fileIsChanged;

    @Inject
    public SharePointExtension(Invoker invoker, SharePointAttributes sharePointAttributes,
                               GraphServiceClientProviderFactory clientProviderFactory,
                               SharePointAttributeStore sharePointAttributeStore, HandleFileIsChanged fileIsChanged) {
        this(invoker, sharePointAttributes, clientProviderFactory,
                new SharePointRequestAuthenticator(sharePointAttributeStore, sharePointAttributes), fileIsChanged);
    }

    public SharePointExtension(Invoker invoker, SharePointAttributes sharePointAttributes,
                               GraphServiceClientProviderFactory clientProviderFactory,
                               SharePointRequestAuthenticator requestAuthenticator, HandleFileIsChanged fileIsChanged) {
        this.routingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        this.sharePointAttributes = sharePointAttributes;
        this.clientProviderFactory = clientProviderFactory;
        this.requestAuthenticator = requestAuthenticator;
        this.fileIsChanged = fileIsChanged;
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
        SharePointSubscription.createSharePointSubscriptionForAllSites(routingUrl, clientProviderFactory.create(attributes));
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

}
