/**
 * Removes duplicated elements.
 *
 * @param input the input array
 */
export function distinct<T extends string | number>(input: T[]): T[] {
  const set = input.reduce((acc, next) => acc.add(next), new Set<T>())
  return Array.from(set.values())
}

/**
 * Sorts an array of strings using the local comparator with base sensitivity.
 *
 * @param input the input array
 */
export function locallySorted(input: string[]): string[] {
  return input.sort((a, b) =>
    a.localeCompare(b, undefined, { sensitivity: 'base' })
  )
}
