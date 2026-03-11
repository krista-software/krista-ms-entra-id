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

import java.util.Map;

/**
 * Interface for all validators in the Office365Excel extension.
 * Each validator is responsible for validating a specific type of resource.
 */
public interface Validator {

    enum ValidationResource {
        ID,
        ID_GET_USER_DETAILS,
        EMAIL,
        USER_MAIL_OR_ID,
        USER_MAIL_OR_ID_REMOVE_GROUP,
        GROUP_OBJECT_ID,
        USER_MAIL_OR_ID_ADD_GROUP,
        GROUP_OBJECT_ID_ADD_GROUP,
        NAME_UPDATE_ACCOUNT,
        MOBILE_NUMBER_UPDATE_ACCOUNT,
        EMAIL_UPDATE_ACCOUNT,
        NAME_CREATE_ACCOUNT,
        MOBILE_NUMBER_CREATE_ACCOUNT,
        EMAIL_CREATE_ACCOUNT
    }

    /**
     * Validates the given resource against validation rules
     * @param resourceValue The resource value to validate
     * @param context Additional context for validation
     * @return true if valid, false otherwise
     */
    Boolean validate(String resourceValue, Map<ValidationResource, String> context);

    /**
     * Gets the field name for fetch operations
     */
    String getFetchFieldName();

    /**
     * Gets the field type for the UI
     */
    String getFieldType();

    /**
     * Gets the message to display during fetch step
     */
    String getFetchStepMessage();

    /**
     * Gets the confirmation message after validation
     */
    String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context);

    /**
     * Gets the error message for invalid resources
     */
    String getErrMessage(String resourceValue);
}
