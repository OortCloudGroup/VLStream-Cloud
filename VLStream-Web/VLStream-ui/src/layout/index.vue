<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div class="layout-container">
    <!--  -->
    <el-header class="layout-header" height="60px">
      <div class="header-content">
        <div class="logo">
          <img
            v-if="platformLogo?.logoUrl && !customLogoFailed"
            class="custom-brand-logo"
            :src="platformLogo.logoUrl"
            :alt="platformLogo.description || 'VLStream Cloud'"
            @error="customLogoFailed = true"
          >
          <template v-else>
            <div class="logo-icon">
            <svg width="40" height="40" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="20" cy="20" r="18" fill="white" stroke="#1A53FF" stroke-width="2"/>
              <text x="20" y="25" text-anchor="middle" fill="#1A53FF" font-family="Arial, sans-serif" font-size="14" font-weight="bold">VLS</text>
            </svg>
            </div>
            <h2>VLStream Cloud</h2>
          </template>
          <!--  -->
          <CollapseToggle
            class="sidebar-toggle"
            :is-expanded="compactVideoNav ? videoNavOpen : !sidebarCollapsed"
            @toggle="toggleSidebar"
          />
          <PlatformAppsEntry />
        </div>

        <!-- menu -->
        <div class="top-menu">
          <div
            v-for="menu in topMenus"
            :key="menu.key"
            class="menu-item"
            :class="{ active: activeTopMenu === menu.key }"
            @click="handleTopMenuClick(menu.key)"
          >
            {{ menu.title }}
          </div>
        </div>

        <div class="header-right" :class="{ 'header-right--platform': tenantMode === 'multi' }">
          <LocaleSwitcher v-if="tenantMode !== 'multi' || rightInfoStatus !== 'ready'" />
          <OortCloudPopover v-if="(tenantMode === 'multi' && rightInfoStatus === 'failed') || (tenantMode === 'single' && menuIsSystemAdmin)" :tenant-mode="tenantMode" />

          <RightInfoBar v-if="tenantMode === 'multi'" @ready-change="rightInfoStatus = $event ? 'ready' : 'failed'" />

          <PlatformHeaderRight
            v-if="tenantMode === 'multi' && rightInfoStatus === 'failed'"
            :fallback-user="currentUser"
            :fallback-tenant="currentTenant"
            @switch-tenant="switchTenant"
          />

          <!-- userinfo -->
          <el-dropdown v-if="tenantMode === 'single'">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ currentUser.userName }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/user-profile')">{{ tp('个人设置') }}</el-dropdown-item>
                <el-dropdown-item @click="$router.push(getCloudPlatformUserPath())">{{ tp('OortCloud 云账号') }}</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">{{ tp('退出登录') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <!-- main -->
    <div class="layout-main" :class="{ 'compact-video-nav': compactVideoNav }" @keydown.esc="videoNavOpen = false">
      <!-- - only in non- -->
      <div
        v-if="showSidebar"
        v-show="!compactVideoNav || videoNavOpen"
        class="layout-sidebar"
        :class="{ 'is-collapsed': sidebarCollapsed && !compactVideoNav }"
        :style="{ width: sidebarCollapsed && !compactVideoNav ? '64px' : '260px' }"
      >
        <el-menu
          :key="sidebarActivePath"
          :default-active="sidebarActivePath"
          :default-openeds="defaultOpenMenuPaths"
          router
          class="sidebar-menu"
          background-color="transparent"
          text-color="var(--el-text-color-regular)"
          active-text-color="var(--el-color-primary)"
          :collapse="sidebarCollapsed && !compactVideoNav"
          @select="videoNavOpen = false"
          style="--el-menu-bg-color: transparent; background: transparent;"
        >
          <SidebarMenuNode
            :items="currentMenuRoutes"
            :get-menu-icon="getMenuIcon"
          />
        </el-menu>
        <!-- 设备统计条：固定在侧栏底部，不随菜单滚动 -->
        <div v-if="sidebarDeviceStats.active" id="app-sidebar-stats" class="app-sidebar-stats">
          <div class="device-stats">
            <div class="stat-item total">
              <div class="stat-icon"><img src="@/assets/设备-总设备@3x.png" :alt="$tp('总设备')" width="20" height="20"></div>
              <div class="stat-content"><span class="stat-label">{{ $tp('总设备：') }}</span><span class="stat-value">{{ sidebarDeviceStats.total }}</span></div>
            </div>
            <div class="stat-item online">
              <div class="stat-icon"><img src="@/assets/设备-在线@3x.png" :alt="$tp('在线')" width="20" height="20"></div>
              <div class="stat-content"><span class="stat-label">{{ $tp('在线：') }}</span><span class="stat-value">{{ sidebarDeviceStats.online }}</span></div>
            </div>
            <div class="stat-item offline">
              <div class="stat-icon"><img src="@/assets/设备-离线@3x.png" :alt="$tp('离线')" width="20" height="20"></div>
              <div class="stat-content"><span class="stat-label">{{ $tp('离线：') }}</span><span class="stat-value">{{ sidebarDeviceStats.offline }}</span></div>
            </div>
            <div class="stat-item unknown">
              <div class="stat-icon"><img src="@/assets/设备-未知@3x.png" :alt="$tp('未知状态')" width="20" height="20"></div>
              <div class="stat-content"><span class="stat-label">{{ $tp('未知状态') }}：</span><span class="stat-value">{{ sidebarDeviceStats.unknown }}</span></div>
            </div>
          </div>
        </div>
      </div>

      <!--  -->
      <button v-if="compactVideoNav && videoNavOpen" type="button" class="video-nav-shade" :aria-label="tp('收起侧边栏')" @click="videoNavOpen = false" />
      <div class="layout-content" :inert="compactVideoNav && videoNavOpen ? '' : null">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { translatePhrase as uiText } from '@/i18n'

