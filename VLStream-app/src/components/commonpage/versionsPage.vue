<!--
 * @Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2025-05-22 11:02:44
 * @Last Modified by:  兰舰
 * @Copyright 奥尔特云(深圳)智慧科技 aPaaS-front-team. All rights reserved.
!-->
<template>
  <div class="page_container">
    <nav-header-banner title="文档说明" />
    <div class="edition_home_body">
      <div style="width: 84.4%;margin: 0px 7.8%;">
        <div v-for="(item, index) in list" :key="index">
          <div style="display: flex;flex-direction: column;justify-content: center;justify-content: space-around;">
            <span style="font-size: 18px;height: 50px;display: flex;align-items: center;">{{ item?.version }}版本介绍</span>
            <div style="display: flex;flex-direction: column;height: 50px;justify-content: space-around;">
              <span style="font-size: 12px;color: #666666;">- {{ item?.ver_description }}</span>
            </div>
          </div>
          <van-divider v-if="index+1 !==list.length" style="margin: 8px 0;border-bottom: 1px solid;" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import NavHeaderBanner from '@/components/navHeaderBanner/index'
import config from '@/config'
export default {
  name: 'VersionsPage',
  components: { NavHeaderBanner },
  data() {
    return {
      list: [{
        version: config?.appInfo?.version,
        ver_description: config?.appInfo?.ver_description
      }]
    }
  }
}

</script>

<style lang="scss" scoped>
    .edition_home_body {
        height: calc(100vh - 48px);
        overflow: scroll;
        background-color: #fff;
    }
</style>
