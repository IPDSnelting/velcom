import {
  ComparisonDataPoint,
  DetailDataPoint,
  Dimension,
  Repo,
  RepoBranch
} from '@/store/types'

const mappers: { [key: string]: any } = {
  Map: (entries: any[]) => new Map(entries),
  [DetailDataPoint.SERIALIZED_NAME]: (rawPoint: DetailDataPoint) =>
    new DetailDataPoint(
      rawPoint.repoId,
      rawPoint.hash,
      rawPoint.parentUids,
      rawPoint.author,
      rawPoint.committerTime,
      rawPoint.positionTime,
      rawPoint.summary,
      rawPoint.values
    ),
  [ComparisonDataPoint.SERIALIZED_NAME]: (rawPoint: ComparisonDataPoint) =>
    new ComparisonDataPoint(
      rawPoint.committerTime,
      rawPoint.positionTime,
      rawPoint.hash,
      rawPoint.repoId,
      rawPoint.values,
      rawPoint.parentUids,
      rawPoint.summary,
      rawPoint.author
    ),
  [Dimension.SERIALIZED_NAME]: (rawDimension: Dimension) =>
    new Dimension(
      rawDimension.benchmark,
      rawDimension.metric,
      rawDimension.unit,
      rawDimension.interpretation
    ),
  [RepoBranch.SERIALIZED_NAME]: (rawBranch: RepoBranch) =>
    new RepoBranch(rawBranch.name, rawBranch.tracked, rawBranch.lastCommit),
  [Repo.SERIALIZED_NAME]: (rawRepo: Repo) =>
    new Repo(
      rawRepo.id,
      rawRepo.name,
      rawRepo.branches,
      rawRepo.dimensions,
      rawRepo.remoteURL
    )
}

export function fromJson<T>(json: string): T {
  return JSON.parse(json, (key, value) => {
    // Ugly hack as Date provides a toJSON method which prevents us from
    // handling this in the toJson method - the replacer is never called for it.
    if (typeof value === 'string' && Number.isFinite(Date.parse(value))) {
      return new Date(value)
    }

    if (typeof value !== 'object' || value === null || value === undefined) {
      return value
    }

    if (Object.prototype.hasOwnProperty.call(value, '__datatype')) {
      if (!mappers[value.__datatype]) {
        throw new Error(`Could not find mapper for '${value.__datatype}'`)
      }
      return mappers[value.__datatype](value.val)
    }

    return value
  })
}

export function toJson(object: unknown): string {
  return JSON.stringify(object, (key, value) => {
    if (typeof value !== 'object') {
      return value
    }

    if (value === null || value === undefined) {
      return value
    }

    if (value instanceof Map) {
      return {
        __datatype: 'Map',
        val: Array.from(value.entries())
      }
    }

    if (Array.isArray(value)) {
      return value
    }

    const constructor = value.constructor
    if (constructor === Object) {
      return value
    }

    if (!Object.prototype.hasOwnProperty.call(constructor, 'SERIALIZED_NAME')) {
      throw new Error(
        'Constructor has no SERIALIZED_NAME property: ' + value.constructor.name
      )
    }

    return {
      __datatype: constructor.SERIALIZED_NAME,
      val: Object.assign({}, value)
    }
  })
}
