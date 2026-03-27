<template>
  <span class="id2NameText">{{ displayName }}</span>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useUserStore } from '@/store/modules/useraPaas'
import { batchFetchUser } from '@/utils/userBatch'

const store = useUserStore()
const props = defineProps({
  id: {
    type: String,
    required: true,
    default: ''
  },
  // 可以添加其他需要的props，比如名字显示格式等
  format: {
    type: String,
    default: 'full' // 'full' | 'short' 等
  }
})

const displayName = ref<string>('') // 默认显示为空

const init = () => {
  if (!props.id) return

  // 先检查store中是否已有该用户信息
  if (store.userListStore[props.id]) {
    setDisplayName(store.userListStore[props.id])
    return
  }

  // 没有则请求获取
  fetchUserInfo()
}

const fetchUserInfo = () => {
  batchFetchUser(props.id)
    .then((userInfo: any) => {
      setDisplayName(userInfo)
    })
    .catch(() => {
      displayName.value = '未知用户'
    })
}

const setDisplayName = (userInfo: any) => {
  // 根据用户信息和props.format设置显示的名字
  if (userInfo.user_name) {
    displayName.value = userInfo.user_name
  } else if (userInfo.user_detail.ex_data.realName) {
    displayName.value = userInfo.user_detail.ex_data.realName
  } else {
    displayName.value = '用户' + props.id.slice(0, 4) // 默认显示ID前4位
  }

  // 可以根据props.format进一步处理
  if (props.format === 'short' && displayName.value.length > 4) {
    displayName.value = displayName.value.slice(0, 4) + '...'
  }
}

// 监听id变化
watch(() => props.id, () => {
  init()
})

// 初始化
onMounted(() => {
  init()
})
</script>

<style lang="scss" scoped>
.id2NameText {
  font-size: 14px;
  color: #333;
  /* 可以根据需要添加其他样式 */
}
</style>
