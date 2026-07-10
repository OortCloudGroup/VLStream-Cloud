<template>
  <div class="event-management">
    <!-- 详情页面视图 -->
    <div v-if="showDetailView" class="detail-view">
      <div class="detail-header">
        <div class="breadcrumb-nav">
          <span class="breadcrumb-item" @click="showDetailView = false">事件列表</span>
          <span class="breadcrumb-separator">></span>
          <span class="breadcrumb-item active">事件详情</span>
        </div>
      </div>

      <div class="detail-content">
        <!-- 事件详情 -->
        <div class="detail-section">
          <h2 class="section-title">事件详情</h2>

          <!-- 事件描述 -->
          <div class="detail-item">
            <div class="detail-icon">
              <el-icon>
                <Document/>
              </el-icon>
            </div>
            <div class="detail-content-wrapper">
              <span class="detail-label">事件描述</span>
              <div class="detail-value">
                <p class="event-description">
                  {{ selectedEvent?.eventDesc || '暂无事件描述' }}
                </p>
                <el-tag :type="getStatusType(selectedEvent?.status)" size="small" class="status-tag">
                  {{ selectedEvent?.status }}
                </el-tag>
              </div>
            </div>
          </div>

          <div class="detail-item">
            <div class="detail-icon">
              <span class="icon-text">ID</span>
            </div>
            <div class="detail-content-wrapper">
              <span class="detail-label">事件ID</span>
              <span class="detail-value">{{ selectedEvent?.id || '--' }}</span>
            </div>
          </div>

          <!-- 上报设备 -->
          <div class="detail-item">
            <div class="detail-icon">
              <el-icon>
                <Monitor/>
              </el-icon>
            </div>
            <div class="detail-content-wrapper">
              <span class="detail-label">上报设备</span>
              <span class="detail-value">{{ selectedEvent?.reportDevice || '--' }}</span>
            </div>
          </div>

          <div class="detail-item">
            <div class="detail-icon">
              <el-icon>
                <Picture/>
              </el-icon>
            </div>
            <div class="detail-content-wrapper">
              <span class="detail-label">上报图片</span>
              <div class="detail-value">
                <div class="report-images">
                  <div v-if="selectedEvent?.reportImg" class="image-item">
                    <el-image
                      class="preview-image"
                      :src="selectedEvent.reportImg"
                      fit="cover"
                      :preview-src-list="[selectedEvent.reportImg]"
                      :hide-on-click-modal="true"
                      preview-teleported
                      lazy
                    />
                  </div>
                  <span v-else class="no-image">暂无图片</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 上报时间 -->
          <div class="detail-item">
            <div class="detail-icon">
              <el-icon>
                <Clock/>
              </el-icon>
            </div>
            <div class="detail-content-wrapper">
              <span class="detail-label">上报时间</span>
              <span class="detail-value">{{ selectedEvent?.reportTime || '--' }}</span>
            </div>
          </div>

          <div class="detail-item">
            <div class="detail-icon">
              <el-icon>
                <Location/>
              </el-icon>
            </div>
            <div class="detail-content-wrapper">
              <span class="detail-label">上报位置</span>
              <span class="detail-value">{{ selectedEvent?.reportLocation || '--' }}</span>
            </div>
          </div>

          <div class="detail-item">
            <div class="detail-icon">
              <el-icon>
                <Document/>
              </el-icon>
            </div>
            <div class="detail-content-wrapper">
              <span class="detail-label">事件数据</span>
              <div class="detail-value event-data-block">
                <pre v-if="selectedEvent?.eventData" class="event-data">{{ selectedEvent.eventData }}</pre>
                <span v-else>--</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 事件反馈 -->
        <div class="feedback-section">
          <h2 class="section-title">事件反馈</h2>

          <!-- 反馈描述 -->
          <div class="detail-item">
            <div class="detail-icon">
              <el-icon>
                <Document/>
              </el-icon>
            </div>
            <div class="detail-content-wrapper">
              <span class="detail-label">反馈描述</span>
              <div class="detail-value">
                <el-input
                    v-model="feedbackForm.description"
                    type="textarea"
                    placeholder="请输入"
                    :rows="4"
                    maxlength="500"
                    show-word-limit
                    class="feedback-textarea"
                    :autosize="false"
                />
              </div>
            </div>
          </div>

          <!-- 上传图像 -->
          <div class="detail-item">
            <div class="detail-icon">
              <el-icon>
                <Picture/>
              </el-icon>
            </div>
            <div class="detail-content-wrapper">
              <span class="detail-label">上传图像</span>
              <div class="detail-value">
                <div class="upload-area">
                  <el-upload
                      class="image-uploader"
                      :show-file-list="false"
                      :before-upload="beforeUpload"
                      :on-success="handleUploadSuccess"
                      action="/api/upload"
                  >
                    <div v-if="!feedbackForm.uploadImage" class="upload-placeholder">
                      <el-icon class="upload-icon">
                        <Plus/>
                      </el-icon>
                    </div>
                    <img v-else :src="feedbackForm.uploadImage" class="uploaded-image"/>
                  </el-upload>
                </div>
              </div>
            </div>
          </div>

          <!-- 事件状态 -->
          <div class="detail-item">
            <div class="detail-icon">
              <el-icon>
                <CircleCheck/>
              </el-icon>
            </div>
            <div class="detail-content-wrapper">
              <span class="detail-label">事件状态</span>
              <div class="detail-value">
                <el-radio-group v-model="feedbackForm.status" class="status-radio-group">
                  <el-radio label="未完成">未完成</el-radio>
                  <el-radio label="已完成">已完成</el-radio>
                </el-radio-group>
              </div>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button type="primary" size="large" @click="handleSubmitFeedback">
            确定
          </el-button>
          <el-button size="large" @click="showDetailView = false">
            取消
          </el-button>
        </div>
      </div>
    </div>

    <!-- 列表页面视图 -->
    <div v-else class="list-view">
      <!-- 查询栏 -->
      <div class="query-bar">
      </div>

      <!-- 主内容区 -->
      <div class="main-content">
        <!-- 操作按钮区域 -->
        <div class="action-section">
          <div class="action-button-group">
            <el-button
                class="export-events-btn"
                @click="handleExportEvents"
            >
              导出事件列表
            </el-button>

            <!-- 高级搜索组件 -->
            <div class="advanced-search-group">
              <AdvancedSearch
                  @search="handleAdvancedSearch"
                  @reset="handleAdvancedSearchReset"
                  @export="handleExportEvents"
                  @upload="handleUpload"
                  @template="handleDownloadTemplate"
                  @batch="handleBatchOperation"
              />
            </div>
          </div>
        </div>

        <!-- 表格内容 -->
        <div class="table-content">
          <el-table
              :data="paginatedData"
              stripe
              @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55"/>
            <el-table-column type="index" label="序号" width="80"/>
            <el-table-column prop="eventDesc" label="事件描述" min-width="150"/>
            <el-table-column prop="reportLocation" label="上报位置" min-width="200"/>
            <el-table-column prop="reportDevice" label="上报设备" min-width="120"/>
            <el-table-column prop="reportTime" label="上报时间" min-width="160"/>
            <el-table-column prop="reportImg" label="上报图片" min-width="100">
              <template #default="scope">
                <el-image
                  v-if="scope.row.reportImg"
                  class="table-report-img"
                  :src="scope.row.reportImg"
                  fit="cover"
                  :preview-src-list="[scope.row.reportImg]"
                  :hide-on-click-modal="true"
                  preview-teleported
                  lazy
                />
                <span v-else class="no-image">--</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="执行状态" min-width="100">
              <template #default="scope">
                <el-tag
                    :type="getStatusType(scope.row.status)"
                    size="small">
                  {{ scope.row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="executor" label="执行人" min-width="150">
              <template #default="scope">
                <div v-if="scope.row.executor" class="executor-info">
                  <div class="executor-avatars">
                    <img
                        v-for="(avatar, index) in scope.row.executorAvatars"
                        :key="index"
                        :src="avatar"
                        class="executor-avatar"
                        :style="{ zIndex: scope.row.executorAvatars.length - index }"
                    />
                  </div>
                  <span class="executor-names">{{ scope.row.executor }}</span>
                </div>
                <span v-else class="no-executor">暂无执行人</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240" fixed="right" align="right">
              <template #default="scope">
                <el-button
                    class="action-btn"
                    @click="handleAssignExecutor(scope.row)">
                  选择执行人
                </el-button>
                <el-button
                    class="action-btn"
                    @click="handleDetail(scope.row)">
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 - 紧贴表格数据 -->
          <div class="table-pagination">
            <el-pagination
                v-model:current-page="pagination.currentPage"
                v-model:page-size="pagination.pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="pagination.total"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"/>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Document, Monitor, Location, Clock, User, Picture, Plus, CircleCheck} from '@element-plus/icons-vue'
import AdvancedSearch from '@/components/AdvancedSearch.vue'
import {
  batchDeleteEvents,
  deleteEvent,
  getEventById,
  getEventPage,
  updateEventStatus
} from '@/api/eventManagement'

const router = useRouter()

// 当前激活的标签
const activeTab = ref('maintenance')

// 选中的行数据
const selectedRows = ref([])

// 视图控制
const showDetailView = ref(false)
const selectedEvent = ref(null)

// 反馈表单
const feedbackForm = ref({
  description: '',
  uploadImage: '',
  status: '未完成'
})

// 搜索表单
const searchForm = reactive({
  eventName: '',
  dateRange: []
})

// 分页配置
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 加载态
const loading = ref(false)

// 表格数据
const tableData = ref([])

// 分页数据（后端已分页）
const paginatedData = computed(() => tableData.value)

const statusLabelMap = {
  pending: '待处理',
  processing: '处理中',
  completed: '已完成',
  closed: '已关闭'
}

const statusCodeMap = {
  未完成: 'processing',
  待处理: 'pending',
  处理中: 'processing',
  已完成: 'completed',
  已关闭: 'closed'
}

const isRequestSuccess = (res) => res?.code === 200 || res?.success === true

const formatDateTime = (value) => {
  if (!value) return ''
  if (typeof value === 'string') {
    return value.replace('T', ' ').split('.')[0]
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n) => (n < 10 ? `0${n}` : n)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const normalizeEvent = (item) => ({
  id: item.id,
  eventDesc: item.eventDesc,
  reportLocation: item.reportLocation,
  reportDevice: item.reportDevice,
  reportTime: formatDateTime(item.reportTime),
  reportImg: item.reportImg,
  eventData: item.eventData,
  status: statusLabelMap[item.eventStatus] || item.eventStatus || '待处理',
  eventStatus: item.eventStatus,
  executor: item.executor,
  executorAvatars: []
})

const loadEvents = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.currentPage,
      size: pagination.pageSize
    }
    if (searchForm.eventName) params.keyword = searchForm.eventName
    if (searchForm.dateRange?.length === 2) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }

    const res = await getEventPage(params)
    if (!isRequestSuccess(res)) {
      ElMessage.error(res?.message || '加载事件列表失败')
      return
    }

    const pageData = res.data || {}
    tableData.value = (pageData.records || []).map(normalizeEvent)
    pagination.total = pageData.total || 0
    pagination.currentPage = pageData.current || pagination.currentPage
    pagination.pageSize = pageData.size || pagination.pageSize
  } catch (error) {
    console.error('加载事件列表失败', error)
    ElMessage.error('加载事件列表失败')
  } finally {
    loading.value = false
  }
}

