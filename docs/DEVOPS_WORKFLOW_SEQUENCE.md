# DevOps 编排服务流程时序图

本文档用时序图展示 DevOps 编排服务的完整执行流程。

---

## 📊 完整流程时序图

### 详细版本（包含所有细节）

[查看完整时序图](./devops_workflow_sequence.puml)

**关键流程**:
1. **应用启动与初始化**: Spring Boot 容器启动，初始化所有服务
2. **执行DevOps流程**: DevOpsOrchestrationService 创建请求并调用主控智能体
3. **ReAct循环 - 需求分析阶段**: 主控智能体调用需求分析智能体
4. **ReAct循环 - 代码编写阶段**: 主控智能体调用编码智能体（远程SSE）
5. **结果处理**: 收集所有结果，返回完整流程报告

---

### 简化版本（核心流程）

[查看简化时序图](./devops_workflow_sequence_simple.puml)

**核心流程**:
1. DevOpsOrchestrationService 创建任务请求
2. 主控智能体开始 ReAct 循环
3. 调用需求分析智能体（Round 1）
4. 调用编码智能体（Round 2）
5. 返回最终结果（Round 3）

---

## 🔄 流程详解

### 阶段1: 应用启动与初始化

```
Application (启动)
    ↓
Spring Boot 容器启动
    ↓
DevOpsOrchestrationService 初始化
    ↓
获取 AgentFramework 实例（已包含所有注册的智能体）
```

**关键点**:
- 所有智能体在 `AgentService` 初始化时已注册完成
- `DevOpsOrchestrationService` 只获取框架引用，不执行初始化逻辑

---

### 阶段2: 执行DevOps流程

```
DevOpsOrchestrationService.executeDevOpsWorkflow()
    ↓
构建任务描述（buildTaskDescription）
    ↓
创建 AgentRequest
    - query: 任务描述
    - caller: "user"
    - targetAgent: "devops_master"
    ↓
调用 AgentFramework.chatWithMaster(request)
```

---

### 阶段3: ReAct循环 - 需求分析阶段

```
devops_master (ReAct循环 Round 1)
    ↓
1. 构建上下文（包含历史记录）
    ↓
2. 调用 LLM 决策
    - LLM 返回: "调用 requirement_agent"
    ↓
3. 解析决策: TOOL_CALL
    ↓
4. 执行工具调用
    - request.call("requirement_agent", arguments)
    - 克隆请求，更新调用栈
    ↓
5. 调用 requirement_agent.execute()
    ↓
requirement_agent (ReAct循环)
    ↓
- 可能需要调用 MCP 工具（read_file）读取需求文档
- 分析需求，生成需求分析报告
    ↓
返回 AgentResponse (需求分析报告)
    ↓
devops_master 更新 react_memory
```

**关键点**:
- 使用 `request.call()` 方法自动处理上下文传递和调用栈更新
- `requirement_agent` 可以独立使用 MCP 工具执行具体操作
- 所有历史记录自动保存在 `react_memory` 中

---

### 阶段4: ReAct循环 - 代码编写阶段

```
devops_master (ReAct循环 Round 2)
    ↓
1. 构建上下文（包含需求分析结果）
    ↓
2. 调用 LLM 决策
    - LLM 返回: "调用 code_agent"
    ↓
3. 解析决策: TOOL_CALL
    ↓
4. 执行工具调用
    - request.call("code_agent", arguments)
    ↓
5. 调用 code_agent.execute()
    ↓
code_agent (SSEOxyGent - 远程智能体)
    ↓
- 通过 HTTP + SSE 协议调用远程服务器
- 接收流式响应（answer/tool_call/observation）
- 解析并收集完整响应
    ↓
返回 AgentResponse (代码实现)
    ↓
devops_master 更新 react_memory
```

**关键点**:
- `code_agent` 是远程智能体，通过 SSE 协议调用
- 支持流式响应，实时接收处理结果
- 远程调用的结果会自动传递回主控智能体

---

### 阶段5: 返回最终结果

```
devops_master (ReAct循环 Round 3)
    ↓
1. 构建上下文（包含所有结果）
    ↓
2. 调用 LLM 决策
    - LLM 返回: "ANSWER - 返回最终结果"
    ↓
3. 解析决策: ANSWER
    ↓
4. 返回 AgentResponse
    - state: COMPLETED
    - output: 完整流程报告（需求分析 + 代码实现）
    ↓
DevOpsOrchestrationService 接收结果
    ↓
打印最终结果
```

---

## 📋 关键组件说明

### DevOpsOrchestrationService
- **职责**: 业务流程编排入口
- **关键方法**: `executeDevOpsWorkflow()`
- **作用**: 创建任务请求，调用主控智能体，处理结果

