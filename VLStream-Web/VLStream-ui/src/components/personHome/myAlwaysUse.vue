/**
Created by  lanjian   on 2021/10/16  9:38
Copyright apaas-front-team. All rights reserved.
*/
<template>
  <div class="dept_page w-full h-full flex flex-row">
    <div class="w50 h-full overflow-y-auto">
      <div
        class="w-40 h-8 flex flex-row justify-center items-center rounded-3xl border border-solid border-opacity-30 border-color999"
        style="margin: 4px auto;cursor: pointer"
      >
        <div class="w-20 h-8 flex justify-center items-center rounded-3xl" :class="{'bg-oortBlue':publicP === 1}" @click="changeTagStatus(1)">
          <span class="text-base" :class="{'text-white':publicP===1}">公开</span>
        </div>
        <div class="w-20 h-8 flex justify-center items-center rounded-3xl" :class="{'bg-oortBlue':publicP === 2}" @click="changeTagStatus(2)">
          <span class="text-base" :class="{'text-white':publicP===2}">个人</span>
        </div>
      </div>
      <div class="w-full flex flex-col dept_list">
        <div v-for="(item,index) in tagList" :key="index" class="w-full h-8 flex justify-start items-center cursor-pointer" :class="{'bg-hoverBg': active === index}" @click="chooseTag(item,index)">
          <span class="text-base text-color333 ml-2">{{ item.name }}</span>
        </div>
        <div v-if="tagList.length ===0" class="w-full h-full flex items-center justify-center">
          <span style="font-size: 16px;color: #999">暂无数据</span>
        </div>
      </div>
    </div>
    <div class="w50 h-full overflow-y-auto always_div">
      <!--      <div class="w-full h-8 flex flex-row justify-end items-end">-->
      <!--        <el-input-->
      <!--          v-model="keyWord"-->
      <!--          class="w-1/3"-->
      <!--          size="mini"-->
      <!--          placeholder="请输入关键字"-->
      <!--        >-->
      <!--          <i slot="suffix" class="el-input__icon el-icon-search" />-->
      <!--        </el-input>-->
      <!--      </div>-->
      <div v-if="personList.length > 0" class="w-full h-8 flex flex-row justify-start" style="margin-left: 16px">
        <el-checkbox
          v-if="!isSingle"
          v-model="checkedAll"
          :indeterminate="isIndeterminate"
          @change="handleCheckAllChange"
        >
          全部
        </el-checkbox>
      </div>
      <div class="person_list">
        <el-checkbox-group
          v-if="personList.length > 0"
          v-model="checkList"
          class="w-full h-full flex flex-wrap justify-between content-start"
          @change="handleCheckedPersonsChange"
        >
          <el-checkbox
            v-for="(item,index) in personList"
            :key="index"
            class="hoverIt w-full h-14 ml-4 hover:bg-oortBlue hover:bg-opacity-10 pl-1 pt-1 item_hover"
            :label="item"
            :disabled="item.disabled"
            style="display: flex;align-items: center"
          >
            <div class="w-full h-full  flex flex-row items-center">
              <OortImg :src="item['photo']" class="w-12 h-12 rounded-full" default-type="user" />
              <div class="flex flex-col ml-4">
                <span class="text-color333 fontsize14">{{ item.user_name }}</span>
              </div>
            </div>
          </el-checkbox>
        </el-checkbox-group>
        <div v-if="personList.length ===0" class="w-full h-full flex items-center justify-center">
          <span style="font-size: 16px;color: #999">暂无数据</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { addressList, addressUserList } from '@/api/system/directory'
import { useUserStore } from '@/store/modules/useraPaas'
import { ElCheckbox, ElCheckboxGroup } from 'element-plus'

