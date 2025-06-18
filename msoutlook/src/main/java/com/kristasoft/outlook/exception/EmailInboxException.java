package com.kristasoft.outlook.exception;

/**
 * Exception thrown when there's an error retrieving emails from the inbox.
 */
public class EmailInboxException extends Exception {
    
    public EmailInboxException(String message) {
        super(message);
    }
    
    public EmailInboxException(String message, Throwable cause) {
        super(message, cause);
    }
}