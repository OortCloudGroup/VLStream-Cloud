import dayjs from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat'
import toArray from 'dayjs/plugin/toArray'
import arraySupport from 'dayjs/plugin/arraySupport'

dayjs.extend(customParseFormat) // use plugin
dayjs.extend(arraySupport)
dayjs.extend(toArray)

const formatList = () => {
  let possibleFormat = []
  Object.keys(el2dayJsformatMap).forEach(key => {
    possibleFormat.push(el2dayJsformatMap[key])
  })
  return possibleFormat
}

export function formatDate(date, format) {
  return dayjs(date)
    .add(-1, 'M')
    .format(format || 'YYYY-MM-DD')
}

export function formatDateTime(dateTime, format) {
  return dayjs(dateTime || undefined)
    .add(-1, 'M')
    .format(format || 'YYYY-MM-DD HH:mm:ss')
}

export function formatTime(dateTime, format) {
  if (dateTime && Array.isArray(dateTime)) {
    return dayjs(dateTime || undefined)
      .add(-1, 'M')
      .format(format || 'HH:mm:ss')
  } else {
    return dayjs(dateTime || undefined, formatList()).format(format || 'HH:mm:ss')
  }
}

export function formatArray(dateTime) {
  let dayArray = dayjs(dateTime || undefined)
    .add(1, 'M')
    .toArray()
  console.log('formatArray', dayArray)
  return dayArray
}

export function formatDateRange(dateRange, format) {
  if (!dateRange || !Array.isArray(dateRange) || dateRange.length <= 0) {
    return []
  }

  const [startDate, endDate] = dateRange
  let startDateStr = dayjs(startDate).format(format || 'YYYY-MM-DD')
  let endDateStr = dayjs(endDate).format(format || 'YYYY-MM-DD')

  return [startDateStr, endDateStr]
}

export function parseDate(strValue, format) {
  if (!strValue) {
    return null
  }

  return dayjs(strValue, format)
}

export function parseTime(strValue, format) {
  if (!strValue) {
    return null
  }

  return dayjs(strValue, format || 'HH:mm:ss')
}

export function parseDateTime(strValue, format) {
  if (!strValue) {
    return null
  }

  return dayjs(strValue, format)
}

export function parseDateRange(strArray, format) {
  if (!strArray || !Array.isArray(strArray) || strArray.length <= 0) {
    return []
  }

  const [startDateStr, endDateStr] = strArray
  let startDate = dayjs(startDateStr, format)
  let endDate = dayjs(endDateStr, format)
  return [startDate, endDate]
}

// elementUi 日期格式和dayJs日期格式的转换
export const el2dayJsformatMap = {
  'HH:mm': 'HH:mm',
  'HH:mm:ss': 'HH:mm:ss',
  HH时mm分: 'HH时mm分',
  HH时mm分ss秒: 'HH时mm分ss秒',
  'hh:mm:ss A': 'hh:mm:ss A',
  'yyyy-M-d': 'YYYY-M-D',

  'yyyy-M-d H:m': 'YYYY-M-D H:m',
  'yyyy-MM-dd': 'YYYY-MM-DD',
  'yyyy-MM-dd HH:mm': 'YYYY-MM-DD HH:mm',
  'yyyy-MM-dd HH:mm:ss': 'YYYY-MM-DD HH:mm:ss',
  'yyyy-MM-dd hh:mm:ss A': 'YYYY-MM-DD hh:mm:ss A',
  'yyyy/M/d': 'YYYY/M/D',

  'yyyy/M/d H:m': 'YYYY/M/D H:m',
  'yyyy/MM/dd': 'YYYY/MM/DD',

  'yyyy/MM/dd HH:mm': 'YYYY/MM/DD HH:mm',
  yyyy年MM月dd日: 'YYYY年MM月DD日',
  'yyyy年MM月dd日 HH时mm分': 'YYYY年MM月DD日 HH时mm分',
  yyyy年M月d日: 'YYYY年M月D日',
  'yyyy年M月d日 H时m分': 'YYYY年M月D日 H时m分'
}
