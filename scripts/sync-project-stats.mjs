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
const systemArchitecture = readTextWithBom(systemArchitecturePath)
let systemArchitectureText = systemArchitecture.text
systemArchitectureText = replaceBlock(
  systemArchitectureText,
  'SYSTEM_ARCH_STATS_CARDS',
  renderDocsHomeCards(data.docsHomeCards)
)
writeTextWithBom(systemArchitecturePath, systemArchitectureText, systemArchitecture.hasBom)

syncStaticDirectory(diagramsSourceDir, diagramsPublicDir)
syncStaticDirectory(coreLinksDiagramsSourceDir, coreLinksDiagramsPublicDir)

console.log('project content synced')
