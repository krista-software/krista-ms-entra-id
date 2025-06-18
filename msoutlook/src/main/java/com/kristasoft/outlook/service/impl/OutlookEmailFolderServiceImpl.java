package com.kristasoft.outlook.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kristasoft.outlook.exception.AuthenticationException;
import com.kristasoft.outlook.exception.EmailFolderException;
import com.kristasoft.outlook.exception.GraphApiException;
import com.kristasoft.outlook.model.AccessToken;
import com.kristasoft.outlook.model.OutlookConfiguration;
import com.kristasoft.outlook.repository.OutlookConfigurationRepository;
import com.kristasoft.outlook.repository.OutlookConfigurationEntity;
import com.kristasoft.outlook.security.EncryptionService;
import com.kristasoft.outlook.service.EmailFolderService;
import com.kristasoft.outlook.service.TokenCacheService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Implementation of EmailFolderService for Microsoft Outlook.
 * Uses TokenCacheService for efficient token management.
 */
public class OutlookEmailFolderServiceImpl implements EmailFolderService {
    
    private static final Logger LOGGER = Logger.getLogger(OutlookEmailFolderServiceImpl.class.getName());
    
    private final OutlookConfigurationRepository configRepository;
    private final TokenCacheService tokenCacheService;
    private final EncryptionService encryptionService;
    
    /**
     * Constructor with dependency injection.
     * 
     * @param configRepository Repository for retrieving saved configurations
     * @param tokenCacheService Service for acquiring and caching access tokens
     * @param encryptionService Service for decrypting client secrets
     */
    public OutlookEmailFolderServiceImpl(
            OutlookConfigurationRepository configRepository,
            TokenCacheService tokenCacheService,
            EncryptionService encryptionService) {
        this.configRepository = configRepository;
        this.tokenCacheService = tokenCacheService;
        this.encryptionService = encryptionService;
    }
    
    @Override
    public List<String> listAllFolders() throws EmailFolderException {
        try {
            // Get the active configuration
            OutlookConfiguration config = getActiveConfiguration();
            if (config == null) {
                throw new EmailFolderException("No active Outlook configuration found");
            }
            
            // Get a valid token (from cache or newly acquired)
            AccessToken token = tokenCacheService.getToken(config);
            
            // Fetch folders using Microsoft Graph API
            return fetchFoldersFromGraphApi(token, config.getGraphApiBaseUrl());
            
        } catch (AuthenticationException e) {
            LOGGER.log(Level.SEVERE, "Authentication error when listing folders", e);
            throw new EmailFolderException("Failed to authenticate with Microsoft Graph API", e);
        } catch (GraphApiException e) {
            LOGGER.log(Level.SEVERE, "Graph API error when listing folders", e);
            throw new EmailFolderException("Failed to retrieve folders from Microsoft Graph API", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error when listing folders", e);
            throw new EmailFolderException("Unexpected error occurred while retrieving folders", e);
        }
    }
    
    /**
     * Fetches the most recent active configuration.
     * 
     * @return The active OutlookConfiguration or null if none exists
     */
    private OutlookConfiguration getActiveConfiguration() {
        List<OutlookConfigurationEntity> activeConfigs = configRepository.findAllActive();
        if (activeConfigs.isEmpty()) {
            LOGGER.warning("No active Outlook configurations found");
            return null;
        }
        
        // Get the most recently updated configuration
        OutlookConfigurationEntity latestConfig = activeConfigs.stream()
                .sorted((c1, c2) -> c2.getUpdatedAt().compareTo(c1.getUpdatedAt()))
                .findFirst()
                .orElse(null);
        
        if (latestConfig == null) {
            return null;
        }
        
        // Convert entity to configuration model
        OutlookConfiguration config = new OutlookConfiguration();
        config.setTenantId(latestConfig.getTenantId());
        config.setClientId(latestConfig.getClientId());
        
        // Decrypt the client secret
        try {
            String decryptedSecret = encryptionService.decrypt(latestConfig.getClientSecretEncrypted());
            config.setClientSecret(decryptedSecret);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to decrypt client secret", e);
            return null;
        }
        
        config.setAuthorityUrl(latestConfig.getAuthorityUrl());
        config.setGraphApiBaseUrl(latestConfig.getGraphApiBaseUrl());
        config.setMultiTenant(latestConfig.isMultiTenant());
        config.setRedirectUri(latestConfig.getRedirectUri());
        
        // Parse scopes from comma-separated string
        if (latestConfig.getScopes() != null && !latestConfig.getScopes().isEmpty()) {
            List<String> scopes = Arrays.asList(latestConfig.getScopes().split(","));
            config.setScopes(scopes);
        } else {
            config.setScopes(Collections.emptyList());
        }
        
        return config;
    }
    
    /**
     * Fetches folders from Microsoft Graph API.
     * 
     * @param token Access token for authentication
     * @param graphApiBaseUrl Base URL for Microsoft Graph API
     * @return List of folder names
     * @throws GraphApiException if there's an error communicating with the API
     */
    private List<String> fetchFoldersFromGraphApi(AccessToken token, String graphApiBaseUrl) 
            throws GraphApiException {
        List<String> folderNames = new ArrayList<>();
        
        try {
            // Use default Graph API URL if not specified
            String baseUrl = graphApiBaseUrl;
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = "https://graph.microsoft.com/v1.0";
            }
            
            // Construct the URL for the mailFolders endpoint
            String endpoint = baseUrl + "/me/mailFolders?$top=100";
            URL url = new URL(endpoint);
            
            // Open connection and set headers
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + token.getToken());
            connection.setRequestProperty("Accept", "application/json");
            
            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new GraphApiException("Failed to retrieve folders. Response code: " + responseCode);
            }
            
