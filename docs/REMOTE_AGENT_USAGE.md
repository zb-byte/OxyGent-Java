# 远程智能体使用指南

本文档说明如何在 react-oxygent-java 中使用远程智能体（SSE 协议）。

---

## 📋 概述

`SSEOxyGent` 类实现了通过 SSE (Server-Sent Events) 协议调用远程智能体的功能，类似于 Python 版本的 `SSEOxyGent`。

---

## 🚀 快速开始

### 1. 创建远程智能体

```java
import framework.agent.SSEOxyGent;

// 创建远程智能体代理
SSEOxyGent mathAgent = new SSEOxyGent(
    "math_agent",                    // 智能体名称
    "远程数学计算智能体",              // 描述
    "http://127.0.0.1:8081"          // 远程服务器地址
);

// 或者使用完整构造函数
SSEOxyGent mathAgent = new SSEOxyGent(
    "math_agent",
    "远程数学计算智能体",
    false,                           // 是否为主控智能体
    "http://127.0.0.1:8081",
    false                            // 是否共享调用栈
);
```

### 2. 注册到框架

```java
import framework.agent.AgentFramework;

AgentFramework framework = new AgentFramework();

// 注册远程智能体（与本地智能体注册方式相同）
framework.registerAgent("math_agent", mathAgent);
```

### 3. 在主控智能体中使用

```java
import framework.agent.ReActAgent;

// 创建主控智能体，包含远程智能体
ReActAgent masterAgent = new ReActAgent(
    "master_agent",
    "主控智能体",
    true,
    llmClient,
    Arrays.asList("file_agent", "math_agent"),  // ⭐ 包含远程智能体名称
    null,
    "你可以调用 file_agent 和 math_agent 完成任务",
    10
);

// 注册主控智能体
framework.registerAgent("master_agent", masterAgent);
```

---

## 📝 完整示例

### 示例 1: 基本使用

```java
package business.devops.service;

import framework.agent.AgentFramework;
import framework.agent.ReActAgent;
import framework.agent.SSEOxyGent;
import framework.llm.LLMClient;

public class RemoteAgentExample {
    
    public AgentFramework setupFramework(LLMClient llmClient) {
        AgentFramework framework = new AgentFramework();
        
        // 1. 创建本地智能体
        ReActAgent fileAgent = new ReActAgent(
            "file_agent",
            "本地文件智能体",
            false,
            llmClient,
            null,
            null,
            "你是文件操作专家",
            5
        );
        
        // 2. 创建远程智能体
        SSEOxyGent mathAgent = new SSEOxyGent(
            "math_agent",
            "远程数学计算智能体",
            "http://127.0.0.1:8081"
        );
        
        // 3. 创建主控智能体
        ReActAgent masterAgent = new ReActAgent(
            "master_agent",
            "主控智能体",
            true,
            llmClient,
            Arrays.asList("file_agent", "math_agent"),
            null,
            "协调 file_agent 和 math_agent 完成任务",
            10
        );
        
        // 4. 注册所有智能体
        framework.registerAgent("file_agent", fileAgent);
        framework.registerAgent("math_agent", mathAgent);
        framework.registerAgent("master_agent", masterAgent);
        
        return framework;
    }
}
```

### 示例 2: 分布式 DevOps 场景

```java
public class DistributedDevOpsExample {
    
    public AgentFramework setupDistributedFramework(LLMClient llmClient) {
        AgentFramework framework = new AgentFramework();
        
        // 远程智能体列表（运行在不同端口的服务上）
        SSEOxyGent requirementAgent = new SSEOxyGent(
            "requirement_agent",
            "远程需求分析智能体",
            "http://127.0.0.1:8101"
        );
        
        SSEOxyGent codeAgent = new SSEOxyGent(
            "code_agent",
            "远程代码编写智能体",
            "http://127.0.0.1:8102"
        );
        
        SSEOxyGent reviewAgent = new SSEOxyGent(
            "review_agent",
            "远程代码审查智能体",
            "http://127.0.0.1:8103"
        );
        
        // 创建主控智能体
        ReActAgent masterAgent = new ReActAgent(
            "devops_master",
            "DevOps主控智能体",
            true,
            llmClient,
            Arrays.asList(
                "requirement_agent",
                "code_agent",
                "review_agent"
            ),
            null,
            "协调 DevOps 流程",
            16
        );
        
        // 注册所有智能体
        framework.registerAgent("requirement_agent", requirementAgent);
        framework.registerAgent("code_agent", codeAgent);
        framework.registerAgent("review_agent", reviewAgent);
        framework.registerAgent("devops_master", masterAgent);
        
        return framework;
    }
}
```

