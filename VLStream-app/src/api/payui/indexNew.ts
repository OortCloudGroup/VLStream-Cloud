/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:46:29
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:46:29
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'

import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method: string, isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-pay' + interfaceName,
    // url: 'http://192.168.60.75:32631' + interfaceName,
    method: method,
    ...params
  })
}

function commonFuncB<T, K>(interfaceName: string, data: T, method: string) {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-pay' + interfaceName,
    method: method,
    data: data,
    responseType: 'blob'
  })
}

// 商户表
/* 商户表-分页列表查询 */
export function pmtMerchantList(data: any) {
  return commonFunc('/pay/pcore/pmtMerchant/list', data, 'GET')
}

/* 商户表-分页列表查询 -商户 */
export function pmtMerchantList1(data: any) {
  return commonFunc('/pay/pcore/pmtMerchant/intoMchList', data, 'GET')
}

/* 应用-商户列表 */
export function payAppMerList(data: any) {
  return commonFunc('/pay/app/mer/list', data, 'GET')
}

/* 商户管理-商户列表list */
export function payMerList(data: any) {
  return commonFunc('/pay/merchant/list', data, 'GET')
}

/* 进件列表记录 */
export function applymentList(data: any) {
  return commonFunc('/pay/mer/applyment/list', data, 'GET')
}

/* 商户模式-分页列表 */
export function payOrdMerList(data: any) {
  return commonFunc('/pay/ordMer/list', data, 'GET')
}

/* 商户模式-分页列表- 编辑 */
export function payOrdMerEdit(data: any) {
  return commonFunc('/pay/merchant/edit', data, 'POST')
}

/* 绑定商户 */
export function payBindBatch(data: any) {
  return commonFunc('/pay/serMer/mer/bindBatch', data, 'POST')
}

/* 服务商list */
export function paySerMerList(data: any) {
  return commonFunc('/pay/serMer/list', data, 'GET')
}

/* 商户表-添加 */
export function pmtMerchantAdd(data: any) {
  return commonFunc('/pay/pcore/pmtMerchant/add', data, 'POST')
}

/* 商户管理-商户列表保存 */
export function payMerSave(data: any) {
  return commonFunc('/pay/merchant/save', data, 'POST')
}

/* 商户表-编辑 */
export function pmtMerchantEdit(data: any) {
  return commonFunc('/pay/pcore/pmtMerchant/edit', data, 'POST')
}

/* 商户管理-商户列表编辑 */
export function payMerEdit(data: any) {
  return commonFunc('/pay/merchant/edit', data, 'POST')
}

/* 渠道-启用禁用 */
export function merEditStatus(data: any) {
  return commonFunc('/pay/channel/enable', data, 'POST')
}

/*  商户模式-启用禁用 */
export function payMerchantEnable(data: any) {
  return commonFunc('/pay/merchant/enable', data, 'POST')
}

/* 商户表-主要用于新建支付订单页面。如有服务商，则查服务商的商户，否则直接查商户*/
export function pmtMerchantListByCond(data: any) {
  return commonFunc('/pay/merchant/aggregation/list', data, 'GET')
}

/* 商户表-商户模式，添加商户时，设置merchType=1 */
export function editMerchType(data: any) {
  return commonFunc('/pay/pcore/pmtMerchant/editMerchType', data, 'POST')
}

/* 商户表-通过id删除 */
export function pmtMerchantDelete(data: any) {
  return commonFunc('/pay/pcore/pmtMerchant/delete', data, 'DELETE')
}

/* 商户管理-商户列表删除 */
export function payMerDel(data: any) {
  return commonFunc('/pay/merchant/removeBatch', data, 'POST')
}

/* 商户表-通过id查询 */
export function pmtMerchantQuery(data: any) {
  return commonFunc('/pay/pcore/pmtMerchant/queryById', data, 'GET')
}

/*  商户管理-商户详情 */
export function payMerDetail(data: any) {
  return commonFunc('/pay/merchant/detail', data, 'GET')
}

/* 商户表-商户进入渠道配置 */
export function pmtMerchantIntoEdit(data: any) {
  return commonFunc('/pay/pcore/pmtMerchant/intoEdit', data, 'POST')
}

/* 商户模式，移除商户时，设置merchType为空 */
export function removeMerchType(data: any) {
  return commonFunc('/pay/pcore/pmtMerchant/removeMerchType', data, 'POST')
}

