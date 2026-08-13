<template>
  <template v-for="item in items" :key="item.path">
    <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.path">
      <template #title>
        <el-icon>
          <img
            v-if="isSvgIcon(getMenuIcon(item.meta?.icon))"
            :src="getMenuIcon(item.meta?.icon)"
            class="menu-svg-icon"
            alt=""
          />
          <component v-else :is="getMenuIcon(item.meta?.icon)" />
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
        <img
          v-if="isSvgIcon(getMenuIcon(item.meta?.icon))"
          :src="getMenuIcon(item.meta?.icon)"
          class="menu-svg-icon"
          alt=""
        />
        <component v-else :is="getMenuIcon(item.meta?.icon)" />
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

const isSvgIcon = (icon) => typeof icon === 'string'
</script>

<style scoped>
.menu-svg-icon {
  width: 1em;
  height: 1em;
  object-fit: contain;
}
</style>
