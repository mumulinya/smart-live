# SmartLive 页面导览

这份文档专门承接 [README.md](../README.md) 里的“效果预览”。

- README 只保留最主要的页面入口，方便第一次进仓库的人快速扫一遍。
- 这里继续展开用户端 App、管理端 UI、待补截图位和源码位置，方便你后面持续补图。
- 用户端页面来源目录：`../smart-live-app/src/views/`
- 管理端页面来源目录：`../smartLive-ui/src/views/`

## 快速跳转

- [用户端 App 核心入口](#app-pages)
- [管理端 UI 核心入口](#admin-pages)
- [本轮补充的重要页面](#new-important-pages)
- [第一批待补截图位](#placeholder-batch-one)
- [第二批可继续补图](#placeholder-batch-two)

## <a id="app-pages"></a>1. 用户端 App 核心入口

### <a id="app-login-entry"></a>1.1 登录与身份进入

![登录页](./screenshots/login-page.png)

- 当前截图：`docs/screenshots/login-page.png`
- 来源页面：`../smart-live-app/src/views/user/auth/Login.vue`
- 建议重点：登录态建立、账号入口、首次进入项目的身份切换感知

### <a id="app-home-hot"></a>1.2 首页四榜与热榜聚合

| 首页聚合流 | 首页四榜热榜 |
|:---:|:---:|
| ![首页聚合流](./screenshots/homepage.png) | ![首页热门榜单](./screenshots/hot-ranking.png) |

- 当前截图：`docs/screenshots/homepage.png`、`docs/screenshots/hot-ranking.png`
- 来源页面：`../smart-live-app/src/views/home/Index.vue`
- 建议重点：热门店铺榜、热门代金券榜、热门团购榜、热门博客榜的首页聚合呈现

### <a id="app-search-map"></a>1.3 搜索结果与 LBS 找店

| 搜索结果 | 地图找店 |
|:---:|:---:|
| ![搜索结果](./screenshots/search-results.png) | ![地图找店](./screenshots/map-view.png) |

- 当前截图：`docs/screenshots/search-results.png`、`docs/screenshots/map-view.png`
- 来源页面：`../smart-live-app/src/views/search/Index.vue`、`../smart-live-app/src/views/map/Index.vue`
- 建议重点：全文检索、筛选排序、地图分布和附近店铺发现路径

### <a id="app-shop-product"></a>1.4 店铺与商品详情

| 店铺详情页 | 商品详情页 | 内容创作发布 |
|:---:|:---:|:---:|
| ![店铺详情页](./screenshots/shop-detail.png) | ![商品详情页](./screenshots/product-detail.png) | ![内容创作发布](./screenshots/publish-page.png) |

- 当前截图：`docs/screenshots/shop-detail.png`、`docs/screenshots/product-detail.png`、`docs/screenshots/publish-page.png`
- 来源页面：`../smart-live-app/src/views/shop/Detail.vue`、`../smart-live-app/src/views/product/Detail.vue`、`../smart-live-app/src/views/review/Publish.vue`
- 建议重点：用户决策下单、商品选择、探店内容发布与评价入口

### <a id="app-social-message"></a>1.5 社交消息与即时通讯

| 动态关注流 | 消息会话列表 | 即时通讯聊天 |
|:---:|:---:|:---:|
| ![动态关注流](./screenshots/feed-flow.png) | ![消息会话列表](./screenshots/app-chat-list.png) | ![即时通讯聊天](./screenshots/app-chat-detail.png) |

- 当前截图：`docs/screenshots/feed-flow.png`、`docs/screenshots/app-chat-list.png`、`docs/screenshots/app-chat-detail.png`
- 来源页面：`../smart-live-app/src/views/home/Index.vue`、`../smart-live-app/src/views/chat/List.vue`、`../smart-live-app/src/views/chat/Detail.vue`
- 建议重点：关注动态、消息会话列表、系统通知入口和私聊详情页

### <a id="app-ai-order-assets"></a>1.6 AI、订单、钱包与积分

| AI 智能助手 | 我的订单 | 我的钱包 | 积分中心 |
|:---:|:---:|:---:|:---:|
| ![AI 智能助手](./screenshots/ai-chat.png) | ![我的订单](./screenshots/order-page.png) | ![我的钱包](./screenshots/wallet-page.png) | ![积分中心](./screenshots/points-page.png) |

- 当前截图：`docs/screenshots/ai-chat.png`、`docs/screenshots/order-page.png`、`docs/screenshots/wallet-page.png`、`docs/screenshots/points-page.png`
- 来源页面：`../smart-live-app/src/views/ai/index.vue`、`../smart-live-app/src/views/order/Index.vue`、`../smart-live-app/src/views/user/wallet/Index.vue`、`../smart-live-app/src/views/user/points/Index.vue`
- 建议重点：用户端 AI 对话、订单状态流转、钱包资产、签到抽奖与积分激励

### <a id="app-growth-content"></a>1.7 用户成长与内容沉淀

| 我的发布/笔记 | 草稿箱 | 我的收藏 | 关注/粉丝列表 |
|:---:|:---:|:---:|:---:|
| ![我的发布/笔记](./screenshots/user-posts.png) | ![草稿箱](./screenshots/draft-box.png) | ![我的收藏](./screenshots/user-favorites.png) | ![关注/粉丝列表](./screenshots/follow-list.png) |

- 当前截图：`docs/screenshots/user-posts.png`、`docs/screenshots/draft-box.png`、`docs/screenshots/user-favorites.png`、`docs/screenshots/follow-list.png`
- 来源页面：`../smart-live-app/src/views/user/content/MyMoments.vue`、`../smart-live-app/src/views/draft/Index.vue`、`../smart-live-app/src/views/user/content/MyStar.vue`、`../smart-live-app/src/views/user/social/MyFollow.vue`
- 建议重点：用户内容沉淀、草稿管理、收藏资产与社交关系页

### <a id="app-profile-discovery"></a>1.8 个人主页与发现补充

| 用户搜索 | 个人中心（含顶部搜索按钮） | 店铺分类列表 | 签到与抽奖 |
|:---:|:---:|:---:|:---:|
| ![用户搜索](./screenshots/user-search.png) | ![个人中心](./screenshots/profile-page.png) | ![店铺分类列表](./screenshots/shop-list.png) | ![签到与抽奖](./screenshots/sign-in.png) |

- 当前截图：`docs/screenshots/user-search.png`、`docs/screenshots/profile-page.png`、`docs/screenshots/shop-list.png`、`docs/screenshots/sign-in.png`
- 来源页面：`../smart-live-app/src/views/search/UserSearch.vue`、`../smart-live-app/src/views/user/profile/Info.vue`、`../smart-live-app/src/views/shop/List.vue`、`../smart-live-app/src/views/user/points/Lottery.vue`
- 建议重点：找人、看人、个人中心顶部搜索按钮、按分类逛店以及签到/抽奖这一套用户发现与成长路径
- 补充说明：个人中心页右上角搜索按钮会直接跳转到 `'/search/user'`，这一点值得在截图里保留下来。

### <a id="app-promo-content"></a>1.9 活动与互动补充

| 限时秒杀专区 | 内容创作发布 | 互动评论/评价 | 系统通知/违规过滤 |
|:---:|:---:|:---:|:---:|
| ![限时秒杀专区](./screenshots/seckill-page.png) | ![内容创作发布](./screenshots/publish-page.png) | ![互动评论/评价](./screenshots/comment-section.png) | ![系统通知/违规过滤](./screenshots/system-notification.png) |

- 当前截图：`docs/screenshots/seckill-page.png`、`docs/screenshots/publish-page.png`、`docs/screenshots/comment-section.png`、`docs/screenshots/system-notification.png`
- 来源页面：`../smart-live-app/src/views/deal/Index.vue`、`../smart-live-app/src/views/review/Publish.vue`、`../smart-live-app/src/views/review/Detail.vue`、`../smart-live-app/src/views/chat/SystemNotice.vue`
- 建议重点：营销活动页、内容创作入口、评论评价详情和系统通知卡片流

## <a id="admin-pages"></a>2. 管理端 UI 核心入口

这一组先保留后台入口索引；本轮新补的后台真实截图集中放在 [3.2 管理端 UI](#new-important-pages) 里，方便你直接预览。

### <a id="admin-dashboard"></a>2.1 经营总览与仪表盘

| 管理端登录页 | 经营总览仪表盘 |
|:---:|:---:|
| ![管理端登录页](./screenshots/admin-login.png) | ![经营总览仪表盘](./screenshots/admin-dashboard.png) |

- 当前截图：`docs/screenshots/admin-login.png`、`docs/screenshots/admin-dashboard.png`
- 来源页面：`../smartLive-ui/src/views/login.vue`、`../smartLive-ui/src/views/index.vue`
- 建议重点：后台入口边界、验证码登录、经营总览、核心指标和后台首屏形态

### <a id="admin-business"></a>2.2 店铺、商品与营销管理

| 页面 | 预留文件名 | 来源页面 | 建议展示重点 |
|:---|:---|:---|:---|
| 店铺管理 | `docs/screenshots/admin-shop-manage.png` | `../smartLive-ui/src/views/business/shop/index.vue` | 店铺列表、审核状态、上下架动作 |
| 商品管理（含代金券 / 团购 / 秒杀） | `docs/screenshots/admin-product-manage.png` | `../smartLive-ui/src/views/business/product/index.vue` | 商品列表、活动类型、库存状态，以及营销商品统一管理 |
| 订单管理 | `docs/screenshots/admin-order-manage.png` | `../smartLive-ui/src/views/business/order/index.vue` | 订单筛选、履约状态、售后入口 |

- 补充说明：当前后台动态菜单和可用接口都以 `商品管理` 为准；历史上的独立代金券页已经不再挂到当前后台菜单，相关能力已经收敛到商品管理里统一维护。

### <a id="admin-audit-governance"></a>2.3 审核中心与内容治理

| 页面 | 预留文件名 | 来源页面 | 建议展示重点 |
|:---|:---|:---|:---|
| 审核中心 | `docs/screenshots/admin-audit-center.png` | `../smartLive-ui/src/views/system/audit/index.vue` | 内容待审、驳回原因、审核结果回写 |
| 评价管理 | `docs/screenshots/admin-review-manage.png` | `../smartLive-ui/src/views/business/review/index.vue` | 评价列表、违规处理、审核链路落点 |
| 评论管理 | `docs/screenshots/admin-comment-manage.png` | `../smartLive-ui/src/views/business/comment/index.vue` | 评论治理、内容清理、互动治理能力 |
| 通知公告 | `docs/screenshots/admin-notice-manage.png` | `../smartLive-ui/src/views/system/notice/index.vue` | 系统公告和后台运营通知 |

### <a id="admin-ai-assistant"></a>2.4 AI 经营助手与系统后台

| 页面 | 预留文件名 | 来源页面 | 建议展示重点 |
|:---|:---|:---|:---|
| AI 经营助手 | `docs/screenshots/admin-ai-assistant.png` | `../smartLive-ui/src/views/business/ai/index.vue` | 商家侧 AI 建议、文案、回复等能力展示 |
| 用户管理 | `docs/screenshots/admin-user-manage.png` | `../smartLive-ui/src/views/system/user/index.vue` | 系统用户、角色权限、账号管理 |
| 抽奖配置 | `docs/screenshots/admin-lottery-config.png` | `../smartLive-ui/src/views/system/points/LotteryConfig.vue` | 积分抽奖、奖品配置、活动参数 |
| 在线监控 | `docs/screenshots/admin-online-monitor.png` | `../smartLive-ui/src/views/monitor/online/index.vue` | 在线用户、会话状态、后台监控入口 |

## <a id="new-important-pages"></a>3. 本轮补充的重要页面

这组是我重新扫了一遍 `../smart-live-app` 和 `../smartLive-ui` 后，认为值得单独展示、但上一版文档还没明确露出来的页面。

### 3.1 用户端 App

#### 支付与钱包资产

| 支付收银台 | 支付结果页 | 钱包充值页 | 支付明细页 |
|:---:|:---:|:---:|:---:|
| ![支付收银台](./screenshots/app-pay-checkout.png)<br>统一支付入口 | ![支付结果页](./screenshots/app-pay-result.png)<br>支付结果态与回跳 | ![钱包充值页](./screenshots/app-wallet-recharge.png)<br>充值与扫码支付 | ![支付明细页](./screenshots/app-payment-record.png)<br>支付状态筛选 |

| 钱包账单页 | 订单详情页 | 我的订单 | 积分明细 |
|:---:|:---:|:---:|:---:|
| ![钱包账单页](./screenshots/app-wallet-bill.png)<br>收入支出流水 | ![订单详情页](./screenshots/app-order-detail.png)<br>履约状态与支付入口 | ![我的订单](./screenshots/order-page.png)<br>订单列表与状态过滤 | ![积分明细](./screenshots/app-points-detail.png)<br>积分收支明细 |

- `app-pay-checkout.png` -> `../smart-live-app/src/views/pay/Checkout.vue`
- `app-pay-result.png` -> `../smart-live-app/src/views/pay/Result.vue`
- `app-wallet-recharge.png` -> `../smart-live-app/src/views/user/wallet/Recharge.vue`
- `app-payment-record.png` -> `../smart-live-app/src/views/user/wallet/PaymentRecord.vue`
- `app-wallet-bill.png` -> `../smart-live-app/src/views/user/wallet/Bill.vue`
- `app-order-detail.png` -> `../smart-live-app/src/views/order/Detail.vue`
- `app-points-detail.png` -> `../smart-live-app/src/views/user/points/Detail.vue`

#### 内容与消息闭环

| 聊天会话列表 | 系统消息页 | 我的评价页 | 我的互动 |
|:---:|:---:|:---:|:---:|
| ![聊天会话列表](./screenshots/app-chat-list.png)<br>消息聚合入口 | ![系统消息页](./screenshots/app-system-notice.png)<br>订单/券/审核通知 | ![我的评价页](./screenshots/app-my-reviews.png)<br>评价状态筛选 | ![我的互动](./screenshots/app-my-interactions.png)<br>被赞/被评消息 |

| 待评价页 | 博客详情页 | AI 博客生成 | AI 评价生成 |
|:---:|:---:|:---:|:---:|
| ![待评价页](./screenshots/app-wait-review.png)<br>履约后去评价 | ![博客详情页](./screenshots/app-blog-detail.png)<br>内容详情与互动 | ![AI 博客生成](./screenshots/app-ai-blog-generate.png)<br>AIGC 探店内容 | ![AI 评价生成](./screenshots/app-ai-review-generate.png)<br>AIGC 消费评价 |

- `app-chat-list.png` -> `../smart-live-app/src/views/chat/List.vue`
- `app-system-notice.png` -> `../smart-live-app/src/views/chat/SystemNotice.vue`
- `app-blog-detail.png` -> `../smart-live-app/src/views/blog/Detail.vue`
- `app-my-reviews.png` -> `../smart-live-app/src/views/review/MyReviews.vue`
- `app-wait-review.png` -> `../smart-live-app/src/views/review/WaitReview.vue`
- `app-ai-blog-generate.png` -> `../smart-live-app/src/views/blog/AiBlogGenerate.vue`
- `app-ai-review-generate.png` -> `../smart-live-app/src/views/review/AiReviewGenerate.vue`
- `app-my-interactions.png` -> `../smart-live-app/src/views/user/social/MyInteractions.vue`

### 3.2 管理端 UI

#### 运营与治理后台

| 管理端登录页 | 博客管理 | 用户运营管理 | 店铺分类管理 |
|:---:|:---:|:---:|:---:|
| ![管理端登录页](./screenshots/admin-login.png)<br>后台入口与验证码登录 | ![博客管理](./screenshots/admin-blog-manage.png)<br>内容运营与详情弹窗 | ![用户运营管理](./screenshots/admin-business-user.png)<br>业务用户运营 | ![店铺分类管理](./screenshots/admin-shop-type.png)<br>首页分类配置 |

| 经营总览仪表盘 | 公告通知管理 | 在线监控 | 审核中心 |
|:---:|:---:|:---:|:---:|
| ![经营总览仪表盘](./screenshots/admin-dashboard.png)<br>经营看板首页 | ![公告通知管理](./screenshots/admin-notice-manage.png)<br>公告与通知运营 | ![在线监控](./screenshots/admin-online-monitor.png)<br>在线用户与强退 | ![审核中心](./screenshots/admin-audit-center.png)<br>审核流与驳回回写 |

| AI 经营助手 |
|:---:|
| ![AI 经营助手](./screenshots/admin-ai-assistant.png)<br>商家侧 AI 建议 |

- `admin-login.png` -> `../smartLive-ui/src/views/login.vue`
- `admin-blog-manage.png` -> `../smartLive-ui/src/views/business/blog/index.vue`
- `admin-business-user.png` -> `../smartLive-ui/src/views/business/user/index.vue`
- `admin-shop-type.png` -> `../smartLive-ui/src/views/business/shopType/index.vue`
- `admin-notice-manage.png` -> `../smartLive-ui/src/views/system/notice/index.vue`
- `admin-online-monitor.png` -> `../smartLive-ui/src/views/monitor/online/index.vue`
- `admin-audit-center.png` -> `../smartLive-ui/src/views/system/audit/index.vue`
- `admin-ai-assistant.png` -> `../smartLive-ui/src/views/business/ai/index.vue`
- `admin-dashboard.png` -> `../smartLive-ui/src/views/index.vue`

#### 业务与履约后台

| 店铺管理 | 商品管理 | 订单管理 | 评价管理 |
|:---:|:---:|:---:|:---:|
| ![店铺管理](./screenshots/admin-shop-manage.png)<br>店铺列表与审核状态 | ![商品管理](./screenshots/admin-product-manage.png)<br>商品与营销商品统一管理 | ![订单管理](./screenshots/admin-order-manage.png)<br>履约状态与筛选 | ![评价管理](./screenshots/admin-review-manage.png)<br>评价治理与状态回写 |

| 评论管理 | 抽奖配置 | 店铺分类管理 | 用户运营管理 |
|:---:|:---:|:---:|:---:|
| ![评论管理](./screenshots/admin-comment-manage.png)<br>评论治理与清理 | ![抽奖配置](./screenshots/admin-lottery-config.png)<br>奖品配置与库存概览 | ![店铺分类管理](./screenshots/admin-shop-type.png)<br>首页分类配置 | ![用户运营管理](./screenshots/admin-business-user.png)<br>业务用户运营 |

- `admin-shop-manage.png` -> `../smartLive-ui/src/views/business/shop/index.vue`
- `admin-product-manage.png` -> `../smartLive-ui/src/views/business/product/index.vue`
- `admin-order-manage.png` -> `../smartLive-ui/src/views/business/order/index.vue`
- `admin-review-manage.png` -> `../smartLive-ui/src/views/business/review/index.vue`
- `admin-comment-manage.png` -> `../smartLive-ui/src/views/business/comment/index.vue`
- `admin-lottery-config.png` -> `../smartLive-ui/src/views/system/points/LotteryConfig.vue`

## <a id="placeholder-batch-one"></a>4. 第一批待补截图位

这组目前已经补齐，原先优先级最高的页面都已经落成真实截图并挪到了上面的展示区。

- 用户端首批待补页面已完成：支付、钱包、消息、评价、AIGC 内容和聊天详情都已经补图。
- 管理端首批待补页面已完成：登录页、总览页、店铺、商品、订单、审核、评价、评论和抽奖配置都已经补图。
- 后续如果继续补图，建议直接从 [第二批可继续补图](#placeholder-batch-two) 往下做。

## <a id="placeholder-batch-two"></a>5. 第二批可继续补图

### 用户端 App

- `../smart-live-app/src/views/chat/ChatHistoryCalendar.vue`
- `../smart-live-app/src/views/search/UserSearch.vue`
- `../smart-live-app/src/views/user/profile/OtherInfo.vue`
- `../smart-live-app/src/views/chat/ChatInfo.vue`
- `../smart-live-app/src/views/blog/Edit.vue`
- `../smart-live-app/src/views/user/profile/Edit.vue`
- `../smart-live-app/src/views/user/social/AddFriend.vue`

### 管理端 UI

- `../smartLive-ui/src/views/system/user/index.vue`
- `../smartLive-ui/src/views/system/config/index.vue`
- `../smartLive-ui/src/views/system/menu/index.vue`
- `../smartLive-ui/src/views/system/role/index.vue`
- `../smartLive-ui/src/views/system/logininfor/index.vue`
- `../smartLive-ui/src/views/system/operlog/index.vue`

## 6. 第三批可选页面

这一组不是首页最优先展示，但如果你后面想把“用户成长路径”和“后台治理能力”再做完整，会比较值得继续补。

### 用户端 App

| 页面 | 预留文件名 | 来源页面 | 为什么可以继续补 |
|:---|:---|:---|:---|
| AI 会话列表页 | `docs/screenshots/app-ai-session-list.png` | `../smart-live-app/src/views/ai/index.vue` | 现在文档只放了 1 张 AI 主对话图，但这个页面本身还有历史会话、新建会话、会话搜索和标题编辑，值得单独补一张 |
| AI 推荐结果卡片 | `docs/screenshots/app-ai-recommend-card.png` | `../smart-live-app/src/views/ai/index.vue` | 这张能把“查询店铺 / 商品后返回推荐卡片 + AI 推荐理由”真正展示出来，比纯聊天气泡更能说明用户端 AI 能力 |
| AI 下单结果与订单卡片 | `docs/screenshots/app-ai-order-card.png` | `../smart-live-app/src/views/ai/index.vue` | 代码里有 `orderId` 卡片、状态轮询和跳转订单详情，这部分现在文档还没展示出来 |
| AI 快捷提问与建议问题 | `docs/screenshots/app-ai-suggestion-prompts.png` | `../smart-live-app/src/views/ai/index.vue` | 页面里有 `getSuggestions()` 和快捷提问入口，适合补成“第一次进入 AI 页能看到什么” |

| 页面 | 预留文件名 | 来源页面 | 为什么可以继续补 |
|:---|:---|:---|:---|
| 热门店铺榜页 | `docs/screenshots/app-top-shops.png` | `../smart-live-app/src/views/shop/TopList.vue` | 能把首页热榜点击后的独立榜单页补完整 |
| 热门商品榜页 | `docs/screenshots/app-top-products.png` | `../smart-live-app/src/views/product/TopList.vue` | 和代金券 / 团购榜的运营展示形成闭环 |
| 他人主页 | `docs/screenshots/app-other-profile.png` | `../smart-live-app/src/views/user/profile/OtherInfo.vue` | 能展示用户社交访问和他人主页视角 |
| 资料编辑页 | `docs/screenshots/app-profile-edit.png` | `../smart-live-app/src/views/user/profile/Edit.vue` | 能体现个人中心不是静态展示，而是完整可编辑资料页 |
| 关注 / 粉丝明细页 | `docs/screenshots/app-user-list.png` | `../smart-live-app/src/views/user/social/UserList.vue` | 能补齐社交关系链路中的明细列表页 |
| 添加朋友页 | `docs/screenshots/app-add-friend.png` | `../smart-live-app/src/views/user/social/AddFriend.vue` | 能体现站内社交拓展入口 |
| 聊天信息页 | `docs/screenshots/app-chat-info.png` | `../smart-live-app/src/views/chat/ChatInfo.vue` | 能展示免打扰、聊天资料等会话设置页 |
| 聊天历史日历 | `docs/screenshots/app-chat-history-calendar.png` | `../smart-live-app/src/views/chat/ChatHistoryCalendar.vue` | 能展示 IM 历史消息按日期查找能力 |
| 博客编辑页 | `docs/screenshots/app-blog-edit.png` | `../smart-live-app/src/views/blog/Edit.vue` | 能把博客草稿、编辑和发布链路补完整 |
| 账号安全页 | `docs/screenshots/app-password-setting.png` | `../smart-live-app/src/views/user/auth/SetPassword.vue` / `../smart-live-app/src/views/user/auth/UpdatePassword.vue` | 能补充用户账号设置与安全入口 |

### 管理端 UI

| 页面 | 预留文件名 | 来源页面 | 为什么可以继续补 |
|:---|:---|:---|:---|
| 订单管理 | `docs/screenshots/admin-order-manage.png` | `../smartLive-ui/src/views/business/order/index.vue` | 详情弹窗里有订单进度、支付信息、用户信息，展示价值很高 |
| 评价管理 | `docs/screenshots/admin-review-manage.png` | `../smartLive-ui/src/views/business/review/index.vue` | 能体现评价治理、详情弹窗和 AI 生成入口 |
| 评论管理 | `docs/screenshots/admin-comment-manage.png` | `../smartLive-ui/src/views/business/comment/index.vue` | 能补齐评论治理与内容风控后台 |
| 积分记录管理 | `docs/screenshots/admin-points-records.png` | `../smartLive-ui/src/views/system/points/RecordList.vue` | 能把积分配置和积分流水后台都补全 |
| 登录日志 | `docs/screenshots/admin-logininfor.png` | `../smartLive-ui/src/views/system/logininfor/index.vue` | 更适合展示运维审计能力 |
| 操作日志 | `docs/screenshots/admin-operlog.png` | `../smartLive-ui/src/views/system/operlog/index.vue` | 能体现后台治理和操作追踪能力 |
| 参数配置 | `docs/screenshots/admin-config.png` | `../smartLive-ui/src/views/system/config/index.vue` | 适合展示系统配置中心能力 |
| 菜单权限 | `docs/screenshots/admin-menu.png` | `../smartLive-ui/src/views/system/menu/index.vue` | 能体现 RBAC 和后台菜单治理能力 |
