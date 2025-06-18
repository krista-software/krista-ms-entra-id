package com.kristasoft.outlook.service.impl;

import com.kristasoft.outlook.exception.EmailInboxException;
import com.kristasoft.outlook.model.AccessToken;
import com.kristasoft.outlook.model.EmailMessage;
import com.kristasoft.outlook.model.InboxResult;
import com.kristasoft.outlook.model.OutlookConfiguration;
import com.kristasoft.outlook.repository.OutlookConfigurationEntity;
import com.kristasoft.outlook.repository.OutlookConfigurationRepository;
import com.kristasoft.outlook.security.AesEncryptionService;
import com.kristasoft.outlook.security.EncryptionService;
import com.kristasoft.outlook.service.EmailInboxService;
import com.kristasoft.outlook.service.GraphApiService;
import com.kristasoft.outlook.service.TokenCacheService;
import com.kristasoft.outlook.service.TokenService;

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Integration tests for OutlookEmailInboxServiceImpl
 * Note: These tests use real credentials and make actual API calls.
 * They should be skipped in automated CI/CD environments.
 */
public class OutlookEmailInboxServiceImplTest {
    private static final Logger LOGGER = Logger.getLogger(OutlookEmailInboxServiceImplTest.class.getName());
    
    // Test credentials - replace with valid test credentials
    private static final String TENANT_ID = "3694f6b4-b5f1-47ef-852f-a0b4a459ab44";
    private static final String CLIENT_ID = "ec0745c8-7635-4b31-97cc-d217944dd620";
    private static final String CLIENT_SECRET = "REDACTED_SECRET_1";
    private static final String SERVICE_ACCOUNT_EMAIL = "service.automation@kristasoft.com";
    
    private EmailInboxService inboxService;
    private TestConfigurationRepository testRepository;
    private EncryptionService encryptionService;
    private TokenCacheService tokenCacheService;
    private GraphApiService graphApiService;
    
    @Before
    public void setUp() {
        // Create encryption service with a test key
        String encryptionKey = AesEncryptionService.generateEncryptionKey();
        encryptionService = new AesEncryptionService(encryptionKey);
        
        // Create test repository
        testRepository = new TestConfigurationRepository();
        
        // Create token service and cache
        TokenService tokenService = new MicrosoftTokenServiceImpl();
        tokenCacheService = new TokenCacheServiceImpl(tokenService);
        
        // Create Graph API service
        graphApiService = new MicrosoftGraphApiServiceImpl();
        
        // Create the service under test
        inboxService = new OutlookEmailInboxServiceImpl(
                testRepository,
                tokenCacheService,
                encryptionService,
                graphApiService);
        
        // Setup test configuration
        setupTestConfiguration();
    }
    
    private void setupTestConfiguration() {
        // Create and encrypt client secret
        String encryptedSecret;
        try {
            encryptedSecret = encryptionService.encrypt(CLIENT_SECRET);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt client secret for test", e);
        }
        
        // Create test configuration entity
        OutlookConfigurationEntity config = new OutlookConfigurationEntity();
        config.setTenantId(TENANT_ID);
        config.setClientId(CLIENT_ID);
        config.setClientSecretEncrypted(encryptedSecret);
        config.setAuthorityUrl("https://login.microsoftonline.com/" + TENANT_ID);
        config.setGraphApiBaseUrl("https://graph.microsoft.com/v1.0");
        config.setRedirectUri("https://localhost:8080/oauth/callback");
        config.setScopes("Mail.Read,Mail.ReadWrite,Mail.Send,Calendars.ReadWrite,User.Read.All");
        config.setMultiTenant(false);
        config.setActive(true);
        config.setUpdatedAt(Instant.now());
        
        // Add to repository
        testRepository.addConfiguration(config);
    }
    
    /**
     * Helper method to determine if live tests should be skipped.
     * This can be controlled via environment variable or system property.
     */
    private boolean shouldSkipLiveTests() {
        // Check environment variable
        String skipEnv = System.getenv("SKIP_LIVE_TESTS");
        if ("true".equalsIgnoreCase(skipEnv)) {
            return true;
        }
        
        // Check system property
        String skipProp = System.getProperty("skipLiveTests");
        if ("true".equalsIgnoreCase(skipProp)) {
            return true;
        }
        
        // By default, skip live tests to avoid unexpected API calls
        return false;
    }
    
    @Test
    public void testListInboxEmailsWithDefaultParameters() throws Exception {
        // Skip this test if we should skip live tests
        if (shouldSkipLiveTests()) {
            LOGGER.info("Skipping live test: testListInboxEmailsWithDefaultParameters");
            return;
        }
        
        // Act
        InboxResult result = inboxService.listInboxEmails(null, null, null);
        
        // Assert
        assertNotNull("Result should not be null", result);
        assertNotNull("Email list should not be null", result.getEmails());
        assertTrue("Total count should be positive", result.getTotalCount() >= 0);
        
        // Log results
        LOGGER.info("Retrieved " + result.getEmails().size() + " emails out of " + result.getTotalCount() + " total");
        
        // Verify email properties if any emails were returned
        if (!result.getEmails().isEmpty()) {
            EmailMessage firstEmail = result.getEmails().get(0);
            assertNotNull("Message ID should not be null", firstEmail.getMessageId());
            assertNotNull("Subject should not be null", firstEmail.getSubject());
            assertNotNull("Sender email should not be null", firstEmail.getSenderEmail());
            assertNotNull("Received date should not be null", firstEmail.getReceivedDateTime());
            assertNotNull("Read status should not be null", firstEmail.getIsRead());
        }
    }
    