---

## 🔧 配置选项

### 构造函数参数

| 参数 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| `name` | String | 智能体名称（必须唯一） | - |
| `description` | String | 智能体描述 | - |
| `isMaster` | boolean | 是否为主控智能体 | false |
| `serverUrl` | String | 远程服务器地址（必须 http:// 或 https:// 开头） | - |
| `isShareCallStack` | boolean | 是否共享调用栈 | false |

### 简化构造函数

```java
// 使用简化构造函数（默认 isMaster=false, isShareCallStack=false）
SSEOxyGent agent = new SSEOxyGent(
    "agent_name",
    "描述",
    "http://127.0.0.1:8081"
);
```

---

## 🌐 远程服务器要求

远程服务器必须实现以下端点：

### 1. SSE 聊天端点

**端点**: `POST /sse/chat`

**请求头**:
```
Accept: text/event-stream
Content-Type: application/json
```

**请求体** (JSON):
```json
{
  "query": "用户查询",
  "trace_id": "trace_001",
  "caller": "user",
  "callee": "agent_name",
  "caller_category": "user",
  "callee_category": "agent"
}
```

**响应格式** (SSE 流):
```
data: {"type": "tool_call", "content": {...}}
data: {"type": "observation", "content": {...}}
data: {"type": "answer", "content": "最终答案"}
data: done
```

### 2. 服务发现端点（可选）

**端点**: `GET /get_organization`

**响应** (JSON):
```json
{
  "data": {
    "organization": {
      "children": [...]
    }
  }
}
```

---

## 🔄 调用流程

```
1. 本地智能体调用远程智能体
   ↓
2. AgentFramework 查找注册表，找到 SSEOxyGent 实例
   ↓
3. SSEOxyGent.execute() 被调用
   ↓
4. 构建请求负载（AgentRequest -> JSON）
   ↓
5. 发送 POST 请求到 http://remote-server:port/sse/chat
   Headers: Accept: text/event-stream
   ↓
6. 建立 HTTP 连接，流式接收 SSE 消息
   ↓
7. 解析消息：
   - type == "answer": 提取最终答案
   - type == "tool_call": 记录日志
   - type == "observation": 记录日志
   ↓
8. 收到 "done" 消息，关闭连接
   ↓
9. 返回 AgentResponse
```

---

## ⚠️ 注意事项

### 1. 服务器地址格式

- ✅ 正确: `http://127.0.0.1:8081`
- ✅ 正确: `https://api.example.com`
- ❌ 错误: `127.0.0.1:8081` (缺少协议)
- ❌ 错误: `ftp://example.com` (不支持非 HTTP 协议)

### 2. 网络连接

- 确保远程服务器可访问
- 检查防火墙设置
- 确保远程服务器实现了 `/sse/chat` 端点

### 3. 错误处理

远程调用失败时会返回包含错误信息的 `AgentResponse`:

```java
AgentResponse response = agent.execute(request).join();
if (!response.isSuccess()) {
    System.err.println("调用失败: " + response.getOutput());
}
```

### 4. 调用栈管理

- `isShareCallStack=true`: 共享调用栈，保持完整调用链
- `isShareCallStack=false`: 清空调用栈，远程调用视为独立请求

---

## 📊 与 Python 版本对比

| 特性 | Python 版本 | Java 版本 |
|------|------------|----------|
| **类名** | `SSEOxyGent` | `SSEOxyGent` |
| **协议** | SSE (Server-Sent Events) | SSE (Server-Sent Events) |
| **HTTP 客户端** | `aiohttp` | `HttpURLConnection` |
| **端点** | `/sse/chat` | `/sse/chat` |
| **消息格式** | JSON | JSON |
| **异步支持** | `async/await` | `CompletableFuture` |

---

## 🎯 总结

通过 `SSEOxyGent` 类，Java 版本现在支持：

1. ✅ **远程智能体调用**: 通过 SSE 协议调用远程服务上的智能体
2. ✅ **统一接口**: 与本地智能体使用相同的接口和注册方式
3. ✅ **自动路由**: AgentFramework 自动路由到本地或远程智能体
4. ✅ **流式响应**: 支持 SSE 流式消息接收

这使得 Java 版本可以实现分布式智能体系统，不同服务上的智能体可以协作完成任务。

