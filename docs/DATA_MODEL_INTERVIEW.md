# 数据建模与表关系怎么讲

> 这页是“面试 / 答辩视角”的压缩版，目标不是把 53 张表全讲一遍，而是帮你在 30 秒到 3 分钟内，把这套数据模型讲得清楚、讲得像真实工程。  
> 详细版请看：[数据模型与核心表关系](/site-pages/DATA_MODEL)。

## 1. 面试先讲哪 3 件事

### 1.1 先讲“按业务域拆表”，不是按页面堆表

这个项目当前核心业务与治理表一共 **53 张**，不是按“登录页、订单页、评价页”去堆表，而是按业务域拆成了几组：

- 用户与个人资产：`user / user_info / user_wallet / user_points_wallet / points_record`
- 店铺与商品：`shop / shop_type / product / shop_analysis_record`
- 交易履约：`order / payment_record / wallet_transaction`
- 内容互动：`blog / review / comment / like_record / star / follow`
- 消息会话：`chat_sessions / chat_messages / user_sessions / chat_system_notice`
- AI 与治理：`merchant_ai_session / merchant_ai_message / audit_task / sys_*`

### 1.2 再讲“订单是一张核心事实表”

如果面试官只允许你挑一张最重要的表，那就讲 `order`。

因为它同时串起了：

- 谁下单：`user_id`
- 买了什么：`product_id / shop_id`
- 钱怎么扣：`payment_record / wallet_transaction`
- 什么时候过期：`valid_start_time / expire_time`
- 是否已核销：核销状态
- 是否已评价：评价状态

也就是说，你这个项目的交易、履约、退款、评价回写，都是围着订单主表展开的。

### 1.3 最后讲“为什么大量采用软关联”

这套项目没有在数据库层大量建跨域外键，而是大量用：

- `user_id`
- `shop_id`
- `order_id`
- `source_type + source_id`
- `biz_type + biz_id`

原因很明确：

- 服务边界优先于数据库强绑定
- 跨服务一致性主要靠 `RabbitMQ + Redis 幂等 + ACK/NACK + 延迟/死信 + XXL-JOB` 兜底
- 评论、点赞、收藏、关注这类互动模型天然更适合多态软关联

## 2. 30 秒怎么讲

这个项目的数据模型不是按页面去堆表，而是按业务域拆成了用户资产、店铺商品、交易履约、内容互动、消息会话、AI 与治理几组。核心事实表是 `order`，它把支付、核销、退款、评价这些状态都串在一起；资金和积分则分别做成了两套资产系统。关系设计上我刻意避免了数据库层跨服务强外键，更多通过业务主键软关联来适配微服务拆分、异步消息和补偿机制。

## 3. 1 分钟怎么讲

这套项目当前核心业务与治理表一共 53 张，我在建模时优先按业务域拆，而不是按功能页面拆。比如用户域负责账号、资料、钱包和积分，店铺商品域负责门店、分类和商品，交易域则用 `order` 作为统一订单事实表承接下单、支付、核销、退款和评价状态。  

为了让交易链路闭环，我把钱包和积分拆成了两套独立资产系统：钱包负责资金收支，积分负责签到、抽奖和消费激励；这样后续接退款、返利和运营活动都不会混。互动层则通过 `review / comment / like_record / star / follow` 承接评价、评论、点赞、收藏和关注。  

关系建模上，我没有把系统做成“服务拆开了，数据库还绑死在一起”的结构，而是用业务主键软关联配合消息驱动和补偿机制，这样更符合微服务场景。

## 4. 最值得讲的 4 组表关系

### 4.1 用户 -> 订单 -> 钱包 -> 积分 -> 评价

<div align="center">
  <img src="./diagrams/er-transaction-asset-review.svg" alt="用户、订单、钱包、积分与评价关系图" width="100%">
</div>

这组关系最适合讲“交易闭环”：

- `user` 决定谁下单
- `order` 承接支付、核销、退款、评价状态
- `payment_record / wallet_transaction` 记录资金变化
- `user_points_wallet / points_record` 记录运营激励
- `review` 再回写订单是否已评价

