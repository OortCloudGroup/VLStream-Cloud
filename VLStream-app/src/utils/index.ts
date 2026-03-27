// 距离转换函数
export function getConverDistance(meters: number): string {
  if (meters < 1000) {
    return `${Math.round(meters)}米`
  } else {
    return `${(meters / 1000).toFixed(2)}公里`
  }
}
// 时间格式化函数
export function formatDateToHM(timestamp: number): string {
  const date = new Date(timestamp)
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

export const covertSecondsToStr = (seconds: number) => {
  // 把时间秒，转换成 时分秒的中文形式 比如 60 ，转换成 1分钟，  50 转换成 50秒， 70 转换成 1分钟 10秒， 3600 转换成 1小时， 3600+60 转换成 1小时 1分钟
  if (seconds < 60) {
    return seconds + '秒'
  } else if (seconds >= 60 && seconds < 3600) {
    let min = Math.floor(seconds / 60)
    let sec = seconds % 60
    return min + '分钟' + sec + '秒'
  } else if (seconds >= 3600) {
    let hour = Math.floor(seconds / 3600)
    let min = Math.floor((seconds % 3600) / 60)
    let sec = (seconds % 3600) % 60
    return hour + '小时' + min + '分钟' + sec + '秒'
  } else {
    return '0秒'
  }
}

export function formatDateToFull(value) {
  if (!value) {
    return ''
  }
  if (value instanceof Date) {
    value = value.getTime()
  } else {
    let tempStr = value + ''
    if (tempStr.length !== 13) {
      value = value * 1000
    }
  }
  const date = new Date(value)
  const dateNumFun = (num) => num < 10 ? `0${num}` : num
  const [Y, M, D, h, m, s] = [
    date.getFullYear(),
    dateNumFun(date.getMonth() + 1),
    dateNumFun(date.getDate()),
    dateNumFun(date.getHours()),
    dateNumFun(date.getMinutes()),
    dateNumFun(date.getSeconds())
  ]
  return `${Y}-${M}-${D} ${h}:${m}:${s}`
}
