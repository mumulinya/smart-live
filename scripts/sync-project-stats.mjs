import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const dataPath = path.join(root, 'docs', 'shared', 'project-stats.json')
const readmePath = path.join(root, 'README.md')
const docsIndexPath = path.join(root, 'docs', 'index.md')
const docsPublicDir = path.join(root, 'docs', 'public')
const diagramsSourceDir = path.join(root, 'docs', 'diagrams')
const diagramsPublicDir = path.join(docsPublicDir, 'diagrams')
const coreLinksDiagramsSourceDir = path.join(root, 'docs', 'core-links', 'diagrams')
const coreLinksDiagramsPublicDir = path.join(docsPublicDir, 'core-links', 'diagrams')
const sitePagesDir = path.join(root, 'docs', 'site-pages')
const systemArchitecturePath = path.join(sitePagesDir, 'SYSTEM_ARCHITECTURE.md')
const techSelectionPath = path.join(sitePagesDir, 'TECH_SELECTION.md')
const pitfallsPath = path.join(sitePagesDir, 'PITFALLS.md')
const faqPath = path.join(sitePagesDir, 'FAQ.md')
const contributionsPath = path.join(sitePagesDir, 'CONTRIBUTIONS.md')
const coreHighlightsPath = path.join(sitePagesDir, 'CORE_HIGHLIGHTS.md')
const projectOverviewPath = path.join(root, 'docs', 'PROJECT_OVERVIEW.md')

const data = JSON.parse(fs.readFileSync(dataPath, 'utf8'))

function readTextWithBom(filePath) {
  const raw = fs.readFileSync(filePath, 'utf8')
  const hasBom = raw.charCodeAt(0) === 0xfeff
  return { hasBom, text: hasBom ? raw.slice(1) : raw }
}

function writeTextWithBom(filePath, text, hasBom) {
  fs.writeFileSync(filePath, hasBom ? `\uFEFF${text}` : text, 'utf8')
}

function replaceBlock(content, marker, replacement) {
  const start = `<!-- AUTO_SYNC:${marker}:START -->`
  const end = `<!-- AUTO_SYNC:${marker}:END -->`
  const pattern = new RegExp(`${escapeRegExp(start)}[\\s\\S]*?${escapeRegExp(end)}`, 'm')
  if (!pattern.test(content)) {
    throw new Error(`Marker not found: ${marker}`)
  }
  return content.replace(pattern, `${start}\n${replacement}\n${end}`)
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function renderReadmeProjectScaleRows(rows) {
  return rows
    .map((row) => `| ${row.metric} | ${row.value} | ${row.description} |`)
    .join('\n')
}

function renderReadmeContributionBullets(items) {
  return items.map((item) => `- ${item}`).join('\n')
}

function renderDocsHomeCards(cards) {
  return cards
    .map(
      (card) => [
        '  <div class="smartlive-stat-card">',
        `    <div class="smartlive-stat-value">${card.value}</div>`,
        `    <div class="smartlive-stat-label">${card.label}</div>`,
        `    <div class="smartlive-stat-desc">${card.description}</div>`,
        '  </div>'
      ].join('\n')
    )
    .join('\n')
}

function renderDocsQuickFacts(rows) {
  return rows
    .map(
      (row) => [
        '  <div class="smartlive-fact-card">',
        `    <div class="smartlive-fact-metric">${row.metric}</div>`,
        `    <div class="smartlive-fact-value">${stripMarkdown(row.value)}</div>`,
        '  </div>'
      ].join('\n')
    )
    .join('\n')
}

function stripMarkdown(value) {
  return value.replace(/\*\*/g, '')
}

function ensureTrailingNewline(text) {
  return text.endsWith('\n') ? text : `${text}\n`
}

function writeGeneratedMarkdown(filePath, content) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  const existing = fs.existsSync(filePath) ? readTextWithBom(filePath) : { hasBom: true }
  writeTextWithBom(filePath, ensureTrailingNewline(content), existing.hasBom)
}

function syncStaticDirectory(sourceDir, targetDir) {
  if (!fs.existsSync(sourceDir)) {
    return
  }
  fs.rmSync(targetDir, { recursive: true, force: true })
  fs.mkdirSync(path.dirname(targetDir), { recursive: true })
  fs.cpSync(sourceDir, targetDir, { recursive: true })
}

function extractReadmeSection(text, startAnchorId, endAnchorId) {
  const lines = text.split('\n')
  const startPattern = `## <a id="${startAnchorId}"></a>`
  const endPattern = endAnchorId ? `## <a id="${endAnchorId}"></a>` : null

  const startIndex = lines.findIndex((line) => line.startsWith(startPattern))
  if (startIndex === -1) {
    throw new Error(`README section not found: ${startAnchorId}`)
  }

  const endIndex = endPattern
    ? lines.findIndex((line, index) => index > startIndex && line.startsWith(endPattern))
    : lines.findIndex((line, index) => index > startIndex && line.startsWith('## <a id="'))

  const sliceEnd = endIndex === -1 ? lines.length : endIndex
  return lines.slice(startIndex + 1, sliceEnd).join('\n').trim()
}

