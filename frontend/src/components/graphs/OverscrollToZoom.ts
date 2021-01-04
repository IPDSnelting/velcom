import { vxm } from '@/store'

export default class OverscrollToZoom {
  private scrollToZoomInProgress = false

  private isZoomedOut(): boolean {
    if (
      vxm.detailGraphModule.zoomXStartValue == null &&
      vxm.detailGraphModule.zoomXEndValue == null
    ) {
      return true
    }

    return (
      vxm.detailGraphModule.zoomXStartValue ===
        vxm.detailGraphModule.startTime.getTime() &&
      vxm.detailGraphModule.zoomXEndValue ===
        vxm.detailGraphModule.endTime.getTime()
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

    vxm.detailGraphModule.startTime = new Date(
      vxm.detailGraphModule.startTime.getTime() - 7 * 1000 * 60 * 60 * 24
    )

    const clamp = (a: number) => Math.min(a, new Date().getTime())
    vxm.detailGraphModule.endTime = new Date(
      clamp(vxm.detailGraphModule.endTime.getTime() + 7 * 1000 * 60 * 60 * 24)
    )
    vxm.detailGraphModule
      .fetchDetailGraph()
      .finally(() => (this.scrollToZoomInProgress = false))
  }
}
