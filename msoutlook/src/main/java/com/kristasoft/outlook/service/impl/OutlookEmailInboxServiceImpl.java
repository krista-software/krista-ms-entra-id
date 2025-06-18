package com.kristasoft.outlook.service.impl;

import com.kristasoft.outlook.exception.AuthenticationException;
import com.kristasoft.outlook.exception.EmailInboxException;
import com.kristasoft.outlook.exception.GraphApiException;
import com.kristasoft.outlook.model.AccessToken;
import com.kristasoft.outlook.model.EmailMessage;
import com.kristasoft.outlook.model.InboxResult;
import com.kristasoft.outlook.model.OutlookConfiguration;
import com.kristasoft.outlook.repository.OutlookConfigurationEntity;
import com.kristasoft.outlook.repository.OutlookConfigurationRepository;
import com.kristasoft.outlook.security.EncryptionService;
import com.kristasoft.outlook.service.EmailInboxService;
import com.kristasoft.outlook.service.GraphApiService;
import com.kristasoft.outlook.service.TokenCacheService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of EmailInboxService for Microsoft Outlook.
 * Follows the Dependency Inversion Principle by depending on abstractions.
 */
public class OutlookEmailInboxServiceImpl implements EmailInboxService {
    
    private static final Logger LOGGER = Logger.getLogger(OutlookEmailInboxServiceImpl.class.getName());
    
    // Default values for parameters
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 999;
    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final boolean DEFAULT_INCLUDE_READ_STATUS = true;
    
    // Dependencies (injected)
    private final OutlookConfigurationRepository configRepository;
    private final TokenCacheService tokenCacheService;
    private final EncryptionService encryptionService;
    private final GraphApiService graphApiService;
    
    /**
     * Constructor with dependency injection.
     * 
     * @param configRepository Repository for retrieving saved configurations
     * @param tokenCacheService Service for acquiring and caching access tokens
     * @param encryptionService Service for decrypting client secrets
     * @param graphApiService Service for interacting with Microsoft Graph API
     */
    public OutlookEmailInboxServiceImpl(
            OutlookConfigurationRepository configRepository,
            TokenCacheService tokenCacheService,
            EncryptionService encryptionService,
            GraphApiService graphApiService) {
        this.configRepository = configRepository;
        this.tokenCacheService = tokenCacheService;
        this.encryptionService = encryptionService;
        this.graphApiService = graphApiService;
    }
    
