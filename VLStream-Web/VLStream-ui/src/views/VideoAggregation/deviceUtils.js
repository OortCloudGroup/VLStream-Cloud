// 设备管理工具函数

import { ElMessage } from 'element-plus'
import { isCameraRtcStream } from '@/utils/oplayer'

/**
 * 格式化日期时间
 * @param {*} row 
 * @param {*} column 
 * @param {*} cellValue 
 * @returns 
 */
export function formatDateTime(row, column, cellValue) {
  if (!cellValue) return '-'
  const date = new Date(cellValue)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

/**
 * 获取流类型
 * @param {string} streamUrl 
 * @returns {string}
 */
export function getStreamType(streamUrl) {
  if (!streamUrl) return 'unknown'
  
  if (isCameraRtcStream(streamUrl)) {
    return 'cameraRTC'
  }

  const url = streamUrl.toLowerCase()
  
  if (url.includes('youtube.com') || url.includes('youtu.be')) {
    return 'youtube'
  } else if (url.startsWith('rtsp://')) {
    return 'rtsp'
  } else if (url.includes('.m3u8')) {
    return 'hls'
  } else if (url.startsWith('http')) {
    return 'http'
  } else if (url.match(/\.(mp4|avi|mov|wmv|flv|mkv)$/)) {
    return 'video'
  }
  
  return 'unknown'
}

/**
 * 获取YouTube嵌入URL
 * @param {string} url 
 * @returns {string}
 */
export function getYouTubeEmbedUrl(url) {
  if (!url) return ''
  
  // 如果已经是嵌入格式的URL，直接返回但确保参数正确
  if (url.includes('/embed/')) {
    // 提取视频ID
    const embedMatch = url.match(/\/embed\/([^?]+)/)
    if (embedMatch && embedMatch[1]) {
      const videoId = embedMatch[1]
      return `https://www.youtube.com/embed/${videoId}?autoplay=1&mute=1&controls=1&enablejsapi=1`
    }
    return url // 如果无法解析，返回原URL
  }
  
  // 处理标准YouTube URL
  let videoId = ''
  if (url.includes('youtube.com/watch?v=')) {
    videoId = url.split('v=')[1]?.split('&')[0]
  } else if (url.includes('youtu.be/')) {
    videoId = url.split('youtu.be/')[1]?.split('?')[0]
  }
  
  return videoId ? `https://www.youtube.com/embed/${videoId}?autoplay=1&mute=1&controls=1&enablejsapi=1` : ''
}

/**
 * 复制流URL到剪贴板
 * @param {string} streamUrl 
 */
export function copyStreamUrl(streamUrl) {
  if (!streamUrl) {
    ElMessage.warning('暂无视频流地址')
    return
  }
  
  navigator.clipboard.writeText(streamUrl).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    // 降级方案
    const textArea = document.createElement('textarea')
    textArea.value = streamUrl
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
    ElMessage.success('已复制到剪贴板')
  })
}

/**
 * 使用VLC打开流
 * @param {string} streamUrl 
 */
export function openInVlc(streamUrl) {
  if (!streamUrl) {
    ElMessage.warning('暂无视频流地址')
    return
  }
  
  // 创建VLC协议链接
  const vlcUrl = `vlc://${streamUrl}`
  window.open(vlcUrl, '_blank')
}

/**
 * 验证视频流URL格式
 * @param {string} url 
 * @returns {boolean}
 */
