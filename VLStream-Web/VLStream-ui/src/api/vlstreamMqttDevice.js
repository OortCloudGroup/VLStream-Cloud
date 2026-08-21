import request from '@/utils/wvpRequest'

// VLStream 协议页面统一使用 WVP 的设备、媒体和预览能力，不回退 VLS 本地接口。
export const getMqttDevicePage = params => request({
  url: '/vlstream/device/list',
  method: 'get',
  params: {
    ...params,
    pageNum: params.pageNum ?? params.current,
    pageSize: params.pageSize ?? params.size
  }
})

export const getMqttDeviceStreams = deviceRowId => request({
  url: `/vlstream/device/${deviceRowId}/streams`,
  method: 'get'
})

export const getMqttDeviceDetail = deviceRowId => request({
  url: `/vlstream/device/${deviceRowId}/detail`,
  method: 'get'
})

export const deployMqttDeviceFirmware = (deviceRowId, firmwareId) => request({
  url: `/vlstream/device/${deviceRowId}/firmware-upgrades`,
  method: 'post',
  data: { firmwareId }
})

export const cancelMqttDeviceFirmwareTask = (deviceRowId, requestId) => request({
  url: `/vlstream/device/${deviceRowId}/firmware-upgrades/${requestId}/cancel`,
  method: 'post'
})

export const createMqttDevicePreview = (deviceRowId, streamId) => request({
  url: `/vlstream/device/${deviceRowId}/preview`,
  method: 'post',
  data: { streamId }
})

export const getMqttDeviceMediaStatus = () => request({
  url: '/vlstream/device/media/status',
  method: 'get'
})
