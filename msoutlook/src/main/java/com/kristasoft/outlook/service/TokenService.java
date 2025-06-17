package com.kristasoft.outlook.service;

import com.kristasoft.outlook.model.AccessToken;
import com.kristasoft.outlook.model.OutlookConfiguration;
import com.kristasoft.outlook.exception.AuthenticationException;

public interface TokenService {
    AccessToken acquireToken(OutlookConfiguration configuration) throws AuthenticationException;
}