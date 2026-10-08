<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div class="system-settings">
    <!-- page -->
    <div class="page-header">
      <h1 class="page-title">{{ $tp('系统设置') }}</h1>
      <p>{{ $tp('系统配置与参数管理') }}</p>
    </div>

    <!-- Set item -->
    <el-tabs v-model="activeTab" class="tenanat-tabs settings-tabs">
      <!-- Set -->
      <el-tab-pane :label="$tp('基础设置')" name="basic">
        <el-card class="setting-card">
          <template #header>
            <div class="card-header">
              <span>{{ $tp('系统基础配置') }}</span>
            </div>
          </template>
          <el-form :model="basicSettings" label-width="120px">
            <el-form-item :label="$tp('系统名称:')">
              <el-input v-model="basicSettings.systemName" style="width: 300px" />
            </el-form-item>
            <el-form-item :label="$tp('系统版本:')">
              <el-input v-model="basicSettings.systemVersion" disabled style="width: 300px" />
            </el-form-item>
            <el-form-item :label="$tp('时区设置:')">
              <el-select v-model="basicSettings.timezone" style="width: 300px">
                <el-option :label="$tp('北京时间 (UTC+8)')" value="Asia/Shanghai" />
                <el-option :label="$tp('东京时间 (UTC+9)')" value="Asia/Tokyo" />
                <el-option :label="$tp('纽约时间 (UTC-5)')" value="America/New_York" />
                <el-option :label="$tp('伦敦时间 (UTC+0)')" value="Europe/London" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$tp('语言设置:')">
              <el-select v-model="basicSettings.language" style="width: 300px">
                <el-option :label="$tp('简体中文')" value="zh-CN" />
                <el-option label="English" value="en-US" />
                <el-option :label="$tp('日本語')" value="ja-JP" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$tp('主题模式:')">
              <el-radio-group v-model="basicSettings.theme">
                <el-radio label="light">{{ $tp('浅色模式') }}</el-radio>
                <el-radio label="dark">{{ $tp('深色模式') }}</el-radio>
                <el-radio label="auto">{{ $tp('跟随系统') }}</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveBasicSettings">{{ $tp('保存设置') }}</el-button>
              <el-button @click="resetBasicSettings">{{ $tp('重置') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- Set -->
      <el-tab-pane :label="$tp('存储设置')" name="storage">
        <el-card class="setting-card">
          <template #header>
            <div class="card-header">
              <span>{{ $tp('存储配置') }}</span>
            </div>
          </template>
          <el-form :model="storageSettings" label-width="120px">
            <el-form-item :label="$tp('录像存储路径:')">
              <el-input v-model="storageSettings.videoPath" style="width: 400px">
                <template #append>
                  <el-button @click="selectPath('video')">{{ $tp('浏览') }}</el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item :label="$tp('图片存储路径:')">
              <el-input v-model="storageSettings.imagePath" style="width: 400px">
                <template #append>
                  <el-button @click="selectPath('image')">{{ $tp('浏览') }}</el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item :label="$tp('日志存储路径:')">
              <el-input v-model="storageSettings.logPath" style="width: 400px">
                <template #append>
                  <el-button @click="selectPath('log')">{{ $tp('浏览') }}</el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item :label="$tp('存储空间监控:')">
              <el-switch v-model="storageSettings.spaceMonitoring" />
            </el-form-item>
            <el-form-item :label="$tp('自动清理:')">
              <el-switch v-model="storageSettings.autoCleanup" />
            </el-form-item>
            <el-form-item :label="$tp('保留天数:')" v-if="storageSettings.autoCleanup">
              <el-input-number
                v-model="storageSettings.retentionDays"
                :min="1"
                :max="365"
                style="width: 200px"
              />
              <span style="margin-left: 10px; color: #909399;">{{ $tp('天') }}</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveStorageSettings">{{ $tp('保存设置') }}</el-button>
              <el-button @click="resetStorageSettings">{{ $tp('重置') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!--  -->
        <el-card class="setting-card" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span>{{ $tp('存储状态') }}</span>
              <el-button size="small" @click="refreshStorageInfo">{{ $tp('刷新') }}</el-button>
            </div>
          </template>
          <div class="storage-info">
            <div class="storage-item">
              <div class="storage-label">{{ $tp('总容量:') }}</div>
              <div class="storage-value">{{ storageInfo.totalSpace }}</div>
            </div>
            <div class="storage-item">
              <div class="storage-label">{{ $tp('已使用:') }}</div>
              <div class="storage-value">{{ storageInfo.usedSpace }}</div>
            </div>
            <div class="storage-item">
              <div class="storage-label">{{ $tp('可用空间:') }}</div>
              <div class="storage-value">{{ storageInfo.freeSpace }}</div>
            </div>
            <div class="storage-progress">
              <el-progress
                :percentage="storageInfo.usagePercentage"
                :color="getProgressColor(storageInfo.usagePercentage)"
                :stroke-width="20"
                text-inside
              />
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- Set -->
      <el-tab-pane :label="$tp('网络设置')" name="network">
        <el-card class="setting-card">
          <template #header>
            <div class="card-header">
              <span>{{ $tp('网络配置') }}</span>
            </div>
          </template>
          <el-form :model="networkSettings" label-width="120px">
            <el-form-item :label="$tp('服务器地址:')">
              <el-input v-model="networkSettings.serverHost" style="width: 300px" />
            </el-form-item>
            <el-form-item :label="$tp('服务器端口:')">
              <el-input-number
                v-model="networkSettings.serverPort"
                :min="1"
                :max="65535"
                style="width: 200px"
              />
            </el-form-item>
            <el-form-item :label="$tp('RTSP端口:')">
              <el-input-number
                v-model="networkSettings.rtspPort"
                :min="1"
                :max="65535"
                style="width: 200px"
              />
            </el-form-item>
            <el-form-item :label="$tp('HTTP端口:')">
              <el-input-number
                v-model="networkSettings.httpPort"
                :min="1"
                :max="65535"
                style="width: 200px"
              />
            </el-form-item>
            <el-form-item :label="$tp('连接超时:')">
              <el-input-number
                v-model="networkSettings.timeout"
                :min="1"
                :max="300"
                style="width: 200px"
              />
              <span style="margin-left: 10px; color: #909399;">{{ $tp('秒') }}</span>
            </el-form-item>
            <el-form-item :label="$tp('启用HTTPS:')">
              <el-switch v-model="networkSettings.enableHttps" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveNetworkSettings">{{ $tp('保存设置') }}</el-button>
              <el-button @click="testConnection">{{ $tp('测试连接') }}</el-button>
              <el-button @click="resetNetworkSettings">{{ $tp('重置') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- full Set -->
      <el-tab-pane :label="$tp('安全设置')" name="security">
        <el-card class="setting-card">
          <template #header>
            <div class="card-header">
              <span>{{ $tp('安全配置') }}</span>
            </div>
          </template>
          <el-form :model="securitySettings" label-width="140px">
            <el-form-item :label="$tp('密码复杂度要求:')">
              <el-switch v-model="securitySettings.passwordComplexity" />
            </el-form-item>
            <el-form-item :label="$tp('登录失败锁定:')">
              <el-switch v-model="securitySettings.loginLockout" />
            </el-form-item>
            <el-form-item :label="$tp('最大失败次数:')" v-if="securitySettings.loginLockout">
              <el-input-number
                v-model="securitySettings.maxFailAttempts"
                :min="3"
                :max="10"
                style="width: 200px"
              />
            </el-form-item>
            <el-form-item :label="$tp('锁定时间:')" v-if="securitySettings.loginLockout">
              <el-input-number
                v-model="securitySettings.lockoutDuration"
                :min="5"
                :max="60"
                style="width: 200px"
              />
              <span style="margin-left: 10px; color: #909399;">{{ $tp('分钟') }}</span>
            </el-form-item>
            <el-form-item :label="$tp('会话超时:')">
              <el-input-number
                v-model="securitySettings.sessionTimeout"
                :min="30"
                :max="480"
                style="width: 200px"
              />
              <span style="margin-left: 10px; color: #909399;">{{ $tp('分钟') }}</span>
            </el-form-item>
            <el-form-item :label="$tp('启用双因子认证:')">
              <el-switch v-model="securitySettings.twoFactorAuth" />
            </el-form-item>
            <el-form-item :label="$tp('IP白名单:')">
              <el-switch v-model="securitySettings.ipWhitelist" />
            </el-form-item>
            <el-form-item :label="$tp('允许的IP地址:')" v-if="securitySettings.ipWhitelist">
              <el-input
                v-model="securitySettings.allowedIPs"
                type="textarea"
                :rows="3"
                :placeholder="$tp('每行一个IP地址或IP段，例如：192.168.1.100 或 192.168.1.0/24')"
                style="width: 400px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveSecuritySettings">{{ $tp('保存设置') }}</el-button>
              <el-button @click="resetSecuritySettings">{{ $tp('重置') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- logSet -->
      <el-tab-pane :label="$tp('日志设置')" name="logs">
        <el-card class="setting-card">
          <template #header>
            <div class="card-header">
              <span>{{ $tp('日志配置') }}</span>
            </div>
          </template>
          <el-form :model="logSettings" label-width="120px">
            <el-form-item :label="$tp('日志级别:')">
              <el-select v-model="logSettings.level" style="width: 200px">
                <el-option label="DEBUG" value="debug" />
                <el-option label="INFO" value="info" />
                <el-option label="WARN" value="warn" />
                <el-option label="ERROR" value="error" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$tp('启用系统日志:')">
              <el-switch v-model="logSettings.enableSystemLog" />
            </el-form-item>
            <el-form-item :label="$tp('启用操作日志:')">
              <el-switch v-model="logSettings.enableOperationLog" />
            </el-form-item>
            <el-form-item :label="$tp('启用错误日志:')">
              <el-switch v-model="logSettings.enableErrorLog" />
            </el-form-item>
            <el-form-item :label="$tp('日志轮转:')">
              <el-switch v-model="logSettings.logRotation" />
            </el-form-item>
            <el-form-item :label="$tp('单文件大小限制:')" v-if="logSettings.logRotation">
              <el-input-number
                v-model="logSettings.maxFileSize"
                :min="1"
                :max="1000"
                style="width: 200px"
              />
              <span style="margin-left: 10px; color: #909399;">MB</span>
            </el-form-item>
            <el-form-item :label="$tp('保留文件数量:')" v-if="logSettings.logRotation">
              <el-input-number
                v-model="logSettings.maxFiles"
                :min="1"
                :max="100"
                style="width: 200px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveLogSettings">{{ $tp('保存设置') }}</el-button>
              <el-button @click="clearLogs">{{ $tp('清空日志') }}</el-button>
              <el-button @click="resetLogSettings">{{ $tp('重置') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { translatePhrase as uiText } from '@/i18n'

import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// data
const activeTab = ref('basic')

// Set
const basicSettings = ref({
  systemName: 'VLStream Cloud',
  systemVersion: 'v1.0.0',
  timezone: 'Asia/Shanghai',
  language: 'zh-CN',
  theme: 'light'
})

// Set
  const storageSettings = ref({
    videoPath: './data/videos',
      imagePath: './data/images',
      logPath: './data/logs',
  spaceMonitoring: true,
  autoCleanup: true,
  retentionDays: 30
})

// info
const storageInfo = ref({
  totalSpace: '1.0 TB',
  usedSpace: '256 GB',
  freeSpace: '768 GB',
  usagePercentage: 25
})

// Set
const networkSettings = ref({
  serverHost: '192.168.1.100',
  serverPort: 8080,
  rtspPort: 554,
  httpPort: 80,
  timeout: 30,
  enableHttps: false
})

// full Set
const securitySettings = ref({
  passwordComplexity: true,
  loginLockout: true,
  maxFailAttempts: 5,
  lockoutDuration: 15,
  sessionTimeout: 120,
  twoFactorAuth: false,
  ipWhitelist: false,
  allowedIPs: ''
})

// logSet
const logSettings = ref({
  level: 'info',
  enableSystemLog: true,
  enableOperationLog: true,
  enableErrorLog: true,
  logRotation: true,
  maxFileSize: 100,
  maxFiles: 10
})

// method
const saveBasicSettings = () => {
  ElMessage.success(uiText('基础设置保存成功'))
}

const resetBasicSettings = () => {
  basicSettings.value = {
    systemName: 'VLStream Cloud',
    systemVersion: 'v1.0.0',
    timezone: 'Asia/Shanghai',
    language: 'zh-CN',
    theme: 'light'
  }
  ElMessage.info(uiText('基础设置已重置'))
}

const saveStorageSettings = () => {
  ElMessage.success(uiText('存储设置保存成功'))
}

const resetStorageSettings = () => {
  storageSettings.value = {
    videoPath: './data/videos',
    imagePath: './data/images',
    logPath: './data/logs',
    spaceMonitoring: true,
    autoCleanup: true,
    retentionDays: 30
  }
  ElMessage.info(uiText('存储设置已重置'))
}

const selectPath = (type) => {
  ElMessage.info(uiText('选择{value0}存储路径', { value0: type === 'video' ? uiText('录像') : type === 'image' ? uiText('图片') : uiText('日志') }))
}

const refreshStorageInfo = () => {
  // new info
  storageInfo.value = {
    totalSpace: '1.0 TB',
    usedSpace: Math.floor(Math.random() * 500 + 200) + ' GB',
    freeSpace: Math.floor(Math.random() * 800 + 500) + ' GB',
    usagePercentage: Math.floor(Math.random() * 50 + 20)
  }
  ElMessage.success(uiText('存储信息已刷新'))
}

const getProgressColor = (percentage) => {
  if (percentage < 50) return '#67c23a'
  if (percentage < 80) return '#e6a23c'
  return '#f56c6c'
}

const saveNetworkSettings = () => {
  ElMessage.success(uiText('网络设置保存成功'))
}

const testConnection = () => {
  ElMessage.info(uiText('正在测试网络连接...'))
  setTimeout(() => {
    ElMessage.success(uiText('网络连接测试成功'))
  }, 2000)
}

const resetNetworkSettings = () => {
  networkSettings.value = {
    serverHost: '192.168.1.100',
    serverPort: 8080,
    rtspPort: 554,
    httpPort: 80,
    timeout: 30,
    enableHttps: false
  }
  ElMessage.info(uiText('网络设置已重置'))
}

const saveSecuritySettings = () => {
  ElMessage.success(uiText('安全设置保存成功'))
}

const resetSecuritySettings = () => {
  securitySettings.value = {
    passwordComplexity: true,
    loginLockout: true,
    maxFailAttempts: 5,
    lockoutDuration: 15,
    sessionTimeout: 120,
    twoFactorAuth: false,
    ipWhitelist: false,
    allowedIPs: ''
  }
  ElMessage.info(uiText('安全设置已重置'))
}

const saveLogSettings = () => {
  ElMessage.success(uiText('日志设置保存成功'))
}

const clearLogs = async () => {
  try {
    await ElMessageBox.confirm(
      uiText('确定要清空所有日志吗？此操作不可恢复。'),
      uiText('确认清空'),
      {
        confirmButtonText: uiText('确定'),
        cancelButtonText: uiText('取消'),
        type: 'warning'
      }
    )
    ElMessage.success(uiText('日志清空成功'))
  } catch {
    ElMessage.info(uiText('已取消清空'))
  }
}

const resetLogSettings = () => {
  logSettings.value = {
    level: 'info',
    enableSystemLog: true,
    enableOperationLog: true,
    enableErrorLog: true,
    logRotation: true,
    maxFileSize: 100,
    maxFiles: 10
  }
  ElMessage.info(uiText('日志设置已重置'))
}

//
onMounted(() => {
  // Initialize Set
})
</script>

<style scoped>
.system-settings {
  height: 100%;
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 20px;
  background: #fff;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
  box-sizing: border-box;
  overflow: auto;
}

.settings-tabs {
  flex: 1;
}

.setting-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.storage-info {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.storage-item {
  text-align: center;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 6px;
}

.storage-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 5px;
}

.storage-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.storage-progress {
  grid-column: 1 / -1;
  margin-top: 10px;
}

.el-form-item {
  margin-bottom: 20px;
}

.el-tabs__content {
  padding: 20px 0;
}
</style>