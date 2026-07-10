<template>
  <div class="node_node" :class="{'node_inactive': isFinish}">
    <div v-if="copyNodeConfig.type < 99" class="node_wrap">
      <div class="node_warp_box" :class="{'error_node': !!errorInfo && isEdit}">
        <div class="node" @click="showPropDialog">
          <oort-svg-icon v-if="copyNodeConfig.type === 0" class="node_img" name="fqr_node" :color="isFinish?'var(--node-main-color)':'var(--el-color-primary)'" />
          <oort-svg-icon v-if="copyNodeConfig.type === 1" class="node_img" name="sp_node" width="100" :color="isFinish?'var(--node-main-color)':'var(--el-color-primary)'" />
          <oort-svg-icon v-if="copyNodeConfig.type === 2" class="node_img" name="cs_node" width="100" :color="isFinish?'var(--node-main-color)':'var(--el-color-primary)'" />
          <oort-svg-icon v-if="copyNodeConfig.type === 3" class="node_img" name="ys_node" width="100" :color="isFinish?'var(--node-main-color)':'var(--el-color-primary)'" />
          <oort-svg-icon v-if="copyNodeConfig.type === 4" class="node_img" name="cfq_node" width="100" :color="isFinish?'var(--node-main-color)':'var(--el-color-primary)'" />
          <!--通知节点：工单有,流程无-->
          <oort-svg-icon v-if="copyNodeConfig.type === 5" class="node_img" name="sp_node" width="100" :color="isFinish?'var(--node-main-color)':'var(--el-color-primary)'" />
          <!-- <img v-if="nodeConfig.type === 0" src="@/assets/img/processui/flownode/fqr.png" />
          <img v-if="nodeConfig.type === 1" src="@/assets/img/processui/flownode/spr.png" />
          <img v-if="nodeConfig.type === 2" src="@/assets/img/processui/flownode/csr.png" />
          <img v-if="nodeConfig.type === 3" src="@/assets/img/processui/flownode/yscl.png" />
          <img v-if="nodeConfig.type === 4" src="@/assets/img/processui/flownode/cfq.png" /> -->
          <span>{{ copyNodeConfig.nodeName }}</span>
          <el-icon v-if="copyNodeConfig.type !== 0 && isEdit" :size="clacPXToVW(20)" color="red" class="close" @click.stop="delNode">
            <CircleCloseFilled />
          </el-icon>
        </div>
        <el-icon v-if="!!errorInfo" color="red" :size="clacPXToVW(24)" class="error_icon_info" @click.stop="showErrorMsg()">
          <WarnTriangleFilled />
        </el-icon>
      </div>
      <addNode v-model:child-node-p="copyNodeConfig.childNode" :is-finish="isFinish" :p-node="copyNodeConfig" :is-edit="isEdit" />
    </div>
    <div v-if="copyNodeConfig.type === 100" class="node_branch">
      <div class="node_branch_wrap">
        <div v-if="isEdit" class="branch_arrow" />
        <div ref="wrapBox" class="node_branch_wrap_box">
          <button v-if="isEdit" class="add_branch" @click="addTerm">
            添加条件
          </button>
          <div v-for="(item,index) in copyNodeConfig.conditionNodes" :key="index" class="node_branch_item" :class="{'node_inactive': isTJFinish(item.id), 'right_boder': !isTJAllFinish(copyNodeConfig.id)}">
            <div class="branch_arrow" />
            <div class="node" @click="showPropDialog(item, index)">
              <!-- <img src="@/assets/img/processui/flownode/tjfz.png" /> -->
              <oort-svg-icon class="node_img" name="tj_node" width="100" :color="isTJFinish(item.id)?'var(--node-main-color)':'var(--el-color-primary)'" />
              <span>{{ item.nodeName }}</span>
              <el-icon v-if="isEdit" :size="clacPXToVW(20)" color="red" class="close" @click.stop.prevent="delTerm(index)">
                <CircleCloseFilled />
              </el-icon>
              <el-icon v-if="!!errorInfo.length > 0 && !!errorInfo[index] && !item.def" color="red" :size="clacPXToVW(24)" class="error_icon_info" @click.stop="showErrorMsg(errorInfo[index])">
                <WarnTriangleFilled />
              </el-icon>
            </div>
            <div v-if="index === 0" class="hide_more_border_top_left" />
            <div v-if="index === 0" class="hide_more_border_top_right" />
            <div v-if="index === copyNodeConfig.conditionNodes.length -1" class="hide_more_border_bottom_left" />
            <div v-if="index === copyNodeConfig.conditionNodes.length -1" class="hide_more_border_bottom_right" />

            <div v-if="isTJFinish(item.id)" class="active_line_right" :style="'height:' + tjLeftHeight + 'px;top:' + tjLeftTop" />
            <div v-if="isTJFinish(item.id) && isTJAllFinish(copyNodeConfig.id)" class="active_line_left" :style="'height:' + tjLeftHeight + 'px;top:' + tjLeftTop" />

            <addNode v-model:child-node-p="item.childNode" :is-finish="isTJFinish(item.id)" :p-node="item" :is-edit="isEdit" />
            <node-wrap v-if="item.childNode" v-model:node-config="item.childNode" :is-edit="isEdit" :show-type="showType" :finished="finished" />
          </div>
        </div>
        <addNode v-model:child-node-p="copyNodeConfig.childNode" :p-node="copyNodeConfig" :is-edit="isEdit" :is-finish="isTJAllFinish(copyNodeConfig.id)" />
      </div>
      <div v-if="isEdit" class="branch_arrow" />
    </div>
    <node-wrap v-if="copyNodeConfig.childNode" v-model:node-config="copyNodeConfig.childNode" :is-edit="isEdit" :show-type="showType" :finished="finished" />
    <el-dialog
      v-if="showType===0"
      v-model="propDialogVisible"
      class="dialog_manage"
      append-to-body
      :title="propsComponents[copyNodeConfig.type]?.title"
      top="5vh"
      :width="clacPXToVW(550)"
      draggable
    >
      <component
        :is="propsComponents[copyNodeConfig.type].component"
        v-if="propDialogVisible && propsComponents[copyNodeConfig.type]?.component "
        v-model:node-config="copyNodeConfig"
        :index="conditionNodeIndex"
        @close="closePropDialog"
      />
    </el-dialog>
    <el-drawer
      v-if="showType===1"
      v-model="propDialogVisible"
      :title="propsComponents[copyNodeConfig.type]?.title"
      direction="rtl"
      append-to-body
      class="drawer_mange"
      size="40%"
    >
      <component
        :is="propsComponents[copyNodeConfig.type].component"
        v-if="propDialogVisible && propsComponents[copyNodeConfig.type]?.component "
        v-model:node-config="copyNodeConfig"
        :index="conditionNodeIndex"
        @close="closePropDialog"
      />
    </el-drawer>
  </div>
