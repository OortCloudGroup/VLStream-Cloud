// 视频广场的设备统计：由页面写入，左侧导航侧栏在底部直接渲染，避免 Teleport 目标缺失。
import { reactive } from 'vue'

export const sidebarDeviceStats = reactive({
  active: false,
  total: 0,
  online: 0,
  offline: 0,
  unknown: 0,
})

export function setSidebarDeviceStats(values = {}) {
  sidebarDeviceStats.active = true
  sidebarDeviceStats.total = Number(values.total) || 0
  sidebarDeviceStats.online = Number(values.online) || 0
  sidebarDeviceStats.offline = Number(values.offline) || 0
  sidebarDeviceStats.unknown = Number(values.unknown) || 0
}

export function clearSidebarDeviceStats() {
  sidebarDeviceStats.active = false
}
