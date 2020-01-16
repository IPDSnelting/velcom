export interface RootState {
  apiBaseURL: string

  colorModule: ColorState
  repoModule: RepoState
  repoComparisonModule: RepoComparisonState
  repoDetailModule: RepoDetailState
  newsModule: NewsState
  commitComparisonModule: CommitComparisonState
  queueModule: QueueState
  userModule: UserState
}

export interface ColorState {
  colors: Array<string>
}

export interface RepoState {
  repos: Map<string, Repo>
}

export interface RepoComparisonState {
  runsByRepoID: Map<string, Array<Run>>
}

export interface RepoDetailState {
  comparisonsByRepoID: Map<string, Array<CommitComparison>>
}

export interface NewsState {
  recentRuns: Array<CommitComparison>
  recentSignificantRuns: Array<CommitComparison>
}

export interface CommitComparisonState {
  comparisons: Map<string, Array<CommitComparison>>
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
  first: Run
  second: Run
  differences: Array<Difference>

  constructor(first: Run, second: Run, differences: Array<Difference>) {
    this.first = first
    this.second = second
    this.differences = differences
  }
}

export class Worker {
  name: string
  osData: string | null
  currentTask: Commit

  constructor(name: string, osData: string, currentTask: Commit) {
    this.name = name
    this.osData = osData
    this.currentTask = currentTask
  }
}

// util for generating new distinguishable hex colors
export class ColorConverter {
  hexToHsl(hex: string) {
    // convert hex to rgb
    var r = parseInt(hex.substr(1, 2), 16)
    var g = parseInt(hex.substr(3, 2), 16)
    var b = parseInt(hex.substr(5, 2), 16)

    // convert rgb to hsl
    r /= 255
    g /= 255
    b /= 255

    var max = Math.max(r, g, b)
    var min = Math.min(r, g, b)

    var h = 0
    var s = 0
    var l = (max + min) / 2

    if (max === min) {
      h = s = 0 // achromatic
    } else {
      var diff = max - min
      s = l > 0.5 ? diff / (2 - max - min) : diff / (max + min)

      switch (max) {
        case r:
          h = (g - b) / diff + (g < b ? 6 : 0)
          break
        case g:
          h = (b - r) / diff + 2
          break
        case b:
          h = (r - g) / diff + 4
          break
      }
      h /= 6
    }

    return [h, s, l]
  }

  hslToHex(h: number, s: number, l: number) {
    var r = 0
    var g = 0
    var b = 0

    if (s === 0) {
      r = g = b = l // achromatic
    } else {
      const hueToRgb = (p: number, q: number, t: number) => {
        if (t < 0) t += 1
        if (t > 1) t -= 1
        if (t < 1 / 6) return p + (q - p) * 6 * t
        if (t < 1 / 2) return q
        if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6
        return p
      }
      const q = l < 0.5 ? l * (1 + s) : l + s - l * s
      const p = 2 * l - q
      r = hueToRgb(p, q, h + 1 / 3)
      g = hueToRgb(p, q, h)
      b = hueToRgb(p, q, h - 1 / 3)
    }
    const toHex = (num: number) => {
      const hex = Math.round(num * 255).toString(16)
      return hex.length === 1 ? '0' + hex : hex
    }
    return `#${toHex(r)}${toHex(g)}${toHex(b)}`
  }
}
