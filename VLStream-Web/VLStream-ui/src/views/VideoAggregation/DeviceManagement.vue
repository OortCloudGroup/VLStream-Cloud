<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div class="device-management tenant_Page draHeaPB">
    <div class="tenant_content">
      <div class="tableTenBox flexRowAC">
        <!-- device -->
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
            />
          </div>
          <el-tree
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
                  <el-tooltip :open-delay="500" effect="light" :content="node.label" placement="top">
                    <div
                      class="tree-node-label"
                      :class="{ activeDept: data.id === currentTreeNodeId }"
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
                    width="14"
                    height="14"
                    name="delete"
                    color="red"
                    class="tree-action-icon"
                    @click="handleDeleteDevice(data)"
                  />
                  <oort-svg-icon
                    width="14"
                    height="14"
                    name="add"
                    class="tree-action-icon"
                    @click="handleAddDevice({ command: 'child', data })"
                  />
                </div>
              </div>
            </template>
          </el-tree>
        </div>

        <!--  -->
        <div class="tableTenItU">
          <!-- ( ) -->
          <div v-if="deviceTreeCollapsed || treeFilterText" class="content-header">
            <div class="breadcrumb">
              <CollapseToggle
                v-if="deviceTreeCollapsed"
                class="expand-device-tree-btn"
                :is-expanded="false"
                @toggle="toggleDeviceTree"
              />
              <span class="breadcrumb-item" @click="clearTreeFilter">设备列表</span>
              <span v-if="treeFilterText" class="breadcrumb-separator">></span>
              <span v-if="treeFilterText" class="breadcrumb-item filter-text" @click="clearTreeFilter">{{ treeFilterText }}</span>
            </div>
          </div>

          <!--  -->
          <div class="table-view">
            <div class="depNameBox_out flexRowAC">
              <div class="depNameBox flexRowAC">
                <div class="exportBtnBox flexRowAC">
                <button type="button" class="exportBtn newBtn flexRowAC" @click="handleAdd">
                  <el-icon class="BtnImg">
                    <Plus />
                  </el-icon>
                  新建
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
              v-loading="loading"
              class="new_table"
              header-cell-class-name="header_tenant_cell"
              stripe
              :data="paginatedTableData"
              current-row-key="id"
              @selection-change="handleSelectionChange"
              @row-click="handleRowClick"
            >
              <el-table-column type="selection" :width="clacPXToVW(55)" />
              <el-table-column label="序号" :width="clacPXToVW(65)">
                <template #default="scope">
                  {{ scope.$index + (currentPage - 1) * pageSize + 1 }}
                </template>
              </el-table-column>
              <el-table-column prop="deviceName" label="设备名称" show-overflow-tooltip />
              <el-table-column prop="deviceId" label="设备ID" show-overflow-tooltip />
              <el-table-column prop="tags" label="标签名称">
                <template #default="scope">
                  <template v-if="scope.row.tags && scope.row.tags.length > 0">
                    <el-tag
                      v-for="tag in scope.row.tags"
                      :key="tag"
                      size="small"
                      type="primary"
                      class="tag_pill"
                    >
                      {{ tag }}
                    </el-tag>
                  </template>
                  <el-tag v-else size="small" type="info">未分类</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="streamUrl" label="视频流路径" show-overflow-tooltip />
              <el-table-column prop="createTime" label="创建时间" :formatter="formatDateTime" />
              <el-table-column fixed="right" align="right" label="操作" :width="clacPXToVW(280)">
                <template #default="scope">
                  <div class="operateAppBox flexRowAC" @click.stop>
                    <div class="new_table_svg_group" @click="handlePlay(scope.row)">
                      <oort-svg-icon width="14" height="14" name="play" class="new_table_svg_group_svg" />
                      <span>播放</span>
                    </div>
                    <div class="new_table_svg_group" @click="handleConfig(scope.row)">
                      <oort-svg-icon width="14" height="14" name="setting" class="new_table_svg_group_svg" />
                      <span>配置录像</span>
                    </div>
                    <el-dropdown @command="handleMoreActions" trigger="click">
                      <div class="new_table_svg_group">
                        <oort-svg-icon width="14" height="14" name="table_more" class="new_table_svg_group_svg" />
                        <span>更多</span>
                      </div>
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
            </TableSelf>

            <div class="paginationBox flexRowAC">
              <el-pagination
                background
                :current-page="currentPage"
                :page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="filteredTotal"
                layout="total, prev, pager, next, sizes"
                class="justifyAlign"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!--  -->
    <el-dialog
      v-model="videoDialogVisible"
      title="视频播放"
      width="90%"
      top="5vh"
      @close="handleVideoClose"
    >
      <div class="video-player-container">
        <!-- control -->
        <div class="player-controls">
          <div class="control-left">
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
            <!-- recording -->
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

            <!-- recordingbutton -->
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

        <!-- and PTZcontrol -->
        <div class="video-player-content">
          <!--  -->
          <div class="video-player-section">
            <div class="simple-video-player">
              <!-- info -->