// 渠道表
/* 渠道表-分页列表查询 */
export function pmtChannelList(data: any) {
  return commonFunc('/pay/pcore/pmtChannel/intoList', data, 'GET')
}

/* 服务商下渠道列表 */
export function paySerMerChannelList(data: any) {
  return commonFunc('/pay/serMer/channel/list', data, 'GET')
}

/* 渠道下的商户列表 */
export function paySerMerChanlist(data: any) {
  return commonFunc('/pay/serMer/mer/list', data, 'GET')
}

/* 渠道表-添加 */
export function pmtChannelAdd(data: any) {
  return commonFunc('/pay/pcore/pmtChannel/add', data, 'POST')
}

/* 商户模式-保存渠道 */ // 服务商渠道
export function payChannelSave(data: any) {
  return commonFunc('/pay/channel/save', data, 'POST')
}

/* 渠道表-编辑 */
export function pmtChannelEdit(data: any) {
  return commonFunc('/pay/pcore/pmtChannel/edit', data, 'POST')
}

/* 服务商-渠道编辑 */
export function payChannelEdit(data: any) {
  return commonFunc('/pay/channel/edit', data, 'POST')
}

/* 渠道表-通过id删除 */
export function pmtChannelDelete(data: any) {
  // return commonFunc('/pay/pcore/pmtChannel/delete', data, 'DELETE')
  return commonFunc('/pay/channel/removeBatch', data, 'POST')
}

// 支付配置表
/* 支付配置表-分页列表查询 */
export function pmtPayConfigList(data: any) {
  return commonFunc('/pay/pcore/pmtPayConfig/list', data, 'GET')
}

/* 支付配置表-添加 */
export function pmtPayConfigAdd(data: any) {
  return commonFunc('/pay/pcore/pmtPayConfig/saveAndUpdate', data, 'POST')
}

// 商户费率设置表
/* 商户费率设置表-分页列表查询 */
export function pmtMerchRateList(data: any) {
  return commonFunc('/pay/pcore/pmtMerchRate/list', data, 'GET')
}

/* 商户费率设置表-添加 */
export function pmtMerchRateAdd(data: any) {
  return commonFunc('/pay/pcore/pmtMerchRate/saveAndUpdate', data, 'POST')
}

// 应用表
/* 应用表-分页列表查询 */
export function pmtAppList(data: any) {
  return commonFunc('/pay/pcore/pmtApp/list', data, 'GET')
}

/* 应用表-分页列表查询-列表 */
export function pmtAppList1(data: any) {
  return commonFunc('/pay/pcore/pmtApp/intoList', data, 'GET')
}

/* 应用表-添加 */
export function pmtAppAdd(data: any) {
  return commonFunc('/pay/pcore/pmtApp/add', data, 'POST')
}

/* 应用表-编辑 */
export function pmtAppEdit(data: any) {
  return commonFunc('/pay/pcore/pmtApp/edit', data, 'POST')
}

/* 应用表-通过id删除 */
export function pmtAppDelete(data: any) {
  return commonFunc('/pay/pcore/pmtApp/delete', data, 'DELETE')
}

/* 应用表-进入支付渠道选择页面 */
export function pmtAppIntoChannel(data: any) {
  return commonFunc('/pay/app/channel/list', data, 'GET')
}

/* 应用和支付渠道关系表-添加-修改一体化方法 */
export function pmtAppChannelSave(data: any) {
  return commonFunc('/pay/pcore/pmtAppChannel/saveAndUpdate', data, 'POST')
}

/* 商户模式 渠道配置 应用配置保存 */
export function channelBindBatch(data: any) {
  return commonFunc('/pay/app/channel/bindBatch', data, 'POST')
}

/* 关联应用 应用表-新增和编辑一体方法*/
export function pmtAppSaveAndUpdate(data: any) {
  return commonFunc('/pay/pcore/pmtApp/saveAndUpdate', data, 'POST')
}

/* 拉卡拉进件相关接口 新增商户进件 */
export function lakalaCommitMchInfo(data: any) {
  return commonFunc('/pay/lakala/commitMchInfo', data, 'POST')
}

/* 进件-进件信息查询 */
export function lakalaQueryContract(data: any) {
  return commonFunc('/pay/lakala/queryContract', data, 'POST')
}

/* 进件-附件上传 */
export function lakalaUploadFiles(data: any) {
  return commonFunc('/pay/lakala/uploadFiles', data, 'POST')
}

/* 支付订单表-分页列表查询 */
export function pmtOrderList(data: any) {
  // return commonFunc('/pay/pcore/pmtOrder/list', data, 'GET')
  return commonFunc('/pay/payOrder/list', data, 'GET')
}

