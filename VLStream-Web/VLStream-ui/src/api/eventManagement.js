import request from '@/utils/request'

/**
 * 分页查询事件列表
 * @param {Object} params
 * @param {number} params.current 当前页
 * @param {number} params.size 每页数量
 * @param {string} [params.eventType] 事件类型
 * @param {string} [params.eventStatus] 事件状态
 * @param {string} [params.eventLevel] 事件级别
 * @param {string} [params.keyword] 搜索关键词
 * @param {string} [params.startTime] 上报开始时间 yyyy-MM-dd HH:mm:ss
 * @param {string} [params.endTime] 上报结束时间 yyyy-MM-dd HH:mm:ss
 */
export function getEventPage(params) {
  return request({
    url: '/vlsEventManagement/page',
    method: 'get',
    params
  })
}

/**
 * 获取事件详情
 * @param {number} id 事件ID
 */
export function getEventById(id) {
  return request({
    url: `/vlsEventManagement/${id}`,
    method: 'get'
  })
}

/**
 * 新建事件
 * @param {Object} data 事件数据
 */
export function createEvent(data) {
  return request({
    url: '/vlsEventManagement',
    method: 'post',
    data
  })
}

/**
 * 更新事件
 * @param {Object} data 事件数据（需包含id）
 */
export function updateEvent(data) {
  return request({
    url: '/vlsEventManagement',
    method: 'put',
    data
  })
}

/**
 * 更新事件状态
 * @param {number} id 事件ID
 * @param {Object} params
 * @param {string} params.status 事件状态
 * @param {string} [params.executor] 执行人
 * @param {string} [params.handleResult] 处理结果
 */
export function updateEventStatus(id, params) {
  return request({
    url: `/vlsEventManagement/${id}/status`,
    method: 'patch',
    params
  })
}

/**
 * 删除事件
 * @param {number} id 事件ID
 */
export function deleteEvent(id) {
  return request({
    url: `/vlsEventManagement/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除事件
 * @param {Array<number>} ids 事件ID列表
 */
export function batchDeleteEvents(ids) {
  return request({
    url: '/vlsEventManagement/batch',
    method: 'delete',
    data: ids
  })
}
