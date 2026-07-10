<template>
  <div>
    <el-dialog v-model="dialogVisible" class="diaOutSet" title="任务分配" top="5vh" width="40%" :before-close="handleClose" destroy-on-close>
      <div class="details_container">
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
              <img src="@/assets/img/maintenance/task.png" alt="巡查详情" />
              <div>事件名称</div>
            </div>
            <div class="d_r_content">
              <span>{{ currentItem.name }}</span>
              <!-- <el-tag type="success" class="tag_box">
            进行中
          </el-tag> -->
            </div>
          </div>
          <div class="detailsBox">
            <div class="details_row_box">
              <img src="@/assets/img/maintenance/popup.png" alt="巡查详情" />
              <div>事件描述</div>
            </div>
            <div class="d_r_content">
              {{ currentItem.describe }}
            </div>
          </div>
          <div class="detailsBox">
            <div class="details_row_box">
              <img src="@/assets/img/maintenance/address.png" alt="巡查详情" />
              <div>事件位置</div>
            </div>
            <div class="d_r_content">
              {{ currentItem.point.address }}
            </div>
          </div>
          <div class="detailsBox">
            <div class="details_row_box">
              <img src="@/assets/img/maintenance/img.png" alt="养护详情" />
              <div>拍传</div>
            </div>
            <div class="d_r_content">
              <el-image
                v-for="(url, index) in currentItem.pics"
                :key="index"
                :src="url"
                :preview-src-list="currentItem.pics"
                :initial-index="index"
                class="d_r_img"
                fit="cover"
                :preview-teleported="true"
              />
              <span v-if="currentItem.pics.length === 0">无</span>
            </div>
          </div>
          <div v-if="currentItem.uuids && currentItem.uuids.length > 0" class="detailsBox">
            <div class="details_row_box">
              <img src="@/assets/img/maintenance/staff.png" alt="巡查详情" />
              <div>上报人员</div>
            </div>
            <div class="details_row_content">
              <!-- <i-d2-head-pic id="1a3d2666-e0fe-459f-81da-c3c21ef3628c" style="width: 4rem;height: 4rem;margin: 0 0.25rem" />
            <div>
              <div class="name_box">
                龙伟
              </div>
              <div>
                园林绿化局｜勘察部｜实施部
                <br />
                实施一组
              </div>
              <div class="phone_box">
                18270971339 <img src="@/assets/img/maintenance/phone.png" alt="" class="phone_icon" />
              </div>
            </div> -->
              <PersonInfoCard v-for="(item,i) in currentItem?.uuids" :key="i" :uuid="item.uuid" />
            </div>
          </div>
          <div class="detailsBox">
            <div class="details_row_box">
              <img src="@/assets/img/maintenance/time.png" alt="巡查详情" />
              <div>上报时间</div>
            </div>
            <div class="date-text">
              {{ currentItem.created_at }}
            </div>
          </div>
        </template>
      </div>
      <div class="details_container">
        <div class="basicTitle codeActBox flexRowAC">
          分配人员
          <div class="line" />
          <div class="lineT flexRowAC" @click="codeAct_1=!codeAct_1">
            {{ codeAct_1 === true ? '收起' : '展开' }}
            <oort-svg-icon v-if="codeAct_1" class="elmenuIconImg" name="shrink" width="24" height="24" color="#999" />
            <oort-svg-icon v-else class="elmenuIconImg" name="open" width="24" height="24" />
          </div>
        </div>
        <template v-if="codeAct_1">
          <div style="width: 95%;">
            <el-form
              ref="formRef"
              :model="formData"
              :rules="formRules"
              label-width="auto"
            >
              <el-form-item label="执行人员" prop="userName">
                <div class="staff-input-wrapper" @click="openStartDeptSelect(1)">
                  <el-input v-model="formData.userName" placeholder="请选择执行人员" />
                  <img :src="staffIcon" class="staff-icon" alt="选择人员" />
                </div>
              </el-form-item>
              <el-form-item label="执行人单位" prop="dept_name">
                <el-input v-model="formData.dept_name" placeholder="请选择执行人单位" clearable @click="openStartDeptSelect(2)" />
              </el-form-item>

              <!-- <el-form-item label="车辆" prop="carName">
              <div class="staff-input-wrapper">
                <el-input v-model="formData.carName" placeholder="请选择车辆" readonly @click="openCarListDialog" />
                <img :src="carIcon" class="staff-icon" alt="车辆图标" />
              </div>
            </el-form-item> -->
              <!-- 描述 -->
              <el-form-item label="描述">
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
                    keyword-empty-tips="请先输入描述单词"
                  />
                </div>
                <CommonExpressions :content="formData.describe" @selectContent="selectContent" />
              </el-form-item>
            </el-form>
          </div>
        </template>
        <div class="d_h_maintainBtns">
          <el-button @click="handleCancel">
            取 消
          </el-button>
          <el-button type="primary" :disabled="formLoading" @click="submitForm">
            确 定
          </el-button>
        </div>
      </div>
      <el-dialog v-model="chooseDeptVis" title="选择执行人员" width="50%">
        <address-seting-dialog
          :active-tab="type==1?1:3"
          :mode="type"
          :is-single="true"
          @saveChoose="confirmDept"
          @cancelChoose="chooseDeptVis=false"
        />
      </el-dialog>
    </el-dialog>
    <!-- <CarListDialog v-model:visible="carListVisible" @select="handleCarSelect" /> -->
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import aiIconRemark from '@/components/aiIconRemark.vue'
import AddressSetingDialog from '@/components/personHome/addressSetingDialog.vue'
import { ElMessage } from 'element-plus'
import staffIcon from '@/assets/img/maintenance/staff.png'
// import carIcon from '@/assets/img/location/car9.png'
import CommonExpressions from '@/components/commonExpressions.vue'
import PersonInfoCard from '@/components/PersonInfoCard.vue'
// import CarListDialog from '@/components/carListDialog.vue'
import { allocate } from '@/api/smartCity/events'
const formRef = ref()
import { useUserStore } from '@/store/modules/useraPaas'
const store = useUserStore()

