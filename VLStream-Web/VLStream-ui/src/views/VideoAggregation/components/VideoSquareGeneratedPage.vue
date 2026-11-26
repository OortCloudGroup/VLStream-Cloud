<template>
    <div ref="root" class="generated-page" :class="{ drafting: draftVisible }">
        <iframe v-if="active && live" ref="liveFrame" :key="liveKey" class="page-frame live" :class="{ shown: liveReady && !draftVisible }"
            :srcdoc="live.html" sandbox="allow-scripts" referrerpolicy="no-referrer"
            allow="camera 'none'; microphone 'none'; geolocation 'none'; clipboard-write 'none'; fullscreen 'none'"
            :title="scene.title || $tp('场景页面')" @load="liveLoaded" />
        <iframe v-if="active && draftSource" ref="draftFrame" class="page-frame draft" :class="{ shown: draftVisible }"
            :srcdoc="draftSource" sandbox="allow-scripts" referrerpolicy="no-referrer" tabindex="-1" aria-hidden="true"
            :title="$tp('页面草稿')" />
        <Transition name="page-fade">
            <div v-if="active && !liveReady && !draftVisible && !hung" class="page-loading" role="status">
                <span class="loading-orbit" aria-hidden="true"><i /><i /><i /></span>{{ $tp('正在启动页面…') }}
            </div>
        </Transition>
        <div v-if="hung" class="page-hung" role="alert">
            <strong>{{ $tp('页面脚本长时间无响应，已停止运行') }}</strong>
            <button type="button" @click="restart">{{ $tp('重新运行') }}</button>
        </div>
    </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { draftSource as draftBootstrap, runtimeSource, threeSource } from 'virtual:vls-generated-page-runtime'
import { authorizedAliases, draftDocument, excerpt, liveDocument, publicCameras, sourceLine, visibleSlot } from '@/generated-page/host.mjs'

const props = defineProps({
    scene: { type: Object, required: true }, devices: { type: Array, default: () => [] },
    draft: { type: Object, default: null }, active: { type: Boolean, default: true },
    theme: { type: String, default: 'light' }, previewId: { type: String, default: null },
})
const emit = defineEmits(['select', 'denied', 'close-preview', 'runtime-error', 'ready', 'slot'])
const root = ref(null), liveFrame = ref(null), draftFrame = ref(null)
const liveKey = ref(0), liveReady = ref(false), hung = ref(false)
const slotRect = shallowRef(null), draftSource = ref(''), draftVisible = ref(false)
const aliases = computed(() => authorizedAliases(props.scene.cameras, props.devices))
const cameras = computed(() => publicCameras(aliases.value))
const live = shallowRef(null)
let loads = 0, lastBeat = 0, watchdog = 0, draftReady = false, pendingDraft = '', errors = []

function nonce() {
    return Array.from(crypto.getRandomValues(new Uint8Array(16)), n => n.toString(16).padStart(2, '0')).join('')
}
function build() {
    loads = 0
    errors = []
    liveReady.value = false
    hung.value = false
    slotRect.value = null
    emit('slot', null)
    live.value = props.active && props.scene?.html
        ? liveDocument(props.scene.html, { runtime: runtimeSource, three: threeSource, cameras: cameras.value, theme: props.theme })
        : null
    liveKey.value++
}
function restart() { build() }
function postLive(message) { liveFrame.value?.contentWindow?.postMessage({ vls: 'host', ...message }, '*') }
function liveLoaded() {
    // A second load means the page navigated itself. Sandboxed pages may not leave the generated document.
    if (++loads > 1) {
        reportError({ kind: 'navigation', message: '页面尝试跳转到其他地址，已阻止并重新载入' })
        build()
    }
}
function reportError(raw) {
    const line = sourceLine(raw.line, live.value?.lines)
    const error = { kind: raw.kind || 'script', message: String(raw.message || '页面脚本执行失败').slice(0, 400), line,
        code: excerpt(props.scene.html, line), stack: raw.stack || '' }
    if (errors.some(item => item.message === error.message && item.line === error.line)) return
    errors = [...errors, error].slice(0, 8)
    emit('runtime-error', errors)
}
function receive(event) {
    const data = event.data
    if (!data || typeof data !== 'object' || event.origin !== 'null') return
    if (draftFrame.value && event.source === draftFrame.value.contentWindow && data.vls === 'draft' && data.type === 'ready') {
        draftReady = true
        sendDraft()
        return
    }
    if (!liveFrame.value || event.source !== liveFrame.value.contentWindow || data.vls !== 'page') return
    lastBeat = Date.now()
    if (data.type === 'ready') {
        liveReady.value = true
        hung.value = false
        if (releaseOnReady) hideDraft()
        postLive({ type: 'preview', id: props.previewId })
        emit('ready')
    } else if (data.type === 'error') {
        reportError(data)
    } else if (data.type === 'preview' && !props.draft) {
        const device = aliases.value.get(data.id)
        if (device) emit('select', { deviceId: device.id, key: data.id })
    } else if (data.type === 'preview-denied') {
        emit('denied', String(data.id || ''))
    } else if (data.type === 'close-preview') {
        emit('close-preview')
    } else if (data.type === 'slot') {
        const frame = liveFrame.value
        slotRect.value = visibleSlot(data.rect, frame.clientWidth, frame.clientHeight)
        emit('slot', slotRect.value)
    }
}
function sendDraft() {
    if (!draftReady || !pendingDraft) return
    // Draw only drafts with visible content. A bare <head> (for example when an edit falls back to a
    // whole-page rewrite) would blank the stage, so the last drawn draft or the live page stays instead.
    if (!/<body[^>]*>[\s\S]*<(?:div|main|section|header|svg|canvas|h1|h2|nav|aside|button)\b/i.test(pendingDraft)) return
    draftFrame.value?.contentWindow?.postMessage({ vls: 'host', type: 'draft', html: pendingDraft }, '*')
    draftVisible.value = true
}
let releaseOnReady = false, releaseTimer = 0
function hideDraft() {
    releaseOnReady = false
    clearTimeout(releaseTimer)
    draftVisible.value = false
    pendingDraft = ''
    draftReady = false
    setTimeout(() => { if (!props.draft) draftSource.value = '' }, 500)
}
function stopDraft() {
    // Runs after the scene watcher: when a new page is starting, keep the draft until it reports ready
    // so the stage crossfades from draft to final page instead of flashing empty.
    setTimeout(() => {
        if (props.draft) return
        if (!draftVisible.value || liveReady.value || !live.value) { hideDraft(); return }
        releaseOnReady = true
        releaseTimer = setTimeout(hideDraft, 8000)
    })
}