import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import { ElMessage, ElMessageBox } from 'element-plus'
import { AuthManager } from '@/utils/auth'
import { logoutUser } from '@/api/auth'
import { logoutModelHubSession } from '@/api/modelHubUser'
import { getCloudPlatformUserPath } from '@/utils/modelHubAuth'
import { getCurrentPlatformLogo } from '@/api/platformLogo'
import { logSafeRequestError } from '@/utils/safeRequestError.mjs'
import { isLocalEventRoute } from '@/utils/localEventAccess.mjs'
import { filterMenuRoutes } from '@/utils/menuAccess.mjs'
import { menuIsSystemAdmin, menuPermissions, navigationUser } from '@/utils/menuAccessState'
import { sidebarDeviceStats } from '@/utils/sidebarDeviceStats.mjs'
import {
  User,
  ArrowDown,
  VideoCamera,
  Monitor,
  Bell,
  Setting,
  DataAnalysis,
  Connection,
  Search,
  Collection,
  School,
  Warning,
  VideoPlay,
  OfficeBuilding,
  DArrowLeft,
  DArrowRight,
  UserFilled,
  Menu as MenuIcon,
  Briefcase,
  Key,
  Lock,
  UploadFilled
} from '@element-plus/icons-vue'
import CollapseToggle from '@/components/CollapseToggle.vue'
import SidebarMenuNode from './SidebarMenuNode.vue'
import OortCloudPopover from './OortCloudPopover.vue'
import { syncLlmAuthorization } from '@/utils/llmAuthorization'
import { getOortCloudLoginIntent } from '@/utils/oortCloudLoginIntent.mjs'
import { getOortCloudSession } from '@/utils/oortCloudSession'
import PlatformHeaderRight from './PlatformHeaderRight.vue'
import RightInfoBar from './RightInfoBar.vue'
import PlatformAppsEntry from './platform-header/PlatformAppsEntry.vue'
import LocaleSwitcher from './LocaleSwitcher.vue'
import { translatePhrase } from '@/i18n'
import deviceManagementIcon from '@/assets/img/svg/device-management.svg'
import vlstreamIcon from '@/assets/img/svg/vlstream.svg'
import isupIcon from '@/assets/img/svg/isup.svg'
import ehomeIcon from '@/assets/img/svg/ehome.svg'
import rtspIcon from '@/assets/img/svg/rtsp.svg'
import onvifIcon from '@/assets/img/svg/onvif.svg'
import gbIcon from '@/assets/img/svg/gb.svg'
import dahuaIcon from '@/assets/img/svg/dahua.svg'

const route = useRoute()
const router = useRouter()
const localEventRoute = computed(() => isLocalEventRoute(route.meta))

//
const authManager = new AuthManager()


const currentUser = computed(() => navigationUser.value || {})
const rightInfoStatus = ref('loading')
const platformLogo = ref(null)
const customLogoFailed = ref(false)

// current menu
const activeTopMenu = ref('workspace')

//
const sidebarCollapsed = ref(false)
const narrowViewport = useMediaQuery('(max-width: 700px)')
const compactVideoNav = computed(() => route.path === '/video-square' && narrowViewport.value)
const videoNavOpen = ref(false)
watch(compactVideoNav, () => { videoNavOpen.value = false })

