<template>
  <div class="algorithm-model">
    <!-- 列表视图 -->
    <div v-if="!showDetailView" class="list-view">

    <!-- 操作按钮区域 -->
    <div class="toolbar-section">
      <div class="toolbar-left">
        <el-button type="primary" class="add-btn-custom" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增
        </el-button>
        <div class="edit-delete-group">
          <el-button class="edit-btn-custom" @click="handleEdit" :disabled="selectedRows.length !== 1">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button type="danger" class="delete-btn-custom" @click="handleDelete" :disabled="selectedRows.length === 0">
            <el-icon><Delete /></el-icon>
            {{ selectedRows.length > 1 ? '批量删除' : '删除' }}
          </el-button>
        </div>
      </div>
      
      <div class="toolbar-right">
        <AdvancedSearch 
          @search="handleAdvancedSearch"
          @reset="handleAdvancedSearchReset"
          @export="handleExport"
          @upload="handleUpload"
          @template="handleDownloadTemplate"
          @batch="handleBatchOperation"
        />
      </div>
    </div>

    <!-- 表格区域 -->
    <div class="table-section">
      <el-table 
        :data="currentPageData" 
        stripe 
        style="width: 100%"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        @row-click="handleRowClick">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="name" label="模型名称"/>
        <el-table-column prop="source" label="模型来源" align="center" />
        <el-table-column prop="version" label="版本" align="center" />
        <el-table-column prop="downloadCount" label="下载次数" align="center" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" align="right" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button
                type="primary"
                link
                size="small"
                @click="handleView(scope.row)"
              >
                查看
              </el-button>
              <el-button
                type="success"
                link
                size="small"
                @click="handleDownloadModel(scope.row)"
              >
                下载模型
              </el-button>
              <el-button
                type="danger"
                link
                size="small"
                @click="handleDeleteItem(scope.row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination-section">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    </div>

    <!-- 详情视图 -->
    <div v-if="showDetailView" class="detail-view">
      <!-- 面包屑导航 -->
      <div class="breadcrumb-section">
        <div class="breadcrumb">
          <span class="breadcrumb-item" @click="handleBackToList">算法模型</span>
          <span class="breadcrumb-separator">></span>
          <span class="breadcrumb-item active">详情</span>
        </div>
      </div>

      <!-- 基础信息 -->
      <div class="detail-info-section">
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">模型名称：</span>
            <span class="info-value">{{ currentModel?.name || '螺丝螺母识别' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">模型ID：</span>
            <span class="info-value">{{ currentModel?.id || '1' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">模型类型：</span>
            <span class="info-value">{{ currentModel?.type || '物体检测' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">模型来源：</span>
            <span class="info-value">{{ currentModel?.source || '零代码训练' }}</span>
          </div>
        </div>
      </div>

      <!-- 版本列表 -->
      <div class="version-section">
        <div class="version-table">
          <el-table :data="versionData" stripe style="width: 100%">
            <el-table-column prop="version" label="版本" align="center" />
            <el-table-column prop="taskName" label="对应训练任务名" min-width="200" />
            <el-table-column prop="trainMethod" label="训练方式" min-width="120" align="center" />
            <el-table-column prop="description" label="描述" min-width="100" align="center" />
            <el-table-column prop="createTime" label="创建时间" />
            <el-table-column label="操作" width="150" align="right">
              <template #default="scope">
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="handleExportModel(scope.row)"
                >
                  下载 {{ scope.row.modelName || '模型文件' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- 新增/编辑模型弹窗 -->
    <el-dialog
      v-model="showModelDialog"
      :title="isEditingModel ? '编辑算法模型' : '新增算法模型'"
      width="640px"
      :close-on-click-modal="false"
      @close="handleModelDialogClose"
    >
      <el-form
        ref="modelFormRef"
        :model="modelForm"
        :rules="modelFormRules"
        label-width="110px"
        label-position="left"
        v-loading="modelDialogLoading"
      >
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="modelForm.modelName" placeholder="请输入模型名称" clearable />
        </el-form-item>

        <el-form-item label="模型版本" prop="version">
          <el-input v-model="modelForm.version" placeholder="例如1" clearable />
        </el-form-item>

        <el-form-item label="模型格式" prop="modelFormat">
          <el-select v-model="modelForm.modelFormat" placeholder="请选择模型格式" style="width: 100%">
            <el-option
              v-for="item in modelFormatOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="模型路径" prop="modelPath">
          <el-input v-model="modelForm.modelPath" placeholder="请输入模型文件存储路径" clearable />
        </el-form-item>

        <el-form-item label="算法ID">
          <el-input v-model="modelForm.algorithmId" placeholder="可选，关联算法ID" clearable />
        </el-form-item>

        <el-form-item label="训练任务ID">
          <el-input v-model="modelForm.trainingId" placeholder="可选，关联训练任务ID" clearable />
        </el-form-item>

        <el-form-item label="模型状态" prop="status">
          <el-select v-model="modelForm.status" placeholder="请选择模型状态" style="width: 100%">
            <el-option
              v-for="item in modelStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="精度(%)" prop="accuracy">
          <el-input v-model="modelForm.accuracy" placeholder="可选，0-100" clearable />
        </el-form-item>

        <el-form-item label="模型描述">
          <el-input
            v-model="modelForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模型描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleModelDialogClose">取消</el-button>
          <el-button type="primary" @click="handleSubmitModel">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, h, onMounted, ref} from 'vue'
import {Delete, Edit, Plus} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, ElRadio, ElRadioGroup} from 'element-plus'
import AdvancedSearch from '@/components/AdvancedSearch.vue'
import {
  batchDeleteModel,
  createModel,
  deleteModel,
  getModelById,
  getModelPage,
  updateModel
} from '@/api/algorithmModel'
import request from "@/utils/request";

// 搜索表单
const searchForm = ref({
  modelName: '',
  modelSource: '',
  modelType: '',
  createTime: []
})

// 表格数据
const tableData = ref([])
const loading = ref(false)

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 计算当前页数据
const currentPageData = computed(() => {
  return tableData.value
})

// 选中行
const selectedRow = ref(null)
const selectedRows = ref([])

// 视图状态
const showDetailView = ref(false)
const currentModel = ref(null)

// 版本数据
const versionData = ref([])

// 模型新增/编辑
const showModelDialog = ref(false)
const isEditingModel = ref(false)
const modelDialogLoading = ref(false)
const modelFormRef = ref(null)
const modelForm = ref({
  id: null,
  modelName: '',
  version: '1',
  modelFormat: 'pt',
  modelPath: '',
  algorithmId: '',
  trainingId: '',
  status: 'published',
  accuracy: null,
  description: ''
})

const modelFormatOptions = [
  { label: 'onnx', value: 'onnx' },
  { label: 'pt', value: 'pt' },
  { label: 'other', value: 'other' }
]

const modelStatusOptions = [
  { label: '已发布', value: 'published' },
  { label: '草稿', value: 'draft' },
  { label: '测试中', value: 'testing' }
]

const validateAccuracy = (rule, value, callback) => {
  if (value === null || value === undefined || value === '') {
    callback()
    return
  }
  const numberValue = Number(value)
  if (Number.isNaN(numberValue) || numberValue < 0 || numberValue > 100) {
    callback(new Error('精度请输入 0 - 100 之间的数字'))
  } else {
    callback()
  }
}

const modelFormRules = {
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  version: [{ required: true, message: '请输入模型版本号', trigger: 'blur' }],
  modelFormat: [{ required: true, message: '请选择模型格式', trigger: 'change' }],
  modelPath: [{ required: true, message: '请输入模型路径', trigger: 'blur' }],
  status: [{ required: true, message: '请选择模型状态', trigger: 'change' }],
  accuracy: [{ validator: validateAccuracy, trigger: 'blur' }]
}

const resetModelForm = () => {
  modelDialogLoading.value = false
  modelForm.value = {
    id: null,
    modelName: '',
    version: '1',
    modelFormat: 'pt',
    modelPath: '',
    algorithmId: '',
    trainingId: '',
    status: 'published',
    accuracy: null,
    description: ''
  }
  if (modelFormRef.value) {
    modelFormRef.value.clearValidate()
  }
}

const fillModelForm = (data) => {
  if (!data) return
  Object.assign(modelForm.value, {
    id: data.id ?? null,
    modelName: data.modelName || data.name || '',
    version: data.version || data.modelVersion,
    modelFormat: data.modelFormat || data.format || 'pt',
    modelPath: data.modelPath || '',
    algorithmId: data.algorithmId ?? '',
    trainingId: data.trainingId ?? '',
    status: data.status || 'published',
    accuracy: data.accuracy ?? null,
    description: data.description || ''
  })
}

const toNumberOrNull = (value) => {
  if (value === '' || value === null || value === undefined) return null
  const num = Number(value)
  return Number.isNaN(num) ? null : num
}

// 生成版本数据
const generateVersionData = (modelData) => {
  if (!modelData) return []

  const originalData = modelData.originalData
  return [
    {
      version: originalData.version || 'V1',
      taskName: originalData.modelName || modelData.name,
      trainMethod: originalData.trainingId ? '零代码训练' : '导入模型',
      description: originalData.description || '-',
      createTime: originalData.createTime || modelData.createTime,
      modelPath: originalData.modelPath,
      modelName: `${modelData.name}.pt`
    }
  ]
}

// 加载模型数据
const loadModelData = async () => {
  try {
    loading.value = true
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      modelName: searchForm.value.modelName,
      status: 'published' // 只显示已发布的模型
    }

    // 处理日期范围
    if (searchForm.value.createTime && searchForm.value.createTime.length === 2) {
      params.createdTimeBegin = searchForm.value.createTime[0]
      params.createdTimeEnd = searchForm.value.createTime[1]
    }
    
    const response = await getModelPage(params)
    if (response.data) {
      tableData.value = response.data.records.map(item => ({
        id: item.id,
        name: item.modelName,
        source: getModelSource(item.trainingId),
        version: item.version,
        downloadCount: item.downloadCount,
        createTime: item.createTime,
        originalData: item
      }))
      total.value = response.data.total
    }
  } catch (error) {
    ElMessage.error('加载模型数据失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 模型来源映射
const getModelSource = (trainingId) => {
  return trainingId ? '零代码训练' : '导入模型'
}

// 事件处理
const handleSearch = () => {
  currentPage.value = 1
  loadModelData()
}

const handleReset = () => {
  searchForm.value = {
    modelName: '',
    modelSource: '',
    modelType: '',
    createTime: []
  }
  currentPage.value = 1
  loadModelData()
}

const handleAdd = () => {
  isEditingModel.value = false
  resetModelForm()
  showModelDialog.value = true
}

const handleEdit = async () => {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning('请选择一条模型进行编辑')
    return
  }

  resetModelForm()
  const row = selectedRows.value[0]
  isEditingModel.value = true
  showModelDialog.value = true
  modelDialogLoading.value = true

  try {
    const response = await getModelById(row.originalData.id)
    fillModelForm(response.data || row.originalData)
  } catch (error) {
    ElMessage.error('加载模型信息失败：' + error.message)
    showModelDialog.value = false
  } finally {
    modelDialogLoading.value = false
  }
}

const handleDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的模型')
    return
  }
  
  try {
    const message = selectedRows.value.length === 1 
      ? `确认要删除模型"${selectedRows.value[0].name}"吗？`
      : `确认要删除选中的${selectedRows.value.length}个模型吗？`
    
    await ElMessageBox.confirm(message, '确认删除', {
      type: 'warning'
    })
    
    if (selectedRows.value.length === 1) {
      await deleteModel(selectedRows.value[0].originalData.id)
    } else {
      const ids = selectedRows.value.map(row => row.originalData.id)
      await batchDeleteModel(ids)
    }
    
    ElMessage.success('删除成功')
    selectedRows.value = []
    await loadModelData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  }
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleRowClick = (row) => {
  selectedRow.value = row
}

const handleView = (row) => {
  console.log('查看模型', row)
  currentModel.value = row

  // 生成版本数据
  versionData.value = generateVersionData(row)

  showDetailView.value = true
}

const handleDownloadModel = async (row) => {
  try {
    console.log('下载模型', row)

    // 直接下载模型文件
    await downloadModelFile(row)

  } catch (error) {
    console.error('下载模型失败:', error)
    ElMessage.error('下载模型失败：' + error.message)
  }
}

const handleDeleteItem = async (row) => {
  try {
    await ElMessageBox.confirm(`确认要删除模型"${row.name}"吗？`, '确认删除', {
      type: 'warning'
    })
    
    await deleteModel(row.originalData.id)
    ElMessage.success('删除成功')
    await loadModelData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  }
}

// 详情视图相关方法
const handleBackToList = () => {
  showDetailView.value = false
  currentModel.value = null
}

// 下载模型文件
const promptDownloadModelType = async () => {
  let chosenType = 'pt'

  const TypeSelector = {
    name: 'DownloadModelTypeSelector',
    setup() {
      const localType = ref(chosenType)
      const updateType = (value) => {
        localType.value = value
        chosenType = value
      }

      return () => h('div', [
        h(
          ElRadioGroup,
          {
            modelValue: localType.value,
            'onUpdate:modelValue': updateType
          },
          () => [
            h(ElRadio, { label: 'pt' }, () => 'pt'),
            h(ElRadio, { label: 'onnx' }, () => 'onnx'),
            h(ElRadio, { label: 'rknn' }, () => 'rknn'),
            h(ElRadio, { label: 'int8-rknn' }, () => 'int8-rknn')
          ]
        )
      ])
    }
  }

  try {
    await ElMessageBox({
      title: '模型下载',
      message: h(TypeSelector),
      showCancelButton: true,
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      closeOnClickModal: false
    })
    return chosenType
  } catch (error) {
    return null
  }
}

const downloadModelFile = async (modelRow) => {
  try {
    console.log('下载模型文件 - 模型数据:', modelRow)

    const originalData = modelRow?.originalData || modelRow
    if (!originalData) {
      ElMessage.error('无法获取模型数据')
      return
    }

    const modelId = originalData.id ?? modelRow?.id
    if (!modelId) {
      ElMessage.error('没有模型ID')
      return
    }

    const downloadType = await promptDownloadModelType()
    if (!downloadType) {
      return
    }

    const fallbackName = modelRow?.name || originalData?.modelName || 'model'
    const fileName = `${fallbackName}.${downloadType}`

    const blob = await request({
      url: '/vlsAlgorithmTraining/download-model',
      method: 'get',
      params: { id: modelId, type: downloadType },
      responseType: 'blob'
    })

    if (blob instanceof Blob && blob.type && blob.type.includes('application/json')) {
      const text = await blob.text()
      let message = 'Failed to download model file'
      try {
        const payload = JSON.parse(text)
        message = payload?.message || payload?.msg || message
      } catch (parseError) {
        message = text || message
      }
      throw new Error(message)
    }

    const blobUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = fileName
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(blobUrl)

    ElMessage.success(`Download started${fileName}`)

  } catch (error) {
    console.error('下载模型文件失败：', error)
    ElMessage.error('下载模型文件失败：' + error.message)
  }
}

const handleExportModel = async (version) => {
  try {
    console.log('导出模型版本:', version)

    // 下载当前模型文件
    await downloadModelFile(currentModel.value)

  } catch (error) {
    ElMessage.error('导出模型文件失败：' + error.message)
  }
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  loadModelData()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadModelData()
}

// 高级搜索相关方法
const handleAdvancedSearch = (searchData) => {
  console.log('高级搜索:', searchData)
  
  // 更新搜索表单
  if (searchData.keyword) {
    searchForm.modelName = searchData.keyword
  }
  if (searchData.modelName) {
    searchForm.modelName = searchData.modelName
  }
  if (searchData.modelSource) {
    searchForm.modelSource = searchData.modelSource
  }
  if (searchData.modelType) {
    searchForm.modelType = searchData.modelType
  }
  if (searchData.dateRange && searchData.dateRange.length > 0) {
    searchForm.createTime = searchData.dateRange
  }
  
  handleSearch()
}

const handleAdvancedSearchReset = () => {
  console.log('重置高级搜索')
  searchForm.modelName = ''
  searchForm.modelSource = ''
  searchForm.modelType = ''
  searchForm.createTime = []
  handleReset()
}

const handleExport = () => {
  console.log('导出数据')
  ElMessage.success('导出数据')
}

const handleUpload = () => {
  console.log('上传文件')
  ElMessage.success('上传功能')
}

const handleDownloadTemplate = () => {
  console.log('下载模板')
  ElMessage.success('下载模板')
}

const handleBatchOperation = () => {
  console.log('批量操作')
  ElMessage.success('批量操作')
}

const handleModelDialogClose = () => {
  showModelDialog.value = false
  isEditingModel.value = false
  resetModelForm()
}

const handleSubmitModel = async () => {
  if (!modelFormRef.value) return
  try {
    await modelFormRef.value.validate()
    const accuracyValue = toNumberOrNull(modelForm.value.accuracy)
    const payload = {
      id: modelForm.value.id,
      modelName: modelForm.value.modelName,
      version: modelForm.value.version,
      modelFormat: modelForm.value.modelFormat,
      algorithmId: toNumberOrNull(modelForm.value.algorithmId),
      trainingId: toNumberOrNull(modelForm.value.trainingId),
      accuracy: accuracyValue
    }
    if (isEditingModel.value) {
      await updateModel(payload)
      ElMessage.success('编辑算法模型成功')
    } else {
      await createModel(payload)
      ElMessage.success('新增算法模型成功')
    }
    handleModelDialogClose()
    selectedRows.value = []
    await loadModelData()
  } catch (error) {
    const message = error?.message || '保存模型信息失败'
    ElMessage.error(message)
  }
}

// 页面挂载时加载数据
onMounted(() => {
  loadModelData()
})
</script>

<style scoped>
.algorithm-model {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

/* 搜索区域 */
.search-section {
  background: #F5F5F5;
  border-radius: 8px 8px 0 0;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.search-form {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.search-input {
  width: 200px;
}

/* 工具栏区域 */
.toolbar-section {
  background: white;
  border-radius: 0;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-top: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

/* 新增按钮自定义样式 */
.add-btn-custom {
  width: 82px !important;
  height: 36px !important;
  border-radius: 18px !important;
  background: #1A53FF !important;
  border-color: #1A53FF !important;
  padding: 0 !important;
  font-size: 14px;
  font-weight: 500;
  color: white !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
}

.add-btn-custom:hover,
.add-btn-custom:focus {
  background: #1A53FF !important;
  border-color: #1A53FF !important;
  color: white !important;
  opacity: 0.8;
}

.add-btn-custom:active {
  background: #1A53FF !important;
  border-color: #1A53FF !important;
  color: white !important;
  opacity: 0.9;
}

/* 编辑删除按钮组合 */
.edit-delete-group {
  display: flex;
  align-items: center;
  margin: 0;
  padding: 0;
}

.edit-delete-group .el-button + .el-button {
  margin-left: 0 !important;
}

/* 编辑按钮自定义样式 */
.edit-btn-custom {
  height: 36px !important;
  border-radius: 18px 0 0 18px !important;
  border-right: none !important;
  padding: 0 16px !important;
  font-size: 14px;
  font-weight: 500;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
  margin-right: 0 !important;
  border-color: #d9d9d9 !important;
}

.edit-btn-custom:hover,
.edit-btn-custom:focus {
  border-right: none !important;
  border-color: #409eff !important;
}

.edit-btn-custom:disabled {
  border-right: none !important;
  border-color: #e4e7ed !important;
}

/* 删除按钮自定义样式 */
.delete-btn-custom {
  height: 36px !important;
  border-radius: 0 18px 18px 0 !important;
  border-left: none !important;
  padding: 0 16px !important;
  font-size: 14px;
  font-weight: 500;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
  background: white !important;
  color: #f56c6c !important;
  border-color: #d9d9d9 !important;
  margin-left: 0 !important;
}

.delete-btn-custom:hover,
.delete-btn-custom:focus {
  border-left: none !important;
  background: white !important;
  color: #f56c6c !important;
  border-color: #f56c6c !important;
}

.delete-btn-custom:active {
  background: white !important;
  color: #f56c6c !important;
  border-color: #f56c6c !important;
  border-left: none !important;
}

.delete-btn-custom:disabled {
  border-left: none !important;
  background: #f5f5f5 !important;
  color: #c0c4cc !important;
  border-color: #e4e7ed !important;
}

/* 表格区域 */
.table-section {
  background: white;
  border-radius: 0;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  border-top: 1px solid #e8e8e8;
}

:deep(.el-table) {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  width: 100%;
}

:deep(.el-table__fixed-right) {
  border-left: 1px solid #ebeef5;
}

:deep(.el-table th) {
  background-color: #fafafa;
  font-weight: 600;
  color: #262626;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-table .el-table__row:hover > td) {
  background-color: #f5f7fa;
}

:deep(.el-table td) {
  border-bottom: 1px solid #ebeef5;
}

/* 操作按钮强制一行展示 */
.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  flex-wrap: nowrap;
}

/* 分页区域 */
.pagination-section {
  display: flex;
  justify-content: center;
  background: white;
  border-radius: 0 0 8px 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-top: 1px solid #e8e8e8;
}

/* 详情视图样式 */
.detail-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* 面包屑导航 */
.breadcrumb-section {
  background: white;
  border-radius: 8px 8px 0 0;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.breadcrumb-item {
  color: #409eff;
  cursor: pointer;
  transition: color 0.3s;
}

.breadcrumb-item:hover {
  color: #337ecc;
}

.breadcrumb-item.active {
  color: #262626;
  cursor: default;
}

.breadcrumb-separator {
  color: #999;
}

/* 基础信息区域 */
.detail-info-section {
  background: white;
  border-radius: 0;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-top: 1px solid #e8e8e8;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  max-width: 800px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.info-label {
  font-size: 14px;
  color: #666;
  min-width: 80px;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

/* 版本区域 */
.version-section {
  background: white;
  border-radius: 0 0 8px 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-top: 1px solid #e8e8e8;
  flex: 1;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 20px 0;
}

.version-table {
  margin-top: 16px;
}

.version-table :deep(.el-table) {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  width: 100%;
}

.version-table :deep(.el-table th) {
  background-color: #fafafa;
  font-weight: 600;
  color: #262626;
  border-bottom: 1px solid #ebeef5;
}

.version-table :deep(.el-table .el-table__row:hover > td) {
  background-color: #f5f7fa;
}

.version-table :deep(.el-table td) {
  border-bottom: 1px solid #ebeef5;
}
</style> 
