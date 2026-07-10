<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-11-15 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <div v-if="showFlowChart" class="flow_designer_page">
    <FlowChart ref="flowChartRef" v-model:node-config="nodeConfig" />
    <div class="bottom">
      <div class="button_group">
        <el-button v-preReClick @click="dialogVisible=true">
          其他设置
        </el-button>
      </div>
      <div class="button_group">
        <el-button @click="cancel">
          取消
        </el-button>
        <el-button v-preReClick type="primary" @click="saveFlow">
          保存
        </el-button>
      </div>
    </div>
    <el-dialog
      v-model="dialogVisible"
      title="其他设置"
      align-center
      width="40%"
      :close-on-click-modal="false"
    >
      <el-form ref="ruleFormRef" :model="form" :rules="rules" label-width="120">
        <el-form-item label="站内消息推送">
          <el-switch
            v-model="form.notifyAllSteps"
            active-color="#13ce66"
            inactive-color="#999"
            :active-value="true"
            :inactive-value="false"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div style="text-align: right;">
          <el-button @click="dialogVisible = false">
            取消
          </el-button>
          <el-button v-preReClick type="primary" @click="dialogVisible = false">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { ref, onMounted, provide, reactive } from 'vue'
import { getJSONModel, saveModel } from '@/api/processui/index.ts'
import { saveModel_a } from '@/api/processui/approval'
import { ElMessage, ElMessageBox } from 'element-plus'
import { shortRandomStr } from '@/utils/index'

const route = useRoute()
const ruleFormRef = ref()
const form = reactive({
  notifyAllSteps: true
})
const rules = reactive({
})
const dialogVisible = ref(false)

const id = shortRandomStr()
const nodeConfig = ref({
  id: id,
  nodeType: 'initiator',
  'nodeName': '发起人',
  'type': 0,
  'childNode': {
    id: 'end',
    nodeType: 'end',
    pid: 'root',
    type: 999,
    nodeName: '结束'
  }
})

// 提示节点必填的信息 {id, name, msg}
const errorInfo = ref([])
provide('flowDesign', {
  nodesError: errorInfo
})

const props = defineProps({
  modelObject: {
    type: Object,
    default: null
  },
  confirmTip: { // 是否将此模型保存为新版本-审批默认新版本
    type: Boolean,
    default: true
  },
  processuiTs: { // 是否是工单
    type: Boolean,
    default: true
  }
})
provide('flowDesignerPage', props.modelObject)

const emits = defineEmits(['close', 'closeDrawer'])
function cancel() {
  emits('close')
}

async function confirmSave(isNewVersion) {
  // 发起人前面加一个startEvent
  nodeConfig.value.pid = 'root'
  const startNode = {
    id: 'root',
    nodeType: 'start',
    type: 0,
    nodeName: '开始',
    childNode: nodeConfig.value,
    formKey: nodeConfig.value.formKey
  }
  const params = {
    modelId: props.modelObject.modelId,
    id: '1',
    code: props.modelObject.modelKey,
    name: props.modelObject.modelName,
    process: startNode,
    newVersion: isNewVersion
  }
  // 发起人-自动创建表单，修改传formId
  if (route.query && route.query.formId) params['formId'] = startNode.formKey
  params['formId'] = startNode?.formKey
  params.notifyAllSteps = form.notifyAllSteps
  let res = props.processuiTs ? await saveModel(params) : await saveModel_a(params)
  if (res.code === 200) {
    if (props.confirmTip) ElMessage.success('保存成功')
    emits('closeDrawer', res.msg)
  }
}

const flowChartRef = ref(null)
function saveFlow() {
  // 保存模型
  // console.log('最终保存的流程数据----；-', JSON.parse(JSON.stringify(nodeConfig.value)))
  // return
  if (flowChartRef.value && !flowChartRef.value?.isflowChartConfigOk) {
    ElMessage.warning(flowChartRef.value.errorStoreMsg.join(','))
    return
  }
  if (!props.confirmTip) {
    confirmSave(true)
  } else {
    ElMessageBox.confirm(
      '是否将此模型保存为新版本？',
      '提示',
      {
        distinguishCancelAndClose: true,
        confirmButtonText: '保存为新版本',
        cancelButtonText: '仅保存',
        type: 'warning'
      }
    ).then(() => {
      confirmSave(true)
    }).catch((value) => {
      if (value === 'cancel') {
        confirmSave(false)
      }
    })
  }
}

const showFlowChart = ref(false)
function getFlowDetail() {
  // 获取流程详情
  const params = {
    modelId: props.modelObject.modelId
  }
  getJSONModel(params).then(res => {
    if (res.code === 200 && res.data?.jsonContent) {
      try {
        // 去除第一个开始节点
        nodeConfig.value = JSON.parse(res.data.jsonContent).process.childNode
        showFlowChart.value = true
        let tempContentObj = JSON.parse(res.data.jsonContent)
        form.notifyAllSteps = tempContentObj.notifyAllSteps
        // console.log(JSON.parse(res.data.jsonContent).process)
      } catch (error) {
        console.error(error)
      }
    }
    showFlowChart.value = true
  })
}

onMounted(() => {
  getFlowDetail()
})

</script>

<style lang="scss">
  .flow_designer_page {
      width: 100%;
      height: 100%;
      display: flex;
      background-color: #fff;
      flex-direction: column;
      align-items: center;
      position: relative;
  }

  .bottom {
      position: absolute;
      background-color: #fff;
      width: 96%;
      margin: 0 auto;
      height: 48px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 20px;
      bottom:0px;
      left: 0px;
      right: 0px;
  }

  .button_group button {
      width: 170px;
      height: 48px;
      border-radius: 2px;
  }

</style>
