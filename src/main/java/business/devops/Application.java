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
        // 1. normal - 普通流程（ReAct 模式，不使用 MCP 工具）
        // 2. mcp - MCP 流程（ReAct 模式，使用 MCP 工具）
        // 3. plan - PlanAndSolve 流程（规划-执行模式）
        
        String mode = args.length > 0 ? args[0] : "plan";
        
        AgentResponse response;
        if ("mcp".equalsIgnoreCase(mode)) {
            // 执行带 MCP 工具调用的流程（ReAct 模式）
            System.out.println("🔧 使用 MCP 工具模式（ReAct 模式）\n");
            response = orchestrationService.executeDevOpsWorkflowWithMCP(requirementId, environment);
        } else if ("plan".equalsIgnoreCase(mode)) {
            // 执行 PlanAndSolve 流程（规划-执行模式）
            System.out.println("📋 使用 PlanAndSolve 流程模式\n");
            System.out.println("💡 PlanAndSolve 特点：");
            System.out.println("   - 先规划：调用 planner_agent 生成执行计划");
            System.out.println("   - 再执行：循环调用 executor_agent 执行每个步骤");
            System.out.println("   - 步骤清晰可追踪\n");
            
            String taskDescription = String.format(
                "请完成开发流程：\n" +
                "1. 分析需求 %s\n" +
                "2. 根据需求编写代码\n\n" +
                "请输出完整的流程报告（需求分析报告 + 代码实现）。",
                requirementId
            );
            
            response = orchestrationService.executeTaskWithPlanAndSolve(taskDescription);
        } else {
            // 执行普通流程（ReAct 模式）
            System.out.println("🔄 使用 ReAct 模式（边推理边执行）\n");
            response = orchestrationService.executeDevOpsWorkflow(requirementId, environment);
        }
        
        orchestrationService.printResult(response);
    }
}

