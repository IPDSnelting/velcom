/**
 * Formats a duration into a human readable string.
 *
 * @export
 * @param {Date} start the start date
 * @param {Date} end the end date
 */
export function formatDurationHuman(start: Date, end: Date): string {
  const [hours, minutes, seconds] = durationToParts(start, end)
  let result = ''

  if (hours === 0 && minutes === 0 && seconds === 0) {
    return '0 seconds'
  }

  if (hours > 0) {
    result += `${hours} hours`
  }
  if (minutes > 0) {
    result += result.length > 0 ? ' and ' : ''
    result += `${minutes} minutes`
  }
  if (seconds > 0) {
    result += result.length > 0 ? ' and ' : ''
    result += `${seconds} seconds`
  }

  return result
}

/**
 * Formats a duration into short string.
 *
 * @export
 * @param {Date} start the start date
 * @param {Date} end the end date
 */
export function formatDurationShort(start: Date, end: Date): string {
  const [hours, minutes, seconds] = durationToParts(start, end)

  const hoursString = leftZeroPad(2, hours)
  const minutesString = leftZeroPad(2, minutes)
  const secondsString = leftZeroPad(2, seconds)

  return `${hoursString}:${minutesString}:${secondsString}`
}

function durationToParts(start: Date, end: Date): [number, number, number] {
  const differenceMillis = Math.abs(end.getTime() - start.getTime())
  let remainingDifferenceSeconds = differenceMillis / 1000

  const hours = Math.floor(remainingDifferenceSeconds / (60 * 60))
  remainingDifferenceSeconds -= hours * 60 * 60
  const minutes = Math.floor(remainingDifferenceSeconds / 60)
  remainingDifferenceSeconds -= minutes * 60

  return [hours, minutes, Math.floor(remainingDifferenceSeconds)]
}

/**
 * Formats a date.
 *
 * @export
 * @param {number | Date} date the date as an epoch seconds timestamp
 * @returns {string} the formatted date
 */
export function formatDate(date: number | Date): string {
  const myDate = date instanceof Date ? date : getDate(date)

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
  const myDate = date instanceof Date ? date : getDate(date)

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
 * Converts a relative date "<number>d" or "<number>w" or "<number>m" or
 * "<number>y" to a Date.
 *
 * @param relative the input string
 * @param anchor the anchor it is relative to
 */
export function dateFromRelative(
  relative: string,
  anchor: Date = new Date()
): Date | undefined {
  const number: number = parseFloat(relative.substring(0, relative.length - 1))

  let multiplier: number
  if (relative.endsWith('d')) {
    // millis * seconds in minute * minutes in hour * day
    multiplier = 1000 * 60 * 60 * 24
  } else if (relative.endsWith('w')) {
    // millis * seconds in minute * minutes in hour * day * 7
    multiplier = 1000 * 60 * 60 * 24 * 7
  } else if (relative.endsWith('m')) {
    // millis * seconds in minute * minutes in hour * day * 30
    multiplier = 1000 * 60 * 60 * 24 * 30
  } else if (relative.endsWith('y')) {
    // millis * seconds in minute * minutes in hour * day * 365
    multiplier = 1000 * 60 * 60 * 24 * 465
  } else {
    return undefined
  }

  if (isNaN(number)) {
    return undefined
  }
  return new Date(anchor.getTime() + number * multiplier)
}

/**
 * Converts an epoch seconds timestamp to a date.
 *
 * @export
 * @param {number} date the epoch seconds
 * @returns {Date} the matching date
 */
export function getDate(date: number): Date {
  const myDate = new Date()
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
