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
    this.branches = branches
    this.trackedBranches = trackedBranches
    this.measurements = measurements
    this.remoteURL = remoteURL
  }
}

export class Measurement {
  id: MeasurementID
  successful: boolean
  unit: string | null
  interpretation: string | null
  values: Array<number> | null
  value: number | null
  errorMessage: string | null

  constructor(
    id: MeasurementID,
    unit?: string,
    interpretation?: string,
    values?: Array<number>,
    value?: number,
    errorMessage?: string
  ) {
    this.id = id
    this.successful = (errorMessage && false) || true
    this.unit = (unit && unit) || null
    this.interpretation = (interpretation && interpretation) || null
    this.values = (values && values) || null
    this.value = (value && value) || null
    this.errorMessage = (errorMessage && errorMessage) || null
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
}

export class Run {
  commit: Commit
  startTime: number
  stopTime: number
  measurements: Array<Measurement> | null
  errorMessage: string | null

  constructor(
    commit: Commit,
    startTime: number,
    stopTime: number,
    measurements?: Array<Measurement>,
    errorMessage?: string
  ) {
    this.commit = commit
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

export class CommitComparison {
  first: Run | null
  second: Run | null
  differences: Difference[]

  constructor(
    first: Run | null,
    second: Run | null,
    differences: Array<Difference>
  ) {
    this.first = first
    this.second = second
    this.differences = differences
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
