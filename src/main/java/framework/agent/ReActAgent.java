package framework.agent;

import framework.memory.ReactMemory;
import framework.memory.Observation;
import framework.llm.LLMClient;
import framework.model.AgentRequest;
import framework.model.AgentResponse;
import framework.model.ToolCall;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * ReAct智能体实现（框架核心）
 * 封装了ReAct循环、内存管理和工具调用机制
 * 
 * ⚠️ 这是框架代码，业务开发人员应该使用此类，但不要修改
 */
public class ReActAgent implements Agent {
    private final String name;
    private final String description;
    private final boolean isMaster;
    private final LLMClient llmClient;
    private final List<String> subAgents; // 可调用的子智能体
    private final List<String> tools;     // 可用的工具
    private final String systemPrompt;    // 系统提示（包含流程描述）
    private final int maxReactRounds;
    
    private AgentFramework framework;
    
    public ReActAgent(String name, String description, boolean isMaster,
                     LLMClient llmClient, List<String> subAgents, 
                     List<String> tools, String systemPrompt, int maxReactRounds) {
        this.name = name;
        this.description = description;
        this.isMaster = isMaster;
        this.llmClient = llmClient;
        this.subAgents = subAgents != null ? subAgents : new ArrayList<>();
        this.tools = tools != null ? tools : new ArrayList<>();
        this.systemPrompt = systemPrompt;
        this.maxReactRounds = maxReactRounds;
    }
    
