# SmartLive 视觉导览

这份文档聚焦“项目全景与关键链路怎么串起来”。它承接 [网站首页](/) 的核心入口，帮助第一次看到 SmartLive 的人，在几分钟内看清项目全景、核心页面和关键链路。

**文档导航：** [网站首页](/) · [页面导览](./PAGE_GALLERY.html) · [开源接入](./OPEN_SOURCE.html)

**第一次建议先看：** [项目全景](#showcase-overview) -> [用户进入、发现与找店](#showcase-discovery) -> [决策、交易与增长](#showcase-trade)

- 网站首页只保留精选入口，这页继续承接“全景 + 页面 + 链路”的完整视觉走查。
- 这页偏“系统与链路视角”，适合先建立整体认知；如果想逐页细看产品页面，可以跳去 [页面导览](./PAGE_GALLERY.html)。
- 如果你想看得更快：用户端建议从 `3 -> 6` 顺读，商家端建议从 `7.1 -> 7.6` 顺读，后台治理建议从 `8.1 -> 8.5` 顺读，链路图建议从 `9.1 -> 9.12` 选读。

## 视觉总览数据

<div class="smartlive-stats-grid">
  <div class="smartlive-stat-card">
    <div class="smartlive-stat-value">85 张</div>
    <div class="smartlive-stat-label">页面截图引用</div>
    <div class="smartlive-stat-desc">覆盖用户端、商家端、平台管理端与调度后台的页面视觉走查</div>
  </div>
  <div class="smartlive-stat-card">
    <div class="smartlive-stat-value">42 张</div>
    <div class="smartlive-stat-label">链路图引用</div>
    <div class="smartlive-stat-desc">覆盖交易、审核、社交、推荐、缓存、调度与 AI 经营等核心链路</div>
  </div>
  <div class="smartlive-stat-card">
    <div class="smartlive-stat-value">3 端</div>
    <div class="smartlive-stat-label">业务入口视角</div>
    <div class="smartlive-stat-desc">用户端 App、商家端 Web、平台管理端 Web 都能从页面和链路两侧看顺</div>
  </div>
  <div class="smartlive-stat-card">
    <div class="smartlive-stat-value">12 组</div>
    <div class="smartlive-stat-label">链路专题分组</div>
    <div class="smartlive-stat-desc">从交易履约、审核搜索到 Redis 缓存与 XXL-JOB 调度专题都已单独拆开</div>
  </div>
</div>

**按目标看图：**

<div class="smartlive-showcase-goals">
  <a href="#showcase-overview" class="smartlive-showcase-goal-card">
    <strong>第一次认识项目</strong>
    <span>先看项目全景，再顺着发现与交易主路径往下读。</span>
  </a>
  <a href="#chains-trade" class="smartlive-showcase-goal-card">
    <strong>想看交易闭环</strong>
    <span>重点看决策、交易与增长，再进入交易与履约图组。</span>
  </a>
  <a href="#chains-social" class="smartlive-showcase-goal-card">
    <strong>想看内容与社交</strong>
    <span>先看社交页面，再看社交消息图组和热榜推荐图组。</span>
  </a>
  <a href="#showcase-ai" class="smartlive-showcase-goal-card">
    <strong>想看 AI 与个人资产</strong>
    <span>先看 AI 页面，再进入审核搜索和 AI 经营图组。</span>
  </a>
  <a href="#chains-merchant" class="smartlive-showcase-goal-card">
    <strong>想看商家端经营链路</strong>
    <span>重点看订单履约、核销、经营分析和 AI 建议如何串起来。</span>
  </a>
  <a href="#chains-admin" class="smartlive-showcase-goal-card">
    <strong>想看后台治理链路</strong>
    <span>重点看审核中心、搜索同步和调度体系如何承接平台治理。</span>
  </a>
  <a href="#chains-cache" class="smartlive-showcase-goal-card">
    <strong>想看 Redis / 调度设计</strong>
    <span>直接看缓存性能专题，再看调度与定时任务总览。</span>
  </a>
</div>

## 1. 快速跳转

<div class="smartlive-showcase-quickjump">
  <a href="#showcase-overview" class="smartlive-showcase-quickjump-card"><strong>项目全景</strong><span>系统架构与全局认知</span></a>
  <a href="#showcase-discovery" class="smartlive-showcase-quickjump-card"><strong>发现与找店</strong><span>登录、首页、热榜、搜索、地图</span></a>
  <a href="#showcase-trade" class="smartlive-showcase-quickjump-card"><strong>交易与增长</strong><span>店铺、商品、秒杀、订单、钱包、积分</span></a>
  <a href="#showcase-social" class="smartlive-showcase-quickjump-card"><strong>内容与社交</strong><span>发布、评价、Feed、IM、系统消息</span></a>
  <a href="#showcase-ai" class="smartlive-showcase-quickjump-card"><strong>AI 与个人资产</strong><span>AI 会话、AIGC、个人中心与安全</span></a>
  <a href="#showcase-merchant" class="smartlive-showcase-quickjump-card"><strong>商家端页面走查</strong><span>经营总览、履约、AI 经营助手</span></a>
  <a href="#showcase-admin" class="smartlive-showcase-quickjump-card"><strong>后台管理端走查</strong><span>审核中心、治理后台、系统权限</span></a>
  <a href="#chains-trade" class="smartlive-showcase-quickjump-card"><strong>交易图组</strong><span>秒杀、订单、支付、退款补偿</span></a>
  <a href="#chains-merchant" class="smartlive-showcase-quickjump-card"><strong>商家端链路图</strong><span>经营分析、核销、订单履约</span></a>
  <a href="#chains-admin" class="smartlive-showcase-quickjump-card"><strong>后台治理图组</strong><span>审核中心、搜索同步、治理调度</span></a>
  <a href="#chains-search" class="smartlive-showcase-quickjump-card"><strong>审核与搜索图组</strong><span>审核责任链、搜索、向量同步</span></a>
  <a href="#chains-rank" class="smartlive-showcase-quickjump-card"><strong>热榜与推荐图组</strong><span>首页热门、算分维护、热榜重建</span></a>
  <a href="#chains-cache" class="smartlive-showcase-quickjump-card"><strong>缓存专题</strong><span>Redis 分层缓存与读写链路</span></a>
  <a href="#chains-schedule" class="smartlive-showcase-quickjump-card"><strong>调度专题</strong><span>XXL-JOB 总览与子链路</span></a>
</div>

<div class="smartlive-showcase-sections">

## <a id="showcase-overview"></a>2. 项目全景

<div align="center">
  <img src="./diagrams/system-architecture-overview.svg" alt="SmartLive 系统架构图" width="100%">
</div>

- 用户端 App、商家端 Web、平台管理端 Web 通过网关统一进入微服务体系。
- 中间层覆盖用户、店铺、商品、订单、互动、AI、IM、审核、积分、钱包等业务域。
- 基础设施层包含 Redis、RabbitMQ、Elasticsearch、Milvus、MinIO、XXL-JOB 等中间件。

## <a id="showcase-discovery"></a>3. 用户进入、发现与找店

### 3.1 登录入口

<p align="center">
  <img src="./screenshots/login-page.png" alt="login" width="300">
</p>

统一登录入口，负责建立移动端用户身份。

### 3.2 首页入口

<p align="center">
  <img src="./screenshots/homepage.png" alt="homepage" width="300">
</p>

首页承接分类、优惠专区、秒杀和后续榜单跳转。

### 3.3 热门内容流

<p align="center">
  <img src="./screenshots/hot-ranking.png" alt="hot-ranking" width="300">
</p>

热门页集中展示内容流与分区切换。

### 3.4 本地必吃榜（店铺热榜）

<p align="center">
  <img src="./screenshots/app-top-shops.png" alt="app-top-shops" width="300">
</p>

独立店铺热榜页展示本地必吃商户。

### 3.5 抢手好券榜

<p align="center">
  <img src="./screenshots/app-top-products.png" alt="app-top-products" width="300">
</p>

独立好券热榜页展示热门优惠商品。

### 3.6 抢手团购榜

<p align="center">
  <img src="./screenshots/app-top-group-deals.png" alt="app-top-group-deals" width="300">
</p>

独立团购热榜页展示热门团购套餐与排行。

### 3.7 搜索结果

<p align="center">
  <img src="./screenshots/search-results.png" alt="search-results" width="300">
</p>

搜索结果承接全文检索、热词和排序策略。

### 3.8 搜索与地图

<p align="center">
  <img src="./screenshots/map-view.png" alt="map-view" width="300">
</p>

地图模式把关键词搜索延伸到附近找店和位置分布。

## <a id="showcase-trade"></a>4. 决策、交易与增长

### 4.1 店铺详情

<p align="center">
  <img src="./screenshots/shop-detail.png" alt="shop-detail" width="300">
</p>

店铺详情承接介绍、商品、评价和互动信息。

### 4.2 商品详情

<p align="center">
  <img src="./screenshots/product-detail.png" alt="product-detail" width="300">
</p>

商品详情承接下单、收藏和评价入口。

### 4.3 秒杀专区

<p align="center">
  <img src="./screenshots/seckill-page.png" alt="seckill-page" width="300">
</p>

秒杀专区对应 Redis Lua 和 MQ 异步削峰链路。

### 4.4 优惠专区

<p align="center">
  <img src="./screenshots/discount-zone.png" alt="discount-zone" width="300">
</p>

优惠专区承接折扣商品、团购券和活动聚合入口。

### 4.5 订单中心

<p align="center">
  <img src="./screenshots/order-page.png" alt="order-page" width="300">
</p>

订单中心展示待支付、进行中和已完成订单。

### 4.6 钱包中心

<p align="center">
  <img src="./screenshots/wallet-page.png" alt="wallet-page" width="300">
</p>

钱包中心承接余额、支付记录和资产入口。

### 4.7 积分中心

<p align="center">
  <img src="./screenshots/points-page.png" alt="points-page" width="300">
</p>

积分中心承接签到、抽奖和成长体系。

### 4.8 签到抽奖

<p align="center">
  <img src="./screenshots/sign-in.png" alt="sign-in" width="300">
</p>

连续签到与抽奖是增长玩法入口。

## <a id="showcase-social"></a>5. 内容创作、社交与消息

### 5.1 发布与编辑

| 发布页 | 编辑页 |
|:---:|:---:|
| <img src="./screenshots/publish-page.png" alt="publish-page" width="230"> | <img src="./screenshots/app-blog-edit.png" alt="app-blog-edit" width="230"> |
| 新内容创建入口 | 已有笔记编辑与改稿 |

发布与编辑页共同承接图文创作、改稿和草稿管理。

### 5.2 评价互动

| 评价列表页 | 评价发布页 | 评价修改页 |
|:---:|:---:|:---:|
| <img src="./screenshots/app-review-list.png" alt="app-review-list" width="180"> | <img src="./screenshots/app-review-publish.png" alt="app-review-publish" width="180"> | <img src="./screenshots/app-review-edit.png" alt="app-review-edit" width="180"> |
| 商品页内查看网友评价 | 评分、文案、媒体上传与 AI 辅助生成 | 已发布评价再次编辑 |

| 评价详情页 | 评论区 | 我的评价页 |
|:---:|:---:|:---:|
| <img src="./screenshots/app-review-detail.png" alt="app-review-detail" width="180"> | <img src="./screenshots/comment-section.png" alt="comment-section" width="180"> | <img src="./screenshots/app-my-reviews.png" alt="app-my-reviews" width="180"> |
| 单条评价详情与评论承接 | 评论与评价承接互动计数 | 评价状态筛选 |

| 待评价页 | 评价草稿页 |
|:---:|:---:|
| <img src="./screenshots/app-wait-review.png" alt="app-wait-review" width="230"> | <img src="./screenshots/app-review-drafts.png" alt="app-review-drafts" width="230"> |
| 履约后去评价 | 待发布评价继续编辑 |

评价相关页面已经收拢为完整的评价链路分组。

### 5.3 我的发布

<p align="center">
  <img src="./screenshots/user-posts.png" alt="user-posts" width="300">
</p>

个人主页可查看已发布内容。

### 5.4 草稿箱

<p align="center">
  <img src="./screenshots/draft-box.png" alt="draft-box" width="300">
</p>

草稿箱承接未发布内容和二次编辑。

### 5.5 动态流

<p align="center">
  <img src="./screenshots/feed-flow.png" alt="feed-flow" width="300">
</p>

动态流展示关注内容与推荐内容混合分发。

### 5.6 我的关注

<p align="center">
  <img src="./screenshots/follow-list.png" alt="follow-list" width="300">
</p>

独立关注页按用户、店铺、商品三类关系组织关注资产。

### 5.7 我的收藏

<p align="center">
  <img src="./screenshots/user-favorites.png" alt="user-favorites" width="300">
</p>

独立收藏页承接店铺、笔记和商品三类收藏资产。

### 5.8 粉丝明细页

<p align="center">
  <img src="./screenshots/app-user-list.png" alt="app-user-list" width="300">
</p>

粉丝明细页承接个人主页下钻后的社交关系查看。

### 5.9 会话列表

<p align="center">
  <img src="./screenshots/app-chat-list.png" alt="app-chat-list" width="300">
</p>

会话列表承接私聊入口与未读状态。

### 5.10 系统通知

<p align="center">
  <img src="./screenshots/app-system-notice.png" alt="app-system-notice" width="300">
</p>

审核结果和系统提醒会通过站内通知下发。

### 5.11 IM 私聊

<p align="center">
  <img src="./screenshots/app-chat-detail.png" alt="app-chat-detail" width="300">
</p>

私聊详情展示实时聊天与消息流转。

## <a id="showcase-ai"></a>6. AI 与个人资产

### 6.1 AI 助手浮层

<p align="center">
  <img src="./screenshots/ai-chat.png" alt="ai-chat" width="300">
</p>

点击小精灵后会先进入 AI 助手浮层，承接快捷问题和历史能力入口。

### 6.2 AI 快捷提问页

<p align="center">
  <img src="./screenshots/app-ai-quick-ask.png" alt="app-ai-quick-ask" width="300">
</p>

快捷提问页展示建议问题、搜索引导和继续对话入口。

### 6.3 AI 店铺推荐卡片

<p align="center">
  <img src="./screenshots/app-ai-store-recommend.png" alt="app-ai-store-recommend" width="300">
</p>

店铺推荐结果会以卡片方式返回附近店铺与经营信息。

### 6.4 AI 商品推荐卡片

<p align="center">
  <img src="./screenshots/app-ai-product-recommend.png" alt="app-ai-product-recommend" width="300">
</p>

商品推荐结果会把团购券和套餐直接卡片化返回，支持继续追问和转下单。

### 6.5 AI 下单结果与订单卡片

<p align="center">
  <img src="./screenshots/app-ai-order-card.png" alt="app-ai-order-card" width="300">
</p>

下单结果会直接展示订单号、状态反馈和订单详情跳转入口。

### 6.6 AI 会话列表

<p align="center">
  <img src="./screenshots/app-ai-session-list.png" alt="app-ai-session-list" width="300">
</p>

会话列表承接历史会话、新建对话和上下文恢复。

### 6.7 AI 博客生成

<p align="center">
  <img src="./screenshots/app-ai-blog-generate.png" alt="app-ai-blog-generate" width="300">
</p>

AIGC 可直接生成探店文案草稿，并回填到发布页继续编辑。

### 6.8 AI 评价生成

<p align="center">
  <img src="./screenshots/app-ai-review-generate.png" alt="app-ai-review-generate" width="300">
</p>

AIGC 可根据评分和消费体验生成评价草稿，减少手动输入成本。

### 6.9 个人中心

<p align="center">
  <img src="./screenshots/profile-page.png" alt="profile-page" width="300">
</p>

个人中心聚合资料、内容和资产入口。

### 6.10 设置密码页

<p align="center">
  <img src="./screenshots/app-set-password.png" alt="app-set-password" width="300">
</p>

首次设置密码页用于补齐密码登录能力。

### 6.11 修改密码页

<p align="center">
  <img src="./screenshots/app-update-password.png" alt="app-update-password" width="300">
</p>

修改密码页承接旧密码校验和账号安全修改。

## <a id="showcase-merchant"></a>7. 商家端 Web 页面走查

### 7.1 登录入口

<p align="center">
  <img src="./screenshots/admin-login.png" alt="merchant-login" width="300">
</p>

商家端先从统一后台登录页进入，再进入自有经营工作台。

### 7.2 经营总览

<p align="center">
  <img src="./screenshots/admin-dashboard.png" alt="admin-dashboard" width="760">
</p>

经营总览负责承接店铺、订单、代金券、用户和评价的总体数据看板。

### 7.3 店铺经营流程

| 店铺列表页 | 新增店铺页 | 编辑店铺页 | 店铺详情页 |
|:---:|:---:|:---:|:---:|
| <img src="./screenshots/admin-shop-manage.png" alt="admin-shop-manage" width="180"> | <img src="./screenshots/merchant-shop-create.png" alt="merchant-shop-create" width="180"> | <img src="./screenshots/merchant-shop-edit.png" alt="merchant-shop-edit" width="180"> | <img src="./screenshots/merchant-shop-detail.png" alt="merchant-shop-detail" width="180"> |
| 店铺列表与状态管理 | 创建店铺、基础信息与地图定位 | 维护店铺资料、图片与定位 | 查看审核状态、经营信息与相册 |

### 7.4 商品经营流程

| 商品列表页 | 新建商品页 | 编辑商品页 | 商品详情页 |
|:---:|:---:|:---:|:---:|
| <img src="./screenshots/admin-product-manage.png" alt="admin-product-manage" width="180"> | <img src="./screenshots/merchant-product-create.png" alt="merchant-product-create" width="180"> | <img src="./screenshots/merchant-product-edit.png" alt="merchant-product-edit" width="180"> | <img src="./screenshots/merchant-product-detail.png" alt="merchant-product-detail" width="180"> |
| 商品列表、上下架与活动筛选 | 配置价格、活动类型与使用规则 | 调整库存、有效期与营销字段 | 查看售价、库存与时间设置 |

### 7.5 订单履约流程

| 订单管理 | 订单详情页 |
|:---:|:---:|
| <img src="./screenshots/admin-order-manage.png" alt="admin-order-manage" width="260"> | <img src="./screenshots/merchant-order-detail.png" alt="merchant-order-detail" width="260"> |
| 订单状态筛选、核销流转与售后入口 | 订单进度、支付信息、商品信息与用户信息查看 |

### 7.6 AI 经营助手流程

<p align="center">
  <img src="./screenshots/admin-ai-assistant.png" alt="admin-ai-assistant" width="760">
</p>

AI 商家助手先承接统一会话入口，再按评价回复、经营分析、营销文案和经营改进四类能力继续下钻。

| 评价回复页 | 经营状况分析页 | 商品营销文案页 | 经营改进建议页 |
|:---:|:---:|:---:|:---:|
| <img src="./screenshots/merchant-ai-review-reply.png" alt="merchant-ai-review-reply" width="180"> | <img src="./screenshots/merchant-ai-analysis.png" alt="merchant-ai-analysis" width="180"> | <img src="./screenshots/merchant-ai-copywriting.png" alt="merchant-ai-copywriting" width="180"> | <img src="./screenshots/merchant-ai-improve.png" alt="merchant-ai-improve" width="180"> |
| AI 辅助生成评价回复建议 | 汇总订单、评分和差评关键词做阶段分析 | 生成商品推广文案与营销表达 | 输出经营改进方向与执行建议 |

## <a id="showcase-admin"></a>8. 平台管理端 Web 页面走查

### 8.1 审核中心流程

| 审核中心 | 审核详情页 |
|:---:|:---:|
| <img src="./screenshots/admin-audit-center.png" alt="admin-audit-center" width="260"> | <img src="./screenshots/admin-audit-detail.png" alt="admin-audit-detail" width="260"> |
| 待审内容汇总、状态筛选与进入审核动作 | 单条审核内容、附件、关联目标与通过 / 驳回动作 |

### 8.2 内容治理流程

| 博客管理列表 | 博客详情页 | 评价管理列表 | 评价详情页 |
|:---:|:---:|:---:|:---:|
| <img src="./screenshots/admin-blog-manage.png" alt="admin-blog-manage" width="180"> | <img src="./screenshots/admin-blog-detail.png" alt="admin-blog-detail" width="180"> | <img src="./screenshots/admin-review-manage.png" alt="admin-review-manage" width="180"> | <img src="./screenshots/admin-review-detail.png" alt="admin-review-detail" width="180"> |
| 博客内容治理与状态筛选 | 单篇博客详情、内容与状态查看 | 评价治理、筛选与批量处理 | 单条评价详情、评分维度与附件查看 |

| 评论管理列表 | 评论详情页 |
|:---:|:---:|
| <img src="./screenshots/admin-comment-manage.png" alt="admin-comment-manage" width="260"> | <img src="./screenshots/admin-comment-detail.png" alt="admin-comment-detail" width="260"> |
| 评论清理、来源筛选与状态治理 | 单条评论内容、来源内容与审核状态查看 |

### 8.3 业务用户与积分运营

| 业务用户 | 业务用户详情页 | 抽奖配置 | 积分记录 |
|:---:|:---:|:---:|:---:|
| <img src="./screenshots/admin-business-user.png" alt="admin-business-user" width="180"> | <img src="./screenshots/admin-business-user-detail.png" alt="admin-business-user-detail" width="180"> | <img src="./screenshots/admin-lottery-config.png" alt="admin-lottery-config" width="180"> | <img src="./screenshots/admin-points-records.png" alt="admin-points-records" width="180"> |
| 业务用户列表与平台查看入口 | 用户资料、头像与基础信息查看 | 奖品配置、概率与上下架管理 | 积分变动流水与业务类型审计 |

### 8.4 系统管理与权限审计

| 用户管理 | 角色管理 | 菜单权限 |
|:---:|:---:|:---:|
| <img src="./screenshots/admin-user-manage.png" alt="admin-user-manage" width="180"> | <img src="./screenshots/admin-role-manage.png" alt="admin-role-manage" width="180"> | <img src="./screenshots/admin-menu.png" alt="admin-menu" width="180"> |
| 系统账号与权限管理 | 角色与权限字符维护 | 菜单树与按钮权限配置 |

| 参数设置 | 在线监控 | 登录日志 | 操作日志 |
|:---:|:---:|:---:|:---:|
| <img src="./screenshots/admin-config.png" alt="admin-config" width="180"> | <img src="./screenshots/admin-online-monitor.png" alt="admin-online-monitor" width="180"> | <img src="./screenshots/admin-logininfor.png" alt="admin-logininfor" width="180"> | <img src="./screenshots/admin-operlog.png" alt="admin-operlog" width="180"> |
| 系统参数与开关配置 | 在线用户与强退 | 登录审计与状态追踪 | 后台操作留痕与行为审计 |

### 8.5 调度后台流程

| 调度总览 | 任务管理 | 执行器管理 |
|:---:|:---:|:---:|
| <img src="./screenshots/admin-xxl-dashboard.png" alt="admin-xxl-dashboard" width="180"> | <img src="./screenshots/admin-xxl-job-manage.png" alt="admin-xxl-job-manage" width="180"> | <img src="./screenshots/admin-xxl-executors.png" alt="admin-xxl-executors" width="180"> |
| 任务、执行器与运行状态总览 | 按执行器查看 JobHandler、CRON 和状态 | 执行器配置与在线节点查看 |

## <a id="showcase-chains"></a>9. 核心链路图集

这里直接展示主链路预览图，详细实现继续保留 `详细版 SVG` 入口。

### 9.1 <a id="chains-trade"></a>交易与履约

| 链路 | 重点看什么 | 展示图 | 详细版入口 |
|:---|:---|:---|:---|
| 秒杀抢购全链路 | Redis Lua 防超卖、RabbitMQ 异步落单、延迟队列兜底 | <a href="./diagrams/seckill-flow.svg" target="_blank" rel="noreferrer"><img src="./diagrams/seckill-flow.svg" alt="秒杀抢购全链路" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/seckill-flow-detailed.svg) |
| 普通下单：订单创建与状态流转 | 下单创建、异步落单、支付生效后的状态流转 | <a href="./diagrams/normal-order-sequence.svg" target="_blank" rel="noreferrer"><img src="./diagrams/normal-order-sequence.svg" alt="普通下单：订单创建与状态流转" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/normal-order-sequence-detailed.svg) |
| 统一支付：支付受理、回调与账务分发 | 支付受理、回调分发、PaymentRecord 与充值入账 / 订单记账 | <a href="./diagrams/unified-pay-sequence.svg" target="_blank" rel="noreferrer"><img src="./diagrams/unified-pay-sequence.svg" alt="统一支付：支付受理、回调与账务分发" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/unified-pay-sequence-detailed.svg) |
| 订单超时取消与库存回滚 | 下单后发送延迟消息、超时未支付自动取消、库存与资格回滚 | <a href="./diagrams/order-timeout-cancel-stock-rollback-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/order-timeout-cancel-stock-rollback-chain.svg" alt="订单超时取消与库存回滚" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/order-timeout-cancel-stock-rollback-chain-detailed.svg) |
| 主动取消 / 退款与钱包补偿 | 用户主动取消或退款后的库存回滚、退款 MQ 与钱包流水 | <a href="./diagrams/order-refund-wallet-compensation-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/order-refund-wallet-compensation-chain.svg" alt="主动取消 / 退款与钱包补偿" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/order-refund-wallet-compensation-chain-detailed.svg) |
| 订单核销、店铺销量与积分奖励 | verifyShopId 校验、核销后销量增长、消费积分异步发放 | <a href="./diagrams/order-verification-points-reward-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/order-verification-points-reward-chain.svg" alt="订单核销、店铺销量与积分奖励" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/order-verification-points-reward-chain-detailed.svg) |

