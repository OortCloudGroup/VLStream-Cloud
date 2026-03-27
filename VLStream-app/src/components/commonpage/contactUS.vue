<!--
 * @Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2025-05-22 11:03:05
 * @Last Modified by:  兰舰
 * @Copyright 奥尔特云(深圳)智慧科技 aPaaS-front-team. All rights reserved.
!-->
<template>
  <div class="page_container">
    <nav-header-banner title="联系我们" />
    <div class="contactUS_body">
      <div class="contactUS_body_text">
        <span>技术支持： <span style="color: #2d8cf0">{{ appInfo?.phone }}</span></span>
        <span>运维支持： <span style="color: #2d8cf0">{{ appInfo?.operate_phone }}</span></span>
        <span>{{ appInfo?.develop_unit }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import NavHeaderBanner from '@/components/navHeaderBanner/index'
import config from '@/config'
export default {
  name: 'ContactUS',
  components: { NavHeaderBanner },
  data() {
    return {
      appInfo: config.appInfo
    }
  }
}
</script>

<style lang="scss" scoped>
  .contactUS_body {
    display: flex;
    justify-content: center;
    align-items: flex-start;
  }
  .contactUS_body_text {
    display: flex;
    flex-direction: column;
    font-size: 14px;
    margin: 16px;
    span {
      margin-top: 16px;
    }
  }
</style>
