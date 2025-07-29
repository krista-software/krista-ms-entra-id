package app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.validators;

import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.extresp.FieldTypes;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.service.Accounts;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Validator for ID field in Get User Details operation.
 * Validates that the ID/Email exists in Azure AD and is accessible.
 */
@Singleton
@Service
public class GetUserDetailsIdValidator extends BaseAzureADUserValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetUserDetailsIdValidator.class);

    @Inject
    public GetUserDetailsIdValidator(Accounts accountService) {
        super(AzureResources.ID, FieldTypes.TEXT_FIELD_LOGICAL_NAME, accountService);
    }

    @Override
    protected boolean isValidFormat(String trimmedValue) {
        // Use base class email validation or basic user ID validation
        return isEmail(trimmedValue) || isValidUserId(trimmedValue);
    }

    @Override
    protected ValidationResult performUserValidation(String trimmedValue, Map<ValidationResource, String> context) {
        // Check if user exists in Azure AD
        if (userExists(trimmedValue)) {
            return success();
        } else {
            return failure("User with ID/Email '" + trimmedValue + "' does not exist in Azure AD");
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
    protected String getFormatErrorMessage(String value) {
        return "Invalid ID format: '" + value + "'. Please enter a valid email address or user ID.";
    }

    @Override
    protected String getBusinessLogicErrorMessage(String value) {
        if (isEmail(value)) {
            return "Email '" + value + "' not found in Azure AD.";
        } else {
            return "User ID '" + value + "' not found in Azure AD.";
        }
    }

    /**
     * Validates user ID format (could be GUID or other formats)
     */
    private boolean isValidUserId(String userId) {
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
}
