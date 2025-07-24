package app.krista.extensions.cyber_security.iam.azureAD.catalog.validators;

import app.krista.extensions.cyber_security.iam.azureAD.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.extresp.FieldTypes;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.azureAD.impl.AccountImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Validator for User Mail or ID validation for activate/deactivate account operations.
 * Validates that the provided user mail or ID exists in Azure AD.
 */
public class UserMailOrIdValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserMailOrIdValidator.class);

    private final AccountImpl accountService;

    public UserMailOrIdValidator(AccountImpl accountService) {
        this.accountService = accountService;
    }

    @Override
    public Boolean validate(String resourceValue, Map<ValidationResource, String> context) {
        LOGGER.debug("UserMailOrIdValidator.validate() called with input: {}", resourceValue);

        // Check if input is null or empty
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            LOGGER.warn("User Mail or ID is null or empty");
            return false;
        }

        try {
            // Try to get user details to validate if user exists
            Account user = accountService.getUser(resourceValue.trim());

            if (user == null) {
                LOGGER.warn("User not found with Mail or ID: {}", resourceValue);
                return false;
            }

            LOGGER.debug("User Mail or ID validation successful for: {}", resourceValue);
            return true;

        } catch (Exception e) {
            LOGGER.error("Error validating user Mail or ID '{}': {}", resourceValue, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        return AzureResources.USER_MAIL_OR_ID;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD_LOGICAL_NAME;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid user email address or user ID that exists in Azure AD.";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        if (isEmail(resourceValue)) {
            return "Please enter a valid email address that exists in Azure AD.";
        } else {
            return "Please enter a valid user ID that exists in Azure AD.";
        }
    }

    @Override
    public String getErrMessage(String resourceValue) {
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            return "User Mail or ID is required.";
        }
        if (isEmail(resourceValue)) {
            return "Email '" + resourceValue + "' not found in Azure AD.";
        } else {
            return "User ID '" + resourceValue + "' not found in Azure AD.";
        }
    }

    /**
     * Checks if the given string is an email address
     */
    private boolean isEmail(String input) {
        return input != null && input.contains("@") && input.contains(".");
    }
}
