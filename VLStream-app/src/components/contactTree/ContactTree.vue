<template>
  <div class="contact_body">
    <div class="depet">
      <img class="depet_tran" :class="{'trans90deg': open}" src="@/assets/img/contact/tran.png" @click.stop="getDeptAndUserList" />
      <oort-image v-if="!!dept.dept_photo" :class="{'mgl': isChooseDept}" :src="dept.dept_photo" />
      <img v-else :class="{'mgl': isChooseDept}" src="@/assets/img/dept_default.png" />
      <span>{{ dept.dept_name }}</span>
      <contact-tree-dept-item
        v-if="isChooseDept"
        :data="dept"
        @deptChooseClick="deptChooseClick"
      />
    </div>
    <div v-if="open && dept.son_dept && dept.son_dept.length !== 0">
      <ContactTree
        v-for="item in dept.son_dept"
        :key="item.dept_id"
        :dept="item"
        :is-choose-person="isChoosePerson"
        :is-choose-dept="isChooseDept"
        @userClick="userClick"
        @deptChooseClick="deptChooseClick"
      />
    </div>
    <div v-if="open && dept.users && dept.users.length !== 0 && isChoosePerson" class="user_list">
      <ContactTreeUserItem
        v-for="itd in dept.users"
        :key="itd.user_id"
        :data="itd"
        @userClick="userClick"
      />
    </div>
  </div>
</template>

<script>
import ContactTreeUserItem from './ContactTreeUserItem'
// import { getDeptUser } from '@/api/unifiedUsert/sso.ts'
import ContactTreeDeptItem from './ContactTreeDeptItem'
// import { getToken } from '@/utils/cache'
export default {
  name: 'ContactTree',
  components: { ContactTreeDeptItem, ContactTreeUserItem },
  props: {
    dept: {
      type: Object,
      default: null
    },
    isChoosePerson: {
      type: Boolean,
      default: true
    },
    isChooseDept: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      open: true
    }
  },
  created() {
  },
  methods: {
    deptChooseClick(data) {
      this.$emit('deptChooseClick', data)
    },
    userClick(data) {
      this.$emit('userClick', data)
    },
    getDeptAndUserList() {
      this.open = !this.open
    }
  }
}
</script>

<style lang="scss" scoped>
  .contact_body {
    height: auto;
    display: flex;
    flex-direction: column;
    padding-left: 4px;
    overflow-x: hidden;
  }
  .user_list {
    padding-left: 24px;
    display: flex;
    flex-direction: column;
  }
  .depet {
    position: relative;
    margin-top: 3px;
    /*padding-left: 12px;*/
    display: flex;
    flex-direction: row;
    width: 100%;
    height: 40px;
    align-items: center;
    background-color: #ffffff;
    img {
      width: 30px;
      height: 30px;
      border-radius: 50%;
    }
    span {
      margin-left: 10px;
      font-size: 15px;
    }
  }
  .depet_tran {
    width: 14px!important;
    height: 14px!important;
    margin: 4px!important;
  }
  .trans90deg {
    transform: rotate(90deg);
  }
  .mgl {
    margin-left: 20px;
  }
</style>
