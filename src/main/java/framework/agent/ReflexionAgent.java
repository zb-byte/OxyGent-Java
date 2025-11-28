package framework.agent;

import framework.model.AgentRequest;
import framework.model.AgentResponse;
import framework.model.AgentState;
import framework.model.ReflectionEvaluation;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * ReflexionAgent - 反思改进智能体
 * 
 * 核心能力：
 * - "回答 → 评价 → 改进"循环
 * - 通过评审 Agent 检查答复质量，给出改进建议
 * - 不满意则生成改进 prompt 再答，直到满意或达轮次上限
 * 
 * 执行流程：
 * 1. 调用 worker_agent 生成答案
 * 2. 调用 reflexion_agent 评价答案质量
 * 3. 如果满意，返回答案
 * 4. 如果不满意且未达最大轮次，生成改进提示，继续循环
 * 
 * 使用示例：
 * <pre>
 * // 创建 worker agent（生成答案）
 * ChatAgent workerAgent = new ChatAgent(...);
 * 
 * // 创建 reflexion agent（评价答案）
 * ChatAgent reflexionAgent = new ChatAgent(...);
 * 
 * // 创建 ReflexionAgent
 * ReflexionAgent reflexion = new ReflexionAgent(
 *     "reflexion_agent",
 *     "反思改进智能体",
 *     true,  // 主控智能体
 *     "worker_agent",
 *     "reflexion_agent",
 *     3  // 最大反思轮次
 * );
 * </pre>
 */
public class ReflexionAgent implements Agent {
    private final String name;
    private final String description;
    private final boolean isMaster;
    private final String workerAgentName;  // 生成答案的 Agent
    private final String reflexionAgentName;  // 评价答案的 Agent
    private final int maxReflexionRounds;  // 最大反思轮次
    
    // 自定义解析函数
    private final Function<String, String> parseWorkerResponse;  // 解析 worker 响应
    private final Function<String, ReflectionEvaluation> parseReflexionResponse;  // 解析 reflexion 响应
    
    // 评价模板
    private final String evaluationTemplate;
    private final String improvementTemplate;
    
    private AgentFramework framework;
    
    /**
     * 构造函数
     * 
     * @param name 智能体名称
     * @param description 智能体描述
     * @param isMaster 是否为主控智能体
     * @param workerAgentName 生成答案的 Agent 名称
     * @param reflexionAgentName 评价答案的 Agent 名称
     * @param maxReflexionRounds 最大反思轮次
     */
    public ReflexionAgent(String name, String description, boolean isMaster,
                         String workerAgentName, String reflexionAgentName,
                         int maxReflexionRounds) {
        this(name, description, isMaster, workerAgentName, reflexionAgentName,
             maxReflexionRounds, null, null, null, null);
    }
    
    /**
     * 完整构造函数
     * 
     * @param name 智能体名称
     * @param description 智能体描述
     * @param isMaster 是否为主控智能体
     * @param workerAgentName 生成答案的 Agent 名称
     * @param reflexionAgentName 评价答案的 Agent 名称
     * @param maxReflexionRounds 最大反思轮次
     * @param parseWorkerResponse 自定义 worker 响应解析函数（可选）
     * @param parseReflexionResponse 自定义 reflexion 响应解析函数（可选）
     * @param evaluationTemplate 评价模板（可选，使用默认模板）
     * @param improvementTemplate 改进模板（可选，使用默认模板）
     */
    public ReflexionAgent(String name, String description, boolean isMaster,
                         String workerAgentName, String reflexionAgentName,
                         int maxReflexionRounds,
                         Function<String, String> parseWorkerResponse,
                         Function<String, ReflectionEvaluation> parseReflexionResponse,
                         String evaluationTemplate,
                         String improvementTemplate) {
        this.name = name;
        this.description = description;
        this.isMaster = isMaster;
        this.workerAgentName = workerAgentName;
        this.reflexionAgentName = reflexionAgentName;
        this.maxReflexionRounds = maxReflexionRounds;
        this.parseWorkerResponse = parseWorkerResponse != null ? parseWorkerResponse : this::defaultParseWorkerResponse;
        this.parseReflexionResponse = parseReflexionResponse != null ? parseReflexionResponse : this::defaultParseReflexionResponse;
        this.evaluationTemplate = evaluationTemplate != null ? evaluationTemplate : getDefaultEvaluationTemplate();
        this.improvementTemplate = improvementTemplate != null ? improvementTemplate : getDefaultImprovementTemplate();
    }
    
