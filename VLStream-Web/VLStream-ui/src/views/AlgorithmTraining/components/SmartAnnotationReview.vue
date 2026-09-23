<template>
  <el-dialog :model-value="Boolean(item)" :title="title" width="94vw" top="4vh" :close-on-click-modal="false" :before-close="close">
    <template v-if="item">
      <div class="review-heading"><span>{{ item.imageName }}</span><span>{{ item.width }} × {{ item.height }} · {{ typeNames[annotationType] }}</span></div>
      <PixelAnnotationEditor v-if="annotationType !== 'object_detection'" ref="pixelEditor" :kind="annotationType" :image-url="item.previewUrl" :width="item.width" :height="item.height" :regions="boxes" :labels="labels" :disabled="saving" @change="dirty = true" />
      <div v-else class="review-workspace">
        <div class="review-canvas">
          <p>拖动空白处新增框；点击框选择，可拖动移动，也可在右侧调整坐标和大小。</p>
          <svg ref="surface" :viewBox="`0 0 ${item.width} ${item.height}`" aria-label="智能标注画布" @pointerdown="start" @pointermove="move" @pointerup="end" @pointercancel="end">
            <image :href="item.previewUrl" :width="item.width" :height="item.height" />
            <g v-for="(box, index) in boxes" :key="index">
              <rect :x="box.x" :y="box.y" :width="box.width" :height="box.height" :stroke="selected === index ? '#ffbe50' : '#3daeff'" fill="#208bdb18" :stroke-width="stroke" @pointerdown.stop="startMove($event, index)" />
              <text :x="box.x + 3" :y="Math.max(fontSize, box.y)" :font-size="fontSize" fill="#fff" stroke="#183047" :stroke-width="stroke / 3" paint-order="stroke" pointer-events="none">{{ index + 1 }} · {{ labelName(box) }}</text>
            </g>
          </svg>
          <el-alert v-if="!boxes.length" title="未发现目标。可手动画框；确认无目标时选择跳过。" type="info" :closable="false" />
        </div>
        <aside class="review-boxes">
          <el-select v-model="newLabel" :disabled="saving" placeholder="新画框使用的标签" style="width:100%"><el-option v-for="label in labels" :key="label.id" :label="label.name" :value="String(label.id)" /></el-select>
          <div v-for="(box, index) in boxes" :key="index" class="box-form" :class="{ selected: selected === index }" @click="selected = index">
            <div class="box-heading"><strong>目标 {{ index + 1 }}</strong><span>{{ box.confidence == null ? '手工新增' : `${Math.round(box.confidence * 100)}%` }}</span><el-button link type="danger" :disabled="saving" @click.stop="remove(index)">删除</el-button></div>
            <p v-if="box.className" class="model-label">模型类别：{{ box.className }}</p>
            <el-select v-model="box.labelId" :disabled="saving" placeholder="请选择对应标签" style="width:100%"><el-option v-for="label in labels" :key="label.id" :label="label.name" :value="String(label.id)" /></el-select>
            <div class="coordinates"><label v-for="field in fields" :key="field.key">{{ field.name }}<el-input-number v-model="box[field.key]" :disabled="saving" :min="field.min" :max="field.key === 'x' || field.key === 'width' ? item.width : item.height" :precision="1" :controls="false" /></label></div>
          </div>
        </aside>
      </div>
    </template>
    <template #footer><el-button :disabled="saving" @click="close">返回</el-button><el-button v-if="allowSkip" :disabled="saving" @click="submit(true)">跳过本图</el-button><el-button type="primary" :loading="saving" :disabled="annotationType === 'object_detection' && !boxes.length" @click="submit(false)">确认并保存标注</el-button></template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PixelAnnotationEditor from './PixelAnnotationEditor.vue'