            // Read the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Parse the JSON response using GSON
            JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
            JsonArray folders = jsonResponse.getAsJsonArray("value");
            
            // Extract folder names
            for (JsonElement folderElement : folders) {
                JsonObject folder = folderElement.getAsJsonObject();
                String displayName = folder.get("displayName").getAsString();
                folderNames.add(displayName);
                
                // Log folder details for debugging
                LOGGER.fine("Found folder: " + displayName + ", ID: " + folder.get("id").getAsString());
            }
            
            // Check if there are more folders (paging)
            if (jsonResponse.has("@odata.nextLink")) {
                String nextLink = jsonResponse.get("@odata.nextLink").getAsString();
                folderNames.addAll(fetchNextPageFolders(token, nextLink));
            }
            
            LOGGER.info("Retrieved " + folderNames.size() + " folders from Outlook");
            return folderNames;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching folders from Graph API", e);
            throw new GraphApiException("Failed to retrieve folders from Microsoft Graph API", e);
        }
    }
    
    /**
     * Fetches the next page of folders when paging is involved.
     * 
     * @param token Access token for authentication
     * @param nextLink URL for the next page of results
     * @return List of folder names from the next page
     * @throws Exception if there's an error communicating with the API
     */
    private List<String> fetchNextPageFolders(AccessToken token, String nextLink) throws Exception {
        List<String> folderNames = new ArrayList<>();
        
        URL url = new URL(nextLink);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + token.getToken());
        connection.setRequestProperty("Accept", "application/json");
        
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new GraphApiException("Failed to retrieve next page of folders. Response code: " + responseCode);
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        // Parse the JSON response using GSON
        JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
        JsonArray folders = jsonResponse.getAsJsonArray("value");
        
        for (JsonElement folderElement : folders) {
            JsonObject folder = folderElement.getAsJsonObject();
            folderNames.add(folder.get("displayName").getAsString());
        }
        
        // Recursively fetch next page if it exists
        if (jsonResponse.has("@odata.nextLink")) {
            String newNextLink = jsonResponse.get("@odata.nextLink").getAsString();
            folderNames.addAll(fetchNextPageFolders(token, newNextLink));
        }
        
        return folderNames;
    }
}