# 系统架构与项目规模

> 本页作为网站详细版页面维护；项目规模卡片继续和共享数据源同步。

## 1. 项目规模与关键数据

<div class="smartlive-stats-grid">
<!-- AUTO_SYNC:SYSTEM_ARCH_STATS_CARDS:START -->
  <div class="smartlive-stat-card">
    <div class="smartlive-stat-value">16 个</div>
    <div class="smartlive-stat-label">业务模块</div>
    <div class="smartlive-stat-desc">覆盖交易、社交、搜索、AI、审核、钱包、IM 等核心能力</div>
  </div>
  <div class="smartlive-stat-card">
    <div class="smartlive-stat-value">19 个</div>
    <div class="smartlive-stat-label">服务应用</div>
    <div class="smartlive-stat-desc">16 个业务模块 + auth / gateway / monitor</div>
  </div>
  <div class="smartlive-stat-card">
    <div class="smartlive-stat-value">7万+</div>
    <div class="smartlive-stat-label">Java 代码</div>
    <div class="smartlive-stat-desc">核心业务口径 3万+，当前仓库 Java 总量约 72,000+ 行</div>
  </div>
  <div class="smartlive-stat-card">
    <div class="smartlive-stat-value">200+</div>
    <div class="smartlive-stat-label">Git 提交</div>
    <div class="smartlive-stat-desc">完整保留从 0 到 1 的迭代与重构轨迹</div>
  </div>
  <div class="smartlive-stat-card">
    <div class="smartlive-stat-value">8+ 个月</div>
    <div class="smartlive-stat-label">独立开发</div>
    <div class="smartlive-stat-desc">覆盖需求、建模、实现、压测、文档与上线展示</div>
  </div>
  <div class="smartlive-stat-card">
    <div class="smartlive-stat-value">3200+</div>
    <div class="smartlive-stat-label">秒杀 QPS</div>
    <div class="smartlive-stat-desc">Lua 预扣库存 + MQ 异步落单 + 延迟补偿兜底</div>
  </div>
  <div class="smartlive-stat-card">
    <div class="smartlive-stat-value">20-40 倍</div>
    <div class="smartlive-stat-label">Redis 提升</div>
    <div class="smartlive-stat-desc">列表、详情、计数等高频读链路经过分层缓存优化</div>
  </div>
  <div class="smartlive-stat-card">
    <div class="smartlive-stat-value">28 个</div>
    <div class="smartlive-stat-label">XXL-JOB 任务</div>
    <div class="smartlive-stat-desc">统一承接订单兜底、秒杀预热、热榜重建与互动回刷</div>
  </div>
<!-- AUTO_SYNC:SYSTEM_ARCH_STATS_CARDS:END -->
</div>

## 2. 统计口径说明

| 指标 | 统计口径 |
|------|----------|
| 业务模块 | 统计 `smartLive-modules` 下的实际业务服务，不包含 `api / common / visual / sentinel / seata-server` 等支撑工程 |
| 服务应用 | 业务模块 + `smartLive-auth` + `smartLive-gateway` + `smartLive-monitor`，按可独立运行的服务口径计算 |
| 代码量 | README 保留“核心业务代码 3万+”口径；网站首页卡片展示仓库当前 Java 总量 `7万+`，两者分别用于强调业务密度与工程总体量 |
| 页面截图 | 统计 `docs/screenshots` 中当前被在线文档实际引用的截图，不包含 `legacy` 归档图 |
| 链路图 | 分开展示版 `SVG` 与详细版 `SVG` 两套数量，均以 `docs/diagrams` 当前可访问文件为准 |
| XXL-JOB 任务 | 以调度后台当前可见任务数为准，和文档中的后台截图口径保持一致 |

## 3. 系统架构

这张图不是为了讲单条时序链路，而是为了先把 **入口、治理、业务服务簇、基础设施** 四层关系放在一张图里，帮助你快速建立全局心智模型。

### 3.1 怎么看这张图

<div class="smartlive-arch-reading-grid">
  <div class="smartlive-arch-reading-card">
    <strong>1. 先看入口层</strong>
  <span>用户端 App、商家端 / 平台管理端 Web 先经过 Nginx，再统一进入 Gateway，理解系统的统一入口和流量边界。</span>
  </div>
  <div class="smartlive-arch-reading-card">
    <strong>2. 再看治理层</strong>
    <span>Auth、Nacos、Sentinel、Seata 这层不直接承接业务页面，但决定了鉴权、注册配置、限流熔断和分布式事务怎么统一治理。</span>
  </div>
  <div class="smartlive-arch-reading-card">
    <strong>3. 再看业务服务簇</strong>
    <span>业务层不是简单平铺服务，而是拆成基础服务、交易履约、内容社交、搜索智能治理四个服务簇，更方便理解职责边界。</span>
  </div>
  <div class="smartlive-arch-reading-card">
    <strong>4. 最后看基础设施层</strong>
    <span>MySQL、Redis、RabbitMQ、Elasticsearch、Milvus、MinIO、XXL-JOB 分别承接存储、缓存、异步、副本检索、向量检索、对象存储和调度兜底。</span>
  </div>
