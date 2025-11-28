package framework.agent.examples;

import framework.agent.WorkflowFunction;
import framework.model.AgentRequest;
import framework.model.AgentResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 工作流函数使用示例
 * 
 * 展示如何实现和使用 WorkflowFunction 接口
 */
public class WorkflowFunctionExamples {
    
    /**
     * 示例1：简单的工作流（直接返回结果）
     */
    public static WorkflowFunction simpleWorkflow() {
        return request -> {
            String query = request.getQuery();
            return CompletableFuture.completedFuture("处理完成: " + query);
        };
    }
    
    /**
     * 示例2：调用其他 Agent 的工作流
     */
    public static WorkflowFunction callAgentWorkflow() {
        return request -> {
            String query = request.getQuery();
            
            // 调用其他 Agent
            CompletableFuture<AgentResponse> agentResponse = request.call(
                "chat_agent",
                Map.of("query", query)
            );
            
            return agentResponse.thenApply(response -> {
                return "Agent 返回结果: " + response.getOutput();
            });
        };
    }
    
    /**
     * 示例3：调用工具的工作流
     */
    public static WorkflowFunction callToolWorkflow() {
        return request -> {
            String query = request.getQuery();
            
            // 调用工具
            CompletableFuture<AgentResponse> toolResponse = request.call(
                "calculator_tool",
                Map.of("query", query)
            );
            
            return toolResponse.thenApply(response -> {
                return "工具计算结果: " + response.getOutput();
            });
        };
    }
    
    /**
     * 示例4：复杂工作流（多步骤执行）
     * 对应 Python 版本的 demo_workflow_agent.py
     */
    public static WorkflowFunction complexWorkflow() {
        return request -> {
            // 1. 获取查询
            String query = request.getQuery();
            System.out.println("--- Current query ---: " + query);
            
            // 2. 调用 LLM 获取精度要求
            String question = "用户的问题是" + query + "，用户想要小数点后多少位圆周率？直接回答数字";
            CompletableFuture<AgentResponse> llmResponse = request.call(
                "default_llm",
                Map.of(
                    "messages", java.util.Arrays.asList(
                        Map.of("role", "system", "content", "You are a helpful assistant."),
                        Map.of("role", "user", "content", question)
                    ),
                    "llm_params", Map.of("temperature", 0.2)
                )
            );
            
            // 3. 解析精度并调用计算工具
            return llmResponse.thenCompose(response -> {
                String precision = response.getOutput().trim();
                System.out.println("--- Precision ---: " + precision);
                
                // 调用计算工具
                return request.call(
                    "calc_pi",
                    Map.of("prec", precision)
                ).thenApply(toolResponse -> {
                    return "Save " + precision + " positions: " + toolResponse.getOutput();
                });
            });
        };
    }
    
    /**
     * 示例5：电商订单取消工作流
     * 对应 Python 版本的 ecommerce 示例
     */
    public static WorkflowFunction cancelOrderWorkflow() {
        return request -> {
            // 从 arguments 中获取订单ID
            Map<String, Object> args = request.getArguments();
            String orderId = (String) args.getOrDefault("order_id", "");
            
            // 1. 获取订单信息
            CompletableFuture<AgentResponse> orderResponse = request.call(
                "order_service",
                Map.of("action", "get_order", "order_id", orderId)
            );
            
            // 2. 取消订单并更新库存（并行执行）
            return orderResponse.thenCompose(orderResp -> {
                // 取消订单
                CompletableFuture<AgentResponse> cancelResponse = request.call(
                    "order_service",
                    Map.of("action", "cancel_order", "order_id", orderId)
                );
                
                // 更新库存
                CompletableFuture<AgentResponse> inventoryResponse = request.call(
                    "inventory_service",
                    Map.of("action", "restore", "order_id", orderId)
                );
                
                // 等待两个操作完成
                return cancelResponse.thenCombine(inventoryResponse, (cancelResp, inventoryResp) -> {
                    String cancelResult = cancelResp.getOutput();
                    String inventoryResult = inventoryResp.getOutput();
                    return "Order cancelled successfully. Order details: " + cancelResult + 
                           ", Inventory update: " + inventoryResult;
                });
            });
        };
    }
    
    /**
     * 示例6：使用同步方法（通过 fromSync 包装）
     */
    public static WorkflowFunction syncWorkflow() {
        return WorkflowFunction.fromSync(request -> {
            // 同步处理逻辑
            String query = request.getQuery();
            return "同步处理结果: " + query;
        });
    }
    
    /**
     * 示例7：条件分支工作流
     */
    public static WorkflowFunction conditionalWorkflow() {
        return request -> {
            String query = request.getQuery().toLowerCase();
            
            if (query.contains("订单")) {
                // 订单相关处理
                return request.call("order_agent", Map.of("query", query))
                    .thenApply(AgentResponse::getOutput);
            } else if (query.contains("库存")) {
                // 库存相关处理
                return request.call("inventory_agent", Map.of("query", query))
                    .thenApply(AgentResponse::getOutput);
            } else {
                // 默认处理
                return CompletableFuture.completedFuture("未识别的查询类型");
            }
        };
    }
    
    /**
     * 示例8：错误处理工作流
     */
    public static WorkflowFunction errorHandlingWorkflow() {
        return request -> {
            try {
                String query = request.getQuery();
                
                // 尝试调用 Agent
                return request.call("some_agent", Map.of("query", query))
                    .thenApply(AgentResponse::getOutput)
                    .exceptionally(throwable -> {
                        // 如果失败，使用备用方案
                        System.err.println("Agent 调用失败: " + throwable.getMessage());
                        return "备用处理结果";
                    });
            } catch (Exception e) {
                return CompletableFuture.completedFuture("工作流执行异常: " + e.getMessage());
            }
        };
    }
}

