<template>
  <div>
    <div class="chooose_item chooose_item_group">
      <div v-for="(item,index) in user" :key="index" class="chooose_item_user" :class="{'chooose_item_user_hover': options.disabled}">
        <oort-img class="chooose_item_user_headpic" :src="item.photo" default-type="user" />
        <div class="chooose_item_user_name">
          <span>{{ item.user_name }}</span>
        </div>
        <el-icon v-if="!options.disabled" class="chooose_item_user_remove" color="red" size="20" @click="removeUser(index)">
          <CircleCloseFilled />
        </el-icon>
      </div>

      <div v-for="(item,index) in dept" :key="index" class="chooose_item_user" :class="{'chooose_item_user_hover': options.disabled}">
        <oort-img class="chooose_item_user_headpic" :src="item.dept_photo" default-type="dept" />
        <div class="chooose_item_user_name">
          <span>{{ item.dept_name }}</span>
        </div>
        <el-icon v-if="!options.disabled" class="chooose_item_user_remove" color="red" size="20" @click="removeDept(index)">
          <CircleCloseFilled />
        </el-icon>
      </div>
      <div v-if="!options.disabled" class="chooose_item chooose_item_add_litte" :class="{'disabled_cursor': options.disabled}">
        <img src="@/assets/img/processui/flownode/add.png" @click="addDeptOrUser" />
      </div>
    </div>

    <el-dialog v-model="chooseUserVis" :title="title" width="50%">
      <!-- 如果是自定义范围 -->
      <address-seting-dialog
        :select-scope="options.addressSetting?options.addressSetting.selectScope:1"
        :scope-user-list="scopeUserList"
        :scope-dept-list="scopeDeptList"
        :user-list="user.map(itm => itm.user_id)"
        :dept-list="dept.map(itm => itm.dept_id)"
        :mode="options.addressSetting?options.addressSetting.selectMod:1"
        :is-single="!isMulti"
        @saveChoose="confirmUser"
        @close="chooseUserVis=false"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AddressSetingDialog from '@/components/personHome/addressSetingDialog.vue'
import { ElDialog, ElIcon } from 'element-plus'
import { useUserStore } from '@/store/modules/useraPaas'
import { getUserList, getDeptList } from '@/api/system/directory'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  isMulti: {
    type: Boolean,
    default: false
  },
  options: {
    type: Object,
    default: () => ({
      selectMod: 1, // 选择模式 1, 部门和人都可以选， 2 只选部门  3 只选人
      selectScope: 1, // 选择范围  1,全部  2 本部门 3 自定义
      defaultValueType: 1, // 1，无， 2 固定值， 3 提交人， 4 提交部门， 5 创建人， 6 创建人部门
      scopeValue: '', // 自定义范围
      defaultValue: '' // 固定值
    })
  }
})

const title = ref('选择人员')
// 兼容老数据
if (props.options.addressSetting) {
  title.value = props.options.addressSetting.selectMod === 3 ? '选择人员' : props.options.addressSetting.selectMod === 2 ? '选择部门' : '选择人员或者组织'
}

const chooseUserVis = ref(false)
function addDeptOrUser() {
  if (!props.options.disabled) {
    chooseUserVis.value = true
  }
}

function removeUser(index) {
  user.value.splice(index, 1)
  refreshList({ user: user.value, dept: dept.value })
}

function removeDept(index) {
  dept.value.splice(index, 1)
  refreshList({ user: user.value, dept: dept.value })
}

const refreshList = (data) => {
  user.value = data.user
  dept.value = data.dept
  let userT = data.user.map(itm => 'user-' + itm.user_id)
  let deptT = data.dept.map(itm => 'dept-' + itm.dept_id)
  emits('update:modelValue', [...userT, ...deptT].join(','))
  emits('change', [...userT, ...deptT].join(','))
  chooseUserVis.value = false
}

const emits = defineEmits(['update:modelValue', 'change'])
function confirmUser(data) {
  refreshList(data)
}

const user = ref([])
const dept = ref([])
const store = useUserStore()
const getUserOrDept = async(tUser, tDept) => {
  if (tDept.length > 0) {
    const params = {
      dept_id: tDept,
      accessToken: store.token,
      tenant_id: store.tenantId
    }
    let res = await getDeptList(params)
    if (res.code === 200) {
      if (res.data.list.length > 0) {
        dept.value = res.data.list || []
      }
    }
  }
  if (tUser.length > 0) {
    const params = {
      user_id: tUser,
      accessToken: store.token,
      tenant_id: store.tenantId
    }
    let res = await getUserList(params)
    if (res.code === 200) {
      if (res.data.list.length > 0) {
        user.value = res.data.list || []
      }
    }
  }
}

const scopeUserList = ref([])
const scopeDeptList = ref([])

onMounted(() => {
  if (props.modelValue && props.modelValue.split) {
    let tempList = props.modelValue.split(',')
    let tempUser = []
    let tempDept = []
    if (tempList.length > 0) {
      tempList.forEach(item => {
        if (item.startsWith('user-') !== -1) {
          tempUser.push(item.replace('user-', ''))
        }
        if (item.startsWith('dept-') !== -1) {
          tempDept.push(item.replace('dept-', ''))
        }
      })
      getUserOrDept(tempUser, tempDept)
    }
  }
  // 如果是自定义范围
  if (props.options.addressSetting && props.options.addressSetting.selectScope === 3) {
    let tempList = props.options.addressSetting.scopeValue.split(',')
    if (tempList.length > 0) {
      tempList.forEach(item => {
        if (item.startsWith('user-') !== -1) {
          scopeUserList.value.push(item.replace('user-', ''))
        }
        if (item.startsWith('dept-') !== -1) {
          scopeDeptList.value.push(item.replace('dept-', ''))
        }
      })
    }
  }
  // TODO 自定义范围为本部门,怎么获取本部门的人员
  // if (props.options.addressSetting && props.options.addressSetting.selectScope === 2) {
  // }
})

</script>

<style lang="scss" scoped>

  .chooose_item_group {
    flex-wrap: wrap;
  }

  .chooose_item {
    display: flex;
    align-items: center;
    min-height: 24px;
    margin: 8px;

    &_add_litte {
      display: flex;
      align-items: center;
      justify-content: center;
      img {
        width: 24px;
        height: 24px;
        cursor: pointer;
      }
    }

    &_user {
      background: #FFFFFF;
      width: 160px;
      min-width: 160px;
      height: 32px;
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
        width: 24px;
        height: 24px;
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
      .chooose_item_user_remove {
        display: flex!important;
      }
    }
  }

  .chooose_item_user_hover {
    box-shadow: 0px 0px 6px 0px rgba(0,0,0,0.15);
    border-radius: 2px;
  }

  .disabled_cursor {
    cursor: not-allowed;
  }

</style>
