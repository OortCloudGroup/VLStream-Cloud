import AppConfig from '@/config/AppConfig'
import { event_group_delete_v2, event_group_save_v2, event_group_tree } from '@/api/smartCity/events'

const GROUP_TYPE_TAG = 3

const responseList = (response) => response?.data?.list || response?.list || []

function mapNodes(nodes, level = 1) {
  return nodes.map(node => ({
    id: String(node.uid),
    tagName: node.name,
    description: node.remark || '',
    categoryType: 'own',
    parentId: node.puid || null,
    level,
    sortOrder: node.sort || 0,
    createTime: node.created_at || node.createTime,
    updateTime: node.updated_at || node.updateTime,
    children: mapNodes(node.children || [], level + 1)
  }))
}

async function loadAllTags() {
  const response = await event_group_tree({
    app_id: AppConfig.events.appID,
    group_type: GROUP_TYPE_TAG
  })
  if (response?.code && response.code !== 200) {
    throw new Error(response.msg || '加载标签失败')
  }
  return mapNodes(responseList(response))
}

function flatten(nodes) {
  return nodes.reduce((result, node) => result.concat(node, flatten(node.children || [])), [])
}

/**
 * Adapt active-safety V2 tags to the existing VideoAggregation TagManagement view model.
 */
export async function getTagTree() {
  const children = await loadAllTags()
  return {
    data: [
      {
        id: 'own',
        tagName: '自有',
        categoryType: 'own',
        level: 0,
        children
      },
      {
        id: 'public',
        tagName: '公共',
        categoryType: 'public',
        level: 0,
        children: []
      }
    ]
  }
}

export async function getTagManagementPage(params = {}) {
  const tags = flatten(await loadAllTags())
  const keyword = String(params.keyword || '').trim().toLowerCase()
  let filtered = tags.filter(tag => {
    if (params.tagId && tag.id !== String(params.tagId)) return false
    if (params.parentId && tag.parentId !== String(params.parentId)) return false
    if (params.level && tag.level !== Number(params.level)) return false
    return !keyword || `${tag.tagName}${tag.description}`.toLowerCase().includes(keyword)
  })
  const current = Number(params.current || 1)
  const size = Number(params.size || 10)
  const total = filtered.length
  filtered = filtered.slice((current - 1) * size, current * size)
  return {
    data: {
      records: filtered,
      total,
      current,
      size,
      pages: Math.ceil(total / size)
    }
  }
}

function parentUid(parentId) {
  return parentId === 'own' || parentId === 'public' || !parentId ? undefined : String(parentId)
}

export async function createTag(data) {
  return event_group_save_v2({
    app_id: AppConfig.events.appID,
    group_type: GROUP_TYPE_TAG,
    puid: parentUid(data.parentId),
    name: data.tagName,
    remark: data.description || ''
  })
}

export async function updateTag(id, data) {
  return event_group_save_v2({
    app_id: AppConfig.events.appID,
    group_type: GROUP_TYPE_TAG,
    uid: String(id),
    name: data.tagName,
    remark: data.description || ''
  })
}

export function deleteTag(id) {
  return event_group_delete_v2({ app_id: AppConfig.events.appID, uid: String(id) })
}

export function batchDeleteTags(ids) {
  return Promise.all(ids.map(deleteTag))
}

export function getTagDevices() {
  return Promise.resolve({ data: [] })
}