### 9.2 积分与用户激励

| 链路 | 重点看什么 | 展示图 | 详细版入口 |
|:---|:---|:---|:---|
| 每日签到积分 | 签到、积分发放、幂等与奖励计算 | <a href="./diagrams/daily-signin-points-sequence.svg" target="_blank" rel="noreferrer"><img src="./diagrams/daily-signin-points-sequence.svg" alt="每日签到积分" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/daily-signin-points-sequence-detailed.svg) |
| 积分抽奖 | 扣减积分、抽奖结果、奖品发放 | <a href="./diagrams/points-lottery-draw-sequence.svg" target="_blank" rel="noreferrer"><img src="./diagrams/points-lottery-draw-sequence.svg" alt="积分抽奖" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/points-lottery-draw-sequence-detailed.svg) |

### 9.3 账户与基础设施

| 链路 | 重点看什么 | 展示图 | 详细版入口 |
|:---|:---|:---|:---|
| 登录鉴权与网关透传 | 短信/密码登录、Redis 登录态、Gateway 请求头透传 | <a href="./diagrams/auth-login-gateway-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/auth-login-gateway-chain.svg" alt="登录鉴权与网关透传" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/auth-login-gateway-chain-detailed.svg) |
| 头像上传与文件替换 | 文件类型校验、MinIO 上传、旧文件删除、登录缓存刷新 | <a href="./diagrams/file-upload-avatar-update-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/file-upload-avatar-update-chain.svg" alt="头像上传与文件替换" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/file-upload-avatar-update-chain-detailed.svg) |

