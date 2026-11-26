// Injected before page scripts that mention THREE. Exposes Three.js plus the addons the authoring
// contract promises, and small helpers for the parts generated code most often gets wrong: framing the
// whole scene, smooth camera moves, click-versus-drag picking and resizing with the chat panel.
import * as core from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { CSS2DObject, CSS2DRenderer } from 'three/examples/jsm/renderers/CSS2DRenderer.js'
import { EffectComposer } from 'three/examples/jsm/postprocessing/EffectComposer.js'
import { RenderPass } from 'three/examples/jsm/postprocessing/RenderPass.js'
import { UnrealBloomPass } from 'three/examples/jsm/postprocessing/UnrealBloomPass.js'
import { OutputPass } from 'three/examples/jsm/postprocessing/OutputPass.js'
import { RoomEnvironment } from 'three/examples/jsm/environments/RoomEnvironment.js'
import { mergeGeometries } from 'three/examples/jsm/utils/BufferGeometryUtils.js'

const THREE = Object.assign({}, core, {
    OrbitControls, CSS2DObject, CSS2DRenderer, EffectComposer, RenderPass, UnrealBloomPass, OutputPass, RoomEnvironment, mergeGeometries,
})
window.THREE = THREE

const vector = value => value?.isVector3 ? value.clone() : Array.isArray(value) ? new THREE.Vector3(...value)
    : new THREE.Vector3(value?.x || 0, value?.y || 0, value?.z || 0)
const ease = t => t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2
const reduced = () => matchMedia('(prefers-reduced-motion: reduce)').matches
const flights = new WeakMap()

function flyTo(camera, controls, position, target, ms = 900) {
    const toPosition = vector(position)
    const toTarget = target == null ? (controls?.target?.clone() || new THREE.Vector3()) : vector(target)
    flights.get(camera)?.()
    const fromPosition = camera.position.clone()
    const fromTarget = controls?.target?.clone() || toTarget.clone()
    const duration = reduced() ? 0 : Math.max(0, Number(ms) || 0)
    return new Promise(resolve => {
        let frame = 0, cancelled = false
        const start = performance.now()
        flights.set(camera, () => { cancelled = true; cancelAnimationFrame(frame); resolve(false) })
        const step = now => {
            if (cancelled) return
            const t = duration ? Math.min(1, (now - start) / duration) : 1
            const k = ease(t)
            camera.position.lerpVectors(fromPosition, toPosition, k)
            if (controls?.target) controls.target.lerpVectors(fromTarget, toTarget, k)
            else camera.lookAt(toTarget)
            controls?.update?.()
            if (t < 1) frame = requestAnimationFrame(step)
            else { flights.delete(camera); resolve(true) }
        }
        frame = requestAnimationFrame(step)
    })
}

/** Frames the object's bounding box exactly for the given view direction, so flat campuses fill the view. */
function fit(camera, controls, object, options = {}) {
    const box = new THREE.Box3().setFromObject(object)
    if (box.isEmpty()) return null
    const padding = options.padding ?? 1.08
    const target = box.getCenter(new THREE.Vector3())
    const direction = options.direction ? vector(options.direction)
        : camera.position.clone().sub(controls?.target || target)
    if (direction.lengthSq() < 1e-6) direction.set(1, 0.75, 1)
    direction.normalize()
    const forward = direction.clone().negate()
    const up = Math.abs(forward.y) > 0.999 ? new THREE.Vector3(0, 0, -1) : new THREE.Vector3(0, 1, 0)
    const right = new THREE.Vector3().crossVectors(forward, up).normalize()
    const screenUp = new THREE.Vector3().crossVectors(right, forward).normalize()
    const corners = []
    for (const x of [box.min.x, box.max.x]) for (const y of [box.min.y, box.max.y]) for (const z of [box.min.z, box.max.z]) {
        const offset = new THREE.Vector3(x, y, z).sub(target)
        corners.push({ x: offset.dot(right), y: offset.dot(screenUp), z: offset.dot(direction) })
    }
    let distance = 0
    if (camera.isOrthographicCamera) {
        const width = camera.right - camera.left, height = camera.top - camera.bottom
        const spanX = Math.max(...corners.map(c => Math.abs(c.x))), spanY = Math.max(...corners.map(c => Math.abs(c.y)))
        camera.zoom = Math.min(width / (2 * spanX), height / (2 * spanY)) / padding
        distance = Math.max(...corners.map(c => c.z)) + box.getSize(new THREE.Vector3()).length()
    } else {
        const tanV = Math.tan(THREE.MathUtils.degToRad(camera.fov || 50) / 2), tanH = tanV * (camera.aspect || 1)
        for (const c of corners) distance = Math.max(distance, c.z + Math.abs(c.x) / tanH, c.z + Math.abs(c.y) / tanV)
        distance *= padding
    }
    const position = target.clone().add(direction.multiplyScalar(distance))
    camera.near = Math.max(0.05, distance / 200)
    camera.far = Math.max(camera.far, distance * 12)
    camera.updateProjectionMatrix()
    if (controls) {
        if (!Number.isFinite(controls.maxDistance)) controls.maxDistance = distance * 3
        if (!controls.minDistance) controls.minDistance = distance * 0.08
    }
    if (options.animate) return flyTo(camera, controls, position, target, options.animate === true ? 900 : options.animate)
    camera.position.copy(position)
    if (controls?.target) { controls.target.copy(target); controls.update?.() } else camera.lookAt(target)
    return { position, target, distance }
}

