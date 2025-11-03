package business.devops.service;

import framework.agent.AgentFramework;
import framework.agent.ReActAgent;
import framework.llm.LLMClient;
import org.springframework.stereotype.Service;

import java.util.Arrays;

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
        
        // 创建所有子智能体
        ReActAgent requirementAgent = createRequirementAgent(llmClient);
        ReActAgent codeAgent = createCodeAgent(llmClient);
        ReActAgent reviewAgent = createReviewAgent(llmClient);
        ReActAgent testAgent = createTestAgent(llmClient);
        ReActAgent gitAgent = createGitAgent(llmClient);
        ReActAgent deployAgent = createDeployAgent(llmClient);
        
        // 创建主控智能体
        ReActAgent masterAgent = createMasterAgent(llmClient);
        
        // 注册所有智能体
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
     * 创建需求分析智能体
     */
    private ReActAgent createRequirementAgent(LLMClient llmClient) {
        return new ReActAgent(
            "requirement_agent",
            "需求分析智能体",
            false,
            llmClient,
            null,
            null,
            "你是需求分析专家。分析需求文档，提取功能清单和技术方案。",
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
     */
    private ReActAgent createMasterAgent(LLMClient llmClient) {
        String workflowPrompt = """
            你是一个DevOps流程编排专家，负责协调整个代码开发流程。
            
            完整开发流程：
            1) **需求分析阶段**：
               - 调用 requirement_agent，传入Wiki需求ID或URL
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
               - 获得提交结果和commit hash
            
            6) **自动部署阶段**：
               - 调用 deploy_agent，传入commit hash或版本号
               - 获得部署结果和访问URL
            
            重要原则：
            - 严格按照流程顺序执行，每个阶段完成后再进入下一阶段
            - 向子智能体传递清晰、完整的上下文信息
            - 如果某阶段失败，返回上一阶段修复
            - 最终输出完整的开发流程报告
            """;
        
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
            null,
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

