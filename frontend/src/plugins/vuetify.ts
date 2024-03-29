import Vue from 'vue'
import Vuetify from 'vuetify/lib/framework'

Vue.use(Vuetify)

export default new Vuetify({
  theme: {
    options: {
      customProperties: true
    },
    themes: {
      light: {
        primary: '#3f51b5',
        secondary: '#c51162',
        accent: '#1faa00',
        error: '#e53935',
        toolbarColor: '#193a9a',
        info: '#3f51b5',
        success: '#1faa00',
        snackbarSuccess: '#1faa00',
        warning: '#c51162',
        graphBackground: '#FFFFFF', // white
        graphFailedOrUnbenchmarked: '#696969', // dimgrey
        graphReferenceElements: '808080', // grey
        rowHighlight: '#d3d3d3',
        // only used in graphs. Should match normal text color
        graphTextColor: '#212121'
      },
      dark: {
        primary: '#7f9bff',
        secondary: '#c51162',
        accent: '#eb7b18',
        toolbarColor: '#4268c6',
        info: '#3f51b5',
        success: '#73bf69',
        snackbarSuccess: '#3db223',
        warning: '#ff9830',
        error: '#f2495c',
        graphBackground: '#2f3136',
        graphFailedOrUnbenchmarked: '#d3d3d3', // lightgray
        graphReferenceElements: '#d3d3d3', // lightgray
        rowHighlight: '#616161',
        // only used in graphs. Should match normal text color
        graphTextColor: '#e6ebf8'
      }
    }
  },
  icons: {
    iconfont: 'mdiSvg'
  }
})
