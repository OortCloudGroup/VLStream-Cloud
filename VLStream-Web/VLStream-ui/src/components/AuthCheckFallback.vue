<template>
  <section class="auth-check" :class="{ 'auth-check-notice': compact }" role="status" aria-live="polite">
    <template v-if="authCheckFailed">
      <strong>{{ text(authCheckForbidden ? '拒绝访问' : '服务暂时不可用，请稍后重试') }}</strong>
      <div class="auth-check-actions">
        <el-button type="primary" @click="retry">{{ text('重试') }}</el-button>
        <el-button @click="openLogin">{{ text('登录') }}</el-button>
      </div>
    </template>
    <p v-else>{{ text('加载中...') }}</p>
  </section>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { translatePhrase as text } from '@/i18n'
import { authCheckFailed, authCheckForbidden, authCheckRetryPath, routeLoadFailed } from '@/utils/authCheckState'
import { safeAuthReturnPath } from '@/utils/authRedirect.mjs'

const router = useRouter()
defineProps({ compact: Boolean })
const returnPath = () => safeAuthReturnPath(authCheckRetryPath.value, import.meta.env.BASE_URL)
const retry = () => {
  const path = returnPath()
  if (routeLoadFailed.value) window.location.replace(router.resolve(path).href)
  else return router.replace(path)
}
const openLogin = () => router.replace({ path: '/login', query: { redirect: returnPath() } })
</script>

<style scoped>
.auth-check {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 24px;
  box-sizing: border-box;
  background: #f5f7fa;
  color: #303133;
}
.auth-check strong { font-size: 20px; font-weight: 500; }
.auth-check-actions { display: flex; gap: 12px; }
.auth-check-notice {
  position: fixed;
  inset-block-start: 72px;
  inset-inline-end: 16px;
  z-index: 2100;
  min-height: auto;
  max-width: calc(100vw - 32px);
  gap: 12px;
  padding: 16px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  box-shadow: var(--el-box-shadow-light);
}
.auth-check-notice strong { font-size: 14px; }
</style>
