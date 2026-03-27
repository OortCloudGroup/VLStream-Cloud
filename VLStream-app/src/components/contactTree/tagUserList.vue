/**
Created by  lanjian   on 2023/3/14  14:53
Copyright 奥尔特云（深圳）智慧科技有限公司. All rights reserved.
*/
<template>
  <div class="tag_body">
    <div class="tag_box">
      <img v-if="isOpen" src="@/assets/img/contact/tag_open.png" />
      <img v-else src="@/assets/img/contact/tag_close.png" />
      <span class="tag_title">{{ isOpen ? '公开标签' : '私有标签' }}</span>
    </div>

    <ContactTreeUserItem
      v-for="item in list"
      :key="item.user_id"
      :data="item"
      @userClick="userClick"
    />
  </div>
</template>

<script>
import { tagUserList } from '@/api/unifiedUsert/sso'
import ContactTreeUserItem from './ContactTreeUserItem.vue'
import { getToken } from '@/utils/cache'

export default {
  name: 'TagUserList',
  components: { ContactTreeUserItem },
  props: ['tid', 'isOpen'],
  data() {
    return {
      list: []
    }
  },
  watch: {
    tid() {
      this.init()
    }
  },
  created() {
    this.init()
  },
  methods: {
    init() {
      this.list = []
      this.getList()
    },
    async getList() {
      const params = {
        accessToken: getToken(),
        page: 1,
        pagesize: 999,
        tid: this.tid
      }
      let res = await tagUserList(params)
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
.tag_body {
  height: auto;
  padding-left: 16px;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
}

.tag_box {
  margin-top: 16px;
  display: flex;
  flex-direction: row;
  align-items: center;
  img  {
    width: 14px;
  }
}

.tag_title {
  font-size: 12px;
  color: #333333;
  margin-left: 4px;
}
</style>
