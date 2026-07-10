<template>
  <div class="algorithm-arrangement">
    <!-- 查询栏 -->
    <!--
    <div class="query-bar">
      <div class="query-content">
        <div class="search-item">
          <el-input 
            v-model="searchForm.deviceName" 
            placeholder="设备名称/设备ID"
            clearable
            style="width: 240px"
          />
        </div>
        <div class="search-item">
          <el-select 
            v-model="searchForm.deviceType" 
            placeholder="设备类型" 
            clearable 
            style="width: 200px"
          >
            <el-option label="全部" value="" />
            <el-option label="球机" value="ball" />
            <el-option label="枪机" value="gun" />
            <el-option label="半球" value="hemisphere" />
          </el-select>
        </div>
        <div class="search-item">
          <DateRangePicker
            v-model="searchForm.createDateRange"
            start-placeholder="创建日期"
            end-placeholder="修改日期"
            style="width: 300px"
          />
        </div>
        <div class="search-buttons">
          <el-button type="primary" :icon="Search" @click="handleSearch">
            搜索
          </el-button>
          <el-button :icon="Refresh" @click="handleReset">
            重置
          </el-button>
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
          @node-click="handleTreeNodeClick"
          @search="handleDeviceTreeSearch"
          @toggle-collapse="toggleDeviceTree"
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
            <span class="breadcrumb-item">算法编排</span>
          </div>
        </div>

        <!-- 工具栏 -->
        <div class="toolbar">
          <div class="toolbar-left">
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

        <!-- 表格内容 -->
        <div class="table-content">
          <el-table 
            :data="tableData" 
            stripe
            v-loading="loading"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column type="index" label="序号" width="80" align="center" />
            <el-table-column prop="deviceName" label="设备名称" min-width="120" />
            <el-table-column prop="tags" label="标签" min-width="100">
              <template #default="scope">
                <el-tag 
                  v-for="tag in scope.row.tags" 
                  :key="tag"
                  size="small" 
                  type="primary"
                >
                  {{ tag }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deviceId" label="设备ID" min-width="120" />
            <el-table-column prop="deviceType" label="设备类型" min-width="100" />
            <el-table-column prop="deviceLocation" label="设备位置" min-width="150" />
            <el-table-column prop="algorithms" label="拥有算法" min-width="200" />
            <el-table-column label="操作" width="180" fixed="right" align="right">
              <template #default="scope">
                <div class="action-buttons">
                  <PlayButton :disabled="!getDeviceStreamUrl(scope.row)" @click="handlePlay(scope.row)" />
                  <el-button
                      type="primary"
                      text
                      size="small"
                      class="config-button"
                      @click="configureAlgorithm(scope.row)">
                    配置AI算法
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="table-pagination">
            <el-pagination
              v-model:current-page="pagination.currentPage"
              v-model:page-size="pagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="pagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 配置AI算法对话框 -->
    <!-- 视频播放对话框 -->
    <el-dialog
      v-model="videoDialogVisible"
      title="视频播放"
      width="1200px"
      top="5vh"
      @close="handleVideoClose"
    >
      <div class="video-dialog-content">
        <div v-if="!currentStreamUrl" class="video-empty">未配置视频流地址</div>

        <iframe
          v-else-if="currentStreamType === 'youtube'"
          class="video-iframe"
          :src="youtubeEmbedUrl"
          frameborder="0"
          allow="autoplay; encrypted-media"
          allowfullscreen
        />

        <div
          v-else-if="currentStreamType === 'cameraRTC'"
          ref="cameraRtcContainer"
          class="video-element camera-rtc-player"
        ></div>

        <video
          v-else-if="currentStreamType === 'hls'"
          ref="hlsVideoPlayer"
          class="video-element"
          controls
          autoplay
          muted
          playsinline
        />

        <video
          v-else-if="currentStreamType === 'rtsp'"
          id="webrtcDirectPlayer"
          ref="webrtcVideoPlayer"
          class="video-element"
          controls
          autoplay
          muted
          playsinline
        />

        <video
          v-else
          ref="nativeVideoPlayer"
          class="video-element"
          :src="currentStreamUrl"
          controls
          autoplay
          muted
          playsinline
        />
      </div>
    </el-dialog>

    <el-dialog
      v-model="showConfigDialog"
      title="配置AI算法"
      width="500px"
      @close="resetConfigForm"
    >
      <div class="config-dialog-content">
        <div class="config-item">
          <label class="config-label">设备名称</label>
          <div class="device-name">{{ currentDevice?.deviceName || '海康云台' }}</div>
        </div>
        
        <div class="config-item">
          <label class="config-label">AI算法</label>
          <div class="algorithm-selection">
            <div class="selected-algorithms">
              <el-tag
                v-for="algorithm in selectedAlgorithms"
                :key="algorithm.id"
                type="primary"
                effect="dark"
                class="selected-tag"
                closable
                @close="removeAlgorithm(algorithm.id)"
              >
                {{ algorithm.name }}
              </el-tag>
            </div>
            <el-button 
              class="select-algorithm-btn"
              @click="showAlgorithmPanel = true"
            >
              选择算法
            </el-button>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showConfigDialog = false">取消</el-button>
          <el-button type="primary" @click="saveConfiguration">确认</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- AI算法选择面板 -->
    <Teleport to="body">
      <div v-if="showAlgorithmPanel" class="algorithm-panel-overlay" @click="closeAlgorithmPanel">
        <div class="algorithm-panel" @click.stop>
          <div class="panel-header">
            <h3 class="panel-title">选择AI算法</h3>
            <el-button
              type="text"
              class="close-btn"
              @click="closeAlgorithmPanel"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </div>

          <div class="panel-content">
            <!-- 分类标签 -->
            <div class="category-tabs">
              <div
                v-for="category in algorithmCategories"
                :key="category.key"
                class="category-tab"
                :class="{ active: activeCategory === category.key }"
                @click="activeCategory = category.key"
              >
                {{ category.label }}
              </div>
            </div>

            <!-- 算法网格 -->
            <div class="algorithm-grid">
              <div
                v-for="algorithm in filteredAlgorithmList"
                :key="algorithm.id"
                class="algorithm-card"
                :class="{ selected: isAlgorithmSelected(algorithm.id) }"
                @click="toggleAlgorithmSelection(algorithm)"
              >
                <div class="card-image">
                  <img :src="algorithm.image" :alt="algorithm.name" />
                  <div v-if="isAlgorithmSelected(algorithm.id)" class="selection-indicator">
                    <el-icon class="check-icon"><Check /></el-icon>
                  </div>
                </div>
                <div class="card-content">
                  <div class="card-title">{{ algorithm.name }}</div>
                  <div class="card-subtitle">{{ algorithm.description }}</div>
                </div>
              </div>
            </div>

            <!-- 分页 -->
            <div class="panel-pagination">
              <el-pagination
                v-model:current-page="algorithmPage.currentPage"
                v-model:page-size="algorithmPage.pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="algorithmPage.total"
                layout="total, sizes, prev, pager, next, jumper"
                small
                @current-change="handleAlgorithmPageChange"
                @size-change="handleAlgorithmSizeChange"
              />
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import {ref, reactive, computed, onMounted, onUnmounted, nextTick, watch, shallowRef} from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Check } from '@element-plus/icons-vue'
import Hls from 'hls.js'
import DeviceTree from '@/components/DeviceTree.vue'
import CollapseToggle from '@/components/CollapseToggle.vue'
import AdvancedSearch from '@/components/AdvancedSearch.vue'
import PlayButton from '@/components/PlayButton.vue'

