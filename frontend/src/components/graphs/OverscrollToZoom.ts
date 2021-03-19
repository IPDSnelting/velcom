import { debounce } from '@/util/Debouncer'

export type OverscrollStore = {
  zoomXStartValue: number | null
  zoomXEndValue: number | null

  startTime: Date
  endTime: Date
}

export default class OverscrollToZoom {
  private scrollToZoomInProgress = false
  private readonly refreshFunction: () => Promise<unknown>
  private readonly store: OverscrollStore

  constructor(refreshFunction: () => Promise<unknown>, store: OverscrollStore) {
    this.refreshFunction = refreshFunction
    this.store = store
  }

  private isZoomedOut(): boolean {
    if (
      this.store.zoomXStartValue == null &&
      this.store.zoomXEndValue == null
    ) {
      return true
    }

    return (
      this.store.zoomXStartValue === this.store.startTime.getTime() &&
      this.store.zoomXEndValue === this.store.endTime.getTime()
    )
  }

  /**
   * Should be called when a wheel event was triggered.
   * @param e the wheel event
   */
  public scrolled(e: WheelEvent): void {
    if (!this.isZoomedOut()) {
      return
    }

    // scrolling upwards => zooming in
    if (e.deltaY <= 0) {
      return
    }

    // Prevent scrolling
    e.preventDefault()

    if (this.scrollToZoomInProgress) {
      return
    }
    this.scrollToZoomInProgress = true

    this.store.startTime = new Date(
      this.store.startTime.getTime() - 7 * 1000 * 60 * 60 * 24
    )

    const clamp = (a: number) => Math.min(a, new Date().getTime())
    this.store.endTime = new Date(
      clamp(this.store.endTime.getTime() + 7 * 1000 * 60 * 60 * 24)
    )

    this.refreshFunction().finally(() => (this.scrollToZoomInProgress = false))
  }
}
