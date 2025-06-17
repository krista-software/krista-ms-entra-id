package com.kristasoft.outlook.model;

public class ExtensionResponse {
    private final String message;
    private final String technicalDetailedErrorReport;
    private final String responseType;
    private final long timeTakenInSeconds;
    
    public ExtensionResponse(
            String message, 
            String technicalDetailedErrorReport, 
            String responseType, 
            long timeTakenInSeconds) {
        this.message = message;
        this.technicalDetailedErrorReport = technicalDetailedErrorReport;
        this.responseType = responseType;
        this.timeTakenInSeconds = timeTakenInSeconds;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getTechnicalDetailedErrorReport() {
        return technicalDetailedErrorReport;
    }
    
    public String getResponseType() {
        return responseType;
    }
    
    public long getTimeTakenInSeconds() {
        return timeTakenInSeconds;
    }
}