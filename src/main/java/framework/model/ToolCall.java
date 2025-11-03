package framework.model;

import java.util.Map;

/**
 * 工具调用（框架核心）
 */
public class ToolCall {
    private String toolName;
    private Map<String, Object> arguments;
    
    public ToolCall(String toolName, Map<String, Object> arguments) {
        this.toolName = toolName;
        this.arguments = arguments;
    }
    
    public String getToolName() {
        return toolName;
    }
    
    public Map<String, Object> getArguments() {
        return arguments;
    }
}

