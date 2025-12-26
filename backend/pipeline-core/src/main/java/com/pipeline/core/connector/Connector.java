package com.pipeline.core.connector;

import com.pipeline.core.schema.ConfigSchema;
import java.util.Map;

/**
 * 数据连接器接口 - 所有数据源连接器的基础接口
 */
public interface Connector {

    /**
     * 获取连接器类型标识
     * @return 类型标识，如 mysql, postgresql, csv 等
     */
    String getType();

    /**
     * 获取连接器显示名称
     */
    String getDisplayName();

    /**
     * 获取连接器描述
     */
    String getDescription();

    /**
     * 获取连接器配置的 JSON Schema
     */
    ConfigSchema getConfigSchema();

    /**
     * 验证配置是否有效
     */
    void validate(Map<String, Object> config) throws ConnectorException;

    /**
     * 测试连接
     */
    boolean testConnection(Map<String, Object> config);

    /**
     * 创建数据读取器
     */
    DataReader createReader(Map<String, Object> config);

    /**
     * 创建数据写入器
     */
    DataWriter createWriter(Map<String, Object> config);

    /**
     * 是否支持读取
     */
    default boolean supportsRead() {
        return true;
    }

    /**
     * 是否支持写入
     */
    default boolean supportsWrite() {
        return true;
    }
}
