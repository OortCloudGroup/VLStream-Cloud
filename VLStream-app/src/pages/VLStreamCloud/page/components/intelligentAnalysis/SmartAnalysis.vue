<template>
  <div class="smart-analysis">
    <!-- 二级标签：分析中 / 已完成 / 已失败 -->
    <van-tabs v-model:active="activeStatus" class="status-tabs" line-width="28">
      <van-tab title="分析中" name="processing" />
      <van-tab title="已完成" name="completed" />
      <van-tab title="已失败" name="failed" />
    </van-tabs>

    <div class="task-list">
      <van-pull-refresh
        v-model="refreshing"
        success-text="刷新成功"
        @refresh="onRefresh"
      >
        <van-list
          v-if="tasks.length"
          v-model="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="onLoad"
        >
          <div
            v-for="task in tasks"
            :key="task.id"
            class="task-card"
            @click="onTaskClick(task)"
          >
            <div class="card-header">
              <div class="task-title">
                {{ task.title }}
              </div>
            </div>
            <img
              v-if="task.requestStatus === 'processing'"
              :src="fxz"
              alt=""
              class="status-img"
            />
            <img
              v-else-if="task.requestStatus === 'completed'"
              :src="ywc"
              alt=""
              class="status-img"
            />
            <img
              v-else
              :src="ysp"
              alt=""
              class="status-img"
            />

            <div class="info-row">
              <div class="label">
                <img :src="fxlx" alt="" class="info-icon" />
                <span>分析类型</span>
              </div>
              <div class="value">
                {{ task.type }}
              </div>
            </div>

            <div class="info-row">
              <div class="label">
                <img :src="fxql" alt="" class="info-icon" />
                <span>分析区域</span>
              </div>
              <div class="value">
                {{ task.area }}
              </div>
            </div>

            <div class="info-row">
              <div class="label">
                <img :src="fxtp" alt="" class="info-icon" />
                <span>分析图片</span>
              </div>
              <div class="thumb">
                <div class="thumb-wrapper">
                  <img v-if="task.thumb" :src="task.thumb" class="thumb-img" alt="" />
                  <div v-if="task.imageCount && task.imageCount > 1" class="thumb-overlay">
                    +{{ task.imageCount - 1 }}
                  </div>
                </div>
              </div>
            </div>

            <div class="info-row">
              <div class="label">
                <img :src="sxt" alt="" class="info-icon" />
                <span>摄像头</span>
              </div>
              <div class="value multi-line">
                {{ task.cameras }}
              </div>
            </div>

            <div class="info-row">
              <div class="label">
                <img :src="sqsj" alt="" class="info-icon" />
                <span>申请时间</span>
              </div>
              <div class="value">
                {{ task.applyTime }}
              </div>
            </div>
          </div>
        </van-list>

        <!-- 空态 -->
        <div v-else class="no_data">
          <img src="@/assets/img/daily_activity/no_data.png" />
          <span>暂无数据</span>
        </div>
      </van-pull-refresh>
    </div>

    <!-- 右下浮动按钮 -->
    <div class="fab" @click="openApplication">
      <van-icon name="plus" class="fab-icon" />
    </div>

    <oort-popup v-model="showDetail" position="right" style="width: 100%;height: 100%;" teleport="body">
      <AnalysisDetail
        v-if="currentTask && (currentTask.status === 'completed' || currentTask.status === 'failed')"
        :task="currentTask"
        @close="showDetail = false"
      />
      <AnalysisApplicationDetail
        v-else-if="currentTask && currentTask.status === 'processing'"
        :task="currentTask"
        @cancel="onCancelApplication"
        @close="showDetail = false"
      />
    </oort-popup>

    <oort-popup v-model="showApplication" position="right" style="width: 100%;height: 100%;" teleport="body">
      <AnalysisApplication @close="onApplicationClose" />
    </oort-popup>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import OortPopup from '@/components/popup/oort_popup.vue'
import AnalysisDetail from './AnalysisDetail.vue'
import AnalysisApplication from './AnalysisApplication.vue'
import AnalysisApplicationDetail from './AnalysisApplicationDetail.vue'
import { getVlsAnalysisk } from '@/api/VLStreamCloud/intelligentAnalysis'
import { useUserStore } from '@/store/modules/useraPaas'
import fxlx from '@/assets/img/VLStreamCloud/znfx/fxlx.png'
import fxql from '@/assets/img/VLStreamCloud/znfx/fxql.png'
import fxtp from '@/assets/img/VLStreamCloud/znfx/fxtp.png'
import sqsj from '@/assets/img/VLStreamCloud/znfx/sqsj.png'
import sxt from '@/assets/img/VLStreamCloud/znfx/sxt.png'
import fxz from '@/assets/img/VLStreamCloud/fxz.png'
import ywc from '@/assets/img/VLStreamCloud/ywc.png'
import ysp from '@/assets/img/VLStreamCloud/ysp.png'

type TaskStatus = 'processing' | 'completed' | 'failed'

const store = useUserStore()
const activeStatus = ref<TaskStatus>('processing')
const showDetail = ref(false)
const currentTask = ref<any | null>(null)
const showApplication = ref(false)

const tasks = ref<any[]>([])

// 分页 & 刷新状态
const refreshing = ref(false)
const loading = ref(false)
const finished = ref(false)
const page = ref(1)
const pageSize = ref(10)

