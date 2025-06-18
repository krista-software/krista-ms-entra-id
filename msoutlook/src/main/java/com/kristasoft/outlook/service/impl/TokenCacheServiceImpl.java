package com.kristasoft.outlook.service.impl;

import com.kristasoft.outlook.model.AccessToken;
import com.kristasoft.outlook.model.OutlookConfiguration;
import com.kristasoft.outlook.service.TokenCacheService;
import com.kristasoft.outlook.service.TokenService;
import com.kristasoft.outlook.exception.AuthenticationException;
import com.kristasoft.outlook.store.KeyValueStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of TokenCacheService that caches tokens in memory
 * and handles token renewal. Uses KeyValueStore as a fallback.
 */
public class TokenCacheServiceImpl implements TokenCacheService {
    
    private static final Logger LOGGER = Logger.getLogger(TokenCacheServiceImpl.class.getName());
    
    // Cache of tokens keyed by tenant ID
    private final Map<String, AccessToken> tokenCache = new ConcurrentHashMap<>();
    
    // Service for acquiring new tokens
    private final TokenService tokenService;
    
    // Persistent store for tokens
    private final KeyValueStore persistentStore;
    
    // Buffer time before expiration to renew token (5 minutes)
    private static final Duration EXPIRY_BUFFER = Duration.ofMinutes(5);
    
    // Key prefix for tokens in the persistent store
    private static final String TOKEN_KEY_PREFIX = "outlook_token_";
    
    /**
     * Constructor with dependency injection.
     * 
     * @param tokenService Service for acquiring new tokens
     */
    public TokenCacheServiceImpl(TokenService tokenService) {
        this.tokenService = tokenService;
        this.persistentStore = new KeyValueStore();
    }
    
    /**
     * Constructor with dependency injection for both token service and persistent store.
     * 
     * @param tokenService Service for acquiring new tokens
     * @param persistentStore Persistent store for tokens
     */
    public TokenCacheServiceImpl(TokenService tokenService, KeyValueStore persistentStore) {
        this.tokenService = tokenService;
        this.persistentStore = persistentStore != null ? persistentStore : new KeyValueStore();
    }
    
    @Override
    public AccessToken getToken(OutlookConfiguration config) throws AuthenticationException {
        if (config == null || config.getTenantId() == null) {
            throw new AuthenticationException("Invalid configuration or missing tenant ID", null);
        }
        
        String cacheKey = createCacheKey(config);
        AccessToken cachedToken = tokenCache.get(cacheKey);
        
        // Check if we have a valid cached token in memory
        if (isTokenValid(cachedToken)) {
            LOGGER.fine("Using in-memory cached token for tenant: " + config.getTenantId());
            return cachedToken;
        }
        
        // No valid in-memory token, try to get from persistent store
        AccessToken persistentToken = getTokenFromPersistentStore(cacheKey);
        if (isTokenValid(persistentToken)) {
            LOGGER.info("Using persistent store token for tenant: " + config.getTenantId());
            // Cache the token in memory for future use
            tokenCache.put(cacheKey, persistentToken);
            return persistentToken;
        }
        
        // No valid token in either cache, acquire a new one
        LOGGER.info("Acquiring new token for tenant: " + config.getTenantId());
        AccessToken newToken = tokenService.acquireToken(config);
        
        // Cache the new token in memory
        tokenCache.put(cacheKey, newToken);
        
        // Also save to persistent store
        saveTokenToPersistentStore(cacheKey, newToken);
        
        LOGGER.info("Cached new token for tenant: " + config.getTenantId() + 
                   " expiring at: " + newToken.getExpiresOn());
        
        return newToken;
    }
    
    @Override
    public void invalidateToken(OutlookConfiguration config) {
        if (config == null || config.getTenantId() == null) {
            return;
        }
        
        String cacheKey = createCacheKey(config);
        AccessToken removedToken = tokenCache.remove(cacheKey);
        
        // Also remove from persistent store
        persistentStore.remove(TOKEN_KEY_PREFIX + cacheKey);
        
        if (removedToken != null) {
            LOGGER.info("Invalidated token for tenant: " + config.getTenantId());
        }
    }
    
    @Override
    public void clearCache() {
        int count = tokenCache.size();
        tokenCache.clear();
        
        // Clear tokens from persistent store
        // This is a simple approach - in a more sophisticated implementation,
        // we might want to only clear tokens with a specific prefix
        persistentStore.clear();
        
        LOGGER.info("Cleared token cache (" + count + " tokens removed)");
    }
    
    /**
     * Checks if a token is valid and not near expiration.
     * 
     * @param token The token to check
     * @return true if the token is valid and not near expiration
     */
    private boolean isTokenValid(AccessToken token) {
        if (token == null) {
            return false;
        }
        
        // Check if token is expired or will expire soon
        Instant now = Instant.now();
        Instant bufferExpiry = token.getExpiresOn().minus(EXPIRY_BUFFER);
        
        return now.isBefore(bufferExpiry);
    }
    
    /**
     * Creates a cache key for the given configuration.
     * Uses tenant ID, client ID, and authority URL to ensure uniqueness.
     * 
     * @param config The configuration to create a key for
     * @return A unique cache key
     */
    private String createCacheKey(OutlookConfiguration config) {
        return config.getTenantId() + "|" + config.getClientId() + "|" + config.getAuthorityUrl();
    }
    
    /**
     * Retrieves a token from the persistent store.
     * 
     * @param cacheKey The cache key for the token
     * @return The token, or null if not found or not deserializable
     */
    private AccessToken getTokenFromPersistentStore(String cacheKey) {
        try {
            String storeKey = TOKEN_KEY_PREFIX + cacheKey;
            Map<String, Object> tokenData = (Map<String, Object>) persistentStore.get(storeKey);
            
            if (tokenData == null) {
                return null;
            }
            
            String tokenValue = (String) tokenData.get("token");
            String expiresOnStr = (String) tokenData.get("expiresOn");
            
            if (tokenValue == null || expiresOnStr == null) {
                return null;
            }
            
            Instant expiresOn = Instant.parse(expiresOnStr);
            return new AccessToken(tokenValue, expiresOn);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to retrieve token from persistent store: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Saves a token to the persistent store.
     * 
     * @param cacheKey The cache key for the token
     * @param token The token to save
     */
    private void saveTokenToPersistentStore(String cacheKey, AccessToken token) {
        try {
            String storeKey = TOKEN_KEY_PREFIX + cacheKey;
            
            Map<String, Object> tokenData = new java.util.HashMap<>();
            tokenData.put("token", token.getToken());
            tokenData.put("expiresOn", token.getExpiresOn().toString());
            
            persistentStore.put(storeKey, tokenData);
            
            LOGGER.fine("Saved token to persistent store with key: " + storeKey);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to save token to persistent store: " + e.getMessage(), e);
        }
    }
}