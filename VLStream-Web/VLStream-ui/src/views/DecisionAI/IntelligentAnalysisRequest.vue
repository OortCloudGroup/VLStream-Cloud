<template>
  <div class="intelligent-analysis-request tenant_Page draHeaPB">
    <div class="tenant_content">
      <div v-if="!showEditView" class="tableTenBox flexRowAC">
        <div class="tableTenItU">
          <div class="depNameBox_out flexRowAC">
            <div class="depNameBox flexRowAC">
              <div class="exportBtnBox flexRowAC">
                <button type="button" class="exportBtn newBtn flexRowAC" @click="handleAdd">
                  <el-icon class="BtnImg">
                    <Plus />
                  </el-icon>
                  申请
                </button>
                <button-group :button-list="toolbarButtonList" />
              </div>
            </div>
            <div class="searchHeight_out flexRowAC">
              <search-height-box
                keyword="keyword"
                placeholder="搜索"
                :data="searchData"
                @handle="searchResetFn"
              />
              <export-excel-pdf :item="exportItem" @handle="handleExport" />
            </div>
          </div>

          <TableSelf
            class="new_table"
            header-cell-class-name="header_tenant_cell"
            stripe
            :data="tableData"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" :width="clacPXToVW(55)" />
            <el-table-column prop="sequence" label="序号" :width="clacPXToVW(80)" align="center" />
            <el-table-column prop="analysisName" label="分析名称" />
            <el-table-column prop="analysisType" label="分析类型" />
            <el-table-column prop="videoThumbnail" label="抓拍截图" align="center">
              <template #default="scope">
                <div class="thumbnail-container">
                  <img :src="scope.row.videoThumbnail" :alt="scope.row.analysisName" class="video-thumbnail" />
                  <div class="thumbnail-overlay">+5</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="region" label="区域" />
            <el-table-column prop="cameras" label="摄像头" min-width="200" />
            <el-table-column prop="analysisStatus" label="分析状态" align="center">
              <template #default="scope">
                <el-tag
                  :type="getStatusTagType(scope.row.analysisStatus)"
                  size="small"
                >
                  {{ scope.row.analysisStatus }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="analysisTime" label="分析时间" />
            <el-table-column label="操作" :width="clacPXToVW(100)" fixed="right" align="right">
              <template #default="scope">
                <div class="operateAppBox flexRowAC" @click.stop>
                  <div class="new_table_svg_group" @click="handlePlay(scope.row)">
                    <oort-svg-icon width="20" height="20" name="play" class="new_table_svg_group_svg" />
                    <span>播放</span>
                  </div>
                </div>
              </template>
            </el-table-column>
          </TableSelf>

          <div class="paginationBox flexRowAC">
            <el-pagination
              background
              v-model:current-page="pagination.currentPage"
              v-model:page-size="pagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="pagination.total"
              layout="total, prev, pager, next, sizes"
              class="justifyAlign"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </div>

    <!-- 编辑视图 -->
    <div v-if="showEditView" class="edit-view">
      <!-- 面包屑导航 -->
      <div class="breadcrumb-nav">
        <span class="breadcrumb-item" @click="showListView">场景列表</span>
        <span class="breadcrumb-separator">></span>
        <span class="breadcrumb-item active">{{ editTitle }}</span>
      </div>

      <!-- 编辑内容 -->
      <div class="edit-content">
        <el-form :model="form" class="analysis-form" label-width="120px">
          <el-form-item label="场景名称" prop="sceneName">
            <el-input 
              v-model="form.sceneName" 
              placeholder="请输入场景名称"
              style="width: 480px"
            />
          </el-form-item>

          <el-form-item label="分析类型" prop="analysisType">
            <div class="selector-item" @click="openAnalysisTypeSelector" style="width: 480px">
              <span class="selector-text">{{ form.analysisType || '请选择' }}</span>
              <el-icon class="selector-arrow"><ArrowRight /></el-icon>
            </div>
          </el-form-item>

          <el-form-item label="抓拍截图上传" prop="screenshots">
            <div class="upload-section" style="width: 480px">
              <div class="upload-header">
                <span class="upload-count">({{ uploadedFiles.length }}/5)</span>
              </div>
              <div class="upload-grid">
                <div 
                  v-for="(file, index) in uploadedFiles" 
                  :key="index" 
                  class="upload-item"
                >
                  <img :src="file.url" :alt="file.name" class="upload-image" />
                  <el-icon class="delete-icon" @click="removeFile(index)"><Close /></el-icon>
                </div>
                <div v-if="uploadedFiles.length < 5" class="upload-placeholder" @click="triggerUpload">
                  <el-icon class="upload-plus"><Plus /></el-icon>
                </div>
              </div>
              <input 
                ref="fileInput" 
                type="file" 
                multiple 
                accept="image/*" 
                style="display: none" 
                @change="handleFileUpload"
              />
            </div>
          </el-form-item>

          <el-form-item label="选择区域" prop="selectedRegions">
            <div class="selector-container" style="width: 480px">
              <div class="tag-list">
                <el-tag 
                  v-for="region in form.selectedRegions" 
                  :key="region" 
                  closable 
                  @close="removeRegion(region)"
                  class="region-tag"
                >
                  {{ region }}
                </el-tag>
              </div>
              <el-icon class="selector-arrow" @click="openRegionSelector"><ArrowRight /></el-icon>
            </div>
          </el-form-item>

          <el-form-item label="选择摄像头" prop="selectedCameras">
            <div class="selector-container" style="width: 480px">
              <div class="tag-list">
                <el-tag 
                  v-for="camera in form.selectedCameras" 
                  :key="camera" 
                  closable 
                  @close="removeCamera(camera)"
                  class="camera-tag"
                >
                  {{ camera }}
                </el-tag>
              </div>
              <el-icon class="selector-arrow" @click="openCameraSelector"><ArrowRight /></el-icon>
            </div>
          </el-form-item>

          <el-form-item label="选择时间段" prop="timeRange">
            <div class="time-range-container" style="width: 480px">
              <DateRangePicker
                v-model="form.timeRange"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                format="MM/DD HH:mm"
                value-format="MM/DD HH:mm"
                width="440px"
              />
              <el-icon class="time-icon"><Clock /></el-icon>
            </div>
          </el-form-item>

          <!-- 操作按钮 -->
          <div class="form-actions">
            <el-button type="primary" @click="handleSubmit" style="width: 120px">申请</el-button>
            <el-button @click="showListView" style="width: 120px">取消</el-button>
          </div>
        </el-form>

        <!-- 分析类型选择对话框 -->
        <el-dialog v-model="showAnalysisTypeSelector" title="选择分析类型" width="25%">
          <el-radio-group v-model="tempAnalysisType" direction="vertical">
            <el-radio value="人员">人员</el-radio>
            <el-radio value="车辆">车辆</el-radio>
            <el-radio value="物体">物体</el-radio>
            <el-radio value="行为">行为</el-radio>
          </el-radio-group>
          <template #footer>
            <div class="dialog-footer">
              <el-button @click="showAnalysisTypeSelector = false">取消</el-button>
              <el-button type="primary" @click="confirmAnalysisType">确定</el-button>
            </div>
          </template>
        </el-dialog>

        <!-- 区域选择对话框 -->
        <el-dialog v-model="showRegionSelector" title="选择区域" width="25%">
          <el-checkbox-group v-model="tempSelectedRegions">
            <div class="checkbox-grid">
              <el-checkbox value="水产大厦">水产大厦</el-checkbox>
              <el-checkbox value="洪湖大厦">洪湖大厦</el-checkbox>
              <el-checkbox value="教学楼A">教学楼A</el-checkbox>
              <el-checkbox value="教学楼B">教学楼B</el-checkbox>
              <el-checkbox value="图书馆">图书馆</el-checkbox>
              <el-checkbox value="体育馆">体育馆</el-checkbox>
            </div>
          </el-checkbox-group>
          <template #footer>
            <div class="dialog-footer">
              <el-button @click="showRegionSelector = false">取消</el-button>
              <el-button type="primary" @click="confirmRegionSelection">确定</el-button>
            </div>
          </template>
        </el-dialog>

        <!-- 摄像头选择对话框 -->
        <el-dialog v-model="showCameraSelector" title="选择摄像头" width="25%">
          <el-checkbox-group v-model="tempSelectedCameras">
            <div class="checkbox-grid">
              <el-checkbox value="摄像头1">摄像头1</el-checkbox>
              <el-checkbox value="摄像头2">摄像头2</el-checkbox>
              <el-checkbox value="摄像头3">摄像头3</el-checkbox>
              <el-checkbox value="摄像头4">摄像头4</el-checkbox>
              <el-checkbox value="摄像头5">摄像头5</el-checkbox>
              <el-checkbox value="摄像头6">摄像头6</el-checkbox>
            </div>
          </el-checkbox-group>
          <template #footer>
            <div class="dialog-footer">
              <el-button @click="showCameraSelector = false">取消</el-button>
              <el-button type="primary" @click="confirmCameraSelection">确定</el-button>
            </div>
          </template>
        </el-dialog>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, ArrowRight, Close, Clock, MessageBox } from '@element-plus/icons-vue'
