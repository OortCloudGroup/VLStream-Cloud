<template>
  <div class="video-square">
    <div v-show="showPreview" class="preview-section">
      <!-- 布局切换 -->
      <div class="layout-row">
        <div class="layout-buttons">
          <div
            v-for="mode in layoutModes"
            :key="mode.value"
            class="layout-icon"
            @click="changeLayout(mode.value)"
          >
            <img :src="activeLayout === mode.value ? mode.activeImg : mode.img" alt="" class="layout-img" />
          </div>
        </div>

        <van-popover v-model:show="showModeMenu" trigger="click" placement="bottom-end">
          <template #reference>
            <img :src="moreImg" alt="" class="more-icon" />
          </template>
          <div class="mode-menu">
            <div
              v-for="item in modeMenu"
              :key="item.value"
              class="mode-menu-item"
              @click="onSelectMenu(item.value)"
            >
              <img :src="item.img" alt="" class="mode-menu-icon" />
              <span>{{ item.label }}</span>
            </div>
          </div>
        </van-popover>
      </div>

      <!-- 预览区域：根据布局和设备列表分屏播放 WebRTC 视频 -->
      <div
        ref="previewGridRef"
        class="preview-grid"
        :class="[`cols-${previewCols}`, `layout-${activeLayout}`, { 'fullscreen-mode': isFullscreen }]"
      >
        <div
          v-for="slot in previewSlots"
          :key="slot.id"
          class="preview-cell"
        >
          <!-- 有设备：播放 WebRTC 视频 -->
          <div v-if="slot.device" class="preview-video-wrapper">
            <video
              :id="`vs-webrtc-${slot.device.id}`"
              class="preview-video"
              autoplay
              muted
              playsinline
              controls
            />
            <!-- 自定义全屏 -->
            <div class="cell-fullscreen-btn" @click.stop="enterCellFullscreen(slot.device.id)">
              <van-icon name="expand-o" />
            </div>
          </div>
          <!-- 无设备：占位 -->
          <div v-else class="preview-empty">
            无信号
          </div>
        </div>
      </div>
    </div>

    <!-- 中间小滑块条（可拖动控制预览区域显示/隐藏） -->
    <div
      class="slider-bar"
      :class="{ dragging: isDragging }"
      @touchstart.prevent="onSliderTouchStart"
      @touchmove.prevent="onSliderTouchMove"
      @touchend.prevent="onSliderTouchEnd"
      @mousedown.prevent="onSliderMouseDown"
    >
      <div class="slider-thumb" />
    </div>

    <!-- 设备卡片列表 -->
    <div class="device-list">
      <div
        v-for="item in devices"
        :key="item.id"
        class="device-card"
        @click="openDetail(item)"
      >
        <div class="card-header">
          <div class="name">
            {{ item.deviceName }}
          </div>
          <span class="privacy-tag">
            {{ item.privacy }}
          </span>
        </div>

        <div class="info-row" style="width: 90%">
          <img src="@/assets/img/VLStreamCloud/spgc/sbId.png" alt="" />
          <div class="label">
            设备ID
          </div>
          <div class="value">
            {{ item.deviceId }}
          </div>
        </div>
        <div class="info-row">
          <img src="@/assets/img/VLStreamCloud/spgc/sbwz.png" alt="" />
          <div class="label">
            设备位置
          </div>
          <div class="value">
            {{ item.address }}
          </div>
        </div>

        <div class="card-footer">
          <div class="chips">
            <div class="chip" :class="{ offline: item.status != '1' }">
              {{ item.status == '1' ? '在线' : '离线' }}
            </div>
            <div v-if="item.deviceType" class="chip">
              {{ item.deviceType }}
            </div>
            <div v-if="item.heightPosition" class="chip">
              {{ item.heightPosition }}
            </div>
          </div>
        </div>
        <div class="play-btn">
          <img v-if="item.status == '1'" src="@/assets/img/VLStreamCloud/spgc/playing.png" alt="" />
          <img v-else src="@/assets/img/VLStreamCloud/spgc/no_playing.png" alt="" />
        </div>
      </div>
    </div>

    <oort-popup v-model="showDetail" position="right" style="width: 100%;height: 100%;" teleport="body">
      <VideoViewDetail
        v-if="currentDevice"
        :device="currentDevice"
        @close="showDetail = false"
      />
    </oort-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useUserStore } from '@/store/modules/useraPaas'
