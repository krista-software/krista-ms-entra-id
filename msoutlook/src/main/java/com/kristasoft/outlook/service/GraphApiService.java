package com.kristasoft.outlook.service;

import com.kristasoft.outlook.model.AccessToken;
import com.kristasoft.outlook.exception.GraphApiException;

public interface GraphApiService {
    void testConnectivity(AccessToken token, String graphApiBaseUrl) throws GraphApiException;
    void testMailboxAccess(AccessToken token, String graphApiBaseUrl) throws GraphApiException;
}