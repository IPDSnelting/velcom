import Vue from 'vue'
import EchartsDetailGraph from '@/components/graphs/graph/EchartsDetailGraph.vue'
import DytailGraph from '@/components/graphs/graph/DytailGraph.vue'

export type GraphVariant = {
  component: typeof Vue
  name: string
}

type GraphVariantPredicate = GraphVariant & {
  predicate: (visiblePoints: number) => boolean
}

export const availableGraphComponents: GraphVariantPredicate[] = [
  {
    predicate: (visiblePoints: number): boolean => {
      // Do not care about zooming, only use echarts when he have only a handful of data points
      return visiblePoints < 30_000
    },
    component: EchartsDetailGraph,
    name: 'Fancy'
  },
  {
    predicate: (): boolean => {
      // matches from first to last. this one is the fallback
      return true
    },
    component: DytailGraph,
    name: 'Fast'
  }
]

/**
 * Selets a fitting graph variant.
 *
 * @param visiblePoints the amount of visible points
 */
export function selectGraphVariant(
  visiblePoints: number
): GraphVariant | undefined {
  return availableGraphComponents.find(it => it.predicate(visiblePoints))
}