<!--              <div v-if="currentVideoDevice.streamUrl" -->
<!--                   class="debug-info">-->
<!-- <div>device: {{ currentVideoDevice.deviceName }}</div> -->
<!--                <div>URL: {{ currentVideoDevice.streamUrl }}</div>-->
<!-- <div> : {{ getStreamType(currentVideoDevice.streamUrl) }}</div> -->
<!-- <div v-if="currentVideoDevice.playMode"> : {{ getPlayModeText(currentVideoDevice.playMode) }}</div> -->
<!--              </div>-->

              <!-- RTSP ZLMediaKit to WebRTC -->
              <RtcPlayer
                v-if="zlmWebrtcUrl"
                :key="zlmWebrtcUrl"
                :video-url="zlmWebrtcUrl"
                class="zlm-rtc-player"
              />

              <!-- non-RTSP OPlayer -->
              <div
                v-else-if="currentVideoDevice.streamUrl"
                ref="oplayerContainer"
                class="oplayer-container"
              />

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

              <!-- HLS -->
              <video
                v-else-if="getStreamType(currentVideoDevice.streamUrl) === 'hls' || currentVideoDevice.playMode === 'hls'"
                ref="simpleHlsPlayer"
                class="video-element"
                controls
                autoplay
                muted
              ></video>

              <!-- MP4 etc. -->
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

              <!-- RTSP Process -->
              <div v-else-if="getStreamType(currentVideoDevice.originalRtspUrl || currentVideoDevice.streamUrl) === 'rtsp'"
                   class="rtsp-container">

                <!-- WebRTC - VLStream-server webrtcUrl -->
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

                <!-- : WebRTC -->
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

                <!-- WebRTCservice after -->
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

                <!-- HLS after -->
                <video
                  v-if="currentVideoDevice.hlsUrl && !webrtcConfig.available"
                  ref="hlsPlayer"
                  class="video-element"
                  controls
                  autoplay
                  muted
                ></video>
              </div>

              <!--  -->
              <div v-else class="video-error">
                <div class="error-content">
                  <h3>无法播放视频</h3>
                  <p>URL: {{ currentVideoDevice.streamUrl || '未设置' }}</p>
                  <p>类型: {{ getStreamType(currentVideoDevice.streamUrl) }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- PTZcontrol -->
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

    <!-- device -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="35%"
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
import {ArrowDown, DocumentCopy, Refresh, VideoCamera, Folder, Collection, Plus } from '@element-plus/icons-vue'
import {ElLoading, ElMessage, ElMessageBox} from 'element-plus'
import Hls from 'hls.js'

// Import component
import PTZControl from '@/components/PTZControl.vue'
import CollapseToggle from '@/components/CollapseToggle.vue'
import RtspPlayer from '@/components/RtspPlayer.vue'
import RtcPlayer from '@/components/rtcPlayer/index.vue'
import DeviceEditForm from './DeviceEditForm.vue'

// Import API
import {
  batchDeleteDevices,
  createDevice,
  createDevicePreview,
  deleteDevice,
  getDeviceById,
  getDeviceList,
  getDeviceTree,
  ptzMove,
  ptzStop,
  ptzZoom
} from '@/api/device'
import {getTagTree} from '@/api/tagManagement'
import {startHLSStream, stopHLSStream} from '@/api/stream'
import {startRecording, stopRecording} from '@/api/videoRecord'
import {ensureWebRTCBackendConfig, getWebRTCBackendConfig, WEBRTC_SERVER_BASE_URL} from '@/api/webrtc'

// Import and
import {formatDateTime, getStreamType, getYouTubeEmbedUrl} from './deviceUtils.js'
import { CAMERA_RTC_SOCKET_URL, ensureOPlayer, parseCameraRtcConfig } from '@/utils/oplayer'
import { clacPXToVW } from '@/utils/index'

const router = useRouter()

// formdata
const searchForm = ref({
  deviceName: '',
  deviceId: '',
  tagName: '',
  dateRange: []
})

// device
const deviceTreeData = ref([])
const searchTreeKeyword = ref('')
const currentTreeNodeId = ref(null)
const hoveredTreeNodeId = ref(null)
const treeDefaultProps = {
  children: 'children',
  label: 'label'
}

const filteredDeviceTreeData = computed(() => {
  if (!searchTreeKeyword.value) return deviceTreeData.value
  const keyword = searchTreeKeyword.value.toLowerCase()
  const filterNode = (nodes) => nodes.filter(node => {
    if (node.label?.toLowerCase().includes(keyword)) return true
    if (node.children?.length) return filterNode(node.children).length > 0
    return false
  }).map(node => node.children?.length ? { ...node, children: filterNode(node.children) } : node)
  return filterNode(deviceTreeData.value)
})

const toolbarButtonList = computed(() => [
  { name: '编辑', svg: 'table_edit', clickFn: handleEdit },
  { name: '删除', svg: 'table_del', clickFn: handleDelete }
])

// tabledata
const tableData = ref([])
const loading = ref(false)
const total = ref(0)

// property: device data
const filteredTableData = computed(() => {
  let filtered = tableData.value

  // device
  if (selectedTreeNode.value) {
    const node = selectedTreeNode.value

    // if in is device node ( node)
    if (node.type === 'device_type') {
      // device
      const deviceTypeLabel = node.label.split(' (')[0] // , " (2)" -> " "
      filtered = filtered.filter(device => device.deviceType === deviceTypeLabel)
    }

    // if in is devicenode ( node)
    if (node.type === 'device') {
      // deviceID , only in device
      filtered = filtered.filter(device => device.id === node.deviceId)
    }
  }

  return filtered
})

// property: data
const paginatedTableData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredTableData.value.slice(start, end)
})

