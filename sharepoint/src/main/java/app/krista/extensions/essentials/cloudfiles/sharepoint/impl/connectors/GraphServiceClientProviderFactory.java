package app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.stores.SharePointAttributeStore;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.stores.RefreshTokenStore;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class GraphServiceClientProviderFactory {

    private final RefreshTokenStore refreshTokenStore;
    private final SharePointAttributeStore sharePointAttributeStore;
    private final RequestContext requestContext;
    private final AuthorizationContext authorizationContext;
    private final GraphServiceClientProvider defaultClientProvider;

    @Inject
    public GraphServiceClientProviderFactory(RefreshTokenStore refreshTokenStore, SharePointAttributeStore sharePointAttributeStore, SharePointAttributes sharePointAttributes, RequestContext requestContext, AuthorizationContext authorizationContext) {
        this(refreshTokenStore, sharePointAttributeStore, requestContext, authorizationContext,
                new GraphServiceClientProvider(refreshTokenStore, sharePointAttributes, requestContext, authorizationContext, null));
    }

    public GraphServiceClientProviderFactory(RefreshTokenStore refreshTokenStore, SharePointAttributeStore sharePointAttributeStore, RequestContext requestContext, AuthorizationContext authorizationContext, GraphServiceClientProvider defaultClientProvider) {
        this.refreshTokenStore = refreshTokenStore;
        this.sharePointAttributeStore = sharePointAttributeStore;
        this.requestContext = requestContext;
        this.authorizationContext = authorizationContext;
        this.defaultClientProvider = defaultClientProvider;
    }

    public GraphServiceClientProvider create(SharePointAttributes sharePointAttributes) {
        String authContextId = sharePointAttributeStore.save(sharePointAttributes);
        return new GraphServiceClientProvider(refreshTokenStore, sharePointAttributes, requestContext, authorizationContext, authContextId);
    }

    public GraphServiceClientProvider create() {
        return defaultClientProvider;
    }

}