// 方法
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadEvents()
}

const handleReset = () => {
  searchForm.eventName = ''
  searchForm.dateRange = []
  pagination.currentPage = 1
  loadEvents()
}

const handleExportEvents = () => {
  ElMessage.success('正在导出事件列表...')
  // 根据需要对接后端导出接口
}

const handleDelete = async () => {
  if (!selectedRows.value || selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要删除的事件')
    return
  }

  try {
    const message = selectedRows.value.length === 1
        ? `确定要删除事件 "${selectedRows.value[0].eventDesc}" 吗？`
        : `确定要删除选中的 ${selectedRows.value.length} 个事件吗？`

    await ElMessageBox.confirm(
        message,
        '确认删除',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    if (selectedRows.value.length === 1) {
      const res = await deleteEvent(selectedRows.value[0].id)
      if (!isRequestSuccess(res)) {
        ElMessage.error('删除失败')
        return
      }
    } else {
      const ids = selectedRows.value.map((row) => row.id)
      const res = await batchDeleteEvents(ids)
      if (!isRequestSuccess(res)) {
        ElMessage.error('批量删除失败')
        return
      }
    }

    ElMessage.success('删除成功')
    selectedRows.value = []
    loadEvents()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败', error)
      ElMessage.info('已取消删除')
    }
  }
}

