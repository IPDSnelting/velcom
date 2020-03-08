// mdiCrosshairsGps as svg path
export var crosshairIcon = {
  draw: function(context: CanvasRenderingContext2D, size: any) {
    let scale: (x: number) => number = (x: number) => 0.02 * size * x

    context.moveTo(scale(12), scale(8))
    context.bezierCurveTo(
      scale(14.209138999323173),
      scale(8),
      scale(16),
      scale(9.790861000676827),
      scale(16),
      scale(12)
    )
    context.bezierCurveTo(
      scale(16),
      scale(14.209138999323173),
      scale(14.209138999323173),
      scale(16),
      scale(12),
      scale(16)
    )
    context.bezierCurveTo(
      scale(9.790861000676827),
      scale(16),
      scale(8),
      scale(14.209138999323173),
      scale(8),
      scale(12)
    )
    context.bezierCurveTo(
      scale(8),
      scale(9.790861000676827),
      scale(9.790861000676827),
      scale(8),
      scale(12),
      scale(8)
    )
    context.moveTo(scale(3.05), scale(13))
    context.lineTo(scale(1), scale(13))
    context.lineTo(scale(1), scale(11))
    context.lineTo(scale(3.05), scale(11))
    context.bezierCurveTo(
      scale(3.5),
      scale(6.83),
      scale(6.83),
      scale(3.5),
      scale(11),
      scale(3.05)
    )
    context.lineTo(scale(11), scale(1))
    context.lineTo(scale(13), scale(1))
    context.lineTo(scale(13), scale(3.05))
    context.bezierCurveTo(
      scale(17.17),
      scale(3.5),
      scale(20.5),
      scale(6.83),
      scale(20.95),
      scale(11)
    )
    context.lineTo(scale(23), scale(11))
    context.lineTo(scale(23), scale(13))
    context.lineTo(scale(20.95), scale(13))
    context.bezierCurveTo(
      scale(20.5),
      scale(17.17),
      scale(17.17),
      scale(20.5),
      scale(13),
      scale(20.95)
    )
    context.lineTo(scale(13), scale(23))
    context.lineTo(scale(11), scale(23))
    context.lineTo(scale(11), scale(20.95))
    context.bezierCurveTo(
      scale(6.83),
      scale(20.5),
      scale(3.5),
      scale(17.17),
      scale(3.05),
      scale(13)
    )
    context.moveTo(scale(12), scale(5))
    context.bezierCurveTo(
      scale(8.134006751184447),
      scale(5.000000000000001),
      scale(4.999999999999999),
      scale(8.134006751184447),
      scale(5),
      scale(12)
    )
    context.bezierCurveTo(
      scale(5.000000000000001),
      scale(15.865993248815553),
      scale(8.134006751184447),
      scale(19),
      scale(12),
      scale(19)
    )
    context.bezierCurveTo(
      scale(15.865993248815553),
      scale(19),
      scale(19),
      scale(15.865993248815553),
      scale(19),
      scale(12)
    )
    context.bezierCurveTo(
      scale(19),
      scale(8.134006751184447),
      scale(15.865993248815553),
      scale(5),
      scale(12),
      scale(5)
    )
    context.closePath()
  }
}