import { getDeviceById, getDeviceList, getDeviceTree, dispatchAlgorithmToDevices } from '@/api/device'
import { getTagTree } from '@/api/tagManagement'
import { WEBRTC_SERVER_BASE_URL } from '@/api/webrtc'
import { getStreamType, getYouTubeEmbedUrl } from '@/views/VideoAggregation/deviceUtils.js'
import { defaultDeviceTreeData } from '@/views/VideoAggregation/constants.js'
import { getAlgorithmPage } from '@/api/algorithmManagement'
import { CAMERA_RTC_SOCKET_URL, ensureOPlayer } from '@/utils/oplayer'

// 搜索表单
const searchForm = ref({
  deviceName: '',
  deviceId: '',
  tagName: '',
  dateRange: []
})

const loading = ref(false)
const selectedTreeNode = ref(null)

// 分页配置
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 选中的设备
const selectedDevices = ref([])

// 设备树折叠状态
const deviceTreeCollapsed = ref(false)

// 配置对话框显示状态
const showConfigDialog = ref(false)

// 视频播放对话框
const videoDialogVisible = ref(false)
const currentVideoDevice = ref(null)
const youtubeEmbedUrl = ref('')
const webrtcVideoPlayer = ref(null)
const hlsVideoPlayer = ref(null)
const nativeVideoPlayer = ref(null)
const hlsInstance = ref(null)
const webrtcPlayer = ref(null)
const cameraRtcContainer = ref(null)
const cameraRtcPlayer = shallowRef(null)
const webrtcPlaySeq = ref(0)

const getDeviceStreamUrl = (device) => {
  return (
    device?.originalRtspUrl ||
    device?.streamUrl ||
    device?.rtspUrl ||
    device?.rtsp ||
    device?.url ||
    ''
  )
}

const currentStreamUrl = computed(() => getDeviceStreamUrl(currentVideoDevice.value))
const currentStreamType = computed(() => getStreamType(currentStreamUrl.value))

