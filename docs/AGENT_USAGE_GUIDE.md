# Agent 使用指南

本文档介绍 react-oxygent-java 框架中所有可用的 Agent 类型及其使用方法。

---

## 📋 Agent 类型概览

框架目前支持以下 Agent 类型：

| Agent 类型 | 核心能力 | 适用场景 |
|-----------|---------|---------|
| **ReActAgent** | 推理-行动循环，自动调用工具 | 需要复杂推理和工具调用的任务 |
| **ChatAgent** | 纯对话，管理短期记忆 | 简单问答、客服、对话系统 |
| **RAGAgent** | 检索增强生成，结合知识库 | 需要外部知识检索的场景 |
| **WorkflowAgent** | 执行自定义业务流程 | 固定业务流程、多步骤任务 |
| **ParallelAgent** | 并行执行多个任务并聚合 | 多角色协作、并行求解 |
| **ReflexionAgent** | 回答→评价→改进循环 | 需要高质量答案、需要自我改进的场景 |

---

## 1. ReActAgent（推理-行动智能体）

### 核心能力

实现 ReAct（推理-行动）循环。LLM 先"思考"再"调用工具"，根据工具返回继续迭代，直到给出答案或达到最大轮次。

### 使用示例

```java
ReActAgent agent = new ReActAgent(
    "agent_name",
    "智能体描述",
    false,  // 是否为主控智能体
    llmClient,
    Arrays.asList("sub_agent1", "sub_agent2"),  // 可调用的子智能体
    Arrays.asList("tool1", "tool2"),            // 可用的工具
    "你是一个有用的助手。",                     // 系统提示
    10  // 最大执行轮次
);

framework.registerAgent("agent_name", agent);
```

### 适用场景

- 需要复杂推理的任务
- 需要调用多个工具的场景
- 需要动态决策的工作流

---

## 2. ChatAgent（对话智能体）

### 核心能力

纯对话，管理短期记忆，将系统提示和历史拼装后直连 LLM。输入为 query 和历史对话，输出为 LLM 的回答。

### 使用示例

```java
ChatAgent chatAgent = new ChatAgent(
    "chat_agent",
    "对话智能体",
    false,
    llmClient,
    "You are a helpful assistant.",  // 系统提示（支持 ${variable} 模板变量）
    10  // 短期记忆大小（保留的对话轮数）
);

framework.registerAgent("chat_agent", chatAgent);
```

### 特性

- **短期记忆管理**：自动从 `AgentRequest.arguments` 中获取 `short_memory` 作为历史对话
- **模板变量支持**：系统提示支持 `${variable}` 格式的变量替换
- **自动限制历史**：根据 `shortMemorySize` 参数自动限制历史对话数量

### 适用场景

- 简单问答系统
- 客服机器人
- 个人助手
- 内容生成

---

## 3. RAGAgent（检索增强生成智能体）

### 核心能力

在 ChatAgent 基础上，执行检索增强（RAG）。调用知识检索函数来拉取知识，填充到 prompt。输入为 query，内部先调用检索函数，输出为结合知识后的 LLM 回答。

### 使用示例

#### 方式1：使用 Lambda 表达式

```java
RAGAgent ragAgent = new RAGAgent(
    "rag_agent",
    "检索增强智能体",
    false,
    llmClient,
    null,  // 使用默认提示词（包含 ${knowledge} 占位符）
    10,
    "knowledge",  // 知识占位符名称
    request -> {
        String query = request.getQuery();
        // 从数据库、向量库或其他来源检索知识
        String knowledge = searchFromDatabase(query);
        return CompletableFuture.completedFuture(knowledge);
    }
);

framework.registerAgent("rag_agent", ragAgent);
```

#### 方式2：使用同步方法

```java
RAGAgent ragAgent = new RAGAgent(
    "rag_agent",
    "检索增强智能体",
    false,
    llmClient,
    null,
    10,
    "knowledge",
    KnowledgeRetriever.fromSync(request -> {
        // 同步检索逻辑
        return "检索到的知识...";
    })
);
```

