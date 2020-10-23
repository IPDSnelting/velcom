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
