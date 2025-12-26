// Pipeline 相关类型

export interface Pipeline {
  id: string;
  name: string;
  description?: string;
  nodes: PipelineNode[];
  edges: PipelineEdge[];
  variables?: Record<string, unknown>;
  status: 'DRAFT' | 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
  createdAt?: string;
  updatedAt?: string;
}

export interface PipelineNode {
  id: string;
  name: string;
  type: 'SOURCE' | 'TRANSFORMER' | 'TARGET';
  pluginType: string;
  config: Record<string, unknown>;
  position: {
    x: number;
    y: number;
  };
}

export interface PipelineEdge {
  id: string;
  sourceNodeId: string;
  targetNodeId: string;
  sourceHandle?: string;
  targetHandle?: string;
}

// Connector 相关类型
export interface ConnectorInfo {
  type: string;
  displayName: string;
  description: string;
  supportsRead: boolean;
  supportsWrite: boolean;
}

// Transformer 相关类型
export interface TransformerInfo {
  type: string;
  displayName: string;
  description: string;
  supportsMultipleInputs: boolean;
}

// Config Schema 类型
export interface ConfigSchema {
  fields: ConfigField[];
}

export interface ConfigField {
  name: string;
  label: string;
  description?: string;
  type: ConfigFieldType;
  required: boolean;
  defaultValue?: unknown;
  options?: Record<string, unknown>;
  validation?: ValidationRule;
}

export type ConfigFieldType =
  | 'STRING'
  | 'NUMBER'
  | 'INTEGER'
  | 'BOOLEAN'
  | 'PASSWORD'
  | 'TEXTAREA'
  | 'SELECT'
  | 'MULTI_SELECT'
  | 'JSON'
  | 'SQL'
  | 'FILE_PATH'
  | 'TABLE_SELECTOR'
  | 'COLUMN_MAPPING';

export interface ValidationRule {
  minLength?: number;
  maxLength?: number;
  min?: number;
  max?: number;
  pattern?: string;
  message?: string;
}

// 执行结果类型
export interface ExecutionResult {
  executionId: string;
  pipelineId: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  startTime: string;
  endTime?: string;
  totalRecordsProcessed: number;
  nodeResults: NodeResult[];
  errorMessage?: string;
}

export interface NodeResult {
  nodeId: string;
  nodeName: string;
  recordsRead: number;
  recordsWritten: number;
  durationMs: number;
  status: string;
  errorMessage?: string;
}
