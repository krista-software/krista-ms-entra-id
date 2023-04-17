package app.krista.extensions.cyber_security.iam.azureAD.impl.stores;

import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.cyber_security.iam.azureAD.AzureADAttributes;
import app.krista.extensions.cyber_security.iam.azureAD.impl.util.Constants;
import app.krista.extensions.util.KeyValueStore;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

@Service
public class AzureADAttributesStore {

    private final Invoker invoker;
    private final KeyValueStore store;

    @Inject
    public AzureADAttributesStore(Invoker invoker, KeyValueStore store) {
        this.invoker = invoker;
        this.store = store;
    }

    public AzureADAttributes load(String authContextId) {
        var attributes = Constants.GSON.fromJson(String.valueOf(store.get(authContextId)), Map.class);
        return new AzureADAttributes(invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE),
                (String) attributes.get(AzureADAttributes.CLIENT_ID),
                (String) attributes.get(AzureADAttributes.CLIENT_SECRET),
                (String) attributes.get(AzureADAttributes.TENANT_ID),
                (String) attributes.get(AzureADAttributes.EMAIL));
    }

    public String save(AzureADAttributes azureADAttributes) {
        String authContextId = UUID.randomUUID().toString();
        store.put(authContextId, Constants.GSON.toJson(Map.of(
                AzureADAttributes.CLIENT_ID, azureADAttributes.getClientId(),
                AzureADAttributes.CLIENT_SECRET, azureADAttributes.getClientSecret(),
                AzureADAttributes.TENANT_ID, azureADAttributes.getTenantId(),
                AzureADAttributes.EMAIL, azureADAttributes.getEmail())));
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