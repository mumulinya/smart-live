---
layout: home

title: SmartLive 智评生活
titleTemplate: 开源本地生活微服务平台

hero:
  name: SmartLive 智评生活
  text: 基于 Spring Cloud Alibaba 的核心全栈微服务架构
  tagline: 发现、下单、履约、评价、社交、AI 问答一站式体验闭环
  actions:
    - theme: brand
      text: 🚀 快速开始
      link: /OPEN_SOURCE
    - theme: alt
      text: 🌊 核心链路解密
      link: /core-links/秒杀抢购全链路详解

features:
  - title: ⚡ 极致性能与交易引擎
    details: 结合 Redis 分层缓存、ZSet 滚动分页与 Lua 脚本防超卖，保障高并发下的并发安全与强一致性。
  - title: 🤖 AI 智能微服务赋能
    details: 深度整合 Spring AI 与 Milvus 向量库，三套 Agent 落地 C端智能检索与 B端图文AIGC分析。
  - title: 🛡️ 企业级边界防线与治理
    details: 覆盖 18+ 模块矩阵，涵盖 GateWay 鉴权、Netty 长连接推送、责任链多维审核与流转调度闭环。
  - title: 👨‍💻 面试与学习绝佳素材
    details: 配备 9 大核心底层链路的源码级深度深度解析表，带你复盘重点，助你彻底吃透微服务大后端。
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

## 🏗️ 系统全景架构

**SmartLive 智评生活** 首创性地将「本地电商交易」与「社交内容分享」完美融合，并全方位拥抱 AI 大模型能力。

<div align="center">
  <img src="./screenshots/architecture.png" alt="System Architecture" width="90%" style="border-radius: 8px; margin-top: 20px; margin-bottom: 20px; box-shadow: 0 4px 14px 0 rgba(0,0,0,0.1);"/>
</div>

## 💎 核心技术亮点与含金量

本项目拒绝常规的 CRUD 式堆砌，真实落地了诸多大厂高频技术与复杂场景的底层解决方案：

- **高并发与强一致交易**：构建 Redis 分层缓存（ZSet/String/计数器三重隔离），借助 Lua 原子脚本完美闭环秒杀防超卖；基于 RabbitMQ 打造消息绝对可靠投递与延迟队列订单补偿防线。
- **大规模社交内容双轴流**：Feed 动态分发采用百万级推拉结合扇出模型，以 ZSet 游标滚动分页彻底斩断数据偏移痛点；通过策略模式与责任链搭建海量 UGC 异步极速审核管道。
- **大盘基建与探索式推荐**：创新性实现热度榜单由于冷启动带来的增量洗牌机制，配合凌晨全量重构建构；Elasticsearch 强力全文检索与 ZINCRBY 热词基建同步，沉淀千人千面探索池。
- **前沿 AI/RAG 融合落地**：深度解耦 Spring AI 与 Milvus 向量引擎架构，实现涵盖消费图文垂类问答、B端商家多维经营分析与复杂意图分流路由的多智能体（Agent）协作中枢。

## 📊 项目规模与微服务架构集群

作为一款着眼于完整生命周期的全栈核心业务线级产品，具备庞大且规范的工程阵列。

| 核心考察维度 | 规模与架构特征详情 |
|:---|:---|
| **核心业务微服务** | **18+ 个独立组件**（涵盖网关、流控、鉴权、交易、社交、治理、搜索、IM、AI 域） |
| **端侧与体验覆盖** | ToC 沉浸式消费探索端 + ToB 商家完整经营链路端 + 平台宏观全维度内容治理 |
| **代码与运行态监控** | 30,000+ 行领域驱动风格核心代码、5,000+ 行严谨单元测试、全局异常降级与熔断保底 |
| **持久化与基建中间件** | MySQL 8.x、Redis 集群架构、RabbitMQ、Elasticsearch 7.x、Milvus 2.x、MinIO |
| **流量网关与微服务治理** | Nacos 注册配置双核心、Spring Cloud Gateway 动态路由、Sentinel 毫秒级熔断限流 |

> 👨‍💻 更多底层的**面试级全链路剖析与架构推演**，请点击顶部导航栏中的 👉 [核心链路解密](/core-links/秒杀抢购全链路详解) 进行源码级深度复盘。

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
