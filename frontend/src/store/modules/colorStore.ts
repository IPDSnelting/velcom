import { createModule, mutation } from 'vuex-class-component'
import { hexToHsl, hslToHex } from '@/util/ColorUtils'
import { vxm } from '..'
import { DimensionId, RepoId } from '@/store/types'

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
  @mutation
  addColors(amount: number): void {
    vxm.colorModule.addColorToTheme({ amount, muted: true })
    vxm.colorModule.addColorToTheme({ amount, muted: false })
  }

  @mutation
  addColorToTheme(payload: { amount: number; muted: boolean }): void {
    // generating new colors in hsl color space using golden ratio to maximize difference
    const colors = payload.muted ? this.mutedColors : this.pastelColors

    const phi = 1.6180339887
    const saturation = 0.5
    const lightness = 0.5

    for (let i = 0; i < payload.amount; i++) {
      const lastColor = colors[colors.length - 1]
      let hue = hexToHsl(lastColor)[0]

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

  /**
   * Returns the color for a given repository.
   */
  get colorForRepo(): (repoId: RepoId) => string {
    return (repoId: RepoId) => {
      const index = vxm.repoModule.allReposSortedById.findIndex(
        value => value.id === repoId
      )
      return this.colorByIndex(index)
    }
  }

  /**
   * Returns the color in the detail graph for a given dimension.
   */
  get colorForDetailDimension(): (dimensionId: DimensionId) => string {
    return (dimensionId: DimensionId) => {
      const index = vxm.detailGraphModule.colorIndex(dimensionId)
      if (index === undefined) {
        return 'red'
      }
      return this.colorByIndex(index)
    }
  }

  /**
   * Converts a given store to a pure object that can be serialized.
   *
   * @param store the store to convert
   */
  static toPlainObject(store: ColorStore): unknown {
    return {
      mutedColors: store.mutedColors,
      pastelColors: store.pastelColors
    }
  }
}
