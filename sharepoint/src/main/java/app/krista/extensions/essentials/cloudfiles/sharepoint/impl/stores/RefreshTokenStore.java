package app.krista.extensions.essentials.cloudfiles.sharepoint.impl.stores;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.util.KeyValueStore;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public final class RefreshTokenStore {

    private final KeyValueStore keyValueStore;

    private final GraphServiceClientProvider provider;

    @Inject
    public RefreshTokenStore(KeyValueStore keyValueStore, GraphServiceClientProvider provider) {
        this.keyValueStore = keyValueStore;
        this.provider = provider;
    }

    public void put(String key, String refToken) {
        keyValueStore.put(key, refToken);
    }

    public String get(String key) {
        return keyValueStore.get(key, String.class);
    }

    public void remove(String key) {
        keyValueStore.remove(key);
    }

    /**
     Validates the refresh token.
     @param accountID The account identifier.
     @throws MustAuthorizeException If refresh token is null then it will throw MustAuthorize Exception.
     */
    public void validateRefreshToken(String accountID) {
        provider.getUserRequestBuilder(false, accountID);
    }

}
