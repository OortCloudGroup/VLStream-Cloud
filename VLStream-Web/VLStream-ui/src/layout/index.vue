<template>
  <div class="layout-container">
    <!-- 顶部导航 -->
    <el-header class="layout-header" height="60px">
      <div class="header-content">
        <div class="logo">
          <!-- VLStream Logo -->
          <div class="logo-icon">
            <svg width="40" height="40" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="20" cy="20" r="18" fill="white" stroke="#1A53FF" stroke-width="2"/>
              <text x="20" y="25" text-anchor="middle" fill="#1A53FF" font-family="Arial, sans-serif" font-size="14" font-weight="bold">VLS</text>
            </svg>
          </div>
          <h2>VLStream Cloud</h2>
          <!-- 侧边栏伸缩图标 -->
          <CollapseToggle 
            v-if="showSidebar"
            class="sidebar-toggle"
            :is-expanded="!sidebarCollapsed"
            @toggle="toggleSidebar"
          />
        </div>
        
        <!-- 顶部菜单 -->
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
        
        <div class="header-right">
          <!-- 用户信息下拉框 -->
          <el-dropdown>
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ currentUser.userName }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/user-profile')">个人设置</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <!-- 主体内容 -->
    <div class="layout-main">
      <!-- 侧边栏 - 只在非工作台时显示 -->
      <div 
        v-if="showSidebar" 
        class="layout-sidebar" 
        :style="{ width: sidebarCollapsed ? '64px' : '200px' }"
      >
        <el-menu
          :default-active="$route.path"
          router
          class="sidebar-menu"
          background-color="#fff"
          text-color="#303133"
          active-text-color="#409eff"
          :collapse="sidebarCollapsed"
        >
          <template v-for="route in currentMenuRoutes" :key="route.path">
            <!-- 有子菜单的项 -->
            <el-sub-menu v-if="route.children && route.children.length > 0" :index="route.path">
              <template #title>
                <el-icon>
                  <component :is="getMenuIcon(route.meta.icon)" />
                </el-icon>
                <span>{{ route.meta.title }}</span>
              </template>
              <el-menu-item
                v-for="child in route.children"
                :key="child.path"
                :index="child.path"
              >
                <el-icon>
                  <component :is="getMenuIcon(child.meta.icon)" />
                </el-icon>
                <template #title>
                  <span>{{ child.meta.title }}</span>
                </template>
              </el-menu-item>
            </el-sub-menu>
            
            <!-- 没有子菜单的项 -->
            <el-menu-item v-else :index="route.path">
              <el-icon>
                <component :is="getMenuIcon(route.meta.icon)" />
              </el-icon>
              <template #title>
                <span>{{ route.meta.title }}</span>
              </template>
            </el-menu-item>
          </template>
        </el-menu>
      </div>

      <!-- 内容区域 -->
      <div class="layout-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { AuthManager } from '@/utils/auth'
import { getUserInfo, logoutUser } from '@/api/auth'
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
  Lock
} from '@element-plus/icons-vue'
import CollapseToggle from '@/components/CollapseToggle.vue'

const route = useRoute()
const router = useRouter()

// 认证管理器
const authManager = new AuthManager()

// 当前用户信息
const currentUser = ref({
  userName: '管理员',
  userId: '',
  loginId: ''
})

// 当前激活的顶部菜单
const activeTopMenu = ref('workspace')

// 侧边栏伸缩状态
const sidebarCollapsed = ref(false)

// 切换侧边栏伸缩状态
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

// 租户相关数据
const currentTenant = ref({
  id: '',
  name: '加载中...'
})

const tenantList = ref([])

// 使用 SpringBlade 当前用户接口更新右上角用户和租户信息。
const loadBladeUserInfo = async () => {
  const token = authManager.getCurrentToken()
  if (!token) {
    currentTenant.value = {
      id: '',
      name: '未登录'
    }
    return false
  }

  const response = await getUserInfo()
  const user = response?.data || {}
  if (response?.code !== 200 || !user) {
    currentTenant.value = {
      id: '',
      name: response?.msg || '加载失败'
    }
    return false
  }

  const userInfo = {
    ...user,
    userName: user.realName || user.name || user.account || user.userName || '管理员',
    userId: user.id || user.userId || '',
    loginId: user.account || user.loginId || '',
    tenantId: user.tenantId || '',
    accessToken: token
  }
  const tenant = {
    id: userInfo.tenantId,
    name: userInfo.tenantId || '默认租户'
  }

  currentUser.value = {
    userName: userInfo.userName,
    userId: userInfo.userId,
    loginId: userInfo.loginId
  }
  currentTenant.value = tenant
  tenantList.value = [tenant]

  await authManager.saveUserToLocal(userInfo)
  return true
}

