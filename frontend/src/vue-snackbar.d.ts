import { ISnackbar } from './util/Snackbar'

declare module 'vue/types/vue' {
  interface Vue {
    $globalSnackbar: ISnackbar
  }
}
