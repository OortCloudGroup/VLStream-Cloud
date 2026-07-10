<template>
  <el-dialog v-model="dialogVisible" class="diaOutSet" title="事件详情" top="5vh" width="60%" :before-close="handleClose" destroy-on-close>
    <div class="basicTitle codeActBox flexRowAC">
      基本属性
      <div class="line" />
      <div class="lineT flexRowAC" @click="codeAct=!codeAct">
        {{ codeAct === true ? '收起' : '展开' }}
        <oort-svg-icon v-if="codeAct" class="elmenuIconImg" name="shrink" width="24" height="24" color="#999" />
        <oort-svg-icon v-else class="elmenuIconImg" name="open" width="24" height="24" />
      </div>
    </div>
    <div v-if="codeAct">
      <div class="detailsBox">
        <div class="details_row_box">
          <img src="@/assets/img/maintenance/task.png" alt="养护详情" />
          <div>事件名称</div>
        </div>
        <div class="d_r_content">
          {{ eventDetailData.name }}
          <el-tag :type="eventDetailData.status === 1 ? 'primary' : 'success'">
            {{ eventDetailData.status === 1 ? '已完成' : '正在处理' }}
          </el-tag>
        </div>
      </div>
      <div class="detailsBox">
        <div class="details_row_box">
          <img src="@/assets/img/maintenance/popup.png" alt="养护详情" />
          <div>事件描述</div>
        </div>
        <div class="d_r_content">
          {{ eventDetailData.describe }}
        </div>
      </div>
      <div class="detailsBox">
        <div class="details_row_box">
          <img src="@/assets/img/maintenance/address.png" alt="养护详情" />
          <div>事件位置</div>
        </div>
        <div class="d_r_content">
          {{ eventDetailData.point?.address }}
        </div>
      </div>
      <div class="detailsBox">
        <div class="details_row_box">
          <img src="@/assets/img/maintenance/img.png" alt="养护详情" />
          <div>拍传</div>
        </div>
        <div class="d_r_content">
          <el-image
            v-for="(url, index) in eventDetailData.pics"
            :key="index"
            :src="url"
            :preview-src-list="eventDetailData.pics"
            :initial-index="index"
            class="d_r_img"
            fit="cover"
            :preview-teleported="true"
          />
        </div>
      </div>
      <div class="detailsBox">
        <div class="details_row_box">
          <img src="@/assets/img/maintenance/staff.png" alt="巡查详情" />
          <div>上报人员</div>
        </div>
        <div class="details_row_content">
          <PersonInfoCard :uuid="eventDetailData.uuid" />
        </div>
      </div>
      <div class="detailsBox">
        <div class="details_row_box">
          <img src="@/assets/img/maintenance/staff2.png" alt="巡查详情" />
          <div>执行人员</div>
        </div>
        <div class="details_row_content">
          <div v-if="eventDetailData.uuids && eventDetailData.uuids.length > 0" class="details_row_content_persons">
            <PersonInfoCard v-for="(item,index) in eventDetailData.uuids" :key="index" :uuid="item.uuid" />
          </div>
        </div>
      </div>
      <!-- <div class="detailsBox">
        <div class="details_row_box">
          <img src="@/assets/img/location/car9.png" alt="巡查详情" />
          <div>车辆</div>
        </div>
        <div class="details_row_content">
          <img src="@/assets/img/maintenance/car.png" alt="" class="d_r_car_icon" />
          <div>
            <div class="name_box">
              粤B 123456
            </div>
            <div>
              何诗琪
            </div>
            <div class="phone_box">
              18270971339 <img src="@/assets/img/maintenance/phone.png" alt="" class="phone_icon" />
            </div>
          </div>
        </div>
      </div> -->
      <div class="detailsBox">
        <div class="details_row_box">
          <img src="@/assets/img/maintenance/time.png" alt="养护详情" />
          <div>上报时间</div>
        </div>
        <div class="d_r_content">
          {{ eventDetailData.created_at }}
        </div>
      </div>
      <div class="detailsBox">
        <div class="details_row_box">
          <img src="@/assets/img/maintenance/time.png" alt="养护详情" />
          <div>完成时间</div>
        </div>
        <div class="d_r_content">
          {{ eventDetailData.finish_at }}
        </div>
      </div>
    </div>
    <div v-if="false&&eventDetailData.status === 2">
      <div class="details_container">
        <div class="basicTitle codeActBox flexRowAC">
          事件反馈
          <div class="line" />
          <div class="lineT flexRowAC" @click="codeAct_2=!codeAct_2">
            {{ codeAct_2 === true ? '收起' : '展开' }}
            <oort-svg-icon v-if="codeAct_2" class="elmenuIconImg" name="shrink" width="24" height="24" color="#999" />
            <oort-svg-icon v-else class="elmenuIconImg" name="open" width="24" height="24" />
          </div>
        </div>
        <div v-if="codeAct_2" class="e_addFeedback">
          <el-form ref="formRef" v-loading="formLoading" :rules="formRules" :model="formData" label-width="auto">
            <el-form-item label="是否转工单" prop="workOrder">
              <el-switch v-model="formData.workOrder" size="large" active-text="是" inactive-text="否" />
            </el-form-item>
            <el-form-item v-if="formData.workOrder" label="选择工单" prop="workOrderId">
              <el-select v-model="formData.workOrderId" placeholder="请选择工单">
                <el-option label="选项1" value="1" />
                <el-option label="选项2" value="2" />
              </el-select>
            </el-form-item>
            <!--            <el-form-item label="是否通知" prop="notice">-->
            <!--              <div>-->
            <!--                <el-switch v-model="formData.notice" size="large" active-text="是" inactive-text="否" />-->
            <!--                <div>-->
            <!--                  <el-checkbox-group v-model="formData.noticeList">-->
            <!--                    <el-checkbox v-for="item in noticeListDate" :key="item" :label="item.label" :value="item.value" />-->
            <!--                  </el-checkbox-group>-->
            <!--                </div>-->
            <!--              </div>-->
            <!--            </el-form-item>-->
            <el-form-item label="反馈图片" prop="fileUrls">
              <div class="avatarBox">
                <multi-image-upload v-model="formData.pics" :size-limit="2" :accept="'image/*'" tip="上传图片" :max-count="5" />
              </div>
            </el-form-item>
            <el-form-item label="反馈描述" prop="describe">
              <div style="width: 100%;">
                <el-input
                  v-model="formData.describe"
                  type="textarea"
                  :rows="5"
                  placeholder="请输入描述"
                  maxlength="200"
                  show-word-limit
                />
                <aiIconRemark
                  v-model="formData.describe"
                  :number="256"
                  mod="描述"
                  :keyword="formData.describe"
                  keyword-empty-tips="请先输入描述"
                />
              </div>
              <CommonExpressions :content="formData.describe" @selectContent="selectContent" />
            </el-form-item>
          </el-form>
          <el-form-item label="是否完成" prop="status">
            <el-switch v-model="formData.status" size="large" :active-value="1" :inactive-value="2" active-text="已完成" inactive-text="未完成" />
          </el-form-item>
        </div>
      </div>
    </div>
    <div class="details_container">
      <div class="basicTitle codeActBox flexRowAC">
        反馈列表
        <div class="line" />
        <div class="lineT flexRowAC" @click="harvest=!harvest">
          {{ harvest === true ? '收起' : '展开' }}
          <oort-svg-icon v-if="harvest" class="elmenuIconImg" name="shrink" width="24" height="24" color="#999" />
          <oort-svg-icon v-else class="elmenuIconImg" name="open" width="24" height="24" />
        </div>
      </div>
      <div v-if="harvest">
        <el-tabs v-model="activeIndex" class="left-tabs">
          <el-tab-pane label="反馈记录" name="1" />
          <el-tab-pane label="工单记录" name="2" />
        </el-tabs>
        <div class="feedbacklist">
          <FeedbackRecord v-if="activeIndex === '1'" :feedback-list-data="feedbackListData" />
          <WorkOrderRecord v-if="activeIndex === '2'" :item="eventDetailData" />
        </div>
      </div>
      <div class="d_h_maintainBtns">
        <el-button @click="handleClose">
          取 消
        </el-button>
        <el-button v-if="false&&eventDetailData.status === 2" type="primary" :disabled="formLoading" @click="addFeedbackForm">
          确 定
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>
<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import MultiImageUpload from '../multiImageUpload.vue'
import CommonExpressions from '@/components/commonExpressions.vue'
import FeedbackRecord from './feedbackRecord.vue'
import PersonInfoCard from '@/components/personInfoCard.vue'
import WorkOrderRecord from './workOrderRecord.vue'
import { eventDetail, addFeedback, feedbackList } from '@/api/smartCity/events'
import { useUserStore } from '@/store/modules/useraPaas'
import { ElMessage } from 'element-plus'
const store = useUserStore()
const activeIndex = ref('1')
interface Props {
  data?: any
  visible?: boolean
}