</template>

<script setup>

import AddNode from './addNode.vue'
import NodeWrap from './nodeWrap.vue'
import { ref, reactive, onMounted, watch, computed, inject } from 'vue'
import ApprovalProp from '../flowProp/approvalProp.vue'
import NotifyNodeProp from '../flowProp/notifyNodeProp.vue'
import CSProp from '../flowProp/csProp.vue'
import YSCLProp from '../flowProp/ysclProp.vue'
import CFQProp from '../flowProp/cfqProp.vue'
import TJProp from '../flowProp/tjProp.vue'
import FQRProp from '../flowProp/fqrProp.vue'
import { shortRandomStr, clacPXToVW } from '@/utils'
import { useErrorMsgStoreHook } from '@/store/modules/useErrorMsg'
import { ElMessage, ElIcon, ElDialog, ElDrawer } from 'element-plus'

let props = defineProps({
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
  showType: { // 属性的弹框类型，0 dialog 1 抽屉
    type: Number,
    default: 1
  }
})

// 模型-列表数据
const flowDesignerPage = inject('flowDesignerPage')
const copyNodeConfig = ref(props.nodeConfig)

watch(() => props.nodeConfig, () => {
  copyNodeConfig.value = props.nodeConfig
})

// 判断除了条件分支的 节点是否完成ff
const isFinish = computed(() => {
  // 如果是条件分支 要判断节点是否完成
  if (props.nodeConfig.type === 100) {
    return false
  } else {
    return props.finished.includes(props.nodeConfig.id)
  }
})

// 判断整个条件的组是否完成了
const isTJAllFinish = () => {
  // 这里要怎么判断， 要判断条件组里面任意的 节点里面有没有直接连到 copyNodeConfig.childNode
  // 如果完成的包含后置节点，那么说明添加组已经完成
  if (props.finished.includes(copyNodeConfig.value?.childNode?.id)) {
    return true
  }
  // 找到条件组里面有完成的
  let nodeObj = copyNodeConfig.value.conditionNodes.find(item => props.finished.includes(item.id))
  if (!nodeObj) {
    return false
  }
  // 如果没有子节点或者没有条件分支，说明条件组已经完成
  if (!nodeObj.childNode && (!nodeObj.conditionNodes || nodeObj.conditionNodes.length === 0)) {
    return true
  }
  // 如果有子节点，需要判断子节点是否完成，递归，直到没有节点或者找到没有完成的子节点
  let isFinishChildNode = checkChildNodeFindIsFinish(nodeObj?.childNode)
  return isFinishChildNode
}

