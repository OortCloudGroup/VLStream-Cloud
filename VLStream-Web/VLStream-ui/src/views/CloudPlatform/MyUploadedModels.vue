<template>
  <div class="my-uploaded-models" v-loading="loading">
    <!-- 模型详情 -->
    <ModelDetail
      v-if="detailUid"
      :uid="detailUid"
      @back="detailUid = ''"
    />

    <template v-else>
    <div class="toolbar depNameBox_out flexRowAC">
      <div class="exportBtnBox flexRowAC">
        <div class="exportBtn newBtn flexRowAC" @click="openCreateDialog">
          <el-icon class="BtnImg">
            <el-icon><Plus /></el-icon>
          </el-icon>
          上传模型
        </div>
      </div>
      <div class="searchHeight_out flexRowAC">
        <search-height-box
          keyword="keyword"
          placeholder="搜索"
          :data="searchData"
          @handle="searchResetFn"
        />
        <export-excel-pdf />
      </div>
    </div>

    <el-empty v-if="!loading && models.length === 0" description="暂无上传的模型" />

    <div v-else class="model-grid">
      <div
        v-for="item in models"
        :key="item.uid"
        class="model-card"
        :class="{ active: activeUid === item.uid }"
        @click="openDetail(item)"
      >
        <div class="card-top">
          <div class="card-title-block">
            <div class="card-title" :title="item.name">{{ item.name || item.alias || '未命名模型' }}</div>
            <div class="card-desc" :title="item.description">
              {{ item.description || '暂无描述' }}
            </div>
          </div>
          <el-dropdown trigger="click" @command="(cmd) => handleCardCommand(cmd, item)">
            <div class="card-more" @click.stop>
              <img :src="iconMore" class="card-icon" alt="more" />
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit">
                  <img :src="iconEdit" class="menu-icon" alt="edit" />
                  编辑模型
                </el-dropdown-item>
                <el-dropdown-item command="delete" divided>
                  <img :src="iconDelete" class="menu-icon" alt="delete" />
                  删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div class="card-cover">
          <img :src="getCover(item)" :alt="item.name" />
          <el-tag v-if="statusText(item.status)" size="small" class="status-tag" :type="statusTagType(item.status)">
            {{ statusText(item.status) }}
          </el-tag>
        </div>

        <div class="card-footer">
          <span class="meta-item">
            <img :src="iconDownload" class="meta-icon" alt="download" />
            {{ item.download_count ?? item.downloads ?? 0 }}
          </span>
          <span class="meta-item">
            <img :src="iconStar" class="meta-icon" alt="star" />
            {{ item.star_count ?? item.stars ?? 0 }}
          </span>
        </div>
      </div>
    </div>

    <div v-if="total > pageSize" class="pager">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        @current-change="handlePageChange"
      />
    </div>
    </template>

    <!-- 上传 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingUid ? '编辑模型' : '创建模型'"
      width="35%"
      destroy-on-close
      class="model-form-dialog"
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="96px"
        label-position="right"
        require-asterisk-position="left"
      >
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="模型别名" prop="alias">
          <el-input v-model="form.alias" placeholder="请输入模型别名" />
        </el-form-item>
        <el-form-item label="模型类型" prop="model_type">
          <el-input v-model="form.model_type" placeholder="请输入模型类型" />
        </el-form-item>
        <el-form-item label="模型文件" prop="file_path">
          <el-upload
            class="model-file-upload"
            drag
            :headers="uploadHeaders"
            :action="uploadURL"
            :show-file-list="false"
            :limit="1"
            :on-success="handleFileSuccess"
            :on-remove="handleFileRemove"
            :on-error="handleFileError"
            :before-upload="beforeFileUpload"
          >
            <div class="upload-zone">
              <div class="upload-icon-wrap">
                <el-icon class="upload-doc"><Document /></el-icon>
                <el-icon class="upload-plus"><Plus /></el-icon>
              </div>
              <div class="upload-text">选择模型文件</div>
              <div v-if="form.file_path" class="upload-path">{{ form.file_path }}</div>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="模型描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入模型描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Document } from '@element-plus/icons-vue'
import {
  addAiModel,
  deleteAiModel,
  editAiModel,
  getAiModelList
} from '@/api/aiModel'
import { getModelHubAccessToken } from '@/utils/modelHubAuth'
import ModelDetail from './ModelDetail.vue'
import iconEdit from '@/assets/img/uploadedModels/edit.png'
import iconDelete from '@/assets/img/uploadedModels/delete.png'
import iconMore from '@/assets/img/uploadedModels/more.png'
import iconDownload from '@/assets/img/uploadedModels/download.png'
import iconStar from '@/assets/img/uploadedModels/star.png'

const PLATFORM_ORIGIN = import.meta.env.DEV
  ? ''
  : 'https://workup-dev.myoumuamua.com:6433'
const uploadURL = `${PLATFORM_ORIGIN}/bus/apaas-fastdfsservice/fastdfs/v1/uploadFile`

