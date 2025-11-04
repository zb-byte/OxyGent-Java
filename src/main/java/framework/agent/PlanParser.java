package framework.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import framework.model.Plan;
import framework.model.Action;
import framework.model.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 计划解析器
 * 用于将 LLM 输出解析为 Plan 或 Action 对象
 * 
 * 简化版本：使用 JSON 解析，期望 LLM 返回 JSON 格式
 */
public class PlanParser {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 解析规划者 Agent 的输出为 Plan 对象
     * 
     * 期望格式：{"steps": ["步骤1", "步骤2", "步骤3"]}
     * 或者简化格式：直接返回步骤列表的文本描述
     */
    public static Plan parsePlan(String llmOutput) {
        try {
            // 尝试解析为 JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> json = objectMapper.readValue(llmOutput, Map.class);
            if (json.containsKey("steps")) {
                @SuppressWarnings("unchecked")
                List<String> steps = (List<String>) json.get("steps");
                return new Plan(steps != null ? steps : new ArrayList<>());
            }
        } catch (Exception e) {
            // JSON 解析失败，尝试文本解析
        }
        
        // 文本解析：尝试从文本中提取步骤列表
        // 格式：1. 步骤1\n2. 步骤2\n3. 步骤3
        List<String> steps = new ArrayList<>();
        String[] lines = llmOutput.split("\n");
        for (String line : lines) {
            line = line.trim();
            // 匹配 "1. 步骤内容" 或 "步骤1: 步骤内容" 格式
            if (line.matches("^\\d+[.：:].*")) {
                String step = line.replaceFirst("^\\d+[.：:]\\s*", "").trim();
                if (!step.isEmpty()) {
                    steps.add(step);
                }
            }
        }
        
        // 如果解析失败，将整个输出作为单个步骤
        if (steps.isEmpty()) {
            steps.add(llmOutput.trim());
        }
        
        return new Plan(steps);
    }
    
    /**
     * 解析重规划者 Agent 的输出为 Action 对象
     * 
     * 期望格式：
     * - {"action": {"response": "答案"}} 或
     * - {"action": {"steps": ["步骤1", "步骤2"]}}
     */
    public static Action parseAction(String llmOutput) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> json = objectMapper.readValue(llmOutput, Map.class);
            if (json.containsKey("action")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> actionMap = (Map<String, Object>) json.get("action");
                
                if (actionMap.containsKey("response")) {
                    // 是 Response 类型
                    String response = (String) actionMap.get("response");
                    return new Action(new Response(response));
                } else if (actionMap.containsKey("steps")) {
                    // 是 Plan 类型
                    @SuppressWarnings("unchecked")
                    List<String> steps = (List<String>) actionMap.get("steps");
                    return new Action(new Plan(steps != null ? steps : new ArrayList<>()));
                }
            }
        } catch (Exception e) {
            // JSON 解析失败
        }
        
        // 如果解析失败，尝试判断是否为直接回答
        // 如果输出包含明确的回答标记，认为是 Response
        if (llmOutput.toLowerCase().contains("answer") || 
            llmOutput.toLowerCase().contains("response") ||
            !llmOutput.contains("step") && !llmOutput.contains("plan")) {
            return new Action(new Response(llmOutput.trim()));
        }
        
        // 否则尝试解析为 Plan
        Plan plan = parsePlan(llmOutput);
        return new Action(plan);
    }
}

