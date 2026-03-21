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

- 前台用户端 App、后台管理端 Web 通过网关统一进入微服务体系。
- 中间层覆盖用户、店铺、商品、订单、互动、AI、IM、审核、积分、钱包等业务域。
- 基础设施层包含 Redis、RabbitMQ、Elasticsearch、Milvus、MinIO、XXL-JOB 等中间件。

## <a id="showcase-discovery"></a>2. 用户发现与决策

### 登录入口

<p align="center">
  <img src="./screenshots/login-page.png" alt="login" width="300">
</p>

统一登录入口，支持移动端用户鉴权。

### 搜索与地图

<p align="center">
  <img src="./screenshots/map-view.png" alt="map-view" width="300">
</p>

支持基于位置的找店和附近推荐。

### 搜索结果

<p align="center">
  <img src="./screenshots/search-results.png" alt="search-results" width="300">
</p>

支持全文检索、热词和排序策略。

### 首页入口

<p align="center">
  <img src="./screenshots/homepage.png" alt="homepage" width="300">
</p>

首页展示分类、优惠专区和秒杀入口。

### 热门内容流

<p align="center">
  <img src="./screenshots/hot-ranking.png" alt="hot-ranking" width="300">
</p>

热门页展示内容流与分区切换。

### 热门店铺榜

<p align="center">
  <img src="./screenshots/app-top-shops.png" alt="app-top-shops" width="300">
</p>

独立店铺热榜页展示热门商户。

### 热门商品榜

<p align="center">
  <img src="./screenshots/app-top-products.png" alt="app-top-products" width="300">
</p>

独立商品热榜页展示热门商品。

### 店铺详情

<p align="center">
  <img src="./screenshots/shop-detail.png" alt="shop-detail" width="300">
</p>

详情页承接店铺介绍、商品、评价与互动。

## <a id="showcase-trade"></a>3. 交易与增长

### 商品详情

<p align="center">
  <img src="./screenshots/product-detail.png" alt="product-detail" width="300">
</p>

商品详情承接下单、收藏、评价入口。

### 秒杀专区

<p align="center">
  <img src="./screenshots/seckill-page.png" alt="seckill-page" width="300">
</p>

秒杀专区对应 Redis Lua + MQ 异步削峰链路。

### 订单中心

<p align="center">
  <img src="./screenshots/order-page.png" alt="order-page" width="300">
</p>

订单中心展示待支付、进行中、已完成订单。

### 钱包中心

<p align="center">
  <img src="./screenshots/wallet-page.png" alt="wallet-page" width="300">
</p>

钱包展示余额、消费和支付记录。

### 积分中心

<p align="center">
  <img src="./screenshots/points-page.png" alt="points-page" width="300">
</p>

积分中心承接签到、抽奖和成长体系。

### 签到抽奖

<p align="center">
  <img src="./screenshots/sign-in.png" alt="sign-in" width="300">
</p>

连续签到与抽奖是增长玩法入口。

## <a id="showcase-social"></a>4. 内容、社交与消息

### 动态流

<p align="center">
  <img src="./screenshots/feed-flow.png" alt="feed-flow" width="300">
</p>

动态流展示关注内容与推荐内容混合分发。

### 发布页

<p align="center">
  <img src="./screenshots/publish-page.png" alt="publish-page" width="300">
</p>

支持内容创作与草稿管理。

### 评论区

<p align="center">
  <img src="./screenshots/comment-section.png" alt="comment-section" width="300">
</p>

评论和评价承接互动计数与审核链路。

### 我的发布

<p align="center">
  <img src="./screenshots/user-posts.png" alt="user-posts" width="300">
</p>

个人主页可查看已发布内容。

### 我的收藏

<p align="center">
  <img src="./screenshots/user-favorites.png" alt="user-favorites" width="300">
</p>

收藏列表承接内容回访与转化。

### 关注列表

<p align="center">
  <img src="./screenshots/follow-list.png" alt="follow-list" width="300">
</p>

关注与粉丝关系支撑 Feed 分发。

### 会话列表

<p align="center">
  <img src="./screenshots/app-chat-list.png" alt="app-chat-list" width="300">
</p>

会话列表承接私聊入口与未读状态。

### 系统通知

<p align="center">
  <img src="./screenshots/app-system-notice.png" alt="app-system-notice" width="300">
</p>

审核结果、系统提醒会通过站内通知下发。

### IM 私聊

<p align="center">
  <img src="./screenshots/app-chat-detail.png" alt="app-chat-detail" width="300">
</p>

私聊详情展示实时聊天与消息流转。

