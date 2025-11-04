package framework.tool;

import framework.model.AgentRequest;
import framework.model.AgentResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * MCP (Model Context Protocol) 客户端接口
 * 
 * 用于与 MCP 服务器通信，调用 MCP 工具
 */
public interface MCPClient {
    /**
     * 初始化 MCP 客户端
     */
    void initialize() throws Exception;
    
    /**
     * 列出可用的工具
     */
    List<MCPToolInfo> listTools() throws Exception;
    
    /**
     * 调用 MCP 工具
     */
    CompletableFuture<AgentResponse> callTool(String toolName, Map<String, Object> arguments);
    
    /**
     * 清理资源
     */
    void cleanup();
    
    /**
     * MCP 工具信息
     */
    class MCPToolInfo {
        private String name;
        private String description;
        private Map<String, Object> inputSchema;
        
        public MCPToolInfo(String name, String description, Map<String, Object> inputSchema) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public Map<String, Object> getInputSchema() {
            return inputSchema;
        }
    }
}