### 9.4 <a id="chains-search"></a>内容审核与搜索

| 链路 | 重点看什么 | 展示图 | 详细版入口 |
|:---|:---|:---|:---|
| 发布审核与搜索 / 向量同步 | 提交待审、审核责任链、回调源服务、ES/Milvus/热榜更新 | <a href="./diagrams/publish-audit-search-sync-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/publish-audit-search-sync-chain.svg" alt="发布审核与搜索 / 向量同步" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/publish-audit-search-sync-chain-detailed.svg) |
| 审核中心责任链与业务回写 | 审核任务落库、敏感词 / AI / 人工审核、驳回通知 | <a href="./diagrams/audit-center-responsibility-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/audit-center-responsibility-chain.svg" alt="审核中心责任链与业务回写" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/audit-center-responsibility-chain-detailed.svg) |
| 搜索读链路与热词沉淀 | ES 检索、LBS 排序、搜索历史与热搜榜 | <a href="./diagrams/search-read-lbs-ranking-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/search-read-lbs-ranking-chain.svg" alt="搜索读链路与热词沉淀" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/search-read-lbs-ranking-chain-detailed.svg) |

### 9.5 <a id="chains-social"></a>社交与消息

| 链路 | 重点看什么 | 展示图 | 详细版入口 |
|:---|:---|:---|:---|
| 关注 Feed 推送与滚动读取 | 粉丝信箱写入、Pipeline 批量 ZSet、ScrollResult 读取聚合 | <a href="./diagrams/follow-feed-scroll-read-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/follow-feed-scroll-read-chain.svg" alt="关注 Feed 推送与滚动读取" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/follow-feed-scroll-read-chain-detailed.svg) |
| IM 私聊可靠投递 | 长连接、消息持久化、ACK / 重试 | <a href="./diagrams/im-private-message-reliable-delivery-sequence.svg" target="_blank" rel="noreferrer"><img src="./diagrams/im-private-message-reliable-delivery-sequence.svg" alt="IM 私聊可靠投递" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/im-private-message-reliable-delivery-sequence-detailed.svg) |
| 系统通知入库与 IM 推送 | 多业务通知汇聚、通知落库、在线实时推送 | <a href="./diagrams/system-notice-im-push-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/system-notice-im-push-chain.svg" alt="系统通知入库与 IM 推送" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/system-notice-im-push-chain-detailed.svg) |

