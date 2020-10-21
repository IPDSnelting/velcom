/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import {
  Task,
  TaskSource,
  TarTaskSource,
  CommitTaskSource,
  CommitDescription,
  Worker,
  StreamedRunnerOutput
} from '@/store/types'

export function workerFromJson(json: any): Worker {
  return new Worker(
    json.name,
    json.info,
    json.working_on,
    json.working_since ? new Date(json.working_since * 1000) : null
  )
}

export function taskFromJson(json: any): Task {
  return new Task(
    json.id,
    json.author,
    new Date(json.since * 1000),
    sourceFromJson(json.source)
  )
}

export function sourceFromJson(json: any): TaskSource {
  if (json.type === 'COMMIT') {
    return new CommitTaskSource(commitDescriptionFromJson(json.source))
  } else if (json.type === 'UPLOADED_TAR') {
    return new TarTaskSource(json.source.description, json.source.repo_id)
  }
  throw new Error('Unknown task type')
}

export function commitDescriptionFromJson(json: any): CommitDescription {
  return new CommitDescription(
    json.repo_id,
    json.hash,
    json.author,
    new Date(json.author_date * 1000),
    json.summary
  )
}

export function streamedRunnerOutputFromJson(json: any): StreamedRunnerOutput {
  return new StreamedRunnerOutput(
    json.output.split('\n'),
    json.index_of_first_line
  )
}
