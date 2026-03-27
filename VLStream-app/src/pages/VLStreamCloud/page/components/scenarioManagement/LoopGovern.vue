<template>
  <div class="loop-wrap">
    <van-pull-refresh
      v-model="refreshing"
      success-text="刷新成功"
      @refresh="onRefresh"
    >
      <van-list
        v-if="loopList.length"
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        class="loop-list"
        @load="onLoad"
      >
        <div v-for="item in loopList" :key="item.id" class="loop-card">
          <div class="card-header">
            <div class="card-title">
              {{ item.name || '-' }}
            </div>
          </div>

          <div class="info-row">
            <div class="label">
              <img :src="aisf" alt="" class="info-icon" />
              <span>AI算法</span>
            </div>
            <div class="value">
              {{ item.algorithmNames || '-' }}
            </div>
          </div>

          <div class="info-row">
            <div class="label">
              <img :src="fxql" alt="" class="info-icon" />
              <span>分析区域</span>
            </div>
            <div class="value">
              {{ item.locationIds || '-' }}
            </div>
          </div>

          <div class="info-row">
            <div class="label">
              <img :src="sxt" alt="" class="info-icon" />
              <span>摄像头</span>
            </div>
            <div class="value multi-line">
              {{ item.cameraNames || '-' }}
            </div>
          </div>

          <div class="info-row">
            <div class="label">
              <img :src="zlsj" alt="" class="info-icon" />
              <span>起止时间</span>
            </div>
            <div class="value">
              {{ item.startTime && item.endTime ? `${item.startTime} ~ ${item.endTime}` : '-' }}
            </div>
          </div>

          <div class="toggle" @click="toggle(item.id)">
            <span class="toggle-text">子循环</span>
            <img class="toggle-icon" :src="expandedMap[item.id] ? sqImg : zkImg" alt="" />
          </div>

          <div v-if="expandedMap[item.id]" class="sub-list">
            <div v-for="sub in item.subTaskList" :key="sub.id" class="sub-card">
              <div class="info-row">
                <div class="label">
                  <img :src="aisf" alt="" class="info-icon" />
                  <span>AI算法</span>
                </div>
                <div class="value">
                  {{ item.algorithmNames || '-' }}
                </div>
              </div>
              <div class="info-row">
                <div class="label">
                  <img :src="fxql" alt="" class="info-icon" />
                  <span>分析区域</span>
                </div>
                <div class="value">
                  {{ sub.locationIds || '-' }}
                </div>
              </div>
              <div class="info-row">
                <div class="label">
                  <img :src="sxt" alt="" class="info-icon" />
                  <span>摄像头</span>
                </div>
                <div class="value multi-line">
                  {{ sub.cameraNames || '-' }}
                </div>
              </div>
              <div class="info-row">
                <div class="label">
                  <img :src="zlsj" alt="" class="info-icon" />
                  <span>起止时间</span>
                </div>
                <div class="value">
                  {{ sub.executeTime || '-' }}
                </div>
              </div>
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

    <div class="fab" @click="onAdd">
      <span class="fab-plus">+</span>
    </div>

    <oort-popup v-model="showAdd" position="right" style="width: 100%;height: 100%;" teleport="body">
      <AddGovern v-if="showAdd" default-mode="loop" @close="handleAddClose" />
    </oort-popup>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import OortPopup from '@/components/popup/oort_popup.vue'
import AddGovern from '@/pages/VLStreamCloud/page/components/scenarioManagement/AddGovern.vue'
import { getVlsMobileSceneGovernanceloopList } from '@/api/VLStreamCloud/sceneManagement'
import { useUserStore } from '@/store/modules/useraPaas'
import zkImg from '@/assets/img/VLStreamCloud/zk.png'
import sqImg from '@/assets/img/VLStreamCloud/sq.png'
import aisf from '@/assets/img/VLStreamCloud/cjzl/aisf.png'
import fxql from '@/assets/img/VLStreamCloud/cjzl/fxql.png'
import sxt from '@/assets/img/VLStreamCloud/cjzl/sxt.png'
import zlsj from '@/assets/img/VLStreamCloud/cjzl/zlsj.png'

// 循环治理场景列表
const loopList = ref([])

const store = useUserStore()

// 分页 & 刷新状态
const refreshing = ref(false)
const loading = ref(false)
const finished = ref(false)
const page = ref(1)
const pageSize = ref(10)

// 获取循环治理列表
const fetchLoopGovernList = async() => {
  try {
    const params = {
      accessToken: store.userInfo?.accessToken,
      current: page.value,
      size: pageSize.value
    }
    const res = await getVlsMobileSceneGovernanceloopList(params)

    if (res && res.code === 200 && res.data) {
      let records = []
      let total = 0

      if (Array.isArray(res.data.records)) {
        records = res.data.records
        total = res.data.total || res.data.count || records.length
      } else if (Array.isArray(res.data)) {
        records = res.data
        total = records.length
      }

      if (page.value === 1) {
        loopList.value = records
      } else {
        loopList.value = [...loopList.value, ...records]
      }

      if (!records.length || loopList.value.length >= total) {
        finished.value = true
      } else if (Array.isArray(res.data.records)) {
        // 仅在后端支持分页时递增页码
        page.value += 1
      }
    } else {
      if (page.value === 1) {
        loopList.value = []
      }
      finished.value = true
    }
  } catch (e) {
    if (page.value === 1) {
      loopList.value = []
    }
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

onMounted(() => {
  fetchLoopGovernList()
})

const expandedMap = reactive({})

const toggle = (id) => {
  expandedMap[id] = !expandedMap[id]
}

// 上拉加载更多
const onLoad = () => {
  if (loading.value || finished.value) return
  loading.value = true
  fetchLoopGovernList()
}

// 下拉刷新
const onRefresh = () => {
  refreshing.value = true
  finished.value = false
  page.value = 1
  loopList.value = []
  fetchLoopGovernList()
}

const showAdd = ref(false)

const onAdd = () => {
  showAdd.value = true
}

// 刷新循环治理列表
const handleAddClose = () => {
  showAdd.value = false
  finished.value = false
  page.value = 1
  loopList.value = []
  fetchLoopGovernList()
}
</script>

<style lang="scss" scoped>
.loop-wrap {
  position: relative;
  z-index: 1;
  margin-top: 16px;
}

.loop-card {
  position: relative;
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.04);
  margin-bottom: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.card-title {
  line-height: 24px;
  font-weight: 700;
  color: #333333;
  font-size: 16px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  margin-top: 8px;
  font-size: 14px;
}

.label {
  min-width: 90px;
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

.toggle {
  padding: 14px 0 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: rgba(26, 83, 255, 1);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  user-select: none;
}

.toggle-icon {
  width: 18px;
  height: 18px;
  object-fit: contain;
}

.sub-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sub-card {
  background:#f6f5f8;
  border-radius: 12px;
  padding: 12px;
}

.fab {
  position: fixed;
  right: 18px;
  bottom: 92px;
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

.fab-plus {
  font-size: 34px;
  line-height: 34px;
  color: #fff;
  font-weight: 500;
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