### 9.6 <a id="chains-rank"></a>排行、热度与推荐

| 链路 | 重点看什么 | 展示图 | 详细版入口 |
|:---|:---|:---|:---|
| 首页热门面板与热门博客读取链路 | 首页顶部热门面板按 activeHotTab 读取店铺榜 / 好券榜 / 团购榜，热门博客列表独立读取 | <a href="./diagrams/home-aggregation-recommend-recall-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/home-aggregation-recommend-recall-chain.svg" alt="首页热门面板与热门博客读取链路" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/home-aggregation-recommend-recall-chain-detailed.svg) |
| 首页热门面板算分与热榜维护链路 | 店铺榜、好券榜、团购榜和热门博客内容流如何进入 calcQueue，经过增量洗牌与凌晨全量重建后持续写回 Redis 热榜 | <a href="./diagrams/home-hot-rank-score-maintenance-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/home-hot-rank-score-maintenance-chain.svg" alt="首页热门面板算分与热榜维护链路" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/home-hot-rank-score-maintenance-chain-detailed.svg) |
| 互动数据回刷与双轨同步（业务视角） | Redis 热数据变化如何驱动回刷、检索同步与热度重算 | <a href="./diagrams/interaction-dual-track-sync-sequence.svg" target="_blank" rel="noreferrer"><img src="./diagrams/interaction-dual-track-sync-sequence.svg" alt="互动数据回刷与双轨同步（业务视角）" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/interaction-dual-track-sync-sequence-detailed.svg) |
| 热榜增量维护与全量重建（业务视角） | calcQueue、Top N merge、榜单更新与凌晨全量兜底 | <a href="./diagrams/hot-rank-wash-rebuild-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/hot-rank-wash-rebuild-chain.svg" alt="热榜增量维护与全量重建（业务视角）" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/hot-rank-wash-rebuild-chain-detailed.svg) |