// property: after
const filteredTotal = computed(() => {
  return filteredTableData.value.length
})

// related
const currentPage = ref(1)
const pageSize = ref(10)

// table in
const selectedRows = ref([])
const selectedRow = ref(null)

// device in node
const selectedTreeNode = ref(null)
const treeFilterText = ref('')

// (Add dialog editMode)
const editMode = ref('add') // 'add' | 'edit'

// device
const deviceTreeCollapsed = ref(false)

// related
const dialogVisible = ref(false)
const dialogTitle = ref('')

// related
const videoDialogVisible = ref(false)
const currentVideoDevice = ref({})
const simpleHlsPlayer = ref(null)
const hlsPlayer = ref(null)
const webrtcVideoPlayer = ref(null)
const cameraRtcContainer = ref(null)
const cameraRtcPlayer = shallowRef(null)
const oplayerContainer = ref(null)
const oplayerInstance = shallowRef(null)
const zlmWebrtcUrl = ref('')
let activeOPlayerTask = null
const converting = ref(false)

// WebRTCrelated
const webrtcConfig = ref({
  serverUrl: WEBRTC_SERVER_BASE_URL,
  available: false,
  enabled: true
})
let WEBRTC_STREAMER_BASE = WEBRTC_SERVER_BASE_URL

// Load new , /api/webrtc/config .
const getWebRtcScriptUrls = () => [
  `${WEBRTC_STREAMER_BASE}/libs/adapter.min.js`,
  `${WEBRTC_STREAMER_BASE}/webrtcstreamer.js`
]
let webrtcScriptLoader = null
const checkingWebRTC = ref(false)

// can related
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

//
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

//
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

  webrtcScriptLoader = Promise.all(getWebRtcScriptUrls().map(loadScriptTag))
    .catch(error => {
      webrtcScriptLoader = null
      throw error
    })

  return webrtcScriptLoader
}

const playMonitorTimer = ref(null)

// recording
const recordingStatus = ref({
  isRecording: false,
  recordId: null,
  duration: 0,
  startTime: null,
  starting: false,
  stopping: false
})

// recording
const recordingTimer = ref(null)

// formdata (Add dialog)
const deviceForm = ref({})

//
onMounted(() => {
  try {
    loadTagNameMap()  // Load
    loadDeviceList()
    loadDeviceTree()
    // checkWebRTCService()
  } catch (error) {
    console.error('设备管理页面初始化失败:', error)
    ElMessage.error('页面初始化失败，请刷新重试')
  }
})

// component
onUnmounted(() => {
  //
  stopPlayMonitoring()

  // recording and
  stopRecordingTimer()

  // if in recording,
  if (recordingStatus.value.isRecording && recordingStatus.value.recordId) {
    console.warn('组件卸载时检测到正在录像，自动停止录像')
    // recording, etc.
    stopRecording(recordingStatus.value.recordId).catch(error => {
      console.error('组件卸载时停止录像失败:', error)
    })
  }

  // WebRTC and HLS
  cleanupWebRTCStream()
  cleanupHLSStream()
  cleanupCameraRTCPlayer()
  cleanupDeviceOPlayer()

  console.log('设备管理组件已卸载，资源已清理')
})

// ID
const tagNameMap = ref(new Map())

