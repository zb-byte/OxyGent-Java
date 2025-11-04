# PlanAndSolve 步骤传递机制详解

## 📋 核心问题

**是的，步骤都是汉字描述（或任意文本），然后拼接到下一步的 prompt 中。**

---

## 🔄 执行流程详解

### 1. 规划阶段：生成步骤列表

```java
// planner_agent 生成的计划
Plan plan = PlanParser.parsePlan(plannerResponse.getOutput());
planSteps = ["分析需求 req-001", "编写代码", "生成测试用例"]
```

**步骤特点**：
- ✅ 可以是汉字描述（如："分析需求 req-001"）
- ✅ 可以是英文描述（如："Analyze requirement req-001"）
- ✅ 可以是任意文本格式
- ✅ 每个步骤是一个独立的、可执行的描述

---

### 2. 执行阶段：步骤传递机制

#### 步骤1：执行第一个步骤

```java
// 当前步骤
String task = "分析需求 req-001";  // 汉字描述

// 构建 prompt（拼接到 prompt 中）
String taskFormatted = String.format(
    "We have finished the following steps: %s\n" +
    "The current step to execute is: %s\n" +
    "You should only execute the current step, and do not execute other steps in our plan.",
    "None",  // pastSteps 为空（第一次执行）
    task     // "分析需求 req-001"
);

// 调用 executor_agent
AgentResponse executorResponse = request.call(
    executorAgentName,
    Map.of("query", taskFormatted)  // ← 步骤描述拼接到 prompt 中
).join();

// 执行结果
String result = "需求分析完成：功能清单包含用户登录、商品浏览、订单管理";
```

#### 步骤2：执行第二个步骤（关键！）

```java
// 记录已完成步骤（累积 pastSteps）
pastSteps += String.format(
    "\ntask:%s, execute task result:%s",
    "分析需求 req-001",  // ← 步骤描述（汉字）
    "需求分析完成：功能清单包含用户登录、商品浏览、订单管理"  // ← 执行结果
);
// pastSteps = "task:分析需求 req-001, execute task result:需求分析完成：功能清单包含用户登录、商品浏览、订单管理"

// 当前步骤
String task = "编写代码";  // 第二个步骤（汉字描述）

// 构建 prompt（拼接到 prompt 中）
String taskFormatted = String.format(
    "We have finished the following steps: %s\n" +  // ← pastSteps 包含已完成的步骤和结果
    "The current step to execute is: %s\n",        // ← 当前步骤（汉字描述）
    pastSteps,  // ← 已完成的步骤和结果
    task        // ← "编写代码"
);

// 调用 executor_agent
AgentResponse executorResponse = request.call(
    executorAgentName,
    Map.of("query", taskFormatted)  // ← 包含历史步骤和当前步骤的完整 prompt
).join();
```

---

## 📝 完整示例

### 规划阶段输出

```
planner_agent 生成的计划：
["分析需求 req-001", "编写代码", "生成测试用例"]
```

### 执行阶段 Prompt 传递

#### Round 1: 执行"分析需求 req-001"

**传递给 executor_agent 的 prompt**：
```
We have finished the following steps: None
The current step to execute is: 分析需求 req-001
You should only execute the current step, and do not execute other steps in our plan.
```

**执行结果**：
```
需求分析完成：功能清单包含用户登录、商品浏览、订单管理
```

**pastSteps 更新**：
```
task:分析需求 req-001, execute task result:需求分析完成：功能清单包含用户登录、商品浏览、订单管理
```

---

#### Round 2: 执行"编写代码"

**传递给 executor_agent 的 prompt**：
```
We have finished the following steps: 
task:分析需求 req-001, execute task result:需求分析完成：功能清单包含用户登录、商品浏览、订单管理
The current step to execute is: 编写代码
You should only execute the current step, and do not execute other steps in our plan.
```

**执行结果**：
```
代码编写完成：已实现用户登录模块（UserController.java, UserService.java）
```

**pastSteps 更新**：
```
task:分析需求 req-001, execute task result:需求分析完成：功能清单包含用户登录、商品浏览、订单管理
task:编写代码, execute task result:代码编写完成：已实现用户登录模块（UserController.java, UserService.java）
```