//
const toggleSidebar = () => {
  if (compactVideoNav.value) {
    videoNavOpen.value = !videoNavOpen.value
    return
  }
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const currentTenant = computed(() => ({ id: currentUser.value.tenantId || '' }))
const tenantMode = ref('loading')

let llmSyncGeneration = 0
let layoutActive = true
const llmSessionEvents = ['modelHubAuthChanged', 'tokenUpdated', 'userTokenUpdated', 'crossSystemTokenUpdated', 'autoCrossSystemTokenUpdated']
const syncDefaultLlmAuthorization = () => {
  const generation = ++llmSyncGeneration
  const mode = tenantMode.value
  if (localEventRoute.value || !['single', 'multi'].includes(mode)) return
  const loginIntent = getOortCloudLoginIntent(getOortCloudSession(mode))
  void syncLlmAuthorization(mode, {
    isCurrent: () => layoutActive && generation === llmSyncGeneration
      && tenantMode.value === mode && !localEventRoute.value
  }).catch(() => {
    if (loginIntent && layoutActive && generation === llmSyncGeneration) {
      ElMessage.warning(uiText('OortCloud 登录后的模型授权未完成，请在大模型管理中重试。'))
    }
  })
}
watch(localEventRoute, syncDefaultLlmAuthorization)


//
const switchingTenant = ref(false)
const switchTenant = async (tenant) => {
  if (switchingTenant.value || !tenant?.id || String(tenant.id) === currentTenant.value.id) return
  switchingTenant.value = true
  const identity = authManager.getIdentitySnapshot()
  try {
    await ElMessageBox.confirm(uiText('确认切换到租户“{value0}”吗？', { value0: tenant.name }), uiText('切换租户'), {
      confirmButtonText: uiText('切换'),
      cancelButtonText: uiText('取消'),
      type: 'warning'
    })
    if (!authManager.isIdentityCurrent(identity)) return
    if (!(await authManager.switchTenant(tenant.id, identity))) return
    ElMessage.success(uiText('租户切换成功'))
    window.location.reload()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.response?.data?.msg || error?.message || uiText('租户切换失败'))
    }
  } finally {
    switchingTenant.value = false
  }
}

// exit notification after , .
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(uiText('确认退出当前账号吗？'), uiText('退出登录'), {
      confirmButtonText: uiText('退出'),
      cancelButtonText: uiText('取消'),
      type: 'warning'
    })
  } catch (error) {
    return
  }

  authManager.invalidatePendingIdentity()
  const identity = authManager.getIdentitySnapshot()
  const [oortCloudLogout, vlstreamLogout] = await Promise.allSettled([
    logoutModelHubSession(),
    logoutUser()
  ])
  if (!authManager.isIdentityCurrent(identity, true)) return
  if (oortCloudLogout.status === 'rejected') {
    logSafeRequestError('OortCloud 远端登出接口调用失败', oortCloudLogout.reason, console.warn)
  }
  if (vlstreamLogout.status === 'rejected') {
    logSafeRequestError('VLStream 后端登出接口调用失败', vlstreamLogout.reason, console.warn)
  }

  authManager.clearAllTokens()
  ElMessage.success(uiText('已退出登录'))
  await router.replace('/login')
}

// menuconfiguration
const allTopMenus = [
  { key: 'workspace', title: '工作台' },
  { key: 'video-aggregation', title: '视频汇聚' },
  { key: 'decision-ai', title: '决策式AI' },
  { key: 'algorithm-warehouse', title: '算法仓库' },
  { key: 'ai-computing', title: 'AI算力调度' },
  { key: 'system-management', title: '系统管理' }
]

const topMenus = computed(() => allTopMenus
  .filter(menu => menu.key !== 'system-management' || tenantMode.value === 'single')
  .filter(menu => menu.key === 'workspace' || getMenuRoutes(menu.key).length > 0)
  .map(menu => ({ ...menu, title: translatePhrase(menu.title) })))

const tp = source => translatePhrase(source)

