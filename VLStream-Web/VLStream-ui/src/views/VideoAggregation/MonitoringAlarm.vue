<template>
  <div class="monitoring-alarms">
    <!-- 搜索区域 - 已注释，使用高级搜索组件替代 -->
    <!--
    <div class="search-section">
      <div class="search-form">
        <div class="search-row">
          <el-input
            v-model="searchForm.deviceName"
            placeholder="设备名称/设备ID"
            clearable
            style="width: 240px;"
          />
          <el-select
            v-model="searchForm.alarmType"
            placeholder="告警类型"
            clearable
            style="width: 150px;"
          >
            <el-option label="全部" value="" />
            <el-option label="跨线告警" value="跨线告警" />
            <el-option label="区域入侵" value="区域入侵" />
            <el-option label="人员聚集" value="人员聚集" />
            <el-option label="异常行为" value="异常行为" />
          </el-select>
          <DateRangePicker
            v-model="searchForm.dateRange"
            start-placeholder="创建时间"
            end-placeholder="结束时间"
          />
          <div class="search-buttons">
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </div>
        </div>
      </div>
    </div>
    -->

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 操作按钮区域 -->
      <div class="action-section">
        <div class="action-button-group">
          <!-- 导出删除按钮组 -->
          <div class="export-delete-group">
            <el-button 
              class="export-btn-custom" 
              @click="handleExport" 
              :disabled="!selectedRows.length"
            >
              <el-icon><Download /></el-icon>
              导出
            </el-button>
            <el-button 
              type="danger" 
              class="delete-btn-custom" 
              @click="handleDelete" 
              :disabled="!selectedRows.length"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
          
          <!-- 高级搜索组件 -->
          <div class="advanced-search-group">
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
      </div>
          
      <!-- 表格区域 -->
      <div class="table-section">
        <el-table 
          :data="currentPageData"
          stripe
          @selection-change="handleSelectionChange"
          style="width: 100%"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column type="index" label="序号" width="80" align="center" />
          <el-table-column prop="deviceName" label="设备名称" min-width="120" />
          <el-table-column prop="tags" label="标签" width="120">
            <template #default="scope">
              <el-tag size="small" type="primary">{{ scope.row.tags }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deviceId" label="设备ID" min-width="120" />
          <el-table-column prop="alarmType" label="告警类型" min-width="120" />
          <el-table-column prop="alarmLocation" label="告警位置" min-width="200" />
          <el-table-column prop="alarmTime" label="告警时间" width="180" />
                          <el-table-column label="操作" width="100" align="right" fixed="right">
            <template #default="scope">
              <div class="action-buttons">
                <PlayButton @click="handlePlay(scope.row)" />
              </div>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 分页 - 紧贴表格数据 -->
        <div class="table-pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
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
import PlayButton from '@/components/PlayButton.vue'
import AdvancedSearch from '@/components/AdvancedSearch.vue'

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

// 模拟视频列表数据
const mockVideoList = ref([
  { timeRange: '11:18:00-11:19:00' },
  { timeRange: '11:19:00-11:20:00' },
  { timeRange: '11:20:00-11:21:00' },
  { timeRange: '11:21:00-11:22:00' },
  { timeRange: '11:22:00-11:23:00' },
  { timeRange: '11:23:00-11:24:00' },
  { timeRange: '11:24:00-11:25:00' },
  { timeRange: '11:25:00-11:26:00' }
])

// 表格数据
const tableData = ref([
  {
    id: 1,
    sequence: 1,
    deviceName: '海康云台',
    tags: '球机监控',
    deviceId: '65131984',
    alarmType: '跨线告警',
    alarmLocation: '东门出入水产大道19K',
    alarmTime: '2024-10-07 12:17:29'
  },
  {
    id: 2,
    sequence: 1,
    deviceName: '海康云台',
    tags: '球机监控',
    deviceId: '65131984',
    alarmType: '跨线告警',
    alarmLocation: '东门出入水产大道19K',
    alarmTime: '2024-10-07 12:17:29'
  },
  {
    id: 3,
    sequence: 1,
    deviceName: '海康云台',
    tags: '球机监控',
    deviceId: '65131984',
    alarmType: '跨线告警',
    alarmLocation: '东门出入水产大道19K',
    alarmTime: '2024-10-07 12:17:29'
  },
  {
    id: 4,
    sequence: 1,
    deviceName: '海康云台',
    tags: '球机监控',
    deviceId: '65131984',
    alarmType: '跨线告警',
    alarmLocation: '东门出入水产大道19K',
    alarmTime: '2024-10-07 12:17:29'
  },
  {
    id: 5,
    sequence: 1,
    deviceName: '海康云台',
    tags: '球机监控',
    deviceId: '65131984',
    alarmType: '跨线告警',
    alarmLocation: '东门出入水产大道19K',
    alarmTime: '2024-10-07 12:17:29'
  },
  {
    id: 6,
    sequence: 1,
    deviceName: '海康云台',
    tags: '球机监控',
    deviceId: '65131984',
    alarmType: '跨线告警',
    alarmLocation: '东门出入水产大道19K',
    alarmTime: '2024-10-07 12:17:29'
  },
  {
    id: 7,
    sequence: 1,
    deviceName: '海康云台',
    tags: '球机监控',
    deviceId: '65131984',
    alarmType: '跨线告警',
    alarmLocation: '东门出入水产大道19K',
    alarmTime: '2024-10-07 12:17:29'
  },
  {
    id: 8,
    sequence: 1,
    deviceName: '海康云台',
    tags: '球机监控',
    deviceId: '65131984',
    alarmType: '跨线告警',
    alarmLocation: '东门出入水产大道19K',
    alarmTime: '2024-10-07 12:17:29'
  },
  {
    id: 9,
    sequence: 1,
    deviceName: '海康云台',
    tags: '球机监控',
    deviceId: '65131984',
    alarmType: '跨线告警',
    alarmLocation: '东门出入水产大道19K',
    alarmTime: '2024-10-07 12:17:29'
  },
  {
    id: 10,
    sequence: 1,
    deviceName: '海康云台',
    tags: '球机监控',
    deviceId: '65131984',
    alarmType: '跨线告警',
    alarmLocation: '东门出入水产大道19K',
    alarmTime: '2024-10-07 12:17:29'
  },
  {
    id: 11,
    sequence: 1,
    deviceName: '大华枪机',
    tags: '枪机监控',
    deviceId: '65131985',
    alarmType: '人员入侵',
    alarmLocation: '南门安防区域',
    alarmTime: '2024-10-07 13:25:15'
  },
  {
    id: 12,
    sequence: 1,
    deviceName: '宇视半球',
    tags: '半球监控',
    deviceId: '65131986',
    alarmType: '区域滞留',
    alarmLocation: '停车场A区',
    alarmTime: '2024-10-07 14:10:08'
  },
  {
    id: 13,
    sequence: 1,
    deviceName: '华为云台',
    tags: '球机监控',
    deviceId: '65131987',
    alarmType: '异常行为',
    alarmLocation: '办公楼大厅',
    alarmTime: '2024-10-07 15:33:22'
  },
  {
    id: 14,
    sequence: 1,
    deviceName: '海康云台',
    tags: '球机监控',
    deviceId: '65131988',
    alarmType: '跨线告警',
    alarmLocation: '西门出入口',
    alarmTime: '2024-10-07 16:45:33'
  },
  {
    id: 15,
    sequence: 1,
    deviceName: '大华枪机',
    tags: '枪机监控',
    deviceId: '65131989',
    alarmType: '物品丢失',
    alarmLocation: '仓库B区',
    alarmTime: '2024-10-07 17:12:44'
  },
  {
    id: 16,
    sequence: 1,
    deviceName: '宇视半球',
    tags: '半球监控',
    deviceId: '65131990',
    alarmType: '聚集告警',
    alarmLocation: '食堂区域',
    alarmTime: '2024-10-07 18:20:55'
  },
  {
    id: 17,
    sequence: 1,
    deviceName: '华为云台',
    tags: '球机监控',
    deviceId: '65131991',
    alarmType: '人脸识别异常',
    alarmLocation: '前台接待处',
    alarmTime: '2024-10-07 19:08:16'
  },
  {
    id: 18,
    sequence: 1,
    deviceName: '海康云台',
    tags: '球机监控',
    deviceId: '65131992',
    alarmType: '车辆违停',
    alarmLocation: '消防通道',
    alarmTime: '2024-10-07 20:15:27'
  },
  {
    id: 19,
    sequence: 1,
    deviceName: '大华枪机',
    tags: '枪机监控',
    deviceId: '65131993',
    alarmType: '烟雾检测',
    alarmLocation: '机房区域',
    alarmTime: '2024-10-07 21:33:38'
  },
  {
    id: 20,
    sequence: 1,
    deviceName: '宇视半球',
    tags: '半球监控',
    deviceId: '65131994',
    alarmType: '温度异常',
    alarmLocation: '设备间',
    alarmTime: '2024-10-07 22:40:49'
  },
  {
    id: 21,
    sequence: 1,
    deviceName: '华为云台',
    tags: '球机监控',
    deviceId: '65131995',
    alarmType: '噪音检测',
    alarmLocation: '生产车间',
    alarmTime: '2024-10-08 08:20:10'
  },
  {
    id: 22,
    sequence: 1,
    deviceName: '海康云台',
    tags: '球机监控',
    deviceId: '65131996',
    alarmType: '人员摔倒',
    alarmLocation: '楼梯间',
    alarmTime: '2024-10-08 09:15:21'
  },
  {
    id: 23,
    sequence: 1,
    deviceName: '大华枪机',
    tags: '枪机监控',
    deviceId: '65131997',
    alarmType: '门禁异常',
    alarmLocation: '重要区域入口',
    alarmTime: '2024-10-08 10:30:32'
  },
  {
    id: 24,
    sequence: 1,
    deviceName: '宇视半球',
    tags: '半球监控',
    deviceId: '65131998',
    alarmType: '光线异常',
    alarmLocation: '地下车库',
    alarmTime: '2024-10-08 11:45:43'
  },
  {
    id: 25,
    sequence: 1,
    deviceName: '华为云台',
    tags: '球机监控',
    deviceId: '65131999',
    alarmType: '设备故障',
    alarmLocation: '监控中心',
    alarmTime: '2024-10-08 12:50:54'
  }
])

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

<style scoped>
/* 监控告警页面样式 - 按照rules规范 */
.monitoring-alarms {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  padding: 20px;
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