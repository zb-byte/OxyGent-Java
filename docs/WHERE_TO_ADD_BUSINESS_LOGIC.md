# 业务流程中增加业务逻辑的位置指南

## 📋 概述

本文档说明在业务流程的不同层级中，可以在哪些位置添加业务逻辑。

---

## 🎯 业务逻辑添加位置（从高层到低层）

### 1️⃣ **业务流程编排层** - `DevOpsOrchestrationService`

**文件**: `business/devops/service/DevOpsOrchestrationService.java`

**适用场景**: 
- ✅ 添加新的业务流程方法
- ✅ 业务流程的前置/后置处理
- ✅ 结果处理和格式化
- ✅ 业务规则验证

**示例**:

```java
@Service
public class DevOpsOrchestrationService {
    
    /**
     * ⭐ 添加新的业务流程方法
     */
    public AgentResponse executeCustomWorkflow(String param1, String param2) {
        // 1. 业务逻辑：参数验证
        if (param1 == null || param1.isEmpty()) {
            throw new IllegalArgumentException("param1 不能为空");
        }
        
        // 2. 业务逻辑：构建任务描述
        String taskDescription = buildCustomTaskDescription(param1, param2);
        
        // 3. 业务逻辑：调用智能体
        AgentRequest request = new AgentRequest(
            taskDescription,
            null,
            "user",
            "devops_master"
        );
        
        // 4. 业务逻辑：执行任务
        AgentResponse response = framework.chatWithMaster(request).join();
        
        // 5. 业务逻辑：后处理（结果解析、格式化等）
        return processResponse(response);
    }
    
    /**
     * ⭐ 添加业务规则验证
     */
    private void validateBusinessRules(String requirementId, String environment) {
        // 业务规则：环境检查
        if (!Arrays.asList("staging", "production", "dev").contains(environment)) {
            throw new IllegalArgumentException("不支持的环境: " + environment);
        }
        
        // 业务规则：需求ID格式检查
        if (!requirementId.matches("req-\\d+")) {
            throw new IllegalArgumentException("需求ID格式错误: " + requirementId);
        }
    }
    
    /**
     * ⭐ 添加结果处理逻辑
     */
    private AgentResponse processResponse(AgentResponse response) {
        // 业务逻辑：结果解析
        String output = response.getOutput();
        
        // 业务逻辑：提取关键信息
        Map<String, String> extractedData = extractKeyInfo(output);
        
        // 业务逻辑：保存到数据库（如果需要）
        // saveToDatabase(extractedData);
        
        // 业务逻辑：发送通知（如果需要）
        // sendNotification(extractedData);
        
        return response;
    }
}
```

---

### 2️⃣ **智能体定义层** - `AgentService`

**文件**: `business/devops/service/AgentService.java`

**适用场景**:
- ✅ 添加新的智能体
- ✅ 修改智能体的 Prompt（行为逻辑）
- ✅ 配置智能体的子智能体列表
- ✅ 设置智能体的执行参数

**示例**:

```java
@Service
public class AgentService {
    
    /**
     * ⭐ 添加新的智能体
     */
    private ReActAgent createNewAgent(LLMClient llmClient) {
        return new ReActAgent(
            "new_agent",
            "新智能体描述",
            false,
            llmClient,
            Arrays.asList("sub_agent_1", "sub_agent_2"),  // 子智能体列表
            Arrays.asList("tool_1", "tool_2"),              // 工具列表
            buildCustomPrompt(),                            // ⭐ 业务逻辑：Prompt
            10                                              // 最大轮次
        );
    }
    
    /**
     * ⭐ 修改智能体的 Prompt（业务逻辑）
     */
    private String buildCustomPrompt() {
        return """
            你是业务专家，负责处理以下业务场景：
            
            业务规则：
            1. 如果遇到A情况，执行B操作
            2. 如果遇到C情况，执行D操作
            
            业务约束：
            - 必须遵守公司的合规要求
            - 必须记录所有操作日志
            
            业务处理流程：
            1. 接收任务
            2. 验证业务规则
            3. 执行业务操作
            4. 返回结果
            
            请根据以上业务规则处理任务。
            """;
    }
    
    /**
     * ⭐ 修改主控智能体的流程逻辑
     */
    private ReActAgent createMasterAgent(LLMClient llmClient) {
        String workflowPrompt = """
            你是一个业务流程编排专家。
            
            ⭐ 自定义业务流程：
            1) **阶段1**：调用 agent_1
            2) **阶段2**：调用 agent_2
            3) **阶段3**：根据阶段2的结果决定调用 agent_3 或 agent_4
            
            业务规则：
            - 如果阶段2的结果包含"成功"，进入阶段3a
            - 如果阶段2的结果包含"失败"，返回阶段1重试
            
            请严格按照以上业务规则执行。
            """;
        
        return new ReActAgent(
            "devops_master",
            "主控智能体",
            true,
            llmClient,
            Arrays.asList("agent_1", "agent_2", "agent_3", "agent_4"),
            null,
            workflowPrompt,
            16
        );
    }
}
```

