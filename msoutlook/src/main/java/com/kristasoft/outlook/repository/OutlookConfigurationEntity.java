package com.kristasoft.outlook.repository;

import java.time.Instant;

public class OutlookConfigurationEntity {
    private String id;
    private String tenantId;
    private String clientId;
    private String clientSecretEncrypted;
    private String authorityUrl;
    private String scopes;
    private String graphApiBaseUrl;
    private boolean multiTenant;
    private String redirectUri;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean active;
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    
    public String getClientSecretEncrypted() { return clientSecretEncrypted; }
    public void setClientSecretEncrypted(String clientSecretEncrypted) { this.clientSecretEncrypted = clientSecretEncrypted; }
    
    public String getAuthorityUrl() { return authorityUrl; }
    public void setAuthorityUrl(String authorityUrl) { this.authorityUrl = authorityUrl; }
    
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    
    public String getGraphApiBaseUrl() { return graphApiBaseUrl; }
    public void setGraphApiBaseUrl(String graphApiBaseUrl) { this.graphApiBaseUrl = graphApiBaseUrl; }
    
    public boolean isMultiTenant() { return multiTenant; }
    public void setMultiTenant(boolean multiTenant) { this.multiTenant = multiTenant; }
    
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}