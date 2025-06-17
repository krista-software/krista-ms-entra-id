package com.kristasoft.outlook.repository;

import java.time.Instant;
import java.util.List;

public interface OutlookConfigurationRepository {
    void saveConfiguration(
        String tenantId,
        String clientId,
        String encryptedClientSecret,
        String authorityUrl,
        String scopes,
        String graphApiBaseUrl,
        boolean multiTenant,
        String redirectUri,
        Instant updatedAt
    ) throws Exception;
    
    List<OutlookConfigurationEntity> findAllActive();
}