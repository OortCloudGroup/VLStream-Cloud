<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<!--
 * @Created by:
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2025-5-15 11:45:51
 * @Last Modified by:
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <div class="stepOne">
    <div class="preview_form">
      <!-- <span class="flexRowAC basic"><span>工单名称</span>{{ infoTemp?.title || '--' }}</span>
      <span class="flexRowAC basic"><span>工单类型</span>{{ infoTemp?.categoryName || '--' }}</span>
      <span class="flexRowAC basic"><span>发起人</span>{{ props.item?.createBy || '--' }}</span>
      <span class="flexRowAC basic"><span>发起人部门</span>{{ props.item?.deptName || '--' }}</span>
      <span class="flexRowAC basic"><span>流程名称</span>{{ infoTemp?.processName || '--' }}</span>
      <span class="flexRowAC basic"><span>流程标识</span>{{ infoTemp?.processKey || '--' }}</span>
      <span class="flexRowAC basic"><span>描述</span>{{ infoTemp?.createTime || '--' }}</span>
      <span class="flexRowAC basic"><span>创建时间</span>{{ infoTemp?.createTime || '--' }}</span> -->

      <!-- model 0 Add 、1, copy , 2 work order1 workflow0 -->
      <work-order-built :is-type="1" :type="infoTemp?.modelId?2:0" :app="infoTemp.appObj" :item="infoTemp" @close="router.back()" @handle="getListFn" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, reactive } from 'vue'
import { useUserStore } from '@/store/modules/useraPaas'
import WorkOrderBuilt from '@/pages/Intelligent_approval/views/page/components/workOrderBuilt.vue'
import { listModel } from '@/api/processui'
import { useRouter } from 'vue-router'

const router = useRouter()
const store: any = useUserStore()

// const form = reactive({
//   processname: '',
//   description: '',
//   attachmentUrls: ''
// })
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  modelName: undefined,
  category: undefined,
  modelKey: undefined
})
const props: any = defineProps(['item'])
let infoTemp = ref<any>({})

watch(() => props.item, (val) => {
  infoTemp.value = val
}, { immediate: true })

const tableData = ref<any>([])
// model
function getListFn() {
  let data = {
    accessToken: store.userInfo.accessToken,
    ...queryParams,
    workOrderCategory: infoTemp.value.appObj?.appId
  }
  listModel(data).then((response: any) => {
    tableData.value = response.rows || []
    tableData.value.map((item: any) => {
      item['categoryName'] = infoTemp.value.appObj?.['applicationName'] || '--'
    })
  })
}
</script>
<style lang="scss" scoped>
.stepOne {
  padding: 20px;
  .preview_form {
    padding: 20px 0 40px;

    .basic {
      padding-bottom: 20px;

      > span {
        flex-shrink: 0;
        width: 120px;
        text-align: right;
        padding-right: 20px;
        font-size: 14px;
        color: #939393;
        font-weight: 400;
      }
    }
  }
}
</style>
