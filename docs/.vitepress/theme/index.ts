import DefaultTheme from 'vitepress/theme'
import { h } from 'vue'
import './style.css'
import DocOutlineToggle from './DocOutlineToggle.vue'

export default {
  extends: DefaultTheme,
  Layout: () =>
    h(DefaultTheme.Layout, null, {
      'aside-outline-before': () => h(DocOutlineToggle)
    })
}
