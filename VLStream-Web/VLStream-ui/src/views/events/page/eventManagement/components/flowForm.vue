<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <div class="eventItem">
    <el-form
      ref="formRef_1"
      :model="form"
      :rules="formRules"
      label-width="auto"
    >
      <el-form-item :label="$tp('选择工单')" prop="processKey">
        <el-select v-model="form.processKey" :placeholder="$tp('请选择工单')" @change="workConfirm">
          <el-option
            v-for="item in options"
            :key="item.value"
            :label="item.label"
            :value="item.value"
            @click="workConfirm(item)"
          />
        </el-select>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { translatePhrase as uiText } from '@/i18n'

import { ref, reactive, onMounted } from 'vue'
// import { getProcessForm } from '@/api/processui/approval'
import { listModel, startEventWorkOrder, stopEventWorkOrder } from '@/api/processui'
import { useUserStore } from '@/store/modules/useraPaas'
import { ElMessage, ElMessageBox } from 'element-plus'
import { event_item_list } from '@/api/smartCity/events'
import { resolveWorkOrderAppContext } from '@/utils/workOrderAppContext'
import { AuthManager } from '@/utils/auth'
import { eventWorkOrderActor, eventWorkOrderIntent, clearEventWorkOrderIntent,
  rememberConfirmedWorkOrder, findPendingWorkOrderIntent, markPreviousOrderStop } from '@/utils/eventWorkOrderIntent.mjs'

const store = useUserStore()
const auth = new AuthManager()
const props = defineProps(['listObj'])
const options = ref<any>([])
const selectedOptions = ref<any>('')
const vmFormRef = ref(null)
let form = reactive({
  processKey: props.listObj?.work_order_data?.processKey,
  work_order_data: props.listObj?.work_order_data || undefined
})
const formRef_1 = ref<any>(null)
const appObj = ref<any>(null)
let activeIntent: any = null

function currentWorkOrderActor(identity) {
  let cached = null
  try { cached = JSON.parse(window.sessionStorage.getItem('userInfo') || 'null') } catch { /* rejected below */ }
  return eventWorkOrderActor(cached, store.userInfo, identity.tenantId)
}

function clearConfirmedIntent() {
  try {
    if (activeIntent && auth.isIdentityCurrent(activeIntent.identity) && String(props.listObj?.id || '') === activeIntent.eventId)
      clearEventWorkOrderIntent(window.localStorage, activeIntent.intent)
  } catch { /* A stale record is safe: it prevents accidental duplicate creation. */ }
}

// form
const formRules = ref({
  processKey: [
    { required: true, message: '请选择工单', trigger: 'blur' }
  ]
})

// work order
const workConfirm = (val) => {
  const target = options.value.find(item => item.value === val)
  if (target) selectedOptions.value = target
}

// work order- (workflow -event )
const workListFn = async() => {
  try {
    appObj.value = await resolveWorkOrderAppContext()
    const data = {
      accessToken: store.userInfo?.accessToken,
      pageNum: 1,
      pageSize: 99,
      workOrderCategory: appObj.value?.appId
    }
    const res: any = await listModel(data)
    if (res?.code === 200 && res?.rows) {
      options.value = res.rows.map((item: any) => ({
        ...item,
        label: item.modelName,
        value: item.modelKey
      }))
      event_item_listFn() // event
    } else {
      options.value = []
    }
  } catch (error) {
    // Get work order failed, null / empty array
    options.value = []
  }
}

// work order history work order prompt / tip, new work order, old work order
const closeWorkorderFn = async(identity = auth.getIdentitySnapshot(), assertCurrent = () => {
  if (!auth.isIdentityCurrent(identity)) throw new Error('登录账户或租户已变化，请刷新后重试')
}) => {
  let data = {
    procInsId: form.work_order_data?.procInsId,
    variables: {}
  }
  let res: any = await stopEventWorkOrder(data, identity, assertCurrent)
  if (res.code === 200) {
    ElMessage.success(uiText('关闭工单成功'))
  } else {
    throw new Error('原工单状态未确认，请刷新后重试')
  }
}

