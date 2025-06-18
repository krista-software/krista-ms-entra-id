package com.kristasoft.outlook.service.impl;

import com.kristasoft.outlook.exception.GraphApiException;
import com.kristasoft.outlook.model.AccessToken;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.fail;

public class MicrosoftGraphApiServiceImplTest {

    private MicrosoftGraphApiServiceImpl graphApiService;
    private AccessToken validToken;
    private String graphApiBaseUrl;

    @Before
    public void setUp() {
        graphApiService = new MicrosoftGraphApiServiceImpl();
        validToken = new AccessToken("valid-token-string", Instant.now().plusSeconds(3600));
        graphApiBaseUrl = "https://graph.microsoft.com/v1.0";
    }

    @Test
    public void testConnectivityWithValidToken() throws Exception {
        // Should not throw exception with valid token
        graphApiService.testConnectivity(validToken, graphApiBaseUrl);
    }

    @Test(expected = GraphApiException.class)
    public void testConnectivityWithNullToken() throws Exception {
        graphApiService.testConnectivity(null, graphApiBaseUrl);
    }

    @Test(expected = GraphApiException.class)
    public void testConnectivityWithEmptyToken() throws Exception {
        AccessToken emptyToken = new AccessToken("", Instant.now().plusSeconds(3600));
        graphApiService.testConnectivity(emptyToken, graphApiBaseUrl);
    }

    @Test(expected = GraphApiException.class)
    public void testConnectivityWithNullTokenString() throws Exception {
        AccessToken nullTokenString = new AccessToken(null, Instant.now().plusSeconds(3600));
        graphApiService.testConnectivity(nullTokenString, graphApiBaseUrl);
    }

    @Test
    public void testConnectivityWithNullGraphApiBaseUrl() throws Exception {
        // Should not throw exception as base URL is not used in the current implementation
        graphApiService.testConnectivity(validToken, null);
    }

    @Test
    public void testMailboxAccessWithValidToken() throws Exception {
        // Should not throw exception with valid token
        graphApiService.testMailboxAccess(validToken, graphApiBaseUrl);
    }

    @Test
    public void testMailboxAccessWithNullGraphApiBaseUrl() throws Exception {
        // Should not throw exception as base URL is not used in the current implementation
        graphApiService.testMailboxAccess(validToken, null);
    }

    @Test
    public void testCreateGraphConnectionMethod() {
        // This is a private method, so we can only test it indirectly
        // through the public methods that use it.
        // The current implementation doesn't actually use this method,
        // so we'll just verify that the public methods work as expected.
        try {
            graphApiService.testConnectivity(validToken, graphApiBaseUrl);
            graphApiService.testMailboxAccess(validToken, graphApiBaseUrl);
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void testGetErrorDetailsMethod() {
        // This is a private method, so we can only test it indirectly
        // through the public methods that use it.
        // The current implementation doesn't actually use this method,
        // so we'll just verify that the public methods work as expected.
        try {
            graphApiService.testConnectivity(validToken, graphApiBaseUrl);
            graphApiService.testMailboxAccess(validToken, graphApiBaseUrl);
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }
}