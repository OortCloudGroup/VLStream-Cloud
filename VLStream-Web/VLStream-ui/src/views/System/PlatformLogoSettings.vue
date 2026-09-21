<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <div class="platform-logo-page">
    <section class="settings-panel">
      <header class="section-heading">
        <h1>{{ tp('平台标识') }}</h1>
        <p>{{ tp('自定义修改平台左上角 LOGO 标识') }}</p>
      </header>

      <div v-loading="loading" class="logo-grid">
        <button class="create-card" type="button" @click="openCreate">
          <el-icon :size="48"><Plus /></el-icon>
          <span>{{ tp('新建配置') }}</span>
        </button>

        <article
          v-for="item in logos"
          :key="item.systemDefault ? 'default' : item.id"
          class="logo-card"
          :class="{ active: item.active }"
        >
          <div class="card-accent" />
          <div class="card-top">
            <div class="brand-preview">
              <template v-if="item.systemDefault">
                <span class="default-mark">VLS</span>
                <strong>VLStream Cloud</strong>
              </template>
              <img v-else :src="item.logoUrl" :alt="item.description || tp('平台标识')">
            </div>
            <el-tag v-if="item.systemDefault" type="success" effect="dark" size="small">{{ tp('默认') }}</el-tag>
            <el-dropdown v-else trigger="click" @command="command => handleCommand(command, item)">
              <button class="more-button" type="button" :aria-label="tp('更多操作')">
                <el-icon><MoreFilled /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">
                    <el-icon><EditPen /></el-icon>{{ tp('编辑') }}
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided>
                    <span class="delete-action"><el-icon><Delete /></el-icon>{{ tp('删除') }}</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <p class="description" :title="item.description">{{ item.description || tp('暂无描述') }}</p>
          <div class="metadata">
            <span><el-icon><User /></el-icon>{{ tp('创建人') }}：{{ item.createBy || 'system' }}</span>
            <span><el-icon><Clock /></el-icon>{{ tp('创建时间') }}：{{ formatDate(item.createTime) }}</span>
            <span><el-icon><Timer /></el-icon>{{ tp('更新时间') }}：{{ formatDate(item.updateTime) }}</span>
          </div>

          <el-button v-if="item.active" class="state-button" type="primary" round disabled>
            <el-icon><CircleCheck /></el-icon>{{ tp('当前生效') }}
          </el-button>
          <el-button v-else class="state-button" round @click="setActive(item)">
            <el-icon><SetUp /></el-icon>{{ tp('设为当前生效') }}
          </el-button>
        </article>
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId == null ? tp('新建配置') : tp('编辑配置')"
      width="min(680px, calc(100vw - 32px))"
      destroy-on-close
      @closed="resetDialog"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="84px">
        <el-form-item label="LOGO" prop="file">
          <el-upload
            ref="uploadRef"
            class="logo-uploader"
            :auto-upload="false"
            :show-file-list="false"
            accept=".png,.jpg,.jpeg"
            :on-change="handleFileChange"
          >
            <div class="upload-box">
              <img v-if="previewUrl" :src="previewUrl" :alt="tp('LOGO 预览')">
              <template v-else>
                <el-icon :size="34"><Plus /></el-icon>
                <span>{{ tp('上传 LOGO') }}</span>
              </template>
            </div>
          </el-upload>
          <div class="upload-help">{{ tp('支持 PNG、JPG、JPEG，最大 2 MiB，建议使用透明背景横版图片。') }}</div>
        </el-form-item>
        <el-form-item :label="tp('描述')" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            :placeholder="tp('请输入描述')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ tp('取消') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ tp('确定') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Clock, Delete, EditPen, MoreFilled, Plus, SetUp, Timer, User } from '@element-plus/icons-vue'
import {
  activatePlatformLogo,
  createPlatformLogo,
  getPlatformLogos,
  removePlatformLogo,
  updatePlatformLogo
} from '@/api/platformLogo'
import { translatePhrase } from '@/i18n'

const MAX_FILE_BYTES = 2 * 1024 * 1024
const ALLOWED_TYPES = ['image/png', 'image/jpeg']

const tp = source => translatePhrase(source)
const loading = ref(false)
const saving = ref(false)
const logos = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)
const existingLogoUrl = ref('')
const previewUrl = ref('')
const previewObjectUrl = ref('')
const formRef = ref()
const uploadRef = ref()
const form = reactive({ file: null, description: '' })

const rules = {
  description: [{ max: 500, message: tp('描述不能超过500个字符'), trigger: 'blur' }]
}

