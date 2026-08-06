<template>
  <template v-for="item in items" :key="item.path">
    <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.path">
      <template #title>
        <el-icon>
          <component :is="getMenuIcon(item.meta?.icon)" />
        </el-icon>
        <span>{{ item.meta?.title }}</span>
      </template>
      <SidebarMenuNode
        :items="item.children"
        :get-menu-icon="getMenuIcon"
      />
    </el-sub-menu>

    <el-menu-item v-else :index="item.path">
      <el-icon>
        <component :is="getMenuIcon(item.meta?.icon)" />
      </el-icon>
      <template #title>
        <span>{{ item.meta?.title }}</span>
      </template>
    </el-menu-item>
  </template>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    default: () => []
  },
  getMenuIcon: {
    type: Function,
    required: true
  }
})
</script>
