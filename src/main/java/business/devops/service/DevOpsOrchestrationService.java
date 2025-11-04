package business.devops.service;

import framework.agent.AgentFramework;
import framework.model.AgentRequest;
import framework.model.AgentResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * DevOps编排服务（DevOps业务）
 * 
 * 负责业务流程的编排和执行
 * 
 * ⭐ 业务逻辑添加位置：
 * - 添加新的业务流程方法：在这里添加新方法
 * - 业务规则验证：在方法中添加验证逻辑
 * - 结果处理：添加结果解析、格式化等逻辑
 */
@Service
public class DevOpsOrchestrationService {
    
    private final AgentFramework framework;
    
    public DevOpsOrchestrationService(AgentService agentService) {
        this.framework = agentService.getFramework();
    }
    
    /**
     * 执行完整的DevOps流程
     * 
     * ⭐ 业务逻辑：可以在这里添加 MCP 工具调用示例
     * 
     * @param requirementId 需求ID或URL
     * @param environment 部署环境（如：staging, production）
     * @return AgentResponse 执行结果
     */
    public AgentResponse executeDevOpsWorkflow(String requirementId, String environment) {
        // ⭐ 业务逻辑：可以在这里直接调用 MCP 工具（如果需要）
        // 示例：如果需要在流程开始前调用 MCP 工具
        if (framework.hasTool("read_file")) {
            System.out.println("📂 使用 MCP 工具读取需求文档...");
            // 可以在这里直接调用 MCP 工具
            // framework.getTool("read_file").execute(...);
        }
        
        String taskDescription = buildTaskDescription(requirementId, environment);
        
        AgentRequest request = new AgentRequest(
            taskDescription,
            null,
            "user",
            "devops_master"
        );
        
        System.out.println("\n📋 开始执行任务...\n");
        CompletableFuture<AgentResponse> future = framework.chatWithMaster(request);
        AgentResponse response = future.join();
        
        return response;
    }
    
    /**
     * 执行带 MCP 工具调用的 DevOps 流程示例
     * 
     * ⭐ 业务逻辑：演示如何在业务流程中使用 MCP 工具
     * 
     * @param requirementId 需求ID
     * @param environment 部署环境
     * @return AgentResponse 执行结果
     */
    public AgentResponse executeDevOpsWorkflowWithMCP(String requirementId, String environment) {
        System.out.println("\n📋 执行 DevOps 流程（使用 MCP 工具）...\n");
        
        // ⭐ 业务逻辑示例：在流程中直接调用 MCP 工具
        try {
            // 示例1: 使用 MCP 文件工具读取需求文档
            if (framework.hasTool("read_file")) {
                System.out.println("📂 步骤1: 使用 MCP 文件工具读取需求文档");
                
                Map<String, Object> fileArgs = new HashMap<>();
                fileArgs.put("path", "./requirements/" + requirementId + ".md");
                
                AgentRequest fileRequest = new AgentRequest(
                    "读取需求文档",
                    null,
                    "user",
                    "read_file"
                );
                fileRequest.getArguments().putAll(fileArgs);
                
                AgentResponse fileResponse = framework.getTool("read_file")
                    .execute(fileRequest)
                    .join();
                
                String output = fileResponse.getOutput();
                String preview = output != null && output.length() > 0 
                    ? output.substring(0, Math.min(100, output.length())) + "..."
                    : "（无内容）";
                System.out.println("✅ 需求文档读取完成: " + preview + "\n");
            }
            
            // 示例2: 继续执行完整的 DevOps 流程
            String taskDescription = buildTaskDescription(requirementId, environment);
            taskDescription += "\n\n注意：需求文档已通过 MCP 工具读取，可以直接使用。";
            
            AgentRequest request = new AgentRequest(
                taskDescription,
                null,
                "user",
                "devops_master"
            );
            
            CompletableFuture<AgentResponse> future = framework.chatWithMaster(request);
            AgentResponse response = future.join();
            
            // 示例3: 流程完成后，使用 MCP 工具保存结果
            if (framework.hasTool("write_file")) {
                System.out.println("\n💾 使用 MCP 文件工具保存流程报告...");
                
                Map<String, Object> saveArgs = new HashMap<>();
                saveArgs.put("path", "./output/devops_report_" + requirementId + ".txt");
                saveArgs.put("content", response.getOutput());
                
                AgentRequest saveRequest = new AgentRequest(
                    "保存流程报告",
                    null,
                    "user",
                    "write_file"
                );
                saveRequest.getArguments().putAll(saveArgs);
                
                framework.getTool("write_file")
                    .execute(saveRequest)
                    .join();
                
                System.out.println("✅ 流程报告已保存\n");
            }
            
            return response;
            
        } catch (Exception e) {
            System.err.println("❌ MCP 工具调用失败: " + e.getMessage());
            e.printStackTrace();
            
            // 如果 MCP 工具失败，回退到普通流程
            System.out.println("⚠️  回退到普通流程（不使用 MCP 工具）\n");
            return executeDevOpsWorkflow(requirementId, environment);
        }
    }
    
    /**
     * 执行自定义任务
     * 
     * @param taskDescription 任务描述
     * @return AgentResponse 执行结果
     */
    public AgentResponse executeCustomTask(String taskDescription) {
        AgentRequest request = new AgentRequest(
            taskDescription,
            null,
            "user",
            "devops_master"
        );
        
        System.out.println("\n📋 开始执行任务...\n");
        CompletableFuture<AgentResponse> future = framework.chatWithMaster(request);
        AgentResponse response = future.join();
        
        return response;
    }
    
    /**
     * 构建任务描述
     */
    private String buildTaskDescription(String requirementId, String environment) {
        return String.format("""
            请完成完整的开发流程：
            1. 从Wiki读取需求 %s
            2. 根据需求编写代码
            3. 进行代码审查
            4. 编写并执行测试
            5. 提交代码到Git
            6. 部署到%s环境
            
            请输出完整的流程报告。
            """, requirementId, environment != null ? environment : "staging");
    }
    
    /**
     * 打印执行结果
     */
    public void printResult(AgentResponse response) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("✅ 任务执行完成");
        System.out.println("=".repeat(60));
        System.out.println("\n📄 最终结果：");
        System.out.println(response.getOutput());
    }
}

