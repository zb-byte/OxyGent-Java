# 如何决定下一步使用哪个 Agent

## 📋 核心问题

**在 PlanAndSolve 流程中，如何知道下一步用哪个 agent 执行？**

---

## 🎯 两个层面的决策

### 层面1：PlanAndSolve 流程层面

**PlanAndSolve 固定调用 executor_agent**

```java
// PlanAndSolve.java 第 169-172 行
AgentResponse executorResponse = request.call(
    executorAgentName,  // ← 固定调用 executor_agent
    Map.of("query", taskFormatted)
).join();
```

**关键点**：
- ✅ PlanAndSolve **不决定**调用哪个 agent
- ✅ PlanAndSolve **总是调用同一个** executor_agent
- ✅ PlanAndSolve 只负责**按顺序执行步骤**

---

### 层面2：executor_agent 内部决策层面

**executor_agent（通常是 ReActAgent）通过 LLM 推理决定调用哪个 agent**

#### 决策流程

```
executor_agent 接收任务
  ↓
构建 Prompt（包含可用工具/智能体列表）
  ↓
调用 LLM 推理
  ↓
LLM 返回 JSON 决策
  ↓
解析决策 → 调用对应的 agent/tool
```

---

## 🔍 详细机制

### 1. 可用选项列表

**executor_agent 在创建时配置了可用的工具和子智能体**：

```java
// AgentService.java 第 387-393 行
ReActAgent executorAgent = new ReActAgent(
    "executor_agent",
    "执行者智能体",
    false,
    llmClient,
    Arrays.asList("requirement_agent", "code_agent"),  // ← 可调用的子智能体列表
    Arrays.asList("read_file", "write_file"),          // ← 可用的工具列表
    executorPrompt,
    10
);
```

### 2. Prompt 中包含可用选项

**executor_agent 的 systemPrompt 会包含可用工具和智能体列表**：

```java
// ReActAgent.java 第 172-195 行
private String buildSystemPrompt() {
    StringBuilder prompt = new StringBuilder(systemPrompt);
    
    // 添加可用工具列表
    if (!subAgents.isEmpty() || !tools.isEmpty()) {
        prompt.append("\n\n可用工具：");
        
        if (!subAgents.isEmpty()) {
            prompt.append("\n- 子智能体: ");
            prompt.append(String.join(", ", subAgents));  // ← "requirement_agent, code_agent"
        }
        
        if (!tools.isEmpty()) {
            prompt.append("\n- 工具: ");
            prompt.append(String.join(", ", tools));      // ← "read_file, write_file"
        }
        
        prompt.append("\n\n调用格式（JSON）:");
        prompt.append("\n{\"type\": \"tool_call\", \"tool_name\": \"工具名\", \"arguments\": {...}}");
    }
    
    return prompt.toString();
}
```

**传递给 LLM 的完整 Prompt 示例**：

```
你是一个执行助手，负责执行计划中的单个步骤。

重要提示：
1. 你只需要完成计划中的**当前步骤**，不要做额外的事情
2. 严格按照当前步骤的要求响应
3. 如果需要工具，从可用工具列表中选择
4. 如果不需要工具，直接回答——不要输出其他内容

可用工具：
- 子智能体: requirement_agent, code_agent
- 工具: read_file, write_file

调用格式（JSON）:
{"type": "tool_call", "tool_name": "工具名", "arguments": {"query": "..."}}
或直接回答:
{"type": "answer", "content": "..."}

---

当前任务：
We have finished the following steps: 
task:分析需求 req-001, execute task result:需求分析完成...
The current step to execute is: 编写代码
You should only execute the current step...
```

### 3. LLM 推理决策

**LLM 根据以下信息做出决策**：

1. **当前步骤描述**：`"编写代码"`
2. **已完成步骤和结果**：`"task:分析需求 req-001, execute task result:需求分析完成..."`
3. **可用选项列表**：`["requirement_agent", "code_agent", "read_file", "write_file"]`
4. **任务上下文**：整个任务的目标

**LLM 可能返回**：

```json
{
    "type": "tool_call",
    "tool_name": "code_agent",  // ← LLM 决定调用 code_agent
    "arguments": {
        "query": "根据需求分析结果编写代码"
    }
}
```

### 4. 解析并执行

```java
// ReActAgent.java 第 203-242 行
private LLMDecision parseLLMResponse(String response) {
    // 解析 JSON，提取 tool_name
    if (jsonStr.contains("\"type\": \"tool_call\"")) {
        String toolName = extractJsonValue(jsonStr, "tool_name");  // ← "code_agent"
        Map<String, Object> arguments = extractArguments(jsonStr);
        
        return new LLMDecision(DecisionType.TOOL_CALL, 
                              new ToolCall(toolName, arguments), null);
    }
    // ...
}

// 然后执行
AgentResponse toolResponse = request.call(toolName, arguments).join();
```

