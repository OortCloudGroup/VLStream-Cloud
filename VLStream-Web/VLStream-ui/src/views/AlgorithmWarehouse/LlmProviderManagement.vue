<!-- SPDX-License-Identifier: MIT -->
<template>
  <div class="llm-provider-page tenant_Page">
    <div class="page-card">
      <div class="page-header">
        <div><h2>大模型管理</h2><p>内置 OortCloud 开箱即用，也可配置其他 OpenAI 兼容中转站。</p></div>
        <el-button type="primary" @click="openEditor()">新增外部大模型</el-button>
      </div>
      <el-alert class="authorization-alert" :title="authorizationTitle" :type="authorized ? 'success' : 'warning'" :closable="false" show-icon>
        <template #default>
          <span v-if="authorized">平台登录令牌过期后，已保存的模型授权仍可供后台复核使用。</span>
          <span v-else>点击页面顶部“登录 OortCloud”，系统会自动选择账户中第一个启用的 API 令牌完成授权。</span>
        </template>
      </el-alert>
      <el-table v-loading="loading" :data="providers" border>
        <el-table-column label="名称" min-width="170"><template #default="scope"><span>{{ scope.row.name }}</span><el-tag v-if="scope.row.systemProvider" class="provider-tag" type="primary" size="small">内置</el-tag></template></el-table-column>
        <el-table-column prop="baseUrl" label="接口地址" min-width="260" show-overflow-tooltip />
        <el-table-column prop="modelName" label="模型" min-width="160" />
        <el-table-column label="API Key" width="110"><template #default="scope"><el-tag :type="scope.row.apiKeyConfigured ? 'success' : 'danger'">{{ scope.row.apiKeyConfigured ? '已配置' : '未配置' }}</el-tag></template></el-table-column>
        <el-table-column prop="timeoutSeconds" label="超时（秒）" width="110" />
        <el-table-column label="状态" width="90"><template #default="scope"><el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="scope">
            <template v-if="scope.row.systemProvider">
              <el-button link type="primary" disabled>编辑</el-button>
              <el-button link type="success" :disabled="!scope.row.authorized" @click="openTest(scope.row)">测试</el-button>
              <el-button link type="danger" disabled>删除</el-button>
            </template>
            <template v-else>
              <el-button link type="primary" @click="openEditor(scope.row)">编辑</el-button>
              <el-button link type="success" :disabled="!scope.row.enabled || !scope.row.apiKeyConfigured" @click="openTest(scope.row)">测试</el-button>
              <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="editorVisible" :title="form.id ? '编辑外部大模型' : '新增外部大模型'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="接口地址" prop="baseUrl"><el-input v-model="form.baseUrl" placeholder="https://api.example.com/v1" /></el-form-item>
        <el-form-item label="模型名称" prop="modelName"><el-input v-model="form.modelName" /></el-form-item>
        <el-form-item label="API Key" :prop="form.id ? '' : 'apiKey'"><el-input v-model="form.apiKey" type="password" show-password :placeholder="form.id ? '留空表示保持原 Key' : '请输入 API Key'" /></el-form-item>
        <el-form-item label="超时（秒）"><el-input-number v-model="form.timeoutSeconds" :min="5" :max="600" /></el-form-item>
        <el-form-item label="是否启用"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="editorVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="testVisible" title="测试视觉模型" width="600px">
      <el-form label-width="90px">
        <el-form-item label="测试模型"><span>{{ testingProvider?.name }} / {{ testingProvider?.modelName }}</span></el-form-item>
        <el-form-item label="提示词"><el-input v-model="testForm.prompt" type="textarea" :rows="3" placeholder="可直接询问图片内容；留空则执行结构化连通性测试" /></el-form-item>
        <el-form-item label="测试图片" required><el-upload :auto-upload="false" :limit="1" accept="image/*" :on-change="onTestImageChange" :on-remove="onTestImageRemove"><el-button>选择图片</el-button></el-upload></el-form-item>
        <el-form-item v-if="testResult" label="测试结果"><el-descriptions :column="1" border class="test-result"><el-descriptions-item label="结论">{{ testResult.decision === 'CONNECTED' ? '连通成功' : testResult.decision }}</el-descriptions-item><el-descriptions-item v-if="testResult.confidence !== null && testResult.confidence !== undefined" label="置信度">{{ testResult.confidence }}</el-descriptions-item><el-descriptions-item label="模型回复">{{ testResult.reason }}</el-descriptions-item></el-descriptions></el-form-item>
      </el-form>
      <template #footer><el-button @click="testVisible = false">关闭</el-button><el-button type="primary" :loading="testing" @click="runTest">开始测试</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createLlmProvider, deleteLlmProvider, getLlmProviders, getOortCloudLlmAuthorization, testLlmProvider, updateLlmProvider } from '@/api/llmReview'

const providers = ref([])
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
const authorizationTitle = computed(() => authorized.value ? `OortCloud 已授权${authorization.value.platformUserName ? `：${authorization.value.platformUserName}` : ''}` : 'OortCloud 尚未授权')

const responseMessage = (response, fallback) => response?.msg || response?.message || fallback
const errorMessage = (error, fallback) => error?.response?.data?.msg || error?.data?.msg || error?.message || fallback

async function load() {
  loading.value = true
  try {
    const [providersResponse, authorizationResponse] = await Promise.all([getLlmProviders(), getOortCloudLlmAuthorization()])
    if (providersResponse.code !== 200) throw new Error(responseMessage(providersResponse, '加载大模型配置失败'))
    if (authorizationResponse.code !== 200) throw new Error(responseMessage(authorizationResponse, '加载 OortCloud 授权失败'))
    providers.value = providersResponse.data || []
    authorization.value = authorizationResponse.data || { authorized: false }
  } catch (error) {
    providers.value = []
    ElMessage.error(errorMessage(error, '加载大模型配置失败'))
  } finally { loading.value = false }
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
    ElMessage.success('保存成功')
    editorVisible.value = false
    await load()
  } catch (error) { ElMessage.error(errorMessage(error, '保存失败')) } finally { saving.value = false }
}

async function remove(row) {
  if (row.systemProvider) return
  try {
    await ElMessageBox.confirm(`确认删除大模型“${row.name}”吗？`, '提示', { type: 'warning' })
    const response = await deleteLlmProvider(row.id)
    if (response.code !== 200) throw new Error(responseMessage(response, '删除失败'))
    ElMessage.success('删除成功')
    await load()
  } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(errorMessage(error, '删除失败')) }
}

function openTest(row) {
  if (row.systemProvider && !row.authorized) return ElMessage.warning('请先点击页面顶部“登录 OortCloud”完成授权')
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
  if (!testForm.imageBase64) return ElMessage.warning('请先选择测试图片')
  testing.value = true
  try {
    const response = await testLlmProvider(testingProvider.value.id, testForm)
    if (response.code !== 200) throw new Error(responseMessage(response, '测试失败'))
    testResult.value = response.data
    ElMessage.success('模型调用成功')
  } catch (error) { ElMessage.error(errorMessage(error, '测试失败')) } finally { testing.value = false }
}

onMounted(load)
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