const uploadHeaders = computed(() => {
  const token = getModelHubAccessToken()
  const tenantId =
    sessionStorage.getItem('modelHubTenantId') ||
    localStorage.getItem('modelHubTenantId') ||
    ''
  return {
    accesstoken: token || '',
    requesttype: 'app',
    appid: '08e3168bd56a4e75ae3d5dee63db0657',
    secretkey: '32e3ca224aa741fbb1362d33070bca2f',
    ...(tenantId ? { tenantid: tenantId } : {})
  }
})

const DEFAULT_COVER =
  'data:image/svg+xml,' +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="320" height="180" viewBox="0 0 320 180">
      <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stop-color="#f6e7c1"/><stop offset="100%" stop-color="#e8c98a"/>
      </linearGradient></defs>
      <rect width="320" height="180" fill="url(#g)"/>
      <circle cx="80" cy="50" r="18" fill="#d4a85a" opacity="0.45"/>
      <circle cx="220" cy="120" r="28" fill="#c9963f" opacity="0.35"/>
    </svg>`
  )

const loading = ref(false)
const submitting = ref(false)
const models = ref([])
const keyword = ref('')
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const activeUid = ref('')
const detailUid = ref('')
const dialogVisible = ref(false)
const editingUid = ref('')
const formRef = ref(null)

/** 高级搜索配置 */
const searchData = ref([
  { label: '模型名称', value: 'keyword', type: 'text', default: '' },
  {
    label: '状态',
    value: 'status',
    type: 'select',
    default: 0,
    option: [
      { label: '全部', value: 0 },
      { label: '已通过', value: 1 },
      { label: '审核中', value: 2 },
      { label: '已拒绝', value: 3 }
    ]
  }
])
const filterStatus = ref(0)

const form = reactive({
  name: '',
  alias: '',
  model_type: '',
  description: '',
  file_path: ''
})

const rules = {
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  alias: [{ required: true, message: '请输入模型别名', trigger: 'blur' }],
  model_type: [{ required: true, message: '请输入模型类型', trigger: 'blur' }],
  file_path: [{ required: true, message: '请先上传模型文件', trigger: 'change' }]
}

const statusText = (status) => {
  const map = { 1: '已通过', 2: '审核中', 3: '已拒绝' }
  return map[status] || ''
}

const statusTagType = (status) => {
  const map = { 1: 'success', 2: 'warning', 3: 'danger' }
  return map[status] || 'info'
}

const getCover = (item) => item.cover || item.cover_url || item.thumbnail || DEFAULT_COVER

const openDetail = (item) => {
  activeUid.value = item.uid
  detailUid.value = item.uid
}

const loadModels = async () => {
  const accessToken = getModelHubAccessToken()
  if (!accessToken) {
    ElMessage.warning('未登录，无法获取模型列表')
    return
  }

  loading.value = true
  try {
    const res = await getAiModelList({
      accessToken,
      page: page.value,
      pageNum: page.value,
      pageSize: pageSize.value,
      pagesize: pageSize.value,
      status: filterStatus.value,
      keyword: keyword.value.trim()
    })

    if (res?.code === 200) {
      const data = res.data || {}
      let list = data.list || data.records || []
      const kw = keyword.value.trim().toLowerCase()
      if (kw) {
        list = list.filter((item) => {
          const text = `${item.name || ''} ${item.alias || ''} ${item.description || ''} ${item.model_type || ''}`.toLowerCase()
          return text.includes(kw)
        })
      }
      models.value = list
      total.value = data.count || data.counts || data.total || list.length
      if (!activeUid.value && models.value.length) {
        activeUid.value = models.value[0].uid
      }
    } else {
      ElMessage.error(res?.msg || '获取模型列表失败')
    }
  } catch (error) {
    console.error('getAiModelList failed:', error)
    ElMessage.error(error?.response?.data?.msg || error?.message || '获取模型列表失败')
  } finally {
    loading.value = false
  }
}

/** SearchHeightBox 回调：搜索 / 重置 */
const searchResetFn = (val, reset) => {
  if (reset) {
    page.value = 1
  }
  keyword.value = val?.keyword || ''
  filterStatus.value = val?.status !== undefined && val?.status !== '' ? val.status : 0
  page.value = 1
  loadModels()
}
const handlePageChange = (p) => {
  page.value = p
  loadModels()
}

const resetForm = () => {
  editingUid.value = ''
  form.name = ''
  form.alias = ''
  form.model_type = ''
  form.description = ''
  form.file_path = ''
  formRef.value?.clearValidate?.()
}

const openCreateDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = async (item) => {
  resetForm()
  editingUid.value = item.uid
  dialogVisible.value = true

  try {
    const res = await getAiModelDetail({ uid: item.uid })
    const detail = res?.code === 200 ? res.data : item
    form.name = detail.name || ''
    form.alias = detail.alias || ''
    form.model_type = detail.model_type || ''
    form.description = detail.description || ''
    form.file_path = detail.file_path || ''
  } catch (error) {
    form.name = item.name || ''
    form.alias = item.alias || ''
    form.model_type = item.model_type || ''
    form.description = item.description || ''
    form.file_path = item.file_path || ''
    console.warn('getAiModelDetail failed, fallback to list item:', error)
  }
}

const handleCardCommand = (command, item) => {
  if (command === 'edit') {
    openEditDialog(item)
  } else if (command === 'delete') {
    handleDelete(item)
  }
}

const beforeFileUpload = (file) => {
  const isLt500M = file.size / 1024 / 1024 < 500
  if (!isLt500M) {
    ElMessage.error('模型文件不能超过 500MB')
    return false
  }
  return true
}

/** fastdfs 返回 path 作为 file_path */
const handleFileSuccess = (res) => {
  const path = res?.data?.path || res?.data?.filePath || res?.data?.url || ''
  if (res?.code === 200 && path) {
    form.file_path = res.data.path || path
    formRef.value?.clearValidate?.('file_path')
    ElMessage.success('文件上传成功')
  } else {
    ElMessage.error(res?.msg || '文件上传失败')
  }
}

const handleFileRemove = () => {
  form.file_path = ''
}

const handleFileError = () => {
  ElMessage.error('文件上传失败')
}

const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (!form.file_path) {
    ElMessage.warning('请先上传模型文件')
    return
  }

  submitting.value = true
  try {
    const payload = {
      name: form.name.trim(),
      alias: form.alias.trim(),
      model_type: form.model_type.trim(),
      description: form.description.trim(),
      file_path: form.file_path
    }

    const res = editingUid.value
      ? await editAiModel({ ...payload, uid: editingUid.value })
      : await addAiModel(payload)

    if (res?.code === 200) {
      ElMessage.success(editingUid.value ? '编辑成功' : '上传成功')
      dialogVisible.value = false
      await loadModels()
    } else {
      ElMessage.error(res?.msg || '操作失败')
    }
  } catch (error) {
    console.error('submit model failed:', error)
    ElMessage.error(error?.response?.data?.msg || error?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm(`确定删除模型「${item.name || item.alias}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    const res = await deleteAiModel({ uid: item.uid })
    if (res?.code === 200) {
      ElMessage.success('删除成功')
      if (activeUid.value === item.uid) activeUid.value = ''
      await loadModels()
    } else {
      ElMessage.error(res?.msg || '删除失败')
    }
  } catch (error) {
    console.error('deleteAiModel failed:', error)
    ElMessage.error(error?.response?.data?.msg || error?.message || '删除失败')
  }
}

