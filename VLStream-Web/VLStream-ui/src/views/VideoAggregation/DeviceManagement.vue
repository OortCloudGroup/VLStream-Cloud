<template>
  <div class="device-management">
    <!-- 搜索表单区域 -->
    <!-- <div class="search-form-container">
      <div class="search-form">
        <div class="search-row">
          <div class="search-item">
            <el-input 
              v-model="searchForm.deviceName" 
              placeholder="请输入设备名称或设备ID"
              clearable
              style="width: 240px; flex: none;"
            />
          </div>
          <div class="search-item">
            <el-select 
              v-model="searchForm.tagName" 
              placeholder="请选择标签" 
              clearable 
              style="width: 200px"
            >
              <el-option label="全部" value="" />
              <el-option label="摄像头" value="摄像头" />
              <el-option label="云台" value="云台" />
              <el-option label="球机" value="球机" />
              <el-option label="枪机" value="枪机" />
              <el-option label="半球" value="半球" />
            </el-select>
          </div>
          <div class="search-item date-with-buttons">
            <DateRangePicker
              v-model="searchForm.dateRange"
              class="date-picker"
            />
            <div class="search-buttons">
              <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
              <el-button :icon="Refresh" @click="handleReset">重置</el-button>
            </div>
          </div>
        </div>
      </div>
    </div> -->

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
          :show-add-actions="true"
          :show-delete-actions="true"
          :show-bottom-actions="true"
          :collapsed="deviceTreeCollapsed"
          @node-click="handleNodeClick"
          @add-device="handleAddDevice"
          @delete-device="handleDeleteDevice"
          @bottom-add="handleBottomAdd"
          @bottom-delete="handleBottomDelete"
          @toggle-collapse="toggleDeviceTree"
          @search="handleDeviceTreeSearch"
        />
      </div>

      <!-- 右侧内容区域 -->
      <div class="table-container">
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
            <span class="breadcrumb-item" @click="showTableView">设备列表</span>
            <span v-if="treeFilterText" class="breadcrumb-separator">></span>
            <span v-if="treeFilterText" class="breadcrumb-item filter-text" @click="clearTreeFilter">{{ treeFilterText }}</span>
            <span v-if="showEditView" class="breadcrumb-separator">></span>
            <span v-if="showEditView" class="breadcrumb-item active">编辑设备</span>
            <span v-if="showCameraSettingsView" class="breadcrumb-separator">></span>
            <span v-if="showCameraSettingsView" class="breadcrumb-item active">摄像机设置</span>
            <span v-if="showConfigView" class="breadcrumb-separator">></span>
            <span v-if="showConfigView" class="breadcrumb-item active">配置参数</span>
            <span v-if="showAIEventView" class="breadcrumb-separator">></span>
<!--            <span v-if="showAIEventView" class="breadcrumb-item active">配置AI事件</span>-->
            <span v-if="showModelMarketView" class="breadcrumb-separator">></span>
<!--            <span v-if="showModelMarketView" class="breadcrumb-item" @click="backToAIEvent">配置AI事件</span>-->
            <span v-if="showModelMarketView" class="breadcrumb-separator">></span>
            <span v-if="showModelMarketView" class="breadcrumb-item active">算法超市</span>
          </div>
        </div>

        <!-- 表格视图 -->
        <div v-if="!showEditView && !showCameraSettingsView && !showConfigView && !showAIEventView && !showModelMarketView" class="table-view">
          <!-- 操作按钮 -->
          <div class="toolbar">
            <div class="toolbar-left">
              <ActionButtonGroup 
                :selected-count="selectedRows.length"
                @add="handleAdd"
                @edit="handleEdit"
                @delete="handleDelete"
              />
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

          <!-- 表格 -->
          <div class="table-content">
            <el-table 
              :data="paginatedTableData" 
              stripe
              v-loading="loading"
              @selection-change="handleSelectionChange"
              @row-click="handleRowClick"
            >
              <el-table-column type="selection" width="55" />
              <el-table-column label="序号" width="80" align="center">
                <template #default="scope">
                  {{ scope.row.id }}
                </template>
              </el-table-column>
              <el-table-column prop="deviceName" label="设备名称" min-width="120" align="left" header-align="left" />
              <el-table-column prop="deviceId" label="设备ID" min-width="120" align="left" header-align="left" />
              <el-table-column prop="tags" label="标签名称" min-width="150" align="left" header-align="left">
                <template #default="scope">
                  <template v-if="scope.row.tags && scope.row.tags.length > 0">
                    <el-tag
                      v-for="tag in scope.row.tags"
                      :key="tag"
                      size="small"
                      type="primary"
                      style="margin-right: 8px; margin-bottom: 4px;"
                    >
                      {{ tag }}
                    </el-tag>
                  </template>
                  <el-tag v-else size="small" type="info">未分类</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="streamUrl" label="视频流路径" min-width="300" show-overflow-tooltip align="left" header-align="left" />
              <el-table-column prop="createTime" label="创建时间" min-width="160" :formatter="formatDateTime" align="left" header-align="left" />
              <el-table-column label="操作" width="420" fixed="right" align="right">
                <template #default="scope">
                  <div class="action-buttons">
                    <PlayButton @click="handlePlay(scope.row)" />
                    <el-button 
                      size="small" 
                      class="config-button"
                      @click="handleConfig(scope.row)"
                    >
                      配置录像
                    </el-button>
<!--                    <el-button -->
<!--                      size="small" -->
<!--                      class="config-button"-->
<!--                      @click="handleAIEvent(scope.row)"-->
<!--                    >-->
<!--                      配置AI事件-->
<!--                    </el-button>-->
                    <el-dropdown @command="handleMoreActions" trigger="click">
                      <el-button size="small">
                        更多 <el-icon><ArrowDown /></el-icon>
                      </el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item :command="{action: 'edit', row: scope.row}">编辑</el-dropdown-item>
                          <el-dropdown-item :command="{action: 'camera-settings', row: scope.row}">设置摄像机</el-dropdown-item>
                          <el-dropdown-item :command="{action: 'delete', row: scope.row}" class="delete-dropdown-item" divided>删除</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </template>
              </el-table-column>
            </el-table>
            
            <!-- 分页 -->
            <div class="table-pagination">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="filteredTotal"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
              />
            </div>
          </div>
        </div>

        <!-- 编辑视图 -->
        <div v-if="showEditView" class="edit-view">
          <DeviceEditForm
            v-model="editForm"
            :mode="editMode"
            @save="handleEditSave"
            @cancel="showTableView"
          />
        </div>

        <!-- 摄像机设置视图 -->
        <div v-if="showCameraSettingsView" class="camera-settings-view">
          <CameraSettings 
            :device-info="selectedRow"
            @back="showTableView" 
          />
        </div>

        <!-- 配置参数视图 -->
        <div v-if="showConfigView" class="config-view">
          <DeviceTimeStrategy
            :device-info="selectedRow"
            @save="handleTimeStrategySave"
            @cancel="showTableView"
          />
        </div>

        <!-- AI事件配置视图 -->
        <div v-if="showAIEventView && !showModelMarketView" class="ai-event-view">
          <DeviceAIEvent
            :device-info="selectedRow"
            @save="handleAIEventSave"
            @cancel="showTableView"
            @open-model-market="openModelMarket"
          />
        </div>

        <!-- 模型超市视图 -->
        <div v-if="showModelMarketView" class="model-market-view">
          <DeviceModelMarket
            :device-info="selectedRow"
            @save="handleModelMarketSave"
            @cancel="backToAIEvent"
          />
        </div>
      </div>
    </div>

    <!-- 视频播放对话框 -->
    <el-dialog
      v-model="videoDialogVisible"
      title="视频播放"
      width="1720px"
      top="5vh"
      @close="handleVideoClose"
    >
      <div class="video-player-container">
        <!-- 播放控制栏 -->
        <div class="player-controls">
          <div class="control-left">
            <div class="play-mode-selector">
              <el-select
                v-model="selectedPlayMode"
                placeholder="选择播放模式"
                size="small"
                style="width: 120px"
              >
                <el-option
                  v-for="option in playModeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </div>
            <div class="play-status">
              <el-tag
                v-if="currentVideoDevice.playMode"
                :type="getPlayModeTagType(currentVideoDevice.playMode)"
                size="small"
              >
                {{ getPlayModeText(currentVideoDevice.playMode) }}
              </el-tag>
              <el-tag
                :type="getPlayStatusType()"
                size="small"
              >
                {{ getPlayStatusText() }}
              </el-tag>
              <el-tag
                v-if="playStatus.retryCount > 0"
                type="warning"
                size="small"
              >
                重试 {{ playStatus.retryCount }}/{{ playStatus.maxRetries }}
              </el-tag>
            </div>
          </div>
          <div class="control-right">
            <!-- 录像状态显示 -->
            <div v-if="recordingStatus.isRecording" class="recording-status">
              <el-tag type="danger" size="small" effect="dark">
                <el-icon class="recording-icon"><VideoCamera /></el-icon>
                录制中 {{ formatRecordingTime(recordingStatus.duration) }}
              </el-tag>
            </div>
            
            <div class="quality-stats">
              <el-tooltip content="播放质量统计" placement="top">
                <div class="stats-info">
                  <span v-if="playStatus.qualityStats.resolution">
                    {{ playStatus.qualityStats.resolution }}
                  </span>
                  <span v-if="playStatus.qualityStats.frameRate">
                    {{ playStatus.qualityStats.frameRate }}fps
                  </span>
                </div>
              </el-tooltip>
            </div>
            
            <!-- 录像按钮 -->
            <el-button
              size="small"
              :type="recordingStatus.isRecording ? 'danger' : 'warning'"
              @click="toggleRecording"
              :loading="recordingStatus.starting || recordingStatus.stopping"
              :disabled="!currentVideoDevice.id"
            >
              <el-icon><VideoCamera /></el-icon>
              {{ recordingStatus.isRecording ? '停止录像' : '开始录像' }}
            </el-button>
            
            <el-button
              size="small"
              type="primary"
              @click="handleReplay"
              :loading="loading"
            >
              <el-icon><Refresh /></el-icon>
              重新播放
            </el-button>
            <el-button
              v-if="currentVideoDevice.originalRtspUrl"
              size="small"
              @click="copyRtspUrl"
            >
              <el-icon><DocumentCopy /></el-icon>
              复制RTSP
            </el-button>
          </div>
        </div>

        <!-- 视频播放器和PTZ控制器区域 -->
        <div class="video-player-content">
          <!-- 视频播放器区域 -->
          <div class="video-player-section">
            <div class="simple-video-player">
              <!-- 调试信息 -->