// WebRTC (默认 RTSP 直连)
const WEBRTC_STREAMER_BASE = WEBRTC_SERVER_BASE_URL
const WEBRTC_SCRIPT_URLS = [
  `${WEBRTC_STREAMER_BASE}/libs/adapter.min.js`,
  `${WEBRTC_STREAMER_BASE}/webrtcstreamer.js`
]
let webrtcScriptLoader = null

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
  if (webrtcScriptLoader) return webrtcScriptLoader
  webrtcScriptLoader = Promise.all(WEBRTC_SCRIPT_URLS.map(loadScriptTag)).catch((error) => {
    webrtcScriptLoader = null
    throw error
  })
  return webrtcScriptLoader
}

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
    } catch (error) {
      console.error('Failed to stop WebRTC stream:', error)
    } finally {
      webrtcPlayer.value = null
    }
  }
}

const cleanupHLSStream = () => {
  if (hlsInstance.value) {
    try {
      hlsInstance.value.destroy()
    } catch (error) {
      console.warn('Failed to destroy HLS instance:', error)
    } finally {
      hlsInstance.value = null
    }
  }
  if (hlsVideoPlayer.value) {
    hlsVideoPlayer.value.pause?.()
    hlsVideoPlayer.value.removeAttribute?.('src')
    hlsVideoPlayer.value.load?.()
  }
}

const cleanupNativeStream = () => {
  if (!nativeVideoPlayer.value) return
  nativeVideoPlayer.value.pause?.()
  nativeVideoPlayer.value.removeAttribute?.('src')
  nativeVideoPlayer.value.load?.()
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

const stopPlayback = async () => {
  cleanupHLSStream()
  cleanupNativeStream()
  cleanupCameraRTCPlayer()
  await cleanupWebRTCStream()
}

const playWithWebRTC = async (rtspUrl) => {
  await ensureWebRtcStreamerScripts()
  if (!window.WebRtcStreamer) {
    throw new Error('WebRtcStreamer library not available')
  }

  await nextTick()
  const videoEl = webrtcVideoPlayer.value
  if (!videoEl) {
    throw new Error('Video element not ready')
  }

  await cleanupWebRTCStream()
  webrtcPlayer.value = new window.WebRtcStreamer(videoEl.id, WEBRTC_STREAMER_BASE)
  if (typeof webrtcPlayer.value.connect !== 'function') {
    throw new Error('WebRtcStreamer.connect is not available')
  }
  webrtcPlayer.value.connect(rtspUrl, '', 'rtptransport=tcp&timeout=60')
}

const playWithCameraRTC = async (device) => {
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

  cleanupCameraRTCPlayer()
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
    name: device?.deviceName || device?.name || ''
  })
}

const initHlsPlayer = async (streamUrl) => {
  await nextTick()
  const videoEl = hlsVideoPlayer.value
  if (!videoEl) return

  cleanupHLSStream()

  if (Hls.isSupported()) {
    const hls = new Hls({
      lowLatencyMode: true,
      enableWorker: true
    })
    hls.loadSource(streamUrl)
    hls.attachMedia(videoEl)
    hlsInstance.value = hls
    videoEl.play?.().catch(() => {})
    return
  }

  if (videoEl.canPlayType('application/vnd.apple.mpegurl')) {
    videoEl.src = streamUrl
    videoEl.play?.().catch(() => {})
    return
  }

  throw new Error('HLS not supported in this browser')
}

const playNative = async () => {
  await nextTick()
  nativeVideoPlayer.value?.play?.().catch(() => {})
}

const handlePlay = async (row) => {
  const deviceForPlay = { ...(row || {}) }
  const streamUrl = getDeviceStreamUrl(deviceForPlay)
  if (!streamUrl || !String(streamUrl).trim()) {
    ElMessage.warning('未配置视频流地址')
    return
  }

  await stopPlayback()

  currentVideoDevice.value = deviceForPlay
  youtubeEmbedUrl.value = ''
  videoDialogVisible.value = true

  const seq = ++webrtcPlaySeq.value
  await nextTick()
  if (seq !== webrtcPlaySeq.value) return

  try {
    const type = getStreamType(streamUrl)
    if (type === 'youtube') {
      const embedUrl = getYouTubeEmbedUrl(streamUrl)
      if (!embedUrl) {
        throw new Error('Invalid YouTube URL')
      }
      youtubeEmbedUrl.value = embedUrl
      return
    }

    if (type === 'cameraRTC') {
      await playWithCameraRTC(deviceForPlay)
      return
    }

    if (type === 'rtsp') {
      await playWithWebRTC(streamUrl)
      return
    }

    if (type === 'hls') {
      await initHlsPlayer(streamUrl)
      return
    }

    await playNative()
  } catch (error) {
    console.error('Play failed:', error)
    ElMessage.error(`播放失败: ${error.message || error}`)
  }
}

