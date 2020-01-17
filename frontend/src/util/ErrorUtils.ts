import { AxiosResponse } from 'axios'

export interface AxiosError {
  response: AxiosResponse | undefined
  message: string | undefined
}

export function extractErrorMessage(error: AxiosError): string {
  if (error.response) {
    let errorMessage: null | string = null
    if (error.response.data.message) {
      errorMessage = error.response.data.message
    } else if (error.response.data.error) {
      errorMessage = error.response.data.error
    }
    return (
      `${errorMessage} (${error.response.status})` ||
      `Received error ${error.response.status}`
    )
  }
  return error.message || 'Unknown error'
}