import { getDeviceList } from '@/api/VLStreamCloud/device'
import { WEBRTC_SERVER_BASE_URL } from '@/api/VLStreamCloud/webrtc'
import OortPopup from '@/components/popup/oort_popup.vue'
import VideoViewDetail from './VideoViewDetail.vue'
import p1 from '@/assets/img/VLStreamCloud/spgc/p1.png'
import p4 from '@/assets/img/VLStreamCloud/spgc/p4.png'
import p6 from '@/assets/img/VLStreamCloud/spgc/p6.png'
import p8 from '@/assets/img/VLStreamCloud/spgc/p8.png'
import p9 from '@/assets/img/VLStreamCloud/spgc/p9.png'
import p16 from '@/assets/img/VLStreamCloud/spgc/p16.png'
import p1_c from '@/assets/img/VLStreamCloud/spgc/p1_c.png'
import p4_c from '@/assets/img/VLStreamCloud/spgc/p4_c.png'
import p6_c from '@/assets/img/VLStreamCloud/spgc/p6_c.png'
import p8_c from '@/assets/img/VLStreamCloud/spgc/p8_c.png'
import p9_c from '@/assets/img/VLStreamCloud/spgc/p9_c.png'
import p16_c from '@/assets/img/VLStreamCloud/spgc/p16_c.png'
import moreImg from '@/assets/img/VLStreamCloud/spgc/more.png'
import qp from '@/assets/img/VLStreamCloud/spgc/qp.png'
import spms from '@/assets/img/VLStreamCloud/spgc/spms.png'
import dtms from '@/assets/img/VLStreamCloud/spgc/dtms.png'

const showPreview = ref(true)

// 拖拽相关状态
const isDragging = ref(false)
const startY = ref(0)
const dragThreshold = 20 // 拖动阈值，超过这个距离才切换状态

// 触摸事件处理（上拉隐藏，下拉显示）
const onSliderTouchStart = (e: TouchEvent) => {
  const touch = e.touches[0]
  isDragging.value = true
  startY.value = touch.clientY
}

const onSliderTouchMove = (e: TouchEvent) => {
  if (!isDragging.value) return
  const touch = e.touches[0]
  const deltaY = touch.clientY - startY.value

  // 上拉（deltaY < 0）：隐藏预览区域
  // 下拉（deltaY > 0）：显示预览区域
  if (Math.abs(deltaY) > dragThreshold) {
    if (deltaY < 0 && showPreview.value) {
      showPreview.value = false
      isDragging.value = false
    } else if (deltaY > 0 && !showPreview.value) {
      showPreview.value = true
      isDragging.value = false
    }
  }
}

const onSliderTouchEnd = () => {
  isDragging.value = false
}

// 鼠标事件处理（PC端）
const onSliderMouseDown = (e: MouseEvent) => {
  isDragging.value = true
  startY.value = e.clientY
}

const handleMouseMove = (e: MouseEvent) => {
  if (!isDragging.value) return
  const deltaY = e.clientY - startY.value

  // 上拉（deltaY < 0）：隐藏预览区域
  // 下拉（deltaY > 0）：显示预览区域
  if (Math.abs(deltaY) > dragThreshold) {
    if (deltaY < 0 && showPreview.value) {
      showPreview.value = false
      isDragging.value = false
    } else if (deltaY > 0 && !showPreview.value) {
      showPreview.value = true
      isDragging.value = false
    }
  }
}

const handleMouseUp = () => {
  isDragging.value = false
}

const layoutModes = [
  { value: 'p1', img: p1, activeImg: p1_c },
  { value: 'p4', img: p4, activeImg: p4_c },
  { value: 'p6', img: p6, activeImg: p6_c },
  { value: 'p8', img: p8, activeImg: p8_c },
  { value: 'p9', img: p9, activeImg: p9_c },
  { value: 'p16', img: p16, activeImg: p16_c }
]

const activeLayout = ref('p4')

const showModeMenu = ref(false)

const modeMenu = [
  { value: 'full', label: '全屏', img: qp },
  { value: 'video', label: '视频模式', img: spms },
  { value: 'map', label: '地图模式', img: dtms }
]

const previewGridRef = ref<HTMLElement | null>(null)
const isFullscreen = ref(false)

// 全屏相关函数
const enterFullscreen = async() => {
  const element = previewGridRef.value
  if (!element) return

  try {
    if (element.requestFullscreen) {
      await element.requestFullscreen()
    } else if ((element as any).webkitRequestFullscreen) {
      // Safari
      await (element as any).webkitRequestFullscreen()
    } else if ((element as any).mozRequestFullScreen) {
      // Firefox
      await (element as any).mozRequestFullScreen()
    } else if ((element as any).msRequestFullscreen) {
      // IE/Edge
      await (element as any).msRequestFullscreen()
    }
    isFullscreen.value = true
  } catch (error) {
    // 全屏失败时忽略错误
  }
}