---

## 📊 完整决策流程示例

### 场景：执行步骤 "编写代码"

```
1. PlanAndSolve 调用 executor_agent
   ↓
2. executor_agent 构建 Prompt
   Prompt = {
     系统提示: "你是一个执行助手...",
     可用选项: ["requirement_agent", "code_agent", "read_file", "write_file"],
     当前步骤: "编写代码",
     历史步骤: "task:分析需求 req-001, result:需求分析完成..."
   }
   ↓
3. LLM 推理
   输入: Prompt
   输出: {
     "type": "tool_call",
     "tool_name": "code_agent",  // ← LLM 根据步骤描述和可用选项决定
     "arguments": {"query": "根据需求分析结果编写代码"}
   }
   ↓
4. executor_agent 解析决策
   提取: toolName = "code_agent"
   ↓
5. 调用 code_agent
   request.call("code_agent", arguments)
   ↓
6. code_agent 执行并返回结果
   ↓
7. executor_agent 记录结果
   pastSteps += "task:编写代码, result:代码编写完成..."
```

---

## 🎯 关键机制总结

### 1. PlanAndSolve 层面

- **固定调用**：总是调用 `executor_agent`
- **不决定**：不决定调用哪个具体 agent
- **职责**：按顺序执行步骤，管理执行流程

### 2. executor_agent 层面（ReActAgent）

- **LLM 决策**：通过 LLM 推理决定调用哪个 agent/tool
- **可用选项**：在创建时配置的 `subAgents` 和 `tools` 列表
- **决策依据**：
  - 当前步骤描述
  - 已完成步骤和结果
  - 可用选项列表
  - 任务上下文

### 3. 决策过程

```
可用选项列表（配置时确定）
    ↓
包含在 Prompt 中
    ↓
LLM 看到所有选项
    ↓
LLM 根据当前步骤选择最合适的 agent/tool
    ↓
返回 JSON 决策
    ↓
解析并执行
```

---

## 💡 设计优势

### 1. 灵活性

- ✅ LLM 可以根据上下文动态选择最合适的 agent
- ✅ 不需要硬编码规则
- ✅ 可以处理复杂的决策场景

### 2. 可配置性

- ✅ 通过 `subAgents` 和 `tools` 列表控制可用选项
- ✅ 可以限制 executor_agent 的权限范围
- ✅ 支持权限控制（通过 `getPermittedToolNameList()`）

### 3. 可解释性

- ✅ LLM 的决策过程可以查看
- ✅ 决策结果以 JSON 格式返回，便于解析
- ✅ 可以记录决策历史

---

## 🔧 如何控制 executor_agent 的选择

### 方法1：配置可用选项

```java
ReActAgent executorAgent = new ReActAgent(
    "executor_agent",
    "执行者智能体",
    false,
    llmClient,
    Arrays.asList("requirement_agent", "code_agent"),  // ← 只允许这两个
    Arrays.asList("read_file"),                         // ← 只允许这个工具
    executorPrompt,
    10
);
```

### 方法2：通过 Prompt 引导

```java
String executorPrompt = """
    你是一个执行助手。
    
    重要提示：
    - 如果步骤涉及需求分析，优先调用 requirement_agent
    - 如果步骤涉及代码编写，优先调用 code_agent
    - 如果步骤涉及文件操作，使用 read_file 或 write_file
    
    可用工具：
    ${tools_description}
    """;
```

### 方法3：权限控制

```java
// 创建带权限控制的 executor_agent
ReActAgent executorAgent = new ReActAgent(...) {
    @Override
    public boolean isPermissionRequired() {
        return true;  // 启用权限控制
    }
    
    @Override
    public List<String> getPermittedToolNameList() {
        return Arrays.asList("requirement_agent", "code_agent");  // 白名单
    }
};
```

---

## ✅ 总结

**如何知道下一步用哪个 agent 执行？**

1. **PlanAndSolve 层面**：固定调用 `executor_agent`，不决定具体 agent
2. **executor_agent 层面**：通过 **LLM 推理**决定，依据：
   - 当前步骤描述
   - 已完成步骤和结果
   - 可用选项列表（`subAgents` + `tools`）
   - 任务上下文
3. **LLM 决策**：LLM 看到所有可用选项，根据当前步骤选择最合适的 agent/tool
4. **控制方式**：通过配置可用选项、Prompt 引导、权限控制等方式影响 LLM 的决策

**核心思想**：
> PlanAndSolve 负责"做什么"（按步骤执行），executor_agent 负责"怎么做"（决定调用哪个 agent/tool）。

