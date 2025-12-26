package com.pipeline.connector.jdbc;

import com.pipeline.core.connector.*;
import com.pipeline.core.schema.ConfigSchema;
import com.pipeline.core.schema.ConfigSchema.ConfigField;
import com.pipeline.core.schema.ConfigSchema.FieldType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;

/**
 * JDBC 连接器 - 支持 MySQL, PostgreSQL
 */
@Slf4j
@Component
public class JdbcConnector implements Connector {

    @Override
    public String getType() {
        return "jdbc";
    }

    @Override
    public String getDisplayName() {
        return "JDBC Database";
    }

    @Override
    public String getDescription() {
        return "Connect to relational databases via JDBC (MySQL, PostgreSQL, etc.)";
    }

    @Override
    public ConfigSchema getConfigSchema() {
        return ConfigSchema.builder()
                .fields(java.util.List.of(
                        ConfigField.builder()
                                .name("databaseType")
                                .label("数据库类型")
                                .type(FieldType.SELECT)
                                .required(true)
                                .options(Map.of(
                                        "options", java.util.List.of(
                                                Map.of("value", "mysql", "label", "MySQL"),
                                                Map.of("value", "postgresql", "label", "PostgreSQL")
                                        )
                                ))
                                .build(),
                        ConfigField.builder()
                                .name("host")
                                .label("主机地址")
                                .type(FieldType.STRING)
                                .required(true)
                                .defaultValue("localhost")
                                .build(),
                        ConfigField.builder()
                                .name("port")
                                .label("端口")
                                .type(FieldType.INTEGER)
                                .required(true)
                                .defaultValue(3306)
                                .build(),
                        ConfigField.builder()
                                .name("database")
                                .label("数据库名")
                                .type(FieldType.STRING)
                                .required(true)
                                .build(),
                        ConfigField.builder()
                                .name("username")
                                .label("用户名")
                                .type(FieldType.STRING)
                                .required(true)
                                .build(),
                        ConfigField.builder()
                                .name("password")
                                .label("密码")
                                .type(FieldType.PASSWORD)
                                .required(true)
                                .build(),
                        ConfigField.builder()
                                .name("table")
                                .label("表名")
                                .type(FieldType.TABLE_SELECTOR)
                                .required(false)
                                .description("读取或写入的表")
                                .build(),
                        ConfigField.builder()
                                .name("query")
                                .label("SQL 查询")
                                .type(FieldType.SQL)
                                .required(false)
                                .description("自定义 SQL 查询（读取时使用）")
                                .build(),
                        ConfigField.builder()
                                .name("writeMode")
                                .label("写入模式")
                                .type(FieldType.SELECT)
                                .required(false)
                                .defaultValue("append")
                                .options(Map.of(
                                        "options", java.util.List.of(
                                                Map.of("value", "append", "label", "追加"),
                                                Map.of("value", "overwrite", "label", "覆盖"),
                                                Map.of("value", "upsert", "label", "更新或插入")
                                        )
                                ))
                                .build(),
                        ConfigField.builder()
                                .name("batchSize")
                                .label("批量大小")
                                .type(FieldType.INTEGER)
                                .required(false)
                                .defaultValue(1000)
                                .build()
                ))
                .build();
    }

    @Override
    public void validate(Map<String, Object> config) throws ConnectorException {
        requireField(config, "host");
        requireField(config, "port");
        requireField(config, "database");
        requireField(config, "username");
        requireField(config, "password");
    }

    @Override
    public boolean testConnection(Map<String, Object> config) {
        try (Connection conn = createConnection(config)) {
            return conn.isValid(5);
        } catch (Exception e) {
            log.error("Connection test failed", e);
            return false;
        }
    }

    @Override
    public DataReader createReader(Map<String, Object> config) {
        return new JdbcDataReader(config);
    }

    @Override
    public DataWriter createWriter(Map<String, Object> config) {
        return new JdbcDataWriter(config);
    }

    private void requireField(Map<String, Object> config, String field) {
        if (!config.containsKey(field) || config.get(field) == null) {
            throw new ConnectorException("Missing required field: " + field);
        }
    }

    static Connection createConnection(Map<String, Object> config) {
        String databaseType = (String) config.getOrDefault("databaseType", "mysql");
        String host = (String) config.get("host");
        int port = ((Number) config.get("port")).intValue();
        String database = (String) config.get("database");
        String username = (String) config.get("username");
        String password = (String) config.get("password");

        String url;
        if ("postgresql".equals(databaseType)) {
            url = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        } else {
            url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true",
                    host, port, database);
        }

        try {
            return DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            throw new ConnectorException("Failed to connect to database", e);
        }
    }
}
