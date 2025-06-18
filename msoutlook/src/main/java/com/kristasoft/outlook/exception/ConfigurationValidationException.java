package com.kristasoft.outlook.exception;

public class ConfigurationValidationException extends Exception {
    public ConfigurationValidationException(String message) {
        super(message);
    }
    
    public ConfigurationValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}