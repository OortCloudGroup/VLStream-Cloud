import request from '@/utils/request'

export const getMqttDevicePage = params => request({ url: '/vlsMqttDevice/page', method: 'get', params })
export const getMqttDeviceStreams = deviceId => request({ url: `/vlsMqttDevice/${deviceId}/streams`, method: 'get' })
export const getMqttDeviceDetail = deviceId => request({ url: `/vlsMqttDevice/${deviceId}/detail`, method: 'get' })
export const deployMqttDeviceFirmware = (deviceId, firmwareId) => request({
  url: `/vlsMqttDevice/${deviceId}/firmware-upgrades`, method: 'post', data: { firmwareId }
})
export const cancelMqttDeviceFirmwareTask = (deviceId, requestId) => request({
  url: `/vlsMqttDevice/${deviceId}/firmware-upgrades/${requestId}/cancel`, method: 'post'
})
export const createMqttDevicePreview = (deviceId, streamId) => request({
  url: `/vlsMqttDevice/${deviceId}/preview`, method: 'post', data: { streamId }
})
export const closeMqttDevicePreview = (deviceId, streamId) => request({
  url: `/vlsMqttDevice/${deviceId}/streams/${streamId}/preview`, method: 'delete'
})
export const getMqttDeviceMediaStatus = () => request({ url: '/vlsMqttDevice/media/status', method: 'get' })
