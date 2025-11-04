package framework.model;

/**
 * 智能体执行状态枚举
 * 对应 Python 版本的 OxyState
 */
public enum AgentState {
    CREATED,    // 已创建
    RUNNING,    // 运行中
    COMPLETED,  // 已完成
    FAILED,     // 失败
    PAUSED,     // 暂停
    SKIPPED,    // 跳过（权限不足等）
    CANCELED    // 取消
}

