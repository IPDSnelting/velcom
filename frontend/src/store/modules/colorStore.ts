import { createModule, mutation, action } from 'vuex-class-component'
import { hexToHsl, hslToHex } from '@/util/ColorUtil'
import { vxm } from '..'

const VxModule = createModule({
  namespaced: 'colorModule',
  strict: false
})

export class ColorStore extends VxModule {
  /* see the muted qualitative colour scheme on
   * https://personal.sron.nl/~pault/#sec:qualitative
   */
  private mutedColors: string[] = [
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

  /* see the light qualitative colour scheme on
   * https://personal.sron.nl/~pault/#sec:qualitative
   */
  private pastelColors: string[] = [
    '#77AADD',
    '#99DDFF',
    '#44BB99',
    '#BBCC33',
    '#AAAA00',
    '#EEDD88',
    '#EE8866',
    '#FFAABB'
  ]

  /**
   * Generates a new hex colors whose hue is the hue of the last color added to this store,
   * translated by the golden ratio in hsl color space
   *
   * @param {number} amount the number of colors to generate
   * @memberof ColorModuleStore
   */
  @action
  async addColors(amount: number) {
    this.addColorToTheme({ amount: amount, muted: true })
    this.addColorToTheme({ amount: amount, muted: false })
  }

  @mutation
  addColorToTheme(payload: { amount: number; muted: boolean }) {
    // generating new colors in hsl color space using golden ratio to maximize difference
    const colors = payload.muted ? this.mutedColors : this.pastelColors

    const phi = 1.6180339887
    const saturation = 0.5
    const lightness = 0.5

    for (let i = 0; i < payload.amount; i++) {
      const lastColor = colors[colors.length - 1]
      var hue = hexToHsl(lastColor)[0]

      hue += phi
      hue %= 1
      const newColor = hslToHex(hue, saturation, lightness)
      if (payload.muted) {
        this.mutedColors.push(newColor)
      } else {
        this.pastelColors.push(newColor)
      }
    }
  }

  /**
   * Returns all colors.
   *
   * @readonly
   * @type {string[]}
   * @memberof ColorModuleStore
   */
  get allColors(): string[] {
    return vxm.userModule.darkThemeSelected
      ? this.pastelColors
      : this.mutedColors
  }

  /**
   * Returns a color by its index.
   *
   * @readonly
   * @memberof ColorModuleStore
   */
  get colorByIndex(): (index: number) => string {
    return (index: number) => {
      if (index > this.allColors.length) {
        this.addColors(index - this.allColors.length + 1)
      }
      return vxm.userModule.darkThemeSelected
        ? this.pastelColors[index]
        : this.mutedColors[index]
    }
  }
}
