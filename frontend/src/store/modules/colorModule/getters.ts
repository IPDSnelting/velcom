import { GetterTree } from 'vuex'
import { ColorState, RootState } from '../../types'
import { commitComparisonModule } from '../commitComparisonModule'
import { colorModule } from '.'

export const getters: GetterTree<ColorState, RootState> = {
  allColors: state => {
    return state.colors
  },

  colorByIndex: state => (index: number) => {
    return state.colors[index]
  }
}
