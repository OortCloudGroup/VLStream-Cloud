/**
Created by  lanjian   on 2023/3/14  14:16
Copyright 奥尔特云（深圳）智慧科技有限公司. All rights reserved.
*/
<template>
  <div class="always_body">
    <ContactTreeUserItem
      v-for="item in list"
      :key="item.oort_uuid"
      :data="item"
      @userClick="userClick"
    />
  </div>
</template>

<script>
import { usedGet } from '@/api/unifiedUsert/sso'
import ContactTreeUserItem from './ContactTreeUserItem.vue'
import { getToken } from '@/utils/cache'

export default {
  name: 'RecentlyUsePerson',
  components: { ContactTreeUserItem },
  data() {
    return {
      list: []
    }
  },
  created() {
    this.init()
  },
  methods: {
    init() {
      this.getList()
    },
    async getList() {
      const params = {
        accessToken: getToken()
      }
      let res = await usedGet(params)
      if (res.code === 200) {
        this.list = res.data.list
      }
    },
    userClick(data) {
      this.$emit('userClick', data)
    }
  }
}
</script>

<style scoped>
  .always_body {
    height: auto;
    padding-left: 16px;
    display: flex;
    flex-direction: column;
    overflow-x: hidden;
  }
</style>
