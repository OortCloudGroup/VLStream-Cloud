
import { request } from '@/utils/service'
import config from '@/config'
import qs from 'qs'
function commonFunc<T, K>(interfaceName: string, data: T, method: string, isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request < K >({
    url: config.URL + config.gateWay + 'zc-smartcity-server/admin-api' + interfaceName,
    method: method,
    ...params
  })
}

//  getGEOM: async(params: any) => {
//     return await request.get({ url: '/smartCity/GCCommon/getGEOM', params })
//   },

type RequestType ={
  url: string,
  params?: any,
  data?: any
}

export default {
  post: async <T = any>(data: RequestType) => {
    let res = await commonFunc(data.url, data?.params || data?.data || {}, 'POST')
    return res.data as unknown as T
  },
  get: async <T = any>(data: RequestType) => {
    if (data.url.includes('?')) {
      data.url += '&' + qs.stringify(data.params, { allowDots: true })
    } else {
      data.url += '?' + qs.stringify(data.params, { allowDots: true })
    }
    let res = await commonFunc(data.url, {}, 'GET')
    return res.data as unknown as T
  },
  put: async <T = any>(data: RequestType) => {
    let res = await commonFunc(data.url, data?.params || data?.data || {}, 'PUT')
    return res.data as unknown as T
  },
  delete: async <T = any>(data: RequestType) => {
    let res = await commonFunc(data.url, data?.params || {}, 'DELETE')
    return res.data as unknown as T
  }
}
