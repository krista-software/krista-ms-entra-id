package example;

import com.kristasoft.outlook.exception.AuthenticationException;
import com.kristasoft.outlook.model.AccessToken;
import com.kristasoft.outlook.model.OutlookConfiguration;
import com.kristasoft.outlook.service.TokenCacheService;
import com.kristasoft.outlook.service.TokenService;
import com.kristasoft.outlook.service.impl.MicrosoftTokenServiceImpl;
import com.kristasoft.outlook.service.impl.TokenCacheServiceImpl;
import com.kristasoft.outlook.store.KeyValueStore;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example demonstrating how to use the TokenCacheService with real credentials
 * This class tests token acquisition, caching, and persistence
 */
public class TokenCacheExample {
    private static final Logger LOGGER = Logger.getLogger(TokenCacheExample.class.getName());
    
    // Test credentials
    private static final String TENANT_ID = "3694f6b4-b5f1-47ef-852f-a0b4a459ab44";
    private static final String CLIENT_ID = "ec0745c8-7635-4b31-97cc-d217944dd620";
    private static final String CLIENT_SECRET = "REDACTED_SECRET_1";
    
    public static void main(String[] args) {
        try {
            // Create the token service
            TokenService tokenService = new MicrosoftTokenServiceImpl();
            
            // Create a persistent store
            KeyValueStore persistentStore = new KeyValueStore();
            
            // Create the token cache service
            TokenCacheService tokenCacheService = new TokenCacheServiceImpl(tokenService, persistentStore);
            
            // Create test configuration
            OutlookConfiguration config = createTestConfiguration();
            
            // Example 1: First token acquisition (should go to Microsoft)
            testFirstTokenAcquisition(tokenCacheService, config);
            
            // Example 2: Second token acquisition (should use in-memory cache)
            testSecondTokenAcquisition(tokenCacheService, config);
            
            // Example 3: Create new cache service instance (should use persistent store)
            testPersistentStoreRetrieval(tokenService, persistentStore, config);
            
            // Example 4: Test token invalidation
            testTokenInvalidation(tokenCacheService, config);
            
            // Example 5: Test cache clearing
            testCacheClearing(tokenCacheService, config);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in example: " + e.getMessage(), e);
        }
    }
    
    private static OutlookConfiguration createTestConfiguration() {
        OutlookConfiguration config = new OutlookConfiguration();
        config.setTenantId(TENANT_ID);
        config.setClientId(CLIENT_ID);
        config.setClientSecret(CLIENT_SECRET);
        config.setAuthorityUrl("https://login.microsoftonline.com/" + TENANT_ID);
        config.setGraphApiBaseUrl("https://graph.microsoft.com/v1.0");
        return config;
    }
    
    private static void testFirstTokenAcquisition(TokenCacheService tokenCacheService, OutlookConfiguration config) {
        try {
            LOGGER.info("=== Example 1: First Token Acquisition ===");
            LOGGER.info("This should acquire a new token from Microsoft");
            
            long startTime = System.currentTimeMillis();
            AccessToken token = tokenCacheService.getToken(config);
            long endTime = System.currentTimeMillis();
            
            LOGGER.info("Token acquired successfully in " + (endTime - startTime) + "ms");
            LOGGER.info("Token: " + maskToken(token.getToken()));
            LOGGER.info("Expires at: " + token.getExpiresOn());
            LOGGER.info("Is expired: " + token.isExpired());
            LOGGER.info("");
            
        } catch (AuthenticationException e) {
            LOGGER.log(Level.SEVERE, "Authentication failed: " + e.getMessage(), e);
        }
    }
    
    private static void testSecondTokenAcquisition(TokenCacheService tokenCacheService, OutlookConfiguration config) {
        try {
            LOGGER.info("=== Example 2: Second Token Acquisition ===");
            LOGGER.info("This should use the in-memory cache");
            
            long startTime = System.currentTimeMillis();
            AccessToken token = tokenCacheService.getToken(config);
            long endTime = System.currentTimeMillis();
            
            LOGGER.info("Token retrieved in " + (endTime - startTime) + "ms");
            LOGGER.info("Token: " + maskToken(token.getToken()));
            LOGGER.info("Expires at: " + token.getExpiresOn());
            LOGGER.info("");
            
            // If retrieval was very fast, it likely came from cache
            if (endTime - startTime < 50) {
                LOGGER.info("Fast retrieval time indicates token came from in-memory cache");
            } else {
                LOGGER.info("Slower retrieval time suggests token might not have come from cache");
            }
            LOGGER.info("");
            
        } catch (AuthenticationException e) {
            LOGGER.log(Level.SEVERE, "Authentication failed: " + e.getMessage(), e);
        }
    }
    