const exitFullscreen = async() => {
  try {
    if (document.exitFullscreen) {
      await document.exitFullscreen()
    } else if ((document as any).webkitExitFullscreen) {
      await (document as any).webkitExitFullscreen()
    } else if ((document as any).mozCancelFullScreen) {
      await (document as any).mozCancelFullScreen()
    } else if ((document as any).msExitFullscreen) {
      await (document as any).msExitFullscreen()
    }
    isFullscreen.value = false
  } catch (error) {
    // 退出全屏失败时忽略错误
  }
}

const toggleFullscreen = async() => {
  if (isFullscreen.value) {
    await exitFullscreen()
  } else {
    await enterFullscreen()
  }
}

// 单个分屏视频全屏（移动端优先走 video 的 webkitEnterFullscreen）
const enterCellFullscreen = async(deviceId: string | number) => {
  const el = document.getElementById(`vs-webrtc-${deviceId}`) as any
  if (!el) return
  try {
    // iOS Safari（最常用）
    if (typeof el.webkitEnterFullscreen === 'function') {
      el.webkitEnterFullscreen()
      return
    }
    // 标准 Fullscreen API
    if (typeof el.requestFullscreen === 'function') {
      await el.requestFullscreen()
      return
    }
    // 兼容旧 webkit
    if (typeof el.webkitRequestFullscreen === 'function') {
      await el.webkitRequestFullscreen()
    }
  } catch (e) {
    // 忽略全屏失败
  }
}

// 监听全屏状态变化
const handleFullscreenChange = () => {
  const isCurrentlyFullscreen = !!(
    document.fullscreenElement ||
    (document as any).webkitFullscreenElement ||
    (document as any).mozFullScreenElement ||
    (document as any).msFullscreenElement
  )
  isFullscreen.value = isCurrentlyFullscreen
}

const onSelectMenu = async(value: string) => {
  showModeMenu.value = false

  if (value === 'full') {
    // 全屏模式
    await toggleFullscreen()
  } else if (value === 'video') {
    // 视频模式
  } else if (value === 'map') {
    // 地图模式
  }
}

const layoutCountMap: Record<string, number> = {
  p1: 1,
  p4: 4,
  p6: 6,
  p8: 8,
  p9: 9,
  p16: 16
}

const layoutColsMap: Record<string, number> = {
  p1: 1,
  p4: 2,
  p6: 3,
  p8: 4,
  p9: 3,
  p16: 4
}

const changeLayout = async(value: string) => {
  activeLayout.value = value
  // 切换布局后，重新刷新当前布局的播放流
  await refreshAllPreviewStreams()
}

// 预览窗口槽位
const previewSlots = computed(() => {
  const count = layoutCountMap[activeLayout.value] || 4
  return Array.from({ length: count }, (_v, index) => ({
    id: index + 1,
    device: devices.value[index] || null
  }))
})

const previewCols = computed(() => layoutColsMap[activeLayout.value] || 2)

const store = useUserStore()

const devices = ref<any[]>([])

// ========= WebRTC 播放相关（移动端多分屏） =========
const WEBRTC_STREAMER_BASE = WEBRTC_SERVER_BASE_URL
const WEBRTC_SCRIPT_URLS = [
  `${WEBRTC_STREAMER_BASE}/libs/adapter.min.js`,
  `${WEBRTC_STREAMER_BASE}/webrtcstreamer.js`
]

// 记录每个设备的 WebRtcStreamer 实例，方便销毁
const webrtcPlayers: Record<string, any> = {}
let webrtcScriptLoader: Promise<unknown> | null = null

const loadScriptTag = (src: string) => {
  return new Promise((resolve, reject) => {
    if (document.querySelector(`script[src="${src}"]`)) {
      resolve(true)
      return
    }
    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.onload = () => resolve(true)
    script.onerror = () => reject(new Error(`Failed to load ${src}`))
    document.head.appendChild(script)
  })
}

const ensureWebRtcStreamerScripts = async() => {
  if (webrtcScriptLoader) return webrtcScriptLoader
  webrtcScriptLoader = Promise.all(WEBRTC_SCRIPT_URLS.map(loadScriptTag)).catch(err => {
    webrtcScriptLoader = null
    throw err
  })
  return webrtcScriptLoader
}

const stopWebrtcForDevice = async(deviceId: string) => {
  const player = webrtcPlayers[deviceId]
  if (!player) return
  try {
    if (typeof player.disconnect === 'function') {
      await player.disconnect()
    } else if (typeof player.stop === 'function') {
      await player.stop()
    }
  } catch (e) {
    // 忽略单个播放器停止时的异常
  }
  delete webrtcPlayers[deviceId]
}

