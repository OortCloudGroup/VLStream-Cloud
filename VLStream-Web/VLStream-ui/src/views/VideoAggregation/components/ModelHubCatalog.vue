<template>
  <section class="hub-catalog">
    <div class="hub-header">
      <slot name="tabs" />
      <div class="hub-toolbar">
        <el-input v-model="keyword" :placeholder="$tp('搜索 Model Hub 模型')" clearable :prefix-icon="Search" @input="scheduleSearch" @keyup.enter="search" />
        <el-select v-if="categories.length" v-model="category" clearable :placeholder="$tp('全部分类')" class="hub-categories" @change="search">
          <el-option v-for="item in categories" :key="item.category" :value="item.category" :label="`${item.category} (${item.count})`" />
        </el-select>
      </div>
    </div>
    <p class="hub-note">{{ $tp('浏览模型和文件目录无需登录，下载、下发时需登录 OortCloud。') }}</p>
    <el-alert v-if="categoryError" :title="categoryError" type="warning" :closable="false" show-icon />
    <div v-if="error" class="hub-error"><el-alert :title="error" type="error" :closable="false" show-icon /><el-button @click="load">{{ $tp('重试') }}</el-button></div>
    <div v-loading="loading" class="hub-list">
      <el-empty v-if="!loading && !error && !models.length" :description="keyword ? $tp('没有匹配的 Model Hub 模型') : $tp('暂无公开模型')" />
      <div class="hub-grid">
        <article v-for="model in models" :key="model.uid" class="hub-card">
          <ModelHubCover class="hub-cover" :resource="model" :alt="model.name || model.alias || 'Model Hub'" />
          <div class="hub-card-body">
            <h3 :title="model.name || model.alias">{{ model.name || model.alias }}</h3>
            <p class="hub-meta" :title="[model.category, model.author_name].filter(Boolean).join(' · ')">{{ model.category || $tp('未分类') }}<span v-if="model.author_name"> · {{ model.author_name }}</span></p>
            <div class="hub-description-area">
              <p class="hub-description">{{ model.description || $tp('暂无模型描述') }}</p>
              <el-button v-if="model.description" class="description-toggle" link type="primary" @click="descriptionModel = model">{{ $tp('查看详情') }}</el-button>
            </div>
            <div class="hub-file-summary">
              <p class="hub-file-type">{{ fileType(model.file_path) }}<span v-if="model.file_size"> · {{ formatSize(model.file_size) }}</span></p>
              <p v-if="!model.repo_owner || !model.repo_name" class="hub-repository-note">{{ $tp('尚未建立模型文件空间') }}</p>
            </div>
            <div class="hub-actions"><el-button type="primary" plain size="small" @click="openFiles(model)">{{ $tp('查看文件') }}</el-button>
              <el-tooltip :content="$tp('尚缺少设备兼容格式、类别文件及校验信息')"><el-button size="small" disabled>{{ $tp('下发') }}</el-button></el-tooltip></div>
          </div>
        </article>
      </div>
    </div>
    <div v-if="total > 12" class="hub-footer">
      <el-pagination v-model:current-page="page" :page-size="12" :total="total" :pager-count="5" :disabled="loading" layout="total, prev, pager, next" background @current-change="load" />
    </div>
    <el-dialog :model-value="Boolean(descriptionModel)" class="model-description-dialog" :title="descriptionModel?.name || descriptionModel?.alias" width="min(560px, 92vw)" append-to-body @update:model-value="descriptionModel = null">
      <p class="full-model-description">{{ descriptionModel?.description }}</p>
      <template #footer><el-button @click="descriptionModel = null">{{ $tp('关闭') }}</el-button></template>
    </el-dialog>
    <el-dialog v-model="filesVisible" class="locale-dialog locale-dialog--wide" :title="$tp('{value0} · 文件', { value0: activeModel?.name || $tp('模型') })" append-to-body :close-on-click-modal="false" :before-close="closeFiles">
      <div class="file-toolbar"><el-button :disabled="!filePath || fileLoading || downloading" @click="parentDirectory">{{ $tp('上一级') }}</el-button><span>{{ filePath || '/' }}</span></div>
      <div v-if="fileError" class="hub-error"><el-alert :title="fileError" type="error" :closable="false" /><el-button v-if="/登录|身份|401|4004/.test(fileError)" @click="login">{{ $tp('重新登录') }}</el-button></div>
      <el-table v-loading="fileLoading" :data="files" scrollbar-always-on :empty-text="$tp('当前目录没有文件')">
        <el-table-column prop="name" :label="$tp('名称')" min-width="240" show-overflow-tooltip />
        <el-table-column :label="$tp('大小')" width="110"><template #default="{ row }">{{ row.type === 'dir' ? $tp('目录') : formatSize(row.size) }}</template></el-table-column>
        <el-table-column :label="$tp('操作')" width="100"><template #default="{ row }"><el-button link type="primary" :disabled="downloading || fileLoading" @click="row.type === 'dir' ? loadFiles(row.path) : download(row)">{{ row.type === 'dir' ? $tp('打开') : $tp('下载') }}</el-button></template></el-table-column>
      </el-table>
      <template #footer><el-button :disabled="downloading" @click="filesVisible = false">{{ $tp('关闭') }}</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { translatePhrase as uiText } from '@/i18n'

