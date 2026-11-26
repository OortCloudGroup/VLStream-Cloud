<template>
    <section class="scene-workspace" :data-theme="theme" :aria-label="$tp(mode === 'bim' ? 'BIM视图' : '拓扑视图')">
        <div ref="studio" class="scene-studio" :class="{ 'chat-open': panelOpen, 'is-narrow': narrow }">
            <section class="scene-stage" :inert="narrow && panelOpen ? '' : null">
                <header class="stage-bar">
                    <span class="mode-badge"><component :is="mode === 'bim' ? OfficeBuilding : Share" aria-hidden="true" /></span>
                    <div class="stage-name">
                        <h2>{{ $tp(mode === 'bim' ? 'BIM视图' : '拓扑视图') }}</h2>
                        <span class="scene-title" data-i18n-ignore>{{ shown.builtin ? $tp(shown.title) : shown.title }}</span>
                    </div>
                    <span class="status-pill" :class="statusTone" aria-live="polite"><i aria-hidden="true" />{{ $tp(status) }}</span>
                    <span v-if="revision" class="revision" :title="$tp('版本')">v{{ revision }}</span>
                    <div class="stage-actions">
                        <button v-if="undoCount" type="button" class="icon-button" :disabled="busy" :title="$tp('回退到上一版')"
                            :aria-label="$tp('回退到上一版')" @click="undoLast"><RefreshLeft aria-hidden="true" /></button>
                        <button type="button" class="icon-button" :title="$tp(theme === 'dark' ? '切换到白天模式' : '切换到夜间模式')"
                            :aria-label="$tp(theme === 'dark' ? '切换到白天模式' : '切换到夜间模式')" @click="toggleTheme">
                            <component :is="theme === 'dark' ? Sunny : Moon" aria-hidden="true" />
                        </button>
                        <button type="button" class="icon-button" :title="$tp(fullscreen ? '退出全屏' : '全屏')" :aria-label="$tp(fullscreen ? '退出全屏' : '全屏')"
                            @click="toggleFullscreen"><FullScreen aria-hidden="true" /></button>
                        <button v-show="!panelOpen" ref="opener" type="button" class="assistant-launch" :aria-controls="panelId" :aria-expanded="panelOpen"
                            @click="togglePanel(true)"><MagicStick aria-hidden="true" />{{ $tp('场景助手') }}</button>
                    </div>
                </header>
                <div ref="canvas" class="stage-canvas" :class="{ generating: phase === 'generating' }">
                    <VideoSquareGeneratedPage v-if="usePage" :scene="pageScene" :devices="devices" :draft="draft"
                        :active="active && visible" :theme="theme" :preview-id="point?.key || null"
                        @select="openPreview" @denied="onDenied" @close-preview="point = null" @runtime-error="onRuntimeError"
                        @slot="slotRect = $event" />
                    <VideoSquareScenePage v-else class="legacy-page" :block="shown.page || defaultPage" @select="openPreview">
                        <template #canvas>
                            <VideoSquareTopology v-if="mode === 'topology' && shown.nodes?.length" :scene="shown" :device-map="deviceMap"
                                :selected="point?.key" @select="openPreview" />
                            <VideoSquareBim v-else-if="mode === 'bim' && shown.buildings?.length" :scene="shown" :device-map="deviceMap"
                                :selected="point?.key" @select="openPreview" @clear="point = null" />
                        </template>
                        <template #preview><span /></template>
                    </VideoSquareScenePage>
                    <Transition name="preview-pop">
                        <aside v-if="active && selectedDevice" class="preview-card" :class="{ anchored: slotRect }" :style="previewStyle"
                            :aria-label="$tp('摄像头预览')">
                            <header class="preview-head" @pointerdown="startDrag">
                                <span class="live-dot" :class="{ off: selectedDevice.status === 'OFFLINE' }" aria-hidden="true" />
                                <strong data-i18n-ignore>{{ selectedDevice.deviceName }}</strong>
                                <span class="preview-badge">{{ $tp('已授权 · 实时') }}</span>
                                <button type="button" class="icon-button small" :aria-label="$tp('关闭预览')" @click="point = null"><Close /></button>
                            </header>
                            <div class="player">
                                <VideoSquareDevicePlayer v-if="visible && selectedDevice.status !== 'OFFLINE'" :key="selectedDevice.id" :device="selectedDevice" />
                                <span v-else>{{ $tp(selectedDevice.status === 'OFFLINE' ? '设备离线' : '预览已暂停') }}</span>
                            </div>
                            <footer>
                                {{ $tp(({ ONLINE: '在线', OFFLINE: '离线' })[selectedDevice.status] || '未知状态') }}
                                <template v-if="selectedDevice.protocols?.length"> · <span data-i18n-ignore>{{ selectedDevice.protocols.join(' / ') }}</span></template>
                                <template v-if="point.location"> · <span data-i18n-ignore>{{ point.location }}</span></template>
                            </footer>
                        </aside>
                    </Transition>
                    <Transition name="toast">
                        <div v-if="toast" class="stage-toast" role="status">{{ $tp(toast) }}</div>
                    </Transition>
                </div>
            </section>
            <div v-if="narrow && panelOpen" class="drawer-backdrop" aria-hidden="true" @click="togglePanel(false)" />
            <Transition name="scene-drawer">
                <aside v-show="panelOpen" :id="panelId" ref="panel" class="chat-panel" :inert="!panelOpen ? '' : null" :role="narrow ? 'dialog' : undefined"
                    :aria-modal="narrow && panelOpen ? 'true' : undefined" :aria-labelledby="panelId + '-title'" @keydown="panelKeys">
                    <header class="chat-head">
                        <span class="assistant-orb" :class="{ busy }" aria-hidden="true"><MagicStick /></span>
                        <div class="chat-title">
                            <h3 :id="panelId + '-title'">{{ $tp('场景助手') }}</h3>
                            <span class="provider" data-i18n-ignore>OortCloud<template v-if="modelLabel"> · {{ modelLabel }}</template></span>
                        </div>
                        <button type="button" class="icon-button" :aria-label="$tp('收起对话')" :title="$tp('收起对话')" :aria-controls="panelId"
                            :aria-expanded="panelOpen" @click="togglePanel(false)"><Fold aria-hidden="true" /></button>
                    </header>
                    <div ref="thread" class="chat-thread" @scroll="trackScroll">
                        <div v-if="!messages.length && phase !== 'loading'" class="starter">
                            <h4>{{ $tp(mode === 'bim' ? '描述你想要的三维场景' : '描述你想要的拓扑页面') }}</h4>
                            <p>{{ $tp('用自然语言决定页面长什么样、点击后发生什么。生成后可以继续对话，在当前页面上修改。') }}</p>
                            <button v-for="example in starters" :key="example" type="button" class="starter-card" @click="useSuggestion(example)">
                                <span>{{ $tp(example) }}</span><TopRight aria-hidden="true" />
                            </button>
                        </div>
                        <div class="chat-log" role="log" :aria-label="$tp('场景对话')" aria-live="polite" aria-relevant="additions text">
                            <article v-for="message in messages" :key="message.id" class="message" :class="[message.role, message.state]">
                                <div v-if="message.role === 'user'" class="bubble" data-i18n-ignore>{{ message.text }}</div>
                                <div v-else-if="message.role === 'system'" class="system-line">
                                    <RefreshLeft aria-hidden="true" />{{ $tp(message.text) }}
                                    <strong v-if="message.title" data-i18n-ignore>{{ message.title }}</strong>
                                </div>
                                <div v-else-if="message.state === 'pending'" class="card pending">
                                    <div class="card-head"><span class="spinner" aria-hidden="true" />{{ progressLabel(message.progress) }}
                                        <span class="elapsed">{{ Math.max(0, Math.round((now - message.progress.started) / 1000)) }}s</span></div>
                                    <p v-if="message.progress.notice" class="card-note" data-i18n-ignore>{{ message.progress.notice }}</p>
                                    <pre v-if="message.progress.tail" class="code-tail" aria-hidden="true">{{ message.progress.tail }}</pre>
                                    <div class="progress-track" aria-hidden="true"><i /></div>
                                </div>
                                <div v-else-if="message.state === 'issue'" class="card issue" role="alert">
                                    <div class="card-head"><Warning aria-hidden="true" />{{ $tp('页面运行时出现问题') }}<span class="count">{{ message.errors.length }}</span></div>
                                    <ul>
                                        <li v-for="(item, index) in message.errors.slice(0, 3)" :key="index">
                                            <span data-i18n-ignore>{{ item.message }}</span>
                                            <code v-if="item.code" data-i18n-ignore>{{ $tp('第 {line} 行', { line: item.line }) }}：{{ item.code.split('\n')[Math.min(1, item.code.split('\n').length - 1)] }}</code>
                                        </li>
                                    </ul>
                                    <div class="card-actions">
                                        <button type="button" class="primary" :disabled="busy" @click="repair(message)"><MagicStick aria-hidden="true" />{{ $tp('一键修复') }}</button>
                                        <button v-if="undoCount" type="button" :disabled="busy" @click="undoLast"><RefreshLeft aria-hidden="true" />{{ $tp('回退到上一版') }}</button>
                                    </div>
                                </div>
                                <div v-else class="card" :class="message.state">
                                    <div class="card-head">
                                        <component :is="message.state === 'saved' ? CircleCheck : Warning" aria-hidden="true" />{{ $tp(message.text) }}
                                    </div>
                                    <strong v-if="message.title" class="result-title" data-i18n-ignore>{{ message.title }}</strong>
                                    <div v-if="message.meta" class="meta">
                                        <span v-if="message.meta.edit">{{ message.meta.edit === 'patch' ? $tp('局部修改 {count} 处', { count: message.meta.edits || 1 }) : $tp('整页生成') }}</span>
                                        <span v-if="message.meta.model" data-i18n-ignore>{{ message.meta.model }}</span>
                                        <span v-if="message.meta.seconds">{{ message.meta.seconds }}s</span>
                                    </div>
                                    <div v-if="message.state === 'failed' && message.retry" class="card-actions">
                                        <button type="button" :disabled="busy" @click="fresh = !!message.retryFresh; submit(message.retry)">{{ $tp('重试') }}</button>
                                    </div>
                                </div>
                            </article>
                        </div>
                    </div>
                    <div class="chat-composer">
                        <div v-if="saved && phase === 'ready'" class="chips">
                            <button v-for="chip in followUps" :key="chip" type="button" @click="useSuggestion(chip)">{{ $tp(chip) }}</button>
                        </div>
                        <p v-if="error" class="notice error" role="alert">{{ $tp(error) }}</p>
                        <button v-if="needsReload" type="button" class="retry" @click="load">{{ $tp('重新载入') }}</button>
                        <p v-if="catalogError" class="notice" role="status">{{ $tp('设备目录暂不可用，已有视图仍可查看。') }}</p>
                        <p v-if="modelError" class="notice error" role="alert">{{ $tp(modelError) }}</p>
                        <form class="composer" @submit.prevent="submit()">
                            <textarea ref="field" v-model="prompt" maxlength="2000" rows="3" :aria-label="$tp('视图描述')"
                                :placeholder="$tp(fresh ? '描述要从头设计的新页面…' : saved ? '描述想增加的功能，或修改当前画面…' : '输入场景描述…')" @keydown="composerKeys" />
                            <div class="composer-tools">
                                <button v-if="saved" type="button" class="tool-select fresh-toggle" :aria-pressed="fresh" :disabled="busy"
                                    :title="$tp('下一次生成不参考当前页面，当前页面仍可回退')" @click="fresh = !fresh">
                                    <DocumentAdd aria-hidden="true" />{{ $tp('新页面') }}
                                </button>
                                <label v-if="devices.length" class="tool-select" :title="$tp('插入摄像头')"><VideoCamera aria-hidden="true" />
                                    <select value="" :aria-label="$tp('插入摄像头')" @change="insertCamera">
                                        <option value="">{{ $tp('插入摄像头') }}</option>
                                        <option v-for="device in devices" :key="device.id" :value="device.id">{{ device.deviceName }}</option>
                                    </select>
                                </label>
                                <button v-if="!choices" type="button" class="tool-select" :disabled="loadingModels || busy" @click="loadModels">
                                    <SetUp aria-hidden="true" />{{ $tp(loadingModels ? '正在读取模型' : '切换模型') }}
                                </button>
                                <label v-else class="tool-select"><SetUp aria-hidden="true" />
                                    <select v-model="selectedModel" :aria-label="$tp('本次生成模型')" :disabled="busy">
                                        <option value="">{{ $tp('租户默认模型') }} · {{ choices.defaultModel }}</option>
                                        <option v-for="model in choices.models" :key="model" :value="model">{{ model }}</option>
                                    </select>
                                </label>
                                <button v-if="phase === 'generating'" type="button" class="send stop" :title="$tp('停止生成')" :aria-label="$tp('停止生成')" @click="stop">
                                    <i aria-hidden="true" />
                                </button>
                                <button v-else type="submit" class="send" :disabled="phase !== 'ready' || !prompt.trim()" :title="$tp('发送描述')" :aria-label="$tp('发送描述')">
                                    <Top aria-hidden="true" />
                                </button>
                            </div>
                        </form>
                        <p class="key-hint">{{ $tp('Enter 发送 · Shift + Enter 换行') }}</p>
                    </div>
                </aside>
            </Transition>
        </div>
    </section>
