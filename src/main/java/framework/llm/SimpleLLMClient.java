package framework.llm;

import java.util.*;

/**
 * 简单的LLM客户端实现（模拟 - 仅用于演示）
 * 
 * ⚠️ 注意：这是一个模拟实现，使用简单的字符串匹配来判断响应
 * 它不调用真实的大模型API，仅用于演示框架的工作流程
 * 
 * 实际使用时应该使用：
 * - OllamaLLMClient: 连接本地Ollama模型
 * - DeepSeekLLMClient: 连接DeepSeek API
 * - OpenAILLMClient: 连接OpenAI API
 * - 或其他真实的LLM客户端实现
 */
public class SimpleLLMClient implements LLMClient {
    
    @Override
    public String chat(List<Map<String, String>> messages) {
        // 获取最后一条用户消息
        String lastUserMessage = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).get("role"))) {
                lastUserMessage = messages.get(i).get("content");
                break;
            }
        }
        
        // 获取系统提示
        String systemPrompt = "";
        for (Map<String, String> msg : messages) {
            if ("system".equals(msg.get("role"))) {
                systemPrompt = msg.get("content");
                break;
            }
        }
        
        // ========== 模拟逻辑（仅用于演示） ==========
        System.out.println("    🔧 模拟LLM处理（实际应调用真实LLM API）...");
        
        // 1. 检查是否是主控智能体的流程编排决策
        if (systemPrompt.contains("DevOps流程编排") || systemPrompt.contains("主控智能体")) {
            // 检查历史记录，判断当前流程阶段
            boolean hasRequirementAnalysis = false;
            boolean hasCodeWriting = false;
            boolean hasCodeReview = false;
            boolean hasTest = false;
            boolean hasGitCommit = false;
            boolean hasDeploy = false;
            
            // 检查消息历史中的观察结果
            for (Map<String, String> msg : messages) {
                String content = msg.get("content");
                if (content != null) {
                    if (content.contains("需求分析报告") || content.contains("需求分析完成")) {
                        hasRequirementAnalysis = true;
                    } else if (content.contains("代码编写完成")) {
                        hasCodeWriting = true;
                    } else if (content.contains("代码审查报告") || content.contains("代码审查完成")) {
                        hasCodeReview = true;
                    } else if (content.contains("测试报告") || content.contains("测试完成")) {
                        hasTest = true;
                    } else if (content.contains("Git提交成功") || content.contains("Git提交完成")) {
                        hasGitCommit = true;
                    } else if (content.contains("部署成功") || content.contains("部署完成")) {
                        hasDeploy = true;
                    }
                }
            }
            
            // 主控智能体：根据流程阶段调用子智能体
            if (!hasRequirementAnalysis && (lastUserMessage.contains("req-001") || lastUserMessage.contains("需求"))) {
                return "{\"type\": \"tool_call\", \"tool_name\": \"requirement_agent\", \"arguments\": {\"query\": \"分析需求req-001\"}}";
            } else if (hasRequirementAnalysis && !hasCodeWriting) {
                return "{\"type\": \"tool_call\", \"tool_name\": \"code_agent\", \"arguments\": {\"query\": \"根据需求分析报告编写代码\"}}";
            } else if (hasCodeWriting && !hasCodeReview) {
                return "{\"type\": \"answer\", \"tool_name\": \"review_agent\", \"arguments\": {\"query\": \"审查代码质量\"}}";
            } else if (hasCodeReview && !hasTest) {
                return "{\"type\": \"tool_call\", \"tool_name\": \"test_agent\", \"arguments\": {\"query\": \"编写和执行测试用例\"}}";
            } else if (hasTest && !hasGitCommit) {
                return "{\"type\": \"tool_call\", \"tool_name\": \"git_agent\", \"arguments\": {\"query\": \"提交代码到Git\"}}";
            } else if (hasGitCommit && !hasDeploy) {
                return "{\"type\": \"tool_call\", \"tool_name\": \"deploy_agent\", \"arguments\": {\"query\": \"部署应用到staging环境\"}}";
            } else if (hasDeploy) {
                return "{\"type\": \"answer\", \"content\": \"完整开发流程已完成：\\n1. 需求分析：完成\\n2. 代码编写：完成\\n3. 代码审查：完成\\n4. 测试：完成\\n5. Git提交：完成\\n6. 部署：完成\\n\\n所有阶段已成功完成！\"}";
            }
        }
        
        // 2. 子智能体：直接完成任务并返回结果
        if (systemPrompt.contains("需求分析专家")) {
            // requirement_agent: 直接返回需求分析结果
            if (lastUserMessage.contains("req-001") || lastUserMessage.contains("分析需求")) {
                return "{\"type\": \"answer\", \"content\": \"需求分析报告（req-001）：\\n- 功能清单：功能A、功能B、功能C\\n- 技术方案：采用Spring Boot架构\\n- 开发优先级：高优先级\\n\\n需求分析完成。\"}";
            }
        } else if (systemPrompt.contains("代码编写专家")) {
            // code_agent: 返回代码编写结果
            return "{\"type\": \"answer\", \"content\": \"代码编写完成。已生成以下文件：\\n- UserController.java\\n- UserService.java\\n- UserRepository.java\\n\\n代码编写完成。\"}";
        } else if (systemPrompt.contains("代码审查专家")) {
            // review_agent: 返回审查结果
            return "{\"type\": \"answer\", \"content\": \"代码审查报告：\\n- 代码质量评分：85/100\\n- 发现的问题：无严重问题\\n- 改进建议：可以添加更多注释\\n\\n代码审查完成。\"}";
        } else if (systemPrompt.contains("测试专家")) {
            // test_agent: 返回测试结果
            return "{\"type\": \"answer\", \"content\": \"测试报告：\\n- 测试通过率：100%\\n- 代码覆盖率：85%\\n- 失败的用例：0\\n\\n测试完成。\"}";
        } else if (systemPrompt.contains("Git专家")) {
            // git_agent: 返回Git提交结果
            return "{\"type\": \"answer\", \"content\": \"Git提交成功。\\n- Commit Hash: abc123def456\\n- 分支：main\\n- 提交信息：feat: 实现用户管理功能\\n\\nGit提交完成。\"}";
        } else if (systemPrompt.contains("部署专家")) {
            // deploy_agent: 返回部署结果
            return "{\"type\": \"answer\", \"content\": \"部署成功。\\n- 环境：staging\\n- 访问URL：https://staging.example.com\\n- 部署版本：v1.0.0\\n\\n部署完成。\"}";
        }
        
        // 默认返回最终答案
        return "{\"type\": \"answer\", \"content\": \"任务完成\"}";
    }
}

