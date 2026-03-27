/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:38:14
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-01-23 14:35:59
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'
function commonFunc<T, K>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-admin-platform/' + interfaceName,
    method: method,
    data: data
  })
}

// 应用市场（应用列表）
export function appmarket(data) {
  return commonFunc('admin-platform/appmarket', data)
}

// 获取分类列表
export function classifylist(data) {
  return commonFunc('admin-platform/classifylist', data)
}

// 保存多租户应用
export function tenantAppSave(data) {
  return commonFunc('admin-platform/tenantAppSave', data)
}

// 应用市场（个人操作板块）
export function appmarketperson(data) {
  return commonFunc('/admin-platform/appmarketmodule', data)
}

// 审核
export function audit(data) {
  return commonFunc('admin-platform/audit', data)
}

// 删除审核
export function auditdelete(data) {
  return commonFunc('admin-platform/auditdelete', data)
}

// 审核详情
export function auditdetail(data) {
  return commonFunc('admin-platform/auditdetail', data)
}

// 审核列表
export function auditlist(data) {
  return commonFunc('admin-platform/auditlist', data)
}

// 新增分类
export function classifyadd(data) {
  return commonFunc('admin-platform/classifyadd', data)
}

// 删除分类
export function classifydelete(data) {
  return commonFunc('admin-platform/classifydelete', data)
}

// 编辑分类
export function classifyedit(data) {
  return commonFunc('admin-platform/classifyedit', data)
}

// 获取管理员日志
export function getadminlog(data) {
  return commonFunc('admin-platform/getadminlog', data)
}

// 新增供应商
export function supplieradd(data) {
  return commonFunc('admin-platform/supplieradd', data)
}

// 删除供应商
export function supplierdelete(data) {
  return commonFunc('admin-platform/supplierdelete', data)
}

// 供应商详情
export function supplierdetail(data) {
  return commonFunc('admin-platform/supplierdetail', data)
}

// 编辑供应商
export function supplieredit(data) {
  return commonFunc('admin-platform/supplieredit', data)
}

// 供应商列表
export function supplierlist(data) {
  return commonFunc('admin-platform/supplierlist', data)
}

//  应用统计
export function num(data) {
  return commonFunc('admin-platform/app/num', data)
}

// 板块模板列表
export function initlist(data) {
  return commonFunc('admin-platform/module/initlist', data)
}

// 编辑板块模板列表 添加应用修改应用
export function initedit(data) {
  return commonFunc('admin-platform/module/initedit', data)
}

// 板块模板列表
export function initdetele(data) {
  return commonFunc('admin-platform/module/initdetele', data)
}

// 板块模板列表详情
export function initdetail(data) {
  return commonFunc('admin-platform/module/initdetail', data)
}

// 版块添加
export function initadd(data) {
  return commonFunc('admin-platform/module/initadd', data)
}

// 版块添加
export function classifyappmore(data) {
  return commonFunc('client/app/classifyappmore', data)
}

// 板块移动
export function initshift(data) {
  return commonFunc('admin-platform/module/initshift', data)
}

// 我的
// 模块列表
export function myMouduleList(data) {
  return commonFunc('client/module/list', data)
}

// 移动端我的模块列表
export function myMouduleMyList(data) {
  return commonFunc('client/module/mylist', data)
}

// 移动模块
export function myMouduleShift(data) {
  return commonFunc('client/module/shift', data)
}

// 编辑模块
export function myMouduleEdit(data) {
  return commonFunc('/client/module/edit', data)
}

// 删除
export function myMouduleDelete(data) {
  return commonFunc('client/module/delete', data)
}

// 还原
export function myMouduleInit(data) {
  return commonFunc('client/module/init', data)
}

// 添加
export function myMouduleAdd(data) {
  return commonFunc('client/module/add', data)
}

//  申请服务
export function apply(data) {
  return commonFunc('supplier-platform/service/apply', data)
}

// 我申请的服务列表
export function list(data) {
  return commonFunc('supplier-platform/service/list', data)
}

// 服务详情
export function serviceDetail(data) {
  return commonFunc('supplier-platform/service/detail', data)
}

// 所有人还原
export function myMouduleInitAll(data) {
  return commonFunc('client/module/initall', data)
}
// 模块强制同步给所有人
export function forceSync(data) {
  return commonFunc('client/module/forcesync', data)
}

