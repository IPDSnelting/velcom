import axios from 'axios'

declare module 'axios' {
  export interface AxiosRequestConfig {
    /**
     * Whether to hide the whole request from all snackbar messages.
     *
     * @type {boolean}
     * @memberof AxiosRequestConfig
     */
    hideFromSnackbar?: boolean

    /**
     * Whether to hide the "please stand by" lading snackbar messages.
     *
     * @type {boolean}
     * @memberof AxiosRequestConfig
     */
    hideLoadingSnackbar?: boolean

    /**
     * Whether to hide the success snackbar messages.
     *
     * @type {boolean}
     * @memberof AxiosRequestConfig
     */
    hideSuccessSnackbar?: boolean

    /**
     * Whether to hide the error snackbar messages.
     *
     * @type {boolean}
     * @memberof AxiosRequestConfig
     */
    hideErrorFromSnackbar?: boolean

    /**
     * Whether to hide the loading *AND* success snackbar message.
     *
     * @type {boolean}
     * @memberof AxiosRequestConfig
     */
    snackbarMessage?: string
  }
}
