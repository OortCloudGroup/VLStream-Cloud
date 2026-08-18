<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-11-15 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <div class="detailsStep">
    <div class="flexRowAC headerStep">
      <div class="tabs flexRowAC">
        <el-tabs v-model="activeName" class="tenanat-tabs">
          <el-tab-pane label="基础信息" name="1">
            <template #label>
              <span class="flexRowAC">
                <oort-svg-icon name="1" width="14" height="14" />
                <span>&nbsp;基础信息</span>
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane label="表单设计" name="2">
            <template #label>
              <span class="flexRowAC">
                <oort-svg-icon name="2" width="14" height="14" />
                <span>&nbsp;表单设计</span>
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane label="流程设计" name="3">
            <template #label>
              <span class="flexRowAC">
                <oort-svg-icon name="3" width="14" height="14" />
                <span>&nbsp;流程设计</span>
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane label="权限" name="4">
            <template #label>
              <span class="flexRowAC">
                <oort-svg-icon name="4" width="14" height="14" />
                <span>&nbsp;权限</span>
              </span>
            </template>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
    <div class="step_contentOut flexRowAC">
      <div class="step_content">
        <div v-if="processTitle" class="process_t flexRowAC">
          <router-link to="" class="nav_title" @click="router.back()">
            返回
          </router-link>
          <div class="nav_title lth">
            /&nbsp;&nbsp;
            <span>{{ processTitle }}</span>
          </div>
        </div>
        <div v-if="activeName==='1'&&dItem" class="step3Box">
          <step-one :item="dItem" />
        </div>
        <div v-if="activeName==='2'&&currentForm" class="step3Box">
          <form-designer-page
            ref="formDesignerPageRef"
            :form-object="currentForm"
          />
        </div>
        <div v-if="activeName==='3'&&currentModel" class="step3Box">
          <flow-designer-page :confirm-tip="false" :model-object="currentModel" @closeDrawer="closeDrawerConfirm" @close="router.back()" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deployModel, getModel } from '@/api/processui'
import StepOne from '@/pages/task_center/views/page/components/stepOne.vue'
import FormDesignerPage from '@/pages/processui/views/page/workOrderDesignManage/components/formDesignerPage.vue'
import FlowDesignerPage from '@/pages/processui/views/page/workOrderDesignManage/components/flowDesignerPage.vue'
import { getStoredWorkOrderAppContext } from '@/utils/workOrderAppContext'

const router = useRouter()
const route = useRoute()
const activeName = ref<any>('1')
let processTitle = ref('')
let dItem = ref('')
const currentForm = ref(null) // 表单设计
const currentModel = ref(null) // 流程设计
let appObj = ref<any>('') // 任务管理app
appObj.value = getStoredWorkOrderAppContext() || {}

// 当前模型
const getModelFn = async() => {
  let data = {
    modelId: route.query?.modelId
  }
  let res:any = await getModel(data)
  if (res.code === 200) {
    // 当前表单
    if (res.data?.formId) currentForm.value = { ...res.data }
    let resData = res.data
    processTitle.value = resData.modelName
    dItem.value = resData
    // 表单设置-工单1 流程0
    resData['formDesignType'] = 1
    // 通知节点：工单有,流程无(workOrderAppAll:true)
    resData['notifyNode'] = route.query?.workOrderAppAll
    resData['appId'] = resData.modelId
    resData['applicationId'] = appObj.value?.applicationId
    currentModel.value = resData
  }
}

// 关闭即部署
const closeDrawerConfirm = (val) => {
/*   ElMessageBox.confirm('部署该模型，将该模型设置为最新，是否继续', '部署', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    let data = {
      modelId: val || route.query?.modelId
    }
    deployModel(data).then((res: any) => {
      if (res.code === 200) {
        ElMessage.success('操作成功')
        router.back()
      }
    })
  }).catch(() => {
  }) */
  let data = {
    modelId: val || route.query?.modelId
  }
  deployModel(data).then((res: any) => {
    if (res.code === 200) {
      // ElMessage.success('操作成功')
      router.back()
    }
  })
}

onMounted(() => {
  if (route.query) activeName.value = route.query?.step || '1'
  if (route.query?.modelId) getModelFn()
})

</script>
<style lang="scss" scoped>
.detailsStep {
  height: calc(100vh - 60px);
  background-color: var(--common-bg-color);

  .headerStep {
    position: relative;

    .back {
      top: 20px;
      left: 20px;
      position: absolute;
    }

    .tabs {
      width: 100%;
      justify-content: center;
    }
  }
}

.step_contentOut {
  width: 100%;
  padding-top: 20px;
  justify-content: center;
  height: calc(100% - 64px);

  .step_content {
    width: 70%;
    height: 100%;
    border-radius: 10px 10px 0 0;
    background-color: #fff;
    position: relative;
  }

  .step3Box{
    width: 100%;
    height: 100%;
    overflow: auto;
    padding-bottom: 40px;
  }
}

.process_t {
  left: 0;
  top: -60px;
  position: absolute;
  gap: 8px;
  font-size: 16px;

  &:before {
    content: '';
    width: 3px;
    height: 18px;
    background-color: var(--el-color-primary);
  }

  .nav_title {
    line-height: 18px;
    color: #333;
    margin-left: 12px;

    &.lth {
      color: #999;
    }
  }
}

:deep(.tenanat-tabs) {
  padding: 0 20px;
  fill: #999 !important;

  .el-tabs__item {
    color: #999999;
  }

  .el-tabs__item.is-active {
    color: var(--el-color-primary);

    .icon_svg {
      fill: var(--el-color-primary) !important;
    }
  }

  .icon_svg {
    fill: #999 !important;
  }
}

// tabs
:deep(.el-tabs__header) {
  padding-top: 10px;

  .el-tabs__nav-wrap::after {
    display: none;
  }

  .el-tabs__item.is-top {
    font-size: 16px;
    font-weight: 700;
  }
}
</style>
