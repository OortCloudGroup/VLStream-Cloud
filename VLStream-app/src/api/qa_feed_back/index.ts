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

function commonFunc<T, K>(interfaceName: string, data: T, method: string, type = false) {
  let params = type ? { params: data } : { data: data }
  return request<K>({
    // url: 'http://192.168.88.56:8099' + interfaceName,
    url: config.URL + config.gateWay + 'apaas-feedback/' + interfaceName,
    method: method,
    ...params
  })
}

// 问题列表
export function list(data) {
  return commonFunc('issue/v1/list', data, 'POST')
}

// 问题详情
export function detail(data) {
  return commonFunc('issue/v1/detail', data, 'POST')
}

// 添加问题
export function add(data) {
  return commonFunc('issue/v1/add', data, 'POST')
}

// 关注某个问题
export function attention(data) {
  return commonFunc('issue/v1/attention', data, 'POST')
}

// 删除一个或者多个问题
export function deleteQA(data) {
  return commonFunc('issue/v1/delete', data, 'POST')
}

// 回答某个问题
export function solve(data) {
  return commonFunc('issue/v1/solve', data, 'POST')
}

// 写评论
export function reply(data) {
  return commonFunc('issue/v1/reply', data, 'POST')
}

// 删除评论
export function replydelete(data) {
  return commonFunc('issue/v1/replydelete', data, 'POST')
}

// 查看某个问题所有的评论
export function replylist(data) {
  return commonFunc('issue/v1/replylist', data, 'POST')
}

// 某个问题已解决未解决
export function solve_ack(data) {
  return commonFunc('issue/v1/solve_ack', data, 'POST')
}
