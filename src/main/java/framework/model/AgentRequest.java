package framework.model;

import framework.agent.Agent;
import framework.agent.AgentFramework;
import framework.tool.Tool;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * 智能体请求（框架核心）
 * 对应 Python 版本的 OxyRequest
 */
public class AgentRequest {
    // ==================== 基础字段 ====================
    
    /**
     * 用户查询内容或任务描述
     * 用于传递给智能体的主要输入信息，可以是自然语言查询或结构化指令
     */
    private String query;
    
    /**
     * 当前节点的唯一追踪ID（对应 Python 版本的 current_trace_id）
     * 每个请求节点都有唯一的 traceId，用于：
     * - 构建调用链的 DAG（有向无环图）结构
     * - 日志追踪和问题排查
     * - 建立父子节点关系（通过 fromTraceId 关联）
     * 每次调用新智能体时，会生成新的 traceId，同时保留 fromTraceId 指向父节点
     */
    private String traceId;
    
    /**
     * 调用者标识（发起调用的智能体或用户名称）
     * 用于：
     * - 权限校验：检查调用者是否有权限调用目标智能体
     * - 调用链追踪：记录谁发起了这次调用
     * - 上下文传递：在 cloneWith() 中，当前 targetAgent 会成为下一次调用的 caller
     */
    private String caller;
    
    /**
     * 目标智能体名称（对应 Python 版本的 callee）
     * 表示当前请求要执行的智能体或工具名称
     * 在 cloneWith() 方法中，会更新为新的 callee，旧的 targetAgent 成为新的 caller
     */
    private String targetAgent;  // 对应 callee
    
    /**
     * 调用者类别标识
     * 用于区分调用者类型："user"（用户）、"agent"（智能体）、"tool"（工具）等
     * 默认值为 "user"，用于权限校验逻辑（仅非用户调用时才进行权限检查）
     */
    private String callerCategory = "user";
    
    /**
     * 被调用者类别标识
     * 用于标识目标智能体的类别，在 cloneWith() 中会更新为新的调用者类别
     */
    private String calleeCategory = "";
    
    // ==================== 调用栈追踪 ====================
    
    /**
     * 调用栈路径列表（调用链的完整路径）
     * 记录从根节点到当前节点的完整调用路径，例如：["user", "agent_a", "agent_b"]
     * 用途：
     * - 调试和日志：快速了解调用链的完整路径
     * - 防止循环调用：检查是否出现无限递归
     * - 上下文理解：智能体可以了解自己在调用链中的位置
     * 每次调用新智能体时，会在栈中追加被调用者名称
     */
    private List<String> callStack = new ArrayList<>();  // 调用栈路径
    
    /**
     * 节点ID栈（与 callStack 对应）
     * 记录每个调用层级对应的节点ID，与 callStack 一一对应
     * 用于：
     * - 精确追踪每个节点的执行状态
     * - 支持节点级别的数据查询和回溯
     * - 构建完整的执行树结构
     */
    private List<String> nodeIdStack = new ArrayList<>();  // 节点ID栈
    
    /**
     * 当前节点的唯一标识符
     * 每个请求节点都有唯一的 nodeId，用于：
     * - 节点级别的数据存储和检索
     * - 建立父子节点关系（通过 fatherNodeId 关联）
     * - 节点执行状态的追踪和管理
     * 在 cloneWith() 中，当前 nodeId 会成为子节点的 fatherNodeId
     */
    private String nodeId = "";  // 当前节点ID
    
    /**
     * 父节点ID（当前节点的直接父节点）
     * 用于：
     * - 构建节点的树形结构关系
     * - 支持向上回溯查找父节点
     * - 数据继承和上下文传递
     * 在 cloneWith() 中，当前 nodeId 会被设置为克隆节点的 fatherNodeId
     */
    private String fatherNodeId = "";  // 父节点ID
    
