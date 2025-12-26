package com.pipeline.connector.file;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pipeline.core.connector.*;
import com.pipeline.core.model.DataRecord;
import com.pipeline.core.model.DataSchema;
import com.pipeline.core.model.DataSchema.DataType;
import com.pipeline.core.model.DataSchema.FieldDefinition;
import com.pipeline.core.schema.ConfigSchema;
import com.pipeline.core.schema.ConfigSchema.ConfigField;
import com.pipeline.core.schema.ConfigSchema.FieldType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

/**
 * JSON 文件连接器
 */
@Slf4j
@Component
public class JsonConnector implements Connector {

    @Override
    public String getType() {
        return "json";
    }

    @Override
    public String getDisplayName() {
        return "JSON File";
    }

    @Override
    public String getDescription() {
        return "Read and write JSON files";
    }

    @Override
    public ConfigSchema getConfigSchema() {
        return ConfigSchema.builder()
                .fields(List.of(
                        ConfigField.builder()
                                .name("filePath")
                                .label("文件路径")
                                .type(FieldType.FILE_PATH)
                                .required(true)
                                .build(),
                        ConfigField.builder()
                                .name("jsonPath")
                                .label("JSON Path")
                                .type(FieldType.STRING)
                                .required(false)
                                .description("提取数据的 JSON 路径，如 $.data.items")
                                .build(),
                        ConfigField.builder()
                                .name("prettyPrint")
                                .label("格式化输出")
                                .type(FieldType.BOOLEAN)
                                .required(false)
                                .defaultValue(true)
                                .build()
                ))
                .build();
    }

    @Override
    public void validate(Map<String, Object> config) throws ConnectorException {
        String filePath = (String) config.get("filePath");
        if (filePath == null || filePath.isBlank()) {
            throw new ConnectorException("File path is required");
        }
    }

    @Override
    public boolean testConnection(Map<String, Object> config) {
        String filePath = (String) config.get("filePath");
        File file = new File(filePath);
        return file.exists() || (file.getParentFile() != null && file.getParentFile().exists());
    }

    @Override
    public DataReader createReader(Map<String, Object> config) {
        return new JsonDataReader(config);
    }

    @Override
    public DataWriter createWriter(Map<String, Object> config) {
        return new JsonDataWriter(config);
    }

    /**
     * JSON 数据读取器
     */
    @Slf4j
    static class JsonDataReader implements DataReader {
        private final Map<String, Object> config;
        private final ObjectMapper objectMapper = new ObjectMapper();
        private List<Map<String, Object>> data;
        private DataSchema schema;

        JsonDataReader(Map<String, Object> config) {
            this.config = config;
        }

        @Override
        public void open() {
            String filePath = (String) config.get("filePath");
            try {
                Object rawData = objectMapper.readValue(new File(filePath), Object.class);

                // 处理 JSON 数据
                if (rawData instanceof List) {
                    data = (List<Map<String, Object>>) rawData;
                } else if (rawData instanceof Map) {
                    // 如果是对象，尝试提取数组字段
                    Map<String, Object> map = (Map<String, Object>) rawData;
                    String jsonPath = (String) config.get("jsonPath");
                    if (jsonPath != null && !jsonPath.isBlank()) {
                        // 简单的路径解析 ($.data.items -> data.items)
                        String path = jsonPath.replace("$.", "").replace("$", "");
                        Object extracted = extractPath(map, path);
                        if (extracted instanceof List) {
                            data = (List<Map<String, Object>>) extracted;
                        } else {
                            data = List.of(map);
                        }
                    } else {
                        data = List.of(map);
                    }
                } else {
                    throw new ConnectorException("JSON must be an array or object");
                }

                log.info("Loaded {} records from JSON file: {}", data.size(), filePath);

            } catch (IOException e) {
                throw new ConnectorException("Failed to read JSON file: " + filePath, e);
            }
        }

        private Object extractPath(Map<String, Object> map, String path) {
            String[] parts = path.split("\\.");
            Object current = map;
            for (String part : parts) {
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(part);
                } else {
                    return null;
                }
            }
            return current;
        }

        @Override
        public DataSchema getSchema() {
            if (schema == null && data != null && !data.isEmpty()) {
                Map<String, Object> sample = data.get(0);
                List<FieldDefinition> fields = new ArrayList<>();
                for (String key : sample.keySet()) {
                    fields.add(FieldDefinition.builder()
                            .name(key)
                            .type(inferType(sample.get(key)))
                            .nullable(true)
                            .build());
                }
                schema = DataSchema.builder().fields(fields).build();
            }
            return schema;
        }

        private DataType inferType(Object value) {
            if (value == null) return DataType.STRING;
            if (value instanceof Integer) return DataType.INTEGER;
            if (value instanceof Long) return DataType.LONG;
            if (value instanceof Double || value instanceof Float) return DataType.DOUBLE;
            if (value instanceof Boolean) return DataType.BOOLEAN;
            if (value instanceof Map) return DataType.JSON;
            if (value instanceof List) return DataType.ARRAY;
            return DataType.STRING;
        }

        @Override
        public Iterator<DataRecord> read() {
            return data.stream()
                    .map(map -> new DataRecord(new LinkedHashMap<>(map)))
                    .iterator();
        }

        @Override
        public long estimateCount() {
            return data != null ? data.size() : -1;
        }

        @Override
        public void close() {
            data = null;
        }
    }

    /**
     * JSON 数据写入器
     */
    @Slf4j
    static class JsonDataWriter implements DataWriter {
        private final Map<String, Object> config;
        private final ObjectMapper objectMapper = new ObjectMapper();
        private DataSchema schema;
        private List<Map<String, Object>> buffer = new ArrayList<>();

        JsonDataWriter(Map<String, Object> config) {
            this.config = config;
            boolean prettyPrint = (Boolean) config.getOrDefault("prettyPrint", true);
            if (prettyPrint) {
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            }
        }

        @Override
        public void setSchema(DataSchema schema) {
            this.schema = schema;
        }

        @Override
        public void open() {
            buffer.clear();
        }

        @Override
        public void write(DataRecord record) {
            buffer.add(new LinkedHashMap<>(record.getFields()));
        }

        @Override
        public void write(Iterator<DataRecord> records) {
            while (records.hasNext()) {
                write(records.next());
            }
        }

        @Override
        public void commit() {
            String filePath = (String) config.get("filePath");
            try {
                objectMapper.writeValue(new File(filePath), buffer);
                log.info("Written {} records to JSON file: {}", buffer.size(), filePath);
            } catch (IOException e) {
                throw new ConnectorException("Failed to write JSON file: " + filePath, e);
            }
        }

        @Override
        public void rollback() {
            buffer.clear();
        }

        @Override
        public void close() {
            buffer = null;
        }

        @Override
        public long getWrittenCount() {
            return buffer != null ? buffer.size() : 0;
        }
    }
}