### 9.7 <a id="chains-ai"></a>AI 与经营

| 链路 | 重点看什么 | 展示图 | 详细版入口 |
|:---|:---|:---|:---|
| AI 对话链路 | SSE 流式响应、意图路由、卡片事件 | <a href="./diagrams/ai-chat-sse-intent-routing-sequence.svg" target="_blank" rel="noreferrer"><img src="./diagrams/ai-chat-sse-intent-routing-sequence.svg" alt="AI 对话链路" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/ai-chat-sse-intent-routing-sequence-detailed.svg) |
| 店铺经营分析与 AI 经营建议 | 订单分析、评价分析、经营建议与差评关键词抽取合在同一组看 | <a href="./diagrams/shop-analysis-aggregation-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/shop-analysis-aggregation-chain.svg" alt="店铺经营分析" width="260"></a><br><a href="./diagrams/shop-suggest-ai-keywords-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/shop-suggest-ai-keywords-chain.svg" alt="AI 经营建议" width="260"></a> | [分析详细](./diagrams/detail-src/shop-analysis-aggregation-chain-detailed.svg) / [AI 建议详细](./diagrams/detail-src/shop-suggest-ai-keywords-chain-detailed.svg) |

### 9.8 <a id="chains-merchant"></a>商家端经营链路图