// 格式化时间
const formatTime = (timeStr: string): string => {
  if (!timeStr) return '--'
  try {
    const date = new Date(timeStr.replace(' ', 'T'))
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    return `${year}.${month}.${day} ${hours}:${minutes}:${seconds}`
  } catch {
    return timeStr
  }
}

// 获取任务列表
const fetchTasks = async() => {
  try {
    const params: any = {
      accessToken: store.userInfo?.accessToken,
      current: page.value,
      size: pageSize.value,
      // processing / completed / failed / cancel
      requestStatus: activeStatus.value
    }
    const res: any = await getVlsAnalysisk(params)
    if (res.code === 200 && res.data && Array.isArray(res.data.records)) {
      const list = res.data.records.map((item: any) => {
        const imageStr = item.images || ''
        const imageArr = imageStr ? String(imageStr).split(',') : []
        const firstImage = imageArr[0] || ''

        const rawStatus = item.requestStatus || ''
        const mappedStatus: TaskStatus | '' =
          rawStatus === 'cancel' ? 'failed' : (rawStatus as TaskStatus | '')

        return {
          ...item, // 保留原始数据
          id: item.id,
          title: item.analysisName || '--',
          type: item.analysisType || '--',
          area: item.regionInfo || '--',
          thumb: firstImage,
          cameras: item.cameraName || '--',
          applyTime: formatTime(item.startTime || ''),
          requestStatus: mappedStatus,
          status: mappedStatus,
          imageCount: imageArr.length || 1
        }
      })

      if (page.value === 1) {
        tasks.value = list
      } else {
        tasks.value = [...tasks.value, ...list]
      }

      const total =
        (res.data && (res.data.total || res.data.count)) || 0

      if (!total || tasks.value.length >= total || list.length === 0) {
        finished.value = true
      } else {
        page.value += 1
      }
    } else {
      if (page.value === 1) {
        tasks.value = []
      }
      finished.value = true
    }
  } catch (error) {
    if (page.value === 1) {
      tasks.value = []
    }
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

// 上拉加载更多
const onLoad = () => {
  if (loading.value || finished.value) return
  loading.value = true
  fetchTasks()
}

// 下拉刷新
const onRefresh = () => {
  refreshing.value = true
  finished.value = false
  page.value = 1
  tasks.value = []
  fetchTasks()
}

// 卡片点击
const onTaskClick = (task: any) => {
  openDetail(task)
}

const openDetail = (task: any) => {
  currentTask.value = task
  showDetail.value = true
}

const openApplication = () => {
  showApplication.value = true
}

// 分析申请弹窗关闭后，刷新任务列表
const onApplicationClose = () => {
  showApplication.value = false
  fetchTasks()
}

const onCancelApplication = (_task: any) => {
  // 取消申请成功后，关闭详情弹窗并刷新列表
  showDetail.value = false
  fetchTasks()
}

// 切换 tab 时重置分页并重新拉取
watch(activeStatus, () => {
  finished.value = false
  page.value = 1
  tasks.value = []
  fetchTasks()
})

// 组件挂载时获取数据
onMounted(() => {
  fetchTasks()
})
</script>

<style scoped lang="scss">
.smart-analysis {
  position: relative;
  z-index: 1;
}

.status-tabs {
  --van-tabs-nav-background: transparent;
}

:deep(.van-tab--active .van-tab__text){
  color: #2f69f8;
}

:deep(.van-tabs__line){
  background:#2f69f8;
}

.task-list {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-card {
  position: relative;
  background:#ffffff;
  border-radius:8px;
  padding: 12px;
  margin-bottom: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.task-title {
  line-height: 24px;
  font-weight: 700;
  color: #333333;
  font-size: 16px;
}

.status-stamp {
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid rgba(59, 130, 246, 0.4);
  color: #2563eb;
  font-size: 12px;
}

.status-stamp.done {
  border-color: rgba(16, 185, 129, 0.4);
  color: #059669;
}

.status-stamp.failed {
  border-color: rgba(239, 68, 68, 0.4);
  color: #ef4444;
}

.status-img {
  position: absolute;
  top: 16px;
  right: 16px;
  height: 38px;
  width: 38px;
  object-fit: contain;
}

.info-row {
  display: flex;
  align-items: flex-start;
  margin-top: 8px;
  font-size: 14px;
}

.label {
  min-width: 100px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  line-height: 22px;
  color: #7a7c85;
  font-size: 14px;
}

.info-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.value {
  flex: 1;
  line-height: 22px;
  color: #333333;
  font-size: 14px;
}

.value.multi-line {
  line-height: 1.5;
}

.thumb {
  flex: 1;
}

.thumb-wrapper {
  position: relative;
  display: inline-block;
}

.thumb-img {
  width: 56px;
  height: 56px;
  border-radius:4px;
  object-fit: cover;
}

.thumb-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.fab {
  position: fixed;
  right: 24px;
  bottom: 96px;
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: #2f69f8;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 24px rgba(47, 105, 248, 0.35);
  z-index: 20;
}

.fab-icon {
  font-size: 30px;
  color: #fff;
}

// 暂无数据
.no_data {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  width: 100%;

  img {
    width: 162px;
    height: auto;
  }

  span {
    margin: 4px 0;
    font-size: 16px;
    color: #999;
    text-align: center;
  }
}
</style>

