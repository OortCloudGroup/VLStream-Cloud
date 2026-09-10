<template>
  <el-dialog :model-value="modelValue" title="视频切图" width="600px" :close-on-click-modal="false" @close="emit('update:modelValue', false)">
    <p>视频：{{ video.imageName }}</p>
    <el-alert title="图片生成后自动加入当前数据集，可继续标注；原视频保留。" type="info" :closable="false" />
    <el-form label-width="110px" class="frame-form">
      <el-form-item label="开始时间（秒）"><el-input-number v-model="form.startSeconds" :min="0" :max="86400" :precision="2" /></el-form-item>
      <el-form-item label="结束时间（秒）"><el-input-number v-model="form.endSeconds" :min="0.01" :max="86400" :precision="2" placeholder="留空直到视频结尾" /><span class="tip">留空直到视频结尾</span></el-form-item>
      <el-form-item label="切图间隔（秒）"><el-input-number v-model="form.intervalSeconds" :min="0.1" :max="3600" :precision="2" /></el-form-item>
      <el-form-item label="最多生成"><el-input-number v-model="form.maxFrames" :min="1" :max="1000" :precision="0" /><span class="tip">张</span></el-form-item>
    </el-form>
    <p class="tip">从开始时间起按间隔取帧，结束时间不包含在内；时间范围超过视频时长时取到视频结尾。相同图片会跳过重复入库。</p>
    <p class="tip">后台处理，任务进度可在“导入记录”查看，关闭页面不会中断已提交的切图任务。</p>
    <template #footer><el-button @click="emit('update:modelValue', false)">取消</el-button><el-button type="primary" :loading="submitting" @click="submit">开始切图</el-button></template>
  </el-dialog>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { extractVideoFrames } from '@/api/datasetImport'
const props = defineProps({ modelValue: Boolean, datasetId: { type: [String, Number], required: true }, video: { type: Object, required: true } })
const emit = defineEmits(['update:modelValue', 'submitted'])
const form = reactive({ startSeconds: 0, endSeconds: undefined, intervalSeconds: 1, maxFrames: 100 })
const submitting = ref(false)
const submit = async () => {
  if (submitting.value) return
  if (!Number.isFinite(form.startSeconds) || !Number.isFinite(form.intervalSeconds) || form.intervalSeconds < 0.1 || !Number.isInteger(form.maxFrames)) return ElMessage.warning('请填写有效的切图时间、间隔和数量')
  if (form.endSeconds != null && form.endSeconds <= form.startSeconds) return ElMessage.warning('结束时间必须大于开始时间')
  submitting.value = true
  try {
    await extractVideoFrames({ datasetId: props.datasetId, videoId: props.video.id, ...form, endSeconds: form.endSeconds ?? null })
    ElMessage.success('已创建视频切图任务'); emit('submitted'); emit('update:modelValue', false)
  } catch (error) { ElMessage.error(error.message || '切图任务创建失败') } finally { submitting.value = false }
}
</script>
<style scoped>.frame-form { margin-top: 22px; }.tip { color: #909399; font-size: 12px; line-height: 1.8; }.el-form-item .tip { margin-left: 8px; }</style>
