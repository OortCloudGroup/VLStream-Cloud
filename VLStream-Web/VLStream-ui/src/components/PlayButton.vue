<template>
  <el-button 
    size="small" 
    class="play-button"
    @click="handleClick"
  >
    <svg class="play-icon" viewBox="0 0 24 24" width="16" height="16">
      <circle cx="12" cy="12" r="10" fill="white"/>
      <polygon points="10,8 16,12 10,16" fill="#1A53FF"/>
    </svg>
    播放
  </el-button>
</template>

<script setup>
// 定义事件
const emit = defineEmits(['click'])

// 处理点击事件
const handleClick = (event) => {
  emit('click', event)
}
</script>

<style scoped>
/* 播放按钮样式 */
.play-button {
  background: linear-gradient(to bottom, #95B0FF 0%, #1A53FF 100%) !important;
  border: none !important;
  color: white !important;
  font-weight: 500;
  border-radius: 4px;
  padding: 6px 12px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.play-icon {
  flex-shrink: 0;
}

.play-button:hover {
  background: linear-gradient(to bottom, #A5C0FF 0%, #2A63FF 100%) !important;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(26, 83, 255, 0.3);
}

.play-button:active {
  background: linear-gradient(to bottom, #85A0FF 0%, #0A43FF 100%) !important;
  transform: translateY(0);
}
</style> 