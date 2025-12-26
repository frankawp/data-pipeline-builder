package com.pipeline.core.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用数据记录，表示一行数据
 */
@Data
@NoArgsConstructor
public class DataRecord {

    /**
     * 字段名 -> 值的映射，保持插入顺序
     */
    private Map<String, Object> fields = new LinkedHashMap<>();

    public DataRecord(Map<String, Object> fields) {
        this.fields = new LinkedHashMap<>(fields);
    }

    public Object get(String fieldName) {
        return fields.get(fieldName);
    }

    public void set(String fieldName, Object value) {
        fields.put(fieldName, value);
    }

    public boolean hasField(String fieldName) {
        return fields.containsKey(fieldName);
    }

    public DataRecord copy() {
        return new DataRecord(new LinkedHashMap<>(fields));
    }
}
