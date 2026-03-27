<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-11-15 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <oort_image class="id2HeaOicImg" :src="srcUrl" :style="round?'border-radius: 100%':''" default-type="user" />
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useUserStore } from '@/store/modules/useraPaas'
import { batchFetchUser } from '@/utils/userBatch'
import defaultImage from '@/assets/img/tx.png'
import Oort_image from '@/components/image/oort_image.vue'

const store: any = useUserStore()
const props = defineProps({
  src: {
    type: String,
    default: null
  },
  id: {
    type: String,
    default: ''
  },
  round: {
    type: Boolean,
    default: false
  }
})
let srcUrl = ref<any>(defaultImage)
const init = () => {
  if (props.src === null) {
    getUserInfoFn()
  } else {
    srcUrl.value = props.src
  }
}

// oort_photo
const getUserInfoFn = () => {
  // 是否vuex中已经存在这个用户，避免重复请求
  if (store.userListStore[props.id]) {
    srcUrl.value = store.userListStore[props.id].photo
    return
  }
  batchFetchUser(props.id)
    .then((user: any) => {
      srcUrl.value = user?.photo || defaultImage
    })
    .catch(() => {
      srcUrl.value = defaultImage
    })
}

watch(() => props.id, () => {
  init()
})

onMounted(() => {
  init()
})

</script>

<style lang="scss" scoped>
.id2HeaOicImg {
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
}
</style>
