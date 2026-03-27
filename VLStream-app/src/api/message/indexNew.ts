/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:46:16
 * @Last Modified by: 兰舰
 * @Last Modified time: 2024-11-15 11:48:01
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
// import { getToken } from '@/utils/cache/cookies'
import config from '@/config'
import Config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method: string) {
  return request<K>({
    // url: 'http://192.168.88.18:8012' + interfaceName,
    url: config.URL + config.gateWay + 'apaas-unified-msg' + interfaceName,
    method: method,
    data: data
  })
}

function commonFuncC<T, K>(interfaceName: string, data: T, method: string, isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-unified-msg' + interfaceName,
    method: method,
    ...params
  })
}

function commonFunc1<T, K>(interfaceName: string, data: T, method: string) {
  return request<K>({
    url: Config.URL + Config.gateWay + interfaceName,
    method: method,
    headers: Config.headers,
    data: data
  })
}

function commonFunc2<T, K>(interfaceName: string, data: T, method: string) {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-unified-msg' + interfaceName,
    method: method,
    data: data,
    responseType: 'blob'
  })
}

// app
/* 应用通道列表 */
export function appCheckList(data: any) {
  return commonFunc('/app/v1/check/list', data, 'post')
}

/* 应用审核 */
export function appCheck(data: any) {
  return commonFunc('/app/v1/check', data, 'post')
}

/* 应用删除 */
export function appDel(data: any) {
  return commonFunc('/app/v1/del', data, 'post')
}

/* 应用通道列表 */
export function appDuct(data: any) {
  return commonFunc('/app/v1/duct', data, 'post')
}

/* 应用列表 */
export function appList(data: any) {
  return commonFunc('/app/v1/list', data, 'post')
}

/* 应用新增修改 */
export function appSave(data: any) {
  return commonFunc('/app/v1/save', data, 'post')
}

/* 应用状态更新 */
export function appStatus(data: any) {
  return commonFunc('/app/v1/status', data, 'post')
}

// channel
/* 新增渠道 */
export function channelAdd(data: any) {
  return commonFunc('/channel/v1/add', data, 'post')
}

/* 渠道删除 */
export function channelDel(data: any) {
  return commonFunc('/channel/v1/del', data, 'post')
}

/* 渠道更新 */
export function channelEdit(data: any) {
  return commonFunc('/channel/v1/edit', data, 'post')
}

/* 渠道信息 */
export function channelInfo(data: any) {
  return commonFunc('/channel/v1/info', data, 'post')
}

/* 渠道状态更新 */
export function channelStatus(data: any) {
  return commonFunc('/channel/v1/status', data, 'post')
}

/* 渠道列表 */
export function channelList(data: any) {
  return commonFunc('/channel/v1/list', data, 'post')
}

/* 通过渠道id获取渠道模板 */
export function channelTemplateList(data: any) {
  return commonFunc('/channel/v1/template/list', data, 'post')
}

/* 获取渠道类型 */
export function channelTypeList(data: any) {
  return commonFunc('/channel/v1/type/list', data, 'post')
}

// duct
/* 选择通道查询绑定渠道 */
export function ductBindChannel(data: any) {
  return commonFunc('/duct/v1/bind/channel', data, 'post')
}

/* 通道列表查询 */
export function ductAppData(data: any) {
  return commonFunc('/duct/v1/app/data', data, 'post')
}

/* 渠道绑定 */
export function ductBind(data: any) {
  return commonFunc('/duct/v1/bind', data, 'post')
}

/* 渠道额度列表 */
export function ductChannelList(data: any) {
  return commonFunc('/duct/v1/channel/list', data, 'post')
}

/* 通道审核列表 */
export function ductCheckList(data: any) {
  return commonFunc('/duct/v1/check/list', data, 'post')
}

/* 通道渠道状态更新 */
export function ductCheckStatus(data: any) {
  return commonFunc('/duct/v1/channel/status', data, 'post')
}

/*  通道审核列表-审核渠道 */
export function ductCheck(data: any) {
  return commonFunc('/duct/v1/check', data, 'post')
}

/* 通道删除 */
export function ductDel(data: any) {
  return commonFunc('/duct/v1/del', data, 'post')
}

/* 通道详情 */
export function ductInfo(data: any) {
  return commonFunc('/duct/v1/info', data, 'post')
}

/* 通道列表 */
export function ductList(data: any) {
  return commonFunc('/duct/v1/list', data, 'post')
}

/* 渠道配额 */
export function ductQuotaSave(data: any) {
  return commonFunc('/duct/v1/quota/save', data, 'post')
}

/* 新增/修改通道 */
export function ductSave(data: any) {
  return commonFunc('/duct/v1/save1', data, 'post')
}

