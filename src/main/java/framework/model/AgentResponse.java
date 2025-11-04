package framework.model;

import java.util.List;
import java.util.Map;

/**
 * 智能体响应（框架核心）
 * 对应 Python 版本的 OxyResponse
 */
public class AgentResponse {
    private AgentState state;  // 执行状态
    private String output;  // 输出内容
    private Map<String, Object> extra;  // 额外元数据（token数、耗时等）
    private AgentRequest request;  // 关联的请求对象
    
    // 兼容旧版本构造函数
    private List<Map<String, String>> history;
    
    /**
     * 新版本构造函数（推荐使用）
     */
    public AgentResponse(AgentState state, String output, Map<String, Object> extra, AgentRequest request) {
        this.state = state;
        this.output = output;
        this.extra = extra != null ? extra : new java.util.HashMap<>();
        this.request = request;
    }
    
    /**
     * 兼容旧版本的构造函数
     */
    public AgentResponse(String output, boolean success, List<Map<String, String>> history) {
        this.state = success ? AgentState.COMPLETED : AgentState.FAILED;
        this.output = output;
        this.history = history;
        this.extra = new java.util.HashMap<>();
    }
    
    public AgentState getState() {
        return state;
    }
    
    public void setState(AgentState state) {
        this.state = state;
    }
    
    public String getOutput() {
        return output;
    }
    
    public void setOutput(String output) {
        this.output = output;
    }
    
    public Map<String, Object> getExtra() {
        return extra;
    }
    
    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }
    
    public AgentRequest getRequest() {
        return request;
    }
    
    public void setRequest(AgentRequest request) {
        this.request = request;
    }
    
    // 兼容旧版本方法
    public boolean isSuccess() {
        return state == AgentState.COMPLETED;
    }
    
    public List<Map<String, String>> getHistory() {
        return history;
    }
}

