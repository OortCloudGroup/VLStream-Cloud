<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div class="pover_right">
    <div class="app_pover_apps">
      <div v-for="(item, index) in appList" :key="item.id || item.app_id || index" class="app_pover_apps_item" @click="emit('open-app', item)">
        <img v-if="item.icon_url" :src="platformImageUrl(item.icon_url)" alt="" @error="useDefaultIcon">
        <img v-else :src="defaultAppIcon" alt="">
        <div class="app_pover_apps_item_name">
          {{ item.applabel }}
        </div>
      </div>
      <div v-if="appList.length === 0" class="no_data">
        <img :src="noAppImage" alt="">
        <span>暂无应用</span>
      </div>
    </div>
    <div class="more_apps">
      <div class="more_apps_tips" @click="emit('more-apps')">
        <span>更多应用</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getPlatformApps } from '@/api/platformHeader'
import defaultAppIcon from '@/assets/img/login/icon_user.png'
import noAppImage from '@/assets/img/no_app_list.png'

const props = defineProps({
  userId: { type: String, default: '' }
})
const emit = defineEmits(['open-app', 'more-apps'])
const appList = ref([])

const platformOrigin = () => {
  const loginUrl = import.meta.env.VITE_PLATFORM_LOGIN_URL
  try {
    return loginUrl ? new URL(loginUrl, window.location.origin).origin : window.location.origin
  } catch (error) {
    return window.location.origin
  }
}

const platformImageUrl = value => {
  if (!value) return defaultAppIcon
  const imageUrl = String(value)
  const gatewayMatch = imageUrl.match(/\/(?:oort)?wj1\/(.*)$/i)
  if (gatewayMatch) return `${platformOrigin()}/bus/wj1/${gatewayMatch[1]}`
  if (/^https?:\/\//i.test(imageUrl)) return imageUrl
  return new URL(imageUrl, `${platformOrigin()}/`).toString()
}

const useDefaultIcon = event => {
  if (event?.target && event.target.src !== defaultAppIcon) event.target.src = defaultAppIcon
}

onMounted(async() => {
  try {
    appList.value = await getPlatformApps(props.userId)
  } catch (error) {
    appList.value = []
    console.warn('加载平台应用失败', error)
  }
})
</script>

<style scoped lang="scss">
.pover_right {
  width: 100%;
  display: flex;
  flex-direction: column;
  border-radius: var(--common-border-radius);
  background-color: #edf3f9;
}

.app_pover_apps {
  width: 98%;
  max-height: 50vh;
  margin: 0 auto;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  overflow: auto;
  border-radius: var(--common-border-radius);
  scrollbar-width: thin;
  scrollbar-color: #dde3ea transparent;

  &::-webkit-scrollbar { width: 6px; height: 6px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { border-radius: 3px; background: #dde3ea; }
}

.app_pover_apps_item {
  width: 100px;
  height: 100px;
  margin: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  &:hover {
    border-radius: var(--common-border-radius);
    background-color: #dde3ea;
  }

  img {
    width: 40px;
    height: 40px;
    border-radius: 100%;
    margin: 8px 0;
    margin-top: 4px;
  }
}

.app_pover_apps_item_name {
  height: 32px;
  padding: 2px 4px;
  color: #575656;
  font-size: 14px;
  line-height: 18px;
  text-align: center;
}

.more_apps {
  height: 48px;
  margin: 12px 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.more_apps_tips {
  padding: 2px 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #2278ff;
  border-radius: 136px;
  cursor: pointer;

  span { color: #2278ff; font-size: 18px; }
}

.no_data {
  width: 100%;
  height: 320px;
  margin: 8px 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  img { width: 160px; height: auto; margin: 8px 0; border-radius: 100%; }
  span { margin-top: 10px; color: #999; font-size: 16px; }
}
</style>