const stopAllWebrtc = async() => {
  const ids = Object.keys(webrtcPlayers)
  await Promise.all(ids.map(id => stopWebrtcForDevice(id)))
}

const startWebrtcForDevice = async(device: any) => {
  const streamUrl =
    device?.streamUrl || device?.originalRtspUrl || device?.rtspUrl || device?.url
  if (!streamUrl) return

  const videoId = `vs-webrtc-${device.id}`
  const videoEl = document.getElementById(videoId) as HTMLVideoElement | null
  if (!videoEl) return

  await ensureWebRtcStreamerScripts()

  // 全局 WebRtcStreamer 挂在 window 上
  const anyWindow = window as any
  if (!anyWindow.WebRtcStreamer) {
    return
  }

  // 先停止旧的
  await stopWebrtcForDevice(device.id)

  const player = new anyWindow.WebRtcStreamer(videoId, WEBRTC_STREAMER_BASE)
  webrtcPlayers[device.id] = player

  try {
    if (typeof player.connect === 'function') {
      player.connect(streamUrl, '', 'rtptransport=tcp&timeout=60')
    } else if (typeof player.play === 'function') {
      player.play(streamUrl)
    }
  } catch (e) {
    // 单个窗口播放异常时忽略，避免打断其它窗口
  }
}

// 当设备列表或布局变化后，刷新当前分屏中的 WebRTC 播放
const refreshAllPreviewStreams = async() => {
  await nextTick()
  const slots = previewSlots.value
  // 先停止所有，再为当前槽位中有设备的重新启动
  await stopAllWebrtc()
  await Promise.all(
    slots
      .filter(slot => !!slot.device)
      .map(slot => startWebrtcForDevice(slot.device))
  )
}

const fetchDevices = async() => {
  const params = {
    accessToken: store.userInfo?.accessToken
  }
  const res: any = await getDeviceList(params)
  if (res.code === 200 && res.data && Array.isArray(res.data.records)) {
    devices.value = res.data.records.map((item: any) => ({
      privacy: item.isPublic === 1 ? '公开' : '私有',
      ...item
    }))
    await refreshAllPreviewStreams()
  }
}

onMounted(async() => {
  await fetchDevices()
  window.addEventListener('mousemove', handleMouseMove)
  window.addEventListener('mouseup', handleMouseUp)
  // 监听全屏状态变化
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  document.addEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.addEventListener('mozfullscreenchange', handleFullscreenChange)
  document.addEventListener('MSFullscreenChange', handleFullscreenChange)
})

onBeforeUnmount(async() => {
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('mouseup', handleMouseUp)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.removeEventListener('mozfullscreenchange', handleFullscreenChange)
  document.removeEventListener('MSFullscreenChange', handleFullscreenChange)
  // 退出全屏
  if (isFullscreen.value) {
    await exitFullscreen()
  }
  await stopAllWebrtc()
})

const showDetail = ref(false)
const currentDevice = ref<any | null>(null)

const openDetail = (item: any) => {
  currentDevice.value = item
  showDetail.value = true
}
</script>

<style scoped lang="scss">
.video-square {
  position: relative;
  z-index: 1;
}

.preview-section {
  position: relative;
}

.layout-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0 10px;
}

