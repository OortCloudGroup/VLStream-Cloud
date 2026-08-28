/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

// device

import { ElMessage } from 'element-plus'
import { isCameraRtcStream } from '@/utils/oplayer'

/**
 * Format
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
 * Get
 * @param {string} streamUrl
 * @returns {string}
 */
export function getStreamType(streamUrl) {
  if (!streamUrl) return 'unknown'

  const url = String(streamUrl).trim().toLowerCase()

  if (url.includes('/aety-') || isCameraRtcStream(streamUrl)) {
    return 'cameraRTC'
  }

  if (url.includes('youtube.com') || url.includes('youtu.be')) {
    return 'youtube'
  } else if (url.startsWith('rtsp://')) {
    return 'rtsp'
  } else if (/\.m3u8($|[?#])/i.test(url)) {
    return 'hls'
  } else if (/\.flv($|[?#])/i.test(url)) {
    return 'flv'
  } else if (/\.(mp4|avi|mov|wmv|mkv)($|[?#])/i.test(url)) {
    return 'video'
  } else if (url.startsWith('http')) {
    return 'http'
  }

  return 'unknown'
}

/**
 * Get YouTube URL
 * @param {string} url
 * @returns {string}
 */
export function getYouTubeEmbedUrl(url) {
  if (!url) return ''

  // if already is URL, parametercorrect
  if (url.includes('/embed/')) {
    // ID
    const embedMatch = url.match(/\/embed\/([^?]+)/)
    if (embedMatch && embedMatch[1]) {
      const videoId = embedMatch[1]
      return `https://www.youtube.com/embed/${videoId}?autoplay=1&mute=1&controls=1&enablejsapi=1`
    }
    return url // if method Parse , URL
  }

  // Process YouTube URL
  let videoId = ''
  if (url.includes('youtube.com/watch?v=')) {
    videoId = url.split('v=')[1]?.split('&')[0]
  } else if (url.includes('youtu.be/')) {
    videoId = url.split('youtu.be/')[1]?.split('?')[0]
  }

  return videoId ? `https://www.youtube.com/embed/${videoId}?autoplay=1&mute=1&controls=1&enablejsapi=1` : ''
}

/**
 * URL
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
    //
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
 * VLC
 * @param {string} streamUrl
 */
export function openInVlc(streamUrl) {
  if (!streamUrl) {
    ElMessage.warning('暂无视频流地址')
    return
  }

  // VLC
  const vlcUrl = `vlc://${streamUrl}`
  window.open(vlcUrl, '_blank')
}

/**
 * URL
 * @param {string} url
 * @returns {boolean}
 */
export function validateStreamUrl(url) {
  if (!url) return false

  const urlPattern = /^(https?|rtsp|rtmp|wss?):\/\/[^\s/$.?#].[^\s]*$/i
  return urlPattern.test(url)
}

/**
 * Get device
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
 * Generate device
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
 *
 * @param {string|number} longitude
 * @param {string|number} latitude
 * @returns {boolean}
 */
export function validateCoordinates(longitude, latitude) {
  // Parse
  const parsedLng = parseCoordinate(longitude)
  const parsedLat = parseCoordinate(latitude)

  return parsedLng !== null && parsedLat !== null &&
         parsedLng >= -180 && parsedLng <= 180 &&
         parsedLat >= -90 && parsedLat <= 90
}

/**
 * Parse ,
 * @param {string|number} coordinate
 * @returns {number|null}
 */
export function parseCoordinate(coordinate) {
  if (!coordinate) return null

  // if already is ,
  const num = parseFloat(coordinate)
  if (!isNaN(num)) return num

  const coordStr = coordinate.toString().trim()

  // : 39°54′26″N 116°23′29″E
  const dmsPattern = /(\d+)°(\d+)′(\d+)″([NSEW])/i
  const match = coordStr.match(dmsPattern)

  if (match) {
    const degrees = parseInt(match[1])
    const minutes = parseInt(match[2])
    const seconds = parseInt(match[3])
    const direction = match[4].toUpperCase()

    // Convert to
    let decimal = degrees + minutes / 60 + seconds / 3600

    //
    if (direction === 'S' || direction === 'W') {
      decimal = -decimal
    }

    return decimal
  }

  // : 39°54′N 116°23′E
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

  // : 39°N 116°E
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
 * Parse
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
 * Generate array (24 )
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
 * whether to in
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
 *
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

  //
  return mergeTimeRanges(newRanges)
}

/**
 *
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
 * all
 * @returns {Array}
 */
export function clearTimeSelection() {
  return []
}

/**
 * configuration
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
