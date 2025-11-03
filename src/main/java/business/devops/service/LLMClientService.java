package business.devops.service;

import business.devops.config.LLMConfig;
import framework.llm.LLMClient;
import framework.llm.DeepSeekLLMClient;
import framework.llm.OllamaLLMClient;
import framework.llm.OpenAILLMClient;
import framework.llm.SimpleLLMClient;
import org.springframework.stereotype.Service;

/**
 * LLM客户端服务（DevOps业务）
 * 
 * 负责创建和初始化LLM客户端
 */
@Service
public class LLMClientService {
    
    private final LLMConfig llmConfig;
    private LLMClient llmClient;
    
    public LLMClientService(LLMConfig llmConfig) {
        this.llmConfig = llmConfig;
        this.llmClient = createLLMClient();
    }
    
    /**
     * 创建LLM客户端
     */
    private LLMClient createLLMClient() {
        if (!llmConfig.isConfigured()) {
            System.out.println("⚠️  未设置API Key，使用SimpleLLMClient（模拟模式）\n");
            System.out.println("💡 提示：要使用真实API，请设置环境变量：");
            System.out.println("   export DEFAULT_LLM_API_KEY=\"your-api-key\"");
            System.out.println("   export DEFAULT_LLM_BASE_URL=\"https://...\"");
            System.out.println("   export DEFAULT_LLM_MODEL_NAME=\"model-name\"\n");
            return new SimpleLLMClient();
        }
        
        String provider = llmConfig.getProvider();
        String apiKey = llmConfig.getApiKey();
        String modelName = llmConfig.getModelName();
        String baseUrl = llmConfig.getBaseUrl();
        
        switch (provider) {
            case "deepseek":
                DeepSeekLLMClient deepSeekClient = new DeepSeekLLMClient(apiKey, modelName, baseUrl);
                System.out.println("✅ DeepSeek LLM客户端初始化成功（使用真实API）");
                System.out.println("   模型: " + deepSeekClient.getModelName());
                System.out.println("   端点: " + deepSeekClient.getBaseUrl() + "\n");
                return deepSeekClient;
                
            default:
                System.out.println("⚠️  未知的LLM提供者: " + provider + "，使用SimpleLLMClient（模拟模式）\n");
                return new SimpleLLMClient();
        }
    }
    
    /**
     * 获取LLM客户端
     */
    public LLMClient getLLMClient() {
        return llmClient;
    }
}

