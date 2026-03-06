# SmartLive 开源使用说明

## 当前状态

- 当前仓库主要面向后端微服务、架构展示和本地开发调试。
- 第一次接入建议优先走本地开发模式，不建议直接把 docker/ 目录当作唯一事实来源。
- docker/ 下仍保留部分历史模块命名与脚本残留，使用前请先核对它与当前 Maven 模块是否一致。

## 两条启动路径

### 路径一：最小可运行链路

适合先把服务跑起来、验证核心链路是否通畅。

- 必需中间件：MySQL、Redis、Nacos、RabbitMQ
- 建议优先启动：smartLive-auth、smartLive-gateway、smartLive-system、smartLive-user、smartLive-shop、smartLive-search
- 如果只验证登录、店铺、基础搜索和后台管理，这条路径通常已经足够

### 路径二：完整功能链路

适合验证 AI、搜索、文件、定时任务、积分和支付相关能力。

- 额外中间件：Elasticsearch、Milvus、MinIO、XXL-JOB、Sentinel
- 额外模块：smartLive-product、smartLive-order、smartLive-interaction、smartLive-chat、smartLive-im、smartLive-ai、smartLive-wallet、smartLive-points、smartLive-blog、smartLive-audit、smartLive-file
- 如果要体验 RAG、向量检索、异步审核、积分抽奖或延迟队列链路，必须补齐这些依赖

## 配置来源

- 本地端口、基础应用名、部分默认连接参数位于各模块的 bootstrap.yml。
- 共享配置和业务配置主要通过 Nacos 提供，初始化数据见 sql/ry_config_20250902.sql。
- order、product、interaction 等模块还依赖 xxl-job-common.yml；如果缺少这个 dataId，定时任务相关能力无法正常运行。
- Docker 相关构建与复制逻辑位于 docker/ 目录，但其中存在历史脚本，请在使用前自行核对。

## 依赖矩阵

| 能力 | 必需依赖 | 备注 |
|:---|:---|:---|
| 登录鉴权 | MySQL、Redis、Nacos | auth + gateway |
| 店铺/商品/订单基础链路 | MySQL、Redis、RabbitMQ、Nacos | 常规业务必需 |
| 搜索 | Elasticsearch、Redis、RabbitMQ、Nacos | search 模块 |
| AI 对话 / RAG | Milvus、Elasticsearch、RabbitMQ、Nacos | 还需要单独配置模型 API key |
| 文件服务 | MinIO、Nacos | 本地文件路径也要配置 |
| 定时任务 | XXL-JOB、Nacos | 依赖 xxl-job-common.yml |
| 流量治理 | Sentinel、Nacos | 可按需启用 |

## 实际服务端口

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

## AI 密钥与敏感配置

不要把真实 API key、数据库密码或外网可用的测试账号直接提交到仓库。

本地开发也可以直接使用 config/smartlive-ai-secrets.yml；仓库内提供了 config/smartlive-ai-secrets.example.yml 作为示例。

建议至少使用以下环境变量：

- SMARTLIVE_ZHIPU_API_KEY
- SMARTLIVE_OPENAI_API_KEY
- SMARTLIVE_OPENAI_CHAT_API_KEY
- SMARTLIVE_EMBEDDING_API_KEY
- MILVUS_HOST
- RABBITMQ_HOST
- NACOS_HOST

## 已知注意事项

- bin/run-*.bat 仍保留旧的 ruoyi-* 路径，当前不应作为对外主推荐启动方式。
- docker/copy.sh、docker/deploy.sh 和 docker-compose.yml 中仍有 marketing、map 等历史命名，使用前请先校对。
- 如果你修改了端口、模块名、依赖或启动顺序，请同时更新 README.md 和本文档。