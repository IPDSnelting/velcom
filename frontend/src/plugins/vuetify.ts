import Vue from 'vue'
import Vuetify from 'vuetify/lib'

Vue.use(Vuetify)

export default new Vuetify({
  theme: {
    themes: {
      light: {
        primary: '#8C9Eff',
        accent: '#78002e',
        error: '#ad1457'

      }
    }
  },
  icons: {
    iconfont: 'mdiSvg'
  }
})
