<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <div class="deptUser_page flexRowAC">
    <div class="deptUser_item">
      <div class="tree_search_content">
        <el-input
          v-model="keyWord"
          placeholder="请输入关键字"
          suffix-icon="Search"
          @change="getSearchDept"
        />
      </div>
      <el-tree
        ref="tree"
        class="dept_e_tree"
        :data="treeData"
        :default-expanded-keys="['', '99']"
        :expand-on-click-node="false"
        node-key="dept_code"
        :props="defaultProps"
        @node-click="deptClick"
      >
        <template #default="{ node, data }">
          <span class="custom-tree-node">
            <OortImg v-if="data.user_id" :src="data?.photo" class="avatarPho" default-type="user" />
            <OortImg v-else :src="data?.dept_photo" class="avatarPho" default-type="dept" />
            <el-tooltip :open-delay="500" class="item" effect="light" :content="node.label" placement="top">
              <span :class="{'activeDept': data.dept_id === currentDeptCode}">{{
                node.label
              }}</span>
            </el-tooltip>
          </span>
        </template>
      </el-tree>
    </div>
    <div class="deptUser_item right personDiv flexRowAC">
      <div v-if="tableData.length > 0" class="deptUser_item_sel flexRowAC">
        <el-checkbox
          v-if="!isSingle"
          v-model="checkedAll"
          :indeterminate="isIndeterminate"
          @change="handleCheckAllChange"
        >
          全选
        </el-checkbox>
      </div>
      <div class="person_list">
        <el-checkbox-group
          v-model="checkList"
          class="person_listG flexRowAC"
          @change="handleCheckedPersonsChange"
        >
          <el-checkbox
            v-for="(item,index) in tableData"
            :key="index"
            :disabled="item.disabled"
            class="person_listG_che hoverIt item_hover flexRowAC"
            :value="item"
          >
            <div v-if="item.dept_code" class="oort_uuid  flexRowAC">
              <OortImg :src="item['dept_photo']" class="avatarU" default-type="dept" />
              <div class="oort_uuid_name flexRowAC">
                <span class="text-color333 fontsize14">{{ item['dept_name'] }}</span>
                <!-- <span class="text-color999 fontsize12">{{ item['oort_depname'] }}</span>
                <span class="text-color999 fontsize12">{{ item['oort_zhiji'] }}</span> -->
              </div>
            </div>
            <div v-else class="avatarUBox flexRowAC">
              <OortImg :src="item['photo']" class="avatarU" default-type="user" />
              <div class="avatarU_name">
                {{ item['user_name'] }}
              </div>
            </div>
          </el-checkbox>
        </el-checkbox-group>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { debounce } from 'lodash-es'
// eslint-disable-next-line vue/no-dupe-keys
import { getDeptUser, usedSet } from '@/api/system/directory'
import { useUserStore } from '@/store/modules/useraPaas'
import { ElCheckbox, ElCheckboxGroup, ElTooltip, ElTree, ElInput } from 'element-plus'
import OortImg from '@/components/oort_img.vue'

const store = useUserStore()

const props = defineProps({
  // already list
  userList: {
    type: Array,
    default: null
  },
  // already department
  deptList: {
    type: Array,
    default: null
  },
  // 1, department and , 2 only 3 only
  mode: {
    type: Number,
    default: 1
  },
  // whether
  isSingle: {
    type: Boolean,
    default: false
  },
  // department
  disalbedDeptList: {
    type: Array,
    default: null
  },
  // user
  disalbedUserList: {
    type: Array,
    default: null
  },
  // whether current department
  isLocalDept: {
    type: Boolean,
    default: false
  }
})
const emits = defineEmits(['addPerson'])
let keyWord = ref('')
let checkedAll = ref(false)
let isIndeterminate = ref(false)
let checkList = ref<any>([])
let treeData = ref<any>([])
let tableData = ref([])
const currentDeptCode = ref<any>('') // current departmentcode
const tempChooseList = ref<any>(JSON.parse(JSON.stringify(props.userList))) // current already user , current box
const tempDeptChooseList = ref<any>(JSON.parse(JSON.stringify(props.deptList))) // current already department , current box
// configuration item
const defaultProps = {
  children: 'son_dept',
  label: 'dept_name',
  isLeaf: (data) => { return (!!data.user_id) }
}