const handleDetail = async (row) => {
  try {
    const res = await getEventById(row.id)
    if (isRequestSuccess(res)) {
      const data = res.data || {}
      selectedEvent.value = {
        ...normalizeEvent(data),
        ...data
      }
      selectedEvent.value.reportTime = formatDateTime(data.reportTime)
      selectedEvent.value.createTime = formatDateTime(data.createTime)
      selectedEvent.value.updateTime = formatDateTime(data.updateTime)
    } else {
      selectedEvent.value = {
        ...row,
        reportTime: formatDateTime(row.reportTime)
      }
    }
  } catch (error) {
    console.error('加载事件详情失败', error)
    selectedEvent.value = {
      ...row,
      reportTime: formatDateTime(row.reportTime)
    }
  }

  showDetailView.value = true
  feedbackForm.value = {
    description: '',
    uploadImage: '',
    status: '未完成'
  }
}

const handleAssignExecutor = (row) => {
  ElMessage.info(`选择执行人: ${row.eventDesc}`)
  // 实际项目中这里会打开执行人选择弹窗
}

const handleTagMaintenance = () => {
  activeTab.value = 'maintenance'
  ElMessage.info('切换到标签维护')
  // 实际项目中这里会跳转到标签维护页面
  // router.push('/tag-management')
}

