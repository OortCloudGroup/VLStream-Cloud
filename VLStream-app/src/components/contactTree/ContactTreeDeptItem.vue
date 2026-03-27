/**
Created by  lanjian   on 2020/9/21  17:12
Copyright 奥尔特云（深圳）智慧科技有限公司. All rights reserved.
*/
<template>
  <div class="check_box">
    <img v-if="show" src="@/assets/img/contact/check_box_active.png" @click.stop="deptClick" />
    <img v-else src="@/assets/img/contact/check_box.png" @click.stop="deptClick" />
  </div>
</template>

<script>
import bus from '@/utils/bus'
export default {
  name: 'ContactTreeDeptItem',
  inject: ['chooseDeptList'],
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
    // console.log(this.chooseDeptList)
    // 查看是否在选中列表中
    if (this.chooseDeptList.findIndex(it => {
      return it.dept_id === this.data.dept_id
    }) !== -1) {
      this.show = true
    }
    // 监听事件
    bus.$on('refrestatus_dept' + this.data.dept_id, this.changeStatus)
    bus.$on('refrestatus_false_dept', this.changeStatus)
  },
  unmounted() {
    bus.$off('refrestatus_dept' + this.data.dept_id, this.changeStatus)
    bus.$off('refrestatus_false_dept', this.changeStatus)
  },
  methods: {
    changeStatus(data) {
      this.show = data
    },
    deptClick() {
      this.show = !this.show
      const d = {
        data: this.data,
        isCheck: this.show
      }
      this.$emit('deptChooseClick', d)
    }
  }
}
</script>

<style lang="scss" scoped>
  .check_box {
    position: absolute;
    left: 20px;
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
    padding-left: 18px;
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
</style>

