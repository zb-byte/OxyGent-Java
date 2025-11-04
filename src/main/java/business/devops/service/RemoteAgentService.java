package business.devops.service;

import framework.agent.AgentFramework;
import framework.agent.ReActAgent;
import framework.agent.SSEOxyGent;
import framework.llm.LLMClient;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 远程智能体服务示例（DevOps业务）
 * 
 * 演示如何使用 SSEOxyGent 连接远程智能体
 * 类似于 Python 版本的分布式调用示例
 */
@Service
public class RemoteAgentService {
    
    private final LLMClientService llmClientService;
    private final AgentFramework framework;
    
    public RemoteAgentService(LLMClientService llmClientService) {
        this.llmClientService = llmClientService;
        this.framework = new AgentFramework();
        initializeAgents();
    }
    
    /**
     * 初始化所有智能体（包含本地和远程）
     */
    private void initializeAgents() {
        LLMClient llmClient = llmClientService.getLLMClient();
        
        // 1、 创建本地智能体
        ReActAgent fileAgent = createFileAgent(llmClient);
        
        // 2、创建远程智能体（通过 SSE 协议调用远程服务）
        SSEOxyGent mathAgent = new SSEOxyGent(
            "math_agent",
            "远程数学计算智能体",
            "http://127.0.0.1:8081"  // 远程服务器地址
        );
        
        // 3、 创建主控智能体（可以调用本地和远程智能体）
        ReActAgent masterAgent = createMasterAgent(llmClient);
        
        // 注册所有智能体
        framework.registerAgent("file_agent", fileAgent);
        framework.registerAgent("math_agent", mathAgent);  // ⭐ 注册远程智能体
        framework.registerAgent("master_agent", masterAgent);
        
        System.out.println("所有智能体注册完成（包含远程智能体）\n");
    }
    
    /**
     * 创建本地文件智能体
     */
    private ReActAgent createFileAgent(LLMClient llmClient) {
        return new ReActAgent(
            "file_agent",
            "本地文件查询智能体",
            false,
            llmClient,
            null,
            null,
            "你是文件操作专家。查询和操作本地文件。",
            5
        );
    }
    
    /**
     * 创建主控智能体（可以调用本地和远程智能体）
     * 
     * ⚠️ 注意：流程模式取决于主控智能体的类型
     * - ReActAgent → ReAct 模式（当前实现）
     * - PlanAndSolveAgent → PlanAndSolve 模式（需要实现）
     * - ReflexionAgent → Reflexion 模式（需要实现）
     */
    private ReActAgent createMasterAgent(LLMClient llmClient) {
        String workflowPrompt = """
            你是一个主控智能体，负责协调多个子智能体完成任务。
            
            可用智能体：
            - file_agent: 本地文件查询和操作
            - math_agent: 远程数学计算（运行在 http://127.0.0.1:8081）
            
            根据用户任务选择合适的智能体：
            - 文件相关任务 → file_agent
            - 数学计算任务 → math_agent
            
            重要原则：
            - 向子智能体传递清晰的任务描述
            - 整合多个智能体的结果
            - 最终输出完整的答案
            """;
        
        return new ReActAgent(
            "master_agent",
            "主控智能体",
            true,
            llmClient,
            Arrays.asList("file_agent", "math_agent"),  // ⭐ 包含远程智能体
            null,
            workflowPrompt,
            10
        );
    }
    
    /**
     * 获取AgentFramework实例
     */
    public AgentFramework getFramework() {
        return framework;
    }
}

