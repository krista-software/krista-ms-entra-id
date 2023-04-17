package app.krista.extensions.cyber_security.iam.azureAD.impl.connectors;

import app.krista.extensions.cyber_security.iam.azureAD.AzureADAttributes;
import app.krista.extensions.cyber_security.iam.azureAD.impl.stores.AzureADAttributesStore;
import app.krista.extensions.cyber_security.iam.azureAD.impl.stores.RefreshTokenStore;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public final class GraphServiceClientProviderFactory {

    private final RefreshTokenStore refreshTokenStore;
    private final AzureADAttributesStore azureADAttributesStore;
    private final RequestContext requestContext;
    private final AuthorizationContext authorizationContext;
    private final GraphServiceClientProvider defaultClientProvider;

    @Inject
    public GraphServiceClientProviderFactory(RefreshTokenStore refreshTokenStore, AzureADAttributesStore azureADAttributesStore, AzureADAttributes azureADAttributes, RequestContext requestContext, AuthorizationContext authorizationContext) {
        this(refreshTokenStore, azureADAttributesStore, requestContext, authorizationContext,
                new GraphServiceClientProvider(refreshTokenStore, azureADAttributes, requestContext, authorizationContext, null));
    }

    public GraphServiceClientProviderFactory(RefreshTokenStore refreshTokenStore, AzureADAttributesStore azureADAttributesStore, RequestContext requestContext, AuthorizationContext authorizationContext, GraphServiceClientProvider defaultClientProvider) {
        this.refreshTokenStore = refreshTokenStore;
        this.azureADAttributesStore = azureADAttributesStore;
        this.requestContext = requestContext;
        this.authorizationContext = authorizationContext;
        this.defaultClientProvider = defaultClientProvider;
    }

    public GraphServiceClientProvider create(AzureADAttributes azureADAttributes) {
        String authContextId = azureADAttributesStore.save(azureADAttributes);
        return new GraphServiceClientProvider(refreshTokenStore, azureADAttributes, requestContext, authorizationContext, authContextId);
    }

    public GraphServiceClientProvider create() {
        return defaultClientProvider;
    }
}