#### 方式3：实现 KnowledgeRetriever 接口

```java
class MyKnowledgeRetriever implements KnowledgeRetriever {
    @Override
    public CompletableFuture<String> retrieve(AgentRequest request) {
        String query = request.getQuery();
        // 你的检索逻辑
        return CompletableFuture.completedFuture("知识内容");
    }
}

RAGAgent ragAgent = new RAGAgent(
    "rag_agent",
    "检索增强智能体",
    false,
    llmClient,
    null,
    10,
    "knowledge",
    new MyKnowledgeRetriever()
);
```

### 特性

- **继承 ChatAgent**：拥有 ChatAgent 的所有能力
- **知识检索**：在预处理阶段自动调用知识检索函数
- **模板填充**：将检索到的知识自动填充到 prompt 的占位符中
- **自定义占位符**：支持自定义知识占位符名称（默认 `knowledge`）

### 适用场景

- 知识库问答
- 文档检索系统
- 需要外部知识增强的场景

### 更多示例

详细示例请参考：`framework.agent.examples.KnowledgeRetrieverExamples`

---

## 4. WorkflowAgent（工作流智能体）

### 核心能力

直接执行用户提供的一个自定义业务流程函数，不做推理、不调工具，仅调用注入的函数，并把其返回作为最终输出。

### 使用示例

#### 方式1：使用 Lambda 表达式

```java
WorkflowAgent workflowAgent = new WorkflowAgent(
    "workflow_agent",
    "工作流智能体",
    false,
    request -> {
        String query = request.getQuery();
        
        // 1. 调用其他 Agent
        AgentResponse agentResp = request.call("chat_agent", 
            Map.of("query", query)).join();
        
        // 2. 调用工具
        AgentResponse toolResp = request.call("calculator_tool",
            Map.of("query", query)).join();
        
        // 3. 返回结果
        return CompletableFuture.completedFuture(
            "Agent结果: " + agentResp.getOutput() + 
            ", 工具结果: " + toolResp.getOutput()
        );
    }
);

framework.registerAgent("workflow_agent", workflowAgent);
```

#### 方式2：复杂多步骤工作流

```java
WorkflowAgent mathAgent = new WorkflowAgent(
    "math_agent",
    "数学计算智能体",
    true,  // 主控智能体
    request -> {
        String query = request.getQuery();
        
        // 1. 调用 LLM 获取精度要求
        CompletableFuture<AgentResponse> llmResp = request.call(
            "default_llm",
            Map.of(
                "messages", Arrays.asList(
                    Map.of("role", "system", "content", "You are a helpful assistant."),
                    Map.of("role", "user", "content", 
                        "用户的问题是" + query + "，用户想要小数点后多少位圆周率？直接回答数字")
                )
            )
        );
        
        // 2. 解析精度并调用计算工具
        return llmResp.thenCompose(response -> {
            String precision = response.getOutput().trim();
            return request.call("calc_pi", Map.of("prec", precision))
                .thenApply(toolResp -> 
                    "Save " + precision + " positions: " + toolResp.getOutput()
                );
        });
    }
);
```

#### 方式3：使用同步方法

```java
WorkflowAgent simpleAgent = new WorkflowAgent(
    "simple_agent",
    "简单智能体",
    false,
    WorkflowFunction.fromSync(request -> {
        // 同步处理逻辑
        return "处理结果: " + request.getQuery();
    })
);
```

### 工作流函数可以做什么

1. **获取请求信息**：
   - `request.getQuery()` - 获取用户查询
   - `request.getArguments()` - 获取参数
   - `request.getSharedData()` - 获取共享数据

2. **调用其他组件**：
   - `request.call("agent_name", arguments)` - 调用其他 Agent
   - `request.call("tool_name", arguments)` - 调用工具
   - `request.call("llm_name", arguments)` - 调用 LLM

3. **执行业务逻辑**：
   - 数据库操作
   - 文件处理
   - 外部 API 调用
   - 复杂计算

