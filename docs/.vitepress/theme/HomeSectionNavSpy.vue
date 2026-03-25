<script setup lang="ts">
import { useRoute } from 'vitepress'
import { nextTick, onBeforeUnmount, onMounted, watch } from 'vue'

type CleanupFn = () => void

const route = useRoute()
let cleanup: CleanupFn | null = null
let flashTimer: number | null = null

function clearActiveState(links: HTMLAnchorElement[]) {
  links.forEach((link) => {
    link.classList.remove('is-active')
    link.removeAttribute('aria-current')
  })
}

function getScrollOffset() {
  const nav = document.querySelector('.VPNav') as HTMLElement | null
  return (nav?.offsetHeight || 72) + 18
}

function flashTarget(target: HTMLElement) {
  if (flashTimer !== null) {
    window.clearTimeout(flashTimer)
    flashTimer = null
  }

  document
    .querySelectorAll<HTMLElement>('.smartlive-home-target-flash')
    .forEach((el) => el.classList.remove('smartlive-home-target-flash'))

  target.classList.add('smartlive-home-target-flash')
  flashTimer = window.setTimeout(() => {
    target.classList.remove('smartlive-home-target-flash')
    flashTimer = null
  }, 1100)
}

function setupHomeSectionSpy() {
  cleanup?.()
  cleanup = null

  const home = document.querySelector('.VPHome')
  const nav = document.querySelector('.smartlive-section-nav')
  if (!home || !nav) return

  const links = Array.from(
    nav.querySelectorAll<HTMLAnchorElement>('.smartlive-section-nav-pill[href^="#"]')
  )
  if (!links.length) return

  const sections = links
    .map((link) => {
      const href = link.getAttribute('href') || ''
      const id = href.replace(/^#/, '')
      const anchor = document.getElementById(id)
      const target = anchor?.closest('h2, h3, section, div') ?? anchor
      if (!id || !target) return null
      return { id, link, target: target as HTMLElement }
    })
    .filter((item): item is { id: string; link: HTMLAnchorElement; target: HTMLElement } => !!item)

  if (!sections.length) return

  let ticking = false
  const unbindLinkClicks: Array<() => void> = []

  const updateActive = () => {
    ticking = false

    const triggerY = window.scrollY + window.innerHeight * 0.28
    let active = sections[0]

    for (const section of sections) {
      const top = section.target.getBoundingClientRect().top + window.scrollY
      if (top <= triggerY) {
        active = section
      } else {
        break
      }
    }

    clearActiveState(links)
    active.link.classList.add('is-active')
    active.link.setAttribute('aria-current', 'true')
  }

  const onScroll = () => {
    if (ticking) return
    ticking = true
    window.requestAnimationFrame(updateActive)
  }

  const onResize = () => {
    onScroll()
  }

  sections.forEach((section) => {
    const onLinkClick = (event: MouseEvent) => {
      event.preventDefault()

      clearActiveState(links)
      section.link.classList.add('is-active')
      section.link.setAttribute('aria-current', 'true')

      const top = section.target.getBoundingClientRect().top + window.scrollY - getScrollOffset()
      const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

      window.history.replaceState(null, '', `#${section.id}`)
      window.scrollTo({
        top: Math.max(top, 0),
        behavior: prefersReducedMotion ? 'auto' : 'smooth'
      })

      window.setTimeout(() => {
        flashTarget(section.target)
      }, prefersReducedMotion ? 0 : 260)
    }

    section.link.addEventListener('click', onLinkClick)
    unbindLinkClicks.push(() => {
      section.link.removeEventListener('click', onLinkClick)
    })
  })

  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', onResize)

  clearActiveState(links)
  updateActive()

  cleanup = () => {
    window.removeEventListener('scroll', onScroll)
    window.removeEventListener('resize', onResize)
    unbindLinkClicks.forEach((unbind) => unbind())
    clearActiveState(links)
  }
}

async function refreshSpy() {
  await nextTick()
  window.requestAnimationFrame(() => {
    window.requestAnimationFrame(setupHomeSectionSpy)
  })
}

onMounted(() => {
  refreshSpy()
})

watch(
  () => route.path,
  () => {
    refreshSpy()
  }
)

onBeforeUnmount(() => {
  if (flashTimer !== null) {
    window.clearTimeout(flashTimer)
    flashTimer = null
  }
  cleanup?.()
})
</script>

<template></template>