.layout-buttons {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.layout-icon {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  img{
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.layout-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.more-icon {
  width: 22px;
  height: 22px;
  object-fit: contain;
}

.mode-menu {
  min-width: 140px;
}

.mode-menu-item {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  font-size: 14px;
  color: #111827;
}

.mode-menu-icon {
  width: 22px;
  height: 22px;
  margin-right: 8px;
  object-fit: contain;
}

.preview-grid {
  display: grid;
  height: 280px;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
}

/* 全屏模式样式 */
.preview-grid.fullscreen-mode {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999;
  border-radius: 0;
  background: #000;
}

/* 基础 1/4/9/16 画面 */
.preview-grid.layout-p1 {
  grid-template-columns: repeat(1, 1fr);
  grid-template-rows: repeat(1, 1fr);
}

.preview-grid.layout-p4 {
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
}

.preview-grid.layout-p9 {
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, 1fr);
}

.preview-grid.layout-p16 {
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(4, 1fr);
}

/* 6 画面布局：左侧 1 个大窗口，右侧上下 2 个小窗口，底部 3 个窗口 */
.preview-grid.layout-p6 {
  grid-template-rows: repeat(3, 1fr);
  grid-template-columns: repeat(3, 1fr);
}

.preview-grid.layout-p6 .preview-cell:nth-child(1) {
  /* 大窗口，占左侧 2 行 2 列 */
  grid-row: 1 / span 2;
  grid-column: 1 / span 2;
}

.preview-grid.layout-p6 .preview-cell:nth-child(2) {
  /* 右上小窗口 */
  grid-row: 1 / 2;
  grid-column: 3 / 4;
}

.preview-grid.layout-p6 .preview-cell:nth-child(3) {
  /* 右中小窗口 */
  grid-row: 2 / 3;
  grid-column: 3 / 4;
}

.preview-grid.layout-p6 .preview-cell:nth-child(4) {
  /* 底部左 */
  grid-row: 3 / 4;
  grid-column: 1 / 2;
}

.preview-grid.layout-p6 .preview-cell:nth-child(5) {
  /* 底部中 */
  grid-row: 3 / 4;
  grid-column: 2 / 3;
}

.preview-grid.layout-p6 .preview-cell:nth-child(6) {
  /* 底部右 */
  grid-row: 3 / 4;
  grid-column: 3 / 4;
}

/* 8 画面布局：左侧 1 个大窗口，右侧上下 2 个小窗口，底部 4 个窗口 */
.preview-grid.layout-p8 {
  grid-template-rows: repeat(4, 1fr);
  grid-template-columns: repeat(4, 1fr);
}

.preview-grid.layout-p8 .preview-cell:nth-child(1) {
  /* 大窗口，占左侧 3 行 3 列 */
  grid-row: 1 / span 3;
  grid-column: 1 / span 3;
}

.preview-grid.layout-p8 .preview-cell:nth-child(2) {
  /* 右上小窗口 */
  grid-row: 1 / 2;
  grid-column: 4 / 5;
}

.preview-grid.layout-p8 .preview-cell:nth-child(3) {
  /* 右中小窗口 */
  grid-row: 2 / 3;
  grid-column: 4 / 5;
}

.preview-grid.layout-p8 .preview-cell:nth-child(4) {
  /* 右中小窗口 */
  grid-row: 3 / 4;
  grid-column: 4 / 5;
}

.preview-video-wrapper{
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.preview-video,.preview-empty{
  width: 100%;
  height: 100%;
}

.preview-cell {
  width: 100%;
  height: 100%;
  position: relative;
  background: #000;
  overflow: hidden;
}

.preview-video {
  display: block;
  object-fit: cover;
}

.cell-fullscreen-btn {
  position: absolute;
  top: 6px;
  right: 6px;
  z-index: 3;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: rgba(0, 0, 0, 0.35);
}

.cell-fullscreen-btn:active {
  background: rgba(0, 0, 0, 0.5);
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  background: #000;
}

.slider-bar {
  display: flex;
  justify-content: center;
  margin: 12px 0 10px;
  cursor: grab;
  user-select: none;
  touch-action: none;
  padding: 8px 0;
  -webkit-tap-highlight-color: transparent;
}

.slider-bar.dragging {
  cursor: grabbing;
}

.slider-thumb {
  width: 48px;
  height: 4px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.18);
  transition: width 0.18s ease, opacity 0.18s ease;
}

.device-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
}

.device-card {
  position: relative;
  background: #fff;
  border-radius: 8px;
  padding: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  gap: 10px;
}

.name {
  line-height: 25px;
  font-weight: 700;
  color: #333333;
  font-size: 16px;
}

.privacy-tag {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  box-sizing: border-box;
  background: rgba(255, 141, 34, 0.12);
  border-radius: 4px;
  line-height: 18px;
  color: #ff8d22;
  font-size: 12px;
  padding: 0 6px;
}

.info-row {
  display: flex;
  margin-top: 4px;
  font-size: 14px;
  line-height: 20px;
  img{
    width: 20px;
    height: 20px;
    margin-right: 4px;
  }
}

.label {
  min-width: 80px;
  color:#7a7c85;
}

.value {
  flex: 1;
  color:#333333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.card-footer {
  display: flex;
  align-items: center;
  margin-top: 10px;
}

.chips {
  display: flex;
  gap: 8px;
}

.chip {
  background: rgba(47, 105, 248, 1);
  border-radius: 2px;
  line-height: 16px;
  color: #ffffff;
  font-size: 12px;
  padding: 4px 12px;
}

.chip.offline {
  background: rgba(216, 216, 216, 1);
  color: #ffffff;
}

.play-btn {
  position: absolute;
  top: 16px;
  right: 12px;
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.play-btn img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
</style>

