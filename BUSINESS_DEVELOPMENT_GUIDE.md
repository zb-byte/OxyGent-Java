# 业务开发指南

## 📁 代码结构说明

本项目代码分为两部分：

### 1. 框架代码（Framework） - 可复用基础设施

框架代码位于 `src/main/java/framework/` 目录，包含可复用的核心组件：

```
framework/
├── agent/          # 智能体框架
│   ├── Agent.java           # 智能体接口
│   ├── AgentFramework.java  # 智能体框架（注册、路由）
│   └── ReActAgent.java      # ReAct智能体实现
├── llm/            # LLM客户端
│   ├── LLMClient.java       # LLM客户端接口
│   ├── DeepSeekLLMClient.java
│   ├── OllamaLLMClient.java
│   └── OpenAILLMClient.java
├── memory/         # 内存管理
│   ├── ReactMemory.java     # ReAct内存管理
│   └── Observation.java     # 观察结果
└── model/          # 数据模型
    ├── AgentRequest.java
    ├── AgentResponse.java
    └── ToolCall.java
```

**框架代码特点：**
- ✅ 不包含业务逻辑
- ✅ 可被多个业务复用
- ✅ 提供稳定的API接口
- ⚠️ **不要修改框架代码**（除非是框架本身的改进）

### 2. 业务代码（Business） - 具体业务实现

业务代码位于 `src/main/java/business/` 目录，每个业务模块一个子目录：

```
business/
└── devops/         # DevOps业务示例
    ├── config/     # 业务配置
    ├── service/    # 业务服务
    └── Application.java  # 业务启动类
```

**业务代码特点：**
- ✅ 包含具体的业务逻辑
- ✅ 使用框架提供的API
- ✅ 每个业务模块独立
- ✅ 可以创建多个业务模块

---

## 🚀 如何开发新业务

### 步骤1：创建业务目录结构

```bash
mkdir -p src/main/java/business/yourbusiness/{config,service}
```

例如，创建一个"客服机器人"业务：

```bash
mkdir -p src/main/java/business/customer-service/{config,service}
```

### 步骤2：创建业务配置类

创建 `business/customer-service/config/ServiceConfig.java`:

```java
package business.customer_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "customer-service")
public class ServiceConfig {
    // 业务相关配置
}
```

### 步骤3：创建业务服务类

#### 3.1 创建LLM客户端服务

创建 `business/customer-service/service/CustomerLLMService.java`:

```java
package business.customer_service.service;

import framework.llm.LLMClient;
import framework.llm.DeepSeekLLMClient;
import org.springframework.stereotype.Service;

@Service
public class CustomerLLMService {
    private final LLMClient llmClient;
    
    public CustomerLLMService() {
        // 初始化LLM客户端
        this.llmClient = new DeepSeekLLMClient(
            System.getenv("DEFAULT_LLM_API_KEY"),
            System.getenv("DEFAULT_LLM_MODEL_NAME"),
            System.getenv("DEFAULT_LLM_BASE_URL")
        );
    }
    
    public LLMClient getLLMClient() {
        return llmClient;
    }
}
```

#### 3.2 创建智能体服务

创建 `business/customer-service/service/CustomerAgentService.java`:

```java
package business.customer_service.service;

import framework.agent.AgentFramework;
import framework.agent.ReActAgent;
import framework.llm.LLMClient;
import org.springframework.stereotype.Service;

@Service
public class CustomerAgentService {
    private final CustomerLLMService llmService;
    private final AgentFramework framework;
    
    public CustomerAgentService(CustomerLLMService llmService) {
        this.llmService = llmService;
        this.framework = new AgentFramework();
        initializeAgents();
    }
    
    private void initializeAgents() {
        LLMClient llmClient = llmService.getLLMClient();
        
        // 创建智能体（使用框架的 ReActAgent）
        ReActAgent inquiryAgent = new ReActAgent(
            "inquiry_agent",
            "咨询智能体",
            false,
            llmClient,
            null,
            null,
            "你是客服咨询专家。回答用户的问题。",
            5
        );
        
        ReActAgent complaintAgent = new ReActAgent(
            "complaint_agent",
            "投诉处理智能体",
            false,
            llmClient,
            null,
            null,
            "你是投诉处理专家。处理用户投诉。",
            5
        );
        
        ReActAgent masterAgent = new ReActAgent(
            "customer_master",
            "客服主控智能体",
            true,
            llmClient,
            Arrays.asList("inquiry_agent", "complaint_agent"),
            null,
            "你是客服流程编排专家。根据用户问题类型，路由到相应的处理智能体。",
            10
        );
        
        // 注册智能体（使用框架的方法）
        framework.registerAgent("inquiry_agent", inquiryAgent);
        framework.registerAgent("complaint_agent", complaintAgent);
        framework.registerAgent("customer_master", masterAgent);
    }
    
    public AgentFramework getFramework() {
        return framework;
    }
}
```

