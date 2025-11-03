package framework.agent;

import framework.model.AgentRequest;
import framework.model.AgentResponse;
import java.util.concurrent.CompletableFuture;

/**
 * 智能体接口（框架核心）
 * 
 * 业务开发人员应该实现此接口或使用 ReActAgent 类来创建智能体
 */
public interface Agent {
    /**
     * 执行智能体任务
     */
    CompletableFuture<AgentResponse> execute(AgentRequest request);
    
    /**
     * 获取智能体名称
     */
    String getName();
    
    /**
     * 获取智能体描述
     */
    String getDescription();
    
    /**
     * 是否为主控智能体
     */
    boolean isMaster();
    
    /**
     * 设置框架引用（用于调用其他智能体）
     */
    void setFramework(AgentFramework framework);
    
    /**
     * 获取框架引用
     */
    AgentFramework getFramework();
}

