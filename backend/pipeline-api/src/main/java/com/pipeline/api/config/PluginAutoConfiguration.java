package com.pipeline.api.config;

import com.pipeline.core.connector.Connector;
import com.pipeline.core.registry.ConnectorRegistry;
import com.pipeline.core.registry.TransformerRegistry;
import com.pipeline.core.transformer.Transformer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 插件自动注册配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PluginAutoConfiguration {

    private final ConnectorRegistry connectorRegistry;
    private final TransformerRegistry transformerRegistry;
    private final List<Connector> connectors;
    private final List<Transformer> transformers;

    @PostConstruct
    public void registerPlugins() {
        // 注册所有 Connector
        connectorRegistry.registerAll(connectors);
        log.info("Registered {} connectors: {}", connectors.size(),
                connectors.stream().map(Connector::getType).toList());

        // 注册所有 Transformer
        transformerRegistry.registerAll(transformers);
        log.info("Registered {} transformers: {}", transformers.size(),
                transformers.stream().map(Transformer::getType).toList());
    }
}