    @Override
    public CompletableFuture<AgentResponse> execute(AgentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("\n🔄 [" + name + "] 开始反思流程: " + request.getQuery());
            
            // 确保请求对象有框架引用
            if (request.getFramework() == null) {
                request.setFramework(framework);
            }
            
            String originalQuery = request.getQuery();
            String currentQuery = originalQuery;
            String currentAnswer = "";
            ReflectionEvaluation lastEvaluation = null;
            
            // 反思循环
            for (int round = 0; round <= maxReflexionRounds; round++) {
                System.out.println("  📍 反思轮次 " + (round + 1) + "/" + (maxReflexionRounds + 1));
                
                try {
                    // 步骤1：调用 worker_agent 生成答案
                    System.out.println("  🤖 调用 " + workerAgentName + " 生成答案...");
                    AgentResponse workerResponse = request.call(workerAgentName, 
                        Map.of("query", currentQuery)).join();
                    
                    if (workerResponse.getState() != AgentState.COMPLETED) {
                        System.out.println("  ❌ Worker Agent 执行失败: " + workerResponse.getOutput());
                        return new AgentResponse(
                            AgentState.FAILED,
                            "Worker Agent 执行失败: " + workerResponse.getOutput(),
                            null,
                            request
                        );
                    }
                    
                    currentAnswer = parseWorkerResponse.apply(workerResponse.getOutput());
                    System.out.println("  ✅ 获得答案: " + currentAnswer.substring(0, Math.min(100, currentAnswer.length())) + "...");
                    
                    // 步骤2：调用 reflexion_agent 评价答案
                    System.out.println("  🔍 调用 " + reflexionAgentName + " 评价答案...");
                    String evaluationQuery = buildEvaluationQuery(originalQuery, currentAnswer);
                    AgentResponse reflexionResponse = request.call(reflexionAgentName,
                        Map.of("query", evaluationQuery)).join();
                    
                    if (reflexionResponse.getState() != AgentState.COMPLETED) {
                        System.out.println("  ⚠️  Reflexion Agent 执行失败，继续使用当前答案");
                        break;
                    }
                    
                    lastEvaluation = parseReflexionResponse.apply(reflexionResponse.getOutput());
                    System.out.println("  📊 评价结果: " + (lastEvaluation.isSatisfactory() ? "满意" : "不满意"));
                    if (!lastEvaluation.getEvaluationReason().isEmpty()) {
                        System.out.println("  💭 评价原因: " + lastEvaluation.getEvaluationReason().substring(0, 
                            Math.min(100, lastEvaluation.getEvaluationReason().length())) + "...");
                    }
                    
                    // 步骤3：如果满意，返回答案
                    if (lastEvaluation.isSatisfactory()) {
                        System.out.println("  ✅ 答案满意，经过 " + (round + 1) + " 轮反思");
                        Map<String, Object> extra = new HashMap<>();
                        extra.put("reflexion_rounds", round + 1);
                        extra.put("final_evaluation", lastEvaluation);
                        return new AgentResponse(
                            AgentState.COMPLETED,
                            "Final answer optimized through " + (round + 1) + " rounds of reflexion:\n\n" + currentAnswer,
                            extra,
                            request
                        );
                    }
                    
                    // 步骤4：如果不满意且未达最大轮次，生成改进提示
                    if (round < maxReflexionRounds) {
                        if (!lastEvaluation.getImprovementSuggestions().isEmpty()) {
                            currentQuery = buildImprovementQuery(originalQuery, 
                                lastEvaluation.getImprovementSuggestions(), currentAnswer);
                            System.out.println("  🔧 生成改进提示，继续下一轮...");
                        } else {
                            // 如果没有具体建议，使用评价原因
                            currentQuery = originalQuery + "\n\nPlease provide a better answer. Previous attempt was: " + 
                                lastEvaluation.getEvaluationReason();
                        }
                    }
                    
                } catch (Exception e) {
                    System.out.println("  ❌ 反思流程执行异常: " + e.getMessage());
                    e.printStackTrace();
                    return new AgentResponse(
                        AgentState.FAILED,
                        "反思流程执行异常: " + e.getMessage(),
                        null,
                        request
                    );
                }
            }
            
            // 达到最大轮次，返回最后一次的答案
            System.out.println("  ⚠️  达到最大反思轮次 (" + (maxReflexionRounds + 1) + ")，返回当前最佳答案");
            Map<String, Object> extra = new HashMap<>();
            extra.put("reflexion_rounds", maxReflexionRounds + 1);
            if (lastEvaluation != null) {
                extra.put("final_evaluation", lastEvaluation);
            }
            extra.put("reached_max_rounds", true);
            
            return new AgentResponse(
                AgentState.COMPLETED,
                "Answer after " + (maxReflexionRounds + 1) + " rounds of reflexion attempts:\n\n" + currentAnswer,
                extra,
                request
            );
        });
    }
    
    /**
     * 构建评价查询
     */
    private String buildEvaluationQuery(String originalQuery, String answer) {
        return evaluationTemplate
            .replace("{query}", originalQuery)
            .replace("{answer}", answer);
    }
    
    /**
     * 构建改进查询
     */
    private String buildImprovementQuery(String originalQuery, String improvementSuggestions, String previousAnswer) {
        return improvementTemplate
            .replace("{original_query}", originalQuery)
            .replace("{improvement_suggestions}", improvementSuggestions)
            .replace("{previous_answer}", previousAnswer);
    }
    
    /**
     * 默认 worker 响应解析（直接返回）
     */
    private String defaultParseWorkerResponse(String response) {
        return response != null ? response.trim() : "";
    }
    
    /**
     * 默认 reflexion 响应解析
     */
    private ReflectionEvaluation defaultParseReflexionResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return new ReflectionEvaluation(false, "No evaluation provided", "");
        }
        
        String[] lines = response.split("\n");
        boolean isSatisfactory = false;
        String evaluationReason = "";
        String improvementSuggestions = "";
        
        for (String line : lines) {
            String lowerLine = line.toLowerCase().trim();
            if (lowerLine.contains("is_satisfactory") || lowerLine.contains("satisfactory")) {
                isSatisfactory = lowerLine.contains("true") || 
                                (lowerLine.contains("satisfactory") && !lowerLine.contains("unsatisfactory"));
            } else if (lowerLine.contains("evaluation_reason") || lowerLine.contains("evaluation reason")) {
                int colonIndex = line.indexOf(":");
                if (colonIndex >= 0) {
                    evaluationReason = line.substring(colonIndex + 1).trim();
                }
            } else if (lowerLine.contains("improvement_suggestions") || lowerLine.contains("improvement suggestions")) {
                int colonIndex = line.indexOf(":");
                if (colonIndex >= 0) {
                    improvementSuggestions = line.substring(colonIndex + 1).trim();
                }
            }
        }
        
        // 如果没有找到明确的满意标记，尝试从文本中推断
        if (!response.toLowerCase().contains("satisfactory") && !response.toLowerCase().contains("unsatisfactory")) {
            // 默认认为不满意，需要改进
            isSatisfactory = false;
            if (evaluationReason.isEmpty()) {
                evaluationReason = "Evaluation result not clearly specified";
            }
        }
        
        return new ReflectionEvaluation(
            isSatisfactory,
            evaluationReason.isEmpty() ? "No specific reason provided" : evaluationReason,
            improvementSuggestions
        );
    }
    
    /**
     * 获取默认评价模板
     */
    private static String getDefaultEvaluationTemplate() {
        return "Please evaluate the quality of the following answer:\n\n" +
               "Original Question: {query}\n\n" +
               "Answer: {answer}\n\n" +
               "Please evaluate based on these criteria:\n" +
               "1. Accuracy: Is the information correct and factual?\n" +
               "2. Completeness: Does it fully address the user's question?\n" +
               "3. Clarity: Is it well-structured and easy to understand?\n" +
               "4. Relevance: Does it stay focused on the user's needs?\n" +
               "5. Helpfulness: Does it provide practical value to the user?\n\n" +
               "Return your evaluation in the following format:\n" +
               "- is_satisfactory: true/false\n" +
               "- evaluation_reason: [Detailed explanation]\n" +
               "- improvement_suggestions: [Specific recommendations if unsatisfactory]";
    }
    
    /**
     * 获取默认改进模板
     */
    private static String getDefaultImprovementTemplate() {
        return "{original_query}\n\n" +
               "Please improve your previous answer based on the following feedback:\n" +
               "{improvement_suggestions}\n\n" +
               "Previous answer: {previous_answer}";
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