    /**
     * 前置节点ID列表（当前节点的所有前置依赖节点）
     * 用于：
     * - 支持并行执行场景：记录同一层级中已完成的前置节点
     * - 数据依赖管理：确保前置节点执行完成后再执行当前节点
     * - DAG 工作流：支持复杂的节点依赖关系
     * 在 cloneWith() 中，当前节点的 latestNodeIds 会复制给子节点的 preNodeIds
     */
    private List<String> preNodeIds = new ArrayList<>();  // 前置节点ID列表
    
    /**
     * 最新节点ID列表（当前层级最新执行的节点）
     * 用于：
     * - 并行执行协调：记录同一层级中最新执行的节点
     * - 数据同步：确保后续节点能获取到最新节点的数据
     * - 状态追踪：追踪当前执行层级的最新状态
     * 在 cloneWith() 中，当前节点的 latestNodeIds 会传递给子节点的 preNodeIds
     */
    private List<String> latestNodeIds = new ArrayList<>();  // 最新节点ID列表
    
    // ==================== 并行执行追踪 ====================
    
    /**
     * 并行执行组ID
     * 用于标识属于同一并行执行组的节点
     * 用途：
     * - 并行任务协调：同一 parallelId 的节点可以并发执行
     * - 结果聚合：等待同一组的所有节点完成后再继续
     * - 数据共享：并行节点可以共享 parallelDict 中的数据
     * 如果当前节点是并行组的第一个节点，会生成新的 parallelId；否则继承父节点的 parallelId
     */
    private String parallelId = "";  // 并行执行ID
    
    /**
     * 并行执行字典（并行组内的共享数据）
     * 用于在并行执行的节点之间共享数据
     * 用途：
     * - 数据传递：并行节点可以读取和写入共享数据
     * - 结果聚合：收集并行节点的执行结果
     * - 状态协调：协调并行节点的执行状态
     * 通过引用共享，确保同一并行组的所有节点访问同一份数据
     */
    private Map<String, Object> parallelDict = new HashMap<>();  // 并行执行字典
    
    // ==================== 上下文数据 ====================
    
    /**
     * 节点级参数数据（当前节点的私有数据）
     * 包含：
     * - 用户输入参数
     * - 工具调用参数
     * - 节点特定的配置数据
     * 特点：
     * - 作用域：仅在当前节点内有效
     * - 数据隔离：每个节点有独立的 arguments，不会相互影响
     * - 数据传递：在 cloneWith() 中会复制给子节点，子节点可以添加新参数
     */
    private Map<String, Object> arguments = new HashMap<>();  // 节点级数据
    
    /**
     * 请求级共享数据（同一 trace 内的所有节点共享）
     * 用于在同一调用链的所有节点之间共享数据
     * 特点：
     * - 作用域：同一 traceId 分支下的所有节点共享
     * - 数据共享：通过引用共享（非深拷贝），所有节点访问同一份数据
     * - 生命周期：随着调用链的结束而失效
     * 用途：
     * - 中间结果传递：子节点可以读取父节点设置的数据
     * - 状态累积：在调用链中累积状态信息
     * - 上下文传递：传递调用链级别的上下文信息
     */
    private Map<String, Object> sharedData = new HashMap<>();  // 请求级共享数据（同trace内共享）
    
    /**
     * 会话级共享数据（同一会话组内的所有请求共享）
     * 用于在同一会话的所有请求之间共享数据
     * 特点：
     * - 作用域：同一 groupId 的所有请求共享
     * - 数据共享：通过引用共享（非深拷贝），跨请求的数据持久化
     * - 生命周期：在整个会话期间有效
     * 用途：
     * - 会话状态：保存用户会话的持久化状态
     * - 历史记录：跨请求的历史信息
     * - 上下文记忆：长期上下文信息的维护
     */
    private Map<String, Object> groupData = new HashMap<>();  // 会话级共享数据（同group内共享）
    
    // ==================== 框架引用 ====================
    
