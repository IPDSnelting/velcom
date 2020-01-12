import { ActionTree } from 'vuex'
import { ColorState, RootState, ColorConverter } from '../../types'
import store from '@/store'

export const actions: ActionTree<ColorState, RootState> = {
  addColor ({ state, commit }) {
    // generating new colors in hsl color space using golden ratio to maximize difference
    var converter = new ColorConverter()
    const colors = state.colors
    const lastColor = colors[colors.length - 1]

    const phi = 1.6180339887
    const saturation = 0.5
    const lightness = 0.5

    var hue = converter.hexToHsl(lastColor)[0]
    hue += phi
    hue %= 1
    const newColor = converter.hslToHex(hue, saturation, lightness)

    commit('ADD_COLOR', newColor)
  }
}
