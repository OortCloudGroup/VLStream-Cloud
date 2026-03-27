<template>
  <van-tabbar v-model="active" route active-color="#1976D2" inactive-color="#999">
    <van-tabbar-item to="/workbench">
      <span>工作台</span>
      <template #icon="props">
        <van-icon
          name="home-o"
          :color="props.active ? '#1976D2' : '#999'"
          size="20"
        />
      </template>
    </van-tabbar-item>
    <van-tabbar-item to="/video">
      <span>视频</span>
      <template #icon="props">
        <van-icon
          name="video-o"
          :color="props.active ? '#1976D2' : '#999'"
          size="20"
        />
      </template>
    </van-tabbar-item>
    <van-tabbar-item to="/event">
      <span>事件</span>
      <template #icon="props">
        <van-icon
          name="orders-o"
          :color="props.active ? '#1976D2' : '#999'"
          size="20"
        />
      </template>
    </van-tabbar-item>
    <van-tabbar-item to="/my">
      <span>我的</span>
      <template #icon="props">
        <van-icon
          name="user-o"
          :color="props.active ? '#1976D2' : '#999'"
          size="20"
        />
      </template>
    </van-tabbar-item>
  </van-tabbar>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const active = ref(0)

// 根据路由更新激活状态
const routeMap = {
  '/workbench': 0,
  '/video': 1,
  '/event': 2,
  '/my': 3
}

watch(() => route.path, (path) => {
  const matchedPath = Object.keys(routeMap).find(key => path.startsWith(key))
  if (matchedPath) {
    active.value = routeMap[matchedPath]
  } else if (path === '/' || path === '') {
    // 处理根路径重定向到工作台
    active.value = 0
  }
}, { immediate: true })
</script>

<style lang="scss" scoped>
:deep(.van-tabbar) {
  border-top: 1px solid #ebedf0;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
}

:deep(.van-tabbar-item) {
  font-size: 12px;

  .van-icon {
    margin-bottom: 4px;
  }
}
</style>
