<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vitepress'

const route = useRoute()
const collapsed = ref(false)

watch(
  () => route.path,
  () => {
    collapsed.value = false
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
  </div>
</template>
