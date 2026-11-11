<!-- SPDX-License-Identifier: MIT -->
<template>
  <div class="llm-provider-page tenant_Page">
    <div class="page-card">
      <div class="page-header">
        <div><h2>{{ $tp('大模型管理') }}</h2><p>{{ $tp('内置 OortCloud 开箱即用，也可配置其他 OpenAI 兼容中转站。') }}</p></div>
        <el-button type="primary" @click="openEditor()">{{ $tp('新增外部大模型') }}</el-button>
      </div>
      <el-alert class="authorization-alert" :title="authorizationTitle" :type="authorized ? 'success' : 'warning'" :closable="false" show-icon>
        <template #default>
          <span v-if="authorized">{{ $tp('平台登录令牌过期后，已保存的模型授权仍可供后台复核使用。') }}</span>
          <span v-else-if="authorization.revoked">{{ $tp('已取消授权，重新登录不会自动恢复；可在下方重新授权。') }}</span>
          <span v-else>{{ $tp('登录 OortCloud 后默认授权，也可在下方点击授权。') }}</span>
        </template>
      </el-alert>
      <el-table v-loading="loading" :data="providers" border>
        <el-table-column :label="$tp('名称')" min-width="170"><template #default="scope"><span>{{ scope.row.name }}</span><el-tag v-if="scope.row.systemProvider" class="provider-tag" type="primary" size="small">{{ $tp('内置') }}</el-tag></template></el-table-column>
        <el-table-column prop="baseUrl" :label="$tp('接口地址')" min-width="260" show-overflow-tooltip />
        <el-table-column :label="$tp('模型')" min-width="220">
          <template #default="{ row }">
            <el-select v-if="row.systemProvider" :model-value="row.modelName" filterable :disabled="!row.authorized || modelSaving" :loading="modelsLoading" :aria-label="$tp('选择模型')" @visible-change="visible => visible && loadModels(row)" @change="value => changeSystemModel(row, value)">
              <el-option v-for="name in modelOptions(row)" :key="name" :label="name" :value="name" />
            </el-select>
            <span v-else>{{ row.modelName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="API Key" width="110"><template #default="scope"><el-tag :type="scope.row.apiKeyConfigured ? 'success' : 'danger'">{{ scope.row.apiKeyConfigured ? $tp('已配置') : $tp('未配置') }}</el-tag></template></el-table-column>
        <el-table-column prop="timeoutSeconds" :label="$tp('超时（秒）')" width="110" />
        <el-table-column :label="$tp('状态')" width="90"><template #default="scope"><el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? $tp('启用') : $tp('停用') }}</el-tag></template></el-table-column>
        <el-table-column :label="$tp('操作')" width="240" fixed="right">
          <template #default="scope">
            <template v-if="scope.row.systemProvider">
              <el-button link type="primary" disabled>{{ $tp('编辑') }}</el-button>
              <el-button link type="success" :disabled="!scope.row.authorized" @click="openTest(scope.row)">{{ $tp('测试') }}</el-button>
              <el-button link type="danger" disabled>{{ $tp('删除') }}</el-button>
              <el-button link :type="scope.row.authorized ? 'warning' : 'primary'" :loading="authorizationSaving" @click="changeAuthorization(scope.row)">{{ scope.row.authorized ? $tp('取消授权') : $tp('授权') }}</el-button>
            </template>
            <template v-else>
              <el-button link type="primary" @click="openEditor(scope.row)">{{ $tp('编辑') }}</el-button>
              <el-button link type="success" :disabled="!scope.row.enabled || !scope.row.apiKeyConfigured" @click="openTest(scope.row)">{{ $tp('测试') }}</el-button>
              <el-button link type="danger" @click="remove(scope.row)">{{ $tp('删除') }}</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="editorVisible" :title="form.id ? $tp('编辑外部大模型') : $tp('新增外部大模型')" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item :label="$tp('名称')" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item :label="$tp('接口地址')" prop="baseUrl"><el-input v-model="form.baseUrl" placeholder="https://api.example.com/v1" /></el-form-item>
        <el-form-item :label="$tp('模型名称')" prop="modelName"><el-select v-model="form.modelName" filterable allow-create default-first-option :loading="modelsLoading" @visible-change="visible => visible && form.id && loadModels(form)"><el-option v-for="name in modelOptions(form)" :key="name" :label="name" :value="name" /></el-select></el-form-item>
        <el-form-item label="API Key" :prop="form.id ? '' : 'apiKey'"><el-input v-model="form.apiKey" type="password" show-password :placeholder="form.id ? $tp('留空表示保持原 Key') : $tp('请输入 API Key')" /></el-form-item>
        <el-form-item :label="$tp('超时（秒）')"><el-input-number v-model="form.timeoutSeconds" :min="5" :max="600" /></el-form-item>
        <el-form-item :label="$tp('是否启用')"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="editorVisible = false">{{ $tp('取消') }}</el-button><el-button type="primary" :loading="saving" @click="save">{{ $tp('保存') }}</el-button></template>
    </el-dialog>

    <el-dialog v-model="testVisible" :title="$tp('测试视觉模型')" width="600px">
      <el-form label-width="90px">
        <el-form-item :label="$tp('测试模型')"><span>{{ testingProvider?.name }} / {{ testingProvider?.modelName }}</span></el-form-item>
        <el-form-item :label="$tp('提示词')"><el-input v-model="testForm.prompt" type="textarea" :rows="3" :placeholder="$tp('可直接询问图片内容；留空则执行结构化连通性测试')" /></el-form-item>
        <el-form-item :label="$tp('测试图片')" required><el-upload :auto-upload="false" :limit="1" accept="image/*" :on-change="onTestImageChange" :on-remove="onTestImageRemove"><el-button>{{ $tp('选择图片') }}</el-button></el-upload></el-form-item>
        <el-form-item v-if="testResult" :label="$tp('测试结果')"><el-descriptions :column="1" border class="test-result"><el-descriptions-item :label="$tp('结论')">{{ testResult.decision === 'CONNECTED' ? $tp('连通成功') : testResult.decision }}</el-descriptions-item><el-descriptions-item v-if="testResult.confidence !== null && testResult.confidence !== undefined" :label="$tp('置信度')">{{ testResult.confidence }}</el-descriptions-item><el-descriptions-item :label="$tp('模型回复')">{{ testResult.reason }}</el-descriptions-item></el-descriptions></el-form-item>
      </el-form>
      <template #footer><el-button @click="testVisible = false">{{ $tp('关闭') }}</el-button><el-button type="primary" :loading="testing" @click="runTest">{{ $tp('开始测试') }}</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { translatePhrase as uiText } from '@/i18n'

import { computed, onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createLlmProvider, deleteLlmProvider, getLlmProviders, getOortCloudLlmAuthorization, testLlmProvider, updateLlmProvider, getLlmProviderModels, revokeOortCloudLlm, selectOortCloudLlmModel } from '@/api/llmReview'

import { syncLlmAuthorization } from '@/utils/llmAuthorization'
import { authManager } from '@/utils/auth'

const providers = ref([])
const authorizationSaving = ref(false)
const modelSaving = ref(false)
const modelsLoading = ref(false)
const modelCatalogues = ref({})
let active = true
let loadVersion = 0
const modelOptions = row => [...new Set([row.modelName, ...(modelCatalogues.value[row.id] || [])].filter(Boolean))]

async function loadModels(row) {
  if (modelsLoading.value) return
  const identity = authManager.getIdentitySnapshot()
  modelsLoading.value = true
  try {
    const response = await getLlmProviderModels(row.id)
    if (!active || !authManager.isIdentityCurrent(identity)) return
    if (Number(response.code) !== 200) throw new Error('Model catalogue unavailable')
    modelCatalogues.value[row.id] = response.data || []
  } catch {
    if (active && authManager.isIdentityCurrent(identity)) ElMessage.warning(uiText('模型列表加载失败，已保留当前模型，请稍后重试'))
  } finally { modelsLoading.value = false }
}

async function changeSystemModel(row, modelName) {
  const identity = authManager.getIdentitySnapshot()
  modelSaving.value = true
  try {
    const response = await selectOortCloudLlmModel(modelName)
    if (!active || !authManager.isIdentityCurrent(identity)) return
    if (Number(response.code) !== 200) throw new Error('Model save failed')
    await load()
    ElMessage.success(uiText('保存成功'))
  } catch { if (active && authManager.isIdentityCurrent(identity)) ElMessage.error(uiText('保存失败')) } finally { modelSaving.value = false }
}

async function changeAuthorization(row) {
  if (authorizationSaving.value) return
  const identity = authManager.getIdentitySnapshot()
  const current = () => active && authManager.isIdentityCurrent(identity)
  authorizationSaving.value = true
  try {
    if (row.authorized) {
      const response = await revokeOortCloudLlm()
      if (!current()) return
      if (Number(response.code) !== 200) throw new Error('Revoke failed')
      modelCatalogues.value = {}
    } else {
      const mode = await authManager.getTenantMode()
      if (!current()) return
      const state = await syncLlmAuthorization(mode, { automatic: false, isCurrent: current })
      if (!current()) return
      if (!state?.authorized) throw new Error('Authorization unavailable')
    }
    await load()
  } catch { if (current()) ElMessage.error(uiText('授权状态更新失败，请确认已登录 OortCloud 后重试')) }
  finally { authorizationSaving.value = false }
}

const authorization = ref({ authorized: false, platformUserName: '', authorizedAt: null })
const loading = ref(false)
const saving = ref(false)
const editorVisible = ref(false)
const formRef = ref(null)
const emptyForm = () => ({ id: null, name: '', baseUrl: '', modelName: '', apiKey: '', timeoutSeconds: 120, enabled: true })
const form = reactive(emptyForm())
const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  baseUrl: [{ required: true, message: '请输入接口地址', trigger: 'blur' }],
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }]
}
const testVisible = ref(false)
const testing = ref(false)
const testingProvider = ref(null)
const testResult = ref(null)
const testForm = reactive({ prompt: '', imageBase64: '' })
const authorized = computed(() => Boolean(authorization.value.authorized))
const authorizationTitle = computed(() => authorized.value ? uiText('OortCloud 已授权{value0}', { value0: authorization.value.platformUserName ? `：${authorization.value.platformUserName}` : '' }) : uiText('OortCloud 尚未授权'))

