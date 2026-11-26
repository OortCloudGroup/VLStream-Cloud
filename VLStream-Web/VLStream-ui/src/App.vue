<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <el-config-provider :locale="elementLocale">
    <div class="application-view">
      <router-view v-slot="{ Component }">
        <component :is="Component" v-if="Component" />
        <AuthCheckFallback v-if="!Component || authCheckFailed" :compact="Boolean(Component)" />
      </router-view>
    </div>
  </el-config-provider>
</template>

<script setup>
import { elementLocale } from '@/i18n'
import AuthCheckFallback from '@/components/AuthCheckFallback.vue'
import { authCheckFailed } from '@/utils/authCheckState'
</script>

<style scoped>
.application-view {
  height: 100vh;
}
</style>
