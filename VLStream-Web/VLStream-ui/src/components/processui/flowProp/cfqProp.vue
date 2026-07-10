<template>
  <div class="prop_body">
    <div class="prop_body_tab">
      <div class="prop_tip">
        <span>触发器支持模板变量替换语法 ${变量名}， 变量名支持所有的【表单字段ID】及
          【扩展变量】 扩展变量如下：[formName 审批表单名] [instanceId 审批实例ID] [owner_id 发起人ID]
          [owner_name 发起人名] [owner_deptId 发起人部门ID] [owner_deptName 发起人部门名]</span>
      </div>
      <div class="prop_title">
        <span>节点名称</span>
      </div>
      <div class="prop_item">
        <el-input
          v-model="nodeName"
          placeholder="请输入节点名称"
        />
      </div>
      <div class="prop_title">
        <span>选择触发器动作</span>
      </div>
      <div class="prop_item">
        <el-radio-group v-model="activeChooseData.triggerType" style="width: 100%;">
          <el-row style="width:100%">
            <el-col :span="8">
              <el-radio :value="1">
                发送网络请求
              </el-radio>
            </el-col>
            <el-col :span="8">
              <el-radio :value="2">
                发送消息
              </el-radio>
            </el-col>
          </el-row>
        </el-radio-group>
      </div>
      <template v-if="activeChooseData.triggerType === 1">
        <div class="prop_item">
          <el-input
            v-model="activeChooseData.url"
            style="max-width: 600px"
            placeholder="请输入URL地址"
          >
            <template #prepend>
              <el-select v-model="activeChooseData.method" style="width: 115px">
                <el-option label="GET" value="GET" />
                <el-option label="POST" value="POST" />
                <el-option label="PUT" value="PUT" />
                <el-option label="DELETE" value="DELETE" />
                <el-option label="PATCH" value="PATCH" />
                <el-option label="HEAD" value="HEAD" />
              </el-select>
            </template>
          </el-input>
        </div>
        <div class="prop_title">
          <span>Header请求头</span>
          <span @click="addHeader">+添加</span>
        </div>
        <div v-for="(item, index) in activeChooseData.headers" :key="index" class="prop_item">
          <el-row style="width:100%">
            <el-col :span="6">
              <el-input
                v-model="item.key"
                placeholder="参数名"
              />
            </el-col>
            <el-col :span="6">
              <el-radio-group v-model="item.keyType">
                <el-radio-button :value="1">
                  表单
                </el-radio-button>
                <el-radio-button :value="2">
                  固定
                </el-radio-button>
              </el-radio-group>
            </el-col>
            <el-col :span="10">
              <el-select v-if="item.keyType === 1" v-model="item.value" placeholder="请选择表单字段项">
                <el-option v-for="(itd, inx) in options" :key="inx" :label="itd.name" :value="itd.id" />
              </el-select>
              <el-input
                v-if="item.keyType === 2"
                v-model="item.value"
                style="max-width: 600px"
                placeholder="参数值"
              />
            </el-col>
            <el-col :span="2" style="display: flex;align-items: center;justify-content: center;">
              <el-icon size="16" color="red" style="cursor: pointer;">
                <DeleteFilled @click="deleteHeader(item, index)" />
              </el-icon>
            </el-col>
          </el-row>
        </div>
        <div class="prop_title">
          <el-col :span="12">
            <span>请求参数</span>
            <span @click="addParams">+添加</span>
          </el-col>
          <el-col :span="12" class="params_type">
            <span>参数类型</span>
            <el-radio-group v-model="activeChooseData.paramsType">
              <el-radio-button :value="1">
                json
              </el-radio-button>
              <el-radio-button :value="2">
                form
              </el-radio-button>
            </el-radio-group>
          </el-col>
        </div>
        <div v-for="(item, index) in activeChooseData.params" :key="index" class="prop_item">
          <el-row style="width:100%">
            <el-col :span="6">
              <el-input
                v-model="item.key"
                placeholder="参数名"
              />
            </el-col>
            <el-col :span="6">
              <el-radio-group v-model="item.keyType">
                <el-radio-button :value="1">
                  表单
                </el-radio-button>
                <el-radio-button :value="2">
                  固定
                </el-radio-button>
              </el-radio-group>
            </el-col>
            <el-col :span="10">
              <el-select v-if="item.keyType === 1" v-model="item.value" placeholder="请选择表单字段项">
                <el-option v-for="(itd, inx) in options" :key="inx" :label="itd.name" :value="itd.id" />
              </el-select>
              <el-input
                v-if="item.keyType === 2"
                v-model="item.value"
                style="max-width: 600px"
                placeholder="参数值"
              />
            </el-col>
            <el-col :span="2" style="display: flex;align-items: center;justify-content: center;">
              <el-icon size="16" color="red" style="cursor: pointer;">
                <DeleteFilled @click="deleteParams(item, index)" />
              </el-icon>
            </el-col>
          </el-row>
        </div>
        <div class="prop_item">
          <span>无论请求结果如何，均通过</span>
        </div>
      </template>
    </div>
    <div class="prop_body_bottom button_group">
      <!-- 两个按钮 一个取消 ，一个确定 -->
      <el-button @click="cancel">
        取消
      </el-button>
      <el-button type="primary" @click="confirm">
        确定
      </el-button>
    </div>
  </div>
