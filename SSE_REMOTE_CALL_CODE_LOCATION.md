# Python 版本 SSE 远程调用代码位置

本文档详细说明 OxyGent (Python版本) 中支持远程调用（SSE 协议）的具体代码位置。

---

## 📍 核心代码文件位置

### 1. **客户端：SSE 远程智能体代理**

**文件路径**: `oxygent/oxy/agents/sse_oxy_agent.py`

这是实现远程调用的核心类，负责通过 SSE 协议调用远程 MAS 服务。

```python
# oxygent/oxy/agents/sse_oxy_agent.py

class SSEOxyGent(RemoteAgent):
    """通过 SSE (Server-Sent Events) 协议与远程 MAS 通信的智能体"""
    
    async def _execute(self, oxy_request: OxyRequest) -> OxyResponse:
        # 1. 构建请求负载
        payload = oxy_request.model_dump(
            exclude={"mas", "parallel_id", "latest_node_ids"}
        )
        
        # 2. 设置 SSE 请求头
        headers = {
            "Accept": "text/event-stream",
            "Content-Type": "application/json",
        }
        
        # 3. 建立 SSE 连接并发送请求
        url = build_url(self.server_url, "/sse/chat")  # ⭐ 关键：/sse/chat 端点
        async with aiohttp.ClientSession() as session:
            async with session.post(
                url, data=json.dumps(payload), headers=headers
            ) as resp:
                # 4. 流式接收 SSE 消息
                async for line in resp.content:
                    if line:
                        decoded_line = line.decode("utf-8").strip()
                        if decoded_line.startswith("data: "):
                            data = decoded_line[6:]
                            if data == "done":
                                break
                            data = json.loads(data)
                            
                            # 5. 处理不同类型的消息
                            if data["type"] == "answer":
                                answer = data.get("content")
                            elif data["type"] in ["tool_call", "observation"]:
                                # 转发消息到本地 MAS
                                await oxy_request.send_message(data)
        
        # 6. 返回最终答案
        return OxyResponse(state=OxyState.COMPLETED, output=answer)
```

**关键代码行**:
- **第 49 行**: `url = build_url(self.server_url, "/sse/chat")` - 构建 SSE 端点 URL
- **第 52-58 行**: 使用 `aiohttp` 建立 SSE 连接
- **第 60-94 行**: 流式接收和处理 SSE 消息

---

### 2. **服务端：SSE 端点实现**

**文件路径**: `oxygent/mas.py`

**服务端 SSE 端点** (第 993-1013 行):

```python
# oxygent/mas.py (第 993-1013 行)

@app.api_route("/sse/chat", methods=["GET", "POST"])
async def sse_chat(request: Request):
    """SSE 聊天端点，接收远程智能体调用请求"""
    payload = await request_to_payload(request)
    
    # 应用请求拦截器（如果配置）
    intercepted_response = self.func_interceptor(payload)
    if intercepted_response is not None:
        return intercepted_response
    
    current_trace_id = payload["current_trace_id"]
    
    logger.info(
        "SSE connection established.",
        extra={"trace_id": current_trace_id},
    )
    
    # 创建 Redis 消息队列的 key
    redis_key = f"{self.message_prefix}:{self.name}:{current_trace_id}"
    
    # 创建异步任务执行智能体
    task = asyncio.create_task(
        self.chat_with_agent(payload=payload, send_msg_key=redis_key)
    )
    
    # 返回 SSE 流式响应
    return EventSourceResponse(
        self.event_stream(redis_key, current_trace_id, task)
    )
```

**SSE 事件流处理** (第 784-829 行):