## <a id="showcase-ai"></a>5. AI 与用户画像

### AI 推荐结果卡片

<p align="center">
  <img src="./screenshots/app-ai-recommend-card.png" alt="app-ai-recommend-card" width="300">
</p>

AI 推荐卡片承接商品与团购推荐结果。

### AI 下单结果与订单卡片

<p align="center">
  <img src="./screenshots/app-ai-order-card.png" alt="app-ai-order-card" width="300">
</p>

下单结果会直接展示订单号、状态轮询和跳转入口。

### AI 会话列表

<p align="center">
  <img src="./screenshots/app-ai-session-list.png" alt="app-ai-session-list" width="300">
</p>

会话列表承接历史会话和快速恢复。

### 个人中心

<p align="center">
  <img src="./screenshots/profile-page.png" alt="profile-page" width="300">
</p>

个人中心聚合用户资料、内容与资产入口。

## <a id="showcase-chains"></a>6. 核心链路图集

这里统一提供双入口：`SVG` 看主链路展示，`PNG` 看更细的历史设计图。旧版 PNG 已归档到 [./diagrams/legacy-png](./diagrams/legacy-png)。

### <a id="chains-trade"></a>交易与履约

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| 秒杀抢购全链路 | Redis Lua 防超卖、RabbitMQ 异步落单、延迟队列兜底 | [查看 SVG](./diagrams/seckill-flow.svg) | [查看 PNG](./diagrams/legacy-png/seckill-flow.png) |
| 普通下单：订单创建与状态流转 | 下单创建、异步落单、支付生效后的状态流转 | [查看 SVG](./diagrams/normal-order-sequence.svg) | [查看 PNG](./diagrams/legacy-png/normal-order-sequence.png) |
| 统一支付：支付受理、回调与钱包更新 | 支付受理、回调分发、支付记录与钱包状态更新 | [查看 SVG](./diagrams/unified-pay-sequence.svg) | [查看 PNG](./diagrams/legacy-png/unified-pay-sequence.png) |
| 订单超时取消与库存回滚 | 下单后发送延迟消息、超时未支付自动取消、库存与资格回滚 | [查看 SVG](./diagrams/order-timeout-cancel-stock-rollback-chain.svg) | SVG only |
| 主动取消 / 退款与钱包补偿 | 用户主动取消或退款后的库存回滚、退款 MQ 与钱包流水 | [查看 SVG](./diagrams/order-refund-wallet-compensation-chain.svg) | SVG only |
| 订单核销、店铺销量与积分奖励 | verifyShopId 校验、核销后销量增长、消费积分异步发放 | [查看 SVG](./diagrams/order-verification-points-reward-chain.svg) | SVG only |

### 积分与用户激励

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| 每日签到积分 | 签到、积分发放、幂等与奖励计算 | [查看 SVG](./diagrams/daily-signin-points-sequence.svg) | [查看 PNG](./diagrams/legacy-png/daily-signin-points-sequence.png) |
| 积分抽奖 | 扣减积分、抽奖结果、奖品发放 | [查看 SVG](./diagrams/points-lottery-draw-sequence.svg) | [查看 PNG](./diagrams/legacy-png/points-lottery-draw-sequence.png) |

### 账户与基础设施

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| 登录鉴权与网关透传 | 短信/密码登录、Redis 登录态、Gateway 请求头透传 | [查看 SVG](./diagrams/auth-login-gateway-chain.svg) | SVG only |
| 头像上传与文件替换 | 文件类型校验、MinIO 上传、旧文件删除、登录缓存刷新 | [查看 SVG](./diagrams/file-upload-avatar-update-chain.svg) | SVG only |

### <a id="chains-search"></a>内容审核与搜索

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| 发布审核与搜索 / 向量同步 | 提交待审、审核责任链、回调源服务、ES/Milvus/热榜更新 | [查看 SVG](./diagrams/publish-audit-search-sync-chain.svg) | SVG only |
| 审核中心责任链与业务回写 | 审核任务落库、敏感词 / AI / 人工审核、驳回通知 | [查看 SVG](./diagrams/audit-center-responsibility-chain.svg) | SVG only |
| 搜索读链路与热词沉淀 | ES 检索、LBS 排序、搜索历史与热搜榜 | [查看 SVG](./diagrams/search-read-lbs-ranking-chain.svg) | SVG only |

