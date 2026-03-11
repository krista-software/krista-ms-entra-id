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

package app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.extresp;


import app.krista.model.field.NamedField;

import java.util.HashMap;

/**
 * Factory for creating named fields for the UI.
 */
public class NamedFieldFactory {
    private NamedFieldFactory() {
    }

    /**
     * Creates a switch field with the given name.
     *
     * @param fieldName The field name
     * @return The named field
     */
    public static NamedField createSwitchField(String fieldName) {
        return new NamedField(fieldName, FieldTypes.SWITCH_FIELD_LOGICAL_NAME, new HashMap<>(), new HashMap<>());
    }

    /**
     * Creates a field with the given name and type.
     *
     * @param fieldName The field name
     * @param fieldType The field type
     * @return The named field
     */
    public static NamedField createField(String fieldName, String fieldType) {
        return new NamedField(fieldName, fieldType, new HashMap<>(), new HashMap<>());
    }

}