    /**
     * 框架引用（用于调用其他智能体或工具）
     * 提供访问框架内所有注册的智能体和工具的能力
     * 用途：
     * - 智能体发现：通过 framework.getAllAgents() 查找可用的智能体
     * - 工具调用：通过 framework.getTool() 获取工具实例
     * - 权限检查：通过 framework.getAgent() 获取智能体配置进行权限校验
     * - 统一调用：在 call() 方法中统一处理智能体和工具的调用
     * 在 cloneWith() 中会复制给子节点，确保子节点也能调用其他智能体
     */
    private AgentFramework framework;
    
    // ==================== 会话标识 ====================
    
    /**
     * 客户端请求ID（客户端生成的唯一请求标识）
     * 用于：
     * - 客户端追踪：客户端可以通过 requestId 追踪自己的请求
     * - 请求去重：防止重复提交相同的请求
     * - 日志关联：将客户端请求与系统内部的 traceId 关联
     * 特点：由客户端生成并传递，在整个请求生命周期中保持不变
     */
    private String requestId;  // 客户端请求ID
    
    /**
     * 会话组ID（会话的唯一标识符）
     * 用于：
     * - 会话管理：标识属于同一会话的所有请求
     * - 数据隔离：不同会话的数据相互隔离
     * - 长期上下文：支持跨请求的会话级数据共享（通过 groupData）
     * 特点：在同一会话的所有请求中保持不变，用于维护会话级别的状态
     */
    private String groupId;  // 会话组ID
    
    /**
     * 父追踪ID（父节点的 traceId）
     * 用于：
     * - 建立调用链关系：通过 fromTraceId 建立父子节点的关联
     * - 向上回溯：可以从子节点追溯到父节点
     * - 调用链可视化：构建完整的调用链 DAG 结构
     * 在 cloneWith() 中，当前节点的 traceId 会成为子节点的 fromTraceId
     */
    private String fromTraceId = "";  // 父追踪ID
    
    /**
     * 根追踪ID列表（所有根节点的 traceId 集合）
     * 用于：
     * - 会话树管理：追踪会话中的所有根节点（最顶层的请求节点）
     * - 多根场景：支持一个会话中有多个并行的根请求
     * - 完整追踪：记录会话的完整执行树结构
     * 特点：在调用链中会复制给所有子节点，确保所有节点都能追溯到根节点
     */
    private List<String> rootTraceIds = new ArrayList<>();  // 根追踪ID列表
    
    public AgentRequest(String query, String traceId, String caller, String targetAgent) {
        this.query = query;
        this.traceId = traceId != null ? traceId : UUID.randomUUID().toString();
        this.caller = caller != null ? caller : "user";
        this.targetAgent = targetAgent;
        this.arguments = new HashMap<>();
        this.callStack = new ArrayList<>(Arrays.asList("user"));
        this.nodeIdStack = new ArrayList<>(Arrays.asList(""));
        this.requestId = UUID.randomUUID().toString();
        this.groupId = UUID.randomUUID().toString();
        this.nodeId = UUID.randomUUID().toString();
    }
    
    /**
     * 克隆请求对象（用于调用其他智能体）
     * 对应 Python 版本的 clone_with()
     */
    public AgentRequest cloneWith(String callee, Map<String, Object> newArguments) {
        AgentRequest cloned = new AgentRequest(
            this.query,
            UUID.randomUUID().toString(),  // 新的 traceId
            this.targetAgent,  // caller 更新为当前 callee
            callee
        );
        
        // 复制上下文数据（共享引用）
        cloned.sharedData = this.sharedData;
        cloned.groupData = this.groupData;
        
        // 复制调用栈
        cloned.callStack = new ArrayList<>(this.callStack);
        cloned.callStack.add(callee);
        
        cloned.nodeIdStack = new ArrayList<>(this.nodeIdStack);
        cloned.nodeIdStack.add(this.nodeId);
        
        // 更新节点关系
        cloned.fatherNodeId = this.nodeId;
        cloned.nodeId = UUID.randomUUID().toString();
        
        // 更新并行执行信息
        if (this.parallelId.isEmpty()) {
            cloned.parallelId = UUID.randomUUID().toString();
        } else {
            cloned.parallelId = this.parallelId;
        }
        
        cloned.parallelDict = this.parallelDict;
        cloned.preNodeIds = new ArrayList<>(this.latestNodeIds);
        
        // Python 版本保持一致：完全替换，不是合并
        // Python 版本：clone_with(arguments={"x": 1}) 会完全替换 arguments，不是合并
        // 如果传入了 newArguments，则完全替换；否则复制原有参数
        if (newArguments != null && !newArguments.isEmpty()) {
            // 完全替换：与 Python 版本的 setattr() 行为一致
            cloned.arguments = new HashMap<>(newArguments);
        } else {
            // 未传入新参数时，复制原有参数
            cloned.arguments = new HashMap<>(this.arguments);
        }
        
        // 复制其他字段
        cloned.callerCategory = this.calleeCategory;
        cloned.calleeCategory = "";
        cloned.framework = this.framework;
        cloned.groupId = this.groupId;
        cloned.fromTraceId = this.traceId;
        cloned.rootTraceIds = new ArrayList<>(this.rootTraceIds);
        
        return cloned;
    }
    
