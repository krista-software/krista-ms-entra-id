package com.kristasoft.outlook.exception;

public class ConfigurationStorageException extends Exception {
    public ConfigurationStorageException(String message) {
        super(message);
    }
    
    public ConfigurationStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}