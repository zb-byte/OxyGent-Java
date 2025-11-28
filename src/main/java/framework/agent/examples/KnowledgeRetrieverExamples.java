package framework.agent.examples;

import framework.agent.KnowledgeRetriever;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Map;

/**
 * 知识检索器使用示例
 * 
 * 展示如何实现和使用 KnowledgeRetriever 接口
 */
public class KnowledgeRetrieverExamples {
    
    /**
     * 示例1：简单的硬编码知识检索
     */
    public static KnowledgeRetriever simpleRetriever() {
        return request -> {
            String query = request.getQuery();
            System.out.println("检索查询: " + query);
            // 返回硬编码的知识
            return CompletableFuture.completedFuture(
                "Pi is 3.141592653589793238462643383279502."
            );
        };
    }
    
    /**
     * 示例2：从内存数据库检索（模拟）
     */
    public static KnowledgeRetriever memoryDatabaseRetriever() {
        // 模拟的知识库
        Map<String, String> knowledgeBase = new HashMap<>();
        knowledgeBase.put("pi", "Pi is 3.141592653589793238462643383279502.");
        knowledgeBase.put("java", "Java is a high-level, class-based, object-oriented programming language.");
        knowledgeBase.put("spring", "Spring Framework is an application framework for Java.");
        
        return request -> {
            String query = request.getQuery().toLowerCase();
            // 简单的关键词匹配
            String knowledge = knowledgeBase.entrySet().stream()
                .filter(entry -> query.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("未找到相关知识");
            
            return CompletableFuture.completedFuture(knowledge);
        };
    }
    
    /**
     * 示例3：使用同步方法（通过 fromSync 包装）
     */
    public static KnowledgeRetriever syncRetriever() {
        return KnowledgeRetriever.fromSync(request -> {
            String query = request.getQuery();
            // 同步检索逻辑
            return "同步检索到的知识: " + query;
        });
    }
    
    /**
     * 示例4：从文件系统检索
     */
    public static KnowledgeRetriever fileSystemRetriever(String knowledgeFilePath) {
        return request -> {
            try {
                // 从文件读取知识（这里只是示例，实际需要实现文件读取逻辑）
                String knowledge = java.nio.file.Files.readString(
                    java.nio.file.Paths.get(knowledgeFilePath)
                );
                return CompletableFuture.completedFuture(knowledge);
            } catch (Exception e) {
                return CompletableFuture.completedFuture("读取知识文件失败: " + e.getMessage());
            }
        };
    }
    
    /**
     * 示例5：调用外部 API 检索（异步）
     */
    public static KnowledgeRetriever apiRetriever(String apiUrl) {
        return request -> {
            String query = request.getQuery();
            // 调用外部 API（这里只是示例，实际需要实现 HTTP 调用）
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // 模拟 API 调用
                    java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                    java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(apiUrl + "?query=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)))
                        .GET()
                        .build();
                    
                    java.net.http.HttpResponse<String> response = client.send(
                        httpRequest, 
                        java.net.http.HttpResponse.BodyHandlers.ofString()
                    );
                    return response.body();
                } catch (Exception e) {
                    return "API 调用失败: " + e.getMessage();
                }
            });
        };
    }
    
    /**
     * 示例6：使用 Lambda 表达式（最简洁的方式）
     */
    public static KnowledgeRetriever lambdaRetriever() {
        // 直接使用 Lambda 表达式
        return request -> CompletableFuture.completedFuture(
            "这是通过 Lambda 表达式检索到的知识，查询是: " + request.getQuery()
        );
    }
}