</template>

<script setup>
import { computed, defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { CircleCheck, Close, DocumentAdd, Fold, FullScreen, MagicStick, Moon, OfficeBuilding, RefreshLeft, SetUp, Share, Sunny, Top,
    TopRight, VideoCamera, Warning } from '@element-plus/icons-vue'
import { authManager } from '@/utils/auth'
import { translatePhrase } from '@/i18n'
import { generateVideoSquareScene, getVideoSquareScene, getVideoSquareSceneModels, undoVideoSquareScene } from '@/api/videoSquareScenes'
import { codeTail, repairPrompt, sceneDeviceIds, sceneFollowUps, sceneStarters, restoredMessages } from '@/utils/videoSquareScenes.mjs'
import { sampleBindings } from '@/generated-page/host.mjs'
import topologySample from '@/generated-page/samples/topology.html?raw'
import bimSample from '@/generated-page/samples/bim.html?raw'
import VideoSquareDevicePlayer from './VideoSquareDevicePlayer.vue'
import VideoSquareTopology from './VideoSquareTopology.vue'
import VideoSquareBim from './VideoSquareBim.vue'
import VideoSquareScenePage from './VideoSquareScenePage.vue'

const VideoSquareGeneratedPage = defineAsyncComponent(() => import('./VideoSquareGeneratedPage.vue'))

const props = defineProps({
    mode: { type: String, required: true }, active: Boolean, devices: { type: Array, required: true }, catalogError: String,
})
const THEME_KEY = 'vls.videoSquare.sceneTheme'
const samples = {
    topology: { title: '园区视频链路 · 实时拓扑', html: topologySample },
    bim: { title: '智慧园区 · 三维 BIM', html: bimSample },
}
const defaultPage = { type: 'row', children: [{ type: 'canvas', grow: 4 }] }
const phase = ref('loading'), error = ref(''), needsReload = ref(false), field = ref(null)
const prompt = ref(''), revision = ref(0), saved = shallowRef(null), draft = shallowRef(null)
const studio = ref(null), panel = ref(null), opener = ref(null), thread = ref(null), canvas = ref(null)
const panelId = computed(() => 'video-square-chat-' + props.mode)
const panelOpen = ref(true), narrow = ref(false), messages = ref([]), fullscreen = ref(false)
const point = ref(null), visible = ref(true), slotRect = shallowRef(null), toast = ref(''), now = ref(Date.now())
const choices = shallowRef(null), selectedModel = ref(''), loadingModels = ref(false), modelError = ref('')
const runtimeErrors = shallowRef([]), fresh = ref(false)
const theme = ref(readTheme())
const drag = ref(null)
const deviceMap = computed(() => new Map(props.devices.map(device => [device.id, device])))
const selectedDevice = computed(() => deviceMap.value.get(point.value?.deviceId))
const builtin = computed(() => ({ format: 'web-page-v1', builtin: true, title: samples[props.mode].title, html: samples[props.mode].html,
    cameras: sampleBindings(props.devices) }))
const shown = computed(() => saved.value || builtin.value)
const isPage = computed(() => shown.value?.format === 'web-page-v1')
const usePage = computed(() => isPage.value || !!draft.value)
const pageScene = computed(() => isPage.value ? shown.value : { html: '', cameras: {} })
const undoCount = computed(() => saved.value?.undoCount || 0)
const busy = computed(() => phase.value === 'generating' || phase.value === 'loading')
const starters = computed(() => sceneStarters[props.mode])
const followUps = computed(() => sceneFollowUps[props.mode])
const modelLabel = computed(() => selectedModel.value || choices.value?.defaultModel || saved.value?.generatedWith || '')
const lastReplyFailed = computed(() => ['failed', 'stopped'].includes(messages.value.filter(m => m.role === 'assistant').at(-1)?.state))
const status = computed(() => phase.value === 'generating' ? '正在生成' : phase.value === 'loading' ? '正在载入'
    : phase.value === 'error' ? '需要重新载入' : runtimeErrors.value.length ? '页面运行有问题'
    : lastReplyFailed.value ? '上次生成失败' : saved.value ? '已保存至我的视图' : '内置示例')
const statusTone = computed(() => ({ generating: phase.value === 'generating', danger: phase.value === 'error' || lastReplyFailed.value,
    warn: !!runtimeErrors.value.length && phase.value !== 'generating', ok: !!saved.value && phase.value === 'ready' && !runtimeErrors.value.length }))
const previewStyle = computed(() => slotRect.value || (drag.value ? { left: drag.value.left + 'px', top: drag.value.top + 'px', right: 'auto', bottom: 'auto' } : null))
let request, identity, modelRequest, sizeObserver, clock = 0, toastTimer = 0, userStopped = false
let initializedSize = false, followLatest = true, nextMessageId = 0

function readTheme() {
    try {
        const value = localStorage.getItem(THEME_KEY)
        if (value === 'dark' || value === 'light') return value
    } catch { /* storage may be unavailable */ }
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}
function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
    try { localStorage.setItem(THEME_KEY, theme.value) } catch { /* per-viewer convenience only */ }
}
function toggleFullscreen() {
    if (document.fullscreenElement) document.exitFullscreen?.()
    else studio.value?.requestFullscreen?.().catch(() => {})
}
function syncFullscreen() { fullscreen.value = document.fullscreenElement === studio.value }
function notify(text) {
    toast.value = text
    clearTimeout(toastTimer)
    toastTimer = setTimeout(() => { toast.value = '' }, 2600)
}
function openPreview(selection) {
    if (!selection?.deviceId || !deviceMap.value.has(selection.deviceId)) return
    if (point.value?.deviceId !== selection.deviceId) drag.value = null
    point.value = { ...selection, location: selection.location || '' }
}
function onDenied() { notify('该点位为示意点位，未绑定已授权摄像头') }
function startDrag(event) {
    if (slotRect.value || event.button > 0 || event.target.closest('button')) return
    const card = event.currentTarget.parentElement, area = canvas.value.getBoundingClientRect(), box = card.getBoundingClientRect()
    const offset = { x: event.clientX - box.left, y: event.clientY - box.top }
    const move = moveEvent => {
        drag.value = {
            left: Math.min(Math.max(0, moveEvent.clientX - area.left - offset.x), area.width - box.width),
            top: Math.min(Math.max(0, moveEvent.clientY - area.top - offset.y), area.height - box.height),
        }
    }
    const end = () => { window.removeEventListener('pointermove', move); window.removeEventListener('pointerup', end) }
    window.addEventListener('pointermove', move)
    window.addEventListener('pointerup', end)
}