const handleVideoClose = async () => {
  await stopPlayback()
  youtubeEmbedUrl.value = ''
  currentVideoDevice.value = null
}

// 当前配置的设备
const currentDevice = ref(null)

// 可选的算法列表
const selectedAlgorithms = ref([])

const showAlgorithmPanel = ref(false)

const activeCategory = ref('all')

const placeholderImage200x150 = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjE1MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjVmNWY1Ii8+PHBhdGggZD0iTTYwLjUgNjBjMCAxMi40MTggMTAuMDgyIDIyLjUgMjIuNSAyMi41czIyLjUtMTAuMDgyIDIyLjUtMjIuNUM5NS40MTggMzcuNSA4MyAzNy41IDYwLjUgNDcuNTgyIDYwLjUgNjB6bTU4IDYwTDEwMCAxMDBsLTIwLTIwLTMwIDMweiIgZmlsbD0iI2RkZCIvPjx0ZXh0IHg9IjEwMCIgeT0iMTMwIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTQiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiPjIwMCB4IDE1MDwvdGV4dD48L3N2Zz4='

const algorithmList = ref([])
const algorithmLoading = ref(false)

const algorithmPage = reactive({
  currentPage: 1,
  pageSize: 12,
  total: 0
})

const algorithmCategories = computed(() => {
  const categories = [{ key: 'all', label: '\u5168\u90e8' }]
  const seen = new Map()
  algorithmList.value.forEach((algorithm) => {
    const key = algorithm.category || algorithm.categoryName
    if (!key || seen.has(key)) return
    const label = algorithm.categoryName || algorithm.category || key
    seen.set(key, label)
    categories.push({ key, label })
  })
  return categories
})

const algorithmById = computed(() => {
  const map = new Map()
  algorithmList.value.forEach((algorithm) => {
    if (algorithm && algorithm.id !== undefined && algorithm.id !== null) {
      map.set(String(algorithm.id), algorithm)
    }
  })
  return map
})

const algorithmByName = computed(() => {
  const map = new Map()
  algorithmList.value.forEach((algorithm) => {
    if (algorithm && algorithm.name) {
      map.set(String(algorithm.name).trim(), algorithm)
    }
  })
  return map
})

const filteredAlgorithmSource = computed(() => {
  let filtered = algorithmList.value
  if (activeCategory.value !== 'all') {
    filtered = filtered.filter(alg => alg.category === activeCategory.value)
  }
  return filtered
})

const filteredAlgorithmList = computed(() => {
  const start = (algorithmPage.currentPage - 1) * algorithmPage.pageSize
  const end = start + algorithmPage.pageSize
  return filteredAlgorithmSource.value.slice(start, end)
})

// 设备树/设备列表对接（参考 DeviceManagement.vue）
const deviceTreeData = ref([...defaultDeviceTreeData])
const deviceList = ref([])
const tagNameMap = ref(new Map())

const loadTagNameMap = async () => {
  try {
    const response = await getTagTree()
    if (response?.code === 200 && response.data) {
      const tagMap = new Map()
      const traverse = (nodes) => {
        if (!Array.isArray(nodes)) return
        nodes.forEach((node) => {
          if (node.id && node.tagName) {
            tagMap.set(node.id, node.tagName)
          }
          if (Array.isArray(node.children)) {
            traverse(node.children)
          }
        })
      }
      traverse(response.data)
      tagNameMap.value = tagMap
    }
  } catch (error) {
    console.warn('加载标签树失败:', error)
  }
}
const normalizeAlgorithm = (algorithm) => {
  const normalized = { ...(algorithm || {}) }
  normalized.id = normalized.id ?? normalized.algorithmId
  normalized.name = normalized.name ?? normalized.algorithmName ?? ''
  normalized.category = normalized.category ?? normalized.categoryName ?? ''
  normalized.categoryName = normalized.categoryName ?? normalized.category ?? ''
  normalized.description = normalized.description ?? normalized.remark ?? ''
  normalized.image = normalized.imageUrl || normalized.image || placeholderImage200x150
  return normalized
}

const loadAlgorithmList = async () => {
  if (algorithmLoading.value) return
  try {
    algorithmLoading.value = true
    const response = await getAlgorithmPage({
      current: 1,
      size: 1000
    })
    if (response?.code === 200) {
      const records = response.data?.records || []
      algorithmList.value = records.map(normalizeAlgorithm)
    } else {
      algorithmList.value = []
      ElMessage.error(response?.message || 'Failed to load algorithms')
    }
  } catch (error) {
    console.error('Failed to load algorithms:', error)
    algorithmList.value = []
    ElMessage.error('Failed to load algorithms')
  } finally {
    algorithmLoading.value = false
  }
}

const ensureAlgorithmList = async () => {
  if (algorithmList.value.length > 0 || algorithmLoading.value) return
  await loadAlgorithmList()
}


