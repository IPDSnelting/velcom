/**
 * converts a hex color into an hsl one
 *
 * @export
 * @param {string} hex the color in hex form
 * @returns tripel of hue, saturation and value as numbers
 */
export function hexToHsl(hex: string): [number, number, number] {
  // convert hex to rgb
  let r: number = parseInt(hex.substr(1, 2), 16)
  let g: number = parseInt(hex.substr(3, 2), 16)
  let b: number = parseInt(hex.substr(5, 2), 16)

  // convert rgb to hsl
  r /= 255
  g /= 255
  b /= 255

  const max: number = Math.max(r, g, b)
  const min: number = Math.min(r, g, b)

  let h: number = 0
  let s: number
  const l: number = (max + min) / 2

  if (max === min) {
    h = s = 0 // achromatic
  } else {
    const diff: number = max - min
    s = l > 0.5 ? diff / (2 - max - min) : diff / (max + min)

    switch (max) {
      case r:
        h = (g - b) / diff + (g < b ? 6 : 0)
        break
      case g:
        h = (b - r) / diff + 2
        break
      case b:
        h = (r - g) / diff + 4
        break
    }
    h /= 6
  }

  return [h, s, l]
}

/**
 * converts a color in hsl form into a hex color
 *
 * @export
 * @param {number} h the hue
 * @param {number} s the satuation
 * @param {number} l the value
 * @returns {string} the given color in hex form
 */
export function hslToHex(h: number, s: number, l: number): string {
  let r: number
  let g: number
  let b: number

  if (s === 0) {
    r = g = b = l // achromatic
  } else {
    const hueToRgb = (p: number, q: number, t: number) => {
      if (t < 0) t += 1
      if (t > 1) t -= 1
      if (t < 1 / 6) return p + (q - p) * 6 * t
      if (t < 1 / 2) return q
      if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6
      return p
    }
    const q: number = l < 0.5 ? l * (1 + s) : l + s - l * s
    const p: number = 2 * l - q
    r = hueToRgb(p, q, h + 1 / 3)
    g = hueToRgb(p, q, h)
    b = hueToRgb(p, q, h - 1 / 3)
  }
  const toHex = (num: number) => {
    const hex: string = Math.round(num * 255).toString(16)
    return hex.length === 1 ? '0' + hex : hex
  }
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`
}
