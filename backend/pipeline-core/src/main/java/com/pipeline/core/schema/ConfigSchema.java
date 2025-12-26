package com.pipeline.core.schema;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 配置 Schema - 用于描述 Connector/Transformer 的配置项
 * 可用于前端动态生成配置表单
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigSchema {

    @Builder.Default
    private List<ConfigField> fields = new ArrayList<>();

    public void addField(ConfigField field) {
        fields.add(field);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfigField {
        private String name;
        private String label;
        private String description;
        private FieldType type;
        private boolean required;
        private Object defaultValue;
        private Map<String, Object> options;  // 用于 select、radio 等类型

        /**
         * 验证规则
         */
        private ValidationRule validation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationRule {
        private Integer minLength;
        private Integer maxLength;
        private Double min;
        private Double max;
        private String pattern;  // 正则表达式
        private String message;  // 错误消息
    }

    public enum FieldType {
        STRING,
        NUMBER,
        INTEGER,
        BOOLEAN,
        PASSWORD,
        TEXTAREA,
        SELECT,
        MULTI_SELECT,
        JSON,
        SQL,
        FILE_PATH,
        TABLE_SELECTOR,   // 选择表
        COLUMN_MAPPING    // 字段映射
    }
}
