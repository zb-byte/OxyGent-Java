package framework.agent;

import framework.model.AgentRequest;
import framework.model.AgentResponse;
import framework.model.AgentState;
import framework.model.Plan;
import framework.model.Action;
import framework.model.Response;
import framework.llm.LLMClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * PlanAndSolve 流程实现（框架核心）
 * 
 * 对应 Python 版本的 PlanAndSolve Flow
 * 
 * 核心思想：先制定完整计划，然后逐步骤执行
 * - 规划阶段：调用 planner_agent 生成执行计划
 * - 执行阶段：循环调用 executor_agent 执行每个步骤
 * - 重规划阶段（可选）：根据执行结果调整计划
 * 
 * 使用场景：
 * - 多步骤、可分解的任务
 * - 需要清晰的步骤追踪
 * - 适合预先规划的场景
 */
public class PlanAndSolve implements Agent {
    private final String name;
    private final String description;
    private final boolean isMaster;
    private final String plannerAgentName;
    private final String executorAgentName;
    private final boolean enableReplanner;
    private final String replannerAgentName;
    private final int maxReplanRounds;
    private final List<String> prePlanSteps;
    private final LLMClient llmClient;  // 用于备用 LLM 调用
    
    private AgentFramework framework;
    
    /**
     * 构造函数
     * 
     * @param name 智能体名称
     * @param description 描述
     * @param isMaster 是否为主控智能体
     * @param plannerAgentName 规划者 Agent 名称
     * @param executorAgentName 执行者 Agent 名称
     * @param enableReplanner 是否启用重规划
     * @param replannerAgentName 重规划者 Agent 名称（如果启用重规划）
     * @param maxReplanRounds 最大重规划轮次
     * @param prePlanSteps 预设计划步骤（可选，如果提供则跳过规划阶段）
     * @param llmClient LLM 客户端（用于备用调用）
     */
    public PlanAndSolve(String name, String description, boolean isMaster,
                       String plannerAgentName, String executorAgentName,
                       boolean enableReplanner, String replannerAgentName,
                       int maxReplanRounds, List<String> prePlanSteps,
                       LLMClient llmClient) {
        this.name = name;
        this.description = description;
        this.isMaster = isMaster;
        this.plannerAgentName = plannerAgentName;
        this.executorAgentName = executorAgentName;
        this.enableReplanner = enableReplanner;
        this.replannerAgentName = replannerAgentName;
        this.maxReplanRounds = maxReplanRounds;
        this.prePlanSteps = prePlanSteps;
        this.llmClient = llmClient;
    }
    
    /**
     * 简化构造函数（不启用重规划）
     */
    public PlanAndSolve(String name, String description, boolean isMaster,
                       String plannerAgentName, String executorAgentName,
                       int maxReplanRounds, List<String> prePlanSteps,
                       LLMClient llmClient) {
        this(name, description, isMaster, plannerAgentName, executorAgentName,
             false, null, maxReplanRounds, prePlanSteps, llmClient);
    }
    
