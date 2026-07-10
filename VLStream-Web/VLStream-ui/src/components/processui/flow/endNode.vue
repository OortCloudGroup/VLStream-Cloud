<template>
  <div class="end_node" :class="{'node_inactive': !!isFinished}">
    <span>完成</span>
    <oort-svg-icon
      class="end_node_img"
      name="finish_node"
      width="100"
      :color="!!isFinished?'var(--node-main-color)':'var(--el-color-primary)'"
    />
    <!-- <img src="@/assets/img/processui/flownode/start.png" /> -->
  </div>
</template>

<script setup>

defineProps({
  isFinished: { // 已完成的节点
    type: Boolean,
    default: false
  }
})

</script>

<style scoped lang="scss">
    .end_node {
      --node-main-color: var(--el-color-primary);
        cursor: pointer;
        width: 100px;
        min-width: 100px;
        height: 38px;
        background-image: linear-gradient(150deg, #dfecff 15%, #C4DFFF 100%);
        box-shadow: inset -1px -1px 3px 0px rgba(40,50,80,0.15);
        box-shadow: inset 1px 1px 1px 0px rgba(255,255,255,1);
        box-shadow: 0px 2px 16px 0px rgba(86,104,119,0.15);
        border-radius: 8px;
        display: flex;
        flex-direction: row;
        align-items: center;
        justify-content: center;
        position: relative;
        &_img {
            width: 24px;
            height: 24px;
            margin: 0 4px;
        }
        span {
            font-size: 14px;
            color: var(--node-main-color);;
            font-weight: 400;
            margin: 0 4px;
        }
        /* &::before {
          content: "";
          position: absolute;
          border-style: solid;
          border-width: 10px 0 10px 10px;
          border-color: transparent transparent transparent var(--node-main-color);;
          left: -10px;
          top: 50%;
          transform: translateY(-50%);
        } */
    }
    .node_inactive {
      --node-main-color:  #04a904!important;
    }
</style>
