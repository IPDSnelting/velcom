export class Repo {
  id: string
  name: string
  branches: Array<string>
  trackedBranches: Array<string>
  measurements: Array<MeasurementID>
  remoteURL: string

  constructor(
    id: string,
    name: string,
    branches: Array<string>,
    trackedBranches: Array<string>,
    measurements: Array<MeasurementID>,
    remoteURL: string
  ) {
    this.id = id
    this.name = name
    this.branches = branches.sort((a, b) =>
      a.localeCompare(b, undefined, { sensitivity: 'base' })
    )
    this.trackedBranches = trackedBranches.sort((a, b) =>
      a.localeCompare(b, undefined, { sensitivity: 'base' })
    )
    this.measurements = measurements
    this.remoteURL = remoteURL
  }
}

export class Measurement {
  id: MeasurementID
  successful: boolean
  unit: string | null
  interpretation: 'LESS_IS_BETTER' | 'MORE_IS_BETTER' | 'NEUTRAL' | null
  values: Array<number> | null
  value: number | null
  errorMessage: string | null

  constructor(
    id: MeasurementID,
    unit?: string,
    interpretation?: 'LESS_IS_BETTER' | 'MORE_IS_BETTER' | 'NEUTRAL',
    values?: Array<number>,
    value?: number,
    errorMessage?: string
  ) {
    this.id = id
    this.successful = !errorMessage
    this.unit = unit || null
    this.interpretation = interpretation || null
    this.values = values || null
    this.value = value || null
    this.errorMessage = errorMessage || null
  }
}

export class MeasurementID {
  benchmark: string
  metric: string

  constructor(benchmark: string, metric: string) {
    this.benchmark = benchmark
    this.metric = metric
  }

  equals(other: MeasurementID): boolean {
    return other.benchmark === this.benchmark && other.metric === this.metric
  }
}

export class Commit {
  repoID: string
  hash: string
  author: string
  authorDate: number | null
  committer: string | null
  committerDate: number | number
  message: string | null
  parents: string[]

  constructor(
    repoID: string,
    hash: string,
    author: string,
    authorDate: number,
    committer: string,
    committerDate: number,
    message: string,
    parents: Array<string>
  ) {
    this.repoID = repoID
    this.hash = hash
    this.author = author
    this.authorDate = authorDate
    this.committer = committer
    this.committerDate = committerDate
    this.message = message
    this.parents = parents
  }

  get summary(): string | null {
    if (!this.message) {
      return null
    }
    let firstNewline = this.message.indexOf('\n')
    if (firstNewline < 0) {
      return this.message
    }
    return this.message.substring(0, firstNewline).trim()
  }

  get bodyWithoutSummary(): string | null {
    if (!this.summary) {
      return null
    }
    let summaryLength = this.summary.length
    if (summaryLength === this.message!.length) {
      return ''
    }
    return this.message!.substring(summaryLength, this.message!.length)
  }
}

export class Run {
  startTime: number
  stopTime: number
  measurements: Measurement[] | null
  errorMessage: string | null

  constructor(
    startTime: number,
    stopTime: number,
    measurements?: Measurement[],
    errorMessage?: string
  ) {
    this.startTime = startTime
    this.stopTime = stopTime
    this.measurements = (measurements && measurements) || null
    this.errorMessage = (errorMessage && errorMessage) || null
  }
}

export class Difference {
  measurement: MeasurementID
  difference: number

  constructor(measurement: MeasurementID, difference: number) {
    this.measurement = measurement
    this.difference = difference
  }
}

export class CommitInfo {
  comparison: CommitComparison
  nextCommit: Commit | null

  constructor(comparison: CommitComparison, nextCommit: Commit | null) {
    this.comparison = comparison
    this.nextCommit = nextCommit
  }
}

export class CommitComparison {
  first: Run | null
  second: Run | null
  firstCommit: Commit | null
  secondCommit: Commit
  differences: Difference[]

  constructor(
    first: Run | null,
    second: Run | null,
    firstCommit: Commit | null,
    secondCommit: Commit,
    differences: Difference[]
  ) {
    this.first = first
    this.second = second
    this.firstCommit = firstCommit
    this.secondCommit = secondCommit
    this.differences = differences
  }
}

export class Datapoint {
  commit: Commit
  value: number

  constructor(commit: Commit, value: number) {
    this.commit = commit
    this.value = value
  }
}

export class Worker {
  name: string
  osData: string | null
  currentTask: Commit | null

  constructor(name: string, osData: string, currentTask: Commit | null) {
    this.name = name
    this.osData = osData
    this.currentTask = currentTask
  }
}

export enum measurementInterpretation {}
