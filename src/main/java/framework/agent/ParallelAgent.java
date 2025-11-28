package framework.agent;

import framework.llm.LLMClient;
import framework.model.AgentRequest;
import framework.model.AgentResponse;
import framework.model.AgentState;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ParallelAgent - 并行执行智能体
 * 
 * 核心能力：
 * - 将同一个任务并行发给多个"队友"（permitted_tool_name_list 中的工具/智能体）
 * - 聚合所有结果
 * - 使用 LLM 总结所有并行执行的结果
 * - 适用于多角色/多策略/多模型并行求解，再统一总结
 */
public class ParallelAgent implements Agent {
    private final String name;
    private final String description;
    private final boolean isMaster;
    private final LLMClient llmClient; // 用于总结结果的 LLM
    private final List<String> permittedToolNameList; // 允许调用的工具/智能体列表
    
    private AgentFramework framework;
    
    public ParallelAgent(String name, String description, boolean isMaster,
                        LLMClient llmClient, List<String> permittedToolNameList) {
        this.name = name;
        this.description = description;
        this.isMaster = isMaster;
        this.llmClient = llmClient;
        this.permittedToolNameList = permittedToolNameList != null ? permittedToolNameList : new ArrayList<>();
    }
    
    @Override
    public CompletableFuture<AgentResponse> execute(AgentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("\n🔄 [" + name + "] 开始并行执行任务: " + request.getQuery());
            
            // 确保请求对象有框架引用
            if (request.getFramework() == null) {
                request.setFramework(framework);
            }
            
            if (permittedToolNameList.isEmpty()) {
                System.out.println("  ⚠️  未设置允许调用的工具/智能体列表");
                return new AgentResponse(
                    AgentState.FAILED,
                    "未设置允许调用的工具/智能体列表",
                    null,
                    request
                );
            }
            
            System.out.println("  📋 并行执行列表: " + String.join(", ", permittedToolNameList));
            
            try {
                // 1. 并行调用所有工具/智能体
                List<CompletableFuture<AgentResponse>> futures = new ArrayList<>();
                String parallelId = UUID.randomUUID().toString();
                
                for (String toolName : permittedToolNameList) {
                    // 克隆请求，设置 parallelId
                    AgentRequest clonedRequest = request.cloneWith(toolName, request.getArguments());
                    clonedRequest.setParallelId(parallelId);
                    
                    CompletableFuture<AgentResponse> future = request.call(toolName, request.getArguments());
                    futures.add(future);
                }
                
                // 2. 等待所有并行任务完成
                System.out.println("  ⏳ 等待所有并行任务完成...");
                List<AgentResponse> responses = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
                
                System.out.println("  ✅ 所有并行任务完成，共 " + responses.size() + " 个结果");
                
                // 3. 聚合结果并使用 LLM 总结
                String summary = summarizeResults(request, responses);
                
                return new AgentResponse(
                    AgentState.COMPLETED,
                    summary,
                    null,
                    request
                );
                
            } catch (Exception e) {
                System.out.println("  ❌ 并行执行失败: " + e.getMessage());
                e.printStackTrace();
                return new AgentResponse(
                    AgentState.FAILED,
                    "并行执行失败: " + e.getMessage(),
                    null,
                    request
                );
            }
        });
    }
    
    /**
     * 使用 LLM 总结所有并行执行的结果
     */
    private String summarizeResults(AgentRequest request, List<AgentResponse> responses) {
        System.out.println("  📊 开始总结并行执行结果...");
        
        // 构建总结提示
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 系统提示
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "You are a helpful assistant. The user's question is: " + request.getQuery() + 
                      "\nPlease summarize the results of the parallel execution of the above tasks.");
        messages.add(systemMsg);
        
        // 用户消息：包含所有并行结果
        StringBuilder resultsText = new StringBuilder("The parallel results are as following:\n");
        for (int i = 0; i < responses.size(); i++) {
            AgentResponse response = responses.get(i);
            resultsText.append((i + 1)).append(". ").append(response.getOutput()).append("\n");
        }
        
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", resultsText.toString());
        messages.add(userMsg);
        
        // 调用 LLM 总结
        try {
            String summary = llmClient.chat(messages);
            System.out.println("  ✅ 总结完成");
            return summary;
        } catch (Exception e) {
            System.out.println("  ⚠️  LLM 总结失败，返回原始结果: " + e.getMessage());
            // 如果 LLM 总结失败，返回所有结果的简单拼接
            return resultsText.toString();
        }
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
    
    @Override
    public List<String> getPermittedToolNameList() {
        return permittedToolNameList;
    }
}