```python
# oxygent/mas.py (第 784-829 行)

async def event_stream(self, redis_key, current_trace_id, task):
    """SSE 事件流处理函数，从 Redis 队列读取消息并推送"""
    try:
        task.add_done_callback(
            lambda future: self.active_tasks.pop(current_trace_id, None)
        )
        self.active_tasks[current_trace_id] = task
        
        while True:
            # 从 Redis 队列读取消息
            bytes_msg = await self.redis_client.rpop(redis_key)
            if bytes_msg is None:
                await asyncio.sleep(0.1)
                continue
            
            # 解码消息
            message = msgpack.unpackb(bytes_msg)
            
            if message:
                if isinstance(message, dict):
                    # 检查是否是终止事件
                    if "event" in message:
                        yield message
                        logger.info(
                            "SSE connection terminated.",
                            extra={"trace_id": current_trace_id},
                        )
                        break
                    
                    # 处理特殊消息格式
                    # ... (消息格式转换逻辑)
                    
                # 发送 SSE 消息（格式：data: {JSON}）
                yield {"data": to_json(message)}
                
    except asyncio.CancelledError:
        logger.info(
            "SSE connection terminated.",
            extra={"trace_id": current_trace_id},
        )
        self.active_tasks[current_trace_id].cancel()
```

**关键代码行**:
- **第 993 行**: `@app.api_route("/sse/chat", methods=["GET", "POST"])` - SSE 端点定义
- **第 1008 行**: `asyncio.create_task(...)` - 创建异步任务执行智能体
- **第 1011-1012 行**: `EventSourceResponse(...)` - 返回 SSE 流式响应
- **第 791 行**: `await self.redis_client.rpop(redis_key)` - 从 Redis 读取消息
- **第 823 行**: `yield {"data": to_json(message)}` - 推送 SSE 消息

---

### 3. **基类：RemoteAgent**

**文件路径**: `oxygent/oxy/agents/remote_agent.py`

```python
# oxygent/oxy/agents/remote_agent.py

class RemoteAgent(BaseAgent):
    """远程智能体的基类，提供与远程系统通信的基础功能"""
    
    server_url: AnyUrl = Field()  # 远程服务器 URL
    org: dict = Field(default_factory=dict)  # 远程组织架构
    
    @field_validator("server_url")
    def check_protocol(cls, v):
        if v.scheme not in ("http", "https"):
            raise ValueError("server_url must start with http:// or https://")
        return v
    
    async def init(self):
        """初始化时获取远程 MAS 的组织架构"""
        await super().init()
        async with httpx.AsyncClient() as client:
            response = await client.get(
                build_url(self.server_url, "/get_organization")
            )
            self.org = response.json()["data"]["organization"]
```

**关键功能**:
- **服务发现**: 通过 `/get_organization` 端点获取远程 MAS 的组织架构
- **URL 验证**: 确保使用 http/https 协议

---

## 🔄 完整的调用流程

### 客户端调用流程

```
1. 本地智能体调用远程智能体
   ↓
2. MAS 查找注册表，找到 SSEOxyGent 实例
   ↓
3. SSEOxyGent._execute() 被调用
   ↓
4. 构建请求负载（OxyRequest -> JSON）
   ↓
5. 发送 POST 请求到 http://remote-server:port/sse/chat
   Headers: Accept: text/event-stream
   ↓
6. 建立 SSE 连接，流式接收消息
   ↓
7. 处理消息：
   - type == "answer": 提取最终答案
   - type == "tool_call": 转发到本地 MAS
   - type == "observation": 转发到本地 MAS
   ↓
8. 收到 "done" 消息，关闭连接
   ↓
9. 返回 OxyResponse
```

### 服务端处理流程

```
1. 接收 POST /sse/chat 请求
   ↓
2. 解析请求负载（JSON -> OxyRequest）
   ↓
3. 创建 Redis 消息队列 key
   ↓
4. 创建异步任务执行 chat_with_agent()
   ↓
5. 返回 EventSourceResponse (SSE 流)
   ↓
6. event_stream() 函数：
   - 从 Redis 队列读取消息
   - 格式化为 SSE 格式：data: {JSON}
   - 推送消息到客户端
   ↓
7. 智能体执行过程中通过 send_message() 发送消息到 Redis
   ↓
8. 执行完成后发送 {"event": "done"} 终止连接
```