// 加载租户信息
const loadTenantInfo = async () => {
  console.log('🚀 loadTenantInfo函数开始执行...')
  await loadBladeUserInfo()
  return
  try {
    console.log('🔍 loadTenantInfo函数开始执行...')
    console.log('🔍 当前URL:', window.location.href)
    console.log('🔍 当前URL参数:', window.location.search)
    console.log('🔍 sessionStorage token:', sessionStorage.getItem('accessToken'))
    console.log('🔍 localStorage token:', localStorage.getItem('accessToken'))
    
    console.log('开始调用getUserTenants API获取用户和租户信息...')

    // 修复：获取token并传递给getUserTenants
    const urlParams = new URLSearchParams(window.location.search)
    const urlToken = urlParams.get('accessToken') || urlParams.get('token')
    const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
    const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    
    const token = urlToken || sessionToken || localToken
    
    if (!token) {
      console.log('❌ loadTenantInfo: 没有找到有效的token')
      console.log('🚫 loadTenantInfo函数提前返回 - 没有token')
      return
    }
    
    console.log('✅ loadTenantInfo: 使用token:', token.substring(0, 8) + '...')
    
    // 修复：传递正确的参数，包含accessToken
    const response = await getUserTenants({ accessToken: token })
    
    console.log('getUserTenants API响应:', response)
    
    // 添加详细的响应数据结构调试
    console.log('🔍 getUserTenants响应详细结构:', {
      code: response.code,
      data: response.data,
      list: response.data?.list,
      firstItem: response.data?.list?.[0]
    })
    
    if (response.code === 200 && response.data) {
      // 处理租户列表
      if (response.data.list && response.data.list.length > 0) {
        const tenants = response.data.list.map(tenant => ({
          id: tenant.tenant_id,
          name: tenant.tenant_name || '未知租户',
          status: tenant.status,
          phrase: tenant.phrase
        }))
        
        tenantList.value = tenants
        
        // 修复：正确从getUserTenants API响应中提取用户信息
        const firstTenant = response.data.list[0]
        console.log('🔍 第一个租户数据:', firstTenant)
        
        // 根据实际API响应结构提取用户信息
        const userInfo = {
          userName: firstTenant.user_name || '管理员',
          userId: firstTenant.user_id || '',
          loginId: firstTenant.user_name || '', // 使用user_name作为loginId
          tenantId: firstTenant.tenant_id || '',
          accessToken: token // 使用当前token
        }
        
        console.log('✅ 从getUserTenants API获取到用户信息:', userInfo)
        
        // 更新用户信息显示
        currentUser.value = {
          userName: userInfo.userName,
          userId: userInfo.userId,
          loginId: userInfo.loginId
        }
        
        console.log('✅ 更新currentUser:', currentUser.value)
        console.log('🔍 检查currentUser响应式更新:', {
          userName: currentUser.value.userName,
          userId: currentUser.value.userId,
          loginId: currentUser.value.loginId
        })
        
        // 强制触发响应式更新
        nextTick(() => {
          console.log('🔄 强制触发响应式更新后的状态:')
          console.log('- currentUser:', currentUser.value)
          console.log('- currentTenant:', currentTenant.value)
        })
        
        // 设置当前租户为第一个租户
        const currentTenantData = tenants[0]
        if (currentTenantData) {
          currentTenant.value = currentTenantData
          console.log('✅ 设置当前租户:', currentTenantData.name)
          console.log('🔍 检查currentTenant响应式更新:', {
            id: currentTenant.value.id,
            name: currentTenant.value.name
          })
        }
        
        // 保存用户信息到本地存储
        try {
          await authManager.saveUserToLocal(userInfo)
          console.log('✅ 用户信息已保存到本地存储')
        } catch (error) {
          console.error('保存用户信息失败:', error)
        }
        
        console.log('✅ 租户信息和用户信息加载成功:', tenants)
        
        // 添加延迟检查，确保页面显示正确
        setTimeout(() => {
          console.log('🔍 延迟检查页面显示状态:')
          console.log('- currentUser:', currentUser.value)
          console.log('- currentTenant:', currentTenant.value)
          console.log('- 页面应该显示用户:', currentUser.value.userName)
          console.log('- 页面应该显示租户:', currentTenant.value.name)
        }, 100)
      } else {
        console.warn('getUserTenants API返回的租户列表为空')
        currentTenant.value = {
          id: '',
          name: '暂无租户'
        }
        console.log('🚫 loadTenantInfo函数结束 - 租户列表为空')
      }
    } else {
      console.warn('获取租户列表失败:', response.msg)
      // 设置默认租户信息
      currentTenant.value = {
        id: '',
        name: '获取租户信息失败'
      }
      console.log('🚫 loadTenantInfo函数结束 - API调用失败')
    }
  } catch (error) {
    console.error('加载租户信息失败:', error)
    // 设置默认租户信息
    currentTenant.value = {
      id: '',
      name: '加载失败'
    }
    console.log('🚫 loadTenantInfo函数结束 - 发生异常')
  }
  console.log('🏁 loadTenantInfo函数执行完成')
}

