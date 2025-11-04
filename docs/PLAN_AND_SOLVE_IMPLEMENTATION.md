# PlanAndSolve 流程实现详解

## 📋 概述

`PlanAndSolve` 是 OxyGent 框架中的一个**预设流程（Flow）**，实现了"规划-执行"模式的问题解决策略。

### 核心思想

> **先制定完整计划，然后逐步骤执行**

与 `ReActAgent` 的"边想边干"不同，`PlanAndSolve` 采用"想好再干"的策略。

---

## 🏗️ Python 版本的实现

### 1. 类结构

```python
class PlanAndSolve(BaseFlow):
    """Plan-and-Solve Prompting Workflow."""
    
    # 核心参数
    max_replan_rounds: int = 30  # 最大重规划轮次
    planner_agent_name: str = "planner_agent"  # 规划者 Agent 名称
    executor_agent_name: str = "executor_agent"  # 执行者 Agent 名称
    enable_replanner: bool = False  # 是否启用重规划
    pre_plan_steps: List[str] = None  # 预设计划步骤（可选）
```

### 2. 执行流程（核心逻辑）

```python
async def _execute(self, oxy_request: OxyRequest) -> OxyResponse:
    plan_steps = []  # 计划步骤列表
    past_steps = ""  # 已完成步骤记录
    original_query = oxy_request.get_query()
    
    # ========== 阶段1：规划阶段 ==========
    if (current_round == 0) and (self.pre_plan_steps is None):
        # 调用规划者 Agent 制定计划
        oxy_response = await oxy_request.call(
            callee=self.planner_agent_name,
            arguments={"query": original_query}
        )
        # 解析计划（使用 Pydantic 解析器）
        plan_response = self.pydantic_parser_planner.parse(oxy_response.output)
        plan_steps = plan_response.steps  # 得到步骤列表，如：["步骤1", "步骤2", "步骤3"]
    
    # ========== 阶段2：执行阶段 ==========
    for current_round in range(self.max_replan_rounds + 1):
        # 取第一个任务执行
        task = plan_steps[0]
        task_formatted = f"""
            We have finished the following steps: {past_steps}
            The current step to execute is: {task}
            You should only execute the current step, and do not execute other steps.
        """.strip()
        
        # 调用执行者 Agent 执行当前步骤
        executor_response = await oxy_request.call(
            callee=self.executor_agent_name,
            arguments={"query": task_formatted}
        )
        
        # 记录已完成的任务
        past_steps += f"\ntask:{task}, execute task result:{executor_response.output}"
        
        # ========== 阶段3：重规划阶段（可选）==========
        if self.enable_replanner:
            # 根据执行结果决定是否需要调整计划
            replanner_response = await oxy_request.call(
                callee=self.replanner_agent_name,
                arguments={"query": generate_replan_query()}
            )
            # 如果返回答案，直接返回
            if is_answer(replanner_response):
                return replanner_response
            # 否则更新计划
            plan_steps = parse_new_plan(replanner_response)
        else:
            # 不启用重规划：移除已完成步骤
            plan_steps = plan_steps[1:]
            if len(plan_steps) == 0:
                return executor_response  # 所有步骤完成
    
    # 如果超过最大轮次，使用 LLM 总结结果
    return summarize_with_llm(plan_steps, past_steps)
```

### 3. 关键设计点

#### 3.1 数据模型

```python
class Plan(BaseModel):
    """计划数据模型"""
    steps: List[str] = Field(
        description="不同步骤列表，应按顺序排列"
    )

class Action(BaseModel):
    """重规划动作"""
    action: Union[Response, Plan] = Field(
        description="如果已回答用户，使用 Response；如果需要继续，使用 Plan"
    )
```

#### 3.2 解析器

```python
# 使用 Pydantic 解析器将 LLM 输出解析为结构化数据
pydantic_parser_planner = PydanticOutputParser(output_cls=Plan)
pydantic_parser_replanner = PydanticOutputParser(output_cls=Action)
```

---

## 💼 业务使用示例

### 完整示例代码

