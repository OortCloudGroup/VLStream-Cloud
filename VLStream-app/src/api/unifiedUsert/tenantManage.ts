/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:47:24
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:47:24
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method = 'post', isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-user' + interfaceName,
    method: method,
    ...params
  })
}

// 组织机构模板添加
export function tempAdd(data) {
  return commonFunc('/temp/v1/add', data)
}

// 组织机构模板引用
export function tempApply(data) {
  return commonFunc('/temp/v1/apply', data)
}

// 组织机构复制
export function tempCopyToDept(data) {
  return commonFunc('/temp/v1/copyToDept', data)
}

// 组织机构模板复制
export function tempCopy(data) {
  return commonFunc('/temp/v1/copy', data)
}

// 组织机构模板删除
export function tempDel(data) {
  return commonFunc('/temp/v1/del', data)
}

// 组织机构模板编辑
export function tempEdit(data) {
  return commonFunc('/temp/v1/edit', data)
}

// 组织机构模板详情
export function tempInfo(data) {
  return commonFunc('/temp/v1/info', data)
}

// 组织机构模板列表
export function tempList(data) {
  return commonFunc('/temp/v1/list', data)
}

// 组织机构模板组织添加
export function tempDeptAdd(data) {
  return commonFunc('/temp/v1/deptAdd', data)
}

// 组织机构模板组织删除
export function tempDeptDel(data) {
  return commonFunc('/temp/v1/deptDel', data)
}

// 组织机构模板组织编辑
export function tempDeptEdit(data) {
  return commonFunc('/temp/v1/deptEdit', data)
}

// 组织机构模板组织列表
export function tempDeptList(data) {
  return commonFunc('/temp/v1/deptList', data)
}

// 组织机构模板组织详情
export function tempDeptInfo(data) {
  return commonFunc('/temp/v1/deptInfo', data)
}

// 租户委托授权添加
export function delegationAdd(data) {
  return commonFunc('/auth/v1/delegation_add', data)
}

// 租户委托授权确认
export function delegationConfirm(data) {
  return commonFunc('/auth/v1/delegation_confirm', data)
}

// 租户委托授权详情
export function delegationInfo(data) {
  return commonFunc('/auth/v1/delegation_info', data)
}

// 租户委托授权列表
export function delegationList(data) {
  return commonFunc('/auth/v1/delegation_list', data)
}

// 租户委托授权撤销
export function delegationRecover(data) {
  return commonFunc('/auth/v1/delegation_recover', data)
}

// 获取租户信息
export function getTenant(data) {
  return commonFunc('/auth/v1/get_tenant', data)
}

// 租户删除
export function tenantDel(data) {
  return commonFunc('/tenant/v1/delete', data)
}

// 租户高级删除
export function tenantAdvancedDel(data) {
  return commonFunc('/tenant/v1/advancedDelete', data)
}

// 租户恢复
export function tenantRestore(data) {
  return commonFunc('/tenant/v1/restore', data)
}

// 企业添加
export function companyAdd(data) {
  return commonFunc('/company/v1/add', data)
}

// 企业审核
export function companyCheck(data) {
  return commonFunc('/company/v1/check', data)
}

// 企业审核列表
export function companyCheckList(data) {
  return commonFunc('/company/v1/checkList', data)
}

// 企业删除
export function companyDel(data) {
  return commonFunc('/company/v1/del', data)
}

// 企业编辑
export function companyEdit(data) {
  return commonFunc('/company/v1/edit', data)
}

// 企业列表-详情
export function companyInfo(data) {
  return commonFunc('/company/v1/info', data)
}

// 企业列表-审核详情
export function companyCheckInfo(data) {
  return commonFunc('/company/v1/checkInfo', data)
}

// 企业列表
export function companyList(data) {
  return commonFunc('/company/v1/list', data)
}

// 企业列表-商户
export function merchantList(data) {
  return commonFunc('/company/v1/merchantList', data)
}

// 企业列表-商户-详情
export function merchantDetail(data) {
  return commonFunc('/company/v1/merchantDetail', data)
}