// menu
const menuRoutesMap = {
  'workspace': [],
  'video-aggregation': [
    { path: '/video-square', meta: { title: '视频广场', icon: '视频广场' } },
    { path: '/video-playback', meta: { title: '视频回放', icon: '视频回放' } },
    // { path: '/monitoring-alarm', meta: { title: ' alarm / alert', icon: ' alarm / alert' } },
    {
      path: 'device-management-menu',
      meta: { title: '设备管理', icon: '设备管理' },
      children: [
        { path: '/vlstream/device', meta: { title: 'VLStream协议', icon: 'wvp-vls' } },
        { path: '/isup/isupDevice', meta: { title: 'ISUP协议', icon: 'wvp-haikang' } },
        { path: '/ehome/device', meta: { title: 'EHome协议', icon: 'wvp-ehome' } },
        { path: '/rtsp/rtspDevice', meta: { title: 'RTSP协议', icon: 'wvp-rtsp' } },
        { path: '/onvif/cameraManage', meta: { title: 'ONVIF协议', icon: 'wvp-onvif' } },
        {
          path: 'gb-protocol-menu',
          meta: { title: '国标协议', icon: 'wvp-gb' },
          children: [
            { path: '/gbmanger/device', meta: { title: '国标设备', icon: '设备管理' } },
            { path: '/gbmanger/cloudRecord', meta: { title: '云端录像', icon: '视频回放' } },
            { path: '/gbmanger/streamPush', meta: { title: '推流列表', icon: '视频广场' } },
            { path: '/gbmanger/streamProxy', meta: { title: '拉流列表', icon: '视频广场' } },
            { path: '/gbmanger/platform', meta: { title: '国标级联', icon: '设备管理' } },
            { path: '/gbmanger/recordPlan', meta: { title: '录像计划', icon: '视频回放' } },
            { path: '/gbmanger/administrativeGrouping', meta: { title: '行政分组', icon: '分组管理' } },
            { path: '/gbmanger/thoroughfare', meta: { title: '通道管理', icon: '设备管理' } },
            { path: '/gbmanger/alarm', meta: { title: '报警管理', icon: '事件' } },
            { path: '/gbmanger/node', meta: { title: '节点管理', icon: '设备管理' } },
            { path: '/gbmanger/wvpLive', meta: { title: '分屏监控', icon: '视频广场' } }
          ]
        },
        { path: '/custom/device', meta: { title: '自定义', icon: '设备管理' } }
      ]
    },
    // { path: '/camera-settings', meta: { title: 'Set ', icon: ' Set ' } },
  ],
  'decision-ai': [
    {
      path: 'event-management-menu',
      meta: { title: '事件管理', icon: '事件' },
      children: [
        { path: '/event-management', meta: { title: '主动安全', icon: '监控告警' } },
        { path: '/llm-review-tasks', meta: { title: '大模型复核', icon: '智能分析结果' } }
      ]
    },
    {
      path: '/active-safety/work-orders',
      meta: { title: '工单管理', icon: '岗位管理' },
      children: [
        { path: '/active-safety/work-orders/my', meta: { title: '我的工单', icon: '角色管理' } },
        { path: '/active-safety/work-orders/pending', meta: { title: '待办工单', icon: '事件' } },
        { path: '/active-safety/work-orders/completed', meta: { title: '已办工单', icon: '标签管理' } },
        { path: '/active-safety/work-orders/claimable', meta: { title: '可接工单', icon: '智能分析结果' } }
      ]
    },
    {
      path: '/active-safety/settings',
      meta: { title: '系统管理', icon: '摄像机设置' },
      children: [
        { path: '/active-safety/settings/secure', meta: { title: '主动安全', icon: '摄像机设置' } },
        { path: '/active-safety/settings/work-orders', meta: { title: '工单设置', icon: '摄像机设置' } }
      ]
    },
    {
      path: 'edge-intelligence-menu',
      meta: { title: '端侧智能', icon: '端侧智能' },
      children: [
        { path: '/algorithm-arrangement', meta: { title: '算法配置', icon: '算法配置' } }
      ]
    },
    {
      path: 'cloud-device-intelligence-menu',
      meta: { title: '云端智能', icon: '云端智能' },
      children: [
        { path: '/cloud-algorithm-config', meta: { title: '算法配置', icon: '算法配置' } }
      ]
    },
    {
      path: 'cloud-intelligence-menu',
      meta: { title: '智能编排', icon: '云端智能' },
      children: [
        { path: '/scene-governance', meta: { title: '场景治理', icon: '场景治理' } },
        { path: '/algorithm-task', meta: { title: '任务配置', icon: '任务配置' } },
        { path: '/task-status', meta: { title: '任务状态', icon: '任务状态' } },
        { path: '/intelligent-analysis-request', meta: { title: '智能分析', icon: '智能分析' } }
      ]
    }
  ],
  'algorithm-warehouse': [
    { path: '/algorithm-management', meta: { title: '算法管理', icon: '算法管理' } },
    { path: '/llm-provider-management', meta: { title: '大模型管理', icon: '算法管理' } },
    {
      path: '/algorithm-training-platform',
      meta: { title: '算法训练平台', icon: '算法训练平台' },
      children: [
        { path: '/data-management', meta: { title: '数据集管理', icon: '算法标注' } },
        { path: '/smart-annotation', meta: { title: '智能标注', icon: '算法标注' } },
        { path: '/llm-smart-annotation', meta: { title: '大模型标注', icon: '算法标注' } },
        { path: '/algorithm-standard', meta: { title: '算法标注', icon: '算法标注' } },
        { path: '/algorithm-training', meta: { title: '算法训练', icon: '算法训练' } },
        { path: '/algorithm-model', meta: { title: '算法模型', icon: '算法模型' } }
      ]
    },
  ],
  'ai-computing': [
    { path: '/container-instances', meta: { title: '训练任务', icon: '容器实例' } },
    { path: '/compute-market', meta: { title: '算力市场', icon: '容器实例' } },
    { path: '/compute-management', meta: { title: '算力管理', icon: '容器实例' } }
  ],
  'system-management': [
    { path: '/system/users', meta: { title: '用户管理', icon: '用户管理' } },
    { path: '/system/roles', meta: { title: '角色管理', icon: '角色管理' } },
    { path: '/system/menus', meta: { title: '菜单管理', icon: '菜单管理' } },
    { path: '/system/depts', meta: { title: '部门管理', icon: '部门管理' } },
    { path: '/system/posts', meta: { title: '岗位管理', icon: '岗位管理' } },
    { path: '/system/data-scopes', meta: { title: '数据权限', icon: '数据权限' } },
    { path: '/system/api-scopes', meta: { title: '接口权限', icon: '接口权限' } },
    { path: '/system/device-firmwares', meta: { title: 'VLS协议设备固件管理', icon: '固件管理' } },
    { path: '/system/platform-logo', meta: { title: '平台设置', icon: '平台设置' } }
  ]
}

