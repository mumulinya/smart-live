import { defineConfig } from 'vitepress'

export default defineConfig({
  ignoreDeadLinks: true,
  title: "SmartLive 智评生活",
  description: "企业级微服务全栈项目架构剖析与全链路面经",
  base: '/smart-live/',
  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      { text: '🎯 快速入门', link: '/OPEN_SOURCE' },
      { text: '👁️ 视觉导览', link: '/SHOWCASE' },
      { text: '🔥 核心链路', link: '/core-links/秒杀抢购全链路详解' },
      { text: '源码仓库', link: 'https://github.com/mumulinya/smart-live' },
    ],

    sidebar: [
      {
        text: '💡 项目概览',
        items: [
          { text: '开源启动与接入', link: '/OPEN_SOURCE' },
          { text: '项目全貌与答辩说明', link: '/PROJECT_OVERVIEW' },
          { text: '页面效果图导览', link: '/PAGE_GALLERY' },
          { text: '业务链路视觉走查', link: '/SHOWCASE' }
        ]
      },
      {
        text: '🔥 核心链路深度解析',
        collapsed: false,
        items: [
          { text: '秒杀抢购全链路', link: '/core-links/秒杀抢购全链路详解' },
          { text: 'Redis 分层缓存架构', link: '/core-links/Redis分层缓存链路详解' },
          { text: '订单、支付与退款补偿', link: '/core-links/2. 下单_统一支付_退款补偿链路' },
          { text: 'RabbitMQ 消息可靠机制', link: '/core-links/RabbitMQ消息可靠性全链路详解' },
          { text: 'Feed 推送与互动同步', link: '/core-links/4. Feed 推送与滚动读取 + 互动双轨同步链路' },
          { text: '多维审核链与搜索双写', link: '/core-links/5. 审核中心责任链 + 发布审核与搜索 向量同步链路' },
          { text: 'LBS 搜索与热词排行', link: '/core-links/6. 搜索读链路 + 热词沉淀链路' },
          { text: '大盘热榜洗牌与重建', link: '/core-links/7. 热榜增量洗牌与全量重建链路' },
          { text: 'AI路由策略与 RAG生成', link: '/core-links/8. AI Agent策略路由与RAG多维增强生成链路' }
        ]
      },
      {
        text: '👨‍💻 学习与复盘总结',
        items: [
          { text: '学习复盘与定投总结表', link: '/core-links/SmartLive_Java_Internship_Review_Plan_Updated' }
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/mumulinya/smart-live' }
    ],

    outline: {
      level: [2, 3],
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
