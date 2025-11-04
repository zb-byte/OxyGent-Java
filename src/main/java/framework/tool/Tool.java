package framework.tool;

import framework.model.AgentRequest;
import framework.model.AgentResponse;
import java.util.concurrent.CompletableFuture;

/**
 * 工具接口（框架核心）
 * 
 * 所有工具都应该实现此接口
 */
public interface Tool {
    /**
     * 执行工具调用
     */
    CompletableFuture<AgentResponse> execute(AgentRequest request);
    
    /**
     * 获取工具名称
     */
    String getName();
    
    /**
     * 获取工具描述
     */
    String getDescription();
}

