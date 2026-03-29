<template>
  <footer class="smartlive-site-footer">
    <div class="footer-shell">
      <div class="footer-top">
        <section class="footer-brand">
          <div class="brand-logo">
            <span class="logo-text">SmartLive</span>
            <span class="logo-dot"></span>
          </div>
          <p class="brand-title">智评生活 · 本地生活微服务平台</p>
          <p class="brand-desc">
            覆盖发现、交易、履约、社交、搜索、IM 与 AI RAG 的完整业务闭环，
            当前文档站已整理为可阅读、可答辩、可开源接入的一体化资料。
          </p>

          <div class="social-links">
            <a
              v-for="item in socialLinks"
              :key="item.text"
              :href="item.href"
              target="_blank"
              rel="noopener noreferrer"
              class="social-btn"
            >
              <span v-html="item.icon"></span>
              <span>{{ item.text }}</span>
            </a>
          </div>
        </section>

        <section class="footer-links">
          <div v-for="group in linkGroups" :key="group.title" class="link-group">
            <h4 class="group-title">{{ group.title }}</h4>
            <ul>
              <li v-for="item in group.items" :key="item.text">
                <a
                  :href="item.href"
                  :target="item.external ? '_blank' : undefined"
                  :rel="item.external ? 'noopener noreferrer' : undefined"
                >
                  {{ item.text }}
                </a>
              </li>
            </ul>
          </div>
        </section>
      </div>

      <div class="footer-bottom">
        <div class="copyright">
          <p>Released under the MIT License</p>
          <p>Copyright © 2026-present mumulinya</p>
        </div>
        <div class="footer-status">
          <span class="status-dot"></span>
          <span>文档持续维护中</span>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { withBase } from 'vitepress'

type FooterLink = {
  text: string
  href: string
  external?: boolean
}

const githubIcon = `
  <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true">
    <path d="M12 2A10 10 0 0 0 2 12c0 4.42 2.87 8.17 6.84 9.5.5.08.66-.23.66-.5v-1.69c-2.77.6-3.36-1.34-3.36-1.34-.45-1.15-1.11-1.46-1.11-1.46-.9-.62.07-.6.07-.6 1 .07 1.53 1.03 1.53 1.03.87 1.52 2.34 1.07 2.91.83.09-.65.35-1.09.63-1.34-2.22-.25-4.55-1.11-4.55-4.92 0-1.11.38-2 1.03-2.71-.1-.25-.45-1.29.1-2.64 0 0 .84-.27 2.75 1.02.79-.22 1.65-.33 2.5-.33.85 0 1.71.11 2.5.33 1.91-1.29 2.75-1.02 2.75-1.02.55 1.35.2 2.39.1 2.64.65.71 1.03 1.6 1.03 2.71 0 3.82-2.34 4.66-4.57 4.91.36.31.69.92.69 1.85V21c0 .27.16.59.67.5C19.14 20.16 22 16.42 22 12A10 10 0 0 0 12 2Z"></path>
  </svg>
`

const giteeIcon = `
  <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true">
    <path d="M11.984 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0a12 12 0 0 0-.016 0zm6.09 5.333c.328 0 .593.266.592.593v1.482a.594.594 0 0 1-.593.592H9.777c-.982 0-1.778.796-1.778 1.778v5.63c0 .327.266.592.593.592h5.63c.982 0 1.778-.796 1.778-1.778v-.296a.593.593 0 0 0-.592-.593h-4.15a.592.592 0 0 1-.592-.592v-1.482a.593.593 0 0 1 .593-.592h6.815c.327 0 .593.265.593.592v3.408a4 4 0 0 1-4 4H5.926a.593.593 0 0 1-.593-.593V9.778a4 4 0 0 1 4-4h8.74v-.001z"></path>
  </svg>
`

const socialLinks: Array<FooterLink & { icon: string }> = [
  {
    text: 'GitHub',
    href: 'https://github.com/mumulinya/smartLive-Cloud',
    external: true,
    icon: githubIcon
  },
  {
    text: 'Gitee',
    href: 'https://gitee.com/mumulinya/smart-live',
    external: true,
    icon: giteeIcon
  }
]

