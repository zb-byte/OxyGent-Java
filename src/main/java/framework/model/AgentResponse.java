package framework.model;

import java.util.List;
import java.util.Map;

/**
 * 智能体响应（框架核心）
 */
public class AgentResponse {
    private String output;
    private boolean success;
    private List<Map<String, String>> history;
    
    public AgentResponse(String output, boolean success, List<Map<String, String>> history) {
        this.output = output;
        this.success = success;
        this.history = history;
    }
    
    public String getOutput() {
        return output;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public List<Map<String, String>> getHistory() {
        return history;
    }
}

