package framework.model;

/**
 * 动作数据模型
 * 对应 Python 版本的 Action 类
 * 
 * 用于重规划阶段，表示下一步的动作：
 * - 如果已找到答案，action 是 Response 类型
 * - 如果需要继续执行，action 是 Plan 类型
 */
public class Action {
    /**
     * 动作内容
     * 可以是 Response（直接回答）或 Plan（新的执行计划）
     */
    private Object action;
    
    public Action() {
    }
    
    public Action(Object action) {
        this.action = action;
    }
    
    public Object getAction() {
        return action;
    }
    
    public void setAction(Object action) {
        this.action = action;
    }
    
    /**
     * 判断动作是否为 Response 类型（直接回答）
     */
    public boolean isResponse() {
        return action instanceof Response;
    }
    
    /**
     * 判断动作是否为 Plan 类型（新的计划）
     */
    public boolean isPlan() {
        return action instanceof Plan;
    }
    
    /**
     * 获取 Response（如果是 Response 类型）
     */
    public Response getResponse() {
        if (isResponse()) {
            return (Response) action;
        }
        return null;
    }
    
    /**
     * 获取 Plan（如果是 Plan 类型）
     */
    public Plan getPlan() {
        if (isPlan()) {
            return (Plan) action;
        }
        return null;
    }
}

