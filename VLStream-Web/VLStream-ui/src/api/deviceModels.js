import request from '@/utils/request'

export const queryDeviceModels = deviceId => request({
  url: '/vlsDeviceModels', method: 'get', params: { deviceId }, timeout: 35000
})

export const deleteDeviceModel = (deviceId, modelId) => request({
  url: '/vlsDeviceModels', method: 'delete', params: { deviceId, modelId }, timeout: 35000
})
