<!--
 * @Author: lanjian
 * @Date: 2021-11-27 11:27:44
 * @LastEditors: lanjian
 * @LastEditTime: 2021-12-10 10:28:27
 * @FilePath: \cordava_utils\demo\src\components\commonpage\aboutPage.vue
 * @Description: Copyright 奥尔特云（深圳）智慧科技有限公司. All rights reserved.
-->
/**
Created by 兰舰 on 2019/12/12  17:14
*/
<template>
  <div class="page_container">
    <nav-header-banner title="关于" />
    <div class="about_home_body">
      <van-cell title-style="text-align: left;" title="版本说明 " is-link to="/versionsPage" />
      <van-cell title-style="text-align: left;" title="联系我们" is-link to="/contactUS" />
    </div>
    <div v-if="appDetail" style="position: absolute;top:66%;width: 100%;display: flex;align-items: center;flex-direction: column;justify-content: space-between;height: 180px;">
      <div style="display: flex;flex-direction: column;align-items: center;justify-content: space-around;height: 140px;">
        <oort-image :src="appDetail.icon_url" style="height: 70px;width: 70px" @click="showVConsole" />
        <span style="font-size: 16px;">{{ appDetail.applabel }}</span>
        <span style="font-size: 12px;color: #999999;">{{ appDetail.version }}</span>
      </div>
      <span style="font-size: 12px;color: #999999;">
        开发公司：{{ appDetail.develop_unit }}
      </span>
    </div>
  </div>
</template>

<script>
import OortImage from '../image/oort_image.vue'
import NavHeaderBanner from '../navHeaderBanner/index'
import config from '@/config'
export default {
  name: 'AboutPage',
  components: { NavHeaderBanner, OortImage },
  data() {
    return {
      appDetail: null,
      clickCount: 0
    }
  },
  created() {
    this.getAPP()
  },
  methods: {
    getAPP() {
      window.androidFunUtils.getAppInfo().then((res) => {
        if (res) {
          this.appDetail = JSON.parse(res)
          config.appInfo = JSON.parse(res)
          console.log(res)
        }
      })
    },
    showVConsole() {
      if (this.clickCount === 5) {
        new window.VConsole()
      }
      this.clickCount++
    }
  }
}

</script>

<style lang="scss" scoped>
  @f_color: #FFFFFF;
  .about_home_body {
    height: calc(100vh - 48px);
    overflow: scroll;
  }
</style>

