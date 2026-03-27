/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:45:18
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-04-12 17:06:18
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'

function commonFuncC<T, K>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-comment/' + interfaceName,
    method: method,
    data: data
  })
}

// 新增评论
export function commentAdd(data) {
  return commonFuncC('/api/v1/commentAdd', data)
}

// 按UID删除评论
export function commentDelByUID(data) {
  return commonFuncC('/api/v1/commentDelByUID', data)
}

// 删除评论
export function commentDelete(data) {
  return commonFuncC('/api/v1/commentDel', data)
}

// 评论列表
export function commentList(data) {
  return commonFuncC('/api/v1/commentList', data)
}

// 创建项目
export function createTag(data) {
  return commonFuncC('/api/v1/createTag', data)
}
