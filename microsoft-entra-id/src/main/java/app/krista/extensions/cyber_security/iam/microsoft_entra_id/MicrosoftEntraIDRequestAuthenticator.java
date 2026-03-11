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

package app.krista.extensions.cyber_security.iam.microsoft_entra_id;

import app.krista.extension.authorization.MustAuthenticateException;
import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.request.ProtoRequest;
import app.krista.extension.request.ProtoResponse;
import app.krista.extension.request.protos.http.HttpRequest;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.connectors.OAuthService;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.stores.MicrosoftEntraIDAttributesStore;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Constants;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.model.field.NamedField;
import app.krista.model.field.NamedValuedField;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.*;

import static com.github.scribejava.core.model.OAuthConstants.STATE;

public class MicrosoftEntraIDRequestAuthenticator implements RequestAuthenticator {

    public static final String EXTENSION_OAUTH_VERIFICATION_PATH_V3 = "/azuread/v3/oauth/callback";
    public static final String EXTENSION_OAUTH_VERIFICATION_PATH_V2 = "/azuread/callback";
    private final MicrosoftEntraIDAttributesStore azureADAttributesStore;
    private final String invokerId;
    private final AuthorizationContext authorizationContext;
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrosoftEntraIDRequestAuthenticator.class);

    public MicrosoftEntraIDRequestAuthenticator(MicrosoftEntraIDAttributesStore azureADAttributesStore, String invokerId, AuthorizationContext authorizationContext) {
        this.azureADAttributesStore = azureADAttributesStore;
        this.invokerId = invokerId;
        this.authorizationContext = authorizationContext;
    }

    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public Set<String> getSupportedProtocols() {
        return Set.of();
    }

    @Override
    public String getAuthenticatedAccountId(ProtoRequest protoRequest) {
        try {
            HttpRequest httpRequest = (HttpRequest) protoRequest;
            if (EXTENSION_OAUTH_VERIFICATION_PATH_V3.equals(httpRequest.getUri().getPath()) ||
                    EXTENSION_OAUTH_VERIFICATION_PATH_V2.equals(httpRequest.getUri().getPath())) {
                httpRequest.bufferBody();
                MultivaluedMap<String, String> queryParameters = httpRequest.getQueryParameters();
                String state = String.valueOf(queryParameters.get(STATE).get(0));
                String[] parts = state.split(Constants.HASH);
                return parts[0];
            } else {
                return null;
            }
        } catch (RuntimeException | IOException cause) {
            LOGGER.error("Failed to get the Authenticated Account Id", cause);
            return null;
        }
    }

    @Override
    public boolean setServiceAuthorization(String s) {
        return false;
    }

    @Override
    public Map<String, NamedField> getAttributeFields() {
        return Map.of();
    }

    public ProtoResponse getMustAuthenticateResponse(MustAuthenticateException cause, ProtoRequest request) {
        return null;
    }

    @Override
    public AuthorizationResponse getMustAuthenticateResponse(MustAuthenticateException cause) {
        return null;
    }

    @Override
    public ProtoResponse getMustAuthorizeResponse(MustAuthorizeException cause, ProtoRequest request) {
        return null;
    }

    @Override
    public AuthorizationResponse getMustAuthorizeResponse(MustAuthorizeException cause) {
        String userId = (String) cause.getDetails().get(0).getValue();
        Optional<NamedValuedField> authContext = cause.getDetails().stream()
                .filter(namedValuedField -> Objects.equals(namedValuedField.getName(), Constants.AUTH_CONTEXT_ID))
                .findFirst();
        String state = userId;
        MicrosoftEntraIDAttributes attributes;
        if (authContext.isPresent()) {
            String authContextId = (String) authContext.get().getValue();
            attributes = azureADAttributesStore.load(authContextId);
            state += Constants.HASH + authContextId;
        } else {
            attributes = azureADAttributesStore.load(invokerId);
        }
        if (Constants.PUBLIC.equals(attributes.getAuthType())) {
            state += Constants.HASH + attributes.getForwardPath();
        }
        OAuth20Service oAuth20Service = new OAuthService(attributes).getOAuth20Service();
        String url = oAuth20Service.getAuthorizationUrl(state) + Constants.AUTH_URL_QUERY_PARAMS;
        return new AuthorizationResponse(url, Collections.emptyList());
    }
}