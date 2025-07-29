package app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.validators;

import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.extresp.FieldTypes;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.impl.AccountImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validator for Email validation specifically for Update Account operations.
 * Validates that the provided email exists in Azure AD and meets requirements for account updates.
 */
public class UpdateAccountEmailValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAccountEmailValidator.class);

    // Pattern for comprehensive email validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // Pattern for local part validation (before @)
    private static final Pattern LOCAL_PART_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]{1,64}$"
    );

    // Pattern for domain part validation (after @)
    private static final Pattern DOMAIN_PART_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9.-]{1,253}$"
    );

    private final AccountImpl accountService;

    public UpdateAccountEmailValidator(AccountImpl accountService) {
        this.accountService = accountService;
    }

    @Override
    public Boolean validate(String resourceValue, Map<ValidationResource, String> context) {
        LOGGER.debug("UpdateAccountEmailValidator.validate() called with input: {}", resourceValue);

        // Email is required for update operations
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            LOGGER.warn("Email is null or empty for account update, which is required");
            return false;
        }

        String trimmedEmail = resourceValue.trim();

        // Basic format validation for account update
        if (!isValidEmailFormatForAccountUpdate(trimmedEmail)) {
            LOGGER.warn("Invalid email format for account update: {}", trimmedEmail);
            return false;
        }

        try {
            // Try to get user details to validate if user exists with this email
            Account user = accountService.getUser(trimmedEmail);

            if (user == null) {
                LOGGER.warn("User not found with email for account update: {}", trimmedEmail);
                return false;
            }

            // Additional validation specific to account update
            if (!isEligibleForAccountUpdate(user, trimmedEmail)) {
                LOGGER.warn("Email is not eligible for account update: {}", trimmedEmail);
                return false;
            }

            LOGGER.debug("Email validation successful for account update: {}", trimmedEmail);
            return true;

        } catch (Exception e) {
            LOGGER.error("Error validating email for account update '{}': {}", trimmedEmail, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        return AzureResources.EMAIL_UPDATE_ACCOUNT;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD_LOGICAL_NAME;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid email address that exists in Azure AD for account update.";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        return "Please enter a valid email address that exists in Azure AD for account update.";
    }

    @Override
    public String getErrMessage(String resourceValue) {
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            return "Email address is required for account update.";
        }

        String trimmedEmail = resourceValue.trim();

        if (!isValidEmailFormatForAccountUpdate(trimmedEmail)) {
            return "Invalid email format: '" + trimmedEmail + "'. Please enter a valid email address for account update.";
        }

        return "Email address '" + trimmedEmail + "' not found in Azure AD or not eligible for account update.";
    }

    /**
     * Checks if the given string is a valid email format for account update operations
     */
    private boolean isValidEmailFormatForAccountUpdate(String email) {
        if (email == null) {
            return false;
        }

        String trimmedEmail = email.trim();

        // Check overall length (RFC 5321 limit)
        if (trimmedEmail.length() < 6 || trimmedEmail.length() > 254) {
            LOGGER.warn("Email length out of bounds for account update: {} characters", trimmedEmail.length());
            return false;
        }

        // Basic pattern check
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            LOGGER.warn("Email does not match basic pattern for account update: {}", trimmedEmail);
            return false;
        }

        // Split into local and domain parts
        String[] parts = trimmedEmail.split("@");
        if (parts.length != 2) {
            LOGGER.warn("Email does not have exactly one @ symbol for account update: {}", trimmedEmail);
            return false;
        }

        String localPart = parts[0];
        String domainPart = parts[1];

        // Validate local part
        if (!isValidLocalPartForAccountUpdate(localPart)) {
            LOGGER.warn("Email local part is invalid for account update: {}", localPart);
            return false;
        }

        // Validate domain part
        if (!isValidDomainPartForAccountUpdate(domainPart)) {
            LOGGER.warn("Email domain part is invalid for account update: {}", domainPart);
            return false;
        }

        return true;
    }

    /**
     * Validates the local part (before @) of the email for account update
     */
    private boolean isValidLocalPartForAccountUpdate(String localPart) {
        if (localPart == null || localPart.isEmpty()) {
            return false;
        }

        // Check length
        if (localPart.length() > 64) {
            return false;
        }

        // Check pattern
        if (!LOCAL_PART_PATTERN.matcher(localPart).matches()) {
            return false;
        }

        // Cannot start or end with dot
        if (localPart.startsWith(".") || localPart.endsWith(".")) {
            return false;
        }

        // Cannot have consecutive dots
        if (localPart.contains("..")) {
            return false;
        }

        return true;
    }

    /**
     * Validates the domain part (after @) of the email for account update
     */
    private boolean isValidDomainPartForAccountUpdate(String domainPart) {
        if (domainPart == null || domainPart.isEmpty()) {
            return false;
        }

        // Check length
        if (domainPart.length() > 253) {
            return false;
        }

        // Check pattern
        if (!DOMAIN_PART_PATTERN.matcher(domainPart).matches()) {
            return false;
        }

        // Must contain at least one dot
        if (!domainPart.contains(".")) {
            return false;
        }

        // Cannot start or end with dot or hyphen
        if (domainPart.startsWith(".") || domainPart.endsWith(".") ||
                domainPart.startsWith("-") || domainPart.endsWith("-")) {
            return false;
        }

        // Cannot have consecutive dots
        if (domainPart.contains("..")) {
            return false;
        }

        // Check TLD (top-level domain) is valid
        String[] domainParts = domainPart.split("\\.");
        String tld = domainParts[domainParts.length - 1];
        if (tld.length() < 2 || !tld.matches("^[a-zA-Z]+$")) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the email is eligible for account update
     * This includes business logic specific to updating account emails
     */
    private boolean isEligibleForAccountUpdate(Account user, String email) {
        if (user == null || email == null || email.trim().isEmpty()) {
            return false;
        }

        // Add specific business logic for account update eligibility
        // For example: check if account is active, not a service account, etc.

        // Check if user has basic required fields
        if (user.id == null || user.id.trim().isEmpty()) {
            LOGGER.warn("User has invalid ID for account update: {}", email);
            return false;
        }

        // For now, we'll assume all existing users with valid IDs are eligible for account update
        // This can be extended based on business requirements such as:
        // - Check if account is not locked or disabled
        // - Check if user has appropriate permissions for updates
        // - Validate against company email domain policies
        // - Check if email is not already in use by another account

        return true;
    }
}
