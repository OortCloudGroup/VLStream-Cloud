<template>
  <div class="firmware-management tenant_Page">
    <div class="tenant_content">
      <div class="tableTenBox">
        <div class="table-content">
          <div class="toolbar">
            <el-button type="primary" :icon="Plus" @click="openUploadDialog">上传固件</el-button>
            <el-form :inline="true" :model="query" class="search-form" @submit.prevent>
              <el-form-item label="摄像头型号">
                <el-input v-model.trim="query.cameraModel" clearable placeholder="输入型号编码" @keyup.enter="search" />
              </el-form-item>
              <el-form-item label="固件版本">
                <el-input v-model.trim="query.firmwareVersion" clearable placeholder="例如 1.0.1.14" @keyup.enter="search" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="search">搜索</el-button>
                <el-button @click="resetSearch">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table v-loading="loading" :data="records" stripe header-cell-class-name="header_tenant_cell">
            <el-table-column label="序号" width="70">
              <template #default="scope">
                {{ scope.$index + (pagination.current - 1) * pagination.size + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="cameraModel" label="摄像头型号" min-width="150" show-overflow-tooltip />
            <el-table-column prop="firmwareVersion" label="固件版本号" min-width="120" />
            <el-table-column prop="originalFileName" label="固件包" min-width="220" show-overflow-tooltip />
            <el-table-column label="文件大小" width="120">
              <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column label="SHA-256" min-width="210" show-overflow-tooltip>
              <template #default="{ row }">{{ row.sha256 || '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.uploadStatus === 'READY' ? 'success' : 'warning'">
                  {{ row.uploadStatus === 'READY' ? '可用' : '待完成' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="上传时间" width="180">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right" align="right">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.uploadStatus !== 'READY'" @click="download(row)">下载</el-button>
                <el-button link type="danger" @click="remove(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-box">
            <el-pagination
              v-model:current-page="pagination.current"
              v-model:page-size="pagination.size"
              background
              :page-sizes="[10, 20, 50, 100]"
              :total="pagination.total"
              layout="total, prev, pager, next, sizes"
              @size-change="loadData"
              @current-change="loadData"
            />
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="上传 VLS 设备固件" width="520px" destroy-on-close>
      <el-alert
        title="固件包将通过短期签名地址直接上传到 MinIO，平台不会向浏览器暴露存储凭据。"
        type="info"
        :closable="false"
        show-icon
        class="upload-alert"
      />
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="摄像头型号" prop="cameraModel">
          <el-input v-model.trim="form.cameraModel" maxlength="128" placeholder="例如 IPC-A100" />
          <div class="form-tip">设备上报时必须使用完全相同的型号编码。</div>
        </el-form-item>
        <el-form-item label="固件版本号" prop="firmwareVersion">
          <el-input v-model.trim="form.firmwareVersion" placeholder="例如 1.0.1.14" />
        </el-form-item>
        <el-form-item label="升级目标">
          <el-input model-value="RootFS" disabled />
          <div class="form-tip">VLS 固件固定升级 RootFS，并强制开启回滚及升级后重启。</div>
        </el-form-item>
        <el-form-item label="固件包" prop="file">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".ota"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-button type="primary" plain>选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">仅支持包含 manifest 的 .ota 固件包，最大 160 MiB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item v-if="uploading" label="上传进度">
          <el-progress :percentage="uploadProgress" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="uploading" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">上传并保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  completeFirmwareUpload,
  getDeviceFirmwarePage,
  getFirmwareDownloadUrl,
  issueFirmwareUpload,
  removeDeviceFirmware,
  uploadFirmwareToMinio
} from '@/api/deviceFirmware'

const MAX_PACKAGE_BYTES = 160 * 1024 * 1024
const ALLOWED_SUFFIXES = ['.ota']

const loading = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const dialogVisible = ref(false)
const formRef = ref()
const uploadRef = ref()
const records = ref([])
const query = reactive({ cameraModel: '', firmwareVersion: '' })
const pagination = reactive({ current: 1, size: 10, total: 0 })
const form = reactive({ cameraModel: '', firmwareVersion: '', file: null })

const rules = {
  cameraModel: [
    { required: true, message: '请输入摄像头型号', trigger: 'blur' },
    { validator: (_rule, value, callback) => value?.length <= 128 && !/[\\/]/.test(value) ? callback() : callback(new Error('型号不能超过 128 个字符或包含路径分隔符')), trigger: 'blur' }
  ],
  firmwareVersion: [
    { required: true, message: '请输入固件版本号', trigger: 'blur' },
    { pattern: /^(0|[1-9]\d*)(\.(0|[1-9]\d*)){2,}$/, message: '请输入至少三段的纯数字点分版本号', trigger: 'blur' }
  ],
  file: [{ required: true, message: '请选择固件包', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    const response = await getDeviceFirmwarePage({
      current: pagination.current,
      size: pagination.size,
      cameraModel: query.cameraModel || undefined,
      firmwareVersion: query.firmwareVersion || undefined
    })
    const page = response?.data || {}
    records.value = page.records || []
    pagination.total = Number(page.total || 0)
  } catch (error) {
    console.error('加载固件列表失败:', error)
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.current = 1
  loadData()
}

function resetSearch() {
  query.cameraModel = ''
  query.firmwareVersion = ''
  pagination.current = 1
  loadData()
}

function openUploadDialog() {
  form.cameraModel = ''
  form.firmwareVersion = ''
  form.file = null
  uploadProgress.value = 0
  dialogVisible.value = true
}

function handleFileChange(uploadFile) {
  const file = uploadFile.raw
  if (!file) return
  const lowerName = file.name.toLowerCase()
  if (!ALLOWED_SUFFIXES.some(suffix => lowerName.endsWith(suffix))) {
    ElMessage.error('仅支持包含 manifest 的 .ota 固件包')
    uploadRef.value?.clearFiles()
    form.file = null
    return
  }
  if (file.size <= 0 || file.size > MAX_PACKAGE_BYTES) {
    ElMessage.error('固件包必须大于 0 且不能超过 160 MiB')
    uploadRef.value?.clearFiles()
    form.file = null
    return
  }
  form.file = file
  formRef.value?.validateField('file')
}

function handleFileRemove() {
  form.file = null
}

async function submitUpload() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid || !form.file) return

  uploading.value = true
  uploadProgress.value = 0
  let firmwareId
  try {
    const contentType = form.file.type || 'application/octet-stream'
    const grantResponse = await issueFirmwareUpload({
      cameraModel: form.cameraModel,
      firmwareVersion: form.firmwareVersion,
      fileName: form.file.name,
      contentType,
      fileSize: form.file.size
    })
    const grant = grantResponse?.data
    firmwareId = grant?.firmwareId
    if (!firmwareId || !grant?.uploadUrl) throw new Error('平台未返回有效上传地址')

    await uploadFirmwareToMinio(
      grant.uploadUrl,
      form.file,
      grant.requiredContentType || contentType,
      percentage => { uploadProgress.value = percentage }
    )
    await completeFirmwareUpload(firmwareId)
    uploadProgress.value = 100
    ElMessage.success('固件上传并校验成功')
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    console.error('固件上传失败:', error)
    if (firmwareId) {
      await removeDeviceFirmware(firmwareId).catch(cleanupError => {
        console.warn('清理未完成固件记录失败:', cleanupError)
      })
    }
    ElMessage.error(error?.response?.data?.msg || error?.message || '固件上传失败')
  } finally {
    uploading.value = false
  }
}

async function download(row) {
  try {
    const response = await getFirmwareDownloadUrl(row.id)
    const url = response?.data?.url
    if (!url) throw new Error('平台未返回下载地址')
    window.open(url, '_blank', 'noopener,noreferrer')
  } catch (error) {
    console.error('获取固件下载地址失败:', error)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除 ${row.cameraModel} / RootFS / ${row.firmwareVersion} 及其 MinIO 固件包吗？`,
      '删除固件',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await removeDeviceFirmware(row.id)
    ElMessage.success('固件已删除')
    await loadData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('删除固件失败:', error)
    }
  }
}

function formatFileSize(bytes) {
  const size = Number(bytes || 0)
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.tenant_Page {
  width: 100%;
  height: 100%;
  background: #f0f2f5;
  display: flex;
  flex-direction: column;
}

.tenant_content {
  flex: 1;
  min-height: 0;
  display: flex;
}

.tableTenBox {
  width: 100%;
  padding: 20px;
  background: #fff;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
  overflow: hidden;
}

.table-content {
  height: 100%;
  overflow: auto;
}

.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}

.pagination-box {
  height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-alert {
  margin-bottom: 20px;
}

.form-tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 20px;
}

:deep(.header_tenant_cell) {
  background: #f8f8f9;
}

@media (max-width: 1100px) {
  .toolbar {
    flex-direction: column;
  }

  .search-form {
    justify-content: flex-start;
  }
}
</style>
