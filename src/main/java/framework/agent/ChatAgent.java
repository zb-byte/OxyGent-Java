package framework.agent;

import framework.llm.LLMClient;
import framework.model.AgentRequest;
import framework.model.AgentResponse;
import framework.model.AgentState;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * ChatAgent - 纯对话智能体
 * 
 * 核心能力：
 * - 管理短期记忆（对话历史）
 * - 将系统提示和历史对话拼装后直连 LLM
 * - 输入为 query 和历史对话，输出为 LLM 的回答
 */
public class ChatAgent implements Agent {
    private final String name;
    private final String description;
    private final boolean isMaster;
    private final LLMClient llmClient;
    private final String systemPrompt;
    private final int shortMemorySize; // 短期记忆大小（保留的对话轮数）
    
    private AgentFramework framework;
    
    public ChatAgent(String name, String description, boolean isMaster,
                    LLMClient llmClient, String systemPrompt, int shortMemorySize) {
        this.name = name;
        this.description = description;
        this.isMaster = isMaster;
        this.llmClient = llmClient;
        this.systemPrompt = systemPrompt != null ? systemPrompt : "You are a helpful assistant.";
        this.shortMemorySize = shortMemorySize > 0 ? shortMemorySize : 10; // 默认保留10轮对话
    }
    
    @Override
    public CompletableFuture<AgentResponse> execute(AgentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("\n💬 [" + name + "] 开始对话: " + request.getQuery());
            
            // 确保请求对象有框架引用
            if (request.getFramework() == null) {
                request.setFramework(framework);
            }
            
            // 构建消息列表
            List<Map<String, String>> messages = buildMessages(request);
            
            // 调用 LLM
            try {
                String llmResponse = llmClient.chat(messages);
                System.out.println("  ✅ 获得回答: " + llmResponse.substring(0, Math.min(100, llmResponse.length())) + "...");
                
                return new AgentResponse(
                    AgentState.COMPLETED,
                    llmResponse,
                    null,
                    request
                );
            } catch (Exception e) {
                System.out.println("  ❌ LLM 调用失败: " + e.getMessage());
                return new AgentResponse(
                    AgentState.FAILED,
                    "LLM 调用失败: " + e.getMessage(),
                    null,
                    request
                );
            }
        });
    }
    
    /**
     * 构建消息列表（系统提示 + 历史对话 + 当前查询）
     */
    private List<Map<String, String>> buildMessages(AgentRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 1. 系统提示（支持模板变量替换）
        String prompt = buildSystemPrompt(request);
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", prompt);
        messages.add(systemMsg);
        
        // 2. 加载短期记忆（历史对话）
        List<Map<String, String>> shortMemory = getShortMemory(request);
        if (shortMemory != null && !shortMemory.isEmpty()) {
            // 限制历史对话数量（保留最近的 N 轮对话）
            int maxHistorySize = shortMemorySize * 2; // 每轮包含 user 和 assistant 两条消息
            int startIndex = Math.max(0, shortMemory.size() - maxHistorySize);
            for (int i = startIndex; i < shortMemory.size(); i++) {
                messages.add(shortMemory.get(i));
            }
        }
        
        // 3. 添加当前用户查询
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", request.getQuery());
        messages.add(userMsg);
        
        return messages;
    }
    
    /**
     * 构建系统提示（支持模板变量替换）
     * 例如：${knowledge} 会被替换为 arguments 中 knowledge 的值
     */
    private String buildSystemPrompt(AgentRequest request) {
        String prompt = systemPrompt;
        
        // 替换模板变量（例如 ${knowledge}）
        if (request.getArguments() != null) {
            for (Map.Entry<String, Object> entry : request.getArguments().entrySet()) {
                String placeholder = "${" + entry.getKey() + "}";
                if (prompt.contains(placeholder)) {
                    prompt = prompt.replace(placeholder, String.valueOf(entry.getValue()));
                }
            }
        }
        
        return prompt;
    }
    
    /**
     * 获取短期记忆（从 arguments 中获取 short_memory）
     * 对应 Python 版本的 get_short_memory()
     */
    private List<Map<String, String>> getShortMemory(AgentRequest request) {
        if (request.getArguments() == null) {
            return new ArrayList<>();
        }
        
        // 从 arguments 中获取 short_memory
        Object shortMemoryObj = request.getArguments().get("short_memory");
        if (shortMemoryObj == null) {
            return new ArrayList<>();
        }
        
        // 类型转换
        if (shortMemoryObj instanceof List) {
            List<?> list = (List<?>) shortMemoryObj;
            List<Map<String, String>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> msg = (Map<String, String>) item;
                    result.add(msg);
                }
            }
            return result;
        }
        
        return new ArrayList<>();
    }
    
    // ========== Getters ==========
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public boolean isMaster() {
        return isMaster;
    }
    
    @Override
    public void setFramework(AgentFramework framework) {
        this.framework = framework;
    }
    
    @Override
    public AgentFramework getFramework() {
        return framework;
    }
}