interface EventDetailData {
  client?: string
  created_at?: string
  describe?: string
  id?: string
  item?: string
  name?: string
  pics?: string[]
  point?: {
    address?: string
    lat?: number
    lat_initial?: number
    lng?: number
    lng_initial?: number
  }
  send_pics?: string[]
  status?: number
  updated_at?: string
  uuid?: string
  uuids?: string[]
}

interface FeedbackItem {
  created_at?: string
  updated_at?: string
  describe?: string
  id?: string
  pics?: string[]
  point?: {
    address?: string
    lat?: number
    lat_initial?: number
    lng?: number
    lng_initial?: number
  }
  status?: number
  task_event_id?: string
  uuid?: string
}
let codeAct = ref(true) // 展开收缩
let codeAct_2 = ref(false) // 展开收缩
const props = defineProps<Props>()
const emit = defineEmits(['update:visible'])
const harvest = ref(false)
const formLoading = ref(false)
const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
})

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false
}
const _submitForm = () => {
  // 提交
}
const selectContent = (content) => {
  formData.value.describe = content
}

const eventDetailData = ref<EventDetailData>({})

const getEventDetail = async() => {
  const params = {
    id: props.data.id,
    accessToken: store.token
  }
  const res: any = await eventDetail(params)
  if (res.code === 200) {
    eventDetailData.value = res.data
  }
}

