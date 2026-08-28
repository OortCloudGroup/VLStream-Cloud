<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <div>
    <template v-for="group in groups" :key="group.type">
      <template v-if="group.items.length">
        <div class="answer_group_title"><span /><span>{{ group.label }}</span></div>
        <div class="industry-grid">
          <div v-for="item in group.items" :key="item.industry_id" class="industry-card" :class="{ selected: item.industry_id === selectedId }" @click="selectIndustry(item, group.type)">
            <div class="card-icon">
              <img v-if="item.logo" class="card-img" :src="platformImageUrl(item.logo)" alt="">
              <oort-svg-icon v-else width="40" height="40" name="tenant" />
            </div>
            <div class="card-title">{{ item.industryName || item.industry_name || item.name }}</div>
            <div class="card-description">{{ item.remarks }}</div>
            <div v-if="item.industry_id === selectedId" class="selected-badge"><img :src="selectedIcon" alt=""><span>当前选择</span></div>
          </div>
        </div>
      </template>
    </template>
    <div v-if="!loading && !groups.some(group => group.items.length)" class="no-config-tip">当前为租户未配置相关配置</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPlatformCurrentIndustry, getPlatformIndustryList, savePlatformIndustry } from '@/api/platformHeader'
import { getPlatformAccessToken } from '@/utils/request'
import { openApaasWebPage } from '@/utils/apaasApiBase'
import selectedIcon from '@/assets/img/sele_icon.png'

const props = defineProps({
  userId: { type: String, default: '' },
  tenantId: { type: String, default: '' }
})
const options = ref({ hyConfig: [], cjConfig: [], znConfig: [] })
const selectedId = ref('')
const loading = ref(true)
const groups = computed(() => [
  { type: 1, label: '行业', items: options.value.hyConfig || [] },
  { type: 2, label: '场景', items: options.value.cjConfig || [] },
  { type: 3, label: '职能', items: options.value.znConfig || [] }
])

const platformOrigin = () => {
  try { return new URL(import.meta.env.VITE_PLATFORM_LOGIN_URL, window.location.origin).origin } catch (error) { return window.location.origin }
}
const platformImageUrl = value => {
  if (!value) return ''
  const imageUrl = String(value)
  const gatewayMatch = imageUrl.match(/\/(?:oort)?wj1\/(.*)$/i)
  if (gatewayMatch) return `${platformOrigin()}/bus/wj1/${gatewayMatch[1]}`
  if (/^https?:\/\//i.test(imageUrl)) return imageUrl
  return new URL(imageUrl, `${platformOrigin()}/`).toString()
}
const platformQuery = () => `accessToken=${encodeURIComponent(getPlatformAccessToken() || '')}&fromWhere=console_manage`

const selectIndustry = async(item, type) => {
  try {
    const result = await savePlatformIndustry(item, type, props.tenantId)
    if (Number(result?.code ?? 200) !== 200) throw new Error(result?.msg || result?.message || '切换失败')
    selectedId.value = item.industry_id
    const homepage = item.homepage_url || item.homepageUrl
    if (homepage) {
      if (/^https?:\/\//i.test(homepage)) window.location.href = homepage
      else openApaasWebPage(homepage, platformQuery(), '_self')
    }
  } catch (error) {
    ElMessage.error(error?.message || '切换失败')
  }
}

onMounted(async() => {
  try {
    const list = await getPlatformIndustryList(props.tenantId)
    options.value = { hyConfig: list.hyConfig || [], cjConfig: list.cjConfig || [], znConfig: list.znConfig || [] }
    const current = await getPlatformCurrentIndustry(props.userId, props.tenantId)
    selectedId.value = current?.industry_id || ''
  } catch (error) {
    console.warn('加载平台行业配置失败', error)
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss">
.industry-grid { padding-bottom: 20px; display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.industry-card { width: 306px; height: 137px; padding: 20px; position: relative; display: flex; flex-direction: column; justify-content: center; border: 1px solid #e8e8e8; border-radius: 4px; box-shadow: 0 0 10px rgb(0 0 0 / 7%); background: #fff; cursor: pointer; transition: all 0.3s ease; box-sizing: border-box; &:hover, &.selected { border: 1px solid var(--el-color-primary); border-radius: 4px; box-shadow: 0 0 10px rgb(40 86 165 / 14%); background: var(--el-color-primary-hb); } }
.card-icon { width: 40px; height: 40px; margin-bottom: 8px; display: flex; align-items: center; justify-content: center; .card-img { width: 40px; height: 40px; } }
.card-title { margin-bottom: 4px; color: #333; font-size: 16px; font-weight: 600; }
.card-description { overflow: hidden; color: #999; font-size: 12px; text-overflow: ellipsis; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.selected-badge { padding: 5px 6px; position: absolute; top: 0; right: 0; display: flex; align-items: center; gap: 3px; border-radius: 0 4px 0 12px; color: #fff; font-size: 12px; background-color: var(--el-color-primary); img { width: 14px; height: 14px; } }
.no-config-tip { min-height: 200px; display: flex; align-items: center; justify-content: center; color: #999; font-size: 16px; }
.answer_group_title { margin: 0 0 12px; display: flex; flex-direction: row; align-items: center; span:nth-of-type(1) { width: 4px; height: 20px; margin-right: 6px; background-color: var(--el-color-primary); } span:nth-of-type(2) { color: #333; font-size: 20px; font-weight: 600; line-height: 30px; } }
</style>
