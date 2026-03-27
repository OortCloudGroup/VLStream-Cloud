/*
 * @Author: lanjian
 * @Date: 2021-11-13 09:13:06
 * @LastEditors: lanjian
 * @LastEditTime: 2021-11-27 12:18:56
 * @FilePath: \cordava_utils\demo\src\router\index.js
 * @Description: Copyright 奥尔特云（深圳）智慧科技有限公司. All rights reserved.
 */
import { createRouter, createWebHashHistory } from 'vue-router'

import About from '@/components/commonpage/aboutPage'
import ContactUS from '@/components/commonpage/contactUS'
import VersionsPage from '@/components/commonpage/versionsPage'
import Home from '@/pages/VLStreamCloud/page/home'
import Workbench from '@/pages/VLStreamCloud/page/index/workbench.vue'

// 2. 定义一些路由
// 每个路由都需要映射到一个组件。
// 我们后面再讨论嵌套路由。
const routes = [
  {
    path: '/',
    component: Home,
    redirect: '/workbench',
    children: [
      {
        path: 'workbench',
        name: 'Workbench',
        component: Workbench,
        meta: {
          index: 100,
          title: '工作台'
        }
      },
      {
        path: 'video',
        name: 'Video',
        component: () => import('@/pages/VLStreamCloud/page/index/video.vue'),
        meta: {
          index: 101,
          title: '视频'
        }
      },
      {
        path: 'event',
        name: 'Event',
        component: () => import('@/pages/VLStreamCloud/page/index/event.vue'),
        meta: {
          index: 102,
          title: '事件'
        }
      },
      {
        path: 'my',
        name: 'My',
        component: () => import('@/pages/VLStreamCloud/page/index/my.vue'),
        meta: {
          index: 103,
          title: '我的'
        }
      }
    ]
  },
  {
    path: '/aboutAppPage',
    name: 'aboutAppPage',
    component: About,
    meta: {
      index: 201,
      title: '关于'
    }
  },
  {
    path: '/contactUS',
    name: 'ContactUS',
    component: ContactUS,
    meta: {
      index: 202,
      title: '联系我们'
    }
  },
  {
    path: '/versionsPage',
    name: 'versionsPage',
    component: VersionsPage,
    meta: {
      index: 203,
      title: '版本介绍'
    }
  },
  {
    path: '/myWorkorder',
    name: 'myWorkorder',
    meta: { title: '我的工单' },
    component: () => import('@/pages/eventManagement/page/myWorkorder/myWorkorder.vue')
  },
  {
    path: '/workorder-detail',
    name: 'WorkorderDetail',
    meta: { title: '工单详情' },
    component: () => import('@/pages/eventManagement/page/myWorkorder/workorderDetail.vue')
  },
  {
    path: '/eventBuilt',
    name: 'eventBuilt',
    meta: { title: '马上新建' },
    component: () => import('@/pages/eventManagement/page/eventBuilt.vue')
  },
  {
    path: '/feedbackRecord',
    name: 'feedbackRecord',
    meta: { title: '反馈记录' },
    component: () => import('@/pages/eventManagement/page/feedbackRecord.vue')
  },
  {
    path: '/feedbackRecordAdd',
    name: 'feedbackRecordAdd',
    meta: { title: '添加反馈' },
    component: () => import('@/pages/eventManagement/page/feedbackRecordBuilt.vue')
  },
  {
    path: '/event-detail',
    name: 'EventDetail',
    component: () => import('@/pages/eventManagement/page/components/eventDetail.vue'),
    meta: {
      index: 103,
      title: '事件详情'
    }
  },
  {
    path: '/deviceDetail',
    name: 'deviceDetail',
    component: () => import('@/pages/eventManagement/page/components/eventDeviceDetail.vue'),
    meta: {
      index: 103,
      title: '事件详情'
    }
  }
]

const router = createRouter({
  // 4. 内部提供了 history 模式的实现。为了简单起见，我们在这里使用 hash 模式。
  history: createWebHashHistory(),
  routes // `routes: routes` 的缩写
})

export default router