这里把商家端真正会看到的“经营、履约、核销、AI 建议”单独收拢，避免它们只散在交易图和 AI 图里不够直观。

| 链路 | 重点看什么 | 展示图 | 详细版入口 |
|:---|:---|:---|:---|
| 商家订单履约与核销 | 店员核销、店铺销量累积、消费积分奖励如何从订单完成后联动触发 | <a href="./diagrams/order-verification-points-reward-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/order-verification-points-reward-chain.svg" alt="商家订单履约与核销" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/order-verification-points-reward-chain-detailed.svg) |
| 商家经营分析聚合链路 | 店铺订单、评价、差评、销量和经营指标如何聚合成经营分析输入 | <a href="./diagrams/shop-analysis-aggregation-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/shop-analysis-aggregation-chain.svg" alt="商家经营分析聚合链路" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/shop-analysis-aggregation-chain-detailed.svg) |
| 商家 AI 经营建议链路 | 差评关键词、经营指标和店铺画像如何驱动改进建议与营销策略输出 | <a href="./diagrams/shop-suggest-ai-keywords-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/shop-suggest-ai-keywords-chain.svg" alt="商家 AI 经营建议链路" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/shop-suggest-ai-keywords-chain-detailed.svg) |
| 商家订单生命周期兜底 | 临期提醒、自动过期、退款补偿和库存回滚如何由调度链路统一收口 | <a href="./diagrams/order-lifecycle-fallback-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/order-lifecycle-fallback-chain.svg" alt="商家订单生命周期兜底" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/order-lifecycle-fallback-chain-detailed.svg) |

