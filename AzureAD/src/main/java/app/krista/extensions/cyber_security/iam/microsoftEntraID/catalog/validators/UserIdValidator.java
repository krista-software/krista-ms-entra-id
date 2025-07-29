package app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.validators;

import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.extresp.FieldTypes;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.impl.AccountImpl;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Validator for User ID or Email validation.
 * Validates that the provided user ID or email exists in Azure AD.
 */
@Singleton
@Service
public class UserIdValidator extends BaseAzureADUserValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserIdValidator.class);

    @Inject
    public UserIdValidator(AccountImpl accountService) {
        super(AzureResources.ID, FieldTypes.TEXT_FIELD_LOGICAL_NAME, accountService);
    }

    @Override
    protected ValidationResult performUserValidation(String trimmedValue, Map<ValidationResource, String> context) {
        // Check if user exists in Azure AD
        if (userExists(trimmedValue)) {
            return success();
        } else {
            return failure("User not found with ID/Email: " + trimmedValue);
        }
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
    protected String getBusinessLogicErrorMessage(String value) {
        if (isEmail(value)) {
            return "Email '" + value + "' not found in Azure AD.";
        } else {
            return "User ID '" + value + "' not found in Azure AD.";
        }
    }
}
