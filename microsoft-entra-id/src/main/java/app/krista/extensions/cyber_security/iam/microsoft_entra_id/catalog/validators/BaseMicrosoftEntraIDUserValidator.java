package app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.validators;

import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.service.Accounts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Base class for Azure AD user validators that provides common functionality
 * for null/empty checks, user lookup, and email validation patterns.
 * 
 * This class eliminates code duplication across all user-related validators
 * by providing template methods for common validation patterns.
 */
public abstract class BaseMicrosoftEntraIDUserValidator extends BaseValidator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseMicrosoftEntraIDUserValidator.class);
    
    // Common email pattern used across validators
    protected static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    protected final Accounts accountService;

    /**
     * Constructor for base Azure AD user validator.
     * 
     * @param fieldName The field name for this validator
     * @param fieldType The field type for this validator
     * @param accountService The account service for user lookups
     */
    protected BaseMicrosoftEntraIDUserValidator(String fieldName, String fieldType, Accounts accountService) {
        super(fieldName, fieldType);
        this.accountService = accountService;
    }

    /**
     * Template method for validation that handles common patterns.
     * Subclasses can override specific steps for customization.
     */
    @Override
    public final Boolean validate(String resourceValue, Map<ValidationResource, String> context) {
        String validatorName = this.getClass().getSimpleName();
        LOGGER.debug("{}.validate() called with input: {}", validatorName, resourceValue);

        // Step 1: Null/empty check (common pattern)
        if (!isValidInput(resourceValue)) {
            LOGGER.warn("{}: Input is null or empty", validatorName);
            return false;
        }

        String trimmedValue = resourceValue.trim();

        // Step 2: Format validation (can be overridden by subclasses)
        if (!isValidFormat(trimmedValue)) {
            LOGGER.warn("{}: Invalid format for input: {}", validatorName, trimmedValue);
            return false;
        }

        // Step 3: User lookup and business logic validation
        try {
            ValidationResult result = performUserValidation(trimmedValue, context);
            
            if (result.isValid()) {
                LOGGER.debug("{}: Validation successful for: {}", validatorName, trimmedValue);
                return true;
            } else {
                LOGGER.warn("{}: Validation failed for: {} - {}", validatorName, trimmedValue, result.getFailureReason());
                return false;
            }

        } catch (Exception e) {
            LOGGER.error("{}: Error validating '{}': {}", validatorName, resourceValue, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Common null/empty check pattern.
     * 
     * @param resourceValue The value to check
     * @return true if the value is valid (not null or empty), false otherwise
     */
    protected final boolean isValidInput(String resourceValue) {
        return resourceValue != null && !resourceValue.trim().isEmpty();
    }

    /**
     * Format validation hook method. Subclasses can override for specific format checks.
     * Default implementation returns true (no format validation).
     * 
     * @param trimmedValue The trimmed input value
     * @return true if format is valid, false otherwise
     */
    protected boolean isValidFormat(String trimmedValue) {
        return true; // Default: no format validation
    }

    /**
     * Abstract method for performing user validation logic.
     * Subclasses must implement this to define their specific validation rules.
     * 
     * @param trimmedValue The trimmed and format-validated input
     * @param context The validation context
     * @return ValidationResult indicating success or failure with reason
     */
    protected abstract ValidationResult performUserValidation(String trimmedValue, Map<ValidationResource, String> context);

    /**
     * Common user lookup pattern with null check.
     * 
     * @param userIdentifier The user identifier (email or ID)
     * @return Account object if found, null otherwise
     */
    protected final Account lookupUser(String userIdentifier) {
        try {
            return accountService.getUser(userIdentifier);
        } catch (Exception e) {
            LOGGER.debug("User lookup failed for '{}': {}", userIdentifier, e.getMessage());
            return null;
        }
    }

    /**
     * Common email format validation pattern.
     * 
     * @param value The value to check
     * @return true if the value matches email pattern, false otherwise
     */
    protected final boolean isEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }

    /**
     * Utility method to check if user exists (common pattern).
     * 
     * @param userIdentifier The user identifier to check
     * @return true if user exists, false otherwise
     */
    protected final boolean userExists(String userIdentifier) {
        Account user = lookupUser(userIdentifier);
        return user != null;
    }

    /**
     * Utility method to check if user does NOT exist (useful for creation validators).
     * 
     * @param userIdentifier The user identifier to check
     * @return true if user does NOT exist, false if user exists
     */
    protected final boolean userDoesNotExist(String userIdentifier) {
        return !userExists(userIdentifier);
    }

    /**
     * Helper method to create a successful validation result.
     * 
     * @return ValidationResult indicating success
     */
    protected final ValidationResult success() {
        return new ValidationResult(true, null);
    }

    /**
     * Helper method to create a failed validation result.
     * 
     * @param reason The reason for failure
     * @return ValidationResult indicating failure with reason
     */
    protected final ValidationResult failure(String reason) {
        return new ValidationResult(false, reason);
    }

    /**
     * Result class for validation operations.
     */
    protected static class ValidationResult {
        private final boolean valid;
        private final String failureReason;

        public ValidationResult(boolean valid, String failureReason) {
            this.valid = valid;
            this.failureReason = failureReason;
        }

        public boolean isValid() {
            return valid;
        }

        public String getFailureReason() {
            return failureReason;
        }
    }

    /**
     * Enhanced error message generation with null/empty check pattern.
     */
    @Override
    public String getErrMessage(String resourceValue) {
        if (!isValidInput(resourceValue)) {
            return getEmptyInputErrorMessage();
        }

        String trimmedValue = resourceValue.trim();
        
        if (!isValidFormat(trimmedValue)) {
            return getFormatErrorMessage(trimmedValue);
        }

        return getBusinessLogicErrorMessage(trimmedValue);
    }

    /**
     * Hook method for empty input error message.
     * Subclasses can override for custom messages.
     */
    protected String getEmptyInputErrorMessage() {
        return fieldName + " is required.";
    }

    /**
     * Hook method for format error message.
     * Subclasses can override for custom messages.
     */
    protected String getFormatErrorMessage(String value) {
        return "Invalid " + fieldName + " format: '" + value + "'.";
    }

    /**
     * Abstract method for business logic error message.
     * Subclasses must implement for specific error messages.
     */
    protected abstract String getBusinessLogicErrorMessage(String value);
}
