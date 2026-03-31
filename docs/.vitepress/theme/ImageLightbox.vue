<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

type LightboxImage = {
  src: string
  alt: string
}

const visible = ref(false)
const images = ref<LightboxImage[]>([])
const currentIndex = ref(0)
const groupLabel = ref('')

const currentImage = computed(() => images.value[currentIndex.value] ?? null)
const canNavigate = computed(() => images.value.length > 1)
const imageCounter = computed(() => `${currentIndex.value + 1} / ${images.value.length}`)

function closeLightbox() {
  visible.value = false
  images.value = []
  currentIndex.value = 0
  groupLabel.value = ''
  document.body.classList.remove('smartlive-lightbox-open')
}

function prevImage() {
  if (!images.value.length) return
  currentIndex.value = (currentIndex.value - 1 + images.value.length) % images.value.length
}

function nextImage() {
  if (!images.value.length) return
  currentIndex.value = (currentIndex.value + 1) % images.value.length
}

function isEligibleImage(image: HTMLImageElement) {
  if (!image.src) return false
  if (image.closest('.smartlive-lightbox')) return false
  if (image.closest('.VPNav, .VPNavBar, .VPSidebar, .VPDocAside, .VPLocalNav')) return false
  if (!image.closest('.VPDoc, .VPHome')) return false
  return true
}

function getRootContainer(image: HTMLImageElement) {
  return (
    image.closest('.vp-doc, .VPDoc, .VPHome .main, .VPHome') as HTMLElement | null
  ) ?? document.body
}

function getSectionHeading(image: HTMLImageElement, root: HTMLElement) {
  const headings = Array.from(
    root.querySelectorAll('h1, h2, h3, h4, h5, h6')
  ) as HTMLElement[]

  let lastHeading: HTMLElement | null = null
  for (const heading of headings) {
    const relation = heading.compareDocumentPosition(image)
    if (relation & Node.DOCUMENT_POSITION_FOLLOWING || heading.contains(image)) {
      lastHeading = heading
      continue
    }
    break
  }

  return lastHeading
}

function getSectionKey(image: HTMLImageElement, root: HTMLElement) {
  const heading = getSectionHeading(image, root)
  if (!heading) {
    return { key: 'root', label: '当前页面图片组' }
  }

  const label = heading.textContent?.trim() || '当前小节图片组'
  const key = heading.id || label
  return { key, label }
}

function collectGroupImages(clickedImage: HTMLImageElement) {
  const root = getRootContainer(clickedImage)
  const currentSection = getSectionKey(clickedImage, root)
  const allImages = Array.from(root.querySelectorAll('img')).filter((item) =>
    isEligibleImage(item as HTMLImageElement)
  ) as HTMLImageElement[]

  const grouped = allImages.filter((item) => {
    const section = getSectionKey(item, root)
    return section.key === currentSection.key
  })

  return {
    label: currentSection.label,
    items: grouped.map((item) => ({
      src: item.currentSrc || item.src,
      alt: item.alt || currentSection.label || '页面截图预览'
    }))
  }
}

function openLightbox(clickedImage: HTMLImageElement) {
  const group = collectGroupImages(clickedImage)
  const clickedSrc = clickedImage.currentSrc || clickedImage.src
  const clickedIdx = group.items.findIndex((item) => item.src === clickedSrc)

  images.value = group.items
  currentIndex.value = clickedIdx >= 0 ? clickedIdx : 0
  groupLabel.value = group.label
  visible.value = true
  document.body.classList.add('smartlive-lightbox-open')
}

function handleClick(event: MouseEvent) {
  const target = event.target as HTMLElement | null
  const image = target?.closest('img') as HTMLImageElement | null
  if (!image || !isEligibleImage(image)) return
  event.preventDefault()
  event.stopPropagation()
  openLightbox(image)
}

function handleKeydown(event: KeyboardEvent) {
  if (!visible.value) return

  if (event.key === 'Escape') {
    closeLightbox()
    return
  }

  if (event.key === 'ArrowLeft') {
    event.preventDefault()
    prevImage()
  }

  if (event.key === 'ArrowRight') {
    event.preventDefault()
    nextImage()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClick)
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClick)
  document.removeEventListener('keydown', handleKeydown)
  document.body.classList.remove('smartlive-lightbox-open')
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible && currentImage"
      class="smartlive-lightbox"
      role="dialog"
      aria-modal="true"
      :aria-label="currentImage.alt"
      @click.self="closeLightbox"
    >
      <button class="smartlive-lightbox__close" type="button" @click="closeLightbox">
        关闭
      </button>

      <div class="smartlive-lightbox__panel">
        <div class="smartlive-lightbox__toolbar">
          <div class="smartlive-lightbox__meta">
            <strong class="smartlive-lightbox__group">{{ groupLabel }}</strong>
            <span class="smartlive-lightbox__count">{{ imageCounter }}</span>
          </div>
          <div v-if="canNavigate" class="smartlive-lightbox__controls">
            <button type="button" class="smartlive-lightbox__nav" @click.stop="prevImage">
              上一张
            </button>
            <button type="button" class="smartlive-lightbox__nav" @click.stop="nextImage">
              下一张
            </button>
          </div>
        </div>

        <div class="smartlive-lightbox__stage">
          <button
            v-if="canNavigate"
            type="button"
            class="smartlive-lightbox__side smartlive-lightbox__side--prev"
            aria-label="上一张"
            @click.stop="prevImage"
          >
            ‹
          </button>

          <img :src="currentImage.src" :alt="currentImage.alt" class="smartlive-lightbox__image" />

          <button
            v-if="canNavigate"
            type="button"
            class="smartlive-lightbox__side smartlive-lightbox__side--next"
            aria-label="下一张"
            @click.stop="nextImage"
          >
            ›
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