/* 支付订单表-添加 */
export function pmtOrderAdd(data: any) {
  return commonFunc('/pay/pcore/pmtOrder/add', data, 'POST')
}

/* 支付订单表-编辑 */
export function pmtOrderEdit(data: any) {
  return commonFunc('/pay/pcore/pmtOrder/edit', data, 'POST')
}

/* 应用表-根据应用ID，查询其渠道（支付订单表）*/
export function queryChannelByAppId(data: any) {
  return commonFunc('/pay/app/channel/selectList', data, 'GET')
}

/* 新建订单-优惠券列表*/
export function cardCouponCheapest(data: any) {
  return commonFunc('/pay/cardCoupon/cheapest?payAmount=100', data, 'GET')
}

/* 退款表-分页列表查询 */
export function pmtRefundList(data: any) {
  // return commonFunc('/pay/pcore/pmtRefund/list', data, 'GET')
  return commonFunc('/pay/refund/list', data, 'GET')
}

/* 退款表-添加 */
export function pmtRefundAdd(data: any) {
  return commonFunc('/pay/pcore/pmtRefund/add', data, 'POST')
}

/* 退款表-编辑 */
export function pmtRefundEdit(data: any) {
  return commonFunc('/pay/pcore/pmtRefund/edit', data, 'POST')
}

/* 退款表-通过id删除 */
export function pmtRefundDelete(data: any) {
  return commonFunc('/pay/pcore/pmtRefund/delete', data, 'DELETE')
}

/* 退款表-通过id查询 */
export function pmtRefundQueryById(data: any) {
  return commonFunc('/pay/pcore/pmtRefund/queryById', data, 'GET')
}

/* 卡BIN查询 */
export function lakalaCardBin(data: any) {
  return commonFunc('/pay/lakala/cardBin', data, 'POST')
}

/* 渠道费率设置表-添加和编辑一体方法 */
export function pmtChannelRateSaveUpdate(data: any) {
  return commonFunc('/pay/pcore/pmtChannelRate/saveAndUpdate', data, 'POST')
}

/* 渠道费率设置表-详情 */
export function pmtPayConfigInfo(data: any) {
  return commonFunc('/pay/pcore/pmtChannelRate/list', data, 'GET')
}

/* 渠道商户表-批量删除 */// 服务商 删除商户
export function pmtChanMerchDelete(data: any) {
  // return commonFunc('/pay/pcore/pmtChanMerch/deleteBatch', data, 'POST')
  return commonFunc('/pay/serMer/mer/unbindBatch', data, 'POST')
}

/* 商户模式下的渠道列表 */
export function payOrdMerChannelList(data: any) {
  return commonFunc('/pay/ordMer/channel/list', data, 'GET')
}

/* 渠道商户表-根据渠道ID，查询商户信息 */
export function queryMerchByChanId(data: any) {
  return commonFunc('/pay/pcore/pmtChanMerch/queryMerchByChanId', data, 'GET')
}

// 四种 支付模式
export function payuPayChannelList(data: any) {
  return commonFunc('/pay/uPay/channel/list', data, 'GET')
}

// 会员中心-列表
export function payMemberList(data: any) {
  return commonFunc('/pay/member/list', data, 'GET')
}

// 会员中心-详情
export function memberPayAndRefund(data: any) {
  return commonFunc('/pay/member/payAndRefund/stat', data, 'GET')
}

// 会员中心-优惠劵详情-num
export function memberCardCoupon(data: any) {
  return commonFunc('/pay/member/cardCoupon/count', data, 'GET')
}

// 会员中心-优惠劵详情-list
export function memberPayOrder(data: any) {
  return commonFunc('/pay/member/payOrder/list', data, 'GET')
}

// 会员中心-优惠劵详情-card
export function memberCard(data: any) {
  return commonFunc('/pay/cardCoupon/list', data, 'GET')
}

// 字典的列表
export function listOfDict(data: any) {
  return commonFunc('/pay/uPay/listOfDict', data, 'post')
}

// 附件上传接口-服务商
export function payUploadFile(data: any) {
  return commonFunc('/pay/uPay/uploadFile', data, 'post')
}

// 附件上传接口-普通商户
export function merUploadFile(data: any) {
  return commonFunc('/pay/merchant/uploadFile', data, 'post')
}

// 附件上传接口-进件
export function payAddMer(data: any) {
  return commonFunc('/pay/uPay/addMer', data, 'post')
}

