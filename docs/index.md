---
layout: home

title: SmartLive 智评生活
titleTemplate: 开源本地生活微服务平台

hero:
  name: SmartLive 智评生活
  text: 本地生活微服务平台
  tagline: 覆盖发现、交易、履约、社交、热榜与 AI/RAG 的完整业务闭环实践
  actions:
    - theme: brand
      text: 🚀 快速开始
      link: /OPEN_SOURCE
    - theme: alt
      text: 👀 查看页面与链路
      link: /SHOWCASE
---

<br/>

<div class="smartlive-tech-strip">
  <span class="smartlive-tech-chip">Spring Boot 3.2.2</span>
  <span class="smartlive-tech-chip">Spring Cloud 2023.0.0</span>
  <span class="smartlive-tech-chip">Spring Cloud Alibaba 2023.0.1.0</span>
  <span class="smartlive-tech-chip">JDK 17+</span>
</div>

<div class="smartlive-home-nav">
  <a href="./site-pages/SYSTEM_ARCHITECTURE.html" class="smartlive-home-nav-pill">架构与规模</a>
  <a href="./PAGE_GALLERY.html" class="smartlive-home-nav-pill">页面导览</a>
  <a href="./SHOWCASE.html" class="smartlive-home-nav-pill">视觉导览</a>
  <a href="./core-links/" class="smartlive-home-nav-pill">核心链路</a>
  <a href="./site-pages/TECH_SELECTION.html" class="smartlive-home-nav-pill">技术选型</a>
</div>

## 📊 项目规模与关键数据

<div class="smartlive-stats-grid">
<!-- AUTO_SYNC:DOCS_HOME_STATS_CARDS:START -->
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
<!-- AUTO_SYNC:DOCS_HOME_STATS_CARDS:END -->
</div>

这套项目不是单个功能样例，而是一整套从用户发现、下单支付、履约评价到社交互动、热榜维护、AI/RAG 增强的完整业务系统。更多统计口径和服务边界可以继续看 [系统架构与项目规模](/site-pages/SYSTEM_ARCHITECTURE)。

## 🏗️ 系统全景架构

**SmartLive 智评生活** 把本地生活场景中的交易、内容、社交和 AI 能力放进同一套微服务体系里，强调真实业务闭环和工程化落地。

<div class="smartlive-figure-frame" align="center">
  <img src="./screenshots/architecture.png" alt="System Architecture" width="90%"/>
</div>

## ✨ 项目能力亮点

<div class="smartlive-feature-grid">
  <div class="smartlive-feature-card">
    <strong>⚡ 极致性能与交易引擎</strong>
    <span>结合 Redis 分层缓存、ZSet 滚动分页与 Lua 脚本防超卖，保障高并发下的并发安全与关键状态收敛。</span>
  </div>
  <div class="smartlive-feature-card">
    <strong>🤖 AI 智能微服务赋能</strong>
    <span>深度整合 Spring AI 与 Milvus 向量库，三套 Agent 落地 C 端智能检索与 B 端图文 AIGC 分析。</span>
  </div>
  <div class="smartlive-feature-card">
    <strong>🛡️ 企业级边界防线与治理</strong>
    <span>覆盖 16 个业务模块与 19 个服务应用，串起 Gateway 鉴权、Netty 推送、责任链审核与调度闭环。</span>
  </div>
  <div class="smartlive-feature-card">
    <strong>🧭 项目驱动复盘资料</strong>
    <span>配套核心链路解析、页面导览与项目驱动学习复盘指南，适合项目讲解、源码阅读与面试展开。</span>
  </div>
</div>

## 🎯 面试与项目拆解入口

<div class="smartlive-link-grid">
  <a href="./site-pages/TECH_SELECTION.html" class="smartlive-link-card">
    <span class="smartlive-link-tag">WHY</span>
    <strong>技术选型理由</strong>
    <span>面试被问“为什么选这个不用那个”时最适合展开。</span>
  </a>
  <a href="./site-pages/PITFALLS.html" class="smartlive-link-card">
    <span class="smartlive-link-tag">PITFALLS</span>
    <strong>难点踩坑与解决方案</strong>
    <span>集中讲缓存一致性、秒杀、Feed、热榜、审核治理这些真实工程问题。</span>
  </a>
  <a href="./site-pages/FAQ.html" class="smartlive-link-card">
    <span class="smartlive-link-tag">FAQ</span>
    <strong>常见问题 FAQ</strong>
    <span>13 条高频问答，适合面试前速查，也适合访客快速建立边界认知。</span>
  </a>
  <a href="./core-links/" class="smartlive-link-card">
    <span class="smartlive-link-tag">CHAINS</span>
    <strong>核心链路总览</strong>
    <span>先知道从哪条链路开始看，再逐条深入到源码级实现和设计取舍。</span>
  </a>
