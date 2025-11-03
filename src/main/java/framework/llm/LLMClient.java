package framework.llm;

import java.util.List;
import java.util.Map;

/**
 * LLM客户端接口（框架核心）
 * 
 * 业务开发人员可以实现此接口来支持新的LLM提供者
 */
public interface LLMClient {
    /**
     * 调用LLM进行对话
     * 
     * @param messages 消息列表，每个消息包含 role 和 content
     * @return LLM的响应文本
     */
    String chat(List<Map<String, String>> messages);
}

