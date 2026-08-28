<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <component :is="eventManagementComponent" v-if="eventManagementComponent" />
</template>

<script setup>
import { defineAsyncComponent, onMounted, shallowRef } from 'vue'
import { getTenantMode } from '@/api/system/localAuth'

const LegacyEventManagement = defineAsyncComponent(() => import('./EventManagement.vue'))
const ActiveSafetyEventManagement = defineAsyncComponent(() => import('@/views/events/page/eventManagement/secure.vue'))
const eventManagementComponent = shallowRef(null)

onMounted(async () => {
  try {
    const response = await getTenantMode()
    const tenantType = response?.data?.tenantType || response?.data?.data?.tenantType
    eventManagementComponent.value = tenantType === 'multi'
      ? LegacyEventManagement
      : ActiveSafetyEventManagement
  } catch (error) {
    console.warn('获取租户模式失败，继续使用单租户事件管理页面:', error?.message)
    eventManagementComponent.value = ActiveSafetyEventManagement
  }
})
</script>
