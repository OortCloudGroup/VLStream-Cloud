<template>
  <div class="video-playback">
    <!-- 搜索表单区域 -->
    <!--
    <div class="search-form-container">
      <div class="search-form">
        <div class="search-row">
          <div class="search-item">
            <el-input
              v-model="searchForm.fileName"
              placeholder="请输入设备名称或设备ID"
              clearable
            />
          </div>
          <div class="search-item">
            <el-select
              v-model="searchForm.recordType"
              placeholder="请选择标签类型"
              clearable
              style="width: 100%"
            >
              <el-option label="全部" value="" />
              <el-option label="球机监控" value="球机监控" />
              <el-option label="枪机监控" value="枪机监控" />
              <el-option label="半球监控" value="半球监控" />
              <el-option label="云台监控" value="云台监控" />
            </el-select>
          </div>
          <div class="search-item date-with-buttons">
            <DateRangePicker
              v-model="searchForm.dateRange"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD"
              width="600px"
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
    </div>
    -->

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧设备树 -->
      <div class="device-tree-container" :class="{ collapsed: deviceTreeCollapsed }">
        <DeviceTree
          :tree-data="deviceTreeData"
          title="设备树"
          :show-search="true"
          :show-collapse-btn="true"
          :show-expand-btn="false"
          :show-add-actions="false"
          :show-delete-actions="false"
          :show-bottom-actions="false"
          :collapsed="deviceTreeCollapsed"
          @node-click="handleNodeClick"
          @toggle-collapse="toggleDeviceTree"
          @search="handleDeviceTreeSearch"
        />
      </div>

      <!-- 右侧内容区域 -->
      <div class="content-area">
        <div class="content-wrapper">
          <!-- 导航栏 -->
          <div class="content-header">
            <div class="breadcrumb">
              <!-- 设备树折叠时显示的展开按钮 -->
              <CollapseToggle
                v-if="deviceTreeCollapsed"
                class="expand-device-tree-btn"
                :is-expanded="false"
                @toggle="toggleDeviceTree"
              />
              <span class="breadcrumb-item" @click="showTableView">视频回放</span>
              <span v-if="showPlayerView" class="breadcrumb-separator">></span>
              <!-- <span v-if="showPlayerView" class="breadcrumb-item active">播放视频</span> -->
            </div>
          </div>

          <!-- 表格视图 -->
          <div v-if="!showPlayerView" class="table-view">
            <!-- 搜索区域 -->
            <div class="search-section">
              <AdvancedSearch
                @search="handleAdvancedSearch"
                @reset="handleAdvancedSearchReset"
                @export="handleExport"
                @upload="handleUpload"
                @template="handleDownloadTemplate"
                @batch="handleBatchOperation"
              />
            </div>

            <!-- 表格 -->
            <div class="table-content">
              <el-table
                :data="currentPageData"
                stripe
                v-loading="loading"
                element-loading-text="正在加载设备数据..."
                @row-click="handleRowClick"
              >
                <el-table-column prop="index" label="序号" width="80" align="center" />
                <el-table-column prop="deviceName" label="设备名称" min-width="120" />
                <el-table-column prop="tag" label="标签" width="100">
                  <template #default="scope">
                    <el-tag size="small" type="primary">
                      {{ scope.row.tag }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="deviceId" label="设备ID" min-width="120" />
                <el-table-column prop="streamPath" label="视频流路径" min-width="300" show-overflow-tooltip />
                <el-table-column prop="status" label="状态" min-width="100" align="center">
                  <template #default="scope">
                    <el-tag
                        :type="scope.row.status === 1 ? 'success' : 'danger'"
                        size="small">
                      {{ scope.row.status === 1 ? '在线' : '离线' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="lastRefreshTime" label="最近一次刷新时间" width="160" />
                <el-table-column label="操作" width="120" fixed="right" align="right">
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
                  :total="filteredData.length"
                  layout="total, sizes, prev, pager, next, jumper"
                  @size-change="handleSizeChange"
                  @current-change="handleCurrentChange"
                />
              </div>
            </div>
          </div>

          <!-- 播放器视图 -->
          <div v-if="showPlayerView" class="player-view">
            <div class="player-content">
              <!-- 视频信息 -->
              <div class="section">
                <h3 class="section-title">视频信息</h3>

                <div class="video-info-grid">
                  <div class="info-row">
                    <div class="info-label">设备名称：</div>
                    <div class="info-value">{{ currentVideo?.deviceName }}</div>
                  </div>
                  <div class="info-row">
                    <div class="info-label">设备ID：</div>
                    <div class="info-value">{{ currentVideo?.deviceId }}</div>
                  </div>
                  <div class="info-row">
                    <div class="info-label">标签：</div>
                    <div class="info-value">
                      <el-tag size="small" type="primary">
                        {{ currentVideo?.tag }}
                      </el-tag>
                    </div>
                  </div>
                  <div class="info-row">
                    <div class="info-label">视频流路径：</div>
                    <div class="info-value">{{ currentVideo?.streamPath }}</div>
                  </div>
                  <div class="info-row">
                    <div class="info-label">状态：</div>
                    <div class="info-value">
                      <el-tag
                          :type="currentVideo.status === 1 ? 'success' : 'danger'"
                          size="small">
                        {{ currentVideo.status === 1 ? '在线' : '离线' }}
                      </el-tag>
                    </div>
                  </div>
                  <div class="info-row">
                    <div class="info-label">最近刷新时间：</div>
                    <div class="info-value">{{ currentVideo?.lastRefreshTime }}</div>
                  </div>
                </div>
              </div>

              <!-- 视频播放器 -->
              <div class="section">
                <h3 class="section-title">视频播放</h3>

                <div class="player-container">
                  <div class="video-screen">
                    <video ref="videoPlayer" controls width="100%" height="400">
                      <source :src="currentVideoUrl" type="video/mp4">
                      您的浏览器不支持视频播放
                    </video>
                  </div>

                  <div class="player-controls">
                    <div class="control-buttons">
                      <el-button-group>
                        <el-button @click="playPause" :type="isPlaying ? 'danger' : 'primary'">
                          <el-icon><VideoPlay v-if="!isPlaying" /><VideoPause v-else /></el-icon>
                          {{ isPlaying ? '暂停' : '播放' }}
                        </el-button>
                        <el-button @click="stopVideo">
                          <el-icon><SwitchButton /></el-icon>
                          停止
                        </el-button>
                        <el-button @click="downloadVideo">
                          <el-icon><Download /></el-icon>
                          下载
                        </el-button>
                      </el-button-group>
                    </div>

                    <div class="timeline-container">
                      <div class="timeline">
                        <el-slider
                          v-model="playProgress"
                          :max="videoDuration"
                          :format-tooltip="formatTime"
                          @change="seekVideo"
                        />
                      </div>
                      <div class="time-display">
                        {{ formatTime(currentTime) }} / {{ formatTime(videoDuration) }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="form-actions">
                <el-button class="action-btn" @click="showTableView">返回列表</el-button>
                <el-button type="primary" class="action-btn" @click="downloadVideo">下载视频</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 视频回放弹窗 -->
    <el-dialog
      v-model="videoDialogVisible"
      title="视频回放"
      width="1400px"
      :close-on-click-modal="false"
      class="video-dialog"
    >
      <div class="video-playback-container">
        <!-- 左侧视频列表区域 -->
        <div class="video-list-section">
          <!-- 日期选择器 -->
          <div class="date-selector">
            <!-- 年份选择 -->
            <div class="date-section">
              <div class="section-title">年份</div>
              <div class="year-list">
                <span
                  v-for="year in availableYears"
                  :key="year"
                  class="year-item"
                  :class="{ active: selectedDate.year === year }"
                  @click="selectYear(year)"
                >
                  {{ year }}
                </span>
              </div>
            </div>

            <!-- 月份选择 -->
            <div class="date-section">
              <div class="section-title">月份</div>
              <div class="month-grid">
                <span
                  v-for="month in availableMonths"
                  :key="month"
                  class="month-item"
                  :class="{ active: selectedDate.month === month }"
                  @click="selectMonth(month)"
                >
                  {{ month }}
                </span>
              </div>
            </div>

            <!-- 日期选择 -->
            <div class="date-section">
              <div class="section-title">日期</div>
              <div class="date-grid">
                <span
                  v-for="day in availableDays"
                  :key="day"
                  class="date-item"
                  :class="{ active: selectedDate.day === day }"
                  @click="selectDay(day)"
                >
                  {{ day }}
                </span>
              </div>
            </div>
          </div>

          <!-- 视频列表标题 -->
          <div class="video-list-title">
            <span>视频列表</span>
          </div>

          <!-- 视频缩略图列表 -->
          <div class="video-thumbnails">
            <div
              v-for="(video, index) in mockVideoList"
              :key="index"
              class="video-thumbnail-item"
              :class="{ active: selectedVideoIndex === index }"
              @click="selectVideo(index)"
            >
              <div class="thumbnail-image">
                <!-- 显示真实缩略图 -->
                <img
                  v-if="video.thumbnailUrl"
                  :src="video.thumbnailUrl"
                  :alt="video.fileName"
                  class="thumbnail-img"
                  @error="handleThumbnailError($event, index)"
                />
                <!-- 缩略图加载失败或不存在时显示占位符 -->
                <div class="thumbnail-placeholder" :style="{ display: video.thumbnailUrl ? 'none' : 'flex' }">
                  <el-icon class="video-icon"><VideoCamera /></el-icon>
                </div>
                <div class="play-overlay">
                  <el-icon class="play-icon"><VideoPlay /></el-icon>
                </div>
              </div>
              <div class="video-time-range">{{ video.timeRange }}</div>
            </div>
          </div>
        </div>

        <!-- 右侧播放器区域 -->
        <div class="player-section">
          <div class="video-player">
            <div class="video-content">
              <!-- 视频播放区域 -->
              <div class="video-display">
                <!-- 录制视频播放器 -->
                <div v-if="currentVideoUrl" class="recorded-video-container">
                  <video
                    ref="recordedVideoPlayer"
                    :src="currentVideoUrl"
                    controls
                    autoplay
                    width="100%"
                    class="recorded-video-player"
                    @loadedmetadata="handleVideoLoaded"
                    @error="handleVideoError"
                  >
                    您的浏览器不支持视频播放
                  </video>
                </div>

                <!-- 无视频时的占位符 -->
                <div v-else class="video-placeholder">
                  <div class="placeholder-content">
                    <el-icon class="placeholder-icon"><VideoCamera /></el-icon>
                    <div class="placeholder-text">请从左侧选择要播放的视频</div>
                  </div>
                </div>
              </div>

              <!-- 视频信息显示 -->
              <div class="video-info" v-if="currentVideo">
                <div class="video-info-item">
                  <span class="info-label">设备：</span>
                  <span class="info-value">{{ currentVideo.deviceName }}</span>
                </div>
                <div class="video-info-item">
                  <span class="info-label">文件：</span>
                  <span class="info-value">{{ currentVideo.fileName }}</span>
                </div>
                <div class="video-info-item" v-if="currentVideo.recordStartTime">
                  <span class="info-label">时间：</span>
                  <span class="info-value">{{ formatRecordTime(currentVideo.recordStartTime, currentVideo.recordEndTime) }}</span>
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
import { ref, reactive, computed, onMounted } from 'vue'
import {
  VideoCamera,
  VideoPlay,
  VideoPause,
  SwitchButton,
  Download,
} from '@element-plus/icons-vue'
import DeviceTree from '@/components/DeviceTree.vue'
import CollapseToggle from '@/components/CollapseToggle.vue'
import PlayButton from '@/components/PlayButton.vue'
import AdvancedSearch from '@/components/AdvancedSearch.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// API 导入
import { getDeviceList, getDeviceTree } from '@/api/device'
import { getDeviceRecords } from '@/api/videoRecord'
import {getBaseURL} from "@/utils/request";

// 响应式数据
const deviceTreeCollapsed = ref(false)
const showPlayerView = ref(false)
const selectedRow = ref(null)
const selectedRows = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const currentVideo = ref(null)
const currentVideoUrl = ref('')
const isPlaying = ref(false)
const playProgress = ref(0)
const currentTime = ref(0)
const videoDuration = ref(0)
const videoPlayer = ref(null)
const recordedVideoPlayer = ref(null)
const videoDialogVisible = ref(false)
const selectedVideoIndex = ref(0)
const loading = ref(false)
const totalRecords = ref(0)

// 获取当前日期
const getCurrentDate = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 搜索表单 - 默认当前日期
const searchForm = reactive({
  fileName: '',
  recordType: '',
  dateRange: [getCurrentDate(), getCurrentDate()]
})

// 设备树数据
const deviceTreeData = ref([])

// 设备列表数据（主列表）
const deviceList = ref([])

// 视频记录数据（播放时使用）
const videoRecords = ref([])

// 日期选择器相关数据
const selectedDate = reactive({
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1,
  day: new Date().getDate()
})

// 可选年份范围（当前年份前后5年）
const availableYears = computed(() => {
  const currentYear = new Date().getFullYear()
  const years = []
  for (let i = currentYear - 2; i <= currentYear + 1; i++) {
    years.push(i)
  }
  return years
})

// 可选月份
const availableMonths = computed(() => {
  return Array.from({ length: 12 }, (_, i) => i + 1)
})

// 可选日期（根据选中的年月动态计算）
const availableDays = computed(() => {
  const daysInMonth = new Date(selectedDate.year, selectedDate.month, 0).getDate()
  return Array.from({ length: daysInMonth }, (_, i) => i + 1)
})

// 选中的日期字符串
const selectedDateStr = computed(() => {
  const year = selectedDate.year
  const month = String(selectedDate.month).padStart(2, '0')
  const day = String(selectedDate.day).padStart(2, '0')
  return `${year}-${month}-${day}`
})

// 过滤后的数据 - 基于设备列表
const filteredData = computed(() => {
  return deviceList.value.filter(item => {
    let match = true

    if (searchForm.fileName) {
      match = match && (
        item.deviceName.toLowerCase().includes(searchForm.fileName.toLowerCase()) ||
        (item.id && item.id.toString().includes(searchForm.fileName))
      )
    }

    if (searchForm.recordType) {
      match = match && item.tag === searchForm.recordType
    }

    return match
  })
})

// 当前页数据
const currentPageData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredData.value.slice(start, end)
})

// 加载设备树数据
const loadDeviceTree = async () => {
  try {
    const response = await getDeviceTree()
    if (response.data && response.data.length > 0) {
      deviceTreeData.value = response.data
    } else {
      // 如果没有设备树数据，使用默认数据
      deviceTreeData.value = [
        {
          id: 1,
          label: '前门区域',
          type: 'group',
          children: [
            { id: 11, label: '前门摄像头01', type: 'device', status: 'online' },
            { id: 12, label: '海康云台', type: 'device', status: 'online' },
            { id: 13, label: '门禁监控', type: 'device', status: 'online' }
          ]
        }
      ]
    }
  } catch (error) {
    console.error('加载设备树失败:', error)
    ElMessage.error('加载设备树失败')
  }
}

// 加载设备列表 - 使用device/page API
const loadDeviceList = async () => {
  loading.value = true
  try {
    const params = {
      current: 1,
      size: 1000, // 获取所有设备
    }

    // 添加设备名称过滤
    if (searchForm.fileName) {
      params.keyword = searchForm.fileName
    }

    console.log('加载设备列表，API调用参数:', params)

    const response = await getDeviceList(params)

    if (response.data && response.data.records) {
      // 转换数据格式以适应表格显示
      deviceList.value = response.data.records.map((device, index) => ({
        index: index + 1,
        deviceName: device.deviceName || '未知设备',
        tag: device.tag || '监控设备',
        deviceId: device.id || '',
        streamPath: device.streamPath || device.streamUrl || '',
        status: device.status,
        lastRefreshTime: device.lastRefreshTime || device.updatedAt || new Date().toLocaleString(),
        // 保留原始设备数据
        deviceData: device
      }))
      totalRecords.value = response.data.total || deviceList.value.length
    } else {
      deviceList.value = []
      totalRecords.value = 0
    }
  } catch (error) {
    console.error('加载设备列表失败:', error)
    ElMessage.error('加载设备列表失败: ' + (error.message || '网络错误'))
    deviceList.value = []
    totalRecords.value = 0
  } finally {
    loading.value = false
  }
}

// 获取设备的视频记录
const getDeviceVideoRecords = async (deviceId, date) => {
  try {
    console.log('获取设备视频记录:', deviceId, date)
    const params = {
      date: date, // 传递日期参数
      pageSize: 100, // 设置页面大小
      currentPage: 1
    }
    const response = await getDeviceRecords(deviceId, params)

    if (response.data && Array.isArray(response.data)) {
      return response.data
    } else {
      return []
    }
  } catch (error) {
    console.error('获取设备视频记录失败:', error)
    // 不在这里显示错误提示，让调用方统一处理
    throw error
  }
}

// 模拟视频列表数据
const mockVideoList = ref([
  { timeRange: '10:20:00 - 10:30:00' },
  { timeRange: '10:07:00 - 10:10:00' },
  { timeRange: '10:07:00 - 10:10:00' }
])

// 方法
const toggleDeviceTree = () => {
  console.log('toggleDeviceTree 被调用，当前状态:', deviceTreeCollapsed.value)
  deviceTreeCollapsed.value = !deviceTreeCollapsed.value
  console.log('toggleDeviceTree 新状态:', deviceTreeCollapsed.value)
  ElMessage.info(`设备树${deviceTreeCollapsed.value ? '已折叠' : '已展开'}`)
}

const handleSearch = () => {
  currentPage.value = 1
  loadDeviceList()
  ElMessage.success('搜索完成')
}

const handleReset = () => {
  searchForm.fileName = ''
  searchForm.recordType = ''
  searchForm.dateRange = [getCurrentDate(), getCurrentDate()]
  currentPage.value = 1
  loadDeviceList()
  ElMessage.info('已重置搜索条件')
}

// 高级搜索相关方法
const handleAdvancedSearch = (searchData) => {
  console.log('高级搜索:', searchData)

  // 更新搜索表单
  if (searchData.keyword) {
    searchForm.fileName = searchData.keyword
  }
  if (searchData.deviceName) {
    searchForm.fileName = searchData.deviceName
  }
  if (searchData.deviceId) {
    searchForm.fileName = searchData.deviceId
  }
  if (searchData.selectedTags && searchData.selectedTags.length > 0) {
    searchForm.recordType = searchData.selectedTags[0]
  }
  if (searchData.dateRange && searchData.dateRange.length > 0) {
    searchForm.dateRange = searchData.dateRange
  }

  currentPage.value = 1
  loadDeviceList()
  ElMessage.success('高级搜索完成')
}

const handleAdvancedSearchReset = () => {
  console.log('重置高级搜索')
  searchForm.fileName = ''
  searchForm.recordType = ''
  searchForm.dateRange = [getCurrentDate(), getCurrentDate()]
  currentPage.value = 1
  loadDeviceList()
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

const handleNodeClick = (data) => {
  if (data.type === 'device') {
    console.log('选择设备:', data)
    // 根据设备过滤视频
    searchForm.fileName = data.label
    handleSearch()
  }
}

const handleDeviceTreeSearch = (keyword) => {
  console.log('设备树搜索:', keyword)
  ElMessage.info(`搜索设备: ${keyword}`)
}

const showTableView = () => {
  showPlayerView.value = false
  selectedRow.value = null
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleRowClick = (row) => {
  selectedRow.value = row
}

const handleDownload = (row) => {
  console.log('下载视频:', row.name)
  ElMessage.success(`开始下载: ${row.name}`)
}

const handleShare = () => {
  if (selectedRow.value) {
    ElMessage.success('分享链接已复制到剪贴板')
  } else if (selectedRows.value.length > 0) {
    ElMessage.success('分享链接已复制到剪贴板')
  }
}

const handlePlayVideo = (video) => {
  currentVideo.value = video
  currentVideoUrl.value = video.url
  showPlayerView.value = true
  console.log('播放视频:', video)
}

const handleDownloadVideo = (video) => {
  console.log('下载视频:', video)
  ElMessage.success(`开始下载 ${video.name}`)
}

const handleAdd = () => {
  ElMessage.info('新增设备')
}

const handleEdit = () => {
  const targetRow = selectedRow.value || (selectedRows.value.length > 0 ? selectedRows.value[0] : null)

  if (!targetRow) {
    ElMessage.warning('请先选择要编辑的设备')
    return
  }

  ElMessage.info(`编辑设备: ${targetRow.name}`)
}

const handleDelete = async () => {
  const targetRows = selectedRow.value ? [selectedRow.value] : selectedRows.value

  if (!targetRows || targetRows.length === 0) {
    ElMessage.warning('请先选择要删除的设备')
    return
  }

  try {
    const message = targetRows.length === 1
      ? `确定要删除设备 "${targetRows[0].name}" 吗？`
      : `确定要删除选中的 ${targetRows.length} 个设备吗？`

    await ElMessageBox.confirm(
      message,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    ElMessage.success(`删除成功，共删除 ${targetRows.length} 个设备`)
    selectedRow.value = null
    selectedRows.value = []
  } catch {
    ElMessage.info('已取消删除')
  }
}

const handlePlay = async (row) => {
  console.log('播放设备视频记录:', row)

  // 只显示播放弹窗，不跳转到内嵌页面
  selectedRow.value = row
  videoDialogVisible.value = true

  // 初始化日期选择器为当前日期
  const now = new Date()
  selectedDate.year = now.getFullYear()
  selectedDate.month = now.getMonth() + 1
  selectedDate.day = now.getDate()

  // 自动加载当前日期的视频记录
  await loadVideoRecords()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  // 客户端分页，不需要重新加载数据
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  // 客户端分页，不需要重新加载数据
}

// 播放器控制
const playPause = () => {
  if (videoPlayer.value) {
    if (isPlaying.value) {
      videoPlayer.value.pause()
    } else {
      videoPlayer.value.play()
    }
    isPlaying.value = !isPlaying.value
  }
}

const stopVideo = () => {
  if (videoPlayer.value) {
    videoPlayer.value.pause()
    videoPlayer.value.currentTime = 0
    isPlaying.value = false
  }
}

const seekVideo = (value) => {
  if (videoPlayer.value) {
    videoPlayer.value.currentTime = value
  }
}

const downloadVideo = () => {
  if (currentVideo.value) {
    ElMessage.success(`开始下载 ${currentVideo.value.deviceName} 的视频流`)
  }
}

// 辅助方法
const formatTime = (seconds) => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = Math.floor(seconds % 60)
  return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 选择视频
const selectVideo = (index) => {
  selectedVideoIndex.value = index
  const selectedVideoData = mockVideoList.value[index]
  console.log('选择视频:', selectedVideoData)

  if (selectedVideoData && selectedVideoData.record) {
    // 更新当前视频信息
    currentVideo.value = {
      ...selectedRow.value,
      fileName: selectedVideoData.fileName,
      recordStartTime: selectedVideoData.record.recordStartTime,
      recordEndTime: selectedVideoData.record.recordEndTime,
      filePath: selectedVideoData.filePath
    }

    // 设置播放URL - 这里需要确保路径正确
    if (selectedVideoData.filePath) {
      // 将文件路径转换为可访问的URL
      // 从完整路径中提取相对于recordings目录的路径
      let relativePath = selectedVideoData.filePath

      // 处理Windows路径格式
      if (relativePath.includes('\\')) {
        relativePath = relativePath.replace(/\\/g, '/')
      }

      // 提取recordings目录后的路径
      const recordingsIndex = relativePath.indexOf('/recordings/')
      if (recordingsIndex !== -1) {
        relativePath = relativePath.substring(recordingsIndex + '/recordings/'.length)
      }

      // 对路径进行URL编码，处理中文文件名
      const encodedPath = relativePath.split('/').map(segment => encodeURIComponent(segment)).join('/')

      // 构建完整的播放URL - 使用正确的API路径
      currentVideoUrl.value = getBaseURL() + `/video-record/file/${encodeURIComponent(relativePath)}`
      console.log('设置播放URL:', currentVideoUrl.value)
    } else {
      currentVideoUrl.value = ''
      ElMessage.warning('视频文件路径不存在')
    }
  }
}

// 判断是否为HTTP流
const isHttpStream = (streamPath) => {
  return streamPath && streamPath.startsWith('http')
}

// 判断是否为RTSP流
const isRtspStream = (streamPath) => {
  return streamPath && streamPath.startsWith('rtsp')
}

// 获取当前时间
const getCurrentTime = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  return `${year}年${month}月${day}日 ${hours}:${minutes}:${seconds}`
}

// 获取缩略图URL
const getThumbnailUrl = (thumbnailPath) => {
  if (!thumbnailPath) return null

  // 处理Windows路径格式
  let relativePath = thumbnailPath
  if (relativePath.includes('\\')) {
    relativePath = relativePath.replace(/\\/g, '/')
  }

  // 提取recordings目录后的路径
  const recordingsIndex = relativePath.indexOf('/recordings/')
  if (recordingsIndex !== -1) {
    relativePath = relativePath.substring(recordingsIndex + '/recordings/'.length)
  } else {
    // 如果没有找到/recordings/，尝试从完整路径中提取相对部分
    // 假设recordings目录在VLStream-server下
    const serverIndex = relativePath.indexOf('/VLStream-server/recordings/')
    if (serverIndex !== -1) {
      relativePath = relativePath.substring(serverIndex + '/VLStream-server/recordings/'.length)
    } else {
      // 如果还是没找到，尝试从Windows路径中提取
      const windowsRecordingsIndex = relativePath.indexOf('VLStream-server\\recordings\\')
      if (windowsRecordingsIndex !== -1) {
        relativePath = relativePath.substring(windowsRecordingsIndex + 'VLStream-server\\recordings\\'.length)
        relativePath = relativePath.replace(/\\/g, '/')
      } else {
        // 如果路径已经是相对路径（不包含完整路径），直接使用
        if (!relativePath.includes(':/') && !relativePath.includes(':\\')) {
          // 这已经是相对路径，直接使用
        } else {
          // 最后尝试直接提取文件名部分
          const lastSlashIndex = relativePath.lastIndexOf('/')
          if (lastSlashIndex !== -1) {
            relativePath = relativePath.substring(lastSlashIndex + 1)
          }
        }
      }
    }
  }

  console.log('Original thumbnailPath:', thumbnailPath)
  console.log('Processed thumbnail relativePath:', relativePath)

  // 使用相对路径通过代理访问，不进行URL编码
  return `/api/video-record/thumbnail/${relativePath}`
}

// 获取视频文件URL
const getVideoFileUrl = (filePath) => {
  if (!filePath) return null

  // 处理Windows路径格式
  let relativePath = filePath
  if (relativePath.includes('\\')) {
    relativePath = relativePath.replace(/\\/g, '/')
  }

  // 提取recordings目录后的路径
  const recordingsIndex = relativePath.indexOf('/recordings/')
  if (recordingsIndex !== -1) {
    relativePath = relativePath.substring(recordingsIndex + '/recordings/'.length)
  } else {
    // 如果没有找到/recordings/，尝试从完整路径中提取相对部分
    // 假设recordings目录在VLStream-server下
    const serverIndex = relativePath.indexOf('/VLStream-server/recordings/')
    if (serverIndex !== -1) {
      relativePath = relativePath.substring(serverIndex + '/VLStream-server/recordings/'.length)
    } else {
      // 如果还是没找到，尝试从Windows路径中提取
      const windowsRecordingsIndex = relativePath.indexOf('VLStream-server\\recordings\\')
      if (windowsRecordingsIndex !== -1) {
        relativePath = relativePath.substring(windowsRecordingsIndex + 'VLStream-server\\recordings\\'.length)
        relativePath = relativePath.replace(/\\/g, '/')
      } else {
        // 如果路径已经是相对路径（不包含完整路径），直接使用
        if (!relativePath.includes(':/') && !relativePath.includes(':\\')) {
          // 这已经是相对路径，直接使用
        } else {
          // 最后尝试直接提取文件名部分
          const lastSlashIndex = relativePath.lastIndexOf('/')
          if (lastSlashIndex !== -1) {
            relativePath = relativePath.substring(lastSlashIndex + 1)
          }
        }
      }
    }
  }

  console.log('Original filePath:', filePath)
  console.log('Processed relativePath:', relativePath)

  // 使用相对路径通过代理访问，不进行URL编码
  return `/api/video-record/file/${relativePath}`
}

// 处理缩略图加载错误
const handleThumbnailError = (event, index) => {
  console.log('缩略图加载失败:', event.target.src)
  // 隐藏图片元素，显示占位符
  event.target.style.display = 'none'
  const thumbnailPlaceholder = event.target.parentElement.querySelector('.thumbnail-placeholder')
  if (thumbnailPlaceholder) {
    thumbnailPlaceholder.style.display = 'flex'
  }

  // 同时在数据中标记缩略图加载失败
  if (mockVideoList.value[index]) {
    mockVideoList.value[index].thumbnailUrl = null
  }
}

// 复制RTSP地址
const copyRtspUrl = () => {
  if (selectedRow.value && selectedRow.value.streamPath) {
    navigator.clipboard.writeText(selectedRow.value.streamPath).then(() => {
      ElMessage.success('RTSP地址已复制到剪贴板')
    }).catch(() => {
      ElMessage.error('复制失败，请手动复制')
    })
  }
}

// 在VLC中打开
const openInVlc = () => {
  if (selectedRow.value && selectedRow.value.streamPath) {
    const vlcUrl = `vlc://${selectedRow.value.streamPath}`
    window.open(vlcUrl, '_blank')
    ElMessage.info('正在尝试在VLC中打开，请确保已安装VLC播放器')
  }
}

// 刷新流
const refreshStream = () => {
  ElMessage.info('正在刷新视频流...')
  // 这里可以添加刷新流的逻辑
}

// 切换全屏
const toggleFullscreen = () => {
  const videoContainer = document.querySelector('.video-player')
  if (videoContainer) {
    if (document.fullscreenElement) {
      document.exitFullscreen()
    } else {
      videoContainer.requestFullscreen()
    }
  }
}

// 处理录制视频加载成功
const handleVideoLoaded = () => {
  console.log('录制视频加载成功')
  ElMessage.success('视频加载成功')
}

// 处理录制视频加载错误
const handleVideoError = (event) => {
  console.error('录制视频加载失败:', event)
  ElMessage.error('视频加载失败，请检查文件是否存在')
}

// 格式化录制时间
const formatRecordTime = (startTime, endTime) => {
  if (!startTime || !endTime) return ''

  const start = new Date(startTime).toLocaleTimeString('zh-CN', { hour12: false })
  const end = new Date(endTime).toLocaleTimeString('zh-CN', { hour12: false })
  return `${start} - ${end}`
}

// 日期选择器方法
const selectYear = (year) => {
  selectedDate.year = year
  // 检查选中的日期是否在新年份的有效范围内
  const maxDay = new Date(year, selectedDate.month, 0).getDate()
  if (selectedDate.day > maxDay) {
    selectedDate.day = maxDay
  }
  // 重新获取视频记录
  if (selectedRow.value) {
    loadVideoRecords()
  }
}

const selectMonth = (month) => {
  selectedDate.month = month
  // 检查选中的日期是否在新月份的有效范围内
  const maxDay = new Date(selectedDate.year, month, 0).getDate()
  if (selectedDate.day > maxDay) {
    selectedDate.day = maxDay
  }
  // 重新获取视频记录
  if (selectedRow.value) {
    loadVideoRecords()
  }
}

const selectDay = (day) => {
  selectedDate.day = day
  // 重新获取视频记录
  if (selectedRow.value) {
    loadVideoRecords()
  }
}

// 加载视频记录
const loadVideoRecords = async () => {
  if (!selectedRow.value) return

  try {
    const deviceId = selectedRow.value.deviceData?.id || selectedRow.value.deviceId
    const records = await getDeviceVideoRecords(deviceId, selectedDateStr.value)

    if (records && records.length > 0) {
      videoRecords.value = records
      // 转换为视频列表格式
      mockVideoList.value = records.map(record => ({
        timeRange: `${record.recordStartTime?.substring(11, 19) || '--:--:--'} - ${record.recordEndTime?.substring(11, 19) || '--:--:--'}`,
        filePath: record.filePath,
        fileName: record.fileName,
        id: record.id,
        thumbnailUrl: getThumbnailUrl(record.thumbnailPath), // 生成缩略图URL
        record: record // 保留完整的记录数据
      }))

      // 设置第一个视频为当前播放项
      const firstRecord = records[0]
      currentVideo.value = {
        ...selectedRow.value,
        fileName: firstRecord.fileName,
        recordStartTime: firstRecord.recordStartTime,
        recordEndTime: firstRecord.recordEndTime,
        filePath: firstRecord.filePath
      }

      // 设置播放URL
      if (firstRecord.filePath) {
        // 将文件路径转换为可访问的URL
        // 从完整路径中提取相对于recordings目录的路径
        let relativePath = firstRecord.filePath

        // 处理Windows路径格式
        if (relativePath.includes('\\')) {
          relativePath = relativePath.replace(/\\/g, '/')
        }

        // 提取recordings目录后的路径
        const recordingsIndex = relativePath.indexOf('/recordings/')
        if (recordingsIndex !== -1) {
          relativePath = relativePath.substring(recordingsIndex + '/recordings/'.length)
        }

        // 构建完整的播放URL - 使用新的函数
        currentVideoUrl.value = getVideoFileUrl(relativePath)
        console.log('初始播放URL:', currentVideoUrl.value)

        // 只在有视频记录时才显示成功提示
        if (videoRecords.value.length > 0) {
          ElMessage.success(`找到 ${records.length} 条视频记录`)
        }
      } else {
        currentVideoUrl.value = ''
        ElMessage.warning('视频文件路径不存在')
      }
    } else {
      videoRecords.value = []
      mockVideoList.value = []
      currentVideoUrl.value = ''
      ElMessage.info(`${selectedDateStr.value} 没有视频记录`)
    }
  } catch (error) {
    console.error('加载视频记录失败:', error)
    videoRecords.value = []
    mockVideoList.value = []
    currentVideoUrl.value = ''

    // 统一的错误处理，只显示一个友好的提示
    ElMessage.info(`${selectedDateStr.value} 没有视频记录`)
  }
}

// 页面初始化
onMounted(async () => {
  await loadDeviceTree()
  await loadDeviceList()
})
</script>

<style scoped>
/* 视频回放页面样式 - 按照rules规范 */
.video-playback {
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  min-height: 100vh;
}

/* 搜索表单样式 - 查询栏背景颜色：#F0F2F5 */
.search-form-container {
  background: #F0F2F5;
  padding: 20px;
  margin-bottom: 0;
  border-radius: 8px 8px 0 0;
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
}

.search-item {
  flex: 1;
  min-width: 200px;
}

.search-item:first-child {
  width: 240px;
  flex: none;
}

.search-item:nth-child(2) {
  min-width: 180px;
  max-width: 200px;
}

.search-item:nth-child(3) {
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

/* 主要内容区域 - 容器无缝连接，零间隙 */
.main-content {
  flex: 1;
  display: flex;
  gap: 0;
  min-height: 0;
}

/* 左侧设备树 */
.device-tree-container {
  width: 280px;
  background: white;
  border-radius: 0 0 0 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  overflow: hidden;
  flex-shrink: 0;
  border-right: 1px solid #f0f0f0;
}

/* 设备树展开状态（默认） */
.device-tree-container:not(.collapsed) {
  width: 280px !important;
  min-width: 280px !important;
  opacity: 1;
  transform: translateX(0);
  visibility: visible;
}

/* 设备树折叠状态 */
.device-tree-container.collapsed {
  width: 0 !important;
  min-width: 0 !important;
  margin-right: 0 !important;
  opacity: 0;
  transform: translateX(-280px);
  visibility: hidden;
  border-right: none;
}

/* 设备树折叠时，内容区域的圆角调整 */
.device-tree-container.collapsed + .content-area .content-wrapper {
  border-radius: 0 0 8px 8px;
}

/* 右侧内容区域 */
.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* 内容包装器 - 白色内容区域使用统一的圆角8px和阴影效果 */
.content-wrapper {
  background: white;
  border-radius: 0 0 8px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
}

/* 导航栏 - 统一的padding: 20px确保内容对齐 */
.content-header {
  background: white;
  border-bottom: 1px solid #e8e8e8;
  padding: 20px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
}

.expand-device-tree-btn {
  background: #ffffff;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
  margin-right: 12px;
  width: 32.15px;
  height: 28.29px;
}

.expand-device-tree-btn:hover {
  background: #f0f4ff;
  border-color: #1A53FF;
  transform: scale(1.05);
}

/* 折叠按钮内部样式优化 */
.expand-device-tree-btn :deep(.collapse-toggle) {
  width: 100%;
  height: 100%;
}

.expand-device-tree-btn :deep(.toggle-icon) {
  width: 24px;
  height: 21px;
}

.expand-device-tree-btn :deep(.line) {
  height: 1.5px;
}

.expand-device-tree-btn :deep(.arrow) {
  stroke-width: 1.5px;
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

/* 表格视图 */
.table-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 搜索区域 */
.search-section {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.table-content {
  flex: 1;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 表格滚动条样式 */
.table-content .el-table {
  flex: 1;
  overflow: auto;
}

.table-content .el-table__body-wrapper {
  overflow-y: auto !important;
  overflow-x: auto !important;
}

/* 自定义滚动条样式 */
.table-content .el-table__body-wrapper::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.table-content .el-table__body-wrapper::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.table-content .el-table__body-wrapper::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.table-content .el-table__body-wrapper::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 操作按钮样式 - 操作列的按钮总是保持在一行显示 */
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
}

/* 分页容器 - 紧贴表格数据，参考DeviceManagement */
.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 播放器视图 */
.player-view {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.player-content {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.section {
  margin-bottom: 24px;
}

.section-title {
  color: #303133;
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 16px 0;
  padding-bottom: 8px;
  border-bottom: 2px solid #1A53FF;
}

.video-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.info-row {
  display: flex;
  align-items: center;
}

.info-label {
  width: 100px;
  color: #606266;
  font-size: 14px;
}

.info-value {
  color: #303133;
  font-size: 14px;
}

.player-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.video-screen {
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.player-controls {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.control-buttons {
  display: flex;
  justify-content: center;
}

.timeline-container {
  display: flex;
  align-items: center;
  gap: 16px;
}

.timeline {
  flex: 1;
}

.time-display {
  color: #606266;
  font-size: 14px;
  min-width: 120px;
  text-align: center;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 0;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.form-actions .action-btn {
  width: 82px;
  height: 36px;
  font-size: 14px;
  border-radius: 18px;
}

.form-actions .action-btn:first-child {
  border-radius: 18px 0 0 18px;
}

.form-actions .action-btn:last-child {
  border-radius: 0 18px 18px 0;
  border-left: none;
}

/* 视频回放弹窗样式 */
.video-dialog {
  .el-dialog__body {
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

/* 左侧视频列表区域 */
.video-list-section {
  width: 420px;
  background: white;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 日期选择器 */
.date-selector {
  display: flex;
  flex-direction: row;
  gap: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.date-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  text-align: center;
  padding-bottom: 8px;
  border-bottom: 1px solid #e9ecef;
}

.year-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.year-item {
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #666;
  transition: all 0.3s;
  text-align: center;
  border: 1px solid transparent;
}

.year-item.active {
  background: #1A53FF;
  color: white;
  border-color: #1A53FF;
}

.year-item:hover:not(.active) {
  background: #f0f4ff;
  color: #1A53FF;
  border-color: #1A53FF;
}

.month-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

.date-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  max-height: 180px;
  overflow-y: auto;
}

.month-item,
.date-item {
  width: 100%;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  color: #666;
  transition: all 0.3s;
  border: 1px solid transparent;
}

.month-item.active,
.date-item.active {
  background: #1A53FF;
  color: white;
  border-color: #1A53FF;
}

.month-item:hover:not(.active),
.date-item:hover:not(.active) {
  background: #f0f4ff;
  color: #1A53FF;
  border-color: #1A53FF;
}

/* 视频列表标题 */
.video-list-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

/* 视频缩略图列表 */
.video-thumbnails {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  max-height: 350px; /* 限制高度，约3条记录的高度：110*3 + 12*2 = 354px */
  padding-right: 8px; /* 为滚动条留出空间 */
  min-height: 120px; /* 最小高度 */
}

/* 自定义滚动条样式 */
.video-thumbnails::-webkit-scrollbar {
  width: 6px;
}

.video-thumbnails::-webkit-scrollbar-track {
  background: #f0f0f0;
  border-radius: 3px;
}

.video-thumbnails::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
  transition: background 0.3s;
}

.video-thumbnails::-webkit-scrollbar-thumb:hover {
  background: #909399;
}

.video-thumbnail-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  border-radius: 6px;
  overflow: hidden;
  transition: all 0.3s;
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #e4e7ed;
  min-height: 110px; /* 保证每个项目的最小高度 */
  flex-shrink: 0; /* 防止项目被压缩 */
}

.video-thumbnail-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-color: #1A53FF;
}

.video-thumbnail-item.active {
  border: 2px solid #1A53FF;
  background: #f0f8ff;
  transform: translateY(-1px);
}

.thumbnail-image {
  position: relative;
  width: 100%;
  height: 90px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.thumbnail-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-image .thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.thumbnail-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eaed 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-icon {
  font-size: 24px;
  color: #c0c4cc;
}

.play-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 32px;
  height: 32px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  opacity: 0;
  transition: opacity 0.3s;
}

.video-thumbnail-item:hover .play-overlay {
  opacity: 1;
}

.video-time-range {
  font-size: 12px;
  color: #666;
  text-align: center;
  padding: 4px 8px;
  background: #f5f7fa;
  border-radius: 0 0 6px 6px;
  font-weight: 500;
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
  display: flex;
  flex-direction: column;
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
}

.control-buttons {
  display: flex;
  align-items: center;
  gap: 15px;
}

.control-btn {
  background: transparent !important;
  border: none !important;
  color: white !important;
  padding: 4px !important;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.1) !important;
}

.time-display {
  font-size: 14px;
  color: white;
  min-width: 60px;
}

.volume-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.volume-icon {
  font-size: 16px;
  color: white;
}

.volume-text {
  font-size: 14px;
  color: white;
  min-width: 20px;
}

.fullscreen-control {
  margin-left: auto;
}

.fullscreen-icon {
  font-size: 18px;
  color: white;
  cursor: pointer;
  transition: all 0.3s;
}

.fullscreen-icon:hover {
  color: #1A53FF;
}

/* 新增的视频播放区域样式 */
.video-display {
  position: relative;
  width: 100%;
  flex: 1;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-info-overlay {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 8px 12px;
  border-radius: 4px;
  z-index: 10;
  font-size: 14px;
}

.video-info-overlay .device-name {
  font-weight: bold;
  margin-bottom: 2px;
}

.video-info-overlay .video-time {
  font-size: 12px;
  opacity: 0.8;
}

/* HTTP流容器 */
.http-stream-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.stream-iframe {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 8px;
}

/* RTSP流容器 */
.rtsp-stream-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
}

.rtsp-info {
  text-align: center;
  padding: 20px;
  max-width: 500px;
}

.rtsp-title {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 12px;
}

.rtsp-url {
  font-family: monospace;
  font-size: 12px;
  color: #606266;
  background: #f0f0f0;
  padding: 8px 12px;
  border-radius: 4px;
  margin-bottom: 16px;
  word-break: break-all;
}

.rtsp-note {
  text-align: left;
  color: #606266;
  font-size: 14px;
  margin-bottom: 20px;
}

.rtsp-note p {
  margin: 8px 0;
}

.rtsp-note ul {
  margin: 8px 0;
  padding-left: 20px;
}

.rtsp-note li {
  margin: 4px 0;
}

.rtsp-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* 默认占位符样式更新 */
.video-placeholder .placeholder-content {
  text-align: center;
  color: #909399;
}

.video-placeholder .placeholder-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.video-placeholder .placeholder-text {
  font-size: 16px;
}

/* 录制视频播放器样式 */
.recorded-video-container {
  width: 100%;
  height: 100%;
}

.recorded-video-player {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 8px;
  min-height: 400px;
}

/* 视频信息显示样式 */
.video-info {
  flex-shrink: 0;
  margin-top: 12px;
  padding: 10px 16px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.video-info-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.video-info-item:last-child {
  margin-bottom: 0;
}

.video-info .info-label {
  font-weight: 500;
  color: #666;
  min-width: 60px;
}

.video-info .info-value {
  color: #333;
  flex: 1;
  word-break: break-all;
}
</style>