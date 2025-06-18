package com.kristasoft.outlook.exception;

/**
 * Exception thrown when there's an error retrieving email folders.
 */
public class EmailFolderException extends Exception {
    
    public EmailFolderException(String message) {
        super(message);
    }
    
    public EmailFolderException(String message, Throwable cause) {
        super(message, cause);
    }
}