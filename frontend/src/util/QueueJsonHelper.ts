import {
  Task,
  TaskSource,
  TarTaskSource,
  CommitTaskSource,
  CommitDescription
} from '@/store/types'

export function taskFromJson(json: any): Task {
  return new Task(
    json.id,
    json.author,
    new Date(json.since * 1000),
    sourceFromJson(json.source)
  )
}

function sourceFromJson(json: any): TaskSource {
  if (json.type === 'COMMIT') {
    return new CommitTaskSource(commitDescriptionFromJson(json.source))
  } else if (json.type === 'UPLOADED_TAR') {
    return new TarTaskSource(json.source.description, json.source.repo_id)
  }
  throw new Error('Unknown task type')
}

function commitDescriptionFromJson(json: any): CommitDescription {
  return new CommitDescription(
    json.repo_id,
    json.hash,
    json.author,
    new Date(json.author_date * 1000),
    json.summary
  )
}
