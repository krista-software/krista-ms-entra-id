package com.kristasoft.outlook.service;

import com.kristasoft.outlook.model.AccessToken;
import com.kristasoft.outlook.exception.GraphApiException;

import java.util.Map;

/**
 * Service interface for interacting with Microsoft Graph API.
 */
public interface GraphApiService {
    /**
     * Tests connectivity to the Microsoft Graph API.
     * 
     * @param token The access token for authentication
     * @param graphApiBaseUrl The base URL for the Graph API
     * @throws GraphApiException if there's an error connecting to the API
     */
    void testConnectivity(AccessToken token, String graphApiBaseUrl) throws GraphApiException;
    
    /**
     * Tests access to the mailbox via Microsoft Graph API.
     * 
     * @param token The access token for authentication
     * @param graphApiBaseUrl The base URL for the Graph API
     * @throws GraphApiException if there's an error accessing the mailbox
     */
    void testMailboxAccess(AccessToken token, String graphApiBaseUrl) throws GraphApiException;
    
    /**
     * Executes a request to the Microsoft Graph API.
     * 
     * @param token The access token for authentication
     * @param apiUrl The full URL for the API request
     * @return A map containing the JSON response from the API
     * @throws GraphApiException if there's an error executing the request
     */
    Map<String, Object> executeGraphApiRequest(AccessToken token, String apiUrl) throws GraphApiException;
}
