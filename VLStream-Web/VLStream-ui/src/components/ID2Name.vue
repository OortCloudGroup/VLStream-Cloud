<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-11-15 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <span>{{ name }}</span>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useUserStore } from '@/store/modules/useraPaas'
import { getUserList } from '@/api/system/directory'

const store: any = useUserStore()
const props = defineProps({
  id: {
    type: String,
    required: true,
    default: ''
  },
  valueKey: {
    type: String,
    default: 'user_name'
  },
  round: {
    type: Boolean,
    default: false
  }
})
const name = ref(props.id)
// 刷新本地用户名称；无用户 ID 时保留空文本。
const init = () => {
  name.value = props.id || ''
  void getUserInfoFn()
}

// 从本地用户目录解析名称，兼容仅存在本项目登录令牌的场景。
const getUserInfoFn = async() => {
  if (!props.id) return

  // 是否vuex中已经存在这个用户，避免重复请求
  const cachedUser = store.userListStore?.[props.id]
  if (cachedUser) {
    name.value = cachedUser[props.valueKey] || props.id
    return
  }

  try {
    const res: any = await getUserList({
      user_id: [props.id],
      hideLoading: true
    })
    const user = res?.code === 200 && Array.isArray(res?.data?.list) ? res.data.list[0] : undefined
    if (user) {
      store.addUser(user)
      name.value = user[props.valueKey] || props.id
    }
  } catch (error) {
    name.value = props.id
  }
}

watch(() => props.id, () => {
  init()
}, { immediate: true })

</script>

<style lang="scss" scoped>
.id2HeaOicImg {
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
}
</style>