### 适用场景

- 固定业务流程
- 多步骤任务编排
- 需要精确控制执行顺序的场景

### 更多示例

详细示例请参考：`framework.agent.examples.WorkflowFunctionExamples`

---

## 5. ParallelAgent（并行执行智能体）

### 核心能力

将同一个任务并行发给多个"队友"（permitted_tool_name_list 中的工具/智能体），聚合结果，再用 LLM 总结。适用于多角色/多策略/多模型并行求解，再统一总结。

### 使用示例

```java
ParallelAgent parallelAgent = new ParallelAgent(
    "parallel_agent",
    "并行执行智能体",
    false,
    llmClient,  // 用于总结结果的 LLM
    Arrays.asList("agent1", "agent2", "agent3")  // 允许调用的工具/智能体列表
);

framework.registerAgent("parallel_agent", parallelAgent);
```

### 执行流程

1. **并行调用**：同时调用 `permittedToolNameList` 中的所有工具/智能体
2. **等待完成**：等待所有并行任务完成
3. **聚合总结**：使用 LLM 总结所有并行执行的结果

### 适用场景

- 多角色协作（例如：同时进行数据分析、文字总结、纠错）
- 多策略并行求解
- 多模型并行推理
- 需要聚合多个结果的场景

### 示例场景

```java
// 注册多个专业 Agent
ChatAgent summarizer = new ChatAgent(...);  // 文本总结
ChatAgent analyser = new ChatAgent(...);   // 数据分析
ChatAgent checker = new ChatAgent(...);    // 文档检查

// 使用 ParallelAgent 并行执行并总结
ParallelAgent analyzer = new ParallelAgent(
    "analyzer",
    "文档分析智能体",
    false,
    llmClient,
    Arrays.asList("summarizer", "analyser", "checker")
);
```

---

## 6. ReflexionAgent（反思改进智能体）

### 核心能力

"回答 → 评价 → 改进"循环，通过评审 Agent 检查答复质量，给出改进建议，不满意则生成改进 prompt 再答，直到满意或达轮次上限。

### 执行流程

1. **生成答案**：调用 `worker_agent` 生成初始答案
2. **评价答案**：调用 `reflexion_agent` 评价答案质量
3. **判断满意**：如果满意，返回答案
4. **改进循环**：如果不满意且未达最大轮次，生成改进提示，继续循环

### 使用示例

```java
// 1. 创建 worker agent（生成答案）
ChatAgent workerAgent = new ChatAgent(
    "worker_agent",
    "工作智能体，负责生成答案",
    false,
    llmClient,
    "You are a helpful assistant that provides detailed answers.",
    10
);

// 2. 创建 reflexion agent（评价答案）
ChatAgent reflexionAgent = new ChatAgent(
    "reflexion_agent",
    "反思智能体，负责评价答案质量",
    false,
    llmClient,
    "You are an expert evaluator that assesses answer quality and provides improvement suggestions.",
    10
);

// 3. 创建 ReflexionAgent
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

### 自定义评价和改进模板

```java
// 使用自定义评价模板
String customEvaluationTemplate = 
    "Evaluate this answer:\n\n" +
    "Question: {query}\n" +
    "Answer: {answer}\n\n" +
    "Check: accuracy, completeness, clarity.\n" +
    "Format: is_satisfactory: true/false\n" +
    "evaluation_reason: [reason]\n" +
    "improvement_suggestions: [suggestions]";

String customImprovementTemplate = 
    "{original_query}\n\n" +
    "Improve based on: {improvement_suggestions}\n" +
    "Previous: {previous_answer}";

ReflexionAgent customReflexion = new ReflexionAgent(
    "custom_reflexion",
    "自定义反思智能体",
    true,
    "worker_agent",
    "reflexion_agent",
    3,
    null,  // 使用默认 worker 解析
    null,  // 使用默认 reflexion 解析
    customEvaluationTemplate,
    customImprovementTemplate
);
```

### 自定义解析函数

```java
// 自定义 worker 响应解析
Function<String, String> parseWorker = response -> {
    // 提取答案部分（例如从 JSON 中提取）
    return response.trim();
};