async function loadLogos() {
  loading.value = true
  try {
    const response = await getPlatformLogos()
    logos.value = response?.data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.file = null
  form.description = ''
  existingLogoUrl.value = ''
  previewUrl.value = ''
  dialogVisible.value = true
}

function openEdit(item) {
  editingId.value = item.id
  form.file = null
  form.description = item.description || ''
  existingLogoUrl.value = item.logoUrl || ''
  previewUrl.value = existingLogoUrl.value
  dialogVisible.value = true
}

function revokePreview() {
  if (previewObjectUrl.value) URL.revokeObjectURL(previewObjectUrl.value)
  previewObjectUrl.value = ''
}

function handleFileChange(uploadFile) {
  const file = uploadFile.raw
  if (!file) return
  if (!ALLOWED_TYPES.includes(file.type) || !/\.(png|jpe?g)$/i.test(file.name)) {
    ElMessage.error(tp('LOGO 仅支持 PNG、JPG、JPEG 格式'))
    uploadRef.value?.clearFiles()
    return
  }
  if (file.size <= 0 || file.size > MAX_FILE_BYTES) {
    ElMessage.error(tp('LOGO 图片不能超过 2 MiB'))
    uploadRef.value?.clearFiles()
    return
  }
  revokePreview()
  form.file = file
  previewObjectUrl.value = URL.createObjectURL(file)
  previewUrl.value = previewObjectUrl.value
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (editingId.value == null && !form.file) {
    ElMessage.warning(tp('请上传 LOGO 图片'))
    return
  }
  saving.value = true
  try {
    if (editingId.value == null) {
      await createPlatformLogo(form.file, form.description)
    } else {
      await updatePlatformLogo(editingId.value, form.file, form.description)
    }
    ElMessage.success(tp('保存成功'))
    dialogVisible.value = false
    await loadLogos()
    notifyBrandChanged()
  } finally {
    saving.value = false
  }
}

async function setActive(item) {
  await activatePlatformLogo(item.systemDefault ? null : item.id)
  ElMessage.success(tp('平台标识已生效'))
  await loadLogos()
  notifyBrandChanged()
}

async function handleCommand(command, item) {
  if (command === 'edit') {
    openEdit(item)
    return
  }
  if (command === 'delete') {
    await ElMessageBox.confirm(
      tp('删除后无法恢复；如果它正在生效，平台将自动恢复默认标识。'),
      tp('删除平台标识'),
      { type: 'warning', confirmButtonText: tp('删除'), cancelButtonText: tp('取消') }
    )
    await removePlatformLogo(item.id)
    ElMessage.success(tp('平台标识删除成功'))
    await loadLogos()
    notifyBrandChanged()
  }
}

function notifyBrandChanged() {
  window.dispatchEvent(new CustomEvent('platform-logo-changed'))
}

function resetDialog() {
  revokePreview()
  editingId.value = null
  form.file = null
  form.description = ''
  existingLogoUrl.value = ''
  previewUrl.value = ''
  uploadRef.value?.clearFiles()
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
  }).format(date)
}

onMounted(loadLogos)
</script>

<style scoped>
.platform-logo-page { min-height: 100%; padding: 24px; background: #f5f7fa; }
.settings-panel { min-height: calc(100vh - 108px); padding: 24px; border-radius: 8px; background: #fff; }
.section-heading { margin-bottom: 22px; padding-left: 12px; border-left: 4px solid var(--el-color-primary); }
.section-heading h1 { margin: 0; color: #303133; font-size: 20px; font-weight: 600; }
.section-heading p { margin: 8px 0 0; color: #909399; font-size: 14px; }
.logo-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; align-items: stretch; }
.create-card, .logo-card { min-height: 224px; border: 1px solid #e4e7ed; border-radius: 6px; background: #f8f9fb; }
.create-card { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 14px; border-style: dashed; color: #a8abb2; cursor: pointer; transition: 0.2s ease; }
.create-card:hover { border-color: var(--el-color-primary); color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.logo-card { position: relative; display: flex; flex-direction: column; padding: 20px; overflow: hidden; transition: 0.2s ease; }
.logo-card.active { border-color: var(--el-color-primary); box-shadow: 0 0 0 1px var(--el-color-primary) inset; }
.card-accent { position: absolute; top: 48px; left: 0; width: 4px; height: 28px; border-radius: 0 4px 4px 0; background: var(--el-color-primary); }
.card-top { display: flex; align-items: flex-start; justify-content: space-between; min-height: 48px; gap: 12px; }
.brand-preview { display: flex; min-width: 0; height: 44px; align-items: center; gap: 10px; color: #1677ff; }
.brand-preview img { display: block; max-width: 210px; height: 44px; object-fit: contain; object-position: left center; }
.brand-preview strong { overflow: hidden; font-size: 20px; white-space: nowrap; text-overflow: ellipsis; }
.default-mark { display: inline-flex; width: 42px; height: 42px; align-items: center; justify-content: center; border: 2px solid #1677ff; border-radius: 50%; font-size: 14px; font-weight: 700; }
.more-button { padding: 4px; border: 0; color: #606266; background: transparent; cursor: pointer; }
.description { min-height: 38px; margin: 14px 0 10px; overflow: hidden; color: #606266; font-size: 14px; line-height: 19px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.metadata { display: grid; gap: 5px; margin-bottom: 16px; color: #a8abb2; font-size: 12px; }
.metadata span { display: flex; align-items: center; gap: 5px; }
.state-button { width: 100%; margin-top: auto; }
.state-button.is-disabled { color: #fff; background: var(--el-color-primary); border-color: var(--el-color-primary); opacity: 1; }
.delete-action { display: inline-flex; align-items: center; gap: 5px; color: var(--el-color-danger); }
.logo-uploader { width: 100%; }
.upload-box { display: flex; width: 180px; height: 100px; flex-direction: column; align-items: center; justify-content: center; gap: 8px; overflow: hidden; border: 1px dashed #dcdfe6; border-radius: 6px; color: #a8abb2; background: #fafafa; }
.upload-box:hover { border-color: var(--el-color-primary); color: var(--el-color-primary); }
.upload-box img { width: 100%; height: 100%; object-fit: contain; }
.upload-help { margin-top: 8px; color: #909399; font-size: 12px; line-height: 18px; }
@media (max-width: 768px) {
  .platform-logo-page { padding: 12px; }
  .settings-panel { padding: 16px; }
  .logo-grid { grid-template-columns: 1fr; }
}
</style>
