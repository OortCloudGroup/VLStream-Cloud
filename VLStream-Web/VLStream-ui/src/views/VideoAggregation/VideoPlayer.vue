<template>
  <div class="video-player-dialog">
    <el-dialog
      v-model="visible"
      title="视频播放"
      width="80%"
      :before-close="handleClose"
      @close="handleClose"
    >
      <div class="video-container">
        <!-- 调试信息 -->
        <div v-if="process.env.NODE_ENV === 'development'" class="debug-info" 
             style="position: absolute; top: 10px; left: 10px; background: rgba(0,0,0,0.8); color: white; padding: 10px; border-radius: 4px; font-size: 12px; z-index: 999;">
          <div>设备信息: {{ JSON.stringify(deviceInfo, null, 2) }}</div>
          <div>流类型: {{ streamType }}</div>
          <div>流URL: {{ deviceInfo.streamUrl }}</div>
          <div>对话框可见: {{ visible }}</div>
          <div>错误信息: {{ videoError }}</div>
        </div>
        
        <!-- 视频播放区域 -->
        <div class="video-area">
          <!-- YouTube视频 -->
          <iframe
            v-if="streamType === 'youtube'"
            ref="youtubePlayer"
            :src="youtubeEmbedUrl"
            class="video-iframe"
            frameborder="0"
            allowfullscreen
          ></iframe>
          
          <!-- HTTP流视频 -->
          <iframe
            v-else-if="streamType === 'http'"
            :src="deviceInfo.streamUrl"
            class="video-iframe"
            frameborder="0"
          ></iframe>
          
          <!-- HLS流视频 -->
          <video
            v-else-if="streamType === 'hls'"
            ref="hlsPlayer"
            class="video-player"
            controls
            autoplay
            muted
          ></video>
          
          <!-- RTSP流视频 -->
          
          <!-- CameraRTC -->
          <div
            v-else-if="streamType === 'cameraRTC'"
            ref="cameraRtcContainer"
            class="video-player camera-rtc-player"
          ></div>

<div v-else-if="streamType === 'rtsp'" class="rtsp-info">
            <div class="rtsp-content">
              <h3>RTSP流播放</h3>
              <p>RTSP流需要专用播放器打开</p>
              <div class="rtsp-url">
                <span>流地址：</span>
                <span class="url-text">{{ deviceInfo.streamUrl }}</span>
              </div>
              <div class="rtsp-actions">
                <el-button type="primary" @click="copyStreamUrl">复制地址</el-button>
                <el-button type="success" @click="openInVlc">使用VLC播放</el-button>
              </div>
            </div>
          </div>
          
          <!-- 普通视频文件 -->
          <video
            v-else-if="streamType === 'video'"
            ref="videoPlayer"
            class="video-player"
            controls
            autoplay
            muted
          >
            <source :src="deviceInfo.streamUrl" type="video/mp4">
            您的浏览器不支持视频播放
          </video>
          
          <!-- 错误或未知格式 -->
          <div v-else class="video-error">
            <div class="error-content">
              <h3>无法播放视频</h3>
              <p>不支持的视频格式或地址无效</p>
              <div class="error-url">
                <span>流地址：</span>
                <span class="url-text">{{ deviceInfo.streamUrl || '未设置' }}</span>
              </div>
            </div>
          </div>
          
          <!-- 视频信息覆盖层 -->
          <div class="video-overlay" v-if="streamType !== 'rtsp' && streamType !== 'unknown'">
            <div class="overlay-content">
              <div class="stream-info">
                <span class="stream-type">{{ streamTypeText }}</span>
                <span class="device-name">{{ deviceInfo.deviceName }}</span>
              </div>
              <div class="controls">
                <el-button
                  size="small"
                  :type="isRecording ? 'danger' : 'primary'"
                  @click="toggleRecording"
                >
                  {{ isRecording ? '停止录制' : '开始录制' }}
                </el-button>
                <span class="current-time">{{ currentTime }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- PTZ控制面板 -->
        <div class="ptz-control-area" v-if="showPTZControl">
          <PTZControl
            :device-info="deviceInfo"
            @ptz-command="handlePTZCommand"
          />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import PTZControl from '@/components/PTZControl.vue'
import { 
  getStreamType, 
  getYouTubeEmbedUrl, 
  copyStreamUrl as copyUrl, 
  openInVlc as openVlc 
} from './deviceUtils.js'
import { CAMERA_RTC_SOCKET_URL, ensureOPlayer } from '@/utils/oplayer'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  deviceInfo: {
    type: Object,
    default: () => ({})
  },
  showPTZControl: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'close', 'ptz-command'])

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const isRecording = ref(false)
const currentTime = ref('')
const videoError = ref('')

