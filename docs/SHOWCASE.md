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

这批图用于细看系统设计，不适合直接压缩内嵌在首页里看。这里统一保留高清原图入口；其中最后补上的 5 张是 SVG，浏览器里可以直接放大看清楚。

| 链路 | 重点看什么 | 高清原图 |
|:---|:---|:---|
| 秒杀抢购全链路 | Redis Lua 防超卖、RabbitMQ 异步落单、延迟队列兜底 | [查看原图](./diagrams/seckill-flow.png) |
| UGC 异步审核与分发 | 审核消息投递、责任链处理、回调源服务 | [查看原图](./diagrams/ugc-audit-flow.png) |
| 统一支付全链路 | 下单、支付、回调、钱包状态更新 | [查看原图](./diagrams/unified-pay-sequence.png) |
| 每日签到积分 | 签到、积分发放、幂等与奖励计算 | [查看原图](./diagrams/daily-signin-points-sequence.png) |
| 积分抽奖 | 扣减积分、抽奖结果、奖品发放 | [查看原图](./diagrams/points-lottery-draw-sequence.png) |
| IM 私聊可靠投递 | 长连接、消息持久化、ACK / 重试 | [查看原图](./diagrams/im-private-message-reliable-delivery-sequence.png) |
| Feed 动态扇出 | 发布动态、粉丝分发、读扩散 / 写扩散 | [查看原图](./diagrams/feed-fanout-sequence.png) |
| 互动数据双轨同步 | Redis 热数据、异步回刷 MySQL、热度重算 | [查看原图](./diagrams/interaction-dual-track-sync-sequence.png) |
| 搜索与向量库同步 | ES 索引同步、Milvus 向量写入、异步一致性 | [查看原图](./diagrams/search-es-milvus-sync-sequence.png) |
| 普通下单链路 | 常规下单、支付、状态流转 | [查看原图](./diagrams/normal-order-sequence.png) |
| AI 对话链路 | SSE 流式响应、意图路由、卡片事件 | [查看原图](./diagrams/ai-chat-sse-intent-routing-sequence.png) |
| 登录鉴权与网关透传 | 短信/密码登录、Redis 登录态、Gateway 请求头透传 | [查看 SVG](./diagrams/auth-login-gateway-chain.svg) |
| 头像上传与文件替换 | 文件类型校验、MinIO 上传、旧文件删除、登录缓存刷新 | [查看 SVG](./diagrams/file-upload-avatar-update-chain.svg) |
| 发布审核与搜索 / 向量同步 | 提交待审、审核责任链、回调源服务、ES/Milvus/热榜更新 | [查看 SVG](./diagrams/publish-audit-search-sync-chain.svg) |
| 关注 Feed 推送与滚动读取 | 粉丝信箱写入、Pipeline 批量 ZSet、ScrollResult 读取聚合 | [查看 SVG](./diagrams/follow-feed-scroll-read-chain.svg) |
| 订单退款与钱包补偿 | 退款状态流转、库存回滚、MQ 退款消息、钱包入账流水 | [查看 SVG](./diagrams/order-refund-wallet-compensation-chain.svg) |

## 7. 如何继续阅读

- 想把项目跑起来：看 [OPEN_SOURCE.md](OPEN_SOURCE.md)
- 想做本地或服务器部署：看 [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
- 想参与贡献：看 [../CONTRIBUTING.md](../CONTRIBUTING.md)
- 想了解安全和密钥边界：看 [../SECURITY.md](../SECURITY.md)