function onRuntimeError(errors) {
    runtimeErrors.value = errors
    if (shown.value.builtin || phase.value === 'generating') return
    const card = messages.value.find(message => message.state === 'issue' && message.revision === revision.value)
    if (card) card.errors = errors
    else addMessage({ role: 'assistant', state: 'issue', errors, revision: revision.value })
}
function repair(message) {
    submit(repairPrompt(message.errors, translatePhrase))
}
function progressLabel(progress) {
    if (!progress.edit) {
        const waited = (now.value - progress.started) / 1000
        return translatePhrase(waited > 25 ? '模型正在构思，复杂页面可能需要一两分钟…' : '正在理解需求…')
    }
    if (progress.edit === 'patch') return translatePhrase('正在修改当前页面 · 已应用 {count} 处', { count: progress.edits || 0 })
    return translatePhrase('正在编写页面 · {size} KB', { size: Math.max(1, Math.round(progress.chars / 1024)) })
}

function trackScroll() {
    if (thread.value && panelOpen.value && props.active) {
        const { scrollHeight, scrollTop, clientHeight } = thread.value
        followLatest = scrollHeight - scrollTop - clientHeight < 60
    }
}
function scrollToLatest() {
    nextTick(() => {
        if (followLatest && panelOpen.value && props.active && thread.value) thread.value.scrollTop = thread.value.scrollHeight
    })
}
function addMessage(message) {
    messages.value.push({ id: ++nextMessageId, ...message })
    scrollToLatest()
    return messages.value[messages.value.length - 1]
}
function togglePanel(open) {
    panelOpen.value = open
    nextTick(() => (open ? field.value : opener.value)?.focus({ preventScroll: true }))
    if (open) scrollToLatest()
}
function updateWidth(width) {
    if (!width) return
    narrow.value = width <= 860
    if (!initializedSize) {
        panelOpen.value = !narrow.value
        initializedSize = true
    }
}
function panelKeys(event) {
    if (event.isComposing) return
    if (event.key === 'Escape') {
        event.preventDefault()
        togglePanel(false)
    } else if (event.key === 'Tab' && narrow.value) {
        const items = panel.value.querySelectorAll('button:not(:disabled), textarea, select:not(:disabled)')
        const first = items[0], last = items[items.length - 1]
        if ((event.shiftKey && event.target === first) || (!event.shiftKey && event.target === last)) {
            event.preventDefault()
            ;(event.shiftKey ? last : first)?.focus()
        }
    }
}
function composerKeys(event) {
    if (event.key !== 'Enter' || event.shiftKey || event.isComposing || event.keyCode === 229) return
    event.preventDefault()
    submit()
}
async function loadModels() {
    if (checkIdentity()) await load()
    const snapshot = authManager.getIdentitySnapshot()
    modelRequest?.abort()
    const local = new AbortController()
    modelRequest = local
    loadingModels.value = true
    modelError.value = ''
    try {
        const response = await getVideoSquareSceneModels({ signal: local.signal, localAuthToken: snapshot.token, platformAuthToken: snapshot.platformToken })
        if (local.signal.aborted || !authManager.isIdentityCurrent(snapshot)) return
        if (!Array.isArray(response.data?.models)) throw new Error('模型列表响应无效')
        choices.value = response.data
    } catch (failure) {
        if (!local.signal.aborted && authManager.isIdentityCurrent(snapshot)) modelError.value = failure.message || '模型列表响应无效'
    } finally {
        if (modelRequest === local) loadingModels.value = false
    }
}
function apply(value) {
    saved.value = value.scene
    revision.value = value.revision
    runtimeErrors.value = []
}
function begin() {
    request?.abort()
    request = new AbortController()
    identity = authManager.getIdentitySnapshot()
    error.value = ''
    needsReload.value = false
    userStopped = false
    return { signal: request.signal, localAuthToken: identity.token, platformAuthToken: identity.platformToken }
}
function current(options) { return !options.signal.aborted && authManager.isIdentityCurrent(identity) }
async function load() {
    if (phase.value === 'generating') return
    checkIdentity()
    phase.value = 'loading'
    const options = begin()
    try {
        const response = await getVideoSquareScene(props.mode, options)
        if (!current(options)) return
        if (!Number.isInteger(response?.data?.revision)) throw new Error('已保存的视图无法读取')
        if (response.data.revision !== revision.value || !messages.value.length) {
            messages.value = restoredMessages(response.data).map(message => ({ id: ++nextMessageId, ...message }))
            followLatest = true
            scrollToLatest()
        }
        apply(response.data)
        phase.value = 'ready'
    } catch (failure) {
        if (current(options)) {
            phase.value = 'error'
            error.value = failure.response?.data?.msg || failure.message || '已保存的视图无法读取'
            needsReload.value = true
        }
    }
}
function useSuggestion(text) {
    prompt.value = translatePhrase(text)
    field.value?.focus()
}
async function submit(text) {
    const instruction = String(text ?? prompt.value).trim().slice(0, 2000)
    if (phase.value !== 'ready' || !instruction) return
    if (checkIdentity()) { await load(); return }
    const options = begin()
    phase.value = 'generating'
    point.value = null
    followLatest = true
    addMessage({ role: 'user', text: instruction, state: 'sent' })
    const reply = addMessage({ role: 'assistant', state: 'pending', text: '', progress: { started: Date.now(), chars: 0, edit: '', edits: 0, notice: '', tail: '' } })
    if (prompt.value.trim() === instruction) prompt.value = ''
    if (narrow.value && panelOpen.value) togglePanel(false)
    const startFresh = fresh.value
    fresh.value = false
    try {
        const result = await generateVideoSquareScene(props.mode, {
            prompt: instruction, model: selectedModel.value || undefined, revision: revision.value, fresh: startFresh || undefined,
            deviceIds: sceneDeviceIds(props.devices, instruction, startFresh ? null : saved.value),
        }, options, scene => {
            if (!current(options)) return
            draft.value = scene
            Object.assign(reply.progress, { chars: scene.chars || reply.progress.chars, edit: scene.edit || reply.progress.edit,
                edits: scene.edits || 0, notice: scene.notice || reply.progress.notice, tail: codeTail(scene.html) })
        })
        if (!current(options)) return
        apply(result)
        Object.assign(reply, { state: 'saved', text: '已生成并保存视图。', title: result.scene.title, meta: {
            edit: result.scene.edit, edits: result.scene.edits, model: result.scene.generatedWith,
            seconds: Math.round((Date.now() - reply.progress.started) / 1000) } })
    } catch (failure) {
        if (userStopped) {
            Object.assign(reply, { state: 'stopped', text: '已停止生成，页面保持不变。' })
        } else if (current(options)) {
            Object.assign(reply, { state: 'failed', text: failure.message || '视图生成失败', retry: instruction, retryFresh: startFresh })
            needsReload.value = failure.reload === true || failure.code === 409 || failure.code === 'SCENE_STREAM_INTERRUPTED' || failure instanceof TypeError
        }
    } finally {
        if (current(options) || userStopped) {
            draft.value = null
            phase.value = needsReload.value ? 'error' : 'ready'
            userStopped = false
            scrollToLatest()
        }
    }
}
function stop() {
    if (phase.value !== 'generating') return
    userStopped = true
    request?.abort()
}
async function undoLast() {
    if (phase.value !== 'ready' || !undoCount.value) return
    if (checkIdentity()) { await load(); return }
    const options = begin()
    phase.value = 'loading'
    try {
        const response = await undoVideoSquareScene(props.mode, { revision: revision.value }, options)
        if (!current(options)) return
        apply(response.data)
        point.value = null
        addMessage({ role: 'system', text: '已回退到上一版', title: response.data.scene?.title || '' })
        phase.value = 'ready'
    } catch (failure) {
        if (!current(options)) return
        error.value = failure.response?.data?.msg || failure.message || '回退失败'
        needsReload.value = failure.code === 409 || failure.response?.status === 409
        phase.value = needsReload.value ? 'error' : 'ready'
    }
}
function checkIdentity() {
    if (!identity || authManager.isIdentityCurrent(identity)) return false
    request?.abort()
    modelRequest?.abort()
    identity = undefined
    choices.value = null
    selectedModel.value = modelError.value = ''
    saved.value = draft.value = point.value = null
    prompt.value = error.value = ''
    messages.value = []
    runtimeErrors.value = []
    followLatest = true
    needsReload.value = false
    revision.value = 0
    phase.value = 'loading'
    return true
}
function refreshIdentity() {
    visible.value = document.visibilityState !== 'hidden'
    if (checkIdentity() && props.active) load()
}
function insertCamera(event) {
    const device = deviceMap.value.get(event.target.value)
    if (device) prompt.value = (prompt.value + (prompt.value ? ' ' : '') + device.deviceName).slice(0, 2000)
    event.target.value = ''
    field.value?.focus()
}
watch(() => props.active, active => {
    if (!active) { point.value = null; return }
    if (checkIdentity() || !needsReload.value) load()
    scrollToLatest()
}, { immediate: true })
watch(selectedDevice, device => { if (!device) point.value = null })
watch(phase, value => {
    clearInterval(clock)
    if (value === 'generating') clock = setInterval(() => { now.value = Date.now() }, 1000)
})
onMounted(() => {
    updateWidth(studio.value.getBoundingClientRect().width)
    sizeObserver = new ResizeObserver(([entry]) => updateWidth(entry.contentRect.width))
    sizeObserver.observe(studio.value)
    refreshIdentity()
    document.addEventListener('visibilitychange', refreshIdentity)
    document.addEventListener('fullscreenchange', syncFullscreen)
    window.addEventListener('storage', refreshIdentity)
    window.addEventListener('focus', refreshIdentity)
})
onBeforeUnmount(() => {
    sizeObserver?.disconnect()
    request?.abort()
    modelRequest?.abort()
    clearInterval(clock)
    clearTimeout(toastTimer)
    document.removeEventListener('visibilitychange', refreshIdentity)
    document.removeEventListener('fullscreenchange', syncFullscreen)
    window.removeEventListener('storage', refreshIdentity)
    window.removeEventListener('focus', refreshIdentity)
})
</script>

