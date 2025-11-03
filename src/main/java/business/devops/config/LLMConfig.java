package business.devops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LLM配置类（DevOps业务）
 * 
 * 从环境变量或application.properties读取配置
 */
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LLMConfig {
    
    private String apiKey;
    private String baseUrl;
    private String modelName;
    private String provider = "deepseek"; // deepseek, ollama, openai
    
    public LLMConfig() {
        // 优先从环境变量读取
        this.apiKey = System.getenv("DEFAULT_LLM_API_KEY");
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            this.apiKey = System.getenv("DEEPSEEK_API_KEY");
        }
        
        this.baseUrl = System.getenv("DEFAULT_LLM_BASE_URL");
        this.modelName = System.getenv("DEFAULT_LLM_MODEL_NAME");
        
        String providerEnv = System.getenv("DEFAULT_LLM_PROVIDER");
        if (providerEnv != null && !providerEnv.isEmpty()) {
            this.provider = providerEnv.toLowerCase();
        }
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }
}