import { onMounted, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import ModelHubCover from './ModelHubCover.vue'
import { getPublicHubModels, getHubCategories, getHubFiles, downloadHubFile } from '@/api/modelHubCatalog'
import { getOortCloudSession } from '@/utils/oortCloudSession'
import { isMultiTenantMode, redirectToPlatformLogin } from '@/utils/platformSession'
import { startModelHubLogin } from '@/utils/modelHubAuth'

const emit = defineEmits(['count', 'busy'])
const keyword = ref(''), page = ref(1), total = ref(0), models = ref([]), loading = ref(false), error = ref('')
const category = ref(''), categories = ref([])
const categoryError = ref('')
const descriptionModel = ref(null)
const currentSession = () => getOortCloudSession(isMultiTenantMode() ? 'multi' : 'single')
const activeModel = ref(null), filesVisible = ref(false), files = ref([]), filePath = ref(''), fileError = ref(''), fileLoading = ref(false), downloading = ref(false)
let sequence = 0, fileSequence = 0, timer
const formatSize = size => !size ? '-' : size < 1048576 ? `${(size / 1024).toFixed(1)} KB` : `${(size / 1048576).toFixed(1)} MB`
function fileType(path) {
  const ext = String(path || '').split('?')[0].split('.').pop()?.toLowerCase()
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg'].includes(ext)) return uiText('图片文件，不能部署到设备')
  return ext && ext.length < 12 ? uiText('{value0} 文件', { value0: ext.toUpperCase() }) : uiText('文件类型待确认')
}
function login() {
  if (isMultiTenantMode()) redirectToPlatformLogin()
  else startModelHubLogin(null, { returnToCurrent: true })
}
async function load() {
  const id = ++sequence
  loading.value = true; error.value = ''; models.value = []
  try {
    const data = await getPublicHubModels({ page: page.value, size: 12, keyword: keyword.value.trim(), category: category.value })
    if (id !== sequence) return
    if (!Array.isArray(data?.list)) throw new Error('Model Hub 列表响应格式不正确')
    models.value = data.list; total.value = Number(data.counts ?? data.count ?? 0)
    if (!keyword.value.trim() && !category.value) emit('count', total.value)
  } catch (e) { if (id === sequence) error.value = e.message || '加载 Model Hub 失败' }
  finally { if (id === sequence) loading.value = false }
}
function search() { clearTimeout(timer); page.value = 1; load() }
function scheduleSearch() { sequence++; clearTimeout(timer); timer = setTimeout(search, 300) }
function closeFiles(done) { if (!downloading.value) { fileSequence++; done() } }
function sameSession(session) { const now = currentSession(); return now.accessToken === session.accessToken && now.tenantId === session.tenantId }
async function openFiles(model) {
  activeModel.value = model; filePath.value = ''; files.value = []; fileError.value = ''; filesVisible.value = true
  if (!model.repo_owner || !model.repo_name) {
    fileError.value = '该历史模型尚未建立 Gitea 文件空间，暂不支持授权文件下载'
    return
  }
  await loadFiles('')
}
async function loadFiles(path) {
  const id = ++fileSequence
  fileLoading.value = true; fileError.value = ''; files.value = []
  try {
    const data = await getHubFiles({uid:activeModel.value.uid,path,branch:activeModel.value.default_branch || ''})
    if (id !== fileSequence) return
    if (!Array.isArray(data)) throw new Error('模型文件列表响应格式不正确')
    files.value = data.filter(file => ['dir','file'].includes(file.type)); filePath.value = path
  } catch (e) { if (id === fileSequence) fileError.value = e.message || '读取模型文件失败' }
  finally { if (id === fileSequence) fileLoading.value = false }
}
function parentDirectory() { loadFiles(filePath.value.split('/').slice(0,-1).join('/')) }
async function download(file) {
  const session = currentSession()
  if (!session.accessToken) return login()
  downloading.value = true; emit('busy', true)
  try {
    const blob = await downloadHubFile({uid:activeModel.value.uid,path:file.path,branch:activeModel.value.default_branch || ''},session)
    if (!sameSession(session)) throw new Error('登录身份已变更，请重新下载')
    const url = URL.createObjectURL(blob), a = document.createElement('a')
    a.href = url; a.download = String(file.name || 'model-file.bin').replace(/[\\/:*?"<>|]/g, '_'); a.click()
    setTimeout(() => URL.revokeObjectURL(url), 60000)
    ElMessage.success(uiText('文件已下载'))
  } catch (e) { fileError.value = e.message || '下载失败，请重试' }
  finally { downloading.value = false; emit('busy', false) }
}
async function loadCategories() {
  categories.value = []
  categoryError.value = ''
  try {
    const data = await getHubCategories()
    if (Array.isArray(data)) categories.value = data.filter(item => item.category)
  } catch (e) { categoryError.value = e.message || '分类暂不可用，仍可浏览公开模型' }
}
onMounted(() => { load(); loadCategories() })
onBeforeUnmount(() => { sequence++; fileSequence++; clearTimeout(timer) })
</script>

<style scoped>
.hub-catalog { display: flex; flex-direction: column; flex: 1; min-height: 0; min-width: 0; overflow: hidden; }
.hub-header { display: flex; align-items: center; justify-content: space-between; gap: 32px; flex: none; padding-bottom: 16px; border-bottom: 1px solid #e5eaf2; }
.hub-header :deep(.source-tabs) { flex: none; min-width: 0; }
.hub-header :deep(.el-tabs__header) { margin-bottom: 0; }
.hub-toolbar { display: flex; justify-content: flex-end; align-items: center; gap: 12px; flex: 1; min-width: 0; }
.hub-toolbar .el-input { width: 340px; min-width: 0; flex: 1; max-width: 340px; }
.hub-categories { width: 200px; flex: 0 0 200px; }
.hub-toolbar :deep(.el-input__wrapper), .hub-toolbar :deep(.el-select__wrapper) { min-height: 38px; border-radius: 8px; background: #fff; box-shadow: 0 0 0 1px #dfe5ee inset; }
.hub-toolbar :deep(.el-input__wrapper.is-focus), .hub-toolbar :deep(.el-select__wrapper.is-focused) { box-shadow: 0 0 0 1px #307bff inset; }
.hub-note { flex: none; font-size: 12px; color: #7b8799; line-height: 1.8; margin: 14px 0 18px; }
.hub-catalog > .el-alert, .hub-error { flex: none; }
.hub-list { flex: 1; min-height: 0; overflow-y: auto; overflow-x: hidden; overscroll-behavior: contain; scrollbar-gutter: stable; padding: 2px 10px 16px 2px; }
.hub-list::-webkit-scrollbar { width: 8px; }
.hub-list::-webkit-scrollbar-thumb { background: #c4ccd8; border-radius: 4px; }
.hub-list::-webkit-scrollbar-track { background: #edf0f5; }
.hub-grid { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); align-items: start; gap: 22px; }
.hub-card { min-width: 0; display: flex; flex-direction: column; background: #fff; border: 1px solid #e2e8f1; border-radius: 12px; overflow: hidden; box-shadow: 0 3px 10px #24395504; transition: border-color .2s, box-shadow .2s; }
.hub-card:hover { border-color: #bdcfea; box-shadow: 0 6px 20px #2439550c; }
.hub-cover { flex: none; height: auto; aspect-ratio: 2.4; border-bottom: 1px solid #edf0f5; }
.hub-card-body { display: flex; flex-direction: column; min-width: 0; padding: 16px; }
.hub-card h3 { font-size: 16px; line-height: 24px; font-weight: 600; color: #26364c; margin: 0 0 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hub-meta { font-size: 12px; line-height: 20px; color: #8691a1; margin: 0 0 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hub-description-area { height: 68px; flex: none; }
.hub-description { font-size: 13px; color: #697789; line-height: 21px; margin: 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; overflow-wrap: anywhere; }
.description-toggle { font-size: 12px; margin-top: 4px; padding: 0; line-height: 20px; }
.hub-file-summary { height: 40px; flex: none; }
.hub-file-type { font-size: 12px; line-height: 20px; color: #7b8799; margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hub-repository-note { margin: 0; color: #97a0ae; font-size: 11px; line-height: 18px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hub-actions { display: flex; gap: 10px; padding-top: 12px; border-top: 1px solid #eef1f6; }
.hub-actions .el-button { margin-left: 0; flex: 1; min-height: 32px; border-radius: 7px; }
.hub-actions .el-button.is-disabled { color: #9ba5b3; background: #f8f9fc; border-color: #e9edf3; }
.hub-error { display:flex; align-items:center; gap:12px; margin-bottom:12px; }
.hub-footer { flex: none; display: flex; justify-content: center; padding-top: 10px; border-top: 1px solid #dfe7f2; background: #f6f8fc; }
.el-pagination { justify-content: center; }
.full-model-description { white-space: pre-wrap; overflow-wrap: anywhere; color: #556479; font-size: 14px; line-height: 1.9; max-height: 50vh; overflow-y: auto; margin: 0; }
.file-toolbar { display:flex; gap:16px; align-items:center; margin-bottom:16px; overflow-wrap:anywhere; }
@media(max-width:1380px){.hub-grid{grid-template-columns:repeat(3,minmax(0,1fr))}}
@media(max-width:800px){.hub-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:1000px){.hub-header{flex-wrap:wrap;gap:8px}.hub-toolbar{flex-basis:100%;justify-content:flex-start;padding-top:0}.hub-toolbar .el-input{max-width:none}}
@media(max-width:540px){.hub-toolbar{flex-wrap:wrap}.hub-toolbar .el-input,.hub-categories{width:100%;flex-basis:100%}.hub-grid{grid-template-columns:minmax(0,1fr)}.el-pagination{flex-wrap:wrap;gap:6px 0}.el-pagination :deep(.el-pagination__total){flex-basis:100%;margin:0;text-align:center}.el-pagination :deep(button),.el-pagination :deep(.el-pager li){min-width:28px;margin:0 2px}}
</style>
