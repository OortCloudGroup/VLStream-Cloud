<template>
  <div class="choosePerBox">
    <div class="prop_title he">
      <span>设置通知对象</span>
    </div>
    <div class="prop_item">
      <el-radio-group v-model="copyActiveChooseData.approvalType" @change="changeApprovalType">
        <el-row>
          <el-col :span="8">
            <el-radio :value="1">
              指定人员
            </el-radio>
          </el-col>
          <slot />
        </el-row>
      </el-radio-group>
    </div>
    <div v-if="copyActiveChooseData.approvalType === 1" class="prop_item prop_item_group bo">
      <div v-for="(item,index) in copyActiveChooseData.users" :key="index" class="prop_item_user">
        <id2HeadPic :id="item" class="prop_item_user_headpic" />
        <div class="prop_item_user_name">
          <id2Name :id="item" />
        </div>
        <el-icon class="prop_item_user_remove" color="red" size="20" @click="removeUser(index)">
          <CircleCloseFilled />
        </el-icon>
      </div>
      <div class="prop_item prop_item_add">
        <img src="@/assets/img/processui/flownode/add.png" @click="addUser" />
      </div>
    </div>

    <div v-if="copyActiveChooseData.approvalType === 2" class="prop_item prop_item_group bo">
      <div v-for="(item,index) in tempDept" :key="index" class="prop_item_user">
        <oort-img class="prop_item_user_headpic" :src="item.dept_photo" default-type="dept" />
        <div class="prop_item_user_name">
          <span>{{ item.dept_name }}</span>
        </div>
        <el-icon class="prop_item_user_remove" color="red" size="20" @click="removeDept(index)">
          <CircleCloseFilled />
        </el-icon>
      </div>
      <div class="prop_item_add">
        <img src="@/assets/img/processui/flownode/add.png" @click="addDept" />
      </div>
    </div>

    <div v-if="copyActiveChooseData.approvalType === 4" class="prop_item bo">
      <el-select
        v-model="copyActiveChooseData.roles"
        multiple
        placeholder="请选择角色"
        style="width: 100%"
      >
        <el-option
          v-for="item in roleListArr"
          :key="item.ruuid"
          :label="item.name"
          :value="item.ruuid"
        />
      </el-select>
    </div>
    <!--岗位和职位-start-->
    <div v-if="copyActiveChooseData.approvalType === 6" class="prop_item bo">
      <el-radio-group v-model="jobPost" class="prop_item jobPostBox">
        <el-radio value="0" class="flexRowAC">
          <span class="prop_item_sp">提交人的岗位：</span>
          <el-select v-model="jobMod" placeholder="请选择岗位">
            <div class="flexRowAC universeBox">
              <span class="sp">{{ JJ ? '从最高上级向下选择' : '从直属上级向上选择' }}</span>
              <el-button class="flexRowAC universeItem" @click="JJ=!JJ">
                <oort-svg-icon width="20" height="20" name="universe" />
                {{ JJ ? '切为直属上级向上' : '切为最高上级向下' }}
              </el-button>
            </div>
            <div v-if="JJ" class="optBox">
              <el-option
                v-for="(item,i) in 20"
                :key="i"
                :value="parseInt(21-item) + ''"
                class="flexRowAC optitem"
                :label="item !== 1 ? '最高上级减' + i + '级' : '最高上级'"
              >
                {{ item !== 1 ? '最高上级减' + i + '级' : '最高上级' }}
                <oort-svg-icon class="optsvg" width="20" height="20" name="consent" />
              </el-option>
            </div>
            <div v-else class="optBox">
              <el-option
                v-for="(item,i) in 20"
                :key="i"
                :value="item + ''"
                class="flexRowAC optitem"
                :label="item !== 1 ? '直属上级加' + i + '级' : '直属上级'"
              >
                {{ item !== 1 ? '直属上级加' + i + '级' : '直属上级' }}
                <oort-svg-icon class="optsvg" width="20" height="20" name="consent" />
              </el-option>
            </div>
          </el-select>
        </el-radio>
        <el-radio value="1">
          <span class="prop_item_sp">提交人的职位：</span>
          <el-select v-model="postMod" placeholder="请选择职位">
            <div class="flexRowAC universeBox">
              <span class="sp">{{ JP ? '从最高上级向下选择' : '从直属上级向上选择' }}</span>
              <el-button class="flexRowAC universeItem" @click="JP=!JP">
                <oort-svg-icon width="20" height="20" name="universe" />
                {{ JP ? '切为直属上级向上' : '切为最高上级向下' }}
              </el-button>
            </div>
            <div v-if="JP" class="optBox">
              <el-option
                v-for="(item,i) in 20"
                :key="i"
                :value="parseInt(21-item) + ''"
                class="flexRowAC optitem"
                :label="item !== 1 ? `最高上级减${i}级` : '最高上级'"
              >
                {{ item !== 1 ? `最高上级减${i}级` : '最高上级' }}
                <oort-svg-icon class="optsvg" width="20" height="20" name="consent" />
              </el-option>
            </div>
            <div v-else class="optBox">
              <el-option
                v-for="(item,i) in 20"
                :key="i"
                :value="item + ''"
                class="flexRowAC optitem"
                :label="item !== 1 ? `直属上级加${i}级` : '直属上级'"
              >
                {{ item !== 1 ? `直属上级加${i}级` : '直属上级' }}
                <oort-svg-icon class="optsvg" width="20" height="20" name="consent" />
              </el-option>
            </div>
          </el-select>
        </el-radio>
      </el-radio-group>
    </div>
    <!--岗位和职位-end-->
    <el-dialog v-model="chooseUserVis" title="选择人员" width="50%">
      <address-seting-dialog
        v-if="chooseUserVis"
        :user-list="copyActiveChooseData.users"
        :mode="3"
        :is-single="false"
        @saveChoose="confirmUser"
        @close="closeAddressDialog"
      />
    </el-dialog>
    <el-dialog v-model="chooseDeptVis" title="选择部门" width="50%">
      <address-seting-dialog
        v-if="chooseDeptVis"
        :dept-list="copyActiveChooseData.dept"
        :mode="2"
        :is-single="false"
        @saveChoose="confirmUser"
        @close="chooseDeptVis=false"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import AddressSetingDialog from '@/components/personHome/addressSetingDialog.vue'
