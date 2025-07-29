package app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.validators;

import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.extresp.FieldTypes;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.impl.AccountImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validator for Mobile Number validation specifically for Update Account operations.
 * Validates that the provided mobile number meets Azure AD requirements for account updates.
 */
public class UpdateAccountMobileNumberValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAccountMobileNumberValidator.class);

    // Pattern for international mobile number format (E.164 format)
    private static final Pattern INTERNATIONAL_MOBILE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    // Pattern for US/Canada mobile number format
    private static final Pattern US_CANADA_MOBILE_PATTERN = Pattern.compile("^\\+1[2-9]\\d{2}[2-9]\\d{2}\\d{4}$");

    // Pattern for basic mobile number validation (digits, spaces, hyphens, parentheses, plus)
    private static final Pattern BASIC_MOBILE_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-()]+$");

    private final AccountImpl accountService;

    public UpdateAccountMobileNumberValidator(AccountImpl accountService) {
        this.accountService = accountService;
    }

    @Override
    public Boolean validate(String resourceValue, Map<ValidationResource, String> context) {
        LOGGER.debug("UpdateAccountMobileNumberValidator.validate() called with input: {}", resourceValue);

        // Mobile number is optional for update operations, so null or empty is allowed
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            LOGGER.debug("Mobile number is null or empty, which is allowed for update operations");
            return true;
        }

        String trimmedMobileNumber = resourceValue.trim();

        // Basic format validation for account update
        if (!isValidMobileNumberFormatForAccountUpdate(trimmedMobileNumber)) {
            LOGGER.warn("Invalid mobile number format for account update: {}", trimmedMobileNumber);
            return false;
        }

        // Additional validation specific to account update
        if (!isEligibleForAccountUpdate(trimmedMobileNumber)) {
            LOGGER.warn("Mobile number is not eligible for account update: {}", trimmedMobileNumber);
            return false;
        }

        LOGGER.debug("Mobile number validation successful for account update: {}", trimmedMobileNumber);
        return true;
    }

    @Override
    public String getFetchFieldName() {
        return AzureResources.MOBILE_NUMBER_UPDATE_ACCOUNT;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD_LOGICAL_NAME;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid mobile number for account update (international format preferred, e.g., +1234567890).";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        return "Please enter a valid mobile number that meets Azure AD requirements for account update.";
    }

    @Override
    public String getErrMessage(String resourceValue) {
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            // This shouldn't happen since empty mobile numbers are allowed, but just in case
            return "Mobile number can be empty for account update, but if provided, it must be valid.";
        }

        String trimmedMobileNumber = resourceValue.trim();

        if (!isValidMobileNumberFormatForAccountUpdate(trimmedMobileNumber)) {
            return "Invalid mobile number format: '" + trimmedMobileNumber + "'. Please use international format (e.g., +1234567890) for account update.";
        }

        return "Mobile number '" + trimmedMobileNumber + "' is not suitable for account update. Please provide a valid mobile number.";
    }

    /**
     * Checks if the given string is a valid mobile number format for account update operations
     */
    private boolean isValidMobileNumberFormatForAccountUpdate(String mobileNumber) {
        if (mobileNumber == null) {
            return false;
        }

        String trimmedMobileNumber = mobileNumber.trim();

        // Check minimum and maximum length for account update
        if (trimmedMobileNumber.length() < 7 || trimmedMobileNumber.length() > 20) {
            LOGGER.warn("Mobile number length out of bounds for account update: {} characters", trimmedMobileNumber.length());
            return false;
        }

        // First check if it contains only valid characters
        if (!BASIC_MOBILE_PATTERN.matcher(trimmedMobileNumber).matches()) {
            LOGGER.warn("Mobile number contains invalid characters for account update: {}", trimmedMobileNumber);
            return false;
        }

        // Normalize the mobile number by removing spaces, hyphens, and parentheses
        String normalizedNumber = trimmedMobileNumber.replaceAll("[\\s\\-()]", "");

        // Check if it's a valid international format
        if (INTERNATIONAL_MOBILE_PATTERN.matcher(normalizedNumber).matches()) {
            LOGGER.debug("Mobile number matches international format for account update: {}", normalizedNumber);
            return true;
        }

        // Check if it's a valid US/Canada format
        if (US_CANADA_MOBILE_PATTERN.matcher(normalizedNumber).matches()) {
            LOGGER.debug("Mobile number matches US/Canada format for account update: {}", normalizedNumber);
            return true;
        }

        // Check for other common valid formats
        if (isValidAlternativeFormat(normalizedNumber)) {
            LOGGER.debug("Mobile number matches alternative valid format for account update: {}", normalizedNumber);
            return true;
        }

        LOGGER.warn("Mobile number does not match any valid format for account update: {}", normalizedNumber);
        return false;
    }

    /**
     * Checks for alternative valid mobile number formats
     */
    private boolean isValidAlternativeFormat(String normalizedNumber) {
        // Check for numbers without country code but with valid length
        if (normalizedNumber.matches("^[1-9]\\d{6,14}$")) {
            return true;
        }

        // Check for numbers starting with 00 (alternative international prefix)
        if (normalizedNumber.matches("^00[1-9]\\d{7,14}$")) {
            return true;
        }

        return false;
    }

    /**
     * Checks if the mobile number is eligible for account update
     * This includes business logic specific to updating account mobile numbers
     */
    private boolean isEligibleForAccountUpdate(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            return false;
        }

        String trimmedMobileNumber = mobileNumber.trim();
        String normalizedNumber = trimmedMobileNumber.replaceAll("[\\s\\-()]", "");

        // Add specific business logic for account update eligibility
        // For example: check if number is not blacklisted, not a premium number, etc.

        // Check for obviously invalid patterns
        if (normalizedNumber.matches("^(\\+?1?0{7,}|\\+?1?1{7,}|\\+?1?2{7,})$")) {
            LOGGER.warn("Mobile number has invalid repetitive pattern for account update: {}", normalizedNumber);
            return false;
        }

        // Check for test numbers or reserved ranges
        if (normalizedNumber.matches("^\\+?1?(555|800|888|877|866|855|844|833|822)\\d{7}$")) {
            LOGGER.warn("Mobile number appears to be a test or toll-free number for account update: {}", normalizedNumber);
            return false;
        }

        // Check for emergency numbers
        if (normalizedNumber.matches("^(\\+?1?911|\\+?44?999|\\+?33?15|\\+?49?110|\\+?81?110)$")) {
            LOGGER.warn("Mobile number appears to be an emergency number for account update: {}", normalizedNumber);
            return false;
        }

        // For now, we'll assume all other valid mobile numbers are eligible for account update
        // This can be extended based on business requirements such as:
        // - Check against a blacklist of known spam numbers
        // - Validate against company mobile number policies
        // - Check for geographic restrictions

        return true;
    }
}
