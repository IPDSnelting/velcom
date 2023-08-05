import { Flavor } from '@/util/FlavorTypes'

export class RepoBranch {
  static readonly SERIALIZED_NAME = 'RepoBranch'

  readonly name: string
  readonly tracked: boolean
  readonly lastCommit: CommitHash

  constructor(name: string, tracked: boolean, lastCommit: CommitHash) {
    this.name = name
    this.tracked = tracked
    this.lastCommit = lastCommit
  }
}

export type RepoId = Flavor<string, 'repo_id'>
export class Repo {
  static readonly SERIALIZED_NAME = 'Repo'

  readonly id: RepoId
  name: string
  branches: RepoBranch[]
  dimensions: Dimension[]
  remoteURL: string
  lastGithubUpdate: Date | undefined

  constructor(
    id: RepoId,
    name: string,
    branches: RepoBranch[],
    dimensions: Dimension[],
    remoteURL: string,
    lastGithubUpdate: Date | undefined
  ) {
    this.id = id
    this.name = name
    this.dimensions = dimensions
    this.remoteURL = remoteURL
    this.branches = branches.sort((a, b) =>
      a.name.localeCompare(b.name, undefined, { sensitivity: 'base' })
    )
    this.lastGithubUpdate = lastGithubUpdate
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

export function dimensionIdToString(dimensionId: DimensionId): string {
  return `${dimensionId.benchmark} - ${dimensionId.metric}`
}

export function dimensionIdEquals(
  first: DimensionId,
  second: DimensionId
): boolean {
  return first.benchmark === second.benchmark && first.metric === second.metric
}

export class Dimension {
  static readonly SERIALIZED_NAME = 'Dimension'

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
   * '{benchmark} - {metric}' without the {}.
   */
  toString(): string {
    return dimensionIdToString(this)
  }

  /**
   * Checks if the dimension equals another. Only checks the benchmark and metric.
   *
   * @param {DimensionId} other the other dimension
   * @returns true if the two have the same benchmark and metric
   */
  equals(other: DimensionId | null): boolean {
    if (other === null) {
      return false
    }
    return other.benchmark === this.benchmark && other.metric === this.metric
  }
}

export type CommitHash = Flavor<string, 'commit_hash'>

export class TrackedCommitDescription {
  readonly tracked: boolean
  readonly description: CommitDescription

  constructor(tracked: boolean, description: CommitDescription) {
    this.tracked = tracked
    this.description = description
  }

  static comparator(): (
    a: TrackedCommitDescription,
    b: TrackedCommitDescription
  ) => number {
    return (a: TrackedCommitDescription, b: TrackedCommitDescription) => {
      if (a.tracked && b.tracked) {
        return a.description.summary.localeCompare(b.description.summary)
      }
      if (a.tracked) {
        return -1
      }
      return 1
    }
  }
}

export class Commit {
  readonly repoId: RepoId
  readonly hash: CommitHash
  readonly author: string
  readonly authorDate: Date
  readonly committer: string
  readonly committerDate: Date
  readonly summary: string
  readonly message: string | ''
  readonly tracked: boolean
  /**
   * Sorted in reverse start order (newest run first)
   */
  readonly runs: RunDescription[]
  /**
   * Tracked parents will come before untracked parents,
   * inside the buckets they are sorted alphabetically
   */
  readonly parents: TrackedCommitDescription[]
  /**
   * Tracked children will come before untracked children,
   * inside the buckets they are sorted alphabetically
   */
  readonly children: TrackedCommitDescription[]

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
    tracked: boolean,
    runs: RunDescription[],
    parents: TrackedCommitDescription[],
    children: TrackedCommitDescription[]
  ) {
    this.repoId = repoId
    this.hash = hash
    this.author = author
    this.authorDate = authorDate
    this.committer = committer
    this.committerDate = committerDate
    this.message = message
    this.summary = summary
    this.tracked = tracked
    this.runs = runs
    this.parents = parents
    this.children = children

    this.runs.sort((a, b) => b.startTime.getTime() - a.startTime.getTime())
    this.parents.sort(TrackedCommitDescription.comparator())
    this.children.sort(TrackedCommitDescription.comparator())
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
  readonly stddev?: number
  readonly stddevPercent?: number

  constructor(
    dimension: Dimension,
    value: number,
    values: number[],
    stddev?: number,
    stddevPercent?: number
  ) {
    this.dimension = dimension
    this.value = value
    this.values = values
    this.stddev = stddev
    this.stddevPercent = stddevPercent
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
  perDimension?: Map<string, Measurement>

  constructor(measurements: Measurement[]) {
    this.measurements = measurements
  }

  /**
   * Returns the measurement for a given dimension from a lazily-populated cache.
   *
   * @param dimension the dimension to look up the measurement for
   */
  public forDimension(dimension: Dimension): Measurement | undefined {
    if (this.perDimension === undefined) {
      this.perDimension = new Map()
      for (const measurement of this.measurements) {
        // We only have one measurement per dimension, so this index is safe and won't collide.
        // Freeze the object as we won't make fine-granular changes to it (we always re-fetch the whole run)
        this.perDimension.set(
          measurement.dimension.toString(),
          Object.freeze(measurement)
        )
      }
    }
    return this.perDimension!.get(dimension.toString())
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
  readonly significantDifferences: DimensionDifference[]
  readonly significantFailedDimensions: Dimension[]

  constructor(
    run: Run,
    differences: DimensionDifference[],
    significantDifferences: DimensionDifference[],
    significantFailedDimensions: Dimension[]
  ) {
    this.run = run
    this.differences = differences
    this.significantDifferences = significantDifferences
    this.significantFailedDimensions = significantFailedDimensions
  }
}

export class RunDescriptionWithDifferences {
  readonly run: RunDescription
  readonly significantDifferences: DimensionDifference[]
  readonly significantFailedDimensions: Dimension[]

  constructor(
    run: RunDescription,
    significantDifferences: DimensionDifference[],
    significantFailedDimensions: Dimension[]
  ) {
    this.run = run
    this.significantDifferences = significantDifferences
    this.significantFailedDimensions = significantFailedDimensions
  }
}

export class Worker {
  readonly name: string
  readonly info: string
  readonly workingOn: string | null
  readonly workingSince: Date | null
  readonly lostConnection: boolean

  constructor(
    name: string,
    info: string,
    workingOn: string | null,
    workingSince: Date | null,
    lostConnection: boolean
  ) {
    this.name = name
    this.info = info
    this.workingOn = workingOn
    this.workingSince = workingSince
    this.lostConnection = lostConnection
  }
}

export class DimensionDifference {
  readonly dimension: Dimension
  readonly oldRunId: RunId
  readonly absDiff: number
  readonly relDiff?: number
  readonly stddevDiff?: number

  constructor(
    dimension: Dimension,
    oldRunId: RunId,
    absDiff: number,
    relDiff?: number,
    stddevDiff?: number
  ) {
    this.dimension = dimension
    this.oldRunId = oldRunId
    this.absDiff = absDiff
    this.relDiff = relDiff
    this.stddevDiff = stddevDiff
  }
}

export class RunComparison {
  readonly run1: Run
  readonly run2: Run
  readonly differences: DimensionDifference[]
  readonly significantDifferences: DimensionDifference[]
  readonly significantFailedDimensions: Dimension[]

  constructor(
    run1: Run,
    run2: Run,
    differences: DimensionDifference[],
    significantDifferences: DimensionDifference[],
    significantFailedDimensions: Dimension[]
  ) {
    this.run1 = run1
    this.run2 = run2
    this.differences = differences
    this.significantDifferences = significantDifferences
    this.significantFailedDimensions = significantFailedDimensions
  }
}

export type SeriesId = Flavor<string, 'series_id'>

export type SeriesInformation = {
  id: SeriesId
  displayName: string
  color: string
  unit?: string
}

export type GraphDataPointValue =
  | number
  | 'NO_RUN'
  | 'NO_MEASUREMENT'
  | 'RUN_FAILED'
  | 'MEASUREMENT_FAILED'

export abstract class GraphDataPoint {
  abstract readonly positionTime: Date
  abstract readonly committerTime: Date
  abstract readonly uid: string
  abstract readonly hash: string
  abstract readonly repoId: RepoId
  abstract readonly values: Map<SeriesId, GraphDataPointValue>
  abstract readonly parentUids: string[]
  abstract readonly summary: string
  abstract readonly author: string

  public successful(series: SeriesId): boolean {
    return typeof this.values.get(series) === 'number'
  }

  public metricNotBenchmarked(series: SeriesId): boolean {
    const value = this.values.get(series)
    return value === 'NO_MEASUREMENT'
  }

  public commitUnbenchmarked(series: SeriesId): boolean {
    const value = this.values.get(series)
    return value === 'NO_RUN'
  }

  /**
   * Either there is no run for this commit or the given metric was not
   * measured for it
   * @param series the series to check
   */
  public unbenchmarked(series: SeriesId): boolean {
    return this.commitUnbenchmarked(series) || this.metricNotBenchmarked(series)
  }

  public failed(series: SeriesId): boolean {
    const value = this.values.get(series)
    return value === 'MEASUREMENT_FAILED' || value === 'RUN_FAILED'
  }

  public abstract positionedAt(positionTime: Date): GraphDataPoint
}

export type AttributedDatapoint = {
  datapoint: GraphDataPoint
  seriesId: SeriesId
}

export class DetailDataPoint extends GraphDataPoint {
  static readonly SERIALIZED_NAME = 'DetailDataPoint'

  readonly hash: CommitHash
  readonly repoId: RepoId
  readonly uid: string
  readonly parentUids: string[]
  readonly author: string
  readonly committerTime: Date
  readonly positionTime: Date // to alter position in day equidistant graphs
  readonly summary: string
  // TODO: Figure out if the map wastes too much memory
  readonly values: Map<SeriesId, GraphDataPointValue>

  // noinspection DuplicatedCode
  constructor(
    repoId: RepoId,
    hash: CommitHash,
    parentUids: string[],
    author: string,
    committerDate: Date,
    positionDate: Date,
    summary: string,
    values: Map<SeriesId, GraphDataPointValue>
  ) {
    super()
    this.repoId = repoId
    this.hash = hash
    this.uid = repoId + hash
    this.parentUids = parentUids
    this.author = author
    this.committerTime = committerDate
    this.positionTime = positionDate
    this.summary = summary
    this.values = values
  }

  positionedAt(positionTime: Date): DetailDataPoint {
    return new DetailDataPoint(
      this.repoId,
      this.hash,
      this.parentUids,
      this.author,
      this.committerTime,
      positionTime,
      this.summary,
      this.values
    )
  }
}

export class ComparisonDataPoint extends GraphDataPoint {
  static readonly SERIALIZED_NAME = 'ComparisonDataPoint'

  readonly positionTime: Date
  readonly committerTime: Date
  readonly hash: string
  readonly repoId: RepoId
  readonly values: Map<SeriesId, GraphDataPointValue>
  readonly parentUids: string[]
  readonly summary: string
  readonly author: string
  readonly uid: string

  /**
   * Creates a new comparison data point.
   *
   * @param committerTime the time the commit was comitted at
   * @param positionTime the time the commit is displayed at in the graph
   * @param hash the hash of the commit
   * @param repoId the id of the repo it is in
   * @param values the *value* (singular) of this commit. Has a single entry with the repoId -> value
   * @param parentUids the UID of the parent commits
   * @param summary the commit message summary (first line)
   * @param author the author of the commit
   */
  constructor(
    committerTime: Date,
    positionTime: Date,
    hash: string,
    repoId: string,
    values: Map<SeriesId, GraphDataPointValue>,
    parentUids: string[],
    summary: string,
    author: string
  ) {
    super()
    this.positionTime = positionTime
    this.committerTime = committerTime
    this.repoId = repoId
    this.hash = hash
    this.values = values
    this.parentUids = parentUids
    this.summary = summary
    this.author = author
    this.uid = this.repoId + this.hash

    if (this.values.size !== 1 || !this.values.has(repoId)) {
      throw new Error(
        "Graph datapoint didn't received the values it expected (" +
          Array.from(this.values.entries()) +
          ') for repo' +
          this.repoId
      )
    }
  }

  positionedAt(positionTime: Date): ComparisonDataPoint {
    return new ComparisonDataPoint(
      this.committerTime,
      positionTime,
      this.hash,
      this.repoId,
      this.values,
      this.parentUids,
      this.summary,
      this.author
    )
  }
}

export class StatusComparisonPoint {
  readonly repoId: RepoId
  readonly run?: Run
  readonly commitHash: CommitHash

  constructor(repoId: RepoId, run: Run | undefined, commitHash: CommitHash) {
    this.repoId = repoId
    this.run = run
    this.commitHash = commitHash
  }
}

export class StreamedRunnerOutput {
  readonly outputLines: string[]
  /**
   * The line number of the first line. Starts with 0.
   */
  readonly indexOfFirstLine: number

  constructor(outputLines: string[], lineOffset: number) {
    this.outputLines = outputLines
    this.indexOfFirstLine = lineOffset
  }
}

export class SearchItemCommit {
  readonly repoId: RepoId
  readonly hash: CommitHash
  readonly author: string
  readonly authorDate: Date
  readonly committer: string
  readonly committerDate: Date
  readonly summary: string
  readonly hasRun: boolean

  // noinspection DuplicatedCode
  constructor(
    repoId: RepoId,
    hash: string,
    author: string,
    authorDate: Date,
    committer: string,
    committerDate: Date,
    summary: string,
    hasRun: boolean
  ) {
    this.repoId = repoId
    this.hash = hash
    this.author = author
    this.authorDate = authorDate
    this.committer = committer
    this.committerDate = committerDate
    this.summary = summary
    this.hasRun = hasRun
  }
}

export class SearchItemRun {
  readonly id: RunId
  readonly repoId?: RepoId
  readonly commitHash?: CommitHash
  readonly commitSummary?: string
  readonly tarDescription?: string
  readonly startTime: Date
  readonly stopTime: Date

  // noinspection DuplicatedCode
  constructor(
    id: RunId,
    repoId: RepoId,
    commitHash: CommitHash,
    commitSummary: string,
    tarDescription: string,
    startTime: Date,
    stopTime: Date
  ) {
    this.id = id
    this.repoId = repoId
    this.commitHash = commitHash
    this.commitSummary = commitSummary
    this.tarDescription = tarDescription
    this.startTime = startTime
    this.stopTime = stopTime
  }
}

export class SearchItemBranch {
  readonly repoId: RepoId
  readonly name: string
  readonly hash: CommitHash
  readonly commitSummary: string
  readonly hasRun: boolean

  constructor(
    repoId: RepoId,
    name: string,
    hash: CommitHash,
    commitSummary: string,
    hasRun: boolean
  ) {
    this.repoId = repoId
    this.name = name
    this.hash = hash
    this.commitSummary = commitSummary
    this.hasRun = hasRun
  }
}

export type SearchItem = SearchItemCommit | SearchItemRun | SearchItemBranch

export type GithubCommentId = Flavor<number, 'github_comment_id'>
export type GithubPrNumber = Flavor<number, 'github_pr_number'>
export type GithubBotCommandState = 'NEW' | 'MARKED_SEEN' | 'QUEUED' | 'ERROR'

export class GithubBotCommand {
  readonly state: GithubBotCommandState
  readonly sourceCommentId: GithubCommentId
  readonly prNumber: GithubPrNumber

  constructor(
    state: GithubBotCommandState,
    sourceCommentId: GithubCommentId,
    prNumber: GithubPrNumber
  ) {
    this.state = state
    this.sourceCommentId = sourceCommentId
    this.prNumber = prNumber
  }
}

export class CleanupDimension {
  readonly dimension: Dimension
  readonly runs: number
  readonly untrackedRuns: number
  readonly unreachableRuns: number

  constructor(
    dimension: Dimension,
    runs: number,
    untrackedRuns: number,
    unreachableRuns: number
  ) {
    this.dimension = dimension
    this.runs = runs
    this.untrackedRuns = untrackedRuns
    this.unreachableRuns = unreachableRuns
  }

  get key(): string {
    return this.dimension.toString()
  }
}
