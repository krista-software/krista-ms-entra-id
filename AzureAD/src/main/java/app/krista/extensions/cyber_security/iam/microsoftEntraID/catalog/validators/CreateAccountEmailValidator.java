package app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.validators;

import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.extresp.FieldTypes;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.impl.AccountImpl;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validator for Email field in Create Account operation.
 * Validates email format and checks if the email already exists in Azure AD.
 */
@Singleton
@Service
public class CreateAccountEmailValidator extends BaseValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateAccountEmailValidator.class);
    
    // Pattern for valid email format
    private static final Pattern VALID_EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private final AccountImpl accountService;

    @Inject
    public CreateAccountEmailValidator(AccountImpl accountService) {
        super(AzureResources.EMAIL_CREATE_ACCOUNT, FieldTypes.TEXT_FIELD_LOGICAL_NAME);
        this.accountService = accountService;
    }

    @Override
    public Boolean validate(String resourceValue, Map<ValidationResource, String> context) {
        LOGGER.debug("CreateAccountEmailValidator.validate() called with input: {}", resourceValue);

        // Check if input is null or empty
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            LOGGER.warn("Email is null or empty for account creation");
            return false;
        }

        String trimmedEmail = resourceValue.trim();

        // Basic format validation
        if (!isValidEmailFormat(trimmedEmail)) {
            LOGGER.warn("Invalid email format for account creation: {}", trimmedEmail);
            return false;
        }

        // Check if email already exists in Azure AD
        try {
            // Try to get user by email - if it exists, validation should fail for creation
            var existingUser = accountService.getUser(trimmedEmail);
            if (existingUser != null) {
                LOGGER.warn("Email already exists in Azure AD for account creation: {}", trimmedEmail);
                return false;
            }
        } catch (Exception e) {
            // If user doesn't exist (exception thrown), that's good for creation
            LOGGER.debug("Email doesn't exist in Azure AD (good for creation): {}", trimmedEmail);
        }

        LOGGER.debug("Email validation successful for account creation: {}", trimmedEmail);
        return true;
    }

    @Override
    public String getFetchFieldName() {
        return AzureResources.EMAIL_CREATE_ACCOUNT;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD_LOGICAL_NAME;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid email address for the new account that doesn't already exist in Azure AD.";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        if (isValidEmailFormat(resourceValue)) {
            return "The email address '" + resourceValue + "' already exists in Azure AD. Please enter a different email address.";
        } else {
            return "The provided email address '" + resourceValue + "' is not in valid format. Please enter a valid email address.";
        }
    }

    @Override
    public String getErrMessage(String resourceValue) {
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            return "Email is required for account creation.";
        }

        String trimmedEmail = resourceValue.trim();

        if (!isValidEmailFormat(trimmedEmail)) {
            return "Invalid email format: '" + trimmedEmail + "'. Please enter a valid email address (e.g., user@domain.com).";
        }

        // Check if it's a duplicate email issue
        try {
            var existingUser = accountService.getUser(trimmedEmail);
            if (existingUser != null) {
                return "Email address '" + trimmedEmail + "' already exists in Azure AD. Please use a different email address.";
            }
        } catch (Exception e) {
            // User doesn't exist, so it's just a format issue
            LOGGER.debug("Email validation error for non-existing user: {}", e.getMessage());
        }

        return "Email '" + trimmedEmail + "' is not valid for account creation.";
    }

    /**
     * Validates the email format.
     * 
     * @param email The email to validate
     * @return true if the email format is valid, false otherwise
     */
    private boolean isValidEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String trimmedEmail = email.trim();

        // Check basic pattern
        if (!VALID_EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            LOGGER.debug("Email pattern validation failed for: {}", trimmedEmail);
            return false;
        }

        // Additional checks
        if (trimmedEmail.length() > 254) { // RFC 5321 limit
            LOGGER.debug("Email too long: {} characters", trimmedEmail.length());
            return false;
        }

        // Check for consecutive dots
        if (trimmedEmail.contains("..")) {
            LOGGER.debug("Email contains consecutive dots: {}", trimmedEmail);
            return false;
        }

        // Check local part length (before @)
        String[] parts = trimmedEmail.split("@");
        if (parts.length == 2 && parts[0].length() > 64) { // RFC 5321 limit
            LOGGER.debug("Email local part too long: {} characters", parts[0].length());
            return false;
        }

        return true;
    }
}
