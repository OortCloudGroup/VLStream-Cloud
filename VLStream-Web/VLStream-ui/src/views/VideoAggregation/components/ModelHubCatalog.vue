<template>
  <section class="hub-catalog">
    <div class="hub-toolbar">
      <el-input v-model="keyword" placeholder="搜索 Model Hub 模型" clearable :prefix-icon="Search" @input="scheduleSearch" @keyup.enter="search" />
    </div>
    <el-select v-if="categories.length" v-model="category" clearable placeholder="全部分类" class="hub-categories" @change="search">
      <el-option v-for="item in categories" :key="item.category" :value="item.category" :label="`${item.category} (${item.count})`" />
    </el-select>
    <p class="hub-note">浏览模型和文件目录无需登录，下载、下发时需登录 OortCloud。</p>
    <el-alert v-if="categoryError" :title="categoryError" type="warning" :closable="false" show-icon />
    <div v-if="error" class="hub-error"><el-alert :title="error" type="error" :closable="false" show-icon /><el-button @click="load">重试</el-button></div>
    <div v-loading="loading" class="hub-list">
      <el-empty v-if="!loading && !error && !models.length" :description="keyword ? '没有匹配的 Model Hub 模型' : '暂无公开模型'" />
      <div class="hub-grid">
        <article v-for="model in models" :key="model.uid" class="hub-card">
          <div class="hub-cover"><el-icon><Box /></el-icon><span>{{ model.framework || model.model_type || 'Model Hub' }}</span></div>
          <div class="hub-card-body">
            <h3>{{ model.name || model.alias }}</h3>
            <p class="hub-meta">{{ model.category || '未分类' }}<span v-if="model.author_name"> · {{ model.author_name }}</span></p>
            <p class="hub-description">{{ model.description || '暂无模型描述' }}</p>
            <p class="hub-file-type">{{ fileType(model.file_path) }}<span v-if="model.file_size"> · {{ formatSize(model.file_size) }}</span></p>
            <p v-if="!model.repo_owner || !model.repo_name" class="hub-meta">尚未建立模型文件空间</p>
            <div class="hub-actions"><el-button type="primary" plain size="small" @click="openFiles(model)">查看文件</el-button>
              <el-tooltip content="尚缺少设备兼容格式、类别文件及校验信息"><el-button size="small" disabled>下发</el-button></el-tooltip></div>
          </div>
        </article>
      </div>
    </div>
    <el-pagination v-if="total > 12" v-model:current-page="page" :page-size="12" :total="total" :disabled="loading" layout="total, prev, pager, next" background @current-change="load" />
    <el-dialog v-model="filesVisible" :title="`${activeModel?.name || '模型'} · 文件`" width="720px" append-to-body :close-on-click-modal="false" :before-close="closeFiles">
      <div class="file-toolbar"><el-button :disabled="!filePath || fileLoading || downloading" @click="parentDirectory">上一级</el-button><span>{{ filePath || '/' }}</span></div>
      <div v-if="fileError" class="hub-error"><el-alert :title="fileError" type="error" :closable="false" /><el-button v-if="/登录|身份|401|4004/.test(fileError)" @click="login">重新登录</el-button></div>
      <el-table v-loading="fileLoading" :data="files" empty-text="当前目录没有文件">
        <el-table-column prop="name" label="名称" min-width="240" show-overflow-tooltip />
        <el-table-column label="大小" width="110"><template #default="{ row }">{{ row.type === 'dir' ? '目录' : formatSize(row.size) }}</template></el-table-column>
        <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="primary" :disabled="downloading || fileLoading" @click="row.type === 'dir' ? loadFiles(row.path) : download(row)">{{ row.type === 'dir' ? '打开' : '下载' }}</el-button></template></el-table-column>
      </el-table>
      <template #footer><el-button :disabled="downloading" @click="filesVisible = false">关闭</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, Search } from '@element-plus/icons-vue'
import { getPublicHubModels, getHubCategories, getHubFiles, downloadHubFile } from '@/api/modelHubCatalog'
import { getOortCloudSession } from '@/utils/oortCloudSession'
import { isMultiTenantMode, redirectToPlatformLogin } from '@/utils/platformSession'
import { startModelHubLogin } from '@/utils/modelHubAuth'

const emit = defineEmits(['count', 'busy'])
const keyword = ref(''), page = ref(1), total = ref(0), models = ref([]), loading = ref(false), error = ref('')
const category = ref(''), categories = ref([])
const categoryError = ref('')
const currentSession = () => getOortCloudSession(isMultiTenantMode() ? 'multi' : 'single')
const activeModel = ref(null), filesVisible = ref(false), files = ref([]), filePath = ref(''), fileError = ref(''), fileLoading = ref(false), downloading = ref(false)
let sequence = 0, fileSequence = 0, timer
const formatSize = size => !size ? '-' : size < 1048576 ? `${(size / 1024).toFixed(1)} KB` : `${(size / 1048576).toFixed(1)} MB`
function fileType(path) {
  const ext = String(path || '').split('?')[0].split('.').pop()?.toLowerCase()
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg'].includes(ext)) return '图片文件，不能部署到设备'
  return ext && ext.length < 12 ? `${ext.toUpperCase()} 文件` : '文件类型待确认'
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
    ElMessage.success('文件已下载')
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
.hub-catalog { display: flex; flex-direction: column; flex: 1; min-height: 0; }
.hub-toolbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.hub-toolbar .el-input { width: 340px; max-width: 65%; }
.hub-categories { margin-top: 14px; width: 240px; }
.hub-toolbar :deep(.el-input__wrapper) { border-radius: 20px; }
.hub-note { font-size: 12px; color: #9098a5; line-height: 1.8; margin: 16px 0 20px; }
.hub-list { flex: 1; min-height: 150px; overflow: auto; }
.hub-grid { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 20px; }
.hub-card { background: #fff; border: 1px solid #e8edf5; border-radius: 8px; overflow: hidden; }
.hub-cover { height: 145px; background: linear-gradient(135deg,#e5effb,#f3f6fc); display:flex; align-items:center; justify-content:center; flex-direction:column; gap:12px; color:#7b9cc4; font-size:12px; }
.hub-cover .el-icon { font-size: 36px; }
.hub-card-body { padding: 16px; }
h3 { font-size: 15px; margin: 0 0 10px; overflow-wrap:anywhere; }
.hub-meta, .hub-file-type { font-size:12px; color:#9098a5; }
.hub-description { font-size:13px; color:#697789; line-height:1.7; min-height:44px; overflow-wrap:anywhere; }
.hub-actions { display:flex; flex-wrap:wrap; gap:8px; margin-top:16px; }
.hub-actions .el-button { margin-left:0; }
.hub-error { display:flex; align-items:center; gap:12px; margin-bottom:12px; }
.el-pagination { padding:16px 0; justify-content:center; }
.file-toolbar { display:flex; gap:16px; align-items:center; margin-bottom:16px; overflow-wrap:anywhere; }
@media(max-width:1200px){.hub-grid{grid-template-columns:repeat(3,minmax(0,1fr))}}
@media(max-width:800px){.hub-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
</style>
