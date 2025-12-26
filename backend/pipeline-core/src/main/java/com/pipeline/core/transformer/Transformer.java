package com.pipeline.core.transformer;

import com.pipeline.core.model.DataRecord;
import com.pipeline.core.model.DataSchema;
import com.pipeline.core.schema.ConfigSchema;

import java.util.Iterator;
import java.util.Map;

/**
 * 数据转换器接口
 */
public interface Transformer {

    /**
     * 获取转换器类型标识
     */
    String getType();

    /**
     * 获取显示名称
     */
    String getDisplayName();

    /**
     * 获取描述
     */
    String getDescription();

    /**
     * 获取配置 Schema
     */
    ConfigSchema getConfigSchema();

    /**
     * 验证配置
     */
    void validate(Map<String, Object> config) throws TransformerException;

    /**
     * 根据输入 Schema 和配置，计算输出 Schema
     */
    DataSchema getOutputSchema(DataSchema inputSchema, Map<String, Object> config);

    /**
     * 转换数据
     */
    Iterator<DataRecord> transform(Iterator<DataRecord> input, Map<String, Object> config);

    /**
     * 是否支持多输入
     */
    default boolean supportsMultipleInputs() {
        return false;
    }

    /**
     * 转换多输入数据（用于 JOIN 等场景）
     */
    default Iterator<DataRecord> transform(Map<String, Iterator<DataRecord>> inputs, Map<String, Object> config) {
        throw new UnsupportedOperationException("Multiple inputs not supported");
    }
}
