<template>
  <div v-if="kind === 'image_classification'" class="classification-editor">
    <img :src="imageUrl" alt="待分类图片" />
    <aside><h3>整图类别</h3><p>为这张图片选择一个类别。</p><el-select v-model="classification" :disabled="disabled" placeholder="选择类别" @change="changed = true; emit('change')"><el-option v-for="label in labels" :key="label.id" :label="label.name" :value="String(label.id)" /></el-select><p v-if="regions[0]?.className">模型建议：{{ regions[0].className }} · {{ Math.round((regions[0].confidence || 0) * 100) }}%</p></aside>
  </div>
  <div v-else class="pixel-editor" v-loading="loading">
    <div class="pixel-main">
      <div class="pixel-toolbar"><el-radio-group v-model="tool" size="small"><el-radio-button value="brush">画笔</el-radio-button><el-radio-button value="erase">橡皮</el-radio-button></el-radio-group><span>画笔</span><el-input-number v-model="brush" :min="1" :max="160" :disabled="disabled" size="small" /><el-button size="small" :disabled="!history.length || disabled" @click="undo">撤销绘制</el-button><span>透明度</span><el-input-number v-model="opacity" :min="0" :max="90" :step="10" size="small" @change="draw" /></div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <div class="pixel-stage"><canvas ref="canvas" :width="width" :height="height" aria-label="像素标注画布" @pointerdown="begin" @pointermove="paint" @pointerup="end" @pointercancel="end" /></div>
      <p class="pixel-hint">{{ semantic ? `每个像素只能有一个类别；还有 ${unassigned.toLocaleString()} 个像素未归类。` : '每个实例独立保存，可涂抹修正轮廓或用橡皮去除多余像素。' }}</p>
    </div>
    <aside class="pixel-sidebar">
      <el-select v-model="newLabel" :disabled="disabled" placeholder="新区域的类别" style="width:100%"><el-option v-for="label in labels" :key="label.id" :label="label.name" :value="String(label.id)" /></el-select>
      <el-button :disabled="disabled || loading || Boolean(error) || !newLabel" @click="add">{{ semantic ? '添加类别区域' : '新增实例' }}</el-button>
      <el-button v-if="semantic" :disabled="disabled || loading || Boolean(error) || selected < 0 || !unassigned" @click="fillRemaining">填充未归类区域</el-button>
      <div v-for="(entry, index) in entries" :key="entry.key" class="pixel-entry" :class="{ selected: selected === index }" @click="selected = index; draw()">
        <div class="pixel-entry-title"><strong>{{ semantic ? '区域' : '实例' }} {{ index + 1 }}</strong><el-button link type="danger" :disabled="disabled" @click.stop="remove(index)">删除</el-button></div>
        <el-select v-model="entry.labelId" :disabled="disabled" placeholder="匹配标签" style="width:100%" @change="entry.edited = true; emit('change'); draw()"><el-option v-for="label in labels" :key="label.id" :label="label.name" :value="String(label.id)" /></el-select>
        <small v-if="entry.className">模型类别：{{ entry.className }}</small>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({ kind: { type: String, required: true }, imageUrl: { type: String, required: true }, width: { type: Number, required: true }, height: { type: Number, required: true }, regions: { type: Array, default: () => [] }, labels: { type: Array, default: () => [] }, disabled: Boolean })
