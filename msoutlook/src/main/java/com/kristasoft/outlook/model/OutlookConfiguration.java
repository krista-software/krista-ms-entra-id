package com.kristasoft.outlook.model;

import java.util.List;

public class OutlookConfiguration {
    private String tenantId;
    private String clientId;
    private String clientSecret;
    private String authorityUrl;
    private List<String> scopes;
    private String graphApiBaseUrl;
    private boolean multiTenant;
    private String redirectUri;
    
    // Getters and setters
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    
    public String getAuthorityUrl() { return authorityUrl; }
    public void setAuthorityUrl(String authorityUrl) { this.authorityUrl = authorityUrl; }
    
    public List<String> getScopes() { return scopes; }
    public void setScopes(List<String> scopes) { this.scopes = scopes; }
    
    public String getGraphApiBaseUrl() { return graphApiBaseUrl; }
    public void setGraphApiBaseUrl(String graphApiBaseUrl) { this.graphApiBaseUrl = graphApiBaseUrl; }
    
    public boolean isMultiTenant() { return multiTenant; }
    public void setMultiTenant(boolean multiTenant) { this.multiTenant = multiTenant; }
    
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
}