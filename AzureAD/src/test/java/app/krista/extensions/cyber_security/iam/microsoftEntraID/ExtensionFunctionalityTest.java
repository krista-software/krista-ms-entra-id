package app.krista.extensions.cyber_security.iam.microsoftEntraID;

import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.validators.*;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.impl.AccountImpl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Functional tests for Microsoft Entra ID Extension.
 * <p>
 * These tests focus on testing actual extension functionality:
 * - Validator behavior with real scenarios
 * - Error handling patterns
 * - Input validation logic
 * - Service integration patterns
 * <p>
 * Tests are designed to work with the actual API signatures and provide
 * meaningful coverage of real extension behavior.
 */
public class ExtensionFunctionalityTest {

    @Mock
    private AccountImpl mockAccountService;

    private EmailValidator emailValidator;
    private CreateAccountEmailValidator createAccountEmailValidator;
    private ValidationOrchestrator validationOrchestrator;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        // Initialize validators with actual constructors
        emailValidator = new EmailValidator(mockAccountService);
        createAccountEmailValidator = new CreateAccountEmailValidator(mockAccountService);
        validationOrchestrator = new ValidationOrchestrator();
    }

    // ========== EMAIL VALIDATOR FUNCTIONALITY TESTS ==========

    @Test
    public void testEmailValidator_WithExistingUser_ReturnsTrue() {
        // Arrange
        String validEmail = "existing@example.com";
        // Mock user exists in Microsoft Entra ID
        when(mockAccountService.getUser(validEmail)).thenReturn(createMockAccount());

        // Act
        Boolean result = emailValidator.validate(validEmail, createEmptyContext());

        // Assert
        assertTrue("Valid existing email should pass validation", result);
        verify(mockAccountService).getUser(validEmail);
    }

    @Test
    public void testEmailValidator_WithNonExistentUser_ReturnsFalse() {
        // Arrange
        String nonExistentEmail = "nonexistent@example.com";
        // Mock user doesn't exist in Microsoft Entra ID
        when(mockAccountService.getUser(nonExistentEmail)).thenReturn(null);

        // Act
        Boolean result = emailValidator.validate(nonExistentEmail, createEmptyContext());

        // Assert
        assertFalse("Non-existent email should fail validation", result);
        verify(mockAccountService).getUser(nonExistentEmail);
    }

    @Test
    public void testEmailValidator_WithInvalidFormat_ReturnsFalse() {
        // Arrange
        String invalidEmail = "not-an-email";

        // Act
        Boolean result = emailValidator.validate(invalidEmail, createEmptyContext());

        // Assert
        assertFalse("Invalid email format should fail validation", result);
        
        // Should not call service for invalid format
        verify(mockAccountService, never()).getUser(anyString());
    }

    @Test
    public void testEmailValidator_WithNullInput_ReturnsFalse() {
        // Act & Assert
        assertFalse("Null input should fail validation", 
                   emailValidator.validate(null, createEmptyContext()));
        assertFalse("Empty input should fail validation", 
                   emailValidator.validate("", createEmptyContext()));
        assertFalse("Whitespace input should fail validation", 
                   emailValidator.validate("   ", createEmptyContext()));
        
        // Should not call service for invalid inputs
        verify(mockAccountService, never()).getUser(anyString());
    }

    // ========== CREATE ACCOUNT EMAIL VALIDATOR TESTS ==========

    @Test
    public void testCreateAccountEmailValidator_WithNonExistentUser_ReturnsTrue() {
        // Arrange - For account creation, email should NOT exist
        String newEmail = "newuser@example.com";
        when(mockAccountService.getUser(newEmail)).thenReturn(null); // User doesn't exist (good for creation)

        // Act
        Boolean result = createAccountEmailValidator.validate(newEmail, createEmptyContext());

        // Assert
        assertTrue("Non-existent email should pass creation validation", result);
        verify(mockAccountService).getUser(newEmail);
    }

    @Test
    public void testCreateAccountEmailValidator_WithExistingUser_ReturnsFalse() {
        // Arrange - For account creation, existing email should fail
        String existingEmail = "existing@example.com";
        when(mockAccountService.getUser(existingEmail)).thenReturn(createMockAccount()); // User exists

        // Act
        Boolean result = createAccountEmailValidator.validate(existingEmail, createEmptyContext());

        // Assert
        assertFalse("Existing email should fail creation validation", result);
        verify(mockAccountService).getUser(existingEmail);
    }

    // ========== VALIDATION ORCHESTRATOR TESTS ==========

    @Test
    public void testValidationOrchestrator_RegisterAndValidate_WorksCorrectly() {
        // Arrange
        EmailValidator testValidator = new EmailValidator(mockAccountService);
        validationOrchestrator.registerValidator(Validator.ValidationResource.EMAIL, testValidator);
        
        // Mock user exists
        when(mockAccountService.getUser("test@example.com")).thenReturn(createMockAccount());
        
        Map<Validator.ValidationResource, String> resources = new HashMap<>();
        resources.put(Validator.ValidationResource.EMAIL, "test@example.com");

        // Act
        var results = validationOrchestrator.validate(resources);

        // Assert
        assertTrue("Validation should pass for existing user", results.isEmpty());
        verify(mockAccountService).getUser("test@example.com");
    }

    @Test
    public void testValidationOrchestrator_WithValidationFailure_ReturnsErrors() {
        // Arrange
        EmailValidator testValidator = new EmailValidator(mockAccountService);
        validationOrchestrator.registerValidator(Validator.ValidationResource.EMAIL, testValidator);
        
        // Mock user doesn't exist
        when(mockAccountService.getUser("nonexistent@example.com")).thenReturn(null);
        
        Map<Validator.ValidationResource, String> resources = new HashMap<>();
        resources.put(Validator.ValidationResource.EMAIL, "nonexistent@example.com");

        // Act
        var results = validationOrchestrator.validate(resources);

        // Assert
        assertFalse("Validation should fail for non-existent user", results.isEmpty());
        assertEquals("Should have one validation error", 1, results.size());
        
        // Verify the validation result structure
        var validationResult = results.get(0);
        assertNotNull("Validation result should not be null", validationResult);
        assertNotNull("Error message should not be null", validationResult.getErrMessage());
        
        verify(mockAccountService).getUser("nonexistent@example.com");
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    public void testEmailValidator_WithServiceException_HandlesGracefully() {
        // Arrange
        String testEmail = "test@example.com";
        when(mockAccountService.getUser(testEmail))
            .thenThrow(new RuntimeException("Microsoft Entra ID service unavailable"));

        // Act
        Boolean result = emailValidator.validate(testEmail, createEmptyContext());

        // Assert
        assertFalse("Should fail validation when service throws exception", result);
        verify(mockAccountService).getUser(testEmail);
    }

    @Test
    public void testCreateAccountEmailValidator_WithServiceException_HandlesGracefully() {
        // Arrange
        String testEmail = "newuser@example.com";
        when(mockAccountService.getUser(testEmail))
            .thenThrow(new RuntimeException("Connection timeout"));

        // Act
        Boolean result = createAccountEmailValidator.validate(testEmail, createEmptyContext());

        // Assert
        assertFalse("Should fail validation when service throws exception", result);
        verify(mockAccountService).getUser(testEmail);
    }

    // ========== VALIDATOR FIELD INFORMATION TESTS ==========

    @Test
    public void testEmailValidator_FieldInformation_IsCorrect() {
        // Act & Assert
        assertEquals("Should have correct field name", "Email", emailValidator.getFetchFieldName());
        assertEquals("Should have correct field type", "com.krista.fields.Text", emailValidator.getFieldType());
        assertNotNull("Should have fetch step message", emailValidator.getFetchStepMessage());
        assertNotNull("Should have confirm step message", 
                     emailValidator.getConfirmStepMessage("test@example.com", createEmptyContext()));
    }

    @Test
    public void testCreateAccountEmailValidator_FieldInformation_IsCorrect() {
        // Act & Assert
        assertEquals("Should have correct field name", "Email", createAccountEmailValidator.getFetchFieldName());
        assertEquals("Should have correct field type", "com.krista.fields.Text", createAccountEmailValidator.getFieldType());
        assertNotNull("Should have fetch step message", createAccountEmailValidator.getFetchStepMessage());
        assertNotNull("Should have confirm step message", 
                     createAccountEmailValidator.getConfirmStepMessage("test@example.com", createEmptyContext()));
    }

    // ========== INTEGRATION SCENARIO TESTS ==========

    @Test
    public void testValidationFlow_MultipleValidators_WorksTogether() {
        // Arrange - Register multiple validators
        EmailValidator emailVal = new EmailValidator(mockAccountService);
        CreateAccountEmailValidator createEmailVal = new CreateAccountEmailValidator(mockAccountService);
        
        validationOrchestrator.registerValidator(Validator.ValidationResource.EMAIL, emailVal);
        validationOrchestrator.registerValidator(Validator.ValidationResource.EMAIL_CREATE_ACCOUNT, createEmailVal);
        
        // Mock different behaviors for different operations
        when(mockAccountService.getUser("existing@example.com")).thenReturn(createMockAccount());
        when(mockAccountService.getUser("new@example.com")).thenReturn(null);
        
        // Test existing user validation (should pass for regular email validation)
        Map<Validator.ValidationResource, String> existingUserTest = new HashMap<>();
        existingUserTest.put(Validator.ValidationResource.EMAIL, "existing@example.com");
        
        var existingResults = validationOrchestrator.validate(existingUserTest);
        assertTrue("Existing user should pass regular email validation", existingResults.isEmpty());
        
        // Test new user validation (should pass for create account email validation)
        Map<Validator.ValidationResource, String> newUserTest = new HashMap<>();
        newUserTest.put(Validator.ValidationResource.EMAIL_CREATE_ACCOUNT, "new@example.com");
        
        var newResults = validationOrchestrator.validate(newUserTest);
        assertTrue("New user should pass create account email validation", newResults.isEmpty());
    }

    // ========== HELPER METHODS ==========

    private Map<Validator.ValidationResource, String> createEmptyContext() {
        return new HashMap<>();
    }

    private Account createMockAccount() {
        // Create a mock Account object for testing
        Account account = new Account();
        account.id = "test-user-id";
        account.name = "Test User";
        account.email = "test@example.com";
        account.mobileNumber = "+1234567890";
        return account;
    }
}