### 4.2 店铺 -> 商品 -> 订单 -> 评价 -> 经营分析

<div align="center">
  <img src="./diagrams/er-shop-product-order-analysis.svg" alt="店铺、商品、订单、评价与经营分析关系图" width="100%">
</div>

这组关系最适合讲“商家经营不是简单 CRUD”：

- `shop / shop_type` 定义店铺和分类
- `product` 承接普通商品、代金券、团购、秒杀等统一模型
- `order` 把交易数据沉淀下来
- `review` 形成口碑数据
- `shop_analysis_record` 形成经营快照

### 4.3 博客 / 评价 -> 评论 / 点赞 / 收藏 / 关注 -> 通知

<div align="center">
  <img src="./diagrams/er-content-interaction-notice.svg" alt="博客、评价、互动与通知关系图" width="100%">
</div>

这组关系最适合讲“内容与社交互动模型”：

- `blog / review` 是内容来源
- `comment / like_record / star / follow` 是通用互动模型
- `chat_system_notice` 承接系统回流通知

这里最值得讲的是：为什么用通用互动表，而不是给博客、店铺、评价各建一套互动表。

### 4.4 商家助手会话 -> 评价 / 商品 / 分析快照

<div align="center">
  <img src="./diagrams/er-merchant-ai-analysis.svg" alt="商家助手会话与经营分析关系图" width="100%">
</div>

这组关系最适合讲“AI 不是摆设，是和业务数据挂在一起的”：

- `merchant_ai_session / merchant_ai_message` 负责保存商家助手上下文
- 会话输入会引用商品、评价、店铺分析等业务数据
- `shop_analysis_record` 负责沉淀分析快照，支持复盘和回看

## 5. 面试高频追问怎么答

### 5.1 为什么不用数据库外键强绑定

因为这个项目不是单体库，而是按服务边界做拆分。跨服务一致性主要靠消息驱动、幂等和补偿机制解决，如果数据库层反而大量建强外键，会影响拆库、异步化和后续演进。

### 5.2 为什么订单能承接这么多状态

因为订单就是交易域的事实中心。支付、履约、退款、评价，本质上都围绕“这笔交易是否完成、是否兑现、是否关闭”展开，聚合在一张核心表里更利于状态机统一和后台回查。

### 5.3 为什么商品只用一张 `product` 表

普通商品、代金券、团购、秒杀虽然业务规则不同，但基础信息高度相似：店铺、售价、原价、库存、有效期、审核状态、使用规则这些字段都能复用。所以我采用统一商品表，再通过分类字段和活动类型字段表达差异。

### 5.4 为什么积分和钱包要拆开

因为这是两套不同资产系统。钱包偏资金，需要支付、退款、流水闭环；积分偏运营，需要签到、抽奖、返利和人工调整。拆开以后语义清晰，也更方便后续扩展。

## 6. 如果面试官继续深挖，你可以往哪几页接

1. 先回到 [项目级能力亮点](/site-pages/CORE_HIGHLIGHTS)，把“这个项目为什么值得继续问”讲清楚。
2. 再接 [我的核心设计与实现](/site-pages/CONTRIBUTIONS) 和 [性能指标与结果](/site-pages/PERFORMANCE)，把设计判断和工程表现讲出来。
3. 如果面试官开始追问具体表关系，再继续展开 [数据模型与核心表关系](/site-pages/DATA_MODEL) 详细版。
4. 最后把表关系和 [核心链路总览](/core-links/) 对上，说明订单、资产、互动和 AI 数据是怎么进入主链路的。

## 7. 推荐使用方式

- **打招呼 / 简历补充**：只用上面的 `30 秒版本`
- **面试开场**：优先讲 `1 分钟版本`
- **被追问“表怎么设计的”**：挑 `4.1` 和 `4.2` 两组关系展开
- **被追问“为什么这样建模”**：直接答 `第 5 节`
