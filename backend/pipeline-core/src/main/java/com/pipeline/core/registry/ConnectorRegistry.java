package com.pipeline.core.registry;

import com.pipeline.core.connector.Connector;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connector 注册中心
 */
@Component
public class ConnectorRegistry {

    private final Map<String, Connector> connectors = new ConcurrentHashMap<>();

    /**
     * 注册连接器
     */
    public void register(Connector connector) {
        connectors.put(connector.getType(), connector);
    }

    /**
     * 获取连接器
     */
    public Optional<Connector> get(String type) {
        return Optional.ofNullable(connectors.get(type));
    }

    /**
     * 获取所有连接器
     */
    public Collection<Connector> getAll() {
        return Collections.unmodifiableCollection(connectors.values());
    }

    /**
     * 获取所有连接器类型
     */
    public Set<String> getTypes() {
        return Collections.unmodifiableSet(connectors.keySet());
    }

    /**
     * 自动注册所有 Connector Bean
     */
    public void registerAll(List<Connector> connectorList) {
        connectorList.forEach(this::register);
    }
}