onMounted(() => {
  loadModels()
})
</script>

<style scoped>
.my-uploaded-models {
  padding: 8px 0 0;
  min-height: 360px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.exportBtnBox {
  gap: 12px;
}

.exportBtn {
  cursor: pointer;
  justify-content: center;
  padding: 0 12px;
  height: 36px;
  background: var(--el-color-primary);
  border-radius: 5px;
  border: 1px solid var(--el-color-primary);
  font-size: 14px;
  color: #ffffff;
  box-shadow: 0 2px 0 0 rgba(0, 0, 0, 0.04);
}

.exportBtn .BtnImg {
  width: 14px;
  height: 14px;
  margin-right: 8px;
}

.exportBtn.newBtn {
  border-radius: 50px !important;
}

.searchHeight_out {
  gap: 20px;
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 20px;
}

.model-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 14px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.model-card:hover,
.model-card.active {
  border-color: #1a53ff;
  box-shadow: 0 4px 14px rgba(26, 83, 255, 0.12);
}

.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 2px;
  margin-bottom: 8px;
}

.card-title-block {
  min-width: 0;
  flex: 1;
}

.card-title {
  font-size: 14px;
  line-height: 22px;
  color: #333333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-desc {
  margin-top: 8px;
  font-size: 12px;
  line-height: 18px;
  color: #666666;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-more {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-icon {
  width: 20px;
  height: 20px;
}

.menu-icon {
  width: 14px;
  height: 14px;
  margin-right: 6px;
  object-fit: contain;
  vertical-align: middle;
  filter: invert(1) brightness(0.45);
}

.card-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 10;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f0e4;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.status-tag {
  position: absolute;
  right: 8px;
  top: 8px;
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
  color: #333333;
  font-size: 12px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.meta-icon {
  width: 18px;
  height: 18px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.file-path {
  display: block;
  margin-top: 4px;
  color: #606266;
  word-break: break-all;
}

.model-file-upload {
  width: 100%;
}

.model-file-upload :deep(.el-upload) {
  width: 100%;
}

.model-file-upload :deep(.el-upload-dragger) {
  width: 100%;
  height: 120px;
  padding: 0;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.model-file-upload :deep(.el-upload-dragger:hover) {
  border-color: #1a53ff;
}

.upload-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
}

.upload-icon-wrap {
  position: relative;
  width: 36px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-doc {
  font-size: 36px;
  color: #4c7dff;
}

.upload-plus {
  position: absolute;
  left: 50%;
  top: 54%;
  transform: translate(-50%, -50%);
  font-size: 14px;
  color: #4c7dff;
  font-weight: 700;
}

.upload-text {
  font-size: 14px;
  color: #606266;
  line-height: 22px;
}

.upload-path {
  max-width: 100%;
  font-size: 12px;
  color: #909399;
  word-break: break-all;
  text-align: center;
}

.model-upload {
  width: 100%;
}
</style>
