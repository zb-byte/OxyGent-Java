package framework.llm;

import java.util.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * DeepSeek LLM客户端实现（框架核心）
 * 
 * 连接DeepSeek大模型API
 * 
 * ⚠️ 这是框架代码，业务开发人员应该使用此类，但不要修改
 */
public class DeepSeekLLMClient implements LLMClient {
    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    /**
     * 构造函数（支持自定义baseUrl）
     * @param apiKey DeepSeek API Key（如果为null，则从环境变量获取）
     * @param modelName 模型名称（如果为null，则从环境变量DEFAULT_LLM_MODEL_NAME获取，默认: deepseek-chat）
     * @param baseUrl Base URL（如果为null，则从环境变量DEFAULT_LLM_BASE_URL获取，默认: https://api.deepseek.com/v1）
     */
    public DeepSeekLLMClient(String apiKey, String modelName, String baseUrl) {
        // 优先使用参数，其次从环境变量获取
        this.apiKey = apiKey != null ? apiKey : 
                      System.getenv("DEFAULT_LLM_API_KEY") != null ? System.getenv("DEFAULT_LLM_API_KEY") :
                      System.getenv("DEEPSEEK_API_KEY");
        
        this.modelName = modelName != null ? modelName : 
                        (System.getenv("DEFAULT_LLM_MODEL_NAME") != null ? System.getenv("DEFAULT_LLM_MODEL_NAME") : "deepseek-chat");
        
        // 处理baseUrl：如果提供的是完整endpoint URL，则提取baseUrl
        String envBaseUrl = System.getenv("DEFAULT_LLM_BASE_URL");
        String providedBaseUrl = baseUrl != null ? baseUrl : envBaseUrl;
        
        if (providedBaseUrl != null && providedBaseUrl.contains("/chat/completions")) {
            // 如果是完整URL，提取base部分
            int idx = providedBaseUrl.indexOf("/chat/completions");
            this.baseUrl = providedBaseUrl.substring(0, idx);
        } else if (providedBaseUrl != null) {
            this.baseUrl = providedBaseUrl;
        } else {
            this.baseUrl = "https://api.deepseek.com/v1";
        }
        
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            throw new IllegalArgumentException(
                "DeepSeek API Key未设置。请设置环境变量DEFAULT_LLM_API_KEY或DEEPSEEK_API_KEY，或传入apiKey参数"
            );
        }
    }
    
    /**
     * 构造函数（使用默认baseUrl）
     */
    public DeepSeekLLMClient(String apiKey, String modelName) {
        this(apiKey, modelName, null);
    }
    
    /**
     * 默认构造函数（从环境变量获取所有配置）
     */
    public DeepSeekLLMClient() {
        this(null, null, null);
    }
    
    @Override
    public String chat(List<Map<String, String>> messages) {
        try {
            System.out.println("    🌐 调用DeepSeek API (模型: " + modelName + ")...");
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("messages", convertMessages(messages));
            requestBody.put("temperature", 0.1);
            requestBody.put("stream", false);
            
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            
            // 发送HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
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
                
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices == null || choices.isEmpty()) {
                    throw new RuntimeException("DeepSeek API返回空响应");
                }
                
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                String content = (String) message.get("content");
                
                System.out.println("    ✅ DeepSeek响应接收成功");
                return content;
            } else {
                String errorBody = response.body();
                throw new RuntimeException(
                    "DeepSeek API调用失败: HTTP " + response.statusCode() + " - " + errorBody
                );
            }
            
        } catch (Exception e) {
            System.err.println("    ❌ DeepSeek API调用失败: " + e.getMessage());
            e.printStackTrace();
            // 失败时返回默认响应
            return "{\"type\": \"answer\", \"content\": \"DeepSeek API调用失败: " + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 转换消息格式
     */
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
    
    /**
     * 获取模型名称
     */
    public String getModelName() {
        return modelName;
    }
    
    /**
     * 获取API端点
     */
    public String getBaseUrl() {
        return baseUrl;
    }
}

