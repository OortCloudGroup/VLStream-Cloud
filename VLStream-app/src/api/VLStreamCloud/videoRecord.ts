import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T = any, K = any>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'vls-server/' + interfaceName,
    method: method,
    data: data
  })
}

/**
 * 视频录制记录 API
 */

// 分页查询视频录制记录
export function getVideoRecordPage(params) {
  return commonFunc('/vlsVideoRecord/page', params, 'GET')
}

// 根据ID获取视频录制记录
export function getVideoRecordById(id) {
  return commonFunc(`/vlsVideoRecord/${id}`, {}, 'GET')
}

// 创建视频录制记录
export function createVideoRecord(data) {
  return commonFunc('/vlsVideoRecord', data)
}

// 更新视频录制记录
export function updateVideoRecord(id, data) {
  return commonFunc(`/vlsVideoRecord/${id}`, data, 'PUT')
}

// 删除视频录制记录
export function deleteVideoRecord(id) {
  return commonFunc(`/vlsVideoRecord/${id}`, {}, 'DELETE')
}

// 批量删除视频录制记录
export function batchDeleteVideoRecords(ids) {
  return commonFunc('/vlsVideoRecord/batch', ids, 'DELETE')
}

// 开始录制
export function startRecording(deviceId, deviceName = '设备', duration = 600, quality = 'medium') {
  return commonFunc(
    '/vlsVideoRecord/start',
    { deviceId, deviceName, duration, quality },
    'POST'
  )
}

// 停止录制
export function stopRecording(recordId) {
  return commonFunc(`/vlsVideoRecord/stop/${recordId}`, {}, 'POST')
}

// 获取录制状态
export function getRecordingStatus(deviceId) {
  return commonFunc(`/vlsVideoRecord/status/${deviceId}`, {}, 'GET')
}

// 获取录制统计信息
export function getRecordingStatistics() {
  return commonFunc('/vlsVideoRecord/statistics', {}, 'GET')
}

// 获取设备录制记录
export function getDeviceRecords(deviceId, params) {
  return commonFunc(`/vlsVideoRecord/device/${deviceId}`, params, 'GET')
}

// 下载录制文件
export function downloadRecordFile(id) {
  return request({
    url: config.URL + config.gateWay + 'vls-server/' + `/vlsVideoRecord/${id}/download`,
    method: 'get',
    data: {},
    responseType: 'blob'
  })
}

// 预览录制文件
export function previewRecordFile(id) {
  return commonFunc(`/vlsVideoRecord/${id}/preview`, {}, 'GET')
}

// 获取录制文件
export function getRecordFile(filePath) {
  return commonFunc(
    `/vlsVideoRecord/file/${encodeURIComponent(filePath)}`,
    {},
    'GET'
  )
}

// 获取录制文件缩略图
export function getRecordThumbnail(filePath) {
  return commonFunc(
    `/vlsVideoRecord/thumbnail/${encodeURIComponent(filePath)}`,
    {},
    'GET'
  )
}
