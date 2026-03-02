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
[![Personal Project](https://img.shields.io/badge/个人独立项目-从零设计开发-ff69b4.svg)]()

[在线文档](http://doc.smartLive.vip) · [演示地址](http://www.smartLive.vip) · [提交 Issue](https://gitee.com/mumulinya/smart-live/issues)

---

</div>

## 📋 目录

- [📖 项目简介](#项目简介)
- [🏗️ 系统架构](#系统架构)
- [🌊 核心业务链路](#核心业务链路)
- [✨ 功能特性](#功能特性)
- [🎨 效果预览](#效果预览)
- [🔧 技术栈](#技术栈)
- [🚀 快速开始](#快速开始)
- [📈 性能压测报告](#性能压测报告)
- [🚧 难点踩坑与解决方案](#难点踩坑与解决方案)
- [❓ 常见问题 FAQ](#常见问题)
- [📁 项目结构](#项目结构)
- [🗺️ 未来规划 Roadmap](#未来规划)
- [📦 项目仓库](#项目仓库)
- [📚 项目文档](#项目文档)
- [🤝 参与贡献](#参与贡献)
- [📄 开源协议](#开源协议)
- [📞 联系我](#联系我)

---

## <a id="项目简介"></a>📖 项目简介

> 🙋 **个人独立项目声明**：本项目从零开始由作者**个人独立设计、编码并持续维护**，
> 非团队协作或培训项目，有完整 Git 提交记录，可现场代码走查。

**SmartLive（智评生活）** 是一个面向本地生活服务的多端智慧商户平台，旨在解决本地商户引流难、用户决策复杂等痛点。平台提供 **商户展示、AI 智能推荐、社交互动、即时通讯、营销下单、内容安全** 六大核心功能，采用微服务架构拆分 **18+ 业务模块**，支持高并发、实时通信与个性化用户体验。

### 🎯 核心亮点

#### 🤖 AI 增强与智能中枢 (AI & Intelligent Brain)
- **多意图路由引擎**：基于关键词正则与自定义 `ChatHandler` 实现 **5 大意图识别路由**（店铺搜索、商品查询、评论分析等）。通过结构化 Prompt 工程与 **Spring AI** 深度集成，支持 SSE 流式响应与 UI 卡片式交互。
- **RAG 向量检索闭环**：深度打通 **Milvus** 向量库，实现业务数据跨维度的向量召回，支持结合 **Geo-Distance** 地理围栏过滤的高精度 RAG 增强检索。
- **AIGC 自动化治理**：集成 AI 自动生成店铺评价功能，通过 `AIGenerated` 标记实现内容真伪管理，赋能商户冷启动。

#### ⚡ 极致性能与交易引擎 (High Performance & Transaction)
- **高性能动态 Feed 流**：采用 **“推拉结合”** 模型与 **Redis ZSet 滚动分页（Scroll Result）** 机制。完美解决传统分页在社交场景下的“数据偏移”问题，支持百万级粉丝的毫秒级写扩散。
- **极致秒杀与延迟队列**：利用 **Redis Lua 原子脚本** 实现“库存预扣 + 一人一单”强一致性校验。结合 **RabbitMQ 死信/延迟队列** 实现 15 分钟未支付订单自动取消与库存精准回滚。
- **深度模式解耦设计**：全项目极度推崇面向对象设计。互动模块采用 **策略与工厂模式** 高度抽象点赞、收藏等 7 大业务域；钱包中心则封装 **PaymentStrategy** 实现微信/支付宝等多支付渠道的安全路由与优雅解耦。

#### 🛡️ 企业级边界防御与微服务基座 (Security & Infrastructure)
- **Gateway 全局安全屏障**：在 Spring Cloud Gateway 层手写 `AuthFilter`（无状态鉴权）、`XssFilter`（防跨站脚本）与 `BlackListUrlFilter`（非法拦截），在流量最前线构筑严密的安全防线。
- **严苛的资源隔离与同步**：采用自定义受控的 `ThreadPoolExecutor` 实现核心链路的线程舱壁隔离；在极端高并发及定时调度场景中，引入 **Redisson 看门狗机制** 构建高可用分布式锁。
- **分布式 IM 与 6 维审核链**：基于 **Netty** 实现 WebSocket 长连接集群与分布式 Session；采用 **责任链工厂模式** 搭配 DFA 敏感词引擎构建覆盖 6 大业务线的异步审核流水线。
- **跨库数据最终一致性**：组合应用 **Seata AT 模式**、MQ 消息可靠投递以及 XXL-JOB 定时对账任务，犹如“塔防”般确保 MySQL、Elasticsearch、Redis 与 Milvus 四端数据的绝对一致。


## <a id="系统架构"></a>🏗️ 系统架构

<div align="center">
  <img src="docs/screenshots/architecture.png" alt="SmartLive 系统架构图" width="100%">
</div>


## <a id="核心业务链路"></a>🌊 核心业务链路

### 1. 秒杀抢购全链路时序图
<div align="center">
  <img src="docs/diagrams/seckill-flow.png" alt="秒杀抢购全链路时序图" width="100%">
</div>

### 2. UGC 异步审核与分发链路
<div align="center">
  <img src="docs/diagrams/ugc-audit-flow.png" alt="UGC 异步审核与分发链路" width="100%">
</div>

### 3. 统一支付全链路时序图
<div align="center">
  <img src="docs/diagrams/unified-pay-sequence.png" alt="统一支付全链路时序图" width="100%">
</div>

### 4. 每日签到积分链路
<div align="center">
  <img src="docs/diagrams/daily-signin-points-sequence.png" alt="每日签到积分链路" width="100%">
</div>

### 5. 积分抽奖链路
<div align="center">
  <img src="docs/diagrams/points-lottery-draw-sequence.png" alt="积分抽奖链路" width="100%">
</div>

### 6. IM 私聊消息可靠投递链路
<div align="center">
  <img src="docs/diagrams/im-private-message-reliable-delivery-sequence.png" alt="IM 私聊消息可靠投递链路" width="100%">
</div>

### 7. Feed 动态扇出链路
<div align="center">
  <img src="docs/diagrams/feed-fanout-sequence.png" alt="Feed 动态扇出链路" width="100%">
</div>

### 8. 互动数据"双轨同步"链路
<div align="center">
  <img src="docs/diagrams/interaction-dual-track-sync-sequence.png" alt="互动数据双轨同步链路" width="100%">
</div>

### 9. 搜索与向量库同步链路 (ES + Milvus)
<div align="center">
  <img src="docs/diagrams/search-es-milvus-sync-sequence.png" alt="搜索与向量库同步链路" width="100%">
</div>

### 10. 普通下单链路 (非秒杀)
<div align="center">
  <img src="docs/diagrams/normal-order-sequence.png" alt="普通下单链路" width="100%">
</div>

### 11. AI 对话链路 (SSE + 意图路由 + 卡片事件)
<div align="center">
  <img src="docs/diagrams/ai-chat-sse-intent-routing-sequence.png" alt="AI 对话链路" width="100%">
</div>


## <a id="功能特性"></a>✨ 功能特性

### 🛍️ 业务功能

#### 👤 用户中心 (smartLive-user) [9201]
| 功能 | 说明 |
|:---|:---|
| 用户管理 | 用户 CRUD、列表查询、用户导出 |
| 个人信息 | 城市/简介/性别/生日/背景图独立更新接口 |
| 账号安全 | 密码设置与修改、账号状态管理 |
| 用户统计 | 粉丝数/关注数/博客数等统计数据聚合 |
| 数据同步 | MQ 异步同步用户数据至 ES / Milvus |

#### 🏪 店铺管理 (smartLive-shop) [9203]
| 功能 | 说明 |
|:---|:---|
| 店铺 CRUD | 店铺新增、修改、删除、查询 |
| 缓存三策略 | 空值防穿透 + 逻辑过期防击穿 + 互斥锁防缓存崩溃 |
| 热门榜单 | 基于 Redis ZSet 的热度排行 + 坐标距离排序 |
| 分层缓存 | 详情层逻辑过期 + 列表层 ZSet ID 索引批量回查 |
| 审核流程 | 店铺发布自动触发 MQ 异步审核 |
| 流量控制 | Sentinel 限流保护热点接口 |
| 数据同步 | MQ 异步同步店铺数据至 ES / Milvus |

#### � 搜索引擎 (smartLive-search) [9204]
| 功能 | 说明 |
|:---|:---|
| 全文搜索 | 基于 Elasticsearch 的 multi_match 全局检索 + 关键词高亮 |
| 多策略排序 | 距离/热度/价格/评分四种排序策略工厂 |
| 行为搜索 | 个人中心 user_resource_index 检索点赞/收藏/关注/发布数据 |
| 搜索历史 | Redis ZSet 管理用户搜索历史（保留最近 10 条） |
| 热词排行 | Redis ZSet ZINCRBY 统计热门搜索关键词 |
| 索引同步 | MQ 异步将商品/店铺/博客/用户行为变更同步至 ES |

#### �🛒 订单管理 (smartLive-order) [9205]
| 功能 | 说明 |
|:---|:---|
| 统一下单 | 普通购买 + 秒杀下单，策略模式路由 |
| 订单查询 | 我的订单列表、订单状态追踪 |
| 订单支付 | 对接钱包服务，支付回调更新状态 |
| 自动过期 | XXL-JOB 每日扫表扫描过期订单，支持临期提醒与过期自动退款/作废 |
| 超时取消 | RabbitMQ 延迟队列自动取消超时未支付订单 |
| 订单核销 | 线下消费确认、核销状态更新 |
| 退款处理 | 订单退款申请、退款状态追踪 |
| 订单导出 | 订单数据导出 Excel |

#### 📦 商品管理 (smartLive-product) [9206]
| 功能 | 说明 |
|:---|:---|
| 商品CRUD | 商品新增、修改、删除、查询 |
| 商品管理 | 商品上下架、库存管理、降价通知 |
| 秒杀抢购 | Redis Lua 原子防超卖与一人一单，独立 XXL-JOB 提供秒杀预热、临期提醒、到期自动回收全生命周期管理 |
| 热门榜单 | 基于 Redis ZSet 的代金券/团购热榜分页 |
| 分层缓存 | 逻辑过期 + 空值防穿透 + ZSet ID 索引批量回查 |
| 内容审核 | 商品发布自动触发 MQ 异步审核流程 |
| Feed 推送 | 上新/降价/补货/上下架事件推送至粉丝动态 |

#### � 社交互动 (smartLive-interaction) [9207]
| 功能 | 说明 |
|:---|:---|
| 策略工厂 | 7 个策略工厂统一点赞/收藏/评论/评价/关注/热榜/资源处理流程 |
| 点赞/收藏 | 按 sourceType 动态路由，Redis 计数 + 脏标记异步落库 |
| 评论/评价 | 多级评论、商品/店铺评价，独立策略体系 |
| 关注体系 | 用户/店铺/商品关注，Redis ZSet 管理关注/粉丝列表，共同关注交集查询 |
| Feed 流 | 推模式写入粉丝分类 Feed + 全量 Feed ZSet，ZREVRANGEBYSCORE 滚动分页 |
| 热榜排行 | 5 种业务类型（Blog/Shop/Product/Review/Comment）热度排行，增量重算 + 全量重建 |
| 数据同步 | XXL-JOB 定时任务，Redis RENAME 原子快照批量回刷计数至 MySQL |

#### 🏠 首页聚合 (smartLive-index) [9208]
| 功能 | 说明 |
|:---|:---|
| 热门推荐 | 热门内容聚合展示 |
| 资源聚合 | 热门店铺、博客、用户推荐 |
| 数据聚合 | 统一首页数据服务 |

#### 📁 文件服务 (smartLive-file) [9209]
| 功能 | 说明 |
|:---|:---|
| 文件上传 | MinIO对象存储集成 |
| 文件下载 | 文件下载与预览 |
| 头像管理 | 用户头像上传与存储 |
| 图片处理 | 图片存储与CDN分发 |

#### 💬 即时通讯 (smartLive-chat + smartLive-im) [9210 / 9214]
| 功能 | 说明 |
|:---|:---|
| Netty 长连接 | WebSocket 服务端，双线程组 + 心跳保活 |
| 身份认证 | Redis Token 校验完成 WebSocket 鉴权 |
| 私聊消息 | 用户一对一私信，Feign 持久化 + MQ 异步推送 |
| 在线状态 | Redis 管理在线标记与活跃会话 |
| 会话管理 | 聊天会话列表、双向会话同步 |
| 消息记录 | 历史消息分页查询 |
| 系统通知 | 审核结果/商品动态/关注触达等系统消息实时下发 |
| 消息可靠 | MQ 异步投递 + 死信队列兜底 |

#### 📝 博客笔记 (smartLive-blog) [9211]
| 功能 | 说明 |
|:---|:---|
| 博客发布 | 发布图文博客，自动触发 MQ 审核 + ES/Milvus 同步 |
| 分层缓存 | 详情层逻辑过期 + 空值防穿透，列表层 ZSet ID 索引批量回查 |
| 热门博客 | 基于 Redis ZSet 热度榜单分页 |
| 批量查询 | 批量查询点赞/收藏/用户信息，减少 RPC 调用次数 |
| 博客管理 | 博客 CRUD、分类筛选、置顶设置 |
| 缓存管理 | 博客详情/列表缓存刷新、批量发布 |

#### ✅ 审核中心 (smartLive-audit) [9212]
| 功能 | 说明 |
|:---|:---|
| 异步审核 | MQ 异步创建审核任务，手动 ACK + nack 拒绝 |
| 敏感词检测 | 敏感词引擎自动拦截，高风险内容标记 |
| 策略回调 | 6 种业务策略（Blog/Product/Shop/Comment/Review/User）回调源服务更新状态 |
| 拒绝通知 | 审核拒绝自动通过消息中心实时通知用户 |
| 审核管理 | 审核任务列表、详情查看、人工复核 |

#### 🤖 AI 智能 (smartLive-ai) [9213]
| 功能 | 说明 |
|:---|:---|
| 意图识别 | 关键词规则匹配路由至不同 ChatHandler |
| AI 对话 | 基于 Spring AI 的智能对话，SSE 流式响应 |
| RAG 检索 | Milvus 向量检索 + Filter Expression 来源过滤 |
| 评价生成 | AI 自动生成商品/店铺评价并以 AIGenerated 入库 |
| 附近推荐 | 基于坐标距离的附近店铺/商品查询与推荐 |
| 会话管理 | AI 对话会话创建与历史记录管理 |
| 数据同步 | MQ 异步同步业务数据至 Milvus 向量库 |

#### 🎁 积分管理 (smartLive-points) [9215]
| 功能 | 说明 |
|:---|:---|
| 积分钱包 | 用户积分余额查询、等级体系（累计积分自动升级） |
| 每日签到 | 连续签到递增奖励，Redis 防重复签到 |
| 积分抽奖 | 加权随机算法抽奖，奖品配置与概率管理 |
| 积分流水 | 收支记录分页查询，按类型过滤 |
| 管理后台 | 管理员手动调整积分（增加/扣除） |

#### 💰 钱包/支付 (smartLive-wallet) [9216]
| 功能 | 说明 |
|:---|:---|
| 支付策略 | 策略工厂模式路由微信支付/支付宝/余额三种支付方式 |
| 钱包余额 | 用户账户余额查询与充值 |
| 在线支付 | 统一下单接口 + 支付回调处理 |
| 交易流水 | 支付/充值/退款交易记录查询 |

### ⚙️ 系统功能

#### 🔧 系统管理 (smartLive-system) [9202]
| 功能 | 说明 |
|:---|:---|
| 用户管理 | 系统用户配置、角色分配、用户状态 |
| 部门管理 | 组织机构树结构、数据权限控制 |
| 菜单管理 | 系统菜单、操作权限、按钮级别权限 |
| 角色管理 | 角色权限分配、数据范围划分 |
| 岗位管理 | 岗位职级配置、人员岗位关联 |
| 字典管理 | 常用固定数据维护、数据字典 |
| 参数管理 | 系统动态配置参数 |
| 通知公告 | 系统通知公告发布与查看 |
| 操作日志 | 操作日志记录与查询追踪 |
| 登录日志 | 登录日志记录、异常登录告警 |
| 在线用户 | 当前活跃用户状态监控 |

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


## <a id="效果预览"></a>🎨 效果预览

> 💡 **提示**：效果展示图由于正在录制/截图中，暂未上传至 `docs/screenshots/` 目录。
> 完整的演示视频与项目截图正在补充中，您可以直接拉取代码在本地或访问线上演示地址查看实际效果。

<!--
| 用户认证/登录 | 首页聚合流 | 动态关注流 |
|:---:|:---:|:---:|
| ![login](docs/screenshots/login-page.png) | ![homepage](docs/screenshots/homepage.png) | ![feed](docs/screenshots/feed-flow.png) |
| **全文检索结果** | **首页热门榜单** | **地图找店(LBS)** |
| ![search](docs/screenshots/search-results.png) | ![hot](docs/screenshots/hot-ranking.png) | ![map](docs/screenshots/map-view.png) |
| **店铺分类列表** | **店铺详情页** | **商品详情页** |
| ![shop-list](docs/screenshots/shop-list.png) | ![shop](docs/screenshots/shop-detail.png) | ![product](docs/screenshots/product-detail.png) |
| **限时秒杀专区** | **内容创作发布** | **互动评论/评价** |
| ![seckill](docs/screenshots/seckill-page.png) | ![publish](docs/screenshots/publish-page.png) | ![comment](docs/screenshots/comment-section.png) |
| **消息交互中心** | **系统通知/违规过滤** | **即时通讯聊天** |
| ![msg-center](docs/screenshots/message-center.png) | ![sys-notify](docs/screenshots/system-notification.png) | ![im](docs/screenshots/im-chat.png) |
| **AI 智能助手** | **用户行为数据** | **个人中心** |
| ![ai](docs/screenshots/ai-chat.png) | ![user-search](docs/screenshots/user-search.png) | ![profile](docs/screenshots/profile-page.png) |
| **我的发布/笔记** | **草稿箱** | **我的收藏** |
| ![user-posts](docs/screenshots/user-posts.png) | ![drafts](docs/screenshots/draft-box.png) | ![user-favorites](docs/screenshots/user-favorites.png) |
| **关注/粉丝列表** | **我的订单** | **我的钱包** |
| ![follow](docs/screenshots/follow-list.png) | ![order](docs/screenshots/order-page.png) | ![wallet](docs/screenshots/wallet-page.png) |
| **积分中心** | **签到与抽奖** | |
| ![points](docs/screenshots/points-page.png) | ![sign-in](docs/screenshots/sign-in.png) | |
-->


## <a id="技术栈"></a>🔧 技术栈

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
| XXL-JOB | 2.4.0 | 分布式任务调度 |
| Netty | 4.1 | 高性能网络框架（IM 长连接） |

### 前端技术

| 技术 | 说明 |
|:---|:---|
| Vue.js | 前端框架 |
| Element UI | 后台管理 UI 组件库 |
| UniApp | 多端前台用户端 |


## <a id="快速开始"></a>🚀 快速开始

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

# 2. 初始化数据库
#    按顺序导入 sql/ 目录下的脚本：
#    ① ry_20250523.sql          → 核心系统表（用户/角色/菜单等）
#    ② ry_config_20250902.sql    → Nacos 配置表
#    ③ ry_seata_20210128.sql     → Seata 分布式事务表
#    ④ quartz.sql                → Quartz 定时任务表
#    ⑤ product.sql               → 商品模块表
#    ⑥ payment.sql               → 支付记录表
#    ⑦ wallet.sql                → 钱包模块表
#    ⑧ points.sql                → 积分模块表
#    ⑨ chat_system_notice.sql    → 系统通知表

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

## <a id="服务端口速查"></a>🔗 服务端口速查

| 服务 | 模块 | 端口 |
|:---|:---|:---:|
| API 网关 | smartLive-gateway | 8080 |
| 前台用户端 | smartLive-html | 8081 |
| Seata 服务端 | smartLive-seata-server | 7091 |
| Nacos 注册中心 | - | 8848 |
| Sentinel 控制台 | smartLive-sentinel | 8718 |
| 监控中心 | smartLive-visual-monitor | 9100 |
| 认证中心 | smartLive-auth | 9200 |
| 用户服务 | smartLive-user | 9201 |
| 系统服务 | smartLive-system | 9202 |
| 店铺服务 | smartLive-shop | 9203 |
| 搜索服务 | smartLive-search | 9204 |
| 订单服务 | smartLive-order | 9205 |
| 商品服务 | smartLive-product | 9206 |
| 互动服务 | smartLive-interaction | 9207 |
| 首页服务 | smartLive-index | 9208 |
| 文件服务 | smartLive-file | 9209 |
| 聊天服务 | smartLive-chat | 9210 |
| 博客服务 | smartLive-blog | 9211 |
| 审核服务 | smartLive-audit | 9212 |
| AI 服务 | smartLive-ai | 9213 |
| IM 服务 | smartLive-im | 9214 / 8888 (Netty) |
| 积分服务 | smartLive-points | 9215 |
| 钱包服务 | smartLive-wallet | 9216 |


## <a id="性能压测报告"></a>📈 性能压测报告

本项目针对核心高并发链路（首页聚合流、秒杀抢购）进行了本地基准压测。
* **压测环境**：单机部署（Intel i7-12700H, 32G RAM），Docker Compose 启动所有中间件，JVM 分配 2G 内存。
* **压测工具**：JMeter 5.5。

| 业务场景 | 压测模型 | 并发线程数 | QPS / TPS 保底 | TP99 响应延迟 | 瓶颈分析与优化策略 |
|:---|:---|:---:|:---:|:---:|:---|
| **获取首页聚合推荐流** | 读多写少，涉及地理围栏与热度排序引擎 | 1,000 | `> 4,500` | `< 45ms` | 纯内存操作计算，瓶颈在于 Redis 序列化开销及网络 I/O，采用多级本地 Caffeine 缓存 + JSON 序列化优化后 QPS 大幅提升。 |
| **高并发秒杀抢购** | 写峰值极高，涉及库存强一致性与一人一单策略 | 5,000 | `> 3,200` | `< 120ms` | 未优化前直连 MySQL 导致 JDBC 连接池爆满发生雪崩。**优化后**：采用 Redis Lua 脚本预扣库存和校验限制，并通过 RabbitMQ 异步落单削峰，实现无数据库并发压力。 |
| **大 V 动态发布（Fan-out）** | 推拉结合，对 10 万+ 活跃粉丝进行 ZSet 时间线同步 | 500 | `> 1,500` | `< 200ms` | 同步推流模式耗时过长，导致接口超时。**优化后**：借助 Kafka/RabbitMQ 异步进行粉丝流分发（推拉结合），主节点直接返回，后台消费者异步完成十万级别写扩散任务。 |


## <a id="难点踩坑与解决方案"></a>🚧 难点踩坑与解决方案

### 1. IM 即时通讯中的 WebSocket 连接保活与内存泄漏问题
* **挑战**：在初期实现聊天集群化时，发现网关频繁报 `504 Timeout`，且服务器内存以每天 300MB 的速度缓慢泄露，连接断开后并未被回收。
* **排查**：使用 `Arthas` 导出堆快照（Heap Dump）并通过 MAT 分析，发现是 Netty 的 `ChannelGroup` 中积压了大量半死连接（Half-Open TCP），且心跳定时任务（HashedWheelTimer）未能正确感知和清理这些僵尸节点。
* **解决方案**：
  1. 实现了基于 `IdleStateHandler` 的精准服务端心跳检测机制（例如超过 60 秒未收到 PING 包直接调用 `ctx.close()` 强杀连接）。
  2. 修复了客户端意外断网时未能感知并在 `ChannelGroup` 中手动 `remove` 的逻辑。
  3. 将连接会话同步维护到 Redis 集群的 Hash 结构中以支持分布式环境下的状态一致性。上线后内存曲线恢复平稳。

### 2. 社交计数（点赞/收藏）的高频写穿透
* **挑战**：内容曝光时会出现突发的高频点赞/取消点赞动作，起初直接双写 Redis+MySQL 导致极高的 DB 事务开销甚至死锁频发。
* **解决方案**：引入了 **"Redis Hash 增量原子更新 + 定时快照批量归档"** 方案。
  1. 所有互动计数及状态实时累加在 Redis 的特定前缀缓存中。
  2. XXL-JOB 每隔 5 分钟执行一次快照归档任务：使用 `RENAME` 指令将当前全量热数据原子重命名为归档 Key。
  3. 异步线程消费归档 Key 并在应用层做状态融合聚合后，按照 `ON DUPLICATE KEY UPDATE` 批量 Upsert 回写 MySQL。彻底解耦读写路径，使得点赞的 TPS 上限只取决于 Redis 甚至网络带宽。


## <a id="常见问题"></a>❓ 常见问题 FAQ

<details>
<summary><b>1. 这个项目是你一个人做的吗？</b></summary>
是的，本项目从需求分析、架构设计、技术选型到前后端全栈开发、环境搭建与部署，均由本人独立完成。
</details>

<details>
<summary><b>2. 为什么选 Milvus 而不是 Pinecone 或 pgvector？</b></summary>
项目中需要结合 AI 进行相似度检索（例如基于向量空间模型的智能推荐），Milvus 作为云原生的开源向量数据库，支持海量向量的高效检索与动态扩展。相比闭源 SaaS 的 Pinecone 数据更自主可控；相比基于 PostgreSQL 扩展的 pgvector，Milvus 在高并发、大规模向量检索场景下性能更优。
</details>

<details>
<summary><b>3. Feed 扇出为什么用写扩散而不是读扩散？</b></summary>
本项目通过“推拉结合”模式平衡读写压力：对于活跃粉丝走写扩散（推模式），保证读取的高效性；对于不活跃粉丝或系统全站热点分发采用读扩散（拉模式）。这样避免了超级大 V 完全写扩散带来的存储灾难，同时保障了普通用户的时间线流（Feed）加载性能。
</details>

<details>
<summary><b>4. 分布式事务用的什么模式？（AT 模式 + 最终一致）</b></summary>
基于 Seata 框架，项目中多数强一致性要求的核心交易（如常规下单扣减库存）采用了 AT 模式，无业务代码侵入；对于高并发及可容忍短暂延迟的场景（例如发布动态奖励积分、数据变更同步至 ES/Milvus），采用了 RabbitMQ 消息可靠投递 + 最终一致性方案，进而保障系统整体的吞吐量。
</details>

<details>
<summary><b>5. 缓存击穿/穿透/雪崩分别怎么处理的？</b></summary>
- <b>缓存击穿：</b> 针对热点店铺或商品详情查询，利用逻辑过期策略快速响应，由独立线程重建缓存，辅以互斥锁（如基于 Redis 的 setnx）避免瞬时大量线程并发请求数据库；
- <b>缓存穿透：</b> 对空结果集进行短暂的缓存处理（空对象模式），防止恶意请求穿透到 DB；
- <b>缓存雪崩：</b> 针对不同业务数据设置不同的过期时间，并加上随机抖动值，同时利用 Redis 高可用架构防止单点宕机导致的雪崩。
</details>

<details>
<summary><b>6. Netty WebSocket 为什么不用 Spring WebSocket？</b></summary>
在即时通讯（IM）场景中，存在海量长连接并且需要频繁处理心跳包保活。虽然 Spring WebSocket 使用简单，但在处理高并发连接时，基于 NIO、事件驱动的 Netty 能以更少的线程开销极大地提升网络吞吐和减少内存消耗。项目通过自定义握手并在认证时结合 Redis Token 控制，在性能和资源占用上都优于 Spring WebSocket。
</details>

<br>


## <a id="项目结构"></a>📁 项目结构

```
com.smartLive
├── smartLive-gateway              // 网关模块 [8080]
├── smartLive-auth                 // 认证中心 [9200]
├── smartLive-api                  // 接口模块（Feign 客户端、DTO、VO）
│       ├── smartLive-api-blog                     // 博客接口
│       ├── smartLive-api-chat                     // 聊天接口
│       ├── smartLive-api-interaction              // 互动接口
│       ├── smartLive-api-order                    // 订单接口
│       ├── smartLive-api-points                   // 积分接口
│       ├── smartLive-api-product                  // 商品接口
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
│       ├── smartLive-common-swagger               // API 文档
│       └── smartLive-common-xxl                   // XXL-JOB 定时任务
├── smartLive-modules              // 业务模块
│       ├── smartLive-ai                           // AI 智能模块 [9213]
│       ├── smartLive-audit                        // 审核模块
│       ├── smartLive-blog                         // 博客笔记 [9211]
│       ├── smartLive-chat                         // 即时通讯 [9210]
│       ├── smartLive-file                         // 文件服务 [9209]
│       ├── smartLive-im                           // IM 消息 [9214]
│       ├── smartLive-index                        // 首页聚合 [9208]
│       ├── smartLive-interaction                  // 社交互动 [9207]
│       ├── smartLive-order                        // 订单管理 [9205]
│       ├── smartLive-product                      // 商品管理 [9206]
│       ├── smartLive-points                       // 积分管理 [9215]
│       ├── smartLive-search                       // 搜索引擎 [9204]
│       ├── smartLive-shop                         // 店铺管理 [9203]
│       ├── smartLive-system                       // 系统管理 [9202]
│       ├── smartLive-user                         // 用户中心 [9201]
│       └── smartLive-wallet                       // 钱包支付 [9216]
├── smartLive-visual               // 图形化管理
│       └── smartLive-visual-monitor               // 监控中心 [9100]
├── smartLive-sentinel             // 限流控制台 [8718]
├── smartLive-seata-server         // 分布式事务服务端 [7091]
├── docker                         // Docker 编排
├── sql                            // 数据库脚本
├── bin                            // 启动脚本
└── pom.xml                        // 父 POM
```


## <a id="未来规划"></a>🗺️ 未来规划 Roadmap

- [ ] **数据最终一致性升级**：引入 `Canal` 实现 MySQL Binlog 解析，替代现有的基于代码层面和 XXL-JOB 的侵入式跨库数据同步（到 ElasticSearch / Milvus）。
- [ ] **云原生可观测性架构**：将现有的 Spring Admin 和基础监控全面重构成 `OpenTelemetry` 体系，结合 `Prometheus + Grafana + Jaeger` 构建一套现代化的金牌可观测性大盘。
- [ ] **高频聚合服务 Go 化改造**：为了探索微服务异构容错，计划将 `smartLive-index` 首页全量聚合等极端依赖 I/O 发挥的高频接口用 `Go` 语言（Gin / Kratos）进行重写，体验 Goroutine 在这类场景下与传统 Java 线程池的性能红利。
- [ ] **自动化流水线 (CI/CD)**：在项目中集成完整的 GitHub Actions 或 GitLab CI/CD 流程，覆盖全链路线上的自动化单元测试与 Docker 镜像构建推送。


## <a id="项目仓库"></a>📦 项目仓库

| 仓库 | 说明 | 链接 |
|:---:|:---:|:---:|
| **smartLive-Cloud** | 后端微服务（本仓库） | [GitHub](https://github.com/mumulinya/smart-live) |
| **smartLive-admin** | 后台管理端（Vue + Element UI） | [GitHub](https://github.com/mumulinya/smartLive-admin) |
| **smartLive-web** | 用户端前台（Vue 响应式，兼容移动端） | [GitHub](https://github.com/mumulinya/smartLive-web) |


## <a id="项目文档"></a>📚 项目文档

- 📘 [在线文档](http://doc.smartLive.vip)
- 📄 [接口文档](http://doc.smartLive.vip) — 基于 SpringDoc OpenAPI 自动生成


## <a id="参与贡献"></a>🤝 参与贡献

我非常欢迎各种形式的贡献！无论是新功能、Bug 修复还是文档改进，都请随时提交。

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


## <a id="开源协议"></a>📄 开源协议

本项目基于 [MIT License](LICENSE) 开源。


## <a id="联系我"></a>📞 联系我

- **邮箱**: mumulinya@foxmail.com
- **GitHub**: [https://github.com/mumulinya](https://github.com/mumulinya)
- **微信/联系方式**: 请通过邮箱联系或者在项目主页查看详情
- **Issues**: [提交问题](https://gitee.com/mumulinya/smart-live/issues)
- **Gitee**: [项目主页](https://gitee.com/mumulinya/smart-live)


---

<div align="center">

**如果觉得不错，请给我一个 ⭐ Star 吧!**

Made with ❤️ by mumulinya · 个人独立开发 · 持续维护中

</div>
