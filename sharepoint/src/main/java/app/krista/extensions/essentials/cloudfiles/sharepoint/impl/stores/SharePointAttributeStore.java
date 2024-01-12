package app.krista.extensions.essentials.cloudfiles.sharepoint.impl.stores;

import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.util.KeyValueStore;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

@Service
public class SharePointAttributeStore {

    private final Invoker invoker;
    private final KeyValueStore store;

    @Inject
    public SharePointAttributeStore(Invoker invoker, KeyValueStore store) {
        this.invoker = invoker;
        this.store = store;
    }

    public SharePointAttributes load(String authContextId) {
        var attributes = Constants.GSON.fromJson(String.valueOf(store.get(authContextId)), Map.class);
        return new SharePointAttributes(invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME,
                RoutingInfo.Type.APPLIANCE),
                (String) attributes.get(SharePointAttributes.CLIENT_ID),
                (String) attributes.get(SharePointAttributes.CLIENT_SECRET),
                (String) attributes.get(SharePointAttributes.EMAIL),
                (Boolean) attributes.get(SharePointAttributes.ALLOW_GROUP_ALERT),
                (String) attributes.get(SharePointAttributes.TENANT_ID));
    }

    public String save(SharePointAttributes sharePointAttributes) {
        String authContextId = UUID.randomUUID().toString();
        store.put(authContextId, Constants.GSON.toJson(Map.of(SharePointAttributes.CLIENT_ID,
                sharePointAttributes.getClientId(),
                SharePointAttributes.CLIENT_SECRET, sharePointAttributes.getClientSecret(),
                SharePointAttributes.TENANT_ID, sharePointAttributes.getTenantId(),
                SharePointAttributes.EMAIL, sharePointAttributes.getMailId(),
                SharePointAttributes.ALLOW_GROUP_ALERT, sharePointAttributes.getAllowGroupAlert())));
        return authContextId;
    }

    /**
     * Removes given authContext id from KeyValueStore
     *
     * @param authContextId authContext id
     */
    public void remove(String authContextId) {
        store.remove(authContextId);
    }

}
