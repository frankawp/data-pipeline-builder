import React, { useCallback, useEffect, useMemo } from 'react';
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  addEdge,
  Connection,
  Node,
  Edge,
  BackgroundVariant,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { usePipelineStore } from '../../store/pipelineStore';
import CustomNode from './CustomNode';
import type { PipelineNode, PipelineEdge } from '../../types';

// 自定义节点类型
const nodeTypes = {
  pipelineNode: CustomNode,
};

// 将 PipelineNode 转换为 ReactFlow Node
const toReactFlowNode = (node: PipelineNode): Node => ({
  id: node.id,
  type: 'pipelineNode',
  position: node.position,
  data: { ...node, label: node.name },
});

// 将 PipelineEdge 转换为 ReactFlow Edge
const toReactFlowEdge = (edge: PipelineEdge): Edge => ({
  id: edge.id,
  source: edge.sourceNodeId,
  target: edge.targetNodeId,
  sourceHandle: edge.sourceHandle,
  targetHandle: edge.targetHandle,
  type: 'smoothstep',
  animated: true,
  style: { stroke: '#1890ff', strokeWidth: 2 },
});

const PipelineEditor: React.FC = () => {
  const {
    nodes: pipelineNodes,
    edges: pipelineEdges,
    setNodes: setPipelineNodes,
    setEdges: setPipelineEdges,
    setSelectedNodeId,
  } = usePipelineStore();

  // ReactFlow 状态
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

  // 同步 store 中的节点到 ReactFlow
  useEffect(() => {
    setNodes(pipelineNodes.map(toReactFlowNode));
  }, [pipelineNodes, setNodes]);

  // 同步 store 中的边到 ReactFlow
  useEffect(() => {
    setEdges(pipelineEdges.map(toReactFlowEdge));
  }, [pipelineEdges, setEdges]);

  // 处理连接
  const onConnect = useCallback(
    (connection: Connection) => {
      if (!connection.source || !connection.target) return;

      const newEdge: PipelineEdge = {
        id: `edge-${connection.source}-${connection.target}`,
        sourceNodeId: connection.source,
        targetNodeId: connection.target,
        sourceHandle: connection.sourceHandle || undefined,
        targetHandle: connection.targetHandle || undefined,
      };

      setPipelineEdges([...pipelineEdges, newEdge]);
    },
    [pipelineEdges, setPipelineEdges]
  );

  // 处理节点拖拽结束
  const onNodeDragStop = useCallback(
    (_: React.MouseEvent, node: Node) => {
      const updatedNodes = pipelineNodes.map((n) =>
        n.id === node.id ? { ...n, position: node.position } : n
      );
      setPipelineNodes(updatedNodes);
    },
    [pipelineNodes, setPipelineNodes]
  );

  // 处理节点选中
  const onNodeClick = useCallback(
    (_: React.MouseEvent, node: Node) => {
      setSelectedNodeId(node.id);
    },
    [setSelectedNodeId]
  );

  // 处理画布点击（取消选中）
  const onPaneClick = useCallback(() => {
    setSelectedNodeId(null);
  }, [setSelectedNodeId]);

  // MiniMap 节点颜色
  const nodeColor = useCallback((node: Node) => {
    const data = node.data as PipelineNode;
    switch (data.type) {
      case 'SOURCE':
        return '#1890ff';
      case 'TRANSFORMER':
        return '#fa8c16';
      case 'TARGET':
        return '#52c41a';
      default:
        return '#999';
    }
  }, []);

  return (
    <div style={{ width: '100%', height: '100%' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onNodeDragStop={onNodeDragStop}
        onNodeClick={onNodeClick}
        onPaneClick={onPaneClick}
        nodeTypes={nodeTypes}
        fitView
        snapToGrid
        snapGrid={[15, 15]}
        defaultEdgeOptions={{
          type: 'smoothstep',
          animated: true,
        }}
      >
        <Background variant={BackgroundVariant.Dots} gap={20} size={1} />
        <Controls />
        <MiniMap nodeColor={nodeColor} zoomable pannable />
      </ReactFlow>
    </div>
  );
};

export default PipelineEditor;