const getMenuRoutes = (menuKey) => {
  const routes = menuRoutesMap[menuKey] || []
  if (menuKey !== 'decision-ai') {
    return filterMenuRoutes(routes, menuPermissions.value)
  }

  // not menu, user menu.
  if (tenantMode.value === 'loading') {
    return []
  }

  if (tenantMode.value !== 'multi') {
    return filterMenuRoutes(routes, menuPermissions.value)
  }

  return filterMenuRoutes([
    { path: '/event-management', meta: { title: '事件管理', icon: '事件' } },
    { path: '/llm-review-tasks', meta: { title: '大模型复核', icon: '智能分析结果' } },
    ...routes.filter(item => item.path !== 'event-management-menu')
  ], menuPermissions.value, tenantMode.value)
}

// whether
const showSidebar = computed(() => {
  return activeTopMenu.value !== 'workspace'
})

// : sub page menu path ( device → device )
const sidebarActivePath = computed(() => {
  return route.meta?.parentPath || route.path
})

// current menu
const currentMenuRoutes = computed(() => {
  return getMenuRoutes(activeTopMenu.value)
})

const menuContainsPath = (items, routePath) => {
  return items.some(item => (
    item.path === routePath
      || (item.children && menuContainsPath(item.children, routePath))
  ))
}

const getOpenMenuPaths = (items, routePath) => {
  return items.reduce((openedPaths, item) => {
    if (!item.children || item.children.length === 0) {
      return openedPaths
    }

    const itemContainsPath = item.path === routePath || menuContainsPath(item.children, routePath)
    if (!itemContainsPath) {
      return openedPaths
    }

    openedPaths.push(item.path)
    return openedPaths.concat(getOpenMenuPaths(item.children, routePath))
  }, [])
}

const getFirstNavigablePath = (items) => {
  for (const item of items) {
    if (!item.children || item.children.length === 0) {
      return item.path
    }

    const childPath = getFirstNavigablePath(item.children)
    if (childPath) {
      return childPath
    }
  }

  return ''
}

// current page in menu , user current page sub menu.
const defaultOpenMenuPaths = computed(() => {
  return getOpenMenuPaths(currentMenuRoutes.value, sidebarActivePath.value)
})

/**
 * Process menu event
 * if is " ", /workspace page
 * menu
 * @param {string} menuKey menu key
 */
const handleTopMenuClick = (menuKey) => {
  activeTopMenu.value = menuKey

  // menu
  if (menuKey === 'workspace') {
    router.push('/workspace') // page
  } else {
    const routes = getMenuRoutes(menuKey)
    if (routes && routes.length > 0) {
      const firstNavigablePath = getFirstNavigablePath(routes)
      if (firstNavigablePath) {
        router.push(firstNavigablePath) // menu
      }
    }
  }
}

// current menu
const getActiveMenuByRoute = (routePath) => {
  if (route.meta?.parentMenu) {
    return route.meta.parentMenu
  }
  // device related sub page
  if (
    routePath.startsWith('/device-') ||
    routePath === '/camera-settings'
  ) {
    return 'video-aggregation'
  }
  for (const menuKey of Object.keys(menuRoutesMap)) {
    const routes = getMenuRoutes(menuKey)
    if (menuContainsPath(routes, routePath)) {
      return menuKey
    }
  }
  return 'workspace'
}

// , Set menu
watch(() => route.path, (newPath) => {
  const activeMenu = getActiveMenuByRoute(newPath)
  if (activeMenu !== activeTopMenu.value) {
    activeTopMenu.value = activeMenu
  }
}, { immediate: true })

let logoRequestId = 0
const loadPlatformLogo = async () => {
  const requestId = ++logoRequestId
  const identity = authManager.getIdentitySnapshot()
  const current = () => layoutActive && requestId === logoRequestId && authManager.isIdentityCurrent(identity)
  try {
    const response = await getCurrentPlatformLogo({ localSessionOnly: localEventRoute.value, silentError: true, timeout: 10_000 })
    if (!current()) return
    customLogoFailed.value = false
    platformLogo.value = response?.data?.systemDefault ? null : response?.data
  } catch (error) {
    if (!current()) return
    platformLogo.value = null
    logSafeRequestError('加载平台标识失败，使用系统默认标识', error, console.warn)
  }
}

onMounted(async () => {
  window.addEventListener('platform-logo-changed', loadPlatformLogo)
  llmSessionEvents.forEach(name => window.addEventListener(name, syncDefaultLlmAuthorization))
  void loadPlatformLogo()
  try {
    const mode = await authManager.getTenantMode()
    if (!layoutActive) return
    tenantMode.value = mode
    syncDefaultLlmAuthorization()
  } catch (error) {
    if (layoutActive) logSafeRequestError('加载租户模式失败', error, console.warn)
  }
})

onUnmounted(() => {
  layoutActive = false
  llmSyncGeneration++
  llmSessionEvents.forEach(name => window.removeEventListener(name, syncDefaultLlmAuthorization))
  window.removeEventListener('platform-logo-changed', loadPlatformLogo)
})