const normalizeDeviceRow = (device, deviceDetail) => {
  const merged = { ...(device || {}), ...(deviceDetail || {}) }

  let tags = []
  if (Array.isArray(merged.selectedTags) && merged.selectedTags.length > 0) {
    tags = merged.selectedTags.map((tagId) => tagNameMap.value.get(tagId) || `标签_${tagId}`)
  } else if (Array.isArray(merged.tags) && merged.tags.length > 0) {
    tags = merged.tags
  }

  let algorithms = merged.algorithms ?? merged.aiAlgorithms ?? merged.aiAlgorithm ?? ''
  let algorithmIds = merged.algorithmIds ?? merged.algorithmId ?? merged.aiAlgorithmIds ?? merged.aiAlgorithmId ?? ''
  if (Array.isArray(algorithms)) {
    const algorithmNames = algorithms
      .map((item) => {
        if (!item) return ''
        if (typeof item === 'string') return item
        if (typeof item === 'number') return String(item)
        return item.name || item.algorithmName || item.label || item.id || ''
      })
      .filter(Boolean)
    const derivedIds = algorithms
      .map((item) => {
        if (!item) return ''
        if (typeof item === 'number') return String(item)
        if (typeof item === 'string') return item
        return item.id || item.algorithmId || item.value || item.name || item.algorithmName || ''
      })
      .filter(Boolean)
    if ((!algorithmIds || (Array.isArray(algorithmIds) && algorithmIds.length == 0)) && derivedIds.length > 0) {
      algorithmIds = derivedIds
    }
    algorithms = algorithmNames.join('\u3001')
  }

  return {
    ...merged,
    deviceId: merged.deviceId ?? merged.id,
    deviceLocation: merged.deviceLocation || merged.location || merged.address || merged.installLocation || '',
    tags,
    displayTag: tags.length > 0 ? tags[0] : merged.displayTag,
    algorithmIds,
    algorithms
  }
}

const loadDeviceList = async () => {
  loading.value = true
  try {
    if (tagNameMap.value.size === 0) {
      await loadTagNameMap()
    }

    const params = {
      page: pagination.currentPage,
      size: pagination.pageSize,
      deviceName: searchForm.value.deviceName,
      deviceId: searchForm.value.deviceId,
      tagName: searchForm.value.tagName,
      dateRange: searchForm.value.dateRange
    }

    const response = await getDeviceList(params)
    const devices = response?.data?.records || []

    const deviceListWithTags = await Promise.all(
      devices.map(async (device) => {
        try {
          const detailResponse = await getDeviceById(device.id)
          if (detailResponse?.code === 200 && detailResponse.data) {
            return normalizeDeviceRow(device, detailResponse.data)
          }
        } catch (error) {
          console.warn(`加载设备详情失败: ${device?.id}`, error)
        }
        return normalizeDeviceRow(device, null)
      })
    )

    deviceList.value = deviceListWithTags
    pagination.total = response?.data?.total ?? deviceListWithTags.length
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
    if (response?.data) {
      deviceTreeData.value = response.data
    }
  } catch (error) {
    console.error('加载设备树失败:', error)
  }
}

const filteredDeviceList = computed(() => {
  let filtered = deviceList.value
  const node = selectedTreeNode.value
  if (!node) return filtered

  if (node.type === 'tag' && node.label) {
    const label = String(node.label).split(' (')[0]
    filtered = filtered.filter((d) => d.displayTag === label || (Array.isArray(d.tags) && d.tags.includes(label)))
  } else if (node.type === 'device_type' && node.label) {
    const deviceTypeLabel = String(node.label).split(' (')[0]
    filtered = filtered.filter((d) => d.deviceType === deviceTypeLabel)
  } else if (node.type === 'device') {
    const deviceId = node.deviceId ?? node.id
    filtered = filtered.filter((d) => d.id === deviceId || d.deviceId === deviceId)
  }

  return filtered
})

const tableData = computed(() => filteredDeviceList.value)

const handleAdvancedSearch = (searchData) => {
  console.log('高级搜索:', searchData)
  
  // 更新搜索表单
  if (searchData.keyword) {
    searchForm.value.deviceName = searchData.keyword
  }
  if (searchData.deviceName) {
    searchForm.value.deviceName = searchData.deviceName
  }
  if (searchData.deviceId !== undefined) {
    searchForm.value.deviceId = searchData.deviceId || ''
  }
  if (searchData.tagName !== undefined) {
    searchForm.value.tagName = searchData.tagName || ''
  }
  if (searchData.selectedTags && searchData.selectedTags.length > 0) {
    // 可以根据需要处理标签搜索
    console.log('标签搜索:', searchData.selectedTags)
  }
  if (searchData.dateRange && searchData.dateRange.length === 2) {
    searchForm.value.dateRange = searchData.dateRange
  }
  
  selectedTreeNode.value = null
  pagination.currentPage = 1
  loadDeviceList()
}

