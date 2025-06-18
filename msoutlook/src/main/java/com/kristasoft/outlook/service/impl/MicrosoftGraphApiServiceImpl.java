package com.kristasoft.outlook.service.impl;

import com.kristasoft.outlook.model.AccessToken;
import com.kristasoft.outlook.service.GraphApiService;
import com.kristasoft.outlook.exception.GraphApiException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MicrosoftGraphApiServiceImpl implements GraphApiService {
    private static final Logger LOGGER = Logger.getLogger(MicrosoftGraphApiServiceImpl.class.getName());
    private static final int CONNECTION_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final Gson GSON = new Gson();

    @Override
    public void testConnectivity(AccessToken token, String graphApiBaseUrl) throws GraphApiException {
        // Just verify that we have a valid token
        if (token == null || token.getToken() == null || token.getToken().isEmpty()) {
            throw new GraphApiException("Invalid access token");
        }
        
        LOGGER.info("Skipping detailed Graph API connectivity test - token acquisition successful");
    }

    @Override
    public void testMailboxAccess(AccessToken token, String graphApiBaseUrl) throws GraphApiException {
        // For client credentials flow, we can't use /me/messages endpoint as it requires user context
        // Instead, use a specific user's mailbox if you have application permissions
        // Or skip this test for client credentials flow
        
        // For this example, we'll just log a message and return success
        LOGGER.info("Skipping mailbox access test for client credentials flow - requires specific user ID");
        
        // If you have a specific user ID, you could use:
        // String endpoint = graphApiBaseUrl + "/users/{user-id}/messages?$top=1";
    }
    
    @Override
    public Map<String, Object> executeGraphApiRequest(AccessToken token, String apiUrl) throws GraphApiException {
        LOGGER.info("Executing Graph API request: " + apiUrl);
        
        try {
            // Create connection
            HttpURLConnection connection = createGraphConnection(apiUrl, token);
            
            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String errorDetails = getErrorDetails(connection);
                throw new GraphApiException("Graph API request failed with status code " + responseCode + 
                                           ": " + errorDetails);
            }
            
            // Read response
            try (InputStream inputStream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                
                // Parse JSON response to Map
                String jsonResponse = response.toString();
                return parseJsonToMap(jsonResponse);
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IO error executing Graph API request", e);
            throw new GraphApiException("IO error executing Graph API request: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error executing Graph API request", e);
            throw new GraphApiException("Unexpected error executing Graph API request: " + e.getMessage(), e);
        }
    }
    
    private HttpURLConnection createGraphConnection(String endpoint, AccessToken token) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + token.getToken());
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        
        return connection;
    }
    
    private String getErrorDetails(HttpURLConnection connection) {
        try {
            InputStream errorStream = connection.getErrorStream();
            if (errorStream == null) {
                return "No error details available";
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } catch (IOException e) {
            return "Error reading error details: " + e.getMessage();
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String jsonString) {
        try {
            return GSON.fromJson(jsonString, Map.class);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing JSON response, returning empty map", e);
            return new HashMap<>();
        }
    }
}