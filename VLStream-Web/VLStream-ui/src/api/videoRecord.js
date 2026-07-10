import request from '@/utils/request'

/**
 * 视频录制记录 API
 */

// 分页查询视频录制记录
export function getVideoRecordPage(params) {
  return request({
    url: '/vlsVideoRecord/page',
    method: 'get',
    params
  })
}

// 根据ID获取视频录制记录
export function getVideoRecordById(id) {
  return request({
    url: `/vlsVideoRecord/${id}`,
    method: 'get'
  })
}

// 创建视频录制记录
export function createVideoRecord(data) {
  return request({
    url: '/vlsVideoRecord',
    method: 'post',
    data
  })
}

// 更新视频录制记录
export function updateVideoRecord(id, data) {
  return request({
    url: `/vlsVideoRecord/${id}`,
    method: 'put',
    data
  })
}

// 删除视频录制记录
export function deleteVideoRecord(id) {
  return request({
    url: `/vlsVideoRecord/${id}`,
    method: 'delete'
  })
}

// 批量删除视频录制记录
export function batchDeleteVideoRecords(ids) {
  return request({
    url: '/vlsVideoRecord/batch',
    method: 'delete',
    data: ids
  })
}

// 开始录制
export function startRecording(deviceId, deviceName = '设备', duration = 600, quality = 'medium') {
  return request({
    url: `/vlsVideoRecord/start`,
    method: 'post',
    params: {
      deviceId,
      deviceName,
      duration,
      quality
    }
  })
}

// 停止录制 
export function stopRecording(recordId) {
  return request({
    url: `/vlsVideoRecord/stop/${recordId}`,
    method: 'post'
  })
}

// 获取录制状态
export function getRecordingStatus(deviceId) {
  return request({
    url: `/vlsVideoRecord/status/${deviceId}`,
    method: 'get'
  })
}

// 获取录制统计信息
export function getRecordingStatistics() {
  return request({
    url: '/vlsVideoRecord/statistics',
    method: 'get'
  })
}

// 获取设备录制记录
export function getDeviceRecords(deviceId, params) {
  return request({
    url: `/vlsVideoRecord/device/${deviceId}`,
    method: 'get',
    params
  })
}

// 下载录制文件
export function downloadRecordFile(id) {
  return request({
    url: `/vlsVideoRecord/${id}/download`,
    method: 'get',
    responseType: 'blob'
  })
}

// 预览录制文件
export function previewRecordFile(id) {
  return request({
    url: `/vlsVideoRecord/${id}/preview`,
    method: 'get'
  })
}

// 获取录制文件
export function getRecordFile(filePath) {
  return request({
    url: `/vlsVideoRecord/file/${encodeURIComponent(filePath)}`,
    method: 'get'
  })
}

// 获取录制文件缩略图
export function getRecordThumbnail(filePath) {
  return request({
    url: `/vlsVideoRecord/thumbnail/${encodeURIComponent(filePath)}`,
    method: 'get'
  })
}
