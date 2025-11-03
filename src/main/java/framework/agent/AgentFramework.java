package framework.agent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import framework.model.AgentRequest;
import framework.model.AgentResponse;

/**
 * Agent Framework - 智能体框架（框架核心代码）
 * 
 * 管理智能体注册、路由和生命周期
 * 
 * ⚠️ 这是框架代码，业务开发人员不应该修改此类
 */
public class AgentFramework {
    // 智能体注册表
    private final Map<String, Agent> agentRegistry = new ConcurrentHashMap<>();
    
    // 主控智能体名称
    private String masterAgentName;
    
    /**
     * 注册智能体到框架
     */
    public void registerAgent(String name, Agent agent) {
        agentRegistry.put(name, agent);
        agent.setFramework(this);
        
        // 如果设置了isMaster，设为master agent
        if (agent.isMaster() && masterAgentName == null) {
            masterAgentName = name;
        }
        
        System.out.println("✅ 注册智能体: " + name + " (类型: " + agent.getClass().getSimpleName() + ")");
    }
    
    /**
     * 根据名称查找智能体
     */
    public Agent getAgent(String name) {
        Agent agent = agentRegistry.get(name);
        if (agent == null) {
            throw new IllegalArgumentException("智能体不存在: " + name);
        }
        return agent;
    }
    
    /**
     * 调用智能体
     */
    public CompletableFuture<AgentResponse> chatWithAgent(String agentName, AgentRequest request) {
        Agent agent = getAgent(agentName);
        return agent.execute(request);
    }
    
    /**
     * 调用主控智能体
     */
    public CompletableFuture<AgentResponse> chatWithMaster(AgentRequest request) {
        if (masterAgentName == null) {
            throw new IllegalStateException("未设置主控智能体");
        }
        return chatWithAgent(masterAgentName, request);
    }
    
    /**
     * 获取所有注册的智能体
     */
    public Set<String> getAllAgents() {
        return new HashSet<>(agentRegistry.keySet());
    }
    
    public String getMasterAgentName() {
        return masterAgentName;
    }
}

