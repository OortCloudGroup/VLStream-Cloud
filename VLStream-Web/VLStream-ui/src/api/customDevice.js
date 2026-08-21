import request, { downloadWvp } from '@/utils/wvpRequest'

export const listCustomDevices = params => request({ url: '/custom/device/list', method: 'get', params })
export const getCustomDevice = id => request({ url: `/custom/device/${id}`, method: 'get' })
export const addCustomDevice = data => request({ url: '/custom/device', method: 'post', data })
export const updateCustomDevice = data => request({ url: '/custom/device', method: 'put', data })
export const deleteCustomDevices = ids => request({ url: `/custom/device/${ids}`, method: 'delete' })
export const previewCustomDevice = id => request({ url: `/custom/device/${id}/preview`, method: 'post' })
export const getCustomMediaStatus = () => request({ url: '/custom/device/media/status', method: 'get' })
export const startCustomRecord = id => request({ url: `/custom/device/${id}/record/start`, method: 'post' })
export const stopCustomRecord = id => request({ url: `/custom/device/${id}/record/stop`, method: 'post' })
export const getCustomRecordStatus = id => request({ url: `/custom/device/${id}/record/status`, method: 'get' })
export const getCustomRecordPlan = id => request({ url: `/custom/device/${id}/record-plan`, method: 'get' })
export const saveCustomRecordPlan = (id, data) => request({ url: `/custom/device/${id}/record-plan`, method: 'put', data })
export const exportCustomDevices = params => downloadWvp(
  '/custom/device/export',
  params,
  `自定义协议设备_${Date.now()}.xlsx`
)
