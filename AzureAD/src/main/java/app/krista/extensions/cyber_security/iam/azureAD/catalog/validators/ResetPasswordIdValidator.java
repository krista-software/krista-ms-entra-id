package app.krista.extensions.cyber_security.iam.azureAD.catalog.validators;

import app.krista.extensions.cyber_security.iam.azureAD.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.extresp.FieldTypes;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.azureAD.impl.AccountImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Validator for ID validation specifically for Reset Password operations.
 * Validates that the provided ID (email or user ID) exists in Azure AD and is eligible for password reset.
 */
public class ResetPasswordIdValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResetPasswordIdValidator.class);

    private final AccountImpl accountService;

    public ResetPasswordIdValidator(AccountImpl accountService) {
        this.accountService = accountService;
    }

    @Override
    public Boolean validate(String resourceValue, Map<ValidationResource, String> context) {
        LOGGER.debug("ResetPasswordIdValidator.validate() called with input: {}", resourceValue);

        // Check if input is null or empty
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            LOGGER.warn("ID is null or empty for password reset");
            return false;
        }

        String trimmedId = resourceValue.trim();

        // Basic format validation
        if (!isValidIdFormat(trimmedId)) {
            LOGGER.warn("Invalid ID format for password reset: {}", trimmedId);
            return false;
        }

        try {
            // Try to get user details to validate if user exists and is eligible for password reset
            Account user = accountService.getUser(trimmedId);

            if (user == null) {
                LOGGER.warn("User not found with ID for password reset: {}", trimmedId);
                return false;
            }

            // Additional validation specific to password reset
            if (!isEligibleForPasswordReset(user)) {
                LOGGER.warn("User is not eligible for password reset: {}", trimmedId);
                return false;
            }

            LOGGER.debug("ID validation successful for password reset: {}", trimmedId);
            return true;

        } catch (Exception e) {
            LOGGER.error("Error validating ID for password reset '{}': {}", trimmedId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        return AzureResources.ID;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD_LOGICAL_NAME;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid user ID or email address for password reset.";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        if (isEmail(resourceValue)) {
            return "Please enter a valid email address that exists in Azure AD for password reset.";
        } else {
            return "Please enter a valid user ID that exists in Azure AD for password reset.";
        }
    }

    @Override
    public String getErrMessage(String resourceValue) {
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            return "User ID or email address is required for password reset.";
        }

        String trimmedId = resourceValue.trim();

        if (!isValidIdFormat(trimmedId)) {
            return "Invalid ID format: '" + trimmedId + "'. Please enter a valid email address or user ID.";
        }

        if (isEmail(trimmedId)) {
            return "Email address '" + trimmedId + "' not found in Azure AD or not eligible for password reset.";
        } else {
            return "User ID '" + trimmedId + "' not found in Azure AD or not eligible for password reset.";
        }
    }

    /**
     * Checks if the given string is a valid ID format (email or user ID)
     */
    private boolean isValidIdFormat(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        // Check if it's an email format
        if (isEmail(id)) {
            return isValidEmailFormat(id);
        }

        // Check if it's a valid user ID format (GUID or other valid formats)
        return isValidUserIdFormat(id);
    }

    /**
     * Checks if the given string is an email address
     */
    private boolean isEmail(String input) {
        return input != null && input.contains("@") && input.contains(".");
    }

    /**
     * Validates email format
     */
    private boolean isValidEmailFormat(String email) {
        return email != null &&
                email.contains("@") &&
                email.contains(".") &&
                email.indexOf("@") > 0 &&
                email.lastIndexOf(".") > email.indexOf("@") &&
                email.lastIndexOf(".") < email.length() - 1 &&
                !email.contains(" ") &&
                email.length() >= 5; // minimum reasonable email length
    }

    /**
     * Validates user ID format (could be GUID or other formats)
     */
    private boolean isValidUserIdFormat(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        // Check for GUID format (common in Azure AD)
        if (userId.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            return true;
        }

        // Allow other reasonable user ID formats (alphanumeric, dots, hyphens, underscores)
        return userId.matches("^[a-zA-Z0-9._-]+$") && userId.length() >= 3 && userId.length() <= 64;
    }

    /**
     * Checks if the user is eligible for password reset
     * This can include additional business logic specific to password reset operations
     */
    private boolean isEligibleForPasswordReset(Account user) {
        if (user == null) {
            return false;
        }

        // Add any specific business logic for password reset eligibility
        // For example: check if user is active, not a service account, etc.

        // For now, we'll assume all existing users are eligible for password reset
        // This can be extended based on business requirements
        return true;
    }
}
