import request from '@/utils/wvpRequest'
import { loadVideoDeviceCatalog } from '@/utils/videoSquareCatalog'

export const getVideoDeviceCatalog = ({ signal, timeout, silentError } = {}) => loadVideoDeviceCatalog(params => request({
  url: '/video/device/list',
  method: 'get',
  params,
  signal,
  timeout,
  silentError,
}))
