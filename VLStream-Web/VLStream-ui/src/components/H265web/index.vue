<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div @dblclick="fullscreenSwich" style="width: 100%; min-height: 450px; background-color: #000; position: relative;">
    <div :id="surfaceId" ref="surface" style="width: 100%; display: flex"></div>
    <div v-if="playError" class="h265-video-error" role="status">{{ $tp(playError) }}</div>
    <div class="buttons-box">
      <div class="buttons-box-left">
        <i v-if="!playing" class="iconfont icon-play h265web-btn" @click="unPause"></i>
        <i v-if="playing" class="iconfont icon-pause h265web-btn" @click="pause"></i>
        <i class="iconfont icon-stop h265web-btn" @click="destroy"></i>
        <i v-if="isNotMute" class="iconfont icon-audio-high h265web-btn" @click="mute()"></i>
        <i v-if="!isNotMute" class="iconfont icon-audio-mute h265web-btn" @click="cancelMute()"></i>
      </div>
      <div class="buttons-box-right">
        <!--          <i class="iconfont icon-file-record1 h265web-btn"></i>-->
        <!--          <i class="iconfont icon-xiangqing2 h265web-btn" ></i>-->
        <i class="iconfont icon-camera1196054easyiconnet h265web-btn" @click="screenshot"
           style="font-size: 1rem !important"></i>
        <i class="iconfont icon-shuaxin11 h265web-btn" @click="playBtnClick"></i>
        <i v-if="!fullscreen" class="iconfont icon-weibiaoti10 h265web-btn" @click="fullscreenSwich"></i>
        <i v-if="fullscreen" class="iconfont icon-weibiaoti11 h265web-btn" @click="fullscreenSwich"></i>
      </div>
    </div>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import { loadH265Web } from '@/utils/legacyVideoDecoders'

/**
 * from github
 * @see https://github.com/numberwolf/h265web.js/blob/master/example_normal/index.js
 */
const token = "base64:QXV0aG9yOmNoYW5neWFubG9uZ3xudW1iZXJ3b2xmLEdpdGh1YjpodHRwczovL2dpdGh1Yi5jb20vbnVtYmVyd29sZixFbWFpbDpwb3JzY2hlZ3QyM0Bmb3htYWlsLmNvbSxRUTo1MzEzNjU4NzIsSG9tZVBhZ2U6aHR0cDovL3h2aWRlby52aWRlbyxEaXNjb3JkOm51bWJlcndvbGYjODY5NCx3ZWNoYXI6bnVtYmVyd29sZjExLEJlaWppbmcsV29ya0luOkJhaWR1";
let nextSurfaceId = 0

export default {
  name: 'H265WebPlayer',
  props: ['videoUrl', 'error', 'hasAudio', 'height'],
  data() {
    return {
      surfaceId: `vls-h265-${++nextSurfaceId}`,
      playing: false,
      isNotMute: false,
      fullscreen: false,
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
        const Player = await loadH265Web()
        if (!current()) return
        const player = markRaw(new Player(url, {
          player: this.surfaceId,
          width: 960,
          height: 450,
          token,
          extInfo: { coreProbePart: 0.4, probeSize: 8192, ignoreAudio: 0 }
        }))
        this.player = player
        player.onOpenFullScreen = () => { if (current()) this.fullscreen = true }
        player.onCloseFullScreen = () => { if (current()) this.fullscreen = false }
        player.onReadyShowDone = () => { if (current()) this.unPause() }
        player.do()
      } catch {
        if (current()) this.playError = '播放器加载失败，请重新播放'
      }
    },
    screenshot() { this.player?.screenshot() },
    playBtnClick() { void this.play(this.streamUrl) },
    unPause() {
      if (!this.player) return this.playBtnClick()
      this.player.play()
      this.playing = this.player.isPlaying()
    },
    pause() {
      this.player?.pause()
      this.playing = false
    },
    mute() {
      this.player?.setVoice(0)
      this.isNotMute = false
    },
    cancelMute() {
      if (!this.player) return
      this.player.setVoice(1)
      this.isNotMute = true
    },
    destroy() {
      this.playVersion++
      const player = this.player
      this.player = null
      this.playing = false
      this.fullscreen = false
      player?.release()
    },
    fullscreenSwich() {
      if (this.fullscreen) this.player?.closeFullScreen()
      else this.player?.fullScreen()
    }
  }
}
</script>

<style>
.h265-video-error {
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

.h265web-btn {
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