const props = defineProps({ item: { type: Object, default: null }, labels: { type: Array, default: () => [] }, saving: Boolean, annotationType: { type: String, default: 'object_detection' }, title: { type: String, default: '确认预标注' }, allowSkip: { type: Boolean, default: true } })
const emit = defineEmits(['close', 'save'])
const boxes = ref([])
const selected = ref(-1)
const newLabel = ref('')
const surface = ref(null)
const dirty = ref(false)
const pixelEditor = ref(null)
const typeNames = { image_classification: '图像分类', object_detection: '物体检测', instance_segmentation: '实例分割', semantic_segmentation: '语义分割' }
let gesture = null
const fields = [{ key: 'x', name: 'X', min: 0 }, { key: 'y', name: 'Y', min: 0 }, { key: 'width', name: '宽', min: 0.1 }, { key: 'height', name: '高', min: 0.1 }]
const stroke = computed(() => Math.max(1, (props.item?.width || 640) / 350))
const fontSize = computed(() => Math.max(12, (props.item?.width || 640) / 45))
watch(() => props.item, item => {
  boxes.value = item ? JSON.parse(item.candidate.boxesJson).map(box => ({ ...box, labelId: box.labelId == null ? '' : String(box.labelId) })) : []
  selected.value = -1
  newLabel.value = props.labels[0] ? String(props.labels[0].id) : ''
  gesture = null
  dirty.value = false
})
watch(boxes, () => { dirty.value = true }, { deep: true, flush: 'sync' })
const labelName = box => props.labels.find(label => String(label.id) === box.labelId)?.name || box.className || '未选择标签'
const point = event => {
  const p = surface.value.createSVGPoint()
  p.x = event.clientX; p.y = event.clientY
  const result = p.matrixTransform(surface.value.getScreenCTM().inverse())
  return { x: Math.max(0, Math.min(props.item.width, result.x)), y: Math.max(0, Math.min(props.item.height, result.y)) }
}
const start = event => {
  if (props.saving || event.button !== 0) return
  const p = point(event)
  boxes.value.push({ x: p.x, y: p.y, width: 0, height: 0, labelId: newLabel.value, confidence: null })
  selected.value = boxes.value.length - 1
  gesture = { mode: 'draw', ...p }
  surface.value.setPointerCapture(event.pointerId)
}
const startMove = (event, index) => {
  if (props.saving || event.button !== 0) return
  selected.value = index
  gesture = { mode: 'move', ...point(event), before: { ...boxes.value[index] } }
  surface.value.setPointerCapture(event.pointerId)
}
const move = event => {
  if (!gesture || props.saving) return
  const p = point(event); const box = boxes.value[selected.value]
  if (gesture.mode === 'draw') {
    box.x = Math.min(gesture.x, p.x); box.y = Math.min(gesture.y, p.y)
    box.width = Math.abs(p.x - gesture.x); box.height = Math.abs(p.y - gesture.y)
  } else {
    box.x = Math.max(0, Math.min(props.item.width - box.width, gesture.before.x + p.x - gesture.x))
    box.y = Math.max(0, Math.min(props.item.height - box.height, gesture.before.y + p.y - gesture.y))
  }
}
const end = () => {
  if (gesture?.mode === 'draw' && (boxes.value[selected.value].width < 1 || boxes.value[selected.value].height < 1)) boxes.value.splice(selected.value, 1)
  gesture = null
}
const remove = index => { boxes.value.splice(index, 1); selected.value = -1 }
const close = async () => {
  if (props.saving) return
  if (dirty.value) { try { await ElMessageBox.confirm('本图修改尚未保存，确认返回？', '返回', { type: 'warning' }) } catch { return } }
  emit('close')
}
const submit = skip => {
  if (!skip && props.annotationType !== 'object_detection') {
    try {
      const regions = pixelEditor.value.getRegions()
      if (!regions.length) throw new Error('请先补充标注；没有目标的图片可以跳过')
      emit('save', { skip: false, boxes: regions })
    } catch (error) { ElMessage.warning(error.message) }
    return
  }
  if (!skip && boxes.value.some(box => !box.labelId || ![box.x, box.y, box.width, box.height].every(Number.isFinite) || box.width <= 0 || box.height <= 0 || box.x < 0 || box.y < 0 || box.x + box.width > props.item.width + 0.01 || box.y + box.height > props.item.height + 0.01)) {
    ElMessage.warning('请为每个目标框选择标签，并确保框位于图片内')
    return
  }
  emit('save', { skip, boxes: skip ? [] : boxes.value.map(box => ({ ...box })) })
}
</script>

<style scoped>
.review-heading,.box-heading{display:flex;align-items:center;justify-content:space-between;gap:12px}.review-heading{margin-bottom:12px}.review-workspace{display:grid;grid-template-columns:minmax(0,1fr) 300px;gap:18px}.review-canvas{min-width:0;background:#152333;padding:14px;border-radius:8px;color:#c6d3e2}.review-canvas p{margin:0 0 12px;font-size:12px}.review-canvas svg{width:100%;max-height:52vh;display:block;touch-action:none;cursor:crosshair;user-select:none}.review-boxes{max-height:58vh;overflow:auto}.box-form{border:1px solid var(--el-border-color);padding:12px;margin-top:12px;border-radius:7px}.box-form.selected{border-color:#e6a23c}.model-label{font-size:12px;color:var(--el-text-color-secondary)}.coordinates{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:10px}.coordinates label{font-size:12px}.coordinates .el-input-number{width:100%}@media(max-width:800px){.review-workspace{grid-template-columns:1fr}.review-boxes{max-height:32vh}}
</style>