</div>

<div align="center">
  <img src="../diagrams/system-architecture-overview.svg" alt="SmartLive 系统架构图" width="100%">
</div>

### 3.2 这张图回答什么问题

<div class="smartlive-arch-focus-grid">
  <div class="smartlive-arch-focus-card">
    <strong>统一入口在哪里</strong>
    <span>先回答“用户请求从哪进来”，避免一上来就迷失在 19 个服务应用里。</span>
  </div>
  <div class="smartlive-arch-focus-card">
    <strong>治理能力放在哪里</strong>
    <span>把认证、配置、限流、事务和监控从业务服务里抽出来看，才能理解为什么这个项目不是“单体拆模块”。</span>
  </div>
  <div class="smartlive-arch-focus-card">
    <strong>业务簇如何协作</strong>
    <span>交易履约、内容社交、搜索智能治理并不是孤立存在，而是围绕缓存、副本、MQ、调度共同协作。</span>
  </div>
</div>

**建议带着这 4 个问题往下读：**

- 哪些服务是统一入口、统一治理，哪些才是真正承接业务闭环的核心服务。
- 交易、社交、搜索、AI 为什么拆成独立服务簇，而不是堆进一个“大业务服务”。
- Redis、RabbitMQ、Elasticsearch、Milvus、MinIO、XXL-JOB 分别在系统里承担什么角色。
- 当你顺着一条链路往下看时，应该从哪个服务开始找 controller、service、listener 和 job。

### 3.3 服务依赖关系总览

这张图不是时序图，而是把入口层、业务服务簇和基础设施依赖放在同一张图里，方便先理解“谁依赖谁”，再去看具体链路。

<div align="center">
  <img src="../diagrams/service-dependency-overview.svg" alt="SmartLive 服务依赖关系总览" width="100%">
</div>

**建议阅读方式：**

- 先看 `用户端 App / 商家端 / 平台管理端 Web -> Gateway -> Auth` 的统一入口。
- 再看四个业务服务簇：基础服务、交易履约、内容社交、搜索智能治理。
- 最后看底部基础设施：`MySQL / Redis / RabbitMQ / Elasticsearch / Milvus / MinIO / Nacos / XXL-JOB`，理解它们分别承接什么职责。

## 4. 服务职责矩阵

这张表和上面的依赖图配合看会更清楚：依赖图负责先看“谁和谁相连”，职责矩阵负责再看“每个服务到底干什么”。

| 服务 / 模块 | 核心职责 | 关键依赖 | 典型协作方 |
|------|----------|----------|------------|
| `smartLive-gateway` | 统一入口、鉴权透传、路由转发、边界过滤 | Nacos、Sentinel | auth、system、user、shop |
| `smartLive-auth` | 登录鉴权、令牌签发、会话校验 | MySQL、Redis | gateway、user、system |
| `smartLive-system` | 后台菜单权限、参数配置、公告与基础管理 | MySQL、Redis | auth、gateway、monitor |
| `smartLive-user` | 用户资料、主页能力、审核消息与 ES 同步投递 | MySQL、Redis、RabbitMQ | auth、shop、search、audit |
| `smartLive-shop` | 店铺资料、店铺详情、经营分析与热榜读取 | MySQL、Redis、RabbitMQ | user、search、ai、order |
| `smartLive-product` | 商品、团购、好券、秒杀与库存管理 | MySQL、Redis、RabbitMQ、XXL-JOB | order、shop、points |
| `smartLive-order` | 下单、支付状态流转、超时取消、退款补偿 | MySQL、Redis、RabbitMQ、XXL-JOB | product、wallet、points |
| `smartLive-wallet` | 充值、支付记录、退款入账与钱包流水 | MySQL、RabbitMQ | order、points |
| `smartLive-points` | 积分、签到、抽奖与积分流水 | MySQL、Redis、RabbitMQ | order、product、wallet |
| `smartLive-blog` | 博客发布、详情读取、内容投递与 Feed 事件 | MySQL、Redis、RabbitMQ | interaction、search、ai |
| `smartLive-interaction` | 点赞、收藏、评论、关注、Feed、热榜与互动回刷 | MySQL、Redis、RabbitMQ、XXL-JOB | blog、shop、product、chat |
| `smartLive-chat` | 会话聚合、系统通知、离线消息落库 | MySQL、Redis、RabbitMQ | im、interaction、order |
| `smartLive-im` | Netty 长连接、在线推送、即时消息投递 | Redis、RabbitMQ、Netty | chat、interaction |
| `smartLive-search` | ES 检索、LBS 查询、热词与搜索副本 | Elasticsearch、Redis、RabbitMQ | shop、product、blog、ai |
| `smartLive-audit` | 审核责任链、人工审核、状态回写 | MySQL、Redis、RabbitMQ | user、shop、product、blog |
| `smartLive-ai` | Spring AI 编排、RAG 检索、向量同步与 AIGC | Milvus、Elasticsearch、RabbitMQ | search、shop、product、blog |
| `smartLive-file` | 文件上传、头像替换、对象存储接入 | MinIO、Redis | user、shop、blog |
| `smartLive-index` | 首页聚合、统计读取与热门面板承接 | MySQL、Redis | shop、product、blog、interaction |
| `smartLive-monitor` | 服务监控与运维入口 | Nacos、监控采集组件 | system、gateway、全部服务 |

