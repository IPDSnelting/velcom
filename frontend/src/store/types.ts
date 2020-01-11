export interface RootState {
  apiBaseURL: string

  colorState: ColorState
  repoState: RepoState
  repoComparisonState: RepoComparisonState
  repoDetailState: RepoDetailState
  newsState: NewsState
  commitComparisonState: CommitComparisonState
  queueState: QueueState
  userState: UserState
}

export interface ColorState {
  colors: Array<string>
}

export interface RepoState {
  repos: { [id: string]: Repo }
}

export interface RepoComparisonState {
  runsByRepoID: { [repoID: string]: Array<Run> }
}

export interface RepoDetailState {
  comparisonsByRepoID: { [repoID: string]: Array<CommitComparison> }
}

export interface NewsState {
  recentRuns: Array<CommitComparison>
  recentSignificantRuns: Array<CommitComparison>
}

export interface CommitComparisonState {
  comparisons: Map<{ repoID: string; hashOne: string}, {[hashTwo: string]: CommitComparison}>
}

export interface QueueState {
  openTasks: Array<Commit>
  workers: Array<Worker>
}

export interface UserState {
  role: 'WEB_ADMIN' | 'REPO_ADMIN' | null
  repoID: string | null
  token: string | null
}

export class Repo {
  id: string | null
  name: string | null
  branches: Array<string>
  trackedBranches: Array<string>
  measurements: Array<MeasurementID>
  remoteURL: string | null

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
  id: MeasurementID | null
  successful: boolean | null
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
  benchmark: string | null
  metric: string | null

  constructor(benchmark: string, metric: string) {
    this.benchmark = benchmark
    this.metric = metric
  }
}

export class Commit {
  repoID: string | null
  hash: string | null
  author: string
  authorDate: number | null
  committer: string | null
  committerDate: number | number
  message: string | null
  parents: Array<string>

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
  commit: Commit | null
  startTime: number | null
  stopTime: number | null
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
  measurement: MeasurementID | null
  difference: number | null

  constructor(measurement: MeasurementID, difference: number) {
    this.measurement = measurement
    this.difference = difference
  }
}

export class CommitComparison {
  first: Run | null
  second: Run | null
  differences: Array<Difference>

  constructor(first: Run, second: Run, differences: Array<Difference>) {
    this.first = first
    this.second = second
    this.differences = differences
  }
}

export class Worker {
  name: string | null
  osData: string | null

  constructor(name: string, osData: string) {
    this.name = name
    this.osData = osData
  }
}
