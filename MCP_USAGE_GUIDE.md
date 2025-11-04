# MCP (Model Context Protocol) 使用指南

## 📋 概述

本文档说明如何在 react-oxygent-java 中使用 MCP (Model Context Protocol) 工具。

---

## ✅ 已实现的功能

### 1. **MCP 客户端接口**
- `MCPClient` - MCP 客户端接口
- `StdioMCPClient` - 通过标准输入输出与 MCP 服务器通信

### 2. **MCP 工具代理**
- `MCPTool` - 代表从 MCP 服务器发现的工具

### 3. **框架集成**
- `AgentFramework` 支持工具注册
- `ReActAgent` 支持调用 MCP 工具

---

## 🚀 快速开始

### 1. 创建 MCP 客户端

```java
// 在 AgentService 中初始化 MCP 工具
private void initializeMCPTools() {
    // 配置 MCP 服务器参数
    Map<String, Object> params = new HashMap<>();
    params.put("command", "npx");
    params.put("args", Arrays.asList(
        "-y", 
        "@modelcontextprotocol/server-filesystem", 
        "./local_file"
    ));
    
    // 创建 MCP 客户端
    StdioMCPClient mcpClient = new StdioMCPClient(
        "file_tools",
        "文件系统 MCP 工具",
        params
    );
    
    // 初始化并发现工具
    mcpClient.initialize();
    
    // 注册发现的工具
    for (MCPClient.MCPToolInfo toolInfo : mcpClient.getTools()) {
        MCPTool mcpTool = new MCPTool(
            toolInfo.getName(),
            toolInfo.getDescription(),
            mcpClient,
            "file_tools"
        );
        framework.registerTool(toolInfo.getName(), mcpTool);
    }
}
```

### 2. 在智能体中使用 MCP 工具

```java
// 创建智能体时指定可用的工具
ReActAgent agent = new ReActAgent(
    "requirement_agent",
    "需求分析智能体",
    false,
    llmClient,
    null,  // 子智能体
    Arrays.asList("read_file", "list_directory"),  // ⭐ MCP 工具列表
    "你是需求分析专家。可以使用文件工具读取需求文档。",
    5
);
```

### 3. 在业务流程中调用 MCP 工具

```java
// 在 DevOpsOrchestrationService 中直接调用 MCP 工具
public AgentResponse executeWorkflowWithMCP(String requirementId) {
    // 直接调用 MCP 工具
    if (framework.hasTool("read_file")) {
        Map<String, Object> args = new HashMap<>();
        args.put("path", "./requirements/" + requirementId + ".md");
        
        AgentRequest request = new AgentRequest(
            "读取需求文档",
            null,
            "user",
            "read_file"
        );
        request.getArguments().putAll(args);
        
        AgentResponse response = framework.getTool("read_file")
            .execute(request)
            .join();
        
        // 使用读取的内容继续流程
        // ...
    }
}
```

---

## 📝 配置示例

### 文件系统工具

```java
Map<String, Object> params = new HashMap<>();
params.put("command", "npx");
params.put("args", Arrays.asList(
    "-y", 
    "@modelcontextprotocol/server-filesystem", 
    "./local_file"  // 工作目录
));

StdioMCPClient fileTools = new StdioMCPClient(
    "file_tools",
    "文件系统工具",
    params
);
```

### Python MCP 服务器

```java
Map<String, Object> params = new HashMap<>();
params.put("command", "uv");
params.put("args", Arrays.asList(
    "--directory", 
    "./mcp_servers", 
    "run", 
    "wiki_tools.py"
));

StdioMCPClient wikiTools = new StdioMCPClient(
    "wiki_tools",
    "Wiki 工具",
    params
);
```

---

## 🔧 在 DevOpsOrchestrationService 中使用

### 方法1: 智能体自动调用（推荐）

智能体在 Prompt 中声明可以使用 MCP 工具，LLM 会自动决定何时调用：

```java
// 在 AgentService 中配置智能体
ReActAgent gitAgent = new ReActAgent(
    "git_agent",
    "Git 提交智能体",
    false,
    llmClient,
    null,
    Arrays.asList("git_commit", "git_push"),  // MCP Git 工具
    "你是 Git 专家。可以使用 Git 工具提交代码。",
    5
);
```

### 方法2: 业务流程中直接调用

在业务流程中直接调用 MCP 工具：

