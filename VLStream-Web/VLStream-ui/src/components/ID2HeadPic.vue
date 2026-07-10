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
import { onMounted, ref, watch } from 'vue'
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
  let data = {
    accessToken: store.userInfo.accessToken,
    user_id: [props.id],
    tenant_id: store.tenantId
  }
  data['hideLoading'] = true
  getUserList(data).then((res: any) => {
    if (res.code === 200) {
      if (res.data.list.length > 0) {
        let tempObj = res.data.list[0]
        store.addUser(tempObj)
        srcUrl.value = tempObj.photo
      }
    }
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