    private static void testPersistentStoreRetrieval(TokenService tokenService, KeyValueStore persistentStore, 
                                                    OutlookConfiguration config) {
        try {
            LOGGER.info("=== Example 3: Persistent Store Retrieval ===");
            LOGGER.info("Creating new cache service instance (simulates application restart)");
            
            // Create a new cache service instance (simulates application restart)
            TokenCacheService newCacheService = new TokenCacheServiceImpl(tokenService, persistentStore);
            
            LOGGER.info("Retrieving token from new cache service instance");
            LOGGER.info("This should use the persistent store since in-memory cache is empty");
            
            long startTime = System.currentTimeMillis();
            AccessToken token = newCacheService.getToken(config);
            long endTime = System.currentTimeMillis();
            
            LOGGER.info("Token retrieved in " + (endTime - startTime) + "ms");
            LOGGER.info("Token: " + maskToken(token.getToken()));
            LOGGER.info("Expires at: " + token.getExpiresOn());
            LOGGER.info("");
            
        } catch (AuthenticationException e) {
            LOGGER.log(Level.SEVERE, "Authentication failed: " + e.getMessage(), e);
        }
    }
    
    private static void testTokenInvalidation(TokenCacheService tokenCacheService, OutlookConfiguration config) {
        try {
            LOGGER.info("=== Example 4: Token Invalidation ===");
            
            // First get a token to ensure it's in cache
            AccessToken token = tokenCacheService.getToken(config);
            LOGGER.info("Retrieved token: " + maskToken(token.getToken()));
            
            // Now invalidate it
            LOGGER.info("Invalidating token...");
            tokenCacheService.invalidateToken(config);
            
            // Try to get a token again - should get a new one
            LOGGER.info("Retrieving token after invalidation");
            long startTime = System.currentTimeMillis();
            AccessToken newToken = tokenCacheService.getToken(config);
            long endTime = System.currentTimeMillis();
            
            LOGGER.info("Token retrieved in " + (endTime - startTime) + "ms");
            LOGGER.info("New token: " + maskToken(newToken.getToken()));
            LOGGER.info("");
            
            // If retrieval was slow, it likely went to Microsoft
            if (endTime - startTime > 500) {
                LOGGER.info("Slow retrieval time indicates a new token was acquired from Microsoft");
            } else {
                LOGGER.info("Fast retrieval time suggests token might still be coming from cache");
            }
            LOGGER.info("");
            
        } catch (AuthenticationException e) {
            LOGGER.log(Level.SEVERE, "Authentication failed: " + e.getMessage(), e);
        }
    }
    
    private static void testCacheClearing(TokenCacheService tokenCacheService, OutlookConfiguration config) {
        try {
            LOGGER.info("=== Example 5: Cache Clearing ===");
            
            // First get a token to ensure it's in cache
            AccessToken token = tokenCacheService.getToken(config);
            LOGGER.info("Retrieved token: " + maskToken(token.getToken()));
            
            // Now clear the cache
            LOGGER.info("Clearing entire token cache...");
            tokenCacheService.clearCache();
            
            // Try to get a token again - should get a new one
            LOGGER.info("Retrieving token after cache clearing");
            long startTime = System.currentTimeMillis();
            AccessToken newToken = tokenCacheService.getToken(config);
            long endTime = System.currentTimeMillis();
            
            LOGGER.info("Token retrieved in " + (endTime - startTime) + "ms");
            LOGGER.info("New token: " + maskToken(newToken.getToken()));
            LOGGER.info("");
            
            // If retrieval was slow, it likely went to Microsoft
            if (endTime - startTime > 500) {
                LOGGER.info("Slow retrieval time indicates a new token was acquired from Microsoft");
            } else {
                LOGGER.info("Fast retrieval time suggests token might still be coming from cache");
            }
            LOGGER.info("");
            
        } catch (AuthenticationException e) {
            LOGGER.log(Level.SEVERE, "Authentication failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Masks a token for display in logs
     * Shows only the first 10 and last 4 characters
     */
    private static String maskToken(String token) {
        if (token == null || token.length() <= 14) {
            return "[PROTECTED]";
        }
        
        return token.substring(0, 10) + "..." + token.substring(token.length() - 4);
    }
}