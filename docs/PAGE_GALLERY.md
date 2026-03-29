# SmartLive 页面导览

这份文档聚焦“页面本身怎么呈现”。它承接 [网站首页](/) 的效果预览，按真实使用路径把用户端 App、商家端 Web 和平台管理端 Web 的核心页面一次看顺。

**文档导航：** [网站首页](/) · [视觉导览](./SHOWCASE.html) · [开源接入](./OPEN_SOURCE.html)

**第一次建议先看：** [登录与进入](#app-login-entry) -> [发现入口与热榜](#app-discovery) -> [店铺决策与商品详情](#app-shop-product) -> [支付、订单、钱包与积分](#app-trade-assets) -> [AI 智能助手与 AIGC](#app-ai-capability)

- 网站首页只保留最主要的页面入口，方便第一次进项目的人先快速建立印象。
- 这页偏“页面视角”，适合先看产品长什么样；如果想继续看系统全景和关键链路，可以跳去 [视觉导览](./SHOWCASE.html)。
- 推荐按“进入 -> 发现 -> 决策 -> 交易 -> 内容 -> 社交 -> AI -> 个人资产”的顺序往下读。

## 1. 快速跳转

<div class="smartlive-gallery-quickjump">
  <a href="#app-login-entry" class="smartlive-gallery-quickjump-card">
    <strong>用户端 App</strong>
    <span>登录与进入</span>
  </a>
  <a href="#app-discovery" class="smartlive-gallery-quickjump-card">
    <strong>用户端 App</strong>
    <span>发现入口与热榜</span>
  </a>
  <a href="#app-search-map" class="smartlive-gallery-quickjump-card">
    <strong>用户端 App</strong>
    <span>搜索与 LBS 找店</span>
  </a>
  <a href="#app-shop-product" class="smartlive-gallery-quickjump-card">
    <strong>用户端 App</strong>
    <span>店铺决策与商品详情</span>
  </a>
  <a href="#app-trade-assets" class="smartlive-gallery-quickjump-card">
    <strong>用户端 App</strong>
    <span>支付、订单、钱包与积分</span>
  </a>
  <a href="#app-content-creation" class="smartlive-gallery-quickjump-card">
    <strong>用户端 App</strong>
    <span>内容创作与评价互动</span>
  </a>
  <a href="#app-social-message" class="smartlive-gallery-quickjump-card">
    <strong>用户端 App</strong>
    <span>社交关系与消息</span>
  </a>
  <a href="#app-ai-capability" class="smartlive-gallery-quickjump-card">
    <strong>用户端 App</strong>
    <span>AI 智能助手与 AIGC</span>
  </a>
  <a href="#app-profile-assets" class="smartlive-gallery-quickjump-card">
    <strong>用户端 App</strong>
    <span>个人中心、收藏与安全设置</span>
  </a>
  <a href="#merchant-dashboard" class="smartlive-gallery-quickjump-card">
    <strong>商家端 Web</strong>
    <span>经营总览</span>
  </a>
  <a href="#merchant-business" class="smartlive-gallery-quickjump-card">
    <strong>商家端 Web</strong>
    <span>店铺与商品经营</span>
  </a>
  <a href="#merchant-growth" class="smartlive-gallery-quickjump-card">
    <strong>商家端 Web</strong>
<span>订单履约与详情</span>
  </a>
  <a href="#merchant-ai" class="smartlive-gallery-quickjump-card">
    <strong>商家端 Web</strong>
    <span>AI 经营助手</span>
  </a>
  <a href="#admin-governance" class="smartlive-gallery-quickjump-card">
    <strong>平台管理端 Web</strong>
    <span>内容治理与运营后台</span>
  </a>
  <a href="#admin-system" class="smartlive-gallery-quickjump-card">
    <strong>平台管理端 Web</strong>
    <span>系统与权限后台</span>
  </a>
  <a href="#admin-scheduler" class="smartlive-gallery-quickjump-card">
    <strong>调度后台</strong>
    <span>XXL-JOB 调度后台</span>
  </a>
</div>

## 2. 页面能力映射表

这张表不是替代截图，而是把“页面入口、后端模块和对应链路”串起来，方便从页面快速跳到源码和链路文档。

<div class="smartlive-capability-matrix">
  <table>
    <thead>
      <tr>
        <th>能力 / 页面</th>
        <th>主要前端页</th>
        <th>对应后端模块</th>
        <th>关键链路</th>
        <th>延伸阅读</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td><strong>首页发现与热门面板</strong></td>
        <td>首页入口、热门内容流、本地必吃榜、抢手好券榜</td>
        <td><code>shop / product / blog / index / interaction</code></td>
        <td>首页热门面板读取链路、热榜维护链路</td>
        <td><a href="./SHOWCASE.html">业务链路视觉走查</a></td>
      </tr>
      <tr>
        <td><strong>搜索与地图找店</strong></td>
        <td>搜索入口页、搜索结果页、地图找店</td>
        <td><code>search / shop / product / interaction</code></td>
        <td>LBS 搜索与热词链路</td>
        <td><a href="./core-links/index.html">核心链路总览</a></td>
      </tr>
      <tr>
        <td><strong>店铺决策与商品详情</strong></td>
        <td>店铺详情页、商品详情页、秒杀专区</td>
        <td><code>shop / product / order / interaction</code></td>
        <td>订单支付退款链路、秒杀抢购链路</td>
        <td><a href="./core-links/index.html">核心链路总览</a></td>
      </tr>
      <tr>
        <td><strong>支付、订单、钱包与积分</strong></td>
        <td>收银台、订单详情、钱包、积分中心</td>
        <td><code>order / wallet / points / product</code></td>
        <td>订单支付退款链路、订单超时取消链路</td>
        <td><a href="./SHOWCASE.html">业务链路视觉走查</a></td>
      </tr>
      <tr>
        <td><strong>内容创作与评价</strong></td>
        <td>发布页、博客详情、评论区、我的评价、草稿箱</td>
        <td><code>blog / interaction / audit / ai</code></td>
        <td>审核责任链与搜索双写、Feed 推送链路</td>
        <td><a href="./SHOWCASE.html">业务链路视觉走查</a></td>
      </tr>
      <tr>
        <td><strong>社交关系与消息</strong></td>
        <td>关注页、粉丝明细、会话列表、即时通讯、系统消息</td>
        <td><code>interaction / chat / im / user</code></td>
        <td>Feed 推送与互动同步、系统通知与 IM 推送</td>
        <td><a href="./core-links/index.html">核心链路总览</a></td>
      </tr>
      <tr>
        <td><strong>AI 智能助手与 AIGC</strong></td>
        <td>AI 会话、快捷提问、店铺推荐、商品推荐、下单卡片、博客生成、评价生成</td>
        <td><code>ai / search / shop / product / blog / interaction</code></td>
        <td>AI 路由策略与 RAG 生成链路</td>
        <td><a href="./core-links/index.html">核心链路总览</a></td>
      </tr>
      <tr>
        <td><strong>商家端经营后台</strong></td>
        <td>经营总览、店铺管理、商品管理、订单管理、AI 经营助手</td>
        <td><code>shop / product / order / ai</code></td>
        <td>订单支付退款链路、AI 路由与经营分析</td>
        <td><a href="./OPEN_SOURCE.html">开源接入说明</a></td>
      </tr>
      <tr>
        <td><strong>平台管理端治理后台</strong></td>
        <td>审核中心、博客管理、评论评价、抽奖配置、积分记录、角色权限、日志与监控</td>
        <td><code>system / audit / blog / user / points / monitor</code></td>
        <td>审核责任链、调度与治理链路</td>
        <td><a href="./OPEN_SOURCE.html">开源接入说明</a></td>
      </tr>
      <tr>
        <td><strong>XXL-JOB 调度后台</strong></td>
        <td>调度总览、任务管理、执行器管理</td>
        <td><code>common-xxl / product / order / interaction</code></td>
        <td>热榜重建、订单兜底、秒杀预热链路</td>
        <td><a href="./SHOWCASE.html">业务链路视觉走查</a></td>
      </tr>
    </tbody>
  </table>
</div>

<div class="smartlive-gallery-sections">

## <a id="app-pages"></a>3. 用户端 App 全链路展示

### <a id="app-login-entry"></a>3.1 登录与进入

<p align="center">
  <img src="./screenshots/login-page.png" alt="登录页" width="260">
</p>

登录页负责建立账号入口和用户登录态，是第一次进入 App 的起点。

### <a id="app-discovery"></a>3.2 发现入口与热榜

| 首页入口 | 热门内容流 |
|:---:|:---:|
| <img src="./screenshots/homepage.png" alt="首页入口" width="230"> | <img src="./screenshots/hot-ranking.png" alt="热门内容流" width="230"> |
| 分类入口、优惠专区和秒杀入口 | 热门页与内容分区切换 |

| 本地必吃榜（店铺热榜） | 抢手好券榜 | 抢手团购榜 |
|:---:|:---:|:---:|
| <img src="./screenshots/app-top-shops.png" alt="本地必吃榜（店铺热榜）" width="150"> | <img src="./screenshots/app-top-products.png" alt="抢手好券榜" width="150"> | <img src="./screenshots/app-top-group-deals.png" alt="抢手团购榜" width="150"> |
| 独立店铺热榜页 | 独立好券热榜页 | 独立团购热榜页 |

| 店铺分类列表 |
|:---:|
| <img src="./screenshots/shop-list.png" alt="店铺分类列表" width="260"> |
| 分类找店与按类浏览入口 |

### <a id="app-search-map"></a>3.3 搜索与 LBS 找店

| 搜索入口页 | 搜索结果页 |
|:---:|:---:|
| <img src="./screenshots/search-page.png" alt="搜索入口页" width="230"> | <img src="./screenshots/search-results.png" alt="搜索结果页" width="230"> |
| 搜索输入与热词入口 | 全文检索与排序筛选 |

| 地图找店 |
|:---:|
| <img src="./screenshots/map-view.png" alt="地图找店" width="260"> |
| 地图模式承接附近找店和 LBS 分布查看 |

### <a id="app-shop-product"></a>3.4 店铺决策与商品详情

| 店铺详情页 | 商品详情页 |
|:---:|:---:|
| <img src="./screenshots/shop-detail.png" alt="店铺详情页" width="230"> | <img src="./screenshots/product-detail.png" alt="商品详情页" width="230"> |
| 店铺介绍、评价与商品入口 | 商品详情与购买入口 |

| 限时秒杀专区 | 优惠专区 |
|:---:|:---:|
| <img src="./screenshots/seckill-page.png" alt="限时秒杀专区" width="230"> | <img src="./screenshots/discount-zone.png" alt="优惠专区" width="230"> |
| 秒杀活动入口与限时抢购承接页 | 优惠商品入口与专区聚合页 |

### <a id="app-trade-assets"></a>3.5 支付、订单、钱包与积分

| 支付收银台 |
|:---:|
| <img src="./screenshots/app-pay-checkout.png" alt="支付收银台" width="230"> |
| 支付收银与确认 |

| 我的订单 | 订单详情页 |
|:---:|:---:|
| <img src="./screenshots/order-page.png" alt="我的订单" width="230"> | <img src="./screenshots/app-order-detail.png" alt="订单详情页" width="230"> |
| 订单列表与履约状态 | 订单履约详情与支付入口 |

| 我的钱包 | 钱包充值页 |
|:---:|:---:|
| <img src="./screenshots/wallet-page.png" alt="我的钱包" width="230"> | <img src="./screenshots/app-wallet-recharge.png" alt="钱包充值页" width="230"> |
| 钱包余额与资产入口 | 充值与扫码支付 |

| 支付明细页 | 钱包账单页 |
|:---:|:---:|
| <img src="./screenshots/app-payment-record.png" alt="支付明细页" width="230"> | <img src="./screenshots/app-wallet-bill.png" alt="钱包账单页" width="230"> |
| 支付流水筛选 | 钱包收支流水 |

| 积分中心 | 积分明细 |
|:---:|:---:|
| <img src="./screenshots/points-page.png" alt="积分中心" width="230"> | <img src="./screenshots/app-points-detail.png" alt="积分明细" width="230"> |
| 积分资产与成长入口 | 积分收支明细 |

| 签到与抽奖 |
|:---:|
| <img src="./screenshots/sign-in.png" alt="签到与抽奖" width="260"> |
| 连续签到、抽奖玩法与积分增长入口 |

### <a id="app-content-creation"></a>3.6 内容创作与评价互动

| 内容创作发布 | 内容编辑页 | 博客详情页 |
|:---:|:---:|:---:|
| <img src="./screenshots/publish-page.png" alt="内容创作发布" width="150"> | <img src="./screenshots/app-blog-edit.png" alt="内容编辑页" width="150"> | <img src="./screenshots/app-blog-detail.png" alt="博客详情页" width="150"> |
| 图文发布与草稿承接 | 已有内容编辑与改稿 | 内容详情与互动 |

| 评价列表页 | 评价发布页 | 评价修改页 |
|:---:|:---:|:---:|
| <img src="./screenshots/app-review-list.png" alt="评价列表页" width="150"> | <img src="./screenshots/app-review-publish.png" alt="评价发布页" width="150"> | <img src="./screenshots/app-review-edit.png" alt="评价修改页" width="150"> |
| 商品页内查看网友评价 | 星级、文案与媒体发布入口 | 已发布评价再次编辑 |

| 评价详情页 | 评论区 | 我的评价页 |
|:---:|:---:|:---:|
| <img src="./screenshots/app-review-detail.png" alt="评价详情页" width="150"> | <img src="./screenshots/comment-section.png" alt="评论区" width="150"> | <img src="./screenshots/app-my-reviews.png" alt="我的评价页" width="150"> |
| 单条评价详情与评论承接 | 评论与评价互动 | 评价状态筛选 |

| 待评价页 | 评价草稿页 | 我的发布/笔记 |
|:---:|:---:|:---:|
| <img src="./screenshots/app-wait-review.png" alt="待评价页" width="150"> | <img src="./screenshots/app-review-drafts.png" alt="评价草稿页" width="150"> | <img src="./screenshots/user-posts.png" alt="我的发布/笔记" width="150"> |
| 履约后去评价 | 评价草稿管理 | 已发布内容沉淀 |

| 草稿箱 |
|:---:|
| <img src="./screenshots/draft-box.png" alt="草稿箱" width="260"> |
| 未发布内容管理 |

### <a id="app-social-message"></a>3.7 社交关系与消息

| 动态关注流 | 我的关注页 |
|:---:|:---:|
| <img src="./screenshots/feed-flow.png" alt="动态关注流" width="230"> | <img src="./screenshots/follow-list.png" alt="我的关注页" width="230"> |
| 关注内容流 | 用户 / 店铺 / 商品关注三标签 |

| 粉丝明细页 | 用户搜索 |
|:---:|:---:|
| <img src="./screenshots/app-user-list.png" alt="粉丝明细页" width="230"> | <img src="./screenshots/user-search.png" alt="用户搜索" width="230"> |
| 个人主页下钻后的粉丝关系明细 | 找人入口与用户搜索 |

| 他人主页 | 添加朋友页 |
|:---:|:---:|
| <img src="./screenshots/app-other-profile.png" alt="他人主页" width="230"> | <img src="./screenshots/app-add-friend.png" alt="添加朋友页" width="230"> |
| 用户访问他人主页视角 | 站内找人和加好友入口 |

| 消息会话列表 | 即时通讯聊天 |
|:---:|:---:|
| <img src="./screenshots/app-chat-list.png" alt="消息会话列表" width="230"> | <img src="./screenshots/app-chat-detail.png" alt="即时通讯聊天" width="230"> |
| 私聊会话入口 | 实时聊天详情 |

| 聊天信息页 | 聊天历史日历 |
|:---:|:---:|
| <img src="./screenshots/app-chat-info.png" alt="聊天信息页" width="230"> | <img src="./screenshots/app-chat-history-calendar.png" alt="聊天历史日历" width="230"> |
| 会话设置与免打扰入口 | 按日期查找聊天记录 |

| 系统消息页 | 我的互动 |
|:---:|:---:|
| <img src="./screenshots/app-system-notice.png" alt="系统消息页" width="230"> | <img src="./screenshots/app-my-interactions.png" alt="我的互动" width="230"> |
| 审核、系统与业务提醒 | 被赞、被评和互动消息汇总 |

### <a id="app-ai-capability"></a>3.8 AI 智能助手与 AIGC

| AI 助手浮层 | AI 会话列表 |
|:---:|:---:|
| <img src="./screenshots/ai-chat.png" alt="AI 助手浮层" width="230"> | <img src="./screenshots/app-ai-session-list.png" alt="AI 会话列表" width="230"> |
| 点击小精灵后的助手入口浮层 | 历史会话恢复 |

| AI 快捷提问页 | AI 店铺推荐卡片 |
|:---:|:---:|
| <img src="./screenshots/app-ai-quick-ask.png" alt="AI 快捷提问页" width="230"> | <img src="./screenshots/app-ai-store-recommend.png" alt="AI 店铺推荐卡片" width="230"> |
| 首次进入时的推荐问题与快捷引导 | 附近店铺推荐结果卡片 |

| AI 商品推荐卡片 | AI 下单结果与订单卡片 |
|:---:|:---:|
| <img src="./screenshots/app-ai-product-recommend.png" alt="AI 商品推荐卡片" width="230"> | <img src="./screenshots/app-ai-order-card.png" alt="AI 下单结果与订单卡片" width="230"> |
| 团购与商品推荐结果卡片 | 下单成功后的订单状态卡片 |

| AI 博客生成 | AI 评价生成 |
|:---:|:---:|
| <img src="./screenshots/app-ai-blog-generate.png" alt="AI 博客生成" width="230"> | <img src="./screenshots/app-ai-review-generate.png" alt="AI 评价生成" width="230"> |
| AIGC 探店内容 | AIGC 消费评价生成 |

### <a id="app-profile-assets"></a>3.9 个人中心、收藏与安全设置

| 个人中心（含顶部搜索按钮） | 资料编辑页 |
|:---:|:---:|
| <img src="./screenshots/profile-page.png" alt="个人中心" width="230"> | <img src="./screenshots/app-profile-edit.png" alt="资料编辑页" width="230"> |
| 个人中心与顶部搜索入口 | 昵称、头像和简介编辑 |

| 我的收藏页 | 账号安全页 |
|:---:|:---:|
| <img src="./screenshots/user-favorites.png" alt="我的收藏页" width="230"> | <img src="./screenshots/app-password-setting.png" alt="账号安全页" width="230"> |
| 店铺 / 笔记 / 商品收藏三标签 | 密码修改与账号安全设置 |

| 设置密码页 | 修改密码页 |
|:---:|:---:|
| <img src="./screenshots/app-set-password.png" alt="设置密码页" width="230"> | <img src="./screenshots/app-update-password.png" alt="修改密码页" width="230"> |
| 首次设置登录密码 | 旧密码校验后的账号安全修改 |

## <a id="merchant-pages"></a>4. 商家端 Web 核心入口

### <a id="merchant-dashboard"></a>4.1 登录与经营总览

| 管理端登录页 | 经营总览仪表盘 |
|:---:|:---:|
| <img src="./screenshots/admin-login.png" alt="管理端登录页" width="420"> | <img src="./screenshots/admin-dashboard.png" alt="经营总览仪表盘" width="420"> |
| 商家端登录入口 | 商家经营总览与数据面板 |

### <a id="merchant-business"></a>4.2 店铺与商品经营

| 店铺列表页 | 商品列表页 |
|:---:|:---:|
| <img src="./screenshots/admin-shop-manage.png" alt="店铺列表页" width="420"> | <img src="./screenshots/admin-product-manage.png" alt="商品列表页" width="420"> |
| 商家自有店铺列表与状态管理 | 商家商品列表、状态维护与上下架管理 |

| 新增店铺页 | 新建商品页 |
|:---:|:---:|
| <img src="./screenshots/merchant-shop-create.png" alt="新增店铺页" width="420"> | <img src="./screenshots/merchant-product-create.png" alt="新建商品页" width="420"> |
| 商家创建店铺、基础信息填写与地图定位 | 商家新建商品、价格规则与营销字段配置 |

| 编辑店铺页 | 编辑商品页 |
|:---:|:---:|
| <img src="./screenshots/merchant-shop-edit.png" alt="编辑店铺页" width="420"> | <img src="./screenshots/merchant-product-edit.png" alt="编辑商品页" width="420"> |
| 商家维护店铺资料、图片素材与地图定位 | 商家调整商品信息、价格与有效期规则 |

| 店铺详情页 | 商品详情页 |
|:---:|:---:|
| <img src="./screenshots/merchant-shop-detail.png" alt="店铺详情页" width="420"> | <img src="./screenshots/merchant-product-detail.png" alt="商品详情页" width="420"> |
| 商家查看店铺经营信息、审核状态与相册 | 商家查看商品价格、库存与时间规则 |

### <a id="merchant-growth"></a>4.3 订单履约与详情

| 订单管理 | 订单详情页 |
|:---:|:---:|
| <img src="./screenshots/admin-order-manage.png" alt="订单管理" width="420"> | <img src="./screenshots/merchant-order-detail.png" alt="订单详情页" width="420"> |
| 商家订单履约状态与筛选 | 商家查看订单进度、支付信息与用户信息 |

### <a id="merchant-ai"></a>4.4 AI 经营助手

| AI 经营助手 |
|:---:|
| <img src="./screenshots/admin-ai-assistant.png" alt="AI 经营助手" width="420"> |
| 商家侧 AI 建议、商品分析与经营辅助问答 |

| 评价回复页 | 经营状况分析页 |
|:---:|:---:|
| <img src="./screenshots/merchant-ai-review-reply.png" alt="评价回复页" width="420"> | <img src="./screenshots/merchant-ai-analysis.png" alt="经营状况分析页" width="420"> |
| AI 辅助生成商家评价回复建议 | AI 分析店铺经营数据并给出阶段性建议 |

| 商品营销文案页 | 经营改进建议页 |
|:---:|:---:|
| <img src="./screenshots/merchant-ai-copywriting.png" alt="商品营销文案页" width="420"> | <img src="./screenshots/merchant-ai-improve.png" alt="经营改进建议页" width="420"> |
| AI 辅助生成商品推广文案与营销表达 | AI 综合经营数据输出改进方向与优化建议 |

## <a id="admin-pages"></a>5. 平台管理端 Web 核心入口

### <a id="admin-governance"></a>5.1 内容治理与运营后台

| 审核中心 | 审核详情页 |
|:---:|:---:|
| <img src="./screenshots/admin-audit-center.png" alt="审核中心" width="420"> | <img src="./screenshots/admin-audit-detail.png" alt="审核详情页" width="420"> |
| 审核流与驳回回写 | 单条审核内容、附件与处理动作查看 |

| 博客管理列表 | 博客详情页 |
|:---:|:---:|
| <img src="./screenshots/admin-blog-manage.png" alt="博客管理列表" width="420"> | <img src="./screenshots/admin-blog-detail.png" alt="博客详情页" width="420"> |
| 博客内容治理与运营维护 | 单篇博客详情、状态与内容查看 |

| 评价管理列表 | 评价详情页 |
|:---:|:---:|
| <img src="./screenshots/admin-review-manage.png" alt="评价管理列表" width="420"> | <img src="./screenshots/admin-review-detail.png" alt="评价详情页" width="420"> |
| 评价治理、状态筛选与批量处理 | 单条评价详情、评分维度与附件查看 |

| 评论管理列表 | 评论详情页 |
|:---:|:---:|
| <img src="./screenshots/admin-comment-manage.png" alt="评论管理列表" width="420"> | <img src="./screenshots/admin-comment-detail.png" alt="评论详情页" width="420"> |
| 评论治理与清理 | 单条评论详情、来源内容与状态查看 |

| 业务用户 | 业务用户详情页 |
|:---:|:---:|
| <img src="./screenshots/admin-business-user.png" alt="业务用户" width="420"> | <img src="./screenshots/admin-business-user-detail.png" alt="业务用户详情页" width="420"> |
| 商家侧用户运营列表与平台查看入口 | 业务用户基础资料与头像信息查看 |

| 抽奖配置 | 积分记录 |
|:---:|:---:|
| <img src="./screenshots/admin-lottery-config.png" alt="抽奖配置" width="420"> | <img src="./screenshots/admin-points-records.png" alt="积分记录" width="420"> |
| 平台侧奖品配置与增长玩法管理 | 平台侧积分记录与发放流水审计 |

### <a id="admin-system"></a>5.2 系统管理与权限审计

| 用户管理 | 菜单权限 |
|:---:|:---:|
| <img src="./screenshots/admin-user-manage.png" alt="用户管理" width="420"> | <img src="./screenshots/admin-menu.png" alt="菜单权限" width="420"> |
| 系统账号与权限管理 | 菜单树与按钮权限配置 |

| 参数设置 | 在线监控 |
|:---:|:---:|
| <img src="./screenshots/admin-config.png" alt="参数设置" width="420"> | <img src="./screenshots/admin-online-monitor.png" alt="在线监控" width="420"> |
| 系统参数与开关配置 | 在线用户与强退 |

| 角色管理 | 登录日志 |
|:---:|:---:|
| <img src="./screenshots/admin-role-manage.png" alt="角色管理" width="420"> | <img src="./screenshots/admin-logininfor.png" alt="登录日志" width="420"> |
| 角色与权限字符维护 | 登录审计与状态追踪 |

| 操作日志 |
|:---:|
| <img src="./screenshots/admin-operlog.png" alt="操作日志" width="420"> |
| 后台操作留痕与行为审计 |

### <a id="admin-scheduler"></a>5.3 XXL-JOB 调度后台

| 调度总览 | 任务管理 |
|:---:|:---:|
| <img src="./screenshots/admin-xxl-dashboard.png" alt="XXL-JOB 调度总览" width="420"> | <img src="./screenshots/admin-xxl-job-manage.png" alt="XXL-JOB 任务管理" width="420"> |
| 28 个任务、3 个在线执行器的运行总览 | 按执行器查看 JobHandler、CRON 和状态 |

| 执行器管理 |
|:---:|
| <img src="./screenshots/admin-xxl-executors.png" alt="XXL-JOB 执行器管理" width="420"> |
| 6 个执行器配置与在线节点查看 |

</div>
