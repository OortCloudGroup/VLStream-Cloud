import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T = any, K = any>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'vls-server/' + interfaceName,
    method: method,
    data: data
  })
}

export const getList = (current, size, params) => {
  return commonFunc('/vlsSceneGovernance/list', { ...params, current, size }, 'GET')
}

export const remove = ids => {
  return commonFunc('/vlsSceneGovernance/remove', { ids })
}

export const add = row => {
  return commonFunc('/vlsSceneGovernance/submit', row)
}

export const update = row => {
  return commonFunc('/vlsSceneGovernance/submit', row)
}