function normalizeReadmeContentForDocs(content) {
  return content
    .replace(/<!--[\s\S]*?-->\n?/g, '')
    .replace(/\[项目仓库\]\(#项目仓库\)/g, 'README 的“项目仓库”')
    .replace(/\]\(docs\/OPEN_SOURCE\.md([^)]+)?\)/g, (_match, hash = '') => `](/OPEN_SOURCE${hash || ''})`)
    .replace(/\]\(docs\/PAGE_GALLERY\.md([^)]+)?\)/g, (_match, hash = '') => `](/PAGE_GALLERY${hash || ''})`)
    .replace(/\]\(docs\/SHOWCASE\.md([^)]+)?\)/g, (_match, hash = '') => `](/SHOWCASE${hash || ''})`)
    .replace(/\]\(docs\/PROJECT_OVERVIEW\.md([^)]+)?\)/g, (_match, hash = '') => `](/PROJECT_OVERVIEW${hash || ''})`)
    .replace(/\]\(docs\/core-links\/([^)]+?)\.md\)/g, (_match, name) => `](/core-links/${name})`)
    .replace(/\]\(README\.md\)/g, '](https://github.com/mumulinya/smartLive-Cloud/blob/main/README.md)')
    .replace(/src="docs\/screenshots\//g, 'src="../screenshots/')
    .replace(/src="docs\/diagrams\//g, 'src="../diagrams/')
    .replace(/\]\(docs\/diagrams\/([^)]+)\)/g, (_match, target) => `](../diagrams/${target})`)
}

function renderReadmeDrivenPage(title, note, body) {
  return `# ${title}

> ${note}

${body}
`
}

function renderFaqPage(body) {
  const sections = []
  const pattern = /<details>\s*<summary><b>(.*?)<\/b><\/summary>\s*([\s\S]*?)<\/details>/g
  let match

  while ((match = pattern.exec(body)) !== null) {
    const title = match[1].trim()
    const content = match[2].trim()
    sections.push(`## ${title}\n\n${content}`)
  }

  const faqBody = sections.length > 0 ? sections.join('\n\n') : body

  return `# ❓ 常见问题 FAQ

> 本页根据 \`README.md\` 中“常见问题 FAQ”章节自动同步。

${faqBody}
`
}

function renderSystemArchitecturePage(cards, architectureBody, projectStructureBody) {
  return `# 🏗️ 系统架构与项目规模

> 本页根据 \`README.md\` 中“系统架构”和“项目结构”章节自动同步。

## 📊 项目规模与关键数据

<div class="smartlive-stats-grid">
<!-- AUTO_SYNC:SYSTEM_ARCH_STATS_CARDS:START -->
${renderDocsHomeCards(cards)}
<!-- AUTO_SYNC:SYSTEM_ARCH_STATS_CARDS:END -->
</div>

## 🔢 统计口径说明

${extractSyncBlock(readmeText, 'README_STATS_SCOPE')}

## 🗺️ 系统架构

${architectureBody}

## 📁 项目结构

${projectStructureBody}
`
}