### <a id="chains-social"></a>社交与消息

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| 关注 Feed 推送与滚动读取 | 粉丝信箱写入、Pipeline 批量 ZSet、ScrollResult 读取聚合 | [查看 SVG](./diagrams/follow-feed-scroll-read-chain.svg) | SVG only |
| IM 私聊可靠投递 | 长连接、消息持久化、ACK / 重试 | [查看 SVG](./diagrams/im-private-message-reliable-delivery-sequence.svg) | [查看 PNG](./diagrams/legacy-png/im-private-message-reliable-delivery-sequence.png) |
| 系统通知入库与 IM 推送 | 多业务通知汇聚、通知落库、在线实时推送 | [查看 SVG](./diagrams/system-notice-im-push-chain.svg) | SVG only |

### <a id="chains-rank"></a>排行、热度与推荐

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| 首页热门榜单聚合读链路 | 并发拉取热门店铺榜、热门代金券榜、热门团购榜、热门博客榜，再按首页分区聚合返回 | [查看 SVG](./diagrams/home-aggregation-recommend-recall-chain.svg) | SVG only |
| 首页热门榜单算分与热榜维护链路 | 上架、销量、互动变化如何推进四榜 calcQueue，经过增量洗牌与凌晨全量重建后持续写回 Redis 热榜 | [查看 SVG](./diagrams/home-hot-rank-score-maintenance-chain.svg) | SVG only |
| 互动数据回刷与双轨同步（业务视角） | Redis 热数据变化如何驱动回刷、检索同步与热度重算 | [查看 SVG](./diagrams/interaction-dual-track-sync-sequence.svg) | [查看 PNG](./diagrams/legacy-png/interaction-dual-track-sync-sequence.png) |
| 热榜增量维护与全量重建（业务视角） | calcQueue、Top N merge、榜单更新与凌晨全量兜底 | [查看 SVG](./diagrams/hot-rank-wash-rebuild-chain.svg) | SVG only |

### <a id="chains-ai"></a>AI 与经营

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| AI 对话链路 | SSE 流式响应、意图路由、卡片事件 | [查看 SVG](./diagrams/ai-chat-sse-intent-routing-sequence.svg) | [查看 PNG](./diagrams/legacy-png/ai-chat-sse-intent-routing-sequence.png) |
| 店铺经营分析与 AI 经营建议 | 订单分析、评价分析、经营建议与差评关键词抽取合在同一组看 | [分析聚合](./diagrams/shop-analysis-aggregation-chain.svg) / [AI 建议](./diagrams/shop-suggest-ai-keywords-chain.svg) | SVG only |

### <a id="chains-cache"></a>缓存与性能专题

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| Redis 缓存分层设计 | 列表、详情、计数、状态、热榜 / Feed 五层缓存拆分 | [查看 SVG](./diagrams/redis-layered-cache-architecture.svg) | SVG only |
| Feed / 列表 ZSet 缓存链路 | ZSet 排序视图、滚动分页、批量详情回填 | [查看 SVG](./diagrams/redis-feed-zset-cache-chain.svg) | SVG only |
| 详情页缓存读写链路 | 逻辑过期、空值缓存、互斥锁重建、写后删缓存 | [查看 SVG](./diagrams/redis-detail-cache-readwrite-chain.svg) | SVG only |

### <a id="chains-schedule"></a>调度与定时任务

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

| 链路 | 重点看什么 | 展示版 SVG | 详细版 PNG |
|:---|:---|:---|:---|
| UGC 异步审核与分发 | 审核消息投递、责任链处理、回调源服务 | [查看 SVG](./diagrams/ugc-audit-flow.svg) | [查看 PNG](./diagrams/legacy-png/ugc-audit-flow.png) |
| Feed 动态扇出 | 发布动态、粉丝分发、读扩散 / 写扩散 | [查看 SVG](./diagrams/feed-fanout-sequence.svg) | [查看 PNG](./diagrams/legacy-png/feed-fanout-sequence.png) |
| 搜索与向量库同步 | ES 索引同步、Milvus 向量写入、异步一致性 | [查看 SVG](./diagrams/search-es-milvus-sync-sequence.svg) | [查看 PNG](./diagrams/legacy-png/search-es-milvus-sync-sequence.png) |

## 7. 如何继续阅读

- 想按页面继续看用户端 App / 管理端 Web：看 [PAGE_GALLERY.md](PAGE_GALLERY.md)
- 想把项目跑起来：看 [OPEN_SOURCE.md](OPEN_SOURCE.md)
- 想做本地或服务器部署：看 [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
- 想参与贡献：看 [../CONTRIBUTING.md](../CONTRIBUTING.md)
- 想了解安全和密钥边界：看 [../SECURITY.md](../SECURITY.md)
