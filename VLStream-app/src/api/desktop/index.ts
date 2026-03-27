/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:46:36
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:46:36
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'
// import config from '@/config'
import { getToken } from '@/utils/cache/cookies'
function commonFunc<T, K>(interfaceName: string, data: T, method: string, isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  config.headers.authorization = getToken()
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-workflowforms' + interfaceName,
    method: method,
    ...params
  })
}

// 代我处理的
export function listTodoProcess(data) {
  return commonFunc('/workflow/process/todoList', data, 'get')
}