const linkGroups: Array<{ title: string; items: FooterLink[] }> = [
  {
    title: '文档',
    items: [
      { text: '项目全貌', href: withBase('/PROJECT_OVERVIEW') },
      { text: '系统架构', href: withBase('/site-pages/SYSTEM_ARCHITECTURE') },
      { text: '页面总览', href: withBase('/PAGE_GALLERY') },
      { text: '业务走查', href: withBase('/SHOWCASE') }
    ]
  },
  {
    title: '专题',
    items: [
      { text: '核心链路', href: withBase('/core-links/') },
      { text: '开源接入', href: withBase('/OPEN_SOURCE') },
      { text: '部署说明', href: withBase('/DEPLOYMENT_GUIDE') },
      { text: '常见问题 FAQ', href: withBase('/site-pages/FAQ') }
    ]
  },
  {
    title: '仓库与协作',
    items: [
      { text: '后端主仓库', href: 'https://github.com/mumulinya/smartLive-Cloud', external: true },
      { text: '提交 Issue', href: 'https://github.com/mumulinya/smartLive-Cloud/issues', external: true },
      { text: '参与贡献', href: 'https://github.com/mumulinya/smartLive-Cloud/blob/main/CONTRIBUTING.md', external: true },
      { text: '开源协议', href: 'https://github.com/mumulinya/smartLive-Cloud/blob/main/LICENSE', external: true }
    ]
  }
]
</script>

<style scoped>
.smartlive-site-footer {
  margin-top: 56px;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.05), transparent 32%),
    radial-gradient(circle at top right, rgba(15, 118, 110, 0.05), transparent 28%),
    linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 1));
}

.dark .smartlive-site-footer {
  border-top-color: rgba(56, 189, 248, 0.1);
  background:
    radial-gradient(circle at top left, rgba(56, 189, 248, 0.08), transparent 28%),
    radial-gradient(circle at top right, rgba(45, 212, 191, 0.08), transparent 24%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.92), rgba(2, 6, 23, 0.98));
}

.footer-shell {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 32px 24px;
}

.footer-top {
  display: flex;
  justify-content: space-between;
  gap: 40px;
  flex-wrap: wrap;
}

.footer-brand {
  flex: 1 1 320px;
  max-width: 420px;
}

.brand-logo {
  display: inline-flex;
  align-items: baseline;
  margin-bottom: 14px;
}

.logo-text {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.02em;
  background: linear-gradient(135deg, #2563eb, #0f766e);
  -webkit-background-clip: text;
  color: transparent;
}

.dark .logo-text {
  background: linear-gradient(135deg, #38bdf8, #2dd4bf);
  -webkit-background-clip: text;
}

.logo-dot {
  width: 8px;
  height: 8px;
  margin-left: 4px;
  border-radius: 999px;
  background: #2563eb;
}

.dark .logo-dot {
  background: #38bdf8;
}

.brand-title {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
}

.brand-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.75;
  color: #64748b;
}

.dark .brand-title {
  color: #e2e8f0;
}

.dark .brand-desc {
  color: #94a3b8;
}

.social-links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.social-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 12px;
  text-decoration: none;
  color: #334155;
  font-size: 14px;
  font-weight: 600;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(255, 255, 255, 0.72);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.social-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  border-color: rgba(37, 99, 235, 0.2);
}

.dark .social-btn {
  color: #cbd5e1;
  border-color: rgba(56, 189, 248, 0.14);
  background: rgba(30, 41, 59, 0.7);
}

.dark .social-btn:hover {
  border-color: rgba(56, 189, 248, 0.28);
  box-shadow: 0 10px 24px rgba(2, 6, 23, 0.32);
}

.footer-links {
  flex: 1 1 560px;
  display: grid;
  grid-template-columns: repeat(3, minmax(150px, 1fr));
  gap: 28px;
}

.group-title {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.dark .group-title {
  color: #f8fafc;
}

.link-group ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.link-group li + li {
  margin-top: 10px;
}

.link-group a {
  color: #64748b;
  font-size: 14px;
  text-decoration: none;
  transition: color 0.2s ease;
}

.link-group a:hover {
  color: #2563eb;
}

.dark .link-group a {
  color: #94a3b8;
}

.dark .link-group a:hover {
  color: #38bdf8;
}

.footer-bottom {
  margin-top: 28px;
  padding-top: 18px;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.dark .footer-bottom {
  border-top-color: rgba(56, 189, 248, 0.12);
}

.copyright p {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
  color: #64748b;
}

.dark .copyright p {
  color: #94a3b8;
}

.footer-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.72);
  color: #475569;
  font-size: 13px;
  font-weight: 500;
}

.dark .footer-status {
  border-color: rgba(56, 189, 248, 0.14);
  background: rgba(30, 41, 59, 0.7);
  color: #cbd5e1;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #10b981;
}

@media (max-width: 960px) {
  .footer-links {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }
}

@media (max-width: 640px) {
  .footer-shell {
    padding: 32px 20px 20px;
  }

  .footer-links {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .footer-bottom {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