// 视频播放器引用
const youtubePlayer = ref(null)
const hlsPlayer = ref(null)
const videoPlayer = ref(null)
const cameraRtcContainer = ref(null)
const cameraRtcPlayer = ref(null)

// 计算属性
const streamType = computed(() => {
  return getStreamType(props.deviceInfo.streamUrl)
})

const youtubeEmbedUrl = computed(() => {
  return getYouTubeEmbedUrl(props.deviceInfo.streamUrl)
})

const streamTypeText = computed(() => {
  const typeMap = {
    'youtube': 'YouTube视频',
    'rtsp': 'RTSP流',
    'hls': 'HLS流',
    'http': 'HTTP流',
    'video': '视频文件',
    'cameraRTC': 'CameraRTC',
    'unknown': '未知格式'
  }
  return typeMap[streamType.value] || '未知格式'
})

// 定时器
let timeTimer = null

// 方法
const updateCurrentTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const startTimeTimer = () => {
  updateCurrentTime()
  timeTimer = setInterval(updateCurrentTime, 1000)
}

const stopTimeTimer = () => {
  if (timeTimer) {
    clearInterval(timeTimer)
    timeTimer = null
  }
}

const handleClose = () => {
  // 停止录制
  if (isRecording.value) {
    toggleRecording()
  }
  
  // 清理播放器资源
  cleanupPlayers()
  
  // 停止定时器
  stopTimeTimer()
  
  // 清理错误状态
  videoError.value = ''
  cleanupCameraRtcPlayer()
  
  emit('close')
}

const toggleRecording = () => {
  isRecording.value = !isRecording.value
  
  if (isRecording.value) {
    ElMessage.success('开始录制')
    startTimeTimer()
  } else {
    ElMessage.info('停止录制')
    stopTimeTimer()
  }
}

const copyStreamUrl = () => {
  copyUrl(props.deviceInfo.streamUrl)
}

const openInVlc = () => {
  openVlc(props.deviceInfo.streamUrl)
}

const handlePTZCommand = (command) => {
  emit('ptz-command', command)
}

// 初始化HLS播放器
const initHLSPlayer = () => {
  if (streamType.value === 'hls' && hlsPlayer.value) {
    const video = hlsPlayer.value
    
    // 清理之前的HLS实例
    if (video.hlsInstance) {
      video.hlsInstance.destroy()
      video.hlsInstance = null
    }
    
    console.log('初始化HLS播放器，URL:', props.deviceInfo.streamUrl)
    
    if (window.Hls && window.Hls.isSupported()) {
      const hls = new window.Hls({
        debug: process.env.NODE_ENV === 'development',
        enableWorker: true,
        lowLatencyMode: true,
        backBufferLength: 90
      })
      
      // 保存实例引用以便后续清理
      video.hlsInstance = hls
      
      hls.loadSource(props.deviceInfo.streamUrl)
      hls.attachMedia(video)
      
      hls.on(window.Hls.Events.MANIFEST_PARSED, () => {
        console.log('HLS manifest 解析成功')
        video.play().catch(error => {
          console.error('HLS自动播放失败:', error)
          ElMessage.warning('视频自动播放失败，请手动点击播放')
        })
      })
      
      hls.on(window.Hls.Events.ERROR, (event, data) => {
        console.error('HLS播放错误:', data)
        if (data.fatal) {
          switch (data.type) {
            case window.Hls.ErrorTypes.NETWORK_ERROR:
              console.log('网络错误，尝试恢复...')
              hls.startLoad()
              break
            case window.Hls.ErrorTypes.MEDIA_ERROR:
              console.log('媒体错误，尝试恢复...')
              hls.recoverMediaError()
              break
            default:
              console.log('无法恢复的错误，销毁播放器')
              hls.destroy()
              videoError.value = '视频播放失败: ' + data.details
              ElMessage.error('视频播放失败')
              break
          }
        }
      })
      
      hls.on(window.Hls.Events.MEDIA_ATTACHED, () => {
        console.log('HLS媒体已附加')
      })
      
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      console.log('使用原生HLS支持')
      video.src = props.deviceInfo.streamUrl
      video.addEventListener('loadedmetadata', () => {
        console.log('视频元数据加载完成')
        video.play().catch(error => {
          console.error('原生HLS自动播放失败:', error)
          ElMessage.warning('视频自动播放失败，请手动点击播放')
        })
      })
      
      video.addEventListener('error', (e) => {
        console.error('原生视频播放错误:', e)
        videoError.value = '视频播放失败'
        ElMessage.error('视频播放失败')
      })
    } else {
      console.error('浏览器不支持HLS播放')
      videoError.value = '浏览器不支持HLS播放，请使用Chrome、Firefox或Safari浏览器'
      ElMessage.error('浏览器不支持HLS播放')
    }
  }
}

