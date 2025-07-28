package app.krista.extensions.cyber_security.iam.azureAD.catalog.extresp;


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
