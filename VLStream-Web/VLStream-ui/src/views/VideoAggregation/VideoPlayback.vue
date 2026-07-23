<template>
  <div class="video-playback tenant_Page draHeaPB">
    <div class="tenant_content">
      <div class="tableTenBox flexRowAC">
        <!-- 左侧设备树 -->
        <div
          v-show="!deviceTreeCollapsed"
          v-yResize
          class="police_aside_use"
        >
          <div class="treeTitle">
            设备树
          </div>
          <div class="tree_search_content flexRowAC">
            <el-input
              v-model="searchTreeKeyword"
              placeholder="搜索"
              debounce="300"
              prefix-icon="Search"
              clearable
              @input="handleTreeSearch"
            />
          </div>
          <el-tree
            ref="deviceTreeRef"
            style="background: #fff;"
            :data="filteredDeviceTreeData"
            highlight-current
            node-key="id"
            default-expand-all
            :props="treeDefaultProps"
            :expand-on-click-node="false"
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div
                class="custom-tree-node flexRowAC"
                @mouseenter="hoveredTreeNodeId = data.id"
                @mouseleave="hoveredTreeNodeId = null"
              >
                <div class="tree-node-main flexRowAC">
                  <el-icon v-if="data.type === 'tag'" class="tree-icon tag-icon">
                    <Collection />
                  </el-icon>
                  <el-icon v-else-if="data.type === 'device'" class="tree-icon device-icon">
                    <VideoCamera />
                  </el-icon>
                  <el-icon v-else class="tree-icon">
                    <Folder />
                  </el-icon>
                  <el-tooltip
                    :open-delay="500"
                    class="item"
                    effect="light"
                    :content="node.label"
                    placement="top"
                  >
                    <div
                      class="tree-node-label"
                      :class="{
                        activeDept: data.id === currentTreeNodeId,
                        'device-offline': data.type === 'device' && data.status !== 'online' && data.status !== 1
                      }"
                    >
                      {{ node.label }}
                    </div>
                  </el-tooltip>
                </div>
                <div
                  v-show="hoveredTreeNodeId === data.id || data.id === currentTreeNodeId"
                  class="tree-node-actions flexRowAC"
                  @click.stop
                >
                  <oort-svg-icon
                    width="16"
                    height="16"
                    name="delete"
                    color="red"
                    class="tree-action-icon delete-icon"
                    @click="handleTreeDelete(data)"
                  />
                  <oort-svg-icon
                    width="16"
                    height="16"
                    name="add"
                    class="tree-action-icon add-icon"
                    @click="handleTreeAdd(data)"
                  />
                </div>
              </div>
            </template>
          </el-tree>
        </div>

        <!-- 右侧表格区域 -->
        <div class="tableTenItU">
          <div class="depNameBox_out flexRowAC">
            <div class="depNameBox flexRowAC">
              <CollapseToggle
                v-if="deviceTreeCollapsed"
                class="expand-device-tree-btn"
                :is-expanded="false"
                @toggle="toggleDeviceTree"
              />
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
            v-loading="loading"
            class="new_table"
            header-cell-class-name="header_tenant_cell"
            stripe
            :data="currentPageData"
            current-row-key="deviceId"
            element-loading-text="正在加载设备数据..."
            @selection-change="handleSelectionChange"
            @row-click="handleRowClick"
          >
            <el-table-column type="selection" :width="clacPXToVW(55)" />
            <el-table-column label="序号" :width="clacPXToVW(65)">
              <template #default="scope">
                {{ scope.$index + (currentPage - 1) * pageSize + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="deviceName" label="设备名称" :width="clacPXToVW(140)" show-overflow-tooltip />
            <el-table-column prop="tag" label="标签" :width="clacPXToVW(120)">
              <template #default="scope">
                <el-tag size="small" type="primary" class="tag_pill">
                  {{ scope.row.tag }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deviceId" label="设备ID" :width="clacPXToVW(140)" show-overflow-tooltip />
            <el-table-column prop="streamPath" label="视频流路径" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" :width="clacPXToVW(100)">
              <template #default="scope">
                <span v-if="scope.row.status === 1" class="staBtns WX">在线</span>
                <span v-else class="staBtns">离线</span>
              </template>
            </el-table-column>
            <el-table-column prop="lastRefreshTime" label="最近一次录制时间" :width="clacPXToVW(180)" />
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
              :total="filteredData.length"
              layout="total, prev, pager, next, sizes"
              class="justifyAlign"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 视频回放弹窗 -->
    <el-dialog
      v-model="videoDialogVisible"
      title="视频回放"
      width="70%"
      :close-on-click-modal="false"
      class="video-dialog"
      align-center
    >
      <div class="video-playback-container">
        <!-- 顶部：年 / 月 / 日 选择 -->
        <div class="date-selector">
          <div class="date-section year-section">
            <div class="year-grid">
              <span
                v-for="year in availableYears"
                :key="year"
                class="date-cell year-item"
                :class="{ active: selectedDate.year === year }"
                @click="selectYear(year)"
              >
                {{ year }}
              </span>
            </div>
          </div>

          <div class="date-section month-section">
            <div class="month-grid">
              <span
                v-for="month in availableMonths"
                :key="month"
                class="date-cell month-item"
                :class="{ active: selectedDate.month === month }"
                @click="selectMonth(month)"
              >
                {{ month }}
              </span>
            </div>
          </div>

          <div class="date-section day-section">
            <div class="day-grid">
              <span
                v-for="day in availableDays"
                :key="day"
                class="date-cell day-item"
                :class="{ active: selectedDate.day === day }"
                @click="selectDay(day)"
              >
                {{ day }}
              </span>
            </div>
          </div>
        </div>

        <!-- 底部：视频列表 + 播放器 -->
        <div class="playback-body">
          <div class="video-list-section">
            <div class="video-list-title">视频列表</div>
            <div class="video-thumbnails">
              <div
                v-for="(video, index) in videoList"
                :key="index"
                class="video-thumbnail-item"
                :class="{ active: selectedVideoIndex === index }"
                @click="selectVideo(index)"
              >
                <div class="thumbnail-image">
                  <img
                    v-if="video.thumbnailUrl"
                    :src="video.thumbnailUrl"
                    :alt="video.fileName"
                    class="thumbnail-img"
                    @error="handleThumbnailError($event, index)"
                  />
                  <div
                    class="thumbnail-placeholder"
                    :style="{ display: video.thumbnailUrl ? 'none' : 'flex' }"
                  >
                    <el-icon class="video-icon"><VideoCamera /></el-icon>
                  </div>
                </div>
                <div class="video-time-range">{{ video.timeRange }}</div>
              </div>
              <div v-if="!videoList.length" class="video-list-empty">暂无视频记录</div>
            </div>
          </div>

          <div class="player-section">
            <div class="video-player">
              <div class="video-display">
                <div v-if="currentVideoUrl" class="recorded-video-container">
                  <video
                    ref="recordedVideoPlayer"
                    :src="currentVideoUrl"
                    controls
                    autoplay
                    class="recorded-video-player"
                    @loadedmetadata="handleVideoLoaded"
                    @error="handleVideoError"
                  >
                    您的浏览器不支持视频播放
                  </video>
                  <div class="player-datetime" v-if="playerOverlayDateTime">
                    {{ playerOverlayDateTime }}
                  </div>
                  <div class="player-device-name" v-if="currentVideo?.deviceName">
                    {{ currentVideo.deviceName }}
                  </div>
                </div>

                <div v-else class="video-placeholder">
                  <div class="placeholder-content">
                    <el-icon class="placeholder-icon"><VideoCamera /></el-icon>
                    <div class="placeholder-text">请从左侧选择要播放的视频</div>
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
import { ref, reactive, computed, onMounted } from 'vue'
import {
  VideoCamera,
  Folder,
  Collection,
} from '@element-plus/icons-vue'
import CollapseToggle from '@/components/CollapseToggle.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clacPXToVW } from '@/utils/index'

// API 导入
import { getDeviceList, getDeviceTree } from '@/api/device'
import { getDeviceRecords } from '@/api/videoRecord'
import { getBaseURL } from '@/utils/request'

// 响应式数据
const deviceTreeCollapsed = ref(false)
const showPlayerView = ref(false)
const selectedRow = ref(null)
const selectedRows = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const exportItem = ref({
  isDisabledExcel: false
})

// 高级搜索配置
const searchData = ref([
  { label: '设备名称', value: 'keyword', type: 'text', default: '' },
  {
    label: '标签类型',
    value: 'recordType',
    type: 'select',
    default: '',
    option: [
      { label: '全部', value: '' },
      { label: '球机监控', value: '球机监控' },
      { label: '枪机监控', value: '枪机监控' },
      { label: '半球监控', value: '半球监控' },
      { label: '云台监控', value: '云台监控' }
    ]
  },
  {
    label: '时间',
    value: 'dateRange',
    type: 'daterange',
    format: 'YYYY-MM-DD',
    default: []
  }
])
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

// 设备树
const deviceTreeRef = ref(null)
const searchTreeKeyword = ref('')
const currentTreeNodeId = ref(null)
const hoveredTreeNodeId = ref(null)
const treeDefaultProps = {
  children: 'children',
  label: 'label'
}

// 过滤后的设备树数据
const filteredDeviceTreeData = computed(() => {
  if (!searchTreeKeyword.value) {
    return deviceTreeData.value
  }

  const keyword = searchTreeKeyword.value.toLowerCase()
  const filterNode = (nodes) => {
    return nodes.filter(node => {
      if (node.label?.toLowerCase().includes(keyword)) {
        return true
      }
      if (node.children?.length) {
        return filterNode(node.children).length > 0
      }
      return false
    }).map(node => {
      if (node.children?.length) {
        return {
          ...node,
          children: filterNode(node.children)
        }
      }
      return node
    })
  }

  return filterNode(deviceTreeData.value)
})

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

// 可选年份范围（展示近若干年，便于网格排布）
const availableYears = computed(() => {
  const currentYear = new Date().getFullYear()
  const years = []
  for (let i = currentYear - 5; i <= currentYear; i++) {
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

// 播放器右上角日期时间文案
const playerOverlayDateTime = computed(() => {
  if (!currentVideo.value) return ''
  const start = currentVideo.value.recordStartTime
  if (start) {
    const date = new Date(start)
    if (!Number.isNaN(date.getTime())) {
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      const hh = String(date.getHours()).padStart(2, '0')
      const mm = String(date.getMinutes()).padStart(2, '0')
      const ss = String(date.getSeconds()).padStart(2, '0')
      return `${y}年${m}月${d}日 ${hh}:${mm}:${ss}`
    }
  }
  const y = selectedDate.year
  const m = String(selectedDate.month).padStart(2, '0')
  const d = String(selectedDate.day).padStart(2, '0')
  return `${y}年${m}月${d}日`
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

// 仅由真实录像查询结果填充。
const videoList = ref([])

// 方法
const toggleDeviceTree = () => {
  deviceTreeCollapsed.value = !deviceTreeCollapsed.value
}

const handleTreeSearch = () => {
  // 本地过滤，无需额外请求
}

const handleSearch = () => {
  currentPage.value = 1
  loadDeviceList()
}

// 搜索 / 重置（search-height-box）
const searchResetFn = (val, reset) => {
  if (reset) currentPage.value = 1
  searchForm.fileName = val?.keyword || ''
  searchForm.recordType = val?.recordType || ''
  searchForm.dateRange = val?.dateRange?.length
    ? val.dateRange
    : [getCurrentDate(), getCurrentDate()]
  loadDeviceList()
}

const handleExport = (type) => {
  ElMessage.error(`当前未接入录像导出接口，未导出${type || ''}数据`)
}

const handleNodeClick = (data) => {
  currentTreeNodeId.value = data.id
  if (data.type === 'device') {
    searchForm.fileName = data.label
    currentPage.value = 1
    loadDeviceList()
  }
}

const handleTreeAdd = (data) => {
  ElMessage.info(`新增节点：${data.label}`)
}

const handleTreeDelete = async (data) => {
  try {
    await ElMessageBox.confirm(
      `确定删除「${data.label}」吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    ElMessage.success('删除成功')
  } catch {
    // 用户取消
  }
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
  const selectedVideoData = videoList.value[index]
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
  if (videoList.value[index]) {
    videoList.value[index].thumbnailUrl = null
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
      videoList.value = records.map(record => ({
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
      videoList.value = []
      currentVideoUrl.value = ''
      ElMessage.info(`${selectedDateStr.value} 没有视频记录`)
    }
  } catch (error) {
    console.error('加载视频记录失败:', error)
    videoRecords.value = []
    videoList.value = []
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

<style scoped lang="scss">
.tenant_Page {
  height: 100%;
  width: 100%;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
  background: #f0f2f5;

  .tenant_content {
    width: 100%;
    height: 100%;
    border-radius: 8px;
  }

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

.police_aside_use {
  width: 300px;
  padding-right: 20px;
  flex-shrink: 0;
  height: 100%;
  overflow: hidden;

  .treeTitle {
    color: var(--el-color-primary);
    padding-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 12px;
    padding-top: 4px;

    &::before {
      content: '';
      width: 3px;
      height: 18px;
      background-color: var(--el-color-primary);
    }
  }

  .tree_search_content {
    justify-content: center;
    padding-bottom: 10px;

    :deep(.el-input__wrapper) {
      background: #fff;
      box-shadow: none;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
    }

    :deep(.el-input__inner) {
      background: #fff;
      border: none;
    }
  }

  :deep(.el-tree-node__content) {
    --el-tree-node-hover-bg-color: var(--el-menu-hover-bg-color);
    height: 38px;
    font-size: 14px;
    color: #333;

    .custom-tree-node {
      width: 100%;
      justify-content: space-between;
      padding-right: 4px;
    }
  }

  :deep(.el-tree-node) {
    .el-tree-node.is-current.is-focusable > .el-tree-node__content {
      background-color: var(--el-color-primary-hb);
      color: var(--el-color-primary);
    }
  }

  :deep(.el-tree) {
    height: calc(100% - 80px);
    overflow: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }
}

.custom-tree-node {
  flex: 1;
  min-width: 0;
  gap: 4px;

  .tree-node-main {
    flex: 1;
    min-width: 0;
    gap: 4px;
    overflow: hidden;
  }

  .tree-node-label {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .tree-node-actions {
    flex-shrink: 0;
    gap: 8px;
    margin-left: 8px;
  }

  .tree-action-icon {
    cursor: pointer;
    flex-shrink: 0;

    &:hover {
      opacity: 0.75;
    }
  }

  .tree-icon {
    flex-shrink: 0;
    font-size: 14px;
    color: var(--el-color-primary);
  }

  .device-icon {
    color: #52C41A;
  }

  .tag-icon {
    color: #8581dc;
  }

  .activeDept {
    color: var(--el-color-primary);
  }

  .device-offline {
    color: #999;
  }
}

.tableTenItU {
  flex: 1;
  height: 100%;
  overflow: auto;
  min-width: 0;

  :deep(.el-table) {
    .el-table__header .el-table__cell .cell {
      background: #F8F8F9;
      font-size: 14px;
      color: #515A6E;
      line-height: 24px;
      font-weight: 700;
    }

    th.el-table-fixed-column--right {
      background-color: #F8F8F9;
    }
  }

  :deep(.header_tenant_cell) {
    background: #F8F8F9;
  }
}

.expand-device-tree-btn {
  background: #ffffff;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
  width: 32px;
  height: 28px;
}

.expand-device-tree-btn:hover {
  background: #f0f4ff;
  border-color: #1A53FF;
  transform: scale(1.05);
}

.paginationBox {
  justify-content: center;
  height: 100px;
}

.operateAppBox {
  justify-content: flex-end;
  gap: 2px;
}

.tag_pill {
  border-radius: 12px;
}

.staBtns {
  padding: 3px 7px;
  font-size: 14px;
  line-height: 14px;
  border-radius: 4px;
  position: relative;
  padding-left: 12px;

  &::before {
    content: "";
    display: flex;
    position: absolute;
    top: 6px;
    left: 0;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: gray;
  }

  &.WX {
    color: #52C41A;

    &::before {
      background: #52C41A;
    }
  }
}

/* 视频回放弹窗样式 */
.video-playback-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #fff;
  min-height: 640px;
}

/* 顶部年月日选择 */
.date-selector {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
}

.date-section {
  border: 1px solid #e8e8e8;
  border-radius: 2px;
  overflow: hidden;
  background: #e8e8e8;
}

.year-section {
  flex: 0 0 220px;
}

.month-section {
  flex: 0 0 260px;
}

.day-section {
  flex: 1;
  min-width: 0;
}

.year-grid,
.month-grid,
.day-grid {
  display: grid;
  width: 100%;
  gap: 1px;
  background: #e8e8e8;
}

.year-grid {
  grid-template-columns: repeat(3, 1fr);
}

.month-grid {
  grid-template-columns: repeat(4, 1fr);
}

.day-grid {
  grid-template-columns: repeat(11, 1fr);
}

.date-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  font-size: 14px;
  color: #303133;
  cursor: pointer;
  user-select: none;
  background: #fff;
  transition: background 0.2s, color 0.2s;
  box-sizing: border-box;
}

.date-cell:hover:not(.active) {
  background: #f5f9ff;
  color: #1890ff;
}

.date-cell.active {
  background: #e6f7ff;
  color: #1890ff;
  font-weight: 500;
}

/* 底部主体区域 */
.playback-body {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 480px;
}

/* 左侧视频列表 */
.video-list-section {
  width: 180px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.video-list-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  line-height: 1;
}

.video-thumbnails {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
  max-height: 480px;
  padding-right: 4px;
}

.video-thumbnails::-webkit-scrollbar {
  width: 4px;
}

.video-thumbnails::-webkit-scrollbar-track {
  background: transparent;
}

.video-thumbnails::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.video-list-empty {
  padding: 24px 0;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.video-thumbnail-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  cursor: pointer;
  flex-shrink: 0;
}

.thumbnail-image {
  position: relative;
  width: 100%;
  height: 90px;
  background: #f0f0f0;
  border-radius: 2px;
  overflow: hidden;
  border: 2px solid transparent;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.video-thumbnail-item.active .thumbnail-image {
  border-color: #1890ff;
}

.video-thumbnail-item:hover .thumbnail-image {
  border-color: #69b1ff;
}

.video-thumbnail-item.active:hover .thumbnail-image {
  border-color: #1890ff;
}

.thumbnail-image img,
.thumbnail-image .thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
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

.video-time-range {
  font-size: 12px;
  color: #8c8c8c;
  text-align: center;
  line-height: 1.2;
  word-break: break-all;
}

.video-thumbnail-item.active .video-time-range {
  color: #1890ff;
}

/* 右侧播放器 */
.player-section {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.video-player {
  flex: 1;
  background: #000;
  border-radius: 2px;
  overflow: hidden;
  position: relative;
  min-height: 480px;
}

.video-display {
  position: relative;
  width: 100%;
  height: 100%;
  background: #000;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.recorded-video-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.recorded-video-player {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
  min-height: 480px;
  background: #000;
}

.player-datetime {
  position: absolute;
  top: 12px;
  right: 16px;
  z-index: 2;
  color: #fff;
  font-size: 14px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.65);
  pointer-events: none;
}

.player-device-name {
  position: absolute;
  left: 16px;
  bottom: 56px;
  z-index: 2;
  color: #fff;
  font-size: 14px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.65);
  pointer-events: none;
}

.video-placeholder {
  width: 100%;
  height: 100%;
  min-height: 480px;
  background: #1a1a1a;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-placeholder .placeholder-content {
  text-align: center;
  color: #909399;
}

.video-placeholder .placeholder-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.video-placeholder .placeholder-text {
  font-size: 14px;
}
</style>

<style lang="scss">
/* el-dialog 挂载到 body，头部样式统一走全局 common.scss */
.video-dialog {
  .el-dialog__body {
    padding: 20px 40px;
  }
}
</style>