// 切换租户
const switchTenant = (tenant) => {
  currentTenant.value = tenant
  // 这里可以添加切换租户的业务逻辑
  console.log('切换到租户:', tenant.name)
}

// 退出登录时先通知后端，再清理本地认证状态并返回登录页。
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (error) {
    return
  }

  try {
    await logoutUser()
  } catch (error) {
    console.warn('后端登出接口调用失败，继续清理本地登录状态:', error)
  }

  authManager.clearAllTokens()
  ElMessage.success('已退出登录')
  await router.replace('/login')
}

// 顶部菜单配置
const topMenus = [
  { key: 'workspace', title: '工作台' },
  { key: 'video-aggregation', title: '视频汇聚' },
  { key: 'decision-ai', title: '决策式AI' },
  { key: 'active-safety', title: '主动安全' },
  { key: 'algorithm-warehouse', title: '算法仓库' },
  { key: 'ai-computing', title: 'AI算力调度' },
  { key: 'system-management', title: '系统管理' }
]

// 不同菜单对应的侧边栏路由
const menuRoutesMap = {
  'workspace': [],
  'video-aggregation': [
    { path: '/video-square', meta: { title: '视频广场', icon: '视频广场' } },
    { path: '/video-playback', meta: { title: '视频回放', icon: '视频回放' } },
    // { path: '/monitoring-alarm', meta: { title: '监控告警', icon: '监控告警' } },
    { path: '/intelligent-analysis-result', meta: { title: '智能分析结果', icon: '智能分析结果' } },
    { path: '/device-management', meta: { title: '设备管理', icon: '设备管理' } },
    // { path: '/camera-settings', meta: { title: '设置摄像机', icon: '摄像机设置' } },
    { path: '/tag-management', meta: { title: '标签管理', icon: '标签管理' } }
  ],
  'decision-ai': [
    { path: '/event-management', meta: { title: '事件管理', icon: '事件' } },
    { path: '/scene-governance', meta: { title: '场景治理', icon: '场景治理' } },
    { path: '/algorithm-arrangement', meta: { title: '算法配置', icon: '算法配置' } },
    { path: '/intelligent-analysis-request', meta: { title: '智能分析申请', icon: '智能分析申请' } },
    { path: '/algorithm-task', meta: { title: '任务配置', icon: '任务配置' } },
  ],
  'active-safety': [
    {
      path: '/active-safety/events',
      meta: { title: '事件管理', icon: '事件' },
      children: [
        { path: '/active-safety/events/secure', meta: { title: '主动安全', icon: '监控告警' } }
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
      meta: { title: '系统设置', icon: '摄像机设置' },
      children: [
        { path: '/active-safety/settings/secure', meta: { title: '主动安全', icon: '摄像机设置' } },
        { path: '/active-safety/settings/work-orders', meta: { title: '工单设置', icon: '摄像机设置' } }
      ]
    }
  ],
  'algorithm-warehouse': [
    { path: '/algorithm-management', meta: { title: '算法管理', icon: '算法管理' } },
    { 
      path: '/algorithm-training-platform', 
      meta: { title: '算法训练平台', icon: '算法训练平台' },
      children: [
        { path: '/algorithm-standard', meta: { title: '算法标注', icon: '算法标注' } },
        { path: '/algorithm-training', meta: { title: '算法训练', icon: '算法训练' } },
        { path: '/algorithm-model', meta: { title: '算法模型', icon: '算法模型' } }
      ]
    },
    { path: '/open-service-center', meta: { title: '开放服务中心', icon: '开放服务中心' } },
  ],
  'ai-computing': [
    { path: '/container-instances', meta: { title: '容器实例', icon: '容器实例' } }
  ],
  'system-management': [
    { path: '/system/users', meta: { title: '用户管理', icon: '用户管理' } },
    { path: '/system/roles', meta: { title: '角色管理', icon: '角色管理' } },
    { path: '/system/menus', meta: { title: '菜单管理', icon: '菜单管理' } },
    { path: '/system/depts', meta: { title: '部门管理', icon: '部门管理' } },
    { path: '/system/posts', meta: { title: '岗位管理', icon: '岗位管理' } },
    { path: '/system/data-scopes', meta: { title: '数据权限', icon: '数据权限' } },
    { path: '/system/api-scopes', meta: { title: '接口权限', icon: '接口权限' } }
  ]
}

// 是否显示侧边栏
const showSidebar = computed(() => {
  return activeTopMenu.value !== 'workspace'
})

// 当前菜单的路由
const currentMenuRoutes = computed(() => {
  return menuRoutesMap[activeTopMenu.value] || []
})

/**
 * 处理顶部菜单的点击事件
 * 如果点击的是“工作台”，跳转到 /workspace 页面
 * 如果点击的是“主动安全”，默认跳转到主动安全事件管理页面 /active-safety/events/secure
 * 其他菜单默认跳转到对应侧边栏的第一个路由
 * @param {string} menuKey 顶部菜单对应的 key
 */
const handleTopMenuClick = (menuKey) => {
  activeTopMenu.value = menuKey
  
  // 根据菜单切换到对应的默认路由
  if (menuKey === 'workspace') {
    router.push('/workspace') // 工作台页面
  } else if (menuKey === 'active-safety') {
    router.push('/active-safety/events/secure') // 默认跳转到主动安全页面
  } else {
    const routes = menuRoutesMap[menuKey]
    if (routes && routes.length > 0) {
      router.push(routes[0].path) // 跳转到该菜单的第一个路由
    }
  }
}

// 根据当前路由确定激活的顶部菜单
const getActiveMenuByRoute = (routePath) => {
  for (const [menuKey, routes] of Object.entries(menuRoutesMap)) {
    // 检查直接路由匹配
    if (routes.some(route => route.path === routePath)) {
      return menuKey
    }
    // 检查子路由匹配
    for (const route of routes) {
      if (route.children && route.children.some(child => child.path === routePath)) {
        return menuKey
      }
    }
  }
  return 'workspace'
}

// 监听路由变化，自动设置激活菜单
watch(() => route.path, (newPath) => {
  const activeMenu = getActiveMenuByRoute(newPath)
  if (activeMenu !== activeTopMenu.value) {
    activeTopMenu.value = activeMenu
  }
}, { immediate: true })

// 强制加载用户和租户信息
const forceLoadUserAndTenantInfo = async () => {
  console.log('🚀 forceLoadUserAndTenantInfo函数开始执行...')
  await loadBladeUserInfo()
  return
  
  try {
    // 获取token
    const urlParams = new URLSearchParams(window.location.search)
    const urlToken = urlParams.get('accessToken') || urlParams.get('token')
    const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
    const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    
    const token = urlToken || sessionToken || localToken
    
    if (!token) {
      console.log('❌ 没有找到有效的token')
      return
    }
    
    console.log('✅ 使用token:', token.substring(0, 8) + '...')
    
    // 直接调用getUserTenants API
    const response = await getUserTenants({ accessToken: token })
    
    console.log('📥 getUserTenants响应:', response)
    
    // 检查响应格式
    if (response.status === 200 && response.data && response.data.code === 200 && response.data.data && response.data.data.list && response.data.data.list.length > 0) {
      const firstTenant = response.data.data.list[0]
      
      console.log('📋 解析到的用户和租户信息:')
      console.log('- 用户名称:', firstTenant.user_name)
      console.log('- 租户名称:', firstTenant.tenant_name)
      console.log('- 用户ID:', firstTenant.user_id)
      console.log('- 租户ID:', firstTenant.tenant_id)
      
      // 直接更新currentUser
      currentUser.value = {
        userName: firstTenant.user_name || '管理员',
        userId: firstTenant.user_id || '',
        loginId: firstTenant.user_name || ''
      }
      
      console.log('✅ 更新currentUser:', currentUser.value)
      
      // 更新租户列表
      const tenants = response.data.data.list.map(tenant => ({
        id: tenant.tenant_id,
        name: tenant.tenant_name || '未知租户',
        status: tenant.status,
        phrase: tenant.phrase
      }))
      
      tenantList.value = tenants
      console.log('✅ 更新tenantList:', tenantList.value)
      
      // 设置当前租户
      if (tenants.length > 0) {
        currentTenant.value = tenants[0]
        console.log('✅ 设置当前租户:', currentTenant.value)
      }
      
      // 保存到localStorage
      localStorage.setItem('userInfo', JSON.stringify({
        userName: firstTenant.user_name,
        userId: firstTenant.user_id,
        tenantName: firstTenant.tenant_name,
        tenantId: firstTenant.tenant_id,
        accessToken: token
      }))
      
      console.log('✅ 用户信息已保存到localStorage')
      
    } else {
      console.warn('getUserTenants API返回数据格式不正确:', response)
    }
  } catch (error) {
    console.error('❌ forceLoadUserAndTenantInfo失败:', error)
  }
  
  console.log('🏁 forceLoadUserAndTenantInfo函数执行完成')
}

// 加载用户信息
const loadUserInfo = async () => {
  try {
    // 优先从Session Storage获取用户信息
    let userInfo = await authManager.checkLocalToken()
    
    // 如果Session Storage中没有，尝试从localStorage获取
    if (!userInfo) {
      const localUserInfo = localStorage.getItem('userInfo')
      if (localUserInfo) {
        try {
          userInfo = JSON.parse(localUserInfo)
          console.log('从localStorage获取到用户信息:', userInfo.userName)
          // 将用户信息同步到Session Storage
          await authManager.saveUserToLocal(userInfo)
        } catch (error) {
          console.error('解析localStorage用户信息失败:', error)
        }
      }
    }
    
    // 如果本地都没有用户信息，不主动调用API，而是依赖loadTenantInfo方法
    // loadTenantInfo方法会调用getUserTenants API并更新用户信息
    if (!userInfo) {
      console.log('本地没有用户信息，等待loadTenantInfo方法通过getUserTenants API获取')
      // 不主动调用API，让loadTenantInfo方法处理
      return
    }
    
    // 如果本地有用户信息，但不设置currentUser.value，确保使用getUserTenants API的数据
    if (userInfo) {
      console.log('本地有用户信息，但不更新显示，等待loadTenantInfo方法处理')
      // 不设置currentUser.value，让loadTenantInfo方法处理
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
  }
}

// 监听token变化事件
const handleTokenUpdate = async (event) => {
  console.log('🔄 检测到token更新事件')
  // 只调用loadTenantInfo，不调用loadUserInfo，确保使用getUserTenants API的数据
  await loadTenantInfo()
}

const handleTokenInvalid = () => {
  console.log('⚠️ 检测到token失效事件')
  // 不直接设置用户信息，让loadTenantInfo方法处理
  // 如果getUserTenants API调用失败，页面会显示默认值
}

// 监听跨系统token变化事件
const handleCrossSystemTokenUpdate = async (event) => {
  console.log('🔄 检测到跨系统token更新事件:', event.detail)
  // 只调用loadTenantInfo，不调用loadUserInfo，确保使用getUserTenants API的数据
  await loadTenantInfo()
}

const handleCrossSystemTokenInvalid = () => {
  console.log('⚠️ 检测到跨系统token失效事件')
  // 不直接设置用户信息，让loadTenantInfo方法处理
  // 如果getUserTenants API调用失败，页面会显示默认值
}

// 监听自动跨系统token变化事件
const handleAutoCrossSystemTokenUpdate = async (event) => {
  console.log('🔄 检测到自动跨系统token更新事件:', event.detail)
  // 只调用loadTenantInfo，不调用loadUserInfo，确保使用getUserTenants API的数据
  await loadTenantInfo()
}

const handleAutoCrossSystemTokenInvalid = () => {
  console.log('⚠️ 检测到自动跨系统token失效事件')
  // 不直接设置用户信息，让loadTenantInfo方法处理
  // 如果getUserTenants API调用失败，页面会显示默认值
}

// 组件挂载时设置初始菜单和加载用户信息
onMounted(async () => {
  console.log('🎬 组件开始挂载...')
  
  try {
  const activeMenu = getActiveMenuByRoute(route.path)
  activeTopMenu.value = activeMenu
    console.log('✅ 设置初始菜单:', activeMenu)
    
    // 强制调用getUserTenants API并更新显示
    console.log('🔄 开始强制调用getUserTenants API...')
    await forceLoadUserAndTenantInfo()
    console.log('✅ 强制加载用户和租户信息完成')
  } catch (error) {
    console.error('❌ onMounted中发生错误:', error)
  }
  
  // 添加延迟检查，确保数据更新
  setTimeout(async () => {
    console.log('⏰ 延迟检查用户和租户信息...')
    if (currentUser.value.userName === '管理员' || currentTenant.value.name === '加载中...') {
      console.log('🔄 检测到默认状态，重新加载用户信息...')
      await forceLoadUserAndTenantInfo()
    }
    }, 2000)
  
  // 添加token变化事件监听器
  window.addEventListener('tokenUpdated', handleTokenUpdate)
  window.addEventListener('tokenInvalid', handleTokenInvalid)
  
  // 添加跨系统token变化事件监听器
  window.addEventListener('crossSystemTokenUpdated', handleCrossSystemTokenUpdate)
  window.addEventListener('crossSystemTokenInvalid', handleCrossSystemTokenInvalid)
  
  // 添加自动跨系统token变化事件监听器
  window.addEventListener('autoCrossSystemTokenUpdated', handleAutoCrossSystemTokenUpdate)
  window.addEventListener('autoCrossSystemTokenInvalid', handleAutoCrossSystemTokenInvalid)
  
  // 监听用户token更新事件
  window.addEventListener('userTokenUpdated', handleUserTokenUpdated)
  
  console.log('🎬 组件挂载完成')
  
  // 暴露函数到window对象，方便调试
  window.forceLoadUserAndTenantInfo = forceLoadUserAndTenantInfo
  console.log('🔧 forceLoadUserAndTenantInfo函数已暴露到window对象')
})

// 组件卸载时移除事件监听器
onUnmounted(() => {
  window.removeEventListener('tokenUpdated', handleTokenUpdate)
  window.removeEventListener('tokenInvalid', handleTokenInvalid)
  window.removeEventListener('crossSystemTokenUpdated', handleCrossSystemTokenUpdate)
  window.removeEventListener('crossSystemTokenInvalid', handleCrossSystemTokenInvalid)
  
  // 移除自动跨系统token变化事件监听器
  window.removeEventListener('autoCrossSystemTokenUpdated', handleAutoCrossSystemTokenUpdate)
  window.removeEventListener('autoCrossSystemTokenInvalid', handleAutoCrossSystemTokenInvalid)
  
  // 移除事件监听
  window.removeEventListener('userTokenUpdated', handleUserTokenUpdated)
})

// 根据图标名称返回对应的图标组件
const getMenuIcon = (iconName) => {
  const iconMap = {
    '视频广场': VideoCamera,
    '设备管理': Monitor,
    '摄像机设置': Setting,
    '事件': Bell,
    '算法管理': Setting,
    '算法编排': Connection,
    '智能分析申请': DataAnalysis,
    '智能分析结果': Search,
    '场景治理': Setting,
    '标签管理': Collection,
    '算法训练平台': School,
    '算法标注': DataAnalysis,
    '算法训练': School,
    '算法模型': Setting,
    '监控告警': Warning,
    '视频回放': VideoPlay,
    '开放服务中心': Connection,
    '容器实例': Monitor,
    '用户管理': UserFilled,
    '角色管理': User,
    '菜单管理': MenuIcon,
    '部门管理': OfficeBuilding,
    '岗位管理': Briefcase,
    '数据权限': Key,
    '接口权限': Lock
  }
  return iconMap[iconName] || Setting
}

// 添加处理函数
const handleUserTokenUpdated = async (event) => {
  console.log('🔄 收到用户token更新事件，重新加载用户信息')
  
  try {
    // 只重新加载租户信息，不重新加载用户信息，确保使用getUserTenants API的数据
    await loadTenantInfo()
    
    console.log('✅ 租户信息已更新')
  } catch (error) {
    console.error('❌ 更新租户信息失败:', error)
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-header {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  min-width: 280px;
}

.logo h2 {
  color: #1A53FF;
  font-size: 20px;
  font-weight: 400;
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

/* 侧边栏伸缩图标样式 */
.sidebar-toggle {
  background: #ffffff;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-left: 24px;
  transition: all 0.3s;
  flex-shrink: 0;
}

.sidebar-toggle:hover {
  background: #ecf5ff;
  border-color: #b3d8ff;
  transform: scale(1.05);
}

/* 顶部菜单样式 */
.top-menu {
  display: flex;
  align-items: center;
  gap: 0;
}

.menu-item {
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  position: relative;
  transition: all 0.3s;
  border-bottom: 3px solid transparent;
}

.menu-item:hover {
  color: #409eff;
  background-color: #f8f9fa;
}

.menu-item.active {
  color: #409eff;
  background-color: #f0f9ff;
  border-bottom-color: #409eff;
}

/* 用户信息样式 */
.user-info {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;
  color: #606266;
  font-size: 14px;
}

.user-info:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

/* 下拉菜单项激活状态 */
:deep(.el-dropdown-menu__item.is-active) {
  background-color: #ecf5ff;
  color: #409eff;
}

:deep(.el-dropdown-menu__item.is-active .el-icon) {
  color: #409eff;
}

.layout-main {
  flex: 1;
  overflow: hidden;
  display: flex;
  transition: all 0.3s ease;
  position: relative;
}

/* 确保主布局区域不显示任何拖拽条 */
.layout-main :deep(.el-container),
.layout-main :deep(.el-aside),
.layout-main :deep(.el-main) {
  resize: none !important;
}

/* 隐藏任何可能的分隔线或拖拽控件 */
.layout-main :deep(.el-divider),
.layout-main :deep(.resize-bar),
.layout-main :deep(.split-pane-trigger) {
  display: none !important;
}

.layout-sidebar {
  background: #fff;
  border-right: 1px solid #e8e8e8;
  box-shadow: 2px 0 4px rgba(0, 0, 0, 0.1);
  transition: width 0.3s ease;
  height: 100%;
  flex-shrink: 0;
  resize: none;
  overflow: hidden;
}

/* 隐藏Element Plus可能的拖拽句柄 */
.layout-sidebar :deep(.el-aside) {
  resize: none !important;
}

/* 隐藏可能的拖拽条或调整句柄 */
.layout-sidebar :deep(.resize-handle),
.layout-sidebar :deep(.drag-handle),
.layout-sidebar :deep(.splitter),
.layout-sidebar :deep(.el-divider--vertical) {
  display: none !important;
}

/* 禁用可能的resize功能 */
.layout-sidebar * {
  resize: none !important;
}

.sidebar-menu {
  border-right: none;
  height: 100%;
}

.sidebar-menu .el-menu-item {
  height: 50px;
  line-height: 50px;
  margin: 4px 8px;
  border-radius: 6px;
  transition: all 0.3s;
}

.sidebar-menu .el-menu-item:hover {
  background-color: #ecf5ff;
}

.sidebar-menu .el-menu-item.is-active {
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: 500;
}

.layout-content {
  background: #f5f7fa;
  overflow-y: auto;
  flex: 1;
  transition: all 0.3s ease;
  min-width: 0;
}
</style>
