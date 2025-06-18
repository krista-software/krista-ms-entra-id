package com.kristasoft.outlook.factory;

import com.kristasoft.outlook.repository.OutlookConfigurationRepository;
import com.kristasoft.outlook.security.EncryptionService;
import com.kristasoft.outlook.service.EmailFolderService;
import com.kristasoft.outlook.service.EmailInboxService;
import com.kristasoft.outlook.service.GraphApiService;
import com.kristasoft.outlook.service.TokenCacheService;
import com.kristasoft.outlook.service.TokenService;
import com.kristasoft.outlook.service.impl.MicrosoftGraphApiServiceImpl;
import com.kristasoft.outlook.service.impl.MicrosoftTokenServiceImpl;
import com.kristasoft.outlook.service.impl.OutlookEmailFolderServiceImpl;
import com.kristasoft.outlook.service.impl.OutlookEmailInboxServiceImpl;
import com.kristasoft.outlook.service.impl.TokenCacheServiceImpl;
import com.kristasoft.outlook.security.AesEncryptionService;

/**
 * Factory for creating email-related services.
 * Follows the Factory Method pattern to create service instances.
 */
public class EmailServiceFactory {
    
    /**
     * Creates an EmailFolderService instance with all required dependencies.
     * 
     * @param configRepository Repository for retrieving saved configurations
     * @param encryptionKey Encryption key for decrypting client secrets
     * @return An EmailFolderService implementation
     */
    public static EmailFolderService createEmailFolderService(
            OutlookConfigurationRepository configRepository,
            String encryptionKey) {
        
        // Create the token service
        TokenService tokenService = new MicrosoftTokenServiceImpl();
        
        // Create the token cache service
        TokenCacheService tokenCacheService = new TokenCacheServiceImpl(tokenService);
        
        // Create the encryption service
        EncryptionService encryptionService = new AesEncryptionService(encryptionKey);
        
        // Create and return the email folder service
        return new OutlookEmailFolderServiceImpl(
                configRepository,
                tokenCacheService,
                encryptionService);
    }
    
    /**
     * Creates an EmailInboxService instance with all required dependencies.
     * 
     * @param configRepository Repository for retrieving saved configurations
     * @param encryptionKey Encryption key for decrypting client secrets
     * @return An EmailInboxService implementation
     */
    public static EmailInboxService createEmailInboxService(
            OutlookConfigurationRepository configRepository,
            String encryptionKey) {
        
        // Create the token service
        TokenService tokenService = new MicrosoftTokenServiceImpl();
        
        // Create the token cache service
        TokenCacheService tokenCacheService = new TokenCacheServiceImpl(tokenService);
        
        // Create the encryption service
        EncryptionService encryptionService = new AesEncryptionService(encryptionKey);
        
        // Create the Graph API service
        GraphApiService graphApiService = new MicrosoftGraphApiServiceImpl();
        
        // Create and return the email inbox service
        return new OutlookEmailInboxServiceImpl(
                configRepository,
                tokenCacheService,
                encryptionService,
                graphApiService);
    }
    
    /**
     * Creates a TokenCacheService instance.
     * 
     * @return A TokenCacheService implementation
     */
    public static TokenCacheService createTokenCacheService() {
        TokenService tokenService = new MicrosoftTokenServiceImpl();
        return new TokenCacheServiceImpl(tokenService);
    }
    
    /**
     * Creates a GraphApiService instance.
     * 
     * @return A GraphApiService implementation
     */
    public static GraphApiService createGraphApiService() {
        return new MicrosoftGraphApiServiceImpl();
    }
    
    /**
     * Creates an EncryptionService instance.
     * 
     * @param encryptionKey The encryption key to use
     * @return An EncryptionService implementation
     */
    public static EncryptionService createEncryptionService(String encryptionKey) {
        return new AesEncryptionService(encryptionKey);
    }
}