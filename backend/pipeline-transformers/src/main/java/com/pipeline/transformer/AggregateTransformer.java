package com.pipeline.transformer;

import com.pipeline.core.model.DataRecord;
import com.pipeline.core.model.DataSchema;
import com.pipeline.core.model.DataSchema.DataType;
import com.pipeline.core.model.DataSchema.FieldDefinition;
import com.pipeline.core.schema.ConfigSchema;
import com.pipeline.core.schema.ConfigSchema.ConfigField;
import com.pipeline.core.schema.ConfigSchema.FieldType;
import com.pipeline.core.transformer.Transformer;
import com.pipeline.core.transformer.TransformerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 聚合转换器 - GROUP BY 和聚合函数
 */
@Slf4j
@Component
public class AggregateTransformer implements Transformer {

    @Override
    public String getType() {
        return "aggregate";
    }

    @Override
    public String getDisplayName() {
        return "Aggregate";
    }

    @Override
    public String getDescription() {
        return "Group by fields and apply aggregate functions (SUM, COUNT, AVG, MIN, MAX)";
    }

    @Override
    public ConfigSchema getConfigSchema() {
        return ConfigSchema.builder()
                .fields(List.of(
                        ConfigField.builder()
                                .name("groupBy")
                                .label("分组字段")
                                .type(FieldType.MULTI_SELECT)
                                .required(false)
                                .description("用于分组的字段列表")
                                .build(),
                        ConfigField.builder()
                                .name("aggregations")
                                .label("聚合配置")
                                .type(FieldType.JSON)
                                .required(true)
                                .description("聚合函数配置: [{\"field\": \"amount\", \"function\": \"SUM\", \"alias\": \"total_amount\"}]")
                                .build()
                ))
                .build();
    }

    @Override
    public void validate(Map<String, Object> config) throws TransformerException {
        Object aggregations = config.get("aggregations");
        if (aggregations == null) {
            throw new TransformerException("Aggregations configuration is required");
        }
    }

    @Override
    public DataSchema getOutputSchema(DataSchema inputSchema, Map<String, Object> config) {
        List<String> groupBy = (List<String>) config.getOrDefault("groupBy", Collections.emptyList());
        List<Map<String, String>> aggregations = (List<Map<String, String>>) config.get("aggregations");

        List<FieldDefinition> fields = new ArrayList<>();

        // 添加分组字段
        for (String field : groupBy) {
            inputSchema.getField(field).ifPresent(fields::add);
        }

        // 添加聚合字段
        for (Map<String, String> agg : aggregations) {
            String alias = agg.getOrDefault("alias", agg.get("field") + "_" + agg.get("function").toLowerCase());
            fields.add(FieldDefinition.builder()
                    .name(alias)
                    .type(DataType.DOUBLE)
                    .nullable(false)
                    .build());
        }

        return DataSchema.builder().fields(fields).build();
    }

    @Override
    public Iterator<DataRecord> transform(Iterator<DataRecord> input, Map<String, Object> config) {
        List<String> groupBy = (List<String>) config.getOrDefault("groupBy", Collections.emptyList());
        List<Map<String, String>> aggregations = (List<Map<String, String>>) config.get("aggregations");

        // 收集所有数据进行分组
        List<DataRecord> allRecords = new ArrayList<>();
        input.forEachRemaining(allRecords::add);

        if (groupBy.isEmpty()) {
            // 无分组，整体聚合
            DataRecord result = aggregateGroup(allRecords, aggregations, Collections.emptyMap());
            return Collections.singletonList(result).iterator();
        }

        // 按分组键分组
        Map<String, List<DataRecord>> groups = allRecords.stream()
                .collect(Collectors.groupingBy(record -> buildGroupKey(record, groupBy)));

        List<DataRecord> results = new ArrayList<>();
        for (Map.Entry<String, List<DataRecord>> entry : groups.entrySet()) {
            List<DataRecord> groupRecords = entry.getValue();
            Map<String, Object> groupValues = extractGroupValues(groupRecords.get(0), groupBy);
            results.add(aggregateGroup(groupRecords, aggregations, groupValues));
        }

        return results.iterator();
    }

    private String buildGroupKey(DataRecord record, List<String> groupBy) {
        return groupBy.stream()
                .map(field -> String.valueOf(record.get(field)))
                .collect(Collectors.joining("|"));
    }

    private Map<String, Object> extractGroupValues(DataRecord record, List<String> groupBy) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String field : groupBy) {
            values.put(field, record.get(field));
        }
        return values;
    }

    private DataRecord aggregateGroup(
            List<DataRecord> records,
            List<Map<String, String>> aggregations,
            Map<String, Object> groupValues) {

        Map<String, Object> result = new LinkedHashMap<>(groupValues);

        for (Map<String, String> agg : aggregations) {
            String field = agg.get("field");
            String function = agg.get("function").toUpperCase();
            String alias = agg.getOrDefault("alias", field + "_" + function.toLowerCase());

            Object aggregatedValue = applyAggregation(records, field, function);
            result.put(alias, aggregatedValue);
        }

        return new DataRecord(result);
    }

    private Object applyAggregation(List<DataRecord> records, String field, String function) {
        List<Double> values = records.stream()
                .map(r -> r.get(field))
                .filter(Objects::nonNull)
                .map(v -> {
                    if (v instanceof Number) {
                        return ((Number) v).doubleValue();
                    }
                    try {
                        return Double.parseDouble(v.toString());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (values.isEmpty()) {
            return function.equals("COUNT") ? 0L : null;
        }

        return switch (function) {
            case "SUM" -> values.stream().mapToDouble(Double::doubleValue).sum();
            case "AVG" -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            case "MIN" -> values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            case "MAX" -> values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            case "COUNT" -> (long) records.size();
            default -> throw new TransformerException("Unknown aggregation function: " + function);
        };
    }
}
