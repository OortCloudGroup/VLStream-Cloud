<!--
 * @Author: lanjian
 * @Date: 2021-11-27 10:01:05
 * @LastEditors: lanjian
 * @LastEditTime: 2021-11-27 12:19:45
 * @FilePath: \cordava_utils\demo\src\components\navHeaderBanner\index.vue
 * @Description: Copyright 奥尔特云（深圳）智慧科技有限公司. All rights reserved.
-->
<template>
  <div
    class="nav_banner_container"
    :class="isFull?'nav_banner_container_full':'nav_banner_container_nor'"
    :style="bgOpacity?'background-color:rgba(0, 0, 0,0)':''"
  >
    <div class="nav_banner" :style="bgOpacity ? 'background-color:rgba(0, 0, 0, 0)' : `background-color: ${bgColor}`">
      <div v-if="!exit">
        <div v-if="!nullTitle" class="nav_banner_img" @click="back">
          <!--      <img src="@/assets/img/nav_icon_  back2.png">-->
          <!--        <img v-if="exit" src="@/assets/img/cha.png">-->
          <!--        <img v-if="!exit" src="@/assets/img/exit_circle.png">-->
          <template v-if="!exit">
            <van-icon v-if="color" name="arrow-left" />
            <img v-else src="@/assets/img/nav_icon_back22.png" />
          </template>
        </div>
      </div>
      <div class="nav_banner_body" :style="{ color: color }">
        {{ title }}
      </div>
      <div v-if="searchShow" class="nav_banner_search">
        <img v-if="!searchVis" src="@/assets/img/commandDispatch/search2.png" alt="" @click="searchVis = true" />
        <van-search v-if="searchVis" v-model="keyword" placeholder="请输入搜索关键词" clear-icon />
      </div>
      <div v-if="!exit" class="nav_banner_right">
        <span class="slot_span">
          <slot />
          <div v-if="more" class="more" style="margin: 0px 16px;" @click="showMoreOpr">
            <!--        <img style="width: 25px" src="@/assets/img/qa_feed_back/more.png">-->
            <img
              style="width: 12px;margin-bottom: 8px;width: 24px;height: 24px;"
              src="@/assets/img/more1_old.png"
              @click="showAbout"
            />
            <!--            <div v-if="showMore" class="more_tab">-->
            <!--              &lt;!&ndash;                        <div><img src="@/assets/img/ic_person.png"/><span>责任民警</span></div>&ndash;&gt;-->
            <!--              <div @click="showAbout"><img src="@/assets/img/ic_about.png"><span>关于</span></div>-->
            <!--            </div>-->
          </div>
        </span>
      </div>
      <div v-else class="nav_banner_right" style="width: 100%">
        <slot />
        <div class="nav_banner_right_home">
          <div class="nav_banner_right_home_more">
            <slot name="more">
              <img class="nav_banner_right_home_imgs" src="@/assets/img/ic_nav_more.png" @click="showAbout" />
            </slot>
            <!--          <div v-if="showMore" class="more_tab">-->
            <!--            <div @click="showAbout"><img src="@/assets/img/ic_about.png"><span>关于</span></div>-->
            <!--          </div>-->
          </div>
          <div class="_line" />
          <div class="nav_banner_right_home_exit" @click="back">
            <img class="nav_banner_right_home_imgs" src="@/assets/img/exit_circle.png" />
          </div>
          <img class="nav_banner_right_home_img_bg" src="@/assets/img/bg2@3x.png" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import bus from '@/utils/bus'

