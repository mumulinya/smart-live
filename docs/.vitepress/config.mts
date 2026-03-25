import { defineConfig } from 'vitepress'

export default defineConfig({
  ignoreDeadLinks: true,
  lastUpdated: true,
  markdown: {
    lineNumbers: true
  },
  title: "SmartLive 智评生活",
  description: "企业级微服务全栈项目架构剖析与全链路面经",
  base: '/smartLive-Cloud/',
  scrollOffset: 96,
  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      { text: '开源接入', link: '/OPEN_SOURCE' },
      { text: '页面导览', link: '/PAGE_GALLERY' },
      { text: '视觉导览', link: '/SHOWCASE' },
      { text: '核心链路', link: '/core-links/' },
      { text: '面试入口', link: '/PROJECT_OVERVIEW' }
    ],

    sidebar: [
      {
        text: '💡 项目概览与导览',
        collapsed: true,
        items: [
          { text: '01 开源启动与接入', link: '/OPEN_SOURCE' },
          { text: '02 系统架构与项目规模', link: '/site-pages/SYSTEM_ARCHITECTURE' },
          { text: '03 页面效果图导览', link: '/PAGE_GALLERY' },
          { text: '04 业务链路视觉走查', link: '/SHOWCASE' }
        ]
      },
      {
        text: '🔥 核心链路与实现',
        collapsed: true,
        items: [
          { text: '01 核心链路总览', link: '/core-links/' },
          { text: '02 订单支付与退款补偿链路', link: '/core-links/2. 下单_统一支付_退款补偿链路' },
          { text: '03 秒杀抢购全链路', link: '/core-links/秒杀抢购全链路详解' },
          { text: '04 Redis 分层缓存链路', link: '/core-links/Redis分层缓存链路详解' },
          { text: '05 RabbitMQ 消息可靠链路', link: '/core-links/RabbitMQ消息可靠性全链路详解' },
          { text: '06 Feed 推送与互动同步链路', link: '/core-links/4. Feed 推送与滚动读取 + 互动双轨同步链路' },
          { text: '07 审核责任链与搜索双写', link: '/core-links/5. 审核中心责任链 + 发布审核与搜索 向量同步链路' },
          { text: '08 LBS 搜索与热词链路', link: '/core-links/6. 搜索读链路 + 热词沉淀链路' },
          { text: '09 热榜洗牌与全量重建链路', link: '/core-links/7. 热榜增量洗牌与全量重建链路' },
          { text: '10 AI 路由策略与 RAG 生成', link: '/core-links/8. AI Agent策略路由与RAG多维增强生成链路' }
        ]
      },
      {
        text: '🎤 面试拆解与答辩',
        collapsed: true,
        items: [
          { text: '01 项目全貌与答辩说明', link: '/PROJECT_OVERVIEW' },
          { text: '02 我的核心设计与实现', link: '/site-pages/CONTRIBUTIONS' },
          { text: '03 技术选型理由', link: '/site-pages/TECH_SELECTION' },
          { text: '04 难点踩坑与解决方案', link: '/site-pages/PITFALLS' },
          { text: '05 常见问题 FAQ', link: '/site-pages/FAQ' }
        ]
      },
      {
        text: '👨‍💻 项目复盘与学习',
        collapsed: true,
        items: [
          { text: '01 项目沉淀与学习复盘', link: '/site-pages/LEARNINGS' },
          { text: '02 项目驱动学习复盘指南', link: '/core-links/SmartLive_Java_Internship_Review_Plan_Updated' }
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/mumulinya/smartLive-Cloud' }
    ],

    outline: {
      level: [2, 4],
      label: '本页大纲'
    },

    search: {
      provider: 'local'
    },

    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Copyright © 2026-present mumulinya'
    }
  }
})
