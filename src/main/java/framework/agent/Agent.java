package framework.agent;

import framework.model.AgentRequest;
import framework.model.AgentResponse;
import java.util.List;
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
    
    /**
     * 是否需要权限校验
     * 对应 Python 版本的 is_permission_required
     */
    default boolean isPermissionRequired() {
        return false;
    }
    
    /**
     * 获取允许调用的工具/智能体名称列表
     * 对应 Python 版本的 permitted_tool_name_list
     */
    default List<String> getPermittedToolNameList() {
        return new java.util.ArrayList<>();
    }
    
    /**
     * 获取超时时间（秒），0 表示不设置超时
     * 对应 Python 版本的 timeout
     */
    default long getTimeout() {
        return 0;  // 默认不设置超时
    }
    
    /**
     * 获取重试次数
     * 对应 Python 版本的 retries
     */
    default int getRetries() {
        return 0;  // 默认不重试
    }
    
    /**
     * 获取重试延迟（秒）
     * 对应 Python 版本的 delay
     */
    default long getDelay() {
        return 1;  // 默认延迟1秒
    }
}