---

### 3️⃣ **子智能体层** - 各个智能体的 Prompt

**文件**: `business/devops/service/AgentService.java` 中的各个 `createXXXAgent()` 方法

**适用场景**:
- ✅ 修改特定智能体的专业领域
- ✅ 添加专业智能体的业务规则
- ✅ 定义智能体的输入输出格式

**示例**:

```java
/**
 * ⭐ 修改需求分析智能体的业务逻辑
 */
private ReActAgent createRequirementAgent(LLMClient llmClient) {
    return new ReActAgent(
        "requirement_agent",
        "需求分析智能体",
        false,
        llmClient,
        null,
        null,
        """
        你是需求分析专家。
        
        ⭐ 业务规则：
        1. 必须提取以下信息：
           - 功能需求列表
           - 非功能需求（性能、安全等）
           - 技术约束
           - 优先级
        
        2. 输出格式（JSON）：
        {
            "functions": [...],
            "non_functions": {...},
            "constraints": [...],
            "priority": "high|medium|low"
        }
        
        3. 如果需求不完整，必须明确标注缺失信息
        
        请按照以上业务规则分析需求。
        """,
        5
    );
}

/**
 * ⭐ 修改代码编写智能体的业务逻辑
 */
private ReActAgent createCodeAgent(LLMClient llmClient) {
    return new ReActAgent(
        "code_agent",
        "代码编写智能体",
        false,
        llmClient,
        null,
        null,
        """
        你是代码编写专家。
        
        ⭐ 业务规则：
        1. 必须遵循公司的代码规范：
           - 命名规范：驼峰命名
           - 注释规范：每个方法必须有JavaDoc
           - 测试覆盖率：>=80%
        
        2. 必须包含：
           - 单元测试
           - 集成测试
           - 错误处理
        
        3. 禁止：
           - 硬编码配置
           - 敏感信息
           - 不安全的API调用
        
        请按照以上业务规则编写代码。
        """,
        5
    );
}
```

---

### 4️⃣ **启动入口层** - `Application`

**文件**: `business/devops/Application.java`

**适用场景**:
- ✅ 添加启动时的初始化逻辑
- ✅ 添加命令行参数处理
- ✅ 添加多个业务流程的调用
- ✅ 添加结果的后处理

**示例**:

```java
@SpringBootApplication
public class Application implements CommandLineRunner {
    
    @Autowired
    private DevOpsOrchestrationService orchestrationService;
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 Java ReAct Agent Framework");
        
        // ⭐ 业务逻辑：解析命令行参数
        String mode = args.length > 0 ? args[0] : "default";
        String requirementId = args.length > 1 ? args[1] : "req-001";
        String environment = args.length > 2 ? args[2] : "staging";
        
        // ⭐ 业务逻辑：根据模式执行不同流程
        AgentResponse response;
        switch (mode) {
            case "devops":
                response = orchestrationService.executeDevOpsWorkflow(requirementId, environment);
                break;
            case "custom":
                response = orchestrationService.executeCustomWorkflow(requirementId, environment);
                break;
            default:
                response = orchestrationService.executeDevOpsWorkflow(requirementId, environment);
        }
        
        // ⭐ 业务逻辑：结果处理
        orchestrationService.printResult(response);
        
        // ⭐ 业务逻辑：保存结果到文件（如果需要）
        saveResultToFile(response, "output/result.txt");
    }
    
    private void saveResultToFile(AgentResponse response, String filePath) {
        // 业务逻辑实现
    }
}
```

---

## 📊 业务逻辑层级图

