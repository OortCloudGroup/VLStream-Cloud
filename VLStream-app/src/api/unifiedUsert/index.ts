/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:47:14
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:47:14
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    // url: config.URL + config.gateWay + 'apaas-user' + interfaceName,
    url: config.URL + config.gateWay + 'apaas-user/' + interfaceName,
    method: method,
    data: data
  })
}

// 获取部门
export function deptList(data) {
  // return commonFunc('/user/v1/deptList', data)
  return commonFunc('tenant/v1/list', data)
}