---

## 📝 使用示例

### 示例 1: 基本使用

```python
# examples/distributed/app_master_agent.py

oxy_space = [
    oxy.ReActAgent(
        name="master_agent",
        sub_agents=["file_agent", "math_agent"],  # ⭐ math_agent 是远程的
        is_master=True,
        llm_model="default_name",
    ),
    oxy.ReActAgent(
        name="file_agent",
        desc="本地文件查询智能体",
        tools=["file_tools"],
        llm_model="default_name",
    ),
    # ⭐ 远程智能体：通过 SSE 调用运行在 8081 端口的远程 MAS
    oxy.SSEOxyGent(
        name="math_agent",
        desc="远程数学计算智能体",
        server_url="http://127.0.0.1:8081",  # 远程服务器地址
        is_share_call_stack=False,  # 是否共享调用栈
    ),
]
```

### 示例 2: 分布式 DevOps 场景

```python
# examples/distributed/app_master_agent.py

oxy_space = [
    oxy.ReActAgent(
        name="devops_master",
        sub_agents=[
            "requirement_agent",  # 远程
            "code_agent",         # 远程
            "review_agent",       # 远程
            "test_agent",         # 远程
            "git_agent",          # 远程
            "deploy_agent",       # 远程
        ],
        is_master=True,
        llm_model="default_name",
    ),
    # 远程智能体列表
    oxy.SSEOxyGent(
        name="requirement_agent",
        server_url="http://127.0.0.1:8101",  # 需求分析服务
    ),
    oxy.SSEOxyGent(
        name="code_agent",
        server_url="http://127.0.0.1:8102",   # 代码编写服务
    ),
    # ... 其他远程智能体
]
```

---

## 🔍 关键代码位置总结

| 功能 | 文件路径 | 关键行号 | 说明 |
|------|---------|---------|------|
| **客户端 SSE 连接** | `oxygent/oxy/agents/sse_oxy_agent.py` | 27-95 | `_execute()` 方法实现 SSE 客户端 |
| **服务端 SSE 端点** | `oxygent/mas.py` | 993-1013 | `/sse/chat` 端点定义 |
| **SSE 事件流处理** | `oxygent/mas.py` | 784-829 | `event_stream()` 方法处理 SSE 流 |
| **远程智能体基类** | `oxygent/oxy/agents/remote_agent.py` | 9-43 | `RemoteAgent` 基类 |
| **服务发现** | `oxygent/oxy/agents/sse_oxy_agent.py` | 20-25 | `init()` 方法获取远程组织架构 |

---

## 💡 技术要点

### 1. **SSE 协议格式**

- **请求**: `POST /sse/chat` with `Accept: text/event-stream`
- **响应**: 流式文本，每行格式为 `data: {JSON}` 或 `event: done`

### 2. **消息类型**

- `tool_call`: 工具调用消息
- `observation`: 观察结果消息
- `answer`: 最终答案消息
- `done`: 连接终止消息

### 3. **Redis 消息队列**

- 使用 Redis 作为消息中间件
- Key 格式: `{message_prefix}:{app_name}:{trace_id}`
- 智能体通过 `send_message()` 发送消息到队列
- `event_stream()` 从队列读取并推送

### 4. **调用栈管理**

- `is_share_call_stack=True`: 共享调用栈，保持完整调用链
- `is_share_call_stack=False`: 清空调用栈，远程调用视为独立请求

---

## 🎯 总结

Python 版本的远程调用（SSE 协议）实现主要分布在：

1. **客户端**: `sse_oxy_agent.py` - 通过 `aiohttp` 建立 SSE 连接
2. **服务端**: `mas.py` - 提供 `/sse/chat` 端点和事件流处理
3. **消息传递**: 通过 Redis 队列实现异步消息传递

这种设计实现了**分布式智能体系统**，允许不同服务上的智能体通过 SSE 协议进行协作。

