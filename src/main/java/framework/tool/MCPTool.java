package framework.tool;

import framework.model.AgentRequest;
import framework.model.AgentResponse;
import java.util.concurrent.CompletableFuture;

/**
 * MCP 工具代理（框架核心）
 * 
 * 代表从 MCP 服务器发现的一个工具
 * 类似于 Python 版本的 MCPTool
 */
public class MCPTool implements Tool {
    private final String name;
    private final String description;
    private final MCPClient mcpClient;
    private final String serverName;
    
    public MCPTool(String name, String description, MCPClient mcpClient, String serverName) {
        this.name = name;
        this.description = description;
        this.mcpClient = mcpClient;
        this.serverName = serverName;
    }
    
    @Override
    public CompletableFuture<AgentResponse> execute(AgentRequest request) {
        // 委托给 MCP 客户端执行（传递 request 对象以使用新版本构造函数）
        return mcpClient.callTool(name, request.getArguments(), request);
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    public MCPClient getMCPClient() {
        return mcpClient;
    }
    
    public String getServerName() {
        return serverName;
    }
}

