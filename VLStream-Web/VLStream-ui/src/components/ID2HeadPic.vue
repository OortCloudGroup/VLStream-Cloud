<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<!--
 * @Created by:
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-11-15 11:45:51
 * @Last Modified by:
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

// user new , .
const init = () => {
  if (props.src) {
    srcUrl.value = props.src
    return
  }
  srcUrl.value = defaultImage
  void getUserInfoFn()
}

// from user Load , old APaaS userinfo in Vue new .
const getUserInfoFn = async() => {
  if (!props.id) return

  // whether vuex in already in user,
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
