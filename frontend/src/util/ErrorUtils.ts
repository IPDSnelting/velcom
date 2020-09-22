import { AxiosResponse } from 'axios'

export interface AxiosError {
  response: AxiosResponse | undefined
  message: string | undefined
}

export function extractErrorMessage(error: AxiosError): string {
  if (error.response) {
    const errorMessage: undefined | string =
      error.response.data.message || error.response.data.error
    if (errorMessage) {
      return `${errorMessage} (${error.response.status})`
    } else if (error.response.status === 401 || error.response.status === 403) {
      return `Unauthorized! Please log in (${error.response.status})`
    }
    return `Received error ${error.response.status}`
  }
  return error.message || 'Unknown error'
}
