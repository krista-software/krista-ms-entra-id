package com.kristasoft.outlook.service.impl;

import com.kristasoft.outlook.exception.ConfigurationValidationException;
import com.kristasoft.outlook.model.OutlookConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MicrosoftConfigurationValidatorImplTest {

    private MicrosoftConfigurationValidatorImpl validator;
    private OutlookConfiguration validConfig;

    @Before
    public void setUp() {
        validator = new MicrosoftConfigurationValidatorImpl();
        
        // Setup valid configuration for testing
        validConfig = new OutlookConfiguration();
        validConfig.setTenantId("3694f6b4-b5f1-47ef-852f-a0b4a459ab44");
        validConfig.setClientId("ec0745c8-7635-4b31-97cc-d217944dd620");
        validConfig.setClientSecret("validClientSecret");
        validConfig.setAuthorityUrl("https://login.microsoftonline.com/tenant-id");
        validConfig.setGraphApiBaseUrl("https://graph.microsoft.com/v1.0");
        validConfig.setRedirectUri("https://example.com/redirect");
        validConfig.setScopes(Arrays.asList(
                "Mail.Read", "Mail.ReadWrite", "Mail.Send", 
                "Calendars.ReadWrite", "Contacts.ReadWrite"));
    }

    @Test
    public void testValidConfigurationWithKristaOAuthApp() throws Exception {
        // Only tenant ID is required when using Krista OAuth App
        OutlookConfiguration config = new OutlookConfiguration();
        config.setTenantId("3694f6b4-b5f1-47ef-852f-a0b4a459ab44");
        
        // Should not throw exception
        validator.validate(config, true);
    }

    @Test
    public void testValidConfigurationWithCustomOAuthApp() throws Exception {
        // Should not throw exception
        validator.validate(validConfig, false);
    }

    @Test(expected = ConfigurationValidationException.class)
    public void testMissingTenantId() throws Exception {
        validConfig.setTenantId(null);
        validator.validate(validConfig, false);
    }

    @Test(expected = ConfigurationValidationException.class)
    public void testInvalidTenantIdFormat() throws Exception {
        validConfig.setTenantId("invalid-uuid-format");
        validator.validate(validConfig, false);
    }

    @Test(expected = ConfigurationValidationException.class)
    public void testMissingClientIdWithCustomOAuth() throws Exception {
        validConfig.setClientId(null);
        validator.validate(validConfig, false);
    }

    @Test(expected = ConfigurationValidationException.class)
    public void testInvalidClientIdFormat() throws Exception {
        validConfig.setClientId("invalid-client-id");
        validator.validate(validConfig, false);
    }

    @Test(expected = ConfigurationValidationException.class)
    public void testMissingClientSecretWithCustomOAuth() throws Exception {
        validConfig.setClientSecret(null);
        validator.validate(validConfig, false);
    }

    @Test(expected = ConfigurationValidationException.class)
    public void testEmptyClientSecretWithCustomOAuth() throws Exception {
        validConfig.setClientSecret("");
        validator.validate(validConfig, false);
    }

    @Test(expected = ConfigurationValidationException.class)
    public void testInvalidAuthorityUrl() throws Exception {
        validConfig.setAuthorityUrl("not-a-url");
        validator.validate(validConfig, false);
    }

    @Test(expected = ConfigurationValidationException.class)
    public void testNonMicrosoftAuthorityUrl() throws Exception {
        validConfig.setAuthorityUrl("https://example.com");
        validator.validate(validConfig, false);
    }

    @Test
    public void testNullAuthorityUrl() throws Exception {
        validConfig.setAuthorityUrl(null);
        // Should not throw exception as it's optional
        validator.validate(validConfig, false);
    }

    @Test(expected = ConfigurationValidationException.class)
    public void testInvalidGraphApiBaseUrl() throws Exception {
        validConfig.setGraphApiBaseUrl("not-a-url");
        validator.validate(validConfig, false);
    }

    @Test(expected = ConfigurationValidationException.class)
    public void testNonMicrosoftGraphApiBaseUrl() throws Exception {
        validConfig.setGraphApiBaseUrl("https://example.com");
        validator.validate(validConfig, false);
    }

    @Test
    public void testNullGraphApiBaseUrl() throws Exception {
        validConfig.setGraphApiBaseUrl(null);
        // Should not throw exception as it's optional
        validator.validate(validConfig, false);
    }

    @Test(expected = ConfigurationValidationException.class)
    public void testMissingScopesWithCustomOAuth() throws Exception {
        validConfig.setScopes(null);
        validator.validate(validConfig, false);
    }

    @Test(expected = ConfigurationValidationException.class)
    public void testEmptyScopesWithCustomOAuth() throws Exception {
        validConfig.setScopes(Collections.emptyList());
        validator.validate(validConfig, false);
    }

    @Test
    public void testMissingSomeRecommendedScopes() throws Exception {
        // Missing some recommended scopes should only log warnings, not throw exceptions
        validConfig.setScopes(Collections.singletonList("Mail.Read"));
        validator.validate(validConfig, false);
    }

    @Test(expected = ConfigurationValidationException.class)
    public void testMissingRedirectUriWithCustomOAuth() throws Exception {
        validConfig.setRedirectUri(null);
        validator.validate(validConfig, false);
    }

    @Test(expected = ConfigurationValidationException.class)
    public void testInvalidRedirectUri() throws Exception {
        validConfig.setRedirectUri("not-a-url");
        validator.validate(validConfig, false);
    }

    @Test
    public void testClientIdNotRequiredWithKristaOAuth() throws Exception {
        OutlookConfiguration config = new OutlookConfiguration();
        config.setTenantId("3694f6b4-b5f1-47ef-852f-a0b4a459ab44");
        config.setClientId(null); // No client ID
        
        // Should not throw exception when using Krista OAuth
        validator.validate(config, true);
    }

    @Test
    public void testErrorMessageForMissingTenantId() {
        try {
            validConfig.setTenantId(null);
            validator.validate(validConfig, false);
            fail("Expected ConfigurationValidationException was not thrown");
        } catch (ConfigurationValidationException e) {
            assertEquals("Tenant ID is required", e.getMessage());
        }
    }

    @Test
    public void testErrorMessageForInvalidTenantIdFormat() {
        try {
            validConfig.setTenantId("invalid-format");
            validator.validate(validConfig, false);
            fail("Expected ConfigurationValidationException was not thrown");
        } catch (ConfigurationValidationException e) {
            assertEquals("Invalid Tenant ID format. Must be a valid GUID.", e.getMessage());
        }
    }
}