#### 3.3 创建业务流程编排服务

创建 `business/customer-service/service/CustomerOrchestrationService.java`:

```java
package business.customer_service.service;

import framework.agent.AgentFramework;
import framework.model.AgentRequest;
import framework.model.AgentResponse;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class CustomerOrchestrationService {
    private final AgentFramework framework;
    
    public CustomerOrchestrationService(CustomerAgentService agentService) {
        this.framework = agentService.getFramework();
    }
    
    public AgentResponse handleCustomerQuery(String query) {
        AgentRequest request = new AgentRequest(
            query,
            null,
            "user",
            "customer_master"
        );
        
        CompletableFuture<AgentResponse> future = framework.chatWithMaster(request);
        return future.join();
    }
}
```

### 步骤4：创建业务启动类

创建 `business/customer-service/Application.java`:

```java
package business.customer_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import business.customer_service.service.CustomerOrchestrationService;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootApplication(scanBasePackages = {"business.customer_service"})
public class Application implements CommandLineRunner {
    
    @Autowired
    private CustomerOrchestrationService orchestrationService;
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    @Override
    public void run(String... args) {
        // 业务逻辑
        var response = orchestrationService.handleCustomerQuery("我想咨询产品信息");
        System.out.println(response.getOutput());
    }
}
```

---

## 📝 开发要点

### ✅ 应该做的

1. **使用框架提供的类和接口**
   ```java
   import framework.agent.AgentFramework;
   import framework.agent.ReActAgent;
   import framework.model.AgentRequest;
   ```

2. **在业务包中创建业务逻辑**
   ```java
   package business.yourbusiness.service;
   ```

3. **通过框架API注册和调用智能体**
   ```java
   framework.registerAgent("agent_name", agent);
   framework.chatWithMaster(request);
   ```

### ❌ 不应该做的

1. **不要修改框架代码**
   ```java
   // ❌ 错误：直接修改框架类
   // framework/agent/ReActAgent.java
   ```

2. **不要将业务逻辑放在框架包中**
   ```java
   // ❌ 错误：业务代码放在框架包中
   // framework/agent/DevOpsAgent.java
   ```

3. **不要硬编码业务配置到框架中**
   ```java
   // ❌ 错误：在框架类中硬编码业务配置
   // framework/agent/AgentFramework.java
   // if (name.equals("devops_master")) { ... }
   ```

---

## 🔍 参考示例

当前项目包含一个完整的 DevOps 业务示例，位于：
- `business/devops/` - DevOps 流程自动化业务

可以参考该示例来开发新的业务模块。

---

## 🎯 框架API速查

### 核心类

- `AgentFramework` - 智能体框架，管理智能体注册和路由
- `ReActAgent` - ReAct智能体实现，封装了ReAct循环
- `LLMClient` - LLM客户端接口
- `AgentRequest` - 智能体请求
- `AgentResponse` - 智能体响应

### 主要方法

```java
// 注册智能体
framework.registerAgent(String name, Agent agent);

// 调用智能体
framework.chatWithAgent(String name, AgentRequest request);

// 调用主控智能体
framework.chatWithMaster(AgentRequest request);

// 创建智能体
new ReActAgent(name, description, isMaster, llmClient, subAgents, tools, prompt, maxRounds);
```

---

## 📚 更多信息

- 框架详细文档：参考 `framework/` 目录下的类注释
- DevOps示例：参考 `business/devops/` 目录
- 配置说明：参考 `README.md`