// 证书上传
export function channelUpload(data: any) {
  return commonFunc('/pay/channel/uploadFile', data, 'post')
}

// 证书上传-查询
export function channelUploadDe(data: any) {
  return commonFunc('/pay/channel/detail', data, 'get')
}

// 费率配置-save
export function rateSave(data: any) {
  return commonFunc('/pay/rate/save', data, 'post')
}

// 费率配置-edit
export function rateEdit(data: any) {
  return commonFunc('/pay/rate/edit', data, 'post')
}

// 费率配置-del
export function rateRemoveBatch(data: any) {
  return commonFunc('/pay/rate/removeBatch', data, 'post')
}

// 费率配置-detail
export function rateDetail(data: any) {
  return commonFunc('/pay/rate/detail', data, 'get')
}

// 费率配置-list
export function rateList(data: any) {
  return commonFunc('/pay/rate/list', data, 'get')
}

// 应用类型
export function appTypeList(data: any) {
  return commonFunc('/pay/app/type/list', data, 'get')
}

// 新建应用-save
export function appSave(data: any) {
  return commonFunc('/pay/app/save', data, 'post')
}

// 编辑应用-removeBatch
export function appRemoveBatch(data: any) {
  return commonFunc('/pay/app/removeBatch', data, 'post')
}

// 编辑应用-edit
export function appEdit(data: any) {
  return commonFunc('/pay/app/edit', data, 'post')
}

// 应用启用禁用-enable
export function appEnable(data: any) {
  return commonFunc('/pay/app/enable', data, 'post')
}

// 应用-list
export function appList(data: any) {
  return commonFunc('/pay/app/list', data, 'get')
}

// 应用-list ser
export function appListSer(data: any) {
  return commonFunc('/pay/serMer/app/list', data, 'get')
}

// 应用-detail
export function appDetail(data: any) {
  return commonFunc('/pay/app/detail', data, 'get')
}

// 订单页-支付
export function counterPage(data: any) {
  return commonFunc('/pay/counter/page', data, 'post')
}

// 订单页-支付-渠道
export function counterChannelList(data: any) {
  return commonFunc('/pay/counter/channel/list', data, 'get')
}

// 订单页-支付-去支付
export function uPayUserScan(data: any) {
  return commonFunc('/pay/uPay/userScan', data, 'post')
}

// 订单页-支付-详情
export function uPayQuery(data: any) {
  return commonFunc('/pay/uPay/query', data, 'get')
}

// 支付订单-关单
export function uPayClose(data: any) {
  return commonFunc('/pay/uPay/close', data, 'post')
}

// 支付订单-查询
export function uPayQueryS(data: any) {
  return commonFunc('/pay/uPay/query', data, 'get')
}

// 对账-列表
export function billList(data: any) {
  return commonFunc('/pay/bill/list', data, 'get')
}

// 对账-详情
export function billTaskDetail(data: any) {
  return commonFunc('/pay/bill/detail', data, 'get')
}

// 对账-任务列表
export function billTaskList(data: any) {
  return commonFunc('/pay/bill/task/list', data, 'get')
}

// 用途-保存
export function billPurposeSave(data: any) {
  return commonFunc('/pay/purpose/save', data, 'post')
}

// 用途-编辑
export function billPurposeEdit(data: any) {
  return commonFunc('/pay/purpose/edit', data, 'post')
}

// 用途-移除
export function billPurposeRemove(data: any) {
  return commonFunc('/pay/purpose/remove', data, 'post')
}

// 用途-列表
export function billPurposeList(data: any) {
  return commonFunc('/pay/purpose/list', data, 'get')
}

// 用途-详情
export function billPurposeDetail(data: any) {
  return commonFunc('/pay/purpose/detail', data, 'get')
}

// 卡劵核销接口
export function cardCouponList(data: any) {
  return commonFunc('/pay/cardCoupon/list', data, 'get')
}

// 积分核销接口
export function payOrderPointsList(data: any) {
  return commonFunc('/pay/payOrder/points/list', data, 'get')
}

// 收支明细-会员管理
export function payOrderMemberList(data: any) {
  return commonFunc('/pay/payOrder/member/list', data, 'get')
}
// 对账数据-下载
export function billDownload(data: any) {
  return commonFuncB('/pay/bill/download', data, 'get')
}

/* 被扫支付 */
export function payMerScan(data: any) {
  return commonFunc('/pay/uPay/merScan', data, 'post')
}