</div>

## 🧭 第一次看项目怎么读

如果你是第一次进入 SmartLive，建议不要一上来就试图把所有模块、页面和中间件一次性看完。更顺的阅读方式是：

1. 先看 [开源启动与接入](/OPEN_SOURCE)，建立项目边界、依赖矩阵和启动顺序的整体认知。
2. 再看 [页面效果图导览](/PAGE_GALLERY)，快速知道用户端 App 和管理端 Web 分别覆盖了哪些真实业务页面。
3. 接着看 [业务链路视觉走查](/SHOWCASE)，把截图和缓存、调度、审核、交易等核心链路对上。
4. 最后进入 [核心链路总览](/core-links/)，按主题选择最适合自己的阅读顺序，再深入看源码级实现细节。

## 🚀 最小体验路径

如果你想先快速感受项目，而不是立刻把全部依赖跑齐，推荐先按下面这条路径体验：

<div class="smartlive-path-grid">
  <div class="smartlive-path-card">
    <div class="smartlive-path-no">01</div>
    <strong>先看页面和图</strong>
    <span>先看 <a href="./PAGE_GALLERY.html">页面效果图导览</a> 和 <a href="./SHOWCASE.html">业务链路视觉走查</a>，建立直观印象。</span>
  </div>
  <div class="smartlive-path-card">
    <div class="smartlive-path-no">02</div>
    <strong>先理解模块边界</strong>
    <span>先看 <a href="./OPEN_SOURCE.html">开源启动与接入</a> 和 <a href="./PROJECT_OVERVIEW.html">项目全貌与答辩说明</a>，建立服务边界认知。</span>
  </div>
  <div class="smartlive-path-card">
    <div class="smartlive-path-no">03</div>
    <strong>先跑最小链路</strong>
    <span>先起 `auth -> gateway -> system -> user -> shop -> search`，再补齐更重的中间件和业务能力。</span>
  </div>
  <div class="smartlive-path-card">
    <div class="smartlive-path-no">04</div>
    <strong>再深挖强链路</strong>
    <span>优先看 <a href="./core-links/秒杀抢购全链路详解.html">秒杀</a>、<a href="./core-links/2.%20下单_统一支付_退款补偿链路.html">订单支付</a>、<a href="./core-links/Redis分层缓存链路详解.html">Redis 缓存</a>。</span>
  </div>
</div>

## 🗣️ 面试讲解路径

<div align="center">
  <img src="./diagrams/interview-pitch-path.svg" alt="SmartLive 面试讲解路径" width="100%">
</div>

如果你准备拿这个项目做面试讲解，最顺的讲法不是把所有模块都过一遍，而是按“全局 -> 页面 -> 强链路 -> 设计判断”来展开：

1. 先讲 [系统架构与项目规模](/site-pages/SYSTEM_ARCHITECTURE)，快速建立“16 个模块、19 个服务、双端覆盖、多中间件协同”的整体认知。
2. 再讲 [页面效果图导览](/PAGE_GALLERY) 和 [业务链路视觉走查](/SHOWCASE)，让面试官先看到真实页面和用户闭环，而不是只听抽象名词。
3. 然后只挑 3 条最强链路重点展开，建议优先讲：  
   [秒杀抢购全链路](/core-links/秒杀抢购全链路详解) / [订单、支付与退款补偿](/core-links/2.%20下单_统一支付_退款补偿链路) / [Redis 分层缓存链路详解](/core-links/Redis分层缓存链路详解)。
4. 最后再回到 [技术选型理由](/site-pages/TECH_SELECTION) 和 [难点踩坑与解决方案](/site-pages/PITFALLS)，把“为什么这么做、做的时候踩过什么坑”讲出来。

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