const checkChildNodeFindIsFinish = (nodeObj) => {
  if (!nodeObj) { // 如果没有子节点，则返回true
    return true
  }
  if (!props.finished.includes(nodeObj.id)) {
    return false
  }
  return checkChildNodeFindIsFinish(nodeObj.childNode)
}

// 判断单个条件是否完成
const isTJFinish = (nodeId) => {
  return props.finished.includes(nodeId)
}

const returnTJIndex = computed(() => {
  if (copyNodeConfig.value.type === 100) {
    if (!copyNodeConfig.value.conditionNodes || copyNodeConfig.value.conditionNodes.length === 0) { // 如果没有子节点，则返回true
      return -1
    } else {
      return copyNodeConfig.value.conditionNodes.findIndex(item => isTJFinish(item.id))
    }
  } else {
    return -1
  }
})

// // 计算条件节点是否在中间，如果在中间两边的线条不高亮
// const isMiddleTJ = computed(() => {
//   if (copyNodeConfig.value.type !== 100) {
//     return false
//   }
//   if (copyNodeConfig.value.conditionNodes.length > 2) {
//     return !(props.finished.includes(copyNodeConfig.value.conditionNodes[0].id) || props.finished.includes(copyNodeConfig.value.conditionNodes[copyNodeConfig.value.conditionNodes.length - 1].id))
//   } else {
//     return false
//   }
// })

const errorStore = useErrorMsgStoreHook()
const errorInfo = computed(() => {
  if (copyNodeConfig.value.type === 100) {
    let errorInfoMsg = []
    copyNodeConfig.value.conditionNodes.forEach(item => {
      if (!item.def) {
        let tempE = errorStore.getNodeErrorMsg(item.id)
        if (tempE) {
          errorInfoMsg.push(tempE.msg.join('!'))
        } else {
          errorInfoMsg.push('')
        }
      } else {
        // 这里要赞个位置
        errorInfoMsg.push('')
      }
    })
    return errorInfoMsg
  } else {
    console.warn('node prop check error:', errorStore.getNodeErrorMsg(copyNodeConfig.value.id))
    return errorStore.getNodeErrorMsg(copyNodeConfig.value.id)
  }
})
const showErrorMsg = (msg) => {
  ElMessage.warning(msg || errorInfo.value.msg.join('!'))
}

const propsComponents = reactive({
  0: { title: '发起节点', component: FQRProp },
  1: { title: flowDesignerPage?.notifyNode ? '处置节点' : '审批节点', component: ApprovalProp },
  2: { title: '抄送节点', component: CSProp },
  3: { title: '延时处理', component: YSCLProp },
  4: { title: '触发器', component: CFQProp },
  5: { title: '通知节点', component: NotifyNodeProp },
  99: { title: '分支条件', component: TJProp },
  100: { title: '分支条件', component: TJProp }
}

)

const conditionNodeIndex = ref(0)
const propDialogVisible = ref(false)
function showPropDialog(item, index) {
  if (item) {
    conditionNodeIndex.value = index
  }
  if (props.isEdit) {
    propDialogVisible.value = true
  }
}

let emits = defineEmits(['update:nodeConfig'])
const delNode = () => {
  if (copyNodeConfig.value.childNode) {
    copyNodeConfig.value.childNode.pid = copyNodeConfig.value.pid
  }
  emits('update:nodeConfig', copyNodeConfig.value.childNode)
  errorStore.removeNodeError(copyNodeConfig.value.id)
}

// 记录添加过第几个条件
const currentConLen = ref(copyNodeConfig.value.conditionNodes?.length + 1 || 2)
const addTerm = () => {
  currentConLen.value++
  copyNodeConfig.value.conditionNodes.push({
    nodeName: '条件' + currentConLen.value,
    nodeType: 'condition',
    type: 99,
    childNode: null,
    property: {},
    id: shortRandomStr(),
    pid: props.nodeConfig.id,
    def: false
  })
  emits('update:nodeConfig', props.nodeConfig)
  checkNodeRequireProp()
}