// msg
/* 定时任务 删除 */
export function msgJobDel(data: any) {
  return commonFunc('/msg/v1/job/del', data, 'post')
}

/* 消息任务信息 */
export function msgJobInfo(data: any) {
  return commonFunc('/msg/v1/job/info', data, 'post')
}

/* 定时任务 */
export function msgJobList(data: any) {
  return commonFunc('/msg/v1/job/list', data, 'post')
}

/* 更新消息任务状态 */ // 定位任务启动
export function msgJobstatus(data: any) {
  return commonFunc('/msg/v1/job/status', data, 'post')
}

/* 消息发送 */
export function msgList(data: any) {
  return commonFunc('/msg/v1/list', data, 'post')
}

/* 消息重发 */
export function msgRepeat(data: any) {
  return commonFunc('/msg/v1/repeat', data, 'post')
}

/* 定时未发送消息撤销 */ // 定位任务撤回
export function msgRevoke(data: any) {
  return commonFunc('/msg/v1/revoke', data, 'post')
}

/* 发送消息 */
export function msgSend(data: any) {
  return commonFunc('/msg/v1/send', data, 'post')
}

// system
/* 获取场景管理列表 */
export function systemSceneAll(data: any) {
  return commonFunc('/system/v1/scene/all', data, 'post')
}

/* 场景管理删除 */
export function systemSceneDel(data: any) {
  return commonFunc('/system/v1/scene/del', data, 'post')
}

/* 场景管理列表 */
export function systemSceneList(data: any) {
  return commonFunc('/system/v1/scene/list', data, 'post')
}

/* 添加场景 */
export function systemSceneSave(data: any) {
  return commonFunc('/system/v1/scene/save', data, 'post')
}

/* 删除敏感词库 */
export function systemSensitiveDel(data: any) {
  return commonFunc('/system/v1/sensitive/del', data, 'post')
}

/* 下载 敏感词库 */
export function systemSensitiveDownload(data: any) {
  return commonFunc2('/system/v1/sensitive/download', data, 'post')
}

/* 获取敏感词配置 */
export function systemSensitiveInfo(data: any) {
  return commonFunc('/system/v1/sensitive/info', data, 'post')
}

/* 敏感词列表库 */
export function systemSensitiveList(data: any) {
  return commonFunc('/system/v1/sensitive/list', data, 'post')
}

/* 敏感词设置 */
export function systemSensitiveSet(data: any) {
  return commonFunc('/system/v1/sensitive/set', data, 'post')
}

/* 启动敏感词库 */
export function systemSensitiveStatus(data: any) {
  return commonFunc('/system/v1/sensitive/status', data, 'post')
}

/* 下载敏感词库模板 */
export function systemSensitiveTemplate(data: any) {
  return commonFunc2('/system/v1/sensitive/template', data, 'get')
}

/* 敏感词库上传 */
export function systemSensitiveUpload(data: any) {
  return commonFunc1('apaas-fastdfsservice/fastdfs/v1/uploadFile', data, 'post')
}

/* 敏感词库上传 */
export function sensitiveUpload(data: any) {
  return commonFunc('/system/v1/sensitive/upload', data, 'post')
}

/* 敏感词库上传 */
export function systemTemplateTypeAll(data: any) {
  return commonFunc('/system/v1/template/type/all', data, 'post')
}

/* 模版类型删除 */
export function systemTemplateTypeDel(data: any) {
  return commonFunc('/system/v1/template/type/del', data, 'post')
}

/* 模版类型列表 */
export function systemTemplateTypeList(data: any) {
  return commonFunc('/system/v1/template/type/list', data, 'post')
}

/* 添加模版类型 */
export function systemTemplateTypeSave(data: any) {
  return commonFunc('/system/v1/template/type/save', data, 'post')
}

// template
/*  模版审核 */
export function templateCheck(data: any) {
  return commonFunc('/template/v1/check', data, 'post')
}

/*  模版审核list */
export function templateCheckList(data: any) {
  return commonFunc('/template/v1/check/list', data, 'post')
}

/* 模版类型删除 */
export function templateDel(data: any) {
  return commonFunc('/template/v1/del', data, 'post')
}

/* 模版列表 */
export function templateList(data: any) {
  return commonFunc('/template/v1/list', data, 'post')
}

/* 新增/修改模版 */
export function templateSave(data: any) {
  return commonFunc('/template/v1/save', data, 'post')
}

/* 获取模版列表下拉框 */
export function templateSelect(data: any) {
  return commonFunc('/template/v1/select', data, 'post')
}

/* 模版详情 */
export function templateShow(data: any) {
  return commonFunc('/template/v1/show', data, 'post')
}

