export class RepoBranch {
  readonly name: string
  readonly tracked: boolean

  constructor(name: string, tracked: boolean) {
    this.name = name
    this.tracked = tracked
  }
}
export class Repo {
  readonly id: string
  name: string
  branches: RepoBranch[]
  dimensions: Dimension[]
  remoteURL: string
  hasToken: boolean

  constructor(
    id: string,
    name: string,
    branches: RepoBranch[],
    dimensions: Dimension[],
    remoteURL: string,
    hasToken: boolean
  ) {
    this.id = id
    this.name = name
    this.dimensions = dimensions
    this.remoteURL = remoteURL
    this.branches = branches.sort((a, b) =>
      a.name.localeCompare(b.name, undefined, { sensitivity: 'base' })
    )
    this.hasToken = hasToken
  }
}

export class Dimension {
  readonly benchmark: string
  readonly metric: string
  readonly unit: string
  readonly interpretation: 'LESS_IS_BETTER' | 'MORE_IS_BETTER' | 'NEUTRAL'

  constructor(
    benchmark: string,
    metric: string,
    unit: string,
    interpretation: 'LESS_IS_BETTER' | 'MORE_IS_BETTER' | 'NEUTRAL'
  ) {
    this.benchmark = benchmark
    this.metric = metric
    this.unit = unit
    this.interpretation = interpretation
  }
}

export class Measurement {
  id: MeasurementID
  successful: boolean
  unit: string | null
  interpretation: 'LESS_IS_BETTER' | 'MORE_IS_BETTER' | 'NEUTRAL' | null
  values: number[] | null
  value: number | null
  errorMessage: string | null

  constructor(
    id: MeasurementID,
    unit?: string,
    interpretation?: 'LESS_IS_BETTER' | 'MORE_IS_BETTER' | 'NEUTRAL',
    values?: number[],
    value?: number,
    errorMessage?: string
  ) {
    this.id = id
    this.successful = !errorMessage
    this.unit = unit === undefined ? null : unit
    this.interpretation = interpretation === undefined ? null : interpretation
    this.values = values === undefined ? null : values
    this.value = value === undefined ? null : value
    this.errorMessage = errorMessage === undefined ? null : errorMessage
  }

  /**
   * Returns a real measurement (prototypes set and all) from a normal JS
   * object that happens to have all necessary properties.
   *
   * @static
   * @param {Measurement} measurement the object to deconstruct it from
   * @returns a real measurement
   * @memberof Measurement
   */
  static fromRawObject(measurement: Measurement): Measurement {
    return new Measurement(
      MeasurementID.fromRawObject(measurement.id),
      measurement.unit === null ? undefined : measurement.unit,
      measurement.interpretation === null
        ? undefined
        : measurement.interpretation,
      measurement.values === null ? undefined : measurement.values,
      measurement.value === null ? undefined : measurement.value,
      measurement.errorMessage === null ? undefined : measurement.errorMessage
    )
  }
}

export class MeasurementID {
  benchmark: string
  metric: string

  constructor(benchmark: string, metric: string) {
    this.benchmark = benchmark
    this.metric = metric
  }

  toString(): string {
    return this.benchmark + ' - ' + this.metric
  }

  equals(other: MeasurementID): boolean {
    return other.benchmark === this.benchmark && other.metric === this.metric
  }

  /**
   * Returns a real measurement id (prototypes set and all) from a normal JS
   * object that happens to have all necessary properties.
   *
   * @static
   * @param {MeasurementID} measurementId the object to deconstruct it from
   * @returns a real measurement id
   * @memberof MeasurementID
   */
  static fromRawObject(measurementId: MeasurementID): MeasurementID {
    return new MeasurementID(measurementId.benchmark, measurementId.metric)
  }
}

export class Commit {
  repoID: string
  hash: string
  author: string
  authorDate: number
  committer: string
  committerDate: number | number
  message: string
  parents: string[]

  constructor(
    repoID: string,
    hash: string,
    author: string,
    authorDate: number,
    committer: string,
    committerDate: number,
    message: string,
    parents: string[]
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

  /**
   * Returns a real commit (prototypes set and all) from a normal JS
   * object that happens to have all necessary properties.
   *
   * @static
   * @param {Commit} commit the object to deconstruct it from
   * @returns a real Commit
   * @memberof Commit
   */
  static fromRawObject(commit: Commit): Commit {
    return new Commit(
      commit.repoID,
      commit.hash,
      commit.author,
      commit.authorDate,
      commit.committer,
      commit.committerDate,
      commit.message,
      commit.parents
    )
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
    this.measurements = measurements === undefined ? null : measurements
    this.errorMessage = errorMessage === undefined ? null : errorMessage
  }

  /**
   * Returns a real run (prototypes set and all) from a normal JS
   * object that happens to have all necessary properties.
   *
   * @static
   * @param {Run} run the object to deconstruct it from
   * @returns a real run
   * @memberof Run
   */
  static fromRawObject(run: Run): Run {
    return new Run(
      run.startTime,
      run.stopTime,
      run.measurements
        ? run.measurements.map(it => Measurement.fromRawObject(it))
        : undefined,
      run.errorMessage === null ? undefined : run.errorMessage
    )
  }
}

export class Difference {
  measurement: MeasurementID
  difference: number

  constructor(measurement: MeasurementID, difference: number) {
    this.measurement = measurement
    this.difference = difference
  }

  /**
   * Returns a real difference (prototypes set and all) from a normal JS
   * object that happens to have all necessary properties.
   *
   * @static
   * @param {Difference} difference the object to deconstruct it from
   * @returns a real difference
   * @memberof Difference
   */
  static fromRawObject(difference: Difference): Difference {
    return new Difference(
      MeasurementID.fromRawObject(difference.measurement),
      difference.difference
    )
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

  /**
   * Returns a real comparison (prototypes set and all) from a normal JS
   * object that happens to have all necessary properties.
   *
   * @static
   * @param {CommitComparison} comparison the object to deconstruct it from
   * @returns a real commit comparison
   * @memberof CommitComparison
   */
  static fromRawObject(comparison: CommitComparison) {
    return new CommitComparison(
      comparison.first ? Run.fromRawObject(comparison.first) : null,
      comparison.second ? Run.fromRawObject(comparison.second) : null,
      comparison.firstCommit
        ? Commit.fromRawObject(comparison.firstCommit)
        : null,
      Commit.fromRawObject(comparison.secondCommit),
      comparison.differences.map(it => Difference.fromRawObject(it))
    )
  }
}

export class Datapoint {
  commit: Commit
  value: number

  constructor(commit: Commit, value: number) {
    this.commit = commit
    this.value = value
  }

  /**
   * Returns a real datapoint (prototypes set and all) from a normal JS
   * object that happens to have all necessary properties.
   *
   * @static
   * @param {Datapoint} datapoint the object to deconstruct it from
   * @returns a real datapoint
   * @memberof Datapoint
   */
  static fromRawObject(datapoint: Datapoint): Datapoint {
    return new Datapoint(
      Commit.fromRawObject(datapoint.commit),
      datapoint.value
    )
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