const delTerm = (index) => {
  const deleCondition = copyNodeConfig.value.conditionNodes.splice(index, 1)
  deleCondition.forEach(item => {
    errorStore.removeNodeError(item.id)
  })
  // props.nodeConfig.conditionNodes.map((item, index) => {
  //   item.priorityLevel = index + 1
  //   item.nodeName = `条件${index + 1}`
  // })
  emits('update:nodeConfig', copyNodeConfig.value)
  if (copyNodeConfig.value.conditionNodes.length === 1) {
    if (copyNodeConfig.value.childNode) {
      if (copyNodeConfig.value.conditionNodes[0].childNode) {
        reData(copyNodeConfig.value.conditionNodes[0].childNode, props.nodeConfig.childNode)
      } else {
        copyNodeConfig.value.conditionNodes[0].childNode = copyNodeConfig.value.childNode
      }
    }
    // 把pid指向调整过来
    copyNodeConfig.value.conditionNodes[0].childNode.pid = copyNodeConfig.value.pid
    emits('update:nodeConfig', copyNodeConfig.value.conditionNodes[0].childNode)
  }
}

const reData = (data, addData) => {
  if (!data.childNode) {
    data.childNode = addData
  } else {
    reData(data.childNode, addData)
  }
}

function closePropDialog() {
  propDialogVisible.value = false
  emits('update:nodeConfig', copyNodeConfig.value)
}

import { setFqrErrorMsg, setApprovalErrorMsg, setYsclErrorMsg, setCfqErrorMsg, setCsErrorMsg, setTjErrorMsg } from '@/utils/setNodeErrorMsg'

function checkNodeRequireProp() {
  // copyNodeConfig.type === 0"
  //  0:发起节点,1审批节点, 2抄送节点 3延时处理，4触发器， 99 100:独占网关
  switch (copyNodeConfig.value.type) {
    case 0:
      setFqrErrorMsg(copyNodeConfig.value, props.nodeConfig.id)
      break
    case 1:
      setApprovalErrorMsg(copyNodeConfig.value, props.nodeConfig.id)
      break
    case 2:
      setCsErrorMsg(copyNodeConfig.value, props.nodeConfig.id)
      break
    case 3:
      setYsclErrorMsg(copyNodeConfig.value, props.nodeConfig.id)
      break
    case 4:
      setCfqErrorMsg(copyNodeConfig.value, props.nodeConfig.id)
      break
    case 100:
      if (copyNodeConfig.value.conditionNodes) {
        copyNodeConfig.value.conditionNodes.forEach(item => {
          if (!item.def) {
            setTjErrorMsg(item, item.id)
          }
        })
      }
      break
  }
}

const tjLeftHeight = ref(0)
const isTop = ref(true)
const tjLeftTop = ref('100%')

// 监听属性变化
watch(() => copyNodeConfig.value, () => {
  checkNodeRequireProp()
})
const wrapBox = ref(null)

const calculateNodePosition = () => {
  if (!wrapBox.value || copyNodeConfig.value.type !== 100) return

  try {
    const rec = wrapBox.value.getBoundingClientRect()
    copyNodeConfig.value.x = rec.x + rec.width / 2
    const midTop = rec.top + rec.height / 2

    const childIndex = returnTJIndex.value
    if (childIndex === -1 || !wrapBox.value.children[childIndex]) return

    const recIndex = wrapBox.value.children[childIndex].getBoundingClientRect()
    const midCTop = recIndex.top + recIndex.height / 2
    const diff = Math.abs(midCTop - midTop)
    if (diff < 5) {
      tjLeftHeight.value = 0
      return
    }
    const heightDiff = diff - recIndex.height / 2
    isTop.value = midTop > midCTop
    const offset = isTop.value ? recIndex.height : heightDiff
    console.log('offset', offset)
    tjLeftTop.value = `${isTop.value ? offset : -offset}px`
    tjLeftHeight.value = heightDiff
  } catch (error) {
    console.error('计算节点位置出错:', error)
  }
}

onMounted(() => {
  // 注意nodewrap是个递归组件，所以这里触发一次, 结合watch使用，监听递归的属性变化
  checkNodeRequireProp()
  if (copyNodeConfig.value.type === 100) {
    calculateNodePosition()
  }
})

</script>

<style>
  .drawer_mange  {
    .el-drawer__header {
      margin-bottom: 0px!important;
    }
    .el-drawer__body {
      padding-left: 0px;
      padding-right: 0px;
    }
  }

  .dialog_manage {
    .el-dialog__body {
      height: 80vh;
    }
  }
</style>

<style lang="scss" scoped>

.node_node {
  --node-main-color: var(--el-color-primary);
  display: flex;
  flex-direction: row;
}

.error_node {
  border-radius: 10px;
  box-shadow: 0px 0px 2px 3px  var(--el-color-error);
}

