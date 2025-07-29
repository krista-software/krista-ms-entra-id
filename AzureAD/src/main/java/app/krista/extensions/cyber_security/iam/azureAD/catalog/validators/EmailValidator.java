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
 * Validator for Email validation for delete account operations.
 * Validates that the provided email exists in Azure AD.
 */
@Singleton
@Service
public class EmailValidator extends BaseAzureADUserValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailValidator.class);

    @Inject
    public EmailValidator(AccountImpl accountService) {
        super(AzureResources.EMAIL, FieldTypes.TEXT_FIELD_LOGICAL_NAME, accountService);
    }

    @Override
    protected boolean isValidFormat(String trimmedValue) {
        // Email format validation using the base class EMAIL_PATTERN
        return isEmail(trimmedValue);
    }

    @Override
    protected ValidationResult performUserValidation(String trimmedValue, Map<ValidationResource, String> context) {
        // Check if user exists in Azure AD
        if (userExists(trimmedValue)) {
            return success();
        } else {
            return failure("User not found with email: " + trimmedValue);
        }
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
    protected String getFormatErrorMessage(String value) {
        return "Invalid email format: '" + value + "'. Please enter a valid email address.";
    }

    @Override
    protected String getBusinessLogicErrorMessage(String value) {
        return "Email '" + value + "' not found in Azure AD.";
    }
}
