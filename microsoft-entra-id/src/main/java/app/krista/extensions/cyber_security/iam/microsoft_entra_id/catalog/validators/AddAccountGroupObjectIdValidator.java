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
 * Validator for Group Object Id validation specifically for Add Account To Group operations.
 * Validates that the provided Group Object Id exists in Microsoft Entra ID and is eligible for user addition.
 */
public class AddAccountGroupObjectIdValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddAccountGroupObjectIdValidator.class);

    private final AccountImpl accountService;

    public AddAccountGroupObjectIdValidator(AccountImpl accountService) {
        this.accountService = accountService;
    }

    @Override
    public Boolean validate(String resourceValue, Map<ValidationResource, String> context) {
        LOGGER.debug("AddAccountGroupObjectIdValidator.validate() called with input: {}", resourceValue);

        // Check if input is null or empty
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            LOGGER.warn("Group Object Id is null or empty for user addition");
            return false;
        }

        String trimmedGroupId = resourceValue.trim();

        // Basic format validation for GUID (more strict for group addition)
        if (!isValidGuidFormatForGroupAddition(trimmedGroupId)) {
            LOGGER.warn("Invalid Group Object Id format for user addition: {}", trimmedGroupId);
            return false;
        }

        try {
            // Try to get group details to validate if group exists
            List<Map<String, String>> allGroups = accountService.getAllGroups();

            Map<String, String> targetGroup = allGroups.stream()
                    .filter(group -> trimmedGroupId.equals(group.get("Group Id")))
                    .findFirst()
                    .orElse(null);

            if (targetGroup == null) {
                LOGGER.warn("Group not found with Object Id for user addition: {}", trimmedGroupId);
                return false;
            }

            // Additional validation specific to user addition to group
            if (!isEligibleForUserAddition(targetGroup, trimmedGroupId)) {
                LOGGER.warn("Group is not eligible for user addition: {}", trimmedGroupId);
                return false;
            }

            LOGGER.debug("Group Object Id validation successful for user addition: {}", trimmedGroupId);
            return true;

        } catch (Exception e) {
            LOGGER.error("Error validating Group Object Id for user addition '{}': {}", trimmedGroupId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        return MicrosoftEntraIDResources.GROUP_OBJECT_ID_ADD_GROUP;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD_LOGICAL_NAME;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid Group Object Id (GUID format) to add the user to.";
    }

    @Override
    public String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context) {
        return "Please enter a valid Group Object Id that exists in Microsoft Entra ID to add the user to.";
    }

    @Override
    public String getErrMessage(String resourceValue) {
        if (resourceValue == null || resourceValue.trim().isEmpty()) {
            return "Group Object Id is required to add the user to a group.";
        }

        String trimmedGroupId = resourceValue.trim();

        if (!isValidGuidFormatForGroupAddition(trimmedGroupId)) {
            return "Invalid Group Object Id format: '" + trimmedGroupId + "'. Please enter a valid GUID format (e.g., 12345678-1234-1234-1234-123456789012) for group addition.";
        }

        return "Group Object Id '" + trimmedGroupId + "' not found in Microsoft Entra ID or not eligible for user addition.";
    }

    /**
     * Validates GUID format specifically for group addition operations
     * More strict validation than general GUID validation
     */
    private boolean isValidGuidFormatForGroupAddition(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return false;
        }

        // Check for standard GUID format: 8-4-4-4-12 hexadecimal digits
        // More strict validation for group addition operations
        if (!groupId.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            return false;
        }

        // Additional validation for group addition
        // Ensure it's not a null GUID or other invalid patterns
        if (groupId.equals("00000000-0000-0000-0000-000000000000")) {
            LOGGER.warn("Null GUID provided for group addition: {}", groupId);
            return false;
        }

        // Check for common invalid patterns
        if (groupId.matches("^[0]{8}-[0]{4}-[0]{4}-[0]{4}-[0]{12}$") ||
                groupId.matches("^[1]{8}-[1]{4}-[1]{4}-[1]{4}-[1]{12}$") ||
                groupId.matches("^[f]{8}-[f]{4}-[f]{4}-[f]{4}-[f]{12}$") ||
                groupId.matches("^[F]{8}-[F]{4}-[F]{4}-[F]{4}-[F]{12}$")) {
            LOGGER.warn("Invalid GUID pattern for group addition: {}", groupId);
            return false;
        }

        return true;
    }

    /**
     * Checks if the group is eligible for user addition
     * This includes business logic specific to adding users to groups
     */
    private boolean isEligibleForUserAddition(Map<String, String> group, String groupId) {
        if (group == null || groupId == null || groupId.trim().isEmpty()) {
            return false;
        }

        // Check if group has a valid mail ID (indicates it's a proper group)
        String groupMailId = group.get("Group Mail Id");
        if (groupMailId == null || groupMailId.trim().isEmpty()) {
            LOGGER.warn("Group has no valid mail ID for user addition: {}", groupId);
        }

        // Check if group ID is valid
        String retrievedGroupId = group.get("Group Id");
        if (!groupId.equals(retrievedGroupId)) {
            LOGGER.warn("Group ID mismatch for user addition: expected {}, got {}", groupId, retrievedGroupId);
            return false;
        }

        return true;
    }
}
