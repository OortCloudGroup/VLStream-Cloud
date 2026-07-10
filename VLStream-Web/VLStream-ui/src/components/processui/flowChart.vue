<template>
  <div ref="viewportRef" class="canvs_panel">
    <div class="zoom">
      <el-icon class="el-icon--right reset_btn" @click="reset">
        <RefreshRight />
      </el-icon>
      <div class="zoom-out" :class="nowVal == 50 && 'disabled'" @click="zoomSize(1)" />
      <span>{{ nowVal }}%</span>
      <div class="zoom-in" :class="nowVal == 300 && 'disabled'" @click="zoomSize(2)" />
    </div>
    <div v-if="isReady" ref="nodeGroupRef" class="flow_node_container" :style="`transform: scale(${ nowVal / 100})`" @mousedown="onMouseDown">
      <start-node :is-finished="finished.length > 0" />
      <node-wrap v-model:node-config="copyNodeConfig" :is-edit="isEdit" :show-type="showType" :finished="finished" />
      <end-node :is-finished="isEndFinished" />
    </div>
    <div v-if="!isEdit" class="finished_tips">
      <span />
      <span>未完成</span>
      <span />
      <span>已完成</span>
    </div>
  </div>
</template>

<script setup>

import { ref, watch, defineExpose, computed, onMounted } from 'vue'
import StartNode from './flow/startNode.vue'
import NodeWrap from './flow/nodewrap.vue'
import EndNode from './flow/endNode.vue'
import { useErrorMsgStoreHook } from '@/store/modules/useErrorMsg'
// import { ElMessage } from 'element-plus'
import { useFlowStore } from '@/store/modules/flow'
import { getForm } from '@/api/processui'
import { extractFormFields } from './flowProp/utils/formFieldExtractor'

const props = defineProps({
  nodeConfig: {
    type: Object,
    default: () => ({})
  },
  isEdit: {
    type: Boolean,
    default: true
  },
  finished: { // 已完成的节点
    type: Array,
    default: () => []
  },
  unFinished: { // 未完成的节点
    type: Array,
    default: () => []
  },
  showType: { // 属性的弹框类型，0 dialog 1 抽屉
    type: Number,
    default: 1
  },
  nowVal: { // 属性的弹框类型，0 dialog 1 抽屉
    type: Number,
    default: 100
  }
})

const isEndFinished = computed(() => { return props.finished.length > 0 && props.unFinished.length === 0 })

const copyNodeConfig = ref(props.nodeConfig)

watch(() => props.nodeConfig, () => {
  copyNodeConfig.value = props.nodeConfig
})

const errorStore = useErrorMsgStoreHook()
const emits = defineEmits(['update:nodeConfig'])
watch(() => copyNodeConfig.value, () => {
  // if (!copyNodeConfig.value.formKey) {
  //   errorStore.addNodeError({ id: copyNodeConfig.value.id, msg: ['发起人节点表单不能为空'] })
  // } else {
  //   errorStore.removeNodeError(copyNodeConfig.value.id)
  // }
  // if (errorStore.nodeError.length > 0) {
  //   console.warn(errorStore.nodeError)
  //   ElMessage.warning(errorStoreMsg.value ?? '请检查节点属性')
  //   return
  // } else {
  //   emits('update:nodeConfig', copyNodeConfig.value)
  // }
  emits('update:nodeConfig', copyNodeConfig.value)
})

const isflowChartConfigOk = computed(() => {
  return !errorStore.nodeError.length
})

const errorStoreMsg = computed(() => {
  // 去除重复的错误信息
  return errorStore.nodeError.map(item => {
    return item.msg
  })
})

defineExpose({ isflowChartConfigOk, errorStoreMsg })

const nowVal = ref(props.nowVal)
function zoomSize(type) {
  reset()
  if (type === 1) {
    if (nowVal.value === 50) {
      return
    }
    nowVal.value -= 10
  } else {
    if (nowVal.value === 300) {
      return
    }
    nowVal.value += 10
  }
}