// 数据初始化
watch([() => props.visible, () => props.data], ([newVisible, newData]) => {
  if (newVisible && newData) {
    formData.value = initFormData()
    formData.value.id = newData.id
    formData.value.point = newData.point
    eventDetailData.value = {}
    feedbackListData.value = []
    activeIndex.value = '1'
    harvest.value = false
    getEventDetail()
    getFeedbackList()
  }
}, { immediate: true })
const initFormData = () => ({
  accessToken: store.token,
  id: undefined,
  describe: '',
  pics: [],
  point: {},
  status: 2, // 1:已完成 2:正在处理
  workOrder: 1, // 1:是 2:否 是否转工单
  workOrderId: 1,
  notice: true,
  noticeList: []
})
const formData = ref(initFormData())
const formRef = ref()
// const noticeListDate = [
//   {
//     label: '短信',
//     value: 1
//   },
//   {
//     label: '邮箱',
//     value: 2
//   },
//   {
//     label: '站内',
//     value: 3
//   },
//   {
//     label: '企业微信',
//     value: 4
//   },
//   {
//     label: '钉钉',
//     value: 5
//   },
//   {
//     label: 'APP',
//     value: 6
//   },
//   {
//     label: '微信机器人',
//     value: 7
//   },
//   {
//     label: '微信公众号',
//     value: 8
//   },
//   {
//     label: 'Workup',
//     value: 9
//   }
// ]

// 表单验证规则
const formRules = ref({
  describe: [
    { required: true, message: '请输入反馈描述', trigger: 'blur' }
  ]
})

