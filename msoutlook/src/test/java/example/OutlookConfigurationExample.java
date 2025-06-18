package example;

import com.kristasoft.outlook.factory.MicrosoftServiceFactory;
import com.kristasoft.outlook.model.ConfigurationSaveResult;
import com.kristasoft.outlook.model.ExtensionResponse;
import com.kristasoft.outlook.model.OutlookConfiguration;
import com.kristasoft.outlook.service.ConfigurationService;
import com.kristasoft.outlook.repository.OutlookConfigurationRepository;
import com.kristasoft.outlook.security.AesEncryptionService;
import com.kristasoft.outlook.repository.OutlookConfigurationEntity;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example demonstrating how to use the Outlook configuration services
 */
public class OutlookConfigurationExample {
    private static final Logger LOGGER = Logger.getLogger(OutlookConfigurationExample.class.getName());
    
    // Test credentials
    private static final String TENANT_ID = "3694f6b4-b5f1-47ef-852f-a0b4a459ab44";
    private static final String CLIENT_ID = "ec0745c8-7635-4b31-97cc-d217944dd620";
    private static final String CLIENT_SECRET = "REDACTED_SECRET_1";
    private static final String EMAIL = "service.automation@kristasoft.com";
    
    public static void main(String[] args) {
        try {
            // Create a repository implementation (this would typically be injected)
            OutlookConfigurationRepository repository = createRepository();
            
            // Generate or load an encryption key
            String encryptionKey = AesEncryptionService.generateEncryptionKey();
            
            // Create the configuration service using the factory
            ConfigurationService configService = MicrosoftServiceFactory.createConfigurationService(
                    repository, encryptionKey);
            
            // Example 1: Save configuration using Krista OAuth App
            saveUsingKristaOAuthApp(configService);
            
            // Example 2: Save configuration using custom OAuth App
            saveUsingCustomOAuthApp(configService);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in example: " + e.getMessage(), e);
        }
    }
    
    private static void saveUsingKristaOAuthApp(ConfigurationService configService) {
        try {
            // Create minimal configuration (only tenant ID required for Krista OAuth App)
            OutlookConfiguration config = new OutlookConfiguration();
            config.setTenantId(TENANT_ID); // Use test tenant ID
            
            // Save configuration
            ConfigurationSaveResult result = configService.saveConfiguration(true, config);
            
            // Process result
            processResult(result, "Krista OAuth App");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving Krista OAuth App configuration: " + e.getMessage(), e);
        }
    }
    
    private static void saveUsingCustomOAuthApp(ConfigurationService configService) {
        try {
            // Create complete configuration for custom OAuth app
            OutlookConfiguration config = new OutlookConfiguration();
            config.setTenantId(TENANT_ID); // Use test tenant ID
            config.setClientId(CLIENT_ID); // Use test client ID
            config.setClientSecret(CLIENT_SECRET); // Use test client secret
            config.setAuthorityUrl("https://login.microsoftonline.com/" + TENANT_ID);
            
            // Add User.Read.All permission for accessing users
            config.setScopes(Arrays.asList(
                "Mail.Read", 
                "Mail.ReadWrite", 
                "Mail.Send", 
                "Calendars.ReadWrite", 
                "Contacts.ReadWrite",
                "User.Read.All"
            ));
            
            config.setGraphApiBaseUrl("https://graph.microsoft.com/v1.0");
            config.setMultiTenant(false);
            config.setRedirectUri("https://localhost:8080/oauth/callback");
            
            // Save configuration
            ConfigurationSaveResult result = configService.saveConfiguration(false, config);
            
            // Process result
            processResult(result, "Custom OAuth App");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving custom OAuth App configuration: " + e.getMessage(), e);
        }
    }
    
    private static void processResult(ConfigurationSaveResult result, String configType) {
        ExtensionResponse response = result.getExtensionResponse();
        
        if (result.isConfigurationSaved()) {
            LOGGER.info(configType + " configuration saved successfully!");
            LOGGER.info("Message: " + response.getMessage());
            LOGGER.info("Time taken: " + response.getTimeTakenInSeconds() + " seconds");
        } else {
            LOGGER.severe(configType + " configuration save failed!");
            LOGGER.severe("Message: " + response.getMessage());
            LOGGER.severe("Error type: " + response.getResponseType());
            LOGGER.severe("Technical details: " + response.getTechnicalDetailedErrorReport());
            LOGGER.severe("Time taken: " + response.getTimeTakenInSeconds() + " seconds");
        }
    }
    
    // This would be replaced with an actual repository implementation
    private static OutlookConfigurationRepository createRepository() {
        // Simple in-memory implementation for example purposes
        return new OutlookConfigurationRepository() {
            @Override
            public void saveConfiguration(String tenantId, String clientId, String encryptedClientSecret,
                                         String authorityUrl, String scopes, String graphApiBaseUrl,
                                         boolean multiTenant, String redirectUri, java.time.Instant updatedAt) {
                LOGGER.info("Configuration saved to repository for tenant: " + tenantId);
                LOGGER.info("Client ID: " + clientId);
                LOGGER.info("Encrypted client secret: " + encryptedClientSecret);
                LOGGER.info("Authority URL: " + authorityUrl);
                LOGGER.info("Scopes: " + scopes);
                LOGGER.info("Graph API base URL: " + graphApiBaseUrl);
                LOGGER.info("Multi-tenant: " + multiTenant);
                LOGGER.info("Redirect URI: " + redirectUri);
                LOGGER.info("Updated at: " + updatedAt);
            }
            
            @Override
            public java.util.List<OutlookConfigurationEntity> findAllActive() {
                return java.util.Collections.emptyList();
            }
        };
    }
}