import DateRangePicker from '@/components/DateRangePicker.vue'
import { clacPXToVW } from '@/utils/index'
import { getAnalysisRequestPage, applyAnalysisRequest, updateAnalysisRequest } from '@/api/analysisRequest'

const router = useRouter()

// 搜索表单
const searchForm = reactive({
  analysisName: '',
  analysisType: '',
  analysisStep: '',
  createDateRange: []
})

// 分页配置
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 选中的行
const selectedRows = ref([])

// 视图控制
const showEditView = ref(false)
const editingItem = ref(null)
const editTitle = ref('编辑场景')

// 编辑表单数据
const form = ref({
  sceneName: '',
  analysisType: '',
  selectedRegions: [],
  selectedCameras: [],
  timeRange: ''
})

// 对话框控制
const showAnalysisTypeSelector = ref(false)
const showRegionSelector = ref(false)
const showCameraSelector = ref(false)

// 临时选择数据
const tempAnalysisType = ref('')
const tempSelectedRegions = ref([])
const tempSelectedCameras = ref([])

// 文件上传
const fileInput = ref(null)
const uploadedFiles = ref([])

// 占位图片数据URL
const placeholderImage80x60 = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iNjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iI2Y1ZjVmNSIvPjxwYXRoIGQ9Ik0zNSAzMGMwIDUuNDE4IDMuMDgyIDEwIDcgMTBzNy00LjU4MiA3LTEwUzQ2LjQxOCAyMCA0MiAyMCAzNSAyNC41ODIgMzUgMzB6bTIwIDIwTDUwIDQ1bC01LTUtMTAgMTB6IiBmaWxsPSIjZGRkIi8+PHRleHQgeD0iNDAiIHk9IjUwIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iOCIgZmlsbD0iIzk5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSI+ODAgeCAwPC90ZXh0Pjwvc3ZnPg=='

