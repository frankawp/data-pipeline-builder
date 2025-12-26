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
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.StreamSupport;

/**
 * 映射转换器 - 字段映射、重命名、计算新字段
 */
@Slf4j
@Component
public class MapTransformer implements Transformer {

    private final ExpressionParser parser = new SpelExpressionParser();

    @Override
    public String getType() {
        return "map";
    }

    @Override
    public String getDisplayName() {
        return "Map / Transform";
    }

    @Override
    public String getDescription() {
        return "Map, rename, or compute new fields";
    }

    @Override
    public ConfigSchema getConfigSchema() {
        return ConfigSchema.builder()
                .fields(List.of(
                        ConfigField.builder()
                                .name("mappings")
                                .label("字段映射")
                                .type(FieldType.COLUMN_MAPPING)
                                .required(true)
                                .description("定义输出字段，格式: [{\"source\": \"原字段\", \"target\": \"新字段\", \"expression\": \"可选的SpEL表达式\"}]")
                                .build(),
                        ConfigField.builder()
                                .name("keepUnmapped")
                                .label("保留未映射字段")
                                .type(FieldType.BOOLEAN)
                                .required(false)
                                .defaultValue(false)
                                .build()
                ))
                .build();
    }

    @Override
    public void validate(Map<String, Object> config) throws TransformerException {
        Object mappings = config.get("mappings");
        if (mappings == null) {
            throw new TransformerException("Mappings configuration is required");
        }
        if (!(mappings instanceof List)) {
            throw new TransformerException("Mappings must be a list");
        }
    }

    @Override
    public DataSchema getOutputSchema(DataSchema inputSchema, Map<String, Object> config) {
        List<Map<String, String>> mappings = (List<Map<String, String>>) config.get("mappings");
        boolean keepUnmapped = (Boolean) config.getOrDefault("keepUnmapped", false);

        List<FieldDefinition> outputFields = new ArrayList<>();

        // 添加映射的字段
        for (Map<String, String> mapping : mappings) {
            String target = mapping.getOrDefault("target", mapping.get("source"));
            String source = mapping.get("source");

            // 尝试从输入 schema 获取类型
            DataType type = DataType.STRING;
            if (source != null) {
                type = inputSchema.getField(source)
                        .map(FieldDefinition::getType)
                        .orElse(DataType.STRING);
            }

            outputFields.add(FieldDefinition.builder()
                    .name(target)
                    .type(type)
                    .nullable(true)
                    .build());
        }

        // 如果保留未映射字段
        if (keepUnmapped) {
            Set<String> mappedSources = new HashSet<>();
            for (Map<String, String> mapping : mappings) {
                mappedSources.add(mapping.get("source"));
            }

            for (FieldDefinition field : inputSchema.getFields()) {
                if (!mappedSources.contains(field.getName())) {
                    outputFields.add(field);
                }
            }
        }

        return DataSchema.builder().fields(outputFields).build();
    }

    @Override
    public Iterator<DataRecord> transform(Iterator<DataRecord> input, Map<String, Object> config) {
        List<Map<String, String>> mappings = (List<Map<String, String>>) config.get("mappings");
        boolean keepUnmapped = (Boolean) config.getOrDefault("keepUnmapped", false);

        // 预编译表达式
        Map<String, Expression> expressions = new HashMap<>();
        for (Map<String, String> mapping : mappings) {
            String expression = mapping.get("expression");
            if (expression != null && !expression.isBlank()) {
                expressions.put(mapping.getOrDefault("target", mapping.get("source")),
                        parser.parseExpression(expression));
            }
        }

        Iterable<DataRecord> iterable = () -> input;
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(record -> transformRecord(record, mappings, expressions, keepUnmapped))
                .iterator();
    }

    private DataRecord transformRecord(
            DataRecord record,
            List<Map<String, String>> mappings,
            Map<String, Expression> expressions,
            boolean keepUnmapped) {

        Map<String, Object> output = new LinkedHashMap<>();
        Set<String> mappedSources = new HashSet<>();

        EvaluationContext context = createContext(record);

        for (Map<String, String> mapping : mappings) {
            String source = mapping.get("source");
            String target = mapping.getOrDefault("target", source);
            mappedSources.add(source);

            Expression expression = expressions.get(target);
            Object value;

            if (expression != null) {
                try {
                    value = expression.getValue(context);
                } catch (Exception e) {
                    log.warn("Failed to evaluate expression for field {}: {}", target, e.getMessage());
                    value = null;
                }
            } else if (source != null) {
                value = record.get(source);
            } else {
                value = null;
            }

            output.put(target, value);
        }

        // 保留未映射字段
        if (keepUnmapped) {
            for (Map.Entry<String, Object> entry : record.getFields().entrySet()) {
                if (!mappedSources.contains(entry.getKey()) && !output.containsKey(entry.getKey())) {
                    output.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return new DataRecord(output);
    }

    private EvaluationContext createContext(DataRecord record) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        for (Map.Entry<String, Object> entry : record.getFields().entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }
        return context;
    }
}
