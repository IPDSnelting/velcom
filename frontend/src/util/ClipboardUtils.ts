import { ISnackbar } from '@/util/Snackbar'

/**
 * Copies the given text to the clipboard.
 * @param text the text to copy
 * @param snackbar the snackbar to use for status reporting
 */
export function copyToClipboard(text: string, snackbar: ISnackbar): void {
  if (navigator.clipboard) {
    navigator.clipboard
      .writeText(text)
      .then(() => snackbar.setSuccess('', 'Copied!'))
      .catch(error =>
        snackbar.setError('', 'Could not copy to clipboard :( ' + error)
      )

    return
  }

  // Compatibility for sites without a secure context
  // (or users that disable navigator.clipboard)

  // setup new node
  const node = document.createElement('span')
  node.innerText = text
  node.contentEditable = 'true'
  document.body.appendChild(node)

  node.focus({ preventScroll: true })

  // Copy its text
  document.execCommand('SelectAll')
  document.execCommand('copy')

  // delete it
  document.body.removeChild(node)

  snackbar.setSuccess('', 'Copied (using compatibility mode)!')
}
