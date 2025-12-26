package com.pipeline.transformer;

import com.pipeline.core.model.DataRecord;
import com.pipeline.core.model.DataSchema;
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
 * 过滤转换器 - 根据条件过滤数据
 */
@Slf4j
@Component
public class FilterTransformer implements Transformer {

    private final ExpressionParser parser = new SpelExpressionParser();

    @Override
    public String getType() {
        return "filter";
    }

    @Override
    public String getDisplayName() {
        return "Filter";
    }

    @Override
    public String getDescription() {
        return "Filter records based on conditions";
    }

    @Override
    public ConfigSchema getConfigSchema() {
        return ConfigSchema.builder()
                .fields(List.of(
                        ConfigField.builder()
                                .name("condition")
                                .label("过滤条件")
                                .type(FieldType.STRING)
                                .required(true)
                                .description("SpEL 表达式，如: #age > 18 and #status == 'active'")
                                .build()
                ))
                .build();
    }

    @Override
    public void validate(Map<String, Object> config) throws TransformerException {
        String condition = (String) config.get("condition");
        if (condition == null || condition.isBlank()) {
            throw new TransformerException("Filter condition is required");
        }
        // 验证表达式语法
        try {
            parser.parseExpression(condition);
        } catch (Exception e) {
            throw new TransformerException("Invalid filter expression: " + e.getMessage());
        }
    }

    @Override
    public DataSchema getOutputSchema(DataSchema inputSchema, Map<String, Object> config) {
        // Filter 不改变 schema
        return inputSchema;
    }

    @Override
    public Iterator<DataRecord> transform(Iterator<DataRecord> input, Map<String, Object> config) {
        String condition = (String) config.get("condition");
        Expression expression = parser.parseExpression(condition);

        Iterable<DataRecord> iterable = () -> input;
        return StreamSupport.stream(iterable.spliterator(), false)
                .filter(record -> {
                    try {
                        EvaluationContext context = createContext(record);
                        Boolean result = expression.getValue(context, Boolean.class);
                        return Boolean.TRUE.equals(result);
                    } catch (Exception e) {
                        log.warn("Failed to evaluate filter condition for record: {}", e.getMessage());
                        return false;
                    }
                })
                .iterator();
    }

    private EvaluationContext createContext(DataRecord record) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        for (Map.Entry<String, Object> entry : record.getFields().entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }
        return context;
    }
}