    @Override
    public CompletableFuture<AgentResponse> execute(AgentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("\n📋 [" + name + "] 开始 PlanAndSolve 流程: " + request.getQuery());
            
            // 确保请求对象有框架引用
            if (request.getFramework() == null) {
                request.setFramework(framework);
            }
            
            String originalQuery = request.getQuery();
            List<String> planSteps;
            String pastSteps = "";
            String planStr = "";
            
            // ========== 阶段1：规划阶段 ==========
            if (prePlanSteps != null && !prePlanSteps.isEmpty()) {
                // 使用预设计划
                planSteps = new ArrayList<>(prePlanSteps);
                planStr = formatPlanSteps(planSteps);
                System.out.println("  📝 使用预设计划: " + planStr);
            } else {
                // 调用规划者 Agent 生成计划
                System.out.println("  🧠 调用规划者: " + plannerAgentName);
                try {
                    AgentResponse planResponse = request.call(
                        plannerAgentName,
                        Map.of("query", originalQuery)
                    ).join();
                    
                    if (planResponse.getState() == AgentState.FAILED) {
                        return new AgentResponse(
                            AgentState.FAILED,
                            "规划阶段失败: " + planResponse.getOutput(),
                            null,
                            request
                        );
                    }
                    
                    // 解析计划
                    Plan plan = PlanParser.parsePlan(planResponse.getOutput());
                    planSteps = plan.getSteps();
                    planStr = formatPlanSteps(planSteps);
                    
                    System.out.println("  ✅ 计划生成成功: " + planStr);
                } catch (Exception e) {
                    return new AgentResponse(
                        AgentState.FAILED,
                        "规划阶段异常: " + e.getMessage(),
                        null,
                        request
                    );
                }
            }
            
            if (planSteps == null || planSteps.isEmpty()) {
                return new AgentResponse(
                    AgentState.FAILED,
                    "未能生成有效的执行计划",
                    null,
                    request
                );
            }
            
            // ========== 阶段2：执行阶段 ==========
            AgentResponse lastExecutorResponse = null;
            
            for (int round = 0; round <= maxReplanRounds && !planSteps.isEmpty(); round++) {
                // 取第一个任务执行
                String task = planSteps.get(0);
                String taskFormatted = String.format(
                    "We have finished the following steps: %s\n" +
                    "The current step to execute is: %s\n" +
                    "You should only execute the current step, and do not execute other steps in our plan. " +
                    "Do not execute more than one step continuously or skip any step.",
                    pastSteps.isEmpty() ? "None" : pastSteps,
                    task
                );
                
                System.out.println("  ⚙️  执行步骤 " + (round + 1) + ": " + task);
                
                try {
                    // 调用执行者 Agent
                    AgentResponse executorResponse = request.call(
                        executorAgentName,
                        Map.of("query", taskFormatted)
                    ).join();
                    
                    lastExecutorResponse = executorResponse;
                    
                    // 检查执行结果
                    if (executorResponse.getState() == AgentState.FAILED) {
                        System.out.println("  ❌ 步骤执行失败: " + executorResponse.getOutput());
                        // 如果失败，可以选择继续或返回失败
                        // 这里选择继续，让重规划或后续步骤处理
                    } else if (executorResponse.getState() == AgentState.SKIPPED) {
                        System.out.println("  ⚠️  步骤被跳过: " + executorResponse.getOutput());
                    } else {
                        System.out.println("  ✅ 步骤执行成功: " + executorResponse.getOutput());
                    }
                    
                    // 记录已完成的任务
                    pastSteps += String.format("\ntask:%s, execute task result:%s",
                        task, executorResponse.getOutput());
                    
                    // ========== 阶段3：重规划阶段（可选）==========
                    if (enableReplanner && replannerAgentName != null) {
                        System.out.println("  🔄 调用重规划者: " + replannerAgentName);
                        
                        String replanQuery = String.format(
                            "The target of user is:\n%s\n\n" +
                            "The origin plan is:\n%s\n\n" +
                            "We have finished the following steps:\n%s\n\n" +
                            "Please update the plan considering the mentioned information. " +
                            "If no more operation is supposed, Use **Response** to answer the user. " +
                            "Otherwise, please update the plan. The plan should only contain the steps to be executed, " +
                            "and do not include the past steps or any other information.",
                            originalQuery, planStr, pastSteps
                        );
                        
                        try {
                            AgentResponse replannerResponse = request.call(
                                replannerAgentName,
                                Map.of("query", replanQuery)
                            ).join();
                            
                            Action action = PlanParser.parseAction(replannerResponse.getOutput());
                            
                            if (action.isResponse()) {
                                // 重规划者返回了直接答案
                                Response response = action.getResponse();
                                System.out.println("  ✅ 重规划者返回最终答案");
                                return new AgentResponse(
                                    AgentState.COMPLETED,
                                    response.getResponse(),
                                    null,
                                    request
                                );
                            } else if (action.isPlan()) {
                                // 重规划者返回了新计划
                                Plan newPlan = action.getPlan();
                                planSteps = newPlan.getSteps();
                                planStr = formatPlanSteps(planSteps);
                                System.out.println("  📝 计划已更新: " + planStr);
                            }
                        } catch (Exception e) {
                            System.out.println("  ⚠️  重规划异常: " + e.getMessage() + "，继续执行原计划");
                        }
                    } else {
                        // 不启用重规划：移除已完成步骤
                        planSteps.remove(0);
                        
                        if (planSteps.isEmpty()) {
                            // 所有步骤完成
                            System.out.println("  ✅ 所有步骤执行完成");
                            return lastExecutorResponse != null ? lastExecutorResponse :
                                new AgentResponse(
                                    AgentState.COMPLETED,
                                    "所有步骤已完成",
                                    null,
                                    request
                                );
                        }
                    }
                } catch (Exception e) {
                    return new AgentResponse(
                        AgentState.FAILED,
                        "执行步骤时发生异常: " + e.getMessage(),
                        null,
                        request
                    );
                }
            }
            
            // 如果超过最大轮次，使用最后一个执行结果
            if (lastExecutorResponse != null) {
                return lastExecutorResponse;
            }
            
            // 如果还有未完成的步骤，使用 LLM 总结
            String summaryQuery = String.format(
                "Your objective was this: %s\n---\nFor the following plan: %s\n" +
                "We have completed some steps but not all. Please provide a summary based on what we have accomplished.",
                originalQuery, planStr
            );
            
            try {
                if (llmClient != null) {
                    List<Map<String, String>> messages = new ArrayList<>();
                    messages.add(Map.of("role", "system", 
                        "content", "Please answer user questions based on the given plan."));
                    messages.add(Map.of("role", "user", "content", summaryQuery));
                    
                    String llmResponse = llmClient.chat(messages);
                    return new AgentResponse(
                        AgentState.COMPLETED,
                        llmResponse,
                        null,
                        request
                    );
                }
            } catch (Exception e) {
                // LLM 调用失败，返回部分结果
            }
            
            return new AgentResponse(
                AgentState.COMPLETED,
                "部分步骤已完成，但未完成全部计划。已完成步骤: " + pastSteps,
                null,
                request
            );
        });
    }
    
    /**
     * 格式化计划步骤为字符串
     */
    private String formatPlanSteps(List<String> steps) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            sb.append(String.format("%d. %s", i + 1, steps.get(i)));
            if (i < steps.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public boolean isMaster() {
        return isMaster;
    }
    
    @Override
    public void setFramework(AgentFramework framework) {
        this.framework = framework;
    }
    
    @Override
    public AgentFramework getFramework() {
        return framework;
    }
    
    @Override
    public long getTimeout() {
        return 0;  // 默认不设置超时，由子 Agent 控制
    }
    
    // Getters
    public String getPlannerAgentName() {
        return plannerAgentName;
    }
    
    public String getExecutorAgentName() {
        return executorAgentName;
    }
    
    public boolean isEnableReplanner() {
        return enableReplanner;
    }
    
    public String getReplannerAgentName() {
        return replannerAgentName;
    }
    
    public int getMaxReplanRounds() {
        return maxReplanRounds;
    }
    
    public List<String> getPrePlanSteps() {
        return prePlanSteps;
    }
}

