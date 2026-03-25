<script setup lang="ts">
import { defineComponent, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useData, useRoute } from 'vitepress'

type HeaderItem = {
  title: string
  slug: string
  children?: HeaderItem[]
}

const route = useRoute()
const { page } = useData()

const collapsed = ref(false)
const collapsedParents = ref<Record<string, boolean>>({})
const activeHash = ref('')
const outlineItems = ref<HeaderItem[]>([])

let refreshTimer: number | null = null
let observer: MutationObserver | null = null

function stripHtml(text: string) {
  return text.replace(/<[^>]*>/g, '')
}

function cleanTitle(text: string) {
  return stripHtml(text).replace(/^\s*\d+(?:\.\d+)*[.\s、-]*/, '').trim()
}

function flatSlugs(items: HeaderItem[]) {
  const result: string[] = []

  const walk = (list: HeaderItem[]) => {
    list.forEach((item) => {
      result.push(item.slug)
      if (item.children?.length) {
        walk(item.children)
      }
    })
  }

  walk(items)
  return result
}

function getOutlineMaxLevel() {
  const outline = page.value.frontmatter?.outline
  if (typeof outline === 'number') return Math.max(2, Math.min(6, outline))
  if (Array.isArray(outline) && typeof outline[1] === 'number') return Math.max(2, Math.min(6, outline[1]))
  return 3
}

function getMainContentRoot() {
  return document.querySelector('.VPDoc .main .vp-doc') as HTMLElement | null
}

function getObserverRoot() {
  return document.querySelector('.VPDoc .main') as HTMLElement | null
}

function getHeadingTitle(heading: HTMLHeadingElement) {
  const clone = heading.cloneNode(true) as HTMLElement
  clone.querySelectorAll('.header-anchor').forEach((anchor) => anchor.remove())
  return cleanTitle(clone.textContent || '')
}

function isCurrentPageContent(root: HTMLElement | null) {
  if (!root) return false

  const pageTitle = cleanTitle(page.value.title || '')
  const h1 = root.querySelector('h1[id]') as HTMLHeadingElement | null
  const headingTitle = h1 ? getHeadingTitle(h1) : ''

  if (!pageTitle || !headingTitle) {
    return true
  }

  return headingTitle.includes(pageTitle) || pageTitle.includes(headingTitle)
}

function buildOutlineFromDocument() {
  const contentRoot = getMainContentRoot()
  if (!contentRoot || !isCurrentPageContent(contentRoot)) {
    outlineItems.value = []
    return false
  }

  const maxLevel = getOutlineMaxLevel()
  const selectors = Array.from({ length: Math.max(0, maxLevel - 1) }, (_, index) => `h${index + 2}[id]`).join(', ')
  const headings = Array.from(contentRoot.querySelectorAll(selectors)) as HTMLHeadingElement[]

  const roots: HeaderItem[] = []
  const stack: Array<{ level: number; node: HeaderItem }> = []

  headings.forEach((heading) => {
    const level = Number(heading.tagName.slice(1))
    const title = getHeadingTitle(heading)
    if (!title || !heading.id) return

    const node: HeaderItem = {
      title,
      slug: heading.id,
      children: []
    }

    while (stack.length && stack[stack.length - 1].level >= level) {
      stack.pop()
    }

    if (!stack.length) {
      roots.push(node)
    } else {
      stack[stack.length - 1].node.children = stack[stack.length - 1].node.children || []
      stack[stack.length - 1].node.children!.push(node)
    }

    stack.push({ level, node })
  })

  outlineItems.value = roots
  return roots.length > 0
}

function getHeadingElement(slug: string) {
  return document.getElementById(slug) || document.getElementById(decodeURIComponent(slug))
}

