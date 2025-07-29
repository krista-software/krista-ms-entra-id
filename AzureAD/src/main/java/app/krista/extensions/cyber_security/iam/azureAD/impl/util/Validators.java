package app.krista.extensions.cyber_security.iam.azureAD.impl.util;

import java.util.Map;

public class Validators {

    public static void addAttributeIfNotNull(Map<String, Object> attributesMap, String key, Object value) {
        if (value != null) {
            attributesMap.put(key, value);
        }
    }

    private Validators() {
    }
}
