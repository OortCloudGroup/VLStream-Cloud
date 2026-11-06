export default {
    mounted(el) {
        let stop = () => {}
        const down = e => {
            if (e.button !== 0 || !e.target.closest('[data-drag-handle]')) return
            const control = e.target.closest('button, input, select, textarea, a, [role="tab"], [contenteditable="true"]')
            if (control && !control.hasAttribute('data-drag-handle')) return
            e.preventDefault(); stop()
            el.classList.add('is-dragging-card')
            const r = el.getBoundingClientRect(), x = e.clientX, y = e.clientY, o = el._offset || [0, 0]
            const move = v => {
                el._offset = [o[0] + Math.max(-r.left, Math.min(innerWidth - r.right, v.clientX - x)), o[1] + Math.max(-r.top, Math.min(innerHeight - r.bottom, v.clientY - y))]
                el.style.transform = `translate(${el._offset[0]}px, ${el._offset[1]}px)`
            }
            stop = () => { el.classList.remove('is-dragging-card'); window.removeEventListener('pointermove', move); window.removeEventListener('pointerup', stop); window.removeEventListener('pointercancel', stop); window.removeEventListener('blur', stop) }
            window.addEventListener('pointermove', move); window.addEventListener('pointerup', stop); window.addEventListener('pointercancel', stop)
            window.addEventListener('blur', stop)
        }
        el.addEventListener('pointerdown', down)
        el._dispose = () => { stop(); el.removeEventListener('pointerdown', down) }
    },
    unmounted(el) { el._dispose?.() }
}
