import AnsiUp from 'ansi_up'
import { RepoId } from '@/store/types'

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
  const safeInput = escapeHtml(input)

  return converter.ansi_to_html(safeInput)
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