const responseMessage = (response, fallback) => response?.msg || response?.message || fallback
const errorMessage = (error, fallback) => error?.response?.data?.msg || error?.data?.msg || error?.message || fallback

async function load() {
  const version = ++loadVersion
  const identity = authManager.getIdentitySnapshot()
  loading.value = true
  try {
    const [providersResponse, authorizationResponse] = await Promise.all([getLlmProviders(), getOortCloudLlmAuthorization()])
    if (!active || version !== loadVersion || !authManager.isIdentityCurrent(identity)) return
    if (providersResponse.code !== 200) throw new Error(responseMessage(providersResponse, '加载大模型配置失败'))
    if (authorizationResponse.code !== 200) throw new Error(responseMessage(authorizationResponse, '加载 OortCloud 授权失败'))
    providers.value = providersResponse.data || []
    authorization.value = authorizationResponse.data || { authorized: false }
  } catch (error) {
    if (!active || version !== loadVersion || !authManager.isIdentityCurrent(identity)) return
    providers.value = []
    ElMessage.error(errorMessage(error, uiText('加载大模型配置失败')))
  } finally { if (version === loadVersion) loading.value = false }
}

function openEditor(row) {
  if (row?.systemProvider) return
  Object.assign(form, emptyForm(), row || {}, { apiKey: '' })
  editorVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const data = { ...form }
    if (!data.apiKey) delete data.apiKey
    const response = data.id ? await updateLlmProvider(data.id, data) : await createLlmProvider(data)
    if (response.code !== 200) throw new Error(responseMessage(response, '保存失败'))
    ElMessage.success(uiText('保存成功'))
    editorVisible.value = false
    await load()
  } catch (error) { ElMessage.error(errorMessage(error, uiText('保存失败'))) } finally { saving.value = false }
}

