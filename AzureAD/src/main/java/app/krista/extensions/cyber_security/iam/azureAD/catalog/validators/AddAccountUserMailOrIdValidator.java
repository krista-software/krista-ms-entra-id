package app.krista.extensions.cyber_security.iam.azureAD.catalog.validators;

import app.krista.extensions.cyber_security.iam.azureAD.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.extresp.FieldTypes;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.azureAD.impl.AccountImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Validator for User Mail or Id validation specifically for Add Account To Group operations.
 * Validates that the provided user mail or ID exists in Azure AD and is eligible for group addition.
 */
public class AddAccountUserMailOrIdValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddAccountUserMailOrIdValidator.class);

    private final AccountImpl accountService;

    public AddAccountUserMailOrIdValidator(AccountImpl accountService) {
        this.accountService = accountService;
    }

    @Override
    public Boolean validate(String resourceValue, Map<ValidationResource, String> context) {
        LOGGER.debug("AddAccountUserMailOrIdValidator.validate() called with input: {}", resourceValue);

        // Check if input is null or empty
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            LOGGER.warn("User Mail or Id is null or empty for group addition");
            return false;
        }

        String trimmedInput = resourceValue.trim();

        // Basic format validation
        if (!isValidUserMailOrIdFormat(trimmedInput)) {
            LOGGER.warn("Invalid User Mail or Id format for group addition: {}", trimmedInput);
            return false;
        }

        try {
            // Try to get user details to validate if user exists
            Account user = accountService.getUser(trimmedInput);

            if (user == null) {
                LOGGER.warn("User not found with Mail or Id for group addition: {}", trimmedInput);
                return false;
            }

            // Additional validation specific to group addition
            if (!isEligibleForGroupAddition(user, trimmedInput)) {
                LOGGER.warn("User is not eligible for group addition: {}", trimmedInput);
                return false;
            }

            LOGGER.debug("User Mail or Id validation successful for group addition: {}", trimmedInput);
            return true;

        } catch (Exception e) {
            LOGGER.error("Error validating User Mail or Id for group addition '{}': {}", trimmedInput, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        return AzureResources.USER_MAIL_OR_ID_ADD_GROUP;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD_LOGICAL_NAME;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid user email address or user ID to add to the group.";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        if (isEmailAddress(resourceValue)) {
            return "Please enter a valid email address that exists in Azure AD to add to the group.";
        } else {
            return "Please enter a valid user ID that exists in Azure AD to add to the group.";
        }
    }

    @Override
    public String getErrMessage(String resourceValue) {
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            return "User Mail or Id is required to add to the group.";
        }

        String trimmedInput = resourceValue.trim();

        if (!isValidUserMailOrIdFormat(trimmedInput)) {
            return "Invalid User Mail or Id format: '" + trimmedInput + "'. Please enter a valid email address or user ID for group addition.";
        }

        if (isEmailAddress(trimmedInput)) {
            return "Email address '" + trimmedInput + "' not found in Azure AD or not eligible for group addition.";
        } else {
            return "User ID '" + trimmedInput + "' not found in Azure AD or not eligible for group addition.";
        }
    }

    /**
     * Checks if the given string is a valid user mail or ID format for group addition
     */
    private boolean isValidUserMailOrIdFormat(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        // Check if it's an email format
        if (isEmailAddress(input)) {
            return isValidEmailFormatForGroupAddition(input);
        }

        // Check if it's a valid user ID format for group addition
        return isValidUserIdFormatForGroupAddition(input);
    }

    /**
     * Checks if the given string is an email address
     */
    private boolean isEmailAddress(String input) {
        return input != null && input.contains("@") && input.contains(".");
    }

    /**
     * Validates email format specifically for group addition operations
     */
    private boolean isValidEmailFormatForGroupAddition(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        return email.contains("@") &&
                email.contains(".") &&
                email.indexOf("@") > 0 &&
                email.lastIndexOf(".") > email.indexOf("@") &&
                email.lastIndexOf(".") < email.length() - 1 &&
                !email.contains(" ") &&
                !email.contains("..") && // No consecutive dots
                email.length() >= 6 && // minimum reasonable email length for group operations
                email.length() <= 254 && // RFC 5321 limit
                !email.startsWith(".") && // Cannot start with dot
                !email.endsWith(".") && // Cannot end with dot
                !email.startsWith("@") && // Cannot start with @
                !email.endsWith("@"); // Cannot end with @
    }

    /**
     * Validates user ID format specifically for group addition operations
     */
    private boolean isValidUserIdFormatForGroupAddition(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        // Check for GUID format (most common in Azure AD for group operations)
        if (userId.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            return true;
        }

        // Allow other reasonable user ID formats for group addition
        // More restrictive than general user ID validation
        return userId.matches("^[a-zA-Z0-9._-]+$") &&
                userId.length() >= 3 &&
                userId.length() <= 64 &&
                !userId.startsWith(".") && // Cannot start with dot
                !userId.endsWith(".") && // Cannot end with dot
                !userId.startsWith("-") && // Cannot start with hyphen
                !userId.endsWith("-") && // Cannot end with hyphen
                !userId.contains("..") && // No consecutive dots
                !userId.contains("--"); // No consecutive hyphens
    }

    /**
     * Checks if the user is eligible for group addition
     * This includes business logic specific to adding users to groups
     */
    private boolean isEligibleForGroupAddition(Account user, String userInput) {
        if (user == null) {
            return false;
        }

        // Add specific business logic for group addition eligibility
        // For example: check if user is active, not already in too many groups, etc.

        // Check if user account has basic required fields for group operations
        if (user.email == null || user.email.trim().isEmpty()) {
            LOGGER.warn("User has invalid email for group addition: {}", userInput);
            return false;
        }

        if (user.id == null || user.id.trim().isEmpty()) {
            LOGGER.warn("User has invalid ID for group addition: {}", userInput);
            return false;
        }

        // For now, we'll assume all existing users with valid email and ID are eligible for group addition
        // This can be extended based on business requirements such as:
        // - Check if user is active (from extraFields)
        // - Check if user is not already in too many groups
        // - Check user principal name from extraFields if needed
        return true;
    }
}