function updateActiveHash() {
  if (typeof window === 'undefined') return

  const routeHash = decodeURIComponent(window.location.hash.replace(/^#/, ''))
  const allSlugs = flatSlugs(outlineItems.value)

  if (routeHash && allSlugs.includes(routeHash)) {
    activeHash.value = routeHash
    return
  }

  let currentSlug = ''
  let bestTop = -Infinity

  allSlugs.forEach((slug) => {
    const heading = getHeadingElement(slug)
    if (!heading) return

    const top = heading.getBoundingClientRect().top
    if (top <= 150 && top > bestTop) {
      bestTop = top
      currentSlug = slug
    }
  })

  activeHash.value = currentSlug || allSlugs[0] || ''
}

function toggleParent(slug: string) {
  collapsedParents.value = {
    ...collapsedParents.value,
    [slug]: !collapsedParents.value[slug]
  }
}

function isItemActive(item: HeaderItem): boolean {
  if (activeHash.value === item.slug) return true
  return !!item.children?.some((child) => isItemActive(child))
}

function disconnectObserver() {
  observer?.disconnect()
  observer = null
}

function scheduleRefresh() {
  if (refreshTimer) {
    window.clearTimeout(refreshTimer)
  }

  refreshTimer = window.setTimeout(() => {
    nextTick(() => {
      requestAnimationFrame(() => {
        const hasOutline = buildOutlineFromDocument()
        updateActiveHash()

        if (hasOutline) {
          disconnectObserver()
        }
      })
    })
  }, 40)
}

function connectObserver() {
  disconnectObserver()

  const root = getObserverRoot()
  if (!root) return

  observer = new MutationObserver(() => {
    scheduleRefresh()
  })

  observer.observe(root, {
    childList: true,
    subtree: true
  })
}

const OutlineBranch = defineComponent({
  name: 'OutlineBranch',
  props: {
    items: {
      type: Array as () => HeaderItem[],
      required: true
    },
    prefix: {
      type: String,
      default: ''
    }
  },
  setup(props) {
    const renderItems = (items: HeaderItem[], parentPrefix = '') => {
      return items.map((item, index) => {
        const number = parentPrefix ? `${parentPrefix}.${index + 1}` : `${index + 1}`
        const hasChildren = !!item.children?.length
        const currentActive = isItemActive(item)
        const isCollapsed = !!collapsedParents.value[item.slug]

        const trigger = hasChildren
          ? h(
              'button',
              {
                type: 'button',
                class: ['outline-link', { active: currentActive }],
                onClick: () => toggleParent(item.slug)
              },
              [
                h('span', { class: 'smartlive-outline-prefix' }, number),
                h('span', { class: 'smartlive-outline-title' }, item.title)
              ]
            )
          : h(
              'a',
              {
                class: ['outline-link', { active: currentActive }],
                href: `#${item.slug}`
              },
              [
                h('span', { class: 'smartlive-outline-prefix' }, number),
                h('span', { class: 'smartlive-outline-title' }, item.title)
              ]
            )

        return h(
          'li',
          {
            class: {
              'has-children': hasChildren,
              'is-children-collapsed': hasChildren && isCollapsed
            }
          },
          [
            trigger,
            hasChildren && !isCollapsed
              ? h('ul', { class: 'VPDocOutlineItem nested' }, renderItems(item.children || [], number))
              : null
          ]
        )
      })
    }

    return () => h('ul', { class: props.prefix ? 'VPDocOutlineItem nested' : 'VPDocOutlineItem root' }, renderItems(props.items, props.prefix))
  }
})

function resetOutlineState() {
  collapsed.value = false
  collapsedParents.value = {}
  activeHash.value = ''
  outlineItems.value = []
}

function refreshForRoute() {
  resetOutlineState()
  connectObserver()
  scheduleRefresh()

  window.setTimeout(() => {
    scheduleRefresh()
  }, 180)
}

onMounted(() => {
  refreshForRoute()
  window.addEventListener('scroll', updateActiveHash, { passive: true })
  window.addEventListener('hashchange', updateActiveHash)
})

onBeforeUnmount(() => {
  if (refreshTimer) {
    window.clearTimeout(refreshTimer)
  }
  disconnectObserver()
  window.removeEventListener('scroll', updateActiveHash)
  window.removeEventListener('hashchange', updateActiveHash)
})

watch(
  () => route.path,
  () => {
    refreshForRoute()
  }
)
</script>

<template>
  <div
    class="smartlive-doc-outline-toggle"
    :class="{ 'is-collapsed': collapsed }"
  >
    <button
      type="button"
      class="smartlive-doc-outline-button"
      @click="collapsed = !collapsed"
      :aria-expanded="(!collapsed).toString()"
      aria-label="切换本页大纲"
    >
      <span class="smartlive-doc-outline-button__title">本页大纲</span>
      <span class="smartlive-doc-outline-button__state">
        {{ collapsed ? '展开' : '收起' }}
      </span>
    </button>

    <nav
      v-if="outlineItems.length && !collapsed"
      class="VPDocAsideOutline has-outline smartlive-custom-outline"
      aria-label="本页大纲"
    >
      <div class="content">
        <OutlineBranch :items="outlineItems" />
      </div>
    </nav>
  </div>
</template>
