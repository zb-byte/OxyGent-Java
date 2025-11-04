package framework.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import framework.model.AgentRequest;
import framework.model.AgentResponse;
import framework.model.AgentState;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * SSE 远程智能体代理（框架核心）
 * 
 * 通过 SSE (Server-Sent Events) 协议与远程 MAS 通信的智能体
 * 类似于 Python 版本的 SSEOxyGent
 * 
 *  */
public class SSEOxyGent extends RemoteAgent {
    private final boolean isShareCallStack;
    private final ObjectMapper objectMapper;
    
    public SSEOxyGent(String name, String description, boolean isMaster, 
                     String serverUrl, boolean isShareCallStack) {
        super(name, description, isMaster, serverUrl);
        this.isShareCallStack = isShareCallStack;
        this.objectMapper = new ObjectMapper();
    }
    
    public SSEOxyGent(String name, String description, String serverUrl) {
        this(name, description, false, serverUrl, false);
    }
    
    @Override
    public CompletableFuture<AgentResponse> execute(AgentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("🔄 [" + name + "] 开始远程调用: " + serverUrl);
            
            try {
                // 1. 构建请求负载
                Map<String, Object> payload = buildPayload(request);
                
                // 2. 构建 SSE 端点 URL
                String sseUrl = buildUrl(serverUrl, "/sse/chat");
                
                // 3. 建立 SSE 连接并发送请求
                String answer = connectAndReceive(sseUrl, payload);
                
                // 4. 返回响应
                return new AgentResponse(
                    AgentState.COMPLETED,
                    answer,
                    null,
                    request
                );
                
            } catch (Exception e) {
                System.err.println("❌ 远程调用失败: " + e.getMessage());
                e.printStackTrace();
                return new AgentResponse(
                    AgentState.FAILED,
                    "远程调用失败: " + e.getMessage(),
                    null,
                    request
                );
            }
        });
    }
    
    /**
     * 构建请求负载
     */
    private Map<String, Object> buildPayload(AgentRequest request) {
        Map<String, Object> payload = new HashMap<>();
        
        // 基本字段
        payload.put("query", request.getQuery());
        payload.put("trace_id", request.getTraceId());
        payload.put("caller", request.getCaller());
        payload.put("callee", request.getTargetAgent());
        
        // 参数
        payload.putAll(request.getArguments());
        
        // 调用栈处理
        if (isShareCallStack) {
            // 共享调用栈（如果需要）
            // 这里可以添加 call_stack 字段
        } else {
            // 不共享调用栈，清空 caller
            payload.put("caller", "user");
        }
        
        // 设置类别
        payload.put("caller_category", "user");
        payload.put("callee_category", "agent");
        
        return payload;
    }
    
    /**
     * 建立 SSE 连接并接收消息
     */
    private String connectAndReceive(String url, Map<String, Object> payload) throws Exception {
        // 构建请求体
        String jsonPayload = objectMapper.writeValueAsString(payload);
        
        // 使用流式读取 SSE
        return readSSEStream(url, jsonPayload);
    }
    
    /**
     * 读取 SSE 流（使用传统 HttpURLConnection 支持流式读取）
     */
    private String readSSEStream(String url, String jsonPayload) throws Exception {
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "text/event-stream");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            
            // 发送请求体
            try (java.io.OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // 读取响应流
            String answer = "";
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        
                        if ("done".equals(data)) {
                            break;
                        }
                        
                        // 解析 JSON 消息
                        try {
                            Map<String, Object> message = objectMapper.readValue(data, Map.class);
                            String type = (String) message.get("type");
                            
                            if ("answer".equals(type)) {
                                Object content = message.get("content");
                                if (content != null) {
                                    answer = content.toString();
                                }
                            } else if ("tool_call".equals(type) || "observation".equals(type)) {
                                // 转发消息到本地框架（如果需要）
                                System.out.println("  📨 收到远程消息: " + type);
                            }
                            
                        } catch (Exception e) {
                            // JSON 解析失败，可能是普通文本
                            if (answer.isEmpty()) {
                                answer = data;
                            }
                        }
                    }
                }
            }
            
            return answer.isEmpty() ? "远程调用完成" : answer;
            
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * 解析 SSE 响应（如果是一次性返回的）
     */
    private String parseSSEResponse(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return "远程调用完成";
        }
        
        // 按行分割
        String[] lines = responseBody.split("\n");
        String answer = "";
        
        for (String line : lines) {
            if (line.startsWith("data: ")) {
                String data = line.substring(6).trim();
                
                if ("done".equals(data)) {
                    break;
                }
                
                try {
                    Map<String, Object> message = objectMapper.readValue(data, Map.class);
                    String type = (String) message.get("type");
                    
                    if ("answer".equals(type)) {
                        Object content = message.get("content");
                        if (content != null) {
                            answer = content.toString();
                        }
                    }
                } catch (Exception e) {
                    // 解析失败，可能是普通文本
                    if (answer.isEmpty()) {
                        answer = data;
                    }
                }
            }
        }
        
        return answer.isEmpty() ? "远程调用完成" : answer;
    }
    
    /**
     * 构建完整 URL
     */
    private String buildUrl(String baseUrl, String path) {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return baseUrl + path;
    }
}

