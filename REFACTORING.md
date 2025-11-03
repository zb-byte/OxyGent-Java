# 项目重构说明

## 重构目标

将 `Application.java` 拆分为：
1. **启动类** - 只负责 Spring Boot 启动
2. **业务服务类** - 负责具体的业务逻辑

## 新的项目结构

```
src/main/java/
├── config/                    # 配置类
│   └── LLMConfig.java        # LLM配置管理
├── service/                   # 业务服务层
│   ├── LLMClientService.java      # LLM客户端服务
│   ├── AgentService.java          # 智能体管理服务
│   └── DevOpsOrchestrationService.java  # DevOps流程编排服务
├── demo/                      # 演示类
│   └── Application.java      # Spring Boot 启动类（已简化）
├── core/                      # 核心框架
├── llm/                       # LLM客户端
├── memory/                    # 内存管理
└── model/                     # 数据模型
```

## 类职责说明

### 1. Application.java（启动类）
**职责：**
- 启动 Spring Boot 应用
- 注入业务服务
- 执行默认示例流程

**特点：**
- 代码简洁（约40行）
- 只负责启动和调用业务服务
- 不包含业务逻辑

### 2. LLMConfig.java（配置类）
**职责：**
- 管理 LLM 相关配置
- 从环境变量或 application.properties 读取配置
- 支持多种 LLM 提供者（DeepSeek、Ollama、OpenAI）

**配置来源优先级：**
1. 环境变量（`DEFAULT_LLM_*`）
2. `application.properties`
3. 默认值

### 3. LLMClientService.java（LLM客户端服务）
**职责：**
- 创建和初始化 LLM 客户端
- 根据配置选择不同的 LLM 提供者
- 提供统一的 LLM 客户端接口

**支持的 LLM：**
- DeepSeek（默认）
- Ollama（本地）
- OpenAI
- SimpleLLMClient（模拟，用于测试）

### 4. AgentService.java（智能体服务）
**职责：**
- 创建所有子智能体
- 创建主控智能体
- 注册智能体到框架
- 管理 AgentFramework 实例

**创建的智能体：**
- `requirement_agent` - 需求分析
- `code_agent` - 代码编写
- `review_agent` - 代码审查
- `test_agent` - 测试
- `git_agent` - Git提交
- `deploy_agent` - 部署
- `devops_master` - 主控智能体

### 5. DevOpsOrchestrationService.java（编排服务）
**职责：**
- 编排和执行 DevOps 流程
- 提供业务流程 API
- 处理任务结果

**主要方法：**
- `executeDevOpsWorkflow()` - 执行完整流程
- `executeCustomTask()` - 执行自定义任务
- `printResult()` - 打印结果

## 依赖关系

```
Application
    └── DevOpsOrchestrationService
            └── AgentService
                    ├── LLMClientService
                    │       └── LLMConfig
                    └── AgentFramework
```

## 配置示例

### 环境变量方式（推荐）
```bash
export DEFAULT_LLM_API_KEY="your-api-key"
export DEFAULT_LLM_BASE_URL="https://..."
export DEFAULT_LLM_MODEL_NAME="deepseek-r1-250528"
export DEFAULT_LLM_PROVIDER="deepseek"
```

### application.properties 方式
```properties
llm.api-key=your-api-key
llm.base-url=https://...
llm.model-name=deepseek-r1-250528
llm.provider=deepseek
```

## 优势

1. **职责分离**：启动类与业务逻辑分离
2. **易于测试**：服务类可以独立测试
3. **易于扩展**：添加新功能只需新增服务类
4. **配置灵活**：支持多种配置方式
5. **符合 Spring Boot 最佳实践**：使用依赖注入、服务层架构

## 使用示例

### 1. 执行默认流程
直接运行 `Application.java` 即可执行默认的 DevOps 流程。

### 2. 自定义任务
可以通过 `DevOpsOrchestrationService` 执行自定义任务：

```java
@Autowired
private DevOpsOrchestrationService orchestrationService;

public void runCustomTask() {
    String task = "请帮我分析需求并编写代码";
    var response = orchestrationService.executeCustomTask(task);
    orchestrationService.printResult(response);
}
```

### 3. 指定需求和环境
```java
var response = orchestrationService.executeDevOpsWorkflow(
    "req-002",    // 需求ID
    "production"  // 部署环境
);
```

## 后续扩展建议

1. **Controller 层**：可以添加 REST API 控制器
2. **Repository 层**：如果需要持久化，可以添加数据访问层
3. **DTO 层**：添加数据传输对象
4. **异常处理**：添加全局异常处理器
5. **日志管理**：使用 SLF4J/Logback 替代 System.out.println

