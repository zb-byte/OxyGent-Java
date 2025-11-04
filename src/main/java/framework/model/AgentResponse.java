package framework.model;

import java.util.Map;

/**
 * 智能体响应（框架核心）
 * 对应 Python 版本的 OxyResponse
 */
public class AgentResponse {
    private AgentState state;  // 执行状态
    private String output;  // 输出内容
    private Map<String, Object> extra;  // 额外元数据（token数、耗时等）
    /**
     * 关联的请求对象
     * 1. 日志记录和追踪
        保存响应时，可同时记录完整请求上下文
        便于问题排查和日志分析
       2. 上下文追溯
        通过响应可追溯到产生它的请求
        在链式调用中，可追溯完整的调用链
     */
    private AgentRequest request;  
    
    public AgentResponse(AgentState state, String output, Map<String, Object> extra, AgentRequest request) {
        this.state = state;
        this.output = output;
        this.extra = extra != null ? extra : new java.util.HashMap<>();
        this.request = request;
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

}