<!--              <div v-if="currentVideoDevice.streamUrl" -->
<!--                   class="debug-info">-->
<!--                <div>设备: {{ currentVideoDevice.deviceName }}</div>-->
<!--                <div>URL: {{ currentVideoDevice.streamUrl }}</div>-->
<!--                <div>类型: {{ getStreamType(currentVideoDevice.streamUrl) }}</div>-->
<!--                <div v-if="currentVideoDevice.playMode">播放模式: {{ getPlayModeText(currentVideoDevice.playMode) }}</div>-->
<!--              </div>-->
              
              <!-- WebRTC播放器 -->
              <div
                v-if="isCameraRtcDevice(currentVideoDevice)"
                ref="cameraRtcContainer"
                class="camera-rtc-player"
              ></div>

              <video
                v-else-if="currentVideoDevice.playMode === 'webrtc'"
                id="webrtcDirectPlayer"
                ref="webrtcVideoPlayer"
                class="video-element"
                controls
                autoplay
                muted
                playsinline
              ></video>
              
              <!-- HLS视频 -->
              <video
                v-else-if="getStreamType(currentVideoDevice.streamUrl) === 'hls' || currentVideoDevice.playMode === 'hls'"
                ref="simpleHlsPlayer"
                class="video-element"
                controls
                autoplay
                muted
              ></video>
              
              <!-- MP4等视频文件 -->
              <video
                v-else-if="getStreamType(currentVideoDevice.streamUrl) === 'video' || getStreamType(currentVideoDevice.streamUrl) === 'http'"
                class="video-element"
                controls
                autoplay
                muted
              >
                <source :src="currentVideoDevice.streamUrl" type="video/mp4">
                您的浏览器不支持视频播放
              </video>

              <!-- RTSP流处理 -->
              <div v-else-if="getStreamType(currentVideoDevice.originalRtspUrl || currentVideoDevice.streamUrl) === 'rtsp'" 
                   class="rtsp-container">
                
                <!-- WebRTC播放器 - 使用VLStream-server的webrtcUrl -->
                <div v-if="currentVideoDevice.webrtcUrl" class="webrtc-player">
                  <iframe 
                    :src="currentVideoDevice.webrtcUrl"
                    width="800"
                    height="450"
                    frameborder="0"
                    allow="camera; microphone"
                    @load="handleWebRTCLoad"
                    @error="handleWebRTCError"
                  ></iframe>
                  <div class="webrtc-info">
                    <small>WebRTC播放 - {{ currentVideoDevice.deviceName }}</small>
                  </div>
                </div>
                
                <!-- 备用：直接WebRTC连接 -->
                <RtspPlayer
                  v-else-if="webrtcConfig.available && !currentVideoDevice.webrtcUrl"
                  :rtsp-url="currentVideoDevice.originalRtspUrl || currentVideoDevice.streamUrl"
                  :webrtc-server="webrtcConfig.serverUrl"
                  :width="800"
                  :height="450"
                  @connected="handleRtspConnected"
                  @disconnected="handleRtspDisconnected"
                  @error="handleRtspError"
                />
                
                <!-- WebRTC服务不可用时的后备方案 -->
                <div v-else class="rtsp-fallback-options">
                  <div class="fallback-info">
                    <div class="rtsp-icon">
                      <el-icon size="48" color="#f56c6c"><VideoCamera /></el-icon>
                    </div>
                    <div class="rtsp-title">WebRTC服务不可用</div>
                    <div class="rtsp-url">{{ currentVideoDevice.originalRtspUrl || currentVideoDevice.streamUrl }}</div>
                    
                    <div class="fallback-note">
                      <p>WebRTC-streamer服务未启动或不可用。</p>
                      <p>您可以尝试以下选项：</p>
                    </div>
                    
                    <div class="fallback-actions">
                      <el-button type="primary" @click="checkWebRTCService" :loading="checkingWebRTC">
                        <el-icon><Refresh /></el-icon>
                        重新检查WebRTC服务
                      </el-button>
                      <el-button type="warning" @click="convertToHLS" :loading="converting">
                        <el-icon><VideoCamera /></el-icon>
                        转换为HLS播放（备用方案）
                      </el-button>
                      <el-button @click="copyRtspUrl">
                        <el-icon><DocumentCopy /></el-icon>
                        复制RTSP地址
                      </el-button>
                    </div>
                    
                    <div class="fallback-tips">
                      <p><strong>提示：</strong></p>
                      <ul>
                        <li>WebRTC播放延时更低（< 1秒）</li>
                        <li>请启动WebRTC-streamer服务以获得最佳体验</li>
                        <li>备用HLS方案延时较高（2-6秒）</li>
                      </ul>
                    </div>
                  </div>
                </div>

                <!-- HLS后备播放器 -->
                <video
                  v-if="currentVideoDevice.hlsUrl && !webrtcConfig.available"
                  ref="hlsPlayer"
                  class="video-element"
                  controls
                  autoplay
                  muted
                ></video>
              </div>
              
              <!-- 错误显示 -->
              <div v-else class="video-error">
                <div class="error-content">
                  <h3>无法播放视频</h3>
                  <p>URL: {{ currentVideoDevice.streamUrl || '未设置' }}</p>
                  <p>类型: {{ getStreamType(currentVideoDevice.streamUrl) }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- PTZ控制区域 -->
          <div class="ptz-control-section">
            <div class="ptz-control-wrapper">
              <PTZControl
                :device-info="currentVideoDevice"
                :show-camera-management="true"
                @ptz-command="handlePTZCommand"
              />
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 添加设备对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <DeviceEditForm
        v-model="deviceForm"
        :mode="'add'"
        @save="handleDeviceFormSave"
        @cancel="handleDialogClose"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, nextTick, onMounted, onUnmounted, ref, shallowRef, watch} from 'vue'
import {useRouter} from 'vue-router'
import {ArrowDown, DocumentCopy, Refresh, VideoCamera} from '@element-plus/icons-vue'
import {ElLoading, ElMessage, ElMessageBox} from 'element-plus'
import Hls from 'hls.js'

// 导入组件
import DeviceTree from '@/components/DeviceTree.vue'
import PTZControl from '@/components/PTZControl.vue'
import CollapseToggle from '@/components/CollapseToggle.vue'
import ActionButtonGroup from '@/components/ActionButtonGroup.vue'
import PlayButton from '@/components/PlayButton.vue'
import AdvancedSearch from '@/components/AdvancedSearch.vue'
import RtspPlayer from '@/components/RtspPlayer.vue'
import CameraSettings from './CameraSettings.vue'
import DeviceEditForm from './DeviceEditForm.vue'
import DeviceTimeStrategy from './DeviceTimeStrategy.vue'
import DeviceAIEvent from './DeviceAIEvent.vue'
import DeviceModelMarket from './DeviceModelMarket.vue'

// 导入API
import {
  batchDeleteDevices,
  createDevice,
  deleteDevice,
  getDeviceById,
  getDeviceList,
  getDeviceTree,
  ptzMove,
  ptzStop,
  ptzZoom,
  updateDevice
} from '@/api/device'
import {getTagTree} from '@/api/tagManagement'
import {saveTimeStrategy as saveTimeStrategyAPI} from '@/api/timeStrategy'
import {startHLSStream, stopHLSStream} from '@/api/stream'
import {startRecording, stopRecording} from '@/api/videoRecord'
import {getWebRTCBackendConfig, WEBRTC_SERVER_BASE_URL} from '@/api/webrtc'

// 导入工具函数和常量
import {formatDateTime, getStreamType, getYouTubeEmbedUrl} from './deviceUtils.js'
import {defaultDeviceTreeData} from './constants.js'
import { CAMERA_RTC_SOCKET_URL, ensureOPlayer } from '@/utils/oplayer'

const router = useRouter()

// 搜索表单数据
const searchForm = ref({
  deviceName: '',
  deviceId: '',
  tagName: '',
  dateRange: []
})

// 设备树数据
const deviceTreeData = ref([...defaultDeviceTreeData])

// 表格数据
const tableData = ref([])
const loading = ref(false)
const total = ref(0)

// 计算属性：根据设备树选择过滤数据
const filteredTableData = computed(() => {
  let filtered = tableData.value
  
  // 应用设备树过滤
  if (selectedTreeNode.value) {
    const node = selectedTreeNode.value
    
    // 如果选中的是设备类型节点（一级节点）
    if (node.type === 'device_type') {
      // 根据设备类型过滤
      const deviceTypeLabel = node.label.split(' (')[0] // 去掉数量显示，如 "球机 (2)" -> "球机"
      filtered = filtered.filter(device => device.deviceType === deviceTypeLabel)
    }
    
    // 如果选中的是具体设备节点（二级节点）
    if (node.type === 'device') {
      // 根据设备ID过滤，只显示选中的设备
      filtered = filtered.filter(device => device.id === node.deviceId)
    }
  }
  
  return filtered
})

// 计算属性：分页数据
const paginatedTableData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredTableData.value.slice(start, end)
})

// 计算属性：过滤后的总数
const filteredTotal = computed(() => {
  return filteredTableData.value.length
})

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)

// 表格选中行
const selectedRows = ref([])
const selectedRow = ref(null)

// 设备树选中节点
const selectedTreeNode = ref(null)
const treeFilterText = ref('')

// 视图状态控制
const showEditView = ref(false)
const showCameraSettingsView = ref(false)
const showConfigView = ref(false)
const showAIEventView = ref(false)
const showModelMarketView = ref(false)

// 设备树折叠状态
const deviceTreeCollapsed = ref(false)

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editMode = ref('add') // 'add' | 'edit'

// 视频播放相关
const videoDialogVisible = ref(false)
const currentVideoDevice = ref({})
const simpleHlsPlayer = ref(null)
const hlsPlayer = ref(null)
const webrtcVideoPlayer = ref(null)
const cameraRtcContainer = ref(null)
const cameraRtcPlayer = shallowRef(null)
const converting = ref(false)

// WebRTC相关
const webrtcConfig = ref({
  serverUrl: WEBRTC_SERVER_BASE_URL,
  available: false,
  enabled: true
})
const WEBRTC_STREAMER_BASE = WEBRTC_SERVER_BASE_URL
const WEBRTC_SCRIPT_URLS = [
  `${WEBRTC_STREAMER_BASE}/libs/adapter.min.js`,
  `${WEBRTC_STREAMER_BASE}/webrtcstreamer.js`
]
let webrtcScriptLoader = null
const checkingWebRTC = ref(false)

// 增强播放功能相关
const playModeOptions = ref([
  { label: 'WebRTC播放', value: 'webrtc' },
])
const selectedPlayMode = ref('webrtc')
const webrtcPlayer = ref(null)
const playStatistics = ref({
  webrtcSuccess: 0,
  hlsSuccess: 0,
  totalAttempts: 0,
  failedAttempts: 0
})

// 播放状态监控
const playStatus = ref({
  isPlaying: false,
  isConnecting: false,
  hasError: false,
  errorMessage: '',
  connectionState: 'disconnected',
  retryCount: 0,
  maxRetries: 3,
  lastErrorTime: null,
  qualityStats: {
    frameRate: 0,
    resolution: '',
    bitrate: 0,
    packetsLost: 0,
    latency: 0
  }
})

// 播放监控定时器
// WebRTC script loader
const loadScriptTag = (src) => {
  return new Promise((resolve, reject) => {
    if (document.querySelector(`script[src="${src}"]`)) {
      resolve()
      return
    }
    
    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error(`Failed to load ${src}`))
    document.head.appendChild(script)
  })
}

const ensureWebRtcStreamerScripts = async () => {
  if (webrtcScriptLoader) {
    return webrtcScriptLoader
  }
  
  webrtcScriptLoader = Promise.all(WEBRTC_SCRIPT_URLS.map(loadScriptTag))
    .catch(error => {
      webrtcScriptLoader = null
      throw error
    })
  
  return webrtcScriptLoader
}

const playMonitorTimer = ref(null)

// 录像状态管理
const recordingStatus = ref({
  isRecording: false,
  recordId: null,
  duration: 0,
  startTime: null,
  starting: false,
  stopping: false
})

// 录像监控定时器
const recordingTimer = ref(null)

