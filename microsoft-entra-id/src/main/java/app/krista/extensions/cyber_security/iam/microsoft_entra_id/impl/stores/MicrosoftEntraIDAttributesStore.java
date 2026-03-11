/*
 * Ms Entra Id Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.stores;

import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extension.util.InvokerAttributes;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.MicrosoftEntraIDAttributes;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Constants;
import app.krista.extensions.util.KeyValueStore;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

import static app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Constants.*;

@Service
public class MicrosoftEntraIDAttributesStore {

    private final Invoker invoker;
    private final KeyValueStore store;

    @Inject
    public MicrosoftEntraIDAttributesStore(Invoker invoker, KeyValueStore store) {
        this.invoker = invoker;
        this.store = store;
    }

    public MicrosoftEntraIDAttributes load(String authContextId) {
        // If authContextId equals invokerId, try to load saved attributes first, then fall back to invoker attributes
        if (authContextId != null && authContextId.equals(invoker.getInvokerId())) {
            // First try to load saved attributes
            Object savedData = store.get(authContextId);
            if (savedData != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> savedAttributes = Constants.GSON.fromJson(String.valueOf(savedData), Map.class);
                String routingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
                return new MicrosoftEntraIDAttributes(
                        (String) savedAttributes.get(CLIENT_ID),
                        (String) savedAttributes.get(CLIENT_SECRET),
                        (String) savedAttributes.get(TENANT_ID),
                        (String) savedAttributes.get(EMAIL),
                        (String) savedAttributes.getOrDefault(AUTH_TYPE, PUBLIC),
                        routingUrl
                );
            }

            // Fall back to invoker attributes if no saved data
            Map<String, Object> invokerAttributes = invoker.getAttributes();
            String routingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
            return new MicrosoftEntraIDAttributes(
                    InvokerAttributes.getStringOrNull(invokerAttributes, CLIENT_ID),
                    InvokerAttributes.getStringOrNull(invokerAttributes, CLIENT_SECRET),
                    InvokerAttributes.getStringOrNull(invokerAttributes, TENANT_ID),
                    InvokerAttributes.getStringOrNull(invokerAttributes, EMAIL),
                    InvokerAttributes.getStringOrDefault(invokerAttributes, AUTH_TYPE, PUBLIC),
                    routingUrl
            );
        }

        // Load temporary attributes for other authContextIds
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = Constants.GSON.fromJson(String.valueOf(store.get(authContextId)), Map.class);
        String routingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        return new MicrosoftEntraIDAttributes(
                (String) attributes.get(CLIENT_ID),
                (String) attributes.get(CLIENT_SECRET),
                (String) attributes.get(TENANT_ID),
                (String) attributes.get(EMAIL),
                (String) attributes.getOrDefault(AUTH_TYPE, PUBLIC),
                routingUrl
        );
    }

    public String save(MicrosoftEntraIDAttributes azureADAttributes) {
        String authContextId = UUID.randomUUID().toString();
        store.put(authContextId, Constants.GSON.toJson(azureADAttributes.toMap()));
        return authContextId;
    }

    public boolean save(MicrosoftEntraIDAttributes azureADAttributes, String invokerId) {
        try {
            // Save attributes to the invoker's configuration
            // This would typically save to the invoker's persistent storage
            // For now, we'll store it in the KeyValueStore with the invokerId as key
            store.put(invokerId, Constants.GSON.toJson(azureADAttributes.toMap()));
            return true;
        } catch (Exception e) {
            return false;
        }
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