    @Override
    public InboxResult listInboxEmails(Integer pageSize, Integer pageNumber, Boolean includeReadStatus) 
            throws EmailInboxException {
        try {
            // Apply default values if parameters are null
            int effectivePageSize = (pageSize != null) ? pageSize : DEFAULT_PAGE_SIZE;
            int effectivePageNumber = (pageNumber != null) ? pageNumber : DEFAULT_PAGE_NUMBER;
            boolean effectiveIncludeReadStatus = (includeReadStatus != null) ? includeReadStatus : DEFAULT_INCLUDE_READ_STATUS;
            
            // Validate parameters
            validateParameters(effectivePageSize, effectivePageNumber);
            
            // Get the active configuration
            OutlookConfiguration config = getActiveConfiguration();
            if (config == null) {
                String message = "No active Outlook configuration found";
                LOGGER.info(message);
                throw new EmailInboxException(message);
            }
            
            // Get a valid token (from cache or newly acquired)
            AccessToken token = tokenCacheService.getToken(config);
            
            // Fetch emails using Microsoft Graph API
            return fetchInboxEmailsFromGraphApi(token, config.getGraphApiBaseUrl(), 
                    effectivePageSize, effectivePageNumber, effectiveIncludeReadStatus);
            
        } catch (EmailInboxException e) {
            // Don't log EmailInboxException as SEVERE since it might be an expected business case
            throw e;
        } catch (AuthenticationException e) {
            LOGGER.log(Level.SEVERE, "Authentication error when listing inbox emails", e);
            throw new EmailInboxException("Failed to authenticate with Microsoft Graph API", e);
        } catch (GraphApiException e) {
            LOGGER.log(Level.SEVERE, "Graph API error when listing inbox emails", e);
            throw new EmailInboxException("Failed to retrieve emails from Microsoft Graph API", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error when listing inbox emails", e);
            throw new EmailInboxException("Unexpected error occurred while retrieving inbox emails", e);
        }
    }
    
    /**
     * Validates the input parameters.
     * 
     * @param pageSize The page size to validate
     * @param pageNumber The page number to validate
     * @throws EmailInboxException if parameters are invalid
     */
    private void validateParameters(int pageSize, int pageNumber) throws EmailInboxException {
        if (pageSize <= 0) {
            throw new EmailInboxException("Page size must be greater than 0");
        }
        
        if (pageSize > MAX_PAGE_SIZE) {
            throw new EmailInboxException("Page size cannot exceed " + MAX_PAGE_SIZE);
        }
        
        if (pageNumber < 0) {
            throw new EmailInboxException("Page number cannot be negative");
        }
    }
    
    /**
     * Gets the active Outlook configuration.
     * 
     * @return The active configuration, or null if none exists
     * @throws Exception if there's an error decrypting the client secret
     */
    private OutlookConfiguration getActiveConfiguration() throws Exception {
        List<OutlookConfigurationEntity> activeConfigs = configRepository.findAllActive();
        
        if (activeConfigs.isEmpty()) {
            return null;
        }
        
        // Use the first active configuration
        OutlookConfigurationEntity entity = activeConfigs.get(0);
        
        // Convert entity to configuration model
        OutlookConfiguration config = new OutlookConfiguration();
        config.setTenantId(entity.getTenantId());
        config.setClientId(entity.getClientId());
        config.setClientSecret(encryptionService.decrypt(entity.getClientSecretEncrypted()));
        config.setAuthorityUrl(entity.getAuthorityUrl());
        config.setGraphApiBaseUrl(entity.getGraphApiBaseUrl());
        
        // Parse scopes from comma-separated string
        String[] scopesArray = entity.getScopes().split(",");
        List<String> scopes = new ArrayList<>();
        for (String scope : scopesArray) {
            scopes.add(scope.trim());
        }
        config.setScopes(scopes);
        
        return config;
    }
    
    /**
     * Fetches inbox emails from Microsoft Graph API.
     * 
     * @param token The access token for authentication
     * @param graphApiBaseUrl The base URL for the Graph API
     * @param pageSize The number of emails to retrieve
     * @param pageNumber The page number for pagination
     * @param includeReadStatus Whether to include read status
     * @return InboxResult containing emails and total count
     * @throws GraphApiException if there's an error calling the Graph API
     */
    private InboxResult fetchInboxEmailsFromGraphApi(
            AccessToken token, String graphApiBaseUrl, 
            int pageSize, int pageNumber, boolean includeReadStatus) throws GraphApiException {
        
        LOGGER.info("Fetching inbox emails from Graph API with pageSize=" + pageSize + 
                   ", pageNumber=" + pageNumber + ", includeReadStatus=" + includeReadStatus);
        
        try {
            // Calculate skip for pagination
            int skip = pageNumber * pageSize;
            
            // Build the Graph API request URL
            StringBuilder urlBuilder = new StringBuilder(graphApiBaseUrl);
            urlBuilder.append("/me/mailFolders/inbox/messages");
            urlBuilder.append("?$top=").append(pageSize);
            urlBuilder.append("&$skip=").append(skip);
            
            // Select fields based on whether read status is needed
            urlBuilder.append("&$select=id,subject,from,receivedDateTime,bodyPreview,importance,hasAttachments");
            if (includeReadStatus) {
                urlBuilder.append(",isRead");
            }
            
            // Order by received date (newest first)
            urlBuilder.append("&$orderby=receivedDateTime desc");
            
            // Make the API call
            String apiUrl = urlBuilder.toString();
            Map<String, Object> response = graphApiService.executeGraphApiRequest(token, apiUrl);
            
            // Parse the response
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("value");
            int totalCount = getTotalCountFromResponse(response, token, graphApiBaseUrl);
            
            // Convert to EmailMessage objects
            List<EmailMessage> emails = new ArrayList<>();
            for (Map<String, Object> item : items) {
                emails.add(convertToEmailMessage(item, includeReadStatus));
            }
            
            LOGGER.info("Retrieved " + emails.size() + " emails from inbox (total: " + totalCount + ")");
            return new InboxResult(emails, totalCount);
            
        } catch (Exception e) {
            throw new GraphApiException("Error fetching inbox emails from Graph API", e);
        }
    }
    
    /**
     * Gets the total count of emails in the inbox.
     * 
     * @param response The initial response from the Graph API
     * @param token The access token for authentication
     * @param graphApiBaseUrl The base URL for the Graph API
     * @return The total count of emails
     * @throws GraphApiException if there's an error calling the Graph API
     */
    private int getTotalCountFromResponse(Map<String, Object> response, AccessToken token, String graphApiBaseUrl) 
            throws GraphApiException {
        // Check if the response already includes the count
        if (response.containsKey("@odata.count")) {
            return ((Number) response.get("@odata.count")).intValue();
        }
        
        // If not, make a separate request to get the count
        try {
            String countUrl = graphApiBaseUrl + "/me/mailFolders/inbox/messages/$count";
            Map<String, Object> countResponse = graphApiService.executeGraphApiRequest(token, countUrl);
            
            if (countResponse.containsKey("value")) {
                return ((Number) countResponse.get("value")).intValue();
            }
            
            // If we can't get the count, return the size of the current page
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("value");
            return items.size();
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not retrieve total email count", e);
            // Return the size of the current page as a fallback
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("value");
            return items.size();
        }
    }
    
    /**
     * Converts a Graph API response item to an EmailMessage.
     * 
     * @param item The Graph API response item
     * @param includeReadStatus Whether read status was included in the request
     * @return An EmailMessage object
     */
    private EmailMessage convertToEmailMessage(Map<String, Object> item, boolean includeReadStatus) {
        EmailMessage.Builder builder = new EmailMessage.Builder()
            .messageId((String) item.get("id"))
            .subject((String) item.get("subject"))
            .previewText((String) item.get("bodyPreview"))
            .importance(normalizeImportance((String) item.get("importance")))
            .hasAttachments((Boolean) item.get("hasAttachments"));
        
        // Parse received date
        String receivedDateTimeStr = (String) item.get("receivedDateTime");
        if (receivedDateTimeStr != null) {
            builder.receivedDateTime(Instant.parse(receivedDateTimeStr));
        }
        
        // Parse sender information
        if (item.containsKey("from")) {
            Map<String, Object> from = (Map<String, Object>) item.get("from");
            if (from.containsKey("emailAddress")) {
                Map<String, Object> emailAddress = (Map<String, Object>) from.get("emailAddress");
                builder.senderName((String) emailAddress.get("name"));
                builder.senderEmail((String) emailAddress.get("address"));
            }
        }
        
        // Add read status if included
        if (includeReadStatus && item.containsKey("isRead")) {
            builder.isRead((Boolean) item.get("isRead"));
        }
        
        return builder.build();
    }
    
    /**
     * Normalizes the importance value to a standard format.
     * 
     * @param importance The importance value from the API
     * @return A normalized importance value (Low, Normal, High)
     */
    private String normalizeImportance(String importance) {
        if (importance == null) {
            return "Normal";
        }
        
        switch (importance.toLowerCase()) {
            case "low":
                return "Low";
            case "high":
                return "High";
            default:
                return "Normal";
        }
    }
}
