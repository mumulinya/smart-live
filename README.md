<div align="center">

# 🏙️ SmartLive 智评生活

**基于 Spring Cloud Alibaba 的智慧商户微服务平台**

帮助本地商户解决引流难题，为用户提供智能化的消费决策体验

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.0-blue.svg)](https://spring.io/projects/spring-cloud)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0.1.0-orange.svg)](https://github.com/alibaba/spring-cloud-alibaba)
[![JDK](https://img.shields.io/badge/JDK-17+-red.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Star](https://gitee.com/mumulinya/smart-live/badge/star.svg?theme=dark)](https://gitee.com/mumulinya/smart-live/stargazers)

[在线文档](http://doc.smartLive.vip) · [演示地址](http://www.smartLive.vip) · [提交 Issue](https://gitee.com/mumulinya/smart-live/issues)

---

**简体中文** | [English](./README_EN.md)

</div>

## 📋 目录

- [📖 项目简介](#项目简介)
- [🏗️ 系统架构](#系统架构)
- [📦 项目仓库](#项目仓库)
- [🔧 技术栈](#技术栈)
- [📁 项目结构](#项目结构)
- [✨ 功能特性](#功能特性)
- [🚀 快速开始](#快速开始)
- [🔗 服务端口速查](#服务端口速查)
- [📚 项目文档](#项目文档)
- [🤝 参与贡献](#参与贡献)
- [📄 开源协议](#开源协议)
- [📞 联系我们](#联系我们)

---

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
│       ├── smartLive-im                           // IM 消息
│       ├── smartLive-index                        // 首页聚合 [9210]
│       ├── smartLive-interaction                  // 社交互动 [9209]
│       ├── smartLive-map                          // 地图服务 [9207]
│       ├── smartLive-marketing                    // 营销活动 [9206]
│       ├── smartLive-order                        // 订单管理 [9205]
│       ├── smartLive-product                      // 商品管理
│       ├── smartLive-points                       // 积分管理
│       ├── smartLive-search                       // 搜索引擎 [9204]
│       ├── smartLive-shop                         // 店铺管理 [9203]
│       ├── smartLive-system                       // 系统管理 [9202]
│       ├── smartLive-user                         // 用户中心 [9201]
│       └── smartLive-wallet                       // 钱包支付
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

#### 👤 用户中心 (smartLive-user) [9201]
| 功能 | 说明 |
|:---|:---|
| 用户管理 | 用户CRUD、用户列表查询、用户导出 |
| 用户详情 | 用户详细信息维护、个人信息编辑 |
| 账号安全 | 密码设置、密码修改、账号状态管理 |
| 用户统计 | 粉丝数、关注数、博客数等统计数据 |
| 用户搜索 | 根据ID查询用户、获取当前用户信息 |
| 缓存管理 | 用户信息全量发布、增量发布 |

#### 🏪 店铺管理 (smartLive-shop) [9203]
| 功能 | 说明 |
|:---|:---|
| 店铺CRUD | 店铺新增、修改、删除、查询 |
| 店铺搜索 | 按店铺名称、区域地址模糊搜索 |
| 热门榜单 | 热门店铺排行榜、距离排序推荐 |
| 店铺详情 | 店铺详细信息查询、营业状态 |
| 店铺审核 | 店铺发布审核、缓存刷新管理 |
| 批量操作 | 全量发布、批量发布店铺 |

#### 🛒 订单管理 (smartLive-order) [9205]
| 功能 | 说明 |
|:---|:---|
| 订单创建 | 创建订单、订单基本信息维护 |
| 订单查询 | 我的订单列表、订单状态追踪 |
| 订单支付 | 订单在线支付、支付状态查询 |
| 订单核销 | 订单使用/核销、线下消费确认 |
| 订单取消 | 用户取消订单、取消状态管理 |
| 退款处理 | 订单退款申请、退款状态追踪 |
| 订单导出 | 订单数据导出Excel |

#### 📦 商品管理 (smartLive-product)
| 功能 | 说明 |
|:---|:---|
| 商品CRUD | 商品新增、修改、删除、查询 |
| 商品管理 | 商品上下架、商品库存管理 |

#### 📝 博客笔记 (smartLive-blog) [9214]
| 功能 | 说明 |
|:---|:---|
| 博客发布 | 发布图文博客、博客内容编辑 |
| 博客管理 | 博客CRUD、博客列表查询 |
| 博客分类 | 按分类查询博客、分类筛选 |
| 我的博客 | 查询用户发布的博客列表 |
| 热门博客 | 热门博客推荐、热门榜单 |
| 博客置顶 | 设置博客是否置顶 |
| 博客缓存 | 刷新博客缓存、批量发布 |

#### 💬 即时通讯 (smartLive-chat) [9213]
| 功能 | 说明 |
|:---|:---|
| 私聊消息 | 用户之间一对一私信聊天 |
| 群聊功能 | 群组消息、多人聊天室 |
| 会话管理 | 聊天会话列表、会话状态 |
| 消息记录 | 历史消息记录、消息分页查询 |
| 系统通知 | 系统公告推送、通知消息 |

#### 🤖 AI 智能 (smartLive-ai) [9215]
| 功能 | 说明 |
|:---|:---|
| AI对话 | 基于Spring AI的智能对话服务 |
| 会话管理 | AI对话会话创建与管理 |
| 消息管理 | AI消息记录与历史查询 |
| 向量推荐 | 基于Milvus的个性化推荐 |

#### 🔍 搜索引擎 (smartLive-search) [9204]
| 功能 | 说明 |
|:---|:---|
| 全文搜索 | 基于Elasticsearch的统一搜索 |
| 用户搜索 | 搜索查找其他用户 |
| 店铺搜索 | 搜索目标店铺 |
| 博客搜索 | 搜索博客内容 |
| 多维度检索 | 支持关键词、分词、过滤器 |

#### 👥 社交互动 (smartLive-interaction) [9209]
| 功能 | 说明 |
|:---|:---|
| 评论 | 对博客/店铺进行评论 |
| 回复 | 评论回复、二级回复 |
| 点赞 | 内容点赞支持 |
| 收藏 | 内容收藏、收藏夹管理 |
| 关注 | 用户关注/粉丝系统 |
| 动态 | 关注动态流 |

#### 🏠 首页聚合 (smartLive-index) [9210]
| 功能 | 说明 |
|:---|:---|
| 热门推荐 | 热门内容聚合展示 |
| 资源聚合 | 热门店铺、博客、用户推荐 |
| 数据聚合 | 统一首页数据服务 |

#### 📁 文件服务 (smartLive-file) [9212]
| 功能 | 说明 |
|:---|:---|
| 文件上传 | MinIO对象存储集成 |
| 文件下载 | 文件下载与预览 |
| 头像管理 | 用户头像上传与存储 |
| 图片处理 | 图片存储与CDN分发 |

#### 🎁 积分管理 (smartLive-points)
| 功能 | 说明 |
|:---|:---|
| 积分查询 | 用户积分余额查询 |
| 积分变动 | 积分获取与消费记录 |
| 积分管理 | 管理员积分配置 |
| 积分商城 | 积分兑换商品 |

#### 💰 钱包/支付 (smartLive-wallet)
| 功能 | 说明 |
|:---|:---|
| 钱包余额 | 用户账户余额查询 |
| 在线支付 | 订单支付、充值 |
| 支付记录 | 支付流水记录 |
| 钱包管理 | 账户资金管理 |

#### ✅ 审核中心 (smartLive-audit)
| 功能 | 说明 |
|:---|:---|
| 内容审核 | 博客内容审核 |
| 店铺审核 | 商户入驻审核 |
| 评论审核 | 用户评论审核 |
| 审核流程 | 审核状态追踪 |

### ⚙️ 系统功能

#### 🔧 系统管理 (smartLive-system) [9202]
| 模块 |---|:---|
| 功能说明 |
|: **用户管理** | 系统用户配置、角色分配、用户状态 |
| **部门管理** | 组织机构树结构、数据权限控制 |
| **菜单管理** | 系统菜单、操作权限、按钮级别权限 |
| **角色管理** | 角色权限分配、数据范围划分 |
| **岗位管理** | 岗位职级配置、人员岗位关联 |
| **字典管理** | 常用固定数据维护、数据字典 |
| **参数管理** | 系统动态配置参数 |
| **通知公告** | 系统通知公告发布与查看 |
| **操作日志** | 操作日志记录与查询追踪 |
| **登录日志** | 登录日志记录、异常登录告警 |
| **在线用户** | 当前活跃用户状态监控 |

#### 📊 监控中心 (smartLive-visual-monitor) [9100]
| 功能 | 说明 |
|:---|:---|
| 服务监控 | 微服务健康状态监控 |
| CPU监控 | 服务器CPU使用率 |
| 内存监控 | JVM内存使用情况 |
| 磁盘监控 | 磁盘空间使用 |
| 线程监控 | 线程池状态 |
| 连接池监视 | 数据库连接池状态分析 |

#### 🛡️ 认证授权 (smartLive-auth) [9200]
| 功能 | 说明 |
|:---|:---|
| 用户登录 | JWT令牌登录认证 |
| 令牌刷新 | Token自动刷新机制 |
| 权限验证 | 基于注解的权限校验 |
| 登录日志 | 登录成功/失败记录 |

#### 🚪 API 网关 (smartLive-gateway) [8080]
| 功能 | 说明 |
|:---|:---|
| 路由转发 | 请求路由与负载均衡 |
| 限流熔断 | Sentinel流量控制 |
| 统一鉴权 | 请求身份验证 |
| 跨域处理 | CORS跨域配置 |

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
| Seata 服务端 | smartLive-seata-server | 7091 |
| 认证中心 | smartLive-auth | 9200 |
| 用户服务 | smartLive-user | 9201 |
| 系统服务 | smartLive-system | 9202 |
| 店铺服务 | smartLive-shop | 9203 |
| 搜索服务 | smartLive-search | 9204 |
| 订单服务 | smartLive-order | 9205 |
| 营销服务 | smartLive-marketing | 9206 |
| 地图服务 | smartLive-map | 9207 |
| 互动服务 | smartLive-interaction | 9209 |
| 首页服务 | smartLive-index | 9210 |
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

## 📞 联系我们

- **Issues**: [提交问题](https://gitee.com/mumulinya/smart-live/issues)
- **Gitee**: [项目主页](https://gitee.com/mumulinya/smart-live)

---

<div align="center">

**如果觉得不错，请给我们一个 ⭐ Star 吧!**

Made with ❤️ by SmartLive Team

</div>
