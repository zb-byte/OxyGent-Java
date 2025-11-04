package business.devops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import business.devops.service.DevOpsOrchestrationService;
import framework.model.AgentResponse;

/**
 * Spring Boot 应用启动类（DevOps业务）
 * 
 * 职责：
 * - 启动 Spring Boot 应用
 * - 初始化业务逻辑
 */
@SpringBootApplication(scanBasePackages = {"business.devops"})
public class Application implements CommandLineRunner {
    
    @Autowired
    private DevOpsOrchestrationService orchestrationService;
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("🚀 Java ReAct Agent Framework - DeepSeek版本");
        System.out.println("=".repeat(60) + "\n");
        
        // 执行默认的DevOps流程示例
        String requirementId = "req-001";
        String environment = "staging";
        
        // ⭐ 选择执行模式：
        // 1. 普通流程（不使用 MCP 工具）
        // 2. MCP 流程（使用 MCP 工具）
        
        String mode = args.length > 0 ? args[0] : "normal";
        
        AgentResponse response;
        if ("mcp".equalsIgnoreCase(mode)) {
            // 执行带 MCP 工具调用的流程
            System.out.println("🔧 使用 MCP 工具模式\n");
            response = orchestrationService.executeDevOpsWorkflowWithMCP(requirementId, environment);
        } else {
            // 执行普通流程
            response = orchestrationService.executeDevOpsWorkflow(requirementId, environment);
        }
        
        orchestrationService.printResult(response);
    }
}

