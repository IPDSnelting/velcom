/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import { PermanentLinkOptions } from '@/store/modules/detailGraphStore'
import { dateFromRelative, roundDateDown, roundDateUp } from '@/util/Times'
import { Route } from 'vue-router'

/**
 * Converts something to a string or undefined, if it was falsy or undefined.
 *
 * @param it the object to convert
 */
export function orUndefined(it: any): string | undefined {
  return it ? '' + it : undefined
}

/**
 * Converts something to a string or `subst`, if it was falsy or undefined.
 *
 * @param it the object to convert
 * @param subst the alternative to use if it was undefined
 */
export function orElse(it: any, subst: any): string {
  return it ? '' + it : '' + subst
}

/**
 * Returns the given value if the option "name" is present in "options",
 * undefined otherwise.
 *
 * @param options the options to use
 * @param name the name of the option
 * @param value the value to return if the option is defined
 */
export function respectOptions<T>(
  options: PermanentLinkOptions | undefined,
  name: keyof PermanentLinkOptions,
  value: T
): T | undefined {
  if (!options || options[name]) {
    return value
  }
  return undefined
}

/**
 * Extracts the query parameter with name "name" from the passed "link" and if
 * the value is a valid float (not null, and not NaN) it calls action with it.
 *
 * @param link the link
 * @param name the name of the query parameter
 * @param action the action to execute with it
 */
export function extractFloatFromQuery(
  link: Route,
  name: string,
  action: (value: number) => void
): void {
  const queryValue = link.query[name]
  if (queryValue && typeof queryValue === 'string') {
    if (!isNaN(parseFloat(queryValue))) {
      action(parseFloat(queryValue))
    }
  }
}

/**
 * Extracts the query parameter with name "name" from the passed "link" and if
 * the value is a valid date (a floats or a "relative time string", see
 * dateFromRelative) it calls action with its relativized form. This form is
 * obtained by calling "dateFromRelative" with the passed relative date and the
 * parsed query parameter.
 *
 * @param link the link
 * @param name the name of the query parameter
 * @param relative the date the parsed parameter is relative to
 * @param action the action to execute with it
 */
export function extractDateFromQuery(
  link: Route,
  name: string,
  relative: Date,
  action: (value: number) => void
): void {
  const queryValue = link.query[name]
  if (queryValue && typeof queryValue === 'string') {
    if (queryValue.match(/^([+-])?(\d|\.)+$/)) {
      action(parseFloat(queryValue))
      return
    }
    const relativeDate = dateFromRelative(queryValue, relative)
    if (relativeDate) {
      action(relativeDate.getTime())
    }
  }
}

export type ZoomDateRangeStore = {
  zoomXStartValue: number | null
  zoomXEndValue: number | null
  zoomYStartValue: number | null
  zoomYEndValue: number | null

  startTime: Date
  endTime: Date
}

/**
 * Parses the zoom and date range (start / end) from a given link and sets the
 * values on the passed store.
 *
 * @param link the link to parse it from
 * @param store the store to set it on
 */
export function parseAndSetZoomAndDateRange(
  link: Route,
  store: ZoomDateRangeStore
) {
  // Anchors to the current date
  extractDateFromQuery(link, 'zoomXEnd', new Date(), value => {
    store.zoomXEndValue = value
    store.endTime = roundDateUp(new Date(value))
  })
  // Anchors to the end date (or the current one if not specified)
  extractDateFromQuery(
    link,
    'zoomXStart',
    new Date(store.zoomXEndValue || new Date().getTime()),
    value => {
      store.zoomXStartValue = value
      store.startTime = roundDateDown(new Date(value))
    }
  )
  extractFloatFromQuery(link, 'zoomYStart', value => {
    store.zoomYStartValue = value
  })
  extractFloatFromQuery(link, 'zoomYEnd', value => {
    store.zoomYEndValue = value
  })
}
