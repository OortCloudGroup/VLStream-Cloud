<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <el-config-provider :locale="elementLocale">
    <div id="app">
      <router-view v-slot="{ Component }">
        <component :is="Component" v-if="Component" />
        <AuthCheckFallback v-else />
      </router-view>
    </div>
  </el-config-provider>
</template>

<script setup>
import { elementLocale } from '@/i18n'
import AuthCheckFallback from '@/components/AuthCheckFallback.vue'
</script>

<style scoped>
#app {
  height: 100vh;
}
</style>
