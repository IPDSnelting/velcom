/**
 * Formats a duration into an "hh:mm:ss" string.
 *
 * @export
 * @param {Date} start the start date
 * @param {Date} end the end date
 */
export function formatDuration(start: Date, end: Date) {
  const differenceMillis = Math.abs(end.getTime() - start.getTime())
  let remainingDifferenceSeconds = differenceMillis / 1000

  const hours = Math.floor(remainingDifferenceSeconds / (60 * 60))
  remainingDifferenceSeconds -= hours * 60 * 60
  const minutes = Math.floor(remainingDifferenceSeconds / 60)
  remainingDifferenceSeconds -= minutes * 60

  const hoursFormatted = leftZeroPad(2, hours)
  const minutesFormatted = leftZeroPad(2, minutes)
  const secondsFormatted = leftZeroPad(2, remainingDifferenceSeconds)

  return `${hoursFormatted}:${minutesFormatted}:${secondsFormatted}`
}

/**
 * Formats a date.
 *
 * @export
 * @param {number | Date} date the date as an epoch seconds timestamp
 * @returns {string} the formatted date
 */
export function formatDate(date: number | Date): string {
  let myDate = date instanceof Date ? date : getDate(date)

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
 * @param {number | Date} date the date to format
 * @returns {string} the formatted date
 */
export function formatDateUTC(date: number | Date): string {
  let myDate = date instanceof Date ? date : getDate(date)

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
