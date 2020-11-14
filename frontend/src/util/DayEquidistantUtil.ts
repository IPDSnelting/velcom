import { DetailDataPoint } from '@/store/types'

const millisPerDay: number = 1000 * 60 * 60 * 24

// https://stackoverflow.com/questions/14446511/most-efficient-method-to-groupby-on-an-array-of-objects
function groupBy<K, V>(list: K[], keyGetter: (key: K) => V) {
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

export function spaceDayEquidistant(
  detailGraph: DetailDataPoint[]
): DetailDataPoint[] {
  const dayGroups: Map<number, DetailDataPoint[]> = groupBy(
    detailGraph,
    key =>
      // round to day
      Math.floor(key.committerDate.getTime() / millisPerDay) * millisPerDay
  )

  return Array.from(dayGroups.entries()).flatMap(([day, points]) => {
    const spacingBetweenElementsMillis = millisPerDay / points.length

    return points.map((point, index) => {
      return new DetailDataPoint(
        point.hash,
        point.parents,
        point.author,
        point.committerDate,
        new Date(day + spacingBetweenElementsMillis * index),
        point.summary,
        point.values
      )
    })
  })
}
