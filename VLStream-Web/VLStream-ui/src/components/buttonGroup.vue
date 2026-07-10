<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2025-04-02 16:45:11
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <div class="oort_button_group">
    <!-- eslint-disable-next-line vue/no-v-for-template-key -->
    <template v-for="(item, index) in buttonList" :key="index">
      <div class="oort_button_group_item">
        <oort-svg-icon width="16" height="16" :name="item.svg" @click="item.clickFn" />
        <span @click="item.clickFn">{{ item.name }}</span>
        <el-dropdown v-if="item.children && item.children.length > 0">
          <div class="new_table_svg_group new_item_more">
            <oort-svg-icon width="16" height="16" name="more" class="new_table_svg_group_svg" />
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="(itd, ind) in item.children" :key="ind" @click="itd.clickFn">
                <div class="new_table_svg_group">
                  <oort-svg-icon v-if="itd.svg" width="20" height="20" :name="itd.svg" class="new_table_svg_group_svg" />
                  <span>{{ itd.name }}</span>
                </div>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div v-if="index !== buttonList.length-1 && buttonList.length > 1" class="oort_button_group_line" />
    </template>
  </div>
</template>

<script setup lang="ts">

// 接收父组件传过来的值 buttonList
// 值说明 buttonList: [
//   {
//     name: '按钮名称',
//     svg: 'svg图标名称',
//     clickFn: () => {
//       // 点击按钮执行的函数
//     },
//     // 可选，若有下拉子菜单，则传 children
//     children: [
//       {
//         name: '子菜单名称',
//         svg: '子菜单svg图标名称',
//         clickFn: () => {
//           // 点击子菜单执行的函数
//         }
//       },
//       // ...更多子菜单
//     ]
//   }
// ]
// 使用示例：
// <button-group
//   :button-list="[
//     {svg: 'export', name: '导出', clickFn: exportXls},
//     {svg: 'delete', name: '删除', clickFn: delMoreClick},
//     {
//       svg: 'more',
//       name: '更多操作',
//       clickFn: () => {},
//       children: [
//         {svg: 'edit', name: '编辑', clickFn: editClick},
//         {svg: 'copy', name: '复制', clickFn: copyClick}
//       ]
//     }
//   ]"
// />

defineProps({
  buttonList: {
    type: Array,
    default: () => []
  }
})

</script>

<style lang="scss" scoped>

  .new_item_more {
    margin-left: 0;
    margin-top: 2px;
  }
 .oort_button_group {
    border-radius: 20px;
    background: var(--el-menu-hover-bg-color);
    height: 36px;
    display: flex;
    align-items: center;
    &_item {
      padding: 0 12px;
      height: 100%;
      cursor: pointer;
      display: flex;
      align-items: center;
      flex-direction: row;
      span {
        color: var(--el-color-primary);
        font-size: 14px;
        margin: 0 4px;
        font-weight: 500;
      }
    }
    &_item:first-child:hover {
      background: var(--el-color-primary-hb);
      border-top-left-radius: 20px;
      border-bottom-left-radius: 20px;
    }
    &_item:hover {
      background: var(--el-color-primary-hb);
    }
    &_item:last-child:hover {
      background: var(--el-color-primary-hb);
      border-top-right-radius: 20px;
      border-bottom-right-radius: 20px;
    }
    &_line {
      width: 1px;
      height: 16px;
      background: var(--el-color-primary-hb2);
    }
  }
</style>