/* 通用模板 状态启用禁用 */
export function templateStatus(data: any) {
  return commonFunc('/template/v1/status', data, 'post')
}

/* 删除模版 */
export function templateVariablesDel(data: any) {
  return commonFunc('/template/v1/variables/del', data, 'post')
}

/* 模版变量列表 */
export function templateVariablesList(data: any) {
  return commonFunc('/template/v1/variables/list', data, 'post')
}

/* 模版变量添加 */
export function templateVariablesSave(data: any) {
  return commonFunc('/template/v1/variables/save', data, 'post')
}

/* 链路 */
export function msgLink(data: any) {
  return commonFunc('/msg/v1/link', data, 'post')
}

/* 链路监听GET */
export function listeninfo() {
  return commonFunc('/msg/v1/link/listeninfo', '', 'GET')
}
/* 链路监听 */
export function listenset(data: any) {
  return commonFunc('/msg/v1/link/listenset', data, 'post')
}

/* 链路 - 清空 */
export function listenDel(data: any) {
  return commonFunc('/msg/v1/link/del', data, 'post')
}

/* 通知列表 */
export function instatmsgList(data: any) {
  return commonFuncC('/msg/v1/instatmsg/list', data, 'get')
}

/* 通知列表 已读*/
export function instatmsgStatus(data: any) {
  return commonFuncC('/msg/v1/instatmsg/status', data, 'post')
}

/* 通知列表 详情*/
export function instatmsgInfo(data: any) {
  return commonFuncC('/msg/v1/instatmsg/info', data, 'get')
}

/* 消息法师 附件地址*/
export function systemUpload(data: any) {
  return commonFunc1('apaas-unified-msg/system/v1/upload/file', data, 'post')
}

/* 推送记录-消息列表详情接口*/
export function msgListInfo(data: any) {
  return commonFuncC('/msg/v1/list/info', data, 'post')
}

/* 链路新增 导出功能*/
export function linkExport(data: any) {
  return commonFunc2('/msg/v1/link/export', data, 'post')
}

// qualification
/* 资质-新增*/
export function qualificationAdd(data: any) {
  return commonFuncC('/channel/v1/qualification/add', data, 'post')
}

/* 资质-编辑*/
export function qualificationEdit(data: any) {
  return commonFuncC('/channel/v1/qualification/edit', data, 'post')
}

/* 资质-list*/
export function qualificationList(data: any) {
  return commonFuncC('/channel/v1/qualification/list', data, 'post')
}

// sign
/* sign-新增*/
export function signAdd(data: any) {
  return commonFuncC('/channel/v1/sign/add', data, 'post')
}

/* sign- 删除 */
export function signDelet(data: any) {
  return commonFuncC('/channel/v1/sign/delet', data, 'post')
}

/* sign-info*/
export function signInfo(data: any) {
  return commonFuncC('/channel/v1/sign/info', data, 'post')
}

/* sign-编辑*/
export function signEdit(data: any) {
  return commonFuncC('/channel/v1/sign/edit', data, 'post')
}

/* sign-上传*/
export function signOssfileupload(data: any) {
  return commonFunc2('/channel/v1/sign/ossfileupload', data, 'post')
}

/* sign-list*/
export function signList(data: any) {
  return commonFuncC('/channel/v1/sign/list', data, 'post')
}

/* smstemplate-申请短信模板 (阿里云审核通过后同步)*/
export function smstemplateAdd(data: any) {
  return commonFuncC('/channel/v1/smstemplate/add', data, 'post')
}

/* smstemplate-修改短信模板 (只能修改未审核通过的)*/
export function smstemplateEdit(data: any) {
  return commonFuncC('/channel/v1/smstemplate/edit', data, 'post')
}

/* smstemplate-查询短信模板详情（阿里云）*/
export function smstemplateInfo(data: any) {
  return commonFuncC('/channel/v1/smstemplate/info', data, 'post')
}

/* smstemplate-查询短信模板列表*/
export function smstemplateList(data: any) {
  return commonFuncC('/channel/v1/smstemplate/list', data, 'post')
}

/* smstemplate-批量同步阿里云短信模板数据*/
export function smstemplateSync(data: any) {
  return commonFuncC('/channel/v1/smstemplate/sync', data, 'post')
}

/* 模版关联渠道通道*/
export function templateBind(data: any) {
  return commonFuncC('/template/v1/bind', data, 'post')
}

/* 模版设置默认生效*/
export function templateSetdefault(data: any) {
  return commonFuncC('/template/v1/setdefault', data, 'post')
}

// 发送视频通话消息
export function sendVideoCallMsg(data: any) {
  return commonFunc('/msg/v1/send/notice', data, 'post')
}