// 表单数据
const editForm = ref({})
const deviceForm = ref({})

// 生命周期
onMounted(() => {
  try {
    loadTagNameMap()  // 先加载标签映射表
    loadDeviceList()
    loadDeviceTree()
    // checkWebRTCService()
  } catch (error) {
    console.error('设备管理页面初始化失败:', error)
    ElMessage.error('页面初始化失败，请刷新重试')
  }
})

// 组件卸载时清理
onUnmounted(() => {
  // 清理播放监控
  stopPlayMonitoring()
  
  // 清理录像状态和计时器
  stopRecordingTimer()
  
  // 如果正在录像，强制停止
  if (recordingStatus.value.isRecording && recordingStatus.value.recordId) {
    console.warn('组件卸载时检测到正在录像，自动停止录像')
    // 异步停止录像，不等待结果
    stopRecording(recordingStatus.value.recordId).catch(error => {
      console.error('组件卸载时停止录像失败:', error)
    })
  }
  
  // 清理WebRTC和HLS流
  cleanupWebRTCStream()
  cleanupHLSStream()
  cleanupCameraRTCPlayer()
  
  console.log('设备管理组件已卸载，资源已清理')
})

// 标签ID到名称的映射表
const tagNameMap = ref(new Map())

// 加载标签树并建立映射表
const loadTagNameMap = async () => {
  try {
    const response = await getTagTree()
    if (response.code === 200 && response.data) {
      const tagMap = new Map()
      
      // 递归遍历标签树，建立ID到名称的映射
      const traverseTagTree = (nodes) => {
        if (!Array.isArray(nodes)) return
        
        nodes.forEach(node => {
          if (node.id && node.tagName) {
            tagMap.set(node.id, node.tagName)
          }
          
          // 递归处理子节点
          if (node.children && Array.isArray(node.children)) {
            traverseTagTree(node.children)
          }
        })
      }
      
      traverseTagTree(response.data)
      tagNameMap.value = tagMap
      console.log('标签映射表加载完成:', tagMap.size, '个标签')
    }
  } catch (error) {
    console.warn('加载标签映射表失败:', error)
  }
}

// 方法
const loadDeviceList = async () => {
  loading.value = true
  try {
    // 先加载标签映射表
    if (tagNameMap.value.size === 0) {
      await loadTagNameMap()
    }
    
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      deviceName: searchForm.value.deviceName,
      tagName: searchForm.value.tagName,
      dateRange: searchForm.value.dateRange
    }
    
    const response = await getDeviceList(params)
    const devices = response.data.records || []
    
    // 参考编辑页面的做法，使用getDeviceById获取每个设备的完整信息，包括标签
    tableData.value = await Promise.all(
        devices.map(async (device) => {
          try {
            // 调用getDeviceById获取设备详情，包括selectedTags
            const detailResponse = await getDeviceById(device.id)

            if (detailResponse.code === 200 && detailResponse.data) {
              const deviceDetail = detailResponse.data

              // 处理selectedTags字段，转换为tags数组用于显示
              let tags = []
              if (Array.isArray(deviceDetail.selectedTags) && deviceDetail.selectedTags.length > 0) {
                // 使用映射表获取标签名称
                tags = deviceDetail.selectedTags.map(tagId => {
                  const tagName = tagNameMap.value.get(tagId)
                  return tagName || `标签${tagId}`
                })
              }

              return {
                ...device,
                ...deviceDetail,
                tags: tags,
                // 不使用临时tag字段，只保留selectedTags相关的标签信息
                displayTag: tags.length > 0 ? tags[0] : '未分类'
              }
            } else {
              // 如果获取详情失败，使用列表中的基本信息
              return {
                ...device,
                tags: [],
                displayTag: '未分类'
              }
            }
          } catch (error) {
            console.warn(`获取设备 ${device.id} 详情失败:`, error)
            return {
              ...device,
              tags: [],
              displayTag: '未分类'
            }
          }
        })
    )
    total.value = response.data.total || 0
  } catch (error) {
    console.error('加载设备列表失败:', error)
    ElMessage.error('加载设备列表失败')
  } finally {
    loading.value = false
  }
}

const loadDeviceTree = async () => {
  try {
    const response = await getDeviceTree()
    if (response.data) {
      deviceTreeData.value = response.data
    }
  } catch (error) {
    console.error('加载设备树失败:', error)
  }
}

// 高级搜索处理
const handleAdvancedSearch = (searchData) => {
  console.log('高级搜索数据:', searchData)
  // 将搜索数据合并到现有的searchForm中
  if (searchData.deviceName) {
    searchForm.value.deviceName = searchData.deviceName
  }
  if (searchData.deviceId) {
    searchForm.value.deviceId = searchData.deviceId
  }
  if (searchData.tagName) {
    searchForm.value.tagName = searchData.tagName
  }
  if (searchData.keyword) {
    // 如果有关键词，可以用于模糊搜索设备名称
    searchForm.value.deviceName = searchData.keyword
  }
  if (searchData.dateRange && searchData.dateRange.length === 2) {
    searchForm.value.dateRange = searchData.dateRange
  }
  currentPage.value = 1
  loadDeviceList()
  ElMessage.success('搜索完成')
}

const handleAdvancedSearchReset = () => {
  searchForm.value = {
    deviceName: '',
    deviceId: '',
    tagName: '',
    dateRange: []
  }
  currentPage.value = 1
  loadDeviceList()
  ElMessage.info('搜索条件已重置')
}

// 导出上传相关处理
const handleExport = () => {
  ElMessage.info('开始导出数据')
  // 实际项目中这里会实现导出逻辑
}

const handleUpload = () => {
  ElMessage.info('打开上传文件对话框')
  // 实际项目中这里会实现上传逻辑
}

const handleDownloadTemplate = () => {
  ElMessage.info('下载模板文件')
  // 实际项目中这里会实现下载模板逻辑
}

const handleBatchOperation = () => {
  ElMessage.info('打开批量操作界面')
  // 实际项目中这里会实现批量操作逻辑
}

// 分页处理
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  // 如果有树节点过滤，不重新加载数据，否则重新加载
  if (!selectedTreeNode.value) {
    loadDeviceList()
  }
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  // 如果有树节点过滤，不重新加载数据，否则重新加载
  if (!selectedTreeNode.value) {
    loadDeviceList()
  }
}

// 表格事件处理
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleRowClick = (row) => {
  selectedRow.value = row
}

// 设备树事件处理
const handleNodeClick = (node) => {
  selectedTreeNode.value = node
  
  // 设置面包屑显示文本
  if (node.type === 'device_type') {
    treeFilterText.value = `设备类型: ${node.label}`
  } else if (node.type === 'device') {
    treeFilterText.value = `设备: ${node.label}`
  } else {
    treeFilterText.value = node.label
  }
  
  // 重置到第一页
  currentPage.value = 1
  
  console.log('设备树节点点击:', node)
  console.log('当前过滤条件:', treeFilterText.value)
  console.log('过滤后数据数量:', filteredTableData.value.length)
}

const toggleDeviceTree = () => {
  deviceTreeCollapsed.value = !deviceTreeCollapsed.value
}

const handleDeviceTreeSearch = (keyword) => {
  // 处理设备树搜索
  console.log('设备树搜索:', keyword)
}

// 清除设备树过滤条件
const clearTreeFilter = () => {
  selectedTreeNode.value = null
  treeFilterText.value = ''
  currentPage.value = 1
  console.log('已清除设备树过滤条件')
}

// 视图切换
const showTableView = () => {
  showEditView.value = false
  showCameraSettingsView.value = false
  showConfigView.value = false
  showAIEventView.value = false
  showModelMarketView.value = false
  selectedRow.value = null
  
  // 清除设备树过滤条件
  selectedTreeNode.value = null
  treeFilterText.value = ''
}

// 设备操作
const handleAdd = () => {
  dialogTitle.value = '添加设备'
  editMode.value = 'add'
  deviceForm.value = {}
  dialogVisible.value = true
}

const handleEdit = async () => {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning('请选择一个设备进行编辑')
    return
  }
  
  try {
    // 加载状态
    const loadingMessage = ElMessage({
      message: '正在加载设备详情...',
      type: 'info',
      duration: 0
    })
    
    // 调用API获取完整的设备信息，包括关联的标签
    const response = await getDeviceById(selectedRows.value[0].id)
    loadingMessage.close()
    
    if (response.code === 200) {
      editMode.value = 'edit'
      editForm.value = { ...response.data }
      selectedRow.value = selectedRows.value[0]
      showEditView.value = true
      
      console.log('获取设备详情成功:', response.data)
      console.log('关联的标签ID:', response.data.selectedTags)
    } else {
      ElMessage.error('获取设备详情失败: ' + response.message)
    }
  } catch (error) {
    ElMessage.error('获取设备详情失败，请重试')
    console.error('获取设备详情失败:', error)
  }
}

const handleDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的设备')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${selectedRows.value.length} 个设备吗？`,
      '删除确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const deviceIds = selectedRows.value.map(row => row.id)
    await batchDeleteDevices(deviceIds)
    
    ElMessage.success('删除成功')
    await loadDeviceList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除设备失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 增强的视频播放功能
const handlePlay = async (row) => {
  console.log('点击播放设备:', row)
  
  // 统计播放尝试次数
  playStatistics.value.totalAttempts++
  
  // 创建设备副本用于播放
  const deviceForPlay = { ...row }
  
  // 如果没有视频流URL，使用测试URL
  if (!deviceForPlay.streamUrl || deviceForPlay.streamUrl.trim() === '') {
    console.log('设备没有配置视频流URL，使用测试视频')
    deviceForPlay.streamUrl = 'https://www.w3schools.com/html/mov_bbb.mp4'
    ElMessage.info('设备未配置视频流，使用测试视频进行演示')
  }
  
  console.log('播放视频URL:', deviceForPlay.streamUrl)
  
  // 检测流类型
  const streamType = getStreamType(deviceForPlay.streamUrl)
  
  // 显示加载中提示
  const loading = ElLoading.service({
    lock: true,
    text: '正在启动视频播放...',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)'
  })
  
  try {
    // pick play strategy
    const playStrategy = await determinePlayStrategy(deviceForPlay, streamType)
    console.log('Play strategy:', playStrategy)
    
    currentVideoDevice.value = deviceForPlay
    videoDialogVisible.value = true
    
    await executePlayStrategy(currentVideoDevice.value, playStrategy)
    
    // start monitoring after player setup
    startPlayMonitoring()
    
    
  } catch (error) {
    console.error('播放失败:', error)
    ElMessage.error(`播放失败: ${error.message}`)
    playStatistics.value.failedAttempts++
  } finally {
    loading.close()
  }
}

// 确定播放策略
const determinePlayStrategy = async (device, streamType) => {
  if (streamType === 'cameraRTC') {
    return 'cameraRTC'
  }
  // 如果用户手动选择了播放模式
  if (selectedPlayMode.value !== 'auto') {
    return selectedPlayMode.value
  }
  
  // 智能播放策略
  const strategy = await getOptimalPlayStrategy(device, streamType)
  console.log('智能播放策略分析结果:', strategy)
  
  return strategy.mode
}

