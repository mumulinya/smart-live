import DefaultTheme from 'vitepress/theme'
import { Fragment, h } from 'vue'
import './style.css'
import DocOutlineToggle from './DocOutlineToggle.vue'
import ImageLightbox from './ImageLightbox.vue'
import HomeSectionNavSpy from './HomeSectionNavSpy.vue'

export default {
  extends: DefaultTheme,
  Layout: () =>
    h(Fragment, [
    h(DefaultTheme.Layout, null, {
      'aside-outline-before': () => h(DocOutlineToggle)
    }),
      h(ImageLightbox),
      h(HomeSectionNavSpy)
    ])
}
