/*
 * Ms Entra Id Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.validators;

import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.util.MicrosoftEntraIDResources;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.extresp.FieldTypes;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.AccountImpl;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validator for Email field in Create Account operation.
 * Validates email format and checks if the email already exists in Microsoft Entra ID.
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
        super(MicrosoftEntraIDResources.EMAIL_CREATE_ACCOUNT, FieldTypes.TEXT_FIELD_LOGICAL_NAME);
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

        // Check if email already exists in Microsoft Entra ID
        try {
            // Try to get user by email - if it exists, validation should fail for creation
            var existingUser = accountService.getUser(trimmedEmail);
            if (existingUser != null) {
                LOGGER.warn("Email already exists in Microsoft Entra ID for account creation: {}", trimmedEmail);
                return false;
            }
            // User doesn't exist (null returned), that's good for creation
            LOGGER.debug("Email doesn't exist in Microsoft Entra ID (good for creation): {}", trimmedEmail);
            return true;
        } catch (Exception e) {
            // Service error - fail validation gracefully
            LOGGER.error("Error checking if email exists in Microsoft Entra ID: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        return MicrosoftEntraIDResources.EMAIL_CREATE_ACCOUNT;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD_LOGICAL_NAME;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid email address for the new account that doesn't already exist in Microsoft Entra ID.";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        if (isValidEmailFormat(resourceValue)) {
            return "The email address '" + resourceValue + "' already exists in Microsoft Entra ID. Please enter a different email address.";
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
                return "Email address '" + trimmedEmail + "' already exists in Microsoft Entra ID. Please use a different email address.";
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
