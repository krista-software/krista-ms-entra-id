package com.kristasoft.outlook.service;

import com.kristasoft.outlook.model.AccessToken;
import com.kristasoft.outlook.model.OutlookConfiguration;
import com.kristasoft.outlook.exception.AuthenticationException;

/**
 * Service interface for caching and managing access tokens.
 */
public interface TokenCacheService {
    
    /**
     * Gets a valid access token for the given configuration.
     * Will return a cached token if available and not expired,
     * or acquire a new token if needed.
     * 
     * @param config The configuration to get a token for
     * @return A valid access token
     * @throws AuthenticationException if token acquisition fails
     */
    AccessToken getToken(OutlookConfiguration config) throws AuthenticationException;
    
    /**
     * Invalidates any cached token for the given configuration.
     * Forces a new token to be acquired on the next request.
     * 
     * @param config The configuration to invalidate tokens for
     */
    void invalidateToken(OutlookConfiguration config);
    
    /**
     * Clears all cached tokens.
     */
    void clearCache();
}