</template>

<script setup>

import { ref, onMounted } from 'vue'
import { useFlowStore } from '@/store/modules/flow'
import { ElMessage } from 'element-plus'
import { ElButton, ElCol, ElRow, ElRadioGroup, ElRadio, ElRadioButton, ElSelect, ElOption,
  ElIcon, ElInput } from 'element-plus'
import { DeleteFilled } from '@element-plus/icons-vue'

const props = defineProps({
  nodeConfig: {
    type: Object,
    default: () => ({})
  }
})

const activeChooseData = ref({
  triggerType: 1, // 1:网络请求，2:消息
  url: '', // 请求地址
  method: 'GET', // 请求方式
  headers: [], // 请求头
  params: [], // 请求参数
  paramsType: 1 // 请求参数类型，1:json，2:form
})

import { setCfqErrorMsg } from '@/utils/setNodeErrorMsg'
const setErrorMsg = () => {
  setCfqErrorMsg(activeChooseData.value, props.nodeConfig.id)
}

const emits = defineEmits(['update:nodeConfig', 'close'])
function cancel() {
  emits('close')
}

const nodeName = ref(props.nodeConfig.nodeName)
function confirm() {
  // 属性单词
  // props.nodeConfig.property = activeChooseData.value
  const nodeConfig = { ...props.nodeConfig, ...activeChooseData.value }
  nodeConfig.nodeName = nodeName.value
  setErrorMsg()
  emits('update:nodeConfig', nodeConfig)
  emits('close')
}

// 获取发起人的表单  即主表表单
const flowStore = useFlowStore()
const options = ref(flowStore.formFiledList || [])

function addHeader() {
  let len = activeChooseData.value.headers.length
  if (len > 0) {
    if (!activeChooseData.value.headers[len - 1].value) {
      ElMessage.warning('请完善之前项后在添加')
      return
    }
  }
  activeChooseData.value.headers.push({
    key: '', keyType: 1, value: ''
  })
}

function deleteHeader(item, index) {
  activeChooseData.value.headers.splice(index, 1)
}

function addParams() {
  let len = activeChooseData.value.params.length
  if (len > 0) {
    if (!activeChooseData.value.params[len - 1].value) {
      ElMessage.warning('请完善之前项后在添加')
      return
    }
  }
  activeChooseData.value.params.push({
    key: '', keyType: 1, value: ''
  })
}

function deleteParams(item, index) {
  activeChooseData.value.params.splice(index, 1)
}

onMounted(() => {
  activeChooseData.value = { ...activeChooseData.value, ...props.nodeConfig }
  setErrorMsg()
})

</script>

<style scoped lang="scss">
    .prop_body {
      display: flex;
      height: 100%;
      width: 100%;
      flex-direction: column;

      &_tab {
        display: flex;
        flex-direction: column;
        flex: 1;
        padding: 16px;
        overflow: auto;
      }

      &_bottom {
        display: flex;
        align-items: center;
        height: 48px;
        padding: 0 32px;
        justify-content: flex-end;
        gap: 10px;
      }
    }

    .prop_title {
      display: flex;
      align-items: center;
      height: 48px;
      span:nth-of-type(1) {
        font-size: 14px;
        color: #333333;
        letter-spacing: 0;
        font-weight: 400;
      }
      span:nth-of-type(2) {
        margin-left: 20px;
        font-size: 12px;
        color: var(--el-color-primary);;
        letter-spacing: 0;
        font-weight: 400;
        cursor: pointer;
      }
    }

    .prop_item {
      display: flex;
      align-items: center;
      min-height: 48px;
      width: 100%;
    }

    .prop_item_group {
      flex-wrap: wrap;
    }

    .prop_tip {
      background: rgba(53,184,129,0.12);
      border-radius: 4px;
      padding: 8px;
      span {
        font-size: 12px;
        color: #35B881;
        letter-spacing: 0;
        line-height: 18px;
        font-weight: 400;
      }
    }

    .params_type {
      display: flex;
      align-items: center;
      span {
        margin:00 8px;
        font-size: 12px;
        color: #333333;
        letter-spacing: 0;
      }
    }

    .button_group button {
      width: 170px;
      height: 48px;
      border-radius: 2px;
    }
</style>