export default {
  name: 'NavHeaderBanner',
  props: {
    // 是否显示标题
    notitle: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: ''
    },
    // 点击返回是否触发自定义方法
    isCallBack: {
      type: Boolean,
      default: false
    },
    // 显示更多
    more: {
      type: Boolean,
      default: false
    },
    // 无标题
    nullTitle: {
      type: Boolean,
      default: false
    },
    // 是否退出
    exit: {
      type: Boolean,
      default: false
    },
    // 背景是否透明
    bgOpacity: {
      type: Boolean,
      default: false
    },
    // 是否沉浸式
    isFullscreen: {
      type: Boolean,
      default: false
    },
    // 背景色
    bgColor: {
      type: String,
      default: '#1156a6'
    },
    // 标题颜色
    color: {
      type: String,
      default: ''
    },
    // 是否需要监听返回键的事件
    isBackEvent: {
      type: Boolean,
      default: true
    },
    // 是否显示搜索框
    searchShow: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      showMore: false,
      isFull: false,
      // 是否页面内有pop等页面
      popupShow: false,
      left_title: '', // 左侧显示的标题
      keyword: '',
      searchVis: false
    }
  },
  created() {
    this.isFull = this.isFullscreen
    this.left_title = this.title
    if (this.isBackEvent) {
      // 这里做一个监听， 用来是pop类的弹框出现在页面中是，点击物理返回键时会触发返回而不是. 注意这里pop类 包含 pop， imageview(函数触发)
      bus.$on('popShow', this.popShow)
      // 监听物理返回
      document.addEventListener('backbutton', this.backButton, false)
    }
  },
  // 当前路由组件本 keep-alive 包裹时 该钩子可替代destroy 或者beforeDestroy
  deactivated() {
    if (this.isBackEvent) {
      // 这里做一个监听， 用来是pop类的弹框出现在页面中是，点击物理返回键时会触发返回而不是
      bus.$off('popShow', this.popShow)
      // 销毁监听物理返回
      document.removeEventListener('backbutton', this.backButton, false)
    }
  },
  beforeUnmount() {
    // 销毁监听物理返回
    document.removeEventListener('backbutton', this.backButton, false)
  },
  methods: {
    popShow(data) {
      this.popupShow = data
      console.log('popShow', this.popupShow, data)
    },
    backButton() {
      // 点击物理返回键时 判断页面内是否有popup这样的页面显示，如果有就先触发隐藏
      if (!this.popupShow) {
        this.back()
      } else {
        bus.$emit('popupHide')
        this.popupShow = false
      }
    },
    showAbout() {
      this.$router.push('aboutAppPage')
    },
    showMoreOpr() {
      this.showMore = !this.showMore
      setTimeout(() => {
        this.showMore = false
      }, 1500)
    },
    back() {
      if (!this.exit) {
        if (this.isCallBack) {
          this.$emit('callBack')
        } else {
          this.$router.back()
        }
      } else {
        if (window.global.is_cordova) {
          if (!!window.cordova && !!window.cordova.exec) {
            this.$emit('appExit')
            window.androidFunUtils.appExit()
          }
        }
      }
    }
  }
}
</script>

<style lang="scss" scoped>

.nav_banner_search img{
  width: 24px;
  height: 24px;
  margin-right: 16px;
  margin-top: 5px;
}
:deep(.van-search){
  width: 180px;
}
:deep(.van-search__content){
  border-radius: 20px;
}
$main_color: #1156a6;
.slot_span {
  display: flex;
  justify-content: center;
  align-items: center;

  span {
    font-size: 14px;
    color: #ffffff;
  }
}

.van-icon-arrow-left {
  font-size: 20px;
}

.more {
  position: relative;
}

.more_tab {
  position: absolute;
  top: 38px;
  right: 16px;
  width: 135px;
  border-radius: 2px;
  background-color: white;
  z-index: 9999;
  border: 0.5px solid rgba(153, 153, 153, 0.46);

  div {
    height: 56px;
    width: 100%;
    font-size: 16px;
    color: #333333;
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;

    img {
      margin-left: 16px;
      width: 24px;
      height: 24px;
    }

    span {
      width: 100px;
      margin-left: 4px;
      color: #333333;
      font-size: 16px;
    }
  }
}

.nav_banner_container_full {
  height: 68px;
  justify-content: flex-end!important;
}

.nav_banner_container_nor {
  height: 48px;
}

.nav_banner_container {
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-end;
}

.nav_banner {
  width: 100%;
  height: 48px;
  display: flex;
  justify-content: center;
  align-items: center;

  &_img {
    display: flex;
    justify-content: center;
    align-items: center;
    flex: 1;
    margin-left: 16px;

    img {
      width: 24px;
      height: 24px;
    }
  }

  .nav_banner_body {
    text-align: left;
    margin-left: 8px;
    flex: 5;
    font-size: 18px;
    color: white;
  }

  &_right {
    display: flex;
    justify-content: flex-end;
    margin-right: 16px;
    align-items: center;
    flex: 2;
  }
}

._line {
  height: 18px;
  width: 0;
  background-color: rgba(255, 255, 255, 0.2);
}

.nav_banner_right_home {
  width: 86px;
  height: 26px;
  display: flex;
  justify-content: space-between;
  flex-direction: row;
  border: 0;
  /*border-radius: 50px;*/
  /*border: 0.5px solid rgba(255, 255, 255,0);*/
  /*box-shadow: 0 1px 1px 0 rgba(0,0,0,0.40);*/
  background: #ffffff00;
  background-size: cover;
  align-items: center;
  position: relative;

  &_more {
    width: 43px;
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 3;
  }

  &_exit {
    width: 43px;
    display: flex;
    justify-content: center;
    align-items: center;
    margin-left: 0px;
    z-index: 3;
  }

  &_img_bg {
    position: absolute;
    width: 87px;
    height: 32px;
    left: 0px;
    z-index: 1;
  }

  &_imgs {
    width: 24px;
    height: 24px;
  }
}

</style>
