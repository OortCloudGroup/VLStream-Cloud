<template>
  <div class="setting-ai-page">
    <nav-header-banner title="配置AI算法" :is-call-back="true" @call-back="onBack" />
    <div class="model-settings">
      <!-- 模型超市按钮 -->
      <div class="model-btn">
        <img src="@/assets/img/VLStreamCloud/mxcs.png" alt="" />
        <span>模型超市</span>
      </div>

      <!-- 检测项列表 -->
      <div class="model-list">
        <div
          v-for="(item, index) in modelList"
          :key="item.id || index"
          class="model-item"
          @click="openAi"
        >
          <span class="model-name">{{ item.name }}</span>
          <van-icon
            name="cross"
            class="delete-icon"
            @click.stop="handleDelete(item)"
          />
        </div>
      </div>
    </div>
    <oort-popup v-model="showAi" position="right" style="width: 100%;height: 100%;">
      <aiAbnormal v-if="showAi" :device-id="props.deviceId" @close="showAi = false" />
    </oort-popup>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getVlsAlgorithmt, removeVlsAlgorithmt } from '@/api/VLStreamCloud/algorithm'
import OortPopup from '@/components/popup/oort_popup.vue'
import { useUserStore } from '@/store/modules/useraPaas'
import { showConfirmDialog } from 'vant'
import aiAbnormal from './aiAbnormal.vue'

const showAi = ref(false)

const props = defineProps<{
  deviceId?: string | number
}>()

const store = useUserStore()

// 检测项列表
const modelList = ref<any[]>([])

// 获取算法列表
const fetchModelList = async() => {
  const params: any = {
    accessToken: store.userInfo?.accessToken
  }
  try {
    const res: any = await getVlsAlgorithmt(params)
    if (res && res.code === 200 && res.data?.records) {
      modelList.value = res.data.records
    } else {
      modelList.value = []
    }
  } catch (e) {
    modelList.value = []
  }
}

// 删除检测项（确认弹窗 + 删除接口 + 重新获取列表）
const handleDelete = async(item: any) => {
  showConfirmDialog({
    title: '提示',
    message: '确认删除该算法吗？'
  })
    .then(async() => {
      const ids = String(item.id)
      const params: any = {
        accessToken: store.userInfo?.accessToken,
        ids
      }
      const res: any = await removeVlsAlgorithmt(params)
      if (res && res.code === 200) {
        await fetchModelList()
      }
    })
    .catch(() => {
    })
}

const emit = defineEmits(['close'])

const openAi = () => {
  showAi.value = true
}

const onBack = () => {
  emit('close')
}

onMounted(() => {
  fetchModelList()
})
</script>

<style scoped lang="scss">
.setting-ai-page {
  height: 100vh;
  background-color: #f7f8fa;
  display: flex;
  flex-direction: column;
}

.model-settings {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  background-color: #ffffff;
  padding: 16px;
  box-sizing: border-box;
}

// 模型超市按钮
.model-btn {
  width: 70px;
  display: flex;
  align-items: center;
  gap: 4px;
  border-radius: 18px;
  background: #2f69f8;
  color: #ffffff;
  font-size: 12px;
  padding: 10px 16px;
  margin-bottom: 14px;
  img{
    width: 14px;
    height: 14px;
  }
}

// 检测项列表容器
.model-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

// 单个检测项
.model-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f9faff;
  border-radius: 4px;
  padding: 16px 20px;
  font-size:14px;
  color: #333333;

  .delete-icon {
    font-size: 16px;
    color: #969799;
    cursor: pointer;
  }
}
</style>
