# SmartLive 视觉导览

这份文档只做一件事：让第一次看到 SmartLive 的人，先在几分钟内看懂“这个项目长什么样、覆盖了哪些业务、有哪些关键链路图”。

## 1. 项目全景

<div align="center">
  <img src="./screenshots/architecture.png" alt="SmartLive 系统架构图" width="100%">
</div>

- 前台用户端、后台管理端通过网关统一进入微服务体系。
- 中间层覆盖用户、店铺、商品、订单、互动、AI、IM、审核、积分、钱包等业务域。
- 基础设施层包含 Redis、RabbitMQ、Elasticsearch、Milvus、MinIO、XXL-JOB 等中间件。

## 2. 用户发现与决策

| 登录与首页 | 搜索与榜单 | 地图与店铺 |
|:---:|:---:|:---:|
| ![login](./screenshots/login-page.png) | ![search-results](./screenshots/search-results.png) | ![map-view](./screenshots/map-view.png) |
| 统一登录入口，支持移动端用户鉴权 | 支持全文检索、热词和排序策略 | 支持基于位置的找店和附近推荐 |

| 首页聚合流 | 热门榜单 | 店铺详情 |
|:---:|:---:|:---:|
| ![homepage](./screenshots/homepage.png) | ![hot-ranking](./screenshots/hot-ranking.png) | ![shop-detail](./screenshots/shop-detail.png) |
| 首页聚合商户、活动和分类入口 | 热度榜单直观展示热门内容和商户 | 详情页承接店铺介绍、商品、评价与互动 |

## 3. 交易与增长

| 商品详情 | 秒杀专区 | 订单中心 |
|:---:|:---:|:---:|
| ![product-detail](./screenshots/product-detail.png) | ![seckill-page](./screenshots/seckill-page.png) | ![order-page](./screenshots/order-page.png) |
| 商品详情承接下单、收藏、评价入口 | 秒杀专区对应 Redis Lua + MQ 异步削峰链路 | 订单中心展示待支付、进行中、已完成订单 |

| 钱包中心 | 积分中心 | 签到抽奖 |
|:---:|:---:|:---:|
| ![wallet-page](./screenshots/wallet-page.png) | ![points-page](./screenshots/points-page.png) | ![sign-in](./screenshots/sign-in.png) |
| 钱包展示余额、消费和支付记录 | 积分中心承接签到、抽奖和成长体系 | 连续签到与抽奖是增长玩法入口 |

## 4. 内容、社交与消息

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

## 5. AI 与用户画像

| AI 助手 | 用户行为检索 | 个人中心 |
|:---:|:---:|:---:|
| ![ai-chat](./screenshots/ai-chat.png) | ![user-search](./screenshots/user-search.png) | ![profile-page](./screenshots/profile-page.png) |
| AI 助手支持对话、推荐、经营辅助等场景 | 行为检索承接点赞、收藏、发布等数据回查 | 个人中心聚合用户资料、内容与资产入口 |

## 6. 核心链路图集

下面这些图适合和 README 中的“核心业务链路”一起看：

| 秒杀抢购 | UGC 审核 | AI 对话 |
|:---:|:---:|:---:|
| ![seckill](./diagrams/seckill-flow.png) | ![ugc-audit](./diagrams/ugc-audit-flow.png) | ![ai-sequence](./diagrams/ai-chat-sse-intent-routing-sequence.png) |
| Redis Lua + MQ 削峰的高并发主链路 | 审核中心的异步审核与回调链路 | SSE 流式响应、意图路由与卡片事件链路 |

| Feed 扇出 | IM 私聊可靠投递 | 互动数据双轨同步 |
|:---:|:---:|:---:|
| ![feed-fanout](./diagrams/feed-fanout-sequence.png) | ![im-sequence](./diagrams/im-private-message-reliable-delivery-sequence.png) | ![interaction-sync](./diagrams/interaction-dual-track-sync-sequence.png) |
| 关注关系与写扩散链路 | 消息投递、ACK 与重试链路 | Redis 热数据回刷 MySQL 的同步策略 |

其余链路图可以直接打开看原图：

- [统一支付链路](./diagrams/unified-pay-sequence.png)
- [每日签到积分链路](./diagrams/daily-signin-points-sequence.png)
- [积分抽奖链路](./diagrams/points-lottery-draw-sequence.png)
- [搜索与向量库同步链路](./diagrams/search-es-milvus-sync-sequence.png)
- [普通下单链路](./diagrams/normal-order-sequence.png)

## 7. 如何继续阅读

- 想把项目跑起来：看 [OPEN_SOURCE.md](OPEN_SOURCE.md)
- 想做本地或服务器部署：看 [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
- 想参与贡献：看 [../CONTRIBUTING.md](../CONTRIBUTING.md)
- 想了解安全和密钥边界：看 [../SECURITY.md](../SECURITY.md)
