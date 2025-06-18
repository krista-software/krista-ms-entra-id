package com.kristasoft.outlook.service.impl;

import com.kristasoft.outlook.model.AccessToken;
import com.kristasoft.outlook.model.ConfigurationSaveResult;
import com.kristasoft.outlook.model.OutlookConfiguration;
import com.kristasoft.outlook.service.ConfigurationService;
import com.kristasoft.outlook.service.ConfigurationValidator;
import com.kristasoft.outlook.service.GraphApiService;
import com.kristasoft.outlook.service.TokenService;
import com.kristasoft.outlook.exception.AuthenticationException;
import com.kristasoft.outlook.exception.ConfigurationStorageException;
import com.kristasoft.outlook.exception.ConfigurationValidationException;
import com.kristasoft.outlook.exception.GraphApiException;
import com.kristasoft.outlook.repository.OutlookConfigurationRepository;
import com.kristasoft.outlook.security.EncryptionService;

import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OutlookConfigurationServiceImpl implements ConfigurationService {
    private static final Logger LOGGER = Logger.getLogger(OutlookConfigurationServiceImpl.class.getName());
    
    // Krista OAuth App default values - updated with test credentials
    private static final String KRISTA_OAUTH_CLIENT_ID = "ec0745c8-7635-4b31-97cc-d217944dd620";
    private static final String KRISTA_OAUTH_CLIENT_SECRET = "REDACTED_SECRET_1";
    private static final String KRISTA_OAUTH_REDIRECT_URI = "https://localhost:8080/oauth/callback";
    private static final String DEFAULT_GRAPH_API_BASE_URL = "https://graph.microsoft.com/v1.0";
    
    private final ConfigurationValidator configValidator;
    private final TokenService tokenService;
    private final GraphApiService graphApiService;
    private final OutlookConfigurationRepository configRepository;
    private final EncryptionService encryptionService;
    
    public OutlookConfigurationServiceImpl(
            ConfigurationValidator configValidator,
            TokenService tokenService,
            GraphApiService graphApiService,
            OutlookConfigurationRepository configRepository,
            EncryptionService encryptionService) {
        this.configValidator = configValidator;
        this.tokenService = tokenService;
        this.graphApiService = graphApiService;
        this.configRepository = configRepository;
        this.encryptionService = encryptionService;
    }
    
    @Override
    public ConfigurationSaveResult saveConfiguration(boolean useKristaOAuthApp, OutlookConfiguration inputConfig) 
            throws ConfigurationStorageException {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Starting save configuration process for tenant: " + inputConfig.getTenantId() + 
                    " using " + (useKristaOAuthApp ? "Krista OAuth App" : "custom OAuth app"));
        
        try {
            // Step 1: Prepare configuration based on mode
            OutlookConfiguration finalConfig = prepareConfiguration(useKristaOAuthApp, inputConfig);
            
            // Step 2: Validate configuration
            configValidator.validate(finalConfig, useKristaOAuthApp);
            
            // Step 3: Test OAuth token acquisition
             AccessToken token = tokenService.acquireToken(finalConfig);
            
            // Step 4: Test Microsoft Graph API connectivity
            graphApiService.testConnectivity(token, finalConfig.getGraphApiBaseUrl());
            
            // Step 5: Test mailbox access
            graphApiService.testMailboxAccess(token, finalConfig.getGraphApiBaseUrl());
            
            // Step 6: Save configuration securely
            saveConfigurationSecurely(finalConfig);
            
            long timeTakenInSeconds = (System.currentTimeMillis() - startTime) / 1000;
            LOGGER.info("Configuration saved successfully for tenant: " + finalConfig.getTenantId() + 
                        " in " + timeTakenInSeconds + " seconds");
            
            return ConfigurationSaveResult.success(
                "Outlook configuration saved successfully using " + 
                (useKristaOAuthApp ? "Krista OAuth App" : "custom OAuth app") + 
                ". Microsoft Graph API connectivity validated for tenant '" + finalConfig.getTenantId() + "'.",
                timeTakenInSeconds);
                
        } catch (ConfigurationValidationException e) {
            LOGGER.log(Level.SEVERE, "Configuration validation failed: " + e.getMessage(), e);
            long timeTakenInSeconds = (System.currentTimeMillis() - startTime) / 1000;
            return ConfigurationSaveResult.failure(
                "Configuration validation failed", 
                "INPUT_ERROR",
                e.getMessage(),
                timeTakenInSeconds);
                
        } catch (AuthenticationException e) {
            LOGGER.log(Level.SEVERE, "Authentication failed: " + e.getMessage(), e);
            long timeTakenInSeconds = (System.currentTimeMillis() - startTime) / 1000;
            return ConfigurationSaveResult.failure(
                "Configuration save failed due to authentication error", 
                "AUTHENTICATION_ERROR",
                "OAuth token acquisition failed: " + e.getMessage() + 
                ". Verify client secret is correct and not expired in Azure AD app registration.",
                timeTakenInSeconds);
                
        } catch (GraphApiException e) {
            LOGGER.log(Level.SEVERE, "Graph API connectivity test failed: " + e.getMessage(), e);
            long timeTakenInSeconds = (System.currentTimeMillis() - startTime) / 1000;
            return ConfigurationSaveResult.failure(
                "Configuration save failed due to Microsoft Graph API connectivity issue", 
                "API_ERROR",
                "Microsoft Graph API connectivity test failed: " + e.getMessage() + 
                ". Verify API permissions and network connectivity.",
                timeTakenInSeconds);
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during configuration save: " + e.getMessage(), e);
            long timeTakenInSeconds = (System.currentTimeMillis() - startTime) / 1000;
            return ConfigurationSaveResult.failure(
                "Configuration save failed due to an unexpected error", 
                "SYSTEM_ERROR",
                "Unexpected error: " + e.getMessage(),
                timeTakenInSeconds);
        }
    }
    
    private OutlookConfiguration prepareConfiguration(boolean useKristaOAuthApp, OutlookConfiguration inputConfig) {
        if (useKristaOAuthApp) {
            // Use Krista's pre-configured OAuth application
            OutlookConfiguration kristaConfig = new OutlookConfiguration();
            kristaConfig.setTenantId(inputConfig.getTenantId()); // Only tenant ID required from user
            kristaConfig.setClientId(KRISTA_OAUTH_CLIENT_ID);
            kristaConfig.setClientSecret(KRISTA_OAUTH_CLIENT_SECRET);
            kristaConfig.setAuthorityUrl("https://login.microsoftonline.com/" + inputConfig.getTenantId());
            
            // Add User.Read.All permission for accessing users
            kristaConfig.setScopes(Arrays.asList(
                "Mail.Read", 
                "Mail.ReadWrite", 
                "Mail.Send", 
                "Calendars.ReadWrite", 
                "Contacts.ReadWrite",
                "User.Read.All"
            ));
            
            kristaConfig.setGraphApiBaseUrl(DEFAULT_GRAPH_API_BASE_URL);
            kristaConfig.setMultiTenant(true);
            kristaConfig.setRedirectUri(KRISTA_OAUTH_REDIRECT_URI);

            LOGGER.info("Using Krista OAuth App configuration for tenant: " + inputConfig.getTenantId());
            return kristaConfig;
        } else {
            // Use customer's custom OAuth application
            // Set defaults for optional fields if not provided
            if (inputConfig.getAuthorityUrl() == null || inputConfig.getAuthorityUrl().isEmpty()) {
                inputConfig.setAuthorityUrl("https://login.microsoftonline.com/" + inputConfig.getTenantId());
            }
            
            if (inputConfig.getGraphApiBaseUrl() == null || inputConfig.getGraphApiBaseUrl().isEmpty()) {
                inputConfig.setGraphApiBaseUrl(DEFAULT_GRAPH_API_BASE_URL);
            }
            
            LOGGER.info("Using custom OAuth App configuration for tenant: " + inputConfig.getTenantId());
            return inputConfig;
        }
    }
    
    private void saveConfigurationSecurely(OutlookConfiguration config) throws ConfigurationStorageException {
        try {
            // Encrypt sensitive fields
            String encryptedClientSecret = encryptionService.encrypt(config.getClientSecret());
            
            // Save to database
            configRepository.saveConfiguration(
                config.getTenantId(),
                config.getClientId(),
                encryptedClientSecret,
                config.getAuthorityUrl(),
                String.join(",", config.getScopes()),
                config.getGraphApiBaseUrl(),
                config.isMultiTenant(),
                config.getRedirectUri(),
                Instant.now()
            );
            
            LOGGER.info("Outlook configuration saved securely for tenant: " + config.getTenantId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save configuration securely: " + e.getMessage(), e);
            throw new ConfigurationStorageException("Failed to save configuration: " + e.getMessage(), e);
        }
    }
}