/**
Created by  lanjian   on 2021/10/16  10:28
Copyright apaas-front-team. All rights reserved.
*/
<template>
  <div class="dept_page w-full h-full flex flex-col box-border">
    <div v-if="tableData.length > 0" class="w-full h-12 box-border px-5 flex flex-row justify-between items-center">
      <div>
        <el-checkbox
          v-if="!isSingle"
          v-model="checkedAll"
          :indeterminate="isIndeterminate"
          @change="handleCheckAllChange"
        >
          全选
        </el-checkbox>
      </div>
    </div>
    <div class="w-full person_list box-border px-5">
      <el-checkbox-group
        v-if="tableData.length > 0"
        v-model="checkList"
        class="w-full h-full flex flex-wrap gap-y-6 box-border pl-6"
        @change="handleCheckedPersonsChange"
      >
        <el-checkbox
          v-for="(item,index) in tableData"
          :key="index"
          class="w-2/5 h-14 mr-12 hover:bg-oortBlue hover:bg-opacity-10 pl-1 item_hover"
          :label="item"
          :disabled="item.disabled"
          style="display: flex;align-items: center"
        >
          <div v-if="!item.dept_id" class="w-full h-full  flex flex-row items-center">
            <OortImg v-if="item.user_id" :src="item?.photo" class="w-12 h-12" default-type="user" />
            <div class="flex flex-col ml-4">
              <span class="text-color333 fontsize14">{{ item.user_name }}</span>
              <!-- <span class="text-color999 fontsize12">{{ item.oort_depname }}</span>
              <span class="text-color999 fontsize12">{{ item.oort_postname }}</span> -->
            </div>
          </div>
          <div v-else class="w-full h-full  flex flex-row items-center">
            <OortImg v-if="item.dept_id" :src="item?.dept_photo" class="w-12 h-12" default-type="dept" />
            <div class="flex flex-col ml-4">
              <span class="text-color333 fontsize14">{{ item.dept_name }}</span>
              <!-- <span class="text-color999 fontsize12">{{ item.oort_depname }}</span>
              <span class="text-color999 fontsize12">{{ item.oort_postname }}</span> -->
            </div>
          </div>
        </el-checkbox>
      </el-checkbox-group>
      <div v-if="tableData.length ===0" class="w-full h-full flex items-center justify-center">
        <span style="font-size: 16px;color: #999">暂无数据</span>
      </div>
    </div>
  </div>
</template>

<script>
import OortImg from '@/components/oort_img.vue'
import { ElCheckbox, ElCheckboxGroup } from 'element-plus'

