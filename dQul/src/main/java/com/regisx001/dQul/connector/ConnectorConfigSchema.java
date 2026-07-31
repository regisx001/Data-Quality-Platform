package com.regisx001.dQul.connector;

import java.util.List;

/**
 * Describes the configuration schema for a connector type.
 * Used by the frontend to render dynamic configuration forms.
 */
public record ConnectorConfigSchema(
        String type,
        String label,
        String description,
        List<ConfigField> fields) {

    public record ConfigField(
            String name,
            String label,
            String type,
            String defaultValue,
            String description,
            boolean required,
            Integer min,
            Integer max,
            List<String> options) {
    }
}
