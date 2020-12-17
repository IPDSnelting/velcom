// fitted to allow somewhat pleasant graph panning/reloading-cycle
export const defaultWaitTime = 200

export function debounce<F extends (...args: any[]) => any>(
  func: F,
  waitTime: number
): (...args: Parameters<F>) => ReturnType<F> {
  let timeout: ReturnType<typeof setTimeout> | null = null

  const debounced = (...args: Parameters<F>) => {
    if (timeout !== null) {
      clearTimeout(timeout)
      timeout = null
    }
    timeout = setTimeout(() => func(...args), waitTime)
  }

  return debounced as (...args: Parameters<F>) => ReturnType<F>
}
