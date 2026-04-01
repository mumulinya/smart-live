<template>
  <div class="smartlive-site-counter" aria-label="站点访问量">
    <span id="busuanzi_container_site_pv" class="smartlive-site-counter__content">
      访问量：
      <strong id="busuanzi_value_site_pv" class="smartlive-site-counter__value">--</strong>
    </span>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, watch } from 'vue'
import { useRoute } from 'vitepress'

type BusuanziPayload = {
  site_pv: string
}

type BusuanziWindow = Window & {
  bszCaller?: {
    fetch: (url: string, callback: (payload: BusuanziPayload) => void) => void
  }
}

const BUSUANZI_SCRIPT_ID = 'smartlive-busuanzi-script'
const BUSUANZI_SCRIPT_SRC = 'https://busuanzi.ibruce.info/busuanzi/2.3/busuanzi.pure.mini.js'
const BUSUANZI_API = '//busuanzi.ibruce.info/busuanzi?jsonpCallback=BusuanziCallback'

const route = useRoute()
let refreshTimer: ReturnType<typeof setTimeout> | undefined

function getBusuanziWindow() {
  return window as BusuanziWindow
}

function setSitePv(value: string) {
  const valueNode = document.getElementById('busuanzi_value_site_pv')

  if (valueNode) {
    valueNode.textContent = value
  }
}

function updateBusuanzi() {
  const busuanziWindow = getBusuanziWindow()

  if (!busuanziWindow.bszCaller) {
    return
  }

  busuanziWindow.bszCaller.fetch(BUSUANZI_API, (payload: BusuanziPayload) => {
    setSitePv(payload.site_pv ?? '--')
  })
}

function ensureBusuanziScript() {
  const existingScript = document.getElementById(BUSUANZI_SCRIPT_ID)

  if (existingScript) {
    updateBusuanzi()
    return
  }

  const script = document.createElement('script')
  script.id = BUSUANZI_SCRIPT_ID
  script.async = true
  script.src = BUSUANZI_SCRIPT_SRC
  script.referrerPolicy = 'no-referrer-when-downgrade'
  script.onload = () => updateBusuanzi()

  document.head.appendChild(script)
}

function refreshMetrics() {
  setSitePv('--')

  if (refreshTimer) {
    clearTimeout(refreshTimer)
  }

  refreshTimer = setTimeout(() => {
    ensureBusuanziScript()
  }, 80)
}

onMounted(() => {
  refreshMetrics()
})

watch(
  () => route.path,
  async () => {
    await nextTick()
    refreshMetrics()
  }
)

onBeforeUnmount(() => {
  if (refreshTimer) {
    clearTimeout(refreshTimer)
  }
})
</script>

<style scoped>
.smartlive-site-counter {
  padding: 10px 20px 20px;
  text-align: center;
}

.smartlive-site-counter__content {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.smartlive-site-counter__value {
  color: #334155;
  font-weight: 700;
}

.dark .smartlive-site-counter__content {
  color: #94a3b8;
}

.dark .smartlive-site-counter__value {
  color: #e2e8f0;
}

@media (max-width: 640px) {
  .smartlive-site-counter {
    padding: 8px 16px 16px;
  }
}
</style>