// work ordersuccessfully
async function addWorkorderFn() {
  activeIntent = null
  if (typeof props.listObj?.id === 'number' && !Number.isSafeInteger(props.listObj.id)) {
    ElMessage.error(uiText('事件编号无法准确识别，请刷新后核实'))
    return null
  }
  const identity = auth.getIdentitySnapshot()
  const eventId = String(props.listObj?.id || '')
  const selectedKey = form.processKey
  const selectedDefinition = selectedOptions.value?.definitionId
  const isCurrent = () => auth.isIdentityCurrent(identity) && String(props.listObj?.id || '') === eventId
    && form.processKey === selectedKey && selectedOptions.value?.definitionId === selectedDefinition
  // Already linked to the selected order: preserve its previous behaviour.
  if (form.processKey && form.work_order_data) {
    const oldOrder = form.work_order_data
    if (oldOrder.processKey === form.processKey) {
      try {
        const actor = currentWorkOrderActor(identity)
        const intent = findPendingWorkOrderIntent(window.localStorage, actor, eventId)
        if (intent && intent.confirmedProcessId !== String(oldOrder.procInsId || '')) {
          throw new Error('上次建单结果未确认，请先核实后再修改内容')
        }
        if (intent) activeIntent = { identity, intent, eventId }
      } catch {
        ElMessage.error(uiText('上次建单结果未确认，请先核实后再修改内容'))
        return null
      }
      return oldOrder
    }
  }
  // 2️⃣ formValidate
  const valid = await formRef_1.value?.validate()
  if (!valid) return
  // 3️⃣ parameterBuild
  const processDefId = selectedOptions.value?.definitionId
  if (!form.processKey || !processDefId) {
    ElMessage.warning(uiText('请选择工单'))
    return
  }
  const data = JSON.parse(JSON.stringify({
    processDefId,
    workOrderBo: {
      processKey: form.processKey,
      title: props.listObj?.name,
      source: props.listObj?.mod_type === 2 ? '主动安全' : '事件拍传',
      eventNumber: props.listObj?.id,
      workorderId: appObj.value?.appId,
      workorderIdExtend: `1,${appObj.value?.appId}`
    },
    frontFlag: true, // work order frontFlagparameter to true formparameter
    /* autoGetFormFlag parameter to true find form value item , formparameter */
    /* Custom is abc, variables then is {abc:"event value "} */
    autoGetFormFlag: true,
    variables: {
      no: props.listObj?.id || undefined, //
      item: props.listObj?.item || undefined, //
      name: props.listObj?.name || undefined, //
      time: props.listObj?.created_at || undefined, //
      address: props.listObj?.point?.address || undefined, //
      describe: props.listObj?.describe || undefined, //
      image: props.listObj?.pics || undefined//
    }
  }))
  try {
    const actor = currentWorkOrderActor(identity)
    // Check an unresolved intent before stopping a previous order or generating a new ID.
    let intent = await eventWorkOrderIntent(window.localStorage, window.crypto, actor, eventId, data, false)
    if (!isCurrent()) throw new Error('登录账户、租户、事件或工单选择已变化，请刷新后重试')
    const oldOrder = form.work_order_data
    if (intent?.previousOrderId && (intent.previousStopState !== 'CONFIRMED'
      || String(oldOrder?.procInsId || '') !== intent.previousOrderId)) {
      throw new Error('原工单状态未确认，请刷新后重试')
    }
    if (oldOrder && oldOrder.processStatus !== 'canceled' && !oldOrder.finishTime) {
      if (!intent?.previousOrderId) {
        try {
          await ElMessageBox.confirm(uiText('选择的工单跟历史工单不一致，是否继续取消旧的工单'), uiText('提示'),
            { confirmButtonText: uiText('确定'), cancelButtonText: uiText('取消'), type: 'warning' })
        } catch { return }
        if (!isCurrent()) throw new Error('登录账户、租户、事件或工单选择已变化，请刷新后重试')
        if (form.work_order_data !== oldOrder) throw new Error('原工单状态未确认，请刷新后重试')
        intent = intent || await eventWorkOrderIntent(window.localStorage, window.crypto, actor, eventId, data, true)
        intent = markPreviousOrderStop(window.localStorage, intent, oldOrder.procInsId, 'PENDING')
        try { await closeWorkorderFn(identity, () => { if (!isCurrent()) throw new Error('当前身份或事件已变化') }) }
        catch { throw new Error('原工单状态未确认，请刷新后重试') }
        if (!isCurrent()) throw new Error('登录账户、租户、事件或工单选择已变化，请刷新后重试')
        intent = markPreviousOrderStop(window.localStorage, intent, oldOrder.procInsId, 'CONFIRMED')
      }
    }
    intent = intent || await eventWorkOrderIntent(window.localStorage, window.crypto, actor, eventId, data, true)
    if (!isCurrent()) throw new Error('登录账户、租户、事件或工单选择已变化，请刷新后重试')
    const res: any = await startEventWorkOrder({ ...data, requestId: intent.requestId }, identity,
      () => { if (!isCurrent()) throw new Error('当前身份或事件已变化') })
    if (!isCurrent()) throw new Error('登录账户、租户、事件或工单选择已变化，请刷新后重试')
    const workOrder = res?.data?.workOrder
    if (res?.code !== 200 || !workOrder?.procInsId) throw new Error('创建结果未确认')
    rememberConfirmedWorkOrder(window.localStorage, intent, workOrder.procInsId)
    activeIntent = { identity, intent, eventId }
    form.work_order_data = workOrder
    return workOrder
  } catch (failure: any) {
    const message = failure?.message || ''
    ElMessage.error(uiText(message.includes('上次建单结果未确认') || message.includes('登录账户、租户、事件或工单选择已变化')
      || message.includes('原工单状态未确认') ? message : '工单创建结果未确认，请刷新并核实后再试'))
    return null
  }
}

// event - work order in
const event_item_listFn = async() => {
  let data = {
    accessToken: store.userInfo?.accessToken
  }
  let res: any = await event_item_list(data)
  if (res.code === 200) {
    let list = res.data.list || []
    let targetText = String(props.listObj?.item).trim()
    let tt = list.find(v => v.item === targetText)
    if (targetText && list && !form.processKey) {
      form.processKey = tt?.config?.process_id?.split(':')?.[0]
      workConfirm(form.processKey) // work order
    }
  }
}

onMounted(() => {
  workListFn() // workflow -event
})

defineExpose({ form, vmFormRef, addWorkorderFn, closeWorkorderFn, clearConfirmedIntent })
</script>

<style lang="scss" scoped>
.eventItem {
}

</style>