export default {
  name: 'ScopePersonOrDept',
  components: {
    ElCheckbox,
    ElCheckboxGroup,
    OortImg
  },
  props: {
    // 是否单选
    isSingle: {
      type: Boolean,
      default: false
    },
    // 已选的人员list
    userList: {
      type: Array,
      default: null
    },
    deptList: {
      type: Array,
      default: null
    },
    // 禁止选择的的用户
    disalbedUserList: {
      type: Array,
      default: null
    },
    // 选择范围的人员
    scopeUserList: {
      type: Array,
      default: null
    },
    // 选择范围的部门
    scopeDeptList: {
      type: Array,
      default: null
    }
  },
  data() {
    return {
      keyWord: '',
      checkedAll: false,
      checkList: [],
      tableData: [],
      isIndeterminate: false,
      tempChooseList: [] // 当前已经选择的，但当前box框没有的
    }
  },
  watch: {
    scopeUserList() {
      this.initScopeSelect()
    }
  },
  mounted() {
    this.initData()
  },
  methods: {
    initData() {
      this.initTempChoose()
      this.initScopeSelect()
    },
    initTempChoose() {
      this.tempChooseList = JSON.parse(JSON.stringify(this.userList))
      this.tempChooseDeptList = JSON.parse(JSON.stringify(this.deptList))
    },
    initScopeSelect() {
      this.tableData = [...(this.scopeUserList || []), ...(this.scopeDeptList || [])]
      // 默认选择的回填
      this.userList.forEach(itt => {
        this.tableData.forEach(idd => {
          if (itt.user_id === idd.user_id) {
            // 是否checklist 有个
            let index = this.checkList.findIndex(itd => {
              return idd.user_id === itd.user_id
            })
            index === -1 && this.checkList.push(idd)
          }
        })
      })
      // 禁止选择的人
      let tempD = this.disalbedUserList || []
      tempD.forEach(itt => {
        this.tableData.forEach(idd => {
          if (itt.user_id === idd.user_id) {
            idd.disabled = true
          }
        })
      })
      // 计算出那些是在当前tab选的
      this.checkList.forEach(itm => {
        let index = this.tempChooseList.findIndex(itd => {
          return itm.user_id === itd.user_id
        })
        index !== -1 && this.tempChooseList.splice(index, 1)
      })
    },
    handleCheckAllChange(val) {
      this.isIndeterminate = false
      this.checkList = val ? this.tableData : []
      this.isIndeterminate = false
      // 全选 把之前的加进来
      // 区分是部门还是人员tableData
      let valueUser = this.tableData.filter(itm => itm.user_id)
      let valueDept = this.tableData.filter(itm => itm.dept_id)
      this.$emit('addPerson', { user: [...valueUser, ...this.tempChooseList], dept: [...valueDept, ...this.tempChooseDeptList] })
    },
    handleCheckedPersonsChange(value) {
      let checkedCount = value.length
      this.checkedAll = checkedCount === this.tableData.length
      this.isIndeterminate = checkedCount > 0 && checkedCount < this.tableData.length
      if (this.isSingle) {
        if (checkedCount === 0) {
          this.$emit('addPerson', { user: [], dept: [] })
        } else {
          // 区分是部门还是人员
          if (value[checkedCount - 1].user_id) {
            this.$emit('addPerson', { user: [value[checkedCount - 1]] })
          } else {
            this.$emit('addPerson', { dept: [value[checkedCount - 1]] })
          }
          this.checkList = [value[checkedCount - 1]]
        }
      } else {
        // 区分是部门还是人员
        let valueUser = value.filter(itm => itm.user_id)
        let valueDept = value.filter(itm => itm.dept_id)
        this.$emit('addPerson', { user: [...valueUser, ...this.tempChooseList], dept: [...valueDept, ...this.tempChooseDeptList] })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.person_list {
  width: 100%;
  margin: 0 auto;
  height: calc(100% - 118px);
  overflow: auto;
}

.item_hover:hover {
  color: var(--el-color-primary) !important;

  span {
    color: var(--el-color-primary) !important;
  }
}

.dept_page {
  overflow-y-auto {
    overflow-y: auto;
  }

  .justify-start {
    justify-content: flex-start;
  }

  .items-center {
    align-items: center;
  }

  .h-8 {
    height: 32px;
  }

  .w50 {
    width: 50%;
  }

  .w33 {
    width: 33%;
  }

  .flex-wrap {
    flex-wrap: wrap;
  }

  .h-14 {
    height: 56px;
  }

  .pt-1 {
    padding-top: 4px;
  }

  .ml-4 {
    margin-left: 16px;
  }

  .pl-1 {
    padding-left: 4px;
  }

  .w-12 {
    width: 48px;
  }

  .h-12 {
    height: 48px;
  }

  .rounded-full {
    border-radius: 50%;
  }
}

.w-full {
  width: 100%;
}

.h-full {
  height: 100%;
}

.flex {
  display: flex;
}

.flex-row {
  flex-direction: row;
}

.flex-col {
  flex-direction: column;
}

.hoverIt {
  &:hover {
    color: var(--el-color-primary);
    background-color: var(--el-color-primary);
  }
}

.bg-oortBlue {
  background-color: var(--el-color-primary);
}

.w-20 {
  width: 80px;
}

.rounded-3xl {
  border-radius: 24px;
}

.justify-center {
  justify-content: center;
}

.text-white {
  color: #fff;
}

.border-color999 {
  border: 1px solid #999;
}

.w-40 {
  width: 160px;
}
</style>
