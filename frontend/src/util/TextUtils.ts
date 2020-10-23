import Convert from 'ansi-to-html'
import { vxm } from '@/store'

const darkThemeConvert = new Convert({
  colors: {
    0: '#1B2B34',
    1: '#EC5f67',
    2: '#99C794',
    3: '#FAC863',
    4: '#6699CC',
    5: '#C594C5',
    6: '#5FB3B3',
    7: '#C0C5CE',
    8: '#65737E',
    9: '#EC5f67',
    10: '#99C794',
    11: '#FAC863',
    12: '#6699CC',
    13: '#C594C5',
    14: '#5FB3B3',
    15: '#D8DEE9'
  }
})

const lightThemeConvert = new Convert({
  colors: {
    0: '#fafafa',
    1: '#ca1243',
    2: '#50a14f',
    3: '#c18401',
    4: '#4078f2',
    5: '#a626a4',
    6: '#0184bc',
    7: '#383a42',
    8: '#a0a1a7',
    9: '#ca1243',
    10: '#50a14f',
    11: '#c18401',
    12: '#4078f2',
    13: '#a626a4',
    14: '#0184bc',
    15: '#090a0b'
  }
})

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

  return vxm.userModule.darkThemeSelected
    ? darkThemeConvert.toHtml(safeInput)
    : lightThemeConvert.toHtml(safeInput)
}
