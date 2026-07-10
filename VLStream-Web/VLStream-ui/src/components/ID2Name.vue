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
import { onMounted, ref, watch } from 'vue'
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
const init = () => {
  getUserInfoFn()
}

// oort_photo
const getUserInfoFn = () => {
  // 是否vuex中已经存在这个用户，避免重复请求
  if (store.userListStore[props.id]) {
    name.value = store.userListStore[props.id][props.valueKey]
    return
  }
  if (props.id) {
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
          name.value = tempObj[props.valueKey]
        }
      }
    })
  }
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
