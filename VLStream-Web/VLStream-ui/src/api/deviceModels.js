import request from '@/utils/request'

export const queryDeviceModels = (deviceId, options = {}) => request({
  url: '/vlsDeviceModels', method: 'get', params: { deviceId }, timeout: 35000, silentError: options.silentError === true
})

export const deleteDeviceModel = (deviceId, modelId) => request({
  url: '/vlsDeviceModels', method: 'delete', params: { deviceId, modelId }, timeout: 35000
})
