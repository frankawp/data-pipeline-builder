package com.pipeline.connector.jdbc;

import com.pipeline.core.connector.ConnectorException;
import com.pipeline.core.connector.DataReader;
import com.pipeline.core.model.DataRecord;
import com.pipeline.core.model.DataSchema;
import com.pipeline.core.model.DataSchema.DataType;
import com.pipeline.core.model.DataSchema.FieldDefinition;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

/**
 * JDBC 数据读取器
 */
@Slf4j
public class JdbcDataReader implements DataReader {

    private final Map<String, Object> config;
    private Connection connection;
    private DataSchema schema;

    public JdbcDataReader(Map<String, Object> config) {
        this.config = config;
    }

    @Override
    public void open() {
        this.connection = JdbcConnector.createConnection(config);
    }

    @Override
    public DataSchema getSchema() {
        if (schema == null) {
            schema = fetchSchema();
        }
        return schema;
    }

    @Override
    public Iterator<DataRecord> read() {
        String sql = buildQuery();
        log.info("Executing query: {}", sql);

        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            return new ResultSetIterator(rs, getSchema());
        } catch (SQLException e) {
            throw new ConnectorException("Failed to execute query", e);
        }
    }

    @Override
    public long estimateCount() {
        String table = (String) config.get("table");
        if (table != null) {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            } catch (SQLException e) {
                log.warn("Failed to estimate count", e);
            }
        }
        return -1;
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.warn("Failed to close connection", e);
            }
        }
    }

    private String buildQuery() {
        String query = (String) config.get("query");
        if (query != null && !query.isBlank()) {
            return query;
        }

        String table = (String) config.get("table");
        if (table == null || table.isBlank()) {
            throw new ConnectorException("Either 'query' or 'table' must be specified");
        }
        return "SELECT * FROM " + table;
    }

    private DataSchema fetchSchema() {
        String sql = buildQuery() + " LIMIT 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            DataSchema.DataSchemaBuilder builder = DataSchema.builder();
            List<FieldDefinition> fields = new ArrayList<>();

            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                fields.add(FieldDefinition.builder()
                        .name(metaData.getColumnLabel(i))
                        .type(mapSqlType(metaData.getColumnType(i)))
                        .nullable(metaData.isNullable(i) == ResultSetMetaData.columnNullable)
                        .build());
            }

            return builder.fields(fields).build();
        } catch (SQLException e) {
            throw new ConnectorException("Failed to fetch schema", e);
        }
    }

    private DataType mapSqlType(int sqlType) {
        return switch (sqlType) {
            case Types.VARCHAR, Types.CHAR, Types.LONGVARCHAR, Types.NVARCHAR, Types.NCHAR -> DataType.STRING;
            case Types.INTEGER, Types.SMALLINT, Types.TINYINT -> DataType.INTEGER;
            case Types.BIGINT -> DataType.LONG;
            case Types.FLOAT, Types.REAL -> DataType.DOUBLE;
            case Types.DOUBLE -> DataType.DOUBLE;
            case Types.DECIMAL, Types.NUMERIC -> DataType.DECIMAL;
            case Types.BOOLEAN, Types.BIT -> DataType.BOOLEAN;
            case Types.DATE -> DataType.DATE;
            case Types.TIME, Types.TIMESTAMP -> DataType.TIMESTAMP;
            case Types.BLOB, Types.BINARY, Types.VARBINARY -> DataType.BINARY;
            default -> DataType.STRING;
        };
    }

    /**
     * ResultSet 迭代器
     */
    private static class ResultSetIterator implements Iterator<DataRecord> {
        private final ResultSet rs;
        private final DataSchema schema;
        private Boolean hasNext;

        ResultSetIterator(ResultSet rs, DataSchema schema) {
            this.rs = rs;
            this.schema = schema;
        }

        @Override
        public boolean hasNext() {
            if (hasNext == null) {
                try {
                    hasNext = rs.next();
                } catch (SQLException e) {
                    throw new ConnectorException("Failed to read result set", e);
                }
            }
            return hasNext;
        }

        @Override
        public DataRecord next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            hasNext = null;

            try {
                Map<String, Object> fields = new LinkedHashMap<>();
                for (FieldDefinition field : schema.getFields()) {
                    fields.put(field.getName(), rs.getObject(field.getName()));
                }
                return new DataRecord(fields);
            } catch (SQLException e) {
                throw new ConnectorException("Failed to read record", e);
            }
        }
    }
}
