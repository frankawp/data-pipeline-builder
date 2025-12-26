import { create } from 'zustand';
import type { Pipeline, PipelineNode, PipelineEdge, ConnectorInfo, TransformerInfo } from '../types';

interface PipelineState {
  // 当前编辑的 Pipeline
  currentPipeline: Pipeline | null;
  nodes: PipelineNode[];
  edges: PipelineEdge[];

  // 可用的连接器和转换器
  connectors: ConnectorInfo[];
  transformers: TransformerInfo[];

  // 选中的节点
  selectedNodeId: string | null;

  // Actions
  setCurrentPipeline: (pipeline: Pipeline | null) => void;
  setNodes: (nodes: PipelineNode[]) => void;
  setEdges: (edges: PipelineEdge[]) => void;
  addNode: (node: PipelineNode) => void;
  updateNode: (id: string, updates: Partial<PipelineNode>) => void;
  removeNode: (id: string) => void;
  addEdge: (edge: PipelineEdge) => void;
  removeEdge: (id: string) => void;
  setSelectedNodeId: (id: string | null) => void;
  setConnectors: (connectors: ConnectorInfo[]) => void;
  setTransformers: (transformers: TransformerInfo[]) => void;
  getPipelineData: () => Partial<Pipeline>;
}

export const usePipelineStore = create<PipelineState>((set, get) => ({
  currentPipeline: null,
  nodes: [],
  edges: [],
  connectors: [],
  transformers: [],
  selectedNodeId: null,

  setCurrentPipeline: (pipeline) => {
    set({
      currentPipeline: pipeline,
      nodes: pipeline?.nodes || [],
      edges: pipeline?.edges || [],
    });
  },

  setNodes: (nodes) => set({ nodes }),
  setEdges: (edges) => set({ edges }),

  addNode: (node) => set((state) => ({
    nodes: [...state.nodes, node]
  })),

  updateNode: (id, updates) => set((state) => ({
    nodes: state.nodes.map((node) =>
      node.id === id ? { ...node, ...updates } : node
    ),
  })),

  removeNode: (id) => set((state) => ({
    nodes: state.nodes.filter((node) => node.id !== id),
    edges: state.edges.filter(
      (edge) => edge.sourceNodeId !== id && edge.targetNodeId !== id
    ),
    selectedNodeId: state.selectedNodeId === id ? null : state.selectedNodeId,
  })),

  addEdge: (edge) => set((state) => ({
    edges: [...state.edges, edge]
  })),

  removeEdge: (id) => set((state) => ({
    edges: state.edges.filter((edge) => edge.id !== id)
  })),

  setSelectedNodeId: (id) => set({ selectedNodeId: id }),

  setConnectors: (connectors) => set({ connectors }),
  setTransformers: (transformers) => set({ transformers }),

  getPipelineData: () => {
    const state = get();
    return {
      ...state.currentPipeline,
      nodes: state.nodes,
      edges: state.edges,
    };
  },
}));