    @Override
    public CompletableFuture<AgentResponse> execute(AgentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("\n🤖 [" + name + "] 开始执行任务: " + request.getQuery());
            
            // 初始化ReAct内存
            ReactMemory reactMemory = new ReactMemory();
            
            // ReAct循环：自动顺序执行
            for (int round = 0; round <= maxReactRounds; round++) {
                System.out.println("  📍 Round " + round + " - " + name);
                
                // 1. 构建完整上下文（包含历史结果）
                List<Map<String, String>> messages = buildMessages(request, reactMemory);
                
                // 2. 调用LLM进行推理决策
                String llmResponse = llmClient.chat(messages);
                System.out.println("  💭 LLM决策: " + llmResponse.substring(0, Math.min(100, llmResponse.length())) + "...");
                
                // 3. 解析LLM响应
                LLMDecision decision = parseLLMResponse(llmResponse);
                
                // 4. 根据决策执行
                if (decision.type == DecisionType.ANSWER) {
                    // 最终答案，退出循环
                    System.out.println("  ✅ 获得最终答案，退出ReAct循环");
                    return new AgentResponse(decision.content, true, reactMemory.toHistory());
                    
                } else if (decision.type == DecisionType.TOOL_CALL) {
                    // 工具调用（可能是子智能体或工具）
                    try {
                        // 执行工具调用（自动路由）
                        AgentResponse toolResponse = executeToolCall(decision.toolCall, request);
                        
                        // 收集执行结果
                        Observation observation = new Observation(
                            decision.toolCall.getToolName(),
                            toolResponse.getOutput()
                        );
                        
                        // 更新react_memory（自动记录和传递历史）
                        reactMemory.addRound(llmResponse, observation.toString());
                        
                        System.out.println("  🔧 工具调用完成: " + decision.toolCall.getToolName());
                        System.out.println("  📝 结果: " + toolResponse.getOutput().substring(0, Math.min(80, toolResponse.getOutput().length())) + "...");
                        
                    } catch (Exception e) {
                        // 工具调用失败，加入内存供下一轮修复
                        reactMemory.addRound(llmResponse, "错误: " + e.getMessage());
                        System.out.println("  ❌ 工具调用失败: " + e.getMessage());
                    }
                    
                } else {
                    // 解析错误，加入内存供下一轮修正
                    reactMemory.addRound(llmResponse, "格式错误，请重试");
                    System.out.println("  ⚠️  LLM响应格式错误，重试中...");
                }
            }
            
            // 达到最大轮次，返回最后一次的结果
            return new AgentResponse("达到最大执行轮次", false, reactMemory.toHistory());
        });
    }
    
    /**
     * 构建完整上下文（包含历史结果）
     */
    private List<Map<String, String>> buildMessages(AgentRequest request, ReactMemory reactMemory) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 1. 系统提示（包含流程描述）
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", buildSystemPrompt());
        messages.add(systemMsg);
        
        // 2. 原始用户查询
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", request.getQuery());
        messages.add(userMsg);
        
        // 3. ReAct历史（自动传递历史结果）
        for (ReactMemory.Round round : reactMemory.getRounds()) {
            Map<String, String> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", round.getThought());
            messages.add(assistantMsg);
            
            Map<String, String> userResponse = new HashMap<>();
            userResponse.put("role", "user");
            userResponse.put("content", round.getObservation());
            messages.add(userResponse);
        }
        
        return messages;
    }
    
    /**
     * 构建系统提示（包含流程描述和可用工具列表）
     */
    private String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder(systemPrompt);
        
        // 添加可用工具列表
        if (!subAgents.isEmpty() || !tools.isEmpty()) {
            prompt.append("\n\n可用工具：");
            
            if (!subAgents.isEmpty()) {
                prompt.append("\n- 子智能体: ");
                prompt.append(String.join(", ", subAgents));
            }
            
            if (!tools.isEmpty()) {
                prompt.append("\n- 工具: ");
                prompt.append(String.join(", ", tools));
            }
            
            prompt.append("\n\n调用格式（JSON）:");
            prompt.append("\n{\"type\": \"tool_call\", \"tool_name\": \"工具名\", \"arguments\": {\"query\": \"...\"}}");
            prompt.append("\n或直接回答:");
            prompt.append("\n{\"type\": \"answer\", \"content\": \"...\"}");
        }
        
        return prompt.toString();
    }
    
    /**
     * 解析LLM响应（判断是工具调用还是最终答案）
     */
    private LLMDecision parseLLMResponse(String response) {
        // 简单JSON解析
        try {
            // 查找JSON对象
            int jsonStart = response.indexOf("{");
            int jsonEnd = response.lastIndexOf("}");
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = response.substring(jsonStart, jsonEnd + 1);
                
                // 简单的JSON解析
                if (jsonStr.contains("\"type\": \"tool_call\"")) {
                    // 提取工具名和参数
                    String toolName = extractJsonValue(jsonStr, "tool_name");
                    Map<String, Object> arguments = extractArguments(jsonStr);
                    
                    return new LLMDecision(DecisionType.TOOL_CALL, 
                                          new ToolCall(toolName, arguments), null);
                } else if (jsonStr.contains("\"type\": \"answer\"")) {
                    String content = extractJsonValue(jsonStr, "content");
                    return new LLMDecision(DecisionType.ANSWER, null, content);
                }
            }
        } catch (Exception e) {
            // JSON解析失败，当作普通文本回答
        }
        
        // 如果包含工具名，尝试提取
        for (String agentName : subAgents) {
            if (response.contains(agentName)) {
                Map<String, Object> arguments = new HashMap<>();
                arguments.put("query", response);
                return new LLMDecision(DecisionType.TOOL_CALL, 
                                     new ToolCall(agentName, arguments), null);
            }
        }
        
        // 默认当作最终答案
        return new LLMDecision(DecisionType.ANSWER, null, response);
    }
    
    /**
     * 执行工具调用（自动路由）
     */
    private AgentResponse executeToolCall(ToolCall toolCall, AgentRequest originalRequest) {
        String toolName = toolCall.getToolName();
        
        // 1. 检查是否是子智能体
        if (subAgents.contains(toolName) && framework != null) {
            // 调用子智能体（通过框架自动路由）
            System.out.println("    🔄 路由到子智能体: " + toolName);
            
            AgentRequest subRequest = new AgentRequest(
                (String) toolCall.getArguments().getOrDefault("query", ""),
                originalRequest.getTraceId(),
                originalRequest.getCaller(),
                toolName
            );
            
            // 传递参数（自动传递结果）
            subRequest.getArguments().putAll(toolCall.getArguments());
            
            try {
                // 同步调用
                return framework.getAgent(toolName).execute(subRequest).join();
            } catch (Exception e) {
                throw new RuntimeException("子智能体调用失败: " + e.getMessage(), e);
            }
        }
        
        // 2. 检查是否是工具
        if (tools.contains(toolName) && framework != null) {
            System.out.println("    🛠️  调用工具: " + toolName);
            // 这里可以扩展工具调用逻辑
            return new AgentResponse("工具调用结果: " + toolName, true, new ArrayList<>());
        }
        
        throw new IllegalArgumentException("未知的工具或智能体: " + toolName);
    }
    
    // 简单的JSON提取方法
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }
    
    private Map<String, Object> extractArguments(String json) {
        Map<String, Object> args = new HashMap<>();
        // 简单提取query参数
        String query = extractJsonValue(json, "query");
        if (!query.isEmpty()) {
            args.put("query", query);
        }
        return args;
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
    
    // ========== 内部类 ==========
    
    private enum DecisionType {
        ANSWER, TOOL_CALL, ERROR
    }
    
    private static class LLMDecision {
        DecisionType type;
        ToolCall toolCall;
        String content;
        
        LLMDecision(DecisionType type, ToolCall toolCall, String content) {
            this.type = type;
            this.toolCall = toolCall;
            this.content = content;
        }
    }
}

