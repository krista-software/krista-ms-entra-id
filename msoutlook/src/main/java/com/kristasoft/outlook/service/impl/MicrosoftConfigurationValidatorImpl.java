package com.kristasoft.outlook.service.impl;

import com.kristasoft.outlook.model.OutlookConfiguration;
import com.kristasoft.outlook.service.ConfigurationValidator;
import com.kristasoft.outlook.exception.ConfigurationValidationException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class MicrosoftConfigurationValidatorImpl implements ConfigurationValidator {
    private static final Logger LOGGER = Logger.getLogger(MicrosoftConfigurationValidatorImpl.class.getName());
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final List<String> REQUIRED_SCOPES = Arrays.asList(
            "Mail.Read", "Mail.ReadWrite", "Mail.Send", "Calendars.ReadWrite", "Contacts.ReadWrite");

    @Override
    public void validate(OutlookConfiguration config, boolean useKristaOAuthApp) throws ConfigurationValidationException {
        LOGGER.info("Validating Outlook configuration for tenant: " + config.getTenantId() + 
                    " using " + (useKristaOAuthApp ? "Krista OAuth App" : "custom OAuth app"));
        
        validateTenantId(config.getTenantId());
        
        if (!useKristaOAuthApp) {
            validateClientId(config.getClientId());
            validateClientSecret(config.getClientSecret());
            validateScopes(config.getScopes());
            validateRedirectUri(config.getRedirectUri());
        }
        
        validateAuthorityUrl(config.getAuthorityUrl());
        validateGraphApiBaseUrl(config.getGraphApiBaseUrl());
        
        LOGGER.info("Configuration validation completed successfully");
    }
    
    private void validateTenantId(String tenantId) throws ConfigurationValidationException {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new ConfigurationValidationException("Tenant ID is required");
        }
        
        if (!UUID_PATTERN.matcher(tenantId).matches()) {
            throw new ConfigurationValidationException("Invalid Tenant ID format. Must be a valid GUID.");
        }
    }
    
    private void validateClientId(String clientId) throws ConfigurationValidationException {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new ConfigurationValidationException("Client ID is required for custom OAuth app");
        }
        
        if (!UUID_PATTERN.matcher(clientId).matches()) {
            throw new ConfigurationValidationException("Invalid Client ID format. Must be a valid GUID.");
        }
    }
    
    private void validateClientSecret(String clientSecret) throws ConfigurationValidationException {
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new ConfigurationValidationException("Client Secret is required for custom OAuth app");
        }
    }
    
    private void validateAuthorityUrl(String authorityUrl) throws ConfigurationValidationException {
        if (authorityUrl == null || authorityUrl.trim().isEmpty()) {
            return; // Optional field, will be auto-filled
        }
        
        try {
            new URL(authorityUrl);
        } catch (MalformedURLException e) {
            throw new ConfigurationValidationException("Invalid Authority URL format: " + e.getMessage());
        }
        
        if (!authorityUrl.contains("login.microsoftonline.com")) {
            throw new ConfigurationValidationException(
                "Authority URL must be a Microsoft identity platform URL (login.microsoftonline.com)");
        }
    }
    
    private void validateScopes(List<String> scopes) throws ConfigurationValidationException {
        if (scopes == null || scopes.isEmpty()) {
            throw new ConfigurationValidationException("At least one scope is required for custom OAuth app");
        }
        
        // Check if all required scopes are present
        for (String requiredScope : REQUIRED_SCOPES) {
            if (!scopes.contains(requiredScope)) {
                LOGGER.warning("Recommended scope missing: " + requiredScope);
            }
        }
    }
    
    private void validateGraphApiBaseUrl(String graphApiBaseUrl) throws ConfigurationValidationException {
        if (graphApiBaseUrl == null || graphApiBaseUrl.trim().isEmpty()) {
            return; // Optional field, will be auto-filled
        }
        
        try {
            new URL(graphApiBaseUrl);
        } catch (MalformedURLException e) {
            throw new ConfigurationValidationException("Invalid Graph API Base URL format: " + e.getMessage());
        }
        
        if (!graphApiBaseUrl.contains("graph.microsoft.com")) {
            throw new ConfigurationValidationException(
                "Graph API Base URL must be a Microsoft Graph API URL (graph.microsoft.com)");
        }
    }
    
    private void validateRedirectUri(String redirectUri) throws ConfigurationValidationException {
        if (redirectUri == null || redirectUri.trim().isEmpty()) {
            throw new ConfigurationValidationException("Redirect URI is required for custom OAuth app");
        }
        
        try {
            new URL(redirectUri);
        } catch (MalformedURLException e) {
            throw new ConfigurationValidationException("Invalid Redirect URI format: " + e.getMessage());
        }
    }
}