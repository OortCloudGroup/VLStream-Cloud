<template>
  <div ref="player" class="player_item" />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Config from '@/config'

const player = ref(null)
const props = defineProps({
  src: {
    type: String,
    required: true,
    default: ''
  },
  type: {
    type: String,
    default: 'flv',
    validator: (value) => {
      // 只允许指定的类型
      return ['flv', 'mp4', 'm3u8', 'rtc', 'webrtc'].includes(value)
    }
  },
  autoSize: {
    type: Boolean,
    default: true
  },
  showHeader: {
    type: Boolean,
    default: false
  },
  name: {
    type: String,
    default: ''
  },
  webRTCSocketURL: {
    type: String,
    default: ''
  }
})

onMounted(() => {
  let tempSrc = props.src
  // 对是处理  如果不是http开头 就加上 当前访问window.location.origin ，
  if (!tempSrc.startsWith('http') && props.type !== 'cameraRTC') {
    tempSrc = (Config.URL.endsWith('/') ? Config.URL.slice(0, -1) : Config.URL) + tempSrc
  }
  if (player.value) {
    const OPlayer = new window.OToolBox.OPlayer(player.value,
      { autoSize: props.autoSize, showHeader: props.showHeader,
        webRTCSocketURL: props.webRTCSocketURL || Config.webRTCSocketURL })
    OPlayer.play({ src: tempSrc, type: props.type, name: props.name })
  }
})

</script>

<style lang="scss" scoped>
    .player_item {
        width: 100%;
        height: 100%;
    }
</style>
