<template>
  <div class="depet">
    <ID2HeadPic :src="data.photo" class="head_pic" />
    <div class="user_info">
      <span>{{ data.user_name }}</span>
      <span />
    </div>
    <div class="check_box">
      <img v-if="show" src="@/assets/img/contact/check_box_active.png" @click="userClick" />
      <img v-else src="@/assets/img/contact/check_box.png" @click="userClick" />
    </div>
  </div>
</template>

<script>
import bus from '@/utils/bus'
import ID2HeadPic from '@/components/ID2HeadPic.vue'

export default {
  name: 'ContactTreeUserItem',
  components: { ID2HeadPic },
  inject: ['chooseList'],
  props: {
    data: {
      type: Object,
      default: () => {
        	return null
      }
    }
  },
  data() {
    return {
      show: false
    }
  },
  created() {
    // console.log(this.chooseList)
    // 查看是否在选中列表中
    if (this.chooseList.findIndex(it => {
      return it.user_id === this.data.user_id
    }) !== -1) {
      this.show = true
    }
    // 监听事件
    bus.$on('refrestatus' + this.data.user_id, this.changeStatus)
    bus.$on('refrestatus_false', this.changeStatus)
    // 监听部门的变化来全选部门下的人员
    bus.$on('refrestatus_dept' + this.data.dept_id, this.allCheckItem)
  },
  unmounted() {
    bus.$off('refrestatus' + this.data.user_id, this.changeStatus)
    bus.$off('refrestatus_false', this.changeStatus)
    bus.$off('refrestatus_dept' + this.data.dept_id, this.allCheckItem)
  },
  methods: {
    changeStatus(data) {
      this.show = data
    },
    userClick() {
      this.show = !this.show
      const d = {
        data: this.data,
        isCheck: this.show
      }
      this.$emit('userClick', d)
      // 取消一个用户勾选，要把部门勾选去掉你
      if (!this.show) {
        bus.$emit('refrestatus_dept' + this.data.dept_id, false)
      }
    },
    // 部门勾选的情况下，自动部门下面人员勾选
    allCheckItem(data) {
      if (!!data) {
        this.show = true
        const d = {
          data: this.data,
          isCheck: this.show
        }
        this.$emit('userClick', d)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
  .head_pic {
    width:30px;
    height: 30px;
    object-fit: cover;border-radius: 50%;
  }
  .check_box {
    position: absolute;
    left: 0px;
    top: 0px;
    display: flex;
    height: 100%;
    justify-content: center;
    align-items: center;
    img {
      width: 16px!important;
      height: 16px!important;
    }
  }
  .depet {
    position: relative;
    margin-top: 3px;
    padding-left: 22px;
    display: flex;
    flex-direction: row;
    width: 100%;
    height: 48px;
    display: flex;
    align-items: center;
    background-color: #ffffff;
    img {
      width: 30px;
      height: 30px;
    }
    span {
      margin-left: 10px;
      font-size: 15px;
    }
  }
  .user_info {
    display: flex;
    flex-direction: column;
    span {
      font-size: 16px;
      margin: 2px 4px;
    }
  }
</style>
