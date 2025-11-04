package business.devops.service;

import framework.agent.AgentFramework;
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
     * 初始化所有智能体
     */
    private void initializeAgents() {
        LLMClient llmClient = llmClientService.getLLMClient();
        
        // 1. 创建DevOps业务所需要 MCP 工具
        initializeMCPTools();
        
        // 2. 创建DevOps业务所需要的子智能体
        ReActAgent requirementAgent = createRequirementAgent(llmClient);
        ReActAgent codeAgent = createCodeAgent(llmClient);
        ReActAgent reviewAgent = createReviewAgent(llmClient);
        ReActAgent testAgent = createTestAgent(llmClient);
        ReActAgent gitAgent = createGitAgent(llmClient);
        ReActAgent deployAgent = createDeployAgent(llmClient);
        
        // 3. 创建主控智能体
        ReActAgent masterAgent = createMasterAgent(llmClient);
        
        // 4. 注册所有智能体
        framework.registerAgent("requirement_agent", requirementAgent);
        framework.registerAgent("code_agent", codeAgent);
        framework.registerAgent("review_agent", reviewAgent);
        framework.registerAgent("test_agent", testAgent);
        framework.registerAgent("git_agent", gitAgent);
        framework.registerAgent("deploy_agent", deployAgent);
        framework.registerAgent("devops_master", masterAgent);
        
        System.out.println("✅ 所有智能体注册完成\n");
    }
    
    /**
     * 初始化 MCP 工具
     * 
     * ⭐ 业务逻辑：在这里添加 MCP 工具配置
     */
    private void initializeMCPTools() {
        try {
            // 示例：文件系统工具（需要 Node.js 环境）
            // 注意：实际使用时需要确保 Node.js 和 MCP 服务器已安装
            Map<String, Object> fileToolsParams = new HashMap<>();
            fileToolsParams.put("command", "npx");
            fileToolsParams.put("args", Arrays.asList(
                "-y", 
                "@modelcontextprotocol/server-filesystem", 
                "./local_file"
            ));
            
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
     * 创建需求分析智能体
     * 
     * ⭐ 业务逻辑：可以添加 MCP 工具（如 wiki_tools）用于读取需求文档
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
     * 创建代码编写智能体
     */
    private ReActAgent createCodeAgent(LLMClient llmClient) {
        return new ReActAgent(
            "code_agent",
            "代码编写智能体",
            false,
            llmClient,
            null,
            null,
            "你是代码编写专家。根据需求分析报告编写高质量的代码。",
            5
        );
    }
    
    /**
     * 创建代码审查智能体
     */
    private ReActAgent createReviewAgent(LLMClient llmClient) {
        return new ReActAgent(
            "review_agent",
            "代码审查智能体",
            false,
            llmClient,
            null,
            null,
            "你是代码审查专家。检查代码质量和规范性。",
            5
        );
    }
    
    /**
     * 创建测试智能体
     */
    private ReActAgent createTestAgent(LLMClient llmClient) {
        return new ReActAgent(
            "test_agent",
            "测试智能体",
            false,
            llmClient,
            null,
            null,
            "你是测试专家。编写和执行测试用例。",
            5
        );
    }
    
    /**
     * 创建Git提交智能体
     */
    private ReActAgent createGitAgent(LLMClient llmClient) {
        return new ReActAgent(
            "git_agent",
            "Git提交智能体",
            false,
            llmClient,
            null,
            null,
            "你是Git专家。提交代码到Git仓库。",
            5
        );
    }
    
    /**
     * 创建部署智能体
     */
    private ReActAgent createDeployAgent(LLMClient llmClient) {
        return new ReActAgent(
            "deploy_agent",
            "部署智能体",
            false,
            llmClient,
            null,
            null,
            "你是部署专家。部署应用到指定环境。",
            5
        );
    }
    
    /**
     * 创建主控智能体
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
            你是一个DevOps流程编排专家，负责协调整个代码开发流程。
            
            完整开发流程：
            1) **需求分析阶段**：
               - 调用 requirement_agent，传入Wiki需求ID或URL
               - requirement_agent 可以使用 MCP 文件工具读取需求文档
               - 获得需求分析报告（功能清单、技术方案、开发优先级）
            
            2) **代码编写阶段**：
               - 调用 code_agent，传入需求分析报告
               - 获得代码文件和实现方案
            
            3) **代码校验阶段**：
               - 调用 review_agent，传入编写的代码
               - 获得审查报告（评分、问题清单、改进建议）
               - 如果审查不通过，返回 code_agent 进行修改
            
            4) **自动测试阶段**：
               - 调用 test_agent，传入代码和需求
               - 获得测试报告（通过率、覆盖率、失败用例）
               - 如果测试失败，返回 code_agent 进行修复
            
            5) **Git提交阶段**：
               - 调用 git_agent，传入代码文件和提交信息
               - git_agent 可以使用 MCP Git 工具进行代码提交
               - 获得提交结果和commit hash
            
            6) **自动部署阶段**：
               - 调用 deploy_agent，传入commit hash或版本号
               - deploy_agent 可以使用 MCP 部署工具进行部署
               - 获得部署结果和访问URL
            
            重要原则：
            - 严格按照流程顺序执行，每个阶段完成后再进入下一阶段
            - 向子智能体传递清晰、完整的上下文信息
            - 如果某阶段失败，返回上一阶段修复
            - 子智能体可以使用 MCP 工具执行具体操作
            - 最终输出完整的开发流程报告
            """;
        
        // 收集可用的工具列表
        List<String> availableTools = new ArrayList<>();
        for (String toolName : framework.getAllTools()) {
            availableTools.add(toolName);
        }
        
        return new ReActAgent(
            "devops_master",
            "DevOps主控智能体",
            true,
            llmClient,
            Arrays.asList(
                "requirement_agent",
                "code_agent",
                "review_agent",
                "test_agent",
                "git_agent",
                "deploy_agent"
            ),
            availableTools.isEmpty() ? null : availableTools,
            workflowPrompt,
            16
        );
    }
    
    /**
     * 获取AgentFramework实例
     */
    public AgentFramework getFramework() {
        return framework;
    }
}