// 获取最优播放策略
const getOptimalPlayStrategy = async (device, streamType) => {
  const strategies = []
  
  // 策略1：基于设备类型推荐
  const deviceTypeStrategy = getDeviceTypeStrategy(device)
  strategies.push(deviceTypeStrategy)
  
  // 策略2：基于流类型推荐
  const streamTypeStrategy = getStreamTypeStrategy(streamType)
  strategies.push(streamTypeStrategy)
  
  // 策略3：基于网络状况推荐
  const networkStrategy = await getNetworkStrategy()
  strategies.push(networkStrategy)
  
  // 策略4：基于历史成功率推荐
  const historyStrategy = getHistoryStrategy()
  strategies.push(historyStrategy)
  
  // 策略5：基于系统可用性推荐
  const availabilityStrategy = await getAvailabilityStrategy()
  strategies.push(availabilityStrategy)
  
  console.log('所有策略分析结果:', strategies)
  
  // 计算综合评分
  const finalStrategy = calculateFinalStrategy(strategies)
  console.log('最终播放策略:', finalStrategy)
  
  return finalStrategy
}

// 基于设备类型的播放策略
const getDeviceTypeStrategy = (device) => {
  const deviceType = device.tag || device.deviceType || 'unknown'
  
  switch (deviceType) {
    case '球机':
    case '云台':
      // 球机和云台通常需要低延时控制
      return {
        mode: 'webrtc',
        score: 0.9,
        reason: '球机/云台设备推荐WebRTC以获得低延时控制'
      }
    case '摄像头':
    case '枪机':
      // 普通摄像头可以使用任何模式
      return {
        mode: 'auto',
        score: 0.5,
        reason: '摄像头设备可使用任何播放模式'
      }
    case '半球':
      // 半球摄像头通常用于监控，可以接受一定延时
      return {
        mode: 'hls',
        score: 0.7,
        reason: '半球摄像头可使用HLS播放'
      }
    default:
      return {
        mode: 'auto',
        score: 0.3,
        reason: '未知设备类型，使用自动选择'
      }
  }
}

// 基于流类型的播放策略
const getStreamTypeStrategy = (streamType) => {
  switch (streamType) {
    case 'cameraRTC':
      return {
        mode: 'cameraRTC',
        score: 0.95,
        reason: 'CameraRTC stream uses OPlayer'
      }
    case 'rtsp':
      return {
        mode: 'webrtc',
        score: 0.8,
        reason: 'RTSP流推荐使用WebRTC以获得更好的性能'
      }
    case 'hls':
      return {
        mode: 'hls',
        score: 0.9,
        reason: 'HLS流直接使用HLS播放'
      }
    case 'video':
    case 'http':
      return {
        mode: 'native',
        score: 0.8,
        reason: '视频文件使用原生播放器'
      }
    default:
      return {
        mode: 'auto',
        score: 0.3,
        reason: '未知流类型，使用自动选择'
      }
  }
}

// 基于网络状况的播放策略
const getNetworkStrategy = async () => {
  try {
    // 检测网络连接类型
    const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection
    
    if (connection) {
      const effectiveType = connection.effectiveType
      const downlink = connection.downlink
      
      console.log('网络状况:', { effectiveType, downlink })
      
      // 根据网络状况推荐播放策略
      if (effectiveType === '4g' || effectiveType === '3g') {
        if (downlink >= 10) {
          return {
            mode: 'webrtc',
            score: 0.8,
            reason: '高速移动网络，推荐WebRTC'
          }
        } else {
          return {
            mode: 'hls',
            score: 0.7,
            reason: '移动网络带宽有限，推荐HLS'
          }
        }
      } else if (effectiveType === 'slow-2g' || effectiveType === '2g') {
        return {
          mode: 'hls',
          score: 0.6,
          reason: '慢速网络，推荐HLS'
        }
      } else {
        // WiFi或其他高速网络
        return {
          mode: 'webrtc',
          score: 0.9,
          reason: '高速网络，推荐WebRTC'
        }
      }
    }
    
    // 如果无法检测网络状况，使用默认策略
    return {
      mode: 'webrtc',
      score: 0.5,
      reason: '无法检测网络状况，默认使用WebRTC'
    }
  } catch (error) {
    console.error('检测网络状况失败:', error)
    return {
      mode: 'auto',
      score: 0.3,
      reason: '网络状况检测失败'
    }
  }
}

// 基于历史成功率的播放策略
const getHistoryStrategy = () => {
  const stats = playStatistics.value
  const total = stats.totalAttempts || 1
  
  const webrtcRate = stats.webrtcSuccess / total
  const hlsRate = stats.hlsSuccess / total
  
  console.log('历史成功率:', { webrtcRate, hlsRate, stats })
  
  if (webrtcRate > hlsRate && webrtcRate > 0.7) {
    return {
      mode: 'webrtc',
      score: 0.8,
      reason: `WebRTC历史成功率高 (${(webrtcRate * 100).toFixed(1)}%)`
    }
  } else if (hlsRate > webrtcRate && hlsRate > 0.7) {
    return {
      mode: 'hls',
      score: 0.8,
      reason: `HLS历史成功率高 (${(hlsRate * 100).toFixed(1)}%)`
    }
  } else {
    return {
      mode: 'auto',
      score: 0.4,
      reason: '历史成功率数据不足'
    }
  }
}

// 基于系统可用性的播放策略
const getAvailabilityStrategy = async () => {
  try {
    // 检查WebRTC服务可用性
    const webrtcAvailable = webrtcConfig.value.available && webrtcConfig.value.enabled
    
    if (webrtcAvailable) {
      return {
        mode: 'webrtc',
        score: 0.9,
        reason: 'WebRTC服务可用'
      }
    } else {
      return {
        mode: 'hls',
        score: 0.7,
        reason: 'WebRTC服务不可用，使用HLS'
      }
    }
  } catch (error) {
    console.error('检查服务可用性失败:', error)
    return {
      mode: 'hls',
      score: 0.5,
      reason: '服务可用性检查失败，使用HLS'
    }
  }
}

// 计算最终播放策略
const calculateFinalStrategy = (strategies) => {
  // 权重配置
  const weights = {
    deviceType: 0.2,
    streamType: 0.3,
    network: 0.2,
    history: 0.2,
    availability: 0.1
  }
  
  // 计算各模式的加权得分
  const modeScores = {}
  
  strategies.forEach((strategy, index) => {
    const weight = Object.values(weights)[index] || 0.1
    const mode = strategy.mode
    const score = strategy.score * weight
    
    if (!modeScores[mode]) {
      modeScores[mode] = { score: 0, reasons: [] }
    }
    
    modeScores[mode].score += score
    modeScores[mode].reasons.push(strategy.reason)
  })
  
  // 找出得分最高的模式
  let bestMode = 'auto'
  let bestScore = 0
  
  for (const [mode, data] of Object.entries(modeScores)) {
    if (data.score > bestScore) {
      bestScore = data.score
      bestMode = mode
    }
  }
  
  // 如果是auto模式，根据其他因素决定
  if (bestMode === 'auto') {
    // 如果WebRTC可用，优先使用WebRTC
    if (webrtcConfig.value.available) {
      bestMode = 'webrtc'
    } else {
      bestMode = 'hls'
    }
  }
  
  return {
    mode: bestMode,
    score: bestScore,
    reasons: modeScores[bestMode]?.reasons || [],
    allScores: modeScores
  }
}

// 执行播放策略
const executePlayStrategy = async (device, strategy) => {
  console.log(`执行播放策略: ${strategy}`)
  
  switch (strategy) {
    case 'webrtc':
      await executeWebRTCPlayStrategy(device)
      break
    case 'hls':
      await executeHLSPlayStrategy(device)
      break
    case 'native':
      await executeNativePlayStrategy(device)
      break
    case 'cameraRTC':
      await executeCameraRTCPlayStrategy(device)
      break
    default:
      throw new Error(`不支持的播放策略: ${strategy}`)
  }
}

// 执行WebRTC播放策略
const executeWebRTCPlayStrategy = async (device) => {
  try {
    console.log('Starting WebRTC direct playback...')
    
    await cleanupWebRTCStream()
    cleanupCameraRTCPlayer()
    
    device.playMode = 'webrtc'
    device.connectionState = 'connecting'
    playStatus.value.isConnecting = true
    playStatus.value.isPlaying = false
    playStatus.value.hasError = false
    playStatus.value.errorMessage = ''
    playStatus.value.connectionState = 'connecting'
    
    await ensureWebRtcStreamerScripts()
    await nextTick()
    
    if (!window.WebRtcStreamer) {
      throw new Error('WebRtcStreamer library not available')
    }
    
    const videoEl = webrtcVideoPlayer.value
    if (!videoEl) {
      throw new Error('WebRTC video element not ready')
    }
    
    if (!videoEl.id) {
      videoEl.id = `webrtc-player-${Date.now()}`
    }
    
    videoEl.srcObject = null
    videoEl.onerror = () => handlePlayError(new Error('WebRTC direct playback error'))
    videoEl.onloadeddata = () => {
      playStatistics.value.webrtcSuccess++
      playStatus.value.isPlaying = true
      playStatus.value.isConnecting = false
      playStatus.value.connectionState = 'connected'
    }
    
    webrtcPlayer.value = new window.WebRtcStreamer(videoEl.id, WEBRTC_STREAMER_BASE)
    if (typeof webrtcPlayer.value.connect === 'function') {
      webrtcPlayer.value.connect(device.streamUrl, '', 'rtptransport=tcp&timeout=60')
    } else {
      throw new Error('WebRtcStreamer.connect is not available')
    }
    
  } catch (error) {
    console.error('WebRTC direct play failed:', error)
    
    if (selectedPlayMode.value === 'auto') {
      console.log('WebRTC failed, falling back to HLS')
      await executeHLSPlayStrategy(device)
    } else {
      throw error
    }
  }
}


const executeCameraRTCPlayStrategy = async (device) => {
  try {
    await cleanupWebRTCStream()
    await cleanupHLSStream()
    cleanupCameraRTCPlayer()

    device.playMode = 'cameraRTC'
    device.connectionState = 'connecting'
    playStatus.value.isConnecting = true
    playStatus.value.isPlaying = false
    playStatus.value.hasError = false
    playStatus.value.errorMessage = ''
    playStatus.value.connectionState = 'connecting'

    await ensureOPlayer()
    await nextTick()

    const container = cameraRtcContainer.value
    if (!container) {
      throw new Error('CameraRTC container not ready')
    }

    const deviceId = device?.deviceId || device?.id
    if (!deviceId) {
      throw new Error('CameraRTC requires deviceId')
    }

    cameraRtcPlayer.value = new window.OToolBox.OPlayer(container, {
      debuggerMode: false,
      autoSize: true,
      backgroundColor: '#000000',
      showHeader: true,
      webRTCSocketURL: CAMERA_RTC_SOCKET_URL
    })
    cameraRtcPlayer.value.play({
      type: 'cameraRTC',
      src: String(deviceId),
      name: device?.deviceName || ''
    })

    playStatus.value.isPlaying = true
    playStatus.value.isConnecting = false
    playStatus.value.connectionState = 'connected'
  } catch (error) {
    console.error('CameraRTC play failed:', error)
    throw error
  }
}

