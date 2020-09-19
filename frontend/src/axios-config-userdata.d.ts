// Needed for our types to refer to the real ones
// eslint-disable-next-line
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
    showSuccessSnackbar?: boolean

    /**
     * Whether to hide the error snackbar messages.
     *
     * @type {boolean}
     * @memberof AxiosRequestConfig
     */
    hideErrorFromSnackbar?: boolean

    /**
     * The tag to append/prepend to messages for this request.
     *
     * @type {boolean}
     * @memberof AxiosRequestConfig
     */
    snackbarTag?: string

    /**
     * A number uniquely identifying this request.
     *
     * @type {number}
     * @memberof AxiosRequestConfig
     */
    randomTag?: number
  }
}
