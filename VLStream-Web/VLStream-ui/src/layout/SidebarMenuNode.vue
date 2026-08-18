<template>
  <template v-for="item in items" :key="item.path">
    <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.path">
      <template #title>
        <el-icon class="submenu-caret submenu-caret-right">
          <CaretRight />
        </el-icon>
        <el-icon class="submenu-caret submenu-caret-bottom">
          <CaretBottom />
        </el-icon>
        <div class="menu-icon-box">
          <span
            v-if="isSvgIcon(getMenuIcon(item.meta?.icon))"
            class="menu-svg-icon"
            :style="svgMaskStyle(getMenuIcon(item.meta?.icon))"
          />
          <el-icon v-else>
            <component :is="getMenuIcon(item.meta?.icon)" />
          </el-icon>
        </div>
        <span class="menu-title-text">{{ item.meta?.title }}</span>
      </template>
      <SidebarMenuNode
        :items="item.children"
        :get-menu-icon="getMenuIcon"
      />
    </el-sub-menu>

    <el-menu-item v-else :index="item.path">
      <div class="menu-icon-box">
        <span
          v-if="isSvgIcon(getMenuIcon(item.meta?.icon))"
          class="menu-svg-icon"
          :style="svgMaskStyle(getMenuIcon(item.meta?.icon))"
        />
        <el-icon v-else>
          <component :is="getMenuIcon(item.meta?.icon)" />
        </el-icon>
      </div>
      <template #title>
        <span class="menu-title-text">{{ item.meta?.title }}</span>
      </template>
    </el-menu-item>
  </template>
</template>

<script setup>
import { CaretBottom, CaretRight } from '@element-plus/icons-vue'

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
const svgMaskStyle = (src) => ({
  maskImage: `url("${src}")`,
  WebkitMaskImage: `url("${src}")`
})
</script>

<style scoped>
.menu-svg-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  display: inline-block;
  background-color: currentColor;
  mask-size: contain;
  mask-repeat: no-repeat;
  mask-position: center;
  -webkit-mask-size: contain;
  -webkit-mask-repeat: no-repeat;
  -webkit-mask-position: center;
}

.menu-icon-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  color: inherit;
}

.menu-icon-box .el-icon {
  width: 20px;
  height: 20px;
  font-size: 18px;
}

.menu-title-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.submenu-caret {
  flex-shrink: 0;
  width: 12px !important;
  height: 12px !important;
  font-size: 12px;
  margin-right: -10px !important;
  margin-left: -12px;
  color: inherit;
}

.submenu-caret-bottom {
  display: none;
}

:deep(.el-sub-menu.is-opened > .el-sub-menu__title .submenu-caret-right) {
  display: none;
}

:deep(.el-sub-menu.is-opened > .el-sub-menu__title .submenu-caret-bottom) {
  display: inline-flex;
}
</style>
