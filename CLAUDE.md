# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Data Pipeline Builder 是一个可视化数据 ETL 工具，类似 Palantir Pipeline Builder。用户可以通过 DAG 编辑器拖拽节点来构建数据管道。

## 常用命令

### 后端 (Java 17 + Spring Boot 3.x)

```bash
# 构建所有模块
cd backend && mvn clean install

# 启动后端服务 (http://localhost:8080)
cd backend/pipeline-api && mvn spring-boot:run

# 运行测试
cd backend && mvn test

# 运行单个模块测试
cd backend/pipeline-core && mvn test
```

### 前端 (React 18 + TypeScript + Vite)

```bash
cd frontend
npm install
npm run dev      # 开发服务器 (http://localhost:3000)
npm run build    # 生产构建
npm run lint     # ESLint 检查
```

### Docker

```bash
docker-compose up -d  # 启动 MySQL (root/root, 数据库: pipeline_builder)
```

## 架构概览

### 后端模块结构

- **pipeline-core**: 核心接口和模型定义
  - `Connector` 接口: 数据源连接器基础接口，提供 `DataReader`/`DataWriter`
  - `Transformer` 接口: 数据转换器接口，处理数据流转换
  - `PipelineExecutor`: Pipeline 执行引擎
  - `ConnectorRegistry`/`TransformerRegistry`: 插件注册表

- **pipeline-connectors**: 数据连接器实现
  - `connector-jdbc`: MySQL/PostgreSQL 连接器
  - `connector-file`: CSV/JSON 文件连接器

- **pipeline-transformers**: 转换器实现 (Filter, Map, Aggregate)

- **pipeline-api**: REST API 服务层
  - `PluginAutoConfiguration`: 通过 Spring 自动注入所有 `@Component` 标注的 Connector/Transformer

### 前端架构

- **状态管理**: Zustand (`pipelineStore.ts`)
- **可视化编辑器**: @xyflow/react (ReactFlow)
- **UI 组件**: Ant Design 5.x
- **表单生成**: react-jsonschema-form (@rjsf)
- **样式**: Tailwind CSS

### 核心数据流

1. 前端通过 ReactFlow 编辑 DAG 图（节点+边）
2. 节点配置通过 JSON Schema 动态生成表单
3. Pipeline 数据通过 REST API 保存/执行
4. 后端 `PipelineExecutor` 拓扑排序后按顺序执行节点

## 扩展开发

### 添加新 Connector

1. 在 `pipeline-connectors` 创建新模块
2. 实现 `Connector` 接口及其 `DataReader`/`DataWriter`
3. 添加 `@Component` 注解，Spring 会自动注册到 `ConnectorRegistry`

### 添加新 Transformer

1. 在 `pipeline-transformers` 模块创建类
2. 实现 `Transformer` 接口
3. 添加 `@Component` 注解

## API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/connectors | 获取所有连接器 |
| GET | /api/connectors/{type}/schema | 获取连接器配置 schema |
| POST | /api/connectors/test | 测试连接 |
| GET | /api/transformers | 获取所有转换器 |
| CRUD | /api/pipelines | Pipeline 管理 |
| POST | /api/pipelines/{id}/execute | 执行 Pipeline |
