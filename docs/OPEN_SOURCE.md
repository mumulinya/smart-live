---
outline: 2
---

# SmartLive 开源接入说明

👉 **完整的图文排版版本，请访问：[SmartLive 在线文档网站](https://mumulinya.github.io/smartLive-Cloud/)**

**文档导航：** [网站首页](/) · [视觉导览](SHOWCASE.md) · [页面导览](PAGE_GALLERY.md)

这份文档聚焦三件事：

1. 第一次接入这个仓库，应该先看什么。
2. 想把服务跑起来，最小链路需要哪些中间件和模块。
3. 想体验完整能力或做部署演示，下一步应该补哪些依赖。

<div align="center">
  <img src="./diagrams/open-source-reading-path.svg" alt="SmartLive 开源文档阅读路径" width="100%">
</div>

<div align="center">
  <img src="./diagrams/startup-decision-tree.svg" alt="SmartLive 首次接入启动决策图" width="100%">
</div>

## 1. 先看哪几份文档

| 文档 | 作用 | 适合谁 |
|:---|:---|:---|
| [网站首页](/) | 了解项目定位、架构全景、业务截图、核心链路与技术亮点 | 第一次认识项目的人 |
| [SHOWCASE.md](SHOWCASE.md) | 按用户链路查看截图、架构图和关键时序图 | 想快速看“项目长什么样”的人 |
| [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) | 看 Docker、JAR、镜像重建、Nacos 配置和排错方式 | 准备部署或做演示的人 |
| [GitHub · CONTRIBUTING.md](https://github.com/mumulinya/smartLive-Cloud/blob/main/CONTRIBUTING.md) | 了解贡献方式、分支约定、提交格式和自查清单 | 准备提 PR 的人 |
| [GitHub · SECURITY.md](https://github.com/mumulinya/smartLive-Cloud/blob/main/SECURITY.md) | 明确敏感配置、密钥注入、账号信息的边界 | 所有会修改配置的人 |

## 2. 按目标阅读更高效

- **第一次认识项目**：先看 [网站首页](/)，再看 `smartLive-auth -> smartLive-gateway -> smartLive-system -> smartLive-user -> smartLive-shop -> smartLive-search`
- **想看交易闭环**：优先读 `smartLive-product -> smartLive-order -> smartLive-wallet -> smartLive-points`，再结合 README 里的交易链路图
- **想看社交与推荐**：优先读 `smartLive-blog -> smartLive-interaction -> smartLive-index -> smartLive-search`，再结合 `SHOWCASE.md` 的 Feed / 热榜图
- **想看 AI 与治理链路**：优先读 `smartLive-ai -> smartLive-audit -> smartLive-chat -> smartLive-im`，再补向量检索、审核责任链和通知推送链路

## 3. 第一次接入建议

- 优先走本地开发模式，不要一开始就把 `docker/` 目录当作唯一事实来源。
- 先跑“最小可运行链路”，确认登录、店铺、搜索和后台管理可用后，再补 AI、审核、积分、支付等扩展能力。
- `docker/` 下仍保留部分历史模块命名与复制脚本，使用前要和当前 Maven 模块、当前端口、当前 JAR 名称逐项核对。

## 4. 项目入口与仓库矩阵

如果你是第一次接这个项目，建议把“仓库入口”也放在启动视角里一起看。先明确后端主仓库、管理端仓库和用户端仓库分别在哪，再决定你这次是只跑后端、只看后台，还是要把 App 也一起联起来。

| 仓库 | 角色定位 | 适合什么时候看 | 入口 |
|:---|:---|:---|:---|
| **smartLive-Cloud** | 后端微服务主仓库，承接交易、搜索、审核、AI、调度与基础设施协同 | 准备启动服务、看链路、看源码时先看它 | [GitHub](https://github.com/mumulinya/smartLive-Cloud.git) |
| **smartLive-ui** | 管理端 Web，覆盖商品、订单、审核、运营、系统后台能力 | 想看后台页面、联调管理端时再看它 | [Gitee](https://gitee.com/mumulinya/smart-live-ui.git) |
| **smart-live-app** | 用户端 App，覆盖登录、发现、下单、评价、社交消息与 AI 页面 | 想看用户端体验、截图与页面联调时再看它 | [Gitee](https://gitee.com/mumulinya/smart-live-html.git) |

## 5. 体验边界说明

| 类型 | 当前状态 | 说明 |
|:---|:---|:---|
| 可直接体验 | 登录鉴权、首页热榜、店铺详情、搜索基础链路、后台管理 | 补齐 MySQL、Redis、Nacos、RabbitMQ 后，先跑最小链路即可完成主要演示 |
| 需要额外配置 | Elasticsearch、Milvus、MinIO、XXL-JOB、模型 API Key、支付相关配置 | 搜索、AI/RAG、对象存储、调度后台、支付回调等能力依赖额外中间件或密钥 |
| 更适合先读源码 | 死信补偿扩展、更多外部平台接入、完整线上观测与告警闭环 | 这些能力在仓库里已经有接口和骨架，更适合作为源码阅读和后续扩展点理解 |

## 6. 两条启动路径

### 路径一：最小可运行链路

适合先把服务跑起来，验证核心主链路是否通畅。

- 必需中间件：MySQL、Redis、Nacos、RabbitMQ
- 建议优先启动：`smartLive-auth`、`smartLive-gateway`、`smartLive-system`、`smartLive-user`、`smartLive-shop`、`smartLive-search`
- 这条路径可以先验证：登录、用户基础信息、店铺列表、店铺详情、后台管理、基础搜索

### 路径二：完整功能链路

适合验证 AI、搜索、文件、定时任务、积分和支付等完整能力。

- 额外中间件：Elasticsearch、Milvus、MinIO、XXL-JOB、Sentinel
- 额外模块：`smartLive-product`、`smartLive-order`、`smartLive-interaction`、`smartLive-chat`、`smartLive-im`、`smartLive-ai`、`smartLive-wallet`、`smartLive-points`、`smartLive-blog`、`smartLive-audit`、`smartLive-file`
- 如果要体验 RAG、向量检索、异步审核、积分抽奖、消息通知或延迟队列链路，必须补齐这些依赖

## 7. 推荐启动顺序

### 最小链路

1. `smartLive-auth`
2. `smartLive-gateway`
3. `smartLive-system`
4. `smartLive-user`
5. `smartLive-shop`
6. `smartLive-search`

### 进阶链路

在最小链路稳定后，再按需补：

1. `smartLive-product`
2. `smartLive-order`
3. `smartLive-interaction`
4. `smartLive-chat`
5. `smartLive-im`
6. `smartLive-blog`
7. `smartLive-audit`
8. `smartLive-ai`
9. `smartLive-wallet`
10. `smartLive-points`
11. `smartLive-file`
12. `smartLive-index`

## 8. 依赖矩阵

| 能力 | 必需依赖 | 备注 |
|:---|:---|:---|
| 登录鉴权 | MySQL、Redis、Nacos | auth + gateway |
| 店铺/商品/订单基础链路 | MySQL、Redis、RabbitMQ、Nacos | 常规业务必需 |
| 搜索 | Elasticsearch、Redis、RabbitMQ、Nacos | search 模块 |
| AI 对话 / RAG | Milvus、Elasticsearch、RabbitMQ、Nacos | 还需要单独配置模型 API key |
| 文件服务 | MinIO、Nacos | 本地文件路径也要配置 |
| 定时任务 | XXL-JOB、Nacos | 依赖 `xxl-job-common.yml` |
| 流量治理 | Sentinel、Nacos | 可按需启用 |

### 8.1 按能力体验的最小依赖矩阵

| 想体验的能力 | 推荐启动模块 | 最少中间件 | 说明 |
|:---|:---|:---|:---|
| 登录与个人中心 | `smartLive-auth`、`smartLive-gateway`、`smartLive-system`、`smartLive-user` | MySQL、Redis、Nacos | 先验证登录、用户信息、后台基础菜单是否可用 |
| 商家后台与店铺基础能力 | 在上一条基础上加 `smartLive-shop` | MySQL、Redis、Nacos | 可体验店铺列表、详情和后台店铺管理 |
| 搜索与附近找店 | 在上一条基础上加 `smartLive-search` | MySQL、Redis、Nacos、Elasticsearch | 热词、搜索和 LBS 找店依赖 ES |
| 下单、支付与积分 | `smartLive-product`、`smartLive-order`、`smartLive-wallet`、`smartLive-points` | MySQL、Redis、Nacos、RabbitMQ | 订单、支付、积分变动和补偿链路都依赖 MQ |
| AI 对话与 RAG | `smartLive-ai`、`smartLive-search`、`smartLive-shop`、`smartLive-product`、`smartLive-blog`、`smartLive-interaction` | MySQL、Redis、Nacos、RabbitMQ、Elasticsearch、Milvus、MinIO | 还需要模型 API Key、向量库和对象存储配置完整可用 |

## 8. 配置来源

- 本地端口、基础应用名、部分默认连接参数位于各模块的 `bootstrap.yml`。
- 共享配置和业务配置主要通过 Nacos 提供，初始化数据见 `sql/ry_config_20250902.sql`。
- `order`、`product`、`interaction` 等模块还依赖 `xxl-job-common.yml`；如果缺少这个 dataId，定时任务相关能力无法正常运行。
- Docker 相关构建与复制逻辑位于 `docker/` 目录，但其中存在历史脚本，请在使用前自行核对。

## 9. 实际服务端口

| 服务 | 端口 |
|:---|:---:|
| smartLive-gateway | 8080 |
| smartLive-auth | 9300 |
| smartLive-system | 9202 |
| smartLive-user | 9201 |
| smartLive-shop | 9203 |
| smartLive-search | 9204 |
| smartLive-order | 9205 |
| smartLive-product | 9206 |
| smartLive-interaction | 9207 |
| smartLive-index | 9208 |
| smartLive-file | 9209 |
| smartLive-chat | 9210 |
| smartLive-blog | 9211 |
| smartLive-audit | 9212 |
| smartLive-ai | 9213 |
| smartLive-im | 9214 |
| smartLive-points | 9215 |
| smartLive-wallet | 9216 |
| smartLive-monitor | 9100 |
| smartLive-sentinel | 8718 |

> 说明：监控中心的目录名是 `smartLive-monitor`，对应 Maven `artifactId` 是 `smartLive-visual-monitor`；`smartLive-sentinel` 主要作为流量治理控制台出现在部署环境中。

## 10. 第一天建议验证什么

- 网关可访问，登录鉴权可用。
- 用户、店铺、搜索三类基础接口可正常返回。
- Nacos 中 `application-dev.yml` 与 `smartLive-*-dev.yml` 已导入。
- RabbitMQ、Redis、MySQL 连接日志正常，没有残留 `127.0.0.1` 的错误配置。
- 如果补了搜索或 AI，确认 Elasticsearch、Milvus 相关连接正常。

## 11. AI 密钥与敏感配置

不要把真实 API key、数据库密码或外网可用的测试账号直接提交到仓库。

本地开发也可以直接使用 `config/smartlive-ai-secrets.yml`；仓库内提供了 `config/smartlive-ai-secrets.example.yml` 作为示例。

建议至少使用以下环境变量：

- `SMARTLIVE_ZHIPU_API_KEY`
- `SMARTLIVE_OPENAI_API_KEY`
- `SMARTLIVE_OPENAI_CHAT_API_KEY`
- `SMARTLIVE_EMBEDDING_API_KEY`
- `MILVUS_HOST`
- `RABBITMQ_HOST`
- `NACOS_HOST`

## 12. 已知注意事项

- `bin/run-*.bat` 仍保留旧的 `ruoyi-*` 路径，当前不应作为对外主推荐启动方式。
- `docker/copy.sh`、`docker/deploy.sh` 和 `docker-compose.yml` 中仍有 `marketing`、`map` 等历史命名，使用前请先校对。
- 如果你修改了端口、模块名、依赖或启动顺序，请同时更新 `README.md`、`docs/OPEN_SOURCE.md` 和 `docs/SHOWCASE.md`。

## 13. 工程阅读与使用提示

如果你准备进一步阅读源码，而不是只把服务跑起来，建议先抓住下面这 5 个重点：

- **先理解缓存分层，再看具体业务读链路**：项目里大量列表场景走 `ZSet`，详情走 `String`，计数走独立键；先建立这个心智模型，再看评论、博客、商品、店铺这些模块会轻松很多。
- **先理解“主库是真实源”，再看 ES / Milvus / Redis 副本**：搜索、热榜、向量检索都不是主事务里强一致写入，而是通过 MQ、幂等消费和调度补偿逐步收敛。
- **先看模式，再看实现**：互动模块的策略/工厂、审核中心的责任链、缓存层的模板化封装，都是仓库里最值得优先阅读的设计点。
- **AI、支付、对象存储都要带着“环境边界”去看**：这几类能力依赖外部服务和敏感配置，本地没配齐时更适合先理解链路和接口，再补全密钥与中间件。
- **先从最小链路读起，不要一上来全模块联调**：推荐先看 `auth -> gateway -> user -> shop -> search`，稳定后再补 `order / interaction / ai / audit / wallet / points`。

### 13.1 重点设计模式落点

- **策略 + 工厂**：点赞、收藏、评论、评价、关注、支付等多条业务线通过统一策略工厂收口。
- **模板方法**：互动同步、缓存读写、防穿透/防击穿策略都采用了可复用的模板化流程。
- **责任链**：审核中心把敏感词、AI 审核、人工审核分层处理，并按终态中止。
- **异步补偿**：订单、库存、热榜、搜索、向量库、副本收敛都不是靠单事务完成，而是靠 MQ + 幂等 + 调度协同闭环。

### 13.2 配置与安全边界

- AI、向量检索、支付、对象存储相关密钥不要直接写入仓库，统一通过环境变量或私有配置注入。
- 本地 AI 配置可参考 `config/smartlive-ai-secrets.example.yml`，复制为私有配置后再填入真实密钥。
- 提交 PR、提交信息格式、编码约束和自查清单见 [CONTRIBUTING.md](https://github.com/mumulinya/smartLive-Cloud/blob/main/CONTRIBUTING.md)。
- 敏感配置和账号使用边界见 [SECURITY.md](https://github.com/mumulinya/smartLive-Cloud/blob/main/SECURITY.md)。
