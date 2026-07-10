<template>
  <div>
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
          {{ eventDetailData.updated_at }}
        </div>
      </div>
    </div>
    <div class="details_container">
      <div class="basicTitle codeActBox flexRowAC">
        工单办理
        <div class="line" />
        <div class="lineT flexRowAC" @click="harvest=!harvest">
          {{ harvest === true ? '收起' : '展开' }}
          <oort-svg-icon v-if="harvest" class="elmenuIconImg" name="shrink" width="24" height="24" color="#999" />
          <oort-svg-icon v-else class="elmenuIconImg" name="open" width="24" height="24" />
        </div>
      </div>
      <!--工单办理-->
      <div v-if="harvest">
        <task-handle-detail
          v-if="harvest&&ditem"
          :submit="false"
          :item="ditem"
          class="eventWorkDetBox"
          tabs-view="record"
          @close="emit('close')"
          @handle="emit('handel')"
        />
      </div>
      <div class="d_h_maintainBtns">
        <el-button class="bigBtn" @click="emit('close')">
          取 消
        </el-button>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
import PersonInfoCard from '@/components/personInfoCard.vue'
import TaskHandleDetail from '@/pages/processui/views/page/flowManage/taskHandleDetail.vue'
import { eventDetail } from '@/api/smartCity/events'
import { useUserStore } from '@/store/modules/useraPaas'

const ditem = ref<any>(null)
const store = useUserStore()
let codeAct = ref(true) // 展开收缩
const props = defineProps(['workorder'])
const emit = defineEmits(['close', 'handle'])
const harvest = ref(false)
const eventDetailData = ref<any>({})

// 事件详情
const eventDetailFn = async() => {
  const params = {
    id: props.workorder?.eventNumber,
    accessToken: store.token
  }
  const res: any = await eventDetail(params)
  if (res.code === 200) {
    eventDetailData.value = res.data || null
  }
}

// 数据初始化
watch(() => props.workorder, (val) => {
  ditem.value = {
    workorderId: val.workorderId,
    procInsId: val.procInsId,
    taskId: val.taskId
  }
  if (val?.eventNumber) eventDetailFn()
}, { immediate: true })
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

.feedbacklist {
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

:deep(.eventWorkDetBox){
  margin: 0 -20px;

  .el-tabs__item.is-top {
    font-size: 14px;
    font-weight: 400;
  }
}
</style>
