<template>
  <div class="timeline-container">
    <el-timeline>
      <el-timeline-item
        v-for="(item, index) in feedbackListData"
        :key="index"
        :timestamp="formatTimestamp(item.created_at || '')"
        :hollow="true"
        type="primary"
      >
        <div class="details_list">
          <div class="detailsBox">
            <div class="details_row_box">
              <img src="@/assets/img/maintenance/staff.png" alt="养护详情" />
              <div>反馈人员</div>
            </div>
            <div class="details_row_content">
              <i-d2-head-pic
                :id="item.uuid"
                style="width: 2rem;height: 2rem;margin: 0 0.25rem"
              />
              <div class="name_box">
                <i-d2-name :id="item.uuid" />
              </div>
              <!-- 接口没有返回status字段 -->
              <!-- <el-tag type="success" class="tag_box">
                {{ item.status === 1 ? '已完成' : '正在处理' }}
              </el-tag> -->
            </div>
          </div>
          <div class="detailsBox">
            <div class="details_row_box">
              <img src="@/assets/img/maintenance/address.png" alt="养护详情" />
              <div>反馈位置</div>
            </div>
            <div class="d_r_content">
              {{ item.point?.address }}
            </div>
          </div>
          <div class="detailsBox">
            <div class="details_row_box">
              <img src="@/assets/img/maintenance/popup.png" alt="养护详情" />
              <div>事件描述</div>
            </div>
            <div class="d_r_content">
              {{ item.describe }}
            </div>
          </div>
          <div class="detailsBox">
            <div class="details_row_box">
              <img src="@/assets/img/maintenance/img.png" alt="养护详情" />
              <div>图片/视频</div>
            </div>
            <div class="d_r_content">
              <el-image
                v-for="(url,ind) in item.pics"
                :key="ind"
                :src="url"
                :preview-src-list="item.pics"
                :initial-index="0"
                class="d_r_img"
                fit="cover"
                :preview-teleported="true"
              />
            </div>
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup lang="ts">
interface FeedbackItem {
  created_at?: string
  updated_at?: string
  describe?: string
  id?: string
  pics?: string[]
  point?: {
    address?: string
    lat?: number
    lat_initial?: number
    lng?: number
    lng_initial?: number
  }
  status?: number
  task_event_id?: string
  uuid?: string
}

interface Props {
  feedbackListData: FeedbackItem[]
}

defineProps<Props>()

// 格式化时间戳
const formatTimestamp = (time?: string) => {
  if (!time) return ''
  let timeArr = time.split(' ')
  let dateStr = timeArr[0]
  let timeStr = timeArr[1]
  return `${timeStr}\n${dateStr}`
}
</script>

<style scoped lang="scss">
.detailsBox {
  display: flex;
  align-items: flex-start; // 改为flex-start以便内容换行时对齐顶部
  flex-wrap: nowrap;
  font-family: SourceHanSansSC-Regular;
  font-size: 14px;
  font-weight: 400;
  color: #333333;
  margin-bottom: 15px;

  .details_row_box {
    display: flex;
    align-items: center;
    color: #515A6E;
    margin-right: 10px;
    width: 100px;
    flex-shrink: 0;
    line-height: 30px;
  }

  .details_row_box img {
    width: 20px;
    margin-right: 5px;
  }
}

.d_r_content {
  flex: 1;
  word-wrap: break-word;
  word-break: break-all;
  line-height: 30px;
}

.d_r_img {
  width: 80px;
  height: 80px;
  margin-right: 10px;
}

.details_list {
  // background: #F7F7F7;
  border-radius: 4px;
  padding: 15px 15px 5px 15px;
}

.details_row_content {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-right: 15px;
  font-family: MicrosoftYaHei;
  font-size: 12px;
  color: #999999;
  font-weight: 400;

  .name_box {
    font-family: MicrosoftYaHei;
    font-size: 14px;
    color: #333333;
    line-height: 19px;
    font-weight: 400;
  }
}

.tag_box {
  margin-left: 5px;
}

// 时间线虚线样式
:deep(.el-timeline-item__node) {
  // background-color: #409eff;
  border: 2px solid var(--el-color-primary);
}

:deep(.el-timeline-item__tail) {
  border-left: 2px dashed var(--el-color-primary);
}

:deep(.el-timeline-item:last-child .el-timeline-item__tail) {
  display: none;
}

// 时间线容器样式
.timeline-container {
  padding-left: 80px;
  position: relative;
}

// 时间戳样式
:deep(.el-timeline-item__timestamp) {
  position: absolute;
  left: -80px;
  top: 0;
  width: 100px;
  text-align: left;
  color: #999;
  font-size: 12px;
  line-height: 20px;
  white-space: pre-line;
  direction: rtl;
}

// 让第一行（时间）加粗加大
:deep(.el-timeline-item__timestamp) {
  &::first-line {
    font-size: 16px;
    font-weight: bold;
    color: #333;
    line-height: 24px;
  }
}

:deep(.el-timeline-item) {
  position: relative;
}
</style>
