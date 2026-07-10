<template>
  <div class="prop_body">
    <div class="prop_body_tab">
      <el-tabs v-model="activeName" class="demo-tabs">
        <el-tab-pane label="节点设置" name="zero">
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
            优先级
          </div>
          <div class="prop_item flexRowAC">
            <el-popover placement="right" :width="400" height="auto" trigger="click">
              <template #reference>
                <el-button :icon="Sort">
                  优先级{{ currentPriorityIndex + 1 }}
                </el-button>
              </template>
              <div class="prop_item flexColumn">
                <div v-for="(item, ind) in conditionList" :key="ind + item.nodeName" class="prop_item_span">
                  <span class="prop_item_span_span">{{ item.nodeName }} <span v-if="index === ind" class="current_btn">当前</span></span>
                  <div class="prop_item_span_btns">
                    <span>优先级{{ ind+1 }}</span>
                    <el-icon v-if="ind !== conditionList.length - 1" color="var(--el-color-primary)" size="22" @click="downCondition(ind)">
                      <el-tooltip content="下移" placement="bottom" effect="light">
                        <SortDown />
                      </el-tooltip>
                    </el-icon>
                    <el-icon v-if="ind!==0" color="var(--el-color-primary)" size="22" @click="upCondition(ind)">
                      <el-tooltip content="下移" placement="bottom" effect="light">
                        <SortUp />
                      </el-tooltip>
                    </el-icon>
                  </div>
                </div>
              </div>
            </el-popover>
          </div>
          <template v-if="!currentCondition.def">
            <div class="prop_title">
              条件组
            </div>
            <div class="prop_item" style=" justify-content: space-between;">
              <div>
                <el-button :icon="CirclePlus" size="large" @click="addConditionGroup">
                  添加条件组
                </el-button>
                <el-button :icon="Link" size="large" @click="linkRuleClick">
                  引用规则
                </el-button>
              </div>
              <span class="prop_item_other">条件组关系 </span>
              <el-switch
                v-model="activeChooseData.operator"
                size="large"
                active-value="||"
                inactive-value="&&"
                active-text="或"
                inactive-text="且"
              />
            </div>
            <div v-for="(item, ind) in activeChooseData.group" :key="ind" class="prop_item">
              <div class="condition_group">
                <div class="condition_group_header">
                  <span class="prop_item_other">条件{{ letter[ind] }}</span>
                  <span class="prop_item_other">条件组关系</span>
                  <el-switch
                    v-model="item.operator"
                    size="large"
                    active-value="||"
                    inactive-value="&&"
                    active-text="或"
                    inactive-text="且"
                  />
                  <el-icon color="var(--el-color-primary)" size="20" @click="addCondition(ind)">
                    <el-tooltip content="添加条件" placement="bottom" effect="light">
                      <Plus />
                    </el-tooltip>
                  </el-icon>
                  <el-icon color="var(--el-color-primary)" size="20" @click="deleteConditionGroup(ind)">
                    <el-tooltip content="删除条件组" placement="bottom" effect="light">
                      <Delete />
                    </el-tooltip>
                  </el-icon>
                </div>
                <div v-for="(ite,i) in item.conditions" :key="i" class="condition_content">
                  <div class="condition_content_item">
                    <el-select
                      v-model="ite.field"
                      placeholder="选择表单字段"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="fileItem in options"
                        :key="fileItem.id"
                        :label="fileItem.name"
                        :value="fileItem.id"
                      />
                    </el-select>
                  </div>
                  <div class="condition_content_item">
                    <el-select
                      v-model="ite.operator"
                      placeholder="条件"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="operItem in conditionOptions"
                        :key="operItem.value"
                        :label="operItem.label"
                        :value="operItem.value"
                      />
                    </el-select>
                  </div>
                  <div class="condition_content_item">
                    <ChoosePerson v-if="ite.field === 'initiator'" v-model="ite.value" />
                    <el-input v-else v-model="ite.value" style="width: 100%" placeholder="条件值" />
                  </div>
                  <el-icon color="var(--el-color-primary)" size="20" @click="deleteCondition(ind,i)">
                    <el-tooltip content="删除条件" placement="bottom" effect="light">
                      <Delete />
                    </el-tooltip>
                  </el-icon>
                </div>
                <div v-if="item.conditions.length === 0" class="condition_content">
                  <span style="margin:0 auto">点击右上角 + 为本条件组添加条件</span>
                </div>
              </div>
            </div>
            <div v-if="activeChooseData.group.length === 0" class="condition_content empty_content">
              <span style="margin:0 auto">点击左上角 + 添加条件组</span>
            </div>
          </template>
          <template v-else>
            <div class="condition_content empty_content">
              <span style="margin:0 auto">当前为默认条件，不设置条件规则</span>
            </div>
          </template>
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
    <el-dialog v-model="linkVis" title="选择规则" width="50%">
      <link-rules-tables @confirm="confirmLinkRules" />
    </el-dialog>
  </div>