const type = ref(1) // 1:执行人员，2:执行人单位
const formLoading = ref(false)
const initFormData = {
  accessToken: store.token,
  userName: '',
  uuids: [],
  carName: '',
  imei: '',
  describe: '',
  dept_name: '',
  id: ''
}
const formData = ref({ ...initFormData })
const formRules = ref({
  userName: [{ required: true, message: '请选择执行人员', trigger: 'blur' }]
})

const chooseDeptVis = ref(false)
let codeAct = ref(true) // 展开收缩
let codeAct_1 = ref(false) // 展开收缩
const props = defineProps<{
  currentItem?: any
  visible?: boolean
}>()

const emit = defineEmits(['update:visible', 'refresh'])

const dialogVisible = computed({
  get: () => props.visible ?? false,
  set: (value: boolean) => {
    emit('update:visible', value)
  }
})

const handleClose = () => {
  dialogVisible.value = false
}
// 打开部门选择对话框
const openStartDeptSelect = (selectType: number) => {
  type.value = selectType
  chooseDeptVis.value = true
}

// 取消操作
const handleCancel = () => {
  dialogVisible.value = false
}

// 提交表单
const submitForm = async() => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    formData.value.id = props.currentItem.id
    const res = await allocate(formData.value)
    if (res.code === 200) {
      ElMessage.success('提交成功')
      dialogVisible.value = false
      emit('refresh')
    }
  } catch (error) {
    ElMessage.error('提交失败')
  } finally {
    formLoading.value = false
  }
}

