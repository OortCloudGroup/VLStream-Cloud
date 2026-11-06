// Existing menu permissions remain authoritative for pages already in sys_menu.
const existingPermissions = {
  '/device-management': 'vls:menu:legacy-device-management',
  '/system/users': 'system:user:list',
  '/system/roles': 'system:role:list',
  '/system/menus': 'system:menu:list',
  '/system/depts': 'system:dept:list',
  '/system/posts': 'system:post:list',
  '/system/device-firmwares': 'vls:firmware:list',
  '/system/platform-logo': 'system:platformLogo:list',
  '/event-management': 'active-safety:events:list',
  '/active-safety/work-orders/my': 'active-safety:workorder:my',
  '/active-safety/work-orders/pending': 'active-safety:workorder:pending',
  '/active-safety/work-orders/completed': 'active-safety:workorder:completed',
  '/active-safety/work-orders/claimable': 'active-safety:workorder:claimable',
  '/active-safety/settings/secure': 'active-safety:settings:secure',
  '/active-safety/settings/work-orders': 'active-safety:settings:workorder'
}

export const pagePaths = [
  '/video-square', '/video-playback', '/device-management', '/vlstream/device',
  '/isup/isupDevice', '/ehome/device', '/rtsp/rtspDevice', '/onvif/cameraManage',
  '/gbmanger/device', '/gbmanger/cloudRecord', '/gbmanger/streamPush', '/gbmanger/streamProxy',
  '/gbmanger/platform', '/gbmanger/recordPlan', '/gbmanger/administrativeGrouping',
  '/gbmanger/thoroughfare', '/gbmanger/alarm', '/gbmanger/node', '/gbmanger/wvpLive', '/custom/device',
  '/event-management', '/llm-review-tasks', '/active-safety/work-orders/my',
  '/active-safety/work-orders/pending', '/active-safety/work-orders/completed',
  '/active-safety/work-orders/claimable', '/active-safety/settings/secure',
  '/active-safety/settings/work-orders', '/algorithm-arrangement', '/scene-governance',
  '/algorithm-task', '/task-status', '/intelligent-analysis-request', '/algorithm-management',
  '/llm-provider-management', '/data-management', '/smart-annotation', '/algorithm-standard',
  '/algorithm-training', '/algorithm-model', '/container-instances', '/compute-market',
  '/compute-management', '/system/users', '/system/roles', '/system/menus', '/system/depts',
  '/system/posts', '/system/device-firmwares', '/system/platform-logo'
]

const pageSet = new Set(pagePaths)
const devicePages = ['/vlstream/device', '/isup/isupDevice', '/ehome/device', '/rtsp/rtspDevice',
  '/onvif/cameraManage', '/gbmanger/device', '/custom/device']
const workOrderPages = ['/active-safety/work-orders/my', '/active-safety/work-orders/pending',
  '/active-safety/work-orders/completed', '/active-safety/work-orders/claimable',
  '/active-safety/settings/work-orders']
// Legacy workflow list grants keep periodic work orders reachable. The server
// independently filters ordinary work orders when the page grant is absent.
const periodicWorkOrderListPermissions = {
  '/active-safety/work-orders/my': 'workflow:workorder:selectPageOwnWorkOrderList',
  '/active-safety/work-orders/pending': 'workflow:workorder:selectPageTodoWorkOrderList',
  '/active-safety/work-orders/completed': 'workflow:workorder:selectPageFinishedWorkOrderList',
  '/active-safety/work-orders/claimable': 'workflow:workorder:claimWorkOrderList'
}
const trainingPlatformPages = ['/algorithm-standard', '/algorithm-training', '/algorithm-model']

// Detail and legacy routes inherit the relevant visible page's access, not a new implicit grant.
const routeAliases = {
  '/device-edit': ['/device-management'],
  '/device-config': ['/device-management'],
  '/device-ai-event': ['/device-management'],
  '/camera-settings': ['/device-management'],
  '/dahua/dahuaDevice': devicePages,
  '/channel/list/index/:deviceId/:parentChannelId?': ['/gbmanger/device'],
  '/channel/gbRecordDetail/index/:deviceId/:channelId': ['/gbmanger/device'],
  '/channel/cloudRecordDetail/index': ['/gbmanger/cloudRecord'],
  '/platform/chooseChannel/index/:platformId': ['/gbmanger/platform'],
  '/recordPlan/associatedChannel/index/:id': ['/gbmanger/recordPlan'],
  '/gbmanger/node/addMediaServer/index': ['/gbmanger/node'],
  '/gbmanger/node/updateMediaServer/index': ['/gbmanger/node'],
  '/dataset-annotation': ['/data-management'],
  '/algorithm-training-platform': trainingPlatformPages,
  '/ssh-connection': ['/compute-management'],
  '/monitoring-alarm': ['/event-management'],
  '/design-drawing': ['/scene-governance'],
  '/scene-edit': ['/scene-governance'],
  '/active-safety/events/secure': ['/event-management'],
  '/workOrderDetails': workOrderPages,
  // These screens are placeholders, not separate implemented permissions.
  '/system/data-scopes': ['/system/menus'],
  '/system/api-scopes': ['/system/menus']
}

export function isManagedMenuRoute(path) {
  return pageSet.has(path) || Object.prototype.hasOwnProperty.call(routeAliases, path)
}

export function permissionForPage(path) {
  if (!pageSet.has(path)) return null
  return existingPermissions[path] || `vls:menu:${path.slice(1).replaceAll('/', ':')}`
}

export function canAccessPage(path, permissions) {
  const required = routeAliases[path] || (pageSet.has(path) ? [path] : null)
  if (!required) return true // login, workspace, personal account and other baseline routes
  if (!Array.isArray(permissions)) return false
  if (permissions.includes('*:*:*')) return true
  return required.some(page => permissions.includes(permissionForPage(page)) ||
    (periodicWorkOrderListPermissions[page] && permissions.includes(periodicWorkOrderListPermissions[page])))
}

export function filterMenuRoutes(routes, permissions) {
  return routes.flatMap(item => {
    if (Array.isArray(item.children) && item.children.length) {
      const children = filterMenuRoutes(item.children, permissions)
      return children.length ? [{ ...item, children }] : []
    }
    return canAccessPage(item.path, permissions) ? [item] : []
  })
}
