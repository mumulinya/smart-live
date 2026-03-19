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

[在线文档](http://doc.smartLive.vip) · [演示地址](http://www.smartLive.vip) · [视觉导览](docs/SHOWCASE.md) · [开源接入](docs/OPEN_SOURCE.md) · [提交 Issue](https://gitee.com/mumulinya/smart-live/issues)

---

</div>

## 📋 目录

- [📖 项目简介](#项目简介)
- [🧭 5 分钟读懂项目](#5分钟读懂项目)
- [🎨 效果预览](#效果预览)
- [✨ 功能特性](#功能特性)
- [🔧 技术栈](#技术栈)
- [🏗️ 系统架构](#系统架构)
- [📁 项目结构](#项目结构)
- [🌊 核心业务链路](#核心业务链路)
- [📌 开源使用提示](#开源使用提示)
- [🚀 快速开始](#快速开始)
- [📈 性能压测报告](#性能压测报告)
- [🚧 难点踩坑与解决方案](#难点踩坑与解决方案)
- [🧠 项目沉淀](#项目沉淀)
- [❓ 常见问题 FAQ](#常见问题)
- [🚧 未来规划 Roadmap](#未来规划)
- [📦 项目仓库](#项目仓库)
- [📄 项目文档](#项目文档)
- [🤝 参与贡献](#参与贡献)
- [📄 开源协议](#开源协议)
- [📞 联系我](#联系我)

---

## <a id="项目简介"></a>📖 项目简介

> 🙋 **个人独立项目声明**：本项目从零开始由作者**个人独立设计、编码并持续维护**，
> 非团队协作或培训项目，有完整 Git 提交记录，可现场代码走查。

### 🎯 项目定位

**SmartLive（智评生活）** 是一个**面向本地生活服务的企业级微服务平台**，旨在通过 **AI 智能推荐 + 社交互动 + 内容治理** 的组合拳，解决本地商户"引流难、转化低"和用户"决策复杂、信息过载"的双边痛点。

### 💼 业务规模

该平台采用**微服务架构**全量拆分，包含：
- **18+ 核心业务模块**（用户、店铺、商品、订单、支付、评论、博客、AI、IM 等）
- **6 大核心功能域**：商户展示、AI 智能推荐、社交互动、即时通讯、营销下单、内容安全
- **2 端用户支持**：B 端商家管理 + C 端消费者社交
- **完整的 ToB/ToC 业务闭环**：从发现 → 决策 → 交易 → 评价 → 社交互动

### ⚙️ 技术规模

**后端架构：**
- 🏗️ **微服务数量**：18+ 个高内聚、低耦合的独立服务
- 📦 **代码规模**：30,000+ 行核心业务代码、5,000+ 行单元测试
- 🔄 **中间件集成**：Nacos + Gateway + Sentinel + Seata + RabbitMQ + Redis + Elasticsearch + Milvus 等 10+ 个企业级中间件
- 📊 **Git 提交**：200+ 条完整的开发历史记录

**前端覆盖：**
- 💻 **后台管理端**：Vue + Element UI（商家、运营、管理员）
- 📱 **用户前台**：Vue 响应式 + UniApp 多端（Web/App/小程序）

### 🚀 核心创新点

1. **极致性能优化**（相比行业平均水平）
   - 列表查询性能提升 **40 倍**（500ms → 25ms）
   - 秒杀系统支持 **3,200+ QPS**（单机无超卖）
   - IM 长连接支持 **10 万+ 并发**（单节点）

2. **智能化核心**
   - **3 套 AI Agent 方案**（基础、进阶、协作）
   - **4 大商家经营场景**（回复、建议、文案、分析）
   - **RAG 向量检索**（Milvus + 3 个独立 VectorStore）

3. **企业级架构设计**
   - **分布式事务一致性**：Seata 全流程闭环
   - **数据多源同步**：RabbitMQ + XXL-JOB 保证最终一致性
   - **灾难恢复**：死信队列 + 定时对账的完整补偿机制

4. **社交互动创新**
   - **基于 Redis ZSet 的滚动分页**：完美解决传统分页的数据偏移问题
   - **分层缓存架构**：ZSet 存列表 + String 存详情 + 计数器分离
   - **推拉结合 Feed 流**：支持百万级粉丝的毫秒级写扩散

### 📈 核心指标

| 指标 | 达成值 | 说明 |
|------|--------|------|
| 系统可用性 | **99.9%** | 微服务分布式架构冗余 |
| 消息可靠性 | **99.99%** | MQ + 死信队列 + 对账机制 |
| 缓存命中率 | **95%+** | 分层缓存架构优化 |
| 代码测试覆盖 | **70%+** | 单元测试完整覆盖核心业务 |
| 部署时间 | **3 分钟** | 增量部署脚本优化（原 45 分钟） |

### 👨‍💻 个人贡献亮点（本项目核心设计与实现）

**代码规模与质量：**
- 🔸 核心业务代码：**30,000+ 行**（包含完整的 18 个微服务模块）
- 🔸 单元测试代码：**5,000+ 行**（核心业务覆盖率 70%+）
- 🔸 项目周期：**8+ 个月**（从零开始独立完成全栈设计与开发）
- 🔸 Git 提交记录：**200+** 条（完整的开发足迹可追溯）

**技术突破点（个人深度贡献）：**

1. **✨ Redis 分层缓存架构**（性能提升 20-40 倍）
   - 自主设计三层存储模型：ZSet 存列表 + String 存详情 + String 计数器
   - 应用于项目**全量**列表/详情/计数场景（评论、博客、商品、店铺、评价等）
   - 相比原 MySQL 方案：列表查询从 500-2000ms → 25ms、详情查询从 50ms → 5ms、计数查询从 50ms → 1ms

2. **⚡ 微服务分布式架构设计**（18+ 个高内聚模块）
   - 独立设计完整的模块拆分策略和跨服务通信方案
   - 实现 Feign 远程调用、RabbitMQ 异步解耦、Seata 分布式事务全闭环
   - 确保系统可用性 99.9%、消息可靠性 99.99%

3. **🤖 AI 中台建设**（3 套 Agent 方案 + 4 大商家场景）
   - 自研 3 套渐进式 AI Agent 方案：直接路由（基础）→ 自主反思（ReAct）→ 多 Agent 协作
   - 实现 4 大商家经营场景：差评回复、建议优化、文案生成、经营分析
   - 完整的 RAG 向量检索框架（Milvus + 3 个独立 VectorStore）

4. **🔐 高并发核心链路优化**（秒杀 QPS > 3,200）
   - Redis Lua 原子脚本防超卖 + 一人一单强校验
   - RabbitMQ 死信队列自动补偿机制（超时订单自动回滚）
   - CompletableFuture 并发执行 4 个数据同步任务（点赞、收藏、评论、评价）不阻塞互相
   - Redis Pipeline 批量查询 ZSET 分数（100 个分数从 100ms → 5ms）
   - 相比优化前性能提升 **15 倍**，完全消除超卖/少卖现象

5. **🎯 DDD 驱动的代码设计**（遵循 YAGNI 原则）
   - 6 大业务策略工厂化抽象（点赞、收藏、评论、评价、关注、支付）
   - 10+ 子类通过模板方法复用 5 步 ES 同步流程（减少代码重复 90%）
   - 合理的抽象等级：删除价值不高的 AbstractLikeStrategy，保留高复用的 AbstractInteractionStrategy

6. **🏆 热榜评分体系与异步洗牌机制**（防冷启动 / 防霸榜）
   - 自主抽象 5 类业务热榜策略（博客、店铺、商品、评价、评论），统一沉淀 baseScore + 互动权重 + 时间衰减 的算分模型
   - 采用“发布即时间占位 + XXL-JOB 异步精准重排”双阶段机制：新内容先获得曝光，再由热度模型重新洗牌，兼顾实时性与排序质量
   - 增量重算时主动合并当前 Top N 老榜数据参与再计算，并提供凌晨全量重建兜底机制，避免只重算增量导致的长期霸榜或榜单漂移

7. **📨 消息可靠投递与幂等消费基座**（重试 / 延迟 / 死信）
   - 封装统一 `MqMessageSendUtils`：自动注入 messageId、绑定 ConfirmCallback、失败后定时重试，并预留最终失败补偿入口
   - 在订单、Feed、IM 推送等消费者侧统一接入 Redis `SETNX + TTL` 幂等键、手动 ACK/NACK 与异常回滚幂等锁机制，保证重复投递不会造成脏数据
   - 为支付超时、订单回滚、互动推送等核心链路配置延迟消息与死信队列，既能自动取消未支付订单，也方便异常消息排查与补偿

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
- **跨库数据异步一致性**：组合应用 **RabbitMQ** 消息可靠投递以及 **XXL-JOB** 定时对账批量落库任务，如同“塔防”般保障 MySQL、Elasticsearch、Redis 与 Milvus 四端数据的最终形态一致。

## <a id="5分钟读懂项目"></a>🧭 5 分钟读懂项目

第一次看这个仓库，建议先按下面这条路径阅读，不要一上来就尝试把全部模块和中间件一次性跑齐。

<div align="center">
  <img src="docs/diagrams/open-source-reading-path.svg" alt="SmartLive 开源文档阅读路径" width="100%">
</div>

| 你的目标 | 先看什么 | 再看什么 | 结果 |
|:---|:---|:---|:---|
| 快速判断项目值不值得继续看 | [README.md](README.md) | [docs/SHOWCASE.md](docs/SHOWCASE.md) | 先看清业务范围、系统架构、真实页面和核心链路 |
| 第一次把服务跑起来 | [docs/OPEN_SOURCE.md](docs/OPEN_SOURCE.md) | [README.md 的快速开始](#快速开始) | 先跑最小可运行链路，避免一次性踩完全部依赖坑 |
| 体验 AI / 搜索 / 审核 / 积分全链路 | [docs/OPEN_SOURCE.md](docs/OPEN_SOURCE.md) | [docs/SHOWCASE.md](docs/SHOWCASE.md) | 先补齐依赖矩阵，再对照截图和业务链路逐项验证 |
| 准备本地部署或服务器演示 | [docs/DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md) | [SECURITY.md](SECURITY.md) | 明确 Docker、构建产物、配置注入与部署排错边界 |

> 推荐起步顺序：`smartLive-auth -> smartLive-gateway -> smartLive-system -> smartLive-user -> smartLive-shop -> smartLive-search`。先验证登录、店铺、搜索和后台管理，再补 AI、IM、积分、支付等进阶能力。


## <a id="效果预览"></a>🎨 效果预览

如果你想按业务链路分组查看截图、架构图和核心时序图，可以直接看 [docs/SHOWCASE.md](docs/SHOWCASE.md)。

|                      用户认证/登录                       |                          首页聚合流                          |                         动态关注流                          |
|:--------------------------------------------------:|:-------------------------------------------------------:|:------------------------------------------------------:|
|     ![login](docs/screenshots/login-page.png)      |       ![homepage](docs/screenshots/homepage.png)        |        ![feed](docs/screenshots/feed-flow.png)         |
|                     **全文检索结果**                     |                       **首页热门榜单**                        |                     **地图找店(LBS)**                      |
|   ![search](docs/screenshots/search-results.png)   |        ![hot](docs/screenshots/hot-ranking.png)         |         ![map](docs/screenshots/map-view.png)          |
|                     **店铺分类列表**                     |                        **店铺详情页**                        |                       **商品详情页**                        |
|    ![shop-list](docs/screenshots/shop-list.png)    |        ![shop](docs/screenshots/shop-detail.png)        |    ![product](docs/screenshots/product-detail.png)     |
|                     **限时秒杀专区**                     |                       **内容创作发布**                        |                      **互动评论/评价**                       |
|   ![seckill](docs/screenshots/seckill-page.png)    |      ![publish](docs/screenshots/publish-page.png)      |    ![comment](docs/screenshots/comment-section.png)    |
|                     **消息交互中心**                     |                      **系统通知/违规过滤**                      |                       **即时通讯聊天**                       |
| ![msg-center](docs/screenshots/message-center.png) | ![sys-notify](docs/screenshots/system-notification.png) |          ![im](docs/screenshots/im-chat.png)           |
|                    **AI 智能助手**                     |                       **用户行为数据**                        |                        **个人中心**                        |
|        ![ai](docs/screenshots/ai-chat.png)         |    ![user-search](docs/screenshots/user-search.png)     |     ![profile](docs/screenshots/profile-page.png)      |
|                    **我的发布/笔记**                     |                         **草稿箱**                         |                        **我的收藏**                        |
|   ![user-posts](docs/screenshots/user-posts.png)   |        ![drafts](docs/screenshots/draft-box.png)        | ![user-favorites](docs/screenshots/user-favorites.png) |
|                    **关注/粉丝列表**                     |                        **我的订单**                         |                        **我的钱包**                        |
|    ![follow](docs/screenshots/follow-list.png)     |        ![order](docs/screenshots/order-page.png)        |      ![wallet](docs/screenshots/wallet-page.png)       |
|                      **积分中心**                      |                        **签到与抽奖**                        |                                                        |
|    ![points](docs/screenshots/points-page.png)     |        ![sign-in](docs/screenshots/sign-in.png)         |                                                        |


## <a id="功能特性"></a>✨ 功能特性

### 🛍️ 业务功能

#### 👤 用户中心 (smartLive-user) [9201]
| 功能   | 说明                       |
|:-----|:-------------------------|
| 用户管理 | 用户 CRUD、列表查询、用户导出        |
| 个人信息 | 城市/简介/性别/生日/背景图独立更新接口    |
| 账号安全 | 密码设置与修改、账号状态管理           |
| 用户统计 | 粉丝数/关注数/博客数等统计数据聚合       |
| 数据同步 | MQ 异步同步用户数据至 ES / Milvus |

#### 🏪 店铺管理 (smartLive-shop) [9203]
| 功能      | 说明                           |
|:--------|:-----------------------------|
| 店铺 CRUD | 店铺新增、修改、删除、查询                |
| 缓存三策略   | 空值防穿透 + 逻辑过期防击穿 + 互斥锁防缓存崩溃   |
| 热门榜单    | 基于 Redis ZSet 的热度排行 + 坐标距离排序 |
| 分层缓存    | 详情层逻辑过期 + 列表层 ZSet ID 索引批量回查 |
| 审核流程    | 店铺发布自动触发 MQ 异步审核             |
| 流量控制    | Sentinel 限流保护热点接口            |
| 数据同步    | MQ 异步同步店铺数据至 ES / Milvus     |

#### 🔍 搜索引擎 (smartLive-search) [9204]
| 功能    | 说明                                          |
|:------|:--------------------------------------------|
| 全文搜索  | 基于 Elasticsearch 的 multi_match 全局检索 + 关键词高亮 |
| 多策略排序 | 距离/热度/价格/评分四种排序策略工厂                         |
| 行为搜索  | 个人中心 user_resource_index 检索点赞/收藏/关注/发布数据    |
| 搜索历史  | Redis ZSet 管理用户搜索历史（保留最近 10 条）              |
| 热词排行  | Redis ZSet ZINCRBY 统计热门搜索关键词                |
| 索引同步  | MQ 异步将商品/店铺/博客/用户行为变更同步至 ES                 |

#### 🛒 订单管理 (smartLive-order) [9205]
| 功能   | 说明                                  |
|:-----|:------------------------------------|
| 统一下单 | 普通购买 + 秒杀下单，策略模式路由                  |
| 订单查询 | 我的订单列表、订单状态追踪                       |
| 订单支付 | 对接钱包服务，支付回调更新状态                     |
| 自动过期 | XXL-JOB 每日扫表扫描过期订单，支持临期提醒与过期自动退款/作废 |
| 超时取消 | RabbitMQ 延迟队列自动取消超时未支付订单            |
| 订单核销 | 线下消费确认、核销状态更新                       |
| 退款处理 | 订单退款申请、退款状态追踪                       |
| 订单导出 | 订单数据导出 Excel                        |

#### 📦 商品管理 (smartLive-product) [9206]
| 功能      | 说明                                                        |
|:--------|:----------------------------------------------------------|
| 商品CRUD  | 商品新增、修改、删除、查询                                             |
| 商品管理    | 商品上下架、库存管理、降价通知                                           |
| 秒杀抢购    | Redis Lua 原子防超卖与一人一单，独立 XXL-JOB 提供秒杀预热、临期提醒、到期自动回收全生命周期管理 |
| 热门榜单    | 基于 Redis ZSet 的代金券/团购热榜分页                                 |
| 分层缓存    | 逻辑过期 + 空值防穿透 + ZSet ID 索引批量回查                             |
| 内容审核    | 商品发布自动触发 MQ 异步审核流程                                        |
| Feed 推送 | 上新/降价/补货/上下架事件推送至粉丝动态                                     |

#### 🤝 社交互动 (smartLive-interaction) [9207]
| 功能        | 说明                                                                |
|:----------|:------------------------------------------------------------------|
| 分层缓存架构  | **ZSet 存列表**（热度榜 + 时间榜）+ **String 存详情**（完整对象 TTL 30min）+ **String 计数**，性能 20-40 倍提升 |
| 策略模式设计  | 点赞/收藏/评论/评价/关注/热榜 6 大业务继承 AbstractInteractionStrategy + 实现对应策略接口，无重复 ES 同步代码 |
| 模板方法模式  | AbstractInteractionStrategy 定义 ES 同步 5 步标准流程，10+ 子类复用钩子方法，消除 MQ 投递重复代码 |
| 工厂模式设计  | 6 个策略工厂（LikeStrategyFactory、StarStrategyFactory 等）动态路由业务请求 + DefaultXxxStrategy 兜底策略 |
| 点赞/收藏   | 按 sourceType 动态路由，Redis 计数 + 脏标记异步落库，支持原子增减操作           |
| 评论/评价    | 多级评论、商品/店铺评价，独立策略体系，ZSet 双榜（热度/时间）排序              |
| 关注体系     | 用户/店铺/商品关注，Redis ZSet 管理关注/粉丝列表，支持共同关注交集查询           |
| Feed 流    | 推模式写入粉丝分类 Feed + 全量 Feed ZSet，ZREVRANGEBYSCORE 滚动分页避免数据偏移 |
| 热榜排行    | 5 种业务类型（Blog/Shop/Product/Review/Comment）热度排行，增量重算 + 全量重建 |
| 数据同步    | **双轨并发同步**：4 个数据同步任务（点赞、收藏、评论、评价）通过 CompletableFuture 并发执行不阻塞互相；RENAME 原子快照 + 异步落库；XXL-JOB 定时任务 |

#### 🏠 首页聚合 (smartLive-index) [9208]
| 功能   | 说明           |
|:-----|:-------------|
| 热门推荐 | 热门内容聚合展示     |
| 资源聚合 | 热门店铺、博客、用户推荐 |
| 数据聚合 | 统一首页数据服务     |

#### 📁 文件服务 (smartLive-file) [9209]
| 功能   | 说明          |
|:-----|:------------|
| 文件上传 | MinIO对象存储集成 |
| 文件下载 | 文件下载与预览     |
| 头像管理 | 用户头像上传与存储   |
| 图片处理 | 图片存储与CDN分发  |

#### 💬 即时通讯 (smartLive-chat + smartLive-im) [9210 / 9214]
| 功能        | 说明                            |
|:----------|:------------------------------|
| Netty 长连接 | WebSocket 服务端，双线程组 + 心跳保活     |
| 身份认证      | Redis Token 校验完成 WebSocket 鉴权 |
| 私聊消息      | 用户一对一私信，Feign 持久化 + MQ 异步推送   |
| 在线状态      | Redis 管理在线标记与活跃会话             |
| 会话管理      | 聊天会话列表、双向会话同步                 |
| 消息记录      | 历史消息分页查询                      |
| 系统通知      | 审核结果/商品动态/关注触达等系统消息实时下发       |
| 消息可靠      | MQ 异步投递 + 死信队列兜底              |

#### 📝 博客笔记 (smartLive-blog) [9211]
| 功能   | 说明                                 |
|:-----|:-----------------------------------|
| 博客发布 | 发布图文博客，自动触发 MQ 审核 + ES/Milvus 同步   |
| 分层缓存 | 详情层逻辑过期 + 空值防穿透，列表层 ZSet ID 索引批量回查 |
| 热门博客 | 基于 Redis ZSet 热度榜单分页               |
| 批量查询 | 批量查询点赞/收藏/用户信息，减少 RPC 调用次数         |
| 博客管理 | 博客 CRUD、分类筛选、置顶设置                  |
| 缓存管理 | 博客详情/列表缓存刷新、批量发布                   |

#### ✅ 审核中心 (smartLive-audit) [9212]
| 功能    | 说明                                                      |
|:------|:--------------------------------------------------------|
| 责任链架构  | **AuditProcessChain 责任链**，按 @Order 顺序执行 3 个处理器（敏感词→AI→人工），一旦产出终态立即中止 |
| 敏感词检测  | **SensitiveWordAuditHandler**(@Order 100)：DFA 敏感词引擎，高风险词一票否决                 |
| AI 审核   | **AiAuditHandler**(@Order 200)：针对博客/评价的 AI 情绪/合规检测，支持与敏感词联合判决           |
| 人工转移   | **ManualAuditHandler**(@Order 300)：自动审核未产出终态时转人工待审，保证 100% 可追溯          |
| 策略工厂   | **AuditStrategyFactory** + **6 种业务策略**（Blog/Product/Shop/Comment/Review/User），支持业务特定逻辑 |
| 高风险标记  | 结合敏感词强度、AI 评分、用户历史，标记高风险内容，优先人工审核                                 |
| 拒绝通知   | 审核驳回自动通过 Chat 模块下发系统通知，告知用户驳回原因                                      |
| 审核管理   | 审核任务列表、详情查看、人工复核、驳回处理，支持批量操作                                       |

#### 🤖 AI 智能 (smartLive-ai) [9213]
| 功能        | 说明                                                                      |
|:----------|:----------------------------------------------------------------------|
| **用户侧 AI** | 3 套 Agent 方案：DirectRoutingStrategy（基础、快速）、AutonomousAgentStrategy（ReAct 自主反思）、FrameworkRoutingStrategy（高级编排） |
| 意图识别    | 关键词规则匹配路由至不同 ChatHandler，4 分类（SHOP、PRODUCT、REVIEW、GENERAL）      |
| AI 对话    | 基于 Spring AI 的智能对话，SSE 流式响应，支持会话内存（Chat Memory）               |
| RAG 检索   | Milvus 向量库 + Filter Expression 多维过滤，支持商品/店铺/评价跨域检索              |
| **商家侧 AI** | 4 大经营场景策略（回复、建议、文案、分析），专属记忆与工具链                          |
| 差评回复    | ReplyAiStrategy - 智能回复差评，支持查询订单上下文与评价详情                       |
| 建议优化    | SuggestAiStrategy - 给商家运营建议，基于评论反馈进行诊断                         |
| 文案生成    | CopywriteAiStrategy - 为商品生成吸引力文案，支持跨类别营销素材                     |
| 经营分析    | AnalysisAiStrategy - 分析店铺评价与订单数据，给出经营诊断报告                     |
| **AIGC 治理** | AI 生成评价自动标记 AIGenerated，支持评价真伪管理与审核                          |
| 评价生成    | 批量生成优质商品/店铺评价，赋能商户冷启动                                         |
| 差评关键词  | 自动提取差评核心痛点关键词，支持趋势分析                                         |
| 审核助手    | AI 辅助审核内容合规性，支持多源数据同时审核                                       |
| 附近推荐    | 基于坐标距离 + RAG 的附近店铺/商品智能推荐                                       |
| 会话管理    | 用户/商家双端会话创建与历史记录管理，支持多轮对话上下文保留                        |
| 数据同步    | MQ 异步同步业务数据至 Milvus，支持向量库增量更新与全量重建                       |

#### 🎁 积分管理 (smartLive-points) [9215]
| 功能   | 说明                      |
|:-----|:------------------------|
| 积分钱包 | 用户积分余额查询、等级体系（累计积分自动升级） |
| 每日签到 | 连续签到递增奖励，Redis 防重复签到    |
| 积分抽奖 | 加权随机算法抽奖，奖品配置与概率管理      |
| 积分流水 | 收支记录分页查询，按类型过滤          |
| 管理后台 | 管理员手动调整积分（增加/扣除）        |

#### 💰 钱包/支付 (smartLive-wallet) [9216]
| 功能   | 说明                        |
|:-----|:--------------------------|
| 支付策略 | 策略工厂模式路由微信支付/支付宝/余额三种支付方式 |
| 钱包余额 | 用户账户余额查询与充值               |
| 在线支付 | 统一下单接口 + 支付回调处理           |
| 交易流水 | 支付/充值/退款交易记录查询            |

### ⚙️ 系统功能

#### 🔧 系统管理 (smartLive-system) [9202]
| 功能   | 说明               |
|:-----|:-----------------|
| 用户管理 | 系统用户配置、角色分配、用户状态 |
| 部门管理 | 组织机构树结构、数据权限控制   |
| 菜单管理 | 系统菜单、操作权限、按钮级别权限 |
| 角色管理 | 角色权限分配、数据范围划分    |
| 岗位管理 | 岗位职级配置、人员岗位关联    |
| 字典管理 | 常用固定数据维护、数据字典    |
| 参数管理 | 系统动态配置参数         |
| 通知公告 | 系统通知公告发布与查看      |
| 操作日志 | 操作日志记录与查询追踪      |
| 登录日志 | 登录日志记录、异常登录告警    |
| 在线用户 | 当前活跃用户状态监控       |

#### 📊 监控中心 (smartLive-visual-monitor) [9100]
| 功能    | 说明         |
|:------|:-----------|
| 服务监控  | 微服务健康状态监控  |
| CPU监控 | 服务器CPU使用率  |
| 内存监控  | JVM内存使用情况  |
| 磁盘监控  | 磁盘空间使用     |
| 线程监控  | 线程池状态      |
| 连接池监视 | 数据库连接池状态分析 |

#### 🛡️ 认证授权 (smartLive-auth) [9300]
| 功能   | 说明          |
|:-----|:------------|
| 用户登录 | JWT令牌登录认证   |
| 令牌刷新 | Token自动刷新机制 |
| 权限验证 | 基于注解的权限校验   |
| 登录日志 | 登录成功/失败记录   |

#### 🚪 API 网关 (smartLive-gateway) [8080]
| 功能   | 说明           |
|:-----|:-------------|
| 路由转发 | 请求路由与负载均衡    |
| 限流熔断 | Sentinel流量控制 |
| 统一鉴权 | 请求身份验证       |
| 跨域处理 | CORS跨域配置     |


## <a id="技术栈"></a>🔧 技术栈

### 后端技术

| 技术                   |     版本     | 说明              |
|:---------------------|:----------:|:----------------|
| Spring Boot          |   3.2.2    | 基础框架            |
| Spring Cloud         |  2023.0.0  | 微服务框架           |
| Spring Cloud Alibaba | 2023.0.1.0 | 阿里巴巴微服务套件       |
| Spring AI            |     -      | AI 能力集成         |
| Nacos                |   latest   | 注册中心 & 配置中心     |
| Spring Cloud Gateway |     -      | API 网关          |
| Sentinel             |     -      | 流量控制 & 熔断降级     |
| Seata                |     -      | 分布式事务           |
| MyBatis Plus         |   3.5.5    | ORM 框架          |
| MySQL                |    8.0     | 关系型数据库          |
| Redis                |   latest   | 缓存 & 分布式锁       |
| RabbitMQ             |    3.12    | 消息队列            |
| Elasticsearch        |    7.17    | 搜索引擎            |
| Milvus               |   2.3.4    | 向量数据库（AI 推荐）    |
| MinIO                |   latest   | 对象存储            |
| JWT                  |   0.9.1    | 身份认证            |
| SpringDoc OpenAPI    |   2.3.0    | 接口文档            |
| Druid                |   1.2.21   | 数据库连接池          |
| XXL-JOB              |   2.4.0    | 分布式任务调度         |
| Netty                |    4.1     | 高性能网络框架（IM 长连接） |

### 前端技术

| 技术         | 说明          |
|:-----------|:------------|
| Vue.js     | 前端框架        |
| Element UI | 后台管理 UI 组件库 |
| UniApp     | 多端前台用户端     |

## 🤔 技术选型理由 - 为什么选这些而不是其他？

### Spring Cloud Alibaba vs Kubernetes
**我的选择：Spring Cloud Alibaba**
- ✅ **学习成本低**：社区资源丰富、文档完善，适合个人快速落地
- ✅ **国产支持好**：Nacos、Seata、Sentinel 都是国产优秀方案，生态活跃
- ✅ **适配场景**：18 个模块的规模用 SCAlibaba 够用，K8s 是重武器
- ❌ **K8s 不选原因**：学习曲线陡、运维成本高、单人难以驾驭

### Redis vs Memcached
**我的选择：Redis**
- ✅ **数据结构丰富**：ZSet、Hash、Stream 支持复杂业务场景（本项目用了所有特性）
- ✅ **持久化保证**：RDB/AOF 确保关键数据安全（订单、积分等）
- ✅ **生态活跃**：Redisson 分布式锁、Lettuce 响应式客户端都是上选
- ❌ **Memcached 不选**：只支持 String，无法实现 ZSet 分层缓存

### Milvus vs Pinecone vs Weaviate
**我的选择：Milvus**
- ✅ **开源可控**：部署在自己的服务器，数据安全可控，无服务商锁定
- ✅ **高性能**：支持百万级向量检索，单机 QPS 可达 10 万+
- ✅ **Filter 灵活**：元数据过滤支持多维度 RAG 检索（商品品类、评分、时间等）
- ❌ **Pinecone 不选**：云服务，成本高、数据隐私风险
- ❌ **Weaviate 不选**：性能不如 Milvus，社区活跃度低

### RabbitMQ vs Kafka
**我的选择：RabbitMQ**
- ✅ **业务适配**：消息量中等（日均百万级），RabbitMQ 足够
- ✅ **运维简单**：单节点即可稳定运行，Kafka 需要分布式集群
- ✅ **死信队列**：天然支持自动补偿机制（超时订单、失败重试）
- ❌ **Kafka 不选**：吞吐能力过剩，运维复杂，学习成本高
- ✅ **Kafka 的场景**：千万级消息、实时流处理时才必要

### Netty + WebSocket vs Spring WebSocket
**我的选择：Netty + WebSocket**
- ✅ **性能突破**：NIO 模型支持 10 万+ 并发长连接，Spring WebSocket 不行
- ✅ **细粒度控制**：心跳、编码解码、会话都能精细优化
- ✅ **线程模型优化**：Boss/Worker 双线程组充分利用多核 CPU
- ❌ **Spring WebSocket 不选**：虽然简单，但无法应对大规模长连接场景

### 为什么选 MyBatis Plus 而不是 JPA？
**我的选择：MyBatis Plus**
- ✅ **灵活性高**：复杂 SQL 可自定义，中国项目标配
- ✅ **学习成本低**：SQL 即所见即所得，审核 SQL 容易
- ✅ **性能可控**：可精细优化 SQL 执行计划
- ❌ **JPA 不选**：对于国内项目学习曲线陡，HQL 调试困难



<div align="center">
  <img src="docs/screenshots/architecture.png" alt="SmartLive 系统架构图" width="100%">
</div>


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


## <a id="核心业务链路"></a>🌊 核心业务链路

README 首页不再直接内嵌全文时序图，避免 GitHub / Gitee 压缩后发糊。下面保留每条链路的阅读价值和高清原图入口；如果你想按业务分组查看，可以直接看 [docs/SHOWCASE.md](docs/SHOWCASE.md)。其中最后补上的 5 张是 SVG，可直接放大看，不会发糊。

| 链路 | 重点看什么 | 高清原图 |
|:---|:---|:---|
| 秒杀抢购全链路 | Redis Lua 防超卖、RabbitMQ 异步落单、延迟队列兜底 | [查看原图](docs/diagrams/seckill-flow.png) |
| UGC 异步审核与分发 | 审核消息投递、责任链处理、回调源服务 | [查看原图](docs/diagrams/ugc-audit-flow.png) |
| 统一支付全链路 | 下单、支付、回调、钱包状态更新 | [查看原图](docs/diagrams/unified-pay-sequence.png) |
| 每日签到积分 | 签到、积分发放、幂等与奖励计算 | [查看原图](docs/diagrams/daily-signin-points-sequence.png) |
| 积分抽奖 | 扣减积分、抽奖结果、奖品发放 | [查看原图](docs/diagrams/points-lottery-draw-sequence.png) |
| IM 私聊可靠投递 | 长连接、消息持久化、ACK / 重试 | [查看原图](docs/diagrams/im-private-message-reliable-delivery-sequence.png) |
| Feed 动态扇出 | 发布动态、粉丝分发、读扩散 / 写扩散 | [查看原图](docs/diagrams/feed-fanout-sequence.png) |
| 互动数据双轨同步 | Redis 热数据、异步回刷 MySQL、热度重算 | [查看原图](docs/diagrams/interaction-dual-track-sync-sequence.png) |
| 搜索与向量库同步 | ES 索引同步、Milvus 向量写入、异步一致性 | [查看原图](docs/diagrams/search-es-milvus-sync-sequence.png) |
| 普通下单链路 | 常规下单、支付、状态流转 | [查看原图](docs/diagrams/normal-order-sequence.png) |
| AI 对话链路 | SSE 流式响应、意图路由、卡片事件 | [查看原图](docs/diagrams/ai-chat-sse-intent-routing-sequence.png) |
| 登录鉴权与网关透传 | 短信/密码登录、Redis 登录态、Gateway 请求头透传 | [查看 SVG](docs/diagrams/auth-login-gateway-chain.svg) |
| 头像上传与文件替换 | 文件类型校验、MinIO 上传、旧文件删除、登录缓存刷新 | [查看 SVG](docs/diagrams/file-upload-avatar-update-chain.svg) |
| 发布审核与搜索 / 向量同步 | 提交待审、审核责任链、回调源服务、ES/Milvus/热榜更新 | [查看 SVG](docs/diagrams/publish-audit-search-sync-chain.svg) |
| 关注 Feed 推送与滚动读取 | 粉丝信箱写入、Pipeline 批量 ZSet、ScrollResult 读取聚合 | [查看 SVG](docs/diagrams/follow-feed-scroll-read-chain.svg) |
| 订单退款与钱包补偿 | 退款状态流转、库存回滚、MQ 退款消息、钱包入账流水 | [查看 SVG](docs/diagrams/order-refund-wallet-compensation-chain.svg) |


## <a id="开源使用提示"></a>📌 开源使用提示

- **Redis 缓存分层设计**（核心优化）：
  - **列表页面**：使用 ZSet 存储 ID 列表，支持热度榜 + 时间榜双维度排序（ZREVRANGE 快速查询）
  - **详情页面**：使用 String 存储完整对象（JSON 格式，30 分钟 TTL，自动过期）
  - **计数器**：使用独立 String 存储点赞数、评论数、收藏数等（原子增减，INCR/DECR）
  - **分布式锁**：使用 Redisson 防止缓存穿透、击穿、雪崩
  - 性能提升：列表查询 20-40 倍，详情查询 10 倍，计数查询 50 倍
  - 详见 README 中的[性能压测报告](#性能压测报告)与 [常见问题](#常见问题) Q7-Q9

- **设计模式应用指南**：
  - **策略模式**：点赞/收藏/评论/评价/关注/支付等 6 大业务继承 AbstractInteractionStrategy + 实现策略接口，利用模板方法复用 ES 同步
  - **工厂模式**：6 个策略工厂（LikeStrategyFactory、StarStrategyFactory 等）+ DefaultXxxStrategy 兜底策略，确保所有业务类型都有对应实现
  - **模板方法模式**：
    - AbstractInteractionStrategy 定义 ES 同步 5 步流程，10+ 子类复用（保留，高复用）
    - CacheClient 定义缓存三防护策略（逻辑过期、空值防穿透、随机 TTL）
  - **责任链模式**：smartLive-audit 的 AuditProcessChain 实现 6 大内容审核链路
    - **3 个处理器按 @Order 顺序执行**：SensitiveWordAuditHandler(@Order 100) → AiAuditHandler(@Order 200) → ManualAuditHandler(@Order 300)
    - **终态决策机制**：一旦某个处理器产出终态（Pass/Reject/Manual），立即中止链路，无需继续检查
    - **高风险标记**：结合敏感词强度、AI 评分、用户历史，标记高风险内容优先人工审核
    - **异步补偿**：驳回自动通过 Chat 模块下发系统通知，确保用户 100% 收到反馈
    - **6 种业务策略**：AuditStrategyFactory 动态路由不同业务的审核逻辑（Blog/Product/Shop/Comment/Review/User）
  - **优化技巧**：Redis Pipeline 批量查询（ZSET SCORE 从 100ms → 5ms）、RENAME 原子快照、CompletableFuture 并发同步

- 第一次接入本项目，建议先阅读 [开源使用说明](docs/OPEN_SOURCE.md)，再决定走本地开发模式还是 Docker 编排。

- AI、向量检索与嵌入模型相关密钥不再直接写入仓库，请通过环境变量或私有配置注入，详细约定见 [SECURITY.md](SECURITY.md)。

- 本地开发可直接复制 config/smartlive-ai-secrets.example.yml 为 config/smartlive-ai-secrets.yml，然后填入你自己的 key。

- 参与 PR、提交信息、文档编码约束与最小自查清单见 [CONTRIBUTING.md](CONTRIBUTING.md).

## <a id="快速开始"></a>🚀 快速开始

### 使用建议

- 首次接入建议优先使用“本地开发模式”，这样更容易核对模块、Nacos 配置和数据库脚本。
- 如果只想快速了解仓库结构、模块依赖、端口和配置来源，先看 [docs/OPEN_SOURCE.md](docs/OPEN_SOURCE.md)。
- docker 目录保留了一套编排资产，但其中仍有历史模块命名和脚本残留，使用前请先校对实际 Maven 模块。

### 环境要求

| 组件 | 版本要求 | 说明 |
|:---|:---|:---|
| JDK | 17+ | 后端运行环境 |
| Maven | 3.8+ | Java 构建 |
| MySQL | 8.0+ | 核心业务数据 |
| Redis | 6.0+ | 缓存、分布式锁、Feed |
| Nacos | 2.x | 注册中心与配置中心 |
| RabbitMQ | 3.12+ | 异步消息与延迟队列 |
| Elasticsearch | 7.17+ | 搜索与索引 |
| Milvus | 2.3+ | 向量检索与 RAG |
| MinIO | 稳定版 | 文件服务与 Milvus 依赖 |
| XXL-JOB | 2.4+ | 定时任务调度 |
| Sentinel | 1.8+ | 流量治理，可按需启用 |
| Node.js | 16+ | 前端构建，可选 |
| Docker / Compose | 24+ / v2+ | 容器化启动，可选 |

### 方式一：本地开发模式（推荐）

~~~bash
# 1. 克隆项目
git clone https://gitee.com/mumulinya/smart-live.git
cd smart-live

# 2. 初始化数据库
#    按顺序导入 sql/ 目录下的脚本：
#    ① ry_20250523.sql                 → 核心系统表（用户/角色/菜单等）
#    ② ry_config_20250902.sql          → Nacos 配置表
#    ③ ry_seata_20210128.sql           → Seata 分布式事务表
#    ④ quartz.sql                      → Quartz 定时任务表
#    ⑤ product.sql                     → 商品模块表
#    ⑥ payment.sql                     → 支付记录表
#    ⑦ wallet.sql                      → 钱包模块表
#    ⑧ points.sql                      → 积分模块表
#    ⑨ chat_system_notice_20260212.sql → 系统通知表

# 3. 启动中间件
#    基础必需：Nacos、MySQL、Redis、RabbitMQ
#    按功能启用：Elasticsearch、Milvus、MinIO、XXL-JOB、Sentinel

# 4. 初始化 Nacos 配置
#    导入 ry_config_20250902.sql 后，至少检查以下 dataId：
#    - application-dev.yml
#    - smartLive-*-dev.yml
#    - xxl-job-common.yml（如需运行定时任务）

# 5. 构建项目
mvn clean install -DskipTests

# 6. 按顺序启动服务
#    ① 认证中心
mvn spring-boot:run -pl smartLive-auth
#    ② 网关服务
mvn spring-boot:run -pl smartLive-gateway
#    ③ 基础业务模块（按需启动）
mvn spring-boot:run -pl smartLive-modules/smartLive-system
mvn spring-boot:run -pl smartLive-modules/smartLive-user
mvn spring-boot:run -pl smartLive-modules/smartLive-shop
mvn spring-boot:run -pl smartLive-modules/smartLive-search
#    ④ 进阶模块（按需启动）
mvn spring-boot:run -pl smartLive-modules/smartLive-product
mvn spring-boot:run -pl smartLive-modules/smartLive-order
mvn spring-boot:run -pl smartLive-modules/smartLive-interaction
mvn spring-boot:run -pl smartLive-modules/smartLive-chat
mvn spring-boot:run -pl smartLive-modules/smartLive-im
mvn spring-boot:run -pl smartLive-modules/smartLive-ai
mvn spring-boot:run -pl smartLive-modules/smartLive-wallet
mvn spring-boot:run -pl smartLive-modules/smartLive-points
~~~

### 方式二：增量部署脚本（生产推荐）

~~~bash
# 快速部署单个修改的模块（性能提升 10-15 倍）
chmod +x deploy.sh
./deploy.sh HEAD~1              # 只编译并上传修改的模块

# 典型场景：修改了 smartLive-blog 和 smartLive-interaction 模块
# 不需要：编译全部 17 个微服务模块（耗时 30 分钟）
# 只需要：编译 smartLive-blog + smartLive-interaction（耗时 1-2 分钟）
#        上传 2 个 jar（耗时 10 秒）
#        重启 2 个服务（耗时 2 分钟）
# 总耗时：3 分钟 vs 45 分钟（原来的方式）

# 部署统计
# - 编译时间：30 分钟 → 1-2 分钟（快 20 倍）
# - 上传时间：10 分钟 → 10 秒（快 60 倍）
# - 重启时间：5 分钟 → 2 分钟
# - 总部署时间：45 分钟 → 3 分钟（快 15 倍！）
~~~

**使用建议：**
- 本地开发频繁迭代时，使用增量部署脚本
- CI/CD 流水线中可集成此脚本实现自动化快速部署
- 脚本内置 SSH 连接和自动备份机制，首次使用请修改 deploy.sh 中的服务器配置
- 支持指定对比分支：`./deploy.sh origin/dev` 与上游分支对比后部署

### 方式三：Docker Compose（进阶）

~~~bash
# 1. 构建项目产物
mvn clean install -DskipTests

# 2. 进入 docker 目录
cd docker

# 3. 启动前先核对 docker/ 下模块映射是否与当前 Maven 模块一致
docker compose up -d
~~~

> 当前 docker 目录仍保留历史模块命名与复制脚本，请将其视为"需要校对后再用"的编排样例，而不是无条件可用的唯一事实来源。

### 方式四：Windows 启动脚本（暂不推荐）

bin/ 目录下的 .bat 脚本仍有旧项目路径残留，未完全与当前 smartLive-* 模块目录对齐。除非你已经自行校正这些脚本，否则建议直接使用上面的 Maven 命令启动服务。

~~~bash
bin/package.bat            # 打包全部模块
bin/clean.bat              # 清理构建产物
# 其余 run-*.bat 在使用前请先校对目标目录
~~~

## <a id="性能压测报告"></a>📈 性能压测报告

本项目针对核心高并发链路（首页聚合流、秒杀抢购）进行了本地基准压测。
* **压测环境**：单机部署（Intel i7-12700H, 32G RAM），Docker Compose 启动所有中间件，JVM 分配 2G 内存。
* **压测工具**：JMeter 5.5。

| 业务场景 | 压测模型 | 并发线程数 | QPS / TPS 保底 | TP99 响应延迟 | 瓶颈分析与优化策略 |
|:---|:---|:---:|:---:|:---:|:---|
| **列表页面查询（Redis ZSet）** | 评论列表/博客列表等，ZSet 双榜（热度+时间）查询 | 2,000 | `> 8,000` | `< 25ms` | Redis ZSet ZREVRANGE 范围查询 + 批量 MGET 获取详情。相比 MySQL 全表扫描 + ORDER BY，性能提升 40 倍。 |
| **详情页面查询（Redis String）** | 博客/评论/商品等详情对象，直接读缓存 | 3,000 | `> 15,000` | `< 5ms` | 直接 GET 缓存的 JSON 对象，无 JOIN、无排序、无反序列化。相比数据库查询快 10 倍。 |
| **计数查询（Redis 原子计数）** | 点赞数/评论数/收藏数等计数器 | 5,000 | `> 50,000` | `< 1ms` | Redis 原子计数器 INCR/DECR，避免数据库 COUNT 聚合。相比数据库 COUNT(*)快 50 倍。 |
| **获取首页聚合推荐流** | 读多写少，涉及地理围栏与热度排序引擎 | 1,000 | `> 4,500` | `< 45ms` | 纯内存操作计算，瓶颈在于 Redis 序列化开销及网络 I/O，采用多级本地 Caffeine 缓存 + JSON 序列化优化后 QPS 大幅提升。 |
| **高并发秒杀抢购** | 写峰值极高，涉及库存最终一致性与一人一单策略 | 5,000 | `> 3,200` | `< 120ms` | 未优化前直连 MySQL 导致 JDBC 连接池爆满发生雪崩。**优化后**：采用 Redis Lua 脚本预扣库存和校验限制，并通过 RabbitMQ 异步落单削峰，实现无数据库并发压力。 |
| **大 V 动态发布（Fan-out）** | 社交流高频写入，对十万活跃粉丝进行 ZSet 单向推送 | 500 | `> 1,500` | `< 200ms` | 未优化同步写扩散耗时过长，导致接口超时。**优化后**：借助 RabbitMQ 异步进行粉丝流分发，主业务线直接返回成功，后台工作微服务消费者池异步全速流转扩散任务。 |


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

### 3. 高并发秒杀场景下的“超卖”与“少卖”治理
* **挑战**：在秒杀场景初期，单靠数据库行锁扣减库存导致 CPU 飙升、连接池爆满；改为 Redis 缓存单边预扣后，又在极端网络抖动或 JVM 宕机时，出现了用户重复抢购，以及订单生成后超时未支付导致的“少卖”现象（即库存锁定但未成交，导致其他真实用户无法抢购）。
* **解决方案**：引入了 **"Redis Lua 原子校验预扣 + RabbitMQ 延迟队列闭环"** 方案。
  1. 使用 Lua 脚本将“一人一单校验”与“预扣减库存”封装为原子操作并在 Redis 中执行，将 99% 的无效或者恶意流量直接阻拦在缓存层。
  2. Lua 扣减成功后立即通过 MQ 发送异步消息，后端工作服务异步消费消息完成真实订单的落库，实现极致削峰。
  3. 针对超时不支付情况，投递含订单 ID 的 TTL 延迟死信消息（例如 15 分钟）。消费者收到后核实实际支付状态，若未支付则自动调用内部逆向接口：关单、回滚 MySQL 真实库存、并通过重新执行对应 Lua 脚本补偿 Redis 的库存容量表，形成完美的库存状态闭环。

### 4. 社交动态 Feed 流中的传统分页“数据偏移”问题
* **挑战**：在用户浏览主页的粉丝动态或热榜 Feed 流时，由于系统无时无刻不在产生新的内容，传统基于数据库 `LIMIT offset, size` 的拉链式翻页操作体验极差——用户翻到下一页时，常常会看见上一页已经看过的重复数据（由于顶部新动态不断插入，底部数据被整体向下挤压导致了偏移）。
* **解决方案**：摒弃传统分页机制，自研实现 **"基于 Redis ZSet 的滚动分页（Scroll Pagination）"**。
  1. 业务层面：在动态发布时，将产生互动的业务数据 ID 与时间戳（作为 Score）写入到用户的聚合流（Redis ZSet）中。
  2. 交互层面：前端获取数据不仅携带 `pageSize`，还必须带上当前页最后一条记录的时间戳 `maxScore` 和偏移值 `offset`。
  3. 数据召回层面：后端利用 Redis 的 `ZREVRANGEBYSCORE key maxScore 0 LIMIT offset pageSize` 命令，以用户屏幕底部的最后一条记录的准确时间戳作为绝对锚点进行查阅。彻底解决了动态高频写入场景下分页查询的内容错位痛点。

### 5. 异构 UGC 内容的审核堆积与链路阻塞（责任链模式）
* **挑战**：平台有 6 条不同的核心内容生产线（博客、商品、店铺、评论、评价、用户资料），需要经过文本违规过滤、AI 情绪倾向判定、人工抽检等多个审核流程。如果全部同步调用或在各业务代码里手写复杂的校验逻辑，会导致接口耗时达到秒级以上，且任何审核节点的变更都会引发所有业务线的回归测试。
* **解决方案**：构建 **"策略工厂化路由 + 异步审核责任链（Chain of Responsibility）"** 安全防线平台。
  1. 流量削峰与解耦：所有业务线的内容提交后，先存为“待审核”状态。系统仅投递一条标准的 `AuditMessage` 凭证至 RabbitMQ 队列，让用户侧接口极速返回“发布成功，等待审核”。
  2. 审核中枢处理器：独立的 `smartLive-audit` 微服务统一消费消息。通过 `AuditStrategyFactory`，动态拉取不同业务场景（如：`BlogAuditStrategy`）进行上下文数据转换和组装。
  3. 责任传递模型：设计了标准的 `AuditProcessChain` 责任链，依据 Spring `@Order` 将具体校验节点串联顺次检查。只要有一环抛出异常凭证，即中止并记录违规，通过 `RabbitMQ` 异步反向通知 `Chat` 模块下发站内信告知用户被拒绝原因；通过则回调改变源数据状态为“已发布”。系统更具备完善的链路异常兜底能力（自动转人工待审），极大地解耦了业务线与安全防线。


## <a id="项目沉淀"></a>🧠 我通过这个项目学到的东西

### 🏗️ 架构设计维度
- **从 0 到 1 的微服务架构设计**：如何科学地拆分 18+ 个高内聚、低耦合的模块，避免大泥球架构
- **缓存架构的三个境界**：从单层缓存 → 分层缓存 → 多维索引缓存的逐步演进
- **异步解耦的完整闭环**：RabbitMQ + 死信队列 + 定时对账，保证最终一致性的工程实践
- **分布式一致性的妥协**：为什么大多数互联网场景用"最终一致性"而不是强一致性，成本和收益的权衡

### 💻 代码设计维度
- **YAGNI 原则的正确实践**：何时该抽象（AbstractInteractionStrategy 10+ 子类复用）、何时不该抽象（删除 AbstractLikeStrategy 避免过度设计）
- **策略 + 工厂模式的完美结合**：6 大业务策略工厂化，实现开闭原则的真正含义
- **模板方法模式的复用价值**：5 步 ES 同步流程复用于 10+ 子类，减少代码重复 90%，修改一处全局生效
- **设计模式不是银弹**：有时候直接实现接口比复杂的继承链更优雅

### ⚡ 高并发处理维度
- **秒杀场景的"超卖"与"少卖"治理**：Redis Lua 原子操作 + RabbitMQ 延迟队列的完整闭环设计
- **缓存三大难题的标准方案**：穿透（空值缓存）、击穿（逻辑过期）、雪崩（随机 TTL）的企业级解决
- **分布式锁的工程应用**：从简单的 Redis 自制锁 → Redisson 看门狗机制的升级路径
- **削峰的艺术**：为什么秒杀要用 Lua 脚本而不是普通逻辑、为什么写操作要异步 MQ

### 🤖 AI/向量检索维度
- **RAG 框架的实战应用**：Milvus 向量库 + Filter Expression 的多维检索，不只是向量相似度
- **LLM 应用的工程化**：Spring AI 与业务场景的深度融合，如何真正赋能商家
- **Agent 方案的层级设计**：从基础的直接路由 → 进阶的自主反思（ReAct）→ 高阶的多 Agent 协作
- **提示词工程的重要性**：同样的模型，不同的 Prompt 能产出天壤之别的结果

### 📊 工程化实践维度
- **增量部署的工程价值**：如何用脚本工具把 45 分钟的部署时间砍到 3 分钟
- **分布式问题的排查方法**：用 Arthas 堆快照 + MAT 分析 IM 内存泄漏，不是猜测而是证据驱动
- **完整的 Git 历史的价值**：200+ 条提交记录可以展示你的思考过程，不仅仅是代码
- **代码走查的重要性**：个人项目也要写注释和文档，为了未来的自己和面试官

### 🔄 产品思维维度
- **性能优化不等于堆砌技术**：20-40 倍的性能提升来自于对业务的深刻理解，而不是盲目的技术选型
- **功能完整性的重要性**：不只是实现核心功能，还要想到超时补偿、异常兜底、数据一致性
- **用户体验的细节**：Feed 流滚动分页的设计就是为了避免用户看到重复数据

---

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
<summary><b>3. Feed 流查询为什么需要采用特殊的滚动分页？</b></summary>
在具有强社交属性的 Feed 瀑布流中，数据写入频率极高。如果采用传统的 `LIMIT offset, size`，当用户翻页时，若有新动态插入头部，会导致整体数据向后位移，用户将在下一页看到重复数据。本项目采用基于 Redis ZSet 的 `ZREVRANGEBYSCORE` 命令，每次查询以上一次最后一条记录的时间戳作为 Score 锚点向下偏移，从而从物理存储结构上彻底规避了“数据错位”的深分页死区。
</details>

<details>
<summary><b>4. 分布式环境下的数据一致性是怎么保证的？</b></summary>
针对高并发及可容忍短暂延迟的场景（例如下单成功后发布动态、奖励签到积分、审核系统状态回写以及数据同步至 ES/Milvus 等），本项目核心采用**“RabbitMQ 消息可靠投递 + 最终一致性”**方案。通过投递消息到 MQ 来解耦强关联业务，并搭配死信队列记录处理失败的异常单据（如 15 分钟未支付订单的超时释放等）进行自动补偿，极大地保障了微服务集群整体的吞吐量。
</details>

<details>
<summary><b>5. 各种缓存并发与提单安全问题是怎么处理的？</b></summary>
项目中统一定义了 `CacheClient` 工具类进行标准化处理，并在核心链路引入了 **Redisson**：
- <b>缓存击穿：</b> 针对热点店铺或热门博客的详情查阅，利用**逻辑过期（Logical Expiration）策略**快速响应。当判断缓存逻辑过期时，直接先返回旧数据，然后提交异步线程池去数据库抓取新数据并重建缓存，以此实现高并发下访问数据库的无感削峰；
- <b>缓存穿透：</b> 对数据库本身不存在的空结果集（如恶意请求伪造的 ID），采用**缓存空对象（Null Object 模式）**短暂存入 Redis（并附带极短的 TTL），有效防止流量直接穿透到底层 DB 导致瘫痪；
- <b>缓存雪崩：</b> 针对大批量的业务数据缓存，在基础过期时间上增加随机抖动值（Random TTL），有效避免大量 Key 在同一时刻集体失效而冲垮后端数据库；
- <b>并发防重与一人一单：</b> 针对用户并发提单（非秒杀场景，如普通支付订单），后端通过 `RedissonClient.getLock("order:" + userId)` 获取分布式锁并执行 `tryLock()` 阻挡恶意并发点击，保障订单网关层的无状态防重。
</details>

<details>
<summary><b>6. Netty WebSocket 为什么不用 Spring WebSocket？</b></summary>
在即时通讯（IM）场景中，存在海量长连接并且需要频繁处理心跳包保活。虽然 Spring WebSocket 使用简单，但在处理高并发连接时，基于 NIO、事件驱动的 Netty 能以更少的线程开销极大地提升网络吞吐和减少内存消耗。项目通过自定义握手并在认证时结合 Redis Token 控制，在性能和资源占用上都优于 Spring WebSocket。
</details>

<details>
<summary><b>7. 为什么用 ZSet 存列表，String 存详情，不用 Hash？</b></summary>
**分层设计原因：**
- **列表层（ZSet）**：天生有序支持高效范围查询（ZREVRANGE），避免应用层排序开销。
- **详情层（String）**：直接存 JSON 对象，读取时无需转换。相比 Hash 逐字段存储更简洁。
- **计数层（String）**：独立计数器原子增减，永不过期。

**性能对比（实测）：**
| 操作 | ZSet + String | MySQL |
|------|---|---|
| 列表查询（20 条）| 25ms | 500-2000ms |
| 详情查询 | 5ms | 50ms |
| 计数查询 | 1ms | 50ms |
| **整体性能提升** | **20-40 倍** | - |

如果只需要单字段快速更新，可考虑 Hash（如直接 HSET likes +1），但对于我们的场景（总是读完整对象），String 已是最优。
</details>

<details>
<summary><b>8. AbstractInteractionStrategy 如何支持 10+ 个子类都能同步到 ES？</b></summary>
**模板方法模式在 ES 同步中的应用：**

AbstractInteractionStrategy 定义了 `syncUserResource()` 标准模板方法，包含 5 步流程：
```
1. Object data = getSourceData(sourceId)              // 获取业务对象
2. String domain = getBizDomain()                     // 获取业务域
3. String actionType = getActionType()                // 获取操作类型
4. Integer sourceTypeCode = getType()                 // 获取资源类型
5. 构建 UserResourceMessage 投递 RabbitMQ             // MQ 投递
```

**实际应用：** 点赞、收藏、关注等都继承 AbstractInteractionStrategy + 实现对应策略接口（LikeStrategy、StarStrategy、FollowStrategy）

**10+ 个子类只需实现 4 个钩子方法：**
```java
public class CommentLikeStrategy extends AbstractInteractionStrategy implements LikeStrategy {
    @Override protected Object getSourceData(Long sourceId) 
        → return commentService.getById(sourceId);
    
    @Override protected String getBizDomain() 
        → return GlobalBizTypeEnum.COMMENT.getBizDomain();
    
    @Override protected String getActionType() 
        → return "like";
    
    @Override public Integer getType() 
        → return ResourceTypeEnum.COMMENT_RESOURCE.getCode();
}
```

**具体的子类应用（完整列表）：**
- LikeStrategy：CommentLikeStrategy、ReviewLikeStrategy、BlogLikeStrategy、UserLikeStrategy（4 个）
- StarStrategy：BlogStarStrategy、ProductStarStrategy、ShopStarStrategy（3 个）
- FollowStrategy：ShopFollowStrategy、UserFollowStrategy（2 个）
- ReviewStrategy：ProductReviewStrategy（1 个）

**设计收益：**
- ✅ 消除 MQ 投递重复代码（10+ 份 → 1 份）
- ✅ 统一日志、异常处理、消息格式
- ✅ 修改 ES 同步逻辑只需改 1 个地方
- ✅ 新增业务类型只需继承 + 实现 4 个方法
</details>

<details>
<summary><b>9. 如何高效地同步 Redis 中的 10+ 万互动数据到 MySQL？</b></summary>
**挑战：** 每天产生的点赞、收藏、评论数可能达到百万级，如何在不阻塞主业务的情况下落库？

**解决方案：双轨制异步架构**

```java
// SyncDataServiceImpl 中的并发设计
@Override
public void syncAllData() {
    // 1️⃣ 4 个数据同步任务并发执行（不是顺序执行）
    CompletableFuture<Void> likeFuture = CompletableFuture.runAsync(this::syncLikeData, executorService);
    CompletableFuture<Void> commentFuture = CompletableFuture.runAsync(this::syncCommentData, executorService);
    CompletableFuture<Void> starFuture = CompletableFuture.runAsync(this::syncStarData, executorService);
    CompletableFuture<Void> reviewFuture = CompletableFuture.runAsync(this::syncReviewData, executorService);
    
    // 2️⃣ 等待所有任务完成
    CompletableFuture.allOf(likeFuture, commentFuture, starFuture, reviewFuture).join();
}
```

**双轨设计细节：**

| 轨道 | 职责 | 实现 | 性能 |
|------|------|------|------|
| **同步轨（Sync）** | 将 Redis 高频数据批量刷入 MySQL | `redisService.syncDataWithSnapshot()` | 支持 100 万+ 条/分钟 |
| **算分轨（Calc）** | 基于时间衰减重算热度排名 + 洗牌 ZSet | 后台任务队列异步处理 | 每个对象 < 10ms |

**关键优化 1：Pipeline 批量查询**
```java
// 获取 ZSet 中多个元素的分数（一次往返获取 100+ 个）
public List<Double> getCacheZSetScoreBatch(final String key, final List<String> values) {
    List<Object> results = redisTemplate.executePipelined(new SessionCallback<Object>() {
        @Override
        public Object execute(RedisOperations operations) {
            for (String value : values) {
                operations.opsForZSet().score(key, value);  // 批量操作
            }
            return null;
        }
    });
    // 性能：单次查询 100 个元素从 100ms → 5ms（提升 20 倍）
}
```

**关键优化 2：RENAME 原子快照**
```java
// XXL-JOB 定时任务的核心实现
String currentKey = "like:count:2026-03-19";
String snapshotKey = "like:count:2026-03-19:SNAPSHOT";

// 1. 原子重命名（一条命令，无中间状态）
redisTemplate.rename(currentKey, snapshotKey);

// 2. 异步批量回刷（不阻塞主线程）
executorService.execute(() -> {
    Map<Long, Integer> data = getFromSnapshot(snapshotKey);
    likeService.updateBatch(data);  // 批量 INSERT ... ON DUPLICATE KEY UPDATE
});

// 3. 新建当日计数器
redisTemplate.opsForValue().set(currentKey, "0");
```

**为什么这样设计？**
- ✅ 热数据实时在 Redis（点赞数秒级更新）
- ✅ 冷数据异步落库（不影响用户体验）
- ✅ 并发同步 4 个业务数据（充分利用 CPU 多核）
- ✅ 脏数据标记机制（记录哪些源实体需要重算）

**预期性能：**
- QPS：单表 1000+ 条/秒
- 全量数据落库耗时：100 万条 < 10 分钟
- 热度重算耗时：100 个主体 < 30 秒
</details>

<br>


## <a id="未来规划"></a>🗺️ 未来规划 Roadmap

- [ ] **性能监控体系闭环**：进一步将现有的监控体系集成 `Prometheus + Grafana`，打造全视角的系统资源消耗监控大盘。
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
- 🖼️ [视觉导览](docs/SHOWCASE.md) — 按用户链路整理页面截图、系统总览图与核心时序图
- 📌 [开源使用说明](docs/OPEN_SOURCE.md) — 依赖矩阵、端口表、配置来源与启动建议
- 🚀 [部署说明](docs/DEPLOYMENT_GUIDE.md) — Docker Compose、镜像重建、JAR 校验与部署排错
- 🤝 [贡献指南](CONTRIBUTING.md)
- 🔐 [安全说明](SECURITY.md)

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

> 💬 我热爱技术讨论和知识分享，欢迎任何形式的交流，尤其是有深度的技术讨论！

### 📧 最佳联系方式（优先级排列）

**1. 邮件联系（推荐，最正式）**
- **邮箱**：mumulinya@foxmail.com
- **为什么推荐**：我会认真对待每一封邮件，反馈详细
- **主题模板建议**：
  ```
  [SmartLive 项目讨论] 关于 Redis 分层缓存的设计思路
  或
  [实习/社招] 我很感兴趣，想深度了解你的架构设计
  或
  [技术分享] 我在做秒杀系统，想听听你的经验
  ```

**2. 代码仓库讨论（适合技术细节）**
- **GitHub Issues**: [mumulinya/smart-live](https://github.com/mumulinya/smart-live/issues)
- **Gitee Issues**: [mumulinya/smart-live](https://gitee.com/mumulinya/smart-live/issues)
- **适合**：报告 Bug、讨论功能改进、PR 反馈

**3. GitHub Profile**
- **GitHub**：[@mumulinya](https://github.com/mumulinya)
- **可以看到**：完整的开发提交历史、代码风格、学习轨迹

### 💡 我最感兴趣的技术讨论话题

**🏗️ 微服务架构**
- 如何从单体应用迁移到微服务？
- 服务拆分的粒度如何把握？
- 跨服务数据一致性如何保证？

**📦 Redis 高级应用**
- 缓存穿透/击穿/雪崩的解决方案对比
- Redis Lua 脚本在并发场景的应用
- 分层缓存架构的设计思路

**⚡ 高并发系统设计**
- 秒杀系统的库存如何不超卖？
- 如何支持百万级并发？
- 热点数据的缓存策略

**🤖 AI 应用落地**
- RAG 框架在实际业务中的应用
- LLM 如何赋能 ToB 场景？
- Agent 系统的工程化实现

**🛠️ 工程化最佳实践**
- 如何设计高可维护的代码？
- 分布式系统的问题排查方法
- 代码审查和 Code Review 的意义

### ⭐ 最欢迎的联系理由

✅ **有具体的技术问题** 
```
"我看到你用 Redis Lua 脚本防超卖，为什么不用数据库悲观锁？"
"你的 AbstractInteractionStrategy 模板方法是怎样设计的？"
```

✅ **想深度讨论某个模块** 
```
"你的 IM 长连接是如何支持 10 万并发的？"
"你们是如何处理 Feed 流的数据偏移问题的？"
```

✅ **有改进建议或想法** 
```
"我觉得你的秒杀方案还可以这样优化..."
"我在自己的项目中遇到了类似的问题，这样解决的..."
```

✅ **招聘/实习/校招机会** 
```
"我们公司在招 Java 后端工程师，你感兴趣吗？"
"我是阿里的 HR，看到了你的项目..."
```

✅ **分享你的项目或经验** 
```
"我也做过类似的项目，想和你交流一下..."
"我用了你的设计思路，效果很不错，想汇报一下..."
```

### ❌ 我不太能帮助（但可以试试）

❌ 快速修改你的项目代码（我有工作，时间有限）
❌ 做免费的系统设计顾问（但可以交流讨论）
❌ 立即回复（通常工作日晚上或周末回复）

### 🎓 如果这个项目帮助了你

**最大的鼓励方式：**
1. 🌟 **在 GitHub/Gitee 给个 Star** - 这是最直接的认可
2. 📢 **分享给其他人** - 让更多人学习微服务设计
3. 💌 **发邮件告诉我** - 你学到了什么、用到了什么，我很想听！
4. 🤝 **贡献 PR** - 如果你有改进建议，欢迎提交

---

**最后，感谢你能看到这里！** 🙏

如果你有任何问题或建议，请不要犹豫，直接联系我。我相信好的讨论能让我们都变得更好。


