/**
Created by  lanjian   on 2023/9/20  16:11
Copyright 奥尔特云（深圳）智慧科技有限公司. All rights reserved.
*/
<template>
  <div class="exit_bg">
    <img class="exit_bg_img" src="@/assets/img/ic_nav_more.png" @click="gotoAbout" />
    <img class="exit_bg_img" src="@/assets/img/exit_circle.png" @click="exitApp" />
  </div>
</template>

<script>
export default {
  name: 'FixNavHeader',
  methods: {
    gotoAbout() {
      this.$router.push('aboutAppPage')
    },
    exitApp() {
      if (window.global) {
        if (window.global.is_cordova) {
          if (!!window.cordova && !!window.cordova.exec) {
            window.androidFunUtils.appExit()
          }
        }
      }
    }
  }
}
</script>

<style lang="scss" scoped>
  .exit_bg {
    width:87px;
    height: 32px;
    background-image:  url("@/assets/img/bg2@3x.png");
    background-size: 100% 100%;
    margin-right: 8px;
    display: flex;
    justify-content: space-around;
    align-items: center;
    position: absolute;
    right: 4px;
    top: 8px;
    z-index: 50;
    &_img {
      width: 24px;
    }
  }
</style>
