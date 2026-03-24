---
layout: home

title: SmartLive 智评生活
titleTemplate: 开源本地生活微服务平台

hero:
  name: SmartLive 智评生活
  text: 本地生活微服务平台与完整业务闭环
  tagline: 覆盖发现、交易、履约、社交、热榜与 AI/RAG 的工程化落地实践
  actions:
    - theme: brand
      text: 🚀 快速开始
      link: /OPEN_SOURCE
    - theme: alt
      text: 🌊 核心链路解密
      link: /core-links/秒杀抢购全链路详解

features:
  - title: ⚡ 极致性能与交易引擎
    details: 结合 Redis 分层缓存、ZSet 滚动分页与 Lua 脚本防超卖，保障高并发下的并发安全与关键状态收敛。
  - title: 🤖 AI 智能微服务赋能
    details: 深度整合 Spring AI 与 Milvus 向量库，三套 Agent 落地 C端智能检索与 B端图文AIGC分析。
  - title: 🛡️ 企业级边界防线与治理
    details: 覆盖 16 个业务模块与 19 个服务应用，涵盖 Gateway 鉴权、Netty 长连接推送、责任链多维审核与流转调度闭环。
  - title: 🧭 项目驱动复盘资料
    details: 配套核心链路解析、页面导览与项目驱动学习复盘指南，适合做项目讲解、源码阅读与面试展开。
---

<br/>

<div align="center">
  <p>
    <img src="https://img.shields.io/badge/Spring%20Boot-3.2.2-brightgreen.svg" alt="Spring Boot">
    <img src="https://img.shields.io/badge/Spring%20Cloud-2023.0.0-blue.svg" alt="Spring Cloud">
    <img src="https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0.1.0-orange.svg" alt="Spring Cloud Alibaba">
    <img src="https://img.shields.io/badge/JDK-17+-red.svg" alt="JDK">
  </p>
</div>

## 📌 快速事实

| 维度 | 数据 |
|:---|:---|
| 核心业务模块 | **16** 个业务模块 |
| 可运行服务应用 | **19** 个 |
| 展示版业务链路图 | **32** 张 |
| 详细版业务链路图 | **32** 张 |
| 真实页面截图 | **85** 张 |
| XXL-JOB 调度任务 | **28** 个 |
| 端侧覆盖 | 用户端 App + 管理端 Web |

## 🧭 第一次看项目怎么读

如果你是第一次进入 SmartLive，建议不要一上来就试图把所有模块、页面和中间件一次性看完。更顺的阅读方式是：

1. 先看 [开源启动与接入](/OPEN_SOURCE)，建立项目边界、依赖矩阵和启动顺序的整体认知。
2. 再看 [页面效果图导览](/PAGE_GALLERY)，快速知道用户端 App 和管理端 Web 分别覆盖了哪些真实业务页面。
3. 接着看 [业务链路视觉走查](/SHOWCASE)，把截图和缓存、调度、审核、交易等核心链路对上。
4. 最后进入 [核心链路解密](/core-links/秒杀抢购全链路详解)，按主题深入看源码级的实现细节。

## 🚀 最小体验路径

如果你想先快速感受项目，而不是立刻把全部依赖跑齐，推荐先按下面这条路径体验：

| 目标 | 先做什么 | 再看什么 | 结果 |
|:---|:---|:---|:---|
| 快速判断项目值不值得继续看 | 看 [页面效果图导览](/PAGE_GALLERY) | 看 [业务链路视觉走查](/SHOWCASE) | 先建立对页面、业务范围和系统复杂度的直观印象 |
| 先理解系统是怎么拆模块的 | 看 [开源启动与接入](/OPEN_SOURCE) | 看 [项目全貌与答辩说明](/PROJECT_OVERVIEW) | 建立模块边界、依赖关系和技术主题的全局认知 |
| 先跑最小链路 | 启动 `auth -> gateway -> system -> user -> shop -> search` | 再回来看 [业务链路视觉走查](/SHOWCASE) | 先验证登录、店铺、搜索和后台管理，再逐步补齐进阶能力 |
| 深挖最有代表性的技术实现 | 从 [秒杀抢购全链路](/core-links/秒杀抢购全链路详解) 开始 | 再补 Redis、RabbitMQ、Feed、审核、AI 相关链路 | 先吃透一条强链路，再扩到整套系统设计 |

## 🏗️ 系统全景架构

**SmartLive 智评生活** 把本地生活场景中的交易、内容、社交和 AI 能力放进同一套微服务体系里，强调真实业务闭环和工程化落地。

<div align="center">
  <img src="./screenshots/architecture.png" alt="System Architecture" width="90%" style="border-radius: 8px; margin-top: 20px; margin-bottom: 20px; box-shadow: 0 4px 14px 0 rgba(0,0,0,0.1);"/>
</div>

## 💎 核心技术亮点

本项目不以单点功能展示为目标，而是围绕真实业务链路，落地了一组可运行、可复盘、可继续扩展的技术方案：

- **高并发与强一致交易**：构建 Redis 分层缓存（ZSet/String/计数器三重隔离），结合 Lua 原子脚本完成秒杀防超卖，并通过 RabbitMQ 延迟队列和补偿机制兜住超时订单与状态回滚。
- **大规模社交内容流**：Feed 动态分发采用推拉结合模型与 ZSet 游标滚动分页，避免社交场景下的数据偏移；审核侧通过策略模式与责任链承接多业务线的异步审核。
- **推荐、搜索与热榜维护**：通过热榜增量洗牌与凌晨全量重建承接榜单维护，以 Elasticsearch 和热词统计支撑搜索发现链路。
- **AI/RAG 与业务融合**：整合 Spring AI 与 Milvus 向量检索，把用户侧问答、商家经营分析、差评回复和 AIGC 生成落到真实业务里。

## 📊 项目规模与工程特征

这个项目更像一套完整业务系统，而不是单个功能样例，因此在模块规模、链路覆盖和中间件协同上都更完整。

| 核心考察维度 | 规模与架构特征详情 |
|:---|:---|
| **核心业务微服务** | **16 个业务模块 + 3 个基础服务**（auth、gateway、monitor） |
| **端侧与体验覆盖** | 用户端 App + 商家后台 Web + 平台治理与调度视角 |
| **代码与工程资产** | 70,000+ 行 Java 代码、展示版/详细版链路图、真实页面截图与在线文档 |
| **持久化与基建中间件** | MySQL 8.x、Redis 集群架构、RabbitMQ、Elasticsearch 7.x、Milvus 2.x、MinIO |
| **流量网关与微服务治理** | Nacos 注册配置双核心、Spring Cloud Gateway 动态路由、Sentinel 毫秒级熔断限流 |

> 想继续往下看核心链路、源码拆解和项目驱动复盘，可以从顶部导航进入 **核心链路解密**。

## 🚀 极速启动与本地体验

想立刻在本地跑起来？只需简单几步克隆代码并一键拉起后端核心依赖：

```bash
# 1. 获取后端源码
git clone https://gitee.com/mumulinya/smart-live.git

# 2. 导入 MySQL / Redis / Nacos 配置
# 请参考我们在 /docs/OPEN_SOURCE.md 中提供的初始化脚本

# 3. IDEA 启动核心微服务组件
```
> 👉 详细的 Docker 完整部署与微服务启动顺序，请参阅 **[快速入门指南](/OPEN_SOURCE)**。

<style>
:root {
  --vp-home-hero-name-color: transparent;
  --vp-home-hero-name-background: -webkit-linear-gradient(120deg, #bd34fe, #41d1ff);
}
</style>
