/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'

export const getOrchestrationPage = params => request({ url: '/vlsAlgorithmOrchestration/page', method: 'get', params })
export const saveOrchestrationRecord = data => request({ url: '/vlsAlgorithmOrchestration/save', method: 'post', data })
export const updateOrchestrationRecord = data => request({ url: '/vlsAlgorithmOrchestration/update', method: 'post', data })
export const removeOrchestrationRecord = id => request({ url: '/vlsAlgorithmOrchestration/remove', method: 'get', params: { ids: String(id) } })
