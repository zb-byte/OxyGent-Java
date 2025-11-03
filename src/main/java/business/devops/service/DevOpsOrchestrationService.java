package business.devops.service;

import framework.agent.AgentFramework;
import framework.model.AgentRequest;
import framework.model.AgentResponse;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * DevOps编排服务（DevOps业务）
 * 
 * 负责业务流程的编排和执行
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
     * @param requirementId 需求ID或URL
     * @param environment 部署环境（如：staging, production）
     * @return AgentResponse 执行结果
     */
    public AgentResponse executeDevOpsWorkflow(String requirementId, String environment) {
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