export function validateStreamUrl(url) {
  if (!url) return false
  
  const urlPattern = /^(https?|rtsp|rtmp|wss?):\/\/[^\s/$.?#].[^\s]*$/i
  return urlPattern.test(url)
}

/**
 * 获取设备状态样式
 * @param {number} status 
 * @returns {object}
 */
export function getDeviceStatusStyle(status) {
  const styles = {
    1: { type: 'success', text: '在线' },
    0: { type: 'danger', text: '离线' },
    2: { type: 'warning', text: '故障' }
  }
  
  return styles[status] || { type: 'info', text: '未知' }
}

/**
 * 生成设备默认名称
 * @param {string} deviceId 
 * @param {string} tag 
 * @returns {string}
 */
export function generateDeviceName(deviceId, tag) {
  const prefix = tag || '设备'
  const suffix = deviceId ? `_${deviceId.slice(-4)}` : `_${Date.now().toString().slice(-4)}`
  return `${prefix}${suffix}`
}

/**
 * 验证经纬度格式
 * @param {string|number} longitude 
 * @param {string|number} latitude 
 * @returns {boolean}
 */
export function validateCoordinates(longitude, latitude) {
  // 尝试解析经纬度
  const parsedLng = parseCoordinate(longitude)
  const parsedLat = parseCoordinate(latitude)
  
  return parsedLng !== null && parsedLat !== null &&
         parsedLng >= -180 && parsedLng <= 180 && 
         parsedLat >= -90 && parsedLat <= 90
}

/**
 * 解析坐标，支持多种格式
 * @param {string|number} coordinate 
 * @returns {number|null}
 */
export function parseCoordinate(coordinate) {
  if (!coordinate) return null
  
  // 如果已经是数字，直接返回
  const num = parseFloat(coordinate)
  if (!isNaN(num)) return num
  
  const coordStr = coordinate.toString().trim()
  
  // 匹配度分秒格式：39°54′26″N 或 116°23′29″E
  const dmsPattern = /(\d+)°(\d+)′(\d+)″([NSEW])/i
  const match = coordStr.match(dmsPattern)
  
  if (match) {
    const degrees = parseInt(match[1])
    const minutes = parseInt(match[2])
    const seconds = parseInt(match[3])
    const direction = match[4].toUpperCase()
    
    // 转换为十进制度数
    let decimal = degrees + minutes / 60 + seconds / 3600
    
    // 根据方向调整符号
    if (direction === 'S' || direction === 'W') {
      decimal = -decimal
    }
    
    return decimal
  }
  
  // 匹配度分格式：39°54′N 或 116°23′E
  const dmPattern = /(\d+)°(\d+)′([NSEW])/i
  const dmMatch = coordStr.match(dmPattern)
  
  if (dmMatch) {
    const degrees = parseInt(dmMatch[1])
    const minutes = parseInt(dmMatch[2])
    const direction = dmMatch[3].toUpperCase()
    
    let decimal = degrees + minutes / 60
    
    if (direction === 'S' || direction === 'W') {
      decimal = -decimal
    }
    
    return decimal
  }
  
  // 匹配度格式：39°N 或 116°E
  const degreePattern = /(\d+)°([NSEW])/i
  const degMatch = coordStr.match(degreePattern)
  
  if (degMatch) {
    const degrees = parseInt(degMatch[1])
    const direction = degMatch[2].toUpperCase()
    
    let decimal = degrees
    
    if (direction === 'S' || direction === 'W') {
      decimal = -decimal
    }
    
    return decimal
  }
  
  return null
}

/**
 * 解析时间范围选择
 * @param {Array} timeRanges 
 * @returns {Array}
 */
export function parseTimeRanges(timeRanges) {
  if (!Array.isArray(timeRanges)) return []
  
  return timeRanges.map(range => ({
    start: range.start || 0,
    end: range.end || 23
  }))
}

/**
 * 生成时间段数组（24小时制）
 * @param {Array} selectedHours 
 * @returns {Array}
 */
export function generateTimeSegments(selectedHours = []) {
  const segments = []
  for (let i = 0; i < 24; i++) {
    segments.push({
      hour: i,
      selected: selectedHours.includes(i),
      label: i.toString().padStart(2, '0') + ':00'
    })
  }
  return segments
}

/**
 * 检查是否为选中的时间
 * @param {Array} timeRanges 
 * @param {number} hour 
 * @returns {boolean}
 */
export function isTimeSelected(timeRanges, hour) {
  if (!Array.isArray(timeRanges)) return false
  
  return timeRanges.some(range => 
    hour >= range.start && hour <= range.end
  )
}

/**
 * 添加时间范围
 * @param {Array} timeRanges 
 * @param {number} startHour 
 * @param {number} endHour 
 * @returns {Array}
 */
export function addTimeRange(timeRanges, startHour, endHour) {
  const newRanges = [...timeRanges]
  newRanges.push({
    start: Math.min(startHour, endHour),
    end: Math.max(startHour, endHour)
  })
  
  // 合并重叠的时间段
  return mergeTimeRanges(newRanges)
}

/**
 * 合并重叠的时间段
 * @param {Array} timeRanges 
 * @returns {Array}
 */
export function mergeTimeRanges(timeRanges) {
  if (timeRanges.length <= 1) return timeRanges
  
  const sorted = timeRanges.sort((a, b) => a.start - b.start)
  const merged = [sorted[0]]
  
  for (let i = 1; i < sorted.length; i++) {
    const current = sorted[i]
    const last = merged[merged.length - 1]
    
    if (current.start <= last.end + 1) {
      last.end = Math.max(last.end, current.end)
    } else {
      merged.push(current)
    }
  }
  
  return merged
}

/**
 * 清除所有时间选择
 * @returns {Array}
 */
export function clearTimeSelection() {
  return []
}

/**
 * 复制时间配置
 * @param {Object} sourceConfig 
 * @param {Array} targetDays 
 * @param {Object} weeklyConfig 
 * @returns {Object}
 */
export function copyTimeConfig(sourceConfig, targetDays, weeklyConfig) {
  const newConfig = { ...weeklyConfig }
  
  targetDays.forEach(day => {
    newConfig[day] = [...sourceConfig]
  })
  
  return newConfig
} 