// 初始化普通视频播放器
const initVideoPlayer = () => {
  if (streamType.value === 'video' && videoPlayer.value) {
    const video = videoPlayer.value
    
    console.log('初始化视频播放器，URL:', props.deviceInfo.streamUrl)
    
    video.addEventListener('loadedmetadata', () => {
      console.log('视频元数据加载完成')
    })
    
    video.addEventListener('error', (e) => {
      console.error('视频播放错误:', e)
      videoError.value = '视频播放失败'
      ElMessage.error('视频播放失败')
    })
    
    video.addEventListener('canplay', () => {
      console.log('视频可以开始播放')
    })
  }
}

// 清理播放器资源

const initCameraRtcPlayer = async () => {
  if (streamType.value !== 'cameraRTC' || !cameraRtcContainer.value) {
    return
  }

  try {
    await ensureOPlayer()
    await nextTick()

    cleanupCameraRtcPlayer()

    const deviceId = props.deviceInfo?.deviceId || props.deviceInfo?.id
    if (!deviceId) {
      throw new Error('CameraRTC requires deviceId')
    }

    cameraRtcPlayer.value = new window.OToolBox.OPlayer(cameraRtcContainer.value, {
      debuggerMode: process.env.NODE_ENV === 'development',
      autoSize: true,
      backgroundColor: '#000000',
      showHeader: true,
      webRTCSocketURL: CAMERA_RTC_SOCKET_URL
    })
    cameraRtcPlayer.value.play({
      type: 'cameraRTC',
      src: String(deviceId),
      name: props.deviceInfo?.deviceName || props.deviceInfo?.name || ''
    })
  } catch (error) {
    console.error('CameraRTC play failed:', error)
    videoError.value = 'CameraRTC play failed'
    ElMessage.error('CameraRTC play failed')
  }
}


const cleanupCameraRtcPlayer = () => {
  if (cameraRtcPlayer.value?.compInstance?.$destroy) {
    cameraRtcPlayer.value.compInstance.$destroy()
  }
  cameraRtcPlayer.value = null

  if (cameraRtcContainer.value) {
    cameraRtcContainer.value.innerHTML = ''
  }
}

const cleanupPlayers = () => {
  cleanupCameraRtcPlayer()
  // 清理HLS播放器
  if (hlsPlayer.value && hlsPlayer.value.hlsInstance) {
    hlsPlayer.value.hlsInstance.destroy()
    hlsPlayer.value.hlsInstance = null
  }
  
  // 清理普通视频播放器
  if (videoPlayer.value) {
    videoPlayer.value.pause()
    videoPlayer.value.currentTime = 0
    videoPlayer.value.src = ''
  }
  
  // 清理HLS播放器
  if (hlsPlayer.value) {
    hlsPlayer.value.pause()
    hlsPlayer.value.currentTime = 0
    hlsPlayer.value.src = ''
  }
}

