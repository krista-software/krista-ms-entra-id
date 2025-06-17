package com.kristasoft.outlook.model;

public class ConfigurationSaveResult {
    private final boolean isConfigurationSaved;
    private final ExtensionResponse extensionResponse;
    
    private ConfigurationSaveResult(boolean isConfigurationSaved, ExtensionResponse extensionResponse) {
        this.isConfigurationSaved = isConfigurationSaved;
        this.extensionResponse = extensionResponse;
    }
    
    public static ConfigurationSaveResult success(String message, long timeTakenInSeconds) {
        ExtensionResponse response = new ExtensionResponse(
            message,
            "",
            "SUCCESS",
            timeTakenInSeconds
        );
        return new ConfigurationSaveResult(true, response);
    }
    
    public static ConfigurationSaveResult failure(
            String message, 
            String responseType, 
            String technicalDetailedErrorReport, 
            long timeTakenInSeconds) {
        ExtensionResponse response = new ExtensionResponse(
            message,
            technicalDetailedErrorReport,
            responseType,
            timeTakenInSeconds
        );
        return new ConfigurationSaveResult(false, response);
    }
    
    public boolean isConfigurationSaved() {
        return isConfigurationSaved;
    }
    
    public ExtensionResponse getExtensionResponse() {
        return extensionResponse;
    }
}