async function remove(row) {
  if (row.systemProvider) return
  try {
    await ElMessageBox.confirm(uiText('确认删除大模型“{value0}”吗？', { value0: row.name }), uiText('提示'), { type: 'warning' })
    const response = await deleteLlmProvider(row.id)
    if (response.code !== 200) throw new Error(responseMessage(response, '删除失败'))
    ElMessage.success(uiText('删除成功'))
    await load()
  } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(errorMessage(error, uiText('删除失败'))) }
}

function openTest(row) {
  if (row.systemProvider && !row.authorized) return ElMessage.warning(uiText('请在大模型管理中授权 OortCloud 后再使用'))
  testingProvider.value = row
  Object.assign(testForm, { prompt: '', imageBase64: '' })
  testResult.value = null
  testVisible.value = true
}

function onTestImageChange(file) {
  const reader = new FileReader()
  reader.onload = () => { testForm.imageBase64 = reader.result }
  reader.readAsDataURL(file.raw)
}
const onTestImageRemove = () => { testForm.imageBase64 = '' }

async function runTest() {
  if (!testForm.imageBase64) return ElMessage.warning(uiText('请先选择测试图片'))
  testing.value = true
  try {
    const response = await testLlmProvider(testingProvider.value.id, testForm)
    if (response.code !== 200) throw new Error(responseMessage(response, '测试失败'))
    testResult.value = response.data
    ElMessage.success(uiText('模型调用成功'))
  } catch (error) { ElMessage.error(errorMessage(error, uiText('测试失败'))) } finally { testing.value = false }
}

onMounted(() => {
  window.addEventListener('llmAuthorizationChanged', load)
  load()
})
onBeforeUnmount(() => {
  active = false
  window.removeEventListener('llmAuthorizationChanged', load)
})
</script>

<style scoped>
.llm-provider-page { padding: 20px; }
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 20px; }
.page-header h2 { margin: 0 0 8px; font-size: 20px; }
.page-header p { margin: 0; color: #8c8c8c; }
.authorization-alert { margin-bottom: 20px; }
.provider-tag { margin-left: 8px; }
.test-result { width: 100%; }
</style>