```
业务逻辑层级（从高到低）
│
├─ 1. 业务流程编排层 (DevOpsOrchestrationService)
│   ├─ 添加新的业务流程方法
│   ├─ 业务规则验证
│   ├─ 结果处理和后处理
│   └─ 数据转换和格式化
│
├─ 2. 智能体定义层 (AgentService)
│   ├─ 添加新的智能体
│   ├─ 修改智能体的 Prompt
│   ├─ 配置智能体关系
│   └─ 设置智能体参数
│
├─ 3. 子智能体层 (各个 createXXXAgent 方法)
│   ├─ 修改专业智能体的业务规则
│   ├─ 定义输入输出格式
│   └─ 设置专业约束
│
└─ 4. 启动入口层 (Application)
    ├─ 命令行参数处理
    ├─ 多流程调用
    └─ 结果保存和通知
```

---

## 🎯 不同场景的推荐位置

### 场景1: 添加新的业务流程

**推荐位置**: `DevOpsOrchestrationService`

```java
// 在 DevOpsOrchestrationService 中添加新方法
public AgentResponse executeNewWorkflow(String param) {
    // 业务逻辑
}
```

### 场景2: 修改现有流程的执行逻辑

**推荐位置**: `AgentService.createMasterAgent()` 中的 `workflowPrompt`

```java
// 修改主控智能体的 Prompt
String workflowPrompt = """
    新的业务流程步骤：
    1. ...
    2. ...
""";
```

### 场景3: 添加新的专业智能体

**推荐位置**: `AgentService` 中添加新的 `createXXXAgent()` 方法

```java
private ReActAgent createNewSpecialistAgent(LLMClient llmClient) {
    // 创建新的专业智能体
}
```

### 场景4: 修改智能体的专业规则

**推荐位置**: 对应智能体的 Prompt

```java
private ReActAgent createXXXAgent(LLMClient llmClient) {
    return new ReActAgent(
        // ...
        "新的业务规则和约束...",  // 修改这里
        // ...
    );
}
```

### 场景5: 添加业务验证和检查

**推荐位置**: `DevOpsOrchestrationService` 中的方法

```java
public AgentResponse executeWorkflow(String param) {
    // 业务验证
    validateBusinessRules(param);
    
    // 执行业务流程
    // ...
}
```

---

## ⚠️ 注意事项

### 1. 不要修改框架代码

❌ **不要修改**: `framework/` 目录下的代码
- 这些是框架代码，应该保持稳定
- 修改会影响其他业务模块

✅ **应该修改**: `business/` 目录下的代码
- 这是业务代码，可以自由修改

### 2. 业务逻辑分层原则

- **高层业务逻辑** → `DevOpsOrchestrationService`
- **流程编排逻辑** → `AgentService` 中的 Prompt
- **专业领域逻辑** → 各个智能体的 Prompt

### 3. Prompt 是业务逻辑的重要载体

- Prompt 定义了智能体的行为规则
- 修改 Prompt 可以改变智能体的业务逻辑
- 建议将业务规则清晰地写在 Prompt 中

---

## 💡 最佳实践

### 1. 业务流程方法放在编排服务中

```java
@Service
public class DevOpsOrchestrationService {
    // 所有业务流程方法都在这里
    public AgentResponse executeWorkflow1() { }
    public AgentResponse executeWorkflow2() { }
    public AgentResponse executeWorkflow3() { }
}
```

### 2. 智能体配置集中在 AgentService

```java
@Service
public class AgentService {
    // 所有智能体的创建和配置都在这里
    private void initializeAgents() {
        // 创建所有智能体
    }
}
```

### 3. 业务规则写在 Prompt 中

```java
String prompt = """
    业务规则：
    1. ...
    2. ...
    3. ...
""";
```

---

## 🎯 总结

**添加业务逻辑的位置**：

1. ✅ **业务流程方法** → `DevOpsOrchestrationService`
2. ✅ **流程编排逻辑** → `AgentService.createMasterAgent()` 的 Prompt
3. ✅ **专业智能体逻辑** → 各个 `createXXXAgent()` 的 Prompt
4. ✅ **启动逻辑** → `Application.run()`

**关键原则**：
- 高层业务逻辑 → 编排服务
- 流程逻辑 → Prompt
- 不要修改框架代码