// 表格数据
// 表格仅由后端真实记录填充，不展示历史静态样例。
const tableData = ref([])

// 方法
const handleSearch = () => {
  console.log('搜索:', searchForm)
}

const handleReset = () => {
  searchForm.analysisName = ''
  searchForm.analysisType = ''
  searchForm.analysisStep = ''
  searchForm.createDateRange = []
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
}

const handleAdd = () => {
  editingItem.value = null
  form.value = {
    sceneName: '',
    analysisType: '',
    selectedRegions: [],
    selectedCameras: [],
    timeRange: ''
  }
  editTitle.value = '智能分析申请'
  showEditView.value = true
}

const handleEdit = () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请选择要编辑的记录')
    return
  }
  
  const row = selectedRows.value[0]
  editingItem.value = row
  form.value = {
    sceneName: '告警场景',
    analysisType: '人员',
    selectedRegions: ['水产大厦', '洪湖大厦'],
    selectedCameras: ['摄像头1', '摄像头2'],
    timeRange: ['08/17 10:20', '08/17 12:20']
  }
  editTitle.value = '编辑场景'
  showEditView.value = true
}

const handleDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请选择要删除的记录')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 条记录吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    ElMessage.success('删除成功')
    
  } catch {
    // 用户取消删除
  }
}

const toolbarButtonList = [
  { name: '编辑', svg: 'table_edit', clickFn: handleEdit },
  { name: '删除', svg: 'table_del', clickFn: handleDelete },
]

const handlePlay = (row) => {
  console.log('播放分析视频:', row.analysisName)
  ElMessage.success(`开始播放 ${row.analysisName} 的分析视频`)
}

const getStatusTagType = (status) => {
  const statusMap = {
    '完成': 'success',
    '分析中': 'warning',
    '失效': 'info'
  }
  return statusMap[status] || 'info'
}

// 返回列表页面
const showListView = () => {
  showEditView.value = false
  resetForm()
}

// 重置表单
const resetForm = () => {
  form.value = {
    sceneName: '',
    analysisType: '',
    selectedRegions: [],
    selectedCameras: [],
    timeRange: ''
  }
  editingItem.value = null
}

