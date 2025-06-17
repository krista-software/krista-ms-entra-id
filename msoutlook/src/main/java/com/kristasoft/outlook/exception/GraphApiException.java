package com.kristasoft.outlook.exception;

public class GraphApiException extends Exception {
    public GraphApiException(String message) {
        super(message);
    }
    
    public GraphApiException(String message, Throwable cause) {
        super(message, cause);
    }
}