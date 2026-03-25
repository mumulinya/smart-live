<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vitepress'

const route = useRoute()
const collapsed = ref(false)

function getRawText(element: HTMLElement) {
  const current = element.dataset.smartliveRawTitle
  if (current) return current
  const text = (element.textContent || '').trim()
  element.dataset.smartliveRawTitle = text
  return text
}

function getCleanText(element: HTMLElement) {
  const current = element.dataset.smartliveCleanTitle
  if (current) return current
  const text = getRawText(element).replace(/^\d+(?:\.\d+)*\s*/, '')
  element.dataset.smartliveCleanTitle = text
  return text
}

function getNumberPrefix(text: string, fallback: string) {
  const match = text.trim().match(/^(\d+(?:\.\d+)*)(?:[.\s]|$)/)
  return match?.[1] || fallback
}

function applyOutlineNumbers() {
  const rootItems = document.querySelectorAll('.VPDocAsideOutline .VPDocOutlineItem.root > li')

  const decorate = (items: NodeListOf<Element>, parentNumber?: string) => {
    items.forEach((item, index) => {
      const element = item as HTMLElement
      const link = element.querySelector(':scope > .outline-link') as HTMLElement | null
      if (!link) return

      const rawText = getRawText(link)
      const cleanText = getCleanText(link)
      const currentNumber = parentNumber ? `${parentNumber}.${index + 1}` : getNumberPrefix(rawText, `${index + 1}`)
      const hasOwnNumber = /^\d+(?:\.\d+)*\s*/.test(rawText)

      if (parentNumber) {
        link.textContent = `${currentNumber} ${cleanText}`
      } else {
        link.textContent = hasOwnNumber ? rawText : `${currentNumber} ${cleanText}`
      }

      const nestedItems = element.querySelectorAll(':scope > .VPDocOutlineItem.nested > li')
      if (nestedItems.length) {
        decorate(nestedItems, currentNumber)
      }
    })
  }

  decorate(rootItems)
}

function markOutlineParents() {
  document
    .querySelectorAll('.VPDocAsideOutline .VPDocOutlineItem li')
    .forEach((item) => {
      const element = item as HTMLElement
      const nested = element.querySelector(':scope > .VPDocOutlineItem.nested')
      if (nested) {
        element.classList.add('has-children')
      } else {
        element.classList.remove('has-children')
        element.classList.remove('is-children-collapsed')
      }
    })
}

function handleOutlineClick(event: MouseEvent) {
  const target = event.target as HTMLElement | null
  const link = target?.closest(
    '.VPDocAsideOutline .VPDocOutlineItem li.has-children > .outline-link'
  ) as HTMLAnchorElement | null

  if (!link) return

  const item = link.parentElement as HTMLElement | null
  item?.classList.toggle('is-children-collapsed')
}

watch(
  () => route.path,
  () => {
    collapsed.value = false
    nextTick(() => {
      markOutlineParents()
      applyOutlineNumbers()
    })
  }
)

onMounted(() => {
  markOutlineParents()
  applyOutlineNumbers()
  document.addEventListener('click', handleOutlineClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleOutlineClick)
})
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
  </div>
</template>
