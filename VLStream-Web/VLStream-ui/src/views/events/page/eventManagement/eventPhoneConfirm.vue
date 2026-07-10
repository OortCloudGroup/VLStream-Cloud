<template>
  <el-dialog v-model="dialogVisible" class="diaOutSet" title="确认事件" top="5vh" width="60%" :before-close="handleClose" destroy-on-close>
    <div v-if="eventDetailData">
      <div class="basicTitle codeActBox flexRowAC">
        基本属性
        <div class="line" />
        <div class="lineT flexRowAC" @click="codeAct=!codeAct">
          {{ codeAct === true ? '收起' : '展开' }}
          <oort-svg-icon v-if="codeAct" class="elmenuIconImg" name="shrink" width="24" height="24" color="#999" />
          <oort-svg-icon v-else class="elmenuIconImg" name="open" width="24" height="24" />
        </div>
      </div>
      <template v-if="codeAct">
        <div class="detailsBox">
          <div class="details_row_box">
            <img src="@/assets/img/maintenance/typeIcon.png" alt="养护详情" />
            <div>事件类型</div>
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
            <img src="@/assets/img/maintenance/img.png" alt="养护详情" />
            <div>抓拍照片</div>
          </div>
          <div class="d_r_content">
            <div v-if="eventDetailData.pics?.length > 0">
              <el-image
                :src="eventDetailData.pics[0]"
                :preview-src-list="eventDetailData.pics"
                :initial-index="0"
                class="d_r_img"
                fit="cover"
                :preview-teleported="true"
              />
              <span class="eventImgText">+{{ eventDetailData.pics.length }}</span>
            </div>
            <span v-else>暂无图片</span>
          </div>
        </div>
        <div class="detailsBox">
          <div class="details_row_box">
            <img src="@/assets/img/maintenance/vidio.png" alt="养护详情" />
            <div>录制视频</div>
          </div>
          <div class="d_r_content">
            <div v-if="eventDetailData.videoUrl" class="video-container" @click="openVideoDialog(eventDetailData)">
            </div>
            <div v-else class="no-video">
              <span>暂无视频</span>
            </div>
          </div>
        </div>
        <!-- <div class="detailsBox">
        <div class="details_row_box">
          <img src="@/assets/img/maintenance/popup.png" alt="养护详情" />
          <div>事件描述</div>
        </div>
        <div class="d_r_content">
          {{ eventDetailData.describe }}
        </div>
      </div> -->
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
            <img src="@/assets/img/maintenance/time.png" alt="养护详情" />
            <div>设备名称</div>
          </div>
          <div class="d_r_content">
            {{ eventDetailData.device_name }}
            <el-tag type="primary">
              {{ eventDetailData.device_tag }}
            </el-tag>
          </div>
        </div>
        <div class="detailsBox">
          <div class="details_row_box">
            <img src="@/assets/img/maintenance/id.png" alt="" />
            <div>设备ID</div>
          </div>
          <div class="d_r_content">
            {{ eventDetailData.device_id }}
          </div>
        </div>
        <div class="detailsBox">
          <div class="details_row_box">
            <img src="@/assets/img/maintenance/time.png" alt="" />
            <div>告警时间</div>
          </div>
          <div class="d_r_content">
            {{ eventDetailData.created_at }}
          </div>
        </div>
      </template>
    </div>
    <div v-if="eventDetailData.status !== 1 && eventDetailData.mod_status === 0">
      <div class="details_container">
        <div class="basicTitle codeActBox flexRowAC">
          事件确认
          <div class="line" />
          <div class="lineT flexRowAC" @click="codeAct_2=!codeAct_2">
            {{ codeAct_2 === true ? '收起' : '展开' }}
            <oort-svg-icon v-if="codeAct_2" class="elmenuIconImg" name="shrink" width="24" height="24" color="#999" />
            <oort-svg-icon v-else class="elmenuIconImg" name="open" width="24" height="24" />
          </div>
        </div>
        <template v-if="codeAct_2">
          <div class="e_addFeedback">
            <el-form
              ref="formRef"
              v-loading="formLoading"
              :model="formData"
              :rules="formRules"
              label-width="auto"
            >
              <el-form-item label="告警确认" prop="mod_status">
                <el-radio-group v-model="formData.mod_status">
                  <el-radio :value="1" size="large">
                    真实告警
                  </el-radio>
                  <!--                <el-radio :value="2" size="large">
                                  维保
                                </el-radio>-->
                  <el-radio :value="3" size="large">
                    误报
                  </el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="是否转工单" prop="name">
                <el-switch
                  v-model="formData.work_order_status"
                  :active-value="1"
                  :inactive-value="0"
                  active-text="是"
                  inactive-text="否"
                />
              </el-form-item>
              <el-form-item v-if="formData.work_order_status" label="选择工单" prop="workOrderId">
                <el-select v-model="formData.workOrderId" placeholder="请选择工单">
                  <el-option
                    v-for="item in options"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item v-if="formData.work_order_status!==1" label="图片" prop="fileUrls">
                <div class="avatarBox">
                  <MultiImageUpload v-model="formData.pics" :size-limit="2" :accept="'image/*'" tip="上传图片" :max-count="5" />
                </div>
              </el-form-item>
              <el-form-item v-if="formData.work_order_status!==1" label="描述">
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
                    keyword-empty-tips="请先输入任务名称"
                  />
                </div>
                <CommonExpressions :content="formData.describe" @selectContent="selectContent" />
              </el-form-item>
              <el-form-item v-if="formData.work_order_status!==1" label="是否完成" prop="status">
                <el-switch
                  v-model="formData.status"
                  size="large"
                  :active-value="1"
                  :inactive-value="2"
                  active-text="已完成"
                  inactive-text="未完成"
                />
              </el-form-item>
            </el-form>
          </div>
        </template>
      </div>
    </div>
    <div class="d_h_maintainBtns">
      <el-button @click="handleClose">
        取 消
      </el-button>
      <el-button v-preReClick type="primary" :disabled="formLoading" @click="addFeedbackForm">
        确 定
      </el-button>
    </div>
    <!-- 视频播放弹窗 -->
    <el-dialog
      v-model="videoDialogVisible"
      title="视频播放"
      width="80%"
    >
      <div class="video-dialog-container">
        <OortVideoPlayer v-if="currentVideoUrl" :video="{ url: currentVideoUrl }" />
      </div>
    </el-dialog>
  </el-dialog>
