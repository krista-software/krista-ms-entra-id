package app.krista.extensions.cyber_security.iam.azureAD.catalog.validators;

import app.krista.extensions.cyber_security.iam.azureAD.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.extresp.FieldTypes;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.azureAD.impl.AccountImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Validator for Email validation for delete account operations.
 * Validates that the provided email exists in Azure AD.
 */
public class EmailValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailValidator.class);

    private final AccountImpl accountService;

    public EmailValidator(AccountImpl accountService) {
        this.accountService = accountService;
    }

    @Override
    public Boolean validate(String resourceValue, Map<ValidationResource, String> context) {
        LOGGER.debug("EmailValidator.validate() called with input: {}", resourceValue);

        // Check if input is null or empty
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            LOGGER.warn("Email is null or empty");
            return false;
        }

        // Basic email format validation
        if (!isValidEmailFormat(resourceValue.trim())) {
            LOGGER.warn("Invalid email format: {}", resourceValue);
            return false;
        }

        try {
            // Try to get user details to validate if user exists
            Account user = accountService.getUser(resourceValue.trim());

            if (user == null) {
                LOGGER.warn("User not found with email: {}", resourceValue);
                return false;
            }

            LOGGER.debug("Email validation successful for: {}", resourceValue);
            return true;

        } catch (Exception e) {
            LOGGER.error("Error validating email '{}': {}", resourceValue, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        return AzureResources.EMAIL;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD_LOGICAL_NAME;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid email address that exists in Azure AD.";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        return "Please enter a valid email address that exists in Azure AD.";
    }

    @Override
    public String getErrMessage(String resourceValue) {
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            return "Email address is required.";
        }
        if (!isValidEmailFormat(resourceValue.trim())) {
            return "Invalid email format: '" + resourceValue + "'. Please enter a valid email address.";
        }
        return "Email '" + resourceValue + "' not found in Azure AD.";
    }

    /**
     * Checks if the given string is a valid email format
     */
    private boolean isValidEmailFormat(String email) {
        return email != null &&
                email.contains("@") &&
                email.contains(".") &&
                email.indexOf("@") > 0 &&
                email.lastIndexOf(".") > email.indexOf("@") &&
                email.lastIndexOf(".") < email.length() - 1;
    }
}
