/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus, { ElMessage } from 'element-plus'
import { installSessionMessageGuard } from '@/utils/sessionExpiry'
import { bootstrapRequestIdentity } from '@/utils/requestIdentity.mjs'
import { authManager } from '@/utils/auth'
import { isLocalEventRoute } from '@/utils/localEventAccess.mjs'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { createPinia } from 'pinia'
import './style.css'
import { i18n, translatePhrase } from '@/i18n'
import { installLegacyDomI18n } from '@/i18n/legacyDom'

import basicContainer from './components/basic-container/main.vue'

// and apaas-web events item
import '@/assets/style/index.scss'
import 'virtual:svg-icons-register'
import '@/assets/wvp/iconfont.css'
import '@/assets/wvp/compat.scss'
import '@/assets/style/polish.scss'
import { installWvpCompat } from '@/utils/wvpCompat'
import { capturePendingModelHubCallback } from '@/utils/modelHubAuth'
import { initializeLocalPlatformRealm } from '@/utils/localPlatformRealm'

// OortCloud and VLStream accessToken parameter; in main before .
initializeLocalPlatformRealm()
capturePendingModelHubCallback()

// VLStream token apaas
installSessionMessageGuard(ElMessage)
// Revalidate a restored Back/Forward cache document through the route guard.
window.addEventListener('pageshow', event => { if (event.persisted) window.location.reload() })
bootstrapRequestIdentity()

const app = createApp(App)
app.config.globalProperties.$tp = translatePhrase

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
app.component('Draggable', Draggable)
app.component('Pagination', Pagination)
app.component('RightToolbar', RightToolbar)
app.component('DictTag', DictTag)
app.component('SvgIcon', SvgIcon)
installWvpCompat(app)

app.use(ElementPlus)
app.use(createPinia())
app.use(i18n)
let workOrderFormsInstalled = false
router.beforeResolve(async to => {
  if (!to.meta.workOrderForms || workOrderFormsInstalled) return
  if (isLocalEventRoute(to.meta) && await authManager.getTenantMode() === 'multi') return
  const { default: workOrderForms } = await import('@/components/index_vform.js')
  if (!workOrderFormsInstalled) {
    app.use(workOrderForms)
    workOrderFormsInstalled = true
  }
})
app.use(router)
app.mount('#app')
installLegacyDomI18n()
