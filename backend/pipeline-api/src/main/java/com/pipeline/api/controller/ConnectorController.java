package com.pipeline.api.controller;

import com.pipeline.core.connector.Connector;
import com.pipeline.core.registry.ConnectorRegistry;
import com.pipeline.core.schema.ConfigSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api/connectors")
@RequiredArgsConstructor
@Tag(name = "Connector", description = "Connector management APIs")
@CrossOrigin(origins = "*")
public class ConnectorController {

    private final ConnectorRegistry connectorRegistry;

    @GetMapping
    @Operation(summary = "Get all available connectors")
    public ResponseEntity<Collection<ConnectorInfo>> getAllConnectors() {
        Collection<ConnectorInfo> connectors = connectorRegistry.getAll().stream()
                .map(c -> new ConnectorInfo(
                        c.getType(),
                        c.getDisplayName(),
                        c.getDescription(),
                        c.supportsRead(),
                        c.supportsWrite()
                ))
                .toList();
        return ResponseEntity.ok(connectors);
    }

    @GetMapping("/{type}/schema")
    @Operation(summary = "Get connector configuration schema")
    public ResponseEntity<ConfigSchema> getConnectorSchema(@PathVariable String type) {
        Connector connector = connectorRegistry.get(type)
                .orElseThrow(() -> new RuntimeException("Connector not found: " + type));
        return ResponseEntity.ok(connector.getConfigSchema());
    }

    @PostMapping("/test")
    @Operation(summary = "Test connector connection")
    public ResponseEntity<TestResult> testConnection(@RequestBody TestRequest request) {
        Connector connector = connectorRegistry.get(request.type())
                .orElseThrow(() -> new RuntimeException("Connector not found: " + request.type()));

        try {
            connector.validate(request.config());
            boolean success = connector.testConnection(request.config());
            return ResponseEntity.ok(new TestResult(success, success ? "Connection successful" : "Connection failed"));
        } catch (Exception e) {
            return ResponseEntity.ok(new TestResult(false, e.getMessage()));
        }
    }

    public record ConnectorInfo(
            String type,
            String displayName,
            String description,
            boolean supportsRead,
            boolean supportsWrite
    ) {}

    public record TestRequest(String type, Map<String, Object> config) {}

    public record TestResult(boolean success, String message) {}
}
