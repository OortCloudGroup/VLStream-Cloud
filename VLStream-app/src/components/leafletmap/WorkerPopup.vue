<template>
  <div class="popup-container">
    <div>
      <div class="info_item">
        <img src="@/assets/img/maintenanceManagement/arrow1.png" class="icon" />
        <div class="info_item_text">
          {{ props.marker?.address || '暂无地址信息' }}
        </div>
      </div>
      <div class="info_item">
        <img src="@/assets/img/maintenanceManagement/noto.png" class="icon" />
        <div class="info_item_text">
          到达时间： {{ props.marker?.report_at || '暂无时间信息' }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, computed } from 'vue'
import zaixian from '@/assets/img/smartGraden/zaixian.png'
import weizhi from '@/assets/img/smartGraden/weizhi.png'
import name from '@/assets/img/smartGraden/name-icon.png'
import workarea from '@/assets/img/smartGraden/work_area.png'
import work from '@/assets/img/smartGraden/work.png'
import titile from '@/assets/img/smartGraden/map_icon.png'
import signal from '@/assets/img/smartGraden/homework_icon.png'
import progress from '@/assets/img/smartGraden/progress.png'
const props = defineProps({
  marker: {
    type: Object,
    required: true
  }
})

// 图片资源
const _pics = {
  zaixian: zaixian,
  name: name,
  weizhi: weizhi,
  workarea: workarea,
  work: work,
  titile: titile,
  signal: signal,
  progress: progress
}

// 信息项配置
const _infoItems = [
  { label: '标识', icon: 'titile', key: 'id' },
  { label: '用户', icon: 'name', key: 'name' },
  { label: '状态', icon: 'zaixian', key: 'status' },
  { label: '电量', icon: 'progress', key: 'battery' },
  { label: '信号', icon: 'progress', key: 'signal' },
  { label: '位置', icon: 'weizhi', key: 'address' },
  { label: 'GPS', icon: 'signal', key: 'gps' },
  { label: '时间', icon: 'progress', key: 'timeRange' },
  { label: '区域', icon: 'workarea', key: 'area' },
  { label: '任务', icon: 'work', key: 'task' }
]

// 操作按钮配置
const _actionButtons = [
  { label: '历史轨迹' },
  { label: '工作时长' },
  { label: '手动定位' },
  { label: '监控指标' }
]

// 根据状态返回不同颜色
const _getStatusColor = computed(() => {
  return (status) => {
    switch (status) {
      case '在线':
        return 'lightgreen'
      case '离线':
        return 'lightgray'
      case '忙碌':
        return 'orange'
      default:
        return 'lightgreen'
    }
  }
})
</script>

<style lang="scss" scoped>
.popup-container {
  // width: 298px;
  // height: 388px;
  min-width: 150px;
  max-width: 50%;
  padding: 0px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.popup-content {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 10px;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.icon {
  width: 16px;
  height: 16px;
  margin-right: 5px;
}

/* 特殊处理标识图标大小 */
.info-item:first-child .icon {
  width: 20px;
  height: 20px;
  margin-right: 10px;
}

.status-tag {
  padding: 2px 6px;
  border-radius: 4px;
}

.button-container {
  display: grid;
  grid-template-columns: repeat(2, 131px);
  gap: 10px;
  justify-content: start;
}

.action-button {
  background-color: #007BFF;
  color: white;
  border: none;
  width: 131px;
  height: 38px;
  border-radius: 4px;
}

.lightgreen {
  background: lightgreen;
  border-radius: 4px;
  padding: 2px 6px;
}

.orange {
  background: orange;
  border-radius: 4px;
  padding: 2px 6px;
}

.lightgray {
  background: lightgray;
  border-radius: 4px;
  padding: 2px 6px;
}

.info_item {
  display: flex;
  margin-bottom: 10px;
}

.info_item_text {
  font-family: SourceHanSansSC-Regular;
  font-size: 12px;
  color: #333333;
  font-weight: 400;
}
</style>