const emit = defineEmits(['change'])
const semantic = computed(() => props.kind === 'semantic_segmentation')
const canvas = ref(null); const entries = ref([]); const selected = ref(-1); const newLabel = ref('')
const classification = ref(''); const changed = ref(false); const tool = ref('brush'); const brush = ref(20); const opacity = ref(40)
const loading = ref(false); const error = ref(''); const unassigned = ref(0); const history = ref([])
let image = null; let pixels = null; let drawing = false; let previousPoint = null; let sequence = 0; let scheduled = null; let nextKey = 0
const loadImage = url => new Promise((resolve, reject) => { const value = new Image(); value.onload = () => resolve(value); value.onerror = () => reject(new Error('图片或掩膜加载失败，请刷新后重试')); value.src = url })
const labelColor = entry => {
  const labelIndex = props.labels.findIndex(label => String(label.id) === entry.labelId)
  const selectedLabel = props.labels[labelIndex]
  const duplicates = props.labels.filter(label => label.color === selectedLabel?.color).length > 1
  const palette = ['#409eff', '#f59e42', '#67c23a', '#a968df', '#e75d7f', '#23b8ad', '#c4a128', '#758de5']
  const value = !duplicates && selectedLabel?.color ? selectedLabel.color : palette[Math.max(0, labelIndex) % palette.length]
  return /^#[0-9a-f]{6}$/i.test(value) ? [1, 3, 5].map(offset => parseInt(value.slice(offset, offset + 2), 16)) : [64, 158, 255]
}
const cloneEntries = () => entries.value.map(entry => ({ ...entry, crop: entry.crop ? { ...entry.crop } : null }))
const remember = () => {
  history.value.push({ entries: cloneEntries(), selected: selected.value, pixels, unassigned: unassigned.value })
  const maximum = Math.max(1, Math.min(10, Math.floor(20000000 / (props.width * props.height))))
  if (history.value.length > maximum) history.value.shift()
  if (semantic.value) pixels = pixels.slice()
  else if (selected.value >= 0 && entries.value[selected.value]?.crop) entries.value[selected.value].crop.data = entries.value[selected.value].crop.data.slice()
}
const undo = () => {
  const state = history.value.pop(); if (!state) return
  entries.value = state.entries; selected.value = state.selected; pixels = state.pixels; unassigned.value = state.unassigned
  emit('change'); draw()
}
const cropPixels = async region => {
  const bitmap = await loadImage(`data:image/png;base64,${region.maskData}`)
  const scratch = document.createElement('canvas'); scratch.width = bitmap.width; scratch.height = bitmap.height
  const context = scratch.getContext('2d', { willReadFrequently: true }); context.drawImage(bitmap, 0, 0)
  const rgba = context.getImageData(0, 0, bitmap.width, bitmap.height).data; const data = new Uint8Array(bitmap.width * bitmap.height)
  for (let index = 0; index < data.length; index++) data[index] = rgba[index * 4] > 0 && rgba[index * 4 + 3] > 0 ? 1 : 0
  return { x: Number(region.x), y: Number(region.y), width: bitmap.width, height: bitmap.height, data }
}
const load = async () => {
  const current = ++sequence; drawing = false; history.value = []; entries.value = []; image = null; error.value = ''; changed.value = false
  classification.value = props.regions[0]?.labelId == null ? '' : String(props.regions[0].labelId)
  newLabel.value = props.labels[0] ? String(props.labels[0].id) : ''
  if (props.kind === 'image_classification') return
  loading.value = true
  try {
    image = await loadImage(props.imageUrl)
    if (current !== sequence) return
    if (image.naturalWidth !== props.width || image.naturalHeight !== props.height) throw new Error('图片尺寸发生变化，请重新载入')
    pixels = semantic.value ? new Uint16Array(props.width * props.height) : null
    unassigned.value = semantic.value ? props.width * props.height : 0
    const loaded = []
    for (const region of props.regions) {
      const crop = await cropPixels(region)
      if (current !== sequence) return
      const entry = { key: ++nextKey, labelId: region.labelId == null ? '' : String(region.labelId), className: region.className, confidence: region.confidence, edited: false, crop: semantic.value ? null : crop }
      loaded.push(entry)
      if (semantic.value) for (let index = 0; index < crop.data.length; index++) if (crop.data[index]) {
        const position = (crop.y + Math.floor(index / crop.width)) * props.width + crop.x + index % crop.width
        if (pixels[position]) throw new Error('语义掩膜存在重叠，请重新生成预测')
        pixels[position] = loaded.length; unassigned.value--
      }
    }
    entries.value = loaded; selected.value = loaded.length ? 0 : -1
    await nextTick(); draw()
  } catch (failure) { if (current === sequence) error.value = failure.message }
  finally { if (current === sequence) loading.value = false }
}
watch(() => [props.imageUrl, props.kind, props.regions], load, { immediate: true })
watch(() => props.labels, () => { if (!newLabel.value && props.labels[0]) newLabel.value = String(props.labels[0].id); draw() })

