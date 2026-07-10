<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-11-15 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <span>{{ name?name: deptId }}</span>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDeptList } from '@/api/system/directory'
import { useUserStore } from '@/store/modules/useraPaas'
const props = defineProps({
  deptId: {
    type: String,
    default: ''
  }
})

const name = ref('')
const store = useUserStore()
const getDeptInfo = () => {
  const params = {
    accessToken: store.token,
    dept_id: [props.deptId],
    tenant_id: store.tenantId
  }
  getDeptList(params).then(res => {
    if (res.code === 200) {
      if (res.data.list.length > 0) {
        name.value = res.data.list[0].dept_name
      }
    }
  })
}

onMounted(() => {
  getDeptInfo()
})

</script>
