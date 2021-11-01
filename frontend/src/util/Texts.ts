import AnsiUp from 'ansi_up'
import { DimensionId, RepoId } from '@/store/types'

const converter = new AnsiUp()
converter.use_classes = true

/**
 * Escapes all HTML in a given string.
 *
 * @param input the input string to escape
 */
export function escapeHtml(input: string): string {
  const p = document.createElement('p')
  p.appendChild(document.createTextNode(input))
  return p.innerHTML
}

/**
 * Converts text with ANSI escape codes to HTML.
 *
 * @param input the input text
 */
export function safeConvertAnsi(input: string): string {
  // The conversion function of ansi_up 5+ sanitizes input by itself, so
  // just converting it is safe
  return converter.ansi_to_html(input)
}

/**
 * Formats a list of repo-branch entries to a single string with the following format:
 * "repo:branch:branch::repo2:branch:branch".
 *
 * @param repos the repositories to format
 */
export function formatRepos(repos: Map<RepoId, string[]>): string {
  return Array.from(repos.entries())
    .filter(([, branches]) => branches.length > 0)
    .map(([repoId, branches]) => {
      return repoId + ':' + branches.join(':')
    })
    .join('::')
}

/**
 * Formats a given list of dimensions according to their API serialization format:
 * 'bench1:metric1.1:metric1.2::bench2:metric2.1' etc.
 */
export function formatDimensions(dimensions: DimensionId[]): string {
  const byBenchmark = new Map<string, string[]>()

  dimensions.forEach(dimension => {
    if (!byBenchmark.has(dimension.benchmark)) {
      byBenchmark.set(dimension.benchmark, [])
    }
    byBenchmark.get(dimension.benchmark)!.push(dimension.metric)
  })

  return Array.from(byBenchmark.entries())
    .map(([benchmark, metrics]) => {
      return benchmark + ':' + metrics.join(':')
    })
    .join('::')
}
