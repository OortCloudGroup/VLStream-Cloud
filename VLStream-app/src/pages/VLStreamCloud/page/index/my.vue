<template>
  <div class="my-page">
    <!-- 顶部背景 -->
    <div class="top-bg" :style="{ backgroundImage: `url(${bgImg})` }" />

    <div class="my-header">
      <div class="top-title">
        我的
      </div>
    </div>

    <div class="my-content">
      <!-- 用户信息卡片 -->
      <div class="profile-card">
        <div class="avatar">
          <OortImage class="avatar-img" default-type="user" />
        </div>
        <div class="profile-info">
          <div class="name-row">
            <span class="name">{{ displayName }}</span>
            <span class="phone">{{ displayPhone }}</span>
          </div>
          <div class="dept">
            {{ displayDept }}
          </div>
        </div>
      </div>

      <!-- 菜单列表 -->
      <div class="menu-list">
        <div
          v-for="item in menuItems"
          :key="item.key"
          class="menu-item"
          @click="onMenuClick(item)"
        >
          <div class="menu-left">
            <div class="menu-icon-wrapper">
              <img :src="item.icon" alt="" class="menu-icon-img" />
            </div>
            <span class="menu-title">{{ item.title }}</span>
          </div>
          <img :src="arrowImg" alt="" class="menu-arrow-img" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/store/modules/useraPaas'
import bgImg from '@/assets/img/VLStreamCloud/bg.png'
import helpImg from '@/assets/img/VLStreamCloud/mine/bz.png'
import aboutImg from '@/assets/img/VLStreamCloud/mine/gywm.png'
import arrowImg from '@/assets/img/VLStreamCloud/mine/jt.png'
import settingImg from '@/assets/img/VLStreamCloud/mine/sz.png'
import OortImage from '@/components/image/oort_image.vue'

// 用户信息
const store = useUserStore()
console.log(store)
const displayName = computed(() => store.userInfo?.userName || '--')
const displayPhone = computed(() => store.userInfo?.phone || '--')
const displayDept = computed(() => store.userInfo?.deptName || '--')

// 菜单配置
const menuItems = [
  { key: 'help', title: '帮助', icon: helpImg },
  { key: 'about', title: '关于我们', icon: aboutImg },
  { key: 'settings', title: '设置', icon: settingImg }
]

const onMenuClick = (item) => {
  // 这里根据实际路由或业务跳转
  // eslint-disable-next-line no-console
  console.log('click my menu:', item.key)
}
</script>

<style lang="scss" scoped>
.my-page {
  min-height: 100vh;
  background: #F9FAFF;
  position: relative;
  padding: 0 16px 24px;
}

.top-bg {
  position: absolute;
  top: -15px;
  left: -20px;
  right: -20px;
  height: 200px;
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  z-index: 0;
}

.my-header {
  position: relative;
  z-index: 1;
  padding-top: 28px;
}

.top-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
}

.my-content {
  position: relative;
  z-index: 1;
  margin-top: 18px;
}

.profile-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background:#f7f7f7;
  border-radius: 8px;
}

.avatar {
  margin-right: 16px;
}

.avatar-img {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
}

.profile-info {
  flex: 1;
}

.name-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 6px;
}

.name {
  color: #333333;
  font-size: 16px;
}

.phone {
  color: #333333;
  font-size: 16px;
}

.dept {
  color: #666666;
  font-size: 12px;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px;
}

.menu-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.menu-icon-wrapper {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
}

.menu-icon-img {
  width: 34px;
  height: 34px;
  object-fit: contain;
}

.menu-title {
  color: #333333;
  font-size: 14px;
}

.menu-arrow-img {
  width: 24px;
  height: 24px;
  object-fit: contain;
}
</style>