### 9.9 <a id="chains-admin"></a>平台管理端治理链路图

这一组对应后台管理端最核心的治理职责：内容审核、业务回写、检索同步和调度编排。

| 链路 | 重点看什么 | 展示图 | 详细版入口 |
|:---|:---|:---|:---|
| 审核中心责任链与业务回写 | 审核中心如何承接待审任务，经过规则、AI、人工后再回写业务源头 | <a href="./diagrams/audit-center-responsibility-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/audit-center-responsibility-chain.svg" alt="审核中心责任链与业务回写" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/audit-center-responsibility-chain-detailed.svg) |
| 发布审核与搜索 / 向量同步 | 内容审核通过后如何同步 ES、Milvus、热榜和搜索可见结果 | <a href="./diagrams/publish-audit-search-sync-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/publish-audit-search-sync-chain.svg" alt="发布审核与搜索向量同步" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/publish-audit-search-sync-chain-detailed.svg) |
| 平台调度总览 | XXL-JOB 如何统一承接审核回刷、热榜重建、订单兜底和库存校准 | <a href="./diagrams/xxl-job-scheduler-overview.svg" target="_blank" rel="noreferrer"><img src="./diagrams/xxl-job-scheduler-overview.svg" alt="平台调度总览" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/xxl-job-scheduler-overview-detailed.svg) |

### 9.10 <a id="chains-cache"></a>缓存与性能专题

