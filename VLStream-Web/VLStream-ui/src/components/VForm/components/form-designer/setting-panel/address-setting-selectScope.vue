<template>
  <div>
    <div class="chooose_item chooose_item_group">
      <div v-for="(item,index) in user" :key="index" class="chooose_item_user">
        <oort-img class="chooose_item_user_headpic" :src="item.photo" default-type="user" />
        <div class="chooose_item_user_name">
          <span>{{ item.user_name }}</span>
        </div>
        <el-icon class="chooose_item_user_remove" color="red" size="20" @click="removeUser(index)">
          <CircleCloseFilled />
        </el-icon>
      </div>

      <div v-for="(item,index) in dept" :key="index" class="chooose_item_user">
        <oort-img class="chooose_item_user_headpic" :src="item.dept_photo" default-type="dept" />
        <div class="chooose_item_user_name">
          <span>{{ item.dept_name }}</span>
        </div>
        <el-icon class="chooose_item_user_remove" color="red" size="20" @click="removeDept(index)">
          <CircleCloseFilled />
        </el-icon>
      </div>
      <div class="chooose_item chooose_item_add_litte">
        <img src="@/assets/img/processui/flownode/add.png" @click="addDeptOrUser" />
      </div>
    </div>

    <el-dialog v-model="chooseUserVis" :title="title" width="50%">
      <address-seting-dialog
        :user-list="user.map(itm => itm.user_id)"
        :dept-list="dept.map(itm => itm.dept_id)"
        :mode="selectMod"
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
  selectMod: {
    type: Number,
    default: 1
  }
})

const title = ref('选择人员')
title.value = props.selectMod === 3 ? '选择人员' : props.selectMod === 2 ? '选择部门' : '选择人员或者组织'

const chooseUserVis = ref(false)
function addDeptOrUser() {
  chooseUserVis.value = true
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
