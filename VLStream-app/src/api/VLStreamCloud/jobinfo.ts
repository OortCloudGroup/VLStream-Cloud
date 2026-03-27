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
  return commonFunc('/blade-job/job-info/list', { ...params, current, size }, 'GET')
}

export const getDetail = id => {
  return commonFunc('/blade-job/job-info/detail', { id }, 'GET')
}

export const remove = ids => {
  return commonFunc('/blade-job/job-info/remove', { ids })
}

export const add = row => {
  return commonFunc('/blade-job/job-info/submit', row)
}

export const update = row => {
  return commonFunc('/blade-job/job-info/submit', row)
}

export const change = row => {
  return commonFunc('/blade-job/job-info/change', { id: row.id, enable: row.enable })
}

export const run = row => {
  return commonFunc('/blade-job/job-info/run', { id: row.id })
}

export const sync = row => {
  return commonFunc('/blade-job/job-info/sync', row)
}