const handleTagDevice = () => {
  activeTab.value = 'device'
  ElMessage.info('切换到标签设备')
  // 实际项目中这里会跳转到标签设备页面
}

const getStatusType = (status) => {
  const normalized = statusCodeMap[status] || status
  const statusMap = {
    pending: '待处理',
    processing: '处理中',
    completed: '已完成:',
    closed: '已关闭',
  }
  return statusMap[normalized] || 'info'
}

const handleSizeChange = (val) => {
  pagination.pageSize = val
  pagination.currentPage = 1
  loadEvents()
}

const handleCurrentChange = (val) => {
  pagination.currentPage = val
  loadEvents()
}

// 图片上传处理
const beforeUpload = (file) => {
  const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG) {
    ElMessage.error('上传图片只能是 JPG/PNG 格式!')
  }
  if (!isLt2M) {
    ElMessage.error('上传图片大小不能超过 2MB!')
  }
  return isJPG && isLt2M
}

const handleUploadSuccess = (response, file) => {
  feedbackForm.value.uploadImage = URL.createObjectURL(file.raw)
  ElMessage.success('图片上传成功')
}

// 提交反馈 -> 使用状态更新接口
const handleSubmitFeedback = async () => {
  if (!feedbackForm.value.description.trim()) {
    ElMessage.warning('请输入反馈描述')
    return
  }

  if (!selectedEvent.value?.id) {
    ElMessage.error('未获取到事件ID')
    return
  }

  const statusCode = statusCodeMap[feedbackForm.value.status] || 'processing'

  try {
    const res = await updateEventStatus(selectedEvent.value.id, {
      status: statusCode,
      executor: selectedEvent.value.executor,
      handleResult: feedbackForm.value.description
    })
    if (isRequestSuccess(res)) {
      ElMessage.success('事件反馈提交成功')
      showDetailView.value = false
      loadEvents()
    } else {
      ElMessage.error('提交失败')
    }
  } catch (error) {
    console.error('提交反馈失败', error)
    ElMessage.error('提交失败')
  }
}

