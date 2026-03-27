/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:46:10
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:46:10
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method: string) {
  return request<K>({
    url: config.URL + config.gateWay + 'oortcloud-unified-msg' + interfaceName,
    method: method,
    data: data
  })
}

/* 新增渠道 */
export function add_channel(data: any) {
  return commonFunc('/api/v1/add_channel', data, 'post')
}

/* 渠道删除 */
export function del_channel(data: any) {
  return commonFunc('/api/v1/del_channel', data, 'post')
}

/* 渠道更新 */
export function edit_channel(data: any) {
  return commonFunc('/api/v1/edit_channel', data, 'post')
}

/* 渠道列表 */
export function channel_list(data: any) {
  return commonFunc('/api/v1/channel_list', data, 'post')
}

/* 渠道信息 */
export function channel_info(data: any) {
  return commonFunc('/api/v1/channel_info', data, 'post')
}

/* 渠道类型列表 */
export function get_channel_type(data: any) {
  return commonFunc('/api/v1/get_channel_type', data, 'post')
}

/* 添加 消息模板 */
export function add_template(data: any) {
  return commonFunc('/api/v1/add_template', data, 'post')
}

/* 模板删除 */
export function del_template(data: any) {
  return commonFunc('/api/v1/del_template', data, 'post')
}

/* 模板更新 */
export function edit_template(data: any) {
  return commonFunc('/api/v1/edit_template', data, 'post')
}

/* 模板列表 */
export function template_list(data: any) {
  return commonFunc('/api/v1/template_list', data, 'post')
}

/* 模板审核审核 */
export function check_template(data: any) {
  return commonFunc('/api/v1/check_template', data, 'post')
}

/* 消息发送send */
export function msgSend(data: any) {
  return commonFunc('/api/v1/send', data, 'post')
}

/* 消息发送 */
export function add_msg(data: any) {
  return commonFunc('/api/v1/add_msg', data, 'post')
}

/* 消息删除 */
export function del_msg(data: any) {
  return commonFunc('/api/v1/del_msg', data, 'post')
}

/* 消息列表数据 */
export function msg_list(data: any) {
  return commonFunc('/api/v1/msg_list', data, 'post')
}

/* 消息查询 */
export function msg_query(data: any) {
  return commonFunc('/api/v1/query', data, 'post')
}

/* 审核 */
export function get_bind_info(data: any) {
  return commonFunc('/api/v1/get_bind_info', data, 'post')
}

/* 数据看板 */
export function statistics(data: any) {
  return commonFunc('/api/v1/statistics', data, 'post')
}

