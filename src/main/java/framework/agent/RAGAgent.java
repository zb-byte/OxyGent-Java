package framework.agent;

import framework.llm.LLMClient;
import framework.model.AgentRequest;
import framework.model.AgentResponse;
import java.util.concurrent.CompletableFuture;

/**
 * RAGAgent - 检索增强生成智能体
 * 
 * 核心能力：
 * - 在 ChatAgent 基础上，执行检索增强（RAG）
 * - 调用知识检索函数（需要用户自定义）来拉取知识
 * - 将检索到的知识填充到 prompt 中
 * - 输入为 query，内部先调用检索函数，输出为结合知识后的 LLM 回答
 * 
 * 使用示例：
 * <pre>
 * // 方式1：使用 KnowledgeRetriever 接口
 * RAGAgent ragAgent = new RAGAgent(
 *     "rag_agent", "检索增强智能体", false,
 *     llmClient, null, 10, "knowledge",
 *     request -> {
 *         String query = request.getQuery();
 *         // 从数据库检索知识
 *         String knowledge = searchFromDatabase(query);
 *         return CompletableFuture.completedFuture(knowledge);
 *     }
 * );
 * 
 * // 方式2：使用同步检索函数（会自动包装为异步）
 * RAGAgent ragAgent = new RAGAgent(
 *     "rag_agent", "检索增强智能体", false,
 *     llmClient, null, 10, "knowledge",
 *     KnowledgeRetriever.fromSync(request -> {
 *         return "检索到的知识...";
 *     })
 * );
 * </pre>
 */
public class RAGAgent extends ChatAgent {
    private final String knowledgePlaceholder; // 知识占位符（默认 "knowledge"）
    private final KnowledgeRetriever knowledgeRetriever; // 知识检索器
    
    /**
     * 构造函数（使用 KnowledgeRetriever 接口）
     * 
     * @param name 智能体名称
     * @param description 智能体描述
     * @param isMaster 是否为主控智能体
     * @param llmClient LLM 客户端
     * @param systemPrompt 系统提示词（支持 ${knowledge} 占位符，如果为 null 则使用默认提示词）
     * @param shortMemorySize 短期记忆大小（保留的对话轮数）
     * @param knowledgePlaceholder 知识占位符名称（默认 "knowledge"）
     * @param knowledgeRetriever 知识检索器（如果为 null，则跳过检索）
     */
    public RAGAgent(String name, String description, boolean isMaster,
                    LLMClient llmClient, String systemPrompt, int shortMemorySize,
                    String knowledgePlaceholder,
                    KnowledgeRetriever knowledgeRetriever) {
        super(name, description, isMaster, llmClient, 
              buildDefaultPrompt(knowledgePlaceholder != null ? knowledgePlaceholder : "knowledge"),
              shortMemorySize);
        this.knowledgePlaceholder = knowledgePlaceholder != null ? knowledgePlaceholder : "knowledge";
        this.knowledgeRetriever = knowledgeRetriever;
    }
    
    /**
     * 构建默认提示词（如果用户未提供）
     */
    private static String buildDefaultPrompt(String placeholder) {
        return "You are a helpful assistant. You can refer to the following information to answer the questions.\n${" + placeholder + "}";
    }
    
    @Override
    public CompletableFuture<AgentResponse> execute(AgentRequest request) {
        // 预处理：调用知识检索函数
        return preProcess(request)
            .thenCompose(preprocessedRequest -> {
                // 调用父类的 execute 方法（ChatAgent 的逻辑）
                return super.execute(preprocessedRequest);
            });
    }
    
    /**
     * 预处理：调用知识检索函数，将结果填充到 arguments 中
     * 对应 Python 版本的 _pre_process() 方法
     */
    private CompletableFuture<AgentRequest> preProcess(AgentRequest request) {
        if (knowledgeRetriever == null) {
            System.out.println("  ⚠️  未设置知识检索器，跳过检索");
            return CompletableFuture.completedFuture(request);
        }
        
        System.out.println("  🔍 开始检索知识...");
        
        return knowledgeRetriever.retrieve(request)
            .thenApply(knowledge -> {
                // 将检索到的知识填充到 arguments 中
                if (request.getArguments() == null) {
                    request.setArguments(new java.util.HashMap<>());
                }
                request.getArguments().put(knowledgePlaceholder, knowledge);
                System.out.println("  ✅ 知识检索完成，已填充到 prompt");
                return request;
            })
            .exceptionally(throwable -> {
                System.out.println("  ❌ 知识检索失败: " + throwable.getMessage());
                // 即使检索失败，也继续执行（知识为空）
                if (request.getArguments() == null) {
                    request.setArguments(new java.util.HashMap<>());
                }
                request.getArguments().put(knowledgePlaceholder, "");
                return request;
            });
    }
}

