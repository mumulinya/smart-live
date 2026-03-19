# SmartLive 开源接入说明

这份文档回答三个问题：

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
| [../README.md](../README.md) | 了解项目定位、架构全景、业务截图、核心链路与技术亮点 | 第一次认识项目的人 |
| [SHOWCASE.md](SHOWCASE.md) | 按用户链路查看截图、架构图和关键时序图 | 想快速看“项目长什么样”的人 |
| [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) | 看 Docker、JAR、镜像重建、Nacos 配置和排错方式 | 准备部署或做演示的人 |
| [../CONTRIBUTING.md](../CONTRIBUTING.md) | 了解贡献方式、分支约定、提交格式和自查清单 | 准备提 PR 的人 |
| [../SECURITY.md](../SECURITY.md) | 明确敏感配置、密钥注入、账号信息的边界 | 所有会修改配置的人 |

## 2. 第一次接入建议

- 优先走本地开发模式，不要一开始就把 `docker/` 目录当作唯一事实来源。
- 先跑“最小可运行链路”，确认登录、店铺、搜索和后台管理可用后，再补 AI、审核、积分、支付等扩展能力。
- `docker/` 下仍保留部分历史模块命名与复制脚本，使用前要和当前 Maven 模块、当前端口、当前 JAR 名称逐项核对。

## 3. 两条启动路径

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

## 4. 推荐启动顺序

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

## 5. 依赖矩阵

| 能力 | 必需依赖 | 备注 |
|:---|:---|:---|
| 登录鉴权 | MySQL、Redis、Nacos | auth + gateway |
| 店铺/商品/订单基础链路 | MySQL、Redis、RabbitMQ、Nacos | 常规业务必需 |
| 搜索 | Elasticsearch、Redis、RabbitMQ、Nacos | search 模块 |
| AI 对话 / RAG | Milvus、Elasticsearch、RabbitMQ、Nacos | 还需要单独配置模型 API key |
| 文件服务 | MinIO、Nacos | 本地文件路径也要配置 |
| 定时任务 | XXL-JOB、Nacos | 依赖 `xxl-job-common.yml` |
| 流量治理 | Sentinel、Nacos | 可按需启用 |

## 6. 配置来源

- 本地端口、基础应用名、部分默认连接参数位于各模块的 `bootstrap.yml`。
- 共享配置和业务配置主要通过 Nacos 提供，初始化数据见 `sql/ry_config_20250902.sql`。
- `order`、`product`、`interaction` 等模块还依赖 `xxl-job-common.yml`；如果缺少这个 dataId，定时任务相关能力无法正常运行。
- Docker 相关构建与复制逻辑位于 `docker/` 目录，但其中存在历史脚本，请在使用前自行核对。

## 7. 实际服务端口

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

## 8. 第一天建议验证什么

- 网关可访问，登录鉴权可用。
- 用户、店铺、搜索三类基础接口可正常返回。
- Nacos 中 `application-dev.yml` 与 `smartLive-*-dev.yml` 已导入。
- RabbitMQ、Redis、MySQL 连接日志正常，没有残留 `127.0.0.1` 的错误配置。
- 如果补了搜索或 AI，确认 Elasticsearch、Milvus 相关连接正常。

## 9. AI 密钥与敏感配置

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

## 10. 已知注意事项

- `bin/run-*.bat` 仍保留旧的 `ruoyi-*` 路径，当前不应作为对外主推荐启动方式。
- `docker/copy.sh`、`docker/deploy.sh` 和 `docker-compose.yml` 中仍有 `marketing`、`map` 等历史命名，使用前请先校对。
- 如果你修改了端口、模块名、依赖或启动顺序，请同时更新 `README.md`、`docs/OPEN_SOURCE.md` 和 `docs/SHOWCASE.md`。
