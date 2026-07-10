<template>
  <div class="person-info-card">
    <img v-if="userInfo.photo" :src="userInfo.photo" class="person-info-card-head-pic" />
    <img v-else src="@/assets/img/tx.png" class="person-info-card-head-pic" />
    <div>
      <div class="name_box">
        {{ userInfo.name }}
      </div>
      <div class="name_py">
        {{ userInfo.deptName }}
        <br v-if="userInfo.subDeptName" />
        {{ userInfo.subDeptName }}
      </div>
      <div v-if="userInfo.phone" class="phone_box">
        {{ userInfo.phone }}
        <img src="@/assets/img/maintenance/phone.png" alt="" class="phone_icon" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { getUserInfoList } from '@/api/sso_apaas'
import { useUserStore } from '@/store/modules/useraPaas'

const store = useUserStore()

interface UserInfo {
  name: string
  deptName: string
  subDeptName: string
  phone: string
}

const props = defineProps<{
  uuid: string
}>()

const userInfo = ref<UserInfo>({
  name: '',
  deptName: '',
  subDeptName: '',
  phone: '',
  photo: ''
})

const getUserInfo = async() => {
  if (!props.uuid) return
  const params = {
    user_id: [props.uuid],
    accessToken: store.token,
    tenant_id: store.tenantId
  }
  const res: any = await getUserInfoList(params)
  if (res.code === 200 && res.data && res.data.list && res.data.list.length > 0) {
    const user = res.data.list[0]
    let deptName = user.dept_list.map((item: any) => item.deptinfo.dept_name).join('｜')
    let jobName = user.dept_list.flatMap((item: any) => item.deptinfo.job || []).join('｜')
    let phone = user.dept_list
      .map((item: any) => item.deptinfo.ex_data.phone || user.phone)
      .filter(phone => phone && phone.trim() !== '') // 过滤掉空字符串和只包含空格的字符串
      .join('｜')
    userInfo.value = {
      name: user.user_name,
      deptName: deptName,
      subDeptName: jobName,
      phone: phone,
      photo: user.photo
    }
  }
}

watch(() => props.uuid, (newUuid) => {
  if (newUuid) {
    getUserInfo()
  }
}, { immediate: true })

onMounted(() => {
  if (props.uuid) {
    getUserInfo()
  }
})
</script>

<style scoped lang="scss">
.person-info-card {
  display: flex;
  align-items: center;
  margin-right: 15px;
  font-family: MicrosoftYaHei;
  font-size: 14px;
  color: #999999;
  font-weight: 400;
  border: 0.5px solid #fcfcfc;
  border-radius: 6px;
  padding: 8px;

  .person-info-card-head-pic {
    width: 42px;
    height: 42px;
    margin: 8px;
    border-radius: 100%;
  }

  .name_box {
    font-family: MicrosoftYaHei;
    font-size: 14px;
    color: #333333;
    line-height: 19px;
    font-weight: 400;
    margin-bottom: 5px;
  }

  .phone_box {
    display: flex;
    align-items: center;
  }

  .phone_icon {
    width: 20px;
    margin-left: 5px;
  }
}

.name_py {
  font-size: 12px;
  color: #999999;
  line-height: 16px;
  font-weight: 400;
  max-width: 140px;
  word-break: break-all;
}
</style>
