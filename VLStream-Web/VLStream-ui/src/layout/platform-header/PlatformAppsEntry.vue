<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <el-popover
    v-model:visible="appsVisible"
    placement="bottom-start"
    :width="popoverWidth"
    :disabled="!hasPlatformToken"
    trigger="hover"
    popper-class="popover_panel"
  >
    <template #reference>
      <button type="button" class="apps-entry" :aria-label="uiText('应用')" @click="handleEntryClick"><img :src="gridIcon" alt=""></button>
    </template>
    <PlatformModuleAppsPopover v-if="appsVisible" :user-id="platformUserId" :tenant-id="platformTenantId" @open-app="openApp" @more-apps="openAppMarket" />
  </el-popover>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPlatformHeaderUser, verifyPlatformToken } from '@/api/platformHeader'
import { getPlatformAccessToken } from '@/utils/request'
import { openApaasWebPage } from '@/utils/apaasApiBase'
import { redirectToPlatformLogin } from '@/utils/platformSession'
import { translatePhrase as uiText } from '@/i18n'
import gridIcon from '@/assets/img/svg/platform-grid-blue.svg'
import PlatformModuleAppsPopover from './PlatformModuleAppsPopover.vue'

const appsVisible = ref(false)
const hasPlatformToken = ref(Boolean(getPlatformAccessToken()))
const platformUserId = ref('')
const platformTenantId = ref('')
const popoverWidth = `${Math.round(window.innerWidth * 380 / 1920)}px`

const handleEntryClick = () => {
  if (!hasPlatformToken.value) redirectToPlatformLogin()
}

onMounted(async() => {
  if (!hasPlatformToken.value) return
  try {
    const user = await getPlatformHeaderUser()
    platformUserId.value = user?.user_id || user?.userId || user?.id || ''
    platformTenantId.value = user?.tenant_id || user?.tenantId || user?.tenant?.tenant_id || user?.tenant?.tenantId || ''
  } catch (error) {
    console.warn('加载平台用户信息失败', error)
  }
})

const ensurePlatformSession = async() => {
  try {
    const result = await verifyPlatformToken()
    if (Number(result?.code ?? 200) === 200) return true
  } catch (error) { console.warn('平台令牌校验失败', error) }
  ElMessage.warning(uiText('用户信息已失效，请重新登录'))
  return false
}
const platformQuery = () => `accessToken=${encodeURIComponent(getPlatformAccessToken() || '')}&fromWhere=console_manage`
const openApp = async app => {
  const path = app.apk_url || app.appweburl || app.url || app.path
  if (!path || !(await ensurePlatformSession())) return
  sessionStorage.setItem('tempObj', JSON.stringify({ accessToken: getPlatformAccessToken(), fromWhere: 'console_manage' }))
  const query = platformQuery()
  const target = Number(app.open_mod) === 2 ? '_self' : Number(app.open_mod) === 3 ? (app.applabel || '_blank') : '_blank'
  const features = Number(app.open_mod) === 1 ? '' : 'popup,location=no'
  if (/^https?:\/\//i.test(path) || path.startsWith('//')) window.open(`${path}${path.includes('?') ? '&' : '?'}${query}`, target, features)
  else openApaasWebPage(path, query, target, features)
  appsVisible.value = false
}
const openAppMarket = async() => {
  if (!(await ensurePlatformSession())) return
  openApaasWebPage('/app_market/index.html#/moreApp', platformQuery())
  appsVisible.value = false
}
</script>

<style scoped lang="scss">
.apps-entry {
  width: 24px;
  height: 24px;
  margin: 0;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  cursor: pointer;

  &:hover { border-radius: 4px; }

  img { width: 24px; height: 24px; display: block; }
}
</style>
