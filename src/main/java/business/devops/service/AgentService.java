package business.devops.service;

import framework.agent.AgentFramework;
import framework.agent.PlanAndSolve;
import framework.agent.ReActAgent;
import framework.llm.LLMClient;
import framework.tool.MCPClient;
import framework.tool.MCPTool;
import framework.tool.StdioMCPClient;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 智能体服务（DevOps业务）
 * 
 * 负责创建和注册所有DevOps相关的智能体
 */
@Service
public class AgentService {
    
    private final LLMClientService llmClientService;
    private final AgentFramework framework;
    
    public AgentService(LLMClientService llmClientService) {
        this.llmClientService = llmClientService;
        this.framework = new AgentFramework();
        initializeAgents();
    }
    
    /**
     * 初始化DevOps业务需要的所有智能体和工具
     */
    private void initializeAgents() {
        
        // 1. 创建DevOps业务所需要 MCP 工具
        initializeMCPTools();
        
        // 2. 创建DevOps业务所需要的子智能体

        // 2.1 创建需求分析智能体,使用 ReActAgent 实现
        LLMClient llmClient = llmClientService.getLLMClient();
        ReActAgent requirementAgent = createRequirementAgent(llmClient);

        // 2.2 创建带权限控制的需求分析智能体（权限演示）
        // ⭐ 权限控制示例：限制智能体只能调用特定的工具或子智能体
        ReActAgent restrictedRequirementAgent = createRestrictedRequirementAgent(llmClient);

        // 2.3 创建编码智能体
        ReActAgent codeAgent = createCodeAgent(llmClient);

        // 3. 创建编码需求的主控智能体（ReAct 模式）
        ReActAgent masterAgent = createMasterAgent(llmClient);
        
        // 4. 注册编码需求的智能体
        framework.registerAgent("requirement_agent", requirementAgent);
        framework.registerAgent("restricted_requirement_agent", restrictedRequirementAgent);
        framework.registerAgent("code_agent", codeAgent);
        framework.registerAgent("devops_master", masterAgent);
        
        System.out.println("✅ 所有智能体注册完成\n");
    }
     /**
     * 以下是演示 PlanAndSolve 流程所需要的智能体
     */
    private void initializePlanAndSolveAgents() {
        
        initializeMCPTools();
        
        LLMClient llmClient = llmClientService.getLLMClient();
        
         // 创建 PlanAndSolve 流程所需的智能体（演示）
         ReActAgent plannerAgent = createPlannerAgent(llmClient);
         ReActAgent executorAgent = createExecutorAgent(llmClient);
         // 创建 PlanAndSolve 流程主控智能体（演示）
         PlanAndSolve planAndSolveMaster = createPlanAndSolveMaster(llmClient);
         

        // PlanAndSolve 流程所需要的智能体
        framework.registerAgent("planner_agent", plannerAgent);
        framework.registerAgent("executor_agent", executorAgent);
        framework.registerAgent("plan_and_solve_master", planAndSolveMaster);
        
        System.out.println("✅ 所有智能体注册完成\n");
    }
    
    
    /**
     * 初始化 MCP 工具
     * 
     * ⭐ 业务逻辑：在这里添加 MCP 工具配置
     */
    private void initializeMCPTools() {
        try {
            // 示例：文件系统工具
            Map<String, Object> fileToolsParams = new HashMap<>();
            // fileToolsParams.put("command", "npx");
            // fileToolsParams.put("args", Arrays.asList(
            //     "-y", 
            //     "@modelcontextprotocol/server-filesystem", 
            //     "./local_file"
            // ));
            
            StdioMCPClient fileToolsClient = new StdioMCPClient(
                "file_tools",
                "文件系统 MCP 工具",
                fileToolsParams
            );
            
            // 初始化 MCP 客户端
            fileToolsClient.initialize();
            
            // 注册发现的工具
            for (MCPClient.MCPToolInfo toolInfo : fileToolsClient.getTools()) {
                MCPTool mcpTool = new MCPTool(
                    toolInfo.getName(),
                    toolInfo.getDescription(),
                    fileToolsClient,
                    "file_tools"
                );
                framework.registerTool(toolInfo.getName(), mcpTool);
            }
            
            System.out.println("✅ MCP 工具初始化完成: file_tools\n");
            
        } catch (Exception e) {
            System.err.println("⚠️  MCP 工具初始化失败（可能缺少 Node.js 环境）: " + e.getMessage());
            System.err.println("💡 提示：MCP 工具需要 Node.js 环境。可以跳过 MCP 工具，使用普通智能体。\n");
        }
    }
    