// 添加事件反馈
const addFeedbackForm = async() => {
  // 表单验证
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch (error) {
    return // 验证失败，不提交
  }
  const res: any = await addFeedback(formData.value)
  if (res.code === 200) {
    ElMessage.success('反馈成功')
    const currentId = formData.value.id
    const currentPoint = formData.value.point
    formData.value = initFormData()
    formData.value.id = currentId
    formData.value.point = currentPoint
    getFeedbackList()
    handleClose()
  }
}
const feedbackListData = ref<FeedbackItem[]>([])
// 反馈列表
const getFeedbackList = async() => {
  const params = {
    accessToken: store.token,
    page: 1,
    pagesize: 9999,
    task_event_id: formData.value.id
  }
  const res: any = await feedbackList(params)
  if (res.code === 200) {
    feedbackListData.value = res.data.list
  }
}
</script>

<style scoped lang="scss">
.detailsBox {
  display: flex;
  align-items: flex-start;
  flex-wrap: nowrap;
  font-family: SourceHanSansSC-Regular;
  font-size: 14px;
  font-weight: 400;
  color: #333333;
  margin-bottom: 15px;

  .details_row_box {
    display: flex;
    align-items: center;
    color: #515A6E;
    margin-right: 10px;
    width: 100px;
    flex-shrink: 0;
    line-height: 30px;

  }

  .details_row_box img {
    width: 20px;
    margin-right: 5px;
  }
}

.d_r_content {
  flex: 1;
  word-wrap: break-word;
  word-break: break-all;
  line-height: 30px;
}

.title_box {
  display: flex;
  align-items: center;
  font-family: MicrosoftYaHei;
  font-size: 16px;
  color: #333333;
  line-height: 22px;
  font-weight: 400;
  margin-bottom: 15px;

  .title_line {
    width: 8px;
    height: 16px;
    border-left: 3px solid #0378FF;

  }
}

.title_top_margin {
  margin-top: 30px;
}

.d_r_img {
  width: 80px;
  height: 80px;
  margin-right: 10px;
}

.details_container {
  //margin-top: 30px;
}

:deep(.el-form-item__label) {
  min-width: auto !important;
  margin-right: 35px;
}

.e_addFeedback {
  width: 70%;
}

.ai-icon-container {
  position: absolute;
  top: 380px;
  right: 100px;
  z-index: 10;
}

.d_r_car_icon {
  width: 4.2rem;
  height: 4.2rem;
  margin-right: 5px;
}

.details_row_content_persons {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.details_row_content {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-right: 15px;
  font-family: MicrosoftYaHei;
  font-size: 12px;
  color: #999999;
  font-weight: 400;

  .name_box {
    font-family: MicrosoftYaHei;
    font-size: 14px;
    color: #333333;
    line-height: 19px;
    font-weight: 400;
  }

  .phone_box {
    display: flex;
    align-items: center;
  }

  .phone_icon {
    width: 20px;
    margin-left: 5px;
  }
}

.d_h_maintainBtns {
  display: flex !important;
  justify-content: flex-end !important;
  //margin-top: 20px !important;
}

.title_box_container {
  display: flex;
  align-items: center;
  margin: 20px 0;

  .title_line2 {
    flex: 1;
    height: 5px;
    border-bottom: 1px dashed #8a8a8a;
    margin: 0 5px;
  }

  .down_up_icon {
    width: 25px;
    height: 25px;
  }
}
.feedbacklist{
  margin-top: 25px;
}

:deep(.el-menu .el-menu-item) {
  transition: none !important;
  border-left: 3px solid transparent !important;
}

:deep(.el-menu .el-menu-item.is-active) {
  border-left: none !important;
}

:deep(.el-menu .el-menu-item:hover) {
  border-left: 3px solid transparent !important;
}

// 更多-展开
.codeActBox {
  gap: 10px;
  color: var(--el-color-primary);
  padding-bottom: 10px;

  .line {
    flex: 1;
    border-bottom: 2px dashed #ccc;
  }

  .lineT {
    cursor: pointer;
  }
}

// tabs
:deep(.el-tabs__header) {
  padding-top: 0px;
  margin-top: -6px;

  .el-tabs__nav-wrap::after {
    display: none;
  }
}
</style>
