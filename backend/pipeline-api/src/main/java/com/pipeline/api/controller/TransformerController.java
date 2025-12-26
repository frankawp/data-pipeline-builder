package com.pipeline.api.controller;

import com.pipeline.core.registry.TransformerRegistry;
import com.pipeline.core.schema.ConfigSchema;
import com.pipeline.core.transformer.Transformer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/transformers")
@RequiredArgsConstructor
@Tag(name = "Transformer", description = "Transformer management APIs")
@CrossOrigin(origins = "*")
public class TransformerController {

    private final TransformerRegistry transformerRegistry;

    @GetMapping
    @Operation(summary = "Get all available transformers")
    public ResponseEntity<Collection<TransformerInfo>> getAllTransformers() {
        Collection<TransformerInfo> transformers = transformerRegistry.getAll().stream()
                .map(t -> new TransformerInfo(
                        t.getType(),
                        t.getDisplayName(),
                        t.getDescription(),
                        t.supportsMultipleInputs()
                ))
                .toList();
        return ResponseEntity.ok(transformers);
    }

    @GetMapping("/{type}/schema")
    @Operation(summary = "Get transformer configuration schema")
    public ResponseEntity<ConfigSchema> getTransformerSchema(@PathVariable String type) {
        Transformer transformer = transformerRegistry.get(type)
                .orElseThrow(() -> new RuntimeException("Transformer not found: " + type));
        return ResponseEntity.ok(transformer.getConfigSchema());
    }

    public record TransformerInfo(
            String type,
            String displayName,
            String description,
            boolean supportsMultipleInputs
    ) {}
}
