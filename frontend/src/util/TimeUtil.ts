/**
 * Formats a date.
 *
 * @export
 * @param {number} date the date as an epoch seconds timestamp
 * @returns {string} the formatted date
 */
export function formatDate(date: number): string {
  let myDate = getDate(date)

  let resultString: string = myDate.getFullYear() + ''
  resultString += '-' + leftZeroPad(2, myDate.getMonth() + 1)
  resultString += '-' + leftZeroPad(2, myDate.getDate())

  resultString += ' '
  resultString += leftZeroPad(2, myDate.getHours()) + ':'
  resultString += leftZeroPad(2, myDate.getMinutes())

  return resultString
}

/**
 * Formats a date relative to UTC time (or appends a zone offset).
 *
 * @export
 * @param {number} date the date to format
 * @returns {string} the formatted date
 */
export function formatDateUTC(date: number): string {
  let myDate = getDate(date)

  let resultString: string = myDate.getFullYear() + ''
  resultString += '-' + leftZeroPad(2, myDate.getUTCMonth() + 1)
  resultString += '-' + leftZeroPad(2, myDate.getUTCDate())

  resultString += ' '
  resultString += leftZeroPad(2, myDate.getUTCHours()) + ':'
  resultString += leftZeroPad(2, myDate.getUTCMinutes())

  resultString += ' UTC'

  return resultString
}

/**
 * Converts an epoch seconds timestamp to a date.
 *
 * @export
 * @param {number} date the epoch seconds
 * @returns {Date} the matching date
 */
export function getDate(date: number): Date {
  let myDate = new Date()
  // Time takes an epoch MILLIS string
  myDate.setTime(date * 1000)
  return myDate
}

function leftZeroPad(length: number, input: number) {
  let asString = input + ''
  while (asString.length < length) {
    asString = '0' + asString
  }
  return asString
}