const handleAdvancedSearchReset = () => {
  console.log('重置高级搜索')
  searchForm.value = {
    deviceName: '',
    deviceId: '',
    tagName: '',
    dateRange: []
  }
  selectedTreeNode.value = null
  pagination.currentPage = 1
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

const handleTreeNodeClick = async (data) => {
  selectedTreeNode.value = data
  pagination.currentPage = 1

  // 标签节点：使用 tagName 走接口过滤
  if (data?.type === 'tag' && data.label) {
    searchForm.value.tagName = String(data.label).split(' (')[0]
    await loadDeviceList()
    return
  }

  // 设备节点：直接加载单个设备
  if (data?.type === 'device') {
    const deviceId = data.deviceId ?? data.id
    if (!deviceId) return

    loading.value = true
    try {
      if (tagNameMap.value.size === 0) {
        await loadTagNameMap()
      }
      const detailResponse = await getDeviceById(deviceId)
      if (detailResponse?.code === 200 && detailResponse.data) {
        deviceList.value = [normalizeDeviceRow(detailResponse.data, detailResponse.data)]
        pagination.total = 1
      } else {
        deviceList.value = []
        pagination.total = 0
      }
    } catch (error) {
      console.error('加载设备失败:', error)
    } finally {
      loading.value = false
    }
    return
  }
  console.log('点击树节点:', data)
}

const handleDeviceTreeSearch = (keyword) => {
  console.log('设备树搜索:', keyword)
}

const toggleDeviceTree = () => {
  deviceTreeCollapsed.value = !deviceTreeCollapsed.value
}

const handleSelectionChange = (selection) => {
  selectedDevices.value = selection
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  if (selectedTreeNode.value?.type !== 'device') {
    loadDeviceList()
  }
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  if (selectedTreeNode.value?.type !== 'device') {
    loadDeviceList()
  }
}

const parseAlgorithmTokens = (value) => {
  if (!value) return []
  if (Array.isArray(value)) {
    return value.flatMap(parseAlgorithmTokens)
  }
  if (typeof value === 'object') {
    const candidate = value.id ?? value.algorithmId ?? value.name ?? value.algorithmName
    return candidate !== undefined && candidate !== null ? [String(candidate)] : []
  }
  if (typeof value === 'string') {
    return value
      .split(/a\?\?|[\u3001\uFF0C,;|\s]+/g)
      .map(item => item.trim())
      .filter(Boolean)
  }
  return [String(value)]
}

const resolveSelectedAlgorithms = (rawAlgorithms) => {
  const tokens = parseAlgorithmTokens(rawAlgorithms)
  const selected = []
  const used = new Set()
  tokens.forEach((token) => {
    const key = String(token).trim()
    if (!key) return
    const match = algorithmById.value.get(key) || algorithmByName.value.get(key)
    if (!match) return
    const uniqueKey = String(match.id ?? match.name)
    if (used.has(uniqueKey)) return
    used.add(uniqueKey)
    selected.push(match)
  })
  return selected
}

const getAlgorithmSelectionSource = (row) => {
  if (!row) return []
  const idCandidates = [
    row.algorithmIds,
    row.algorithmId,
    row.aiAlgorithmIds,
    row.aiAlgorithmId
  ]
  for (const candidate of idCandidates) {
    if (Array.isArray(candidate)) {
      if (candidate.length > 0) return candidate
      continue
    }
    if (candidate !== undefined && candidate !== null && String(candidate).trim() !== '') {
      return candidate
    }
  }
  const nameCandidates = [
    row.aiAlgorithms,
    row.aiAlgorithm,
    row.algorithms
  ]
  for (const candidate of nameCandidates) {
    if (Array.isArray(candidate)) {
      if (candidate.length > 0) return candidate
      continue
    }
    if (candidate !== undefined && candidate !== null && String(candidate).trim() !== '') {
      return candidate
    }
  }
  return []
}


const isAlgorithmSelected = (algorithmId) => {
  if (algorithmId === undefined || algorithmId === null) return false
  return selectedAlgorithms.value.some(item => String(item.id) === String(algorithmId))
}

const configureAlgorithm = async (row) => {
  currentDevice.value = row
  await ensureAlgorithmList()

  selectedAlgorithms.value = resolveSelectedAlgorithms(getAlgorithmSelectionSource(row))
  showConfigDialog.value = true
}

const removeAlgorithm = (algorithmId) => {
  selectedAlgorithms.value = selectedAlgorithms.value.filter(
    item => String(item.id) !== String(algorithmId)
  )
}

const toggleAlgorithmSelection = (algorithm) => {
  if (!algorithm || algorithm.id === undefined || algorithm.id === null) return
  const index = selectedAlgorithms.value.findIndex(
    item => String(item.id) === String(algorithm.id)
  )
  if (index > -1) {
    selectedAlgorithms.value.splice(index, 1)
  } else {
    selectedAlgorithms.value.push(algorithm)
  }
}

const closeAlgorithmPanel = () => {
  showAlgorithmPanel.value = false
}

const handleAlgorithmPageChange = (page) => {
  algorithmPage.currentPage = page
}

const handleAlgorithmSizeChange = (size) => {
  algorithmPage.pageSize = size
  algorithmPage.currentPage = 1
}

const resetConfigForm = () => {
  currentDevice.value = null
  selectedAlgorithms.value = []
  showAlgorithmPanel.value = false
}



const saveConfiguration = async () => {
  if (!currentDevice.value) {
    ElMessage.warning('\u8bf7\u9009\u62e9\u8bbe\u5907')
    return
  }

  const deviceId = currentDevice.value.id
  if (!deviceId) {
    ElMessage.warning('\u8bbe\u5907ID\u7f3a\u5931')
    return
  }

  const algorithmIds = Array.from(
    new Set(selectedAlgorithms.value.map(item => item.id).filter(id => id !== undefined && id !== null))
  )
  if (algorithmIds.length === 0) {
    ElMessage.warning('\u8bf7\u9009\u62e9\u7b97\u6cd5')
    return
  }

  try {
    for (const algorithmId of algorithmIds) {
      const response = await dispatchAlgorithmToDevices(algorithmId, String(deviceId))
      if (response?.code !== 200) {
        ElMessage.error(response?.message || '\u7b97\u6cd5\u914d\u7f6e\u5931\u8d25')
        return
      }
    }

    const algorithmNames = selectedAlgorithms.value
      .map(item => item.name)
      .filter(Boolean)
    const displayAlgorithms = algorithmNames.join('\u3001')

    currentDevice.value.algorithms = displayAlgorithms
    currentDevice.value.algorithmIds = algorithmIds
    const matchRow = deviceList.value.find(
      item => String(item.deviceId ?? item.id) === String(deviceId)
    )
    if (matchRow) {
      matchRow.algorithms = displayAlgorithms
      matchRow.algorithmIds = algorithmIds
    }

    ElMessage.success('\u914d\u7f6e\u6210\u529f')
    showConfigDialog.value = false
    showAlgorithmPanel.value = false
  } catch (error) {
    console.error('Failed to configure algorithms:', error)
    ElMessage.error('\u7b97\u6cd5\u914d\u7f6e\u5931\u8d25')
  }
}

onMounted(() => {
  loadDeviceTree()
  loadDeviceList()
  loadAlgorithmList()
})

watch([algorithmList, activeCategory, () => algorithmPage.pageSize], () => {
  const total = filteredAlgorithmSource.value.length
  algorithmPage.total = total
  const maxPage = Math.max(1, Math.ceil(total / algorithmPage.pageSize))
  if (algorithmPage.currentPage > maxPage) {
    algorithmPage.currentPage = 1
  }
}, { immediate: true })

watch(activeCategory, () => {
  algorithmPage.currentPage = 1
})

watch(showAlgorithmPanel, (visible) => {
  if (visible) {
    ensureAlgorithmList()
  }
})
watch(videoDialogVisible, (visible) => {
  if (!visible) {
    stopPlayback()
    youtubeEmbedUrl.value = ''
    currentVideoDevice.value = null
  }
})

onUnmounted(() => {
  stopPlayback()
})
</script>

<style scoped>
.algorithm-arrangement {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
  background-color: #f5f7fa;
  padding: 20px;
  overflow: hidden;
}

/* 查询栏 */
.query-bar {
  background: #F0F2F5;
  border-radius: 8px 8px 0 0;
  padding: 20px;
}

.query-content {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.search-item {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.search-item:first-child {
  min-width: 240px;
}

.search-item:nth-child(2) {
  min-width: 200px;
}

.search-buttons {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  background: white;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  gap: 0;
  overflow: hidden;
}

/* 左侧设备树 */
.device-tree-container {
  width: 280px;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  overflow: hidden;
  flex-shrink: 0;
  border-right: 1px solid #f0f0f0;
}

/* 设备树折叠状态 */
.device-tree-container.collapsed {
  width: 0;
  min-width: 0;
}

/* 右侧表格区域 */
.table-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
  z-index: 1;
}

.breadcrumb-item {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.expand-device-tree-btn {
  margin-right: 8px;
  cursor: pointer !important;
  color: #606266;
  transition: color 0.3s;
  pointer-events: auto !important;
  z-index: 999;
  position: relative;
}

.expand-device-tree-btn:hover {
  color: #409eff;
}

/* 确保CollapseToggle内部元素可点击 */
.expand-device-tree-btn :deep(.collapse-toggle) {
  pointer-events: auto !important;
  cursor: pointer !important;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

/* 配置AI算法按钮样式 */
.config-ai-algorithm-btn {
  width: 124px !important;
  height: 36px !important;
  background: white !important;
  border: 1px solid #d9d9d9 !important;
  border-radius: 18px !important;
  padding: 0 16px !important;
  font-size: 14px;
  font-weight: 500;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
  color: #333333 !important;
}

.config-ai-algorithm-btn:hover {
  background: rgba(26, 83, 255, 0.05) !important;
  border-color: #1A53FF !important;
  color: #1A53FF !important;
}

.config-ai-algorithm-btn:focus {
  background: white !important;
  border-color: #d9d9d9 !important;
  color: #333333 !important;
  outline: none !important;
  box-shadow: none !important;
}

.config-ai-algorithm-btn:active {
  background: rgba(26, 83, 255, 0.1) !important;
  border-color: #1A53FF !important;
  color: #1A53FF !important;
}

.config-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

/* 表格内容 */
.table-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  .action-buttons {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
    align-items: center;

    .config-button {
      flex-shrink: 0;
    }
  }
}

 /* 分页容器样式 */
.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px 20px 20px;
  border-top: 1px solid #f0f0f0;
}

/* 表格样式调整 */
:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table td) {
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-table .el-table__row:hover) {
  background-color: #f5f7fa;
}

/* 表格操作列按钮样式 */
:deep(.el-table .el-button--primary.is-text) {
  color: #1A53FF !important;
  background: transparent !important;
  border: none !important;
  padding: 2px 8px !important;
}

:deep(.el-table .el-button--primary.is-text:hover) {
  color: #3d70ff !important;
  background: transparent !important;
  border: none !important;
  text-decoration: underline !important;
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

:deep(.el-pagination .el-pager li.active) {
  background-color: #1A53FF;
  border-color: #1A53FF;
}

:deep(.el-pagination .el-pager li:hover) {
  color: #1A53FF;
}

:deep(.el-pagination .btn-next:hover, .el-pagination .btn-prev:hover) {
  color: #1A53FF;
}

/* 标签样式 */
.device-tag {
  margin-right: 4px;
}

.el-tag + .el-tag {
  margin-left: 4px;
}

/* 配置对话框样式 */
.config-dialog-content {
  padding: 20px 0;
}

.config-item {
  margin-bottom: 24px;
}

.config-item:last-child {
  margin-bottom: 0;
}

.config-label {
  display: block;
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
  font-weight: 500;
}

.device-name {
  font-size: 16px;
  color: #303133;
  font-weight: 500;
}

.algorithm-selection {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.selected-algorithms {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 32px;
}

.selected-tag {
  margin: 0;
}

.select-algorithm-btn {
  align-self: flex-start;
  border-style: dashed;
  color: #1A53FF;
  border-color: #1A53FF;
}

.select-algorithm-btn:hover {
  background-color: rgba(26, 83, 255, 0.05);
}

.dialog-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 20px 0 0;
}

.dialog-footer .el-button {
  min-width: 80px;
  height: 36px;
}

/* 算法选择面板样式 */
.algorithm-panel-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 3000;
  display: flex;
  justify-content: flex-end;
}

.algorithm-panel {
  width: 800px;
  height: 100vh;
  background: white;
  display: flex;
  flex-direction: column;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.1);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e4e7ed;
}

