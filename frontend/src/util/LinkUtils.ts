/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import { PermanentLinkOptions } from '@/store/modules/detailGraphStore'

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
