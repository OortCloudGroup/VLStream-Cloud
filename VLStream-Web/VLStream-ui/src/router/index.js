import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'
import { AuthManager } from '@/utils/auth'

const authManager = new AuthManager()

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', hideInMenu: true }
  },
  {
    path: '/test-auth',
    name: 'TestAuth',
    component: () => import('@/views/TestAuth.vue'),
    meta: { title: '测试认证', hideInMenu: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/workspace',
    meta: { requiresAuth: true },
    children: [
      {
        path: '/workspace',
        name: 'Workspace',
        component: () => import('@/views/Workspace.vue'),
        meta: { title: '工作台', icon: '工作台' }
      },
      {
        path: '/video-square',
        name: 'VideoSquare',
        component: () => import('@/views/VideoAggregation/VideoSquareRefactored.vue'),
        meta: { title: '视频广场', icon: '视频广场' }
      },
      {
        path: '/device-management',
        name: 'DeviceManagement',
        component: () => import('@/views/VideoAggregation/DeviceManagement.vue'),
        meta: { title: '设备管理', icon: '设备管理' }
      },
      {
        path: '/event-management',
        name: 'EventManagement',
        component: () => import('@/views/DecisionAI/EventManagement.vue'),
        meta: { title: '事件管理', icon: '事件' }
      },
      {
        path: '/algorithm-management',
        name: 'AlgorithmManagement',
        component: () => import('@/views/AlgorithmWarehouse/AlgorithmManagement.vue'),
        meta: { title: '算法管理', icon: '算法管理' }
      },

      {
        path: '/scene-governance',
        name: 'SceneGovernance',
        component: () => import('@/views/DecisionAI/SceneGovernance.vue'),
        meta: { title: '场景治理', icon: '场景治理' }
      },
      {
        path: '/intelligent-analysis-request',
        name: 'IntelligentAnalysisRequest',
        component: () => import('@/views/DecisionAI/IntelligentAnalysisRequest.vue'),
        meta: { title: '智能分析申请', icon: '智能分析申请' }
      },
      {
        path: '/intelligent-analysis-result',
        name: 'IntelligentAnalysisResult',
        component: () => import('@/views/VideoAggregation/IntelligentAnalysisResult.vue'),
        meta: { title: '智能分析结果', icon: '智能分析结果' }
      },
      {
        path: '/tag-management',
        name: 'TagManagement',
        component: () => import('@/views/VideoAggregation/TagManagement.vue'),
        meta: { title: '标签管理', icon: '标签管理' }
      },
      {
        path: '/algorithm-training-platform',
        name: 'AlgorithmTrainingPlatform',
        component: () => import('@/views/AlgorithmWarehouse/AlgorithmTrainingPlatform.vue'),
        meta: { title: '算法训练平台', icon: '算法训练平台' }
      },
      {
        path: '/algorithm-standard',
        name: 'AlgorithmStandard',
        component: () => import('@/views/AlgorithmTraining/AlgorithmStandard.vue'),
        meta: { title: '算法标注', icon: '算法标注' }
      },
      {
        path: '/algorithm-model',
        name: 'AlgorithmModel',
        component: () => import('@/views/AlgorithmTraining/AlgorithmModel.vue'),
        meta: { title: '算法模型', icon: '算法模型' }
      },
      {
        path: '/algorithm-training',
        name: 'AlgorithmTraining',
        component: () => import('@/views/AlgorithmTraining/AlgorithmTraining.vue'),
        meta: { title: '算法训练', icon: '算法训练' }
      },
      {
        path: '/ssh-connection',
        name: 'SSHConnection',
        component: () => import('@/views/AlgorithmTraining/SSHConnection.vue'),
        meta: { title: 'SSH连接', icon: 'SSH连接' }
      },
      {
        path: '/monitoring-alarm',
        name: 'MonitoringAlarm',
        component: () => import('@/views/VideoAggregation/MonitoringAlarm.vue'),
        meta: { title: '监控告警', icon: '监控告警' }
      },
      {
        path: '/video-playback',
        name: 'VideoPlayback',
        component: () => import('@/views/VideoAggregation/VideoPlayback.vue'),
        meta: { title: '视频回放', icon: '视频回放' }
      },
      {
        path: '/camera-settings',
        name: 'CameraSettings',
        component: () => import('@/views/VideoAggregation/CameraSettings.vue'),
        meta: { title: '设置摄像机', icon: '摄像机设置' }
      },
      {
        path: '/design-drawing',
        name: 'DesignDrawing',
        component: () => import('@/views/DecisionAI/DesignDrawing.vue'),
        meta: { title: '设计图', icon: '设计图' }
      },
            {
        path: '/scene-edit',
        name: 'SceneEdit',
        component: () => import('@/views/DecisionAI/SceneEdit.vue'),
        meta: { title: '编辑场景', icon: '编辑场景' }
      },
      {
        path: '/algorithm-arrangement',
        name: 'AlgorithmArrangement',
        component: () => import('@/views/DecisionAI/AlgorithmArrangement.vue'),
        meta: { title: '算法配置', icon: '算法配置' }
      },
      {
        path: '/algorithm-task',
        name: 'AlgorithmTask',
        component: () => import('@/views/DecisionAI/AlgorithmTask.vue'),
        meta: { title: '任务配置', icon: '任务配置' }
      },
      {
        path: '/container-instances',
        name: 'ContainerInstances',
        component: () => import('@/views/AIComputingScheduling/ContainerInstances.vue'),
        meta: { title: '容器实例', icon: '容器实例' }
      },
      {
        path: '/user-profile',
        name: 'UserProfile',
        component: () => import('@/views/UserProfile.vue'),
        meta: { title: '个人设置', icon: '个人设置' }
      },
      {
        path: '/open-service-center',
        name: 'OpenServiceCenter',
        component: () => import('@/views/OpenServiceCenter.vue'),
        meta: { title: '开放服务中心', icon: '开放服务中心' }
      },
      {
        path: '/system/users',
        name: 'SystemUserManagement',
        component: () => import('@/views/System/UserManagement.vue'),
        meta: { title: '用户管理', icon: '用户管理' }
      },
      {
        path: '/system/roles',
        name: 'SystemRoleManagement',
        component: () => import('@/views/System/RoleManagement.vue'),
        meta: { title: '角色管理', icon: '角色管理' }
      },
      {
        path: '/system/menus',
        name: 'SystemMenuManagement',
        component: () => import('@/views/System/MenuManagement.vue'),
        meta: { title: '菜单管理', icon: '菜单管理' }
      },
      {
        path: '/system/depts',
        name: 'SystemDeptManagement',
        component: () => import('@/views/System/DeptManagement.vue'),
        meta: { title: '部门管理', icon: '部门管理' }
      },
      {
        path: '/system/posts',
        name: 'SystemPostManagement',
        component: () => import('@/views/System/PostManagement.vue'),
        meta: { title: '岗位管理', icon: '岗位管理' }
      },
      {
        path: '/system/data-scopes',
        name: 'SystemDataScopeManagement',
        component: () => import('@/views/System/DataScopeManagement.vue'),
        meta: { title: '数据权限', icon: '数据权限' }
      },
      {
        path: '/system/api-scopes',
        name: 'SystemApiScopeManagement',
        component: () => import('@/views/System/ApiScopeManagement.vue'),
        meta: { title: '接口权限', icon: '接口权限' }
      },
      // === 主动安全模块路由配置 ===
      {
        path: '/active-safety/events/secure',
        name: 'ActiveSafetySecureEvents',
        component: () => import('@/views/events/page/eventManagement/secure.vue'),
        meta: { title: '主动安全事件', icon: '安全' }
      },
      {
        path: '/active-safety/work-orders/my',
        name: 'ActiveSafetyWorkOrdersMy',
        component: () => import('@/views/events/page/myWorkorder/myWorkorder.vue'),
        meta: { title: '我的工单', icon: '工单' }
      },
      {
        path: '/active-safety/work-orders/pending',
        name: 'ActiveSafetyWorkOrdersPending',
        component: () => import('@/views/events/page/workOrderDesignManage/pendingWorkOrder.vue'),
        meta: { title: '待办工单', icon: '工单' }
      },
      {
        path: '/active-safety/work-orders/completed',
        name: 'ActiveSafetyWorkOrdersCompleted',
        component: () => import('@/views/events/page/workOrderDesignManage/completedWorkOrder.vue'),
        meta: { title: '已办工单', icon: '工单' }
      },
      {
        path: '/active-safety/work-orders/claimable',
        name: 'ActiveSafetyWorkOrdersClaimable',
        component: () => import('@/views/events/page/workOrderDesignManage/workOrderClaim.vue'),
        meta: { title: '可接工单', icon: '工单' }
      },
      {
        path: '/active-safety/settings/secure',
        name: 'ActiveSafetySettingsSecure',
        component: () => import('@/views/events/page/eventManagement/sysSecure2.vue'),
        meta: { title: '主动安全设置', icon: '设置' }
      },
      {
        path: '/active-safety/settings/work-orders',
        name: 'ActiveSafetySettingsWorkOrders',
        component: () => import('@/views/events/page/eventManagement/sysWorkOrderSetting.vue'),
        meta: { title: '工单设置', icon: '设置' }
      },
      {
        path: '/workOrderDetails',
        name: 'ActiveSafetyWorkOrderDetails',
        component: () => import('@/pages/task_center/views/page/workOrderDetailsStep.vue'),
        meta: { title: '工单详情', icon: '设置' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory('/bus/vls-ui/'),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  console.log('路由跳转:', to.path)

  try {
  // 如果访问登录页面或测试页面，直接通过
  if (to.path === '/login' || to.path === '/test-auth') {
    next()
    return
  }

  // 强制清除旧的测试token
  const oldTestToken = 'c0c81bef2c934f829df667a202c99d1e'
  if (localStorage.getItem('accessToken') === oldTestToken) {
    console.log('检测到旧测试token，正在清除...')
    localStorage.removeItem('accessToken')
    localStorage.removeItem('userInfo')
  }

  // 检查是否需要登录验证
  if (to.meta.requiresAuth) {
      try {
    // 优先级1：检查外部统一用户平台登录状态（SSO）
    const externalUserInfo = await authManager.checkExternalPlatformLogin()
    if (externalUserInfo) {
      console.log('外部平台SSO验证成功，用户:', externalUserInfo.userName || externalUserInfo.loginId)
      next()
      return
    }

    // 如果没有外部平台token，尝试同步外部平台信息
    // console.log('没有外部平台token，尝试同步外部平台Session Storage信息')
    // 不跳转，让用户手动处理或显示登录页面

    // 优先级2：检查URL中的accessToken（SSO跳转）
    const urlParams = new URLSearchParams(window.location.search)
    const urlAccessToken = urlParams.get('accessToken')
    if (urlAccessToken) {
      console.log('检测到URL中的accessToken，进行SSO验证:', urlAccessToken)
      try {
        const userInfo = await authManager.verifyToken(urlAccessToken)
        if (userInfo) {
          console.log('URL accessToken验证成功，用户:', userInfo.userName)
          // 保存用户信息到本地，避免重复验证
          await authManager.saveUserToLocal(userInfo)
          // 只有在验证成功后才保存token到Session Storage
          sessionStorage.setItem('token', urlAccessToken)
          sessionStorage.setItem('accessToken', urlAccessToken)
          // 清除URL参数
          const newUrl = window.location.pathname
          window.history.replaceState({}, document.title, newUrl)
          next()
          return
        } else {
          console.log('URL accessToken验证失败，清除可能存在的无效token')
          // 验证失败时立即清除可能存在的无效token
          sessionStorage.removeItem('token')
          sessionStorage.removeItem('accessToken')
          // 不再清除URL参数，让系统继续检查其他token
        }
      } catch (error) {
        console.error('URL accessToken验证失败:', error)
        // 验证失败时立即清除可能存在的无效token
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('accessToken')
        // 不再清除URL参数，让系统继续检查其他token
      }
    }

    // 优先级2.5：检查URL中的token（兼容旧格式）
        try {
    const urlUserInfo = await authManager.checkUrlToken()
    if (urlUserInfo) {
      console.log('URL token验证成功，用户:', urlUserInfo.userName)
      next()
      return
          }
        } catch (error) {
          console.error('检查URL token失败:', error)
    }

    // 优先级2.5：检查Session Storage中的新token
    const sessionToken = sessionStorage.getItem('accessToken')
    // console.log('Session Storage中的token:', sessionToken)
    if (sessionToken) {
      console.log('检测到Session Storage中的token:', sessionToken)
          try {
      const sessionUserInfo = await authManager.verifyToken(sessionToken)
      if (sessionUserInfo) {
        console.log('Session token验证成功，用户:', sessionUserInfo.userName)
        await authManager.saveUserToLocal(sessionUserInfo)
        next()
        return
      } else {
        console.log('Session token验证失败，清除无效token')
        authManager.clearSessionTokens()
            }
          } catch (error) {
            console.error('Session token验证失败:', error)
        authManager.clearSessionTokens()
      }
    }

    // 优先级3：检查本地存储的token
        try {
    const localUserInfo = await authManager.checkLocalToken()
    if (localUserInfo) {
      console.log('本地token验证成功，用户:', localUserInfo.userName)
      next()
      return
          }
        } catch (error) {
          console.error('检查本地token失败:', error)
    }

    // 没有有效 token 时跳转登录页，避免后续业务接口持续返回 401。
    next({
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    })
      } catch (error) {
        console.error('路由守卫验证过程中发生错误:', error)
        // 即使出错也要允许页面继续加载
        next()
      }
  } else {
      next()
    }
  } catch (error) {
    console.error('路由守卫发生严重错误:', error)
    // 确保总是调用next()
    next()
  }
})

export default router