.panel-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.close-btn {
  color: #909399;
  font-size: 20px;
}

.close-btn:hover {
  color: #606266;
}

.panel-content {
  display: flex;
  flex-direction: column;
  padding: 20px 24px;
  overflow: hidden;
}

.category-tabs {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
  border-bottom: 1px solid #e4e7ed;
  padding-bottom: 16px;
}

.category-tab {
  padding: 8px 0;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.category-tab.active {
  color: #1A53FF;
  border-bottom-color: #1A53FF;
  font-weight: 500;
}

.category-tab:hover:not(.active) {
  color: #1A53FF;
}

.algorithm-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  overflow-y: auto;
  padding-right: 8px;
}

.algorithm-card {
  cursor: pointer;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s;
  background: white;
}

.algorithm-card:hover {
  border-color: #1A53FF;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(26, 83, 255, 0.15);
}

.algorithm-card.selected {
  border-color: #1A53FF;
  box-shadow: 0 2px 8px rgba(26, 83, 255, 0.2);
}

.card-image {
  position: relative;
  width: 100%;
  height: 120px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.selection-indicator {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  background: #1A53FF;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.check-icon {
  font-size: 14px;
  color: white;
}

.card-content {
  padding: 12px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.card-subtitle {
  font-size: 12px;
  color: #909399;
}

.panel-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

.video-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.video-element,
.video-iframe {
  width: 100%;
  height: 620px;
  background: #000;
  border-radius: 8px;
}

.video-empty {
  height: 620px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0b0b0b;
  color: #c0c4cc;
  border-radius: 8px;
}

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
</style> 
