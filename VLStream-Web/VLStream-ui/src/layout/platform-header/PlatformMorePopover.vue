<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div class="pover_right">
    <div class="pover_more">
      <div class="pover_more_item" @click="emit('change-tenant')">
        <div class="pover_more_item_title">
          <div><img :src="tenantIcon"><span>{{ tenantInfo.tenant_name || labels.currentTenant }}</span></div>
          <el-icon><ArrowRightBold /></el-icon>
        </div>
      </div>
      <div v-if="currentIndustryLabel" class="pover_more_item" @click="emit('change-industry')">
        <div class="pover_more_item_title">
          <div><img :src="industryIcon"><span>{{ currentIndustryLabel }}</span></div>
          <el-icon><ArrowRightBold /></el-icon>
        </div>
      </div>
      <div class="pover_more_item">
        <div class="pover_more_item_title" @click="emit('privacy')">
          <div><img :src="privacyIcon"><span>{{ labels.privacy }}</span></div>
          <el-icon><ArrowRightBold /></el-icon>
        </div>
      </div>
      <div class="pover_more_item">
        <div class="pover_more_item_title">
          <div><img :src="feedbackIcon"><span>{{ labels.feedback }}</span></div>
          <el-icon><ArrowRightBold /></el-icon>
        </div>
      </div>
      <div class="pover_more_item">
        <div class="pover_more_item_title" @click="openThemePanel = !openThemePanel">
          <div><img :src="themeIcon"><span>{{ labels.theme }}</span></div>
          <el-icon :class="{ 'rotate-icon': openThemePanel }"><ArrowRightBold /></el-icon>
        </div>
        <div v-if="openThemePanel" class="pover_more_item_content">
          <div class="theme_predefine">
            <div
              v-for="(item, index) in predefineColors"
              :key="index"
              :style="{ backgroundColor: localThemeColor === item ? '#ffffff00' : item, borderColor: item }"
              class="theme_predefine_item"
              :class="{ 'theme_predefine_item_active': localThemeColor === item }"
              @click="changeThemeColor(item)"
            >
              <div v-if="localThemeColor === item" :style="{ backgroundColor: item }" />
            </div>
            <el-color-picker v-model="localThemeColor" class="theme_predefine_item" :teleported="false" @change="changeThemeColor" />
          </div>
        </div>
      </div>
      <div class="pover_more_item" @click="emit('switch-account')">
        <div class="pover_more_item_title">
          <div><img :src="switchIcon"><span>{{ labels.switchAccount }}</span></div>
          <el-icon><ArrowRightBold /></el-icon>
        </div>
      </div>
      <div class="pover_more_item" @click="emit('logout')">
        <div class="pover_more_item_title">
          <div><img :src="logoutIcon"><span style="color: #ff3c3c">{{ labels.logout }}</span></div>
          <el-icon><ArrowRightBold /></el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ArrowRightBold } from '@element-plus/icons-vue'
import { getPlatformCurrentIndustry, getPlatformTenant } from '@/api/platformHeader'
import tenantIcon from '@/assets/img/popover_tenant.png'
import industryIcon from '@/assets/img/popover_hy.png'
import privacyIcon from '@/assets/img/popover_yinsi.png'
import feedbackIcon from '@/assets/img/popover_feecback.png'
import themeIcon from '@/assets/img/popover_theme.png'
import switchIcon from '@/assets/img/prpover_switch.png'
import logoutIcon from '@/assets/img/popover_logout.png'

const props = defineProps({
  labels: { type: Object, required: true },
  userId: { type: String, default: '' },
  tenantId: { type: String, default: '' },
  themeColor: { type: String, default: '#2856A5' },
  predefineColors: { type: Array, default: () => [] }
})
const emit = defineEmits(['change-tenant', 'change-industry', 'privacy', 'switch-account', 'logout', 'theme-change'])
const tenantInfo = ref({})
const currentIndustry = ref({})
const openThemePanel = ref(false)
const localThemeColor = ref(props.themeColor)

const industryName = computed(() => currentIndustry.value.industryName || currentIndustry.value.industry_name || currentIndustry.value.name || '')
const currentIndustryLabel = computed(() => {
  const type = Number(currentIndustry.value.set_type)
  const prefix = type === 1 ? props.labels.industry : type === 2 ? props.labels.scene : type === 3 ? props.labels.function : ''
  return prefix && industryName.value ? `${prefix}（${industryName.value}）` : ''
})

const changeThemeColor = color => {
  if (!color) return
  localThemeColor.value = color
  emit('theme-change', color)
}

onMounted(async() => {
  const [tenantResult, industryResult] = await Promise.allSettled([
    getPlatformTenant(),
    getPlatformCurrentIndustry(props.userId, props.tenantId)
  ])
  if (tenantResult.status === 'fulfilled') tenantInfo.value = tenantResult.value || {}
  if (industryResult.status === 'fulfilled') currentIndustry.value = industryResult.value || {}
})
</script>

<style scoped lang="scss">
.pover_right {
  width: 100%;
  display: flex;
  flex-direction: column;
  border-radius: var(--common-border-radius);
  background-color: #fff;
}

.pover_more {
  width: 100%;
  padding: 0;
  display: flex;
  flex-direction: column;
  border-radius: 10px;
}

.pover_more_item {
  min-height: 48px;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  background-color: #edf3f9;

  &:hover { background-color: #dde3ea; }
}

.pover_more_item_title {
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;

  > div {
    display: flex;
    align-items: center;

    img { width: 24px; height: 24px; margin-right: 8px; }
    span { color: #575656; font-size: 16px; }
  }
}

.pover_more_item_content { display: flex; border-top: 1px solid #fdfdfd; }

.theme_predefine {
  padding: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.theme_predefine_item {
  width: 24px;
  height: 24px;
  border: 1px solid #f8f8f9;
  border-radius: 4px;
  cursor: pointer;

  &:hover { border: 1px solid #464646; }
}

.theme_predefine_item_active {
  display: flex;
  align-items: center;
  justify-content: center;

  div { width: 16px !important; height: 16px !important; border-radius: 2px; }
}

.el-icon { cursor: pointer; transition: transform 0.3s ease; }
.rotate-icon { transform: rotate(90deg); }

:deep(.el-color-picker) { width: 24px; height: 24px; border: 1px solid #f8f8f9; border-radius: 4px; cursor: pointer; }
:deep(.el-color-picker__trigger) { width: 100% !important; height: 100% !important; padding: 0; border: 0; border-radius: 4px; }
</style>
