// First script inside a generated page. The page itself runs in an opaque-origin sandboxed iframe whose
// CSP blocks network access; this bootstrap is not a boundary against the page. It gives generated code a
// small, stable host API and reports problems so the conversation can repair them.
const host = window.parent
const post = (type, values = {}) => host.postMessage({ vls: 'page', type, ...values }, '*')
const configElement = document.getElementById('vls-config')
let config = {}
try { config = JSON.parse(configElement?.textContent || '{}') } catch { config = {} }
configElement?.remove()

let cameras = Array.isArray(config.cameras) ? config.cameras : []
let theme = config.theme === 'dark' ? 'dark' : 'light'
let previewing = null
let threeKit = null
const listeners = new Map([['theme', new Set()], ['cameras', new Set()], ['preview', new Set()]])

// Peer connections are not covered by connect-src. Nothing on these pages needs them.
for (const name of ['RTCPeerConnection', 'webkitRTCPeerConnection', 'RTCDataChannel']) {
    try { Object.defineProperty(window, name, { value: undefined, writable: false, configurable: false }) } catch { /* already absent */ }
}

const reported = new Set()
function report(kind, message, detail = {}) {
    const text = String(message ?? '未知错误').slice(0, 400)
    const key = kind + '|' + text + '|' + (detail.line || '')
    if (reported.has(key) || reported.size >= 20) return
    reported.add(key)
    post('error', { kind, message: text, line: detail.line || 0, column: detail.column || 0,
        stack: String(detail.stack || '').slice(0, 800) })
}
function emit(type, value) {
    for (const listener of listeners.get(type)) {
        try { listener(value) } catch (error) { report('script', error?.message || error, { stack: error?.stack }) }
    }
}

addEventListener('error', event => {
    if (event instanceof ErrorEvent) {
        report('script', event.message, { line: event.lineno, column: event.colno, stack: event.error?.stack })
    } else if (event.target && event.target !== window && event.target.tagName) {
        report('resource', '资源无法加载：<' + event.target.tagName.toLowerCase() + '>，页面运行在离线环境中')
    }
}, true)
addEventListener('unhandledrejection', event => {
    const reason = event.reason
    report('promise', reason?.message || reason, { stack: reason?.stack })
})
const nativeError = console.error.bind(console)
console.error = (...values) => {
    nativeError(...values)
    report('console', values.map(value => value instanceof Error ? value.message
        : typeof value === 'string' ? value : (() => { try { return JSON.stringify(value) } catch { return String(value) } })()).join(' '))
}

let lastInput = -Infinity
for (const type of ['pointerdown', 'pointerup', 'click', 'keydown', 'touchend']) {
    addEventListener(type, event => { if (event.isTrusted) lastInput = performance.now() }, true)
}
// Links cannot leave the sandbox; keep in-page anchors working.
addEventListener('click', event => {
    const link = event.target?.closest?.('a[href]')
    if (link && !link.getAttribute('href').startsWith('#')) event.preventDefault()
}, true)
window.open = () => null

function applyTheme() {
    const root = document.documentElement
    root.dataset.vlsTheme = theme
    root.style.colorScheme = theme
}
applyTheme()

const api = Object.freeze({
    get cameras() { return cameras.map(camera => ({ ...camera })) },
    get theme() { return theme },
    get previewing() { return previewing },
    get three() { return threeKit },
    preview(id) {
        const alias = String(id ?? '')
        if (!cameras.some(camera => camera.id === alias)) {
            post('preview-denied', { id: alias.slice(0, 40) })
            return false
        }
        const activated = navigator.userActivation ? navigator.userActivation.isActive : false
        if (!activated && performance.now() - lastInput > 3000) {
            console.warn('[VLS] VLS.preview 需要在点击等用户操作中调用')
            return false
        }
        post('preview', { id: alias })
        return true
    },
    closePreview() { post('close-preview') },
    on(type, listener) {
        const set = listeners.get(type)
        if (!set || typeof listener !== 'function') return () => {}
        set.add(listener)
        return () => set.delete(listener)
    },
    off(type, listener) { listeners.get(type)?.delete(listener) },
})
// Configurable so that `const VLS = window.VLS` in a page script stays legal; not writable, so pages
// cannot replace the camera list with invented devices.
Object.defineProperty(window, 'VLS', { value: api, enumerable: true, writable: false, configurable: true })
Object.defineProperty(window, '__vlsInstallThree', {
    value: kit => { if (!threeKit) threeKit = Object.freeze(kit) }, configurable: true,
})

addEventListener('message', event => {
    const data = event.data
    if (event.source !== host || !data || data.vls !== 'host') return
    if (data.type === 'theme' && (data.theme === 'dark' || data.theme === 'light') && data.theme !== theme) {
        theme = data.theme
        applyTheme()
        emit('theme', theme)
    } else if (data.type === 'cameras' && Array.isArray(data.cameras)) {
        cameras = data.cameras
        emit('cameras', api.cameras)
    } else if (data.type === 'preview') {
        previewing = typeof data.id === 'string' ? data.id : null
        document.documentElement.dataset.vlsPreviewing = previewing || ''
        emit('preview', previewing)
    }
})

// The host overlays the real player on an element marked data-vls-preview, when the page provides one.
let slotKey = ''
let slotTask = 0
function measureSlot() {
    slotTask = 0
    const element = document.querySelector('[data-vls-preview]')
    const box = element?.getBoundingClientRect()
    const visible = box && box.width > 0 && box.height > 0 && getComputedStyle(element).visibility !== 'hidden'
    const rect = visible ? { x: box.x, y: box.y, width: box.width, height: box.height } : null
    const key = JSON.stringify(rect)
    if (key !== slotKey) {
        slotKey = key
        post('slot', { rect })
    }
}
const scheduleSlot = () => { if (!slotTask) slotTask = setTimeout(() => requestAnimationFrame(measureSlot), 120) }
addEventListener('resize', scheduleSlot)
addEventListener('scroll', scheduleSlot, true)

function ready() {
    new MutationObserver(scheduleSlot).observe(document.documentElement, { subtree: true, childList: true, attributes: true,
        attributeFilter: ['class', 'style', 'hidden', 'data-vls-preview'] })
    requestAnimationFrame(() => requestAnimationFrame(() => {
        measureSlot()
        post('ready', { canvases: document.querySelectorAll('canvas').length })
    }))
}
if (document.readyState === 'complete') ready()
else addEventListener('load', ready, { once: true })
setInterval(() => post('beat'), 2000)
