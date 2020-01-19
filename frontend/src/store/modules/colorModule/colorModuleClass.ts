import { createModule, mutation, action } from 'vuex-class-component'
import { ColorConverter } from '@/store/types'

const VxModule = createModule({
  namespaced: 'colorModule',
  strict: false
})

export class ColorStore extends VxModule {
  /* see the muted qualitative colour scheme on
   * https://personal.sron.nl/~pault/#sec:qualitative
   */
  private colors: string[] = [
    '#332288',
    '#88CCEE',
    '#44AA99',
    '#117733',
    '#999933',
    '#DDCC77',
    '#CC6677',
    '#882255',
    '#AA4499'
  ]

  /**
   * Generates a new hex colors whose hue is the hue of the last color added to this store,
   * translated by the golden ratio in hsl color space
   *
   * @param {number} amount the number of colors to generate
   * @memberof ColorModuleStore
   */
  @mutation
  addColors(amount: number) {
    // generating new colors in hsl color space using golden ratio to maximize difference
    var converter = new ColorConverter()
    const colors = this.colors

    const phi = 1.6180339887
    const saturation = 0.5
    const lightness = 0.5

    for (let i = 0; i < amount; i++) {
      const lastColor = colors[colors.length - 1]
      var hue = converter.hexToHsl(lastColor)[0]

      hue += phi
      hue %= 1
      const newColor = converter.hslToHex(hue, saturation, lightness)

      this.colors.push(newColor)
    }
  }

  /**
   * Adds a new color.
   *
   * @param {string} payload the color to add
   * @memberof ColorModuleStore
   */
  @mutation
  addColor(payload: string) {
    this.colors.push(payload)
  }

  /**
   * Returns all colors.
   *
   * @readonly
   * @type {string[]}
   * @memberof ColorModuleStore
   */
  get allColors(): string[] {
    return this.colors
  }

  /**
   * Returns a color by its index.
   *
   * @readonly
   * @memberof ColorModuleStore
   */
  get colorByIndex(): (index: number) => string {
    return (index: number) => this.colors[index]
  }
}