// 企业状态设置
export function companySetStatus(data) {
  return commonFunc('/company/v1/setStatus', data)
}

// 企业状态设置-失效
export function userSetStatus(data) {
  return commonFunc('/company/v1/userSetStatus', data)
}

// 认证等级-添加
export function identLevelAdd(data) {
  return commonFunc('/ident/v1/levelAdd', data)
}

// 认证等级-删除
export function identLevelDelete(data) {
  return commonFunc('/ident/v1/levelDelete', data)
}

// 认证等级-编辑
export function identLevelEdit(data) {
  return commonFunc('/ident/v1/levelEdit', data)
}

// 认证等级-详情
export function identLevelInfo(data) {
  return commonFunc('/ident/v1/levelInfo?' + data, '', 'get')
}

// 认证等级-列表
export function identLevelList(data) {
  return commonFunc('/ident/v1/levelList', data, 'get')
}

// 认证用户列表
export function identUserList(data) {
  return commonFunc('/ident/v1/identUserList', data, 'get')
}

// 用户认证等级设置
export function setUserLevel(data) {
  return commonFunc('/ident/v1/setUserLevel', data, 'post')
}

// 认证组织列表
export function identDeptList(data) {
  return commonFunc('/ident/v1/identDeptList', data, 'get')
}

// 组织认证等级设置
export function setDeptLevel(data) {
  return commonFunc('/ident/v1/setDeptLevel', data, 'post')
}

// 获取认证配置-类型
export function ideConfigTypeList(data) {
  return commonFunc('/userident/v1/configTypeList', data, 'get')
}

// 获取认证配置-添加
export function ideConfiAdd(data) {
  return commonFunc('/userident/v1/configAdd', data)
}

// 获取认证配置-删除
export function ideConfigDelete(data) {
  return commonFunc('/userident/v1/configDelete', data)
}

// 获取认证配置-编辑
export function ideConfigEdit(data) {
  return commonFunc('/userident/v1/configEdit', data)
}

// 获取认证配置-详情
export function ideConfigInfo(data) {
  return commonFunc('/userident/v1/configInfo', data, 'get')
}

// 获取认证配置-列表
export function ideConfigList(data) {
  return commonFunc('/userident/v1/configList', data, 'get')
}

// 认证配置状态-删除
export function configDelete(data) {
  return commonFunc('/userident/v1/configDelete', data, 'get')
}

// 认证配置状态-设置
export function configSetStatus(data) {
  return commonFunc('/userident/v1/configSetStatus', data, 'get')
}

// ident
// 添加应用认证等级
export function addAppLevel(data) {
  return commonFunc('/ident/v1/addAppLevel', data)
}

// 编辑应用认证等级
export function editAppLevel(data) {
  return commonFunc('/ident/v1/editAppLevel', data)
}
// 应用认证详情
export function identAppInfo(data) {
  return commonFunc('/ident/v1/identAppInfo', data)
}
// 应用认证列表
export function identAppList(data) {
  return commonFunc('/ident/v1/identAppList', data)
}

// 设置应用二次认证包括身份证和人脸识别
export function setAppAccessAuth(data) {
  return commonFunc('/ident/v1/setAppAccessAuth', data)
}

//  identApply
// 用户发起应用申请认证
export function identApply(data) {
  return commonFunc('/userident/v1/identApply', data)
}

// 用户申请认证审核
export function identCheck(data) {
  return commonFunc('/userident/v1/identCheck', data)
}
// 用户申请认证审核详情
export function identCheckInfo(data) {
  return commonFunc('/userident/v1/identCheckInfo', data)
}
// 用户应用申请认证审核列表
export function identCheckList(data) {
  return commonFunc('/userident/v1/identCheckList', data)
}

// 用户认证-默认认证-获取
export function userIdentLoginInfo(data) {
  return commonFunc('/userident/v1/userIdentLoginInfo', data)
}

// 用户认证-默认认证-设置
export function setUserIdentLogin(data) {
  return commonFunc('/userident/v1/setUserIdentLogin', data)
}

// 实名核验-新增按钮
export function identUserInfo(data) {
  return commonFunc('/userident/v1/identUserInfo', data)
}

