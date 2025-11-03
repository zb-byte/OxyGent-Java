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

✅ **ReAct循环**：自动顺序执行  
✅ **react_memory管理**：自动记录和传递历史  
✅ **LLM集成**：支持 DeepSeek、Ollama、OpenAI  
✅ **工具路由**：通过框架自动路由  
✅ **Spring Boot集成**：完整的Spring Boot应用

## 开发指南

### 添加新的智能体

```java
ReActAgent agent = new ReActAgent(
    "agent_name",
    "智能体描述",
    false,
    llmClient,
    subAgents,
    tools,
    systemPrompt,
    maxRounds
);

framework.registerAgent("agent_name", agent);
```

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

## 更多信息

- DeepSeek API 文档: https://platform.deepseek.com/api-docs
- Spring Boot 文档: https://spring.io/projects/spring-boot