const flowStore = useFlowStore()
function getFormValue() {
  if (copyNodeConfig.value.formKey) {
    const params = {
      formId: copyNodeConfig.value.formKey
    }
    getForm(params).then(res => {
      if (!copyNodeConfig.value.formProperties) {
        copyNodeConfig.value.formProperties = []
      }
      if (res.code === 200) {
        flowStore.setCurrentFlowForm(res.data)
        try {
          let tempFormProperties = []
          const jsonList = JSON.parse(res.data.content)
          tempFormProperties = extractFormFields(jsonList)

          // 如果当前的formProperties为空，则从表单里面获取
          if (copyNodeConfig.value.formProperties.length === 0) {
            flowStore.setFormFiledList(JSON.parse(JSON.stringify(tempFormProperties)))
          } else {
            // 对比下是否有差异，有差异提示用户
            let newTempName = tempFormProperties.map(item => item.id)
            let oldTempName = copyNodeConfig.value.formProperties.map(item => item.id)
            if (JSON.stringify(newTempName) !== JSON.stringify(oldTempName)) {
              // 表单数据有差异
              flowStore.setFormFiledList(JSON.parse(JSON.stringify(tempFormProperties)))
            } else {
              flowStore.setFormFiledList(JSON.parse(JSON.stringify(copyNodeConfig.value.formProperties)))
            }
          }
        } catch (error) {
          console.log(error)
        }
      }
    })
  }
}

const reset = () => {
  lastX = 0
  lastY = 0
  const content = nodeGroupRef.value
  content.style.left = `${0}px`
  content.style.top = `${0}px`
}

const nodeGroupRef = ref(null)
const viewportRef = ref(null)
let lastX = 0
let lastY = 0
const onMouseDown = (e) => {
  const content = nodeGroupRef.value
  // const viewport = viewportRef.value
  const startX = e.clientX
  const startY = e.clientY

  const initialLeft = lastX
  const initialTop = lastY
  content.classList.add('pressed')
  const onMouseMove = (event) => {
    let x = initialLeft + event.clientX - startX
    let y = initialTop + event.clientY - startY

    // // Ensure the draggable content stays within the bounds of the viewport
    // x = Math.max((viewport.clientWidth - content.clientWidth) / 2, Math.min(x, 0))
    // y = Math.max((viewport.clientHeight - content.clientHeight) / 2, Math.min(y, 0))
    lastX = x
    lastY = y
    content.style.left = `${x}px`
    content.style.top = `${y}px`
  }
  const onMouseUp = () => {
    content.classList.remove('pressed')
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

const isReady = ref(false)
onMounted(() => {
  // 清楚nodeErrorstor的报错
  errorStore.clearNodeError()
  isReady.value = true
  if (props.isEdit) {
    // 获取表单
    getFormValue()
  }
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

  .canvs_panel {
    display: flex;
    position: absolute;
    inset: 0;
    width: calc(100% - 5px);
    height: calc(100% - 58px);
    overflow: hidden!important;
  }

  .flow_node_container {
    margin: 0 auto;
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: row;
    position: relative;
    height: fit-content;
    width: fit-content;
    min-height: 100%;
    user-select: none;
  }

  .flow_node_container.pressed {
        cursor: grabbing;
    }

  .bottom {
      position: absolute;
      background-color: #fff;
      width: 96%;
      height: 48px;
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 20px;
      bottom:0px;
      left: 0px;
      right: 0px;
  }

  .reset_btn {
    width: 24px;
    height: 24px;
    margin: 0 4px;
    font-size: 20px;
    cursor: pointer;
  }

  .zoom {
      display: flex;
      position: absolute;
      -webkit-box-align: center;
      -ms-flex-align: center;
      align-items: center;
      -webkit-box-pack: justify;
      -ms-flex-pack: justify;
      justify-content: space-between;
      height: 40px;
      width: 180px;
      right: 40px;
      margin-top: 30px;
      z-index: 10
  }

  .zoom .zoom-in,
  .zoom .zoom-out {
      width: 30px;
      height: 30px;
      background: #fff;
      color: #c1c1cd;
      cursor: pointer;
      background-size: 100%;
      background-repeat: no-repeat
  }

  .zoom .zoom-out {
      background-image: url('@/assets/img/processui/flownode/zoom_in.png')
  }

  .zoom .zoom-out.disabled {
      opacity: .5
  }

  .zoom .zoom-in {
      background-image:url('@/assets/img/processui/flownode/zoom_out.png')
  }

  .zoom .zoom-in.disabled {
      opacity: .5
  }

  .finished_tips {
    position: absolute;
    bottom: 10px;
    left: 10px;
    display: flex;
    flex-direction: row;
    align-items: center;
    span {
      font-size: 14px;
      margin-left: 4px;
      color: var(--el-color-primary);
    }
    span:nth-of-type(1) {
      width: 28px;
      height: 14px;
      border-radius: 2px;
      background-color: var(--el-color-primary);
    }
    span:nth-of-type(3) {
      border-radius: 2px;
      margin-left: 12px;
      width: 28px;
      height: 14px;
      background-color: #04a904;
    }
    span:nth-of-type(4) {
      color: #04a904;
    }
  }

</style>
