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
  // 已选的人员list
  userList: {
    type: Array,
    default: null
  },
  // 已选的部门
  deptList: {
    type: Array,
    default: null
  },
  // 选择模式 1, 部门和人都可以选， 2 只选  3 只选人
  mode: {
    type: Number,
    default: 1
  },
  // 是否单选
  isSingle: {
    type: Boolean,
    default: false
  },
  // 禁止选择的的部门
  disalbedDeptList: {
    type: Array,
    default: null
  },
  // 禁止选择的的用户
  disalbedUserList: {
    type: Array,
    default: null
  },
  // 是否登录人当前部门
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
const currentDeptCode = ref<any>('') // 当前部门code
const tempChooseList = ref<any>(JSON.parse(JSON.stringify(props.userList))) // 当前已经选择用户的，但当前box框没有的
const tempDeptChooseList = ref<any>(JSON.parse(JSON.stringify(props.deptList))) // 当前已经选择部门的，但当前box框没有的
// 配置选项
const defaultProps = {
  children: 'son_dept',
  label: 'dept_name',
  isLeaf: (data) => { return (!!data.user_id) }
}

// 监听userList he deptList 给tempChooseList 和 tempDeptChooseList 赋值
onMounted(() => {
  tempChooseList.value = JSON.parse(JSON.stringify(props.userList))
  tempDeptChooseList.value = JSON.parse(JSON.stringify(props.deptList))
})

// 部门树搜索
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
        // 列表模式
        tableData.value = [...(res.data.list || [])]
      }
      if (props.mode === 2) {
        tableData.value = [...(res.data.list || [])]
      }
      if (props.mode === 3) {
        tableData.value = []
      }
      // 默认选择的回填
      resetCheckList(tableData.value)
    }
  })
}

// 默认选择的回填
const resetCheckList = (arr: any) => {
  // 用户
  props.userList.forEach((itt: any) => {
    arr.forEach((idd: any) => {
      if (itt.user_id && itt.user_id === idd.user_id) {
        // 是否checklist 有个
        let index = checkList.value.findIndex((itd: any) => {
          return idd.user_id === itd.user_id
        })
        index === -1 && checkList.value.push(idd)
      }
    })
  })
  // 部门
  props.deptList.forEach((itt: any) => {
    arr.forEach(idd => {
      if (itt.dept_code && itt.dept_code === idd.dept_code) {
        // 是否checklist 有个
        let index = checkList.value.findIndex(itd => {
          return idd.dept_code === itd.dept_code
        })
        index === -1 && checkList.value.push(idd)
      }
    })
  })
  // 禁止选择的人
  let tempD = props.disalbedUserList || []
  tempD.forEach((itt: any) => {
    tableData.value.forEach((idd: any) => {
      if (itt.user_id === idd.user_id) {
        idd.disabled = true
      }
    })
  })
  // 禁止选择的部门
  let tempDe = props.disalbedDeptList || []
  tempDe.forEach((itt: any) => {
    tableData.value.forEach((idd: any) => {
      if (itt.dept_code === idd.dept_code) {
        idd.disabled = true
      }
    })
  })
}

// 部门树选中
const deptClick = (data) => {
  showDeptClickData(data.son_dept || [], data.users || [])
}

// 获取当前部门子部门和用户
const showDeptClickData = (dept, users) => {
  // 判断模式 1 ，部门和人都可以选， 2，只可以选部门  3，只可以选人
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
  // 默认选择的回填
  resetCheckList(tableData.value)
  // 设置全选按钮的状态
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

// 全选
const handleCheckAllChange = (val) => {
  isIndeterminate.value = false
  checkList.value = val ? tableData.value : []
  // 将用户和部门区分开来
  if (val) {
    emitData2Parent(tableData.value)
  } else {
    emitData2Parent([])
  }
}

// 单选
const handleCheckedPersonsChange = (value) => {
  let checkedCount = value.length
  checkedAll.value = checkedCount === tableData.value.length
  isIndeterminate.value = checkedCount > 0 && checkedCount < tableData.value.length
  // 是否单选
  if (props.isSingle) {
    if (checkedCount === 0) {
      tempChooseList.value = []
      tempDeptChooseList.value = []
      // 将用户和部门区分开来
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

// 将用户和部门区分开来
const emitData2Parent = (value) => {
  if (value) {
    let user: any = []
    let dept: any = []
    // 先把所有的去除，在根据选中的添加进来
    // 第一步
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
    // 第二步
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
