package com.pipeline.connector.file;

import com.pipeline.core.connector.ConnectorException;
import com.pipeline.core.connector.DataWriter;
import com.pipeline.core.model.DataRecord;
import com.pipeline.core.model.DataSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;

/**
 * CSV 数据写入器
 */
@Slf4j
public class CsvDataWriter implements DataWriter {

    private final Map<String, Object> config;
    private DataSchema schema;
    private BufferedWriter writer;
    private CSVPrinter printer;
    private long writtenCount = 0;

    public CsvDataWriter(Map<String, Object> config) {
        this.config = config;
    }

    @Override
    public void setSchema(DataSchema schema) {
        this.schema = schema;
    }

    @Override
    public void open() {
        String filePath = (String) config.get("filePath");
        String encoding = (String) config.getOrDefault("encoding", "UTF-8");
        String delimiter = (String) config.getOrDefault("delimiter", ",");
        String writeMode = (String) config.getOrDefault("writeMode", "overwrite");
        boolean append = "append".equals(writeMode);

        try {
            File file = new File(filePath);
            boolean fileExists = file.exists();

            writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(filePath, append), Charset.forName(encoding)));

            CSVFormat.Builder formatBuilder = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter.charAt(0));

            // 如果是追加模式且文件已存在，不写入 header
            if (!append || !fileExists) {
                formatBuilder.setHeader(schema.getFieldNames().toArray(new String[0]));
            }

            printer = new CSVPrinter(writer, formatBuilder.build());
            log.info("Opened CSV file for writing: {} (mode: {})", filePath, writeMode);

        } catch (IOException e) {
            throw new ConnectorException("Failed to open CSV file for writing: " + filePath, e);
        }
    }

    @Override
    public void write(DataRecord record) {
        try {
            Object[] values = schema.getFieldNames().stream()
                    .map(record::get)
                    .toArray();
            printer.printRecord(values);
            writtenCount++;
        } catch (IOException e) {
            throw new ConnectorException("Failed to write record", e);
        }
    }

    @Override
    public void write(Iterator<DataRecord> records) {
        while (records.hasNext()) {
            write(records.next());
        }
    }

    @Override
    public void commit() {
        try {
            printer.flush();
            log.info("Written {} records to CSV", writtenCount);
        } catch (IOException e) {
            throw new ConnectorException("Failed to flush CSV writer", e);
        }
    }

    @Override
    public void rollback() {
        // CSV 不支持真正的回滚
        log.warn("CSV writer does not support rollback");
    }

    @Override
    public void close() {
        try {
            if (printer != null) printer.close();
            if (writer != null) writer.close();
        } catch (IOException e) {
            log.warn("Failed to close CSV writer", e);
        }
    }

    @Override
    public long getWrittenCount() {
        return writtenCount;
    }
}
