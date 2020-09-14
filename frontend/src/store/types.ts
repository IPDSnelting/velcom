import { Flavor } from '@/util/FlavorTypes'

export class RepoBranch {
  readonly name: string
  readonly tracked: boolean

  constructor(name: string, tracked: boolean) {
    this.name = name
    this.tracked = tracked
  }
}

export type RepoId = Flavor<string, 'repo_id'>
export class Repo {
  readonly id: RepoId
  name: string
  branches: RepoBranch[]
  dimensions: Dimension[]
  remoteURL: string
  hasToken: boolean

  constructor(
    id: RepoId,
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

  /**
   * Returns all tracked branches.
   *
   * @returns {string[]} all tracked branches
   * @memberof Repo
   */
  public get trackedBranches(): string[] {
    return this.branches.filter(it => it.tracked).map(it => it.name)
  }
}

export type DimensionInterpretation = Flavor<
  'LESS_IS_BETTER' | 'MORE_IS_BETTER' | 'NEUTRAL',
  'dimension'
>

export type DimensionId = {
  readonly benchmark: string
  readonly metric: string
}

export class Dimension {
  readonly benchmark: string
  readonly metric: string
  readonly unit: string
  readonly interpretation: DimensionInterpretation

  constructor(
    benchmark: string,
    metric: string,
    unit: string,
    interpretation: DimensionInterpretation
  ) {
    this.benchmark = benchmark
    this.metric = metric
    this.unit = unit
    this.interpretation = interpretation
  }

  /**
   * Returns the metric and benchmark in the following format:
   * '{benchmarl} - {metric}' without the {}.
   */
  toString(): string {
    return `${this.benchmark} - ${this.metric}`
  }

  /**
   * Checks if the dimension equals another. Only checks the benchmark and metric.
   *
   * @param {DimensionId} other the other dimension
   * @returns true if the two have the same benchmark and metric
   */
  equals(other: DimensionId): boolean {
    return other.benchmark === this.benchmark && other.metric === this.metric
  }
}

export type CommitHash = Flavor<string, 'commit_hash'>

export class Commit {
  readonly repoId: RepoId
  readonly hash: CommitHash
  readonly author: string
  readonly authorDate: Date
  readonly committer: string
  readonly committerDate: Date
  readonly summary: string
  readonly message: string | ''
  readonly runs: RunDescription[]
  readonly parents: CommitDescription[]
  readonly children: CommitDescription[]

  // noinspection DuplicatedCode
  constructor(
    repoId: RepoId,
    hash: CommitHash,
    author: string,
    authorDate: Date,
    committer: string,
    committerDate: Date,
    message: string,
    summary: string,
    runs: RunDescription[],
    parents: CommitDescription[],
    children: CommitDescription[]
  ) {
    this.repoId = repoId
    this.hash = hash
    this.author = author
    this.authorDate = authorDate
    this.committer = committer
    this.committerDate = committerDate
    this.message = message
    this.summary = summary
    this.runs = runs
    this.parents = parents
    this.children = children
  }
}

export class CommitDescription {
  readonly repoId: RepoId
  readonly hash: CommitHash
  readonly author: string
  readonly authorDate: Date
  readonly summary: string

  constructor(
    repoId: RepoId,
    hash: CommitHash,
    author: string,
    authorDate: Date,
    summary: string
  ) {
    this.repoId = repoId
    this.hash = hash
    this.author = author
    this.authorDate = authorDate
    this.summary = summary
  }
}

export class CommitTaskSource {
  readonly type: string = 'COMMIT'
  readonly commitDescription: CommitDescription

  constructor(commitDescription: CommitDescription) {
    this.commitDescription = commitDescription
  }
}

export class TarTaskSource {
  readonly type: string = 'UPLOADED_TAR'
  readonly description: string
  readonly repoId: string

  constructor(description: string, repoId: string) {
    this.description = description
    this.repoId = repoId
  }
}

export type TaskSource = TarTaskSource | CommitTaskSource
export type TaskId = Flavor<string, 'task_id'>

export class Task {
  readonly id: TaskId
  readonly author: string
  readonly since: Date
  readonly source: TaskSource

  constructor(id: TaskId, author: string, since: Date, source: TaskSource) {
    this.id = id
    this.author = author
    this.since = since
    this.source = source
  }
}

export class MeasurementSuccess {
  readonly dimension: Dimension
  readonly value: number
  readonly values: number[]

  constructor(dimension: Dimension, value: number, values: number[]) {
    this.dimension = dimension
    this.value = value
    this.values = values
  }
}

export class MeasurementError {
  readonly dimension: Dimension
  readonly error: string

  constructor(dimension: Dimension, error: string) {
    this.dimension = dimension
    this.error = error
  }
}

export type Measurement = MeasurementError | MeasurementSuccess

export type RunId = Flavor<string, 'run_id'>
export type RunDescriptionSuccess = Flavor<
  'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILURE',
  'run_description_success'
>

export class RunResultScriptError {
  readonly error: string

