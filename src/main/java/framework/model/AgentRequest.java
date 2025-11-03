package framework.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 智能体请求（框架核心）
 */
public class AgentRequest {
    private String query;
    private String traceId;
    private String caller;
    private String targetAgent;
    private Map<String, Object> arguments;
    
    public AgentRequest(String query, String traceId, String caller, String targetAgent) {
        this.query = query;
        this.traceId = traceId;
        this.caller = caller;
        this.targetAgent = targetAgent;
        this.arguments = new HashMap<>();
    }
    
    public String getQuery() {
        return query;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public String getCaller() {
        return caller;
    }
    
    public String getTargetAgent() {
        return targetAgent;
    }
    
    public Map<String, Object> getArguments() {
        return arguments;
    }
}

