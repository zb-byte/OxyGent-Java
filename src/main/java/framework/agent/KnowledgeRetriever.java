package framework.agent;

import framework.model.AgentRequest;
import java.util.concurrent.CompletableFuture;

/**
 * 知识检索接口
 * 
 * 用于 RAGAgent 的知识检索功能。
 * 用户可以实现此接口来定义自己的知识检索逻辑。
 * 
 * 使用示例：
 * <pre>
 * // 方式1：实现接口
 * KnowledgeRetriever retriever = new KnowledgeRetriever() {
 *     {@literal @}Override
 *     public CompletableFuture<String> retrieve(AgentRequest request) {
 *         String query = request.getQuery();
 *         // 从数据库、向量库或其他来源检索知识
 *         String knowledge = searchFromDatabase(query);
 *         return CompletableFuture.completedFuture(knowledge);
 *     }
 * };
 * 
 * // 方式2：使用 Lambda 表达式
 * KnowledgeRetriever retriever = request -> {
 *     String query = request.getQuery();
 *     return CompletableFuture.completedFuture("检索到的知识...");
 * };
 * 
 * // 方式3：使用静态方法引用（如果是同步方法）
 * KnowledgeRetriever retriever = MyKnowledgeService::retrieve;
 * </pre>
 */
@FunctionalInterface
public interface KnowledgeRetriever {
    /**
     * 检索知识
     * 
     * @param request 智能体请求，包含用户查询等信息
     * @return 检索到的知识内容（异步）
     */
    CompletableFuture<String> retrieve(AgentRequest request);
    
    /**
     * 创建一个同步的知识检索器（将同步方法包装为异步）
     * 
     * @param syncRetriever 同步检索函数
     * @return 异步知识检索器
     */
    static KnowledgeRetriever fromSync(java.util.function.Function<AgentRequest, String> syncRetriever) {
        return request -> CompletableFuture.completedFuture(syncRetriever.apply(request));
    }
}