---

#### Round 3: 执行"生成测试用例"

**传递给 executor_agent 的 prompt**：
```
We have finished the following steps: 
task:分析需求 req-001, execute task result:需求分析完成：功能清单包含用户登录、商品浏览、订单管理
task:编写代码, execute task result:代码编写完成：已实现用户登录模块（UserController.java, UserService.java）
The current step to execute is: 生成测试用例
You should only execute the current step, and do not execute other steps in our plan.
```

**执行结果**：
```
测试用例生成完成：UserControllerTest.java, UserServiceTest.java
```

---

## 🎯 关键机制

### 1. 步骤描述（汉字/任意文本）

```java
// 步骤可以是任意文本描述
planSteps = [
    "分析需求 req-001",           // 汉字
    "Analyze requirement",         // 英文
    "调用 requirement_agent",      // 调用智能体
    "使用 read_file 工具读取文档",  // 调用工具
    "整合结果并生成报告"            // 综合任务
]
```

### 2. Prompt 拼接机制

每次调用 executor_agent 时，都会：
1. **拼接已完成步骤**：`pastSteps` 包含所有已完成步骤的描述和结果
2. **拼接当前步骤**：当前要执行的步骤描述
3. **传递给 executor_agent**：作为 `query` 参数

```java
String taskFormatted = 
    "We have finished the following steps: " + pastSteps + "\n" +
    "The current step to execute is: " + task + "\n" +
    "You should only execute the current step...";
```

### 3. 结果累积机制

```java
// 每次执行后，累积步骤和结果
pastSteps += String.format(
    "\ntask:%s, execute task result:%s",
    task,                    // 步骤描述（如："分析需求 req-001"）
    executorResponse.getOutput()  // 执行结果（如："需求分析完成..."）
);
```

---

## 💡 设计优势

### 1. 上下文传递

- ✅ executor_agent 可以看到**所有已完成步骤的描述和结果**
- ✅ executor_agent 知道**当前要执行的步骤**
- ✅ executor_agent 可以基于历史结果做出决策

### 2. 步骤独立性

- ✅ 每个步骤的描述是独立的文本
- ✅ 不依赖特定的数据结构
- ✅ 支持任意语言和格式

### 3. 可追踪性

- ✅ 每一步都有清晰的描述
- ✅ 每一步都有执行结果
- ✅ 完整的执行历史记录在 `pastSteps` 中

---

## 🔍 代码位置

### Java 实现

**文件**：`framework/agent/PlanAndSolve.java`

**关键代码**：
```java
// 第 155-163 行：构建 prompt
String task = planSteps.get(0);  // 步骤描述（汉字/任意文本）
String taskFormatted = String.format(
    "We have finished the following steps: %s\n" +
    "The current step to execute is: %s\n" +
    "You should only execute the current step...",
    pastSteps.isEmpty() ? "None" : pastSteps,  // 已完成的步骤和结果
    task  // 当前步骤描述
);

// 第 188-189 行：累积结果
pastSteps += String.format(
    "\ntask:%s, execute task result:%s",
    task, executorResponse.getOutput()
);
```

### Python 实现（参考）

**文件**：`OxyGent/oxygent/oxy/flows/plan_and_solve.py`

**关键代码**：
```python
# 第 109-114 行：构建 prompt
task = plan_steps[0]  # 步骤描述
task_formatted = f"""
    We have finished the following steps: {past_steps}
    The current step to execute is:{task}
    You should only execute the current step...
""".strip()

# 第 119-123 行：累积结果
past_steps = (
    past_steps
    + "\n"
    + f"task:{task}, execute task result:{excutor_response.output}"
)
```

---

## ✅ 总结

1. **步骤是文本描述**：可以是汉字、英文或任意文本
2. **步骤拼接到 prompt**：通过 `taskFormatted` 字符串拼接
3. **结果会累积**：`pastSteps` 包含所有已完成步骤的描述和结果
4. **上下文传递**：executor_agent 可以看到完整的历史和当前步骤

**执行流程**：
```
步骤描述（汉字） → 拼接到 prompt → 传递给 executor_agent → 执行 → 记录结果 → 下一步
```

