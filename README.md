# Data Pipeline Builder

一个类似 Palantir Pipeline Builder 的可视化数据 ETL 工具。

## 技术栈

- **后端**: Java 17 + Spring Boot 3.x
- **前端**: React 18 + TypeScript + ReactFlow
- **数据库**: MySQL (元数据存储)

## 功能特性

- 可视化 DAG 编辑器
- 插件化数据连接器 (JDBC, CSV, JSON)
- 可扩展转换器 (Filter, Map, Aggregate)
- Pipeline 执行引擎
- 支持批处理和流处理模式

## 项目结构

```
data-pipeline-builder/
├── backend/                          # Java Spring Boot
│   ├── pipeline-core/                # 核心模块
│   ├── pipeline-connectors/          # 数据连接器
│   │   ├── connector-jdbc/           # MySQL, PostgreSQL
│   │   └── connector-file/           # CSV, JSON
│   ├── pipeline-transformers/        # 数据转换器
│   └── pipeline-api/                 # REST API
├── frontend/                         # React + TypeScript
└── docker-compose.yml
```

## 快速开始

### 1. 启动 MySQL

```bash
docker-compose up -d
```

### 2. 启动后端

```bash
cd backend
mvn clean install
cd pipeline-api
mvn spring-boot:run
```

后端服务将运行在 http://localhost:8080

API 文档: http://localhost:8080/swagger-ui.html

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端将运行在 http://localhost:3000

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/connectors | 获取所有可用连接器 |
| GET | /api/connectors/{type}/schema | 获取连接器配置 schema |
| POST | /api/connectors/test | 测试连接 |
| GET | /api/transformers | 获取所有可用转换器 |
| GET | /api/pipelines | 获取所有 Pipeline |
| POST | /api/pipelines | 创建 Pipeline |
| PUT | /api/pipelines/{id} | 更新 Pipeline |
| DELETE | /api/pipelines/{id} | 删除 Pipeline |
| POST | /api/pipelines/{id}/execute | 执行 Pipeline |

## 扩展开发

### 添加新的 Connector

1. 在 `pipeline-connectors` 下创建新模块
2. 实现 `Connector` 接口
3. 实现 `DataReader` 和 `DataWriter`
4. 添加 `@Component` 注解自动注册

```java
@Component
public class MyConnector implements Connector {
    @Override
    public String getType() {
        return "my-connector";
    }
    // ...
}
```

### 添加新的 Transformer

1. 在 `pipeline-transformers` 模块中创建类
2. 实现 `Transformer` 接口
3. 添加 `@Component` 注解

```java
@Component
public class MyTransformer implements Transformer {
    @Override
    public String getType() {
        return "my-transformer";
    }
    // ...
}
```

## License

MIT