const draw = () => {
  if (!canvas.value || !image) return
  const context = canvas.value.getContext('2d'); context.clearRect(0, 0, props.width, props.height); context.drawImage(image, 0, 0, props.width, props.height)
  const alpha = Math.round(opacity.value * 2.55)
  if (semantic.value && pixels) {
    const scratch = document.createElement('canvas'); scratch.width = props.width; scratch.height = props.height
    const overlay = scratch.getContext('2d'); const rgba = overlay.createImageData(props.width, props.height)
    const palette = entries.value.map(labelColor)
    for (let index = 0; index < pixels.length; index++) if (pixels[index]) {
      const color = palette[pixels[index] - 1]; if (!color) continue
      const offset = index * 4; rgba.data[offset] = color[0]; rgba.data[offset + 1] = color[1]; rgba.data[offset + 2] = color[2]; rgba.data[offset + 3] = alpha
    }
    overlay.putImageData(rgba, 0, 0); context.drawImage(scratch, 0, 0)
  } else for (const [index, entry] of entries.value.entries()) {
    const crop = entry.crop; if (!crop?.width || !crop?.height) continue
    const scratch = document.createElement('canvas'); scratch.width = crop.width; scratch.height = crop.height
    const overlay = scratch.getContext('2d'); const rgba = overlay.createImageData(crop.width, crop.height); const color = labelColor(entry)
    for (let pixel = 0; pixel < crop.data.length; pixel++) if (crop.data[pixel]) {
      const offset = pixel * 4; rgba.data[offset] = color[0]; rgba.data[offset + 1] = color[1]; rgba.data[offset + 2] = color[2]; rgba.data[offset + 3] = alpha
    }
    overlay.putImageData(rgba, 0, 0); context.drawImage(scratch, crop.x, crop.y)
    if (index === selected.value) { context.strokeStyle = '#ffbf50'; context.lineWidth = Math.max(1, props.width / 600); context.strokeRect(crop.x, crop.y, crop.width, crop.height) }
  }
}
const redraw = () => { if (scheduled == null) scheduled = requestAnimationFrame(() => { scheduled = null; draw() }) }
const add = () => {
  remember(); entries.value.push({ key: ++nextKey, labelId: newLabel.value, className: '', confidence: null, edited: true, crop: null })
  selected.value = entries.value.length - 1; emit('change'); draw()
}
const remove = index => {
  remember()
  if (semantic.value) for (let pixel = 0; pixel < pixels.length; pixel++) {
    if (pixels[pixel] === index + 1) { pixels[pixel] = 0; unassigned.value++ } else if (pixels[pixel] > index + 1) pixels[pixel]--
  }
  entries.value.splice(index, 1); selected.value = Math.min(index, entries.value.length - 1); emit('change'); draw()
}
const fillRemaining = () => {
  if (selected.value < 0) return
  remember(); for (let index = 0; index < pixels.length; index++) if (!pixels[index]) pixels[index] = selected.value + 1
  unassigned.value = 0; entries.value[selected.value].edited = true; emit('change'); draw()
}
const position = event => {
  const bounds = canvas.value.getBoundingClientRect()
  return { x: Math.max(0, Math.min(props.width - 1, Math.floor((event.clientX - bounds.left) * props.width / bounds.width))), y: Math.max(0, Math.min(props.height - 1, Math.floor((event.clientY - bounds.top) * props.height / bounds.height))) }
}
const expand = (entry, left, top, right, bottom) => {
  const old = entry.crop
  if (old) { left = Math.min(left, old.x); top = Math.min(top, old.y); right = Math.max(right, old.x + old.width); bottom = Math.max(bottom, old.y + old.height) }
  if (old && left === old.x && top === old.y && right - left === old.width && bottom - top === old.height) return
  const width = right - left, height = bottom - top, data = new Uint8Array(width * height)
  if (old) for (let row = 0; row < old.height; row++) data.set(old.data.subarray(row * old.width, (row + 1) * old.width), (old.y - top + row) * width + old.x - left)
  entry.crop = { x: left, y: top, width, height, data }
}
const dab = point => {
  const radius = Math.max(.5, brush.value / 2), erase = tool.value === 'erase', entry = entries.value[selected.value]
  const left = Math.max(0, Math.floor(point.x - radius)), right = Math.min(props.width, Math.ceil(point.x + radius + 1))
  const top = Math.max(0, Math.floor(point.y - radius)), bottom = Math.min(props.height, Math.ceil(point.y + radius + 1))
  if (!semantic.value && !erase) expand(entry, left, top, right, bottom)
  for (let y = top; y < bottom; y++) for (let x = left; x < right; x++) {
    if ((x - point.x) ** 2 + (y - point.y) ** 2 > radius ** 2) continue
    if (semantic.value) {
      const index = y * props.width + x, previous = pixels[index], next = erase ? 0 : selected.value + 1
      if (previous !== next) {
        if (!previous) unassigned.value--; if (!next) unassigned.value++
        if (previous) entries.value[previous - 1].edited = true
        pixels[index] = next
      }
    } else {
      const crop = entry.crop
      if (crop && x >= crop.x && x < crop.x + crop.width && y >= crop.y && y < crop.y + crop.height) crop.data[(y - crop.y) * crop.width + x - crop.x] = erase ? 0 : 1
    }
  }
  entry.edited = true
}
const begin = event => {
  if (props.disabled || loading.value || error.value || event.button !== 0) return
  if (selected.value < 0) { if (!newLabel.value) return; add() }
  remember(); drawing = true; previousPoint = position(event); canvas.value.setPointerCapture(event.pointerId); dab(previousPoint); emit('change'); redraw()
}
const paint = event => {
  if (!drawing || props.disabled) return
  const point = position(event), steps = Math.max(1, Math.ceil(Math.hypot(point.x - previousPoint.x, point.y - previousPoint.y) / Math.max(1, brush.value / 3)))
  for (let step = 1; step <= steps; step++) dab({ x: previousPoint.x + (point.x - previousPoint.x) * step / steps, y: previousPoint.y + (point.y - previousPoint.y) * step / steps })
  previousPoint = point; redraw()
}
const end = () => { drawing = false; previousPoint = null }
const encode = (entry, crop) => {
  let left = crop.width, top = crop.height, right = -1, bottom = -1
  for (let index = 0; index < crop.data.length; index++) if (crop.data[index]) { const x = index % crop.width, y = Math.floor(index / crop.width); left = Math.min(left, x); right = Math.max(right, x); top = Math.min(top, y); bottom = Math.max(bottom, y) }
  if (right < left) return null
  const width = right - left + 1, height = bottom - top + 1, scratch = document.createElement('canvas'); scratch.width = width; scratch.height = height
  const context = scratch.getContext('2d'), rgba = context.createImageData(width, height)
  for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
    const value = crop.data[(y + top) * crop.width + x + left] ? 255 : 0, offset = (y * width + x) * 4
    rgba.data[offset] = value; rgba.data[offset + 1] = value; rgba.data[offset + 2] = value; rgba.data[offset + 3] = 255
  }
  context.putImageData(rgba, 0, 0)
  return { labelId: entry.labelId, className: entry.className, confidence: entry.edited ? null : entry.confidence, x: crop.x + left, y: crop.y + top, width, height, maskData: scratch.toDataURL('image/png').split(',')[1] }
}
const getRegions = () => {
  if (props.kind === 'image_classification') {
    if (!classification.value) throw new Error('请选择整图类别')
    return [{ labelId: classification.value, className: props.regions[0]?.className, confidence: changed.value ? null : props.regions[0]?.confidence ?? null }]
  }
  if (loading.value || error.value) throw new Error(error.value || '图片仍在加载')
  if (semantic.value && unassigned.value) throw new Error('请为剩余像素指定类别后再确认')
  if (entries.value.some(entry => !entry.labelId)) throw new Error('请为所有区域选择数据集标签')
  if (!semantic.value) return entries.value.map(entry => entry.crop ? encode(entry, entry.crop) : null).filter(Boolean)
  const result = []
  for (let index = 0; index < entries.value.length; index++) {
    const data = new Uint8Array(pixels.length)
    for (let pixel = 0; pixel < pixels.length; pixel++) data[pixel] = pixels[pixel] === index + 1 ? 1 : 0
    const region = encode(entries.value[index], { x: 0, y: 0, width: props.width, height: props.height, data }); if (region) result.push(region)
  }
  return result
}
defineExpose({ getRegions })
onBeforeUnmount(() => { sequence++; if (scheduled != null) cancelAnimationFrame(scheduled) })
</script>

