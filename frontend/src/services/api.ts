import axios from 'axios';
import type { Pipeline, ConnectorInfo, TransformerInfo, ConfigSchema, ExecutionResult } from '../types';

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
});

// Pipeline APIs
export const pipelineApi = {
  getAll: () => api.get<Pipeline[]>('/pipelines').then(res => res.data),

  getById: (id: string) => api.get<Pipeline>(`/pipelines/${id}`).then(res => res.data),

  create: (pipeline: Partial<Pipeline>) =>
    api.post<Pipeline>('/pipelines', pipeline).then(res => res.data),

  update: (id: string, pipeline: Partial<Pipeline>) =>
    api.put<Pipeline>(`/pipelines/${id}`, pipeline).then(res => res.data),

  delete: (id: string) => api.delete(`/pipelines/${id}`),

  execute: (id: string) =>
    api.post<ExecutionResult>(`/pipelines/${id}/execute`).then(res => res.data),

  getExecutions: (id: string) =>
    api.get(`/pipelines/${id}/executions`).then(res => res.data),
};

// Connector APIs
export const connectorApi = {
  getAll: () => api.get<ConnectorInfo[]>('/connectors').then(res => res.data),

  getSchema: (type: string) =>
    api.get<ConfigSchema>(`/connectors/${type}/schema`).then(res => res.data),

  testConnection: (type: string, config: Record<string, unknown>) =>
    api.post<{ success: boolean; message: string }>('/connectors/test', { type, config })
      .then(res => res.data),
};

// Transformer APIs
export const transformerApi = {
  getAll: () => api.get<TransformerInfo[]>('/transformers').then(res => res.data),

  getSchema: (type: string) =>
    api.get<ConfigSchema>(`/transformers/${type}/schema`).then(res => res.data),
};

export default api;
