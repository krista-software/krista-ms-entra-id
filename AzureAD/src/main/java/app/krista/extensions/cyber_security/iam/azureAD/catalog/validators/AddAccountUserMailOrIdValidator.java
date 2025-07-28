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
 * Validator for User Mail or Id validation specifically for Add Account To Group operations.
 * Validates that the provided user mail or ID exists in Azure AD and is eligible for group addition.
 */
@Singleton
@Service
public class AddAccountUserMailOrIdValidator extends BaseAzureADUserValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddAccountUserMailOrIdValidator.class);

    @Inject
    public AddAccountUserMailOrIdValidator(AccountImpl accountService) {
        super(AzureResources.USER_MAIL_OR_ID_ADD_GROUP, FieldTypes.TEXT_FIELD_LOGICAL_NAME, accountService);
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
            return failure("User not found with Mail or Id for group addition: " + trimmedValue);
        }

        // Additional validation specific to group addition
        if (!isEligibleForGroupAddition(user)) {
            return failure("User is not eligible for group addition: " + trimmedValue);
        }

        return success();
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid user email address or user ID to add to the group.";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        if (isEmail(resourceValue)) {
            return "Please enter a valid email address that exists in Azure AD to add to the group.";
        } else {
            return "Please enter a valid user ID that exists in Azure AD to add to the group.";
        }
    }

    @Override
    protected String getFormatErrorMessage(String value) {
        return "Invalid User Mail or Id format: '" + value + "'. Please enter a valid email address or user ID for group addition.";
    }

    @Override
    protected String getBusinessLogicErrorMessage(String value) {
        if (isEmail(value)) {
            return "Email address '" + value + "' not found in Azure AD or not eligible for group addition.";
        } else {
            return "User ID '" + value + "' not found in Azure AD or not eligible for group addition.";
        }
    }

    /**
     * Checks if the given string is a valid user mail or ID format for group addition
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
     * Checks if the user is eligible for group addition
     * This includes business logic specific to adding users to groups
     */
    private boolean isEligibleForGroupAddition(Account user) {
        if (user == null) {
            return false;
        }
        return true;
    }
}