<style scoped>
.classification-editor{display:grid;grid-template-columns:minmax(0,1fr) 300px;gap:20px}.classification-editor img{max-width:100%;max-height:52vh;object-fit:contain;margin:auto}.pixel-editor{display:grid;grid-template-columns:minmax(0,1fr) 290px;gap:16px}.pixel-main{min-width:0}.pixel-toolbar{display:flex;gap:8px;align-items:center;flex-wrap:wrap;font-size:12px;margin-bottom:10px}.pixel-toolbar .el-input-number{width:90px}.pixel-stage{display:flex;justify-content:center;align-items:center;overflow:auto;background:#152333;min-height:240px;border-radius:7px}.pixel-stage canvas{max-width:100%;max-height:48vh;width:auto;height:auto;touch-action:none;cursor:crosshair}.pixel-hint{font-size:12px;color:var(--el-text-color-secondary)}.pixel-sidebar{max-height:58vh;overflow:auto}.pixel-sidebar>.el-button{margin:8px 6px 0 0}.pixel-entry{padding:10px;border:1px solid var(--el-border-color);border-radius:6px;margin-top:10px;cursor:pointer}.pixel-entry.selected{border-color:#e6a23c}.pixel-entry-title{display:flex;align-items:center;justify-content:space-between}.pixel-entry small{display:block;margin-top:8px;color:var(--el-text-color-secondary)}@media(max-width:800px){.classification-editor,.pixel-editor{grid-template-columns:1fr}.pixel-sidebar{max-height:30vh}}
</style>
