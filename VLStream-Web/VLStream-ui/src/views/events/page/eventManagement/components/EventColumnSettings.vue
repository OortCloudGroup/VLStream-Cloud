<template>
    <el-popover placement="bottom-end" trigger="click" :width="310">
        <template #reference>
            <el-button :title="$tp('列设置')" :aria-label="$tp('列设置')">
                <el-icon><Setting /></el-icon>
                <span>{{ $tp('列设置') }}</span>
            </el-button>
        </template>
        <div class="column-settings-title">
            <strong>{{ $tp('列设置') }}</strong>
            <el-button link type="primary" @click="$emit('reset')">{{ $tp('恢复默认') }}</el-button>
        </div>
        <p class="column-settings-hint">{{ $tp('拖动调整顺序，勾选显示字段') }}</p>
        <div class="column-settings-list">
            <div v-for="(column, index) in modelValue" :key="column.key" class="column-setting-row"
                draggable="true" @dragstart="startDrag($event, column.key)" @dragover.prevent
                @drop.prevent="dropColumn(column.key)" @dragend="draggedKey = ''">
                <span class="drag-handle" aria-hidden="true">⠿</span>
                <el-checkbox :model-value="column.visible"
                    :disabled="column.visible && visibleCount === 1"
                    @change="toggleColumn(column.key, $event)">{{ $tp(column.label) }}</el-checkbox>
                <el-button text :disabled="index === 0" :aria-label="`${$tp('上移')} ${$tp(column.label)}`"
                    @click="moveColumn(column.key, modelValue[index - 1].key)">↑</el-button>
                <el-button text :disabled="index === modelValue.length - 1" :aria-label="`${$tp('下移')} ${$tp(column.label)}`"
                    @click="moveColumn(column.key, modelValue[index + 1].key)">↓</el-button>
            </div>
        </div>
    </el-popover>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Setting } from '@element-plus/icons-vue'
import { moveEventColumn } from './eventColumns.mjs'

const props = defineProps({ modelValue: { type: Array, required: true } })
const emit = defineEmits(['update:modelValue', 'reset'])
const draggedKey = ref('')
const visibleCount = computed(() => props.modelValue.filter(column => column.visible).length)
const toggleColumn = (key, visible) => {
    emit('update:modelValue', props.modelValue.map(column => column.key === key ? { ...column, visible } : column))
}
const moveColumn = (source, target) => emit('update:modelValue', moveEventColumn(props.modelValue, source, target))
const startDrag = (event, key) => {
    draggedKey.value = key
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', key)
}
const dropColumn = target => {
    if (draggedKey.value) moveColumn(draggedKey.value, target)
    draggedKey.value = ''
}
</script>

<style scoped>
.column-settings-title { display: flex; align-items: center; justify-content: space-between; }
.column-settings-hint { color: var(--el-text-color-secondary); font-size: 12px; }
.column-settings-list { max-height: 430px; overflow-y: auto; }
.column-setting-row { display: flex; align-items: center; gap: 6px; border-radius: 4px; }
.column-setting-row:hover { background: var(--el-fill-color-light); }
.column-setting-row .el-checkbox { flex: 1; min-width: 0; }
.column-setting-row .el-button { margin: 0; padding: 5px; }
.drag-handle { cursor: grab; padding: 6px; color: var(--el-text-color-secondary); }
</style>
