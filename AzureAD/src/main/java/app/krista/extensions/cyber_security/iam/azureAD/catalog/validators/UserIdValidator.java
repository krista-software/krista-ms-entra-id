package app.krista.extensions.cyber_security.iam.azureAD.catalog.validators;

import app.krista.extensions.cyber_security.iam.azureAD.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.extresp.FieldTypes;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.azureAD.impl.AccountImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Validator for User ID or Email validation.
 * Validates that the provided user ID or email exists in Azure AD.
 */
public class UserIdValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserIdValidator.class);

    private final AccountImpl accountService;

    public UserIdValidator(AccountImpl accountService) {
        this.accountService = accountService;
    }

    @Override
    public Boolean validate(String resourceValue, Map<ValidationResource, String> context) {
        LOGGER.debug("UserIdValidator.validate() called with input: {}", resourceValue);

        // Check if input is null or empty
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            LOGGER.warn("User ID/Email is null or empty");
            return false;
        }

        try {
            // Try to get user details to validate if user exists
            Account user = accountService.getUser(resourceValue.trim());

            if (user == null) {
                LOGGER.warn("User not found with ID/Email: {}", resourceValue);
                return false;
            }

            LOGGER.debug("User validation successful for: {}", resourceValue);
            return true;

        } catch (Exception e) {
            LOGGER.error("Error validating user ID/Email '{}': {}", resourceValue, e.getMessage(), e);
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
        return "Please enter a valid user ID or email address that exists in Azure AD.";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        if (isEmail(resourceValue)) {
            return "Please enter a valid Email that exists in Azure AD.";
        } else {
            return "Please enter a valid user ID that exists in Azure AD.";
        }
    }

    @Override
    public String getErrMessage(String resourceValue) {
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
