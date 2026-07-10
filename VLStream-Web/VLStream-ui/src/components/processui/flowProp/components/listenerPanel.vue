<template>
  <div class="flow_opration_panel">
    <div class="prop_title">
      <span>监听器类型</span>
    </div>
    <div class="prop_item">
      <el-radio-group v-model="copyData.triggerType" style="width: 100%;" @change="updateData">
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
    <div class="prop_title">
      <span>触发时机</span>
    </div>
    <div class="prop_item">
      <el-select v-model="copyData.event" placeholder="请选择触发时机" @change="updateData">
        <el-option label="在任务开始时触发" value="create" />
        <el-option label="在任务被分配给某个办理人之后触发（委派，转办）" value="assignment" />
        <el-option label="在任务完成时触发（通过）" value="complete" />
        <el-option label="在任务即将被删除前触发。（通过，拒绝，退回）" value="delete" />
      </el-select>
    </div>
    <template v-if="copyData.triggerType === 1">
      <div class="prop_item">
        <el-input
          v-model="copyData.url"
          style="max-width: 600px"
          placeholder="请输入URL地址"
          @change="updateData"
        >
          <template #prepend>
            <el-select v-model="copyData.method" style="width: 115px" @change="updateData">
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
      <div v-for="(item, index) in copyData.headers" :key="index" class="prop_item">
        <el-row style="width:100%">
          <el-col :span="6">
            <el-input
              v-model="item.key"
              placeholder="参数名"
              @change="updateData"
            />
          </el-col>
          <el-col :span="6">
            <el-radio-group v-model="item.keyType" @change="updateData">
              <el-radio-button :value="1">
                表单
              </el-radio-button>
              <el-radio-button :value="2">
                固定
              </el-radio-button>
            </el-radio-group>
          </el-col>
          <el-col :span="10">
            <el-select v-if="item.keyType === 1" v-model="item.value" placeholder="请选择表单字段项" @change="updateData">
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
          <el-radio-group v-model="copyData.paramsType">
            <el-radio-button :value="1">
              json
            </el-radio-button>
            <el-radio-button :value="2">
              form
            </el-radio-button>
          </el-radio-group>
        </el-col>
      </div>
      <div v-for="(item, index) in copyData.params" :key="index" class="prop_item">
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
              <!-- 主表单 -->
              <el-option-group
                key="主表单（发起节点）"
                label="主表单（发起节点）"
              >
                <el-option v-for="(itd, inx) in optionsMain" :key="inx" :label="itd.name" :value="itd.id" />
              </el-option-group>
              <el-option-group
                key="节点表单（当前节点）"
                label="节点表单（当前节点）"
              >
                <el-option v-for="(itd, inx) in options" :key="inx" :label="itd.name" :value="itd.id" />
              </el-option-group>
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
    </template>
  </div>
</template>

<script setup>
import { defineProps, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ElCol, ElRow, ElRadioGroup, ElRadio, ElRadioButton, ElSelect, ElOption,
  ElIcon, ElInput } from 'element-plus'
import { useFlowStore } from '@/store/modules/flow'
import { DeleteFilled } from '@element-plus/icons-vue'
import { getOrCreateNodeFormState } from '../utils/nodeFormStateManager'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  },
  nodeId: {
    type: String,
    default: ''
  }
})

const copyData = ref(props.data)
// watch 监听nodeConfig
watch(() => props.data, () => {
  initData()
})

const initData = () => {
  copyData.value = {
    ...{
      triggerType: 1, // 1:网络请求，2:消息
      event: 'complete', // 触发时机
      url: '', // 请求地址
      method: 'GET', // 请求方式
      headers: [], // 请求头
      params: [], // 请求参数
      paramsType: 1 // 请求参数类型，1:json，2:form
    }, ...props.data }
}

// 从节点级别的状态管理器中获取表单字段，而不是从全局Store
const nodeFormState = getOrCreateNodeFormState(props.nodeId)
const options = ref(nodeFormState.formFiledList.value || [])
const flowStore = useFlowStore()
const optionsMain = ref(flowStore.formFiledList || [])

// 监听节点管理的formFiledList变化，实时更新组件中的options
watch(
  () => nodeFormState.formFiledList.value,
  (newVal) => {
    options.value = newVal || []
  },
  { deep: true }
)

function addHeader() {
  let len = copyData.value.headers.length
  if (len > 0) {
    if (!copyData.value.headers[len - 1].value) {
      ElMessage.warning('请完善之前项后在添加')
      return
    }
  }
  copyData.value.headers.push({
    key: '', keyType: 1, value: ''
  })

  updateData()
}

function deleteHeader(item, index) {
  copyData.value.headers.splice(index, 1)
  updateData()
}

function addParams() {
  let len = copyData.value.params.length
  if (len > 0) {
    if (!copyData.value.params[len - 1].value) {
      ElMessage.warning('请完善之前项后在添加')
      return
    }
  }
  copyData.value.params.push({
    key: '', keyType: 1, value: ''
  })
  updateData()
}

function deleteParams(item, index) {
  copyData.value.params.splice(index, 1)
}

const emits = defineEmits(['update:item'])
const updateData = () => {
  emits('update:item', copyData.value)
}

onMounted(() => {
  initData()
})

</script>

<style lang="scss" scoped>
    .flow_opration_panel{
      border: 1px solid #ebeef5;
      margin: 4px 0px;
      padding: 4px;
      border-radius: 4px;
      background-color: #fbfbfb;
      width: 100%;
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
        margin:0 8px;
        font-size: 12px;
        color: #333333;
        letter-spacing: 0;
      }
    }
</style>
