package app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.validators;

import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.extresp.FieldTypes;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validator for Mobile Number field in Create Account operation.
 * Validates that the mobile number follows international format conventions.
 */
@Singleton
@Service
public class CreateAccountMobileNumberValidator extends BaseValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateAccountMobileNumberValidator.class);
    
    // Pattern for valid mobile number - supports international formats
    // Allows +country code followed by digits, spaces, hyphens, and parentheses
    private static final Pattern VALID_MOBILE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$|^\\+?[1-9]\\d{0,3}[\\s\\-\\(\\)]*\\d{4,14}$");
    
    // Simple pattern for basic validation - at least 10 digits
    private static final Pattern BASIC_MOBILE_PATTERN = Pattern.compile("^[\\+]?[0-9\\s\\-\\(\\)]{10,15}$");

    public CreateAccountMobileNumberValidator() {
        super(AzureResources.MOBILE_NUMBER_CREATE_ACCOUNT, FieldTypes.TEXT_FIELD_LOGICAL_NAME);
    }

    @Override
    public Boolean validate(String resourceValue, Map<ValidationResource, String> context) {
        LOGGER.debug("CreateAccountMobileNumberValidator.validate() called with input: {}", resourceValue);

        // Check if input is null or empty
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            LOGGER.warn("Mobile Number is null or empty for account creation");
            return false;
        }

        String trimmedMobileNumber = resourceValue.trim();

        // Basic format validation for account creation
        if (!isValidMobileNumberFormatForAccountCreation(trimmedMobileNumber)) {
            LOGGER.warn("Invalid mobile number format for account creation: {}", trimmedMobileNumber);
            return false;
        }

        LOGGER.debug("Mobile Number validation successful for account creation: {}", trimmedMobileNumber);
        return true;
    }

    @Override
    public String getFetchFieldName() {
        return AzureResources.MOBILE_NUMBER_CREATE_ACCOUNT;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD_LOGICAL_NAME;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid mobile number for the new account (10-15 digits, international format supported with + prefix).";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        return "The provided mobile number '" + resourceValue + "' is not valid for account creation. Please enter a valid mobile number.";
    }

    @Override
    public String getErrMessage(String resourceValue) {
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            return "Mobile Number is required for account creation.";
        }

        String trimmedMobileNumber = resourceValue.trim();

        if (!isValidMobileNumberFormatForAccountCreation(trimmedMobileNumber)) {
            return "Invalid mobile number format: '" + trimmedMobileNumber + "'. Please enter a valid mobile number (10-15 digits, international format supported with + prefix).";
        }

        return "Mobile Number '" + trimmedMobileNumber + "' is not valid for account creation.";
    }

    /**
     * Validates the mobile number format for account creation.
     * 
     * @param mobileNumber The mobile number to validate
     * @return true if the mobile number format is valid, false otherwise
     */
    private boolean isValidMobileNumberFormatForAccountCreation(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            return false;
        }

        String trimmedMobileNumber = mobileNumber.trim();

        // Remove common formatting characters for validation
        String cleanedNumber = trimmedMobileNumber.replaceAll("[\\s\\-\\(\\)]", "");

        // Check basic pattern first
        if (!BASIC_MOBILE_PATTERN.matcher(trimmedMobileNumber).matches()) {
            LOGGER.debug("Mobile number basic pattern validation failed for: {}", trimmedMobileNumber);
            return false;
        }

        // Check length constraints on cleaned number
        if (cleanedNumber.length() < 10 || cleanedNumber.length() > 15) {
            LOGGER.debug("Mobile number length validation failed: {} digits", cleanedNumber.length());
            return false;
        }

        // If it starts with +, ensure it's followed by digits
        if (trimmedMobileNumber.startsWith("+")) {
            String numberPart = trimmedMobileNumber.substring(1).replaceAll("[\\s\\-\\(\\)]", "");
            if (numberPart.isEmpty() || !numberPart.matches("^[0-9]+$")) {
                LOGGER.debug("Mobile number with + prefix validation failed: {}", trimmedMobileNumber);
                return false;
            }
        }

        // Ensure it contains at least some digits
        if (!trimmedMobileNumber.matches(".*\\d.*")) {
            LOGGER.debug("Mobile number contains no digits: {}", trimmedMobileNumber);
            return false;
        }

        return true;
    }
}
