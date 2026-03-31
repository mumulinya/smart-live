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

**文档导航：** [在线文档](https://mumulinya.github.io/smartLive-Cloud/) · [视觉导览](https://mumulinya.github.io/smartLive-Cloud/SHOWCASE) · [页面导览](https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY) · [开源接入](https://mumulinya.github.io/smartLive-Cloud/OPEN_SOURCE) · [提交 Issue](https://github.com/mumulinya/smartLive-Cloud/issues)

---

</div>

## <a id="项目仓库"></a>📦 项目仓库矩阵

| 仓库 | 说明 | 链接 |
|:---:|:---:|:---:|
| **smartLive-Cloud** | 后端主仓库与在线总文档（本仓库） | [GitHub](https://github.com/mumulinya/smartLive-Cloud) |
| **smartLive-web** | 用户端 App（Vue 3 / H5 页面） | [GitHub](https://github.com/mumulinya/smartLive-web.git) |
| **smartLive-admin** | 商家端与平台管理后台（Vue 2 + Element UI） | [GitHub](https://github.com/mumulinya/smartLive-admin.git) |

> 说明：仓库名沿用 `smartLive-web / smartLive-admin`，在线文档中则按业务角色分别展示为“用户端 App”“商家端 Web”“平台管理端 Web”。

## 👀 第一次看这个仓库，建议先从这里进入

这个 README 不只是“项目介绍”，更是你第一次阅读源码时的入口导航。  
如果你是第一次点进仓库，建议先按下面这张表选阅读路径，而不是直接在 `smartLive-modules/` 里盲翻。

| 你现在最想判断什么 | 先看哪里 | 再看哪里 | 为什么 |
|:---|:---|:---|:---|
| 项目是不是完整、是不是自己做的 | [项目全貌与答辩说明](https://mumulinya.github.io/smartLive-Cloud/PROJECT_OVERVIEW) | [系统架构与项目规模](https://mumulinya.github.io/smartLive-Cloud/site-pages/SYSTEM_ARCHITECTURE) | 先建立模块规模、服务边界、数据模型和文档资产认知 |
| 页面是不是对应真实业务，不是空壳 Demo | [页面导览](https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY) | [业务走查](https://mumulinya.github.io/smartLive-Cloud/SHOWCASE) | 先看用户端、商家端、平台管理端页面，再看业务流程 |
| 哪几条链路最值得读源码 | [核心链路总览](https://mumulinya.github.io/smartLive-Cloud/core-links/) | [性能压测与工程表现](https://mumulinya.github.io/smartLive-Cloud/site-pages/PERFORMANCE) | 先知道“读哪条链路最值”，再看这些设计为什么站得住 |
| 想直接把服务跑起来 | [开源接入说明](https://mumulinya.github.io/smartLive-Cloud/OPEN_SOURCE) | [部署说明](https://mumulinya.github.io/smartLive-Cloud/DEPLOYMENT_GUIDE) | 先补依赖矩阵、配置来源和启动顺序，避免一上来踩全套环境坑 |

### 🧭 GitHub 源码阅读入口

如果你更习惯直接从目录读代码，这几组入口最值得优先看：

| 想看什么能力 | 推荐先读的目录 / 模块 | 重点看什么 |
|:---|:---|:---|
| 统一入口、安全与双端鉴权 | [`smartLive-gateway`](./smartLive-gateway) · [`smartLive-auth`](./smartLive-auth) | Gateway 过滤链、后台 JWT 与 App Token 的入口分流 |
| 店铺、商品、订单、支付与退款 | [`smartLive-modules/smartLive-order`](./smartLive-modules/smartLive-order) · [`smartLive-modules/smartLive-product`](./smartLive-modules/smartLive-product) · [`smartLive-modules/smartLive-wallet`](./smartLive-modules/smartLive-wallet) | 下单、统一支付、退款补偿、订单过期与钱包回滚 |
| 博客、评价、评论、点赞、关注与 Feed | [`smartLive-modules/smartLive-blog`](./smartLive-modules/smartLive-blog) · [`smartLive-modules/smartLive-interaction`](./smartLive-modules/smartLive-interaction) · [`smartLive-modules/smartLive-review`](./smartLive-modules/smartLive-review) | 互动策略工厂、Feed 滚动分页、审核流转与副本同步 |
| 搜索、热榜、LBS 与向量检索 | [`smartLive-modules/smartLive-search`](./smartLive-modules/smartLive-search) · [`smartLive-modules/smartLive-shop`](./smartLive-modules/smartLive-shop) | ES 检索、Milvus 降级、热门词与热榜维护 |
| AI 对话、推荐卡片与经营助手 | [`smartLive-modules/smartLive-ai`](./smartLive-modules/smartLive-ai) | Agent Router、意图识别、RAG 检索、商家助手与 AIGC |
| 实时聊天、会话与通知 | [`smartLive-modules/smartLive-im`](./smartLive-modules/smartLive-im) · [`smartLive-modules/smartLive-chat`](./smartLive-modules/smartLive-chat) | Netty 长连接、消息持久化、会话聚合与系统通知 |

> 如果你是因为 `Spring AI / RAG` 点进来的，建议先看 [AI Agent 策略路由与 RAG 多维增强生成链路](https://mumulinya.github.io/smartLive-Cloud/core-links/8.%20AI%20Agent策略路由与RAG多维增强生成链路)。这页现在单独补了“业务数据 -> MQ -> Milvus”“用户问题 -> Agent / Tool / RAG / SSE”，以及“控制 JSON -> 后端回填真实数据”这 3 条关键图，先建立心智模型，再回头读 `smartLive-modules/smartLive-ai` 会顺很多。

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
- [👀 第一次看这个仓库](#第一次看这个仓库建议先从这里进入)
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

- [项目全貌与答辩说明](https://mumulinya.github.io/smartLive-Cloud/PROJECT_OVERVIEW)

### <a id="项目规模"></a>📏 项目规模

| 维度 | 数值 | 说明 |
|------|------|------|
<!-- AUTO_SYNC:README_PROJECT_SCALE:START -->
| 核心业务模块 | **16 个** | 用户、店铺、商品、订单、博客、互动、搜索、AI、IM 等核心业务模块 |
| 可运行服务应用 | **19 个** | 16 个业务模块 + `auth` + `gateway` + `monitor` |
| 核心能力域 | **6 大类** | 发现推荐、交易履约、社交互动、内容治理、经营分析、基础设施 |
| 端侧覆盖 | **3 类前端入口** | 用户端 App + 商家端 Web + 平台管理端 Web |
| 核心业务代码 | **30,000+ 行** | 以服务端业务逻辑为主 |
| 文档与图示资产 | **70+ 链路图 / 100+ 截图** | 当前文档站已沉淀 73 张链路图、102 张引用截图与 11 篇专题页 |
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

### 🗓️ 项目开发时间线（6 个月）

按当前仓库的 Git 提交记录粗粒度回看，这个项目大致经历了下面 6 个阶段：

| 阶段 | 时间 | 主要完成内容 |
|------|------|------|
| 第 1 月 | 2025.10 | 搭起基础工程与用户主链路，完成登录、首页、地图、搜索、私信与 AI 评论 / 下单雏形 |
| 第 2 月 | 2025.11 | 引入 RabbitMQ、死信队列、线程池与 ES 同步策略，补齐首页聚合与异步解耦能力 |
| 第 3 月 | 2025.12 | 合并互动域，抽象点赞 / 收藏 / 评论 / 关注策略模式，升级到 Boot 3.2.2，并把 AI 工程并回主项目 |
| 第 4 月 | 2026.01 | 重构为 Netty IM，完善代金券 / 订单 / 动态推送 / 审核中心等主业务骨架 |
| 第 5 月 | 2026.02 | 深化 AI 会话与审核链路，重构缓存体系、评论评价系统、XXL-JOB 同步与热榜 / 搜索策略 |
| 第 6 月 | 2026.03 | 完成订单过期与自动退款、MQ 幂等与重试补偿、商家 AI 助手，并沉淀完整文档站与链路图资产 |

如果你准备从“项目是不是一步步做起来的”这个角度判断它，推荐直接看 [项目全貌与答辩说明](https://mumulinya.github.io/smartLive-Cloud/PROJECT_OVERVIEW) 里的时间线讲解版。

### <a id="个人贡献亮点"></a>👨‍💻 [个人贡献亮点（本项目核心设计与实现）](https://mumulinya.github.io/smartLive-Cloud/site-pages/CONTRIBUTIONS)

这里只保留首页版摘要，完整展开建议直接看：

- [我的核心设计与实现](https://mumulinya.github.io/smartLive-Cloud/site-pages/CONTRIBUTIONS)

先看交付密度与 6 条最强个人贡献：

**交付规模：**
<!-- AUTO_SYNC:README_CONTRIBUTION_SCALE:START -->
- 🔸 核心业务代码：**30,000+ 行**（覆盖 16 个业务模块与完整服务应用骨架）
- 🔸 文档与图示资产：**完整在线文档 + 业务链路图 + 真实页面截图**（便于讲解、复盘与源码阅读）
- 🔸 项目周期：**6 个月**（从零开始独立完成全栈设计与开发）
- 🔸 Git 提交记录：**200+** 条（完整的开发足迹可追溯）
<!-- AUTO_SYNC:README_CONTRIBUTION_SCALE:END -->

- **Redis 分层缓存架构**：把列表、详情、计数三类读链拆成不同缓存结构，首页、博客、商品、店铺等高频场景整体提速约 `20-40 倍`。
- **微服务拆分与跨服务协同**：独立完成 `16` 个业务模块与 `19` 个服务应用的边界设计，落地 Feign、RabbitMQ 的跨服务协同闭环，并预留 Seata 强一致治理基础设施。
- **高并发交易链路优化**：秒杀链路采用 `Lua + MQ + 延迟补偿`，在 `5000` 并发线程下把 QPS 稳定在 `3200+`。
- **热榜评分与异步洗牌**：抽象 5 类业务热榜策略，结合时间衰减、互动权重、增量重算与凌晨全量重建解决冷启动和长期霸榜。
- **消息可靠投递与幂等消费**：统一封装发送端 Confirm / Return、消费端 Redis 幂等和手动 ACK/NACK，把重复消费和补偿边界收口。
- **用户端 AI 与 AIGC 闭环**：打通 SSE 对话、RAG 检索、推荐卡片、博客生成、评价生成等用户侧可感知能力。

### <a id="核心亮点"></a>🎯 [核心亮点](https://mumulinya.github.io/smartLive-Cloud/site-pages/CORE_HIGHLIGHTS)

这里只保留项目级能力摘要，完整版本建议直接看：

- [项目级能力亮点](https://mumulinya.github.io/smartLive-Cloud/site-pages/CORE_HIGHLIGHTS)

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
| 快速判断项目值不值得继续看 | [README.md](README.md) | [视觉导览](https://mumulinya.github.io/smartLive-Cloud/SHOWCASE) | 先看清业务范围、系统架构、真实页面和核心链路 |
| 想先理解模块拆分和代码边界 | [系统架构](#系统架构) | [项目结构](#项目结构) | 先建立全局心智模型，再回头读具体业务模块 |
| 第一次把服务跑起来 | [开源接入说明](https://mumulinya.github.io/smartLive-Cloud/OPEN_SOURCE) | [README.md 的快速开始](#快速开始) | 先跑最小可运行链路，避免一次性踩完全部依赖坑 |
| 体验 AI / 搜索 / 审核 / 积分全链路 | [开源接入说明](https://mumulinya.github.io/smartLive-Cloud/OPEN_SOURCE) | [视觉导览](https://mumulinya.github.io/smartLive-Cloud/SHOWCASE) | 先补齐依赖矩阵，再对照截图和业务链路逐项验证 |
| 准备本地部署或服务器演示 | [部署说明](https://mumulinya.github.io/smartLive-Cloud/DEPLOYMENT_GUIDE) | [SECURITY.md](SECURITY.md) | 明确 Docker、构建产物、配置注入与部署排错边界 |

> 推荐起步顺序：`smartLive-auth -> smartLive-gateway -> smartLive-system -> smartLive-user -> smartLive-shop -> smartLive-search`。先验证登录、店铺、搜索和后台管理，再补 AI、IM、积分、支付等进阶能力。


## <a id="效果预览"></a>🎨 效果预览

README 这里只保留最主要的 App 端核心页面，帮助你快速建立第一印象；完整页面请直接看 [PAGE_GALLERY](https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY)。

### 1. 登录与身份进入

<p align="center">
  <a href="https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY#app-login-entry"><img src="docs/screenshots/login-page.png" alt="login" width="300"></a>
</p>

登录页负责建立账号入口和用户身份，是第一次进入 App 的起点。  
[查看登录页与账号入口](https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY#app-login-entry)

### 2. 首页入口与热榜榜单

<p align="center">
  <a href="https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY#app-discovery"><img src="docs/screenshots/homepage.png" alt="home-hot" width="300"></a>
</p>

首页承接分类入口、热门内容流和榜单跳转，是用户发现内容和店铺的主入口。  
[查看首页入口、热门内容与热榜榜单](https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY#app-discovery)

### 3. 搜索与地图找店

<p align="center">
  <a href="https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY#app-search-map"><img src="docs/screenshots/map-view.png" alt="map" width="300"></a>
</p>

搜索页支持关键词、热词和位置感知，地图模式进一步放大了附近找店体验。  
[查看搜索结果与 LBS 找店](https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY#app-search-map)

### 4. 店铺与商品详情

<p align="center">
  <a href="https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY#app-shop-product"><img src="docs/screenshots/shop-detail.png" alt="shop" width="300"></a>
</p>

详情页承接店铺介绍、商品购买、评价查看和内容互动，是下单前的核心决策页。  
[查看店铺页、商品页与内容发布](https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY#app-shop-product)

### 5. 社交消息与即时通讯

<p align="center">
  <a href="https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY#app-social-message"><img src="docs/screenshots/app-chat-detail.png" alt="im" width="300"></a>
</p>

这里集中展示了会话列表、系统通知和私聊消息，是社交互动与消息流转的主要承接页。  
[查看动态、消息中心与 IM](https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY#app-social-message)

### 6. AI 与订单资产

<p align="center">
  <a href="https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY#app-ai-capability"><img src="docs/screenshots/app-ai-order-card.png" alt="ai-order" width="300"></a>
</p>

AI 页不只负责对话，还能返回推荐卡片、生成内容，并直接承接下单结果和订单跳转。  
[查看 AI 对话、会话与 AIGC](https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY#app-ai-capability)

### 🧩 更多页面入口

- 完整用户端 App 页面：看 [PAGE_GALLERY - 用户端 App](https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY#app-pages)
- 完整管理端 Web 页面与 XXL-JOB 后台：看 [PAGE_GALLERY - 管理端 Web](https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY#admin-pages)
- 按业务链路看截图、架构图和时序图：看 [SHOWCASE](https://mumulinya.github.io/smartLive-Cloud/SHOWCASE)


## <a id="快速开始"></a>🚀 快速开始

README 首页只保留最小可运行链路，完整启动顺序、端口表、依赖矩阵和部署方式请直接看：

- [开源接入说明](https://mumulinya.github.io/smartLive-Cloud/OPEN_SOURCE)
- [部署说明](https://mumulinya.github.io/smartLive-Cloud/DEPLOYMENT_GUIDE)

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

- 本地完整接入：看 [开源接入说明](https://mumulinya.github.io/smartLive-Cloud/OPEN_SOURCE)
- Docker Compose、增量部署脚本、Windows 启动脚本：看 [部署说明](https://mumulinya.github.io/smartLive-Cloud/DEPLOYMENT_GUIDE)

## <a id="项目文档"></a>📚 项目文档

如果你已经看完上半部分，这里只需要记住 6 个最有用的文档入口：

| 你接下来要做什么 | 直接看哪里 |
|:---|:---|
| 看完整在线文档 | [在线文档网站](https://mumulinya.github.io/smartLive-Cloud/) |
| 看所有页面和截图 | [页面导览](https://mumulinya.github.io/smartLive-Cloud/PAGE_GALLERY) |
| 按业务流程看页面和链路 | [视觉导览](https://mumulinya.github.io/smartLive-Cloud/SHOWCASE) |
| 看系统规模、服务边界、数据模型 | [系统架构与项目规模](https://mumulinya.github.io/smartLive-Cloud/site-pages/SYSTEM_ARCHITECTURE) |
| 看接入和启动顺序 | [开源接入说明](https://mumulinya.github.io/smartLive-Cloud/OPEN_SOURCE) |
| 看 Docker 和部署排错 | [部署说明](https://mumulinya.github.io/smartLive-Cloud/DEPLOYMENT_GUIDE) |

## <a id="功能特性"></a>✨ 功能特性

GitHub 首页只保留一句话版本，完整矩阵看 [功能特性与技术栈](https://mumulinya.github.io/smartLive-Cloud/site-pages/FEATURES_STACK)。

当前项目可以先概括成 4 组能力：

- 交易闭环：商品、订单、支付、退款、积分、钱包、秒杀
- 内容社交：博客、评论、评价、关注、Feed、IM、系统通知
- 搜索推荐：LBS 搜索、热词、热榜、ES / Milvus 双副本收敛
- 平台治理：审核中心、认证网关、监控中心、XXL-JOB 调度

## <a id="技术栈"></a>🔧 技术栈

首页只保留技术轮廓，完整对比请看 [技术选型理由](https://mumulinya.github.io/smartLive-Cloud/site-pages/TECH_SELECTION)。

- 服务基础：Spring Boot 3.2.2 + Spring Cloud 2023 + Spring Cloud Alibaba 2023.0.1.0
- 治理组件：Nacos、Gateway、Sentinel、Seata、XXL-JOB
- 数据与缓存：MySQL、Redis、Elasticsearch、Milvus、MinIO
- 异步与实时：RabbitMQ、Netty + WebSocket
- AI 与文档：Spring AI、SpringDoc OpenAPI、UniApp、Vue

## <a id="系统架构"></a>🏗️ 系统架构

GitHub 首页只保留一张总图，详细讲法直接看 [系统架构与项目规模](https://mumulinya.github.io/smartLive-Cloud/site-pages/SYSTEM_ARCHITECTURE)。

<div align="center">
  <img src="docs/diagrams/system-architecture-overview.svg" alt="SmartLive 系统架构图" width="100%">
</div>

先记住这 3 个点：

- `Gateway + Auth` 统一承接入口层鉴权与流量治理
- 业务上按基础服务、交易履约、内容社交、搜索智能治理四个服务簇拆分
- 基础设施由 `MySQL / Redis / RabbitMQ / Elasticsearch / Milvus / MinIO / Nacos / XXL-JOB` 协同支撑

## <a id="项目结构"></a>📁 项目结构

这里只保留源码阅读主干，详细结构说明请看 [系统架构与项目规模](https://mumulinya.github.io/smartLive-Cloud/site-pages/SYSTEM_ARCHITECTURE)。

```text
smartLive-auth / smartLive-gateway / smartLive-monitor
smartLive-modules/*                  # 16 个业务模块
smartLive-api/*                      # 10 个 API 契约模块
smartLive-common/*                   # 11 个通用基础组件
docs/                                # 在线文档、链路图、页面导览
sql/                                 # 建库、业务库、配置库、调度库脚本
```

第一次读源码，建议先从 `auth -> gateway -> user -> shop -> search -> order` 这条主干开始。

## <a id="技术选型理由"></a>🤔 [技术选型理由](https://mumulinya.github.io/smartLive-Cloud/site-pages/TECH_SELECTION) - 为什么选这些而不是其他？

首页只保留 5 个判断，详细对比在网站详细版里。

1. `Spring Boot + Spring Cloud Alibaba`：单服务开发底座和微服务治理分层清楚，适合当前 `19` 个服务应用规模。
2. `Nacos + Gateway + Sentinel + Seata`：注册、配置、统一入口与限流熔断能力已经接齐，`Seata` 作为强一致链路的基础设施预留，后续主要用于余额支付、退款与券状态流转等场景。
3. `Redis + Elasticsearch + Milvus + MinIO`：分别承接缓存、搜索、副本检索和对象存储，不强行让一种技术做所有事情。
4. `RabbitMQ + XXL-JOB`：MQ 负责事件异步，XXL-JOB 负责定时扫描、补偿兜底和批量重建。
5. `Spring AI + Netty + MyBatis Plus`：分别对应 AI 编排、IM 长连接和可控的数据访问层。


## <a id="核心业务链路"></a>🌊 核心业务链路

第一次读源码，最值得先追的 4 条链路是：

- [秒杀抢购全链路](https://mumulinya.github.io/smartLive-Cloud/core-links/%E7%A7%92%E6%9D%80%E6%8A%A2%E8%B4%AD%E5%85%A8%E9%93%BE%E8%B7%AF%E8%AF%A6%E8%A7%A3)
- [订单支付与退款补偿](https://mumulinya.github.io/smartLive-Cloud/core-links/2.%20%E4%B8%8B%E5%8D%95_%E7%BB%9F%E4%B8%80%E6%94%AF%E4%BB%98_%E9%80%80%E6%AC%BE%E8%A1%A5%E5%81%BF%E9%93%BE%E8%B7%AF)
- [Redis 分层缓存链路](https://mumulinya.github.io/smartLive-Cloud/core-links/Redis%E5%88%86%E5%B1%82%E7%BC%93%E5%AD%98%E9%93%BE%E8%B7%AF%E8%AF%A6%E8%A7%A3)
- [发布审核与搜索 / 向量同步](https://mumulinya.github.io/smartLive-Cloud/core-links/7.%20%E5%8F%91%E5%B8%83_%E5%AE%A1%E6%A0%B8_%E6%90%9C%E7%B4%A2_Milvus_%E5%85%A8%E9%93%BE%E8%B7%AF)

完整链路图集请直接看 [业务链路视觉走查](https://mumulinya.github.io/smartLive-Cloud/SHOWCASE) 或 [核心链路总览](https://mumulinya.github.io/smartLive-Cloud/core-links/)。


## <a id="核心链路与面经"></a>🔥 核心链路与面经

如果你准备拿这个项目做面试讲解，建议顺序是：

1. [项目全貌与答辩说明](https://mumulinya.github.io/smartLive-Cloud/PROJECT_OVERVIEW)
2. [系统架构与项目规模](https://mumulinya.github.io/smartLive-Cloud/site-pages/SYSTEM_ARCHITECTURE)
3. [核心链路总览](https://mumulinya.github.io/smartLive-Cloud/core-links/)
4. [项目驱动学习复盘指南](https://mumulinya.github.io/smartLive-Cloud/core-links/SmartLive_Java_Internship_Review_Plan_Updated)


## <a id="开源使用提示"></a>📌 开源使用提示

第一次接项目，记住这 4 条就够了：

- 先跑最小链路，不要一上来把 AI、支付、审核、IM 一次性全开。
- 先理解 `MySQL 是真实源，ES / Milvus / Redis 是查询副本`，再去看搜索、热榜和向量检索。
- 先抓住缓存分层、策略工厂、责任链、MQ + 调度补偿这几个核心设计点，再读具体模块。
- AI、对象存储、支付、向量检索都依赖外部配置，第一次阅读更适合先看链路和接口，再补齐环境。

完整接入说明见 [开源接入说明](https://mumulinya.github.io/smartLive-Cloud/OPEN_SOURCE)，安全边界见 [SECURITY.md](SECURITY.md)。

## <a id="性能压测报告"></a>📈 [性能压测报告](https://mumulinya.github.io/smartLive-Cloud/site-pages/PERFORMANCE)

如果你只看首页，先记住这 3 个结论：

- 秒杀抢购链路在 `5000` 并发线程下，QPS 保底 `3200+`
- 列表类查询通过 `Redis ZSet + 批量回填`，相比直查数据库提升约 `20-40 倍`
- 高并发写链路尽量走 `Lua + MQ + 定时补偿`，避免直接把压力打到主库

完整压测表和讲法看 [性能压测与工程表现](https://mumulinya.github.io/smartLive-Cloud/site-pages/PERFORMANCE)。

## <a id="难点踩坑与解决方案"></a>🚧 [难点踩坑与解决方案](https://mumulinya.github.io/smartLive-Cloud/site-pages/PITFALLS)

首页只保留 4 个最值得讲的坑：

1. `缓存与一致性`：高频写场景下，用 `Redis Hash 增量更新 + RENAME 快照归档 + 批量落库` 把读写路径拆开。
2. `交易与补偿`：秒杀和订单链路通过 `Lua + RabbitMQ 延迟队列 + 状态校验` 处理超卖、少卖和重复补偿。
3. `消息与最终一致性`：发送端 Confirm / Return、消费端 Redis 幂等、手动 ACK/NACK、延迟 / 死信队列一起闭环。
4. `推荐与社交`：Feed 用滚动分页解决数据偏移，热榜用增量洗牌 + 全量重建解决冷启动和长期霸榜。

完整踩坑复盘见 [网站详细版](https://mumulinya.github.io/smartLive-Cloud/site-pages/PITFALLS)。

## <a id="项目沉淀"></a>🧠 [我通过这个项目学到的东西](https://mumulinya.github.io/smartLive-Cloud/site-pages/LEARNINGS)

如果只保留一句话，我最想强调的是：  
**好项目不仅要能跑，还要能把边界、链路、取舍和复盘讲清楚。**

完整复盘看：
- [项目沉淀与学习复盘](https://mumulinya.github.io/smartLive-Cloud/site-pages/LEARNINGS)
- [项目驱动学习复盘指南](https://mumulinya.github.io/smartLive-Cloud/core-links/SmartLive_Java_Internship_Review_Plan_Updated)

---

## <a id="常见问题"></a>❓ [常见问题 FAQ](https://mumulinya.github.io/smartLive-Cloud/site-pages/FAQ)

首页版 FAQ 我压成 4 条短答案，完整问答看 [网站详细版](https://mumulinya.github.io/smartLive-Cloud/site-pages/FAQ)。

| 问题 | 首页版回答 |
|:---|:---|
| 这个仓库包含前端吗？ | 当前仓库是后端主仓库；前端已拆成 `smartLive-admin` 和 `smartLive-web` 两个独立仓库。 |
| 第一次本地启动最小需要什么？ | 先准备 `MySQL + Redis + Nacos + RabbitMQ`，优先跑 `auth -> gateway -> system -> user -> shop -> search`。 |
| AI 在项目里做了什么？ | 不只是聊天，而是覆盖用户端问答检索、AIGC 和商家端经营助手的一套 AI 中台能力。 |
| 为什么同时用了 MQ 和 XXL-JOB？ | MQ 负责事件异步，XXL-JOB 负责定时扫描、补偿兜底和批处理，两者职责不同。 |


## <a id="未来规划"></a>🗺️ [未来规划 Roadmap](https://mumulinya.github.io/smartLive-Cloud/site-pages/ROADMAP)

首页只保留 3 个方向，完整规划看 [网站详细版](https://mumulinya.github.io/smartLive-Cloud/site-pages/ROADMAP)。

- [ ] **可观测性闭环**：补齐 Prometheus + Grafana + 告警与排障链路。
- [ ] **CI / CD 与质量保障**：把测试、构建、镜像与部署流程进一步标准化。
- [ ] **强一致链路深化**：未来在余额支付、退款与券状态流转等强一致业务上引入更严格的一致性治理。


## <a id="参与贡献"></a>🤝 参与贡献

我非常欢迎各种形式的贡献！无论是新功能、Bug 修复还是文档改进，都请随时提交。

在提交 PR 之前，建议先阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。其中补充了：

- 分支与提交信息约定
- 文档与代码统一使用 UTF-8 编码的约束
- 开发前自查清单与提交流程
- 安全相关变更的报告方式

提交信息格式仍建议保持：type(scope): subject

## <a id="开源协议"></a>📄 开源协议

本项目基于 [MIT License](LICENSE) 开源，可用于学习、参考、修改与二次开发；如基于本项目继续分发或演进，请保留原始版权声明与许可说明。


## <a id="联系我"></a>📞 联系我 - 找我聊技术

如果你想交流项目、源码设计或招聘机会，首页保留这两个入口就够了：

- **邮箱**：mumulinya167@gmail.com
- **GitHub Issues**：[mumulinya/smartLive-Cloud](https://github.com/mumulinya/smartLive-Cloud/issues)

更适合交流的话题：

- 微服务拆分与跨服务一致性
- Redis / MQ / 秒杀 / 热榜 等高并发链路
- AI/RAG 在真实业务里的落地方式
- Java 后端、实习/校招/社招机会

如果这个项目对你有帮助，欢迎给仓库一个 `Star`，或者直接发邮件告诉我你用到了哪一部分。

