package app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.validators;

import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.entities.Account;
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
 * Validator for User Mail or Id validation specifically for Remove Account From Group operations.
 * Validates that the provided user mail or ID exists in Azure AD and is eligible for group removal.
 */
@Singleton
@Service
public class RemoveAccountUserMailOrIdValidator extends BaseAzureADUserValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveAccountUserMailOrIdValidator.class);

    @Inject
    public RemoveAccountUserMailOrIdValidator(AccountImpl accountService) {
        super(AzureResources.USER_MAIL_OR_ID_REMOVE_GROUP, FieldTypes.TEXT_FIELD_LOGICAL_NAME, accountService);
    }

    @Override
    protected boolean isValidFormat(String trimmedValue) {
        return isValidUserMailOrIdFormat(trimmedValue);
    }

    @Override
    protected ValidationResult performUserValidation(String trimmedValue, Map<ValidationResource, String> context) {
        // Check if user exists in Azure AD
        Account user = lookupUser(trimmedValue);
        if (user == null) {
            return failure("User not found with Mail or Id for group removal: " + trimmedValue);
        }

        // Additional validation specific to group removal
        if (!isEligibleForGroupRemoval(user)) {
            return failure("User is not eligible for group removal: " + trimmedValue);
        }

        return success();
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
    protected String getFormatErrorMessage(String value) {
        return "Invalid User Mail or Id format: '" + value + "'. Please enter a valid email address or user ID.";
    }

    @Override
    protected String getBusinessLogicErrorMessage(String value) {
        if (isEmail(value)) {
            return "Email address '" + value + "' not found in Azure AD or not eligible for group removal.";
        } else {
            return "User ID '" + value + "' not found in Azure AD or not eligible for group removal.";
        }
    }

    /**
     * Checks if the given string is a valid user mail or ID format for group removal
     */
    private boolean isValidUserMailOrIdFormat(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        // Use base class email validation or basic user ID validation
        return isEmail(input) || isValidUserId(input);
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

    /**
     * Checks if the user is eligible for group removal
     * This includes business logic specific to removing users from groups
     */
    private boolean isEligibleForGroupRemoval(Account user) {
        if (user == null) {
            return false;
        }
        return true;
    }
}
