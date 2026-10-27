<template>
  <div class="square-playback-controls" :class="{ 'is-compact': compact, 'is-overlay': overlay }" role="toolbar" :aria-label="$tp('视频控制')" @pointerdown.stop @dblclick.stop @click.stop>
    <button type="button" class="playback-control" data-action="microphone" disabled
      :aria-label="$tp('麦克风')" :title="$tp('当前预览未开放对讲')">
      <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="8" y="2" width="8" height="13" rx="4" /><path d="M5 10v2a7 7 0 0 0 14 0v-2M12 19v3M8 22h8" /></svg>
    </button>
    <button type="button" class="playback-control" data-action="speaker" :disabled="!canMute || busy"
      :aria-label="$tp('扬声器')" :aria-pressed="!muted" :title="$tp(muted ? '开启声音' : '关闭声音')" @click="dispatch('toggle-mute')">
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 9h4l5-4v14l-5-4H4z" /><path v-if="muted" d="m17 9 5 6m0-6-5 6" /><path v-else d="M17 8a6 6 0 0 1 0 8m3-11a10 10 0 0 1 0 14" /></svg>
    </button>
    <button type="button" class="playback-control" data-action="screenshot" :disabled="!canScreenshot || busy"
      :aria-label="$tp('截屏')" :title="$tp('截屏')" @click="dispatch('screenshot')">
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 3H3v5m13-5h5v5M3 16v5h5m13-5v5h-5" /><circle cx="8" cy="15.5" r="2.2" /><circle cx="15.5" cy="15.5" r="2.2" /><path d="m9.2 13.7 6.5-7m-1.4 7L8 6.7" /></svg>
    </button>
    <button type="button" class="playback-control" :class="{ 'is-active': ptzOpen }" data-action="ptz" :disabled="busy"
      aria-label="PTZ" :aria-pressed="ptzOpen" :title="$tp(ptzOpen ? '收起PTZ' : '显示PTZ')" @click="dispatch('toggle-ptz')">
      <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5.5" y="2" width="13" height="20" rx="5" /><circle cx="12" cy="9" r="3.5" /><path d="M12 6v6m-3-3h6M10 17h4" /></svg>
    </button>
    <button type="button" class="playback-control" data-action="ai-hosting" disabled
      :aria-label="$tp('AI托管')" :title="$tp('AI托管暂不可用')">
      <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10" cy="14" r="7" /><circle cx="18" cy="4" r="2" /><circle cx="21" cy="10" r="1.1" /></svg>
    </button>
    <button type="button" class="playback-control" data-action="fullscreen" :disabled="busy"
      :aria-label="$tp(fullscreen ? '退出全屏' : '全屏')" :aria-pressed="fullscreen" :title="$tp(fullscreen ? '退出全屏' : '全屏')" @click="dispatch('toggle-fullscreen')">
      <svg v-if="fullscreen" viewBox="0 0 24 24" aria-hidden="true"><path d="M8 3v5H3m13-5v5h5M3 16h5v5m13-5h-5v5" /></svg>
      <svg v-else viewBox="0 0 24 24" aria-hidden="true"><path d="M8 3H3v5m13-5h5v5M3 16v5h5m13-5v5h-5" /></svg>
    </button>
    <button type="button" class="playback-control close-control" data-action="close" :disabled="!canClose || busy"
      :aria-label="$tp('关闭当前视频')" :title="$tp('关闭当前视频')" @click="dispatch('close-video')">
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 14c5-5 13-5 18 0l-2 4-4-2v-3a10 10 0 0 0-6 0v3l-4 2z" /></svg>
    </button>
  </div>
</template>

<script setup>
const props = defineProps({
  compact: Boolean,
  overlay: Boolean,
  ptzOpen: Boolean,
  muted: { type: Boolean, default: true },
  canMute: Boolean,
  canScreenshot: Boolean,
  canClose: Boolean,
  fullscreen: Boolean,
  busy: Boolean,
})
const emit = defineEmits(['toggle-mute', 'screenshot', 'toggle-ptz', 'toggle-fullscreen', 'close-video'])
function dispatch(action) {
  if (props.busy || (action === 'toggle-mute' && !props.canMute) ||
    (action === 'screenshot' && !props.canScreenshot) || (action === 'close-video' && !props.canClose)) return
  if (['toggle-mute', 'screenshot', 'toggle-ptz', 'toggle-fullscreen', 'close-video'].includes(action)) emit(action)
}
</script>

