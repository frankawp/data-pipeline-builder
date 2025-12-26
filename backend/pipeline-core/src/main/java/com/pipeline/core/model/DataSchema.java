package com.pipeline.core.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 数据结构定义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSchema {

    private List<FieldDefinition> fields = new ArrayList<>();

    public void addField(String name, DataType type) {
        fields.add(new FieldDefinition(name, type, true, null));
    }

    public void addField(String name, DataType type, boolean nullable) {
        fields.add(new FieldDefinition(name, type, nullable, null));
    }

    public Optional<FieldDefinition> getField(String name) {
        return fields.stream()
                .filter(f -> f.getName().equals(name))
                .findFirst();
    }

    public List<String> getFieldNames() {
        return fields.stream().map(FieldDefinition::getName).toList();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldDefinition {
        private String name;
        private DataType type;
        private boolean nullable;
        private String description;
    }

    public enum DataType {
        STRING,
        INTEGER,
        LONG,
        DOUBLE,
        BOOLEAN,
        DATE,
        DATETIME,
        TIMESTAMP,
        DECIMAL,
        BINARY,
        JSON,
        ARRAY,
        MAP,
        UNKNOWN
    }
}
