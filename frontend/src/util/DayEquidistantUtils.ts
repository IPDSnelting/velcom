import { GraphDataPoint } from '@/store/types'

const millisPerDay: number = 1000 * 60 * 60 * 24

// https://stackoverflow.com/questions/14446511/most-efficient-method-to-groupby-on-an-array-of-objects
export function groupBy<K, V>(
  list: V[],
  keyGetter: (value: V) => K
): Map<K, V[]> {
  const map = new Map()
  list.forEach(item => {
    const key = keyGetter(item)
    const collection = map.get(key)
    if (!collection) {
      map.set(key, [item])
    } else {
      collection.push(item)
    }
  })
  return map
}

export function spaceDayEquidistant<T extends GraphDataPoint>(
  detailGraph: T[]
): T[] {
  const dayGroups: Map<number, GraphDataPoint[]> = groupBy(
    detailGraph,
    key =>
      // round to day
      Math.floor(key.committerTime.getTime() / millisPerDay) * millisPerDay
  )

  const groupEntries = Array.from(dayGroups.entries())
  groupEntries.sort((a, b) => a[0] - b[0])

  return groupEntries.flatMap(([day, points]) => {
    const spacingBetweenElementsMillis = millisPerDay / points.length

    return points.map((point, index) => {
      return point.positionedAt(
        new Date(day + spacingBetweenElementsMillis * index)
      ) as T
    })
  })
}
