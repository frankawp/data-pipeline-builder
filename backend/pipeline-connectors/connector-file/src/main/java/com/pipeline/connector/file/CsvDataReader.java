package com.pipeline.connector.file;

import com.pipeline.core.connector.ConnectorException;
import com.pipeline.core.connector.DataReader;
import com.pipeline.core.model.DataRecord;
import com.pipeline.core.model.DataSchema;
import com.pipeline.core.model.DataSchema.DataType;
import com.pipeline.core.model.DataSchema.FieldDefinition;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * CSV 数据读取器
 */
@Slf4j
public class CsvDataReader implements DataReader {

    private final Map<String, Object> config;
    private BufferedReader reader;
    private CSVParser parser;
    private DataSchema schema;

    public CsvDataReader(Map<String, Object> config) {
        this.config = config;
    }

    @Override
    public void open() {
        String filePath = (String) config.get("filePath");
        String encoding = (String) config.getOrDefault("encoding", "UTF-8");
        String delimiter = (String) config.getOrDefault("delimiter", ",");
        boolean hasHeader = (Boolean) config.getOrDefault("hasHeader", true);

        try {
            reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filePath), Charset.forName(encoding)));

            CSVFormat.Builder formatBuilder = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter.charAt(0))
                    .setIgnoreEmptyLines(true)
                    .setTrim(true);

            if (hasHeader) {
                formatBuilder.setHeader().setSkipHeaderRecord(true);
            }

            parser = new CSVParser(reader, formatBuilder.build());
            log.info("Opened CSV file: {}", filePath);

        } catch (IOException e) {
            throw new ConnectorException("Failed to open CSV file: " + filePath, e);
        }
    }

    @Override
    public DataSchema getSchema() {
        if (schema == null) {
            // 从 header 推断 schema
            List<String> headers = parser.getHeaderNames();
            List<FieldDefinition> fields = new ArrayList<>();
            for (String header : headers) {
                fields.add(FieldDefinition.builder()
                        .name(header)
                        .type(DataType.STRING)  // CSV 默认都是字符串
                        .nullable(true)
                        .build());
            }
            schema = DataSchema.builder().fields(fields).build();
        }
        return schema;
    }

    @Override
    public Iterator<DataRecord> read() {
        return new CsvRecordIterator(parser.iterator(), getSchema());
    }

    @Override
    public long estimateCount() {
        // CSV 无法高效估计行数，返回 -1
        return -1;
    }

    @Override
    public void close() {
        try {
            if (parser != null) parser.close();
            if (reader != null) reader.close();
        } catch (IOException e) {
            log.warn("Failed to close CSV reader", e);
        }
    }

    private static class CsvRecordIterator implements Iterator<DataRecord> {
        private final Iterator<CSVRecord> csvIterator;
        private final DataSchema schema;

        CsvRecordIterator(Iterator<CSVRecord> csvIterator, DataSchema schema) {
            this.csvIterator = csvIterator;
            this.schema = schema;
        }

        @Override
        public boolean hasNext() {
            return csvIterator.hasNext();
        }

        @Override
        public DataRecord next() {
            CSVRecord csvRecord = csvIterator.next();
            Map<String, Object> fields = new LinkedHashMap<>();
            for (FieldDefinition field : schema.getFields()) {
                String value = csvRecord.get(field.getName());
                fields.put(field.getName(), value);
            }
            return new DataRecord(fields);
        }
    }
}
