# SmartLive 视觉导览

这份文档只做一件事：让第一次看到 SmartLive 的人，先在几分钟内看懂“这个项目长什么样、覆盖了哪些业务、有哪些关键链路图”。

**按目标看图：**

- **第一次认识项目**：[项目全景](#showcase-overview) -> [用户发现与决策](#showcase-discovery) -> [交易与增长](#showcase-trade)
- **想看交易闭环**：[交易与增长](#showcase-trade) -> [交易与履约图组](#chains-trade)
- **想看社交与推荐**：[内容、社交与消息](#showcase-social) -> [社交与消息图组](#chains-social) -> [排行、热度与推荐图组](#chains-rank)
- **想看 AI 与治理**：[AI 与用户画像](#showcase-ai) -> [内容审核与搜索图组](#chains-search) -> [AI 与经营图组](#chains-ai)
- **想看 Redis / 调度设计**：[缓存与性能专题](#chains-cache) -> [调度与定时任务](#chains-schedule)

## <a id="showcase-overview"></a>1. 项目全景

<div align="center">
  <img src="./screenshots/architecture.png" alt="SmartLive 系统架构图" width="100%">
</div>

- 前台用户端、后台管理端通过网关统一进入微服务体系。
- 中间层覆盖用户、店铺、商品、订单、互动、AI、IM、审核、积分、钱包等业务域。
- 基础设施层包含 Redis、RabbitMQ、Elasticsearch、Milvus、MinIO、XXL-JOB 等中间件。

## <a id="showcase-discovery"></a>2. 用户发现与决策

| 登录与首页 | 搜索与榜单 | 地图与店铺 |
|:---:|:---:|:---:|
| ![login](./screenshots/login-page.png) | ![search-results](./screenshots/search-results.png) | ![map-view](./screenshots/map-view.png) |
| 统一登录入口，支持移动端用户鉴权 | 支持全文检索、热词和排序策略 | 支持基于位置的找店和附近推荐 |

| 首页聚合流 | 热门榜单 | 店铺详情 |
|:---:|:---:|:---:|
| ![homepage](./screenshots/homepage.png) | ![hot-ranking](./screenshots/hot-ranking.png) | ![shop-detail](./screenshots/shop-detail.png) |
| 首页聚合商户、活动和分类入口 | 热度榜单直观展示热门内容和商户 | 详情页承接店铺介绍、商品、评价与互动 |

## <a id="showcase-trade"></a>3. 交易与增长

| 商品详情 | 秒杀专区 | 订单中心 |
|:---:|:---:|:---:|
| ![product-detail](./screenshots/product-detail.png) | ![seckill-page](./screenshots/seckill-page.png) | ![order-page](./screenshots/order-page.png) |
| 商品详情承接下单、收藏、评价入口 | 秒杀专区对应 Redis Lua + MQ 异步削峰链路 | 订单中心展示待支付、进行中、已完成订单 |

| 钱包中心 | 积分中心 | 签到抽奖 |
|:---:|:---:|:---:|
| ![wallet-page](./screenshots/wallet-page.png) | ![points-page](./screenshots/points-page.png) | ![sign-in](./screenshots/sign-in.png) |
| 钱包展示余额、消费和支付记录 | 积分中心承接签到、抽奖和成长体系 | 连续签到与抽奖是增长玩法入口 |

## <a id="showcase-social"></a>4. 内容、社交与消息

| 动态流 | 发布页 | 评论区 |
|:---:|:---:|:---:|
| ![feed-flow](./screenshots/feed-flow.png) | ![publish-page](./screenshots/publish-page.png) | ![comment-section](./screenshots/comment-section.png) |
| 动态流展示关注内容与推荐内容混合分发 | 支持内容创作与草稿管理 | 评论和评价承接互动计数与审核链路 |

| 我的发布 | 我的收藏 | 关注列表 |
|:---:|:---:|:---:|
| ![user-posts](./screenshots/user-posts.png) | ![user-favorites](./screenshots/user-favorites.png) | ![follow-list](./screenshots/follow-list.png) |
| 个人主页可查看已发布内容 | 收藏列表承接内容回访与转化 | 关注与粉丝关系支撑 Feed 分发 |

| 消息中心 | 系统通知 | IM 私聊 |
|:---:|:---:|:---:|
| ![message-center](./screenshots/message-center.png) | ![system-notification](./screenshots/system-notification.png) | ![im-chat](./screenshots/im-chat.png) |
| 消息中心汇总互动与业务通知 | 审核结果、系统提醒会通过站内通知下发 | IM 支撑实时聊天和会话管理 |

## <a id="showcase-ai"></a>5. AI 与用户画像

| AI 助手 | 用户行为检索 | 个人中心 |
|:---:|:---:|:---:|
| ![ai-chat](./screenshots/ai-chat.png) | ![user-search](./screenshots/user-search.png) | ![profile-page](./screenshots/profile-page.png) |
| AI 助手支持对话、推荐、经营辅助等场景 | 行为检索承接点赞、收藏、发布等数据回查 | 个人中心聚合用户资料、内容与资产入口 |

## <a id="showcase-chains"></a>6. 核心链路图集

这批图用于细看系统设计，不适合直接压缩内嵌在首页里看。这里统一提供双入口：`SVG` 看主链路展示，`PNG` 看更细的历史设计图。旧版 PNG 已归档到 [./diagrams/legacy-png](./diagrams/legacy-png)。

这里也按主题分组展示。前面的分组更适合第一次快速读项目，最后的“历史详细图补充”主要保留旧设计视角，方便继续深挖。

### <a id="chains-trade"></a>交易与履约

这组优先回答“项目怎么完成下单、支付、退款、核销和履约闭环”。

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| 秒杀抢购全链路 | Redis Lua 防超卖、RabbitMQ 异步落单、延迟队列兜底 | [查看 SVG](./diagrams/seckill-flow.svg) | [查看 PNG](./diagrams/legacy-png/seckill-flow.png) |
| 普通下单：订单创建与状态流转 | 下单创建、异步落单、支付生效后的状态流转 | [查看 SVG](./diagrams/normal-order-sequence.svg) | [查看 PNG](./diagrams/legacy-png/normal-order-sequence.png) |
| 统一支付：支付受理、回调与钱包更新 | 支付受理、回调分发、支付记录与钱包状态更新 | [查看 SVG](./diagrams/unified-pay-sequence.svg) | [查看 PNG](./diagrams/legacy-png/unified-pay-sequence.png) |
| 订单超时取消与库存回滚 | 下单后发送延迟消息、超时未支付自动取消、库存与资格回滚 | [查看 SVG](./diagrams/order-timeout-cancel-stock-rollback-chain.svg) | SVG only |
| 主动取消 / 退款与钱包补偿 | 用户主动取消或退款后的库存回滚、退款 MQ 与钱包流水 | [查看 SVG](./diagrams/order-refund-wallet-compensation-chain.svg) | SVG only |
| 订单核销、店铺销量与积分奖励 | verifyShopId 校验、核销后销量增长、消费积分异步发放 | [查看 SVG](./diagrams/order-verification-points-reward-chain.svg) | SVG only |

### 积分与用户激励

这组聚焦签到、抽奖、消费奖励这些用户增长和激励机制。

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| 每日签到积分 | 签到、积分发放、幂等与奖励计算 | [查看 SVG](./diagrams/daily-signin-points-sequence.svg) | [查看 PNG](./diagrams/legacy-png/daily-signin-points-sequence.png) |
| 积分抽奖 | 扣减积分、抽奖结果、奖品发放 | [查看 SVG](./diagrams/points-lottery-draw-sequence.svg) | [查看 PNG](./diagrams/legacy-png/points-lottery-draw-sequence.png) |

### 账户与基础设施

这组主要看登录态、网关透传、文件上传这类基础能力怎么支撑全站业务。

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| 登录鉴权与网关透传 | 短信/密码登录、Redis 登录态、Gateway 请求头透传 | [查看 SVG](./diagrams/auth-login-gateway-chain.svg) | SVG only |
| 头像上传与文件替换 | 文件类型校验、MinIO 上传、旧文件删除、登录缓存刷新 | [查看 SVG](./diagrams/file-upload-avatar-update-chain.svg) | SVG only |

### <a id="chains-search"></a>内容审核与搜索

这组适合看“内容怎么过审、怎么进搜索、用户又是怎么搜出来的”。

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| 发布审核与搜索 / 向量同步 | 提交待审、审核责任链、回调源服务、ES/Milvus/热榜更新 | [查看 SVG](./diagrams/publish-audit-search-sync-chain.svg) | SVG only |
| 审核中心责任链与业务回写 | 审核任务落库、敏感词 / AI / 人工审核、驳回通知 | [查看 SVG](./diagrams/audit-center-responsibility-chain.svg) | SVG only |
| 搜索读链路与热词沉淀 | ES 检索、LBS 排序、搜索历史与热搜榜 | [查看 SVG](./diagrams/search-read-lbs-ranking-chain.svg) | SVG only |

### <a id="chains-social"></a>社交与消息

这组重点看 Feed、私聊、系统通知三条社交消息链如何拆分协作。

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| 关注 Feed 推送与滚动读取 | 粉丝信箱写入、Pipeline 批量 ZSet、ScrollResult 读取聚合 | [查看 SVG](./diagrams/follow-feed-scroll-read-chain.svg) | SVG only |
| IM 私聊可靠投递 | 长连接、消息持久化、ACK / 重试 | [查看 SVG](./diagrams/im-private-message-reliable-delivery-sequence.svg) | [查看 PNG](./diagrams/legacy-png/im-private-message-reliable-delivery-sequence.png) |
| 系统通知入库与 IM 推送 | 多业务通知汇聚、通知落库、在线实时推送 | [查看 SVG](./diagrams/system-notice-im-push-chain.svg) | SVG only |

### <a id="chains-rank"></a>排行、热度与推荐

这组更偏“首页热门内容和热榜机制”，既看榜单是怎么读出来的，也看热度是怎么持续维护的。

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| 首页热门榜单聚合读链路 | 并发拉取热门店铺榜、热门代金券榜、热门团购榜、热门博客榜，再按首页分区聚合返回 | [查看 SVG](./diagrams/home-aggregation-recommend-recall-chain.svg) | SVG only |
| 首页热门榜单算分与热榜维护链路 | 上架、销量、互动变化如何推进四榜 calcQueue，经过增量洗牌与凌晨全量重建后持续写回 Redis 热榜 | [查看 SVG](./diagrams/home-hot-rank-score-maintenance-chain.svg) | SVG only |
| 互动数据回刷与双轨同步（业务视角） | Redis 热数据变化如何驱动回刷、检索同步与热度重算 | [查看 SVG](./diagrams/interaction-dual-track-sync-sequence.svg) | [查看 PNG](./diagrams/legacy-png/interaction-dual-track-sync-sequence.png) |
| 热榜增量维护与全量重建（业务视角） | calcQueue、Top N merge、榜单更新与凌晨全量兜底 | [查看 SVG](./diagrams/hot-rank-wash-rebuild-chain.svg) | SVG only |

### <a id="chains-ai"></a>AI 与经营

这组主要展示 AI 对话、商家经营分析和 AI 辅助建议是怎么落进真实业务里的。

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| AI 对话链路 | SSE 流式响应、意图路由、卡片事件 | [查看 SVG](./diagrams/ai-chat-sse-intent-routing-sequence.svg) | [查看 PNG](./diagrams/legacy-png/ai-chat-sse-intent-routing-sequence.png) |
| 店铺经营分析与 AI 经营建议 | 订单分析、评价分析、经营建议与差评关键词抽取合在同一组看 | [分析聚合](./diagrams/shop-analysis-aggregation-chain.svg) / [AI 建议](./diagrams/shop-suggest-ai-keywords-chain.svg) | SVG only |

### <a id="chains-cache"></a>缓存与性能专题

这组不是业务流程图，而是专门解释 Redis 分层缓存和性能优化设计。

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| Redis 缓存分层设计 | 列表、详情、计数、状态、热榜 / Feed 五层缓存拆分 | [查看 SVG](./diagrams/redis-layered-cache-architecture.svg) | SVG only |
| Feed / 列表 ZSet 缓存链路 | ZSet 排序视图、滚动分页、批量详情回填 | [查看 SVG](./diagrams/redis-feed-zset-cache-chain.svg) | SVG only |
| 详情页缓存读写链路 | 逻辑过期、空值缓存、互斥锁重建、写后删缓存 | [查看 SVG](./diagrams/redis-detail-cache-readwrite-chain.svg) | SVG only |

### <a id="chains-schedule"></a>调度与定时任务

这组不讲单次用户请求，而是把 XXL-JOB 体系单独拎出来：先看总览，再看 4 张任务域子图。

#### 第一层：总览图

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| XXL-JOB 定时任务体系总览 | 26 个定时任务如何拆到订单、秒杀、互动、热榜和销量同步六大类 | [查看 SVG](./diagrams/xxl-job-scheduler-overview.svg) | SVG only |

#### 第二层：4 张子图

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| 秒杀预热与库存校准链路 | 预热库存和详情缓存，后续再按订单销量校准 MySQL 与缓存 | [查看 SVG](./diagrams/seckill-preheat-stock-calibration-chain.svg) | SVG only |
| 热榜增量维护与全量重建链路（调度视角） | HotRankJobHandler / FullRebuildJobHandler 如何分发增量维护与全量重建 | [查看 SVG](./diagrams/hot-rank-wash-rebuild-chain.svg) | SVG only |
| 互动数据回刷与双轨同步链路（调度视角） | InteractionSyncXxlJob / SyncDataServiceImpl 如何回刷 MySQL 并联动热榜 | [查看 SVG](./diagrams/interaction-dual-track-sync-sequence.svg) | SVG only |
| 订单生命周期兜底处理链路 | 临期提醒、过期处理、库存销量回滚与退款补偿兜底 | [查看 SVG](./diagrams/order-lifecycle-fallback-chain.svg) | SVG only |

### 历史详细图补充

这组主要保留旧设计视角，方便继续深挖。

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| UGC 异步审核与分发 | 审核消息投递、责任链处理、回调源服务 | [查看 SVG](./diagrams/ugc-audit-flow.svg) | [查看 PNG](./diagrams/legacy-png/ugc-audit-flow.png) |
| Feed 动态扇出 | 发布动态、粉丝分发、读扩散 / 写扩散 | [查看 SVG](./diagrams/feed-fanout-sequence.svg) | [查看 PNG](./diagrams/legacy-png/feed-fanout-sequence.png) |
| 搜索与向量库同步 | ES 索引同步、Milvus 向量写入、异步一致性 | [查看 SVG](./diagrams/search-es-milvus-sync-sequence.svg) | [查看 PNG](./diagrams/legacy-png/search-es-milvus-sync-sequence.png) |

## 7. 如何继续阅读

- 想把项目跑起来：看 [OPEN_SOURCE.md](OPEN_SOURCE.md)
- 想做本地或服务器部署：看 [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
- 想参与贡献：看 [../CONTRIBUTING.md](../CONTRIBUTING.md)
- 想了解安全和密钥边界：看 [../SECURITY.md](../SECURITY.md)
