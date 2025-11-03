package framework.llm;

import java.util.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Ollama LLM客户端实现（框架核心）
 * 
 * 连接本地Ollama服务
 */
public class OllamaLLMClient implements LLMClient {
    private final String baseUrl;
    private final String modelName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public OllamaLLMClient(String baseUrl, String modelName) {
        this.baseUrl = baseUrl != null ? baseUrl : "http://localhost:11434";
        this.modelName = modelName != null ? modelName : "llama2";
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String chat(List<Map<String, String>> messages) {
        try {
            System.out.println("    🌐 调用Ollama API (模型: " + modelName + ")...");
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("messages", convertMessages(messages));
            requestBody.put("stream", false);
            
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            
            // 发送HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();
            
            HttpResponse<String> response = httpClient.send(
                request, 
                HttpResponse.BodyHandlers.ofString()
            );
            
            if (response.statusCode() == 200) {
                // 解析响应
                Map<String, Object> responseBody = objectMapper.readValue(
                    response.body(), 
                    Map.class
                );
                
                Map<String, Object> message = (Map<String, Object>) responseBody.get("message");
                String content = (String) message.get("content");
                
                System.out.println("    ✅ Ollama响应接收成功");
                return content;
            } else {
                throw new RuntimeException(
                    "Ollama API调用失败: HTTP " + response.statusCode() + " - " + response.body()
                );
            }
            
        } catch (Exception e) {
            System.err.println("    ❌ Ollama API调用失败: " + e.getMessage());
            e.printStackTrace();
            return "{\"type\": \"answer\", \"content\": \"Ollama API调用失败: " + e.getMessage() + "\"}";
        }
    }
    
    private List<Map<String, Object>> convertMessages(List<Map<String, String>> messages) {
        List<Map<String, Object>> converted = new ArrayList<>();
        for (Map<String, String> msg : messages) {
            Map<String, Object> convertedMsg = new HashMap<>();
            convertedMsg.put("role", msg.get("role"));
            convertedMsg.put("content", msg.get("content"));
            converted.add(convertedMsg);
        }
        return converted;
    }
}

