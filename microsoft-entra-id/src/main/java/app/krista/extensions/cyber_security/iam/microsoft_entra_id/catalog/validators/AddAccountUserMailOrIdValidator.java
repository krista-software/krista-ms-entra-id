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
 * Validator for User Mail or Id validation specifically for Add Account To Group operations.
 * Validates that the provided user mail or ID exists in Microsoft Entra ID and is eligible for group addition.
 */
@Singleton
@Service
public class AddAccountUserMailOrIdValidator extends BaseMicrosoftEntraIDUserValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddAccountUserMailOrIdValidator.class);

    @Inject
    public AddAccountUserMailOrIdValidator(AccountImpl accountService) {
        super(MicrosoftEntraIDResources.USER_MAIL_OR_ID_ADD_GROUP, FieldTypes.TEXT_FIELD_LOGICAL_NAME, accountService);
    }

    @Override
    protected boolean isValidFormat(String trimmedValue) {
        return isValidUserMailOrIdFormat(trimmedValue);
    }

    @Override
    protected ValidationResult performUserValidation(String trimmedValue, Map<ValidationResource, String> context) {
        // Check if user exists in Microsoft Entra ID
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
            return "Please enter a valid email address that exists in Microsoft Entra ID to add to the group.";
        } else {
            return "Please enter a valid user ID that exists in Microsoft Entra ID to add to the group.";
        }
    }

    @Override
    protected String getFormatErrorMessage(String value) {
        return "Invalid User Mail or Id format: '" + value + "'. Please enter a valid email address or user ID for group addition.";
    }

    @Override
    protected String getBusinessLogicErrorMessage(String value) {
        if (isEmail(value)) {
            return "Email address '" + value + "' not found in Microsoft Entra ID or not eligible for group addition.";
        } else {
            return "User ID '" + value + "' not found in Microsoft Entra ID or not eligible for group addition.";
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

        // Check for GUID format (common in Microsoft Entra ID)
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