// import { listRole } from '@/api/processui/index.ts'
import id2HeadPic from '@/components/ID2HeadPic.vue'
import id2Name from '@/components/ID2Name.vue'
import { CircleCloseFilled } from '@element-plus/icons-vue'
import { ElCol, ElRow, ElRadioGroup, ElRadio, ElDialog, ElIcon, ElSelect, ElOption } from 'element-plus'
import OortImg from '@/components/oort_img.vue'
import { useUserStore } from '@/store/modules/useraPaas'
import { getDeptList, roleList } from '@/api/system/directory'

const store = useUserStore()
const props = defineProps({
  activeChooseData: {
    type: Object,
    default: () => ({})
  }
})

const copyActiveChooseData = ref(props.activeChooseData)
let JJ = ref(false)
let JP = ref(false)
// 设置通知对象-岗位和职位-单选 (0岗位 1职位）
let jobPost = ref(copyActiveChooseData.value?.jobLeaders ? '0' : '1')
let jobMod = ref('1')
let postMod = ref('1')

// watch 监听nodeConfig
watch(() => props.activeChooseData, () => {
  copyActiveChooseData.value = props.activeChooseData
  if (props.activeChooseData.dept?.length) {
    let data = {
      accessToken: store.token,
      dept_id: props.activeChooseData.dept,
      tenant_id: store.tenantId
    }
    getDeptList(data).then(res => {
      tempDept.value = res.data.list || []
    })
  }
  console.log('设置通知对象', props.activeChooseData)
  // 设置通知对象-岗位和职位-单选 (0岗位 1职位）
  jobPost.value = copyActiveChooseData.value?.jobLeaders ? '0' : '1'
  jobMod.value = copyActiveChooseData.value?.postLeaders?.[0] || '1'
  postMod.value = copyActiveChooseData.value?.jobLeaders?.[0] || '1'
})