// userList he deptList tempChooseList and tempDeptChooseList value
onMounted(() => {
  tempChooseList.value = JSON.parse(JSON.stringify(props.userList))
  tempDeptChooseList.value = JSON.parse(JSON.stringify(props.deptList))
})

// department
const getSearchDept = () => {
  const params = {}
  params.accessToken = store.token
  params.tenant_id = store.tenantId || window.sessionStorage.getItem('tenantId')
  if (keyWord.value) {
    params['keyword'] = keyWord.value
  }
  if (props.isLocalDept) {
    if (store.userInfo.dept_list && store.userInfo.dept_list.length > 0) {
      params['dept_id'] = store.userInfo.dept_list[0].dept_id
    }
  }
  getDeptUser(params).then(res => {
    if (res.code === 200) {
      // treeData.value = res.data.list || []
      treeData.value = [{ dept_name: '全部', dept_id: '', dept_code: '', son_dept: res.data.list || [] }]
      tableData.value = []
      if (props.mode === 1) {
        //
        tableData.value = [...(res.data.list || [])]
      }
      if (props.mode === 2) {
        tableData.value = [...(res.data.list || [])]
      }
      if (props.mode === 3) {
        tableData.value = []
      }
      //
      resetCheckList(tableData.value)
    }
  })
}

//
const resetCheckList = (arr: any) => {
  // user
  props.userList.forEach((itt: any) => {
    arr.forEach((idd: any) => {
      if (itt.user_id && itt.user_id === idd.user_id) {
        // whether checklist
        let index = checkList.value.findIndex((itd: any) => {
          return idd.user_id === itd.user_id
        })
        index === -1 && checkList.value.push(idd)
      }
    })
  })
  // department
  props.deptList.forEach((itt: any) => {
    arr.forEach(idd => {
      if (itt.dept_code && itt.dept_code === idd.dept_code) {
        // whether checklist
        let index = checkList.value.findIndex(itd => {
          return idd.dept_code === itd.dept_code
        })
        index === -1 && checkList.value.push(idd)
      }
    })
  })
  //
  let tempD = props.disalbedUserList || []
  tempD.forEach((itt: any) => {
    tableData.value.forEach((idd: any) => {
      if (itt.user_id === idd.user_id) {
        idd.disabled = true
      }
    })
  })
  // department
  let tempDe = props.disalbedDeptList || []
  tempDe.forEach((itt: any) => {
    tableData.value.forEach((idd: any) => {
      if (itt.dept_code === idd.dept_code) {
        idd.disabled = true
      }
    })
  })
}

// department in
const deptClick = (data) => {
  showDeptClickData(data.son_dept || [], data.users || [])
}

// Get current department sub department and user
const showDeptClickData = (dept, users) => {
  // Check 1 , department and , 2, only department 3, only
  if (props.mode === 1) {
    let tempUserArr = users
    tableData.value = [...(dept || []), ...tempUserArr]
  }
  if (props.mode === 2) {
    tableData.value = [...(dept || [])]
  }
  if (props.mode === 3) {
    let tempUserArr = users
    tableData.value = tempUserArr
  }
  checkList.value = []
  //
  resetCheckList(tableData.value)
  // Set full button
  if (checkList.value.length === tableData.value.length) {
    checkedAll.value = true
  }
  if (checkList.value.length === 0) {
    checkedAll.value = false
  }
  if (checkList.value.length !== 0 && tableData.value.length !== 0 && checkList.value.length !== tableData.value.length) {
    checkedAll.value = false
    isIndeterminate.value = true
  }
}

// full
const handleCheckAllChange = (val) => {
  isIndeterminate.value = false
  checkList.value = val ? tableData.value : []
  // user and department
  if (val) {
    emitData2Parent(tableData.value)
  } else {
    emitData2Parent([])
  }
}

//
const handleCheckedPersonsChange = (value) => {
  let checkedCount = value.length
  checkedAll.value = checkedCount === tableData.value.length
  isIndeterminate.value = checkedCount > 0 && checkedCount < tableData.value.length
  // whether
  if (props.isSingle) {
    if (checkedCount === 0) {
      tempChooseList.value = []
      tempDeptChooseList.value = []
      // user and department
      emitData2Parent([])
    } else {
      tempChooseList.value = []
      tempDeptChooseList.value = []
      checkList.value = [value[checkedCount - 1]]
      emitData2Parent([value[checkedCount - 1]])
    }
  } else {
    emitData2Parent(value)
  }
  if (value.length > 0 && value[value.length - 1].user_id) {
    usedSetPerson(value[value.length - 1])
  }
}

