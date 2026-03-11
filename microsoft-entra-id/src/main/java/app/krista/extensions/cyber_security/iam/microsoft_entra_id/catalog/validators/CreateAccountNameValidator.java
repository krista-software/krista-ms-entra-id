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
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validator for Name field in Create Account operation.
 * Validates that the name is not empty and follows basic naming conventions.
 */
@Singleton
@Service
public class CreateAccountNameValidator extends BaseValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateAccountNameValidator.class);
    
    // Pattern for valid name - allows letters, spaces, hyphens, apostrophes, and dots
    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s\\-'.]{2,50}$");

    public CreateAccountNameValidator() {
        super(MicrosoftEntraIDResources.NAME_CREATE_ACCOUNT, FieldTypes.TEXT_FIELD_LOGICAL_NAME);
    }

    @Override
    public Boolean validate(String resourceValue, Map<ValidationResource, String> context) {
        LOGGER.debug("CreateAccountNameValidator.validate() called with input: {}", resourceValue);

        // Check if input is null or empty
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            LOGGER.warn("Name is null or empty for account creation");
            return false;
        }

        String trimmedName = resourceValue.trim();

        // Basic format validation for account creation
        if (!isValidNameFormatForAccountCreation(trimmedName)) {
            LOGGER.warn("Invalid name format for account creation: {}", trimmedName);
            return false;
        }

        LOGGER.debug("Name validation successful for account creation: {}", trimmedName);
        return true;
    }

    @Override
    public String getFetchFieldName() {
        return MicrosoftEntraIDResources.NAME_CREATE_ACCOUNT;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD_LOGICAL_NAME;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid name for the new account (2-50 characters, letters, spaces, hyphens, apostrophes, and dots allowed).";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        return "The provided name '" + resourceValue + "' is not valid for account creation. Please enter a valid name.";
    }

    @Override
    public String getErrMessage(String resourceValue) {
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            return "Name is required for account creation.";
        }

        String trimmedName = resourceValue.trim();

        if (!isValidNameFormatForAccountCreation(trimmedName)) {
            return "Invalid name format: '" + trimmedName + "'. Please enter a valid name (2-50 characters, letters, spaces, hyphens, apostrophes, and dots allowed).";
        }

        return "Name '" + trimmedName + "' is not valid for account creation.";
    }

    /**
     * Validates the name format for account creation.
     * 
     * @param name The name to validate
     * @return true if the name format is valid, false otherwise
     */
    private boolean isValidNameFormatForAccountCreation(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        String trimmedName = name.trim();

        // Check length constraints
        if (trimmedName.length() < 2 || trimmedName.length() > 50) {
            LOGGER.debug("Name length validation failed: {} characters", trimmedName.length());
            return false;
        }

        // Check pattern
        if (!VALID_NAME_PATTERN.matcher(trimmedName).matches()) {
            LOGGER.debug("Name pattern validation failed for: {}", trimmedName);
            return false;
        }

        // Additional checks
        if (trimmedName.startsWith(" ") || trimmedName.endsWith(" ")) {
            LOGGER.debug("Name has leading/trailing spaces: '{}'", trimmedName);
            return false;
        }

        // Check for consecutive spaces
        if (trimmedName.contains("  ")) {
            LOGGER.debug("Name contains consecutive spaces: {}", trimmedName);
            return false;
        }

        return true;
    }
}
