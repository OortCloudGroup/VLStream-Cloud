<template>
  <main class="auth-check" role="status" aria-live="polite">
    <template v-if="authCheckFailed">
      <h1>{{ text(authCheckForbidden ? '拒绝访问' : '服务暂时不可用，请稍后重试') }}</h1>
      <div class="auth-check-actions">
        <el-button type="primary" @click="retry">{{ text('重试') }}</el-button>
        <el-button @click="openLogin">{{ text('登录') }}</el-button>
      </div>
    </template>
    <p v-else>{{ text('加载中...') }}</p>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { translatePhrase as text } from '@/i18n'
import { authCheckFailed, authCheckForbidden, authCheckRetryPath } from '@/utils/authCheckState'
import { safeAuthReturnPath } from '@/utils/authRedirect.mjs'

const router = useRouter()
const returnPath = () => safeAuthReturnPath(authCheckRetryPath.value, import.meta.env.BASE_URL)
const retry = () => router.replace(returnPath())
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
.auth-check h1 { margin: 0; font-size: 20px; font-weight: 500; }
.auth-check-actions { display: flex; gap: 12px; }
</style>