// 从真实分析请求表加载列表，失败时保持空数据并显示原因。
const loadAnalysisRequests = async () => {
  try {
    const response = await getAnalysisRequestPage({
      current: pagination.currentPage,
      size: pagination.pageSize,
      analysisName: searchForm.analysisName,
      analysisType: searchForm.analysisType
    })
    if (response?.code !== 200) throw new Error(response?.msg || response?.message || '加载失败')
    const records = response?.data?.records || []
    tableData.value = records.map((item, index) => ({
      ...item,
      sequence: (pagination.currentPage - 1) * pagination.pageSize + index + 1,
      name: item.analysisName,
      deviceInfo: item.cameraName || item.deviceIds || '-',
      status: item.requestStatus,
      videoThumbnail: item.resultPath || null
    }))
    pagination.total = Number(response?.data?.total || 0)
  } catch (error) {
    tableData.value = []
    pagination.total = 0
    ElMessage.error(`加载分析请求失败：${error.message || error}`)
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!form.value.sceneName.trim()) {
    ElMessage.warning('请输入场景名称')
    return
  }

  const payload = {
    id: editingItem.value?.id,
    analysisName: form.value.sceneName,
    analysisType: form.value.analysisType,
    deviceIds: (form.value.selectedCameras || []).join(','),
    regionInfo: (form.value.selectedRegions || []).join(','),
    timeRange: form.value.timeRange,
    images: uploadedFiles.value.map(file => file.url).join(','),
    description: form.value.description || '智能分析请求'
  }
  try {
    const response = editingItem.value
      ? await updateAnalysisRequest(payload)
      : await applyAnalysisRequest(payload)
    if (response?.code !== 200) throw new Error(response?.msg || response?.message || '操作失败')
    await loadAnalysisRequests()
    showEditView.value = false
    selectedRows.value = []
    ElMessage.success(editingItem.value ? '更新成功' : '申请已真实入库')
  } catch (error) {
    ElMessage.error(`分析请求提交失败：${error.message || error}`)
  }
}

// 文件上传相关方法
const triggerUpload = () => {
  fileInput.value?.click()
}

const handleFileUpload = (event) => {
  const files = Array.from(event.target.files)
  files.forEach(file => {
    if (uploadedFiles.value.length < 5) {
      const reader = new FileReader()
      reader.onload = (e) => {
        uploadedFiles.value.push({
          name: file.name,
          url: e.target.result
        })
      }
      reader.readAsDataURL(file)
    }
  })
  event.target.value = ''
}

const removeFile = (index) => {
  uploadedFiles.value.splice(index, 1)
}

onMounted(loadAnalysisRequests)

// 标签删除方法
const removeRegion = (region) => {
  const index = form.value.selectedRegions.indexOf(region)
  if (index > -1) {
    form.value.selectedRegions.splice(index, 1)
  }
}

const removeCamera = (camera) => {
  const index = form.value.selectedCameras.indexOf(camera)
  if (index > -1) {
    form.value.selectedCameras.splice(index, 1)
  }
}

// 打开对话框方法
const openAnalysisTypeSelector = () => {
  tempAnalysisType.value = form.value.analysisType
  showAnalysisTypeSelector.value = true
}

const openRegionSelector = () => {
  tempSelectedRegions.value = [...form.value.selectedRegions]
  showRegionSelector.value = true
}

const openCameraSelector = () => {
  tempSelectedCameras.value = [...form.value.selectedCameras]
  showCameraSelector.value = true
}

// 对话框确认方法
const confirmAnalysisType = () => {
  form.value.analysisType = tempAnalysisType.value
  showAnalysisTypeSelector.value = false
}

const confirmRegionSelection = () => {
  form.value.selectedRegions = [...tempSelectedRegions.value]
  showRegionSelector.value = false
}

const confirmCameraSelection = () => {
  form.value.selectedCameras = [...tempSelectedCameras.value]
  showCameraSelector.value = false
}

const exportItem = ref({ isDisabledExcel: false })
const searchData = ref([
  { label: '关键词', value: 'keyword', type: 'text', default: '' }
])

const searchResetFn = (val, reset) => {
  if (reset && !(val && val.keyword)) {
    handleAdvancedSearchReset()
    return
  }
  handleAdvancedSearch(val || {})
}

// 高级搜索相关方法
const handleAdvancedSearch = (searchData) => {
  console.log('高级搜索:', searchData)
  
  // 更新搜索表单
  if (searchData.keyword) {
    searchForm.analysisName = searchData.keyword
  }
  if (searchData.deviceName) {
    searchForm.analysisName = searchData.deviceName
  }
  if (searchData.deviceId) {
    searchForm.analysisName = searchData.deviceId
  }
  if (searchData.selectedTags && searchData.selectedTags.length > 0) {
    searchForm.analysisType = searchData.selectedTags[0]
  }
  if (searchData.dateRange && searchData.dateRange.length > 0) {
    searchForm.createDateRange = searchData.dateRange
  }
  
  ElMessage.success('高级搜索完成')
}

