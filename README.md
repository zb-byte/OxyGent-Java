# react-oxygent-java

基于 Spring Boot + Maven + JDK 21 的 ReAct Agent Framework Java实现。

## 技术栈

- **JDK**: 21
- **Spring Boot**: 3.3.4
- **Maven**: 构建管理
- **LLM**: DeepSeek API（默认），支持 Ollama、OpenAI

## 快速开始

### 1. 环境要求

- JDK 21+
- Maven 3.6+

### 2. 获取 DeepSeek API Key

1. 访问 [DeepSeek Platform](https://platform.deepseek.com)
2. 注册账号并获取 API Key
3. 设置环境变量：

```bash
export DEEPSEEK_API_KEY="your-api-key-here"
```

或者在项目根目录创建 `.env` 文件（参考 `.env.example`）：

```bash
cp .env.example .env
# 编辑 .env 文件，填入你的 API Key
```

### 3. 编译项目

```bash
mvn clean compile
```

### 4. 运行项目

```bash
mvn spring-boot:run
```

或者先打包再运行：

```bash
mvn clean package
java -jar target/react-oxygent-java-0.1.0.jar
```

## 使用其他LLM

### 使用 Ollama（本地大模型）

```java
// 在 Application.java 中替换
LLMClient llmClient = new OllamaLLMClient(
    "http://localhost:11434",
    "llama2"
);
```

### 使用 OpenAI

```java
// 在 Application.java 中替换
LLMClient llmClient = new OpenAILLMClient(
    System.getenv("OPENAI_API_KEY"),
    "gpt-4"
);
```

## 项目结构

```
react-oxygent-java/
├── src/main/java/
│   ├── core/              # 核心框架
│   │   ├── AgentFramework.java
│   │   ├── Agent.java
│   │   └── ReActAgent.java
│   ├── memory/            # 内存管理
│   │   ├── ReactMemory.java
│   │   └── Observation.java
│   ├── llm/               # LLM客户端
│   │   ├── LLMClient.java
│   │   ├── DeepSeekLLMClient.java  ⭐ 默认使用
│   │   ├── OllamaLLMClient.java
│   │   └── OpenAILLMClient.java
│   ├── model/             # 数据模型
│   │   ├── AgentRequest.java
│   │   ├── AgentResponse.java
│   │   └── ToolCall.java
│   └── demo/              # 示例代码
│       ├── Application.java        # Spring Boot入口
│       └── DevOpsDemo.java
├── pom.xml                # Maven配置
└── README.md
```

## DeepSeek 模型说明

### 可用模型

- **deepseek-chat**: 通用对话模型（默认）
- **deepseek-coder**: 代码专用模型（推荐用于代码相关任务）

### API 配置

DeepSeek API 端点为：`https://api.deepseek.com/v1/chat/completions`

格式与 OpenAI API 兼容，使用方式类似。

## 核心特性

✅ **多种 Agent 类型**：支持 ReActAgent、ChatAgent、RAGAgent、WorkflowAgent、ParallelAgent  
✅ **ReAct循环**：自动顺序执行  
✅ **react_memory管理**：自动记录和传递历史  
✅ **LLM集成**：支持 DeepSeek、Ollama、OpenAI  
✅ **工具路由**：通过框架自动路由  
✅ **Spring Boot集成**：完整的Spring Boot应用

## 开发指南

### Agent 类型

框架支持以下 5 种 Agent 类型：

#### 1. ReActAgent（推理-行动智能体）

```java
ReActAgent agent = new ReActAgent(
    "agent_name",
    "智能体描述",
    false,
    llmClient,
    subAgents,      // 可调用的子智能体
    tools,          // 可用的工具
    systemPrompt,   // 系统提示
    maxRounds       // 最大执行轮次
);

framework.registerAgent("agent_name", agent);
```

#### 2. ChatAgent（对话智能体）

```java
ChatAgent chatAgent = new ChatAgent(
    "chat_agent",
    "对话智能体",
    false,
    llmClient,
    "You are a helpful assistant.",  // 系统提示
    10  // 短期记忆大小
);

framework.registerAgent("chat_agent", chatAgent);
```

#### 3. RAGAgent（检索增强生成智能体）

```java
RAGAgent ragAgent = new RAGAgent(
    "rag_agent",
    "检索增强智能体",
    false,
    llmClient,
    null,  // 使用默认提示词
    10,
    "knowledge",  // 知识占位符
    request -> {
        // 知识检索逻辑
        String knowledge = searchFromDatabase(request.getQuery());
        return CompletableFuture.completedFuture(knowledge);
    }
);

framework.registerAgent("rag_agent", ragAgent);
```

#### 4. WorkflowAgent（工作流智能体）

```java
WorkflowAgent workflowAgent = new WorkflowAgent(
    "workflow_agent",
    "工作流智能体",
    false,
    request -> {
        // 自定义业务流程
        AgentResponse response = request.call("other_agent", 
            Map.of("query", request.getQuery())).join();
        return CompletableFuture.completedFuture(response.getOutput());
    }
);

framework.registerAgent("workflow_agent", workflowAgent);
```

#### 5. ParallelAgent（并行执行智能体）

```java
ParallelAgent parallelAgent = new ParallelAgent(
    "parallel_agent",
    "并行执行智能体",
    false,
    llmClient,  // 用于总结的 LLM
    Arrays.asList("agent1", "agent2", "agent3")  // 并行执行的 Agent 列表
);

framework.registerAgent("parallel_agent", parallelAgent);
```

#### 6. ReflexionAgent（反思改进智能体）

```java
// 创建 worker agent（生成答案）
ChatAgent workerAgent = new ChatAgent("worker_agent", ...);

// 创建 reflexion agent（评价答案）
ChatAgent reflexionAgent = new ChatAgent("reflexion_agent", ...);

// 创建 ReflexionAgent
ReflexionAgent reflexion = new ReflexionAgent(
    "reflexion_agent",
    "反思改进智能体",
    true,  // 主控智能体
    "worker_agent",  // 生成答案的 Agent
    "reflexion_agent",  // 评价答案的 Agent
    3  // 最大反思轮次
);

framework.registerAgent("worker_agent", workerAgent);
framework.registerAgent("reflexion_agent", reflexionAgent);
framework.registerAgent("reflexion_agent", reflexion);
```

### 如何选择 Agent

- **简单问答** → ChatAgent
- **需要知识库** → RAGAgent
- **复杂推理** → ReActAgent
- **固定流程** → WorkflowAgent
- **并行协作** → ParallelAgent
- **高质量答案** → ReflexionAgent

详细说明请参考：[Agent 使用指南](./docs/AGENT_USAGE_GUIDE.md)

### 自定义LLM客户端

实现 `LLMClient` 接口即可：

```java
public class CustomLLMClient implements LLMClient {
    @Override
    public String chat(List<Map<String, String>> messages) {
        // 实现LLM调用逻辑
    }
}
```

## 依赖说明

主要依赖（在 `pom.xml` 中）：

- `spring-boot-starter`: Spring Boot 基础
- `jackson-databind`: JSON 处理（用于LLM客户端）

## 故障排查

### API Key 未设置

```
IllegalArgumentException: DeepSeek API Key未设置
```

**解决方法**：设置环境变量 `DEEPSEEK_API_KEY`

### 连接失败

检查：
1. API Key 是否正确
2. 网络连接是否正常
3. DeepSeek 服务是否可用

### 编译错误

确保使用 JDK 21：

```bash
java -version  # 应该显示 21.x
```

## 📚 文档

详细文档请查看 [docs/](./docs/) 目录：

### 核心文档

- **[文档索引](./docs/README.md)** - 所有文档的索引
- **[Agent 使用指南](./docs/AGENT_USAGE_GUIDE.md)** - 所有 Agent 类型的使用说明 ⭐ 新增
- **[业务开发指南](./docs/BUSINESS_DEVELOPMENT_GUIDE.md)** - 如何开发新业务
- **[启动顺序说明](./docs/STARTUP_SEQUENCE.md)** - 应用启动加载顺序

### 功能文档

- **[MCP 工具使用指南](./docs/MCP_USAGE_GUIDE.md)** - MCP 工具使用说明
- **[远程智能体使用指南](./docs/REMOTE_AGENT_USAGE.md)** - 远程调用使用说明

### 技术文档

- **[A2A 核心思路对比](./docs/A2A_CORE_COMPARISON.md)** - 与 Python 版本的对比
- **[智能体流程模式](./docs/AGENT_FLOW_PATTERNS.md)** - ReAct、PlanAndSolve 等模式详解

## 更多信息

- DeepSeek API 文档: https://platform.deepseek.com/api-docs
- Spring Boot 文档: https://spring.io/projects/spring-boot