const executeHLSPlayStrategy = async (device) => {
  try {
    cleanupCameraRTCPlayer()
    console.log('开始HLS播放流程...')
    
    const streamType = getStreamType(device.streamUrl)
    
    if (streamType === 'rtsp') {
      // RTSP流需要先转换为HLS
      const hlsUrl = await startHLSConversion(device)
      if (hlsUrl) {
        device.streamUrl = hlsUrl
        device.originalRtspUrl = device.streamUrl
        device.hlsUrl = hlsUrl
        device.playMode = 'hls'
        playStatistics.value.hlsSuccess++
      } else {
        throw new Error('RTSP转HLS失败')
      }
    } else if (streamType === 'hls') {
      // 已经是HLS流，直接播放
      device.playMode = 'hls'
      playStatistics.value.hlsSuccess++
    } else {
      throw new Error(`不支持的流类型: ${streamType}`)
    }
    
    console.log('HLS播放流程完成')
    
  } catch (error) {
    console.error('HLS播放策略执行失败:', error)
    throw error
  }
}

// 执行原生播放策略
const executeNativePlayStrategy = async (device) => {
  try {
    cleanupCameraRTCPlayer()
    console.log('开始原生播放流程...')
    
    // 对于原生支持的格式，直接播放
    device.playMode = 'native'
    
    console.log('原生播放流程完成')
    
  } catch (error) {
    console.error('原生播放策略执行失败:', error)
    throw error
  }
}

// 根据设备类型获取测试视频URL
const getTestVideoUrl = (deviceTag) => {
  // 导入测试视频URL
  const { testVideoUrls } = require('./constants.js')
  
  switch (deviceTag) {
    case '摄像头':
    case '枪机':
      return testVideoUrls.mp4
    case '球机':
    case '云台':
      return testVideoUrls.hls
    case '半球':
      return testVideoUrls.mp4Small
    default:
      return testVideoUrls.mp4
  }
}

// 获取流类型文本
const getStreamTypeText = (streamUrl) => {
  const type = getStreamType(streamUrl)
  const typeMap = {
    'youtube': 'YouTube视频',
    'rtsp': 'RTSP流',
    'hls': 'HLS流',
    'http': 'HTTP流',
    'video': '视频文件',
    'cameraRTC': 'CameraRTC',
    'unknown': '未知格式'
  }
  return typeMap[type] || '未知格式'
}


const isCameraRtcDevice = (device) => {
  if (!device) return false
  if (device.playMode === 'cameraRTC') return true
  return getStreamType(device.streamUrl) === 'cameraRTC'
}


const handleVideoClose = () => {
  // 清理HLS播放器
  if (simpleHlsPlayer.value && simpleHlsPlayer.value.hlsInstance) {
    simpleHlsPlayer.value.hlsInstance.destroy()
    simpleHlsPlayer.value.hlsInstance = null
  }
  
  // 清理WebRTC播放器
  if (webrtcVideoPlayer.value) {
    webrtcVideoPlayer.value.srcObject = null
  }
  
  // 清理流
  cleanupHLSStream()
  cleanupWebRTCStream()
  cleanupCameraRTCPlayer()
  
  // 停止播放监控
  stopPlayMonitoring()
  
  // 清理录像状态（如果正在录像，提醒用户）
  if (recordingStatus.value.isRecording) {
    ElMessage.warning('检测到正在录像，请先停止录像')
    return // 不关闭对话框
  }
  
  // 清理录像计时器
  stopRecordingTimer()
  
  // 重置录像状态
  recordingStatus.value.isRecording = false
  recordingStatus.value.recordId = null
  recordingStatus.value.duration = 0
  recordingStatus.value.startTime = null
  recordingStatus.value.starting = false
  recordingStatus.value.stopping = false
  
  videoDialogVisible.value = false
  currentVideoDevice.value = {}
}

// 复制RTSP地址
const copyRtspUrl = async () => {
  try {
    const rtspUrl = currentVideoDevice.value.originalRtspUrl || currentVideoDevice.value.streamUrl
    await navigator.clipboard.writeText(rtspUrl)
    ElMessage.success('RTSP地址已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败，请手动复制')
  }
}

// WebRTC相关方法
const checkWebRTCService = async () => {
  try {
    checkingWebRTC.value = true
    
    try {
      const response = await getWebRTCBackendConfig()
      
      if (response && response.code === 200) {
        webrtcConfig.value = response.data
        console.log('WebRTC配置获取成功:', response.data)
        
        if (response.data.available) {
          console.log('WebRTC服务可用')
        } else {
          console.log('WebRTC服务不可用')
        }
      } else {
        console.error('获取WebRTC配置失败:', response?.message || '未知错误')
        webrtcConfig.value.available = false
      }
    } catch (apiError) {
      console.warn('WebRTC配置API调用失败，使用默认配置:', apiError)
      
      // API失败时使用默认配置
      webrtcConfig.value = {
        serverUrl: WEBRTC_SERVER_BASE_URL,
        available: true,  // 假设可用，让播放器尝试
        enabled: true
      }
      console.log('WebRTC使用默认配置 (Fallback模式)')
    }
  } catch (error) {
    console.error('检查WebRTC服务失败:', error)
    webrtcConfig.value.available = false
  } finally {
    checkingWebRTC.value = false
  }
}

const handleRtspConnected = () => {
  console.log('RTSP WebRTC连接成功')
  ElMessage.success('视频流连接成功')
}

const handleRtspDisconnected = () => {
  console.log('RTSP WebRTC连接断开')
  ElMessage.info('视频流连接断开')
}

const handleRtspError = async (error) => {
  console.error('RTSP WebRTC连接错误:', error)
  
  // 如果WebRTC失败，自动尝试HLS转换
  if (currentVideoDevice.value.originalRtspUrl) {
    ElMessage.warning('WebRTC播放失败，正在尝试HLS播放模式...')
    
    try {
      // 调用HLS转换API
      const hlsUrl = await convertRtspToHls(currentVideoDevice.value.originalRtspUrl)
      if (hlsUrl) {
        currentVideoDevice.value.streamUrl = hlsUrl
        currentVideoDevice.value.hlsUrl = hlsUrl
        currentVideoDevice.value.playMode = 'hls'
        
        ElMessage.success('已切换到HLS播放模式')
        
        // 初始化HLS播放器
        setTimeout(() => {
          initHLSPlayer(hlsUrl)
        }, 1000)
        
        return
      }
    } catch (hlsError) {
      console.error('HLS转换也失败:', hlsError)
    }
  }
  
  ElMessage.error('视频播放失败: ' + error)
  
  // 显示故障排除建议
  showTroubleshootingTips()
}

const showTroubleshootingTips = () => {
  ElMessageBox.alert(`
    <div style="text-align: left;">
      <h4>播放失败故障排除：</h4>
      <ol>
        <li><strong>检查网络连接</strong>：确认能ping通摄像头IP</li>
        <li><strong>验证RTSP地址</strong>：用VLC播放器测试RTSP URL</li>
        <li><strong>检查认证信息</strong>：确认用户名密码正确</li>
        <li><strong>重启WebRTC服务</strong>：运行修复脚本重启webrtc-streamer</li>
        <li><strong>防火墙设置</strong>：确认554端口（RTSP）未被阻止</li>
      </ol>
      <p><strong>RTSP地址</strong>: ${currentVideoDevice.value.originalRtspUrl || currentVideoDevice.value.streamUrl}</p>
    </div>
  `, '播放故障排除', {
    dangerouslyUseHTMLString: true,
    confirmButtonText: '知道了'
  })
}

// WebRTC iframe事件处理
const handleWebRTCLoad = () => {
  console.log('WebRTC iframe加载完成')
  ElMessage.success('WebRTC播放器加载成功')
  currentVideoDevice.value.connectionState = 'connected'
}

const handleWebRTCError = (error) => {
  console.error('WebRTC iframe加载失败:', error)
  ElMessage.error('WebRTC播放器加载失败')
  currentVideoDevice.value.connectionState = 'failed'
  
  // 自动尝试HLS备用方案
  handleRtspError('WebRTC iframe加载失败')
}

// 启动HLS流转换
const startHLSConversion = async (device) => {
  try {
    const response = await startHLSStream({
      deviceId: device.deviceId,
      rtspUrl: device.streamUrl,
      quality: 'medium'
    })
    
    if (response.code === 200) {
      // 更新设备的流URL为HLS地址
      device.hlsUrl = response.data
      console.log('HLS转换启动成功:', response.data)
      return response.data
    } else {
      throw new Error(response.message || 'HLS转换启动失败')
    }
  } catch (error) {
    console.error('启动HLS转换失败:', error)
    ElMessage.error('启动HLS转换失败: ' + error.message)
    return null
  }
}

// 手动转换为HLS
const convertToHLS = async () => {
  converting.value = true
  
  try {
    const hlsUrl = await startHLSConversion(currentVideoDevice.value)
    if (hlsUrl) {
      currentVideoDevice.value.hlsUrl = hlsUrl
      // 等待DOM更新后初始化HLS播放器
      await nextTick()
      initHLSPlayer(hlsUrl)
      ElMessage.success('视频流转换成功，开始播放')
    }
  } catch (error) {
    ElMessage.error('转换失败，请重试')
  } finally {
    converting.value = false
  }
}

// 初始化HLS播放器
const initHLSPlayer = (hlsUrl) => {
  const video = hlsPlayer.value
  if (!video) {
    console.error('HLS播放器元素未找到')
    return
  }
  
  console.log('初始化HLS播放器:', hlsUrl)
  
  // 清理之前的实例
  if (video.hlsInstance) {
    video.hlsInstance.destroy()
    video.hlsInstance = null
  }
  
  if (Hls.isSupported()) {
    const hls = new Hls({
      debug: false,
      enableWorker: true,
      lowLatencyMode: true
    })
    
    video.hlsInstance = hls
    
    hls.loadSource(hlsUrl)
    hls.attachMedia(video)
    
    hls.on(Hls.Events.MANIFEST_PARSED, () => {
      console.log('HLS manifest 解析成功')
      video.play().catch(error => {
        console.error('HLS 自动播放失败:', error)
        ElMessage.warning('视频自动播放失败，请手动点击播放')
      })
    })
    
    hls.on(Hls.Events.ERROR, (event, data) => {
      console.error('HLS 播放错误:', data)
      if (data.fatal) {
        ElMessage.error('HLS播放失败: ' + data.details)
      }
    })
  } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
    console.log('使用原生HLS支持')
    video.src = hlsUrl
    video.play().catch(error => {
      console.error('原生HLS播放失败:', error)
      ElMessage.warning('视频自动播放失败，请手动点击播放')
    })
  } else {
    console.error('浏览器不支持HLS播放')
    ElMessage.error('浏览器不支持HLS播放')
  }
}

// 清理HLS流
const cleanupHLSStream = async () => {
  if (currentVideoDevice.value.hlsUrl && currentVideoDevice.value.deviceId) {
    try {
      await stopHLSStream({ deviceId: currentVideoDevice.value.deviceId })
      console.log('HLS流已清理:', currentVideoDevice.value.deviceId)
    } catch (error) {
      console.error('停止HLS流失败:', error)
    }
  }
  
  // 清理播放器实例
  if (hlsPlayer.value && hlsPlayer.value.hlsInstance) {
    hlsPlayer.value.hlsInstance.destroy()
    hlsPlayer.value.hlsInstance = null
  }
}

const cleanupCameraRTCPlayer = () => {
  if (cameraRtcPlayer.value?.compInstance?.$destroy) {
    cameraRtcPlayer.value.compInstance.$destroy()
  }
  cameraRtcPlayer.value = null

  if (cameraRtcContainer.value) {
    cameraRtcContainer.value.innerHTML = ''
  }
}