    /**
     * 调用其他智能体或工具
     * 对应 Python 版本的 call() 方法
     * 封装复杂逻辑：一行代码完成调用，避免手动处理多层细节
     * 自动维护调用链：正确维护 traceId、调用栈、节点关系等上下文
     * 安全机制：自动进行权限校验和超时控制
     * 统一接口：工具和智能体使用同一调用方式
     * 数据共享：自动传递 sharedData 和 groupData
     * 设计原则
     * 符合“单一职责”和“封装”原则：AgentRequest.call() 负责调用链管理，ReActAgent 只需关注 ReAct 循环逻辑，职责清晰。
     */

    public CompletableFuture<AgentResponse> call(String callee, Map<String, Object> arguments) {
        if (framework == null) {
            return CompletableFuture.completedFuture(
                new AgentResponse(
                    AgentState.FAILED,
                    "Framework not set in request",
                    null,
                    null
                )
            );
        }
        
        // 克隆请求
        AgentRequest calleeRequest = cloneWith(callee, arguments);
        
        // 检查智能体是否存在
        if (!framework.getAllAgents().contains(callee) && !framework.hasTool(callee)) {
            return CompletableFuture.completedFuture(
                new AgentResponse(
                    AgentState.FAILED,
                    "Agent or tool not found: " + callee,
                    null,
                    null
                )
            );
        }
        
        // 权限校验（对非用户的调用者进行权限检查）
        if (!"user".equals(callerCategory)) {
            try {
                Agent callerAgent = framework.getAgent(this.caller);
                if (callerAgent.isPermissionRequired()) {
                    //获取调用者允许调用的工具/智能体名称列表（白名单），不在白名单则拒绝调用
                    if (!callerAgent.getPermittedToolNameList().contains(callee)) {
                        return CompletableFuture.completedFuture(
                            new AgentResponse(
                                AgentState.SKIPPED,
                                "No permission for agent: " + callee,
                                null,
                                null
                            )
                        );
                    }
                }
            } catch (Exception e) {
                // 如果调用者不是智能体，跳过权限校验
            }
        }
        
        // 执行调用（支持超时和重试）
        try {
            Agent agent = framework.getAgent(callee);
            
            // 如果有超时设置，使用超时控制
            if (agent.getTimeout() > 0) {
                return agent.execute(calleeRequest)
                    .orTimeout(agent.getTimeout(), TimeUnit.SECONDS)
                    .exceptionally(throwable -> {
                        if (throwable.getCause() instanceof java.util.concurrent.TimeoutException) {
                            return new AgentResponse(
                                AgentState.FAILED,
                                "Agent execution timed out: " + callee,
                                null,
                                null
                            );
                        }
                        return new AgentResponse(
                            AgentState.FAILED,
                            "Agent execution failed: " + throwable.getMessage(),
                            null,
                            null
                        );
                    });
            } else {
                return agent.execute(calleeRequest);
            }
        } catch (IllegalArgumentException e) {
            // 可能是工具，尝试调用工具
            if (framework.hasTool(callee)) {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        Tool tool = framework.getTool(callee);
                        Map<String, Object> toolArgs = calleeRequest.getArguments();
                        if (arguments != null) {
                            toolArgs.putAll(arguments);
                        }
                        
                        AgentRequest toolRequest = new AgentRequest(
                            (String) toolArgs.getOrDefault("query", ""),
                            calleeRequest.getTraceId(),
                            calleeRequest.getCaller(),
                            callee
                        );
                        toolRequest.getArguments().putAll(toolArgs);
                        
                        // 工具返回的是 CompletableFuture<AgentResponse>，需要 join()
                        return tool.execute(toolRequest).join();
                    } catch (Exception ex) {
                        return new AgentResponse(
                            AgentState.FAILED,
                            "Tool execution failed: " + ex.getMessage(),
                            null,
                            null
                        );
                    }
                });
            }
            
            return CompletableFuture.completedFuture(
                new AgentResponse(
                    AgentState.FAILED,
                    "Agent or tool not found: " + callee,
                    null,
                    null
                )
            );
        }
    }
    
    // Getters and Setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
    public String getCaller() {
        return caller;
    }
    
    public void setCaller(String caller) {
        this.caller = caller;
    }
    
    public String getTargetAgent() {
        return targetAgent;
    }
    
    public void setTargetAgent(String targetAgent) {
        this.targetAgent = targetAgent;
    }
    
    public String getCallerCategory() {
        return callerCategory;
    }
    
    public void setCallerCategory(String callerCategory) {
        this.callerCategory = callerCategory;
    }
    
    public String getCalleeCategory() {
        return calleeCategory;
    }
    
    public void setCalleeCategory(String calleeCategory) {
        this.calleeCategory = calleeCategory;
    }
    
    public List<String> getCallStack() {
        return callStack;
    }
    
    public void setCallStack(List<String> callStack) {
        this.callStack = callStack;
    }
    
    public List<String> getNodeIdStack() {
        return nodeIdStack;
    }
    
    public void setNodeIdStack(List<String> nodeIdStack) {
        this.nodeIdStack = nodeIdStack;
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    
    public String getFatherNodeId() {
        return fatherNodeId;
    }
    
    public void setFatherNodeId(String fatherNodeId) {
        this.fatherNodeId = fatherNodeId;
    }
    
    public List<String> getPreNodeIds() {
        return preNodeIds;
    }
    
    public void setPreNodeIds(List<String> preNodeIds) {
        this.preNodeIds = preNodeIds;
    }
    
    public List<String> getLatestNodeIds() {
        return latestNodeIds;
    }
    
    public void setLatestNodeIds(List<String> latestNodeIds) {
        this.latestNodeIds = latestNodeIds;
    }
    
    public String getParallelId() {
        return parallelId;
    }
    
    public void setParallelId(String parallelId) {
        this.parallelId = parallelId;
    }
    
    public Map<String, Object> getParallelDict() {
        return parallelDict;
    }
    
    public void setParallelDict(Map<String, Object> parallelDict) {
        this.parallelDict = parallelDict;
    }
    
    public Map<String, Object> getArguments() {
        return arguments;
    }
    
    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
    
    public Map<String, Object> getSharedData() {
        return sharedData;
    }
    
    public void setSharedData(Map<String, Object> sharedData) {
        this.sharedData = sharedData;
    }
    
    public Map<String, Object> getGroupData() {
        return groupData;
    }
    
    public void setGroupData(Map<String, Object> groupData) {
        this.groupData = groupData;
    }
    
    public AgentFramework getFramework() {
        return framework;
    }
    
    public void setFramework(AgentFramework framework) {
        this.framework = framework;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    public String getFromTraceId() {
        return fromTraceId;
    }
    
    public void setFromTraceId(String fromTraceId) {
        this.fromTraceId = fromTraceId;
    }
    
    public List<String> getRootTraceIds() {
        return rootTraceIds;
    }
    
    public void setRootTraceIds(List<String> rootTraceIds) {
        this.rootTraceIds = rootTraceIds;
    }
}

