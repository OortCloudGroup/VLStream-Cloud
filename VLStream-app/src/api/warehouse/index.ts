/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:50:17
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:50:17
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'oortcloud-warehouse' + interfaceName,
    // url: config.URL + interfaceName,
    method: method,
    data: data
  })
}

// 添加分类
export function ckmtClassifyAdd(data) {
  return commonFunc('/api/v1/ckmt.classify.add', data)
}

// 分类树
export function ckmtClassifyAll(data) {
  return commonFunc('/api/v1/ckmt.classify.all', data)
}

// 删除分类
export function ckmtClassifyDel(data) {
  return commonFunc('/api/v1/ckmt.classify.del', data)
}

// 编辑分类
export function ckmtClassifyEdit(data) {
  return commonFunc('/api/v1/ckmt.classify.edit', data)
}

// 分类界面添加
export function ckmtClassifyInAdd(data) {
  return commonFunc('/api/v1/ckmt.classify.interface.add', data)
}

// 分类界面列表
export function ckmtClassifyInAll(data) {
  return commonFunc('/api/v1/ckmt.classify.interface.all', data)
}

// 分类界面删除
export function ckmtClassifyInDel(data) {
  return commonFunc('/api/v1/ckmt.classify.interface.del', data)
}

// 分类界面修改
export function ckmtClassifyInEdit(data) {
  return commonFunc('/api/v1/ckmt.classify.interface.edit', data)
}

// 获取当前生效的界面信息
export function ckmtClassifyInGet(data) {
  return commonFunc('/api/v1/ckmt.classify.interface.get', data)
}

// 分类界面详情
export function ckmtClassifyInInfo(data) {
  return commonFunc('/api/v1/ckmt.classify.interface.info', data)
}

export function gdmtInterfaceInfo(data) {
  return commonFunc('/api/v1/ckmt.classify.interface.info', data)
}

// 分类界面状态设置
export function ckmtClassifyInStatus(data) {
  return commonFunc('/api/v1/ckmt.classify.interface.status', data)
}

// 分类界面保存设计
export function ckmtClassifyInTem(data) {
  return commonFunc('/api/v1/ckmt.classify.interface.templet', data)
}

export function gdmtInterfaceTemplet(data) {
  return commonFunc('/api/v1/ckmt.classify.interface.templet', data)
}

// 分类列表
export function ckmtClassifylist(data) {
  return commonFunc('/api/v1/ckmt.classify.list', data)
}

// 添加
export function ckmtDataAdd(data) {
  return commonFunc('/api/v1/ckmt.data.add', data)
}

// 删除
export function ckmtDataDel(data) {
  return commonFunc('/api/v1/ckmt.data.del', data)
}

// 修改
export function ckmtDataEdit(data) {
  return commonFunc('/api/v1/ckmt.data.edit', data)
}

// 详情
export function ckmtDataInfo(data) {
  return commonFunc('/api/v1/ckmt.data.info', data)
}

// 数据列表
export function ckmtDataList(data) {
  return commonFunc('/api/v1/ckmt.data.list', data)
}

// 修改状态
export function ckmtDataStatus(data) {
  return commonFunc('/api/v1/ckmt.data.status', data)
}

// 主表字段添加
export function ckmtFieldAdd(data) {
  return commonFunc('/api/v1/ckmt.field.add', data)
}

// 获取所有字段
export function ckmtFieldAll(data) {
  return commonFunc('/api/v1/ckmt.field.all', data)
}

// 主表字段删除
export function ckmtFieldDel(data) {
  return commonFunc('/api/v1/ckmt.field.del', data)
}

// 主表字段编辑
export function ckmtFieldEdit(data) {
  return commonFunc('/api/v1/ckmt.field.edit', data)
}

// 主表字段详情
export function ckmtFieldInfo(data) {
  return commonFunc('/api/v1/ckmt.field.info', data)
}

// 主表字段列表
export function ckmtFieldlist(data) {
  return commonFunc('/api/v1/ckmt.field.list', data)
}

// 添加记录
export function ckmtOperationAdd(data) {
  return commonFunc('/api/v1/ckmt.operation.add', data)
}

// 数据列表_领用
export function ckmtOperationList(data) {
  return commonFunc('/api/v1/ckmt.operation.list', data)
}

// 主页统计记录数据
export function ckmtOStatistics(data) {
  return commonFunc('/api/v1/ckmt.operation.statistics', data)
}

// 统计
export function ckmtOTimestatistics(data) {
  return commonFunc('/api/v1/ckmt.operation.timestatistics', data)
}