.error_node::after {

}

.node_wrap {
    display: flex;
    flex-direction: row;
    align-items: center;
}

.node {
    cursor: pointer;
    width: 100px;
    min-width: 100px;
    height: 100px;
    position: relative;
    min-height: 100px;
    background-image: linear-gradient(150deg, #dfecff 15%, #C4DFFF 100%);
    box-shadow: inset -1px -1px 3px 0px rgba(40,50,80,0.15);
    box-shadow: inset 1px 1px 1px 0px rgba(255,255,255,1);
    box-shadow: 0px 2px 16px 0px rgba(86,104,119,0.15);
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    &_img {
        width: 24px;
        height: 24px;
        margin: 4px 0;
    }
    span {
        font-size: 14px;
        color: var(--node-main-color);;
        font-weight: 400;
        text-align: center;
        margin: 4px 6px;
    }
    position: relative;
    /* &::before {
      content: "";
      position: absolute;
      border-style: solid;
      border-width: 10px 0 10px 10px;
      border-color: transparent transparent transparent var(--node-main-color);
      left: -10px;
      top: 50%;
      transform: translateY(-50%); */
    /* } */
}

.node:hover {
  .close {
    display: flex;
  }
}

.close {
    display: none;
    position: absolute;
    top: 2px;
    right: 4px;
}

.node_warp_box {
  position: relative;
}

.error_icon_info {
  cursor: pointer;
  display: flex;
  position: absolute;
  top: -4px;
  right: -30px;
}

.node_branch {
    display: flex;
    flex-direction: row;
    height: 100%;
    min-width: 270px;
}

.node_branch_wrap {
    display: flex;
    flex-direction: row;
    position: relative;
}

.node_branch_wrap_box {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    position: relative;
    min-width: 360px;
    /* border-left: 2px solid var(--node-main-color);;
    border-right: 2px solid var(--node-main-color);; */
}

.add_branch {
    border: none;
    outline: none;
    user-select: none;
    justify-content: center;
    font-size: 12px;
    padding: 0 10px;
    width: 80px;
    height: 30px;
    line-height: 30px;
    border-radius: 15px;
    color: #3296fa;
    background: #fff;
    box-shadow: 0 2px 4px 0 rgba(0, 0, 0, .1);
    position: absolute;
    top: calc(50% - 15px);
    left: 0;
    transform: translateX(-50%);
    transform-origin: center center;
    cursor: pointer;
    z-index: 1;
    display: inline-flex;
    align-items: center;
    -webkit-transition: all .3s cubic-bezier(.645, .045, .355, 1);
    transition: all .3s cubic-bezier(.645, .045, .355, 1);
}

.node_branch_item {
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  padding: 8px 0px;
  background-color: #fff;
  border-left: 2px solid var(--node-main-color);;
  border-right: 2px solid var(--node-main-color);;
  &:before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 0;
    margin: auto;
    width: 100%;
    height: 2px;
    background-color: var(--node-main-color);
  }
}

.active_line_right {
  position: absolute;
  height: 0;
  width: 2px;
  top: 100%;
  z-index: 10;
  left: -2px;
  background-color: var(--node-main-color);
}

.active_line_left {
  position: absolute;
  height: 0;
  width: 2px;
  top: 100%;
  z-index: 10;
  right: -2px;
  background-color: var(--node-main-color);
}

.hide_more_border_top_left {
  position: absolute;
  height: calc(50% + 8px);
  width: 4px;
  background-color: #fff;
  left: -3px;
  top: -9px;
}

.hide_more_border_top_right {
  position: absolute;
  height: calc(50% + 8px);
  width: 4px;
  background-color: #fff;
  right: -3px;
  top: -9px;
}

.hide_more_border_bottom_left {
  position: absolute;
  height: calc(50% + 8px);
  width: 4px;
  background-color: #fff;
  left: -3px;
  bottom: -9px;
}

.hide_more_border_bottom_right {
  position: absolute;
  height: calc(50% + 8px);
  width: 4px;
  background-color: #fff;
  right: -3px;
  bottom: -9px;
}

.branch_arrow {
  min-width: 40px;
  position: relative;
  &:before {
      content: "";
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 0;
      margin: auto;
      width: 100%;
      height: 2px;
      background-color: var(--node-main-color);
  }
}

.node_inactive {
    --node-main-color:  #04a904!important;
}

.node_tj_middle {
  border-color: var(--el-color-primary)!important;
}

.right_boder {
  border-right: 2px solid var(--el-color-primary)!important;
}

</style>