// component
const getMenuIcon = (iconName) => {
  const iconMap = {
    '视频广场': VideoCamera,
    '设备管理': deviceManagementIcon,
    '摄像机设置': Setting,
    '事件': Bell,
    '算法管理': Setting,
    '算法编排': Connection,
    '智能分析申请': DataAnalysis,
    '智能分析结果': Search,
    '场景治理': Setting,
    '标签管理': Collection,
    '区域管理': OfficeBuilding,
    '分组管理': Collection,
    '算法训练平台': School,
    '算法标注': DataAnalysis,
    '算法训练': School,
    '算法模型': Setting,
    '监控告警': Warning,
    '视频回放': VideoPlay,
    '容器实例': Monitor,
    '端侧智能': DataAnalysis,
    '云端智能': Connection,
    '任务状态': Monitor,
    '智能分析': DataAnalysis,
    '用户管理': UserFilled,
    '角色管理': User,
    '菜单管理': MenuIcon,
    '部门管理': OfficeBuilding,
    '岗位管理': Briefcase,
    '数据权限': Key,
    '接口权限': Lock,
    '固件管理': UploadFilled,
    '平台设置': Monitor,
    'wvp-haikang': isupIcon,
    'wvp-ehome': ehomeIcon,
    'wvp-rtsp': rtspIcon,
    'wvp-onvif': onvifIcon,
    'wvp-gb': gbIcon,
    'wvp-dahua': dahuaIcon,
    'wvp-vls': vlstreamIcon
  }
  return iconMap[iconName] || Setting
}

</script>

<style scoped>
.layout-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--vls-page-bg, #f5f7fa);
}

.header-content {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 20px;
  gap: 12px;
  min-width: 0;
  overflow: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  min-width: 240px;
}

