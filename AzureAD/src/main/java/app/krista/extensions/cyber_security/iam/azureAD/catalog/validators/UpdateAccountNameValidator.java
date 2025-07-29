package app.krista.extensions.cyber_security.iam.azureAD.catalog.validators;

import app.krista.extensions.cyber_security.iam.azureAD.catalog.extresp.FieldTypes;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.azureAD.impl.AccountImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validator for Name validation specifically for Update Account operations.
 * Validates that the provided name meets Azure AD requirements for account updates.
 */
public class UpdateAccountNameValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAccountNameValidator.class);

    // Pattern for valid name characters (letters, spaces, hyphens, apostrophes, periods)
    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s\\-'.]+$");

    // Pattern to detect invalid consecutive characters
    private static final Pattern INVALID_CONSECUTIVE_PATTERN = Pattern.compile("(\\s{2,}|\\-{2,}|'{2,}|\\.{2,})");

    private final AccountImpl accountService;

    public UpdateAccountNameValidator(AccountImpl accountService) {
        this.accountService = accountService;
    }

    @Override
    public Boolean validate(String resourceValue, Map<ValidationResource, String> context) {
        LOGGER.debug("UpdateAccountNameValidator.validate() called with input: {}", resourceValue);

        // Name is optional for update operations, so null or empty is allowed
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            LOGGER.debug("Name is null or empty, which is allowed for update operations");
            return true;
        }

        String trimmedName = resourceValue.trim();

        // Basic format validation for account update
        if (!isValidNameFormatForAccountUpdate(trimmedName)) {
            LOGGER.warn("Invalid name format for account update: {}", trimmedName);
            return false;
        }

        // Additional validation specific to account update
        if (!isEligibleForAccountUpdate(trimmedName)) {
            LOGGER.warn("Name is not eligible for account update: {}", trimmedName);
            return false;
        }

        LOGGER.debug("Name validation successful for account update: {}", trimmedName);
        return true;
    }

    @Override
    public String getFetchFieldName() {
        return AzureResources.NAME_UPDATE_ACCOUNT;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD_LOGICAL_NAME;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid name for account update (letters, spaces, hyphens, apostrophes, and periods allowed).";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        return "Please enter a valid name that meets Azure AD requirements for account update.";
    }

    @Override
    public String getErrMessage(String resourceValue) {
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            // This shouldn't happen since empty names are allowed, but just in case
            return "Name can be empty for account update, but if provided, it must be valid.";
        }

        String trimmedName = resourceValue.trim();

        if (!isValidNameFormatForAccountUpdate(trimmedName)) {
            return "Invalid name format: '" + trimmedName + "'. Name must contain only letters, spaces, hyphens, apostrophes, and periods for account update.";
        }

        return "Name '" + trimmedName + "' is not suitable for account update. Please provide a valid display name.";
    }

    /**
     * Checks if the given string is a valid name format for account update operations
     */
    private boolean isValidNameFormatForAccountUpdate(String name) {
        if (name == null) {
            return false;
        }

        String trimmedName = name.trim();

        // Check minimum and maximum length for account update
        if (trimmedName.length() < 1 || trimmedName.length() > 64) {
            LOGGER.warn("Name length out of bounds for account update: {} characters", trimmedName.length());
            return false;
        }

        // Check for valid characters only
        if (!VALID_NAME_PATTERN.matcher(trimmedName).matches()) {
            LOGGER.warn("Name contains invalid characters for account update: {}", trimmedName);
            return false;
        }

        // Check for invalid consecutive characters
        if (INVALID_CONSECUTIVE_PATTERN.matcher(trimmedName).find()) {
            LOGGER.warn("Name contains invalid consecutive characters for account update: {}", trimmedName);
            return false;
        }

        // Cannot start or end with space, hyphen, apostrophe, or period
        if (trimmedName.startsWith(" ") || trimmedName.endsWith(" ") ||
                trimmedName.startsWith("-") || trimmedName.endsWith("-") ||
                trimmedName.startsWith("'") || trimmedName.endsWith("'") ||
                trimmedName.startsWith(".") || trimmedName.endsWith(".")) {
            LOGGER.warn("Name has invalid start/end characters for account update: {}", trimmedName);
            return false;
        }

        // Must contain at least one letter
        if (!trimmedName.matches(".*[a-zA-Z].*")) {
            LOGGER.warn("Name must contain at least one letter for account update: {}", trimmedName);
            return false;
        }

        return true;
    }

    /**
     * Checks if the name is eligible for account update
     * This includes business logic specific to updating account names
     */
    private boolean isEligibleForAccountUpdate(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        String trimmedName = name.trim();

        // Add specific business logic for account update eligibility
        // For example: check if name is not a reserved word, not offensive, etc.

        // Check for reserved words or patterns that shouldn't be used as display names
        String lowerName = trimmedName.toLowerCase();
        if (lowerName.equals("admin") || lowerName.equals("administrator") ||
                lowerName.equals("root") || lowerName.equals("system") ||
                lowerName.equals("service") || lowerName.equals("test") ||
                lowerName.equals("user") || lowerName.equals("guest")) {
            LOGGER.warn("Name contains reserved word for account update: {}", trimmedName);
            return false;
        }

        // Check for names that are too generic or might cause confusion
        if (lowerName.matches("^(user|account|profile)\\d*$")) {
            LOGGER.warn("Name is too generic for account update: {}", trimmedName);
            return false;
        }

        // For now, we'll assume all other valid names are eligible for account update
        // This can be extended based on business requirements such as:
        // - Check against a blacklist of inappropriate names
        // - Check for names that might conflict with system accounts
        // - Validate against company naming conventions

        return true;
    }
}
