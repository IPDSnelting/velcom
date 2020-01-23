import Vue from 'vue'
import Vuetify from 'vuetify/lib'

Vue.use(Vuetify)

export default new Vuetify({
  theme: {
    themes: {
      light: {
        primary: '#7986cb',
        secondary: '#c51162', // '#8C9Eff',
        accent: '#1faa00',
        error: '#e53935',
        info: '#aab6fe',
        success: '#1faa00',
        warning: '#e53935',
        lightPrim: '#aab6fe',
        lightSec: '#fd558f',
        darkPrim: '#49599a',
        darkSec: '#8e0038'
      }
    }
  },
  icons: {
    iconfont: 'mdiSvg'
  }
})
