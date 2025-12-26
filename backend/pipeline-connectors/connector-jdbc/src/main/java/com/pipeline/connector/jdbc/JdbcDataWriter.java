package com.pipeline.connector.jdbc;

import com.pipeline.core.connector.ConnectorException;
import com.pipeline.core.connector.DataWriter;
import com.pipeline.core.model.DataRecord;
import com.pipeline.core.model.DataSchema;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

/**
 * JDBC 数据写入器
 */
@Slf4j
public class JdbcDataWriter implements DataWriter {

    private final Map<String, Object> config;
    private Connection connection;
    private DataSchema schema;
    private PreparedStatement insertStmt;
    private long writtenCount = 0;
    private int batchSize;
    private int currentBatchSize = 0;

    public JdbcDataWriter(Map<String, Object> config) {
        this.config = config;
        this.batchSize = ((Number) config.getOrDefault("batchSize", 1000)).intValue();
    }

    @Override
    public void setSchema(DataSchema schema) {
        this.schema = schema;
    }

    @Override
    public void open() {
        this.connection = JdbcConnector.createConnection(config);
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new ConnectorException("Failed to set auto commit", e);
        }

        String writeMode = (String) config.getOrDefault("writeMode", "append");
        if ("overwrite".equals(writeMode)) {
            truncateTable();
        }

        prepareInsertStatement();
    }

    @Override
    public void write(DataRecord record) {
        try {
            setParameters(insertStmt, record);
            insertStmt.addBatch();
            currentBatchSize++;

            if (currentBatchSize >= batchSize) {
                executeBatch();
            }
        } catch (SQLException e) {
            throw new ConnectorException("Failed to write record", e);
        }
    }

    @Override
    public void write(Iterator<DataRecord> records) {
        while (records.hasNext()) {
            write(records.next());
        }
        // 执行剩余的批次
        if (currentBatchSize > 0) {
            executeBatch();
        }
    }

    @Override
    public void commit() {
        try {
            if (currentBatchSize > 0) {
                executeBatch();
            }
            connection.commit();
            log.info("Committed {} records", writtenCount);
        } catch (SQLException e) {
            throw new ConnectorException("Failed to commit", e);
        }
    }

    @Override
    public void rollback() {
        try {
            connection.rollback();
            writtenCount = 0;
        } catch (SQLException e) {
            log.error("Failed to rollback", e);
        }
    }

    @Override
    public void close() {
        try {
            if (insertStmt != null) insertStmt.close();
            if (connection != null) connection.close();
        } catch (SQLException e) {
            log.warn("Failed to close resources", e);
        }
    }

    @Override
    public long getWrittenCount() {
        return writtenCount;
    }

    private void truncateTable() {
        String table = (String) config.get("table");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE " + table);
            log.info("Truncated table: {}", table);
        } catch (SQLException e) {
            throw new ConnectorException("Failed to truncate table", e);
        }
    }

    private void prepareInsertStatement() {
        String table = (String) config.get("table");
        if (table == null || table.isBlank()) {
            throw new ConnectorException("Table name is required for writing");
        }

        if (schema == null) {
            throw new ConnectorException("Schema must be set before opening writer");
        }

        List<String> columns = schema.getFieldNames();
        String columnList = String.join(", ", columns);
        String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", table, columnList, placeholders);

        String writeMode = (String) config.getOrDefault("writeMode", "append");
        if ("upsert".equals(writeMode)) {
            String databaseType = (String) config.getOrDefault("databaseType", "mysql");
            if ("mysql".equals(databaseType)) {
                String updateClause = columns.stream()
                        .map(c -> c + " = VALUES(" + c + ")")
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                sql += " ON DUPLICATE KEY UPDATE " + updateClause;
            }
        }

        log.info("Prepared statement: {}", sql);
        try {
            insertStmt = connection.prepareStatement(sql);
        } catch (SQLException e) {
            throw new ConnectorException("Failed to prepare insert statement", e);
        }
    }

    private void setParameters(PreparedStatement stmt, DataRecord record) throws SQLException {
        int index = 1;
        for (String fieldName : schema.getFieldNames()) {
            Object value = record.get(fieldName);
            stmt.setObject(index++, value);
        }
    }

    private void executeBatch() {
        try {
            int[] results = insertStmt.executeBatch();
            writtenCount += Arrays.stream(results).filter(r -> r >= 0).count();
            currentBatchSize = 0;
        } catch (SQLException e) {
            throw new ConnectorException("Failed to execute batch", e);
        }
    }
}
