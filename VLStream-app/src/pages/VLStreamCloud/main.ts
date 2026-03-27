/*
 * @Author: lanjian
 * @Date: 2021-11-13 09:13:06
 * @LastEditors: lanjian
 * @LastEditTime: 2021-11-27 12:19:09
 * @FilePath: \cordava_utils\demo\src\main.js
 * @Description: Copyright 奥尔特云（深圳）智慧科技有限公司. All rights reserved.
 */
import { createApp } from 'vue'
import store from '@/store'
import App from './App.vue'

import router from './router/index'

const VueApp = createApp(App)

// 表单渲染
import Vant from 'vant'	// 引入vant库
import 'vant/lib/index.css'	// 引入vant样式
VueApp.use(Vant) // 全局注册vant

import VmFormRender from '@/components/VForm/index.js'
VueApp.use(VmFormRender) // 全局注册VFormRender等组件

import i18n from '@/lang/index'
VueApp.use(i18n)

VueApp.use(store)
VueApp.use(router)
VueApp.mount('#app')
