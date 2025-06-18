package com.kristasoft.outlook.service.impl;

import com.kristasoft.outlook.exception.AuthenticationException;
import com.kristasoft.outlook.model.AccessToken;
import com.kristasoft.outlook.model.OutlookConfiguration;
import com.kristasoft.outlook.service.TokenCacheService;
import com.kristasoft.outlook.service.TokenService;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class TokenCacheServiceImplTest {
    
    private TokenCacheService tokenCacheService;
    private CountingTokenService tokenService;
    private OutlookConfiguration config;
    
    @Before
    public void setUp() {
        // Create a token service that counts acquisitions
        tokenService = new CountingTokenService();
        
        // Create the service under test
        tokenCacheService = new TokenCacheServiceImpl(tokenService);
        
        // Create a test configuration
        config = new OutlookConfiguration();
        config.setTenantId("test-tenant-id");
        config.setClientId("test-client-id");
        config.setClientSecret("test-client-secret");
        config.setAuthorityUrl("https://login.microsoftonline.com/test-tenant-id");
    }
    
    @Test
    public void testGetTokenAcquiresNewToken() throws AuthenticationException {
        // Act
        AccessToken token = tokenCacheService.getToken(config);
        
        // Assert
        assertNotNull("Token should not be null", token);
        assertEquals("Token acquisition count should be 1", 1, tokenService.getAcquisitionCount());
    }
    
    @Test
    public void testGetTokenReusesCachedToken() throws AuthenticationException {
        // Act
        AccessToken token1 = tokenCacheService.getToken(config);
        AccessToken token2 = tokenCacheService.getToken(config);
        
        // Assert
        assertNotNull("First token should not be null", token1);
        assertNotNull("Second token should not be null", token2);
        assertSame("Tokens should be the same instance", token1, token2);
        assertEquals("Token acquisition count should be 1", 1, tokenService.getAcquisitionCount());
    }
    
    @Test
    public void testGetTokenRenewsExpiredToken() throws AuthenticationException {
        // Arrange - set token service to return expired tokens
        tokenService.setReturnExpiredTokens(true);
        
        // Act
        AccessToken token1 = tokenCacheService.getToken(config);
        AccessToken token2 = tokenCacheService.getToken(config);
        
        // Assert
        assertNotNull("First token should not be null", token1);
        assertNotNull("Second token should not be null", token2);
        assertNotSame("Tokens should be different instances", token1, token2);
        assertEquals("Token acquisition count should be 2", 2, tokenService.getAcquisitionCount());
    }
    
    @Test
    public void testInvalidateToken() throws AuthenticationException {
        // Arrange
        AccessToken token1 = tokenCacheService.getToken(config);
        
        // Act
        tokenCacheService.invalidateToken(config);
        AccessToken token2 = tokenCacheService.getToken(config);
        
        // Assert
        assertNotNull("First token should not be null", token1);
        assertNotNull("Second token should not be null", token2);
        assertNotSame("Tokens should be different instances", token1, token2);
        assertEquals("Token acquisition count should be 2", 2, tokenService.getAcquisitionCount());
    }
    
    @Test
    public void testClearCache() throws AuthenticationException {
        // Arrange
        AccessToken token1 = tokenCacheService.getToken(config);
        
        // Act
        tokenCacheService.clearCache();
        AccessToken token2 = tokenCacheService.getToken(config);
        
        // Assert
        assertNotNull("First token should not be null", token1);
        assertNotNull("Second token should not be null", token2);
        assertNotSame("Tokens should be different instances", token1, token2);
        assertEquals("Token acquisition count should be 2", 2, tokenService.getAcquisitionCount());
    }
    
    @Test
    public void testDifferentConfigsGetDifferentTokens() throws AuthenticationException {
        // Arrange
        OutlookConfiguration config2 = new OutlookConfiguration();
        config2.setTenantId("different-tenant-id");
        config2.setClientId("test-client-id");
        config2.setClientSecret("test-client-secret");
        config2.setAuthorityUrl("https://login.microsoftonline.com/different-tenant-id");
        
        // Act
        AccessToken token1 = tokenCacheService.getToken(config);
        AccessToken token2 = tokenCacheService.getToken(config2);
        
        // Assert
        assertNotNull("First token should not be null", token1);
        assertNotNull("Second token should not be null", token2);
        assertNotSame("Tokens should be different instances", token1, token2);
        assertEquals("Token acquisition count should be 2", 2, tokenService.getAcquisitionCount());
    }
    
    /**
     * Test implementation of TokenService that counts token acquisitions
     */
    private static class CountingTokenService implements TokenService {
        private final AtomicInteger acquisitionCount = new AtomicInteger(0);
        private boolean returnExpiredTokens = false;
        
        public int getAcquisitionCount() {
            return acquisitionCount.get();
        }
        
        public void setReturnExpiredTokens(boolean returnExpiredTokens) {
            this.returnExpiredTokens = returnExpiredTokens;
        }
        
        @Override
        public AccessToken acquireToken(OutlookConfiguration config) throws AuthenticationException {
            acquisitionCount.incrementAndGet();
            
            Instant expiresOn = returnExpiredTokens ? 
                Instant.now().minusSeconds(60) : // Expired token
                Instant.now().plusSeconds(3600); // Valid token
                
            return new AccessToken("test-token-" + acquisitionCount.get(), expiresOn);
        }
    }
}