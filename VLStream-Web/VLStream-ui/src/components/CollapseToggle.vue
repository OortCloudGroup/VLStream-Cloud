<template>
  <div 
    class="collapse-toggle" 
    @click="handleToggle"
    :class="{ 'expanded': isExpanded }"
  >
    <svg 
      class="toggle-icon" 
      width="24" 
      height="24" 
      viewBox="0 0 16 16"
      :class="{ 'rotated': isExpanded }"
    >
    <rect x="2" y="2" width="20" height="1.5" fill="currentColor" class="line"/>
      <!-- 三条横杠 -->
      <rect x="8" y="6" width="8" height="1.5" fill="currentColor" class="line"/>
      <rect x="8" y="9" width="8" height="1.5" fill="currentColor" class="line"/>
      <!-- 三角形箭头 - 拉伸到中间两条线的最高点和最低点 -->
      <path 
        d="M6 6 L6 10.5 L2 8.25 Z" 
        fill="currentColor"
        class="arrow"
      />
    <rect x="2" y="13" width="20" height="1.5" fill="currentColor" class="line"/>
    </svg>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

// 定义属性
const props = defineProps({
  isExpanded: {
    type: Boolean,
    default: false
  }
})

// 定义事件
const emit = defineEmits(['toggle'])

// 处理点击事件
const handleToggle = () => {
  emit('toggle')
}
</script>

<style scoped>
.collapse-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 32.15px;
  min-height: 28.29px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #909399;
}

.collapse-toggle:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.toggle-icon {
  transition: transform 0.3s ease;
  transform-origin: center;
}

/* 展开状态：保持原始方向，箭头指向左（表示可以折叠） */
.collapse-toggle.expanded .toggle-icon {
  transform: rotate(0deg);
}

/* 折叠状态：图标向右旋转180度，箭头指向右（表示可以展开） */
.collapse-toggle:not(.expanded) .toggle-icon {
  transform: rotate(180deg);
}

/* 箭头和横杠的渐变效果 */
.toggle-icon .arrow,
.toggle-icon .line {
  transition: opacity 0.3s ease;
}

/* 线条样式 */
.toggle-icon .line {
  stroke-width: 1.5px;
}

.toggle-icon .arrow {
  stroke-width: 1.5px;
}
</style> 