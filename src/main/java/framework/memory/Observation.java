package framework.memory;

/**
 * 观察结果（框架核心）
 */
public class Observation {
    private String toolName;
    private String result;
    
    public Observation(String toolName, String result) {
        this.toolName = toolName;
        this.result = result;
    }
    
    public String getToolName() {
        return toolName;
    }
    
    public String getResult() {
        return result;
    }
    
    @Override
    public String toString() {
        return "工具: " + toolName + ", 结果: " + result;
    }
}

