import Convert from 'ansi-to-html'

const convert = new Convert()

/**
 * Escapes all HTML in a given string.
 *
 * @param input the input string to escape
 */
function escapeHtml(input: string): string {
  const p = document.createElement('p')
  p.appendChild(document.createTextNode(input))
  return p.innerHTML
}

/**
 * Converts text with ANSI escape codes to HTML.
 *
 * @param input the input text
 */
export function safeConvertAnsi(input: string): string {
  const safeInput = escapeHtml(input)

  return convert.toHtml(safeInput)
}
