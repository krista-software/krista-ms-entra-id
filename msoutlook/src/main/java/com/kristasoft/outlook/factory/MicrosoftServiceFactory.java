package com.kristasoft.outlook.factory;

import com.kristasoft.outlook.security.AesEncryptionService;
import com.kristasoft.outlook.service.ConfigurationService;
import com.kristasoft.outlook.service.ConfigurationValidator;
import com.kristasoft.outlook.service.GraphApiService;
import com.kristasoft.outlook.service.TokenService;
import com.kristasoft.outlook.service.impl.MicrosoftConfigurationValidatorImpl;
import com.kristasoft.outlook.service.impl.MicrosoftGraphApiServiceImpl;
import com.kristasoft.outlook.service.impl.MicrosoftTokenServiceImpl;
import com.kristasoft.outlook.service.impl.OutlookConfigurationServiceImpl;
import com.kristasoft.outlook.repository.OutlookConfigurationRepository;
import com.kristasoft.outlook.security.EncryptionService;

/**
 * Factory for creating Microsoft-specific service implementations
 */
public class MicrosoftServiceFactory {
    
    /**
     * Creates a complete Microsoft-specific configuration service with all dependencies
     * 
     * @param configRepository The repository for storing configurations
     * @param encryptionKey Base64 encoded encryption key for securing client secrets
     * @return A fully configured ConfigurationService implementation
     */
    public static ConfigurationService createConfigurationService(
            OutlookConfigurationRepository configRepository, 
            String encryptionKey) {
        
        // Create the encryption service
        EncryptionService encryptionService = new AesEncryptionService(encryptionKey);
        
        // Create the validator
        ConfigurationValidator validator = new MicrosoftConfigurationValidatorImpl();
        
        // Create the token service
        TokenService tokenService = new MicrosoftTokenServiceImpl();
        
        // Create the Graph API service
        GraphApiService graphApiService = new MicrosoftGraphApiServiceImpl();
        
        // Create and return the configuration service with all dependencies
        return new OutlookConfigurationServiceImpl(
                validator,
                tokenService,
                graphApiService,
                configRepository,
                encryptionService);
    }
    
    /**
     * Creates a Microsoft-specific token service
     * 
     * @return A TokenService implementation for Microsoft OAuth
     */
    public static TokenService createTokenService() {
        return new MicrosoftTokenServiceImpl();
    }
    
    /**
     * Creates a Microsoft-specific Graph API service
     * 
     * @return A GraphApiService implementation for Microsoft Graph API
     */
    public static GraphApiService createGraphApiService() {
        return new MicrosoftGraphApiServiceImpl();
    }
    
    /**
     * Creates a Microsoft-specific configuration validator
     * 
     * @return A ConfigurationValidator implementation for Microsoft OAuth configurations
     */
    public static ConfigurationValidator createConfigurationValidator() {
        return new MicrosoftConfigurationValidatorImpl();
    }
    
    /**
     * Creates an encryption service for securing sensitive configuration data
     * 
     * @param encryptionKey Base64 encoded encryption key
     * @return An EncryptionService implementation
     */
    public static EncryptionService createEncryptionService(String encryptionKey) {
        return new AesEncryptionService(encryptionKey);
    }
}