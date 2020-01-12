import { Module } from 'vuex'
import { RootState, ColorState } from '../../types'
import { mutations } from './mutations'
import { actions } from './actions'
import { getters } from './getters'

export const state: ColorState = {
  /* see the muted qualitative colour scheme on
  https://personal.sron.nl/~pault/#sec:qualitative
  */
  colors: [
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
}

const namespaced: boolean = true

export const colorModule: Module<ColorState, RootState> = {
  namespaced,
  state,
  mutations,
  actions,
  getters
}
