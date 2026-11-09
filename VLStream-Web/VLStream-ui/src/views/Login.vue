<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div class="login-container">
    <label class="login-locale-switcher">
      <select :aria-label="$t('app.language')" :value="currentLocale" @change="setLocale($event.target.value)">
        <option v-for="option in localeOptions" :key="option.code" :value="option.code">
          {{ option.name }}
        </option>
      </select>
    </label>
    <header class="login-brand" aria-label="VLStream Cloud"><span class="brand-symbol" aria-hidden="true">VLS</span><span>VLStream <em>Cloud</em></span></header>
    <section class="vision-story" :aria-label="$tp('视觉智能平台')">
      <div class="optical-scene" aria-hidden="true">
        <div class="optical-orbit orbit-outer"></div><div class="optical-orbit orbit-inner"></div>
        <div class="optical-plane plane-back"></div>
        <div class="optical-lens"><div class="lens-rim"></div><div class="lens-glass"><div class="lens-core"></div></div><div class="lens-reflection"></div></div>
        <div class="optical-plane plane-front"><div class="perception-grid"></div></div>
        <span class="optical-point point-one"></span><span class="optical-point point-two"></span><span class="optical-point point-three"></span>
      </div>
      <div class="vision-copy"><h2>{{ $tp('看见，更进一步。') }}</h2><p>{{ $tp('连接现场与业务的视觉智能平台') }}</p></div>
    </section>
    <div class="login-card">
      <!--  -->
      <div class="login-header">
        <span class="login-kicker">VLSTREAM CONSOLE</span>
        <h1 class="login-title">{{ $tp('欢迎回来') }}</h1>
        <p class="login-subtitle">{{ $tp('登录您的工作空间') }}</p>
      </div>

      <!-- form -->
      <div v-if="tenantMode === 'single'" class="login-form">
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-position="top"
          @submit.prevent="handleLogin"
        >
          <!-- user -->
          <el-form-item prop="loginId" :label="$tp('用户名')">
            <el-input
              v-model="loginForm.loginId"
              autocomplete="username"
              :placeholder="$tp('请输入用户名')"
              prefix-icon="el-icon-user"
              size="large"
            />
          </el-form-item>

          <!--  -->
          <el-form-item prop="password" :label="$tp('密码')">
            <el-input
              v-model="loginForm.password"
              autocomplete="current-password"
              type="password"
              :placeholder="$tp('请输入密码')"
              prefix-icon="el-icon-lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <!-- button -->
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loginLoading"
              class="login-button"
              @click="handleLogin"
            >
              {{ loginLoading ? $tp('登录中...') : $tp('登录') }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>
      <div v-else-if="tenantMode === 'multi'" class="platform-login-tip">
        <h3>{{ $tp('请从应用平台进入') }}</h3>
        <p>{{ $tp('当前服务已启用多租户模式，请从应用平台点击 VLStream 进入。') }}</p>
        <p v-if="platformLoginError" class="platform-login-error">{{ platformLoginError }}</p>
        <el-button type="primary" size="large" class="login-button" @click="handlePlatformLogin"> {{ $tp('前往应用平台') }} </el-button>
      </div>
      <div v-else class="platform-login-tip">{{ $tp('正在读取登录模式...') }}</div>

    </div>

    <footer class="login-footer">VLStream Cloud <span>·</span> {{ $tp('视觉智能平台') }}</footer>
  </div>
</template>

<script setup>
import { translatePhrase as uiText } from '@/i18n'

import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import smCrypto from 'sm-crypto'
import { safeAuthReturnPath } from '@/utils/authRedirect.mjs'
import { isSessionExpiryError } from '@/utils/sessionExpiry'
import { AuthManager } from '@/utils/auth'
import { extractAuthCallbackParams, stripAuthCallbackParams } from '@/utils/authCallbackParams.mjs'
import { logSafeRequestError } from '@/utils/safeRequestError.mjs'
import { loginUser } from '@/api/auth'
import { currentLocale, localeOptions, setLocale } from '@/i18n'

const router = useRouter()
const route = useRoute()
const authManager = new AuthManager()
const { sm2 } = smCrypto
const BLADE_AUTH_PUBLIC_KEY = import.meta.env.VITE_BLADE_AUTH_PUBLIC_KEY || '049787e408dea94acb3655acc5a7c7c7010bb9f140c84926c667ea616366082a118141c8dcb3e78a9d85d64fb765a250ff73448b18938f2219b94f782e28e1df64'
const SINGLE_TENANT_ID = '000000'
// form
const loginFormRef = ref()

// formdata
const loginForm = reactive({
  loginId: '',
  password: '',
  tenantId: SINGLE_TENANT_ID
})

//
const loginLoading = ref(false)
const tenantMode = ref('loading')
const platformLoginError = ref('')

// form
const loginRules = {
  loginId: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

// after configuration SM2 .
const encryptPassword = (password) => {
  return sm2.doEncrypt(password, BLADE_AUTH_PUBLIC_KEY, 0)
}

const handlePlatformLogin = () => {
  authManager.redirectToPlatformLogin(safeAuthReturnPath(route.query.redirect, import.meta.env.BASE_URL))
}

// 表单校验：Element Plus 的 validate() 校验失败时是 reject 而不是返回 false，
// 必须单独捕获，否则会落进下面请求失败的 catch 里被误报成“网络错误”。
const isLoginFormValid = async () => {
  try {
    await loginFormRef.value.validate()
    return true
  } catch {
    return false
  }
}

// 只有真正发出去并失败的请求才提示网络类错误，业务错误优先展示后端原文。
const resolveLoginErrorMessage = (error) => {
  const payload = error.response?.data || error.data
  const businessMessage = payload?.msg || payload?.message || (typeof payload === 'string' ? payload : '')
  if (businessMessage) return businessMessage
  if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') return uiText('请求超时，请稍后重试')
  if (!error.response) return uiText('服务暂时不可用，请检查网络或稍后重试')
  const status = Number(error.response.status)
  if (status === 400 || status === 401) return uiText('账号或密码错误')
  if (status === 403) return uiText('账号无登录权限')
  if (status === 503) return uiText('服务暂时不可用，请稍后重试')
  if (status >= 500) return uiText('服务异常，请稍后重试')
  return error.message || uiText('登录失败')
}

// Process
// 同步占位：校验是异步的，仅靠 loginLoading 在 await 期间仍有空窗，
// 连续两次回车或回车+点击会重复提交。
let loginPending = false

const handleLogin = async () => {
  if (!loginFormRef.value || loginPending) return
  loginPending = true

  try {
    if (!(await isLoginFormValid())) return

    loginLoading.value = true

    // Build SpringBlade parameter
    const loginData = {
      grantType: 'password',
      tenantId: SINGLE_TENANT_ID,
      account: loginForm.loginId,
      password: encryptPassword(loginForm.password)
    }

    // interface
    const response = await loginUser(loginData)

    if (response.code === 200) {
      const authInfo = response.data
      const userData = {
        ...authInfo,
        loginId: authInfo.account,
        userName: authInfo.userName || authInfo.account
      }

      // userinfo data
      authManager.invalidatePendingIdentity()
      await authManager.saveUserToLocal(userData)

      ElMessage.success(uiText('登录成功'))

      // successfully after , redirect .
      await router.replace(safeAuthReturnPath(route.query.redirect, import.meta.env.BASE_URL))

    } else {
      ElMessage.error(response.msg || uiText('登录失败'))
    }

  } catch (error) {
    if (isSessionExpiryError(error)) return
    logSafeRequestError('登录失败', error)
    ElMessage.error(uiText('登录失败: ') + resolveLoginErrorMessage(error))
  } finally {
    loginLoading.value = false
    loginPending = false
  }
}

// pageLoad whether already
onMounted(async () => {
  const callbackHref = window.location.href
  const { accessToken: token } = extractAuthCallbackParams(callbackHref)
  authManager.cleanUrlToken()
  const cleanRoute = router.resolve(stripAuthCallbackParams(route.fullPath))
  if (cleanRoute.fullPath !== route.fullPath) {
    await router.replace({ path: cleanRoute.path, query: cleanRoute.query, hash: cleanRoute.hash })
  }
  tenantMode.value = await authManager.getTenantMode()
  if (route.query.platformCallbackError === '1') platformLoginError.value = '平台登录凭证校验失败，请返回统一平台重新进入。'

  if (token) {
    try {
      const userInfo = await authManager.checkUrlToken(callbackHref)
      if (userInfo) {
        const redirect = safeAuthReturnPath(route.query.redirect, import.meta.env.BASE_URL)
        await router.replace(redirect)
        return
      }
      if (tenantMode.value === 'multi') {
        platformLoginError.value = '平台登录凭证校验失败，请返回统一平台重新进入。'
      }
    } catch (error) {
      logSafeRequestError('平台回调校验失败', error)
      platformLoginError.value = '平台登录失败，请重新授权或稍后重试'
    }
  }

  if (tenantMode.value === 'multi') {
    return
  }

  // token
  const localToken = authManager.getCurrentToken()
  if (localToken) {
    try {
      const userInfo = await authManager.checkLocalToken()
      if (userInfo) {
        await router.replace(safeAuthReturnPath(route.query.redirect, import.meta.env.BASE_URL))
        return
      }
    } catch (error) {
      logSafeRequestError('本地会话验证失败', error)
    }
  }
})
</script>

<style scoped>
.login-container {
  box-sizing: border-box;
  min-height: 100vh;
  min-height: 100svh;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: clamp(48px, 7vw, 120px);
  padding: 120px 6vw 100px;
  position: relative;
  overflow: hidden;
  isolation: isolate;
  color: #203247;
  background: radial-gradient(ellipse at 24% 40%, #d5e6f3 0, transparent 52%), radial-gradient(ellipse at 82% 80%, #e0e6f0 0, transparent 50%), #f2f5f8;
}
.login-container::before {
  content: "";
  position: absolute;
  inset: -30%;
  z-index: -1;
  pointer-events: none;
  background: linear-gradient(120deg, transparent 35%, #ffffffa6 48%, transparent 60%);
  animation: ambient-light 18s ease-in-out infinite alternate;
}
.login-brand { position: absolute; inset-block-start: 38px; inset-inline-start: 4vw; display: flex; align-items: center; gap: 12px; color: #203c5a; font-size: 24px; font-weight: 650; letter-spacing: -.8px; }
.login-brand em { font-style: normal; font-weight: 400; color: #688198; }
.brand-symbol { display: grid; place-items: center; width: 38px; height: 38px; border: 1.5px solid #3e77b5; border-radius: 50%; color: #3473b9; font-size: 12px; letter-spacing: -.5px; }
.login-locale-switcher { position: absolute; inset-block-start: 38px; inset-inline-end: 4vw; z-index: 2; }
.login-locale-switcher select { max-width: 170px; padding: 10px 28px 10px 12px; border: 1px solid #ffffffad; border-radius: 10px; color: #50647a; background: #ffffff59; outline-offset: 4px; cursor: pointer; }
.vision-story { width: min(44vw, 600px); flex: 0 1 600px; text-align: center; animation: enter .9s ease-out both; }
.optical-scene { position: relative; width: 100%; aspect-ratio: 1.2; direction: ltr; }
.optical-orbit { position: absolute; border: 1px solid #8aa7c32e; border-radius: 50%; inset: 5% 10%; transform: rotate(-28deg) scaleY(.76); }
.orbit-inner { inset: 0 16%; transform: rotate(38deg) scaleY(.86); border-color: #ffffffa8; }
.optical-lens { position: absolute; width: 60%; aspect-ratio: 1; left: 20%; top: 13%; border-radius: 50%; background: linear-gradient(135deg, #f9fcff, #a2bdd1 48%, #edf5fb 70%, #b3c8da); box-shadow: 18px 38px 50px #486b8e26, inset 1px 2px 2px #fff, inset -2px -2px 6px #6c92b64d; transform: rotate(-24deg) scaleY(.94); animation: lens-float 9s ease-in-out infinite; }
.lens-rim { position: absolute; inset: 6%; border-radius: 50%; border: 1px solid #fff9; background: repeating-conic-gradient(from 0deg, #a0b7ca66 0deg 1deg, #edf4f97a 1deg 3deg); box-shadow: inset 0 0 12px #7f9bb559; }
.lens-glass { position: absolute; inset: 13%; border-radius: 50%; background: conic-gradient(from 35deg, #739ab9, #e9f6ff, #417398, #a9ccdf, #325575, #adcfe2, #739ab9); box-shadow: inset 0 0 0 7px #dcebf17a, inset 0 0 30px #1c4467; }
.lens-core { position: absolute; inset: 20%; border-radius: 50%; background: radial-gradient(circle at 35% 28%, #b1e1f5 0, #4385b0 20%, #1b4767 49%, #092c49 69%, #4f8bb2 74%, #193e5b 80%); box-shadow: 0 0 0 10px #edf7ff2b, 0 0 0 11px #e7f5ff80, 0 0 0 23px #17395324; }
.lens-core::after { content: ""; position: absolute; width: 30%; height: 15%; top: 22%; left: 20%; border-radius: 50%; transform: rotate(-30deg); background: #ffffffb3; filter: blur(5px); }
.lens-reflection { position: absolute; inset: 0; border-radius: 50%; background: linear-gradient(125deg, #fff0 25%, #ffffff80 41%, #fff0 55%); animation: lens-shine 9s ease-in-out infinite; }
.optical-plane { position: absolute; border: 1px solid #ffffffcc; border-radius: 22px; background: linear-gradient(140deg, #ffffff82, #ffffff0a); box-shadow: 0 18px 36px #52749112, inset 0 1px #fff; backdrop-filter: blur(6px); }
.plane-back { width: 38%; height: 39%; left: 7%; top: 10%; transform: rotate(-16deg); }
.plane-front { width: 35%; height: 37%; right: 5%; bottom: 12%; transform: rotate(12deg); animation: plane-float 9s ease-in-out infinite; }
.perception-grid { position: absolute; inset: 22%; background: radial-gradient(circle, #5687af99 1.3px, transparent 1.8px) 0 0 / 14px 14px; mask-image: radial-gradient(ellipse, #000 20%, transparent 75%); }
.optical-point { position: absolute; width: 6px; height: 6px; border-radius: 50%; background: #89afd0; box-shadow: 0 0 0 5px #ffffff80; }
.point-one { top: 15%; right: 25%; }.point-two { top: 52%; left: 12%; }.point-three { bottom: 12%; left: 42%; }
.vision-copy { margin-top: 12px; }
.vision-copy h2 { margin: 0 0 16px; font-size: clamp(28px, 2.8vw, 40px); font-weight: 500; letter-spacing: 2px; color: #2b4661; }
.vision-copy p { margin: 0; color: #697f94; font-size: 15px; letter-spacing: 1px; line-height: 1.8; }
.login-card { box-sizing: border-box; flex: 0 0 410px; width: 410px; max-width: 100%; padding: 48px 40px; position: relative; border-radius: 24px; border: 1px solid #ffffffd9; background: linear-gradient(150deg, #ffffffbf, #ffffff69); backdrop-filter: blur(30px) saturate(130%); -webkit-backdrop-filter: blur(30px) saturate(130%); box-shadow: 0 28px 70px #36557414, 0 4px 12px #36557405, inset 0 1px 0 #fff; animation: enter .8s .12s ease-out both; }
.login-header { margin-bottom: 36px; text-align: start; }
.login-kicker { color: #72869a; font-size: 10px; font-weight: 600; letter-spacing: 2.5px; }
.login-title { margin: 18px 0 10px; color: #243b53; font-size: 28px; font-weight: 600; letter-spacing: -.5px; }
.login-subtitle { margin: 0; color: #78899b; font-size: 14px; line-height: 1.6; }
.login-form :deep(.el-form-item) { margin-bottom: 24px; }
.login-form :deep(.el-form-item__label) { font-size: 13px; color: #4c6177; margin-bottom: 9px; line-height: 20px; }
.login-form :deep(.el-form-item__label::before) { display: none; }
.login-form :deep(.el-form-item:last-child) { margin: 32px 0 0; }
.login-card :deep(.el-input__wrapper) { min-height: 48px; box-sizing: border-box; padding: 4px 14px; border-radius: 10px; background: #ffffff73; box-shadow: inset 0 0 0 1px #9aafc333; transition: background .2s, box-shadow .2s; }
.login-card :deep(.el-input__wrapper:hover) { background: #ffffffb3; box-shadow: inset 0 0 0 1px #8eabc066; }
.login-card :deep(.el-input__wrapper.is-focus) { background: #ffffffe0; box-shadow: 0 0 0 3px #267aff12, inset 0 0 0 1px #548dd3; }
.login-card :deep(.el-input__inner) { color: #243b53; background: transparent; }
.login-card :deep(.el-input__inner::placeholder) { color: #98a7b6; font-size: 13px; }
.login-card :deep(input:-webkit-autofill) { -webkit-box-shadow: 0 0 0 1000px #eff5fa inset; -webkit-text-fill-color: #243b53; border-radius: 5px; }
.login-container .login-card .el-button.login-button { width: 100%; height: 48px; border: 0; border-radius: 10px; background: linear-gradient(135deg, #347ce4, #2163ce) !important; color: #fff !important; font-size: 15px; font-weight: 500; box-shadow: 0 6px 14px #2469ce24; transition: transform .2s, box-shadow .2s, filter .2s; }
.login-container .login-card .el-button.login-button:hover { transform: translateY(-1px); filter: brightness(1.08); box-shadow: 0 9px 20px #2469ce33; }
.login-container .login-card .el-button.login-button:active { transform: translateY(0); }
.login-container .login-card .el-button.login-button:focus-visible { outline: 3px solid #8cb5ed; outline-offset: 4px; }
.platform-login-tip { color: #61758a; font-size: 14px; line-height: 1.9; }
.platform-login-tip h3 { color: #243b53; font-weight: 500; }
.platform-login-error { color: #c84646; }
.login-footer { position: absolute; bottom: 28px; inset-inline: 24px; text-align: center; color: #8596a7; font-size: 11px; letter-spacing: .5px; }
.login-footer span { padding: 0 10px; }
@keyframes ambient-light { from { transform: translateX(-8%); } to { transform: translateX(8%); } }
@keyframes lens-float { 0%,100% { transform: translateY(0) rotate(-24deg) scaleY(.94); } 50% { transform: translateY(-12px) rotate(-20deg) scaleY(.94); } }
@keyframes plane-float { 0%,100% { transform: translateY(0) rotate(12deg); } 50% { transform: translateY(8px) rotate(15deg); } }
@keyframes lens-shine { 0%,100% { opacity: .45; } 50% { opacity: .9; } }
@keyframes enter { from { opacity: 0; transform: translateY(14px); } to { opacity: 1; transform: translateY(0); } }
@media (max-width: 1000px) { .login-container { gap: 32px; padding-inline: 32px; }.login-card { flex-basis: 360px; padding: 40px 28px; }.vision-copy h2 { font-size: 28px; } }
@media (max-width: 760px) {
  .login-container { flex-direction: column; padding: 108px 24px 70px; gap: 28px; }
  .login-brand { inset-inline-start: 24px; inset-block-start: 28px; font-size: 20px; gap: 8px; }
  .brand-symbol { width: 32px; height: 32px; font-size: 10px; }
  .login-locale-switcher { inset-block-start: 28px; inset-inline-end: 20px; }
  .login-locale-switcher select { max-width: 106px; font-size: 12px; padding: 8px; }
  .vision-story { flex: none; width: 100%; }
  .optical-scene { width: 240px; margin: -20px auto -5px; }
  .vision-copy { margin-top: 0; }.vision-copy h2 { font-size: 24px; margin-bottom: 8px; }.vision-copy p { font-size: 12px; }
  .login-card { flex: none; width: 400px; padding: 30px 28px; border-radius: 20px; }
  .login-header { margin-bottom: 24px; }.login-title { font-size: 24px; margin-top: 12px; }
  .login-footer { bottom: 22px; }
}
@media (prefers-reduced-motion: reduce) { .login-container::before,.vision-story,.login-card,.optical-lens,.plane-front,.lens-reflection { animation: none; }.login-card :deep(*) { transition: none !important; } }
</style>