> 阅读建议：如果你第一次读这个仓库，可以先挑 `gateway / auth / user / shop / search` 这五个服务看，再回来看交易、社交和 AI 模块会顺很多。

## 5. 项目结构

下面只展示与阅读项目最相关的目录，省略 `.idea`、`logs`、`arthas-output` 等环境或运行时目录。

```text
smart-live-Cloud
├── .github                        // Issue / PR 模板与 Actions 配置
├── docs                           // 开源文档、链路图、截图导览
│   ├── diagrams                                   // SVG / PNG 业务链路图
│   └── screenshots                                // 页面截图与架构图
├── smartLive-gateway              // API 网关 [8080]
├── smartLive-auth                 // 认证中心 [9200]
├── smartLive-api                  // Feign 接口、DTO、VO 定义
│   ├── smartLive-api-ai                           // AI 接口
│   ├── smartLive-api-blog                         // 博客接口
│   ├── smartLive-api-chat                         // 聊天接口
│   ├── smartLive-api-interaction                  // 互动接口
│   ├── smartLive-api-order                        // 订单接口
│   ├── smartLive-api-points                       // 积分接口
│   ├── smartLive-api-product                      // 商品接口
│   ├── smartLive-api-shop                         // 店铺接口
│   ├── smartLive-api-system                       // 系统接口
│   └── smartLive-api-user                         // 用户接口
├── smartLive-common               // 通用基础设施
│   ├── smartLive-common-core                      // 核心工具与公共配置
│   ├── smartLive-common-datascope                 // 数据权限
│   ├── smartLive-common-datasource                // 多数据源
│   ├── smartLive-common-log                       // 日志记录
│   ├── smartLive-common-rabbitmq                  // MQ 封装
│   ├── smartLive-common-redis                     // Redis 封装
│   ├── smartLive-common-seata                     // 分布式事务
│   ├── smartLive-common-security                  // 安全认证
│   ├── smartLive-common-sensitive                 // 敏感词 / 脱敏能力
│   ├── smartLive-common-swagger                   // API 文档
│   └── smartLive-common-xxl                       // XXL-JOB 定时任务基座
├── smartLive-modules              // 核心业务模块
│   ├── smartLive-ai                               // AI 智能 [9213]
│   ├── smartLive-audit                            // 审核中心 [9212]
│   ├── smartLive-blog                             // 博客笔记 [9211]
│   ├── smartLive-chat                             // 聊天会话 [9210]
│   ├── smartLive-file                             // 文件服务 [9209]
│   ├── smartLive-im                               // IM 推送 [9214]
│   ├── smartLive-index                            // 首页聚合 [9208]
│   ├── smartLive-interaction                      // 互动中心 [9207]
│   ├── smartLive-order                            // 订单管理 [9205]
│   ├── smartLive-points                           // 积分管理 [9215]
│   ├── smartLive-product                          // 商品管理 [9206]
│   ├── smartLive-search                           // 搜索引擎 [9204]
│   ├── smartLive-shop                             // 店铺管理 [9203]
│   ├── smartLive-system                           // 系统管理 [9202]
│   ├── smartLive-user                             // 用户中心 [9201]
│   └── smartLive-wallet                           // 钱包支付 [9216]
├── smartLive-visual               // 图形化管理
│   └── smartLive-monitor                          // 监控中心目录（artifact: smartLive-visual-monitor）[9100]
├── smartLive-sentinel             // Sentinel 控制台 [8718]
├── smartLive-seata-server         // Seata Server [7091]
├── arthas                         // Arthas 诊断工具与脚本
├── bin                            // 本地启动 / 部署脚本
├── docker                         // Docker 编排与镜像脚本
├── seata                          // Seata 本地配置与运行目录
├── skywalking                     // SkyWalking 本地链路追踪环境
├── sql                            // 数据库初始化脚本
├── CONTRIBUTING.md                // 贡献约定
├── SECURITY.md                    // 安全说明
├── README.md                      // 首页文档
└── pom.xml                        // Maven 父工程
```

**推荐阅读顺序：**

- **第一次认识项目**：`smartLive-auth -> smartLive-gateway -> smartLive-system -> smartLive-user -> smartLive-shop -> smartLive-search`
- **想看交易闭环**：`smartLive-product -> smartLive-order -> smartLive-wallet -> smartLive-points`
- **想看社交与推荐**：`smartLive-blog -> smartLive-interaction -> smartLive-index -> smartLive-search`
- **想看 AI 与治理链路**：`smartLive-ai -> smartLive-audit -> smartLive-chat -> smartLive-im`
