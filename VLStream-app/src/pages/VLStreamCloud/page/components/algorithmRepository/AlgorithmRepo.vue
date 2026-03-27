<template>
  <div class="algo-repo">
    <!-- 二级标签：根据接口返回动态生成 -->
    <van-tabs v-model:active="activeTab" class="sub-tabs" line-width="24">
      <van-tab
        v-for="item in tabs"
        :key="item.name"
        :title="item.title"
        :name="item.name"
      />
    </van-tabs>

    <!-- 顶部提示条 -->
    <div v-if="showTip" class="tip-bar">
      <van-icon name="info-o" class="tip-icon" />
      <span class="tip-text">点击算法即可将算法下发至摄像机终端</span>
      <van-icon name="cross" class="tip-close" @click="showTip = false" />
    </div>

    <!-- 算法卡片网格 -->
    <div>
      <van-pull-refresh
        v-model="refreshing"
        success-text="刷新成功"
        @refresh="onRefresh"
      >
        <van-list
          v-if="algoList.length"
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="onLoad"
        >
          <div class="card-grid">
            <div
              v-for="item in algoList"
              :key="item.id"
              class="algo-card"
              @click="onClickAlgo(item)"
            >
              <img :src="item.cover" class="card-img" alt="" />
              <div class="card-content">
                <div class="card-title">
                  {{ item.name }}
                </div>
                <div class="card-desc">
                  {{ item.desc }}
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
    </div>

    <oort-popup v-model="showDispatch" position="right" style="width: 100%;height: 100%;" teleport="body">
      <AlgorithmDispatch
        v-if="currentAlgo"
        :algorithm-id="currentAlgo?.id"
        @close="showDispatch = false"
      />
    </oort-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { getVlsAlgorithmRepositoryt, getVlsAlgorithmt } from '@/api/VLStreamCloud/algorithm'
import OortPopup from '@/components/popup/oort_popup.vue'
import AlgorithmDispatch from './AlgorithmDispatch.vue'
import { useUserStore } from '@/store/modules/useraPaas'

type TabKey = string

const activeTab = ref<TabKey>('')
const showTip = ref(true)
const showDispatch = ref(false)

// 算法卡片
interface AlgoItem {
  id: number | string
  name: string
  desc: string
  cover: string
}

const allAlgos = ref<AlgoItem[]>([])
const currentAlgo = ref<AlgoItem | null>(null)

const tabs = ref<{ name: TabKey; title: string; repositoryId: string | number }[]>([])

const store = useUserStore()

// 分页 & 刷新状态
const refreshing = ref(false)
const loading = ref(false)
const finished = ref(false)
const page = ref(1)
const pageSize = ref(10)

// 获取算法仓库列表
const fetchAlgorithmRepository = async() => {
  const params = {}
  const res: any = await getVlsAlgorithmRepositoryt(params)
  if (res && res.code === 200 && res.data && Array.isArray(res.data.records)) {
    const records = res.data.records

    tabs.value = records.map((item: any) => ({
      name: (item.repositoryType || 'basic') as TabKey,
      title: item.name || item.repositoryType || '未命名仓库',
      repositoryId: item.id
    }))

    if (!activeTab.value && tabs.value.length > 0) {
      const basicTab = tabs.value.find(t => t.name === 'basic')
      activeTab.value = (basicTab?.name || tabs.value[0].name) as TabKey
    }
  } else {
    allAlgos.value = []
    tabs.value = []
  }
}

// 查询算法列表
const fetchAlgorithmListByActiveTab = async() => {
  const currentTab = tabs.value.find(t => t.name === activeTab.value)
  if (!currentTab || !currentTab.repositoryId) {
    allAlgos.value = []
    return
  }

  try {
    const params: any = {
      repositoryId: currentTab.repositoryId,
      accessToken: store.userInfo?.accessToken,
      current: page.value,
      size: pageSize.value
    }
    const res: any = await getVlsAlgorithmt(params)
    if (res && res.code === 200 && res.data && Array.isArray(res.data.records)) {
      const list = res.data.records.map((item: any, index: number) => ({
        id: item.id ?? index,
        name: item.name || '',
        desc: item.description || '',
        cover: item.imageUrl || ''
      }))

      if (page.value === 1) {
        allAlgos.value = list
      } else {
        allAlgos.value = [...allAlgos.value, ...list]
      }

      const total = (res.data && (res.data.total || res.data.count)) || 0
      if (!total || allAlgos.value.length >= total || list.length === 0) {
        finished.value = true
      } else {
        page.value += 1
      }
    } else {
      if (page.value === 1) {
        allAlgos.value = []
      }
      finished.value = true
    }
  } catch (e) {
    if (page.value === 1) {
      allAlgos.value = []
    }
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

onMounted(() => {
  fetchAlgorithmRepository()
})

// 监听 tab 变化
watch(activeTab, () => {
  finished.value = false
  page.value = 1
  allAlgos.value = []
  fetchAlgorithmListByActiveTab()
})

// 当前展示的算法列表
const algoList = computed(() => allAlgos.value)

const onClickAlgo = (item: AlgoItem) => {
  currentAlgo.value = item
  showDispatch.value = true
}

// 上拉加载更多
const onLoad = () => {
  if (loading.value || finished.value) return
  loading.value = true
  fetchAlgorithmListByActiveTab()
}

// 下拉刷新
const onRefresh = () => {
  refreshing.value = true
  finished.value = false
  page.value = 1
  allAlgos.value = []
  fetchAlgorithmListByActiveTab()
}
</script>

<style scoped lang="scss">
.algo-repo {
  position: relative;
  z-index: 1;
}

.sub-tabs {
  --van-tabs-nav-background: transparent;
}

.tip-bar {
  margin: 10px 0;
  padding: 12px 13px;
  background:rgba(47, 105, 248, 0.12);
  border-radius:6px;
  display: flex;
  align-items: center;
  gap: 5px;
  color:#2f69f8;
  font-size:12px;
}

:deep(.van-tab--active .van-tab__text){
  color: #2f69f8;
}

:deep(.van-tabs__line){
  background:#2f69f8;
}

.tip-icon {
  color:#2f69f8;
  padding-top: 2px;
}

.tip-text {
  flex: 1;
}

.tip-close {
  color:#2f69f8;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.algo-card {
  background:#ffffff;
  border-radius:12px;
  overflow: hidden;
}

.card-img {
  width: 100%;
  height: 100px;
  object-fit: cover;
}

.card-content {
  padding: 12px;
}

.card-title {
  line-height: 19px;
  font-weight: 600;
  color: #333333;
  font-size: 14px;
  margin-bottom: 8px;
}

.card-desc {
  line-height: 18px;
  color: #666666;
  font-size: 12px;
  letter-spacing: 0.34px;
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

