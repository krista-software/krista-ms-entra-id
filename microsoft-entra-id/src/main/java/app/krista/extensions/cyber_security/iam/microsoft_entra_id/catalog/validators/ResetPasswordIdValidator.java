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

import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.extresp.FieldTypes;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.util.MicrosoftEntraIDResources;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.AccountImpl;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Validator for ID validation specifically for Reset Password operations.
 * Validates that the provided ID (email or user ID) exists in Microsoft Entra ID and is eligible for password reset.
 */
@Singleton
@Service
public class ResetPasswordIdValidator extends BaseMicrosoftEntraIDUserValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResetPasswordIdValidator.class);

    @Inject
    public ResetPasswordIdValidator(AccountImpl accountService) {
        super(MicrosoftEntraIDResources.ID, FieldTypes.TEXT_FIELD_LOGICAL_NAME, accountService);
    }

    @Override
    protected boolean isValidFormat(String trimmedValue) {
        // Use the specific format validation for password reset
        return isValidIdFormat(trimmedValue);
    }

    @Override
    protected ValidationResult performUserValidation(String trimmedValue, Map<ValidationResource, String> context) {
        // Check if user exists in Microsoft Entra ID
        Account user = lookupUser(trimmedValue);
        if (user == null) {
            return failure("User not found with ID for password reset: " + trimmedValue);
        }

        // Additional validation specific to password reset
        if (!isEligibleForPasswordReset(user)) {
            return failure("User is not eligible for password reset: " + trimmedValue);
        }

        return success();
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid user ID or email address for password reset.";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        if (isEmail(resourceValue)) {
            return "Please enter a valid email address that exists in Microsoft Entra ID for password reset.";
        } else {
            return "Please enter a valid user ID that exists in Microsoft Entra ID for password reset.";
        }
    }

    @Override
    protected String getFormatErrorMessage(String value) {
        return "Invalid ID format: '" + value + "'. Please enter a valid email address or user ID.";
    }

    @Override
    protected String getBusinessLogicErrorMessage(String value) {
        if (isEmail(value)) {
            return "Email address '" + value + "' not found in Microsoft Entra ID or not eligible for password reset.";
        } else {
            return "User ID '" + value + "' not found in Microsoft Entra ID or not eligible for password reset.";
        }
    }

    /**
     * Checks if the given string is a valid ID format (email or user ID)
     */
    private boolean isValidIdFormat(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        // Check if it's an email format
        if (isEmail(id)) {
            return isValidEmailFormat(id);
        }

        // Check if it's a valid user ID format (GUID or other valid formats)
        return isValidUserIdFormat(id);
    }



    /**
     * Validates email format
     */
    private boolean isValidEmailFormat(String email) {
        return email != null &&
                email.contains("@") &&
                email.contains(".") &&
                email.indexOf("@") > 0 &&
                email.lastIndexOf(".") > email.indexOf("@") &&
                email.lastIndexOf(".") < email.length() - 1 &&
                !email.contains(" ") &&
                email.length() >= 5; // minimum reasonable email length
    }

    /**
     * Validates user ID format (could be GUID or other formats)
     */
    private boolean isValidUserIdFormat(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        // Check for GUID format (common in Microsoft Entra ID)
        if (userId.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            return true;
        }

        // Allow other reasonable user ID formats (alphanumeric, dots, hyphens, underscores)
        return userId.matches("^[a-zA-Z0-9._-]+$") && userId.length() >= 3 && userId.length() <= 64;
    }

    /**
     * Checks if the user is eligible for password reset
     * This can include additional business logic specific to password reset operations
     */
    private boolean isEligibleForPasswordReset(Account user) {
        if (user == null) {
            return false;
        }
        return true;
    }
}
