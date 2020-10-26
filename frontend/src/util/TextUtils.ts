import Convert from 'ansi-to-html'
import { vxm } from '@/store'

// base16-bright : http://chriskempson.com/projects/base16/
const darkThemeConvert = new Convert({
  colors: {
    0: '#000000',
    1: '#fb0120',
    2: '#a1c659',
    3: '#fda331',
    4: '#6fb3d2',
    5: '#d381c3',
    6: '#76c7b7',
    7: '#e0e0e0',
    8: '#b0b0b0',
    9: '#fb0120',
    10: '#a1c659',
    11: '#fda331',
    12: '#6fb3d2',
    13: '#d381c3',
    14: '#76c7b7',
    15: '#ffffff'
  }
})

// base16-one-light: http://chriskempson.com/projects/base16/
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