// cleanup WebRTC stream
const cleanupWebRTCStream = async () => {
  if (webrtcVideoPlayer.value) {
    webrtcVideoPlayer.value.srcObject = null
    webrtcVideoPlayer.value.onerror = null
    webrtcVideoPlayer.value.onloadeddata = null
  }
  
  if (webrtcPlayer.value) {
    try {
      if (typeof webrtcPlayer.value.disconnect === 'function') {
        await webrtcPlayer.value.disconnect()
      } else if (typeof webrtcPlayer.value.stop === 'function') {
        await webrtcPlayer.value.stop()
      }
      console.log('WebRTC stream cleaned')
    } catch (error) {
      console.error('Failed to stop WebRTC stream:', error)
    }
    webrtcPlayer.value = null
  }
  
  playStatus.value.connectionState = 'disconnected'
  playStatus.value.isConnecting = false
  playStatus.value.isPlaying = false
}

const getPlayModeTagType = (playMode) => {
  switch (playMode) {
    case 'webrtc':
      return 'success'
    case 'hls':
      return 'warning'
    case 'native':
      return 'info'
    case 'cameraRTC':
      return 'success'
    default:
      return 'info'
  }
}

// 获取播放模式文本
const getPlayModeText = (playMode) => {
  switch (playMode) {
    case 'webrtc':
      return 'WebRTC'
    case 'hls':
      return 'HLS'
    case 'native':
      return '原生'
    case 'cameraRTC':
      return 'CameraRTC'
    default:
      return '未知'
  }
}

// 获取连接状态标签类型
const getConnectionStateTagType = (state) => {
  switch (state) {
    case 'connected':
      return 'success'
    case 'connecting':
      return 'warning'
    case 'disconnected':
    case 'failed':
      return 'danger'
    default:
      return 'info'
  }
}

// 重新播放
const handleReplay = async () => {
  if (!currentVideoDevice.value.deviceId) {
    return
  }
  
  // 清理当前播放
  await cleanupWebRTCStream()
  await cleanupHLSStream()
  cleanupCameraRTCPlayer()
  
  // 重新播放
  await handlePlay(currentVideoDevice.value)
}

// 开始播放状态监控
const startPlayMonitoring = () => {
  console.log('开始播放状态监控...')
  
  // 清理旧的定时器
  if (playMonitorTimer.value) {
    clearInterval(playMonitorTimer.value)
  }
  
  // 设置新的监控定时器
  playMonitorTimer.value = setInterval(() => {
    monitorPlayStatus()
  }, 2000) // 每2秒检查一次
  
  // 重置状态
  playStatus.value.isConnecting = true
  playStatus.value.hasError = false
  playStatus.value.errorMessage = ''
  playStatus.value.connectionState = 'connecting'
}

// 停止播放状态监控
const stopPlayMonitoring = () => {
  console.log('停止播放状态监控...')
  
  if (playMonitorTimer.value) {
    clearInterval(playMonitorTimer.value)
    playMonitorTimer.value = null
  }
  
  playStatus.value.isPlaying = false
  playStatus.value.isConnecting = false
  playStatus.value.connectionState = 'disconnected'
}

// 监控播放状态
const monitorPlayStatus = async () => {
  try {
    const device = currentVideoDevice.value
    if (!device || !device.deviceId) {
      return
    }
    
    // 检查WebRTC播放状态
    if (device.playMode === 'webrtc' && webrtcPlayer.value) {
      await monitorWebRTCStatus(webrtcPlayer.value)
    }
    
    // 检查HLS播放状态
    if (device.playMode === 'hls' && (simpleHlsPlayer.value || hlsPlayer.value)) {
      await monitorHLSStatus(simpleHlsPlayer.value || hlsPlayer.value)
    }
    
    // 检查播放质量
    await monitorPlayQuality()
    
  } catch (error) {
    console.error('播放状态监控失败:', error)
    await handlePlayError(error)
  }
}

// 监控WebRTC播放状态
const monitorWebRTCStatus = async (player) => {
  try {
    const peer = player?.peerConnection || player?.pc
    if (!peer) {
      return
    }
    
    const connectionState = peer.connectionState || peer.iceConnectionState || 'unknown'
    playStatus.value.connectionState = connectionState
    
    if (connectionState === 'connected') {
      playStatus.value.isPlaying = true
      playStatus.value.isConnecting = false
      playStatus.value.hasError = false
      playStatus.value.retryCount = 0
      
      if (peer.getStats) {
        const stats = await peer.getStats()
        updateQualityStats(stats)
      }
      
    } else if (connectionState === 'disconnected' || connectionState === 'failed') {
      playStatus.value.isPlaying = false
      playStatus.value.isConnecting = false
      
      if (connectionState === 'failed') {
        handlePlayError(new Error('WebRTC connection failed'))
      }
    }
    
  } catch (error) {
    console.error('WebRTC status monitor failed:', error)
    await handlePlayError(error)
  }
}

const monitorHLSStatus = async (videoElement) => {
  try {
    if (!videoElement) {
      return
    }
    
    const isPlaying = !videoElement.paused && !videoElement.ended && videoElement.readyState > 2
    playStatus.value.isPlaying = isPlaying
    playStatus.value.isConnecting = false
    playStatus.value.connectionState = isPlaying ? 'connected' : 'disconnected'
    
    if (isPlaying) {
      playStatus.value.hasError = false
      playStatus.value.retryCount = 0
      
      // 更新质量统计
      playStatus.value.qualityStats.resolution = `${videoElement.videoWidth}x${videoElement.videoHeight}`
      playStatus.value.qualityStats.frameRate = videoElement.mozPresentedFrames || 0
    }
    
  } catch (error) {
    console.error('HLS状态监控失败:', error)
    await handlePlayError(error)
  }
}

// 监控播放质量
const monitorPlayQuality = async () => {
  try {
    const device = currentVideoDevice.value
    if (!device || !device.deviceId) {
      return
    }
    
    // 获取网络质量信息
    const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection
    if (connection) {
      // 记录网络状况变化
      if (connection.effectiveType !== playStatus.value.lastNetworkType) {
        console.log('网络状况变化:', connection.effectiveType)
        playStatus.value.lastNetworkType = connection.effectiveType
        
        // 如果网络状况恶化，考虑切换播放模式
        if (connection.effectiveType === 'slow-2g' || connection.effectiveType === '2g') {
          if (device.playMode === 'webrtc') {
            console.log('检测到网络状况恶化，考虑切换到HLS播放')
            showNetworkDegradationWarning()
          }
        }
      }
    }
    
  } catch (error) {
    console.error('播放质量监控失败:', error)
  }
}

// 更新质量统计
const updateQualityStats = (stats) => {
  try {
    stats.forEach(report => {
      if (report.type === 'inbound-rtp' && report.mediaType === 'video') {
        playStatus.value.qualityStats.bitrate = report.bytesReceived || 0
        playStatus.value.qualityStats.packetsLost = report.packetsLost || 0
        playStatus.value.qualityStats.frameRate = report.framesPerSecond || 0
      }
    })
  } catch (error) {
    console.error('更新质量统计失败:', error)
  }
}

// 处理播放错误
const handlePlayError = async (error) => {
  console.error('播放错误:', error)
  
  playStatus.value.hasError = true
  playStatus.value.errorMessage = error.message || '播放出现未知错误'
  playStatus.value.lastErrorTime = new Date()
  playStatus.value.isPlaying = false
  playStatus.value.isConnecting = false
  
  // 更新失败统计
  playStatistics.value.failedAttempts++
  
  // 如果重试次数未达到上限，尝试自动重试
  if (playStatus.value.retryCount < playStatus.value.maxRetries) {
    console.log(`播放失败，将在3秒后进行第${playStatus.value.retryCount + 1}次重试...`)
    playStatus.value.retryCount++
    
    setTimeout(() => {
      attemptAutoRecovery()
    }, 3000)
  } else {
    console.log('已达到最大重试次数，停止自动重试')
    ElMessage.error(`播放失败: ${error.message}`)
    stopPlayMonitoring()
  }
}

// 尝试自动恢复
const attemptAutoRecovery = async () => {
  try {
    console.log('尝试自动恢复播放...')
    
    const device = currentVideoDevice.value
    if (!device || !device.deviceId) {
      return
    }
    
    // 清理当前播放状态
    await cleanupWebRTCStream()
    await cleanupHLSStream()
    cleanupCameraRTCPlayer()
    
    // 重新选择播放策略
    const streamType = getStreamType(device.streamUrl)
    let newStrategy = await determinePlayStrategy(device, streamType)
    
    // 如果当前策略失败，尝试其他策略
    if (newStrategy === device.playMode) {
      if (device.playMode === 'webrtc') {
        console.log('WebRTC失败，尝试HLS播放')
        newStrategy = 'hls'
      } else if (device.playMode === 'hls') {
        console.log('HLS失败，尝试WebRTC播放')
        newStrategy = 'webrtc'
      }
    }
    
    // 执行新的播放策略
    await executePlayStrategy(device, newStrategy)
    
    console.log('自动恢复播放成功')
    ElMessage.success('播放已自动恢复')
    
  } catch (error) {
    console.error('自动恢复失败:', error)
    await handlePlayError(error)
  }
}

// 显示网络状况恶化警告
const showNetworkDegradationWarning = () => {
  ElMessage.warning('检测到网络状况恶化，建议切换到HLS播放以获得更稳定的体验')
}

// 获取播放状态文本
const getPlayStatusText = () => {
  if (playStatus.value.isPlaying) {
    return '播放中'
  } else if (playStatus.value.isConnecting) {
    return '连接中'
  } else if (playStatus.value.hasError) {
    return '播放错误'
  } else {
    return '已停止'
  }
}

// 获取播放状态类型
const getPlayStatusType = () => {
  if (playStatus.value.isPlaying) {
    return 'success'
  } else if (playStatus.value.isConnecting) {
    return 'warning'
  } else if (playStatus.value.hasError) {
    return 'danger'
  } else {
    return 'info'
  }
}

// 在VLC中打开RTSP流
const openInVlc = () => {
  const rtspUrl = currentVideoDevice.value.originalRtspUrl || currentVideoDevice.value.streamUrl
  const vlcUrl = `vlc://${rtspUrl}`
  try {
    window.open(vlcUrl, '_blank')
    ElMessage.info('已尝试在VLC中打开RTSP流')
  } catch (error) {
    console.error('打开VLC失败:', error)
    ElMessage.warning('无法自动打开VLC，请手动在VLC中打开RTSP地址')
  }
}

// 初始化HLS播放器
const initSimpleHLSPlayer = () => {
  if (!simpleHlsPlayer.value || !currentVideoDevice.value.streamUrl) return
  
  const video = simpleHlsPlayer.value
  const streamUrl = currentVideoDevice.value.streamUrl
  
  console.log('初始化简单HLS播放器，URL:', streamUrl)
  
  // 清理之前的实例
  if (video.hlsInstance) {
    video.hlsInstance.destroy()
    video.hlsInstance = null
  }
  
  if (window.Hls && window.Hls.isSupported()) {
    const hls = new window.Hls({
      debug: true,
      enableWorker: true
    })
    
    video.hlsInstance = hls
    
    hls.loadSource(streamUrl)
    hls.attachMedia(video)
    
    hls.on(window.Hls.Events.MANIFEST_PARSED, () => {
      console.log('HLS manifest 解析成功')
      video.play().catch(error => {
        console.error('HLS 自动播放失败:', error)
        ElMessage.warning('视频自动播放失败，请手动点击播放')
      })
    })
    
    hls.on(window.Hls.Events.ERROR, (event, data) => {
      console.error('HLS 播放错误:', data)
      if (data.fatal) {
        ElMessage.error('HLS播放失败: ' + data.details)
      }
    })
  } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
    console.log('使用原生HLS支持')
    video.src = streamUrl
    video.play().catch(error => {
      console.error('原生HLS播放失败:', error)
      ElMessage.warning('视频自动播放失败，请手动点击播放')
    })
  } else {
    console.error('浏览器不支持HLS播放')
    ElMessage.error('浏览器不支持HLS播放')
  }
}

