package com.kristasoft.outlook.factory;

import com.kristasoft.outlook.repository.OutlookConfigurationRepository;
import com.kristasoft.outlook.security.EncryptionService;
import com.kristasoft.outlook.service.EmailFolderService;
import com.kristasoft.outlook.service.TokenCacheService;
import com.kristasoft.outlook.service.TokenService;
import com.kristasoft.outlook.service.impl.OutlookEmailFolderServiceImpl;
import com.kristasoft.outlook.service.impl.MicrosoftTokenServiceImpl;
import com.kristasoft.outlook.service.impl.TokenCacheServiceImpl;

/**
 * Factory for creating email-related services.
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
        EncryptionService encryptionService = new com.kristasoft.outlook.security.AesEncryptionService(encryptionKey);
        
        // Create and return the email folder service
        return new OutlookEmailFolderServiceImpl(
                configRepository,
                tokenCacheService,
                encryptionService);
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
}