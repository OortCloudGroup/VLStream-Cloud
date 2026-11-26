<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div ref="container" @dblclick="fullscreenSwich"
       style="width:100%; height: 100%; background-color: #000000;margin:0 auto;position: relative;">
    <div ref="surface" style="width: 100%; aspect-ratio: 16 / 9;"></div>
    <div v-if="playError" class="legacy-video-error" role="status">{{ $tp(playError) }}</div>
    <div class="buttons-box">
      <div class="buttons-box-left">
        <i v-if="!playing" class="iconfont icon-play jessibuca-btn" @click="playBtnClick"></i>
        <i v-if="playing" class="iconfont icon-pause jessibuca-btn" @click="pause"></i>
        <i class="iconfont icon-stop jessibuca-btn" @click="destroy"></i>
        <i v-if="isNotMute" class="iconfont icon-audio-high jessibuca-btn" @click="mute()"></i>
        <i v-if="!isNotMute" class="iconfont icon-audio-mute jessibuca-btn" @click="cancelMute()"></i>
      </div>
      <div class="buttons-box-right">
        <span class="jessibuca-btn">{{ kBps }} kb/s</span>
        <!--          <i class="iconfont icon-file-record1 jessibuca-btn"></i>-->
        <!--          <i class="iconfont icon-xiangqing2 jessibuca-btn" ></i>-->
        <i class="iconfont icon-camera1196054easyiconnet jessibuca-btn" @click="screenshot"
           style="font-size: 1rem !important"></i>
        <i class="iconfont icon-shuaxin11 jessibuca-btn" @click="playBtnClick"></i>
        <i v-if="!fullscreen" class="iconfont icon-weibiaoti10 jessibuca-btn" @click="fullscreenSwich"></i>
        <i v-if="fullscreen" class="iconfont icon-weibiaoti11 jessibuca-btn" @click="fullscreenSwich"></i>
      </div>
    </div>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import { loadJessibuca } from '@/utils/legacyVideoDecoders'

export default {
  name: 'JessibucaPlayer',
  props: ['videoUrl', 'error', 'hasAudio', 'height'],
  data() {
    return {
      playing: false,
      isNotMute: false,
      fullscreen: false,
      kBps: 0,
      player: null,
      playVersion: 0,
      playError: ''
    }
  },
  computed: {
    streamUrl() {
      return this.videoUrl ?? this.$route?.params?.url ?? ''
    }
  },
  mounted() {
    void this.play(this.streamUrl)
  },
  watch: {
    streamUrl(url) {
      void this.play(url)
    }
  },
  beforeUnmount() {
    this.destroy()
  },
  methods: {
    async play(url) {
      this.destroy()
      this.playError = ''
      if (typeof url !== 'string' || !url) return
      const version = this.playVersion
      const current = () => version === this.playVersion && this.$refs.surface
      try {
        const Player = await loadJessibuca()
        if (!current()) return
        const player = markRaw(new Player({
          container: this.$refs.surface,
          autoWasm: true,
          background: '',
          controlAutoHide: false,
          debug: false,
          decoder: `${import.meta.env.BASE_URL}jessibuca/decoder.js`,
          forceNoOffscreen: false,
          hasAudio: this.hasAudio ?? true,
          isNotMute: this.isNotMute,
          isFlv: false,
          hiddenAutoPause: false,
          hotKey: true,
          isFullResize: false,
          isResize: false,
          keepScreenOn: true,
          useMSE: true,
          useWCS: false,
          useWebFullScreen: true,
          wcsUseVideoRender: true,
          videoBuffer: 0.1,
          timeout: 10,
          heartTimeout: 5,
          heartTimeoutReplay: true,
          heartTimeoutReplayTimes: 3,
          loadingTimeout: 10,
          loadingTimeoutReplay: true,
          loadingTimeoutReplayTimes: 3,
          loadingText: this.$tp('视频加载中，请稍候'),
          openWebglAlignment: false,
          recordType: 'mp4',
          rotate: 0,
          showBandwidth: false,
          supportDblclickFullscreen: false,
          wasmDecodeErrorReplay: true,
          operateBtns: { fullscreen: false, screenshot: false, play: false, audio: false, record: false }
        }))
        this.player = player
        player.on('play', () => {
          if (!current()) return
          this.playing = true
          this.playError = ''
        })
        player.on('pause', () => { if (current()) this.playing = false })
        player.on('fullscreen', value => { if (current()) this.fullscreen = value })
        player.on('mute', value => { if (current()) this.isNotMute = !value })
        player.on('kBps', value => { if (current()) this.kBps = Math.round(value) })
        const failed = () => { if (current()) this.playError = '视频连接失败，请重新播放' }
        player.on('error', failed)
        const start = async () => {
          if (!current()) return
          try { await player.play(url) } catch { failed() }
        }
        if (player.hasLoaded()) void start()
        else player.on('load', start)
      } catch {
        if (current()) this.playError = '播放器加载失败，请重新播放'
      }
    },
    playBtnClick() { void this.play(this.streamUrl) },
    pause() { this.player?.pause() },
    screenshot() { this.player?.screenshot() },
    mute() { this.player?.mute() },
    cancelMute() { this.player?.cancelMute() },
    destroy() {
      this.playVersion++
      const player = this.player
      this.player = null
      this.playing = false
      this.fullscreen = false
      this.kBps = 0
      player?.destroy()
    },
    fullscreenSwich() {
      this.player?.setFullscreen(!this.fullscreen)
    }
  }
}
</script>

<style>
.legacy-video-error {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: #fff;
  pointer-events: none;
}

.buttons-box {
  width: 100%;
  height: 28px;
  background-color: rgba(43, 51, 63, 0.7);
  position: absolute;
  display: -webkit-box;
  display: -ms-flexbox;
  display: flex;
  left: 0;
  bottom: 0;
  user-select: none;
  z-index: 10;
}

.jessibuca-btn {
  width: 20px;
  color: rgb(255, 255, 255);
  line-height: 27px;
  margin: 0px 10px;
  padding: 0px 2px;
  cursor: pointer;
  text-align: center;
  font-size: 0.8rem !important;
}

.buttons-box-right {
  position: absolute;
  right: 0;
}
</style>