const chooseUserVis = ref(false)
function addUser() {
  chooseUserVis.value = true
}

const chooseDeptVis = ref(false)
function addDept() {
  chooseDeptVis.value = true
}

const closeAddressDialog = () => {
  chooseUserVis.value = false
  chooseDeptVis.value = false
}

const tempDept = ref([])
const removeDept = (index) => {
  tempDept.value.splice(index, 1)
  copyActiveChooseData.value.dept.splice(index, 1)
  emit('update:activeChooseData', copyActiveChooseData.value)
}

const removeUser = (index) => {
  copyActiveChooseData.value.users.splice(index, 1)
  emit('update:activeChooseData', copyActiveChooseData.value)
}

const emit = defineEmits(['update:activeChooseData'])
function confirmUser(data) {
  tempDept.value = data.dept || []
  copyActiveChooseData.value.users = data.user.map(item => item.user_id)
  copyActiveChooseData.value.dept = data.dept.map(item => item.dept_id)
  chooseUserVis.value = false
  chooseDeptVis.value = false
  emit('update:activeChooseData', copyActiveChooseData.value)
}

function changeApprovalType() {
  // 发起人自己 或 发起人自选
  if (copyActiveChooseData.value.approvalType === 3 || copyActiveChooseData.value.approvalType === 5) {
    copyActiveChooseData.value.users = ['initiator']
    copyActiveChooseData.value.dept = []
    emit('update:activeChooseData', copyActiveChooseData.value)
  }
  // 节点不能又有指定人initiator，又有上级
  if (copyActiveChooseData.value.approvalType === 6) {
    copyActiveChooseData.value.users = undefined
  }
}

const roleListArr = ref([])
function getRoleList() {
  // 角色拿统一用户的
  let data = {
    accessToken: store.token || store.userInfo?.accessToken || '',
    page: 1,
    pagesize: 99,
    tenant_id: store.tenantId
  }
  roleList(data).then(res => {
    roleListArr.value = res.data.list || []
  })
}

onMounted(() => {
  getRoleList()
})

defineExpose({ jobPost, jobMod, postMod })
</script>

<style lang="scss" scoped>
.universeBox {
  padding: 0 20px 6px;
  justify-content: space-between;

  .universeItem {
    border: none;
    color: var(--el-color-primary);
  }

  .sp {
    font-size: 14px;
    color: #646a73;
  }
}

.optBox {
  width: 100%;
  height: 200px;
  overflow: auto;

  .optitem {
    justify-content: space-between;
  }

  .is-selected {
    .optsvg {
      display: flex;
    }
  }

  .optsvg {
    display: none;
  }
}

.prop_item_sp{
  font-size: 14px;
  color: #999;
  flex-shrink: 0;
}

  .prop_item_group {
    flex-wrap: wrap;
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

  .prop_item_user_headpic {
    width: 48px;
  }

:deep(.jobPostBox) {
  width: 100%;
  flex-direction: column;
  gap: 10px;

  .el-radio,.el-radio__label{
    display: flex;
    align-items: center;
    width: 100%;
    margin: 0;
  }
}

.choosePerBox {
  border: 1px solid #e4e5e7;
  border-radius: 6px;

  .he {
    border-radius: 6px;
    padding: 0 16px;
    background: #f5f6f7;
    height: 36px;
    line-height: 36px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .bo{
    border-top: 1px solid #e4e5e7;
    padding-top: 8px;
  }
}
</style>
