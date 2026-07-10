<template>
  <div class="add_node" :class="{'add_node_only_r':!isEdit, 'node_inactive': isFinish}">
    <el-popover v-model:visible="visible" placement="bottom" :width="clacPXToVW(528)" height="auto" :style="'min-width: ' +clacPXToVW(528)+ 'px'" trigger="click">
      <template #reference>
        <oort-svg-icon v-if="isEdit" class="arrorBox_add" name="add_node_flow" />
      </template>
      <div class="node_list">
        <div class="node_list_header">
          <span>添加流程节点</span>
        </div>
        <div class="node_group">
          <div class="node_item" @click="addNode(1)">
            <img src="@/assets/img/processui/flownode/xzspr.png" />
            <span v-if="flowDesignerPage?.notifyNode">处置节点</span>
            <span v-else>审批节点</span>
          </div>
          <div
            v-if="flowDesignerPage?.notifyNode"
            class="node_item"
            @click="addNode(5)"
          >
            <img src="@/assets/img/processui/flownode/tzjd.png" />
            <span>通知节点</span>
          </div>
          <div class="node_item" @click="addNode(2)">
            <img src="@/assets/img/processui/flownode/xzcsr.png" />
            <span>抄送节点</span>
          </div>
          <div class="node_item" @click="addNode(3)">
            <img src="@/assets/img/processui/flownode/yscl.png" />
            <span>延时处理</span>
          </div>
          <div class="node_item" @click="addNode(4)">
            <img src="@/assets/img/processui/flownode/cfq.png" />
            <span>触发器</span>
          </div>
          <div class="node_item" @click="addNode(100)">
            <img src="@/assets/img/processui/flownode/tjfz.png" />
            <span>条件分支</span>
          </div>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<script setup>
import { inject, ref } from 'vue'
import { shortRandomStr, clacPXToVW } from '@/utils/index'
import { ElPopover } from 'element-plus'
let props = defineProps({
  childNodeP: {
    type: Object,
    default: () => ({})
  },
  pNode: {
    type: Object,
    default: () => ({})
  },
  isEdit: {
    type: Boolean,
    default: true
  },
  isFinish: {
    type: Boolean,
    default: false
  }

})
const flowDesignerPage = inject('flowDesignerPage') // 通知节点：工单有,流程无
let emits = defineEmits(['update:childNodeP'])
let visible = ref(false)
const addNode = (type) => {
  const id = shortRandomStr()
  let childNode = null
  if (props.childNodeP) {
    childNode = { ...props.childNodeP, pid: id }
  }
  visible.value = false
  if (type !== 100) {
    let data
    if (type === 1) {
      data = {
        nodeName: flowDesignerPage?.notifyNode ? '处置节点' : '审批节点',
        type: 1,
        nodeType: 'approval',
        childNode: childNode
      }
    } else if (type === 2) {
      data = {
        nodeName: '抄送节点',
        type: 2,
        nodeType: 'cc',
        childNode: childNode
      }
    } else if (type === 3) {
      data = {
        nodeName: '延时处理',
        type: 3,
        nodeType: 'timer',
        childNode: childNode
      }
    } else if (type === 4) {
      data = {
        nodeName: '触发器',
        type: 4,
        nodeType: 'trigger',
        childNode: childNode
      }
    } else if (type === 5) {
      data = {
        nodeName: '通知节点',
        type: 5,
        nodeType: 'messageNode',
        childNode: childNode
      }
    }
    data.id = id
    data.pid = props.pNode.id
    emits('update:childNodeP', data)
  } else {
    let data = {
      'nodeName': '独占网关',
      'type': 100,
      pid: props.pNode.id,
      id: id,
      nodeType: 'exclusive',
      'childNode': childNode,
      'conditionNodes': [
        {
          nodeType: 'condition',
          'nodeName': '条件1',
          'type': 99,
          property: {},
          id: shortRandomStr(),
          pid: id,
          def: false,
          'childNode': null
        },
        {
          nodeType: 'condition',
          'nodeName': '默认条件',
          'type': 99,
          property: {},
          id: shortRandomStr(),
          pid: id,
          def: true,
          'childNode': null
        }
      ]
    }
    emits('update:childNodeP', data)
  }
}

</script>

<style scoped lang="scss">

    .add_node_only_r {
      width: 60px!important;
    }
    .add_node {
        width: 120px;
        display: -webkit-inline-box;
        display: -ms-inline-flexbox;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        -ms-flex-negative: 0;
        flex-shrink: 0;
        -webkit-box-flex: 1;
        -ms-flex-positive: 1;
        position: relative;
        &::before {
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
        &::after {
          content: "";
          position: absolute;
          border-style: solid;
          border-width: 10px 0 10px 10px;
          border-color: transparent transparent transparent var(--node-main-color);
          right: 0px;
          top: 50%;
          transform: translateY(-50%);
        }
    }

    .arrorBox_add {
        z-index: 1;
        width: 48px;
        height: 48px;
        cursor: pointer;
    }
    .node_list {
        display: flex;
        flex-direction: column;
        &_header {
            height: 48px;
            display: flex;
            align-items: center;
            span {
                font-size: 16px;
                color: #333333;
                letter-spacing: 0;
            }
        }
    }

    .node_group {
        display: flex;
        flex-wrap: wrap;
        width: 100%;
        gap: 5px;
    }

    .node_item:hover {
        background: #f3f3f3;
    }

    .node_item {
        cursor: pointer;
        margin: 2px;
        background: #F7F7F7;
        border-radius: 8px;
        width: 48%;
        height: 72px;
        display: flex;
        align-items: center;
        justify-content: center;
        img {
            width: 40px;
            height: 40px;
            margin-right: 8px;
        }
        span  {
            font-size: 14px;
            color: #333333;
            letter-spacing: 0;
            font-weight: 400;
        }
    }

  .node_inactive {
    --node-main-color:  #04a904!important;
  }

</style>
