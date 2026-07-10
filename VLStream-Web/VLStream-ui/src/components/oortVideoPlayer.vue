<template>
  <div :id="playerId" class="video-player-wrapper" />
</template>

<script setup lang="ts">
import { watch, onMounted, onBeforeUnmount, ref, nextTick } from 'vue'
import Player from 'xgplayer'
import 'xgplayer/dist/index.min.css' // 必须引入的样式文件

interface VideoProps {
  url: string
  // 其他视频属性按需添加
}

const props = defineProps<{
  video: VideoProps
  autoplay?: boolean
}>()

// 生成唯一的播放器ID
const playerId = ref(`xgplayer-${Math.random().toString(36).substr(2, 9)}`)
let player: InstanceType<typeof Player> | null = null

const initPlayer = async() => {
  try {
    await nextTick()

    // 如果播放器已存在，先销毁
    if (player) {
      player.destroy()
      player = null
    }

    // 检查容器是否存在
    const container = document.getElementById(playerId.value)
    if (!container) {
      console.error('视频播放器容器未找到')
      return
    }

    // 检查视频URL
    if (!props.video?.url) {
      console.error('视频URL为空')
      return
    }

    player = new Player({
      id: playerId.value,
      url: props.video.url,
      fluid: true,
      videoInit: true,
      controls: [
        'play',
        'fullscreen',
        'progress',
        'volume',
        'pip',
        'flex'
      ],
      pip: true,
      autoplay: !!props.autoplay, // 禁用自动播放
      playsinline: true, // 内联播放
      preload: 'auto' // 预加载元数据
    })

    player.on('error', (error) => {
      console.error('视频播放器错误:', error)
    })

    if (props.autoplay) {
      player.on('canplay', () => {
        player?.play()?.catch?.(() => {})
      })
    }
  } catch (error) {
    console.error('初始化视频播放器失败:', error)
  }
}

watch(() => props.video, (newVal) => {
  if (newVal?.url && newVal.url !== player?.src()) {
    if (player) {
      player.src(newVal.url)
      if (props.autoplay) {
        player.play()?.catch?.(() => {})
      }
    } else {
      initPlayer()
    }
  }
}, { deep: true })

onMounted(() => {
  initPlayer()
})

onBeforeUnmount(() => {
  if (player) {
    player.destroy()
    player = null
  }
})
</script>

<style scoped lang="scss">
.video-player-wrapper {
  width: 100%;
  height: 100%;
  min-height: 48px;
  min-width: 48px;
  position: relative;
  background: #000;
  border-radius: 4px;
  overflow: hidden;
}

:deep(.xgplayer) {
  width: 100% !important;
  height: 100% !important;
  min-height: 48px !important;
  min-width: 48px !important;
}

:deep(.xgplayer-video) {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover;
  min-height: 48px !important;
  min-width: 48px !important;
}

:deep(.xgplayer-controls) {
  opacity: 0.8;
  transition: opacity 0.3s ease;
}

:deep(.xgplayer):hover .xgplayer-controls {
  opacity: 1;
}

// 在表格中的特殊样式
:deep(.el-table .video-player-wrapper) {
  height: 48px;
  width: 48px;
}

:deep(.el-table .xgplayer) {
  height: 48px !important;
  width: 48px !important;
}

:deep(.el-table .xgplayer-video) {
  height: 48px !important;
  width: 48px !important;
}
</style>
