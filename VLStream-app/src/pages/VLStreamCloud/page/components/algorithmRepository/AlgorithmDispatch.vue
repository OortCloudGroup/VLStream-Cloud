<template>
  <div class="algo-dispatch">
    <nav-header-banner title="算法下发" :is-call-back="true" @call-back="onBack" />

    <div class="dispatch-content">
      <!-- 顶部筛选条 -->
      <div class="filter-row">
        <div
          v-for="item in filterTabs"
          :key="item.value"
          class="filter-item"
          @click="onFilter(item.value as FilterType)"
        >
          {{ item.label }}
          <van-icon name="arrow-down" />
        </div>
      </div>

      <!-- 列表标题 -->
      <div class="list-header-bar">
        <span class="list-title">设备列表</span>
        <div class="list-actions">
          <img src="@/assets/img/VLStreamCloud/xf.png" alt="" />
          <span class="all-push" @click="onDispatchAll">全部下发</span>
        </div>
      </div>

      <!-- 表头 -->
      <div class="table-header">
        <div class="col checkbox">
          <van-checkbox :model-value="isAllChecked" shape="square" @click.stop="toggleAll" />
        </div>
        <div class="col">
          设备名称
        </div>
        <div class="col">
          设备ID
        </div>
        <div class="col">
          状态
        </div>
      </div>

      <!-- 表体 -->
      <van-checkbox-group v-model="checkedIds">
        <div
          v-for="dev in devices"
          :key="dev.id"
          class="table-row"
        >
          <div class="col checkbox">
            <van-checkbox :name="dev.id" shape="square" />
          </div>
          <div class="col name">
            {{ dev.deviceName }}
          </div>
          <div class="col id">
            {{ dev.deviceId }}
          </div>
          <div class="col status">
            <div
              :class="(dev.status === 1 || dev.status === '1') ? 'success' : 'default'"
              class="status-tag"
            >
              {{ (dev.status === 1 || dev.status === '1') ? '在线' : '离线' }}
            </div>
          </div>
        </div>
      </van-checkbox-group>
    </div>

    <!-- 顶部筛选弹出面板 -->
    <VideoFilterPanel
      v-model:show="showFilterPanel"
      :default-tab="filterType"
      @confirm="onFilterConfirm"
    />

    <!-- 底部按钮 -->
    <div class="footer-bar">
      <van-button type="primary" block round class="push-btn" @click="onDispatch">
        下发
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { getDeviceList } from '@/api/VLStreamCloud/device'
import { dispatchAlgorithmToDevices } from '@/api/VLStreamCloud/algorithm'
import NavHeaderBanner from '@/components/navHeaderBanner/index.vue'
import VideoFilterPanel from '../Filter/VideoFilterPanel.vue'
import { useUserStore } from '@/store/modules/useraPaas'

const emit = defineEmits(['close'])

// 可选算法ID
const props = defineProps<{
  algorithmId?: string | number
}>()

type FilterType = 'tree' | 'group' | 'tag'

const filterTabs = [
  { label: '设备树', value: 'tree' },
  // { label: '分组', value: 'group' },
  { label: '标签', value: 'tag' }
]

// 设备列表
const devices = ref<any[]>([])
// 已勾选的设备 ID
const checkedIds = ref<number[]>([])
const store = useUserStore()

// 顶部筛选相关
const showFilterPanel = ref(false)
const filterType = ref<FilterType>('tree')
const tagName = ref<string | null>(null)

// 调用接口获取设备列表（支持 tag 过滤）
const fetchDevices = async() => {
  const params: any = {
    accessToken: store.userInfo?.accessToken
  }
  if (tagName.value) {
    params.tag = tagName.value
  }
  const res: any = await getDeviceList(params)
  if (res && res.code === 200 && res.data && Array.isArray(res.data.records)) {
    devices.value = res.data.records
  } else {
    devices.value = []
  }
}

onMounted(() => {
  fetchDevices()
})

// 打开筛选面板
const onFilter = (type: FilterType) => {
  filterType.value = type
  showFilterPanel.value = true
}

// 接收筛选面板返回的筛选结果
const onFilterConfirm = (filters: any) => {
  if (filters.filterType === 'tag' && filters.filterValue) {
    tagName.value = filters.filterValue
  } else if (filters.filterType === 'tree' && filters.filterValue) {
    tagName.value = filters.filterValue
  } else {
    tagName.value = null
  }
  showFilterPanel.value = false
  fetchDevices()
}

const isAllChecked = computed(
  () =>
    devices.value.length > 0 &&
    checkedIds.value.length === devices.value.length
)

const toggleAll = () => {
  if (isAllChecked.value) {
    checkedIds.value = []
  } else {
    checkedIds.value = devices.value.map(d => d.id)
  }
}

// 全部下发
const onDispatchAll = async() => {
  if (!devices.value.length) {
    showToast({ type: 'fail', message: '暂无可下发设备' })
    return
  }
  checkedIds.value = devices.value.map(d => d.id)
  await onDispatch()
}

// 下发算法到选中的设备
const onDispatch = async() => {
  if (!props.algorithmId) {
    showToast({ type: 'fail', message: '缺少算法ID' })
    return
  }
  if (!checkedIds.value.length) {
    showToast({ type: 'fail', message: '请先选择设备' })
    return
  }

  const deviceIdsStr = checkedIds.value.join(',')
  const body = {
    algorithmId: props.algorithmId,
    deviceIds: deviceIdsStr
  }

  const res: any = await dispatchAlgorithmToDevices(body)
  if (res && res.code === 200) {
    showToast({ type: 'success', message: res.msg })
  }
}

const onBack = () => {
  emit('close')
}
</script>

<style scoped lang="scss">
.algo-dispatch {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f9faff;
}

.dispatch-content {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 12px;
}

.filter-row {
  display: flex;
  padding: 13px 16px;
  line-height: 18px;
  font-weight: 500;
  color: #333333;
  font-size: 14px;
}

.filter-item {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.list-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 8px;
  font-size: 14px;
  color: #374151;
}

.list-title {
  line-height: 24px;
  font-weight: 500;
  color: #969799;
  font-size: 12px;
}

.list-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color:#1a53ff;
  font-size:12px;
  img{
    width: 16px;
    height: 16px;
  }
}

.list-icon {
  font-size: 16px;
}

.table-header {
  display: grid;
  grid-template-columns: 44px 1.4fr 1.3fr 0.9fr;
  padding: 10px 16px;
  color:#333333;
  font-size:14px;
  background:#fafafa;
  margin: 0 16px;
}

.table-row {
  display: grid;
  grid-template-columns: 44px 1.4fr 1.3fr 0.9fr;
  padding: 14px 16px;
  border-bottom: 1px solid #f3f4f6;
  background: #ffffff;
  font-size:14px;
  color:#333333;
  margin: 0 16px;
}

.col {
  display: flex;
  align-items: center;
}

.status-tag {
  display: flex;
  align-items: center;
  border-radius: 4px;
  padding: 0 6px;
  line-height:18px;
  font-size: 12px;
  text-align: center;
}

.success{
  background:rgba(1, 196, 102, 0.12);
  color:#01c466;
}

.default{
  background:rgba(153, 153, 153, 0.12);
  color:#999999;
}

.footer-bar {
  padding: 10px 16px 24px;
  background: #f9faff;
}

.push-btn {
  font-size: 16px;
}
</style>