// 监听视频对话框状态
watch(videoDialogVisible, (newValue) => {
  if (newValue && currentVideoDevice.value.streamUrl) {
    console.log('视频对话框打开，准备初始化播放器')
    console.log('设备信息:', currentVideoDevice.value)
    console.log('原始URL:', currentVideoDevice.value.streamUrl)
    
    const streamType = getStreamType(currentVideoDevice.value.streamUrl)
    console.log('检测到的流类型:', streamType)
    
    setTimeout(() => {
      // 根据播放模式初始化播放器
      if (currentVideoDevice.value.playMode === 'webrtc') {
        return
      } else if (currentVideoDevice.value.playMode === 'hls' || streamType === 'hls') {
        // 如果是转换后的HLS流，使用新的播放器
        if (currentVideoDevice.value.hlsUrl) {
          initHLSPlayer(currentVideoDevice.value.streamUrl)
        } else {
          initSimpleHLSPlayer()
        }
      } else if (streamType === 'youtube') {
        const embedUrl = getYouTubeEmbedUrl(currentVideoDevice.value.streamUrl)
        console.log('YouTube嵌入URL:', embedUrl)
        
        if (!embedUrl) {
          console.error('YouTube嵌入URL生成失败')
          ElMessage.error('YouTube视频URL处理失败')
        }
      } else if (streamType === 'rtsp') {
        console.log('RTSP流已准备就绪，显示RTSP播放提示')
        if (!currentVideoDevice.value.hlsUrl) {
          ElMessage.info('检测到RTSP流，可以转换为HLS格式在浏览器中播放')
        }
      }
    }, 500)
  } else if (!newValue) {
    // 对话框关闭时清理资源
    cleanupHLSStream()
    cleanupWebRTCStream()
  }
})


// 配置操作
const handleConfig = (row) => {
  selectedRow.value = row
  showConfigView.value = true
}

const handleAIEvent = (row) => {
  selectedRow.value = row
  showAIEventView.value = true
}

// 下拉菜单操作
const handleMoreActions = async ({ action, row }) => {
  selectedRow.value = row
  
  switch (action) {
    case 'edit':
      await handleEditSingle(row)
      break
    case 'camera-settings':
      showCameraSettingsView.value = true
      break
    case 'delete':
      handleDeleteSingle(row)
      break
  }
}

// 编辑单个设备
const handleEditSingle = async (row) => {
  try {
    // 加载状态
    const loadingMessage = ElMessage({
      message: '正在加载设备详情...',
      type: 'info',
      duration: 0
    })
    
    // 调用API获取完整的设备信息，包括关联的标签
    const response = await getDeviceById(row.id)
    loadingMessage.close()
    
    if (response.code === 200) {
      editMode.value = 'edit'
      editForm.value = { ...response.data }
      showEditView.value = true
      
      console.log('获取设备详情成功:', response.data)
      console.log('关联的标签ID:', response.data.selectedTags)
    } else {
      ElMessage.error('获取设备详情失败: ' + response.message)
    }
  } catch (error) {
    ElMessage.error('获取设备详情失败，请重试')
    console.error('获取设备详情失败:', error)
  }
}

const handleDeleteSingle = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除设备 "${row.deviceName}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteDevice(row.id)
    ElMessage.success('删除成功')
    loadDeviceList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除设备失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 模型超市
const openModelMarket = () => {
  showModelMarketView.value = true
}

const backToAIEvent = () => {
  showModelMarketView.value = false
}

