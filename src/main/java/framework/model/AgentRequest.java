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
    // 基础字段
    private String query;
    private String traceId;
    private String caller;
    private String targetAgent;  // 对应 callee
    private String callerCategory = "user";
    private String calleeCategory = "";
    
    // 调用栈追踪
    private List<String> callStack = new ArrayList<>();  // 调用栈路径
    private List<String> nodeIdStack = new ArrayList<>();  // 节点ID栈
    private String nodeId = "";  // 当前节点ID
    private String fatherNodeId = "";  // 父节点ID
    private List<String> preNodeIds = new ArrayList<>();  // 前置节点ID列表
    private List<String> latestNodeIds = new ArrayList<>();  // 最新节点ID列表
    
    // 并行执行追踪
    private String parallelId = "";  // 并行执行ID
    private Map<String, Object> parallelDict = new HashMap<>();  // 并行执行字典
    
    // 上下文数据
    private Map<String, Object> arguments = new HashMap<>();  // 节点级数据
    private Map<String, Object> sharedData = new HashMap<>();  // 请求级共享数据（同trace内共享）
    private Map<String, Object> groupData = new HashMap<>();  // 会话级共享数据（同group内共享）
    
    // 框架引用（用于调用其他智能体）
    private AgentFramework framework;
    
    // 会话标识
    private String requestId;  // 客户端请求ID
    private String groupId;  // 会话组ID
    private String fromTraceId = "";  // 父追踪ID
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
        
        // 更新参数
        cloned.arguments = new HashMap<>(this.arguments);
        if (newArguments != null) {
            cloned.arguments.putAll(newArguments);
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
        
        // 权限校验（如果调用者不是用户）
        if (!"user".equals(callerCategory)) {
            try {
                Agent callerAgent = framework.getAgent(this.caller);
                if (callerAgent.isPermissionRequired()) {
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

