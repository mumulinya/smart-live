<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'

function handleMove(e: MouseEvent) {
  const card = (e.target as HTMLElement).closest(
    '.smartlive-stat-card, .smartlive-feature-card, .smartlive-link-card, .smartlive-path-card, .smartlive-highlight-card, .smartlive-hero-metric'
  ) as HTMLElement | null
  if (!card) return
  const rect = card.getBoundingClientRect()
  card.style.setProperty('--glow-x', `${e.clientX - rect.left}px`)
  card.style.setProperty('--glow-y', `${e.clientY - rect.top}px`)
}

function handleLeave(e: MouseEvent) {
  const card = (e.target as HTMLElement).closest(
    '.smartlive-stat-card, .smartlive-feature-card, .smartlive-link-card, .smartlive-path-card, .smartlive-highlight-card, .smartlive-hero-metric'
  ) as HTMLElement | null
  if (!card) return
  card.style.removeProperty('--glow-x')
  card.style.removeProperty('--glow-y')
}

onMounted(() => {
  if (typeof window === 'undefined') return
  document.addEventListener('mousemove', handleMove, { passive: true })
  document.addEventListener('mouseleave', handleLeave, true)
})

onUnmounted(() => {
  document.removeEventListener('mousemove', handleMove)
  document.removeEventListener('mouseleave', handleLeave)
})
</script>

<template>
  <div style="display:none" />
</template>