// 自定义 reflexion 响应解析
Function<String, ReflectionEvaluation> parseReflexion = response -> {
    // 解析评价结果（例如从 JSON 中解析）
    // 返回 ReflectionEvaluation 对象
    return new ReflectionEvaluation(
        true,  // isSatisfactory
        "Good answer",  // evaluationReason
        ""  // improvementSuggestions
    );
};

ReflexionAgent customReflexion = new ReflexionAgent(
    "custom_reflexion",
    "自定义解析的反思智能体",
    true,
    "worker_agent",
    "reflexion_agent",
    3,
    parseWorker,
    parseReflexion,
    null,  // 使用默认模板
    null
);
```

### 特性

- **自动循环**：自动执行"生成→评价→改进"循环
- **质量保证**：通过评价机制确保答案质量
- **可配置**：支持自定义评价模板、改进模板和解析函数
- **灵活控制**：可设置最大反思轮次

### 适用场景

- 需要高质量答案的场景
- 需要自我改进和优化的任务
- 数学问题求解（需要验证正确性）
- 代码生成（需要检查代码质量）
- 文档撰写（需要检查完整性和准确性）

### 评价标准

默认评价模板包含以下标准：
1. **准确性**：信息是否正确和真实
2. **完整性**：是否完全回答了用户的问题
3. **清晰度**：结构是否清晰、易于理解
4. **相关性**：是否聚焦用户需求
5. **有用性**：是否提供实用价值

---

## 📚 如何选择 Agent

### 决策树

```
需要复杂推理和工具调用？
├─ 是 → ReActAgent
└─ 否 → 需要外部知识检索？
    ├─ 是 → RAGAgent
    └─ 否 → 需要执行固定业务流程？
        ├─ 是 → WorkflowAgent
        └─ 否 → 需要并行执行多个任务？
            ├─ 是 → ParallelAgent
            └─ 否 → 需要高质量答案和自我改进？
                ├─ 是 → ReflexionAgent
                └─ 否 → ChatAgent（简单对话）
```

### 快速参考

| 需求 | 推荐 Agent |
|------|-----------|
| 简单问答 | ChatAgent |
| 需要知识库 | RAGAgent |
| 复杂推理 | ReActAgent |
| 固定流程 | WorkflowAgent |
| 并行协作 | ParallelAgent |
| 高质量答案 | ReflexionAgent |

---

## 🔗 相关文档

- **[AGENT_FLOW_PATTERNS.md](./AGENT_FLOW_PATTERNS.md)** - 智能体流程模式详解
- **[HOW_TO_CHOOSE_AGENT.md](./HOW_TO_CHOOSE_AGENT.md)** - 如何选择 Agent（如果存在）
- **[BUSINESS_DEVELOPMENT_GUIDE.md](./BUSINESS_DEVELOPMENT_GUIDE.md)** - 业务开发指南

---

## 💡 最佳实践

1. **简单任务用 ChatAgent**：对于简单的问答场景，ChatAgent 足够使用
2. **知识检索用 RAGAgent**：需要结合外部知识库时，使用 RAGAgent
3. **复杂推理用 ReActAgent**：需要动态决策和工具调用时，使用 ReActAgent
4. **固定流程用 WorkflowAgent**：业务流程明确时，使用 WorkflowAgent 更高效
5. **并行协作用 ParallelAgent**：需要多个 Agent 协作时，使用 ParallelAgent
6. **高质量答案用 ReflexionAgent**：需要确保答案质量、需要自我改进时，使用 ReflexionAgent

---

## 📝 示例代码位置

所有示例代码位于：
- `framework.agent.examples.KnowledgeRetrieverExamples` - RAGAgent 知识检索示例
- `framework.agent.examples.WorkflowFunctionExamples` - WorkflowAgent 工作流示例

