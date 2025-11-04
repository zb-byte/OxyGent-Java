package framework.model;

/**
 * 响应数据模型
 * 对应 Python 版本的 Response 类
 * 
 * 用于重规划阶段，表示可以直接返回给用户的答案
 */
public class Response {
    /**
     * 响应内容（直接回答用户的问题）
     */
    private String response;
    
    public Response() {
    }
    
    public Response(String response) {
        this.response = response;
    }
    
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
}

