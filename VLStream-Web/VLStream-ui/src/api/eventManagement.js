/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/request'

/**
 * Query event list
 * @param {Object} params
 * @param {number} params.current current
 * @param {number} params.size
 * @param {string} [params.eventType] event
 * @param {string} [params.eventStatus] event
 * @param {string} [params.eventLevel] event
 * @param {string} [params.keyword]
 * @param {string} [params.startTime] start yyyy-MM-dd HH:mm:ss
 * @param {string} [params.endTime] finish yyyy-MM-dd HH:mm:ss
 */
export function getEventPage(params) {
  return request({
    url: '/vlsEventManagement/page',
    method: 'get',
    params
  })
}

/**
 * Get event
 * @param {number} id eventID
 */
export function getEventById(id) {
  return request({
    url: `/vlsEventManagement/${id}`,
    method: 'get'
  })
}

/**
 * new event
 * @param {Object} data eventdata
 */
export function createEvent(data) {
  return request({
    url: '/vlsEventManagement',
    method: 'post',
    data
  })
}

/**
 * new event
 * @param {Object} data eventdata ( id)
 */
export function updateEvent(data) {
  return request({
    url: '/vlsEventManagement',
    method: 'put',
    data
  })
}

/**
 * new event
 * @param {number} id eventID
 * @param {Object} params
 * @param {string} params.status event
 * @param {string} [params.executor] Execute
 * @param {string} [params.handleResult] Process
 */
export function updateEventStatus(id, params) {
  return request({
    url: `/vlsEventManagement/${id}/status`,
    method: 'patch',
    params
  })
}

/**
 * Delete event
 * @param {number} id eventID
 */
export function deleteEvent(id) {
  return request({
    url: `/vlsEventManagement/${id}`,
    method: 'delete'
  })
}

/**
 * Batch delete event
 * @param {Array<number>} ids eventID
 */
export function batchDeleteEvents(ids) {
  return request({
    url: '/vlsEventManagement/batch',
    method: 'delete',
    data: ids
  })
}
