package app.krista.extensions.essentials.cloudfiles.sharepoint;

import app.krista.extension.authorization.MustAuthenticateException;
import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.request.ProtoRequest;
import app.krista.extension.request.ProtoResponse;
import app.krista.extension.request.protos.http.HttpRequest;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.stores.SharePointAttributeStore;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.model.field.NamedField;
import app.krista.model.field.NamedValuedField;
import com.github.scribejava.core.oauth.OAuth20Service;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.*;

public class SharePointRequestAuthenticator implements RequestAuthenticator {

    public static final String SHAREPOINT_CALLBACK = "/sharepoint/callback";
    private final SharePointAttributeStore sharePointAttributeStore;
    private final SharePointAttributes sharePointAttributes;

    public SharePointRequestAuthenticator(SharePointAttributeStore sharePointAttributeStore,
                                          SharePointAttributes sharePointAttributes) {
        this.sharePointAttributeStore = sharePointAttributeStore;
        this.sharePointAttributes = sharePointAttributes;
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
            if (SHAREPOINT_CALLBACK.equals(httpRequest.getUri().getPath())) {
                httpRequest.bufferBody();
                MultivaluedMap<String, String> queryParameters = httpRequest.getQueryParameters();
                String state = String.valueOf(queryParameters.get(Constants.STATE).get(0));
                String[] parts = state.split(Constants.HASH);
                return parts[0];
            } else {
                return null;
            }
        } catch (RuntimeException | IOException cause) {
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

    @Override
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
        Optional<NamedValuedField> authContextIdField = cause.getDetails().stream()
                .filter(namedValuedField -> Objects.equals(namedValuedField.getName(), Constants.AUTH_CONTEXT_ID))
                .findFirst();

        SharePointAttributes effectiveSharePointAttributes;
        String state;
        if (authContextIdField.isPresent()) {
            String authContextId = (String) authContextIdField.get().getValue();
            effectiveSharePointAttributes = sharePointAttributeStore.load(authContextId);
            state = userId + Constants.HASH + authContextId;
        } else {
            effectiveSharePointAttributes = sharePointAttributes;
            state = userId;
        }
        OAuth20Service oAuth20Service = effectiveSharePointAttributes.getOAuth20Service();
        String url = oAuth20Service.getAuthorizationUrl(state);
        return new AuthorizationResponse(url + Constants.ACCESS_TYPE_OFFLINE_APPROVAL_PROMPT_FORCE,
                Collections.emptyList());
    }

}
