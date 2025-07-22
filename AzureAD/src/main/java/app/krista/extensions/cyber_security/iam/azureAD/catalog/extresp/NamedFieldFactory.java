package app.krista.extensions.cyber_security.iam.azureAD.catalog.extresp;


import app.krista.model.field.NamedField;
import app.krista.model.field.NamedValuedField;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating named fields for the UI.
 */
public class NamedFieldFactory {
    private NamedFieldFactory() {}

    /**
     * Creates a text field with the given name.
     *
     * @param fieldName The field name
     * @return The named field
     */
    public static NamedField createTextField(String fieldName) {
        return new NamedField(fieldName, FieldTypes.TEXT_FIELD_LOGICAL_NAME, new HashMap<>(), new HashMap<>());
    }

    /**
     * Creates a number field with the given name.
     *
     * @param fieldName The field name
     * @return The named field
     */
    public static NamedField createNumberField(String fieldName) {
        return new NamedField(fieldName, FieldTypes.NUMBER_FIELD_LOGICAL_NAME, new HashMap<>(), new HashMap<>());
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
     * Creates a boolean field with the given name.
     *
     * @param fieldName The field name
     * @return The named field
     */
    public static NamedField createBooleanField(String fieldName) {
        return new NamedField(fieldName, FieldTypes.BOOLEAN_FIELD_LOGICAL_NAME, new HashMap<>(), new HashMap<>());
    }

    /**
     * Creates a date field with the given name.
     *
     * @param fieldName The field name
     * @return The named field
     */
    public static NamedField createDateField(String fieldName) {
        return new NamedField(fieldName, FieldTypes.DATE_FIELD_LOGICAL_NAME, new HashMap<>(), new HashMap<>());
    }

    /**
     * Creates a rich text field with the given name.
     *
     * @param fieldName The field name
     * @return The named field
     */
    public static NamedField createRichTextField(String fieldName) {
        return new NamedField(fieldName, FieldTypes.RICH_TEXT_FIELD_LOGICAL_NAME, new HashMap<>(), new HashMap<>());
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

    /**
     * Creates a field with the given name, type, and attributes.
     *
     * @param fieldName  The field name
     * @param fieldType  The field type
     * @param attributes The field attributes
     * @return The named field
     */
    public static NamedField createField(String fieldName, String fieldType, Map<String, Object> attributes) {
        return new NamedField(fieldName, fieldType, attributes, new HashMap<>());
    }

    /**
     * Creates a field with the given name, type, attributes, and options.
     *
     * @param fieldName  The field name
     * @param fieldType  The field type
     * @param attributes The field attributes
     * @param options    The field options
     * @return The named field
     */
    public static NamedField createField(String fieldName, String fieldType, Map<String, Object> attributes, Map<String, NamedValuedField> options) {
        return new NamedField(fieldName, fieldType, attributes, options);
    }


}
