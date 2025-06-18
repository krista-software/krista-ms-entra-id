# Global Catalog Change Request Template

|**Ecosystem**|Essentials|
| :- | :- |
|**Domain**|Collaboration|
|**Area**|Messaging|
|**Request Type**|Change System|
|**Request Name**|Save Configuration|
|**Request Description**|This save configuration request stores the Outlook/Microsoft Graph connection credentials and settings in the database and performs a test connection using the provided parameters. It validates OAuth application credentials, tenant configuration, and API connectivity to ensure proper integration with Microsoft Graph services.|
|**Extension Name**|Outlook|
|<p>**Extension Description**</p><p></p>|The Outlook Extension integrates with Microsoft Graph APIs to enable comprehensive email management operations including folder management, email operations, calendar management, contact management, and task management through conversational workflows.|
|**Use Case**|To configure and validate Microsoft Graph API connection settings for Outlook integration, ensuring secure authentication and proper API connectivity for email management operations|
|**Data Storage**|OAuth configuration and connection settings stored securely in extension database with encrypted credentials|
|**Data Caching**|Configuration validation results cached temporarily for performance optimization|
|**Authentication**|OAuth 2.0 with Microsoft Graph API using Application Registration credentials|

## Input Parameters

**Name:** Use Krista OAuth App

**Type:** Boolean

**Is Required:** FALSE

**Tooltip:** Whether to use Krista's pre-configured OAuth application with default settings (default: true). When enabled, uses Krista's registered application with predefined scopes and configuration. Set to false to use custom OAuth application settings.

---

**Name:** Configuration

**Type:** MultiField

**Is Required:** TRUE

**Tooltip:** Microsoft Graph API configuration settings including OAuth application details, tenant information, and connection parameters. When 'Use Krista OAuth App' is enabled, only Tenant ID is required as other values are pre-filled.

**Configuration Sub Fields:**

