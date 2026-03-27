<div align="center">

# 🏙️ SmartLive 智评生活

**基于 Spring Cloud Alibaba 的本地生活微服务平台，覆盖发现、交易、社交、热榜与 AI/RAG**

围绕“用户发现 -> 决策下单 -> 履约评价 -> 社交互动 -> 商家经营 -> 平台治理”构建完整业务闭环，重点展示复杂业务如何被微服务与中间件工程化落地

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.0-blue.svg)](https://spring.io/projects/spring-cloud)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0.1.0-orange.svg)](https://github.com/alibaba/spring-cloud-alibaba)
[![JDK](https://img.shields.io/badge/JDK-17+-red.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/mumulinya/smartLive-Cloud?style=flat&logo=github)](https://github.com/mumulinya/smartLive-Cloud/stargazers)
![Personal Project](https://img.shields.io/badge/个人独立项目-从零设计开发-ff69b4.svg)

### 📚 **[项目在线文档网站（完整排版版）](https://mumulinya.github.io/smartLive-Cloud/)**

**文档导航：** [在线文档](https://mumulinya.github.io/smartLive-Cloud/) · [视觉导览](docs/SHOWCASE.md) · [页面导览](docs/PAGE_GALLERY.md) · [开源接入](docs/OPEN_SOURCE.md) · [提交 Issue](https://github.com/mumulinya/smartLive-Cloud/issues)

---

</div>

## 📦 项目仓库矩阵

| 仓库 | 说明 | 链接 |
|:---:|:---:|:---:|
| **smartLive-Cloud** | 后端主仓库与在线总文档（本仓库） | [GitHub](https://github.com/mumulinya/smartLive-Cloud) |
| **smartLive-web** | 用户端 App（Vue 3 / H5 页面） | [GitHub](https://github.com/mumulinya/smartLive-web.git) |
| **smartLive-admin** | 商家端与平台管理后台（Vue 2 + Element UI） | [GitHub](https://github.com/mumulinya/smartLive-admin.git) |

## 🖼️ 页面导览入口

| 分组 | 内容说明 | 入口 |
|:---|:---|:---|
| 用户端视觉导览 | 登录、首页、搜索、店铺、商品、订单、评价、社交、AI、钱包与积分的完整走查 | [SHOWCASE](https://mumulinya.github.io/smartLive-Cloud/SHOWCASE) |
| 商家端 Web 页面走查 | 经营总览、店铺管理、商品管理、订单履约、商家助手等链路展示 | [SHOWCASE](https://mumulinya.github.io/smartLive-Cloud/SHOWCASE) |
| 平台管理端页面走查 | 审核中心、博客管理、评论管理、评价管理、业务用户、积分配置等治理页面 | [SHOWCASE](https://mumulinya.github.io/smartLive-Cloud/SHOWCASE) |
| 页面映射总表 | 用户端、商家端、平台管理端全部页面名称与截图索引 | [PAGE_GALLERY](https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY) |
| 开源接入与部署 | 本地运行、依赖矩阵、端口配置、部署与排错入口 | [OPEN_SOURCE](https://mumulinya.github.io/smartLive-Cloud/OPEN_SOURCE) |

- 推荐阅读顺序：先看“页面导览入口”，再看“效果预览”和“5 分钟读懂项目”；如果准备本地运行，直接从开源接入说明开始。

## 📋 文档导航

- [📦 项目仓库矩阵](#项目仓库矩阵)
- [🖼️ 页面导览入口](#页面导览入口)
- [📖 项目简介](#项目简介)
- [📏 项目规模](#项目规模)
- [👨‍💻 个人贡献亮点](#个人贡献亮点)
- [🧭 5 分钟读懂项目](#5分钟读懂项目)
- [🎨 效果预览](#效果预览)
- [🚀 快速开始](#快速开始)
- [📄 项目文档](#项目文档)
- [🎯 核心亮点](#核心亮点)
- [📚 扩展导航](#扩展导航)
- [❓ 常见问题 FAQ](#常见问题)
- [🚧 未来规划 Roadmap](#未来规划)

---

## <a id="扩展导航"></a>📚 扩展导航

主目录只保留首页最核心的入口，下面这些“下沉但仍保留在 README 正文中的章节”可以从这里直接跳转：

- **架构与能力：** [功能特性](#功能特性) · [技术栈](#技术栈) · [系统架构](#系统架构) · [项目结构](#项目结构)
- **链路与实践：** [核心业务链路](#核心业务链路) · [核心链路与面经](#核心链路与面经) · [技术选型理由](#技术选型理由) · [性能压测报告](#性能压测报告)
- **工程与维护：** [开源使用提示](#开源使用提示) · [难点踩坑与解决方案](#难点踩坑与解决方案) · [项目沉淀](#项目沉淀)
- **仓库与协作：** [项目仓库](#项目仓库) · [参与贡献](#参与贡献) · [开源协议](#开源协议) · [联系我](#联系我)

---

## <a id="项目简介"></a>📖 项目简介

> 🙋 **个人独立项目声明**：本项目由作者从零开始独立设计、编码并持续维护，
> 非培训项目或拼接式 Demo，仓库内保留了完整的开发与演进记录。

### 🎯 项目定位

**SmartLive（智评生活）** 是一个面向**本地生活服务场景**的微服务平台，覆盖用户发现、交易履约、内容互动、商家经营与平台治理。  
它不是单一业务 Demo，而是把**交易、搜索、社交、审核、IM、支付、积分、热榜、AI/RAG** 等能力放进同一套可运行、可追踪、可继续扩展的系统里。

### 🧩 这个仓库覆盖什么

- **后端主仓库**：包含 `16` 个业务模块，并连同 `auth / gateway / monitor` 组成 `19` 个可运行服务应用。
- **双端业务闭环**：既覆盖用户侧找店、下单、评价、博客、聊天与 AI，也覆盖商家侧商品管理、经营分析与平台治理。
- **多中间件协同**：Redis、RabbitMQ、Elasticsearch、Milvus、MinIO、XXL-JOB、Nacos、Gateway、Sentinel、Seata 共同支撑核心链路。
- **文档资产完整**：页面导览、链路图、系统架构、答辩页和网站文档都已补齐。

想看完整项目全貌、仓库矩阵和统计口径，建议直接看：

- [项目全貌与答辩说明](docs/PROJECT_OVERVIEW.md)

### <a id="项目规模"></a>📏 项目规模

| 维度 | 数值 | 说明 |
|------|------|------|
<!-- AUTO_SYNC:README_PROJECT_SCALE:START -->
| 核心业务模块 | **16 个** | 用户、店铺、商品、订单、博客、互动、搜索、AI、IM 等核心业务模块 |
| 可运行服务应用 | **19 个** | 16 个业务模块 + `auth` + `gateway` + `monitor` |
| 核心能力域 | **6 大类** | 发现推荐、交易履约、社交互动、内容治理、经营分析、基础设施 |
| 端侧覆盖 | **2 端** | B 端商家管理 + C 端消费者体验 |
| 核心业务代码 | **30,000+ 行** | 以服务端业务逻辑为主 |
| 文档与图示资产 | **完整导览** | 展示版/详细版链路图、页面截图、在线文档与开源接入指南 |
| Git 提交记录 | **200+** | 可回溯完整开发过程 |
<!-- AUTO_SYNC:README_PROJECT_SCALE:END -->

### 🔢 统计口径说明

<!-- AUTO_SYNC:README_STATS_SCOPE:START -->
| 指标 | 统计口径 |
|------|----------|
| 业务模块 | 统计 `smartLive-modules` 下的实际业务服务，不包含 `api / common / visual / sentinel / seata-server` 等支撑工程 |
| 服务应用 | 业务模块 + `smartLive-auth` + `smartLive-gateway` + `smartLive-monitor`，按可独立运行的服务口径计算 |
| 代码量 | README 保留“核心业务代码 3万+”口径；网站首页卡片展示仓库当前 Java 总量 `7万+`，两者分别用于强调业务密度与工程总体量 |
| 页面截图 | 统计 `docs/screenshots` 中当前被在线文档实际引用的截图，不包含 `legacy` 归档图 |
| 链路图 | 分开展示版 `SVG` 与详细版 `SVG` 两套数量，均以 `docs/diagrams` 当前可访问文件为准 |
| XXL-JOB 任务 | 以调度后台当前可见任务数为准，和文档中的后台截图口径保持一致 |
<!-- AUTO_SYNC:README_STATS_SCOPE:END -->

### <a id="个人贡献亮点"></a>👨‍💻 [个人贡献亮点（本项目核心设计与实现）](docs/site-pages/CONTRIBUTIONS.md)

这里只保留首页版摘要，完整展开建议直接看：

- [我的核心设计与实现](docs/site-pages/CONTRIBUTIONS.md)

先看交付密度与 6 条最强个人贡献：

**交付规模：**
<!-- AUTO_SYNC:README_CONTRIBUTION_SCALE:START -->
- 🔸 核心业务代码：**30,000+ 行**（覆盖 16 个业务模块与完整服务应用骨架）
- 🔸 文档与图示资产：**完整在线文档 + 业务链路图 + 真实页面截图**（便于讲解、复盘与源码阅读）
- 🔸 项目周期：**8+ 个月**（从零开始独立完成全栈设计与开发）
- 🔸 Git 提交记录：**200+** 条（完整的开发足迹可追溯）
<!-- AUTO_SYNC:README_CONTRIBUTION_SCALE:END -->

- **Redis 分层缓存架构**：把列表、详情、计数三类读链拆成不同缓存结构，首页、博客、商品、店铺等高频场景整体提速约 `20-40 倍`。
- **微服务拆分与跨服务协同**：独立完成 `16` 个业务模块与 `19` 个服务应用的边界设计，落地 Feign、RabbitMQ、Seata 的协同闭环。
- **高并发交易链路优化**：秒杀链路采用 `Lua + MQ + 延迟补偿`，在 `5000` 并发线程下把 QPS 稳定在 `3200+`。
- **热榜评分与异步洗牌**：抽象 5 类业务热榜策略，结合时间衰减、互动权重、增量重算与凌晨全量重建解决冷启动和长期霸榜。
- **消息可靠投递与幂等消费**：统一封装发送端 Confirm / Return、消费端 Redis 幂等和手动 ACK/NACK，把重复消费和补偿边界收口。
- **用户端 AI 与 AIGC 闭环**：打通 SSE 对话、RAG 检索、推荐卡片、博客生成、评价生成等用户侧可感知能力。

### <a id="核心亮点"></a>🎯 [核心亮点](docs/site-pages/CORE_HIGHLIGHTS.md)

这里只保留项目级能力摘要，完整版本建议直接看：

- [项目级能力亮点](docs/site-pages/CORE_HIGHLIGHTS.md)

首页先看这 3 组项目卖点：

- **AI 增强与智能中枢**：用户端 AI 对话、RAG 检索、推荐卡片、博客/评价生成与商家经营分析落在同一套 Spring AI + Milvus 底座上。
- **高性能交易与推荐闭环**：秒杀、订单、支付、热榜、Feed、搜索读链路都围绕 `Redis + MQ + 调度补偿` 做了工程化优化。
- **企业级治理与边界防护**：Gateway 鉴权与过滤、Netty IM、责任链审核、分布式锁、跨库副本收敛共同构成完整的基础设施能力。

## <a id="5分钟读懂项目"></a>🧭 5 分钟读懂项目

第一次进入这个仓库，建议先按下面这条路径阅读，不要一上来就尝试把全部模块和中间件一次性跑齐。

<div align="center">
  <img src="docs/diagrams/open-source-reading-path.svg" alt="SmartLive 开源文档阅读路径" width="100%">
</div>

| 你的目标 | 先看什么 | 再看什么 | 结果 |
|:---|:---|:---|:---|
| 快速判断项目值不值得继续看 | [README.md](README.md) | [docs/SHOWCASE.md](docs/SHOWCASE.md) | 先看清业务范围、系统架构、真实页面和核心链路 |
| 想先理解模块拆分和代码边界 | [系统架构](#系统架构) | [项目结构](#项目结构) | 先建立全局心智模型，再回头读具体业务模块 |
| 第一次把服务跑起来 | [docs/OPEN_SOURCE.md](docs/OPEN_SOURCE.md) | [README.md 的快速开始](#快速开始) | 先跑最小可运行链路，避免一次性踩完全部依赖坑 |
| 体验 AI / 搜索 / 审核 / 积分全链路 | [docs/OPEN_SOURCE.md](docs/OPEN_SOURCE.md) | [docs/SHOWCASE.md](docs/SHOWCASE.md) | 先补齐依赖矩阵，再对照截图和业务链路逐项验证 |
| 准备本地部署或服务器演示 | [docs/DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md) | [SECURITY.md](SECURITY.md) | 明确 Docker、构建产物、配置注入与部署排错边界 |

> 推荐起步顺序：`smartLive-auth -> smartLive-gateway -> smartLive-system -> smartLive-user -> smartLive-shop -> smartLive-search`。先验证登录、店铺、搜索和后台管理，再补 AI、IM、积分、支付等进阶能力。


## <a id="效果预览"></a>🎨 效果预览

README 这里只保留最主要的 App 端核心页面，帮助你快速建立第一印象；完整页面请直接看 [docs/PAGE_GALLERY.md](docs/PAGE_GALLERY.md)。

### 1. 登录与身份进入

<p align="center">
  <a href="docs/PAGE_GALLERY.md#app-login-entry"><img src="docs/screenshots/login-page.png" alt="login" width="300"></a>
</p>

登录页负责建立账号入口和用户身份，是第一次进入 App 的起点。  
[查看登录页与账号入口](docs/PAGE_GALLERY.md#app-login-entry)

### 2. 首页入口与热榜榜单

<p align="center">
  <a href="docs/PAGE_GALLERY.md#app-discovery"><img src="docs/screenshots/homepage.png" alt="home-hot" width="300"></a>
</p>

首页承接分类入口、热门内容流和榜单跳转，是用户发现内容和店铺的主入口。  
[查看首页入口、热门内容与热榜榜单](docs/PAGE_GALLERY.md#app-discovery)

### 3. 搜索与地图找店

<p align="center">
  <a href="docs/PAGE_GALLERY.md#app-search-map"><img src="docs/screenshots/map-view.png" alt="map" width="300"></a>
</p>

搜索页支持关键词、热词和位置感知，地图模式进一步放大了附近找店体验。  
[查看搜索结果与 LBS 找店](docs/PAGE_GALLERY.md#app-search-map)

### 4. 店铺与商品详情

<p align="center">
  <a href="docs/PAGE_GALLERY.md#app-shop-product"><img src="docs/screenshots/shop-detail.png" alt="shop" width="300"></a>
</p>

详情页承接店铺介绍、商品购买、评价查看和内容互动，是下单前的核心决策页。  
[查看店铺页、商品页与内容发布](docs/PAGE_GALLERY.md#app-shop-product)

### 5. 社交消息与即时通讯

<p align="center">
  <a href="docs/PAGE_GALLERY.md#app-social-message"><img src="docs/screenshots/app-chat-detail.png" alt="im" width="300"></a>
</p>

这里集中展示了会话列表、系统通知和私聊消息，是社交互动与消息流转的主要承接页。  
[查看动态、消息中心与 IM](docs/PAGE_GALLERY.md#app-social-message)

### 6. AI 与订单资产

<p align="center">
  <a href="docs/PAGE_GALLERY.md#app-ai-capability"><img src="docs/screenshots/app-ai-order-card.png" alt="ai-order" width="300"></a>
</p>

AI 页不只负责对话，还能返回推荐卡片、生成内容，并直接承接下单结果和订单跳转。  
[查看 AI 对话、会话与 AIGC](docs/PAGE_GALLERY.md#app-ai-capability)

### 🧩 更多页面入口

- 完整用户端 App 页面：看 [docs/PAGE_GALLERY.md - 用户端 App](docs/PAGE_GALLERY.md#app-pages)
- 完整管理端 Web 页面与 XXL-JOB 后台：看 [docs/PAGE_GALLERY.md - 管理端 Web](docs/PAGE_GALLERY.md#admin-pages)
- 按业务链路看截图、架构图和时序图：看 [docs/SHOWCASE.md](docs/SHOWCASE.md)


## <a id="快速开始"></a>🚀 快速开始

README 首页只保留最小可运行链路，完整启动顺序、端口表、依赖矩阵和部署方式请直接看：

- [开源接入说明](docs/OPEN_SOURCE.md)
- [部署说明](docs/DEPLOYMENT_GUIDE.md)

### 首页版启动建议

- 第一次接入优先走本地开发模式，不建议一开始就直接用 `docker/` 或 `bin/*.bat`
- 先把登录、店铺、搜索三条最小链路跑通，再补订单、互动、AI、审核、支付
- `docker/` 和部分脚本目录仍保留历史命名，使用前请先和当前 Maven 模块核对

### 最小环境

- 必需：`JDK 17+`、`Maven 3.8+`、`MySQL 8+`、`Redis 6+`、`Nacos 2.x`、`RabbitMQ 3.12+`
- 按需补：`Elasticsearch`、`Milvus`、`MinIO`、`XXL-JOB`、`Sentinel`、`Node.js`、`Docker / Compose`

### 首页版最小依赖矩阵

| 想先体验什么 | 推荐启动模块 | 最少中间件 |
|:---|:---|:---|
| 登录与后台基础能力 | `auth`、`gateway`、`system`、`user`、`shop` | MySQL、Redis、Nacos |
| 搜索与附近找店 | 在上一条基础上加 `search` | MySQL、Redis、Nacos、Elasticsearch |
| 下单、支付与积分 | `product`、`order`、`wallet`、`points` | MySQL、Redis、Nacos、RabbitMQ |
| AI 对话与 RAG | `ai`、`search`、`shop`、`product`、`blog`、`interaction` | MySQL、Redis、Nacos、RabbitMQ、Elasticsearch、Milvus、MinIO |

### 首页版启动顺序

1. 导入数据库脚本和 Nacos 配置
2. 启动 `MySQL / Redis / Nacos / RabbitMQ`
3. 执行 `mvn clean install -DskipTests`
4. 优先启动 `smartLive-auth -> smartLive-gateway -> smartLive-system -> smartLive-user -> smartLive-shop -> smartLive-search`
5. 验证登录、店铺列表、搜索接口可用后，再补 `product / order / interaction / ai / wallet / points`

### 更多启动与部署方式

- 本地完整接入：看 [开源接入说明](docs/OPEN_SOURCE.md)
- Docker Compose、增量部署脚本、Windows 启动脚本：看 [部署说明](docs/DEPLOYMENT_GUIDE.md)

## <a id="项目文档"></a>📚 项目文档

README 首页只保留 4 个最常用的文档入口：

- 📘 [在线文档网站](https://mumulinya.github.io/smartLive-Cloud/) — 完整排版版，适合顺着导航继续阅读
- 🖼️ [视觉导览](docs/SHOWCASE.md) — 看页面截图、系统总览图和链路图集
- 📌 [开源使用说明](docs/OPEN_SOURCE.md) — 看依赖矩阵、端口表、配置来源和启动建议
- 🚀 [部署说明](docs/DEPLOYMENT_GUIDE.md) — 看 Docker、增量部署、镜像与排错

## <a id="功能特性"></a>✨ 功能特性

这里保留首页版摘要，完整功能矩阵和技术列表已独立到网站页，适合继续深入看：

- [功能特性与技术栈](docs/site-pages/FEATURES_STACK.md)
- [页面效果图导览](docs/PAGE_GALLERY.md)
- [业务链路视觉走查](docs/SHOWCASE.md)

当前项目可以先概括成 4 组能力：

- 交易闭环：商品、订单、支付、退款、积分、钱包、秒杀
- 内容社交：博客、评论、评价、关注、Feed、IM、系统通知
- 搜索推荐：LBS 搜索、热词、热榜、ES / Milvus 双副本收敛
- 平台治理：审核中心、认证网关、监控中心、XXL-JOB 调度

## <a id="技术栈"></a>🔧 技术栈

这里保留首页版摘要，完整技术清单和选型说明建议直接看：

- [功能特性与技术栈](docs/site-pages/FEATURES_STACK.md)
- [技术选型理由](docs/site-pages/TECH_SELECTION.md)

当前这套工程的核心技术组合可以先记住这 5 组：

- 服务基础：Spring Boot 3.2.2 + Spring Cloud 2023 + Spring Cloud Alibaba 2023.0.1.0
- 治理组件：Nacos、Gateway、Sentinel、Seata、XXL-JOB
- 数据与缓存：MySQL、Redis、Elasticsearch、Milvus、MinIO
- 异步与实时：RabbitMQ、Netty + WebSocket
- AI 与文档：Spring AI、SpringDoc OpenAPI、UniApp、Vue

## <a id="系统架构"></a>🏗️ 系统架构

首页只保留系统全景入口，完整的架构说明、服务依赖图和项目结构请直接看：

- [系统架构与项目规模](docs/site-pages/SYSTEM_ARCHITECTURE.md)

<div align="center">
  <img src="docs/diagrams/system-architecture-overview.svg" alt="SmartLive 系统架构图" width="100%">
</div>

首页先记住这 3 个点：

- `Gateway + Auth` 统一承接入口层鉴权与流量治理
- 业务上按基础服务、交易履约、内容社交、搜索智能治理四个服务簇拆分
- 基础设施由 `MySQL / Redis / RabbitMQ / Elasticsearch / Milvus / MinIO / Nacos / XXL-JOB` 协同支撑


## <a id="项目结构"></a>📁 项目结构

README 首页不再展开完整目录树，模块结构、服务依赖和推荐阅读顺序请直接看：

- [系统架构与项目规模](docs/site-pages/SYSTEM_ARCHITECTURE.md)

如果只想先抓主干，建议先从 `auth -> gateway -> user/shop/search` 读起，再按交易、社交、AI 三条线往下展开。

## <a id="技术选型理由"></a>🤔 [技术选型理由](docs/site-pages/TECH_SELECTION.md) - 为什么选这些而不是其他？

这里保留首页版摘要，完整选型对比建议直接看：

- [技术选型理由（网站详细版）](docs/site-pages/TECH_SELECTION.md)

首页先记住这 5 个最关键的判断：

1. `Spring Boot + Spring Cloud Alibaba`：单服务开发底座和微服务治理分层清楚，适合当前 `19` 个服务应用规模。
2. `Nacos + Gateway + Sentinel + Seata`：注册、配置、统一入口、限流熔断和分布式事务一次接齐。
3. `Redis + Elasticsearch + Milvus + MinIO`：分别承接缓存、搜索、副本检索和对象存储，不强行让一种技术做所有事情。
4. `RabbitMQ + XXL-JOB`：MQ 负责事件异步，XXL-JOB 负责定时扫描、补偿兜底和批量重建。
5. `Spring AI + Netty + MyBatis Plus`：分别对应 AI 编排、IM 长连接和可控的数据访问层。


## <a id="核心业务链路"></a>🌊 核心业务链路

README 首页不再展开全量链路图，完整的展示版 / 详细版 SVG 请直接看：

- [业务链路视觉走查](docs/SHOWCASE.md)

如果只想先看代表性链路，建议优先点这 4 条：

- [秒杀抢购全链路](docs/diagrams/seckill-flow.svg)
- [订单支付与退款补偿](docs/diagrams/unified-pay-sequence.svg)
- [Redis 分层缓存设计](docs/diagrams/redis-layered-cache-architecture.svg)
- [发布审核与搜索 / 向量同步](docs/diagrams/publish-audit-search-sync-chain.svg)

其余按交易、搜索、社交、AI、Redis、调度分组的完整图集，放到网站页里阅读会更舒服。


## <a id="核心链路与面经"></a>🔥 核心链路与面经

首页只保留入口，完整深度解析和面试展开顺序请直接看：

- [核心链路总览](docs/core-links/index.md)

如果你准备做项目讲解，建议先讲秒杀、订单支付退款、Redis 分层缓存，再回到 [项目驱动学习复盘指南](docs/core-links/SmartLive_Java_Internship_Review_Plan_Updated.md) 串起答辩顺序。


## <a id="开源使用提示"></a>📌 开源使用提示

这部分只保留首页版提醒，完整说明已放进：

- [开源接入说明](docs/OPEN_SOURCE.md)
- [安全与敏感配置边界](SECURITY.md)
- [参与贡献说明](CONTRIBUTING.md)

首页先记住这 4 条：

- 先跑最小链路，不要一上来把 AI、支付、审核、IM 一次性全开。
- 先理解 `MySQL 是真实源，ES / Milvus / Redis 是查询副本`，再去看搜索、热榜和向量检索。
- 先抓住缓存分层、策略工厂、责任链、MQ + 调度补偿这几个核心设计点，再读具体模块。
- AI、对象存储、支付、向量检索都依赖外部配置，第一次阅读更适合先看链路和接口，再补齐环境。

## <a id="性能压测报告"></a>📈 [性能压测报告](docs/site-pages/PERFORMANCE.md)

这里保留首页版摘要，完整压测表与工程表现已独立到网站页：

- [性能压测与工程表现](docs/site-pages/PERFORMANCE.md)

先看最关键的 4 个结论：

- 秒杀抢购链路在 `5000` 并发线程下，QPS 保底 `3200+`
- 列表类查询通过 `Redis ZSet + 批量回填`，相比直查数据库提升约 `20-40 倍`
- 详情类读链路通过 `String 缓存 + 逻辑过期`，TP99 可以压到 `< 5ms`
- 高并发写场景尽量走 `Lua + MQ + 定时补偿`，避免直接把压力打到主库

## <a id="难点踩坑与解决方案"></a>🚧 [难点踩坑与解决方案](docs/site-pages/PITFALLS.md)

这里保留首页版摘要，完整踩坑复盘建议直接看：

- [难点踩坑与解决方案（网站详细版）](docs/site-pages/PITFALLS.md)

首页先保留 5 组最能体现工程性的难点：

1. `缓存与一致性`：高频写场景下，用 `Redis Hash 增量更新 + RENAME 快照归档 + 批量落库` 把读写路径拆开。
2. `交易与补偿`：秒杀和订单链路通过 `Lua + RabbitMQ 延迟队列 + 状态校验` 处理超卖、少卖和重复补偿。
3. `消息与最终一致性`：发送端 Confirm / Return、消费端 Redis 幂等、手动 ACK/NACK、延迟 / 死信队列一起闭环。
4. `推荐与社交`：Feed 用滚动分页解决数据偏移，热门面板用增量洗牌 + 全量重建解决冷启动和长期霸榜。
5. `内容治理`：审核中心用策略工厂 + 责任链，把博客、商品、评论、用户资料等多条线统一收口。


## <a id="项目沉淀"></a>🧠 [我通过这个项目学到的东西](docs/site-pages/LEARNINGS.md)

这里保留首页版摘要，完整复盘内容已独立到网站页：

- [项目沉淀与学习复盘](docs/site-pages/LEARNINGS.md)
- [项目驱动学习复盘指南](docs/core-links/SmartLive_Java_Internship_Review_Plan_Updated.md)

如果只看首页，我最想保留的一句话是：好项目不仅要能跑，还要能把边界、链路、取舍和复盘讲清楚。

---

## <a id="常见问题"></a>❓ [常见问题 FAQ](docs/site-pages/FAQ.md)

这里保留首页版最有用的 4 条，完整问答建议直接看：

- [常见问题 FAQ（网站详细版）](docs/site-pages/FAQ.md)

<details>
<summary><b>1. 这个仓库包含前端吗？前后端仓库分别是什么？</b></summary>
当前仓库主要是 **后端微服务主仓库**，负责用户、店铺、商品、订单、互动、搜索、AI、IM、审核、钱包等核心服务，以及中间件编排和部署脚本。  
前端仓库已单独拆分：

- `smartLive-admin`：后台管理端（Vue + Element UI）
- `smartLive-web`：用户端 App（Vue 移动端 / H5 页面）

对应仓库入口可以直接查看文档中的 [项目仓库](#项目仓库) 一节。
</details>

<details>
<summary><b>2. 第一次本地启动，最小需要哪些中间件和模块？</b></summary>
如果只是想先把系统跑起来并验证主链路，建议优先准备：

- **基础中间件**：MySQL、Redis、Nacos、RabbitMQ
- **核心服务**：Gateway、Auth、User、Shop、Product、Order、Blog、Interaction、Search
- **管理端 / 用户端前端**：按你的体验目标选择 `smartLive-admin` 或 `smartLive-web`

AI、Milvus、支付、IM、审核中心等能力可以放到第二阶段再补。更完整的接入顺序见 [docs/OPEN_SOURCE.md](docs/OPEN_SOURCE.md)。
</details>

<details>
<summary><b>3. AI 模块在这个项目里到底做了什么？</b></summary>
这个项目里的 AI 不只是“接一个聊天接口”，而是同时覆盖了 **用户端问答检索** 和 **商家端经营辅助** 两条线：

- **用户端**：支持店铺 / 商品 / 评价 / 博客问答、流式 AI 对话、探店博客生成、消费评价生成
- **商家端**：支持差评回复、经营建议、营销文案生成、经营分析
- **底层能力**：基于 Spring AI + Milvus，构建店铺、商品、评价、博客等多套向量检索能力，并支持不同 Agent 策略切换

所以它更像一个嵌入业务系统的 AI 中台，而不是单点聊天 Demo。
</details>

<details>
<summary><b>4. 为什么项目里同时用了 RabbitMQ 和 XXL-JOB？</b></summary>
两者职责不同，不是重复建设：

- **RabbitMQ**：负责异步解耦和准实时处理，例如订单创建、库存扣减、审核投递、消息推送、支付回调后的后续动作
- **XXL-JOB**：负责周期性扫描、补偿兜底和批处理，例如热榜重算、互动数据回刷、秒杀预热、订单超时处理、销量同步

可以把它理解成：MQ 解决“事件驱动”，XXL-JOB 解决“定时调度与补偿兜底”。两者配合起来，才能把实时性和最终一致性同时兼顾。
</details>

<br>


## <a id="未来规划"></a>🗺️ [未来规划 Roadmap](docs/site-pages/ROADMAP.md)

这里保留首页版两条核心方向，完整规划建议直接看：

- [未来规划 Roadmap（网站详细版）](docs/site-pages/ROADMAP.md)

- [ ] **性能监控体系闭环**：进一步将现有的监控体系集成 `Prometheus + Grafana`，打造全视角的系统资源消耗监控大盘。
- [ ] **自动化流水线 (CI/CD)**：在项目中集成完整的 GitHub Actions 或 GitLab CI/CD 流程，覆盖全链路线上的自动化单元测试与 Docker 镜像构建推送。


## <a id="项目仓库"></a>📦 项目仓库

| 仓库 | 说明 | 链接 |
|:---:|:---:|:---:|
| **smartLive-Cloud** | 后端微服务（本仓库） | [GitHub](https://github.com/mumulinya/smartLive-Cloud) |
| **smartLive-admin** | 后台管理端（Vue + Element UI） | [GitHub](https://github.com/mumulinya/smartLive-admin.git) |
| **smartLive-web** | 用户端 App（Vue 移动端 / H5 页面） | [GitHub](https://github.com/mumulinya/smartLive-web.git) |


## <a id="参与贡献"></a>🤝 参与贡献

我非常欢迎各种形式的贡献！无论是新功能、Bug 修复还是文档改进，都请随时提交。

在提交 PR 之前，建议先阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。其中补充了：

- 分支与提交信息约定
- 文档与代码统一使用 UTF-8 编码的约束
- 开发前自查清单与提交流程
- 安全相关变更的报告方式

提交信息格式仍建议保持：type(scope): subject

## <a id="开源协议"></a>📄 开源协议

本项目基于 [MIT License](LICENSE) 开源。


## <a id="联系我"></a>📞 联系我 - 找我聊技术

如果你想交流项目、源码设计或招聘机会，首页保留这 3 个入口就够了：

- **邮箱**：mumulinya167@gmail.com
- **GitHub Issues**：[mumulinya/smartLive-Cloud](https://github.com/mumulinya/smartLive-Cloud/issues)
- **GitHub Issues**：[mumulinya/smartLive-Cloud](https://github.com/mumulinya/smartLive-Cloud/issues)

更适合交流的话题：

- 微服务拆分与跨服务一致性
- Redis / MQ / 秒杀 / 热榜 等高并发链路
- AI/RAG 在真实业务里的落地方式
- Java 后端、实习/校招/社招机会

如果这个项目对你有帮助，欢迎给仓库一个 `Star`，或者直接发邮件告诉我你用到了哪一部分。