// 确认选择部门/人员
const confirmDept = async(data: any) => {
  if (type.value === 1) {
    // 选择执行人员
    if (data.user && data.user.length > 0) {
      formData.value.uuids = data.user.map((item: any) => ({
        u_type: 1,
        uuid: item.user_id
      }))
      formData.value.userName = data.user.map((item: any) => item.user_name).join(',')
    }
  } else if (type.value === 2) {
    // 选择执行人单位
    if (data.dept && data.dept.length > 0) {
      formData.value.dept_name = data.dept[0].dept_name || data.dept[0].name || ''
    }
  }
  chooseDeptVis.value = false
}
const selectContent = (content: any) => {
  formData.value.describe = content
}
// const carListVisible = ref(false)
// const openCarListDialog = () => {
//   carListVisible.value = true
// }
// const handleCarSelect = (selection: any) => {
//   formData.value.imei = selection[0].imei
//   formData.value.carName = selection[0].name
// }
</script>

<style scoped lang="scss">
.detailsBox {
  display: flex;
  align-items: flex-start; // 改为flex-start以便内容换行时对齐顶部
  flex-wrap: nowrap;
  font-family: SourceHanSansSC-Regular;
  font-size: 14px;
  font-weight: 400;
  color: #333333;
  margin-bottom: 15px;

  .details_row_box {
    display: flex;
    align-items: center;
    color: #B1B9C5;
    // margin-right: 10px;
    width: 90px;
    flex-shrink: 0;
    line-height: 30px;
    margin-left: 20px;

  }

  .details_row_box img {
    width: 20px;
    margin-right: 5px;
  }

  .details_row_content {
    display: flex;
    align-items: center;
    margin-right: 10px;
  }

  .details_row_content img {
    width: 36px;
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
  margin-left: 10px;

  .title_line {
    width: 8px;
    height: 16px;
    border-left: 3px solid #0378FF;

  }
}
.title_top_margin{
  margin-top: 30px;
}
.d_r_img{
  width: 80px;
  height: 80px;
  margin-right: 10px;
}
:deep(.el-form-item__label){
  min-width: auto !important;
}
.d_h_maintainBtns{
  display: flex !important;
  justify-content: flex-end !important;
  //margin-top: 20px !important;
}
:deep(.el-form-item__content){
  margin-left: 15px !important;
}

.detailsBox {
  display: flex;
  align-items: flex-start; // 改为flex-start以便内容换行时对齐顶部
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

  .date-text {
    line-height: 30px;
  }

  .details_row_box img {
    width: 20px;
    margin-right: 5px;
  }

  .details_row_content {
    display: flex;
    align-items: center;
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

  .d_r_car_icon {
    width: 2.5rem;
    height: 2.5rem;
    margin-right: 5px;
  }
}

.mapBox {
  width: 100%;
  height: 540px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.d_r_content {
  flex: 1;
  word-wrap: break-word;
  word-break: break-all;
  line-height: 30px;
}

/* 主题切换控件样式 */
:deep(.theme-control) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

:deep(.theme-select) {
  background: rgba(255, 255, 255, 0.8) !important;
  border: 1px solid rgba(0, 0, 0, 0.2);
  border-radius: 4px;
  padding: 4px 6px;
  font-size: 12px;
  transition: all 0.3s;
}

:deep(.theme-select:hover) {
  background: rgba(255, 255, 255, 0.9) !important;
}

:deep(.theme-select:focus) {
  outline: none;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

.tag_box {
  margin-left: 5px;
}
.detailsBox {
  display: flex;
  align-items: flex-start; // 改为flex-start以便内容换行时对齐顶部
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

  .details_row_content {
    display: flex;
    align-items: center;
    margin-right: 10px;
  }

  .details_row_content img {
    width: 36px;
    margin-right: 5px;
  }

}
.staff-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;

  .el-input {
    width: 100%;
  }

  .staff-icon {
    position: absolute;
    right: 8px;
    top: 50%;
    transform: translateY(-50%);
    width: 16px;
    // height: 16px;
    cursor: pointer;
    transition: opacity 0.3s;
    z-index: 10;

    &:hover {
      opacity: 0.8;
    }
  }
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