// 表单保存处理
const handleEditSave = async (formData) => {
  try {
    if (editMode.value === 'edit') {
      await updateDevice(selectedRow.value.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createDevice(formData)
      ElMessage.success('添加成功')
    }
    
    showTableView()
    loadDeviceList()
  } catch (error) {
    console.error('保存设备失败:', error)
    ElMessage.error('保存失败')
  }
}

const handleDeviceFormSave = async (formData) => {
  try {
    await createDevice(formData)
    ElMessage.success('添加成功')
    dialogVisible.value = false
    loadDeviceList()
  } catch (error) {
    console.error('保存设备失败:', error)
    ElMessage.error('保存失败')
  }
}

const handleTimeStrategySave = async (strategyData) => {
  try {
    await saveTimeStrategyAPI(strategyData)
    ElMessage.success('时间策略保存成功')
  } catch (error) {
    console.error('保存时间策略失败:', error)
    ElMessage.error('保存失败')
  }
}

const handleAIEventSave = async (aiEventData) => {
  try {
    // 这里应该调用AI事件配置保存API
    console.log('保存AI事件配置:', aiEventData)
    ElMessage.success('AI事件配置保存成功')
  } catch (error) {
    console.error('保存AI事件配置失败:', error)
    ElMessage.error('保存失败')
  }
}

const handleModelMarketSave = async (modelData) => {
  try {
    // 这里应该调用模型安装API
    console.log('安装模型:', modelData)
    ElMessage.success('模型安装成功')
  } catch (error) {
    console.error('安装模型失败:', error)
    ElMessage.error('安装失败')
  }
}

// 录像控制功能
const toggleRecording = async () => {
  if (!currentVideoDevice.value || !currentVideoDevice.value.id) {
    ElMessage.warning('未选择有效的设备')
    return
  }
  
  if (recordingStatus.value.isRecording) {
    await stopVideoRecording()
  } else {
    await startVideoRecording()
  }
}

// 开始录像
const startVideoRecording = async () => {
  try {
    recordingStatus.value.starting = true
    
    const deviceId = currentVideoDevice.value.id
    const deviceName = currentVideoDevice.value.deviceName || '设备'
    const duration = 60 // 默认60秒
    
    console.log('开始录像:', { deviceId, deviceName, duration })
    
    const response = await startRecording(deviceId, deviceName, duration, 'medium')
    
    if (response && response.data) {
      recordingStatus.value.recordId = response.data.id || response.data.recordId
      recordingStatus.value.isRecording = true
      recordingStatus.value.startTime = new Date()
      recordingStatus.value.duration = 0
      
      // 开始计时器
      startRecordingTimer()
      
      ElMessage.success('开始录像成功')
      console.log('录像已开始，记录ID:', recordingStatus.value.recordId)
    } else {
      throw new Error('录像启动失败：响应数据异常')
    }
  } catch (error) {
    console.error('开始录像失败:', error)
    ElMessage.error(`开始录像失败: ${error.message || '未知错误'}`)
    
    // 重置状态
    recordingStatus.value.isRecording = false
    recordingStatus.value.recordId = null
  } finally {
    recordingStatus.value.starting = false
  }
}

// 停止录像
const stopVideoRecording = async () => {
  try {
    recordingStatus.value.stopping = true
    
    if (!recordingStatus.value.recordId) {
      ElMessage.warning('未找到录像记录ID')
      return
    }
    
    console.log('停止录像:', recordingStatus.value.recordId)
    
    const response = await stopRecording(recordingStatus.value.recordId)
    
    if (response) {
      // 停止计时器
      stopRecordingTimer()
      
      // 重置录像状态
      recordingStatus.value.isRecording = false
      recordingStatus.value.recordId = null
      recordingStatus.value.duration = 0
      recordingStatus.value.startTime = null
      
      ElMessage.success('录像已停止')
      console.log('录像已停止')
    } else {
      throw new Error('录像停止失败：响应异常')
    }
  } catch (error) {
    console.error('停止录像失败:', error)
    ElMessage.error(`停止录像失败: ${error.message || '未知错误'}`)
  } finally {
    recordingStatus.value.stopping = false
  }
}

// 开始录像计时器
const startRecordingTimer = () => {
  if (recordingTimer.value) {
    clearInterval(recordingTimer.value)
  }
  
  recordingTimer.value = setInterval(() => {
    if (recordingStatus.value.startTime) {
      const now = new Date()
      recordingStatus.value.duration = Math.floor((now - recordingStatus.value.startTime) / 1000)
    }
  }, 1000)
}

// 停止录像计时器
const stopRecordingTimer = () => {
  if (recordingTimer.value) {
    clearInterval(recordingTimer.value)
    recordingTimer.value = null
  }
}

// 格式化录像时间
const formatRecordingTime = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// PTZ控制
const handlePTZCommand = async (command) => {
  try {
    console.log('PTZ控制命令:', command)
    
    if (!currentVideoDevice.value || !currentVideoDevice.value.id) {
      ElMessage.warning('未选择有效的设备')
      return
    }
    
    const deviceId = currentVideoDevice.value.id
    
    // 根据命令类型调用相应的API
    switch (command.type) {
      case 'move':
        // 移动控制
        await ptzMove(deviceId, {
          direction: command.direction,
          speed: command.speed || 5
        })
        break
        
      case 'stop':
        // 停止移动
        await ptzStop(deviceId)
        break
        
      case 'zoom':
        // 缩放控制
        await ptzZoom(deviceId, {
          direction: command.direction, // 'in' | 'out'
          speed: command.speed || 5
        })
        break
        
      case 'preset':
        // 预置位控制
        if (command.action === 'set') {
          console.log(`设置预置位 ${command.presetId}`)
          // 这里可以调用设置预置位的API
        } else if (command.action === 'go') {
          console.log(`转到预置位 ${command.presetId}`)
          // 这里可以调用转到预置位的API
        }
        break
        
      case 'focus':
        // 聚焦控制
        console.log('聚焦控制:', command.direction)
        break
        
      case 'iris':
        // 光圈控制
        console.log('光圈控制:', command.direction)
        break
        
      default:
        console.warn('未知的PTZ控制命令:', command.type)
        ElMessage.warning('未知的PTZ控制命令')
        return
    }
    
    ElMessage.success(`PTZ ${command.type} 控制成功`)
  } catch (error) {
    console.error('PTZ控制失败:', error)
    ElMessage.error(`PTZ控制失败: ${error.message || '未知错误'}`)
  }
}

// 对话框关闭
const handleDialogClose = () => {
  dialogVisible.value = false
  deviceForm.value = {}
}

// 设备树相关操作
const handleAddDevice = (node) => {
  console.log('添加设备到节点:', node)
  handleAdd()
}

const handleDeleteDevice = (node) => {
  console.log('删除设备节点:', node)
}

const handleBottomAdd = () => {
  handleAdd()
}

const handleBottomDelete = () => {
  handleDelete()
}

// 错误处理
const handleError = (error, context) => {
  console.error(`${context}错误:`, error)
  ElMessage.error(`${context}失败，请重试`)
}

// 新增的YouTube视频加载和错误处理
const handleIframeLoad = () => {
  console.log('YouTube视频加载成功')
}

const handleIframeError = () => {
  console.error('YouTube视频加载失败')
  ElMessage.error('YouTube视频加载失败')
}
</script>

<style scoped lang="scss">
.device-management {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  
  .search-form-container {
    background: white;
    padding: 16px 20px;
    border-bottom: 1px solid #ebeef5;
    
    .search-form {
      .search-row {
        display: flex;
        align-items: center;
        gap: 16px;
        flex-wrap: wrap;
        
        .search-item {
          display: flex;
          align-items: center;
          gap: 8px;
          
          &.date-with-buttons {
            display: flex;
            align-items: center;
            gap: 12px;
            
            .search-buttons {
              display: flex;
              gap: 8px;
            }
          }
        }
      }
    }
  }
  
  .main-content {
    flex: 1;
    display: flex;
    overflow: hidden;
    
    .device-tree-container {
      width: 280px;
      transition: all 0.3s ease;
      border-right: 1px solid #ebeef5;
      background: white;
      
      &.collapsed {
        width: 0;
        overflow: hidden;
      }
    }
    
    .table-container {
      flex: 1;
      display: flex;
      flex-direction: column;
      background: white;
      
      .content-header {
        padding: 16px 20px;
        border-bottom: 1px solid #ebeef5;
        
        .breadcrumb {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 14px;
          
          .breadcrumb-item {
            color: #606266;
            cursor: pointer;
            
            &:hover {
              color: #409eff;
            }
            
            &.active {
              color: #409eff;
              font-weight: 500;
            }
            
            &.filter-text {
              color: #409eff;
              background: #ecf5ff;
              padding: 2px 8px 2px 12px;
              border-radius: 4px;
              position: relative;
              padding-right: 20px;
              
              &::after {
                content: '×';
                position: absolute;
                right: 6px;
                top: 50%;
                transform: translateY(-50%);
                font-size: 14px;
                color: #409eff;
                opacity: 0.6;
                transition: opacity 0.2s;
              }
              
              &:hover::after {
                opacity: 1;
              }
            }
          }
          
          .breadcrumb-separator {
            color: #c0c4cc;
          }
          
          .expand-device-tree-btn {
            margin-right: 8px;
          }
        }
      }
      
      .table-view {
        flex: 1;
        display: flex;
        flex-direction: column;
        padding: 20px;
        
        .toolbar {
          margin-bottom: 16px;
          display: flex;
          justify-content: space-between;
          align-items: center;
          
          .toolbar-left {
            display: flex;
            gap: 12px;
          }
          
          .toolbar-right {
            display: flex;
            align-items: center;
          }
        }
        
        .table-content {
          flex: 1;
          display: flex;
          flex-direction: column;
          
          .action-buttons {
            display: flex;
            gap: 8px;
            justify-content: flex-end;
            align-items: center;
            
            .config-button {
              flex-shrink: 0;
            }
            
            // PlayButton组件使用自己的原始样式，不做任何覆盖
          }
          
          .table-pagination {
            display: flex;
            justify-content: center;
            margin-top: 20px;
            padding-top: 16px;
            border-top: 1px solid #ebeef5;
          }
        }
      }
      
      .edit-view,
      .camera-settings-view,
      .config-view,
      .ai-event-view,
      .model-market-view {
        flex: 1;
        overflow-y: auto;
      }
    }
  }
  
  :deep(.delete-dropdown-item) {
    color: #f56c6c;
    
    &:hover {
      background-color: #fef0f0;
      color: #f56c6c;
    }
  }
  
  // 统一按钮颜色样式
  .config-button {
    background: transparent !important;
    border: none !important;
    color: #1A53FF !important;
    
    &:hover {
      background: rgba(26, 83, 255, 0.1) !important;
      color: #1A53FF !important;
    }
    
    &:active {
      background: rgba(26, 83, 255, 0.2) !important;
      color: #1A53FF !important;
    }
  }
  
  // 更多按钮和其他操作按钮样式
  .action-buttons {
    :deep(.el-button) {
      &:not(.play-button) {
        background: transparent !important;
        border: none !important;
        color: #1A53FF !important;
        
        &:hover {
          background: rgba(26, 83, 255, 0.1) !important;
          color: #1A53FF !important;
        }
        
        &:active {
          background: rgba(26, 83, 255, 0.2) !important;
          color: #1A53FF !important;
        }
      }
    }
  }
  
  // 表格工具栏按钮样式 - 排除ActionButtonGroup内的按钮
  .table-toolbar {
    :deep(.el-button) {
      &:not(.el-button--danger):not(.add-btn-custom):not(.edit-btn-custom):not(.delete-btn-custom) {
        background: transparent !important;
        border: none !important;
        color: #1A53FF !important;
        
        &:hover {
          background: rgba(26, 83, 255, 0.1) !important;
          color: #1A53FF !important;
        }
        
        &:active {
          background: rgba(26, 83, 255, 0.2) !important;
          color: #1A53FF !important;
        }
      }
    }
  }
  
  // 搜索按钮样式
  .search-buttons {
    :deep(.el-button) {
      &:not(.el-button--danger) {
        background: transparent !important;
        border: none !important;
        color: #1A53FF !important;
        
        &:hover {
          background: rgba(26, 83, 255, 0.1) !important;
          color: #1A53FF !important;
        }
        
        &:active {
          background: rgba(26, 83, 255, 0.2) !important;
          color: #1A53FF !important;
        }
      }
    }
  }
}

// 响应式设计
@media (max-width: 1200px) {
  .device-management {
    .main-content {
      .device-tree-container {
        width: 240px;
      }
    }
    
    .search-form {
      .search-row {
        flex-direction: column;
        align-items: stretch;
        
        .search-item {
          width: 100%;
          
          &.date-with-buttons {
            flex-direction: column;
            gap: 12px;
            

            
            .search-buttons {
              justify-content: center;
            }
          }
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .device-management {
    .main-content {
      flex-direction: column;
      
      .device-tree-container {
        width: 100%;
        height: 200px;
        border-right: none;
        border-bottom: 1px solid #ebeef5;
        
        &.collapsed {
          height: 0;
        }
      }
    }
    
    .table-content {
      .action-buttons {
        flex-direction: column;
        align-items: stretch;
        
        .el-button {
          margin: 0;
        }
      }
    }
  }
}

// 视频播放对话框样式
.video-player-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 800px;
  max-height: 85vh;
  overflow: hidden;
  
  // 播放控制栏样式
  .player-controls {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: #f5f7fa;
    border-radius: 8px;
    border: 1px solid #e4e7ed;
    
    .control-left {
      display: flex;
      align-items: center;
      gap: 16px;
      
      .play-mode-selector {
        display: flex;
        align-items: center;
        gap: 8px;
        
        &::before {
          content: '播放模式:';
          font-size: 14px;
          color: #606266;
        }
      }
      
      .play-status {
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }
    
    .control-right {
      display: flex;
      align-items: center;
      gap: 8px;
      
      .recording-status {
        margin-right: 8px;
        
        .el-tag {
          .recording-icon {
            animation: blink 1.5s infinite;
          }
        }
      }
      
      .quality-stats {
        margin-right: 8px;
        
        .stats-info {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 12px;
          color: #666;
          
          span {
            padding: 2px 6px;
            background: #f0f0f0;
            border-radius: 3px;
            font-weight: 500;
          }
        }
      }
    }
  }
  
  // 视频播放器区域容器
  .video-player-content {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    flex: 1;
    min-height: 0;
  }
  
  .video-player-section {
    flex: none; // 固定尺寸，不伸缩
    
    .simple-video-player {
      width: 1340px;
      height: 762px;
      background: #000;
      border-radius: 8px;
      overflow: hidden;
      position: relative;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); // 添加阴影
      
      .debug-info {
        position: absolute;
        top: 10px;
        left: 10px;
        background: rgba(0, 0, 0, 0.8);
        color: white;
        padding: 10px;
        border-radius: 4px;
        font-size: 12px;
        z-index: 999;
      }
      
      .video-element {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .camera-rtc-player {
        width: 100%;
        height: 100%;
      }
      
      .video-iframe {
        width: 100%;
        height: 100%;
        border: none;
      }
      
      .video-error {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 100%;
        background: #f5f5f5;
        
        .error-content {
          text-align: center;
          color: #666;
        }
      }
      
      // RTSP流容器样式
      .rtsp-container {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 100%;
        background: #000;
        
        // WebRTC播放器样式
        .webrtc-player {
          position: relative;
          width: 100%;
          height: 100%;
          background: #000;
          border-radius: 8px;
          overflow: hidden;
          
          iframe {
            width: 100%;
            height: 100%;
            border: none;
            border-radius: 8px;
          }
          
          .webrtc-info {
            position: absolute;
            bottom: 8px;
            left: 8px;
            background: rgba(0, 0, 0, 0.7);
            color: white;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 12px;
            pointer-events: none;
          }
        }
        
        .rtsp-conversion-options {
          display: flex;
          align-items: center;
          justify-content: center;
          height: 100%;
          background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
          
          .conversion-info {
            text-align: center;
            padding: 40px;
            background: white;
            border-radius: 12px;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
            max-width: 600px;
            
            .rtsp-icon {
              margin-bottom: 20px;
            }
            
            .rtsp-title {
              font-size: 24px;
              font-weight: 600;
              color: #303133;
              margin-bottom: 16px;
            }
            
            .rtsp-url {
              background: #f5f7fa;
              border: 1px solid #e4e7ed;
              border-radius: 6px;
              padding: 12px 16px;
              font-family: 'Courier New', monospace;
              font-size: 14px;
              color: #606266;
              word-break: break-all;
              margin-bottom: 20px;
              user-select: all;
            }
            
            .conversion-note {
              text-align: left;
              color: #606266;
              line-height: 1.6;
              margin-bottom: 24px;
              
              p {
                margin: 0 0 12px 0;
              }
            }
            
            .conversion-actions {
              display: flex;
              justify-content: center;
              gap: 12px;
              flex-wrap: wrap;
              margin-bottom: 20px;
            }
            
            .conversion-tips {
              background: #f8f9fa;
              border-radius: 6px;
              padding: 16px;
              text-align: left;
              
              p {
                margin: 0 0 8px 0;
                font-weight: 600;
                color: #303133;
              }
              
              ul {
                margin: 8px 0 0 0;
                padding-left: 20px;
                
                li {
                  margin-bottom: 6px;
                  color: #666;
                  font-size: 13px;
                }
              }
            }
          }
        }
      }
    }
  }
  
  .ptz-control-section {
    flex: none; // 固定宽度
    width: 330px; // 给PTZ控制面板足够的宽度
    max-height: 85vh; // 限制最大高度
    display: flex;
    flex-direction: column;
    
    .ptz-control-wrapper {
      height: 100%;
      overflow-y: auto; // 允许垂直滚动
      overflow-x: hidden; // 隐藏水平滚动
      background: transparent; // 取消背景色
      border-radius: 0; // 取消圆角
      padding: 0; // 取消内边距
      // 取消阴影和边框
      
      // 自定义滚动条样式
      &::-webkit-scrollbar {
        width: 6px;
      }
      
      &::-webkit-scrollbar-track {
        background: #f1f1f1;
        border-radius: 3px;
      }
      
      &::-webkit-scrollbar-thumb {
        background: #c1c1c1;
        border-radius: 3px;
        
        &:hover {
          background: #a8a8a8;
        }
      }
    }
  }
}

// 录像图标闪烁动画
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0.3; }
}

// 对于小屏幕的响应式处理
@media (max-height: 900px) {
  .video-player-container {
    max-height: 80vh;
    
    .ptz-control-section {
      max-height: 80vh;
    }
  }
}
</style> 
