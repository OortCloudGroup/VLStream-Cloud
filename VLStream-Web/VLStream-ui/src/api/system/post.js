/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/request'
import {
  mapBladePostToRuoyi,
  mapRuoyiPost,
  toBladeList,
  toBladePage,
  toRuoyiPageParams
} from './ruoyiCompat'

/**
 * Query list
 * @param {Object} params current, size, postCode, postName etc.parameter
 * @return s {Promise} data
 */
export function getPostList(params) {
  const ruoyiParams = toRuoyiPageParams(params)
  return request({ url: '/system/post/list', method: 'get', params: ruoyiParams })
    .then((response) => toBladePage(response, mapRuoyiPost, params))
}

/**
 * Get ( )
 * @param {Object} params Query parameter, tenantId
 * @return s {Promise} item data
 */
export function getPostSelect(params) {
  return request({ url: '/system/post/optionselect', method: 'get', params })
    .then((response) => toBladeList(response, mapRuoyiPost))
}

/**
 * Add Update data
 * @param {Object} data object
 * @return s {Promise} Promise object
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
 * Delete
 * @param {String} ids ID
 * @return s {Promise} Promise object
 */
export function removePosts(ids) {
  return request({ url: `/system/post/${encodeURIComponent(ids)}`, method: 'delete' })
}