const handleAdvancedSearchReset = () => {
  console.log('重置高级搜索')
  searchForm.analysisName = ''
  searchForm.analysisType = ''
  searchForm.analysisStep = ''
  searchForm.createDateRange = []
  ElMessage.info('已重置高级搜索条件')
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


</script>

<style scoped lang="scss">
.tenant_Page {
  height: 100%;
  width: 100%;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
  background: #f0f2f5;
  .tenant_content { width: 100%; height: 100%; border-radius: 8px; }
  .tableTenBox {
    padding: 20px;
    width: 100%;
    height: 100%;
    border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
    flex: 1;
    background: #fff;
    align-items: flex-start;
  }
}
.tableTenItU {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: auto;
  :deep(.header_tenant_cell) { background: #F8F8F9; }
}
.paginationBox { justify-content: center; height: 100px; }
.operateAppBox { justify-content: flex-end; gap: 2px; }

.intelligent-analysis-request {
  height: 100%;
}

/* 查询栏 - 无缝连接设计 */
.query-bar {
  background: #F0F2F5;
  border-radius: 8px 8px 0 0;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.search-form {
  width: 100%;
}

.search-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: nowrap;
  overflow-x: auto;
  min-width: 1000px;
}

.search-item {
  flex: 1;
  min-width: 200px;
  max-width: 280px;
}

.search-item:nth-child(4) {
  width: auto;
  flex: none;
}

.date-with-buttons {
  display: flex;
  align-items: center;
  gap: 12px;
}



.search-buttons {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

/* 视频缩略图样式 */
.thumbnail-container {
  position: relative;
  display: inline-block;
}

.video-thumbnail {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.thumbnail-overlay {
  position: absolute;
  top: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 2px;
}

/* 主题色更新 */
:deep(.el-button--primary) {
  background-color: #1A53FF;
  border-color: #1A53FF;
}

:deep(.el-button--primary:hover) {
  background-color: #3d70ff;
  border-color: #3d70ff;
}

/* 编辑视图样式 */
.edit-view {
  height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 0;
  background-color: #f5f7fa;
  padding: 20px;
  overflow: hidden;
}

/* 面包屑导航 */
.breadcrumb-nav {
  background: #F0F2F5;
  border-radius: 8px 8px 0 0;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.breadcrumb-item {
  color: #606266;
  cursor: pointer;
  font-size: 14px;
}

.breadcrumb-item:hover {
  color: #1A53FF;
}

.breadcrumb-item.active {
  color: #303133;
  font-weight: 500;
}

.breadcrumb-separator {
  color: #c0c4cc;
  margin: 0 8px;
}

/* 编辑内容区域 */
.edit-content {
  flex: 1;
  background: white;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
  overflow-y: auto;
}

.analysis-form {
  max-width: 600px;
}

/* 表单字段宽度480px */
.analysis-form .el-input,
.analysis-form .el-select {
  width: 480px;
}

/* 操作按钮 */
.form-actions {
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 12px;
}

/* 选择器样式 */
.selector-item {
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  background: white;
  transition: border-color 0.2s;
}

.selector-item:hover {
  border-color: #1A53FF;
}

.selector-text {
  color: #606266;
  font-size: 14px;
}

.selector-arrow {
  color: #c0c4cc;
  font-size: 14px;
}

/* 标签容器样式 */
.selector-container {
  min-height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 8px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: white;
}

.tag-list {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  flex: 1;
}

.region-tag,
.camera-tag {
  background: #ecf5ff;
  border-color: #b3d8ff;
  color: #409eff;
}

/* 文件上传样式 */
.upload-section {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 16px;
  background: white;
}

.upload-header {
  margin-bottom: 12px;
}

.upload-count {
  color: #909399;
  font-size: 14px;
}

.upload-grid {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.upload-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
}

.upload-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.delete-icon {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 12px;
}

.upload-placeholder {
  width: 80px;
  height: 80px;
  border: 2px dashed #c0c4cc;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.2s;
}

.upload-placeholder:hover {
  border-color: #1A53FF;
}

.upload-plus {
  font-size: 24px;
  color: #c0c4cc;
}

/* 时间选择器容器 */
.time-range-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.time-icon {
  color: #c0c4cc;
  font-size: 16px;
}

/* 对话框样式 */
.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style> 