// 监听设备信息变化
watch(() => props.deviceInfo, (newDeviceInfo, oldDeviceInfo) => {
  console.log('VideoPlayer - 设备信息变化:', {
    new: newDeviceInfo,
    old: oldDeviceInfo,
    streamUrl: newDeviceInfo?.streamUrl,
    streamType: getStreamType(newDeviceInfo?.streamUrl)
  })
  
  videoError.value = ''
  
  // 当设备信息变化时，重新初始化播放器
  if (visible.value && newDeviceInfo?.streamUrl) {
    console.log('VideoPlayer - 重新初始化播放器')
    setTimeout(() => {
      initHLSPlayer()
      initVideoPlayer()
      initCameraRtcPlayer()
    }, 200)
  }
}, { deep: true, immediate: true })

// 监听对话框显示状态
watch(visible, (newValue) => {
  console.log('VideoPlayer - 对话框显示状态变化:', newValue)
  
  if (newValue) {
    console.log('VideoPlayer - 对话框打开，设备信息:', props.deviceInfo)
    startTimeTimer()
    
    // 延迟初始化播放器，确保DOM已渲染
    setTimeout(() => {
      console.log('VideoPlayer - 开始初始化播放器')
      initHLSPlayer()
      initVideoPlayer()
      initCameraRtcPlayer()
    }, 300)
  } else {
    console.log('VideoPlayer - 对话框关闭')
    cleanupPlayers()
    stopTimeTimer()
  }
})

// 生命周期
onMounted(() => {
  if (visible.value) {
    startTimeTimer()
  }
})

onBeforeUnmount(() => {
  stopTimeTimer()
})
</script>

<style scoped lang="scss">
.video-player-dialog {
  .video-container {
    display: flex;
    gap: 20px;
    height: 70vh;
    min-height: 500px;
  }
  
  .video-area {
    flex: 1;
    position: relative;
    background: #000;
    border-radius: 8px;
    overflow: hidden;
  }
  
  .video-iframe,
  .video-player {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  
  .rtsp-info,
  .video-error {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    background: #f5f5f5;
    
    .rtsp-content,
    .error-content {
      text-align: center;
      padding: 40px;
      
      h3 {
        margin: 0 0 16px 0;
        font-size: 20px;
        color: #333;
      }
      
      p {
        margin: 0 0 20px 0;
        color: #666;
      }
      
      .rtsp-url,
      .error-url {
        margin: 20px 0;
        padding: 12px;
        background: #fff;
        border: 1px solid #dcdfe6;
        border-radius: 6px;
        
        .url-text {
          font-family: monospace;
          color: #409eff;
          word-break: break-all;
        }
      }
      
      .rtsp-actions {
        display: flex;
        gap: 12px;
        justify-content: center;
      }
    }
  }
  
  .video-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(
      to bottom,
      rgba(0, 0, 0, 0.7) 0%,
      rgba(0, 0, 0, 0) 30%,
      rgba(0, 0, 0, 0) 70%,
      rgba(0, 0, 0, 0.7) 100%
    );
    pointer-events: none;
    
    .overlay-content {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      padding: 20px;
      
      .stream-info {
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .stream-type {
          background: rgba(24, 144, 255, 0.8);
          color: white;
          padding: 4px 8px;
          border-radius: 4px;
          font-size: 12px;
        }
        
        .device-name {
          color: white;
          font-weight: 500;
        }
      }
      
      .controls {
        display: flex;
        justify-content: space-between;
        align-items: center;
        pointer-events: auto;
        
        .current-time {
          color: white;
          font-family: monospace;
          background: rgba(0, 0, 0, 0.5);
          padding: 6px 12px;
          border-radius: 4px;
        }
      }
    }
  }
  
  .ptz-control-area {
    width: 300px;
    flex-shrink: 0;
  }
}

// 响应式设计
@media (max-width: 1200px) {
  .video-player-dialog {
    .video-container {
      flex-direction: column;
      height: auto;
    }
    
    .ptz-control-area {
      width: 100%;
    }
  }
}
</style> 
