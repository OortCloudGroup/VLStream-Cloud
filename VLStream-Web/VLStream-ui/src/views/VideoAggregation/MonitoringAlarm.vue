<template>
  <div class="monitoring-alarms tenant_Page draHeaPB">
    <div class="tenant_content">
      <div class="tableTenBox flexRowAC">
        <div class="tableTenItU">
          <div class="depNameBox_out flexRowAC">
            <div class="depNameBox flexRowAC">
              <button-group :button-list="toolbarButtonList" />
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
            :data="currentPageData"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" :width="clacPXToVW(55)" />
            <el-table-column label="序号" :width="clacPXToVW(65)">
              <template #default="scope">
                {{ scope.$index + (currentPage - 1) * pageSize + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="deviceName" label="设备名称" :width="clacPXToVW(140)" show-overflow-tooltip />
            <el-table-column prop="tags" label="标签" :width="clacPXToVW(120)">
              <template #default="scope">
                <el-tag size="small" type="primary">{{ scope.row.tags }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deviceId" label="设备ID" :width="clacPXToVW(140)" show-overflow-tooltip />
            <el-table-column prop="alarmType" label="告警类型" :width="clacPXToVW(120)" />
            <el-table-column prop="alarmLocation" label="告警位置" show-overflow-tooltip />
            <el-table-column prop="alarmTime" label="告警时间" :width="clacPXToVW(180)" />
            <el-table-column fixed="right" align="right" label="操作" :width="clacPXToVW(120)">
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
              :current-page="currentPage"
              :page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="total"
              layout="total, prev, pager, next, sizes"
              class="justifyAlign"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 视频播放弹窗 -->
    <el-dialog
      v-model="videoDialogVisible"
      title="视频播放"
      width="1400px"
      :close-on-click-modal="false"
      class="video-dialog"
    >
      <div class="video-playback-container">
        <!-- 右侧播放器区域 -->
        <div class="player-section">
          <div class="video-player">
            <div class="video-content">
              <!-- 模拟视频画面 -->
              <div class="video-placeholder">
                <div class="video-info">
                  <div class="device-name">{{ selectedRow?.deviceName || '奥尔特云前台' }}</div>
                  <div class="video-time">2021年04月15日 11:18:27</div>
                </div>
              </div>

              <!-- 视频控制条 -->
              <div class="video-controls">
                <div class="progress-bar">
                  <div class="progress-track">
                    <div class="progress-fill" :style="{ width: '45%' }"></div>
                    <div class="progress-handle" :style="{ left: '45%' }"></div>
                  </div>
                </div>
                <div class="control-buttons">
                  <el-button class="control-btn" size="small">
                    <el-icon><VideoPlay /></el-icon>
                  </el-button>
                  <span class="time-display">00:22:14/07:34:12</span>
                  <div class="volume-control">
                    <el-icon class="volume-icon"><Microphone /></el-icon>
                    <span class="volume-text">1.0</span>
                  </div>
                  <div class="fullscreen-control">
                    <el-icon class="fullscreen-icon"><FullScreen /></el-icon>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import {
  Search,
  Refresh,
  Download,
  Delete,
  VideoCamera,
  VideoPlay,
  Microphone,
  FullScreen
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DateRangePicker from '@/components/DateRangePicker.vue'
import { clacPXToVW } from '@/utils/index'

// 搜索表单
const searchForm = reactive({
  deviceName: '',
  alarmType: '',
  dateRange: []
})

// 视频播放弹窗相关
const videoDialogVisible = ref(false)
const selectedRow = ref(null)
const selectedVideoIndex = ref(0)
const selectedYear = ref(2021)
const selectedMonth = ref(4)

// 年份和月份数据
const years = ref([2021, 2022, 2023, 2024])
const months = ref([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12])

// 未获得真实告警录像前保持空列表。
const videoList = ref([])

// 表格数据
// 告警表格不再展示静态样例；只能由真实事件接口填充。
const tableData = ref([])

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const total = computed(() => tableData.value.length)

// 当前页数据
const currentPageData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return tableData.value.slice(start, end)
})

// 选中行
const selectedRows = ref([])

const toolbarButtonList = computed(() => [
  { name: '导出', svg: 'export', clickFn: () => handleExport() },
  { name: '删除', svg: 'table_del', clickFn: () => handleDelete() }
])

// 方法
const handleSearch = () => {
  console.log('搜索:', searchForm)
  ElMessage.success('搜索完成')
}

const handleReset = () => {
  Object.assign(searchForm, {
    deviceName: '',
    alarmType: '',
    dateRange: []
  })
  ElMessage.info('已重置搜索条件')
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleExport = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要导出的记录')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要导出选中的 ${selectedRows.value.length} 条记录吗？`,
      '确认导出',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    ElMessage.success(`导出 ${selectedRows.value.length} 条记录成功`)
    // 这里可以添加实际的导出逻辑
  } catch {
    ElMessage.info('已取消导出')
  }
}

const handleDelete = async () => {
  if (selectedRows.value.length === 0) {
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

    // 删除逻辑
    selectedRows.value.forEach(row => {
      const index = tableData.value.findIndex(item => item.id === row.id)
      if (index !== -1) {
        tableData.value.splice(index, 1)
      }
    })

    selectedRows.value = []
    ElMessage.success('删除成功')
  } catch {
    ElMessage.info('已取消删除')
  }
}

const handlePlay = (row) => {
  console.log('播放告警视频:', row.deviceName)
  selectedRow.value = row
  videoDialogVisible.value = true
  ElMessage.success(`开始播放设备 ${row.deviceName} 的告警视频`)
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handleCurrentChange = (val) => {
  currentPage.value = val
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
    searchForm.deviceName = searchData.keyword
  }
  if (searchData.deviceName) {
    searchForm.deviceName = searchData.deviceName
  }
  if (searchData.deviceId) {
    searchForm.deviceName = searchData.deviceId
  }
  if (searchData.alarmType) {
    searchForm.alarmType = searchData.alarmType
  }
  if (searchData.dateRange && searchData.dateRange.length > 0) {
    searchForm.dateRange = searchData.dateRange
  }

  handleSearch()
}

const handleAdvancedSearchReset = () => {
  console.log('重置高级搜索')
  searchForm.deviceName = ''
  searchForm.alarmType = ''
  searchForm.dateRange = []
  handleReset()
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

// 视频播放相关方法
const selectYear = (year) => {
  selectedYear.value = year
}

const selectMonth = (month) => {
  selectedMonth.value = month
}

const selectVideo = (index) => {
  selectedVideoIndex.value = index
}
</script>

<style scoped lang="scss">
.tenant_Page {
  height: 100%;
  width: 100%;
  background: #f0f2f5;
  .tenant_content { width: 100%; height: 100%; }
  .tableTenBox {
    padding: 20px;
    width: 100%;
    height: 100%;
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

.monitoring-alarms {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
}

/* 搜索区域 - 查询栏背景颜色：#F0F2F5 */
.search-section {
  background: #F0F2F5;
  border-radius: 8px 8px 0 0;
  padding: 20px;
  margin-bottom: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.search-form {
  width: 100%;
}

.search-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.search-buttons {
  display: flex;
  gap: 12px;
}

/* 主要内容区域 - 容器无缝连接，零间隙 */
.main-content {
  background: white;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

/* 操作按钮区域 */
.action-section {
  background: white;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.action-button-group {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

/* 导出删除按钮组合 */
.export-delete-group {
  display: flex;
  align-items: center;
  margin: 0;
  padding: 0;
}

/* 高级搜索组件区域 */
.advanced-search-group {
  display: flex;
  align-items: center;
}

.export-delete-group .el-button + .el-button {
  margin-left: 0 !important;
}

/* 导出按钮自定义样式 */
.export-btn-custom {
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

.export-btn-custom:hover,
.export-btn-custom:focus {
  border-right: none !important;
  border-color: #409eff !important;
}

.export-btn-custom:disabled {
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
  padding: 20px;
  flex: 1;
  overflow: hidden;
}

/* 操作列按钮样式 */
.table-section .action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
}

/* 分页区域 - 使用全局样式，居中对齐 */
.pagination-section {
  background: white;
  padding: 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 表格样式调整 */
:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #fafafa;
  font-weight: 600;
}

:deep(.el-table td) {
  padding: 12px 0;
}

/* 标签样式 */
:deep(.el-tag) {
  border-radius: 4px;
}

/* 视频播放弹窗样式 */
.video-dialog {
  :deep(.el-dialog__body) {
    padding: 0;
  }
}

.video-playback-container {
  display: flex;
  height: 680px;
  background: #f5f7fa;
  padding: 20px;
  gap: 20px;
}

/* 右侧播放器区域 */
.player-section {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.video-player {
  flex: 1;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.video-content {
  width: 100%;
  height: 100%;
  position: relative;
}

.video-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  padding: 20px;
  position: relative;
  background-image: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" fill="%23f0f0f0"/><circle cx="30" cy="30" r="20" fill="%23e0e0e0"/><rect x="60" y="20" width="30" height="20" fill="%23e0e0e0"/></svg>');
  background-size: cover;
  background-position: center;
}

.video-info {
  color: white;
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 2;
}

.device-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}

.video-time {
  font-size: 14px;
  opacity: 0.8;
}

/* 视频控制条 */
.video-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.8));
  padding: 20px;
  color: white;
}

.progress-bar {
  margin-bottom: 12px;
}

.progress-track {
  height: 4px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
  position: relative;
  cursor: pointer;
}

.progress-fill {
  height: 100%;
  background: #1A53FF;
  border-radius: 2px;
  transition: width 0.3s;
}

.progress-handle {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 12px;
  height: 12px;
  background: #1A53FF;
  border-radius: 50%;
  cursor: pointer;
  transition: left 0.3s;
}

.control-buttons {
  display: flex;
  align-items: center;
  gap: 16px;
}

.control-btn {
  background: rgba(255, 255, 255, 0.2) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  color: white !important;
  padding: 8px 12px;
  border-radius: 4px;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.3) !important;
}

.time-display {
  font-size: 14px;
  color: white;
}

.volume-control, .fullscreen-control {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.volume-icon, .fullscreen-icon {
  font-size: 16px;
}

.volume-text {
  font-size: 14px;
}
</style>
