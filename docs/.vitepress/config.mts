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
      { text: '3分钟评审', link: '/THREE_MINUTES' },
      { text: '项目全貌', link: '/PROJECT_OVERVIEW' },
      { text: '业务走查', link: '/SHOWCASE' },
      { text: '核心链路', link: '/core-links/' },
      { text: '开源接入', link: '/OPEN_SOURCE' }
    ],

    sidebar: [
      {
        text: '💡 项目概览与导览',
        collapsed: true,
        items: [
          { text: '01 3分钟快速评审', link: '/THREE_MINUTES' },
          { text: '02 系统架构与项目规模', link: '/site-pages/SYSTEM_ARCHITECTURE' },
          { text: '03 数据模型与核心表关系', link: '/site-pages/DATA_MODEL' },
          { text: '04 页面效果图导览', link: '/PAGE_GALLERY' },
          { text: '05 业务链路视觉走查', link: '/SHOWCASE' },
          { text: '06 开源启动与接入', link: '/OPEN_SOURCE' },
          { text: '07 部署说明', link: '/DEPLOYMENT_GUIDE' }
        ]
      },
      {
        text: '🔥 核心链路与实现',
        collapsed: true,
        items: [
          { text: '01 核心链路总览', link: '/core-links/' },
          { text: '02 秒杀抢购链路', link: '/core-links/秒杀抢购全链路详解' },
          { text: '03 订单支付退款', link: '/core-links/2. 下单_统一支付_退款补偿链路' },
          { text: '04 Redis 分层缓存', link: '/core-links/Redis分层缓存链路详解' },
          { text: '05 RabbitMQ 可靠性', link: '/core-links/RabbitMQ消息可靠性全链路详解' },
          { text: '06 审核链与搜索双写', link: '/core-links/5. 审核中心责任链 + 发布审核与搜索 向量同步链路' },
          { text: '07 Feed 与互动同步', link: '/core-links/4. Feed 推送与滚动读取 + 互动双轨同步链路' },
          { text: '08 AI 路由与 RAG', link: '/core-links/8. AI Agent策略路由与RAG多维增强生成链路' },
          { text: '09 LBS 搜索与热词', link: '/core-links/6. 搜索读链路 + 热词沉淀链路' },
          { text: '10 热榜洗牌与重建', link: '/core-links/7. 热榜增量洗牌与全量重建链路' }
        ]
      },
      {
        text: '🎤 面试拆解与答辩',
        collapsed: true,
        items: [
          { text: '01 项目全貌与答辩说明', link: '/PROJECT_OVERVIEW' },
          { text: '02 核心亮点与项目价值', link: '/site-pages/CORE_HIGHLIGHTS' },
          { text: '03 我的核心设计与实现', link: '/site-pages/CONTRIBUTIONS' },
          { text: '04 性能指标与结果', link: '/site-pages/PERFORMANCE' },
          { text: '05 数据建模与表关系怎么讲', link: '/DATA_MODEL_INTERVIEW' },
          { text: '06 技术选型理由', link: '/site-pages/TECH_SELECTION' },
          { text: '07 Spring AI 与传统 RAG 对照', link: '/site-pages/SPRING_AI_RAG_COMPARISON' },
          { text: '08 难点踩坑与解决方案', link: '/site-pages/PITFALLS' },
          { text: '09 未来规划 Roadmap', link: '/site-pages/ROADMAP' },
          { text: '10 核心架构拷问 FAQ', link: '/site-pages/FAQ' }
        ]
      },
      {
        text: '👨‍💻 项目复盘与学习',
        collapsed: true,
        items: [
          { text: '01 项目沉淀与学习复盘', link: '/site-pages/LEARNINGS' },
          { text: '02 开发历程与演进记录', link: '/site-pages/DEVELOPMENT_HISTORY' },
          { text: '03 项目驱动学习复盘指南', link: '/core-links/SmartLive_Java_Internship_Review_Plan_Updated' }
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
      provider: 'local',
      options: {
        translations: {
          button: {
            buttonText: '站内搜索',
            buttonAriaLabel: '站内搜索'
          },
          modal: {
            displayDetails: '显示详细结果',
            resetButtonTitle: '清空搜索',
            backButtonTitle: '关闭搜索',
            noResultsText: '未找到与以下内容相关的结果',
            footer: {
              selectText: '打开结果',
              selectKeyAriaLabel: '回车',
              navigateText: '切换结果',
              navigateUpKeyAriaLabel: '向上箭头',
              navigateDownKeyAriaLabel: '向下箭头',
              closeText: '关闭',
              closeKeyAriaLabel: 'Esc'
            }
          }
        }
      }
    },

  }
})
