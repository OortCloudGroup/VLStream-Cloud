import morphdom from 'morphdom'

// Streaming drafts are inert: this frame's CSP only lets the nonced bootstrap run, so scripts, inline
// handlers and javascript: URLs in the partial page never execute. Morphing keeps already drawn parts
// steady while the rest of the page arrives.
const host = window.parent
let styles = [], styleKey = ''
const kept = new Set(['lang', 'dir', 'class', 'style'])
function copyAttributes(from, to) {
    for (const { name } of Array.from(to.attributes)) {
        if ((kept.has(name) || name.startsWith('data-')) && !name.startsWith('data-vls') && !from.hasAttribute(name)) to.removeAttribute(name)
    }
    for (const { name, value } of Array.from(from.attributes)) {
        if ((kept.has(name) || name.startsWith('data-')) && !name.startsWith('data-vls')) to.setAttribute(name, value)
    }
}
function render(html) {
    const page = new DOMParser().parseFromString(html, 'text/html')
    for (const element of page.querySelectorAll('script,iframe,object,embed,link,meta,base,form,audio,video')) element.remove()
    const found = Array.from(page.querySelectorAll('style'))
    const key = found.map(style => style.textContent).join('\n/**/\n')
    if (key !== styleKey) {
        for (const style of styles) style.remove()
        styles = found.map(style => {
            const copy = document.createElement('style')
            copy.textContent = style.textContent
            document.head.append(copy)
            return copy
        })
        styleKey = key
    }
    for (const style of found) style.remove()
    copyAttributes(page.documentElement, document.documentElement)
    copyAttributes(page.body, document.body)
    morphdom(document.body, page.body, { childrenOnly: true })
}
addEventListener('message', event => {
    if (event.source !== host || event.data?.vls !== 'host') return
    if (event.data.type === 'draft' && typeof event.data.html === 'string') {
        try { render(event.data.html) } catch { /* a malformed prefix is replaced by the next draft */ }
    } else if (event.data.type === 'theme') {
        document.documentElement.dataset.vlsTheme = event.data.theme === 'dark' ? 'dark' : 'light'
    }
})
addEventListener('click', event => event.preventDefault(), true)
host.postMessage({ vls: 'draft', type: 'ready' }, '*')
