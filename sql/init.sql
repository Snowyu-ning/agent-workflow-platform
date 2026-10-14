# Agent Workflow Platform

> 基于 Spring AI Alibaba Graph 构建的 Agent 任务编排与执行平台，通过任务状态管理、异步执行、工具标准化接入和链路追踪，将大模型能力工程化为可靠的后端服务。

[![Java](https://img.shields.io/badge/Java-21-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-green)]()
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.2-blue)]()
[![MCP](https://img.shields.io/badge/MCP-SSE-purple)]()

---

## 项目简介

随着大语言模型应用的发展，单纯的对话式 AI 已经无法满足复杂业务需求。真实场景中的 Agent 往往需要：

- 多步骤任务规划与条件路由
- 外部工具调用（数据库、缓存、代码分析等）
- 长耗时任务的异步执行
- 异常恢复与失败重试
- 执行过程的可观测与链路追踪
- 可量化的效果评测

本项目基于 Spring AI Alibaba Graph 构建 Agent 任务编排平台，以 **AI 研发助手** 为示例场景，提供 SQL 性能分析、Redis Key 分析、代码质量分析三类研发辅助能力。

**核心设计理念**：把不稳定的 AI 能力，用 Java 后端工程手段包装成可靠的任务系统。

---

## 核心特性

| 特性 | 说明 |
|------|------|
| **Graph 工作流编排** | 基于 Spring AI Alibaba Graph 实现条件路由、多工具协同 |
| **Agent Task Center** | 任务生命周期管理、状态机、失败重试、幂等控制 |
| **RabbitMQ 异步执行** | 长耗时 Agent 任务异步化，HTTP 请求快速返回 |
| **MCP 工具标准化接入** | 工具独立部署为 MCP Server，通过 SSE 协议通信；采用受控 Agent 模式，由 LLM 决策意图、代码固定路由到对应工具 |
| **多模型接入** | 用 `@Qualifier` 区分 DeepSeek 和 Ollama 两个 ChatClient，支持按场景切换 |
| **AOP 链路追踪** | `@AgentTrace` 注解记录每个节点的耗时、状态、异常 |
| **LLM-as-Judge 评测** | 38 条测试用例，三重判定，平均 Judge 分 7.84 |

---

## 系统架构
用户请求
│
▼
Spring Boot API
│
▼
┌───────────────┐
│ Task Center │ ← 状态机：WAITING → RUNNING → SUCCESS/FAILED
│ (MySQL) │
└───────┬───────┘
│ 发送任务ID
▼
┌───────────────┐
│ RabbitMQ │
└───────┬───────┘
│ 消费
▼
┌───────────────┐
│ Agent Worker │
└───────┬───────┘
│
▼
Spring AI Alibaba Graph
│
┌─────────────────┼─────────────────┐
│ │ │
▼ ▼ ▼
IntentClassify Tool Call AnswerGenerate
(LLM 节点) (MCP Client) (LLM 节点)
│
│ SSE 协议
▼
┌───────────────────────┐
│ agent-tools-mcp-server│
│ ├── sql_explain │
│ ├── redis_analyze │
│ └── code_analyze │
└───────────────────────┘

┌────────────────────────────────┐
│ AOP Trace 贯穿所有节点 │
│ 写入 agent_trace 表 │
└────────────────────────────────┘

text

---

## 技术栈

**核心框架**
- Java 21
- Spring Boot 3.5.8
- Spring AI 1.1.2
- Spring AI Alibaba 1.1.2.0（Graph 工作流引擎）

**基础设施**
- MySQL 8.0（任务、Trace、评测数据）
- Redis 7.0（上下文缓存、Key 分析对象）
- RabbitMQ 3.12（异步任务队列）

**Agent 能力**
- Spring AI Alibaba Graph（StateGraph + 条件边 + 工具节点）
- MCP（Model Context Protocol）Java SDK
- 多模型接入（DeepSeek / Ollama）

**工程能力**
- AOP 链路追踪
- 状态机任务管理
- 异步任务处理
- LLM-as-Judge 评测

---

## 核心模块

### 1. Task Center（任务中心）

- `agent_task` 表管理任务生命周期
- 状态流转：`WAITING → RUNNING → SUCCESS / FAILED`
- 支持节点级重试（`nodeRetryCount`）
- 通过 `requestId` + Redis `SETNX` 实现幂等

### 2. Workflow Engine（工作流引擎）

基于 Spring AI Alibaba Graph 编排：
START → IntentClassify → [条件路由]
├── SQL_ANALYZE → SqlAnalyzeNode
├── REDIS_ANALYZE → RedisAnalyzeNode
└── CODE_ANALYZE → CodeAnalyzeNode
│
▼
AnswerGenerate → END

text

采用**受控 Agent 模式**：
- **LLM 负责决策**：意图分类、结果校验
- **Graph 负责可靠执行**：状态传递、条件路由、异常恢复
- **代码固定路由**：意图确定后，节点硬编码调用对应工具，避免二次 LLM 决策引入不确定性

### 3. Async Worker（异步执行）

- HTTP 请求创建任务后立即返回 `taskId`
- RabbitMQ 投递任务，Worker 异步执行 Graph
- 用户通过 `taskId` 轮询任务状态和结果

### 4. Tool Center（MCP 工具接入）

工具独立部署为 **agent-tools-mcp-server** 项目，通过 MCP SSE 协议与主项目通信：

| 工具 | 能力 |
|------|------|
| `sql_explain` | 分析 SQL 执行计划，基于 MySQL EXPLAIN |
| `redis_analyze` | 分析 Redis Key 类型、TTL、命名规范 |
| `code_analyze` | 分析代码潜在 Bug、性能问题、安全风险 |

**为什么用 MCP？**

- **解耦**：工具与 Agent 从代码级解耦，通过标准协议通信
- **独立部署**：工具可以单独发版，不影响 Agent 主服务
- **可复用**：任何支持 MCP 协议的 Agent 系统都能复用这些工具
- **标准化**：基于 JSON-RPC 2.0，工具定义（name / description / 参数 schema）标准化

### 5. Trace Module（链路追踪）

通过 `@AgentTrace` 注解 + AOP 切面，记录每个节点的：

- TraceId（跨 RabbitMQ 线程通过消息头传递）
- 节点名、类型（LLM / TOOL / ROUTER）
- 耗时、状态、异常信息
- 写入 `agent_trace` 表，支持按 traceId 还原完整执行链路

### 6. Evaluation（评测体系）

- **测试集**：38 条用例，覆盖 SQL / Redis / 代码分析三类场景
- **三重判定**：流程成功 + 分支正确 + LLM-as-Judge 评分 ≥ 7
- **迭代优化**：通过评测发现工具假数据、Prompt 兜底错误、Redis 测试数据缺失等问题，逐步修复

---

## 评测数据

**最终批次结果**（batch-1791903281920）：

| 指标 | 数值 |
|------|------|
| 测试用例总数 | 38 |
| 成功数 | 37 |
| **任务成功率** | **97.37%** |
| **Tool 调用准确率** | **97.37%** |
| **平均 Judge 分** | **7.84 / 10** |
| 平均端到端延迟 | 7305 ms |

**分类别表现**：

| 类别 | 用例数 | 通过 | 成功率 | 平均分 | 平均延迟 |
|------|--------|------|--------|--------|----------|
| SQL 分析 | 12 | 11 | 91.67% | 7.55 | 8649 ms |
| Redis 分析 | 14 | 14 | 100% | 7.93 | 7164 ms |
| 代码分析 | 12 | 12 | 100% | 8.00 | 6125 ms |

**开发过程优化记录**：

| 阶段 | 优化内容 | 成功率变化 |
|------|---------|-----------|
| 初版 | 流程判定（分支 + 非空） | 100%（判定过松） |
| 加入 LLM-as-Judge | 发现回答质量问题 | 39.47% |
| 修复 AnswerGenerate 兜底 | 避免工具失败时答非所问 | 94.74% |
| 补充 EXPLAIN Prompt 约束 | 防止 LLM 编造执行计划 | 100% |
| **MCP 工具标准化改造** | 工具独立部署，通过协议通信 | **97.37%** |

**失败案例分析**：

唯一失败的用例（case 1）输入为 `select * from user where name='John Doe'`，失败原因是"任务超时"。分析后发现：

- MCP 改造后平均延迟从 3 秒涨到 7.3 秒，多出来的部分是 SSE 协议开销、JSON 序列化和网络往返
- 其中一条用例恰好超过 30 秒超时阈值
- **改进方向**：引入本地缓存减少 MCP 调用频率，或对同一工具调用做批处理；同时把超时阈值从 30 秒放宽到 60 秒

---

## 快速开始

### 依赖环境

- JDK 21+
- MySQL 8.0（端口 3306）
- Redis 7.0（端口 6379）
- RabbitMQ 3.12（端口 5672）
- Ollama（可选，本地部署 qwen3:4b，端口 11434）

### 1. 启动依赖服务

```bash
# MySQL
docker run -d --name agent-mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=7777 \
  -e MYSQL_DATABASE=agent_db mysql:8.0

# Redis
docker run -d --name agent-redis -p 6379:6379 redis:7.0

# RabbitMQ
docker run -d --name agent-rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  rabbitmq:3.12-management
2. 初始化数据库
执行 sql/init.sql 创建四张表：agent_task、agent_trace、agent_evaluation、user

3. 配置 API Key
bash
export DEEPSEEK_API_KEY=sk-xxxxxx
4. 启动 MCP 工具服务
bash
cd agent-tools-mcp-server
mvn spring-boot:run
# 服务监听 8081，SSE 端点：http://localhost:8081/sse
5. 启动 Agent 主项目
bash
cd agent-workflow-platform
mvn spring-boot:run
# 服务监听 8080
6. 提交任务
bash
curl -X POST http://localhost:8080/api/agent/task \
  -H "Content-Type: application/json" \
  -d '{"userId":10001,"inputText":"select * from user where name=\"张三\""}'
7. 触发评测
bash
curl -X POST http://localhost:8080/api/eval/run
项目结构
主项目（Agent Workflow Platform）

text
agent-workflow-platform/
├── src/main/java/com/zxr/
│   ├── controller/          API 入口
│   ├── workflow/            Graph 工作流
│   │   ├── config/          Graph 定义
│   │   ├── node/            节点（IntentClassify / SqlAnalyze / ...）
│   │   └── state/           AgentState
│   ├── task/                任务中心
│   │   ├── entity/
│   │   ├── mapper/
│   │   ├── service/
│   │   └── enums/
│   ├── worker/              RabbitMQ 异步执行
│   │   ├── config/
│   │   ├── producer/
│   │   └── consumer/
│   ├── trace/               AOP 链路追踪
│   │   ├── annotation/
│   │   └── aspect/
│   ├── evaluation/          评测模块
│   ├── memory/              Redis 上下文
│   └── config/              AiConfig（多模型）
└── pom.xml
MCP Server 项目（agent-tools-mcp-server）

text
agent-tools-mcp-server/
├── src/main/java/com/zxr/tools/
│   ├── AgentTool.java       工具接口
│   ├── ToolResult.java
│   ├── impl/
│   │   ├── SqlExplainTool.java
│   │   ├── RedisAnalyzeTool.java
│   │   └── CodeAnalyzeTool.java
│   └── config/
│       └── McpServerConfig.java
└── pom.xml
后续规划
□ Docker Sandbox：Agent 生成代码后在容器内隔离执行
□ Prompt 版本管理：支持热更新和 A/B 测试
□ 评测集扩展：引入对抗性用例（多意图混合、Prompt 注入）
□ 多 Judge 交叉验证：减少单一模型的评分偏差
□ Trace 可视化：基于 Web UI 展示执行链路
技术亮点总结
Agent 工程化：不是调用 LLM API，而是把 Agent 执行封装为可管理、可追踪、可恢复的后端系统

异步架构：RabbitMQ 解决长耗时任务阻塞问题

MCP 标准化：工具从 Agent 中解耦，通过标准协议独立部署

可观测性：AOP Trace 记录每个节点执行，支持链路回放

可量化评测：LLM-as-Judge + 三重判定 + 38 条测试用例，成功率 97.37%

多模型接入：通过 @Qualifier 区分 DeepSeek 和 Ollama 两个 ChatClient，生产链路使用 DeepSeek，本地模型用于成本对比实验

text

---

## 三、README 还需要补充的东西

**README 里引用了 `sql/init.sql`，你还需要创建这个文件。**

在主项目根目录建 `sql/init.sql`，内容：

```sql
CREATE DATABASE IF NOT EXISTS agent_db;
USE agent_db;

CREATE TABLE user (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      name VARCHAR(50),
                      age INT,
                      email VARCHAR(100),
                      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO user (name, age, email) VALUES
                                        ('张三', 20, 'zhangsan@test.com'),
                                        ('李四', 25, 'lisi@test.com'),
                                        ('王五', 30, 'wangwu@test.com');

CREATE TABLE agent_task (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            user_id BIGINT NOT NULL,
                            task_type VARCHAR(50),
                            input_text TEXT NOT NULL,
                            status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
                            current_node VARCHAR(50),
                            retry_count INT DEFAULT 0,
                            max_retry INT DEFAULT 3,
                            result TEXT,
                            error_message TEXT,
                            trace_id VARCHAR(64),
                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            INDEX idx_user_status (user_id, status),
                            INDEX idx_trace (trace_id)
);

CREATE TABLE agent_trace (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             trace_id VARCHAR(64) NOT NULL,
                             task_id BIGINT,
                             node_name VARCHAR(50) NOT NULL,
                             node_type VARCHAR(20),
                             start_time DATETIME NOT NULL,
                             duration_ms BIGINT,
                             status VARCHAR(20),
                             error_message TEXT,
                             created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                             INDEX idx_trace_id (trace_id),
                             INDEX idx_task_id (task_id)
);

CREATE TABLE agent_evaluation (
                                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                  eval_batch_id VARCHAR(64) NOT NULL,
                                  case_id INT NOT NULL,
                                  input_text TEXT NOT NULL,
                                  expected_tool VARCHAR(50),
                                  actual_tool VARCHAR(50),
                                  status VARCHAR(20),
                                  judge_score INT,
                                  judge_reason TEXT,
                                  latency_ms BIGINT,
                                  final_answer TEXT,
                                  error_message TEXT,
                                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                  INDEX idx_batch (eval_batch_id)
);