    /**
     * 示例：创建需求分析智能体
     * 
     *业务逻辑：可以添加 MCP 工具（如 wiki_tools）用于读取需求文档
     */
    private ReActAgent createRequirementAgent(LLMClient llmClient) {
        // 检查是否有 MCP 工具可用
        List<String> tools = new ArrayList<>();
        if (framework.hasTool("read_file") || framework.hasTool("list_directory")) {
            // 添加文件系统工具用于读取需求文档
            if (framework.hasTool("read_file")) {
                tools.add("read_file");
            }
            if (framework.hasTool("list_directory")) {
                tools.add("list_directory");
            }
        }
        
        return new ReActAgent(
            "requirement_agent",
            "需求分析智能体",
            false,
            llmClient,
            null,
            tools.isEmpty() ? null : tools,
            "你是需求分析专家。分析需求文档，提取功能清单和技术方案。\n" +
            (tools.isEmpty() ? "" : "可以使用文件系统工具读取需求文档。"),
            5
        );
    }
    
    /**
     * 示例：创建带权限控制的需求分析智能体
     * 
     * ⭐ 权限控制演示：
     * 1. 启用权限校验：isPermissionRequired() 返回 true
     * 2. 设置白名单：只允许调用 read_file 和 list_directory 工具
     * 3. 当该智能体尝试调用不在白名单中的工具/智能体时，会被拒绝（返回 SKIPPED 状态）
     * 
     * 使用场景：
     * - 限制智能体的权限范围，提高安全性
     * - 防止智能体调用危险的工具（如删除文件、执行系统命令等）
     * - 实现细粒度的权限控制
     * 
     * 权限校验流程：
     * 1. 当 restricted_requirement_agent 调用工具时，框架会检查 isPermissionRequired()
     * 2. 如果返回 true，检查目标工具是否在 getPermittedToolNameList() 中
     * 3. 如果不在白名单中，返回 AgentState.SKIPPED，调用被拒绝
     * 4. 智能体可以处理 SKIPPED 状态，进行错误处理或重试其他方案
     */
    private ReActAgent createRestrictedRequirementAgent(LLMClient llmClient) {
        // 检查是否有 MCP 工具可用
        List<String> tools = new ArrayList<>();
        if (framework.hasTool("read_file") || framework.hasTool("list_directory")) {
            if (framework.hasTool("read_file")) {
                tools.add("read_file");
            }
            if (framework.hasTool("list_directory")) {
                tools.add("list_directory");
            }
        }
        
        // 构建系统提示
        String systemPrompt = "你是需求分析专家。分析需求文档，提取功能清单和技术方案。\n" +
            (tools.isEmpty() ? "" : "可以使用文件系统工具读取需求文档。\n" +
            "⚠️ 注意：你只能使用 read_file 和 list_directory 工具，其他工具调用会被拒绝。");
        
        // 使用匿名内部类继承 ReActAgent，重写权限相关方法
        return new ReActAgent(
            "restricted_requirement_agent",
            "带权限控制的需求分析智能体",
            false,
            llmClient,
            null,  // 不允许调用子智能体
            tools.isEmpty() ? null : tools,
            systemPrompt,
            5
        ) {
            /**
             * 启用权限校验
             * 当该方法返回 true 时，框架会检查该智能体是否有权限调用目标工具/智能体
             * 
             * 权限校验位置：AgentRequest.call() 方法中
             * 检查逻辑：
             * - 如果调用者不是用户（callerCategory != "user"）
             * - 且调用者启用了权限校验（isPermissionRequired() == true）
             * - 则检查目标是否在白名单中
             */
            @Override
            public boolean isPermissionRequired() {
                return true;  // 启用权限控制
            }
            
            /**
             * 获取允许调用的工具/智能体白名单
             * 只有在这个列表中的工具/智能体才能被调用
             * 不在列表中的调用会被拒绝，返回 SKIPPED 状态
             * 
             * 示例：
             * - ✅ 允许：read_file, list_directory（在白名单中）
             * - ❌ 拒绝：write_file, delete_file, code_agent（不在白名单中）
             * 
             * 拒绝调用时，AgentRequest.call() 会返回：
             * AgentResponse(state=SKIPPED, output="No permission for agent: xxx")
             */
            @Override
            public List<String> getPermittedToolNameList() {
                // 只允许调用文件读取相关的工具
                List<String> permitted = new ArrayList<>();
                if (framework.hasTool("read_file")) {
                    permitted.add("read_file");
                }
                if (framework.hasTool("list_directory")) {
                    permitted.add("list_directory");
                }
                // 不允许调用其他工具（如 write_file、delete_file 等）
                // 不允许调用其他智能体（如 code_agent）
                return permitted;
            }
        };
    }
    