```java
// 在 DevOpsOrchestrationService 中
public AgentResponse executeDevOpsWorkflowWithMCP(String requirementId, String environment) {
    // 1. 使用 MCP 工具读取需求
    if (framework.hasTool("read_file")) {
        AgentResponse fileResponse = framework.getTool("read_file")
            .execute(fileRequest)
            .join();
    }
    
    // 2. 执行 DevOps 流程
    AgentResponse response = framework.chatWithMaster(request).join();
    
    // 3. 使用 MCP 工具保存结果
    if (framework.hasTool("write_file")) {
        framework.getTool("write_file")
            .execute(saveRequest)
            .join();
    }
    
    return response;
}
```

---

## 📊 完整示例

### 示例：DevOps 流程中使用 MCP 工具

```java
@Service
public class DevOpsOrchestrationService {
    
    public AgentResponse executeDevOpsWorkflowWithMCP(String requirementId, String environment) {
        System.out.println("\n📋 执行 DevOps 流程（使用 MCP 工具）...\n");
        
        try {
            // 步骤1: 使用 MCP 文件工具读取需求文档
            if (framework.hasTool("read_file")) {
                System.out.println("📂 步骤1: 读取需求文档");
                
                Map<String, Object> fileArgs = new HashMap<>();
                fileArgs.put("path", "./requirements/" + requirementId + ".md");
                
                AgentRequest fileRequest = new AgentRequest(
                    "读取需求文档",
                    null,
                    "user",
                    "read_file"
                );
                fileRequest.getArguments().putAll(fileArgs);
                
                AgentResponse fileResponse = framework.getTool("read_file")
                    .execute(fileRequest)
                    .join();
                
                System.out.println("✅ 需求文档读取完成\n");
            }
            
            // 步骤2: 执行完整的 DevOps 流程
            String taskDescription = buildTaskDescription(requirementId, environment);
            AgentRequest request = new AgentRequest(
                taskDescription,
                null,
                "user",
                "devops_master"
            );
            
            AgentResponse response = framework.chatWithMaster(request).join();
            
            // 步骤3: 使用 MCP 工具保存结果
            if (framework.hasTool("write_file")) {
                System.out.println("\n💾 保存流程报告...");
                
                Map<String, Object> saveArgs = new HashMap<>();
                saveArgs.put("path", "./output/devops_report_" + requirementId + ".txt");
                saveArgs.put("content", response.getOutput());
                
                AgentRequest saveRequest = new AgentRequest(
                    "保存流程报告",
                    null,
                    "user",
                    "write_file"
                );
                saveRequest.getArguments().putAll(saveArgs);
                
                framework.getTool("write_file")
                    .execute(saveRequest)
                    .join();
                
                System.out.println("✅ 流程报告已保存\n");
            }
            
            return response;
            
        } catch (Exception e) {
            System.err.println("❌ MCP 工具调用失败: " + e.getMessage());
            // 回退到普通流程
            return executeDevOpsWorkflow(requirementId, environment);
        }
    }
}
```

---

## ⚠️ 注意事项

### 1. 环境要求

- **Node.js**: 大部分 MCP 服务器需要 Node.js 环境
- **MCP 服务器**: 需要安装对应的 MCP 服务器包
- **Python**: 如果使用 Python MCP 服务器，需要 Python 环境

### 2. 错误处理

- MCP 工具初始化可能失败（缺少环境）
- 建议添加 try-catch 处理
- 提供回退机制（不使用 MCP 工具）

### 3. 协议实现

当前实现是简化版本，完整的 MCP 协议需要：
- JSON-RPC 2.0 协议支持
- 完整的握手流程
- 错误处理和重试机制

---

## 🎯 与 Python 版本对比

| 特性 | Python 版本 | Java 版本 | 状态 |
|------|------------|----------|------|
| **StdioMCPClient** | ✅ | ✅ | ✅ 一致 |
| **工具发现** | ✅ | ✅ | ✅ 一致 |
| **工具调用** | ✅ | ✅ | ✅ 一致 |
| **框架集成** | ✅ | ✅ | ✅ 一致 |

---

## 📝 总结

Java 版本现在支持：

1. ✅ **MCP 客户端**: `StdioMCPClient` 通过 stdio 与 MCP 服务器通信
2. ✅ **工具发现**: 自动发现 MCP 服务器提供的工具
3. ✅ **工具注册**: 工具注册到 `AgentFramework`
4. ✅ **智能体调用**: `ReActAgent` 可以调用 MCP 工具
5. ✅ **业务流程集成**: `DevOpsOrchestrationService` 中可以直接调用 MCP 工具

这使得 Java 版本可以使用 MCP 工具生态系统，扩展智能体的能力。