// 高级搜索相关方法
const handleAdvancedSearch = (searchData) => {
  // 更新搜索表单
  if (searchData.keyword) {
    searchForm.eventName = searchData.keyword
  }
  if (searchData.eventName) {
    searchForm.eventName = searchData.eventName
  }
  if (searchData.dateRange && searchData.dateRange.length > 0) {
    searchForm.dateRange = searchData.dateRange
  }

  handleSearch()
}

const handleAdvancedSearchReset = () => {
  searchForm.eventName = ''
  searchForm.dateRange = []
  handleReset()
}

const handleUpload = () => {
  ElMessage.success('上传功能')
}

const handleDownloadTemplate = () => {
  ElMessage.success('下载模板')
}

const handleBatchOperation = () => {
  ElMessage.success('批量操作')
}

onMounted(() => {
  loadEvents()
})
</script>

<style scoped>
.event-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
  background-color: #f5f7fa;
  padding: 20px;
  overflow: hidden;
}

/* 详情视图样式 */
.detail-view {
  flex: 1;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.detail-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.breadcrumb-nav {
  font-size: 14px;
  color: #606266;
}

.breadcrumb-item {
  cursor: pointer;
  color: #1A53FF;
  transition: color 0.3s;
}

.breadcrumb-item:hover {
  color: #3d70ff;
}

.breadcrumb-item.active {
  color: #303133;
  cursor: default;
}

.breadcrumb-separator {
  margin: 0 8px;
  color: #c0c4cc;
}

.detail-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 32px;
}

.feedback-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1A53FF;
  margin-bottom: 20px;
}

.detail-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;
}

.detail-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  flex-shrink: 0;
  margin-top: 4px;
}

.icon-text {
  font-size: 12px;
  font-weight: 600;
  color: #909399;
}

.detail-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-label {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
  min-width: 80px;
}

.detail-value {
  font-size: 14px;
  color: #303133;
  font-weight: 400;
  line-height: 1.5;
}

.event-description {
  margin: 0;
  line-height: 1.6;
  color: #303133;
  margin-bottom: 8px;
}

.event-data-block {
  max-width: 100%;
}

.event-data {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: monospace;
  background: #f7f8fa;
  padding: 8px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}

.status-tag {
  margin-left: 0;
  align-self: flex-start;
}

/* ä¸ŠæŠ¥å›¾åƒæ ·å¼ */
.report-images {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.image-item {
  width: 120px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-image,
.table-report-img {
  width: 120px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
}

.table-report-img {
  display: block;
}

.no-image {
  color: #909399;
  font-size: 12px;
}

/* 反馈表单样式 */
.feedback-textarea {
  width: 530px;
}

.feedback-textarea :deep(.el-textarea__inner) {
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  padding: 12px;
  font-size: 14px;
  line-height: 1.5;
  width: 530px;
  height: 113px;
  resize: none;
}

.feedback-textarea :deep(.el-textarea__inner):focus {
  border-color: #1A53FF;
}

/* 上传区域样式 */
.upload-area {
  width: 120px;
  height: 80px;
}

.image-uploader {
  width: 100%;
  height: 100%;
}

:deep(.el-upload) {
  width: 100%;
  height: 100%;
  border: 2px dashed #d3d3d3;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
}

:deep(.el-upload:hover) {
  border-color: #1A53FF;
}

.upload-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafbfc;
}

.upload-icon {
  font-size: 24px;
  color: #8c939d;
}

.uploaded-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 状态单选组样式 */
.status-radio-group {
  display: flex;
  gap: 24px;
}

.status-radio-group :deep(.el-radio) {
  margin-right: 0;
}

.status-radio-group :deep(.el-radio__label) {
  font-size: 14px;
  color: #303133;
}

.status-radio-group :deep(.el-radio__input.is-checked .el-radio__inner) {
  background-color: #1A53FF;
  border-color: #1A53FF;
}

.status-radio-group :deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #1A53FF;
}