</template>

<script setup>

import { ref, onMounted, computed, watch } from 'vue'
import { Sort, CirclePlus } from '@element-plus/icons-vue'
import { useFlowStore } from '@/store/modules/flow'
import ChoosePerson from './components/choosePerson.vue'
import { ElButton, ElSelect, ElOption,
  ElSwitch, ElIcon, ElInput, ElTooltip, ElPopover, ElTabs, ElTabPane, ElDialog } from 'element-plus'
import { Plus, Delete, SortUp, SortDown, Link } from '@element-plus/icons-vue'
import LinkRulesTables from './components/linkRulesTables.vue'

const props = defineProps({
  nodeConfig: {
    type: Object,
    default: () => ({})
  },
  index: {
    type: Number,
    default: 0
  }
})

const activeName = ref('zero')
const flowStore = useFlowStore()
// 26个英文字母
const letter = ref(['A', 'B', 'C', 'D', 'E', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'])
const activeChooseData = ref({
  operator: '||', // 1或 ，2 且
  group: [
    // { operator: '||', conditions: [] } // { field: '', operator: 'eq', value: '' }
  ],
  conditions: []
})

import { setTjErrorMsg } from '@/utils/setNodeErrorMsg'
const setErrorMsg = () => {
  // 默认条件不设置error
  if (!currentCondition.value.def) {
    setTjErrorMsg(currentCondition.value, currentCondition.value.id)
  }
}

// 过滤表单字段 必填的
// const options = ref((flowStore.formFiledList || []).filter(item => item.required))
const options = ref(flowStore.formFiledList || [])
// 添加发起人的条件选项
if (options.value.findIndex(item => item.id === 'initiator') === -1) {
  options.value.unshift({ id: 'initiator', name: '发起人' })
}
const conditionOptions = ref([
  { label: '等于', value: '==' },
  { label: '不等于', value: '!=' },
  { label: '大于', value: '>' },
  { label: '小于', value: '<' },
  { label: '大于等于', value: '>=' },
  { label: '小于等于', value: '<=' }
])
const addConditionGroup = () => {
  activeChooseData.value.group.push({ operator: 'or', conditions: [{ field: '', operator: '==', value: '' }] })
}
const deleteConditionGroup = (index) => {
  activeChooseData.value.group.splice(index, 1)
}
const addCondition = (index) => {
  activeChooseData.value.group[index].conditions.push({ field: '', operator: '==', value: '' })
}
const deleteCondition = (groupIndex, index) => {
  activeChooseData.value.group[groupIndex].conditions.splice(index, 1)
}

const emits = defineEmits(['update:nodeConfig', 'close'])
function cancel() {
  emits('close')
}

const conditionList = ref(props.nodeConfig.conditionNodes)
const currentCondition = computed(() => { return conditionList.value[props.index] })
const currentPriorityIndex = computed(() => {
  return conditionList.value.findIndex(item => item.nodeName === currentCondition.value.nodeName && item.id === currentCondition.value.id)
})

// 监听index的变化
watch(() => currentCondition.value, () => {
  initPropCondition()
})

// 调整conditionList的顺序
const downCondition = (index) => {
  let temp = conditionList.value[index]
  conditionList.value[index] = conditionList.value[index + 1]
  conditionList.value[index + 1] = temp
}
const upCondition = (index) => {
  let temp = conditionList.value[index]
  conditionList.value[index] = conditionList.value[index - 1]
  conditionList.value[index - 1] = temp
}

const nodeName = ref(props.nodeConfig.conditionNodes[props.index].nodeName)
function confirm() {
  // 属性
  // props.nodeConfig.conditionNodes[currentPriorityIndex.value] = activeChooseData.value
  const nodeConfig = props.nodeConfig
  nodeConfig.conditionNodes[currentPriorityIndex.value].property = activeChooseData.value
  nodeConfig.conditionNodes[currentPriorityIndex.value].nodeName = nodeName.value
  // 更新express
  nodeConfig.conditionNodes[currentPriorityIndex.value].expression = saveRules()
  setErrorMsg()
  emits('update:nodeConfig', nodeConfig)
  emits('close')
}

const lowerLetter = ref('abcdefghijklmnopqrstuvwxyz')
const saveRules = () => {
  let rule = {
    rule_flag: activeChooseData.value.operator,
    rule: {}
  }
  activeChooseData.value.group.forEach((item, index) => {
    // key
    let keyIndex = index + 1
    rule.rule['r' + keyIndex] = {
      rule_flag: item.operator
    }
    item.conditions.forEach((ite, i) => {
      rule.rule['r' + keyIndex][lowerLetter.value[i]] = `${ite.field}${ite.operator}${ite.value}`
    })
  })
  return JSON.stringify(rule)
}
const parseRules = (rules) => {
  // 应用规则舍弃掉外层的rule_flag
  // activeChooseData.value.operator = rules.rule_flag
  if (rules.rule) {
    let tempRules = rules.rule
    for (let key in tempRules) {
      let tempGroupItem = {
        operator: tempRules[key].rule_flag,
        conditions: []
      }
      for (let i in tempRules[key]) {
        // i 是否在小写字母中
        if (lowerLetter.value.includes(i)) {
          // tempRules[key][i] 是个字符串，以操作符切割，保存在filed operator value 中，eg: a < 1, b < 2, d < 3, b>0, d>0 ,a = "11", b!= 0, b !=="00" a<=1 a<="99"
          let tempArr = tempRules[key][i].split(/(==|!=|>=|<=|<|>)/)
          if (tempArr.length === 3) {
            tempGroupItem.conditions.push({
              field: tempArr[0],
              operator: tempArr[1],
              value: tempArr[2]
            })
          }
        }
      }
      activeChooseData.value.group.push(tempGroupItem)
    }
  }
}

const getActiveDataFromNodeConfig = () => {
  activeChooseData.value = {
    operator: '||', // 1或 ，2 且
    group: [
    // { operator: '||', conditions: [] } // { field: '', operator: 'eq', value: '' }
    ],
    conditions: []
  }
  try {
    if (props.nodeConfig.conditionNodes[currentPriorityIndex.value].expression) {
      let rules = JSON.parse(props.nodeConfig.conditionNodes[currentPriorityIndex.value].expression)
      if (rules.rule_flag) {
        activeChooseData.value.operator = rules.rule_flag
        parseRules(rules)
      }
    }
  } catch (e) {
    console.error(e)
  }
}

const linkVis = ref(false)
const linkRuleClick = () => {
  linkVis.value = true
}

const confirmLinkRules = (rulesArr) => {
  if (rulesArr && rulesArr.length > 0) {
    rulesArr.forEach(item => {
      try {
        let ruleItem = JSON.parse(item)
        parseRules(ruleItem)
      } catch (error) {
        console.error('规则解析失败:', item, error)
      }
    })
  }
  linkVis.value = false
}

const initPropCondition = () => {
  getActiveDataFromNodeConfig()
  // activeChooseData.value = { ...activeChooseData.value, ...props.nodeConfig.conditionNodes[currentPriorityIndex.value].property }
  nodeName.value = props.nodeConfig.conditionNodes[currentPriorityIndex.value].nodeName
  setErrorMsg()
}

onMounted(() => {
  initPropCondition()
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

      &_other {
        font-size: 16px;
        margin-left: 24px;
        margin-right:12px;
      }
    }

    .prop_item_group {
      flex-wrap: wrap;
    }

    .prop_item_tj {
      display: flex;
      align-items: center;
    }

    .prop_item_span {
      cursor: pointer;
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 48px;
      background-color: var(--el-menu-hover-bg-color);
      padding: 4px;
      margin: 4px;
      &_span {
        font-size: 16px;
        margin-left: 8px;
        color: var(--el-color-primary)
      }
      &_btns {
        display: flex;
        flex-direction: row;
        gap: 10px;
        margin: 12px;
      }
    }

    .condition_group {
      display: flex;
      flex-direction: column;
      width: 100%;
      border: 1px solid rgba(220,223,230,1);
      border-radius: 2px;
      &_header {
        min-height: 40px;
        display: flex;
        align-items: center;
        justify-content: space-around;
        background: #F3F3F3;
        border-radius: 2px 2px 0px 0px;
        span {
          font-size: 14px;
          color: #333;
        }
      }
    }

    .condition_content {
      padding: 10px;
      min-height: 52px;
      display: flex;
      flex-direction: row;
      align-items: center;
      gap: 6px;
      min-height: 40px;

      &_item {
        display: flex;
        flex: 1;
      }
    }

    .empty_content {
      background-color: #f5f5f5;
      border-radius: 2px;
      width: 100%;
      border: 1px solid #D8D8D8;
    }

    .current_btn {
      background-color: var(--el-color-primary);
      color: #fff;
      padding: 2px 4px;
      border-radius: 4px;
      font-size: 12px;
    }

    .button_group button {
      width: 170px;
      height: 48px;
      border-radius: 2px;
    }

</style>