const store = useUserStore()
export default {
  name: 'MyAlwaysUse',
  components: {
    ElCheckbox,
    ElCheckboxGroup
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
    // 禁止选择的的用户
    disalbedUserList: {
      type: Array,
      default: null
    }
  },
  data() {
    return {
      checkedAll: false,
      isIndeterminate: false,
      checkList: [],
      keyWord: '',
      publicP: 1,
      active: -1,
      personList: [],
      currentTag: null,
      tagList: [],
      tempChooseList: [] // 当前已经选择的，但当前box框没有的
    }
  },
  watch: {
    userList() {
      this.tempChooseList = JSON.parse(JSON.stringify(this.userList))
    }
  },
  created() {
    this.init()
  },
  methods: {
    init() {
      this.tempChooseList = JSON.parse(JSON.stringify(this.userList))
      this.getTagList()
    },
    changeTagStatus(index) {
      this.publicP = index
      this.getTagList()
    },
    // 再次选中 为取消选中
    chooseTag(tag, index) {
      // if (this.active !== index) {
      this.currentTag = tag
      this.active = index
      this.getTagPersonList()
      // }
    },
    getTagList() {
      const params = {
        accessToken: store.token
      }
      if (this.publicP === 1) {
        params.is_open = 1
      }
      addressList(params).then(res => {
        if (res.code === 200) {
          this.tagList = res.data.list || []
          if (this.tagList.length > 0) {
            this.chooseTag(this.tagList[0], 0)
          }
        }
      })
    },
    getTagPersonList() {
      const params = {
        accessToken: store.token,
        page: 1,
        pagesize: 999,
        tid: this.currentTag.tid
      }
      addressUserList(params).then(res => {
        if (res.code === 200) {
          this.personList = res.data.list || []
          this.checkList = []
          // 默认选择的回填
          this.userList.forEach(itt => {
            this.personList.forEach(idd => {
              if (itt.user_id === idd.user_id) {
                // 是否checklist 有个
                let index = this.checkList.findIndex(itd => { return idd.user_id === itd.user_id })
                index === -1 && this.checkList.push(idd)
              }
            })
          })
          // 禁止选择的人
          let tempD = this.disalbedUserList || []
          tempD.forEach(itt => {
            this.personList.forEach(idd => {
              if (itt.user_id === idd.user_id) {
                idd.disabled = true
              }
            })
          })
          // 设置全选按钮的状态
          if (this.checkList.length === this.personList.length) {
            this.checkedAll = true
          }
          if (this.checkList.length === 0) {
            this.checkedAll = false
          }
          if (this.checkList.length !== 0 && this.personList.length !== 0 && this.checkList.length !== this.personList.length) {
            this.checkedAll = false
            this.isIndeterminate = true
          }
          // // 计算出那些是在当前tab选的
          // this.checkList.forEach(itm => {
          //   let index = this.tempChooseList.findIndex(itd => { return itm.user_id === itd.user_id })
          //   index !== -1 && this.tempChooseList.splice(index, 1)
          // })
        }
      })
    },
    handleCheckAllChange(val) {
      this.isIndeterminate = false
      this.checkList = val ? this.personList : []
      this.isIndeterminate = false
      let tempUser = []
      // 根据全选的开关，决定值
      if (val) {
        this.personList.forEach(item => {
          let index = this.tempChooseList.findIndex(itd => { return item.user_id === itd.user_id })
          index === -1 && tempUser.push(item)
        })
        tempUser = [...tempUser, ...this.tempChooseList]
      } else {
        this.personList.forEach(item => {
          let index = this.tempChooseList.findIndex(itd => { return item.user_id === itd.user_id })
          index !== -1 && this.tempChooseList.splice(index, 1)
        })
        tempUser = [...this.tempChooseList]
      }
      this.$emit('addPerson', tempUser)
    },
    handleCheckedPersonsChange(value) {
      let checkedCount = value.length
      this.checkedAll = checkedCount === this.personList.length
      this.isIndeterminate = checkedCount > 0 && checkedCount < this.personList.length
      if (this.isSingle) {
        if (checkedCount === 0) {
          this.$emit('addPerson', [])
        } else {
          this.$emit('addPerson', [value[checkedCount - 1]])
          this.checkList = [value[checkedCount - 1]]
        }
      } else {
        // 先把所有的去除，在根据选中的添加进来
        // 第一步
        this.personList.forEach(item => {
          let index = this.tempChooseList.findIndex(itd => { return item.user_id === itd.user_id })
          index !== -1 && this.tempChooseList.splice(index, 1)
        })
        value.forEach(item => {
          let index = this.tempChooseList.findIndex(itd => { return item.user_id === itd.user_id })
          index === -1 && this.tempChooseList.push(item)
        })
        this.$emit('addPerson', [...this.tempChooseList])
      }
    }
  }
}
</script>

<style lang="scss" scoped>
  .always_div {
    border-left: 0.5px solid #99999950;
  }
  .person_list {
    width: 100%;
    margin:  0 auto;
    height: calc(100%);
    overflow: auto;
  }
  .dept_list {
    height: calc(100% - 44px);
    overflow: auto;
  }
  .item_hover:hover {
    color: #fff!important;
    span{
      color: #fff!important;
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
      margin-left: 12px;
    }

    .pl-1 {
      padding-left: 4px;
    }

    .w-12 {
      width: 32px;
    }

    .h-12 {
      height: 32px;
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

  .cursor-pointer{
    cursor: pointer;
  }

  .bg-hoverBg {
    background-color: var(--el-color-primary-hb2);
  }

  .flex-row {
    flex-direction: row;
  }

  .flex-col {
    flex-direction: column;
  }

  .hoverIt {
    &:hover {
      background-color: var(--el-color-primary-hb2);
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

   .content-start{
    align-content: flex-start;
  }
</style>
