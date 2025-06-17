package com.kristasoft.outlook.service.impl;

import com.kristasoft.outlook.model.AccessToken;
import com.kristasoft.outlook.model.OutlookConfiguration;
import com.kristasoft.outlook.service.TokenService;
import com.kristasoft.outlook.exception.AuthenticationException;
import com.microsoft.aad.msal4j.*;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class MicrosoftTokenServiceImpl implements TokenService {
    private static final Logger LOGGER = Logger.getLogger(MicrosoftTokenServiceImpl.class.getName());
    private static final int TOKEN_TIMEOUT_SECONDS = 30;

    @Override
    public AccessToken acquireToken(OutlookConfiguration config) throws AuthenticationException {
        try {
            ConfidentialClientApplication app = buildConfidentialClientApplication(config);
            ClientCredentialParameters parameters = buildClientCredentialParameters();
            
            LOGGER.info("Attempting to acquire token for tenant: " + config.getTenantId());
            CompletableFuture<IAuthenticationResult> future = app.acquireToken(parameters);
            IAuthenticationResult result = future.get(TOKEN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            LOGGER.info("Token acquired successfully for tenant: " + config.getTenantId());
            return new AccessToken(result.accessToken(), result.expiresOnDate().toInstant());
        } catch (MalformedURLException e) {
            LOGGER.severe("Invalid authority URL: " + e.getMessage());
            throw new AuthenticationException("Invalid authority URL: " + e.getMessage(), e);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.severe("Token acquisition failed: " + e.getMessage());
            throw new AuthenticationException("Failed to acquire token: " + e.getMessage(), e);
        } catch (TimeoutException e) {
            LOGGER.severe("Token acquisition timed out after " + TOKEN_TIMEOUT_SECONDS + " seconds");
            throw new AuthenticationException("Token acquisition timed out", e);
        }
    }
    
    private ConfidentialClientApplication buildConfidentialClientApplication(OutlookConfiguration config) 
            throws MalformedURLException {
        return ConfidentialClientApplication.builder(
                config.getClientId(),
                ClientCredentialFactory.createFromSecret(config.getClientSecret()))
                .authority(config.getAuthorityUrl())
                .build();
    }
    
    private ClientCredentialParameters buildClientCredentialParameters() {
        Set<String> scopes = new HashSet<>(Collections.singletonList("https://graph.microsoft.com/.default"));
        return ClientCredentialParameters.builder(scopes).build();
    }
}