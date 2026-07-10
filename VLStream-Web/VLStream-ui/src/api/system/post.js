import request from '@/utils/request'
import {
  mapBladePostToRuoyi,
  mapRuoyiPost,
  toBladeList,
  toBladePage,
  toRuoyiPageParams
} from './ruoyiCompat'

/**
 * 分页查询岗位列表
 * @param {Object} params 包含 current, size, postCode, postName 等参数
 * @returns {Promise} 返回岗位分页数据
 */
export function getPostList(params) {
  const ruoyiParams = toRuoyiPageParams(params)
  return request({ url: '/system/post/list', method: 'get', params: ruoyiParams })
    .then((response) => toBladePage(response, mapRuoyiPost, params))
}

/**
 * 获取用于下拉框选择的岗位列表 (通常不分页)
 * @param {Object} params 查询参数，可包含 tenantId
 * @returns {Promise} 返回岗位选项数据
 */
export function getPostSelect(params) {
  return request({ url: '/system/post/optionselect', method: 'get', params })
    .then((response) => toBladeList(response, mapRuoyiPost))
}

/**
 * 新增或修改岗位数据
 * @param {Object} data 岗位实体对象
 * @returns {Promise} 返回请求响应的 Promise 对象
 */
export function submitPost(data) {
  const payload = mapBladePostToRuoyi(data)
  return request({
    url: '/system/post',
    method: payload.postId ? 'put' : 'post',
    data: payload
  })
}

/**
 * 批量或单个删除岗位
 * @param {String} ids 逗号分隔的岗位ID
 * @returns {Promise} 返回请求响应的 Promise 对象
 */
export function removePosts(ids) {
  return request({ url: `/system/post/${encodeURIComponent(ids)}`, method: 'delete' })
}
