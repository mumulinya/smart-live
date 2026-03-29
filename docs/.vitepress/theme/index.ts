import DefaultTheme from 'vitepress/theme'
import { Fragment, h } from 'vue'
import './style.css'
import DocOutlineToggle from './DocOutlineToggle.vue'
import ImageLightbox from './ImageLightbox.vue'
import HomeSectionNavSpy from './HomeSectionNavSpy.vue'
import ScrollReveal from './ScrollReveal.vue'
import CardGlow from './CardGlow.vue'
import HomeFooter from './HomeFooter.vue'

export default {
  extends: DefaultTheme,
  Layout: () =>
    h(Fragment, [
      h(DefaultTheme.Layout, null, {
        'aside-outline-before': () => h(DocOutlineToggle),
        'layout-bottom': () => h(HomeFooter)
      }),
      h(ImageLightbox),
      h(HomeSectionNavSpy),
      h(ScrollReveal),
      h(CardGlow)
    ])
}