// Load
const loadTagNameMap = async () => {
  try {
    const response = await getTagTree()
    if (response.code === 200 && response.data) {
      const tagMap = new Map()

      // , ID
      const traverseTagTree = (nodes) => {
        if (!Array.isArray(nodes)) return

        nodes.forEach(node => {
          if (node.id && node.tagName) {
            tagMap.set(node.id, node.tagName)
          }

          // Process sub node
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

// method
const loadDeviceList = async () => {
  loading.value = true
  try {
    // Load
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

    // page method , getDeviceByIdGet each device info,
    tableData.value = await Promise.all(
        devices.map(async (device) => {
          try {
            // getDeviceByIdGet device , selectedTags
            const detailResponse = await getDeviceById(device.id)

            if (detailResponse.code === 200 && detailResponse.data) {
              const deviceDetail = detailResponse.data

              // Process selectedTagsfield, Convert to tagsarray
              let tags = []
              if (Array.isArray(deviceDetail.selectedTags) && deviceDetail.selectedTags.length > 0) {
                // Get
                tags = deviceDetail.selectedTags.map(tagId => {
                  const tagName = tagNameMap.value.get(tagId)
                  return tagName || `标签${tagId}`
                })
              }

              return {
                ...device,
                ...deviceDetail,
                tags: tags,
                // tagfield, only selectedTagsrelated info
                displayTag: tags.length > 0 ? tags[0] : '未分类'
              }
            } else {
              // if Get failed, in info
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

// Process
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

const handleAdvancedSearch = (searchData) => {
  console.log('高级搜索数据:', searchData)
  // data searchForm in
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
    // if , device
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

// Export relatedProcess
const handleExport = () => {
  ElMessage.info('开始导出数据')
  // item in will Export
}

const handleUpload = () => {
  ElMessage.info('打开上传文件对话框')
  // item in will
}

const handleDownloadTemplate = () => {
  ElMessage.info('下载模板文件')
  // item in will
}

const handleBatchOperation = () => {
  ElMessage.info('打开批量操作界面')
  // item in will operation
}

// Process
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  // if node , new Load data, new Load
  if (!selectedTreeNode.value) {
    loadDeviceList()
  }
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  // if node , new Load data, new Load
  if (!selectedTreeNode.value) {
    loadDeviceList()
  }
}

// tableeventProcess
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleRowClick = (row) => {
  selectedRow.value = row
}

// device eventProcess
const handleNodeClick = (node) => {
  selectedTreeNode.value = node
  currentTreeNodeId.value = node.id

  if (node.type === 'device_type') {
    treeFilterText.value = `设备类型: ${node.label}`
  } else if (node.type === 'device') {
    treeFilterText.value = `设备: ${node.label}`
  } else {
    treeFilterText.value = node.label
  }

  currentPage.value = 1
}

const toggleDeviceTree = () => {
  deviceTreeCollapsed.value = !deviceTreeCollapsed.value
}

const handleDeviceTreeSearch = (keyword) => {
  // Process device
  console.log('设备树搜索:', keyword)
}

// device
const clearTreeFilter = () => {
  selectedTreeNode.value = null
  treeFilterText.value = ''
  currentPage.value = 1
  console.log('已清除设备树过滤条件')
}

//
const showTableView = () => {
  selectedRow.value = null
  selectedTreeNode.value = null
  treeFilterText.value = ''
}

// deviceoperation
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
  router.push({ path: '/device-edit', query: { id: selectedRows.value[0].id } })
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

/**
 * device OPlayer instance.
 */
const cleanupDeviceOPlayer = () => {
  activeOPlayerTask = null

  if (oplayerInstance.value?.compInstance?.$destroy) {
    oplayerInstance.value.compInstance.$destroy()
  }
  oplayerInstance.value = null

  if (oplayerContainer.value) {
    oplayerContainer.value.innerHTML = ''
  }
}

/**
 * device Generate OPlayer parameter.
 */
const createDeviceOPlayerOptions = async (streamUrl) => {
  const streamType = getStreamType(streamUrl)
  const playerConfig = {
    debuggerMode: false,
    autoSize: true,
    backgroundColor: '#000000',
    showHeader: true
  }

  if (streamType === 'cameraRTC') {
    const { cameraId, socketUrl } = parseCameraRtcConfig(streamUrl)
    playerConfig.webRTCSocketURL = socketUrl
    return { playerConfig, playConfig: { type: 'cameraRTC', src: cameraId } }
  }

  if (streamType === 'rtsp') {
    throw new Error('RTSP视频流必须通过ZLMediaKit预览接口播放')
  }

  const playTypeMap = { flv: 'flv', hls: 'm3u8', video: 'mp4', http: 'mp4' }
  const playType = playTypeMap[streamType]
  if (!playType) throw new Error(`暂不支持该视频流类型：${streamType}`)

  return { playerConfig, playConfig: { type: playType, src: streamUrl } }
}

// device
const handlePlay = async (row) => {
  console.log('点击播放设备:', row)
  playStatistics.value.totalAttempts++
  const deviceForPlay = { ...row }

  if (!deviceForPlay.streamUrl || deviceForPlay.streamUrl.trim() === '') {
    ElMessage.warning('设备未配置视频流地址')
    return
  }

  cleanupDeviceOPlayer()
  zlmWebrtcUrl.value = ''
  const playbackTask = Symbol('device-oplayer')
  activeOPlayerTask = playbackTask
  currentVideoDevice.value = deviceForPlay
  videoDialogVisible.value = true

  const loading = ElLoading.service({
    lock: true,
    text: '正在启动视频播放...',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  try {
    if (getStreamType(deviceForPlay.streamUrl) === 'rtsp') {
      const response = await createDevicePreview(deviceForPlay.id)
      const preview = response?.data ?? response
      const webrtcUrl = preview?.webrtcUrl
      if (!webrtcUrl) throw new Error('后端未返回ZLM WebRTC播放地址')
      if (activeOPlayerTask !== playbackTask) return

      zlmWebrtcUrl.value = webrtcUrl
      currentVideoDevice.value.playMode = 'zlm-webrtc'
      await nextTick()
      playStatus.value.isConnecting = false
      playStatus.value.isPlaying = true
      playStatus.value.hasError = false
      playStatus.value.connectionState = 'connected'
      return
    }

    await Promise.all([ensureOPlayer(), nextTick()])
    const { playerConfig, playConfig } = await createDeviceOPlayerOptions(deviceForPlay.streamUrl)
    const container = oplayerContainer.value
    if (activeOPlayerTask !== playbackTask) return
    if (!container) throw new Error('播放器容器未准备好')

    const player = new window.OToolBox.OPlayer(container, playerConfig)
    oplayerInstance.value = player
    player.play({
      ...playConfig,
      name: deviceForPlay.deviceName || deviceForPlay.name || ''
    })

    const playModeMap = { cameraRTC: 'cameraRTC', rtsp: 'rtsp', m3u8: 'hls' }
    currentVideoDevice.value.playMode = playModeMap[playConfig.type] || 'native'
    playStatus.value.isConnecting = false
    playStatus.value.isPlaying = true
    playStatus.value.hasError = false
    playStatus.value.connectionState = 'connected'
  } catch (error) {
    if (activeOPlayerTask !== playbackTask) return
    console.error('播放失败:', error)
    ElMessage.error(`播放失败: ${error.message}`)
    playStatistics.value.failedAttempts++
    playStatus.value.isConnecting = false
    playStatus.value.isPlaying = false
    playStatus.value.hasError = true
    playStatus.value.errorMessage = error.message || '播放失败'
    playStatus.value.connectionState = 'failed'
    cleanupDeviceOPlayer()
    videoDialogVisible.value = false
  } finally {
    loading.close()
  }
}

//
const determinePlayStrategy = async (device, streamType) => {
  if (streamType === 'cameraRTC') {
    return 'cameraRTC'
  }
  // if user
  if (selectedPlayMode.value !== 'auto') {
    return selectedPlayMode.value
  }

  // can
  const strategy = await getOptimalPlayStrategy(device, streamType)
  console.log('智能播放策略分析结果:', strategy)

  return strategy.mode
}

// Get
const getOptimalPlayStrategy = async (device, streamType) => {
  const strategies = []

  // 1: device
  const deviceTypeStrategy = getDeviceTypeStrategy(device)
  strategies.push(deviceTypeStrategy)

  // 2:
  const streamTypeStrategy = getStreamTypeStrategy(streamType)
  strategies.push(streamTypeStrategy)

  // 3:
  const networkStrategy = await getNetworkStrategy()
  strategies.push(networkStrategy)

  // 4: history successfully
  const historyStrategy = getHistoryStrategy()
  strategies.push(historyStrategy)

  // 5:
  const availabilityStrategy = await getAvailabilityStrategy()
  strategies.push(availabilityStrategy)

  console.log('所有策略分析结果:', strategies)

  //
  const finalStrategy = calculateFinalStrategy(strategies)
  console.log('最终播放策略:', finalStrategy)

  return finalStrategy
}

// device
const getDeviceTypeStrategy = (device) => {
  const deviceType = device.tag || device.deviceType || 'unknown'

  switch (deviceType) {
    case '球机':
    case '云台':
      // and need to control
      return {
        mode: 'webrtc',
        score: 0.9,
        reason: '球机/云台设备推荐WebRTC以获得低延时控制'
      }
    case '摄像头':
    case '枪机':
      //
      return {
        mode: 'auto',
        score: 0.5,
        reason: '摄像头设备可使用任何播放模式'
      }
    case '半球':
      // ,
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

//
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

//
const getNetworkStrategy = async () => {
  try {
    //
    const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection

    if (connection) {
      const effectiveType = connection.effectiveType
      const downlink = connection.downlink

      console.log('网络状况:', { effectiveType, downlink })

      //
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
        // WiFi
        return {
          mode: 'webrtc',
          score: 0.9,
          reason: '高速网络，推荐WebRTC'
        }
      }
    }

    // if method ,
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

// history successfully
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

//
const getAvailabilityStrategy = async () => {
  try {
    // WebRTCservice
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

//
const calculateFinalStrategy = (strategies) => {
  // configuration
  const weights = {
    deviceType: 0.2,
    streamType: 0.3,
    network: 0.2,
    history: 0.2,
    availability: 0.1
  }

  //
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

  //
  let bestMode = 'auto'
  let bestScore = 0

  for (const [mode, data] of Object.entries(modeScores)) {
    if (data.score > bestScore) {
      bestScore = data.score
      bestMode = mode
    }
  }

  // if is auto ,
  if (bestMode === 'auto') {
    // if WebRTC , WebRTC
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

// Execute
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

// Execute WebRTC
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
      // RTSP need to Convert to HLS
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
      // already is HLS ,
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

// Execute
const executeNativePlayStrategy = async (device) => {
  try {
    cleanupCameraRTCPlayer()
    console.log('开始原生播放流程...')

    // ,
    device.playMode = 'native'

    console.log('原生播放流程完成')

  } catch (error) {
    console.error('原生播放策略执行失败:', error)
    throw error
  }
}

// Get
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
  zlmWebrtcUrl.value = ''
  // HLS
  if (simpleHlsPlayer.value && simpleHlsPlayer.value.hlsInstance) {
    simpleHlsPlayer.value.hlsInstance.destroy()
    simpleHlsPlayer.value.hlsInstance = null
  }

  // WebRTC
  if (webrtcVideoPlayer.value) {
    webrtcVideoPlayer.value.srcObject = null
  }

  //
  cleanupHLSStream()
  cleanupWebRTCStream()
  cleanupCameraRTCPlayer()
  cleanupDeviceOPlayer()

  //
  stopPlayMonitoring()

  // recording (if in recording, user)
  if (recordingStatus.value.isRecording) {
    ElMessage.warning('检测到正在录像，请先停止录像')
    return //
  }

  // recording
  stopRecordingTimer()

  // recording
  recordingStatus.value.isRecording = false
  recordingStatus.value.recordId = null
  recordingStatus.value.duration = 0
  recordingStatus.value.startTime = null
  recordingStatus.value.starting = false
  recordingStatus.value.stopping = false

  videoDialogVisible.value = false
  currentVideoDevice.value = {}
}

// RTSP
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

// WebRTCrelated method
const checkWebRTCService = async () => {
  try {
    checkingWebRTC.value = true

    try {
      const response = await getWebRTCBackendConfig()

      if (response && response.code === 200) {
        webrtcConfig.value = response.data
        WEBRTC_STREAMER_BASE = response.data.serverUrl || WEBRTC_SERVER_BASE_URL
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

      // APIfailed configuration
      webrtcConfig.value = {
        serverUrl: WEBRTC_SERVER_BASE_URL,
        available: true,  // assuming ,
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

  // if WebRTCfailed, HLSConvert
  if (currentVideoDevice.value.originalRtspUrl) {
    ElMessage.warning('WebRTC播放失败，正在尝试HLS播放模式...')

    try {
      // HLSConvert API
      const hlsUrl = await convertRtspToHls(currentVideoDevice.value.originalRtspUrl)
      if (hlsUrl) {
        currentVideoDevice.value.streamUrl = hlsUrl
        currentVideoDevice.value.hlsUrl = hlsUrl
        currentVideoDevice.value.playMode = 'hls'

        ElMessage.success('已切换到HLS播放模式')

        // Initialize HLS
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

  //
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

// WebRTC iframeeventProcess
const handleWebRTCLoad = () => {
  console.log('WebRTC iframe加载完成')
  ElMessage.success('WebRTC播放器加载成功')
  currentVideoDevice.value.connectionState = 'connected'
}

const handleWebRTCError = (error) => {
  console.error('WebRTC iframe加载失败:', error)
  ElMessage.error('WebRTC播放器加载失败')
  currentVideoDevice.value.connectionState = 'failed'

  // HLS
  handleRtspError('WebRTC iframe加载失败')
}

// HLS Convert
const startHLSConversion = async (device) => {
  try {
    const response = await startHLSStream({
      deviceId: device.deviceId,
      rtspUrl: device.streamUrl,
      quality: 'medium'
    })

    if (response.code === 200) {
      // new device URL to HLS
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

// Convert to HLS
const convertToHLS = async () => {
  converting.value = true

  try {
    const hlsUrl = await startHLSConversion(currentVideoDevice.value)
    if (hlsUrl) {
      currentVideoDevice.value.hlsUrl = hlsUrl
      // etc. DOM new afterInitialize HLS
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

// Initialize HLS
const initHLSPlayer = (hlsUrl) => {
  const video = hlsPlayer.value
  if (!video) {
    console.error('HLS播放器元素未找到')
    return
  }

  console.log('初始化HLS播放器:', hlsUrl)

  // before instance
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

// HLS
const cleanupHLSStream = async () => {
  if (currentVideoDevice.value.hlsUrl && currentVideoDevice.value.deviceId) {
    try {
      await stopHLSStream({ deviceId: currentVideoDevice.value.deviceId })
      console.log('HLS流已清理:', currentVideoDevice.value.deviceId)
    } catch (error) {
      console.error('停止HLS流失败:', error)
    }
  }

  // instance
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
    case 'zlm-webrtc':
      return 'success'
    case 'hls':
      return 'warning'
    case 'native':
      return 'info'
    case 'cameraRTC':
      return 'success'
    case 'rtsp':
      return 'success'
    default:
      return 'info'
  }
}

// Get
const getPlayModeText = (playMode) => {
  switch (playMode) {
    case 'webrtc':
      return 'WebRTC'
    case 'zlm-webrtc':
      return 'ZLM WebRTC'
    case 'hls':
      return 'HLS'
    case 'native':
      return '原生'
    case 'cameraRTC':
      return 'CameraRTC'
    case 'rtsp':
      return 'RTSP'
    default:
      return '未知'
  }
}

// Get
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

// new
const handleReplay = async () => {
  if (!currentVideoDevice.value.deviceId) {
    return
  }

  // current
  await cleanupWebRTCStream()
  await cleanupHLSStream()
  cleanupCameraRTCPlayer()
  cleanupDeviceOPlayer()

  // new
  await handlePlay(currentVideoDevice.value)
}

// start
const startPlayMonitoring = () => {
  console.log('开始播放状态监控...')

  // old
  if (playMonitorTimer.value) {
    clearInterval(playMonitorTimer.value)
  }

  // Set new
  playMonitorTimer.value = setInterval(() => {
    monitorPlayStatus()
  }, 2000) // 2

  //
  playStatus.value.isConnecting = true
  playStatus.value.hasError = false
  playStatus.value.errorMessage = ''
  playStatus.value.connectionState = 'connecting'
}

//
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

//
const monitorPlayStatus = async () => {
  try {
    const device = currentVideoDevice.value
    if (!device || !device.deviceId) {
      return
    }

    // WebRTC
    if (device.playMode === 'webrtc' && webrtcPlayer.value) {
      await monitorWebRTCStatus(webrtcPlayer.value)
    }

    // HLS
    if (device.playMode === 'hls' && (simpleHlsPlayer.value || hlsPlayer.value)) {
      await monitorHLSStatus(simpleHlsPlayer.value || hlsPlayer.value)
    }

    //
    await monitorPlayQuality()

  } catch (error) {
    console.error('播放状态监控失败:', error)
    await handlePlayError(error)
  }
}

// WebRTC
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

      // new
      playStatus.value.qualityStats.resolution = `${videoElement.videoWidth}x${videoElement.videoHeight}`
      playStatus.value.qualityStats.frameRate = videoElement.mozPresentedFrames || 0
    }

  } catch (error) {
    console.error('HLS状态监控失败:', error)
    await handlePlayError(error)
  }
}

//
const monitorPlayQuality = async () => {
  try {
    const device = currentVideoDevice.value
    if (!device || !device.deviceId) {
      return
    }

    // Get info
    const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection
    if (connection) {
      // record
      if (connection.effectiveType !== playStatus.value.lastNetworkType) {
        console.log('网络状况变化:', connection.effectiveType)
        playStatus.value.lastNetworkType = connection.effectiveType

        // if ,
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

// new
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

// Process
const handlePlayError = async (error) => {
  console.error('播放错误:', error)

  playStatus.value.hasError = true
  playStatus.value.errorMessage = error.message || '播放出现未知错误'
  playStatus.value.lastErrorTime = new Date()
  playStatus.value.isPlaying = false
  playStatus.value.isConnecting = false

  // new failed
  playStatistics.value.failedAttempts++

  // if not ,
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

//
const attemptAutoRecovery = async () => {
  try {
    console.log('尝试自动恢复播放...')

    const device = currentVideoDevice.value
    if (!device || !device.deviceId) {
      return
    }

    // current
    await cleanupWebRTCStream()
    await cleanupHLSStream()
    cleanupCameraRTCPlayer()

    // new
    const streamType = getStreamType(device.streamUrl)
    let newStrategy = await determinePlayStrategy(device, streamType)

    // if current failed,
    if (newStrategy === device.playMode) {
      if (device.playMode === 'webrtc') {
        console.log('WebRTC失败，尝试HLS播放')
        newStrategy = 'hls'
      } else if (device.playMode === 'hls') {
        console.log('HLS失败，尝试WebRTC播放')
        newStrategy = 'webrtc'
      }
    }

    // Execute new
    await executePlayStrategy(device, newStrategy)

    console.log('自动恢复播放成功')
    ElMessage.success('播放已自动恢复')

  } catch (error) {
    console.error('自动恢复失败:', error)
    await handlePlayError(error)
  }
}

//
const showNetworkDegradationWarning = () => {
  ElMessage.warning('检测到网络状况恶化，建议切换到HLS播放以获得更稳定的体验')
}

// Get
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

// Get
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

// in VLC in RTSP
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

// Initialize HLS
const initSimpleHLSPlayer = () => {
  if (!simpleHlsPlayer.value || !currentVideoDevice.value.streamUrl) return

  const video = simpleHlsPlayer.value
  const streamUrl = currentVideoDevice.value.streamUrl

  console.log('初始化简单HLS播放器，URL:', streamUrl)

  // before instance
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

//
watch(videoDialogVisible, (newValue) => {
  if (!newValue) {
    cleanupDeviceOPlayer()
  }
})


// configurationoperation
const handleConfig = (row) => {
  router.push({ path: '/device-config', query: { id: row.id } })
}

const handleAIEvent = (row) => {
  router.push({ path: '/device-ai-event', query: { id: row.id } })
}

// menuoperation
const handleMoreActions = async ({ action, row }) => {
  selectedRow.value = row

  switch (action) {
    case 'edit':
      router.push({ path: '/device-edit', query: { id: row.id } })
      break
    case 'camera-settings':
      router.push({ path: '/camera-settings', query: { id: row.id } })
      break
    case 'delete':
      handleDeleteSingle(row)
      break
  }
}

// device
const handleEditSingle = async (row) => {
  router.push({ path: '/device-edit', query: { id: row.id } })
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

// form Process (Add dialog)
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

// recordingcontrol can
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

// startrecording
const startVideoRecording = async () => {
  try {
    recordingStatus.value.starting = true

    const deviceId = currentVideoDevice.value.id
    const deviceName = currentVideoDevice.value.deviceName || '设备'
    const duration = 60 // 60

    console.log('开始录像:', { deviceId, deviceName, duration })

    const response = await startRecording(deviceId, deviceName, duration, 'medium')

    if (response && response.data) {
      recordingStatus.value.recordId = response.data.id || response.data.recordId
      recordingStatus.value.isRecording = true
      recordingStatus.value.startTime = new Date()
      recordingStatus.value.duration = 0

      // start
      startRecordingTimer()

      ElMessage.success('开始录像成功')
      console.log('录像已开始，记录ID:', recordingStatus.value.recordId)
    } else {
      throw new Error('录像启动失败：响应数据异常')
    }
  } catch (error) {
    console.error('开始录像失败:', error)
    ElMessage.error(`开始录像失败: ${error.message || '未知错误'}`)

    //
    recordingStatus.value.isRecording = false
    recordingStatus.value.recordId = null
  } finally {
    recordingStatus.value.starting = false
  }
}

// recording
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
      //
      stopRecordingTimer()

      // recording
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

// startrecording
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

// recording
const stopRecordingTimer = () => {
  if (recordingTimer.value) {
    clearInterval(recordingTimer.value)
    recordingTimer.value = null
  }
}

// Format recording
const formatRecordingTime = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// PTZcontrol
const handlePTZCommand = async (command) => {
  try {
    console.log('PTZ控制命令:', command)

    if (!currentVideoDevice.value || !currentVideoDevice.value.id) {
      ElMessage.warning('未选择有效的设备')
      return
    }

    const deviceId = currentVideoDevice.value.id

    // API
    switch (command.type) {
      case 'move':
        // control
        await ptzMove(deviceId, {
          direction: command.direction,
          speed: command.speed || 5
        })
        break

      case 'stop':
        //
        await ptzStop(deviceId)
        break

      case 'zoom':
        // control
        await ptzZoom(deviceId, {
          direction: command.direction, // 'in' | 'out'
          speed: command.speed || 5
        })
        break

      case 'preset':
        // control
        if (command.action === 'set') {
          console.log(`设置预置位 ${command.presetId}`)
          // Set API
        } else if (command.action === 'go') {
          console.log(`转到预置位 ${command.presetId}`)
          // API
        }
        break

      case 'focus':
        // control
        console.log('聚焦控制:', command.direction)
        break

      case 'iris':
        // control
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

//
const handleDialogClose = () => {
  dialogVisible.value = false
  deviceForm.value = {}
}

// device relatedoperation
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

// Process
const handleError = (error, context) => {
  console.error(`${context}错误:`, error)
  ElMessage.error(`${context}失败，请重试`)
}

// Add YouTube Load and Process
const handleIframeLoad = () => {
  console.log('YouTube视频加载成功')
}

const handleIframeError = () => {
  console.error('YouTube视频加载失败')
  ElMessage.error('YouTube视频加载失败')
}
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
  }

  .tree-icon {
    flex-shrink: 0;
    font-size: 14px;
    color: var(--el-color-primary);
  }

  .device-icon { color: #52C41A; }
  .tag-icon { color: #8581dc; }
  .activeDept { color: var(--el-color-primary); }
}

.tableTenItU {
  flex: 1;
  height: 100%;
  overflow: auto;
  min-width: 0;

  :deep(.header_tenant_cell) {
    background: #F8F8F9;
  }
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
  margin-right: 4px;
  border-radius: 12px;
}

.content-header {
  padding-bottom: 12px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
}

.breadcrumb-item {
  color: #606266;
  cursor: pointer;
  font-size: 14px;
}

.breadcrumb-item.active {
  color: #303133;
  font-weight: 500;
}

.breadcrumb-separator {
  color: #c0c4cc;
}

.device-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;

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

            // PlayButtoncomponent ,
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

  // button
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

  // button and operationbutton
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

  // table button - ActionButtonGroup button
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

  // button
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

//
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

//
.video-player-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 800px;
  max-height: 85vh;
  overflow: hidden;

  // control
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

  //
  .video-player-content {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    flex: 1;
    min-height: 0;
  }

  .video-player-section {
    flex: none; // ,

    .simple-video-player {
      width: 1340px;
      height: 762px;
      background: #000;
      border-radius: 8px;
      overflow: hidden;
      position: relative;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); //

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

      .oplayer-container {
        width: 100%;
        height: 100%;
        background: #000;
        overflow: hidden;
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

      // RTSP
      .rtsp-container {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 100%;
        background: #000;

        // WebRTC
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
    flex: none; //
    width: 330px; // PTZcontrol
    max-height: 85vh; //
    display: flex;
    flex-direction: column;

    .ptz-control-wrapper {
      height: 100%;
      overflow-y: auto; //
      overflow-x: hidden; //
      background: transparent; //
      border-radius: 0; //
      padding: 0; //
      // and

      // Custom
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

// recording
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0.3; }
}

// Process
@media (max-height: 900px) {
  .video-player-container {
    max-height: 80vh;

    .ptz-control-section {
      max-height: 80vh;
    }
  }
}
</style>
