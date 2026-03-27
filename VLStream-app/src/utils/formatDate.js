/**
 * 时间戳转换为友好时间格式
 * @param {number|string} value - 时间戳（10位或13位）
 * @returns {string} 格式化后的时间字符串
 */
export function formatDate(value) {
  if (!value) {
    return ''
  }

  // 处理时间戳（支持10位和13位）
  let timestamp = Number(value)
  if (timestamp.toString().length === 10) {
    timestamp *= 1000
  }

  const now = Date.now()
  const diff = now - timestamp

  // 刚刚（5秒内）
  if (diff < 5 * 1000) return '刚刚'

  // 秒前
  if (diff < 60 * 1000) {
    const seconds = Math.floor(diff / 1000)
    return `${seconds}秒前`
  }

  // 分钟前
  if (diff < 60 * 60 * 1000) {
    const minutes = Math.floor(diff / (60 * 1000))
    return `${minutes}分钟前`
  }

  // 小时前
  if (diff < 24 * 60 * 60 * 1000) {
    const hours = Math.floor(diff / (60 * 60 * 1000))
    return `${hours}小时前`
  }

  // 完整日期格式化
  const padZero = num => num < 10 ? `0${num}` : num
  const date = new Date(timestamp)

  const year = date.getFullYear()
  const month = padZero(date.getMonth() + 1)
  const day = padZero(date.getDate())
  const hours = padZero(date.getHours())
  const minutes = padZero(date.getMinutes())
  const seconds = padZero(date.getSeconds())

  // 不同年份显示完整日期
  return date.getFullYear() !== new Date().getFullYear()
    ? `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    : `${month}-${day} ${hours}:${minutes}:${seconds}`
}
