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

// 表单验证规则
const formRules = ref({
  processKey: [
    { required: true, message: '请选择工单', trigger: 'blur' }
  ]
})

// 选择工单
const workConfirm = (val) => {
  const target = options.value.find(item => item.value === val)
  if (target) selectedOptions.value = target
}

// 工单-列表（流程管理-事件管理）
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
      event_item_listFn() // 事件类型
    } else {
      options.value = []
    }
  } catch (error) {
    // 获取工单列表失败，使用空数组
    options.value = []
  }
}

// 选择的工单跟历史工单不一致的时候提示，同时发起新的工单，关闭旧的工单
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

// 发起工单成功
async function addWorkorderFn() {
  // 1️⃣ 已存在工单逻辑
  if (form.processKey && form.work_order_data) {
    const oldOrder = form.work_order_data
    // 同一工单，直接返回
    if (oldOrder.processKey === form.processKey) {
      return oldOrder
    }
    // 不同工单，且旧工单未结束 → 提示是否关闭
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
        // 用户取消
        return
      }
    }
  }
  // 2️⃣ 表单校验
  const valid = await formRef_1.value?.validate()
  if (!valid) return
  // 3️⃣ 参数构建
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
    frontFlag: true, // 发起工单的时候传frontFlag参数为true跟表单参数同级
    /* autoGetFormFlag这个参数为true表示自动查找表单值对应项，跟表单参数同级*/
    /* 比如自定义名称这里是abc，你传variables给我就是{abc:"事件类型值"} */
    autoGetFormFlag: true,
    variables: {
      no: props.listObj?.id || undefined, // 编号
      item: props.listObj?.item || undefined, // 类型
      name: props.listObj?.name || undefined, // 名称
      time: props.listObj?.created_at || undefined, // 时间
      address: props.listObj?.point?.address || undefined, // 地址
      describe: props.listObj?.describe || undefined, // 描述
      image: props.listObj?.pics || undefined//  图片
    }
  }
  // 4️⃣ 发起流程
  const res: any = await startProcess(data)
  const workOrder = res?.data?.workOrder
  form.work_order_data = workOrder
  return workOrder
}

// 事件类型-自动转工单默认选中
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
      workConfirm(form.processKey) // 选择工单
    }
  }
}

onMounted(() => {
  workListFn() // 流程管理-事件管理
})

defineExpose({ form, vmFormRef, addWorkorderFn, closeWorkorderFn })
</script>

<style lang="scss" scoped>
.eventItem {
}

</style>
