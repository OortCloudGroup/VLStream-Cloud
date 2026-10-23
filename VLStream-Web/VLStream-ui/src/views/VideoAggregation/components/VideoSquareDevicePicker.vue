<template>
  <el-dialog :model-value="modelValue" :title="$tp('添加视频')" width="560px" class="square-device-picker"
    @update:model-value="$emit('update:modelValue', $event)" @closed="reset">
    <el-input v-model="keyword" :placeholder="$tp('搜索设备...')" clearable />
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div v-loading="loading" class="picker-devices">
      <el-empty v-if="!filteredDevices.length && !loading" :description="$tp('暂无数据')" :image-size="60" />
      <el-radio-group v-model="selectedId" class="picker-options">
        <el-radio v-for="device in filteredDevices" :key="device.id" :label="device.id">
          <span data-i18n-ignore>{{ device.deviceName }}</span>
          <small data-i18n-ignore>{{ device.deviceId || device.id }}</small>
        </el-radio>
      </el-radio-group>
    </div>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">{{ $tp('取消') }}</el-button>
      <el-button type="primary" :disabled="!selectedDevice || loading" @click="confirm">{{ $tp('确定') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { matchesVideoDevice } from '@/utils/videoSquareCatalog'
const props = defineProps({ modelValue: Boolean, devices: { type: Array, default: () => [] }, loading: Boolean, error: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue', 'select'])
const keyword = ref('')
const selectedId = ref('')
const filteredDevices = computed(() => props.devices.filter(device => matchesVideoDevice(device, keyword.value)))
const selectedDevice = computed(() => props.devices.find(device => device.id === selectedId.value))
const reset = () => { selectedId.value = ''; keyword.value = '' }
const confirm = () => {
  if (!selectedDevice.value || props.loading) return
  emit('select', selectedDevice.value)
  emit('update:modelValue', false)
}
</script>

<style>
.square-device-picker.el-dialog { max-width: calc(100vw - 32px); border-radius: 12px; background: #f0f4f9; }
.square-device-picker .el-dialog__body { display: flex; flex-direction: column; gap: 16px; }
.square-device-picker .el-dialog__footer { display: flex; justify-content: flex-end; gap: 20px; }
.square-device-picker .el-dialog__footer .el-button { width: 170px; max-width: 45%; height: 48px; margin: 0; border-radius: 2px; }
</style>
<style scoped>
.picker-devices { height: min(360px, 45dvh); overflow: auto; }
.picker-options { display: flex; flex-direction: column; align-items: stretch; width: 100%; }
.picker-options .el-radio { min-height: 54px; height: auto; margin: 0; padding-inline: 8px; }
.picker-options :deep(.el-radio__label) { white-space: normal; overflow-wrap: anywhere; min-width: 0; }
.picker-options small { display: block; color: #778da2; margin-top: 4px; }
</style>
