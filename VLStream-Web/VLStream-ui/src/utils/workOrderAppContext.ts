import { listWorkOrder } from '@/api/processui'

const STORAGE_KEY = 'taskCenterClassify'
const DEFAULT_WORK_ORDER_APP_ID = '000000-event-management-app'
const DEFAULT_WORK_ORDER_APP_NAME = '事件管理'

export function getStoredWorkOrderAppContext() {
  const raw = window.sessionStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw)
    return parsed?.appId ? parsed : null
  } catch (error) {
    window.sessionStorage.removeItem(STORAGE_KEY)
    return null
  }
}

export function saveWorkOrderAppContext(app: any) {
  if (!app?.appId) return null
  window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(app))
  return app
}

export async function resolveWorkOrderAppContext() {
  const stored = getStoredWorkOrderAppContext()
  if (stored?.appId) return stored

  const response: any = await listWorkOrder({ pageNum: 1, pageSize: 20 })
  const list = Array.isArray(response?.data) ? response.data : Array.isArray(response?.rows) ? response.rows : []
  const eventManagement =
    list.find((item: any) => item?.appId === DEFAULT_WORK_ORDER_APP_ID) ||
    list.find((item: any) => item?.applicationName === DEFAULT_WORK_ORDER_APP_NAME) ||
    list.find((item: any) => item?.appId)

  return saveWorkOrderAppContext(eventManagement)
}
