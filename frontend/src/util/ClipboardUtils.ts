import { ISnackbar } from '@/util/Snackbar'

/**
 * Copies the given text to the clipboard.
 * @param text the text to copy
 * @param snackbar the snackbar to use for status reporting
 */
export function copyToClipboard(text: string, snackbar: ISnackbar): void {
  navigator.clipboard
    .writeText(text)
    .then(() => snackbar.setSuccess('', 'Copied!'))
    .catch(error =>
      snackbar.setError('', 'Could not copy to clipboard :( ' + error)
    )
}