    // /**
    //  * 远程 SSE 方式：创建代码编写智能体
    //  */
    // private SSEOxyGent createCodeAgent() {
    //     return new SSEOxyGent(
    //         "code_agent",
    //         "编码智能体",
    //         "http://www.codeagent.com"  // 远程服务器地址
    //     );
    // }
     /**
     * 远程 SSE 方式：创建代码编写智能体
     */
    private ReActAgent createCodeAgent(LLMClient llmClient) {
         return new ReActAgent(
            "code_agent",
            "创建代码编写智能体",
            false,
            llmClient,
            null,  // 不允许调用子智能体
            null,
            "你是代码编写专家。根据需求分析报告，编写代码文件和实现方案。",
            5
      );
    }
   
    
    /**
     * 创建主控智能体（简化演示版本）
     * 推理智能体的控制核心主要是模型的决策，因此业务逻辑主要写在 workflowPrompt 中
     * List<String> subAgents, List<String> tools 是可调用的子智能体和工具列表，顺序不分前后，模型会根据工具调用结果决定下一步调用哪个智能体或工具
     * ⭐ 业务逻辑添加位置：
     * - 修改流程步骤：修改 workflowPrompt 中的流程描述
     * - 添加业务规则：在 workflowPrompt 中添加规则说明
     * - 新增智能体：修改 subAgents 列表
     * - 新增工具：修改 tools 列表
     */
    private ReActAgent createMasterAgent(LLMClient llmClient) {
        String workflowPrompt = """
            你是一个DevOps流程编排专家，负责协调代码开发流程。
            
            简化开发流程（核心演示）：
            1) **需求分析阶段**：
               - 调用 requirement_agent，传入需求ID或URL
               - requirement_agent 可以使用 MCP 文件工具读取需求文档
               - 获得需求分析报告（功能清单、技术方案、开发优先级）
            
            2) **代码编写阶段**：
               - 调用 code_agent，传入需求分析报告
               - 获得代码文件和实现方案
            
            重要原则：
            - 严格按照流程顺序执行，先完成需求分析，再进行代码编写
            - 向子智能体传递清晰、完整的上下文信息
            - 如果代码编写不满足需求，可以返回需求分析阶段重新分析
            - 子智能体可以使用 MCP 工具执行具体操作
            - 最终输出完整的开发流程报告（需求分析报告 + 代码实现）
            """;
        
        // 收集可用的工具列表
        List<String> availableTools = new ArrayList<>();
        for (String toolName : framework.getAllTools()) {
            availableTools.add(toolName);
        }
        
        return new ReActAgent(
            "devops_master",
            "DevOps主控智能体（简化演示）",
            true,
            llmClient,
            Arrays.asList(
                "requirement_agent",
                "code_agent"
            ),
            availableTools.isEmpty() ? null : availableTools,
            workflowPrompt,
            10
        );
    }
    