<style scoped>
/* MasterGo 257:25424 — 控制栏 gap 固定 12，按钮固定 56×48（不参与收缩），红键 72×48 */
.square-playback-controls { display:flex; flex:0 0 auto; flex-wrap:wrap; align-items:center; justify-content:center; gap:12px; width:auto; max-width:100%; margin-inline:auto; padding-block:0; box-sizing:border-box; }
.playback-control { display:inline-flex; align-items:center; justify-content:center; flex:0 0 56px; min-width:0; height:48px; padding:0; border:0; border-radius:200px; background:#343537; color:#fff; cursor:pointer; }
.playback-control svg { width:24px; height:24px; fill:none; stroke:currentColor; stroke-width:1.8; stroke-linecap:round; stroke-linejoin:round; pointer-events:none; }
.playback-control.is-active { background:#2278ff; }
.playback-control.close-control { flex-basis:72px; min-width:30px; max-width:72px; background:#dc362e; }
.playback-control:disabled { opacity:.5; cursor:default; }
.playback-control:focus-visible { outline:2px solid #2278ff; outline-offset:3px; }
.playback-control:not(:disabled):hover { filter:brightness(1.12); }
.square-playback-controls.is-compact { flex:0 0 auto; flex-wrap:nowrap; width:auto; margin:0; padding:0; gap:2px; }
.is-compact .playback-control { flex:0 0 23px; min-width:23px; width:23px; height:26px; border-radius:3px; background:transparent; }
.is-compact .playback-control svg { width:17px; height:17px; }
.is-compact .playback-control.close-control { flex-basis:28px; min-width:28px; background:#dc362e; }
.is-compact .playback-control.is-active { background:rgb(0 0 0 / 20%); }
.square-playback-controls.is-overlay { justify-content:flex-start; width:100%; max-width:none; margin:0; padding:0; gap:20px; }
.is-overlay .playback-control { background:transparent; flex:0 0 24px; min-width:24px; max-width:24px; height:40px; border-radius:0; }
.is-overlay .playback-control.close-control { margin-left:auto; flex:0 0 40px; min-width:40px; max-width:40px; border-radius:200px; background:#dc362e; }
.is-overlay .playback-control.is-active { background:transparent; color:#7eb4ff; }
.is-overlay.is-compact { gap:4px; flex-wrap:wrap; }
.is-overlay.is-compact .playback-control { height:26px; }
.is-overlay.is-compact .playback-control.close-control { flex-basis:28px; min-width:28px; max-width:28px; }
.playback-control svg { display:none; }
.playback-control::before { content:''; width:24px; height:24px; background:currentColor; mask:var(--control-icon) center / contain no-repeat; pointer-events:none; }
.is-compact .playback-control::before { width:17px; height:17px; }
.playback-control[data-action="microphone"] { --control-icon:url('../../../assets/mastergo/video-square/microphone.svg'); order:0; }
.playback-control[data-action="speaker"] { --control-icon:url('../../../assets/mastergo/video-square/speaker.svg'); order:1; }
.playback-control[data-action="screenshot"] { --control-icon:url('../../../assets/mastergo/video-square/screenshot.svg'); order:2; }
.playback-control[data-action="ptz"] { --control-icon:url('../../../assets/mastergo/video-square/ptz.svg'); order:3; }
.playback-control[data-action="ai-hosting"] { --control-icon:url('../../../assets/mastergo/video-square/ai-hosting.svg'); order:4; }
.playback-control[data-action="fullscreen"] { --control-icon:url('../../../assets/mastergo/video-square/fullscreen.svg'); order:5; }
.playback-control[data-action="close"] { --control-icon:url('../../../assets/mastergo/video-square/close.svg'); order:6; }
@media(max-width:700px) { .is-compact .playback-control { height:26px; } .is-compact .playback-control svg { width:17px; height:17px; } }
@media(max-width:700px) {
  /* 窄屏允许收缩，避免 7 个固定宽度按钮溢出 */
  .playback-control { flex:0 1 56px; height:40px; }
  .playback-control svg { width:22px; height:22px; }
}
</style>
