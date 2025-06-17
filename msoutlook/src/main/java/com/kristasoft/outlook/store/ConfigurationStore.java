package com.kristasoft.outlook.store;

import com.kristasoft.outlook.Constants;

import java.util.Map;

public class ConfigurationStore {

    private final KeyValueStore store;
    private final String invokerId;

    public ConfigurationStore(KeyValueStore store, String invokerId) {
        this.store = store;
        this.invokerId = invokerId;
    }

    /**
     * Retrieves the configuration for the current invoker.
     * <p>
     * This method validates the store and invoker, then loads and deserializes
     * the configuration data from storage.
     *
     * @return The configuration as a Map, or null if no configuration exists
     */
    public Map<String, Object> getConfiguration() {
        validateStore();
        final String loadedAttributes = String.valueOf(store.get(invokerId));
        if (loadedAttributes == null || loadedAttributes.isBlank()) {
            throw new IllegalStateException("Appliance configuration is not configured");
        }
        return (Map<String, Object>) Constants.GSON.fromJson(loadedAttributes, Map.class);
    }

    /**
     * Validates that the invoker and store are properly initialized.
     * <p>
     * This method checks that the invoker and store are not null,
     * and that the invoker has a valid ID.
     *
     * @throws IllegalArgumentException if validation fails
     */
    private void validateStore() {
        if (store == null) {
            throw new IllegalArgumentException("KeyValueStore cannot be null");
        }
        if (invokerId == null) {
            throw new IllegalArgumentException("Invoker id is missing");
        }
    }

    /**
     * Saves the configuration for the current invoker.
     * <p>
     * This method validates the configuration and store, then serializes
     * and saves the configuration data to storage.
     *
     * @param configuration The configuration to save
     * @throws IllegalArgumentException if the configuration is null or validation fails
     */
    public void saveConfiguration(Map<String, Object> configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        validateStore();
        store.put(invokerId, Constants.GSON.toJson(configuration));
    }

    /**
     * Deletes the configuration for the current invoker.
     * <p>
     * This method retrieves the current configuration, removes it from storage,
     * and returns the deleted configuration.
     *
     * @return The deleted configuration, or null if no configuration existed
     */
    public Map<String, Object> deleteConfiguration() {
        Map<String, Object> configuration = getConfiguration();
        store.remove(invokerId);
        return configuration;
    }

}