```python
from oxygent import MAS, Config, oxy

# 配置加载
Config.load_from_json("./config.json", env="default")

oxy_space = [
    # ========== 1. LLM 模型 ==========
    oxy.HttpLLM(
        name="default_llm",
        api_key=os.getenv("DEFAULT_LLM_API_KEY"),
        base_url=os.getenv("DEFAULT_LLM_BASE_URL"),
        model_name=os.getenv("DEFAULT_LLM_MODEL_NAME"),
    ),
    
    # ========== 2. 规划者 Agent（ChatAgent）==========
    oxy.ChatAgent(
        name="planner_agent",
        desc="负责制定执行计划的智能体",
        llm_model="default_llm",
        prompt="""
            对于给定的目标，创建一个简单且可逐步执行的计划。
            计划应该简洁，每个步骤应该是一个独立的、完整的功能模块。
            确保每个步骤都是可执行的，并且包含所有必要的信息。
            最后一步的结果应该是最终答案。
        """.strip(),
    ),
    
    # ========== 3. 执行者 Agent（ReActAgent）==========
    oxy.ReActAgent(
        name="executor_agent",
        desc="负责执行每个步骤的智能体",
        sub_agents=["time_agent", "math_agent", "file_agent"],  # 可调用的子智能体
        tools=["joke_tool"],  # 可用的工具
        llm_model="default_llm",
        timeout=100,
        prompt="""
            你是一个有用的助手，可以使用以下工具：
            ${tools_description}
            
            ⚠️ 重要：你只需要完成计划中的**当前步骤**，不要做额外的事情。
            严格按照当前步骤的要求响应。
            如果需要工具，从上述工具列表中选择一个。不要选择其他工具。
            如果不需要工具，直接回答——不要输出其他内容。
        """,
    ),
    
    # ========== 4. PlanAndSolve 流程编排器 ==========
    oxy.PlanAndSolve(
        name="master_agent",  # 主控智能体名称
        is_master=True,  # 标记为主控智能体
        llm_model="default_llm",  # 备用 LLM 模型
        planner_agent_name="planner_agent",  # 规划者 Agent
        executor_agent_name="executor_agent",  # 执行者 Agent
        enable_replanner=False,  # 是否启用重规划
        timeout=100,  # 超时时间
    ),
    
    # ========== 5. 子智能体和工具（供 executor_agent 调用）==========
    oxy.ReActAgent(
        name="time_agent",
        desc="查询时间的工具",
        tools=["time_tools"],
        llm_model="default_llm",
    ),
    oxy.ReActAgent(
        name="math_agent",
        desc="数学计算工具",
        tools=["math_tools"],
        llm_model="default_llm",
    ),
    oxy.ReActAgent(
        name="file_agent",
        desc="文件操作工具",
        tools=["file_tools"],
        llm_model="default_llm",
    ),
]

# 启动服务
async def main():
    mas = await MAS.create(oxy_space=oxy_space)
    await mas.start_web_service(first_query="当前时间是什么？请保存到文件 log.txt")
```

### 执行流程示例

假设用户查询：`"当前时间是什么？请保存到文件 log.txt"`

```
1. 用户发起请求
   ↓
2. PlanAndSolve.master_agent 接收请求
   ↓
3. 【规划阶段】调用 planner_agent
   输入: "当前时间是什么？请保存到文件 log.txt"
   输出: Plan(steps=[
        "1. 查询当前时间",
        "2. 将时间信息保存到文件 log.txt"
    ])
   ↓
4. 【执行阶段-步骤1】调用 executor_agent
   输入: "已完成步骤：无\n当前步骤：查询当前时间"
   → executor_agent 内部推理 → 调用 time_agent
   → time_agent 查询时间 → 返回 "2024-01-15 14:30:00"
   ↓
5. 【执行阶段-步骤2】调用 executor_agent
   输入: "已完成步骤：task:查询当前时间, result:2024-01-15 14:30:00\n当前步骤：将时间信息保存到文件 log.txt"
   → executor_agent 内部推理 → 调用 file_agent
   → file_agent 写入文件 → 返回 "文件已保存"
   ↓
6. 所有步骤完成，返回最终结果
```

---

## 🔄 PlanAndSolve vs ReActAgent

### 对比表

