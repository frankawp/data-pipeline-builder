package com.pipeline.core.engine;

import com.pipeline.core.connector.Connector;
import com.pipeline.core.connector.DataReader;
import com.pipeline.core.connector.DataWriter;
import com.pipeline.core.model.*;
import com.pipeline.core.registry.ConnectorRegistry;
import com.pipeline.core.registry.TransformerRegistry;
import com.pipeline.core.transformer.Transformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Pipeline 执行引擎
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineExecutor {

    private final ConnectorRegistry connectorRegistry;
    private final TransformerRegistry transformerRegistry;

    /**
     * 执行 Pipeline
     */
    public ExecutionResult execute(Pipeline pipeline) {
        ExecutionContext context = ExecutionContext.builder()
                .pipelineId(pipeline.getId())
                .status(ExecutionContext.ExecutionStatus.RUNNING)
                .build();

        ExecutionResult.ExecutionResultBuilder resultBuilder = ExecutionResult.builder()
                .executionId(context.getExecutionId())
                .pipelineId(pipeline.getId())
                .startTime(context.getStartTime());

        List<ExecutionResult.NodeResult> nodeResults = new ArrayList<>();
        long totalRecords = 0;

        try {
            // 拓扑排序获取执行顺序
            List<Node> executionOrder = topologicalSort(pipeline);
            log.info("Pipeline {} execution order: {}", pipeline.getId(),
                    executionOrder.stream().map(Node::getName).toList());

            // 存储每个节点的输出数据
            Map<String, Iterator<DataRecord>> nodeOutputs = new HashMap<>();

            for (Node node : executionOrder) {
                long startTime = System.currentTimeMillis();
                ExecutionResult.NodeResult.NodeResultBuilder nodeResultBuilder =
                        ExecutionResult.NodeResult.builder()
                                .nodeId(node.getId())
                                .nodeName(node.getName());

                try {
                    Iterator<DataRecord> output = executeNode(node, pipeline, nodeOutputs, context);
                    if (output != null) {
                        // 对于非目标节点，需要缓存数据供下游使用
                        if (node.getType() != NodeType.TARGET) {
                            List<DataRecord> cachedData = new ArrayList<>();
                            while (output.hasNext()) {
                                cachedData.add(output.next());
                            }
                            nodeOutputs.put(node.getId(), cachedData.iterator());
                            nodeResultBuilder.recordsRead(cachedData.size());
                            totalRecords += cachedData.size();
                        }
                    }
                    nodeResultBuilder.status(ExecutionContext.ExecutionStatus.COMPLETED);
                } catch (Exception e) {
                    log.error("Node {} execution failed", node.getId(), e);
                    nodeResultBuilder.status(ExecutionContext.ExecutionStatus.FAILED)
                            .errorMessage(e.getMessage());
                    throw e;
                } finally {
                    nodeResultBuilder.durationMs(System.currentTimeMillis() - startTime);
                    nodeResults.add(nodeResultBuilder.build());
                }
            }

            context.setStatus(ExecutionContext.ExecutionStatus.COMPLETED);
            context.setEndTime(LocalDateTime.now());

            return resultBuilder
                    .status(ExecutionContext.ExecutionStatus.COMPLETED)
                    .endTime(LocalDateTime.now())
                    .totalRecordsProcessed(totalRecords)
                    .nodeResults(nodeResults)
                    .build();

        } catch (Exception e) {
            log.error("Pipeline {} execution failed", pipeline.getId(), e);
            return resultBuilder
                    .status(ExecutionContext.ExecutionStatus.FAILED)
                    .endTime(LocalDateTime.now())
                    .totalRecordsProcessed(totalRecords)
                    .nodeResults(nodeResults)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 执行单个节点
     */
    private Iterator<DataRecord> executeNode(
            Node node,
            Pipeline pipeline,
            Map<String, Iterator<DataRecord>> nodeOutputs,
            ExecutionContext context) {

        log.info("Executing node: {} ({})", node.getName(), node.getPluginType());

        switch (node.getType()) {
            case SOURCE:
                return executeSourceNode(node);
            case TRANSFORMER:
                return executeTransformerNode(node, pipeline, nodeOutputs);
            case TARGET:
                executeTargetNode(node, pipeline, nodeOutputs);
                return null;
            default:
                throw new IllegalArgumentException("Unknown node type: " + node.getType());
        }
    }

    /**
     * 执行数据源节点
     */
    private Iterator<DataRecord> executeSourceNode(Node node) {
        Connector connector = connectorRegistry.get(node.getPluginType())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown connector type: " + node.getPluginType()));

        DataReader reader = connector.createReader(node.getConfig());
        reader.open();
        return reader.read();
    }

    /**
     * 执行转换器节点
     */
    private Iterator<DataRecord> executeTransformerNode(
            Node node,
            Pipeline pipeline,
            Map<String, Iterator<DataRecord>> nodeOutputs) {

        Transformer transformer = transformerRegistry.get(node.getPluginType())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown transformer type: " + node.getPluginType()));

        // 获取上游节点的输出
        List<Edge> incomingEdges = pipeline.getEdgesToNode(node.getId());
        if (incomingEdges.isEmpty()) {
            throw new IllegalStateException("Transformer node has no input: " + node.getId());
        }

        // 简单情况：单输入
        if (incomingEdges.size() == 1) {
            String sourceNodeId = incomingEdges.get(0).getSourceNodeId();
            Iterator<DataRecord> input = nodeOutputs.get(sourceNodeId);
            if (input == null) {
                throw new IllegalStateException("No data from source node: " + sourceNodeId);
            }
            // 重新包装 iterator（因为已经被消费过）
            List<DataRecord> inputList = new ArrayList<>();
            input.forEachRemaining(inputList::add);
            nodeOutputs.put(sourceNodeId, inputList.iterator()); // 重置
            return transformer.transform(inputList.iterator(), node.getConfig());
        }

        // 多输入情况
        if (!transformer.supportsMultipleInputs()) {
            throw new IllegalStateException(
                    "Transformer " + node.getPluginType() + " does not support multiple inputs");
        }

        Map<String, Iterator<DataRecord>> inputs = new HashMap<>();
        for (Edge edge : incomingEdges) {
            inputs.put(edge.getSourceNodeId(), nodeOutputs.get(edge.getSourceNodeId()));
        }
        return transformer.transform(inputs, node.getConfig());
    }

    /**
     * 执行目标节点
     */
    private void executeTargetNode(
            Node node,
            Pipeline pipeline,
            Map<String, Iterator<DataRecord>> nodeOutputs) {

        Connector connector = connectorRegistry.get(node.getPluginType())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown connector type: " + node.getPluginType()));

        // 获取上游节点的输出
        List<Edge> incomingEdges = pipeline.getEdgesToNode(node.getId());
        if (incomingEdges.isEmpty()) {
            throw new IllegalStateException("Target node has no input: " + node.getId());
        }

        String sourceNodeId = incomingEdges.get(0).getSourceNodeId();
        Iterator<DataRecord> input = nodeOutputs.get(sourceNodeId);

        DataWriter writer = connector.createWriter(node.getConfig());
        try {
            writer.open();
            writer.write(input);
            writer.commit();
            log.info("Target node {} wrote {} records", node.getName(), writer.getWrittenCount());
        } catch (Exception e) {
            writer.rollback();
            throw e;
        } finally {
            writer.close();
        }
    }

    /**
     * 拓扑排序 - 确定节点执行顺序
     */
    private List<Node> topologicalSort(Pipeline pipeline) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjList = new HashMap<>();

        // 初始化
        for (Node node : pipeline.getNodes()) {
            inDegree.put(node.getId(), 0);
            adjList.put(node.getId(), new ArrayList<>());
        }

        // 构建邻接表和入度
        for (Edge edge : pipeline.getEdges()) {
            adjList.get(edge.getSourceNodeId()).add(edge.getTargetNodeId());
            inDegree.merge(edge.getTargetNodeId(), 1, Integer::sum);
        }

        // BFS 拓扑排序
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<Node> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            result.add(pipeline.getNodeById(nodeId));

            for (String neighbor : adjList.get(nodeId)) {
                inDegree.merge(neighbor, -1, Integer::sum);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        if (result.size() != pipeline.getNodes().size()) {
            throw new IllegalStateException("Pipeline contains cycle");
        }

        return result;
    }
}
