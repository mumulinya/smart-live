<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const visible = ref(false)
const imageSrc = ref('')
const imageAlt = ref('')

function closeLightbox() {
  visible.value = false
  imageSrc.value = ''
  imageAlt.value = ''
  document.body.classList.remove('smartlive-lightbox-open')
}

function openLightbox(src: string, alt: string) {
  imageSrc.value = src
  imageAlt.value = alt
  visible.value = true
  document.body.classList.add('smartlive-lightbox-open')
}

function handleClick(event: MouseEvent) {
  const target = event.target as HTMLElement | null
  const image = target?.closest(
    '.smartlive-gallery-sections img, .smartlive-showcase-sections img'
  ) as HTMLImageElement | null
  if (!image) return
  event.preventDefault()
  event.stopPropagation()
  openLightbox(image.currentSrc || image.src, image.alt || '页面截图预览')
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && visible.value) {
    closeLightbox()
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
      v-if="visible"
      class="smartlive-lightbox"
      role="dialog"
      aria-modal="true"
      :aria-label="imageAlt"
      @click.self="closeLightbox"
    >
      <button class="smartlive-lightbox__close" type="button" @click="closeLightbox">
        关闭
      </button>
      <div class="smartlive-lightbox__panel">
        <img :src="imageSrc" :alt="imageAlt" class="smartlive-lightbox__image" />
      </div>
    </div>
  </Teleport>
</template>