| 维度 | PlanAndSolve | ReActAgent |
|------|-------------|------------|
| **类型** | 流程编排器（Flow） | 智能体执行引擎（Agent） |
| **继承** | BaseFlow | LocalAgent → BaseAgent |
| **模式** | 规划-执行-评估 | 推理-行动循环 |
| **Agent 数量** | 2-3 个（planner + executor + replanner） | 1 个（自己） |
| **决策方式** | 预先规划，按计划执行 | 每轮动态决策 |
| **计划** | ✅ 显式计划列表 | ❌ 隐式推理链 |
| **适用场景** | 多步骤、可分解任务 | 需要动态调整的任务 |

### 组合使用

**重要发现**：PlanAndSolve 的 `executor_agent` 通常就是一个 **ReActAgent**！

```python
# PlanAndSolve 编排流程
oxy.PlanAndSolve(
    planner_agent_name="planner_agent",  # ChatAgent：负责规划
    executor_agent_name="executor_agent",  # ← ReActAgent：负责执行
)

# executor_agent 本身是 ReActAgent
oxy.ReActAgent(
    name="executor_agent",
    tools=["time_tools", "file_tools"],
    # 可以调用工具，也可以调用子智能体
)
```

**关系**：
- PlanAndSolve 是**框架**，用于编排多个 Agent
- ReActAgent 是**执行引擎**，可以在框架内扮演执行者
- 两者是**组合关系**，而非竞争关系

---

## 🎯 适用场景

### PlanAndSolve 适合

✅ **多步骤任务，可以预先分解**
- 电商订单处理：验证 → 记录 → 更新库存 → 通知
- 数据分析流程：读取 → 清洗 → 分析 → 生成报表
- 内容创作：确定主题 → 收集资料 → 写作 → 润色

✅ **需要清晰的任务进度追踪**
- 每个步骤可独立验证
- 步骤之间有明确的依赖关系

### ReActAgent 适合

✅ **需要动态调整的任务**
- 问答系统：根据查询动态决定调用哪些工具
- 交互式调试：不断测试、观察、调整
- 探索性任务：路径动态扩展

---

## 📝 Java 版本实现建议

### 设计要点

1. **继承关系**：应该继承 `BaseFlow` 或类似的流程基类
2. **两个 Agent**：
   - `plannerAgent`：规划者（可以是 ChatAgent）
   - `executorAgent`：执行者（通常是 ReActAgent）
3. **执行流程**：
   - 规划阶段：调用 plannerAgent 生成计划
   - 执行阶段：循环调用 executorAgent 执行每个步骤
   - 重规划阶段（可选）：根据结果调整计划

### 伪代码示例

```java
public class PlanAndSolve extends BaseFlow {
    private String plannerAgentName;
    private String executorAgentName;
    private boolean enableReplanner;
    private int maxReplanRounds;
    
    @Override
    public CompletableFuture<AgentResponse> execute(AgentRequest request) {
        // 1. 规划阶段
        AgentResponse planResponse = request.call(plannerAgentName, 
            Map.of("query", request.getQuery())).join();
        List<String> planSteps = parsePlan(planResponse);
        
        // 2. 执行阶段
        String pastSteps = "";
        for (String step : planSteps) {
            String taskQuery = String.format(
                "已完成步骤：%s\n当前步骤：%s", pastSteps, step
            );
            AgentResponse execResponse = request.call(executorAgentName,
                Map.of("query", taskQuery)).join();
            pastSteps += String.format("\ntask:%s, result:%s", 
                step, execResponse.getOutput());
        }
        
        // 3. 返回最终结果
        return CompletableFuture.completedFuture(lastResponse);
    }
}
```

---

## ✅ 总结

1. **PlanAndSolve 是流程编排器**，负责组织多个 Agent 的协作
2. **采用"规划-执行"模式**，先制定完整计划，再逐步骤执行
3. **执行者通常是 ReActAgent**，说明两者可以组合使用
4. **适用于可分解的多步骤任务**，需要清晰的步骤追踪

**核心思想**：
> PlanAndSolve 是"指挥官"，负责制定作战计划；ReActAgent 是"士兵"，负责执行具体任务。

