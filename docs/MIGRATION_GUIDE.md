# 代码结构迁移指南

## 目标结构

```
src/main/java/
├── framework/              # 框架代码（可复用）
│   ├── agent/
│   │   ├── Agent.java
│   │   ├── AgentFramework.java
│   │   └── ReActAgent.java
│   ├── llm/
│   │   ├── LLMClient.java
│   │   ├── DeepSeekLLMClient.java
│   │   ├── OllamaLLMClient.java
│   │   ├── OpenAILLMClient.java
│   │   └── SimpleLLMClient.java
│   ├── memory/
│   │   ├── ReactMemory.java
│   │   └── Observation.java
│   └── model/
│       ├── AgentRequest.java
│       ├── AgentResponse.java
│       └── ToolCall.java
│
└── business/              # 业务代码（具体业务实现）
    └── devops/            # DevOps业务示例
        ├── config/
        │   └── LLMConfig.java
        ├── service/
        │   ├── LLMClientService.java
        │   ├── AgentService.java
        │   └── DevOpsOrchestrationService.java
        └── Application.java
```

## 迁移步骤

### 1. 创建目录结构

```bash
cd src/main/java
mkdir -p framework/{agent,llm,memory,model}
mkdir -p business/devops/{config,service}
```

### 2. 移动框架代码

```bash
# 移动核心框架代码
mv core/* framework/agent/
mv llm/* framework/llm/
mv memory/* framework/memory/
mv model/* framework/model/

# 删除空目录
rmdir core llm memory model
```

### 3. 移动业务代码

```bash
# 移动DevOps业务代码
mv config/* business/devops/config/
mv service/* business/devops/service/
mv demo/Application.java business/devops/Application.java

# 删除空目录
rmdir config service demo
```

### 4. 更新包名和import语句

#### 框架代码包名更新

所有框架代码的包名从：
- `package core;` → `package framework.agent;`
- `package llm;` → `package framework.llm;`
- `package memory;` → `package framework.memory;`
- `package model;` → `package framework.model;`

#### 业务代码包名更新

所有业务代码的包名从：
- `package config;` → `package business.devops.config;`
- `package service;` → `package business.devops.service;`
- `package demo;` → `package business.devops;`

#### 更新import语句

在所有文件中，将：
```java
import core.*;
import llm.*;
import memory.*;
import model.*;
```

替换为：
```java
import framework.agent.*;
import framework.llm.*;
import framework.memory.*;
import framework.model.*;
```

## 自动化迁移脚本

创建 `migrate.sh` 脚本：

```bash
#!/bin/bash

cd src/main/java

# 1. 创建新目录
mkdir -p framework/{agent,llm,memory,model}
mkdir -p business/devops/{config,service}

# 2. 移动文件
[ -d core ] && mv core/* framework/agent/ && rmdir core
[ -d llm ] && mv llm/* framework/llm/ && rmdir llm
[ -d memory ] && mv memory/* framework/memory/ && rmdir memory
[ -d model ] && mv model/* framework/model/ && rmdir model
[ -d config ] && mv config/* business/devops/config/ && rmdir config
[ -d service ] && mv service/* business/devops/service/ && rmdir service
[ -d demo ] && mv demo/Application.java business/devops/Application.java 2>/dev/null; rmdir demo 2>/dev/null

# 3. 更新包名（使用sed）
find framework -name "*.java" -exec sed -i '' 's/^package core;/package framework.agent;/g' {} \;
find framework -name "*.java" -exec sed -i '' 's/^package llm;/package framework.llm;/g' {} \;
find framework -name "*.java" -exec sed -i '' 's/^package memory;/package framework.memory;/g' {} \;
find framework -name "*.java" -exec sed -i '' 's/^package model;/package framework.model;/g' {} \;

find business -name "*.java" -exec sed -i '' 's/^package config;/package business.devops.config;/g' {} \;
find business -name "*.java" -exec sed -i '' 's/^package service;/package business.devops.service;/g' {} \;
find business -name "*.java" -exec sed -i '' 's/^package demo;/package business.devops;/g' {} \;

# 4. 更新import语句
find . -name "*.java" -exec sed -i '' 's/import core\./import framework.agent./g' {} \;
find . -name "*.java" -exec sed -i '' 's/import llm\./import framework.llm./g' {} \;
find . -name "*.java" -exec sed -i '' 's/import memory\./import framework.memory./g' {} \;
find . -name "*.java" -exec sed -i '' 's/import model\./import framework.model./g' {} \;
find . -name "*.java" -exec sed -i '' 's/import config\./import business.devops.config./g' {} \;
find . -name "*.java" -exec sed -i '' 's/import service\./import business.devops.service./g' {} \;

echo "迁移完成！"
```

## 验证迁移

迁移后，执行以下命令验证：

```bash
# 编译项目
mvn clean compile

# 检查包结构
find src/main/java -name "*.java" | head -20
```

## 注意事项

1. **备份代码**：迁移前请先备份代码
2. **测试编译**：迁移后立即编译测试
3. **更新文档**：更新README等文档中的包路径引用
4. **Spring Boot扫描**：更新Application类的扫描路径

```java
@SpringBootApplication(scanBasePackages = {"business.devops"})
```