  constructor(error: string) {
    this.error = error
  }
}
export class RunResultVelcomError {
  readonly error: string

  constructor(error: string) {
    this.error = error
  }
}
export class RunResultSuccess {
  readonly measurements: Measurement[]

  constructor(measurements: Measurement[]) {
    this.measurements = measurements
  }
}
export type RunResult =
  | RunResultScriptError
  | RunResultVelcomError
  | RunResultSuccess

export class Run {
  readonly id: RunId
  readonly author: string
  readonly runnerName: string
  readonly runnerInfo: string
  readonly startTime: Date
  readonly stopTime: Date
  readonly source: TaskSource
  readonly result: RunResult

  // noinspection DuplicatedCode
  constructor(
    id: RunId,
    author: string,
    runnerName: string,
    runnerInfo: string,
    startTime: Date,
    stopTime: Date,
    source: TaskSource,
    result: RunResult
  ) {
    this.id = id
    this.author = author
    this.runnerName = runnerName
    this.runnerInfo = runnerInfo
    this.startTime = startTime
    this.stopTime = stopTime
    this.source = source
    this.result = result
  }
}

export class RunDescription {
  readonly runId: RunId
  readonly startTime: Date
  readonly success: RunDescriptionSuccess
  readonly source: TaskSource

  constructor(
    runId: RunId,
    startTime: Date,
    success: RunDescriptionSuccess,
    source: TaskSource
  ) {
    this.runId = runId
    this.startTime = startTime
    this.success = success
    this.source = source
  }
}

export class RunWithDifferences {
  readonly run: Run
  readonly differences: DimensionDifference[]

  constructor(run: Run, differences: DimensionDifference[]) {
    this.run = run
    this.differences = differences
  }
}

export class RunDescriptionWithDifferences {
  readonly run: RunDescription
  readonly differences: DimensionDifference[]

  constructor(run: RunDescription, differences: DimensionDifference[]) {
    this.run = run
    this.differences = differences
  }
}

export class Worker {
  readonly name: string
  readonly info: string
  readonly workingOn: string | null
  readonly workingSince: Date | null

  constructor(
    name: string,
    info: string,
    workingOn: string | null,
    workingSince: Date | null
  ) {
    this.name = name
    this.info = info
    this.workingOn = workingOn
    this.workingSince = workingSince
  }
}

export class DimensionDifference {
  readonly dimension: Dimension
  readonly absDiff: number
  readonly relDiff: number
  readonly stddev: number | undefined

  constructor(
    dimension: Dimension,
    absDiff: number,
    relDiff: number,
    stddev: number | undefined
  ) {
    this.dimension = dimension
    this.absDiff = absDiff
    this.relDiff = relDiff
    this.stddev = stddev
  }
}

export class RunComparison {
  readonly run1: Run
  readonly run2: Run
  readonly differences: DimensionDifference[]

  constructor(run1: Run, run2: Run, differences: DimensionDifference[]) {
    this.run1 = run1
    this.run2 = run2
    this.differences = differences
  }
}

export type DetailDataPointValue =
  | number
  | 'NO_RUN'
  | 'NO_MEASUREMENT'
  | 'RUN_FAILED'
  | 'MEASUREMENT_FAILED'

export class DetailDataPoint {
  readonly hash: CommitHash
  readonly parents: CommitHash[]
  readonly author: string
  readonly authorDate: Date
  readonly summary: string
  // TODO: Figure out if the map wastes too much memory
  readonly values: Map<DimensionId, DetailDataPointValue>

  constructor(
    hash: CommitHash,
    parents: CommitHash[],
    author: string,
    authorDate: Date,
    summary: string,
    values: Map<DimensionId, DetailDataPointValue>
  ) {
    this.hash = hash
    this.parents = parents
    this.author = author
    this.authorDate = authorDate
    this.summary = summary
    this.values = values
  }

  public successful(dimension: DimensionId): boolean {
    return typeof this.values.get(dimension) === 'number'
  }

  public unbenchmarked(dimension: DimensionId): boolean {
    const value = this.values.get(dimension)
    return value === 'NO_RUN' || value === 'NO_MEASUREMENT'
  }

  public failed(dimension: DimensionId): boolean {
    const value = this.values.get(dimension)
    return value === 'MEASUREMENT_FAILED' || value === 'RUN_FAILED'
  }
}
export class ComparisonDataPoint {
  readonly hash: CommitHash
  readonly author: string
  readonly authorDate: Date
  readonly summary: string
  readonly value: number
  readonly repoId: RepoId

  constructor(
    hash: CommitHash,
    author: string,
    authorDate: Date,
    summary: string,
    value: number,
    repoId: RepoId
  ) {
    this.hash = hash
    this.author = author
    this.authorDate = authorDate
    this.summary = summary
    this.value = value
    this.repoId = repoId
  }
}
