package com.kristasoft.outlook.service;

import com.kristasoft.outlook.model.OutlookConfiguration;
import com.kristasoft.outlook.exception.ConfigurationValidationException;

public interface ConfigurationValidator {
    void validate(OutlookConfiguration configuration, boolean useKristaOAuthApp) throws ConfigurationValidationException;
}