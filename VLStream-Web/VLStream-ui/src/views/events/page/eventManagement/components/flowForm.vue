<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <div class="eventItem">
    <el-form
      ref="formRef_1"
      :model="form"
      :rules="formRules"
      label-width="auto"
    >
      <el-form-item label="选择工单" prop="processKey">
        <el-select v-model="form.processKey" placeholder="请选择工单" @change="workConfirm">
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
import { ref, reactive, onMounted } from 'vue'
// import { getProcessForm } from '@/api/processui/approval'
import { listModel, startProcess } from '@/api/processui'
import { useUserStore } from '@/store/modules/useraPaas'
import { ElMessage, ElMessageBox } from 'element-plus'
import { stopProcess } from '@/api/processui/approval'
import { event_item_list } from '@/api/smartCity/events'
import { resolveWorkOrderAppContext } from '@/utils/workOrderAppContext'

const store = useUserStore()
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
const closeWorkorderFn = async() => {
  let data = {
    procInsId: form.work_order_data?.procInsId,
    variables: {}
  }
  let res: any = await stopProcess(data)
  if (res.code === 200) {
    ElMessage.success('关闭工单成功')
  }
}

// work ordersuccessfully
async function addWorkorderFn() {
  // 1️⃣ already in work order
  if (form.processKey && form.work_order_data) {
    const oldOrder = form.work_order_data
    // work order,
    if (oldOrder.processKey === form.processKey) {
      return oldOrder
    }
    // work order, old work order not finish → prompt / tipwhether
    if (oldOrder.processStatus !== 'canceled' && !oldOrder.finishTime) {
      try {
        await ElMessageBox.confirm(
          '选择的工单跟历史工单不一致，是否继续取消旧的工单',
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        await closeWorkorderFn()
      } catch {
        // user
        return
      }
    }
  }
  // 2️⃣ formValidate
  const valid = await formRef_1.value?.validate()
  if (!valid) return
  // 3️⃣ parameterBuild
  const processDefId = selectedOptions.value?.definitionId
  if (!form.processKey && !processDefId) {
    ElMessage.warning('请选择工单')
    return
  }
  const data = {
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
  }
  // 4️⃣ workflow
  const res: any = await startProcess(data)
  const workOrder = res?.data?.workOrder
  form.work_order_data = workOrder
  return workOrder
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

defineExpose({ form, vmFormRef, addWorkorderFn, closeWorkorderFn })
</script>

<style lang="scss" scoped>
.eventItem {
}

</style>