const visibleChain = object => {
    for (let node = object; node; node = node.parent) if (!node.visible) return false
    return true
}

function onPick(renderer, camera, objects, callback, options = {}) {
    const element = renderer.domElement
    const raycaster = new THREE.Raycaster()
    const pointer = new THREE.Vector2()
    const candidates = () => (typeof objects === 'function' ? objects() : objects) || []
    function hit(event) {
        const box = element.getBoundingClientRect()
        pointer.set(((event.clientX - box.left) / box.width) * 2 - 1, -((event.clientY - box.top) / box.height) * 2 + 1)
        raycaster.setFromCamera(pointer, camera)
        const list = candidates()
        const roots = new Set(list)
        for (const intersection of raycaster.intersectObjects(list, true)) {
            if (!visibleChain(intersection.object)) continue
            let owner = intersection.object
            while (owner && !roots.has(owner)) owner = owner.parent
            return { object: owner || intersection.object, intersection }
        }
        return null
    }
    let down = null
    const pressed = event => { down = { x: event.clientX, y: event.clientY, at: performance.now() } }
    const released = event => {
        if (!down || event.button > 0) return
        const moved = Math.hypot(event.clientX - down.x, event.clientY - down.y)
        const slow = performance.now() - down.at > 700
        down = null
        if (moved > 6 || slow) return
        const found = hit(event)
        callback(found?.object || null, found?.intersection || null, event)
    }
    let hovered = null
    const moved = event => {
        if (options.hover === false || event.buttons) return
        const found = hit(event)?.object || null
        element.style.cursor = found ? 'pointer' : ''
        if (found !== hovered) { hovered = found; options.onHover?.(found, event) }
    }
    element.addEventListener('pointerdown', pressed)
    element.addEventListener('pointerup', released)
    element.addEventListener('pointermove', moved)
    return () => {
        element.removeEventListener('pointerdown', pressed)
        element.removeEventListener('pointerup', released)
        element.removeEventListener('pointermove', moved)
    }
}

function autoResize(renderer, camera, container, composer, labelRenderer) {
    const target = container || renderer.domElement.parentElement || document.body
    const apply = () => {
        const width = Math.max(1, target.clientWidth || innerWidth), height = Math.max(1, target.clientHeight || innerHeight)
        renderer.setPixelRatio(Math.min(devicePixelRatio || 1, 2))
        renderer.setSize(width, height)
        if (camera.isPerspectiveCamera) camera.aspect = width / height
        else if (camera.isOrthographicCamera) {
            const half = (camera.top - camera.bottom) / 2
            camera.left = -half * width / height
            camera.right = half * width / height
        }
        camera.updateProjectionMatrix()
        composer?.setSize?.(width, height)
        labelRenderer?.setSize?.(width, height)
    }
    apply()
    const observer = new ResizeObserver(apply)
    observer.observe(target)
    return () => observer.disconnect()
}

function label(text, options = {}) {
    const element = document.createElement('div')
    element.className = options.className || 'vls-label'
    element.textContent = text
    if (options.style) Object.assign(element.style, options.style)
    return new CSS2DObject(element)
}

window.__vlsInstallThree?.({ fit, flyTo, onPick, autoResize, label })
