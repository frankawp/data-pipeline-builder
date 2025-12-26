package com.pipeline.connector.file;

import com.pipeline.core.connector.*;
import com.pipeline.core.schema.ConfigSchema;
import com.pipeline.core.schema.ConfigSchema.ConfigField;
import com.pipeline.core.schema.ConfigSchema.FieldType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * CSV 文件连接器
 */
@Slf4j
@Component
public class CsvConnector implements Connector {

    @Override
    public String getType() {
        return "csv";
    }

    @Override
    public String getDisplayName() {
        return "CSV File";
    }

    @Override
    public String getDescription() {
        return "Read and write CSV files";
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
                                .name("delimiter")
                                .label("分隔符")
                                .type(FieldType.STRING)
                                .required(false)
                                .defaultValue(",")
                                .build(),
                        ConfigField.builder()
                                .name("hasHeader")
                                .label("包含表头")
                                .type(FieldType.BOOLEAN)
                                .required(false)
                                .defaultValue(true)
                                .build(),
                        ConfigField.builder()
                                .name("encoding")
                                .label("文件编码")
                                .type(FieldType.SELECT)
                                .required(false)
                                .defaultValue("UTF-8")
                                .options(Map.of(
                                        "options", List.of(
                                                Map.of("value", "UTF-8", "label", "UTF-8"),
                                                Map.of("value", "GBK", "label", "GBK"),
                                                Map.of("value", "ISO-8859-1", "label", "ISO-8859-1")
                                        )
                                ))
                                .build(),
                        ConfigField.builder()
                                .name("writeMode")
                                .label("写入模式")
                                .type(FieldType.SELECT)
                                .required(false)
                                .defaultValue("overwrite")
                                .options(Map.of(
                                        "options", List.of(
                                                Map.of("value", "overwrite", "label", "覆盖"),
                                                Map.of("value", "append", "label", "追加")
                                        )
                                ))
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
        // 对于读取，文件必须存在；对于写入，目录必须存在
        return file.exists() || (file.getParentFile() != null && file.getParentFile().exists());
    }

    @Override
    public DataReader createReader(Map<String, Object> config) {
        return new CsvDataReader(config);
    }

    @Override
    public DataWriter createWriter(Map<String, Object> config) {
        return new CsvDataWriter(config);
    }
}
