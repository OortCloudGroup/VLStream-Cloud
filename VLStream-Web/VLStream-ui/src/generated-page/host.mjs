/** Host-side helpers for generated pages. Pages run in an opaque-origin iframe (sandbox="allow-scripts"
 * without allow-same-origin), so they cannot read the application's session, storage or DOM. The CSP below
 * blocks network access and external resources; pages only receive camera aliases and names. */
export const LIVE_CSP = [
    "default-src 'none'", "script-src 'unsafe-inline' 'unsafe-eval'", "style-src 'unsafe-inline'",
    'img-src data: blob:', 'font-src data:', 'media-src data: blob:', "connect-src 'none'", 'worker-src blob:',
    "frame-src 'none'", "object-src 'none'", "base-uri 'none'", "form-action 'none'", "manifest-src 'none'",
].join('; ')

const escapeScript = source => source.replace(/<\/(script)/gi, '<\\/$1')
const escapeJson = value => JSON.stringify(value).replace(/</g, '\\u003c').replace(/[\u2028\u2029]/g, c => '\\u' + c.charCodeAt(0).toString(16))
const lineCount = text => (text.match(/\n/g) || []).length

export const usesThree = html => /\bTHREE\b/.test(html)

/**
 * Builds the executable document. Host scripts are injected right after <head>; `lines` lets the host map
 * reported line numbers back to the saved source shown to the model.
 */
export function liveDocument(html, { runtime, three, cameras = [], theme = 'light' }) {
    const source = String(html || '').replace(/<meta[^>]+http-equiv\s*=\s*["']?refresh[^>]*>/gi, '')
    const scripts = '<script>' + escapeScript(runtime) + '</script>'
        + (usesThree(source) && three ? '<script>' + escapeScript(three) + '</script>' : '')
    const inject = '<meta http-equiv="Content-Security-Policy" content="' + LIVE_CSP + '">'
        + '<meta name="referrer" content="no-referrer">'
        + '<script type="application/json" id="vls-config">' + escapeJson({ cameras, theme }) + '</script>' + scripts
    const head = /<head(?:\s[^>]*)?>/i.exec(source)
    const root = head ? null : /<html(?:\s[^>]*)?>/i.exec(source)
    const anchor = head || root
    const at = anchor ? anchor.index + anchor[0].length : (/^\s*<!doctype[^>]*>/i.exec(source)?.[0].length || 0)
    return {
        html: source.slice(0, at) + inject + source.slice(at),
        lines: { at: lineCount(source.slice(0, at)) + 1, shift: lineCount(inject) },
    }
}

/** Maps a document line from an error report to the saved page, or 0 for host scripts. */
export function sourceLine(line, lines) {
    if (!line || !lines) return 0
    if (line <= lines.at) return line
    if (line > lines.at + lines.shift) return line - lines.shift
    return 0
}

export function excerpt(html, line, radius = 1) {
    if (!line) return ''
    const all = String(html || '').split('\n')
    return all.slice(Math.max(0, line - 1 - radius), line + radius).map(text => text.trim().slice(0, 180)).join('\n')
}

export function draftDocument(bootstrap, nonce, theme = 'light') {
    const csp = ["default-src 'none'", "script-src 'nonce-" + nonce + "'", "style-src 'unsafe-inline'",
        'img-src data:', 'font-src data:', "base-uri 'none'", "form-action 'none'"].join('; ')
    return '<!doctype html><html data-vls-theme="' + (theme === 'dark' ? 'dark' : 'light') + '"><head><meta charset="utf-8">'
        + '<meta http-equiv="Content-Security-Policy" content="' + csp + '"><meta name="referrer" content="no-referrer">'
        + '<style>html,body{height:100%;margin:0}canvas{background:linear-gradient(110deg,rgba(120,150,200,.08) 30%,rgba(120,170,255,.2) 50%,rgba(120,150,200,.08) 70%) 0 0/300% 100%;animation:vls-draft 1.6s linear infinite}'
        + '@keyframes vls-draft{to{background-position:-150% 0}}</style></head>'
        + '<body><script nonce="' + nonce + '">' + escapeScript(bootstrap) + '</script></body></html>'
}

export function authorizedAliases(bindings, devices) {
    const catalog = new Map(devices.map(device => [device.id, device]))
    const aliases = new Map()
    for (const [key, actual] of Object.entries(bindings || {})) {
        if (/^c\d+$/.test(key) && catalog.has(actual)) aliases.set(key, catalog.get(actual))
    }
    return aliases
}

export function publicCameras(aliases) {
    return Array.from(aliases, ([id, device]) => ({
        id,
        name: device.deviceName && device.deviceName !== device.id ? String(device.deviceName).slice(0, 80) : id,
        status: ['ONLINE', 'OFFLINE'].includes(device.status) ? device.status : 'UNKNOWN',
    }))
}

/** Built-in pages bind the first few playable cameras in the viewer's current directory. */
export function sampleBindings(devices, limit = 6) {
    const rank = device => device.status === 'ONLINE' ? 0 : device.status === 'OFFLINE' ? 2 : 1
    return Object.fromEntries([...devices].sort((a, b) => rank(a) - rank(b)).slice(0, limit)
        .map((device, index) => ['c' + (index + 1), device.id]))
}

export function visibleSlot(rect, width, height) {
    if (!rect || ![rect.x, rect.y, rect.width, rect.height].every(Number.isFinite)) return null
    const x = Math.max(0, rect.x), y = Math.max(0, rect.y)
    const w = Math.min(width, rect.x + rect.width) - x, h = Math.min(height, rect.y + rect.height) - y
    if (w < 160 || h < 100) return null
    return { left: x + 'px', top: y + 'px', width: w + 'px', height: h + 'px' }
}
