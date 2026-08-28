/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { createPinia } from 'pinia'
import './style.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import Avue from '@smallwei/avue';
import '@smallwei/avue/lib/index.css';
import crudCommon from '@/mixins/crud.js';
import axios from './utils/request';
import basicContainer from './components/basic-container/main.vue';

// and apaas-web events item
import '@/assets/style/index.scss'
import 'virtual:svg-icons-register'
import '@/assets/wvp/iconfont.css'
import '@/assets/wvp/compat.scss'
import { installWvpCompat } from '@/utils/wvpCompat'
import { capturePendingModelHubCallback } from '@/utils/modelHubAuth'

window.$crudCommon = crudCommon;

// OortCloud and VLStream accessToken parameter; in main before .
capturePendingModelHubCallback()

// VLStream token apaas
const vlsAccessToken = localStorage.getItem('accessToken')
if (vlsAccessToken) {
  localStorage.setItem('apaas_token', vlsAccessToken)
  sessionStorage.setItem('token', vlsAccessToken)
}

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.component('basicContainer', basicContainer);

// and apaas-web events/main.ts full component
import aiIconRemark from '@/components/aiIconRemark.vue'
import TableSelf from '@/components/TableSelf.vue'
import ButtonGroup from '@/components/buttonGroup.vue'
import SearchHeightBox from '@/components/SearchHeightBox.vue'
import ExportExcelPdf from '@/components/exportExcelPdf.vue'
import Directive from '@/directive/index'
import commonComponents from '@/components/index.js'
import indexVform from '@/components/index_vform.js'
import Draggable from 'vuedraggable'
import Pagination from '@/components/Pagination/index.vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import SvgIcon from '@/components/SvgIcon/index.vue'

app.component('AiIconRemark', aiIconRemark)
app.component('TableSelf', TableSelf)
app.component('ButtonGroup', ButtonGroup)
app.component('SearchHeightBox', SearchHeightBox)
app.component('ExportExcelPdf', ExportExcelPdf)
app.use(Directive)
app.use(commonComponents)
app.use(indexVform)
app.component('Draggable', Draggable)
app.component('Pagination', Pagination)
app.component('RightToolbar', RightToolbar)
app.component('DictTag', DictTag)
app.component('SvgIcon', SvgIcon)
installWvpCompat(app)

app.use(ElementPlus, {
  locale: zhCn,
})
app.use(ElementPlus)
app.use(createPinia())
app.use(router)
app.use(Avue, {
  axios,
  calcHeight: 10,
});
app.mount('#app')
