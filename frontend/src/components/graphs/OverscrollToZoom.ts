import { vxm } from '@/store'

export default class OverscrollToZoom {
  private scrollToZoomInProgress = false

  /**
   * Should be called when a wheel event was triggered.
   * @param e the wheel event
   */
  public scrolled(e: WheelEvent): void {
    if (
      vxm.detailGraphModule.zoomXEndValue !== null ||
      vxm.detailGraphModule.zoomXStartValue !== null
    ) {
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
