package com.kristasoft.outlook.service;

import com.kristasoft.outlook.model.OutlookConfiguration;
import com.kristasoft.outlook.model.ConfigurationSaveResult;
import com.kristasoft.outlook.exception.ConfigurationStorageException;

public interface ConfigurationService {
    ConfigurationSaveResult saveConfiguration(boolean useKristaOAuthApp, OutlookConfiguration configuration) 
        throws ConfigurationStorageException;
}