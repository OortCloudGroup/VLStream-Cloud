<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-11-15 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <oort-img class="id2HeaOicImg" :src="srcUrl" :style="round?'border-radius: 100%':''" default-type="user" />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useUserStore } from '@/store/modules/useraPaas'
import { getUserList } from '@/api/system/directory'
import defaultImage from '@/assets/img/tx.png'

const store: any = useUserStore()
const props = defineProps({
  src: {
    type: String,
    default: null
  },
  id: {
    type: String,
    required: true,
    default: ''
  },
  round: {
    type: Boolean,
    default: false
  }
})
const srcUrl = ref<any>(defaultImage)

// 根据外部图片或本地用户目录刷新头像，任何目录异常都回退到默认头像。
const init = () => {
  if (props.src) {
    srcUrl.value = props.src
    return
  }
  srcUrl.value = defaultImage
  void getUserInfoFn()
}

// 从本地用户目录加载头像，避免旧 APaaS 用户信息缺失时中断 Vue 更新队列。
const getUserInfoFn = async() => {
  if (!props.id) return

  // 是否vuex中已经存在这个用户，避免重复请求
  const cachedUser = store.userListStore?.[props.id]
  if (cachedUser) {
    srcUrl.value = cachedUser.photo || defaultImage
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
      srcUrl.value = user.photo || defaultImage
    }
  } catch (error) {
    srcUrl.value = defaultImage
  }
}

watch(() => [props.id, props.src], () => {
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