.logo h2 {
  color: #1A53FF;
  background: linear-gradient(90deg, #1A53FF 0%, #4C6FFF 60%, #6A5CFF 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.2px;
  min-width: 141px;
  height: 27px;
  margin: 0;
  display: flex;
  align-items: center;
  white-space: nowrap;
  flex-shrink: 0;
}

.logo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-icon svg {
  transition: transform 0.3s ease;
}

.logo-icon:hover svg {
  transform: scale(1.05);
}

.custom-brand-logo {
  display: block;
  width: auto;
  max-width: 188px;
  height: 40px;
  object-fit: contain;
  object-position: left center;
  flex-shrink: 0;
}

/*  */
.sidebar-toggle {
  margin-inline-start: 12px;
  flex-shrink: 0;
}

.sidebar-toggle:hover {
  color: var(--el-color-primary);
}

/* menu */
.top-menu {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  flex: 1 1 0;
  min-width: 0;
  overflow: hidden;
}

.menu-item {
  padding: 0 clamp(8px, 1vw, 18px);
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1 1 0;
  min-width: 0;
  cursor: pointer;
  font-size: clamp(13px, 0.85vw, 16px);
  line-height: 1.15;
  text-align: center;
  overflow-wrap: normal;
  word-break: normal;
  hyphens: auto;
  font-weight: 500;
  color: var(--el-text-color-regular);
  position: relative;
  transition: color 0.2s ease;
  background-color: transparent;
  border-bottom: none;
}

.menu-item:hover {
  color: var(--el-color-primary);
  background-color: var(--el-fill-color-light);
}

.menu-item.active {
  color: var(--el-color-primary);
  font-weight: 600;
  background-color: transparent;
}

.menu-item.active::after {
  content: "";
  position: absolute;
  bottom: 0;
  left: 50%;
  height: 3px;
  width: 20px;
  border-radius: 3px 3px 0 0;
  background: linear-gradient(90deg, #1A53FF, #6A5CFF);
  box-shadow: 0 -2px 8px rgba(26, 83, 255, 0.35);
  transform: translateX(-50%);
}

/* userinfo */
.user-info {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 999px;
  transition: background-color 0.2s ease, color 0.2s ease;
  color: var(--el-text-color-regular);
  font-size: 14px;
  font-weight: 500;
}

.user-info:hover {
  background-color: #ffffff;
  color: var(--el-color-primary);
  box-shadow: 0 1px 3px rgba(23, 34, 64, 0.08);
}

.header-right {
  display: flex;
  align-items: center;
  justify-content: safe flex-end;
  gap: 8px;
  /* Route-specific account controls must not resize the navigation. */
  flex: 0 0 clamp(280px, 24vw, 460px);
  min-width: 0;
  overflow-x: auto;
}

.header-right > * {
  flex-shrink: 0;
}

/* RightInfo selects its full toolbar at a measured width of 520px. */
.header-right--platform {
  flex-basis: min(520px, 42vw);
}

.header-right--platform > :deep(.right-info-sdk-container) {
  flex: 1 1 0;
  min-width: 0;
}

@media (max-width: 1500px) {
  .header-content {
    gap: 8px;
    padding-inline: 14px;
  }

  .logo {
    min-width: 214px;
    gap: 8px;
  }

  .logo h2 {
    min-width: 0;
    font-size: 18px;
  }

  .sidebar-toggle {
    margin-inline-start: 4px;
  }

  .menu-item {
    padding-inline: 6px;
    font-size: 14px;
  }

  .header-right {
    gap: 4px;
  }

  .user-info {
    padding-inline: 6px;
  }
}

@media (max-width: 1180px) {
  .logo h2 {
    display: none;
  }

  .logo {
    min-width: 76px;
  }

  .custom-brand-logo {
    max-width: 40px;
  }

  .menu-item {
    font-size: 13px;
  }
}

/* menu item */
:deep(.el-dropdown-menu__item.is-active) {
  background-color: var(--el-color-primary-hb);
  color: var(--el-color-primary);
}

:deep(.el-dropdown-menu__item.is-active .el-icon) {
  color: var(--el-color-primary);
}

.layout-main {
  flex: 1;
  overflow: hidden;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  transition: all 0.3s ease;
  position: relative;
  background: transparent;
}

/* main */
.layout-main :deep(.el-container),
.layout-main :deep(.el-aside),
.layout-main :deep(.el-main) {
  resize: none !important;
}

/* can */
.layout-main :deep(.el-divider),
.layout-main :deep(.resize-bar),
.layout-main :deep(.split-pane-trigger) {
  display: none !important;
}

.layout-sidebar {
  box-shadow: none;
  transition: width 0.3s ease;
  padding-top: 12px;
  flex-shrink: 0;
  resize: none;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  --el-menu-bg-color: transparent;
  --el-menu-hover-bg-color: var(--el-fill-color-light);
  --el-menu-active-color: var(--el-color-primary);
}

/* 设备统计条：固定在侧栏底部，菜单滚动时不动 */
.app-sidebar-stats {
  flex: 0 0 auto;
  margin: 8px 12px 16px;
  padding: 7px 15px;
  border: 1px solid rgb(155 174 203 / 25%);
  border-radius: 16px;
  background: #f1f5fb;
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 92%), 0 3px 10px rgb(45 65 110 / 8%);
  box-sizing: border-box;
}
@supports ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
  .app-sidebar-stats {
    background: linear-gradient(155deg, rgb(255 255 255 / 68%), rgb(225 235 249 / 58%));
    -webkit-backdrop-filter: blur(12px) saturate(130%);
    backdrop-filter: blur(12px) saturate(130%);
  }
}
.app-sidebar-stats:empty { display: none; }
.app-sidebar-stats .device-stats { display: grid; grid-template-columns: minmax(0, 1fr); gap: 0; }
.app-sidebar-stats .stat-item { display: flex; align-items: center; justify-content: flex-start; min-height: 24px; gap: 8px; text-align: left; }
.app-sidebar-stats .stat-icon { width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; }
.app-sidebar-stats .stat-icon img { width: 20px; height: 20px; }
.app-sidebar-stats .stat-content { display: flex; align-items: center; gap: 2px; }
.app-sidebar-stats .stat-label { font-size: 12px; line-height: 18px; color: var(--el-text-color-regular); white-space: nowrap; }
.app-sidebar-stats .stat-value { font-size: 13px; line-height: 18px; font-weight: 600; color: #1f2533; white-space: nowrap; font-variant-numeric: tabular-nums; }
/* 折叠成 64px 图标条时隐藏统计，避免压住菜单图标 */
.layout-sidebar.is-collapsed .app-sidebar-stats { display: none; }

/* Element Plus can */
.layout-sidebar :deep(.el-aside) {
  resize: none !important;
}

/* can */
.layout-sidebar :deep(.resize-handle),
.layout-sidebar :deep(.drag-handle),
.layout-sidebar :deep(.splitter),
.layout-sidebar :deep(.el-divider--vertical) {
  display: none !important;
}

/* can resize can */
.layout-sidebar * {
  resize: none !important;
}

.sidebar-menu {
  border-right: none !important;
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  padding-top: 4px;
  padding-left: 12px;
  padding-right: 12px;
  background: transparent !important;
  background-color: transparent !important;
  --el-menu-bg-color: transparent;
  border-radius: 16px;
}

.sidebar-menu.el-menu,
.layout-sidebar :deep(.el-menu),
.layout-sidebar :deep(.el-menu--vertical) {
  background: transparent !important;
  background-color: transparent !important;
  border-right: 0 !important;
  color: var(--el-text-color-regular);
}

.layout-sidebar :deep(.el-menu-item),
.layout-sidebar :deep(.el-sub-menu__title) {
  height: 36px !important;
  line-height: 36px !important;
  border-radius: 10px;
  margin-bottom: 2px;
  color: var(--el-text-color-regular);
  font-weight: 500;
  transition: background-color 0.2s ease, color 0.2s ease;
  background: transparent !important;
  background-color: transparent !important;
  gap: 10px;
}

.layout-sidebar :deep(.el-sub-menu > .el-sub-menu__title) {
  margin-bottom: 2px;
}

.layout-sidebar :deep(.el-menu-item:hover),
.layout-sidebar :deep(.el-sub-menu > .el-sub-menu__title:hover) {
  background-color: var(--el-fill-color-light) !important;
  color: var(--el-text-color-primary) !important;
}

.layout-sidebar :deep(.el-menu-item.is-active) {
  background-color: var(--el-color-primary-hb) !important;
  color: var(--el-color-primary) !important;
  font-weight: 600;
}

.layout-sidebar :deep(.el-menu-item.is-active)::before {
  content: "";
  position: absolute;
  inset-inline-start: 0;
  top: 9px;
  bottom: 9px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--el-color-primary);
}

