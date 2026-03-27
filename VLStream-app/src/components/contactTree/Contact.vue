<template>
  <div class="page_container">
    <nav-header-banner :is-call-back="true" :title="$t('contact.selectP')" @callBack="back" />
    <div class="contact_result" style="background-color: #ffffff">
      <div class="left_meun">
        <div v-if="isChoosePerson" class="left_meun_item" :class="{'left_meun_item_active': activeMeun === 0}" @click="activeMeun = 0">
          <img v-if="activeMeun === 0" src="@/assets/img/contact/recently_active.png" />
          <img v-else src="@/assets/img/contact/recently.png" />
          <span>{{ $t('contact.recently') }}</span>
        </div>
        <div class="left_meun_item" :class="{'left_meun_item_active': activeMeun === 1}" @click="activeMeun = 1">
          <img v-if="activeMeun === 1" src="@/assets/img/contact/org_active.png" />
          <img v-else src="@/assets/img/contact/org.png" />
          <span>{{ $t('contact.org') }}</span>
        </div>
        <!-- <div  v-if="isChoosePerson" class="left_meun_item" :class="{'left_meun_item_active': activeMeun === 3}" @click="activeMeun = 3;">
          <img v-if="activeMeun === 3" src="@/assets/img/contact/tag_active.png" />
          <img v-else src="@/assets/img/contact/tag.png" />
          <span>{{ $t('contact.tag') }}</span>
        </div> -->
        <template v-if="isChoosePerson">
          <div
            v-for="(item,index) in tagList"
            :key="index"
            class="left_meun_item"
            :class="{'left_meun_item_active': activeMeun === index + 4}"
            @click="tagClick(item,index)"
          >
            <img v-if="activeMeun === index + 4" src="@/assets/img/contact/tag_active.png" />
            <img v-else src="@/assets/img/contact/tag.png" />
            <span>{{ item.name }}</span>
          </div>
        </template>
      </div>
      <div class="right_content">
        <RecentlyUsePerson v-if="activeMeun ===0" @userClick="chooseChange" />
        <ContactTree v-if="activeMeun ===1 && rootDept.son_dept.length > 0" :dept="rootDept" :is-choose-person="isChoosePerson" :is-choose-dept="isChooseDept" @userClick="chooseChange" @deptChooseClick="chooseDeptChange" />
        <TagUserList v-if="activeMeun >=3 && currentTag && currentTag.tid" :is-open="currentTag.is_open === 1" :tid="currentTag.tid" @userClick="chooseChange" />
      </div>
    </div>
    <div class="hr_div" />
    <div class="chooseResult">
      <div style="display: flex;padding: 8px">
        <span style="font-size: 16px;color: #333333;">{{ $t('contact.choosed') }}</span>
      </div>
      <div style="display: flex;flex-direction: column;align-items: center;">
        <div class="chooseList">
          <div v-for="item in chooseList" :key="item.user_id" class="choooseItem">
            <ID2HeadPic :id="item.user_id" class="pic_head" />
            <span>{{ item.user_name }}</span>
          </div>
          <div v-for="item in chooseDeptList" :key="item.dept_id" class="choooseItem">
            <img v-if="!!item.dept_photo" :src="item.dept_photo" />
            <img v-else src="@/assets/img/dept_default.png" />
            <span>{{ item.dept_name }}</span>
          </div>
        </div>
        <div v-if="!isSingle" class="contact_sub" @click="backContact">
          {{ $t('contact.confrim') }}
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import ContactTree from './ContactTree'
import NavHeaderBanner from '@/components/navHeaderBanner/index'
import bus from '@/utils/bus'
import RecentlyUsePerson from './recentlyUsePerson.vue'
import ID2HeadPic from '@/components/ID2HeadPic.vue'
import { tagList } from '@/api/unifiedUsert/sso'
import TagUserList from './tagUserList.vue'
import { getToken } from '@/utils/cache'
import { getDeptUser } from '@/api/unifiedUsert/sso.ts'
export default {
  name: 'Contact',
  components: { TagUserList, ID2HeadPic, RecentlyUsePerson, NavHeaderBanner, ContactTree },
  provide() {
    return {
      chooseList: this.personList,
      chooseDeptList: this.deptList
    }
  },
  props: {
    data: {
      type: Object,
      default: () => {
      	return null
      }
    },
    personList: {
      type: Array,
      default: () => {
        return []
      }
    },
    deptList: {
      type: Array,
      default: () => {
        return []
      }
    },
    // 是否单选人
    isSingle: {
      type: Boolean,
      default: false
    },
    // 是否单选部门
    isSingleDept: {
      type: Boolean,
      default: false
    },
    // 可选择人
    isChoosePerson: {
      type: Boolean,
      default: true
    },
    // 可选部门
    isChooseDept: {
      type: Boolean,
      default: false
    },
    // 是否选中部门后选中该部门展开的子元素
    isLinkChild: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      rootDept: {
        dept_id: '',
        dept_name: '所有',
        parentdeptcode: null,
        oort_dpath: '/',
        dept_idpath: '/' + '所有' + '/',
        sort: 1,
        son_dept: []
      },
      value: '',
      list: [],
      currentItem: null,
      chooseList: [],
      chooseDeptList: [],
      activeMeun: 1,
      tagList: [],
      currentTag: null
    }
  },
  watch: {
    personList(value) {
      console.log('personList', value)
      this.chooseList = value
      this.refreshChildStatus()
    },
    deptList(value) {
      this.chooseDeptList = value
      this.isLinkChild && this.refreshChildStatusDept()
    }
  },
  created() {
    this.chooseList = this.personList
    this.chooseDeptList = this.deptList
    // 获取标签列表
    this.getTagList()
    // 获取部门树
    this.getDeptTree()
  },
  methods: {
    async getDeptTree() {
      const params = {
        accessToken: getToken()
      }
      params.dept_id = ''
      getDeptUser(params).then(res => {
        this.rootDept.son_dept = res.data.list || []
      })
    },
    async getTagList() {
      const params = {
        accessToken: getToken()
      }
      let res = await tagList(params)
      params.is_open = 1
      let res2 = await tagList(params)
      if (res.code === 200 && res2.code === 200) {
        this.tagList = res.data.list.concat(res2.data.list)
        if (this.tagList.length > 0) {
          this.currentTag = this.tagList[0]
        }
      }
    },
    tagClick(data, index) {
      this.activeMeun = index + 4
      this.currentTag = data
    },
    refreshChildStatus() {
      bus.$emit('refrestatus_false', false)
      this.personList.forEach(itm => {
        bus.$emit('refrestatus' + itm.user_id, true)
      })
    },
    refreshChildStatusDept() {
      bus.$emit('refrestatus_false_dept', false)
      //  是否选中部门后选中该部门展开的子元素
      this.deptList.forEach(itm => {
        bus.$emit('refrestatus_dept' + itm.dept_id, true)
      })
    },
    // 多选
    // 返回传入的数组
    back() {
      this.$emit('editClose', { userList: this.personList, deptList: this.deptList })
    },
    // 确定 返回
    backContact() {
      this.$emit('editClose', { userList: this.chooseList, deptList: this.chooseDeptList })
    },
    // 点击返回的处理数据的逻辑
    // 选人
    chooseChange(data) {
      if (data.isCheck) {
        const isExist = this.chooseList.some(item => { return item.user_id === data.data.user_id })
        if (!isExist) {
          // 如果是单选
          if (this.isSingle) {
            this.chooseList = [data.data]
          } else {
            this.chooseList.push(data.data)
          }
        }
      } else {
        const index = this.chooseList.findIndex(item => { return item.user_id === data.data.user_id })
        if (index !== -1) {
          this.chooseList.splice(index, 1)
        }
      }
      if (this.isSingle) {
        this.backContact()
      }
    },
    // 选部门
    chooseDeptChange(data) {
      if (data.isCheck) {
        const isExist = this.chooseDeptList.some(item => { return item.dept_id === data.data.dept_id })
        if (!isExist) {
          // 如果是单选
          if (this.isSingleDept) {
            this.chooseDeptList = [data.data]
          } else {
            this.chooseDeptList.push(data.data)
          }
        }
      } else {
        const index = this.chooseDeptList.findIndex(item => { return item.dept_id === data.data.dept_id })
        if (index !== -1) {
          this.chooseDeptList.splice(index, 1)
        }
      }
      if (this.isSingleDept) {
        this.backContact()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
  .chooseResult {
    width: 100%;
    height: 220px;
    display: flex;
    flex-direction: column;
  }
  .hr_div {
    height: 10px;
    width: 100%;
    background: #F7F7F7;
    margin: 4px 0;
  }
  .contact_sub {
    height: 36px;
    width: 91.1%;
    background: #047FF6;
    margin-top: 10px;
    border-radius: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    color: #FFFFFF;
  }
  .chooseList {
    overflow: scroll;
    display: flex;
    flex-wrap: wrap;
    /*height: 100%;*/
    align-items: flex-start;
    height: 128px;
    width: 85%;
    padding: 0 16px;
  }
  .choooseItem {
    min-width: 48px;
    width: 48px;
    margin-bottom: 8px;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    img {
      width: 24px;
      height: 24px;
      object-fit: cover;
      border-radius: 100%;
    }
    span {
      font-size: 12px;
      color: #333333;
    }
  }
  .choose_group {
    border: 0.5px solid rgba(153, 153, 153, 0.19);
    width: 100%;
    height: 48px;
    flex-direction: row;
    align-items: center;
    overflow-y: scroll;
    &_item {
      width: 48px;
      height: 48px;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      img {
        width: 22px;
        height: 22px;
      }
      span{
        font-size: 15px;
      }
    }
  }
  .contact_header {
    padding: 0 10px;
    height: 55px;
    background-color: #FFFFFF;
    display: flex;
    align-items: center;
    flex-direction: row;
    .back {
      width: 20.5px;
      height: 20.5px;
    }
  }
  .contact_result {
    width: 95%;
    height: calc(100vh - 68px - 220px);
    display: flex;
  }

  .left_meun {
    display: flex;
    width: 94px;
    flex-direction: column;
    height: 100%;
    overflow: auto;
    &_item {
      display: flex;
      width: 100%;
      height: 50px;
      min-height: 50px;
      background: #F4F4F4;
      align-items: center;
      border-radius: 0px 0px 4px 0px;
      img {
        margin-left: 10px;
        width: 14px;
        height: 14px;
      }
      span {
        margin-left: 6px;
        font-size: 14px;
        color: #666666;
      }
    }
    &_item_active {
      background: #FFFFFF;
      span {
        color: #1156A6;
      }
    }
  }

  .right_content {
    flex:1;
    height: 100%;
    overflow: scroll;
  }

  .pic_head {
    width: 30px;
    height: 30px;
  }

</style>
