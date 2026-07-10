<template>
  <div class="rup_page flexRowAC">
    <div class="rup_inputBox flexRowAC">
      <el-input
        v-model="keyWord"
        class="rup_input"
        placeholder="请输入关键字"
        suffix-icon="Search"
        @input="searchDebounce"
        @change="searchDebounce"
      />
    </div>
    <div v-if="usedList.length > 0" class="rup_useL_selBox flexRowAC">
      <el-checkbox
        v-if="!isSingle"
        v-model="checkedAll"
        :indeterminate="isIndeterminate"
        @change="handleCheckAllChange"
      >
        全选
      </el-checkbox>
      <span class="txColor" @click="clearContact">清空联系人</span>
    </div>
    <div class="rup_useLBox person_list">
      <el-checkbox-group
        v-if="usedList.length > 0"
        v-model="checkList"
        class="rup_useLItem flexRowAC"
        @change="handleCheckedPersonsChange"
      >
        <el-checkbox
          v-for="(item,index) in usedList"
          :key="index"
          class="rup_useLItemIt flexRowAC item_hover"
          :value="item"
          :disabled="item.disabled"
          style="display: flex;align-items: center;"
        >
          <div class="flexRowAC">
            <OortImg :src="item['photo']" class="avatarU" default-type="user" />
            <div class="flexRowAC rup_useLItemItText">
              <span class="rup_useLItemItTextS">{{ item['user_name'] }}</span>
            </div>
          </div>
        </el-checkbox>
      </el-checkbox-group>
      <div v-if="usedList.length ===0" class="selPer flexRowAC">
        暂未选择
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { debounce } from 'lodash-es'
import { usedDel, usedGet } from '@/api/system/directory'
import { useUserStore } from '@/store/modules/useraPaas'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ElCheckbox, ElCheckboxGroup } from 'element-plus'

const store: any = useUserStore()
const props = defineProps({
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
  // 禁止选择的的用户
  disalbedUserList: {
    type: Array,
    default: null
  }
})
const emits = defineEmits(['addPerson'])
let keyWord = ref<any>('')
let checkedAll = ref<any>(false)
let checkList = ref<any>([])
let usedList = ref<any>([])
let tempChooseList = ref<any>([]) // 当前已经选择的，但当前box框没有的
let isIndeterminate = ref<any>(false)
let searchDebounce = ref<any>(null)

// 清空联系人
const clearContact = () => {
  ElMessageBox.confirm('确认清空最近使用', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const params = {
      accessToken: store.token
    }
    usedDel(params).then((res: any) => {
      if (res.code === 200) {
        ElMessage.success('操作成功')
        usedList.value = []
      }
    })
  })
}

// 获取最近联系人
const getRecentyPerson = () => {
  let data = {
    accessToken: store.token,
    keyword: keyWord.value
  }
  usedGet(data).then((res: any) => {
    if (res.code === 200) {
      usedList.value = res.data.list || []
      // 默认选择的回填
      props.userList.forEach((itt: any) => {
        usedList.value.forEach((idd: any) => {
          if (itt.user_id === idd.user_id) {
            // 是否checklist 有个
            let index = checkList.value.findIndex(itd => {
              return idd.user_id === itd.user_id
            })
            index === -1 && checkList.value.push(idd)
          }
        })
      })
      // 禁止选择的人
      let tempD = props.disalbedUserList || []
      tempD.forEach((itt: any) => {
        usedList.value.forEach(idd => {
          if (itt.user_id === idd.user_id) {
            idd.disabled = true
          }
        })
      })
      // 计算出那些是在当前tab选的
      checkList.value.forEach(itm => {
        let index = tempChooseList.value.findIndex(itd => {
          return itm.user_id === itd.user_id
        })
        index !== -1 && tempChooseList.value.splice(index, 1)
      })
    }
  })
}

// 全选
const handleCheckAllChange = (val) => {
  isIndeterminate.value = false
  checkList.value = val ? usedList.value : []
  // 全选 把之前的加进来
  emits('addPerson', val ? [...usedList.value, ...tempChooseList.value] : tempChooseList.value)
}

// 勾选
const handleCheckedPersonsChange = (value) => {
  let checkedCount = value.length
  checkedAll.value = checkedCount === usedList.value.length
  isIndeterminate.value = checkedCount > 0 && checkedCount < usedList.value.length
  if (props.isSingle) {
    if (checkedCount === 0) {
      emits('addPerson', [])
    } else {
      emits('addPerson', [value[checkedCount - 1]])
      checkList.value = [value[checkedCount - 1]]
    }
  } else {
    emits('addPerson', [...value, ...tempChooseList.value])
  }
}
const init = () => {
  searchDebounce.value = debounce(() => getRecentyPerson(), 800)
  tempChooseList.value = JSON.parse(JSON.stringify(props.userList))
  getRecentyPerson()
}

onMounted(() => {
  init()
})

</script>

<style lang="scss" scoped>
.rup_page {
  width: 100%;
  height: 100%;
  flex-direction: column;

  .rup_inputBox {
    width: 100%;
    height: 48px;
    margin-top: 8px;
    justify-content: center;

    .rup_input {
      width: 60%;
    }
  }
}

// 全选
.rup_useL_selBox {
  height: 48px;
  width: 100%;
  justify-content: space-between;
  padding-left: 20px;
  padding-right: 20px;

  .txColor {
    cursor: pointer;
    color: var(--el-color-primary);
  }
}

// 人员
.rup_useLBox {
  padding-left: 20px;
  padding-right: 20px;

  .rup_useLItem {
    flex-wrap: wrap;
    row-gap: 20px;
    padding-left: 20px;
    align-content: flex-start;
  }

  .rup_useLItemIt {
    height: 56px;
    width: 40%;
    padding-left: 4px;
    margin-right: 48px;

    &:hover {
      background-color: var(--el-color-primary-hb2);
    }
  }

  .rup_useLItemItText {
    margin-left: 16px;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .rup_useLItemItTextS {
    font-size: 14px;
    color: #333;
  }

  .rup_useLItemItTextS.fs12 {
    color: #999;
    font-size: 12px;
  }
}

// 暂未选择
.selPer {
  width: 100%;
  height: 100%;
  justify-content: center;
  font-size: 16px;
  color: #999;
}

.person_list {
  width: 100%;
  margin: 0 auto;
  height: calc(100% - 118px);
  overflow: auto;
}

.item_hover:hover {
  color: #fff !important;

  span {
    color: #fff !important;
  }
}

  .avatarU {
    width: 32px;
    height: 32px;
    border-radius: 50%;
  }
</style>
