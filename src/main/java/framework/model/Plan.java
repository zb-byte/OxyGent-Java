package framework.model;

import java.util.List;

/**
 * 计划数据模型
 * 对应 Python 版本的 Plan 类
 * 
 * 用于存储由规划者 Agent 生成的执行计划
 */
public class Plan {
    /**
     * 计划步骤列表（按顺序排列）
     * 例如：["步骤1: 查询当前时间", "步骤2: 保存到文件"]
     */
    private List<String> steps;
    
    public Plan() {
    }
    
    public Plan(List<String> steps) {
        this.steps = steps;
    }
    
    public List<String> getSteps() {
        return steps;
    }
    
    public void setSteps(List<String> steps) {
        this.steps = steps;
    }
}