const usedSetPerson = (data) => {
  const params = {
    accessToken: store.token,
    user_id: data.user_id
  }
  usedSet(params)
}

// user and department
const emitData2Parent = (value) => {
  if (value) {
    let user: any = []
    let dept: any = []
    // all , in in
    //
    tableData.value.forEach((itm: any) => {
      if (itm.dept_code) {
        let index = tempDeptChooseList.value.findIndex(itd => {
          return itm.dept_code === itd.dept_code
        })
        index !== -1 && tempDeptChooseList.value.splice(index, 1)
      } else {
        let index = tempChooseList.value.findIndex(itd => {
          return itm.user_id === itd.user_id
        })
        index !== -1 && tempChooseList.value.splice(index, 1)
      }
    })
    //
    value.forEach((item: any) => {
      if (item.user_id) {
        let index = tempChooseList.value.findIndex(itd => {
          return item.user_id === itd.user_id
        })
        index === -1 && user.push(item)
      } else {
        let index = tempDeptChooseList.value.findIndex(itd => {
          return item.dept_code === itd.dept_code
        })
        index === -1 && dept.push(item)
      }
    })
    emits('addPerson', { user: [...user, ...tempChooseList.value], dept: [...dept, ...tempDeptChooseList.value] })
  } else {
    tableData.value.forEach((item: any) => {
      if (item.dept_code) {
        let index = tempDeptChooseList.value.findIndex(itd => {
          return item.dept_code === itd.dept_code
        })
        index !== -1 && tempDeptChooseList.value.splice(index, 1)
      } else {
        let index = tempChooseList.value.findIndex(itd => {
          return item.user_id === itd.user_id
        })
        index !== -1 && tempChooseList.value.splice(index, 1)
      }
    })
    emits('addPerson', { user: tempChooseList.value, dept: tempDeptChooseList.value })
  }
}

onMounted(() => {
  getSearchDept()
  debounce(getSearchDept, 800)
})
</script>

<style lang="scss" scoped>
.tree_search_content {
  width: 320px;
  padding-top: 10px;
}

.deptUser_page {
  width: 100%;
  height: 100%;
  align-items: flex-start;

  .deptUser_item {
    flex: 1;
    height: 100%;
    overflow-y: auto;
  }

  .deptUser_item_sel {
    padding-left: 16px;
    height: 32px;
    width: 100%;
    justify-content: flex-start;
  }

  .deptUser_item.right {
    flex-direction: column;
    justify-content: space-between;
  }

  .avatarU {
    width: 32px;
    height: 32px;
    border-radius: 50%;
  }

  .avatarU_name {
    font-size: 14px;
    color: #333333;
    margin-left: 12px;
  }

  .avatarUBox {
    width: 100%;
    height: 100%;
  }
}

.custom-tree-node {
  display: flex;
  align-items: center;
}

.personDiv {
  border-left: 0.5px solid #99999950;
}

.person_list {
  width: 100%;
  margin: 0 auto;
  height: calc(100% - 20px);
  overflow: auto;

  .person_listG {
    width: 100%;
    height: 100%;
    flex-wrap: wrap;
    align-content: flex-start;
  }

  .person_listG_che {
    width: 100%;
    padding-top: 4px;
    padding-left: 4px;
    height: 56px;
  }
}

.activeDept {
  color: var(--oort-primary);
  font-weight: bold;
}

.item_hover:hover {
  color: var(--el-color-primary) !important;

  span {
    color: var(--el-color-primary) !important;
  }
}
.oort_uuid {
  width: 100%;
  height: 100%;
}

.oort_uuid_name {
  flex-direction: column;
  margin-left: 16px
}

.hoverIt {
  &:hover {
    color: var(--el-color-primary);
    background-color: var(--el-menu-hover-bg-color);
  }
}

.avatarCus {
  width: 16px;
  margin: 0 4px;
}

.avatarPho {
  width: 16px;
  height: 16px;
  margin: 0 4px;
  border-radius: 50%
}

.dept_e_tree {
  height: calc(100% - 20px - 36px);
   margin: 10px
}
</style>