function renderProjectOverviewPage(body) {
  const normalizedBody = body
    .replace(/^### /gm, '## ')
    .replace(/^#### /gm, '### ')

  return `# 🎤 项目全貌与答辩说明

> 本页根据 \`README.md\` 中“项目简介”章节自动同步，适合用作项目介绍、答辩说明与面试展开的统一入口。

如果你准备围绕项目做一次完整讲解，建议优先按这个顺序展开：

1. 项目定位与规模
2. 为什么值得继续看
3. 个人贡献亮点
4. 核心亮点与设计判断

${normalizedBody}
`
}

function extractHeadingBlock(text, startPattern, endPatterns = []) {
  const lines = text.split('\n')
  const startIndex = lines.findIndex((line) => startPattern.test(line))
  if (startIndex === -1) {
    throw new Error(`Heading block not found: ${startPattern}`)
  }

  let endIndex = lines.length
  for (let i = startIndex + 1; i < lines.length; i += 1) {
    if (endPatterns.some((pattern) => pattern.test(lines[i]))) {
      endIndex = i
      break
    }
  }

  return lines.slice(startIndex + 1, endIndex).join('\n').trim()
}

function renderContributionPage(body) {
  return `# 👨‍💻 我的核心设计与实现

> 本页根据 \`README.md\` 中“个人贡献亮点”章节自动同步，适合直接拿来做项目贡献讲解与简历展开。

建议讲法：

1. 先讲规模与交付密度
2. 再讲 2-3 条最强技术突破点
3. 最后补一条最能体现工程判断的设计取舍

${body}
`
}

function renderCoreHighlightsPage(body) {
  const normalizedBody = body.replace(/^#### /gm, '## ')

  return `# 🎯 项目级能力亮点

> 本页根据 \`README.md\` 中“核心亮点”章节自动同步，适合从项目视角快速讲清系统卖点与整体工程价值。

建议使用方式：

- 先用这页讲“项目厉害在哪”
- 再回到“个人贡献亮点”讲“我具体做了什么”
- 最后再挑 1-2 条核心链路展开到源码级实现

${normalizedBody}
`
}

function extractSyncBlock(text, marker) {
  const start = `<!-- AUTO_SYNC:${marker}:START -->`
  const end = `<!-- AUTO_SYNC:${marker}:END -->`
  const pattern = new RegExp(`${escapeRegExp(start)}\\n?([\\s\\S]*?)\\n?${escapeRegExp(end)}`, 'm')
  const match = text.match(pattern)
  if (!match) {
    throw new Error(`Sync block not found: ${marker}`)
  }
  return match[1].trim()
}

const readme = readTextWithBom(readmePath)
let readmeText = readme.text
readmeText = replaceBlock(
  readmeText,
  'README_PROJECT_SCALE',
  renderReadmeProjectScaleRows(data.readmeProjectScaleRows)
)
readmeText = replaceBlock(
  readmeText,
  'README_CONTRIBUTION_SCALE',
  renderReadmeContributionBullets(data.readmeContributionBullets)
)
writeTextWithBom(readmePath, readmeText, readme.hasBom)

const docsIndex = readTextWithBom(docsIndexPath)
let docsIndexText = docsIndex.text
docsIndexText = replaceBlock(
  docsIndexText,
  'DOCS_HOME_STATS_CARDS',
  renderDocsHomeCards(data.docsHomeCards)
)
if (docsIndexText.includes('<!-- AUTO_SYNC:DOCS_HOME_QUICK_FACTS:START -->')) {
  docsIndexText = replaceBlock(
    docsIndexText,
    'DOCS_HOME_QUICK_FACTS',
    renderDocsQuickFacts(data.docsHomeQuickFacts)
  )
}
writeTextWithBom(docsIndexPath, docsIndexText, docsIndex.hasBom)

const architectureBody = normalizeReadmeContentForDocs(extractReadmeSection(readmeText, '系统架构', '项目结构'))
const projectStructureBody = normalizeReadmeContentForDocs(extractReadmeSection(readmeText, '项目结构', '技术选型理由'))
const projectOverviewBody = normalizeReadmeContentForDocs(extractReadmeSection(readmeText, '项目简介', '5分钟读懂项目'))
const contributionBody = normalizeReadmeContentForDocs(
  extractHeadingBlock(
    extractReadmeSection(readmeText, '项目简介', '5分钟读懂项目'),
    /^### 👨‍💻 个人贡献亮点/,
    [/^### 🎯 核心亮点/]
  )
)
const coreHighlightsBody = normalizeReadmeContentForDocs(
  extractHeadingBlock(
    extractReadmeSection(readmeText, '项目简介', '5分钟读懂项目'),
    /^### 🎯 核心亮点/
  )
)
const techSelectionBody = normalizeReadmeContentForDocs(extractReadmeSection(readmeText, '技术选型理由', '核心业务链路'))
const pitfallsBody = normalizeReadmeContentForDocs(extractReadmeSection(readmeText, '难点踩坑与解决方案', '项目沉淀'))
const faqBody = normalizeReadmeContentForDocs(extractReadmeSection(readmeText, '常见问题', '未来规划'))

writeGeneratedMarkdown(
  projectOverviewPath,
  renderProjectOverviewPage(projectOverviewBody)
)
writeGeneratedMarkdown(
  contributionsPath,
  renderContributionPage(contributionBody)
)
writeGeneratedMarkdown(
  coreHighlightsPath,
  renderCoreHighlightsPage(coreHighlightsBody)
)
writeGeneratedMarkdown(
  systemArchitecturePath,
  renderSystemArchitecturePage(data.docsHomeCards, architectureBody, projectStructureBody)
)
writeGeneratedMarkdown(
  techSelectionPath,
  renderReadmeDrivenPage('🤔 技术选型理由', '本页根据 `README.md` 中“技术选型理由”章节自动同步。', techSelectionBody)
)
writeGeneratedMarkdown(
  pitfallsPath,
  renderReadmeDrivenPage('🚧 难点踩坑与解决方案', '本页根据 `README.md` 中“难点踩坑与解决方案”章节自动同步。', pitfallsBody)
)
writeGeneratedMarkdown(
  faqPath,
  renderFaqPage(faqBody)
)

syncStaticDirectory(diagramsSourceDir, diagramsPublicDir)
syncStaticDirectory(coreLinksDiagramsSourceDir, coreLinksDiagramsPublicDir)

console.log('project content synced')
