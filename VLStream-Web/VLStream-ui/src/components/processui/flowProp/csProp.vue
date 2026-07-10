<template>
  <div class="prop_body">
    <div class="prop_body_tab">
      <el-tabs v-model="activeName" class="demo-tabs" @tab-click="handleClick">
        <el-tab-pane label="设置抄送人" name="first">
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
            <span>选择抄送人</span>
          </div>
          <choose-empty-user v-model:active-choose-data="activeChooseData" />
          <div class="prop_title">
            <span>允许发起人添加抄送人</span>
          </div>
          <div class="prop_item prop_item_group">
            <el-switch v-model="activeChooseData.canSelectCS" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="表单权限设置" name="second">
          <FormPermissionTable v-if="activeChooseData.formProperties" v-model:filed-list="activeChooseData.formProperties" />
          <div v-else>
            <span>请在发起人节点设置表单</span>
          </div>
        </el-tab-pane>
        <el-tab-pane label="节点表单" name="fivth">
          <div class="prop_title">
            <span>设置节点表单</span>
          </div>
          <div class="prop_item" style="flex-direction: column;">
            <div v-if="currentForm" class="current_form">
              <span>
                当前表单： {{ currentForm.formName }}
              </span>
            </div>
            <el-cascader
              :props="cascaderProps"
              placeholder="请选择表单"
              style="width: 100%"
              @change="setFormKey"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
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

import { ref, onMounted, inject } from 'vue'
import ChooseEmptyUser from './components/chooseEmptyUser.vue'
import FormPermissionTable from './components/formPermissionTable.vue'
import { ElButton, ElTabs, ElTabPane, ElSwitch, ElInput, ElCascader } from 'element-plus'
import { formAppList, formSynthesisList, getForm } from '@/api/processui'
import { extractFormFields } from './utils/formFieldExtractor'
import { getOrCreateNodeFormState, updateNodeFormFields } from './utils/nodeFormStateManager'

const props = defineProps({
  nodeConfig: {
    type: Object,
    default: () => ({})
  }
})
// 模型-列表数据
const flowDesignerPage = inject('flowDesignerPage')
// 0 流程 1 工单
let formDesignType = ref(undefined)
// 默认应用
let classifyType = ref(true)
formDesignType.value = flowDesignerPage.formDesignType
if (flowDesignerPage?.synthesisId) classifyType.value = false

const activeName = ref('first')
// 初始化时从节点级别的状态管理器获取表单字段，而不是从全局Store
const nodeFormState = getOrCreateNodeFormState(props.nodeConfig.id)
const activeChooseData = ref({
  user: [], // 用户列表
  canSelectCS: false, // 允许发起人添加抄送人
  formProperties: nodeFormState.formFiledList.value || [], // 从节点级别状态获取，而不是全局Store
  formKey: ''
})