/* 操作按钮样式 */
.action-buttons {
  display: flex;
  justify-content: flex-start;
  gap: 16px;
  padding-top: 32px;
  border-top: 1px solid #f0f0f0;
  margin-top: 32px;
}

.action-buttons .el-button {
  min-width: 120px;
  height: 40px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
}

.action-buttons .el-button--primary {
  background-color: #1A53FF;
  border-color: #1A53FF;
}

.action-buttons .el-button--primary:hover {
  background-color: #3d70ff;
  border-color: #3d70ff;
}

/* 列表视图样式 */
.list-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* 查询栏 */
.query-bar {
  background: #F0F2F5;
  border-radius: 8px 8px 0 0;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

/* 标签按钮区域 */
.tag-buttons {
  display: flex;
  gap: 32px;
  margin-bottom: 16px;
  padding-bottom: 0;
  border-bottom: 1px solid #f0f0f0;
}

.tag-tab {
  position: relative;
  padding: 12px 0 16px 0;
  font-size: 16px;
  color: #666666;
  cursor: pointer;
  transition: color 0.3s ease;
  user-select: none;
}

.tag-tab:hover {
  color: #1A53FF;
}

.tag-tab.active {
  color: #1A53FF;
  font-weight: 500;
}

.tag-tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: #1A53FF;
  border-radius: 2px 2px 0 0;
}

.search-form {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.search-input {
  width: 240px;
  flex: none;
}


/* 主内容区 */
.main-content {
  flex: 1;
  background: white;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 操作按钮区域 */
.action-section {
  margin-bottom: 16px;
}

.action-button-group {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

/* 高级搜索组件区域 */
.advanced-search-group {
  display: flex;
  align-items: center;
}

/* 导出事件列表按钮样式 */
.export-events-btn {
  width: 138px !important;
  height: 36px !important;
  background: rgba(255, 255, 255, 0.00) !important;
  border: 1px solid rgba(217, 217, 217, 1) !important;
  border-radius: 18px !important;
  padding: 0 16px !important;
  font-size: 14px;
  font-weight: 500;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  color: #333333 !important;
}

.export-events-btn:hover {
  background: rgba(26, 83, 255, 0.05) !important;
  border-color: #1A53FF !important;
  color: #1A53FF !important;
}

.export-events-btn:focus {
  background: rgba(255, 255, 255, 0.00) !important;
  border-color: rgba(217, 217, 217, 1) !important;
  color: #333333 !important;
  outline: none !important;
  box-shadow: none !important;
}

.export-events-btn:active {
  background: rgba(26, 83, 255, 0.1) !important;
  border-color: #1A53FF !important;
  color: #1A53FF !important;
}

/* 表格内容 */
.table-content {
  flex: 1;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 分页容器样式 */
.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 操作按钮样式 */
.action-btn {
  background: none !important;
  border: none !important;
  color: #1A53FF !important;
  padding: 4px 8px !important;
  margin: 0 4px !important;
  font-size: 14px !important;
  height: auto !important;
  line-height: 1.5 !important;
  cursor: pointer !important;
}

.action-btn:hover {
  background: none !important;
  border: none !important;
  color: #3d70ff !important;
  text-decoration: underline !important;
}

.action-btn:focus {
  background: none !important;
  border: none !important;
  color: #1A53FF !important;
  outline: none !important;
  box-shadow: none !important;
}

.action-btn:active {
  background: none !important;
  border: none !important;
  color: #1A53FF !important;
}

/* 执行人信息 */
.executor-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.executor-avatars {
  display: flex;
  position: relative;
}

.executor-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid white;
  margin-left: -8px;
}

.executor-avatar:first-child {
  margin-left: 0;
}

.executor-names {
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
}

.no-executor {
  color: #909399;
  font-size: 12px;
}

/* 表格样式调整 */
:deep(.el-table) {
  font-size: 14px;
  flex: 1;
}

:deep(.el-table th) {
  background-color: #fafafa;
  color: #606266;
  font-weight: 500;
}

:deep(.el-table .el-table__row:hover > td) {
  background-color: #f8faff;
}

/* 标签样式 */
:deep(.el-tag) {
  border-radius: 4px;
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
</style>
