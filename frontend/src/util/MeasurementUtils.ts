/**
 * Returns the height of an HTML element without any border, padding or margin.
 *
 * This might might be slow, as it uses the computed style and parses numbers
 * left and right.
 *
 * @param element the element to get the size for
 */
export function getInnerHeight(element: Element): number {
  const style = getComputedStyle(element)
  let height: number = element.clientHeight

  height -= parseFloat(style.paddingTop) + parseFloat(style.paddingBottom)
  height -= parseFloat(style.marginTop) + parseFloat(style.marginBottom)
  height -=
    parseFloat(style.borderTopWidth) + parseFloat(style.borderBottomWidth)

  return height
}