| 链路 | 重点看什么 | 展示图 | 详细版入口 |
|:---|:---|:---|:---|
| Redis 缓存分层设计 | 列表、详情、计数、状态、热榜 / Feed 五层缓存拆分 | <a href="./diagrams/redis-layered-cache-architecture.svg" target="_blank" rel="noreferrer"><img src="./diagrams/redis-layered-cache-architecture.svg" alt="Redis 缓存分层设计" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/redis-layered-cache-architecture-detailed.svg) |
| Feed / 列表 ZSet 缓存链路 | ZSet 排序视图、滚动分页、批量详情回填 | <a href="./diagrams/redis-feed-zset-cache-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/redis-feed-zset-cache-chain.svg" alt="Feed / 列表 ZSet 缓存链路" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/redis-feed-zset-cache-chain-detailed.svg) |
| 详情页缓存读写链路 | 逻辑过期、空值缓存、互斥锁重建、写后删缓存 | <a href="./diagrams/redis-detail-cache-readwrite-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/redis-detail-cache-readwrite-chain.svg" alt="详情页缓存读写链路" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/redis-detail-cache-readwrite-chain-detailed.svg) |

### 9.11 <a id="chains-schedule"></a>调度与定时任务

这组主要看订单兜底、秒杀预热、互动回刷、热榜重建和销量同步如何由调度体系统一承接。真实后台页、任务列表和执行器截图可以继续看 [页面导览 - XXL-JOB 调度后台](./PAGE_GALLERY.html#admin-scheduler)。

#### 9.11.1 第一层：总览图

| 链路 | 重点看什么 | 展示图 | 详细版入口 |
|:---|:---|:---|:---|
| XXL-JOB 定时任务体系总览 | 28 个定时任务如何拆到订单、秒杀、互动、热榜和销量同步六大类 | <a href="./diagrams/xxl-job-scheduler-overview.svg" target="_blank" rel="noreferrer"><img src="./diagrams/xxl-job-scheduler-overview.svg" alt="XXL-JOB 定时任务体系总览" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/xxl-job-scheduler-overview-detailed.svg) |

#### 9.11.2 第二层：4 张子图

| 链路 | 重点看什么 | 展示图 | 详细版入口 |
|:---|:---|:---|:---|
| 秒杀预热与库存校准链路 | 预热库存和详情缓存，后续再按订单销量校准 MySQL 与缓存 | <a href="./diagrams/seckill-preheat-stock-calibration-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/seckill-preheat-stock-calibration-chain.svg" alt="秒杀预热与库存校准链路" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/seckill-preheat-stock-calibration-chain-detailed.svg) |
| 热榜增量维护与全量重建链路（调度视角） | HotRankJobHandler / FullRebuildJobHandler 如何分发增量维护与全量重建 | <a href="./diagrams/hot-rank-wash-rebuild-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/hot-rank-wash-rebuild-chain.svg" alt="热榜增量维护与全量重建链路（调度视角）" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/hot-rank-wash-rebuild-chain-detailed.svg) |
| 互动数据回刷与双轨同步链路（调度视角） | InteractionSyncXxlJob / SyncDataServiceImpl 如何回刷 MySQL 并联动热榜 | <a href="./diagrams/interaction-dual-track-sync-sequence.svg" target="_blank" rel="noreferrer"><img src="./diagrams/interaction-dual-track-sync-sequence.svg" alt="互动数据回刷与双轨同步链路（调度视角）" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/interaction-dual-track-sync-sequence-detailed.svg) |
| 订单生命周期兜底处理链路 | 临期提醒、过期处理、库存销量回滚与退款补偿兜底 | <a href="./diagrams/order-lifecycle-fallback-chain.svg" target="_blank" rel="noreferrer"><img src="./diagrams/order-lifecycle-fallback-chain.svg" alt="订单生命周期兜底处理链路" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/order-lifecycle-fallback-chain-detailed.svg) |

### 9.12 历史详细图补充

| 链路 | 重点看什么 | 展示图 | 详细版入口 |
|:---|:---|:---|:---|
| UGC 异步审核与分发 | 审核消息投递、责任链处理、回调源服务 | <a href="./diagrams/ugc-audit-flow.svg" target="_blank" rel="noreferrer"><img src="./diagrams/ugc-audit-flow.svg" alt="UGC 异步审核与分发" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/ugc-audit-flow-detailed.svg) |
| Feed 动态扇出 | 发布动态、粉丝分发、读扩散 / 写扩散 | <a href="./diagrams/feed-fanout-sequence.svg" target="_blank" rel="noreferrer"><img src="./diagrams/feed-fanout-sequence.svg" alt="Feed 动态扇出" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/feed-fanout-sequence-detailed.svg) |
| 搜索与向量库同步 | ES 索引同步、Milvus 向量写入、异步一致性 | <a href="./diagrams/search-es-milvus-sync-sequence.svg" target="_blank" rel="noreferrer"><img src="./diagrams/search-es-milvus-sync-sequence.svg" alt="搜索与向量库同步" width="260"></a> | [查看详细 SVG](./diagrams/detail-src/search-es-milvus-sync-sequence-detailed.svg) |

</div>

## 10. 如何继续阅读

- 想按页面继续看用户端 App / 商家端 Web / 平台管理端 Web：看 [页面导览](./PAGE_GALLERY.html)
- 想把项目跑起来：看 [开源接入](./OPEN_SOURCE.html)
- 想做本地或服务器部署：看 [部署指南](./DEPLOYMENT_GUIDE.html)
- 想参与贡献：看 [GitHub · CONTRIBUTING.md](https://github.com/mumulinya/smartLive-Cloud/blob/main/CONTRIBUTING.md)
- 想了解安全和密钥边界：看 [GitHub · SECURITY.md](https://github.com/mumulinya/smartLive-Cloud/blob/main/SECURITY.md)
