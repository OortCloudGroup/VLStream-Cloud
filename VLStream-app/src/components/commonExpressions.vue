<template>
  <div class="common_wrapper">
    <div class="add_common_btn" @click="addCommonOpinion">
      <van-icon name="plus" class="add_common_btn_icon" />
      添加为常用语
    </div>
    <div class="common_btn_list">
      <div v-for="(item,index) in listData" :key="index" class="common_content">
        <div @click="clickContent(item.content)">
          {{ item.content }}
        </div>
        <img class="common_close" src="@/assets/img/patrolManagement/delete.png" alt="" @click="delCommonOpinion(item.id)" />
      </div>
    </div>
  </div>
</template>
<script setup>
import { myOpinionList, myOpinionDel, myOpinionSave } from '@/api/unifiedUsert/logManage'
import { defineProps, onMounted, ref, defineEmits } from 'vue'
import { useUserStore } from '@/store/modules/useraPaas'
const store = useUserStore()
const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})
const listData = ref([])
const getCommonOpinionsList = async() => {
  const params = {
    accessToken: store.token,
    is_open: 0, // 0 个人 1 公开
    page: 0,
    pagesize: 999
  }
  const res = await myOpinionList(params)
  if (res.code === 200) {
    listData.value = res.data.list
  }
}
const addCommonOpinion = async() => {
  const params = {
    accessToken: store.token,
    content: props.content,
    is_open: 0
  }
  const res = await myOpinionSave(params)
  if (res.code === 200) {
    getCommonOpinionsList()
  }
}
const delCommonOpinion = async(id) => {
  const params = {
    accessToken: store.token,
    id: id
  }
  const res = await myOpinionDel(params)
  if (res.code === 200) {
    getCommonOpinionsList()
  }
}
onMounted(() => {
  getCommonOpinionsList()
})
const emit = defineEmits(['selectContent'])
const clickContent = (content) => {
  emit('selectContent', content)
}
</script>
<style scoped lang="scss">
.common_wrapper{
    width: 100%;

.add_common_btn {
    width: 120px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: MicrosoftYaHei;
font-size: 12px;
color: #1A53FF;
letter-spacing: 0;
font-weight: 400;
    padding:10px 0px;
    margin-top: 10px;
    cursor: pointer;
    background: rgba(26,83,255,0.12);
    border-radius: 30px;

    .add_common_btn_icon {
        font-size: 16px;
        font-weight: 700;
        margin-right: 5px;
    }

    &:hover {
        background: var(--van-primary-color-light);
        opacity: 0.8;
    }
}

.common_btn_list {
    display: flex;
    flex-wrap: wrap;
    align-items: center;

    .common_content {
        display: flex;
        align-items: center;
        justify-content: space-between;
        font-family: MicrosoftYaHei;
        font-size: 12px;
        font-family: MicrosoftYaHei;
font-size: 12px;
color: #1890FF;
letter-spacing: 0;
font-weight: 400;
background: rgba(24,144,255,0.12);
        font-weight: 400;
        padding:10px;
        margin: 10px 10px 0 0;
        border-radius: 5px;
        cursor: pointer;
        // position: relative;

        &:hover {
            background: var(--van-primary-color-light);
            opacity: 0.8;
        }
        .common_close {
            width: 13px;
            height: 13px;
            margin-left: 7px;
        }

        &:hover .common_close {
            display: block;
        }
    }
}
}
</style>