// pc获取所有应用
export function appList(data) {
  return commonFunc('client/getallapp', data)
}

// 应用详情
export function appDetail(data) {
  return commonFunc('supplier-platform/app/detail', data)
}

// 申请应用
export function clientApplyApp(data) {
  return commonFunc('client/applyApp', data)
}

// 获取应用申请状态
export function getAppApplyStatusList(data) {
  return commonFunc('client/getAppApplyStatusList', data)
}

// 应用授权用户列表
export function getApplyList(data) {
  return commonFunc('supplier-platform/app/getApplyList', data)
}
// 事项
// 创建事项
export function itemADD(data) {
  return commonFunc('supplier-platform/item/add', data)
  // return commonFunc('supplier-platform/app/add', data)
}

// 事项详情
export function itemDetail(data) {
  return commonFunc('supplier-platform/item/detail', data)
  // return commonFunc('supplier-platform/app/detail', data)
}

// 我的事项
export function itemList(data) {
  return commonFunc('supplier-platform/item/list', data)
  // return commonFunc('supplier-platform/app/list', data)
}

// 实现数量情况
export function itemNum(data) {
  return commonFunc('admin-platform/item/num', data)
  // return commonFunc('admin-platform/app/num', data)
}

// 事项分类
// 新增分类
export function itemClassifyAdd(data) {
  return commonFunc('admin-platform/item/classifyadd', data)
  // return commonFunc('admin-platform/classifyadd', data)
}

// 删除分类
export function itemClassifyDelete(data) {
  // return commonFunc('admin-platform/item/classifydelete', data)
  return commonFunc('admin-platform/classifydelete', data)
}

// 编辑分类
export function itemClassifyEdit(data) {
  return commonFunc('admin-platform/item/classifyedit', data)
  // return commonFunc('admin-platform/classifyedit', data)
}

// 获取分类列表
export function itemClassifyList(data) {
  return commonFunc('admin-platform/item/classifylist', data)
}

// 获取事项市场
export function itemmarket(data) {
  return commonFunc('admin-platform/itemmarket', data)
}
// 事项模块
// 添加事项模块
export function itemInitAdd(data) {
  return commonFunc('admin-platform/item/module/initadd', data)
}

// 编辑事项模块
export function itemInitEdit(data) {
  return commonFunc('admin-platform/item/module/initedit', data)
}

// 事项模块详情
export function itemInitDetail(data) {
  return commonFunc('admin-platform/item/module/initdetail', data)
}

// 删除事项模块
export function itemInitDelete(data) {
  return commonFunc('admin-platform/item/module/initdetele', data)
}

// 事项模块列表
export function itemInitList(data) {
  return commonFunc('admin-platform/item/module/initlist', data)
}

// 事项模块列表
export function itemInitshift(data) {
  return commonFunc('admin-platform/item/module/initshift', data)
}

// 用户事项模块
// 添加事项模块
export function userItemModuleAdd(data) {
  return commonFunc('client/item/module/add', data)
}

// 删除事项模块
export function userItemModuleDelete(data) {
  return commonFunc('client/item/module/delete', data)
}

// 编辑事项模块
export function userItemModuleEdit(data) {
  return commonFunc('client/item/module/edit', data)
}

// 强制推送事项模块
export function userItemModuleForcesync(data) {
  return commonFunc('client/item/module/forcesync', data)
}

// 事项板块还原
export function userItemModuleInit(data) {
  return commonFunc('client/item/module/init', data)
}

// 我的事项模块列表
export function userItemModuleList(data) {
  return commonFunc('client/item/module/list', data)
}

// 我的事项模块移动
export function userItemModuleShift(data) {
  return commonFunc('client/item/module/shift', data)
}

// 应用市场个人操作板块
export function itemmarketmodule(data) {
  return commonFunc('admin-platform/itemmarketmodule', data)
}

// 所有人还原
export function itemMouduleInitAll(data) {
  return commonFunc('client/item/module/initall', data)
}

// 移动端我的事项模块列表
export function itemMyMouduleMyList(data) {
  return commonFunc('client/item/module/mylist', data)
}

// 获取我的pc应用列表
export function myPcList(data) {
  return commonFunc('client/module/mypclist', data)
}

export function idbysecretdetail(data) {
  return commonFunc('supplier-platform/app/idbysecretdetail', data)
}
