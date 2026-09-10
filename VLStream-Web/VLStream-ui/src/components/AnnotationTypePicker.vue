<template>
  <div class="annotation-picker">
    <div class="type-grid" role="group" aria-label="标注类型">
      <button v-for="item in types" :key="item.value" type="button" class="type-card"
        :class="{ selected: modelValue === item.value }" :aria-pressed="modelValue === item.value"
        @click="$emit('update:modelValue', item.value)">
        <span v-if="modelValue === item.value" class="selected-mark" aria-hidden="true">✓</span>
        <strong>{{ item.label }}</strong>
        <img :src="item.image" :alt="item.label + '真实图像示例'" referrerpolicy="no-referrer" />
      </button>
    </div>
    <div class="type-description" aria-live="polite"><span aria-hidden="true">ⓘ</span><p>{{ selected?.description || '请选择标注类型，查看任务说明。' }}</p></div>
    <p class="example-source">真实图像示例来自 <a :href="`https://docs.ultralytics.com/tasks/${selected?.task || 'classify'}/`" target="_blank" rel="noopener noreferrer">Ultralytics 官方文档</a>，仅用于说明任务区别。</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ modelValue: { type: String, default: '' } })
defineEmits(['update:modelValue'])
const types = [
  { value: 'image_classification', label: '图像分类', hint: '整图：户外人物', description: '判断整张图片“是什么”，为整张图片添加类别标签。关注图像的整体内容，不需要标出物体的位置、数量或轮廓，例如判断图片属于室内还是室外。' },
  { value: 'object_detection', label: '物体检测', hint: '矩形框：人物', description: '识别图片中“有什么、在哪里”，用矩形框标出每个目标并指定类别。适合人员、车辆、安全帽等目标的位置识别和数量统计。' },
  { value: 'instance_segmentation', label: '实例分割', hint: '每个目标独立', description: '沿物体的轮廓精确标注，并区分同一类别的不同个体。例如两个人分别拥有独立的轮廓标注，适合需要精确边界和逐个计数的任务。' },
  { value: 'semantic_segmentation', label: '语义分割', hint: '同类区域同色', description: '为图像区域中的像素赋予类别，同类区域使用相同标签，不区分独立个体。例如标出道路、天空和人员所在的区域，适合场景区域划分。' }
]
const selected = computed(() => types.find(item => item.value === props.modelValue))
const examples = [
  ['classify', 'f61d24ccd6fd778924222927dacaf105'],
  ['detect', '3072f66ad6b2cc3c423a81128af9842f'],
  ['segment', '4f347873b4473e2ccab72d0df4350e7e'],
  ['semantic', '30702b4e25c1ef422e4638fedd8795c0']
]
types.forEach((item, index) => {
  item.task = examples[index][0]
  item.image = `https://cdn.ul.run/i/${examples[index][1]}.avif`
})
types[2].description = '为每个目标分别标注像素级掩膜，并赋予类别。同一类别的不同个体也要分开，例如两个人分别对应两个独立掩膜。多边形可以用于描绘目标轮廓。'
types[3].description = '为图像中的每个像素指定类别，形成类别区域；不区分同一类别的不同个体。例如两个人都属于“人员”，道路和天空分别属于其他类别。示例中的颜色用于区分类别。'
</script>

<style scoped>
.annotation-picker { width: 100%; min-width: 0; }
.type-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(145px, 100%), 1fr)); gap: 12px; }
.type-card { position: relative; min-width: 0; padding: 16px 10px 10px; border: 1px solid #dfe5ee; border-radius: 10px; background: #fff; color: #303a4b; cursor: pointer; font: inherit; }
.type-card strong { display: block; margin-bottom: 12px; font-size: 15px; white-space: nowrap; }
.type-card img { display: block; width: 100%; aspect-ratio: 4 / 3; object-fit: cover; background: #f4f6fa; border-radius: 6px; }
.type-card:hover { border-color: #80adff; background: #f8fbff; }
.type-card.selected { border-color: #287cff; background: #f0f6ff; box-shadow: 0 0 0 1px #287cff; }
.type-card:focus-visible { outline: 3px solid #a9caff; outline-offset: 3px; }
.selected-mark { position: absolute; top: 5px; left: 5px; width: 17px; line-height: 17px; border-radius: 5px; color: white; background: #287cff; font-size: 12px; }
.type-description { display: flex; gap: 9px; margin-top: 16px; padding: 13px 16px; border-radius: 9px; color: #355d85; background: #edf5ff; line-height: 1.8; font-size: 13px; }
.type-description p { margin: 0; }
.example-source { margin: 8px 0 0; color: #909399; font-size: 12px; line-height: 1.5; }
.example-source a { color: #287cff; }
</style>