    @Test
    public void testListInboxEmailsWithCustomPageSize() throws Exception {
        // Skip this test if we should skip live tests
        if (shouldSkipLiveTests()) {
            LOGGER.info("Skipping live test: testListInboxEmailsWithCustomPageSize");
            return;
        }
        
        // Arrange
        int customPageSize = 10;
        
        // Act
        InboxResult result = inboxService.listInboxEmails(customPageSize, null, null);
        
        // Assert
        assertNotNull("Result should not be null", result);
        assertNotNull("Email list should not be null", result.getEmails());
        
        // Verify that we got at most the requested number of emails
        assertTrue("Should have at most " + customPageSize + " emails", 
                  result.getEmails().size() <= customPageSize);
        
        LOGGER.info("Retrieved " + result.getEmails().size() + " emails with page size " + customPageSize);
    }
    
    @Test
    public void testListInboxEmailsWithPagination() throws Exception {
        // Skip this test if we should skip live tests
        if (shouldSkipLiveTests()) {
            LOGGER.info("Skipping live test: testListInboxEmailsWithPagination");
            return;
        }
        
        // Arrange
        int pageSize = 5;
        int pageNumber = 1; // Second page
        
        // Act
        InboxResult result = inboxService.listInboxEmails(pageSize, pageNumber, null);
        
        // Assert
        assertNotNull("Result should not be null", result);
        assertNotNull("Email list should not be null", result.getEmails());
        
        // Verify that we got at most the requested number of emails
        assertTrue("Should have at most " + pageSize + " emails", 
                  result.getEmails().size() <= pageSize);
        
        LOGGER.info("Retrieved " + result.getEmails().size() + " emails from page " + pageNumber);
        
        // Get first page to compare
        if (result.getEmails().size() > 0) {
            InboxResult firstPageResult = inboxService.listInboxEmails(pageSize, 0, null);
            
            // If both pages have emails, verify they're different
            if (!firstPageResult.getEmails().isEmpty() && !result.getEmails().isEmpty()) {
                String firstPageId = firstPageResult.getEmails().get(0).getMessageId();
                String secondPageId = result.getEmails().get(0).getMessageId();
                
                assertNotEquals("First email on different pages should have different IDs",
                               firstPageId, secondPageId);
            }
        }
    }
    
    @Test
    public void testListInboxEmailsWithoutReadStatus() throws Exception {
        // Skip this test if we should skip live tests
        if (shouldSkipLiveTests()) {
            LOGGER.info("Skipping live test: testListInboxEmailsWithoutReadStatus");
            return;
        }
        
        // Act
        InboxResult result = inboxService.listInboxEmails(null, null, false);
        
        // Assert
        assertNotNull("Result should not be null", result);
        assertNotNull("Email list should not be null", result.getEmails());
        
        // Verify that emails don't have read status (should be null)
        if (!result.getEmails().isEmpty()) {
            EmailMessage firstEmail = result.getEmails().get(0);
            assertNull("Read status should be null when not requested", firstEmail.getIsRead());
        }
        
        LOGGER.info("Retrieved " + result.getEmails().size() + " emails without read status");
    }
    
    @Test(expected = EmailInboxException.class)
    public void testListInboxEmailsWithInvalidPageSize() throws Exception {
        // This test doesn't need to be skipped as it doesn't make API calls
        inboxService.listInboxEmails(-1, null, null);
    }
    
    @Test(expected = EmailInboxException.class)
    public void testListInboxEmailsWithTooLargePageSize() throws Exception {
        // This test doesn't need to be skipped as it doesn't make API calls
        inboxService.listInboxEmails(1000, null, null);
    }
    
    @Test(expected = EmailInboxException.class)
    public void testListInboxEmailsWithInvalidPageNumber() throws Exception {
        // This test doesn't need to be skipped as it doesn't make API calls
        inboxService.listInboxEmails(null, -1, null);
    }
    
    @Test
    public void testListInboxEmailsWithNoActiveConfiguration() throws Exception {
        // Arrange - clear the repository
        testRepository.clearConfigurations();
        
        try {
            // Act
            inboxService.listInboxEmails(null, null, null);
            fail("Should throw exception when no active configuration exists");
        } catch (EmailInboxException e) {
            // Assert
            assertEquals("No active Outlook configuration found", e.getMessage());
            
            // This is expected, so we don't need to log it as an error
            LOGGER.info("Caught expected exception: " + e.getMessage());
        }
    }
    
    /**
     * Test implementation of OutlookConfigurationRepository
     */
    private static class TestConfigurationRepository implements OutlookConfigurationRepository {
        private final List<OutlookConfigurationEntity> configurations = new ArrayList<>();
        
        public void addConfiguration(OutlookConfigurationEntity config) {
            configurations.add(config);
        }
        
        public void clearConfigurations() {
            configurations.clear();
        }
        
        public List<OutlookConfigurationEntity> getConfigurations() {
            return configurations;
        }

        @Override
        public void saveConfiguration(String tenantId, String clientId, String encryptedClientSecret, String authorityUrl, String scopes, String graphApiBaseUrl, boolean multiTenant, String redirectUri, Instant updatedAt) throws Exception {

        }

        @Override
        public List<OutlookConfigurationEntity> findAllActive() {
            return configurations.stream()
                .filter(OutlookConfigurationEntity::isActive)
                .collect(java.util.stream.Collectors.toList());
        }
    }
}