<style scoped>
.scene-workspace {
    --sw-bg: #e9eff7; --sw-stage: #f2f6fc; --sw-surface: rgba(255, 255, 255, .92); --sw-surface-2: #f4f7fc; --sw-raised: #ffffff;
    --sw-border: #d9e2ee; --sw-text: #13223a; --sw-muted: #5a6a80; --sw-faint: #8796ab;
    --sw-accent: #2f6bff; --sw-accent-2: #00b8d9; --sw-accent-soft: rgba(47, 107, 255, .1);
    --sw-ok: #0f9f6e; --sw-warn: #c27a0e; --sw-danger: #c2412d; --sw-shadow: 0 10px 30px rgba(20, 50, 100, .1);
    --sw-code: #1d2b45; --sw-code-text: #a7c4ff;
    container: scene-workspace / inline-size;
    display: flex; flex: 1; min-width: 0; min-height: 0;
    padding: var(--toolbar-inset, 82px) 12px 12px; box-sizing: border-box;
    color: var(--sw-text); background: var(--sw-bg); overflow: hidden;
    transition: background .4s ease, color .3s ease;
}
.scene-workspace[data-theme="dark"] {
    --sw-bg: #060b16; --sw-stage: #0b1220; --sw-surface: rgba(14, 23, 41, .9); --sw-surface-2: rgba(22, 35, 60, .72); --sw-raised: #111c31;
    --sw-border: rgba(120, 160, 230, .18); --sw-text: #e6eefc; --sw-muted: #9badc8; --sw-faint: #6f819d;
    --sw-accent: #4d8dff; --sw-accent-2: #22d3ee; --sw-accent-soft: rgba(77, 141, 255, .14);
    --sw-ok: #2dd4a7; --sw-warn: #f5b041; --sw-danger: #ff7a66; --sw-shadow: 0 12px 40px rgba(0, 0, 0, .45);
    --sw-code: #070e1c; --sw-code-text: #8fb6ff;
}
.scene-studio {
    position: relative; display: flex; flex: 1; min-width: 0; min-height: 0; overflow: hidden;
    border: 1px solid var(--sw-border); border-radius: 14px; background: var(--sw-stage); box-shadow: var(--sw-shadow);
}
.scene-studio:fullscreen { border-radius: 0; border: 0; }
.scene-stage { position: relative; display: flex; flex-direction: column; flex: 1; min-width: 0; min-height: 0; }
.stage-bar, .chat-head, .stage-name, .status-pill, .stage-actions, .card-head, .preview-head, .composer-tools, .chips, .system-line, .meta, .card-actions { display: flex; align-items: center; }
.stage-bar {
    flex: none; gap: 12px; min-height: 52px; padding: 8px 12px 8px 14px; box-sizing: border-box;
    border-bottom: 1px solid var(--sw-border); background: var(--sw-surface); backdrop-filter: blur(16px); z-index: 2;
}
.mode-badge, .assistant-orb { display: inline-grid; flex: none; place-items: center; width: 32px; height: 32px; border-radius: 10px; color: #fff;
    background: linear-gradient(135deg, var(--sw-accent), var(--sw-accent-2)); box-shadow: 0 6px 16px color-mix(in srgb, var(--sw-accent) 35%, transparent); }