|**Field Type**|**Name**|**Tooltip**|**Required When Krista App**|**Required When Custom App**|
| :-: | :-: | :-: | :-: | :-: |
|Text|Tenant ID|Azure AD tenant identifier (GUID format) for the organization|TRUE|TRUE|
|Text|Client ID|Application (client) ID from Azure AD app registration (auto-filled when using Krista OAuth App)|FALSE|TRUE|
|Text(secured)|Client Secret|Application client secret for authentication (auto-filled when using Krista OAuth App, stored encrypted)|FALSE|TRUE|
|Text|Authority URL|Microsoft identity platform authority URL (auto-filled: https://login.microsoftonline.com/{tenant})|FALSE|FALSE|
|List of Text|Scopes|Required Microsoft Graph API scopes (auto-filled: Mail.Read, Mail.ReadWrite, Mail.Send, Calendars.ReadWrite, Contacts.ReadWrite)|FALSE|TRUE|
|Text|Graph API Base URL|Microsoft Graph API base URL (auto-filled: https://graph.microsoft.com/v1.0)|FALSE|FALSE|
|Boolean|Use Multi-Tenant|Whether the application supports multi-tenant authentication (auto-filled: true for Krista app)|FALSE|FALSE|
|Text|Redirect URI|OAuth redirect URI configured in Azure AD app registration (auto-filled when using Krista OAuth App)|FALSE|TRUE|

## Output Parameters

**Parameter Name:** Is Configuration Saved \
**Field Type:** Boolean \
**Tooltip:** Returns true if the configuration was successfully saved and validated, including the result of the test connection to Microsoft Graph API.

**Parameter Name:** Extension Response\
**Field Type:** Entity\
**Tooltip:** Represents the response returned by the extension process, including the result of the operation.\
**Sub Fields:**

|**Field Type**|**Name**|**Tooltip**|
| :-: | :-: | :-: |
|Text|Message|A detailed message describing the extension's response|
|Rich Text|Technical Detailed Error Report|Provides error details to assist the technical team in troubleshooting|
|Pick One|Response Type|Specifies the type of exception to aid in categorizing and handling errors|
|Number|Time Taken In Seconds|Indicates the total duration of the operation, measured in seconds|

## Configuration Validation Process

### Step 1: Configuration Mode Selection
- Determine if using Krista OAuth App or custom application
- Apply appropriate validation rules based on selection
- Auto-fill Krista OAuth App values when selected

### Step 2: Input Validation
- Validate Tenant ID format (GUID) - required for both modes
- **When using Krista OAuth App:**
  - Auto-fill Client ID with Krista's registered application ID
  - Auto-fill Client Secret with Krista's application secret
  - Auto-fill Authority URL: `https://login.microsoftonline.com/{tenant}`
  - Auto-fill Scopes: `["Mail.Read", "Mail.ReadWrite", "Mail.Send", "Calendars.ReadWrite", "Contacts.ReadWrite"]`
  - Auto-fill Graph API Base URL: `https://graph.microsoft.com/v1.0`
  - Auto-fill Multi-Tenant: `true`
  - Auto-fill Redirect URI with Krista's registered redirect URI
- **When using Custom OAuth App:**
  - Validate Client ID format (GUID)
  - Verify Client Secret is provided and not empty
  - Validate Authority URL format and accessibility
  - Verify required scopes are provided
  - Validate Redirect URI format

### Step 3: OAuth Application Test
- Attempt to acquire access token using client credentials flow
- Validate application permissions and consent
- Test token refresh capability
- Verify scope availability

### Step 4: Microsoft Graph API Connectivity Test
- Test basic Graph API connectivity
- Validate user profile access
- Test mailbox access with provided scopes
- Verify API response format and authentication

### Expected Test API Calls
```
POST https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials
&client_id={client_id}
&client_secret={client_secret}
&scope=https://graph.microsoft.com/.default

GET https://graph.microsoft.com/v1.0/me
Authorization: Bearer {access_token}
```

## Error Handling Scenarios:

|**Scenario**|**Exception Type**|**When It Occurs**|**Remediation Action**|**Related Java Exception**|
| :-: | :-: | :-: | :-: | :-: |
|Missing tenant ID|INPUT\_ERROR|Tenant ID is not provided or empty|Provide a valid Azure AD tenant ID (GUID format)|IllegalArgumentException, NullPointerException|
|Invalid tenant ID format|INPUT\_ERROR|Tenant ID is not in valid GUID format|Ensure tenant ID follows GUID format (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)|IllegalArgumentException, PatternSyntaxException|
|Missing client ID (custom app)|INPUT\_ERROR|Client ID is not provided when using custom OAuth app|Provide a valid application client ID from Azure AD app registration|IllegalArgumentException, NullPointerException|
|Invalid client ID format (custom app)|INPUT\_ERROR|Client ID is not in valid GUID format when using custom OAuth app|Ensure client ID follows GUID format from Azure AD app registration|IllegalArgumentException, PatternSyntaxException|
|Missing client secret (custom app)|INPUT\_ERROR|Client secret is not provided when using custom OAuth app|Provide a valid client secret from Azure AD app registration|IllegalArgumentException, NullPointerException|
|Krista OAuth App not configured|SYSTEM\_ERROR|Krista OAuth App credentials are not properly configured in the system|Contact system administrator to configure Krista OAuth App credentials|ConfigurationException, SystemConfigurationException|
|Invalid authority URL|INPUT\_ERROR|Authority URL is malformed or unreachable|Use valid Microsoft identity platform authority URL|IllegalArgumentException, MalformedURLException|
|Missing required scopes|INPUT\_ERROR|No scopes provided or invalid scope format|Provide required Microsoft Graph API scopes (e.g., Mail.Read, Mail.ReadWrite)|IllegalArgumentException, ValidationException|
|Invalid redirect URI|INPUT\_ERROR|Redirect URI format is invalid or not configured in Azure AD|Ensure redirect URI matches Azure AD app registration configuration|IllegalArgumentException, MalformedURLException|
|Tenant not found|AUTHENTICATION\_ERROR|Specified tenant ID does not exist or is not accessible|Verify tenant ID is correct and accessible|AuthenticationException, TenantNotFoundException|
|Application not found|AUTHENTICATION\_ERROR|Client ID does not correspond to a valid Azure AD application|Verify client ID is correct and application exists in Azure AD|AuthenticationException, ApplicationNotFoundException|
|Invalid client secret|AUTHENTICATION\_ERROR|Client secret is incorrect or expired|Update client secret in Azure AD app registration and configuration|AuthenticationException, InvalidClientSecretException|
|Insufficient application permissions|AUTHENTICATION\_ERROR|Application lacks required permissions for Microsoft Graph API|Grant required permissions in Azure AD app registration and admin consent|SecurityException, InsufficientPermissionsException|
|Admin consent required|AUTHENTICATION\_ERROR|Application requires admin consent for requested scopes|Obtain admin consent for the application in Azure AD|SecurityException, AdminConsentRequiredException|
|Application disabled|AUTHENTICATION\_ERROR|Azure AD application is disabled or suspended|Enable the application in Azure AD app registration|SecurityException, ApplicationDisabledException|
|Token acquisition failed|AUTHENTICATION\_ERROR|Failed to acquire access token from Microsoft identity platform|Check application configuration and network connectivity|AuthenticationException, TokenAcquisitionException|
|Graph API access denied|AUTHENTICATION\_ERROR|Access token cannot access Microsoft Graph API|Verify application permissions and scope configuration|SecurityException, GraphApiAccessDeniedException|
|Network connectivity issues|UNAVAILABILITY\_ERROR|Unable to reach Microsoft identity platform or Graph API|Check network connectivity and firewall settings|ConnectException, UnknownHostException|
|Authority endpoint unreachable|UNAVAILABILITY\_ERROR|Microsoft identity platform authority is unreachable|Verify authority URL and network connectivity|ConnectException, ServiceUnavailableException|
|Graph API service outage|UNAVAILABILITY\_ERROR|Microsoft Graph API is temporarily unavailable|Monitor service status and retry later|ServiceUnavailableException, ConnectException|
|Configuration storage failed|SYSTEM\_ERROR|Failed to save configuration to database|Check database connectivity and retry|SQLException, DatabaseException|
|Encryption failed|SYSTEM\_ERROR|Failed to encrypt client secret for storage|Check encryption service and retry|EncryptionException, SecurityException|
|Timeout during validation|UNAVAILABILITY\_ERROR|Configuration validation takes too long|Increase timeout values and check network performance|SocketTimeoutException, TimeoutException|
|SSL/TLS certificate error|SYSTEM\_ERROR|Certificate validation fails for Microsoft endpoints|Update certificate store or check SSL configuration|SSLException, CertificateException|

## Open Telemetry Metrics:

|**Metric Name**|**Type**|**Description**|||||
| :-: | :-: | :-: | :- | :- | :- | :- |
|outlook\_save\_config\_request\_count|Counter|Total number of Save Configuration requests received|||||
|outlook\_save\_config\_success\_count|Counter|Number of successful configuration save operations|||||
|outlook\_save\_config\_failure\_count|Counter|Number of failed configuration save operations|||||
|outlook\_save\_config\_latency\_ms|Histogram|Time taken to complete the configuration save operation (in milliseconds)|||||
|outlook\_save\_config\_error\_type|Counter (with labels)|Count of errors categorized by ExceptionType (e.g., INPUT\_ERROR, AUTHENTICATION\_ERROR, etc.)|||||
|outlook\_config\_validation\_errors|Counter|Number of validation errors encountered during configuration validation|||||
|outlook\_token\_acquisition\_attempts|Counter|Number of OAuth token acquisition attempts during validation|||||
|outlook\_token\_acquisition\_success\_count|Counter|Number of successful OAuth token acquisitions|||||
|outlook\_graph\_api\_test\_calls\_count|Counter|Number of test API calls made to Microsoft Graph during validation|||||
|outlook\_graph\_api\_test\_latency\_ms|Histogram|Response time for Microsoft Graph API test calls (in milliseconds)|||||
|outlook\_config\_encryption\_operations|Counter|Number of configuration encryption operations performed|||||
|outlook\_multi\_tenant\_configs\_count|Counter|Number of multi-tenant configurations saved|||||

## Logs and log levels

|**Log Level**|**What to Log**|**Sample Log Messages**|
| :-: | :-: | :-: |
|**INFO**|High-level successful operation steps|- "Save Configuration request started for Outlook extension with tenant 'contoso.onmicrosoft.com' using Krista OAuth App."|
| | |- "Save Configuration request started for Outlook extension with tenant 'contoso.onmicrosoft.com' using custom OAuth app."|
| | |- "Configuration validation completed successfully in 1.2 seconds using Krista pre-configured settings."|
| | |- "Outlook configuration saved successfully with encrypted credentials (Krista OAuth App mode)."|
| | |- "Microsoft Graph API connectivity test passed for tenant 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx' with Krista scopes."|
|**WARN**|Recoverable issues or unexpected but non-fatal states|- "Client secret expires in less than 30 days, consider renewal."|
| | |- "Configuration validation response time nearing timeout threshold."|
| | |- "Multi-tenant configuration detected, ensure proper consent management."|
| | |- "Some requested scopes may require admin consent."|
|**ERROR**|Failures impacting operation or user experience|- "Save Configuration failed: Invalid tenant ID format provided."|
| | |- "OAuth token acquisition failed: Invalid client secret for application 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'."|
| | |- "Microsoft Graph API test failed: Insufficient permissions for Mail.Read scope."|
| | |- "Configuration save failed: Database encryption error."|
| | |- "Unexpected system error during configuration save: java.net.ConnectException: Connection refused."|
|**DEBUG**|Detailed flow for troubleshooting|- "Validating configuration parameters: Tenant ID, Client ID, Authority URL format."|
| | |- "Attempting OAuth token acquisition with client credentials flow."|
| | |- "Testing Microsoft Graph API connectivity: GET /v1.0/me."|
| | |- "Encrypting client secret for secure database storage."|
| | |- "Configuration validation completed: All tests passed, saving to database."|
|**TRACE**|Very fine-grained trace for deep debugging (optional)|- "Entered method saveConfiguration() with tenant 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'."|
| | |- "OAuth token acquired successfully, expires in 3600 seconds."|
| | |- "Microsoft Graph API response: {displayName: 'User Name', mail: 'user@contoso.com'}."|
| | |- "Configuration encrypted and stored with ID 'config_12345'."|
| | |- "Exiting method saveConfiguration() after 1.2 seconds with successful save."|

## Microsoft Graph Configuration Management

### OAuth Application Setup Requirements
1. **Azure AD App Registration**: Application must be registered in Azure Active Directory
2. **API Permissions**: Required Microsoft Graph API permissions must be granted
3. **Client Secret**: Valid client secret must be generated and not expired
4. **Redirect URI**: Proper redirect URI must be configured for OAuth flows
5. **Admin Consent**: Admin consent may be required for certain scopes

### Configuration Validation Implementation
```java
// Comprehensive configuration validation with Krista OAuth App support
public ConfigurationValidationResult validateOutlookConfiguration(boolean useKristaOAuthApp, OutlookConfiguration config) {
    try {
        // Step 1: Configuration mode selection and auto-fill
        OutlookConfiguration finalConfig = prepareConfiguration(useKristaOAuthApp, config);

        // Step 2: Input validation
        validateInputParameters(finalConfig, useKristaOAuthApp);

        // Step 3: Test OAuth token acquisition
        AccessToken token = acquireAccessToken(finalConfig);

        // Step 4: Test Microsoft Graph API connectivity
        testGraphApiConnectivity(token, finalConfig);

        // Step 5: Validate scopes and permissions
        validateScopesAndPermissions(token, finalConfig.getScopes());

        // Step 6: Test mailbox access
        testMailboxAccess(token);

        return ConfigurationValidationResult.success(finalConfig);

    } catch (Exception e) {
        return ConfigurationValidationResult.failure(e);
    }
}

// Prepare configuration based on Krista OAuth App selection
private OutlookConfiguration prepareConfiguration(boolean useKristaOAuthApp, OutlookConfiguration inputConfig) {
    if (useKristaOAuthApp) {
        // Use Krista's pre-configured OAuth application
        OutlookConfiguration kristaConfig = new OutlookConfiguration();
        kristaConfig.setTenantId(inputConfig.getTenantId()); // Only tenant ID required from user
        kristaConfig.setClientId(KRISTA_OAUTH_CLIENT_ID); // Krista's registered app ID
        kristaConfig.setClientSecret(KRISTA_OAUTH_CLIENT_SECRET); // Krista's app secret
        kristaConfig.setAuthorityUrl("https://login.microsoftonline.com/" + inputConfig.getTenantId());
        kristaConfig.setScopes(Arrays.asList("Mail.Read", "Mail.ReadWrite", "Mail.Send",
                                           "Calendars.ReadWrite", "Contacts.ReadWrite"));
        kristaConfig.setGraphApiBaseUrl("https://graph.microsoft.com/v1.0");
        kristaConfig.setMultiTenant(true);
        kristaConfig.setRedirectUri(KRISTA_OAUTH_REDIRECT_URI); // Krista's registered redirect URI

        LOGGER.info("Using Krista OAuth App configuration for tenant: {}", inputConfig.getTenantId());
        return kristaConfig;
    } else {
        // Use customer's custom OAuth application
        LOGGER.info("Using custom OAuth App configuration for tenant: {}", inputConfig.getTenantId());
        return inputConfig;
    }
}

// OAuth token acquisition for validation
private AccessToken acquireAccessToken(OutlookConfiguration config) throws AuthenticationException {
    ConfidentialClientApplication app = ConfidentialClientApplication.builder(
        config.getClientId(),
        ClientCredentialFactory.createFromSecret(config.getClientSecret()))
        .authority(config.getAuthorityUrl())
        .build();

    ClientCredentialParameters parameters = ClientCredentialParameters.builder(
        Set.of("https://graph.microsoft.com/.default"))
        .build();

    CompletableFuture<IAuthenticationResult> future = app.acquireToken(parameters);
    IAuthenticationResult result = future.get(30, TimeUnit.SECONDS);

    return new AccessToken(result.accessToken(), result.expiresOnDate());
}
```

## Security and Encryption

### Credential Protection
- **Client Secret Encryption**: All client secrets encrypted before database storage
- **Secure Storage**: Configuration stored in encrypted database fields
- **Access Control**: Configuration access restricted to authorized users
- **Audit Logging**: All configuration changes logged for security monitoring

### Encryption Implementation
```java
// Secure configuration storage
public void saveConfigurationSecurely(OutlookConfiguration config) {
    try {
        // Encrypt sensitive fields
        String encryptedClientSecret = encryptionService.encrypt(config.getClientSecret());

        // Create secure configuration entity
        OutlookConfigurationEntity entity = new OutlookConfigurationEntity();
        entity.setTenantId(config.getTenantId());
        entity.setClientId(config.getClientId());
        entity.setClientSecretEncrypted(encryptedClientSecret);
        entity.setAuthorityUrl(config.getAuthorityUrl());
        entity.setScopes(String.join(",", config.getScopes()));
        entity.setGraphApiBaseUrl(config.getGraphApiBaseUrl());
        entity.setMultiTenant(config.isMultiTenant());
        entity.setRedirectUri(config.getRedirectUri());
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        // Save to database
        configurationRepository.save(entity);

        LOGGER.info("Outlook configuration saved securely for tenant: {}", config.getTenantId());

    } catch (Exception e) {
        LOGGER.error("Failed to save Outlook configuration securely", e);
        throw new ConfigurationStorageException("Failed to save configuration", e);
    }
}
```

## Example Response Scenarios

### Successful Configuration Save Response (Using Krista OAuth App)
```json
{
  "isConfigurationSaved": true,
  "extensionResponse": {
    "message": "Outlook configuration saved successfully using Krista OAuth App. Microsoft Graph API connectivity validated for tenant 'contoso.onmicrosoft.com' with pre-configured scopes: Mail.Read, Mail.ReadWrite, Mail.Send, Calendars.ReadWrite, Contacts.ReadWrite.",
    "technicalDetailedErrorReport": "",
    "responseType": "SUCCESS",
    "timeTakenInSeconds": 1.2
  }
}
```

### Successful Configuration Save Response (Using Custom OAuth App)
```json
{
  "isConfigurationSaved": true,
  "extensionResponse": {
    "message": "Outlook configuration saved successfully using custom OAuth application. Microsoft Graph API connectivity validated for tenant 'contoso.onmicrosoft.com' with custom scopes and settings.",
    "technicalDetailedErrorReport": "",
    "responseType": "SUCCESS",
    "timeTakenInSeconds": 1.8
  }
}
```

### Invalid Client Secret Response
```json
{
  "isConfigurationSaved": false,
  "extensionResponse": {
    "message": "Configuration save failed due to authentication error",
    "technicalDetailedErrorReport": "OAuth token acquisition failed: Invalid client secret for application 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'. Verify client secret is correct and not expired in Azure AD app registration.",
    "responseType": "AUTHENTICATION_ERROR",
    "timeTakenInSeconds": 2.5
  }
}
```

### Insufficient Permissions Response
```json
{
  "isConfigurationSaved": false,
  "extensionResponse": {
    "message": "Configuration save failed due to insufficient permissions",
    "technicalDetailedErrorReport": "Application lacks required permissions for Microsoft Graph API. Required scopes: [Mail.Read, Mail.ReadWrite, Mail.Send]. Grant permissions in Azure AD app registration and obtain admin consent.",
    "responseType": "AUTHENTICATION_ERROR",
    "timeTakenInSeconds": 1.8
  }
}
```

### Network Connectivity Error Response
```json
{
  "isConfigurationSaved": false,
  "extensionResponse": {
    "message": "Configuration save failed due to connectivity issues",
    "technicalDetailedErrorReport": "Unable to reach Microsoft identity platform at 'https://login.microsoftonline.com/tenant-id/oauth2/v2.0/token'. Check network connectivity and firewall settings.",
    "responseType": "UNAVAILABILITY_ERROR",
    "timeTakenInSeconds": 30.0
  }
}
```

### Krista OAuth App Not Configured Response
```json
{
  "isConfigurationSaved": false,
  "extensionResponse": {
    "message": "Configuration save failed: Krista OAuth App not properly configured",
    "technicalDetailedErrorReport": "Krista OAuth App credentials are not configured in the system. Contact system administrator to configure Krista OAuth App settings (Client ID, Client Secret, Redirect URI) before using this option.",
    "responseType": "SYSTEM_ERROR",
    "timeTakenInSeconds": 0.1
  }
}
```

## Performance Considerations

### Validation Optimization
- **Parallel Validation**: Run multiple validation steps concurrently when possible
- **Caching**: Cache validation results for recently tested configurations
- **Timeout Management**: Implement appropriate timeouts for external API calls
- **Connection Pooling**: Use connection pooling for HTTP requests

### Database Operations
- **Batch Operations**: Batch multiple configuration updates when possible
- **Index Optimization**: Ensure proper database indexing for configuration queries
- **Connection Management**: Use database connection pooling
- **Transaction Management**: Proper transaction handling for configuration saves

### Error Recovery
- Implement exponential backoff for transient failures
- Use circuit breaker pattern for repeated failures
- Provide graceful degradation when external services are unavailable
- Implement retry logic with jitter to avoid thundering herd

## Configuration Management Best Practices

### Azure AD App Registration Guidelines
- **Principle of Least Privilege**: Request only necessary API permissions
- **Certificate-Based Authentication**: Consider using certificates instead of client secrets for enhanced security
- **Regular Secret Rotation**: Implement regular client secret rotation
- **Multi-Tenant Considerations**: Properly configure multi-tenant applications when needed

### Monitoring and Maintenance
```java
// Configuration health monitoring
@Scheduled(fixedRate = 3600000) // Every hour
public void monitorConfigurationHealth() {
    List<OutlookConfigurationEntity> configs = configurationRepository.findAllActive();

    for (OutlookConfigurationEntity config : configs) {
        try {
            // Test configuration health
            ConfigurationHealthResult health = testConfigurationHealth(config);

            if (!health.isHealthy()) {
                LOGGER.warn("Configuration health issue detected for tenant: {}, Issue: {}",
                    config.getTenantId(), health.getIssueDescription());

                // Send alert for configuration issues
                alertService.sendConfigurationAlert(config, health);
            }

            // Update health metrics
            updateConfigurationHealthMetrics(config, health);

        } catch (Exception e) {
            LOGGER.error("Error monitoring configuration health for tenant: {}",
                config.getTenantId(), e);
        }
    }
}
```

### Compliance and Audit
- **Audit Trail**: Maintain complete audit trail of configuration changes
- **Data Retention**: Implement proper data retention policies
- **Compliance Reporting**: Generate compliance reports for configuration management
- **Access Logging**: Log all configuration access and modifications

## Integration Testing

### Automated Validation Tests
- **Token Acquisition Test**: Verify OAuth token can be acquired successfully
- **API Connectivity Test**: Test basic Microsoft Graph API connectivity
- **Permission Validation Test**: Verify all required permissions are available
- **Scope Testing**: Test each configured scope individually
- **Multi-Tenant Testing**: Validate multi-tenant configurations when applicable

### Test Implementation Example
```java
// Comprehensive integration test
@Test
public void testOutlookConfigurationIntegration() {
    // Arrange
    OutlookConfiguration testConfig = createTestConfiguration();

    // Act
    ConfigurationSaveResult result = outlookConfigurationService.saveConfiguration(testConfig);

    // Assert
    assertTrue(result.isSuccess());
    assertNotNull(result.getConfigurationId());

    // Verify token acquisition works
    AccessToken token = outlookAuthService.acquireToken(testConfig);
    assertNotNull(token);
    assertFalse(token.isExpired());

    // Verify Graph API connectivity
    GraphApiTestResult apiTest = graphApiService.testConnectivity(token);
    assertTrue(apiTest.isSuccessful());

    // Verify mailbox access
    MailboxTestResult mailboxTest = mailboxService.testAccess(token);
    assertTrue(mailboxTest.hasAccess());
}
```

The implementation should demonstrate enterprise-level configuration management with comprehensive validation, robust security practices, and optimized performance for Microsoft Graph API integration with proper credential protection, audit trails, and health monitoring capabilities.
