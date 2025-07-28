package app.krista.extensions.cyber_security.iam.azureAD.catalog.validators;

import app.krista.extensions.cyber_security.iam.azureAD.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.extresp.FieldTypes;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.azureAD.impl.AccountImpl;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Validator for User Mail or ID validation for activate/deactivate account operations.
 * Validates that the provided user mail or ID exists in Azure AD.
 */
@Singleton
@Service
public class UserMailOrIdValidator extends BaseAzureADUserValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserMailOrIdValidator.class);

    @Inject
    public UserMailOrIdValidator(AccountImpl accountService) {
        super(AzureResources.USER_MAIL_OR_ID, FieldTypes.TEXT_FIELD_LOGICAL_NAME, accountService);
    }

    @Override
    protected ValidationResult performUserValidation(String trimmedValue, Map<ValidationResource, String> context) {
        // Check if user exists in Azure AD
        if (userExists(trimmedValue)) {
            return success();
        } else {
            return failure("User not found with Mail or ID: " + trimmedValue);
        }
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
    protected String getBusinessLogicErrorMessage(String value) {
        if (isEmail(value)) {
            return "Email '" + value + "' not found in Azure AD.";
        } else {
            return "User ID '" + value + "' not found in Azure AD.";
        }
    }
}
