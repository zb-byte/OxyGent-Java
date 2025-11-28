package framework.agent;

import framework.model.AgentRequest;
import framework.model.AgentResponse;
import framework.model.AgentState;
import java.util.concurrent.CompletableFuture;

/**
 * WorkflowAgent - 工作流智能体
 * 
 * 核心能力：
 * - 直接执行用户提供的一个自定义业务流程函数
 * - 不做推理、不调工具，仅调用注入的函数
 * - 把函数返回作为最终输出
 * 
 * 工作流函数可以：
 * - 获取请求信息（query、memory、arguments 等）
 * - 调用其他 Agent（通过 request.call()）
 * - 调用工具（通过 request.call()）
 * - 调用 LLM（通过 request.call()）
 * - 执行自定义业务逻辑
 * 
 * 使用示例：
 * <pre>
 * // 方式1：使用 Lambda 表达式
 * WorkflowAgent workflowAgent = new WorkflowAgent(
 *     "workflow_agent", "工作流智能体", false,
 *     request -> {
 *         String query = request.getQuery();
 *         // 调用其他 Agent
 *         AgentResponse response = request.call("other_agent", Map.of("query", query)).join();
 *         return CompletableFuture.completedFuture("结果: " + response.getOutput());
 *     }
 * );
 * 
 * // 方式2：使用同步方法（会自动包装为异步）
 * WorkflowAgent workflowAgent = new WorkflowAgent(
 *     "workflow_agent", "工作流智能体", false,
 *     WorkflowFunction.fromSync(request -> {
 *         return "处理结果";
 *     })
 * );
 * </pre>
 */
public class WorkflowAgent implements Agent {
    private final String name;
    private final String description;
    private final boolean isMaster;
    private final WorkflowFunction workflowFunction; // 工作流函数
    
    private AgentFramework framework;
    
    /**
     * 构造函数
     * 
     * @param name 智能体名称
     * @param description 智能体描述
     * @param isMaster 是否为主控智能体
     * @param workflowFunction 工作流函数（如果为 null，则返回错误）
     */
    public WorkflowAgent(String name, String description, boolean isMaster,
                        WorkflowFunction workflowFunction) {
        this.name = name;
        this.description = description;
        this.isMaster = isMaster;
        this.workflowFunction = workflowFunction;
    }
    
    @Override
    public CompletableFuture<AgentResponse> execute(AgentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("\n⚙️  [" + name + "] 开始执行工作流: " + request.getQuery());
            
            // 确保请求对象有框架引用
            if (request.getFramework() == null) {
                request.setFramework(framework);
            }
            
            if (workflowFunction == null) {
                System.out.println("  ❌ 未设置工作流函数");
                return new AgentResponse(
                    AgentState.FAILED,
                    "未设置工作流函数",
                    null,
                    request
                );
            }
            
            try {
                // 执行工作流函数
                String result = workflowFunction.execute(request).join();
                System.out.println("  ✅ 工作流执行完成: " + result.substring(0, Math.min(100, result.length())) + "...");
                
                return new AgentResponse(
                    AgentState.COMPLETED,
                    result,
                    null,
                    request
                );
            } catch (Exception e) {
                System.out.println("  ❌ 工作流执行失败: " + e.getMessage());
                return new AgentResponse(
                    AgentState.FAILED,
                    "工作流执行失败: " + e.getMessage(),
                    null,
                    request
                );
            }
        });
    }
    
    // ========== Getters ==========
    
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
}