import { setCsErrorMsg } from '@/utils/setNodeErrorMsg'
const setErrorMsg = () => {
  setCsErrorMsg(activeChooseData.value, props.nodeConfig.id)
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

import { listForm } from '@/api/processui'
import { useUserStore } from '@/store/modules/useraPaas'
const store = useUserStore()
const currentForm = ref(null)
function getFormList() {
  const params = {
    type: formDesignType.value,
    pageNum: 1,
    pageSize: 999,
    isFormComponents: 0
  }
  listForm(params).then(res => {
    (res.rows || []).map(item => {
      if (item.formId === activeChooseData.value.formKey) {
        currentForm.value = item
      }
    })
  })
}
const setFormKey = (data) => {
  activeChooseData.value.formKey = data[data.length - 1]
  getFormList()
  setNodeFormFieldProp(activeChooseData.value.formKey)
}
const cascaderProps = {
  lazy: true,
  lazyLoad(node, resolve) {
    const { level } = node
    // if (level === 0) {
    //   resolve([{
    //     value: '',
    //     label: '全部',
    //     leaf: false
    //   }])
    //   return
    // }
    const params = {
      accessToken: store.token,
      type: formDesignType.value,
      pageNo: 1,
      pageSize: 999
    }
    if (node.data.categoryId) {
      params.parentId = node.data.categoryId
    }
    if (flowDesignerPage?.applicationId) {
      params.applicationId = flowDesignerPage.applicationId
    }
    let resFn = classifyType.value ? formAppList(params) : formSynthesisList(params)
    resFn.then((res) => {
      if (res.code === 200) {
        let resData = classifyType.value ? res.rows : res.data
        let tempCateGory = resData?.map((item) => ({
          value: item.categoryId,
          label: (item.applicationName || item.categoryName) + '(分类)',
          leaf: false,
          categoryId: item.categoryId
        }))
        if (level !== 0) {
          const pp = {
            pageNo: 1,
            pageSize: 999,
            type: formDesignType.value,
            isFormComponents: '0'
          }
          if (node.data.categoryId) {
            pp.categoryId = node.data.categoryId
          }
          listForm(pp).then((resp) => {
            if (resp.code === 200) {
              resp.rows.map((item) => {
                tempCateGory.push({
                  value: item.formId,
                  label: item.formName,
                  leaf: true
                })
              })
            }
            resolve(tempCateGory)
          })
        } else {
          resolve(tempCateGory)
        }
      } else {
        resolve([])
      }
    })
  }
}

const currentNodeFormProp = ref([])
const setNodeFormFieldProp = async(formKey) => {
  const params = {
    formId: formKey
  }
  let res = await getForm(params)
  if (res.code === 200) {
    try {
      // 使用统一的字段提取函数
      const jsonList = JSON.parse(res.data.content)
      const formFields = extractFormFields(jsonList)
      currentNodeFormProp.value = JSON.parse(JSON.stringify(formFields))
      // 切换表单强制将权限修改为当前切换的
      activeChooseData.value.formProperties = currentNodeFormProp.value || []
      // 更新节点级别的表单字段状态，确保数据隔离性
      updateNodeFormFields(props.nodeConfig.id, activeChooseData.value.formProperties)
      // store保存当前表单信息
    } catch (error) {
      console.log(error)
    }
  }
}

onMounted(() => {
  activeChooseData.value = { ...activeChooseData.value, ...props.nodeConfig }
  // 如果nodeConfig中有formProperties，需要同步到节点级别的状态管理器
  if (props.nodeConfig.formProperties && props.nodeConfig.formProperties.length > 0) {
    updateNodeFormFields(props.nodeConfig.id, props.nodeConfig.formProperties)
    activeChooseData.value.formProperties = props.nodeConfig.formProperties
  } else if (props.nodeConfig.formKey && activeChooseData.value.formProperties?.length === 0) {
    setNodeFormFieldProp(props.nodeConfig.formKey)
  }
  if (!props.nodeConfig.formKey) {
    activeChooseData.value.formProperties = []
  }
  setErrorMsg()
  getFormList()
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
      span {
        font-size: 14px;
        color: #333333;
        letter-spacing: 0;
        font-weight: 400;
      }
    }

    .prop_item {
      display: flex;
      align-items: center;
      min-height: 48px;
      margin: 8px;

      &_add {
        display: flex;
        align-items: center;
        justify-content: center;
        img {
          width: 48px;
          height: 48px;
          cursor: pointer;
        }
      }

      &_user {
        background: #FFFFFF;
        width: 220px;
        min-width: 220px;
        height: 60px;
        position: relative;
        cursor: pointer;
        display: flex;
        flex-direction: row;
        align-items: center;
        margin: 4px;
        &_remove {
          display: none;
          width: 16px;
          height: 16px;
          position: absolute;
          right: 4px;
          top: 4px;
        }
        &_headpic {
          width: 48px;
          height: 48px;
          margin: 8px;
          border-radius: 100%;
        }
        &_name {
          flex: 1;
          display: flex;
          flex-direction: column;
          span:nth-child(1) {
            font-size: 14px;
            color: #333333;
            letter-spacing: 0;
            font-weight: 700;
          }
          span:nth-child(2) {
            font-size: 12px;
            color: #8D93A2;
            letter-spacing: 0;
            font-weight: 400;
          }
        }
      }
      &_user:hover {
        box-shadow: 0px 0px 6px 0px rgba(0,0,0,0.15);
        border-radius: 2px;
        .prop_item_user_remove {
          display: flex!important;
        }
      }
    }

    .prop_item_group {
      flex-wrap: wrap;
    }

    .current_form {
      display: flex;
      width: 100%;
      align-items: center;
      height:32px;
      span {
        font-size: 14px;
        color: var(--el-color-primary);
      }
    }

    .button_group button {
      width: 170px;
      height: 48px;
      border-radius: 2px;
    }
</style>
