<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'

let observer: IntersectionObserver | null = null

onMounted(() => {
  if (typeof window === 'undefined') return

  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible')
          observer?.unobserve(entry.target)
        }
      })
    },
    { threshold: 0.08, rootMargin: '0px 0px -40px 0px' }
  )

  const selectors = [
    '.smartlive-stat-card',
    '.smartlive-feature-card',
    '.smartlive-link-card',
    '.smartlive-path-card',
    '.smartlive-highlight-card',
    '.smartlive-figure-frame',
    '.smartlive-hero-metric',
    '.VPHome h2',
    '.VPDoc h2',
    '.VPDoc h3'
  ]

  document.querySelectorAll(selectors.join(',')).forEach((el, i) => {
    el.classList.add('scroll-reveal')
    ;(el as HTMLElement).style.setProperty('--reveal-i', String(i % 8))
    observer?.observe(el)
  })
})

onUnmounted(() => {
  observer?.disconnect()
})
</script>

<template>
  <div style="display:none" />
</template>
