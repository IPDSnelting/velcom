import AnsiUp from 'ansi_up'

const converter = new AnsiUp()
converter.use_classes = true

/**
 * Escapes all HTML in a given string.
 *
 * @param input the input string to escape
 */
export function escapeHtml(input: string): string {
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

  return converter.ansi_to_html(safeInput)
}
