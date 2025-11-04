package framework.agent;

import framework.model.AgentRequest;
import framework.model.AgentResponse;
import java.util.concurrent.CompletableFuture;

/**
 * 远程智能体基类（框架核心）
 * 
 * 提供与远程系统通信的基础功能，类似于 Python 版本的 RemoteAgent
 *  */
public abstract class RemoteAgent implements Agent {
    protected final String name;
    protected final String description;
    protected final boolean isMaster;
    protected final String serverUrl;
    protected AgentFramework framework;
    
    public RemoteAgent(String name, String description, boolean isMaster, String serverUrl) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            throw new IllegalArgumentException("serverUrl 不能为空");
        }
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            throw new IllegalArgumentException("serverUrl 必须以 http:// 或 https:// 开头");
        }
        this.name = name;
        this.description = description;
        this.isMaster = isMaster;
        this.serverUrl = serverUrl;
    }
    
    @Override
    public abstract CompletableFuture<AgentResponse> execute(AgentRequest request);
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public boolean isMaster() {
        return isMaster;
    }
    
    @Override
    public void setFramework(AgentFramework framework) {
        this.framework = framework;
    }
    
    @Override
    public AgentFramework getFramework() {
        return framework;
    }
    
    public String getServerUrl() {
        return serverUrl;
    }
}

