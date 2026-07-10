/**
 * 全屏半透明水印指令
 * 用法: v-watermark 或 v-watermark="{ text: '张三 - XJ123456 - XX分局 - 2025-01-01' }"
 * 防截图泄密：使用 Canvas 绘制水印覆盖全屏，阻止删除（MutationObserver 监听）
 */
import type { Directive, DirectiveBinding } from 'vue'

interface WatermarkOptions {
    text?: string
    fontSize?: number
    color?: string
    opacity?: number
    gap?: number
    angle?: number
}

const defaultOptions: WatermarkOptions = {
  fontSize: 14,
  color: '#000000',
  opacity: 0.08,
  gap: 160,
  angle: -22
}

let observer: MutationObserver | null = null
let watermarkEl: HTMLElement | null = null

function createWatermark(options: WatermarkOptions) {
  const { text = '', fontSize, color, opacity, gap, angle } = { ...defaultOptions, ...options }
  if (!text) return ''

  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')!
  const dpr = window.devicePixelRatio || 1

  // 计算单个水印单元尺寸
  ctx.font = `${fontSize! * dpr}px Arial`
  const metrics = ctx.measureText(text)
  const textWidth = metrics.width / dpr
  const textHeight = fontSize!

  const unitW = textWidth + gap!
  const unitH = textHeight * 3 + gap!

  canvas.width = unitW * dpr
  canvas.height = unitH * dpr
  ctx.scale(dpr, dpr)

  // 绘制水印文字
  ctx.font = `${fontSize}px Arial`
  ctx.fillStyle = color!
  ctx.globalAlpha = opacity!
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.translate(unitW / 2, unitH / 2)
  ctx.rotate((angle! * Math.PI) / 180)
  ctx.fillText(text, 0, 0)
  ctx.restore()

  return canvas.toDataURL('image/png')
}

function mountWatermark(el: HTMLElement, binding: DirectiveBinding<WatermarkOptions | string>) {
  const options: WatermarkOptions = typeof binding.value === 'string' ? { text: binding.value } : { ...binding.value }

  // 移除旧水印
  removeWatermark()

  const bgUrl = createWatermark(options)
  if (!bgUrl) return

  // 创建水印覆盖层
  watermarkEl = document.createElement('div')
  watermarkEl.setAttribute('data-watermark', 'true')
  Object.assign(watermarkEl.style, {
    position: 'fixed',
    top: '0',
    left: '0',
    width: '100%',
    height: '100%',
    pointerEvents: 'none',
    zIndex: '99999',
    backgroundImage: `url(${bgUrl})`,
    backgroundRepeat: 'repeat',
    backgroundSize: 'auto',
    userSelect: 'none'
  })

  document.body.appendChild(watermarkEl)

  // MutationObserver 防止水印被删除或修改
  observer = new MutationObserver((mutations) => {
    for (const mutation of mutations) {
      // 水印被删除则重新挂载
      if (mutation.type === 'childList' && mutation.removedNodes.length) {
        for (const node of mutation.removedNodes) {
          if (node === watermarkEl || (node as HTMLElement).getAttribute?.('data-watermark') === 'true') {
            mountWatermark(el, binding)
            return
          }
        }
      }
      // 水印属性被篡改则重新挂载
      if (mutation.type === 'attributes' && mutation.target === watermarkEl) {
        mountWatermark(el, binding)
        return
      }
    }
  })

  observer.observe(document.body, {
    childList: true,
    subtree: true,
    attributes: true,
    attributeFilter: ['style', 'class']
  })
}

function removeWatermark() {
  if (observer) {
    observer.disconnect()
    observer = null
  }
  if (watermarkEl && watermarkEl.parentNode) {
    watermarkEl.parentNode.removeChild(watermarkEl)
    watermarkEl = null
  }
}

const watermark: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<WatermarkOptions | string>) {
    mountWatermark(el, binding)
  },
  updated(el: HTMLElement, binding: DirectiveBinding<WatermarkOptions | string>) {
    mountWatermark(el, binding)
  },
  unmounted() {
    removeWatermark()
  }
}

export default watermark
