<template>
    <nav class="square-thumbnail-strip" :aria-label="$tp('视频缩略图')">
        <button v-for="device in devices" :key="device.id" type="button" class="square-thumbnail"
            :class="{ active: device.id === selectedId }" :aria-pressed="device.id === selectedId"
            :aria-label="device.deviceName || device.name" @click="$emit('select', device.id)">
            <img v-if="frames[device.id]" :src="frames[device.id]" alt="" />
            <span v-else class="thumbnail-unavailable">{{ $tp(device.status === 'OFFLINE' ? '无信号' : '未预览') }}</span>
            <span class="thumbnail-name" data-i18n-ignore>{{ device.deviceName || device.name }}</span>
        </button>
        <span v-if="!devices.length" class="thumbnail-empty">{{ $tp('暂无设备') }}</span>
    </nav>
</template>

<script setup>
defineProps({ devices: { type: Array, default: () => [] }, selectedId: { type: [String, Number], default: '' }, frames: { type: Object, default: () => ({}) } })
defineEmits(['select'])
</script>

<style scoped>
.square-thumbnail-strip { display: flex; flex: 0 0 auto; min-width: 0; max-width: 100%; gap: 16px; padding: 4px 2px 12px; overflow-x: auto; overscroll-behavior: contain; }
.square-thumbnail { position: relative; flex: 0 0 clamp(90px, 12vw, 140px); width: clamp(90px, 12vw, 140px); aspect-ratio: 1; overflow: hidden; padding: 0; border: 2px solid transparent; border-radius: 8px; background: #171b22; cursor: pointer; color: #a7b0bd; }
.square-thumbnail.active { border-color: #026fff; }
.square-thumbnail:focus-visible { outline: 2px solid #026fff; outline-offset: -4px; }
.square-thumbnail img { width: 100%; height: 100%; display: block; object-fit: contain; }
.thumbnail-unavailable { font-size: 12px; }
.thumbnail-name { position: absolute; inset: auto 0 0; padding: 5px 6px; background: rgb(0 0 0 / 55%); color: #fff; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; overflow: hidden; }
.thumbnail-empty { padding: 12px; color: #778da2; font-size: 12px; }
</style>
