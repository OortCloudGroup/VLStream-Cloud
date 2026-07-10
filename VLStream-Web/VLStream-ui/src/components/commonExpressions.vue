<template>
  <div class="common_wrapper">
    <div
      class="add_common_btn"
      :class="{ 'disabled': !content || content.trim() === '' }"
      @click="addCommonOpinion"
    >
      <el-icon class="add_common_btn_icon">
        <Plus />
      </el-icon>添加为常用语
    </div>
    <div class="common_btn_list">
      <div v-for="(item,index) in listData" :key="index" class="common_content">
        <div @click="clickContent(item.content)">
          {{ item.content }}
        </div>
        <el-icon class="common_close" @click="delCommonOpinion(item.id)">
          <CircleCloseFilled />
        </el-icon>
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
  if (!props.content || props.content.trim() === '') {
    return
  }
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
    font-family: MicrosoftYaHei;
    font-size: 12px;
    color: var(--el-color-primary);
    font-weight: 400;
    background: var(--el-color-primary-hb);
    border: 1px solid var(--el-color-primary-hb);
    border-radius: 16px;
    padding: 0 10px;
    margin-top: 10px;
    cursor: pointer;

    .add_common_btn_icon {
        font-size: 16px;
        font-weight: 700;
        margin-right: 5px;
    }

    &:hover {
        background: var(--el-color-primary-hb);
        opacity: 0.8;
    }

    &.disabled {
        color: #c0c4cc;
        background: #f5f7fa;
        border-color: #e4e7ed;
        cursor: not-allowed;
        opacity: 0.6;

        &:hover {
            background: #f5f7fa;
            opacity: 0.6;
        }
    }
}

.common_btn_list {
    display: flex;
    flex-wrap: wrap;
    align-items: center;

    .common_content {
        font-family: MicrosoftYaHei;
        font-size: 12px;
        color: var(--el-color-primary);
        background: var(--el-color-primary-hb);
        font-weight: 400;
        padding: 0 10px;
        margin: 10px 10px 0 0;
        border-radius: 5px;
        cursor: pointer;
        position: relative;

        &:hover {
            background: var(--el-color-primary-hb);
            opacity: 0.8;
        }
        .common_close {
            position: absolute;
            right: -5px;
            top: -5px;
            cursor: pointer;
            font-size: 18px;
            display: none;
        }

        &:hover .common_close {
            display: block;
        }
    }
}
}
</style>