.layout-sidebar :deep(.el-menu-item.is-active .el-icon),
.layout-sidebar :deep(.el-menu-item.is-active .menu-icon-box) {
  color: var(--el-color-primary) !important;
}

.layout-sidebar :deep(.el-menu-item.is-active .menu-svg-icon) {
  background-color: var(--el-color-primary) !important;
}

.layout-sidebar :deep(.el-menu-item.is-active:hover) {
  background-color: var(--el-color-primary-hb) !important;
  color: var(--el-color-primary) !important;
}

.layout-sidebar :deep(.el-sub-menu.is-active > .el-sub-menu__title),
.layout-sidebar :deep(.el-sub-menu.is-active > .el-sub-menu__title .el-icon),
.layout-sidebar :deep(.el-sub-menu.is-active > .el-sub-menu__title .menu-icon-box) {
  background-color: transparent !important;
  color: var(--el-text-color-primary) !important;
}

.layout-sidebar :deep(.el-sub-menu.is-active > .el-sub-menu__title:hover) {
  background-color: var(--el-fill-color-light) !important;
  color: var(--el-text-color-primary) !important;
}

.layout-sidebar :deep(.el-sub-menu.is-active > .el-sub-menu__title .menu-svg-icon) {
  background-color: currentColor !important;
}

.layout-sidebar :deep(.el-sub-menu__icon-arrow) {
  display: none !important;
}

/* : in , */
.layout-sidebar :deep(.el-menu--collapse) {
  width: auto;
  padding-left: 0;
  padding-right: 0;
}

.layout-sidebar :deep(.el-menu--collapse > .el-menu-item),
.layout-sidebar :deep(.el-menu--collapse > .el-sub-menu) {
  width: 32px;
  margin: 0 auto 2px;
}

.layout-sidebar :deep(.el-menu--collapse > .el-menu-item),
.layout-sidebar :deep(.el-menu--collapse .el-sub-menu > .el-sub-menu__title) {
  width: 32px;
  min-width: 32px;
  padding: 0 !important;
  margin: 0 auto 2px;
  justify-content: center;
  gap: 0;
  position: relative;
}

.layout-sidebar :deep(.el-menu--collapse > .el-menu-item .el-menu-tooltip__trigger) {
  width: 32px;
  padding: 0 !important;
  justify-content: center;
}

.layout-sidebar :deep(.el-menu--collapse .el-menu-item.is-active)::before {
  display: none;
}

.layout-sidebar :deep(.el-menu--collapse .submenu-caret) {
  display: none !important;
}

.layout-sidebar :deep(.el-menu--collapse .menu-icon-box) {
  position: absolute;
  top: 50%;
  left: 50%;
  margin: 0;
  transform: translate(-50%, -50%);
}

.layout-content {
  background: #ffffff;
  border-radius: 16px 16px 0 0;
  border: 1px solid var(--el-border-color);
  border-bottom: none;
  box-shadow: 0 1px 2px rgba(23, 34, 64, 0.04), 0 12px 32px -20px rgba(23, 34, 64, 0.16);
  overflow-y: auto;
  flex: 1;
  transition: margin 0.3s ease;
  min-width: 0;
  margin-top: 12px;
  margin-inline-end: 16px;
  height: calc(100% - 12px);
  padding: 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.layout-content:first-child {
  margin-inline-start: 16px;
}
.compact-video-nav .layout-content {
  margin: 4px;
  height: calc(100% - 8px);
}
.compact-video-nav .layout-sidebar {
  position: absolute;
  inset: 0 auto 0 0;
  z-index: 82;
  max-width: 82vw;
  background: var(--el-bg-color, white);
  box-shadow: 4px 0 18px rgb(26 47 79 / .14);
}
.video-nav-shade {
  position: absolute;
  inset: 0;
  z-index: 81;
  border: 0;
  background: rgb(23 39 61 / .25);
}

.layout-content > * {
  flex: 1;
  min-height: 0;
  width: 100%;
}
</style>

<style lang="scss">
/* menu item / and Manage */
.layout-sidebar .el-menu-item,
.layout-sidebar .el-sub-menu__title {
  height: 32px !important;
  line-height: 32px !important;
  border-radius: 16px !important;
}

.layout-sidebar .el-menu-item.is-active {
  background-color: var(--el-color-primary-hb) !important;
  color: var(--el-color-primary) !important;
}

.layout-sidebar .el-sub-menu__icon-arrow {
  display: none !important;
}
</style>
