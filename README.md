<div align="center">

# 🏙️ SmartLive 智评生活

**基于 Spring Cloud 的智慧商户微服务平台**

帮助本地商户解决引流难题，为用户提供智能化的消费决策体验

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.0-blue.svg)](https://spring.io/projects/spring-cloud)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0.1.0-orange.svg)](https://github.com/alibaba/spring-cloud-alibaba)
[![JDK](https://img.shields.io/badge/JDK-17+-red.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[在线文档](http://doc.smartLive.vip) · [演示地址](http://www.smartLive.vip) · [提交 Issue](https://gitee.com/mumulinya/smart-live/issues)

---

**简体中文** | English

</div>

## 📖 项目简介

**SmartLive（智评生活）** 是一个面向本地生活服务的多端智慧商户平台，旨在解决本地商户引流难、用户决策复杂等痛点。平台提供 **商户展示、AI 智能推荐、社交互动、营销管理** 四大核心功能，采用微服务架构拆分 **15+ 业务模块**，支持高并发与个性化用户体验。

### 🎯 核心亮点

- 🤖 **AI 智能推荐** — 基于 Spring AI + Milvus 向量数据库，实现个性化内容推荐与智能对话
- 🔍 **全文搜索引擎** — 基于 Elasticsearch 的统一搜索能力，覆盖用户、店铺、博客、优惠券等
- 💬 **即时通讯** — 基于 WebSocket 实现私聊、群聊与消息记录
- 🛒 **完整商业闭环** — 店铺入驻 → 内容发布 → 营销活动 → 在线下单 → 评价互动
- 🏗️ **微服务架构** — Spring Cloud Alibaba 全家桶，Nacos + Gateway + Sentinel + Seata
- 🐳 **一键部署** — 提供完整的 Docker Compose 编排，轻松启动全部服务

## 🏗️ 系统架构

```
                                    ┌─────────────────┐
                                    │   Nginx 代理     │
                                    │  (80 / 443)      │
                                    └────────┬─────────┘
                                             │
                           ┌─────────────────┼─────────────────┐
                           │                 │                 │
                    ┌──────▼──────┐   ┌──────▼──────┐   ┌─────▼──────┐
                    │ 后台管理 UI  │   │ 前台用户端   │   │ 小程序端   │
                    │  (Vue)      │   │  (H5/App)   │   │ (UniApp)   │
                    └──────┬──────┘   └──────┬──────┘   └─────┬──────┘
                           │                 │                 │
                           └─────────────────┼─────────────────┘
                                             │
                                    ┌────────▼─────────┐
                                    │  Spring Cloud     │
                                    │  Gateway (8080)   │
                                    │  + Sentinel 限流  │
                                    └────────┬─────────┘
                                             │
                        ┌────────────────────┼────────────────────┐
                        │                    │                    │
               ┌────────▼────────┐  ┌────────▼────────┐  ┌───────▼────────┐
               │  Auth 认证中心   │  │  Nacos 注册中心  │  │  Sentinel 控制台│
               │   (9200)        │  │ (8848)          │  │  (8718)         │
               └─────────────────┘  └─────────────────┘  └────────────────┘
                                             │
               ┌──────────────────── 业务微服务集群 ─────────────────────┐
               │                                                        │
               │  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐    │
               │  │User │ │Shop │ │Blog │ │Order│ │ AI  │ │Chat │    │
               │  │9201 │ │9203 │ │9214 │ │9205 │ │9215 │ │9213 │    │
               │  └─────┘ └─────┘ └─────┘ └─────┘ └─────┘ └─────┘    │
               │  ┌─────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌─────┐ ┌────┐  │
               │  │Index│ │Search│ │Market│ │Inter.│ │ Map │ │File│  │
               │  │9210 │ │9204  │ │9206  │ │9209  │ │9207 │ │9212│  │
               │  └─────┘ └──────┘ └──────┘ └──────┘ └─────┘ └────┘  │
               └────────────────────────────────────────────────────────┘
                                             │
               ┌─────────────────── 基础设施层 ─────────────────────────┐
               │                                                        │
               │  ┌───────┐ ┌───────┐ ┌──────────┐ ┌───────┐ ┌──────┐ │
               │  │ MySQL │ │ Redis │ │ RabbitMQ │ │  ES   │ │MinIO │ │
               │  │       │ │       │ │          │ │       │ │      │ │
               │  └───────┘ └───────┘ └──────────┘ └───────┘ └──────┘ │
               │                     ┌────────┐                        │
               │                     │ Milvus │                        │
               │                     └────────┘                        │
               └────────────────────────────────────────────────────────┘
```

## 📦 项目仓库

| 仓库 | 说明 | 链接 |
|:---:|:---:|:---:|
| **smart-live-Cloud** | 后端微服务（本仓库） | [Gitee](https://gitee.com/mumulinya/smart-live) |
| **smart-live-ui** | 后台管理前端（Vue） | [Gitee](https://gitee.com/mumulinya/smart-live-ui) |
| **smart-live-html** | 用户端前端（H5/小程序） | [Gitee](https://gitee.com/mumulinya/smart-live-html) |
| **smart-live-ai** | AI 模块（独立部署版） | [Gitee](https://gitee.com/mumulinya/smart-live-ai) |

## 🔧 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|:---|:---:|:---|
| Spring Boot | 3.2.2 | 基础框架 |
| Spring Cloud | 2023.0.0 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.1.0 | 阿里巴巴微服务套件 |
| Spring AI | - | AI 能力集成 |
| Nacos | latest | 注册中心 & 配置中心 |
| Spring Cloud Gateway | - | API 网关 |
| Sentinel | - | 流量控制 & 熔断降级 |
| Seata | - | 分布式事务 |
| MyBatis Plus | 3.5.5 | ORM 框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | latest | 缓存 & 分布式锁 |
| RabbitMQ | 3.12 | 消息队列 |
| Elasticsearch | 7.17 | 搜索引擎 |
| Milvus | 2.3.4 | 向量数据库（AI 推荐） |
| MinIO | latest | 对象存储 |
| JWT | 0.9.1 | 身份认证 |
| SpringDoc OpenAPI | 2.3.0 | 接口文档 |
| Druid | 1.2.21 | 数据库连接池 |

### 前端技术

| 技术 | 说明 |
|:---|:---|
| Vue.js | 前端框架 |
| Element UI | 后台管理 UI 组件库 |
| UniApp | 多端前台用户端 |

## 📁 项目结构

```
com.smartLive
├── smartLive-gateway              // 网关模块 [8080]
├── smartLive-auth                 // 认证中心 [9200]
├── smartLive-api                  // 接口模块（Feign 客户端、DTO、VO）
│       ├── smartLive-api-blog                     // 博客接口
│       ├── smartLive-api-chat                     // 聊天接口
│       ├── smartLive-api-interaction              // 互动接口
│       ├── smartLive-api-marketing                // 营销接口
│       ├── smartLive-api-order                    // 订单接口
│       ├── smartLive-api-shop                     // 店铺接口
│       ├── smartLive-api-system                   // 系统接口
│       └── smartLive-api-user                     // 用户接口
├── smartLive-common               // 通用模块
│       ├── smartLive-common-core                  // 核心工具
│       ├── smartLive-common-datascope             // 数据权限
│       ├── smartLive-common-datasource            // 多数据源
│       ├── smartLive-common-log                   // 日志记录
│       ├── smartLive-common-redis                 // 缓存服务
│       ├── smartLive-common-rabbitmq              // 消息队列
│       ├── smartLive-common-seata                 // 分布式事务
│       ├── smartLive-common-security              // 安全认证
│       ├── smartLive-common-sensitive             // 数据脱敏
│       └── smartLive-common-swagger               // API 文档
├── smartLive-modules              // 业务模块
│       ├── smartLive-ai                           // AI 智能模块 [9215]
│       ├── smartLive-audit                        // 审核模块
│       ├── smartLive-blog                         // 博客笔记 [9214]
│       ├── smartLive-chat                         // 即时通讯 [9213]
│       ├── smartLive-file                         // 文件服务 [9212]
│       ├── smartLive-gen                          // 代码生成 [9211]
│       ├── smartLive-im                           // IM 消息
│       ├── smartLive-index                        // 首页聚合 [9210]
│       ├── smartLive-interaction                  // 社交互动 [9209]
│       ├── smartLive-job                          // 定时任务 [9208]
│       ├── smartLive-map                          // 地图服务 [9207]
│       ├── smartLive-marketing                    // 营销活动 [9206]
│       ├── smartLive-order                        // 订单管理 [9205]
│       ├── smartLive-search                       // 搜索引擎 [9204]
│       ├── smartLive-shop                         // 店铺管理 [9203]
│       ├── smartLive-system                       // 系统管理 [9202]
│       └── smartLive-user                         // 用户中心 [9201]
├── smartLive-visual               // 图形化管理
│       └── smartLive-visual-monitor               // 监控中心 [9100]
├── smartLive-sentinel             // 限流控制台 [8718]
├── smartLive-seata-server         // 分布式事务服务端 [7091]
├── docker                         // Docker 编排
├── sql                            // 数据库脚本
├── bin                            // 启动脚本
└── pom.xml                        // 父 POM
```

## ✨ 功能特性

### 🛍️ 业务功能

| 模块 | 功能说明 |
|:---|:---|
| **用户中心** | 注册登录、个人信息维护、账号安全、关注/粉丝管理 |
| **店铺管理** | 商户入驻、店铺信息管理、营业状态、店铺审核 |
| **博客笔记** | 图文发布、分类管理、内容编辑、阅读统计 |
| **订单管理** | 订单创建、支付、退款、全生命周期管理 |
| **营销管理** | 优惠券发放、秒杀活动、限时折扣 |
| **搜索引擎** | 基于 ES 的全文检索，覆盖用户/店铺/博客/优惠券 |
| **社交互动** | 评论、回复、点赞、收藏、关注动态 |
| **即时通讯** | WebSocket 私聊、群聊、消息记录 |
| **AI 推荐** | Spring AI + Milvus 向量检索，智能推荐与对话 |
| **地图服务** | 地理定位、范围搜索、地图标点 |
| **首页聚合** | 热门内容推荐、资源聚合展示 |
| **审核中心** | 内容审核、评论审核流程管理 |
| **文件服务** | MinIO 对象存储、文件上传/下载 |

### ⚙️ 系统功能

| 模块 | 功能说明 |
|:---|:---|
| **用户管理** | 系统用户配置、角色分配 |
| **部门管理** | 组织机构树结构、数据权限 |
| **菜单管理** | 系统菜单、操作权限、按钮权限 |
| **角色管理** | 角色权限分配、数据范围划分 |
| **字典管理** | 常用固定数据维护 |
| **参数管理** | 系统动态配置参数 |
| **通知公告** | 系统通知公告发布 |
| **操作日志** | 操作日志记录与查询 |
| **登录日志** | 登录日志记录（含异常） |
| **在线用户** | 活跃用户状态监控 |
| **定时任务** | 在线任务调度管理 |
| **代码生成** | Java/HTML/XML/SQL 代码生成 |
| **系统接口** | 自动生成 API 文档 |
| **服务监控** | CPU/内存/磁盘/堆栈监控 |
| **连接池监视** | 数据库连接池状态分析 |

## 🚀 快速开始

### 环境要求

| 环境 | 版本要求 |
|:---|:---|
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |
| Nacos | 2.x |
| Node.js | 16+（前端构建） |

### 方式一：Docker Compose 一键部署（推荐）

```bash
# 1. 克隆项目
git clone https://gitee.com/mumulinya/smart-live.git
cd smart-live

# 2. 构建所有服务
mvn clean install -DskipTests

# 3. 进入 docker 目录，一键启动
cd docker
docker-compose up -d

# 4. 查看服务状态
docker-compose ps
```

> **启动顺序说明**：Docker Compose 已配置依赖关系，MySQL → Nacos → 其他中间件 → 业务服务，会自动按序启动。

### 方式二：本地开发模式

```bash
# 1. 克隆项目
git clone https://gitee.com/mumulinya/smart-live.git
cd smart-live

# 2. 导入数据库脚本
#    执行 sql/ 目录下的 SQL 文件至 MySQL

# 3. 启动中间件
#    确保 Nacos、MySQL、Redis、RabbitMQ 已启动

# 4. 修改 Nacos 配置
#    在 Nacos 控制台中配置各服务的数据库连接、Redis 等参数

# 5. 构建项目
mvn clean install -DskipTests

# 6. 按顺序启动服务
#    ① 网关服务
mvn spring-boot:run -pl smartLive-gateway
#    ② 认证中心
mvn spring-boot:run -pl smartLive-auth
#    ③ 业务模块（按需启动）
mvn spring-boot:run -pl smartLive-modules/smartLive-system
mvn spring-boot:run -pl smartLive-modules/smartLive-user
mvn spring-boot:run -pl smartLive-modules/smartLive-shop
# ... 其他模块
```

### 方式三：使用 Windows 启动脚本

项目提供了 `bin/` 目录下的 `.bat` 脚本，可快速启动核心服务：

```bash
bin/run-gateway.bat        # 启动网关
bin/run-auth.bat           # 启动认证中心
bin/run-modules-system.bat # 启动系统模块
bin/run-modules-file.bat   # 启动文件服务
# ...
```

## 🔗 服务端口速查

| 服务 | 模块 | 端口 |
|:---|:---|:---:|
| Nginx 代理 | - | 80 / 443 |
| API 网关 | smartLive-gateway | 8080 |
| 前台用户端 | smartLive-html | 8081 |
| Sentinel 控制台 | smartLive-sentinel | 8718 |
| Nacos 注册中心 | - | 8848 |
| 认证中心 | smartLive-auth | 9200 |
| 用户服务 | smartLive-user | 9201 |
| 系统服务 | smartLive-system | 9202 |
| 店铺服务 | smartLive-shop | 9203 |
| 搜索服务 | smartLive-search | 9204 |
| 订单服务 | smartLive-order | 9205 |
| 营销服务 | smartLive-marketing | 9206 |
| 地图服务 | smartLive-map | 9207 |
| 定时任务 | smartLive-job | 9208 |
| 互动服务 | smartLive-interaction | 9209 |
| 首页服务 | smartLive-index | 9210 |
| 代码生成 | smartLive-gen | 9211 |
| 文件服务 | smartLive-file | 9212 |
| 聊天服务 | smartLive-chat | 9213 |
| 博客服务 | smartLive-blog | 9214 |
| AI 服务 | smartLive-ai | 9215 |
| 监控中心 | smartLive-visual-monitor | 9100 |

## 📚 项目文档

- 📘 [在线文档](http://doc.smartLive.vip)
- 📄 [接口文档](http://doc.smartLive.vip) — 基于 SpringDoc OpenAPI 自动生成

## 🤝 参与贡献

我们非常欢迎各种形式的贡献！无论是新功能、Bug 修复还是文档改进，都请随时提交。

### 贡献流程

1. **Fork** 本仓库
2. **创建** 你的功能分支 (`git checkout -b feature/AmazingFeature`)
3. **提交** 你的更改 (`git commit -m 'feat: 添加了一些很棒的功能'`)
4. **推送** 到远程分支 (`git push origin feature/AmazingFeature`)
5. **创建** Pull Request

### 提交规范

提交信息格式：`type(scope): subject`

| 类型 | 说明 |
|:---|:---|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更新 |
| `style` | 代码格式调整 |
| `refactor` | 代码重构 |
| `test` | 测试用例 |
| `chore` | 构建/工具更新 |

## 📄 开源协议

本项目基于 [MIT License](LICENSE) 开源。

## 🌟 Star 历史

如果这个项目对你有帮助，请给它一个 ⭐ Star！你的支持是我们持续改进的动力。

## 📞 联系我们

- **Issues**: [提交问题](https://gitee.com/mumulinya/smart-live/issues)
- **Gitee**: [项目主页](https://gitee.com/mumulinya/smart-live)

---

<div align="center">

**如果觉得不错，请给我们一个 ⭐ Star 吧!**

Made with ❤️ by SmartLive Team

</div>