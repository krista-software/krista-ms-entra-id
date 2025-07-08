package app.krista.extensions.cyber_security.iam.azureAD.impl.stores;

import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extension.util.InvokerAttributes;
import app.krista.extensions.cyber_security.iam.azureAD.AzureADAttributes;
import app.krista.extensions.cyber_security.iam.azureAD.impl.util.Constants;
import app.krista.extensions.util.KeyValueStore;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

import static app.krista.extensions.cyber_security.iam.azureAD.impl.util.Constants.*;

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
        // If authContextId equals invokerId, return default attributes from invoker
        if (authContextId != null && authContextId.equals(invoker.getInvokerId())) {
            Map<String, Object> invokerAttributes = invoker.getAttributes();
            String routingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
            return new AzureADAttributes(
                    InvokerAttributes.getStringOrNull(invokerAttributes, CLIENT_ID),
                    InvokerAttributes.getStringOrNull(invokerAttributes, CLIENT_SECRET),
                    InvokerAttributes.getStringOrNull(invokerAttributes, TENANT_ID),
                    InvokerAttributes.getStringOrNull(invokerAttributes, EMAIL),
                    InvokerAttributes.getBooleanOrDefault(invokerAttributes, ALLOW_MAIL_ALERT, false),
                    InvokerAttributes.getStringOrDefault(invokerAttributes, AUTH_TYPE, PUBLIC),
                    routingUrl
            );
        }

        var attributes = Constants.GSON.fromJson(String.valueOf(store.get(authContextId)), Map.class);
        String routingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        return new AzureADAttributes(
                (String) attributes.get(CLIENT_ID),
                (String) attributes.get(CLIENT_SECRET),
                (String) attributes.get(TENANT_ID),
                (String) attributes.get(EMAIL),
                (Boolean) attributes.getOrDefault(ALLOW_MAIL_ALERT, false),
                (String) attributes.getOrDefault(AUTH_TYPE, PUBLIC),
                routingUrl
        );
    }

    public String save(AzureADAttributes azureADAttributes) {
        String authContextId = UUID.randomUUID().toString();
        store.put(authContextId, Constants.GSON.toJson(azureADAttributes.toMap()));
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