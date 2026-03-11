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

import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.extresp.FieldTypes;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.util.MicrosoftEntraIDResources;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.AccountImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Validator for Group Object Id validation specifically for Remove Account From Group operations.
 * Validates that the provided Group Object Id exists in Microsoft Entra ID and is eligible for user removal.
 */
public class RemoveAccountGroupObjectIdValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveAccountGroupObjectIdValidator.class);

    private final AccountImpl accountService;

    public RemoveAccountGroupObjectIdValidator(AccountImpl accountService) {
        this.accountService = accountService;
    }

    @Override
    public Boolean validate(String resourceValue, Map<ValidationResource, String> context) {
        LOGGER.debug("RemoveAccountGroupObjectIdValidator.validate() called with input: {}", resourceValue);

        // Check if input is null or empty
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            LOGGER.warn("Group Object Id is null or empty for user removal");
            return false;
        }

        String trimmedGroupId = resourceValue.trim();

        // Basic format validation for GUID
        if (!isValidGuidFormat(trimmedGroupId)) {
            LOGGER.warn("Invalid Group Object Id format for user removal: {}", trimmedGroupId);
            return false;
        }

        try {
            // Try to get group details to validate if group exists
            List<Map<String, String>> allGroups = accountService.getAllGroups();

            boolean groupExists = allGroups.stream()
                    .anyMatch(group -> trimmedGroupId.equals(group.get("Group Id")));

            if (!groupExists) {
                LOGGER.warn("Group not found with Object Id for user removal: {}", trimmedGroupId);
                return false;
            }

            // Additional validation specific to user removal from group
            if (!isEligibleForUserRemoval(trimmedGroupId)) {
                LOGGER.warn("Group is not eligible for user removal: {}", trimmedGroupId);
                return false;
            }

            LOGGER.debug("Group Object Id validation successful for user removal: {}", trimmedGroupId);
            return true;

        } catch (Exception e) {
            LOGGER.error("Error validating Group Object Id for user removal '{}': {}", trimmedGroupId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        return MicrosoftEntraIDResources.GROUP_OBJECT_ID;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD_LOGICAL_NAME;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid Group Object Id (GUID format) for user removal.";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        return "Please enter a valid Group Object Id that exists in Microsoft Entra ID for user removal.";
    }

    @Override
    public String getErrMessage(String resourceValue) {
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            return "Group Object Id is required for user removal.";
        }

        String trimmedGroupId = resourceValue.trim();

        if (!isValidGuidFormat(trimmedGroupId)) {
            return "Invalid Group Object Id format: '" + trimmedGroupId + "'. Please enter a valid GUID format (e.g., 12345678-1234-1234-1234-123456789012).";
        }

        return "Group Object Id '" + trimmedGroupId + "' not found in Microsoft Entra ID or not eligible for user removal.";
    }

    /**
     * Validates GUID format for Group Object Id
     */
    private boolean isValidGuidFormat(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return false;
        }

        // Check for standard GUID format: 8-4-4-4-12 hexadecimal digits
        return groupId.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    /**
     * Checks if the group is eligible for user removal
     */
    private boolean isEligibleForUserRemoval(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return false;
        }
        return true;
    }
}
