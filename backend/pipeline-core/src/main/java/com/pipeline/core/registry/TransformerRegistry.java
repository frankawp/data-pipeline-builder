package com.pipeline.core.registry;

import com.pipeline.core.transformer.Transformer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transformer 注册中心
 */
@Component
public class TransformerRegistry {

    private final Map<String, Transformer> transformers = new ConcurrentHashMap<>();

    /**
     * 注册转换器
     */
    public void register(Transformer transformer) {
        transformers.put(transformer.getType(), transformer);
    }

    /**
     * 获取转换器
     */
    public Optional<Transformer> get(String type) {
        return Optional.ofNullable(transformers.get(type));
    }

    /**
     * 获取所有转换器
     */
    public Collection<Transformer> getAll() {
        return Collections.unmodifiableCollection(transformers.values());
    }

    /**
     * 获取所有转换器类型
     */
    public Set<String> getTypes() {
        return Collections.unmodifiableSet(transformers.keySet());
    }

    /**
     * 自动注册所有 Transformer Bean
     */
    public void registerAll(List<Transformer> transformerList) {
        transformerList.forEach(this::register);
    }
}
