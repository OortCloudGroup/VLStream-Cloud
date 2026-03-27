<template>
  <div class="playback-list">
    <van-pull-refresh
      v-model="refreshing"
      success-text="刷新成功"
      @refresh="onRefresh"
    >
      <van-list
        v-if="devices.length"
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <div
          v-for="item in devices"
          :key="item.id"
          class="playback-card"
          @click="openDetail(item)"
        >
          <div class="card-main">
            <div class="card-title-row">
              <div class="card-title">
                {{ item.deviceName }}
              </div>
            </div>

            <div class="info-row" style="width: 90%">
              <img src="@/assets/img/VLStreamCloud/spgc/sbId.png" alt="" />
              <div class="label">
                设备ID
              </div>
              <div class="value">
                {{ item.deviceId }}
              </div>
            </div>
            <div class="info-row">
              <img src="@/assets/img/VLStreamCloud/spgc/sbwz.png" alt="" />
              <div class="label">
                设备位置
              </div>
              <div class="value">
                {{ item.address }}
              </div>
            </div>

            <div class="card-footer">
              <div class="chips">
                <div class="chip" :class="{ offline: item.status != '1' }">
                  {{ item.status == '1' ? '在线' : '离线' }}
                </div>
                <div v-if="item.deviceType" class="chip">
                  {{ item.deviceType }}
                </div>
                <div v-if="item.heightPosition" class="chip">
                  {{ item.heightPosition }}
                </div>
              </div>
            </div>
            <div class="play-btn">
              <img v-if="item.status == '1'" src="@/assets/img/VLStreamCloud/spgc/playing.png" alt="" />
              <img v-else src="@/assets/img/VLStreamCloud/spgc/no_playing.png" alt="" />
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
    <oort-popup v-model="showDetail" position="right" style="width: 100%;height: 100%;" teleport="body">
      <VideoPlaybackDetail
        v-if="currentDevice"
        :device-id="currentDevice.id"
        :device-name="currentDevice.deviceName"
        @close="showDetail = false"
      />
    </oort-popup>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import OortPopup from '@/components/popup/oort_popup.vue'
import VideoPlaybackDetail from './VideoPlaybackDetail.vue'
import { getDeviceList } from '@/api/VLStreamCloud/device'
import { useUserStore } from '@/store/modules/useraPaas'

const store = useUserStore()

const devices = ref<any[]>([])

// 分页 & 刷新状态
const refreshing = ref(false)
const loading = ref(false)
const finished = ref(false)
const page = ref(1)
const pageSize = ref(10)

const fetchDevices = async() => {
  try {
    const params: any = {
      accessToken: store.userInfo?.accessToken,
      current: page.value,
      size: pageSize.value
    }
    const res: any = await getDeviceList(params)
    if (res.code === 200 && res.data && Array.isArray(res.data.records)) {
      const list = res.data.records
      if (page.value === 1) {
        devices.value = list
      } else {
        devices.value = [...devices.value, ...list]
      }

      const total = (res.data && (res.data.total || res.data.count)) || 0
      if (!total || devices.value.length >= total || list.length === 0) {
        finished.value = true
      } else {
        page.value += 1
      }
    } else {
      if (page.value === 1) {
        devices.value = []
      }
      finished.value = true
    }
  } catch (e) {
    if (page.value === 1) {
      devices.value = []
    }
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

onMounted(() => {
  fetchDevices()
})

// 上拉加载更多
const onLoad = () => {
  if (loading.value || finished.value) return
  loading.value = true
  fetchDevices()
}

// 下拉刷新
const onRefresh = () => {
  refreshing.value = true
  finished.value = false
  page.value = 1
  devices.value = []
  fetchDevices()
}

const showDetail = ref(false)
const currentDevice = ref<any | null>(null)

const openDetail = (item: any) => {
  currentDevice.value = item
  showDetail.value = true
}
</script>

<style scoped lang="scss">
.playback-list {
  margin-top: 16px;
}

.playback-card {
  position: relative;
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.card-main {
  flex: 1;
}

.card-title-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  gap: 10px;
}

.card-title {
  line-height: 25px;
  font-weight: 700;
  color: #333333;
  font-size: 16px;
}

.info-row {
  display: flex;
  margin-top: 4px;
  font-size: 14px;
  line-height: 20px;
  img {
    width: 20px;
    height: 20px;
    margin-right: 4px;
  }
}

.label {
  min-width: 80px;
  color: #7a7c85;
}

.value {
  flex: 1;
  color: #333333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.card-footer {
  display: flex;
  align-items: center;
  margin-top: 10px;
}

.chips {
  display: flex;
  gap: 8px;
}

.chip {
  background: rgba(47, 105, 248, 1);
  border-radius: 2px;
  line-height: 16px;
  color: #ffffff;
  font-size: 12px;
  padding: 4px 12px;
}

.chip.offline {
  background: rgba(216, 216, 216, 1);
  color: #ffffff;
}

.play-btn {
  position: absolute;
  top: 16px;
  right: 12px;
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.play-btn img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

// 暂无数据（同 eventPat.vue）
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

