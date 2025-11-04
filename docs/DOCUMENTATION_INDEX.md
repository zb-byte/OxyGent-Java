# 文档目录索引

本文档目录包含 react-oxygent-java 项目的所有说明文档，按主题分类组织。

---

## 📑 文档清单（按主题分类）

### 🔄 A2A 通信机制

| 文档 | 说明 |
|------|------|
| [A2A_CORE_COMPARISON.md](./A2A_CORE_COMPARISON.md) | OxyGent (Python) 与 react-oxygent-java 的 A2A 核心思路对比分析 |
| [SSE_REMOTE_CALL_CODE_LOCATION.md](./SSE_REMOTE_CALL_CODE_LOCATION.md) | Python 版本 SSE 远程调用代码位置详解 |
| [REMOTE_AGENT_USAGE.md](./REMOTE_AGENT_USAGE.md) | 远程智能体使用指南（Java 版本） |

### 🎯 业务流程和模式

| 文档 | 说明 |
|------|------|
| [AGENT_FLOW_PATTERNS.md](./AGENT_FLOW_PATTERNS.md) | 智能体流程模式详解（ReAct、PlanAndSolve、Reflexion） |
| [WHERE_TO_ADD_BUSINESS_LOGIC.md](./WHERE_TO_ADD_BUSINESS_LOGIC.md) | 业务流程中增加业务逻辑的位置指南 |

### 🚀 启动和初始化

| 文档 | 说明 |
|------|------|
| [STARTUP_SEQUENCE.md](./STARTUP_SEQUENCE.md) | Application 启动加载顺序详解 |
| [SPRING_BEAN_INITIALIZATION_ORDER.md](./SPRING_BEAN_INITIALIZATION_ORDER.md) | Spring Bean 初始化顺序详解 |

### 🔧 MCP 工具支持

| 文档 | 说明 |
|------|------|
| [MCP_USAGE_GUIDE.md](./MCP_USAGE_GUIDE.md) | MCP (Model Context Protocol) 使用指南 |
| [MCP_IMPLEMENTATION_SUMMARY.md](./MCP_IMPLEMENTATION_SUMMARY.md) | MCP 实现总结和技术细节 |
| [MCP_FEATURE_COMPLETE.md](./MCP_FEATURE_COMPLETE.md) | MCP 功能完成报告 |

### 📖 开发指南

| 文档 | 说明 |
|------|------|
| [BUSINESS_DEVELOPMENT_GUIDE.md](./BUSINESS_DEVELOPMENT_GUIDE.md) | 业务开发指南 |
| [CODE_STRUCTURE.md](./CODE_STRUCTURE.md) | 代码结构说明 |

### 📝 迁移和重构

| 文档 | 说明 |
|------|------|
| [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md) | 迁移指南 |
| [MIGRATION_COMPLETE.md](./MIGRATION_COMPLETE.md) | 迁移完成报告 |
| [REFACTORING.md](./REFACTORING.md) | 重构说明 |

### 🛠️ 其他

| 文档 | 说明 |
|------|------|
| [GIT_GUIDE.md](./GIT_GUIDE.md) | Git 使用指南 |
| [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md) | 远程调用功能实现总结 |

---

## 🎯 按使用场景导航

### 场景1: 新手入门

**推荐阅读顺序**：
1. [BUSINESS_DEVELOPMENT_GUIDE.md](./BUSINESS_DEVELOPMENT_GUIDE.md) - 了解项目结构和如何开发
2. [STARTUP_SEQUENCE.md](./STARTUP_SEQUENCE.md) - 了解应用启动流程
3. [WHERE_TO_ADD_BUSINESS_LOGIC.md](./WHERE_TO_ADD_BUSINESS_LOGIC.md) - 了解在哪里添加业务逻辑

### 场景2: 理解核心机制

**推荐阅读顺序**：
1. [A2A_CORE_COMPARISON.md](./A2A_CORE_COMPARISON.md) - 理解 A2A 通信机制
2. [AGENT_FLOW_PATTERNS.md](./AGENT_FLOW_PATTERNS.md) - 理解不同的流程模式
3. [SPRING_BEAN_INITIALIZATION_ORDER.md](./SPRING_BEAN_INITIALIZATION_ORDER.md) - 理解 Spring 初始化顺序

### 场景3: 使用远程智能体

**推荐阅读顺序**：
1. [REMOTE_AGENT_USAGE.md](./REMOTE_AGENT_USAGE.md) - 远程智能体使用指南
2. [SSE_REMOTE_CALL_CODE_LOCATION.md](./SSE_REMOTE_CALL_CODE_LOCATION.md) - Python 版本参考实现

### 场景4: 使用 MCP 工具

**推荐阅读顺序**：
1. [MCP_USAGE_GUIDE.md](./MCP_USAGE_GUIDE.md) - MCP 工具使用指南
2. [MCP_IMPLEMENTATION_SUMMARY.md](./MCP_IMPLEMENTATION_SUMMARY.md) - 实现技术细节

### 场景5: 对比 Python 版本

**推荐阅读顺序**：
1. [A2A_CORE_COMPARISON.md](./A2A_CORE_COMPARISON.md) - 核心思路对比
2. [SSE_REMOTE_CALL_CODE_LOCATION.md](./SSE_REMOTE_CALL_CODE_LOCATION.md) - SSE 实现对比

---

## 📊 文档统计

- **总文档数**: 17 个
- **最新更新**: 2024-11-04
- **覆盖主题**: A2A 通信、业务流程、启动流程、MCP 工具、开发指南等

---

## 💡 文档维护

- 所有文档位于 `docs/` 目录
- 主 README.md 保留在项目根目录
- 文档索引：`docs/README.md` 和 `docs/DOCUMENTATION_INDEX.md`