    /**
     * 示例：创建规划者智能体（用于 PlanAndSolve 流程）
     * 
     * 规划者负责将复杂任务分解为可执行的步骤列表
     */
    private ReActAgent createPlannerAgent(LLMClient llmClient) {
        String plannerPrompt = """
            你是一个计划制定专家，负责将复杂任务分解为可执行的步骤。
            
            对于给定的目标，创建一个简单且可逐步执行的计划。
            计划应该简洁，每个步骤应该是一个独立的、完整的功能模块。
            确保每个步骤都是可执行的，并且包含所有必要的信息。
            最后一步的结果应该是最终答案。
            
            输出格式：
            1. 步骤1的描述
            2. 步骤2的描述
            3. 步骤3的描述
            
            或者 JSON 格式：
            {"steps": ["步骤1", "步骤2", "步骤3"]}
            """;
        
        return new ReActAgent(
            "planner_agent",
            "规划者智能体（负责制定执行计划）",
            false,
            llmClient,
            null,  // 规划者不需要调用子智能体
            null,  // 规划者不需要工具
            plannerPrompt,
            5
        );
    }
    
    /**
     * 示例：创建执行者智能体（用于 PlanAndSolve 流程）
     * 
     * 执行者负责执行计划中的每个步骤，通常是一个 ReActAgent
     */
    private ReActAgent createExecutorAgent(LLMClient llmClient) {
        // 收集可用的工具和子智能体
        List<String> availableTools = new ArrayList<>();
        for (String toolName : framework.getAllTools()) {
            availableTools.add(toolName);
        }
        
        String executorPrompt = """
            你是一个执行助手，负责执行计划中的单个步骤。
            
            重要提示：
            1. 你只需要完成计划中的**当前步骤**，不要做额外的事情
            2. 严格按照当前步骤的要求响应
            3. 如果需要工具，从可用工具列表中选择
            4. 如果不需要工具，直接回答——不要输出其他内容
            5. 每次只调用一个工具，不要连续调用多个工具
            
            可用工具：
            ${tools_description}
            
            可用子智能体：
            ${sub_agents_description}
            """;
        
        return new ReActAgent(
            "executor_agent",
            "执行者智能体（负责执行计划中的每个步骤）",
            false,
            llmClient,
            Arrays.asList("requirement_agent", "code_agent"),  // 可以调用子智能体
            availableTools.isEmpty() ? null : availableTools,  // 可以使用工具
            executorPrompt,
            10  // 每个步骤最多执行 10 轮 ReAct 循环
        );
    }
    
    /**
     * 示例：创建 PlanAndSolve 流程主控智能体
     * 
     * ⭐ PlanAndSolve 流程演示：
     * 1. 规划阶段：调用 planner_agent 生成执行计划
     * 2. 执行阶段：循环调用 executor_agent 执行每个步骤
     * 3. 重规划阶段（可选）：根据执行结果调整计划
     * 
     * 适用场景：
     * - 多步骤、可分解的任务
     * - 需要清晰的步骤追踪
     * - 适合预先规划的场景
     * 
     * 与 ReActAgent 的区别：
     * - PlanAndSolve：先规划后执行（"想好再干"）
     * - ReActAgent：边推理边执行（"边想边干"）
     */
    private PlanAndSolve createPlanAndSolveMaster(LLMClient llmClient) {
        return new PlanAndSolve(
            "plan_and_solve_master",
            "PlanAndSolve 流程主控智能体（演示）",
            true,  // 主控智能体
            "planner_agent",  // 规划者 Agent 名称
            "executor_agent",  // 执行者 Agent 名称
            false,  // 不启用重规划（简化演示）
            null,  // 重规划者名称（未启用）
            30,  // 最大重规划轮次
            null,  // 预设计划步骤（null 表示需要动态规划）
            llmClient  // LLM 客户端（用于备用调用）
        );
    }
    
    /**
     * 获取AgentFramework实例
     */
    public AgentFramework getFramework() {
        return framework;
    }
}