.mode-badge svg, .assistant-orb svg { width: 17px; height: 17px; }
.stage-name { gap: 12px; min-width: 0; }
.stage-name h2, .chat-head h3, .starter h4 { margin: 0; color: var(--sw-text); font-size: 15px; font-weight: 650; line-height: 1.3; white-space: nowrap; }
.scene-title { padding-left: 12px; border-left: 1px solid var(--sw-border); color: var(--sw-muted); font-size: 12.5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.status-pill { flex: none; gap: 7px; height: 26px; padding: 0 10px; margin-left: auto; border-radius: 999px; font-size: 12px; color: var(--sw-muted);
    background: var(--sw-surface-2); border: 1px solid var(--sw-border); white-space: nowrap; }
.status-pill i { width: 7px; height: 7px; border-radius: 50%; background: var(--sw-faint); }
.status-pill.ok i { background: var(--sw-ok); box-shadow: 0 0 0 3px color-mix(in srgb, var(--sw-ok) 22%, transparent); }
.status-pill.warn { color: var(--sw-warn); } .status-pill.warn i { background: var(--sw-warn); }
.status-pill.danger { color: var(--sw-danger); } .status-pill.danger i { background: var(--sw-danger); }
.status-pill.generating { color: var(--sw-accent); border-color: color-mix(in srgb, var(--sw-accent) 40%, transparent); }
.status-pill.generating i { background: var(--sw-accent); animation: sw-pulse 1.2s ease-in-out infinite; }
.revision { flex: none; padding: 2px 7px; border-radius: 6px; font-size: 11px; font-variant-numeric: tabular-nums; color: var(--sw-muted); background: var(--sw-surface-2); }
.stage-actions { flex: none; gap: 4px; }
button, select, textarea { font: inherit; color: inherit; }
button { cursor: pointer; }
button:disabled { cursor: default; opacity: .5; }
button:focus-visible, select:focus-visible, textarea:focus-visible { outline: 2px solid var(--sw-accent); outline-offset: 2px; }
.icon-button { display: inline-grid; place-items: center; width: 34px; height: 34px; padding: 0; border: 1px solid transparent; border-radius: 10px;
    background: transparent; color: var(--sw-muted); transition: background .2s, color .2s, border-color .2s; }
.icon-button svg { width: 17px; height: 17px; }
.icon-button:hover:not(:disabled) { background: var(--sw-accent-soft); color: var(--sw-accent); }
.icon-button.small { width: 26px; height: 26px; border-radius: 8px; }
.icon-button.small svg { width: 14px; height: 14px; }
.assistant-launch { display: inline-flex; align-items: center; gap: 7px; height: 34px; padding: 0 14px; margin-left: 4px; border: 0; border-radius: 10px; color: #fff; font-size: 13px; font-weight: 600;
    background: linear-gradient(120deg, var(--sw-accent), var(--sw-accent-2)); box-shadow: 0 6px 18px color-mix(in srgb, var(--sw-accent) 40%, transparent); }
.assistant-launch svg { width: 16px; height: 16px; }
.assistant-launch:hover { filter: brightness(1.08); }
.stage-canvas { position: relative; display: flex; flex: 1; min-height: 0; min-width: 0; overflow: hidden; background: var(--sw-stage); }
.stage-canvas > :first-child { flex: 1; }
.stage-canvas.generating::after {
    content: ""; position: absolute; inset: 0; z-index: 3; pointer-events: none; padding: 2px;
    background: conic-gradient(from var(--sw-angle), transparent 0 55%, var(--sw-accent) 70%, var(--sw-accent-2) 80%, transparent 92%);
    -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0); -webkit-mask-composite: xor; mask-composite: exclude;
    animation: sw-spin 2.6s linear infinite;
}
@property --sw-angle { syntax: '<angle>'; inherits: false; initial-value: 0deg; }
@keyframes sw-spin { to { --sw-angle: 360deg; } }
@keyframes sw-pulse { 50% { opacity: .35; transform: scale(.8); } }
.legacy-page { flex: 1; min-height: 0; overflow: auto; padding: 16px; background: var(--sw-stage); color: var(--sw-text); }
.preview-card {
    position: absolute; right: 16px; bottom: 16px; z-index: 4; width: min(380px, calc(100% - 32px)); display: flex; flex-direction: column;
    border-radius: 14px; overflow: hidden; background: var(--sw-surface); border: 1px solid var(--sw-border); box-shadow: 0 18px 50px rgba(0, 0, 0, .3); backdrop-filter: blur(16px);
}
.preview-card.anchored { right: auto; bottom: auto; width: auto; border-radius: 10px; }
.preview-card.anchored footer, .preview-card.anchored .preview-badge { display: none; }
.preview-card.anchored .preview-head { position: absolute; inset: 0 0 auto; z-index: 1; cursor: default; color: #fff; background: linear-gradient(rgb(0 0 0 / 65%), transparent); }
.preview-card.anchored .preview-head .icon-button { color: #fff; }
.preview-card.anchored .player { height: 100%; aspect-ratio: auto; }
.preview-head { gap: 8px; padding: 9px 10px 9px 12px; cursor: grab; user-select: none; touch-action: none; }
.preview-head strong { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.preview-badge { flex: none; margin-left: auto; padding: 2px 8px; border-radius: 999px; font-size: 11px; color: var(--sw-ok); background: color-mix(in srgb, var(--sw-ok) 14%, transparent); }
.live-dot { flex: none; width: 8px; height: 8px; border-radius: 50%; background: var(--sw-ok); animation: sw-live 2s ease-out infinite; }
.live-dot.off { background: var(--sw-faint); animation: none; }
@keyframes sw-live { 0% { box-shadow: 0 0 0 0 color-mix(in srgb, var(--sw-ok) 60%, transparent); } 100% { box-shadow: 0 0 0 8px transparent; } }
.player { display: grid; place-items: center; width: 100%; aspect-ratio: 16 / 9; background: #05080f; color: #cfd8e6; font-size: 12px; }
.preview-card footer { padding: 8px 12px 10px; font-size: 12px; color: var(--sw-muted); overflow-wrap: anywhere; }
.preview-pop-enter-active, .preview-pop-leave-active { transition: opacity .25s ease, transform .3s cubic-bezier(.2, .8, .2, 1); }
.preview-pop-enter-from, .preview-pop-leave-to { opacity: 0; transform: translateY(14px) scale(.97); }
.stage-toast { position: absolute; left: 50%; bottom: 20px; z-index: 5; transform: translateX(-50%); padding: 8px 16px; border-radius: 999px; font-size: 12.5px;
    background: var(--sw-surface); border: 1px solid var(--sw-border); box-shadow: var(--sw-shadow); backdrop-filter: blur(12px); white-space: nowrap; }
.toast-enter-active, .toast-leave-active { transition: opacity .25s, transform .25s; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, 10px); }

.chat-panel {
    z-index: 4; display: flex; flex-direction: column; flex: 0 0 clamp(300px, 27%, 380px); min-width: 0; min-height: 0; box-sizing: border-box;
    border-left: 1px solid var(--sw-border); background: var(--sw-surface); backdrop-filter: blur(18px); text-align: left;
}
.scene-drawer-enter-active, .scene-drawer-leave-active { overflow: hidden; transition: flex-basis .32s cubic-bezier(.2, .8, .2, 1), transform .32s cubic-bezier(.2, .8, .2, 1), opacity .22s ease; }
.scene-drawer-enter-from, .scene-drawer-leave-to { flex-basis: 0; transform: translateX(24px); opacity: 0; }
.scene-drawer-leave-active { pointer-events: none; }
.is-narrow .scene-drawer-enter-from, .is-narrow .scene-drawer-leave-to { transform: translateX(100%); }
.chat-head { gap: 10px; flex: none; min-height: 60px; padding: 10px 12px 10px 14px; box-sizing: border-box; border-bottom: 1px solid var(--sw-border); }
.assistant-orb { width: 34px; height: 34px; border-radius: 50%; position: relative; }
.assistant-orb::after { content: ""; position: absolute; inset: -3px; border-radius: 50%; border: 2px solid transparent; border-top-color: var(--sw-accent-2); opacity: 0; }
.assistant-orb.busy::after { opacity: 1; animation: sw-turn 1s linear infinite; }
@keyframes sw-turn { to { transform: rotate(360deg); } }
.chat-title { min-width: 0; flex: 1; }
.provider { display: block; margin-top: 2px; color: var(--sw-muted); font-size: 11.5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chat-thread { flex: 1; min-height: 0; overflow: auto; overscroll-behavior: contain; scrollbar-width: thin; padding: 16px 14px; }
.starter h4 { font-size: 16px; white-space: normal; }
.starter p { margin: 6px 0 14px; color: var(--sw-muted); font-size: 13px; line-height: 1.65; }
.starter-card { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; width: 100%; margin-bottom: 8px; padding: 11px 12px; text-align: left;
    border: 1px solid var(--sw-border); border-radius: 12px; background: var(--sw-surface-2); font-size: 12.5px; line-height: 1.6; color: var(--sw-text);
    transition: border-color .2s, transform .2s, box-shadow .2s; }
.starter-card svg { flex: none; width: 14px; height: 14px; margin-top: 3px; color: var(--sw-accent); }
.starter-card:hover { border-color: color-mix(in srgb, var(--sw-accent) 55%, transparent); transform: translateY(-1px); box-shadow: 0 8px 20px color-mix(in srgb, var(--sw-accent) 15%, transparent); }
.chat-log { display: flex; flex-direction: column; gap: 12px; }
.message { display: flex; flex-direction: column; align-items: flex-start; animation: sw-in .3s ease both; }
.message.user { align-items: flex-end; }
@keyframes sw-in { from { opacity: 0; transform: translateY(6px); } }
.bubble { max-width: 92%; padding: 9px 12px; border-radius: 14px 14px 4px 14px; font-size: 13px; line-height: 1.6; white-space: pre-wrap; overflow-wrap: anywhere;
    color: #fff; background: linear-gradient(135deg, var(--sw-accent), color-mix(in srgb, var(--sw-accent) 70%, var(--sw-accent-2))); }
.system-line { gap: 6px; align-self: center; padding: 4px 10px; border-radius: 999px; font-size: 12px; color: var(--sw-muted); background: var(--sw-surface-2); }
.system-line svg { width: 13px; height: 13px; }
.card { width: 100%; box-sizing: border-box; padding: 10px 12px; border-radius: 4px 14px 14px 14px; font-size: 13px; line-height: 1.55;
    background: var(--sw-surface-2); border: 1px solid var(--sw-border); }
.card-head { gap: 7px; font-weight: 600; }
.card-head svg { width: 15px; height: 15px; flex: none; }
.card.saved .card-head svg { color: var(--sw-ok); }
.card.failed, .card.stopped { border-color: color-mix(in srgb, var(--sw-danger) 35%, transparent); }
.card.failed .card-head, .card.stopped .card-head { font-weight: 500; color: var(--sw-danger); overflow-wrap: anywhere; }
.card.stopped .card-head { color: var(--sw-muted); }
.result-title { display: block; margin-top: 4px; font-size: 13.5px; overflow-wrap: anywhere; }
.meta { flex-wrap: wrap; gap: 6px; margin-top: 7px; }
.meta span { padding: 1px 7px; border-radius: 6px; font-size: 11px; color: var(--sw-muted); background: var(--sw-raised); border: 1px solid var(--sw-border); }
.card.pending { position: relative; overflow: hidden; border-color: color-mix(in srgb, var(--sw-accent) 35%, transparent); }
.card.pending .card-head { font-weight: 500; color: var(--sw-text); }
.elapsed { margin-left: auto; font-size: 11.5px; color: var(--sw-muted); font-variant-numeric: tabular-nums; font-weight: 400; }
.spinner { width: 13px; height: 13px; border-radius: 50%; border: 2px solid color-mix(in srgb, var(--sw-accent) 25%, transparent); border-top-color: var(--sw-accent); animation: sw-turn .8s linear infinite; }
.card-note { margin: 7px 0 0; font-size: 12px; color: var(--sw-warn); }
.code-tail { margin: 8px 0 0; padding: 8px 10px; max-height: 92px; overflow: hidden; border-radius: 8px; font: 11px/1.55 ui-monospace, SFMono-Regular, Consolas, monospace;
    color: var(--sw-code-text); background: var(--sw-code); white-space: pre; mask-image: linear-gradient(transparent, #000 40%); }
.progress-track { position: relative; height: 2px; margin-top: 9px; border-radius: 2px; overflow: hidden; background: color-mix(in srgb, var(--sw-accent) 15%, transparent); }
.progress-track i { position: absolute; inset: 0 auto 0 0; width: 40%; border-radius: inherit; background: linear-gradient(90deg, transparent, var(--sw-accent), var(--sw-accent-2)); animation: sw-slide 1.4s ease-in-out infinite; }
@keyframes sw-slide { from { transform: translateX(-100%); } to { transform: translateX(260%); } }
.card.issue { border-color: color-mix(in srgb, var(--sw-warn) 45%, transparent); background: color-mix(in srgb, var(--sw-warn) 8%, var(--sw-surface-2)); }
.card.issue .card-head svg { color: var(--sw-warn); }
.count { margin-left: auto; min-width: 20px; padding: 0 6px; border-radius: 999px; text-align: center; font-size: 11px; color: #fff; background: var(--sw-warn); }
.card.issue ul { margin: 7px 0 0; padding-left: 16px; font-size: 12px; color: var(--sw-text); }
.card.issue li { margin-bottom: 4px; overflow-wrap: anywhere; }
.card.issue code { display: block; margin-top: 3px; padding: 3px 7px; border-radius: 6px; font-size: 11px; white-space: pre-wrap; color: var(--sw-code-text); background: var(--sw-code); }
.card-actions { gap: 6px; margin-top: 9px; flex-wrap: wrap; }
.card-actions button { display: inline-flex; align-items: center; gap: 5px; height: 28px; padding: 0 11px; border-radius: 8px; font-size: 12px; border: 1px solid var(--sw-border); background: var(--sw-raised); }
.card-actions button svg { width: 13px; height: 13px; }
.card-actions button.primary { border: 0; color: #fff; background: linear-gradient(120deg, var(--sw-accent), var(--sw-accent-2)); }
.chat-composer { flex: none; padding: 10px 12px 12px; border-top: 1px solid var(--sw-border); }
.chips { gap: 6px; flex-wrap: nowrap; overflow-x: auto; margin-bottom: 8px; scrollbar-width: none; mask-image: linear-gradient(90deg, #000 85%, transparent); }
.chips button { flex: none; height: 26px; padding: 0 10px; border-radius: 999px; font-size: 12px; color: var(--sw-text); border: 1px solid var(--sw-border); background: var(--sw-surface-2); white-space: nowrap; transition: border-color .2s, color .2s; }
.chips button:hover { border-color: var(--sw-accent); color: var(--sw-accent); }
.composer { border: 1px solid var(--sw-border); border-radius: 14px; background: var(--sw-raised); transition: border-color .2s, box-shadow .2s; }
.composer:focus-within { border-color: color-mix(in srgb, var(--sw-accent) 70%, transparent); box-shadow: 0 0 0 3px color-mix(in srgb, var(--sw-accent) 18%, transparent); }
textarea { display: block; box-sizing: border-box; width: 100%; min-height: 66px; max-height: 140px; padding: 11px 12px 4px; border: 0; border-radius: 14px; resize: none;
    color: var(--sw-text); background: transparent; font-size: 13px; line-height: 1.6; }
textarea:focus-visible { outline: none; }
textarea::placeholder { color: var(--sw-faint); }
.composer-tools { gap: 2px; min-height: 38px; padding: 0 6px 6px 8px; overflow-x: auto; scrollbar-width: none; }
.tool-select select { max-width: 92px; }
.tool-select { display: inline-flex; flex: none; align-items: center; gap: 4px; min-width: 0; height: 28px; padding: 0 6px; border: 0; border-radius: 8px; font-size: 12px; white-space: nowrap; color: var(--sw-muted); background: transparent; }
.tool-select:hover:not(:disabled) { background: var(--sw-accent-soft); color: var(--sw-accent); }
.fresh-toggle[aria-pressed="true"] { color: var(--sw-accent); background: var(--sw-accent-soft); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--sw-accent) 45%, transparent); }
.tool-select svg { flex: none; width: 14px; height: 14px; }
.tool-select select { max-width: 112px; min-width: 0; border: 0; font-size: 12px; color: inherit; background: transparent; }
.tool-select select option { color: #13223a; }
.send { flex: none; display: inline-grid; place-items: center; width: 32px; height: 32px; margin-left: auto; border: 0; border-radius: 10px; color: #fff;
    background: linear-gradient(135deg, var(--sw-accent), var(--sw-accent-2)); box-shadow: 0 4px 12px color-mix(in srgb, var(--sw-accent) 35%, transparent); }
.send svg { width: 16px; height: 16px; }
.send:disabled { background: var(--sw-surface-2); color: var(--sw-faint); box-shadow: none; opacity: 1; }
.send.stop { background: var(--sw-danger); box-shadow: none; }
.send.stop i { width: 10px; height: 10px; border-radius: 2px; background: #fff; }
.key-hint { margin: 6px 2px 0; color: var(--sw-faint); font-size: 11px; text-align: right; }
.notice { margin: 0 0 8px; color: var(--sw-muted); font-size: 12px; line-height: 1.5; overflow-wrap: anywhere; }
.notice.error { color: var(--sw-danger); }
.retry { margin: 0 0 8px; padding: 4px 10px; border: 1px solid var(--sw-border); border-radius: 8px; background: var(--sw-raised); color: var(--sw-accent); font-size: 12px; }
.drawer-backdrop { position: absolute; inset: 0; z-index: 3; background: rgb(8 16 32 / 40%); backdrop-filter: blur(2px); }
.is-narrow .chat-panel { position: absolute; inset: 0 0 0 auto; width: min(370px, calc(100% - 24px)); box-shadow: -10px 0 40px rgb(0 0 0 / 25%); }
@container scene-workspace (max-width: 620px) {
    .stage-bar { gap: 8px; padding-inline: 8px; }
    .stage-name { flex-direction: column; align-items: flex-start; gap: 1px; }
    .scene-title { border: 0; padding: 0; font-size: 11px; max-width: 100%; }
    .revision, .status-pill { display: none; }
}
@media (max-width: 700px) {
    .scene-workspace { min-height: 480px; height: min(76dvh, 760px); padding: 0 6px 6px; flex-basis: auto; }
}
@media (prefers-reduced-motion: reduce) {
    .scene-drawer-enter-active, .scene-drawer-leave-active, .preview-pop-enter-active, .preview-pop-leave-active { transition: none; }
    .stage-canvas.generating::after, .progress-track i, .live-dot, .status-pill.generating i, .assistant-orb.busy::after, .message { animation: none; }
}
</style>
