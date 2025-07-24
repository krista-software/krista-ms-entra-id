package app.krista.extensions.cyber_security.iam.azureAD.catalog.validators;

import app.krista.extensions.cyber_security.iam.azureAD.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.extresp.FieldTypes;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.azureAD.impl.AccountImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Validator for User Mail or Id validation specifically for Remove Account From Group operations.
 * Validates that the provided user mail or ID exists in Azure AD and is eligible for group removal.
 */
public class RemoveAccountUserMailOrIdValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveAccountUserMailOrIdValidator.class);

    private final AccountImpl accountService;

    public RemoveAccountUserMailOrIdValidator(AccountImpl accountService) {
        this.accountService = accountService;
    }

    @Override
    public Boolean validate(String resourceValue, Map<ValidationResource, String> context) {
        LOGGER.debug("RemoveAccountUserMailOrIdValidator.validate() called with input: {}", resourceValue);

        // Check if input is null or empty
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            LOGGER.warn("User Mail or Id is null or empty for group removal");
            return false;
        }

        String trimmedInput = resourceValue.trim();

        // Basic format validation
        if (!isValidUserMailOrIdFormat(trimmedInput)) {
            LOGGER.warn("Invalid User Mail or Id format for group removal: {}", trimmedInput);
            return false;
        }

        try {
            // Try to get user details to validate if user exists
            Account user = accountService.getUser(trimmedInput);

            if (user == null) {
                LOGGER.warn("User not found with Mail or Id for group removal: {}", trimmedInput);
                return false;
            }

            // Additional validation specific to group removal
            if (!isEligibleForGroupRemoval(user)) {
                LOGGER.warn("User is not eligible for group removal: {}", trimmedInput);
                return false;
            }

            LOGGER.debug("User Mail or Id validation successful for group removal: {}", trimmedInput);
            return true;

        } catch (Exception e) {
            LOGGER.error("Error validating User Mail or Id for group removal '{}': {}", trimmedInput, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        return AzureResources.USER_MAIL_OR_ID_REMOVE_GROUP;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD_LOGICAL_NAME;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid user email address or user ID for group removal.";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        if (isEmail(resourceValue)) {
            return "Please enter a valid email address that exists in Azure AD for group removal.";
        } else {
            return "Please enter a valid user ID that exists in Azure AD for group removal.";
        }
    }

    @Override
    public String getErrMessage(String resourceValue) {
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            return "User Mail or Id is required for group removal.";
        }

        String trimmedInput = resourceValue.trim();

        if (!isValidUserMailOrIdFormat(trimmedInput)) {
            return "Invalid User Mail or Id format: '" + trimmedInput + "'. Please enter a valid email address or user ID.";
        }

        if (isEmail(trimmedInput)) {
            return "Email address '" + trimmedInput + "' not found in Azure AD or not eligible for group removal.";
        } else {
            return "User ID '" + trimmedInput + "' not found in Azure AD or not eligible for group removal.";
        }
    }

    /**
     * Checks if the given string is a valid user mail or ID format
     */
    private boolean isValidUserMailOrIdFormat(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        // Check if it's an email format
        if (isEmail(input)) {
            return isValidEmailFormat(input);
        }

        // Check if it's a valid user ID format
        return isValidUserIdFormat(input);
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
     * Validates user ID format
     */
    private boolean isValidUserIdFormat(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        // Check for GUID format (common in Azure AD)
        if (userId.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            return true;
        }

        // Allow other reasonable user ID formats
        return userId.matches("^[a-zA-Z0-9._-]+$") && userId.length() >= 3 && userId.length() <= 64;
    }

    /**
     * Checks if the user is eligible for group removal
     */
    private boolean isEligibleForGroupRemoval(Account user) {
        if (user == null) {
            return false;
        }

        // Add any specific business logic for group removal eligibility
        // For example: check if user is active, not a system account, etc.

        // For now, we'll assume all existing users are eligible for group removal
        // This can be extended based on business requirements
        return true;
    }
}