### AgentFramework
- **职责**: 智能体注册表和管理
- **关键方法**: `chatWithMaster()`, `getAgent()`
- **作用**: 路由请求到正确的智能体

### devops_master (主控智能体)
- **类型**: ReActAgent
- **职责**: 流程编排和协调
- **ReAct循环**: 最多10轮
- **子智能体**: requirement_agent, code_agent

### requirement_agent (需求分析智能体)
- **类型**: ReActAgent
- **职责**: 需求分析和文档生成
- **ReAct循环**: 最多5轮
- **可用工具**: MCP 文件工具（read_file, list_directory）

### code_agent (编码智能体)
- **类型**: SSEOxyGent (远程智能体)
- **职责**: 代码编写和实现
- **通信方式**: HTTP + SSE 协议
- **服务器**: 远程服务器（可配置）

---

## 🔄 ReAct 循环机制

### 主控智能体的 ReAct 循环

```
for round in 0..maxRounds:
    1. 构建上下文（buildMessages）
       - 系统提示
       - 历史记录（react_memory）
       - 当前任务
    
    2. 调用 LLM 决策
       - LLM 返回: 工具调用或最终答案
    
    3. 解析决策（parseLLMResponse）
       - TOOL_CALL: 调用子智能体或工具
       - ANSWER: 返回最终结果
    
    4. 执行工具调用（executeToolCallWithRetry）
       - 使用 request.call() 方法
       - 自动处理权限、超时、重试
       - 更新调用栈和上下文
    
    5. 更新 react_memory
       - 记录 LLM 响应
       - 记录观察结果（工具调用结果）
```

### 子智能体的 ReAct 循环

```
子智能体（如 requirement_agent）也有独立的 ReAct 循环：
- 可以调用 MCP 工具执行具体操作
- 可以调用其他子智能体（如果配置）
- 返回结果给主控智能体
```

---

## 🎯 数据流

### 请求流

```
User
  ↓
DevOpsOrchestrationService
  ↓ AgentRequest
AgentFramework
  ↓ AgentRequest (caller="user", targetAgent="devops_master")
devops_master
  ↓ AgentRequest (cloneWith, caller="devops_master", targetAgent="requirement_agent")
requirement_agent
  ↓ AgentRequest (caller="requirement_agent", targetAgent="read_file")
MCP工具
```

### 响应流

```
MCP工具
  ↓ AgentResponse (state=COMPLETED, output=文件内容)
requirement_agent
  ↓ AgentResponse (state=COMPLETED, output=需求分析报告)
devops_master
  ↓ AgentResponse (state=COMPLETED, output=完整流程报告)
AgentFramework
  ↓ AgentResponse
DevOpsOrchestrationService
  ↓ AgentResponse
User
```

---

## 💡 关键特性

### 1. 自动上下文传递
- 使用 `request.call()` 方法自动传递上下文
- `sharedData` 和 `groupData` 在同一次请求中共享
- 调用栈自动更新（`callStack`, `nodeIdStack`）

### 2. 权限校验
- `request.call()` 方法中自动检查权限
- 如果智能体需要权限，检查调用者是否在允许列表中
- 权限不足时返回 `AgentState.SKIPPED`

### 3. 超时控制
- 智能体可以配置超时时间（`getTimeout()`）
- 使用 `CompletableFuture.orTimeout()` 实现
- 超时后返回 `AgentState.FAILED`

### 4. 重试机制
- `executeToolCallWithRetry()` 方法支持重试
- 根据智能体的 `getRetries()` 配置重试次数
- 重试间隔由 `getDelay()` 控制

### 5. 远程调用支持
- `code_agent` 通过 SSE 协议调用远程服务器
- 支持流式响应，实时接收处理结果
- 自动解析 SSE 消息格式（answer/tool_call/observation）

---

## 📝 使用示例

### 查看时序图

**使用 PlantUML 工具**:
```bash
# 安装 PlantUML
npm install -g @plantuml/plantuml

# 生成时序图
plantuml docs/devops_workflow_sequence.puml
plantuml docs/devops_workflow_sequence_simple.puml
```

**在线查看**:
- 访问 [PlantUML Online Server](http://www.plantuml.com/plantuml/uml/)
- 复制 `.puml` 文件内容
- 查看生成的时序图

---

## 🔗 相关文档

- [A2A 核心思路对比](./A2A_CORE_COMPARISON.md) - 了解 A2A 通信机制
- [启动顺序说明](./STARTUP_SEQUENCE.md) - 了解应用启动流程
- [业务流程位置指南](./WHERE_TO_ADD_BUSINESS_LOGIC.md) - 了解在哪里添加业务逻辑