</template>
<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import MultiImageUpload from './components/multiImageUpload.vue'
import CommonExpressions from '@/components/commonExpressions.vue'
import OortVideoPlayer from '@/components/oortVideoPlayer.vue'
import { eventDetail, addFeedback, feedbackList } from '@/api/smartCity/events'
import { useUserStore } from '@/store/modules/useraPaas'
import { ElMessage } from 'element-plus'
import { listModel } from '@/api/processui'
import { resolveWorkOrderAppContext } from '@/utils/workOrderAppContext'

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
let codeAct_2 = ref(false) // 展开收缩
let codeAct = ref(true) // 展开收缩
const options = ref<any>([])
const props = defineProps<Props>()
const emit = defineEmits(['update:visible', 'refresh'])
const harvest = ref(false)
const formLoading = ref(false)
const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
})

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false
  emit('refresh')
}
const _submitForm = () => {
  // 提交
}
const selectContent = (content) => {
  formData.value.describe = content
}

const eventDetailData = ref<EventDetailData>(null)

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

const videoDialogVisible = ref(false)
const currentVideoUrl = ref<string>('')
const openVideoDialog = (data) => {
  if (data.videoUrl) {
    currentVideoUrl.value = data.videoUrl
    videoDialogVisible.value = true
  }
}

// 数据初始化
watch([() => props.visible, () => props.data], async([newVisible, newData]) => {
  if (newVisible && newData) {
    formData.value = initFormData()
    eventDetailData.value = {}
    feedbackListData.value = []
    activeIndex.value = '1'
    harvest.value = false
    await getEventDetail()
    await getFeedbackList()
  }
}, { immediate: true })
const initFormData = () => ({
  id: eventDetailData.value?.id,
  mod_status: 0,
  work_order_status: 0,
  describe: '',
  workOrderId: '',
  pics: [] as string[],
  status: 1
})
const formData = ref(initFormData())
const formRef = ref()

// 表单验证规则
const formRules = ref({
  describe: [
    { required: true, message: '请输入反馈描述', trigger: 'blur' }
  ],
  mod_status: [
    { required: true, message: '请选择告警确认', trigger: 'blur' }
  ],
  workOrderId: [
    { required: true, message: '请选择工单', trigger: 'blur' }
  ]
})

// 获取工单列表
const fetchWorkOrderList = async() => {
  try {
    const appObj = await resolveWorkOrderAppContext()
    const data = {
      accessToken: store.userInfo?.accessToken,
      pageNum: 1,
      pageSize: 99,
      workOrderCategory: appObj?.appId
    }

    const res: any = await listModel(data)
    if (res?.code === 200 && res?.rows) {
      options.value = res.rows.map((item: any) => ({
        label: item.modelName,
        value: item.modelId
      }))
    } else {
      options.value = []
    }
  } catch (error) {
    // 获取工单列表失败，使用空数组
    options.value = []
  }
}

// 添加事件反馈
const addFeedbackForm = async() => {
  // 表单验证
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (error) {
    return // 验证失败，不提交
  }
  const params = {
    accessToken: store.token,
    ...formData.value,
    id: eventDetailData.value?.id
  }
  // 反馈时状态为待确认时,必填 (状态 1:已完成 2:待处理)
  if (eventDetailData.value.status === 2 && !params.mod_status) {
    return ElMessage.warning('请选择告警确认')
  }
  const res: any = await addFeedback(params)
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
    task_event_id: eventDetailData.value?.id
  }
  const res: any = await feedbackList(params)
  if (res.code === 200) {
    feedbackListData.value = res.data.list
  }
}

onMounted(() => {
  fetchWorkOrderList() // 获取工单列表
})
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
  //margin-top: 10px !important;
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

</style>