watch(() => props.draft, draft => {
    if (!draft) { stopDraft(); return }
    if (!draftSource.value) {
        draftReady = false
        draftSource.value = draftDocument(draftBootstrap, nonce(), props.theme)
    }
    if (typeof draft.html === 'string' && draft.html !== pendingDraft) {
        pendingDraft = draft.html
        sendDraft()
    }
})
watch([() => props.scene?.html, () => props.active], ([, active], previous) => {
    if (!active) { live.value = null; return }
    if (!previous || props.scene?.html !== live.value?.source || !live.value) {
        build()
        if (live.value) live.value.source = props.scene.html
    }
}, { immediate: true })
watch(cameras, value => postLive({ type: 'cameras', cameras: value }))
watch(() => props.theme, theme => {
    postLive({ type: 'theme', theme })
    draftFrame.value?.contentWindow?.postMessage({ vls: 'host', type: 'theme', theme }, '*')
})
watch(() => props.previewId, id => postLive({ type: 'preview', id }))

onMounted(() => {
    window.addEventListener('message', receive)
    watchdog = window.setInterval(() => {
        // Pages report every two seconds. Stop a page that hangs, but never while the tab is hidden.
        if (!props.active || !liveReady.value || document.hidden || hung.value) { lastBeat = Date.now(); return }
        if (Date.now() - lastBeat > 12000) {
            hung.value = true
            live.value = null
            reportError({ kind: 'hang', message: '页面脚本长时间无响应（可能存在死循环或过重的计算）' })
        }
    }, 3000)
})
onBeforeUnmount(() => {
    window.removeEventListener('message', receive)
    clearInterval(watchdog)
    clearTimeout(releaseTimer)
})
defineExpose({ restart })
</script>

<style scoped>
.generated-page { position: relative; display: flex; flex: 1; min-width: 0; min-height: 0; overflow: hidden; background: var(--sw-stage, #0b1220); }
.page-frame { position: absolute; inset: 0; width: 100%; height: 100%; border: 0; display: block; opacity: 0; transition: opacity .45s ease; }
.page-frame.shown { opacity: 1; }
.page-frame.draft { pointer-events: none; }
.page-loading, .page-hung {
    position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; gap: 12px;
    color: var(--sw-muted, #93a4bf); font-size: 13px; letter-spacing: .02em;
}
.page-hung { flex-direction: column; color: var(--sw-text, #e6eefc); }
.page-hung button { padding: 6px 14px; border-radius: 8px; border: 1px solid var(--sw-border, #33415c); background: var(--sw-surface, #121c2f); color: inherit; cursor: pointer; }
.loading-orbit { position: relative; width: 26px; height: 26px; }
.loading-orbit i { position: absolute; inset: 0; border-radius: 50%; border: 2px solid transparent; border-top-color: var(--sw-accent, #4d8dff); animation: orbit 1.1s linear infinite; }
.loading-orbit i:nth-child(2) { inset: 4px; border-top-color: var(--sw-accent-2, #22d3ee); animation-duration: .8s; animation-direction: reverse; }
.loading-orbit i:nth-child(3) { inset: 9px; border-top-color: var(--sw-accent, #4d8dff); animation-duration: 1.4s; }
@keyframes orbit { to { transform: rotate(360deg); } }
.page-fade-enter-active, .page-fade-leave-active { transition: opacity .3s ease; }
.page-fade-enter-from, .page-fade-leave-to { opacity: 0; }
@media (prefers-reduced-motion: reduce) {
    .page-frame { transition: none; }
    .loading-orbit i { animation-duration: 3s; }
}
</style>
