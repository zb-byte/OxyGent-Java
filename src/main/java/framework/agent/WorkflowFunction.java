package framework.agent;

import framework.model.AgentRequest;
import java.util.concurrent.CompletableFuture;

/**
 * 工作流函数接口
 * 
 * 用于 WorkflowAgent 的自定义业务流程执行。
 * 工作流函数可以：
 * - 获取请求信息（query、memory、arguments 等）
 * - 调用其他 Agent（通过 request.call()）
 * - 调用工具（通过 request.call()）
 * - 调用 LLM（通过 request.call()）
 * - 执行自定义业务逻辑
 * - 返回最终结果
 * 
 * 使用示例：
 * <pre>
 * // 方式1：实现接口
 * WorkflowFunction workflow = new WorkflowFunction() {
 *     {@literal @}Override
 *     public CompletableFuture<String> execute(AgentRequest request) {
 *         String query = request.getQuery();
 *         // 调用其他 Agent
 *         AgentResponse response = request.call("other_agent", Map.of("query", query)).join();
 *         return CompletableFuture.completedFuture("结果: " + response.getOutput());
 *     }
 * };
 * 
 * // 方式2：使用 Lambda 表达式
 * WorkflowFunction workflow = request -> {
 *     // 工作流逻辑
 *     return CompletableFuture.completedFuture("结果");
 * };
 * 
 * // 方式3：使用静态方法引用（如果是同步方法）
 * WorkflowFunction workflow = WorkflowFunction.fromSync(MyService::process);
 * </pre>
 */
@FunctionalInterface
public interface WorkflowFunction {
    /**
     * 执行工作流
     * 
     * @param request 智能体请求，包含用户查询、上下文等信息
     * @return 工作流执行结果（异步）
     */
    CompletableFuture<String> execute(AgentRequest request);
    
    /**
     * 创建一个同步的工作流函数（将同步方法包装为异步）
     * 
     * @param syncFunction 同步工作流函数
     * @return 异步工作流函数
     */
    static WorkflowFunction fromSync(java.util.function.Function<AgentRequest, String> syncFunction) {
        return request -> CompletableFuture.completedFuture(syncFunction.apply(request));
    }
}

