package com.kristasoft.outlook.model;

import java.time.Instant;

public class AccessToken {
    private final String token;
    private final Instant expiresOn;
    
    public AccessToken(String token, Instant expiresOn) {
        this.token = token;
        this.expiresOn = expiresOn;
    }
    
    public String getToken() {
        return token;
    }
    
    public Instant getExpiresOn() {
        return expiresOn;
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiresOn);
    }
}