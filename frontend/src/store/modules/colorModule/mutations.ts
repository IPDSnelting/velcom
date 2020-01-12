import { MutationTree } from 'vuex'
import { ColorState } from '../../types'

export const mutations: MutationTree<ColorState> = {
  ADD_COLOR: (state: ColorState, payload: string) => {
    state.colors.push(payload)
  }
}
