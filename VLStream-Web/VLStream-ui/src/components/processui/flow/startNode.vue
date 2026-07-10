<template>
  <div class="start_node" :class="{'node_inactive': !!isFinished}">
    <div class="start">
      <oort-svg-icon
        class="start_img"
        name="start_node"
        width="100"
        height="100"
        :color="isFinished?'var(--node-main-color)':'var(--el-color-primary)'"
      />
      <!-- <img src="@/assets/img/processui/flownode/start.png" /> -->
      <span>开始</span>
    </div>
    <div class="arrowBox">
      <span class="arrowBox_line" />
    </div>
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

<style lang="scss" scoped>

  .start_node {
    display: flex;
    flex-direction: row;
    align-items: center;
    --node-main-color: var(--el-color-primary);
  }
    .start {
        cursor: pointer;
        min-width: 100px;
        width: 100px;
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
        &_img {
            width: 24px;
            height: 24px;
            margin: 0 4px;
        }
        span {
            font-size: 14px;
            color: var(--node-main-color);
            font-weight: 400;
            margin: 0 4px;
        }
    }
    .arrowBox {
      position: relative;
      height: 50px;
      background-color: transparent;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-direction: row;
    }

    .arrowBox_line {
      width: 40px;
      min-width: 40px;
      height: 2px;
      background-color: var(--node-main-color);;
    }

    .arrowBox::before {
      content: "";
      position: absolute;
      border-style: solid;
      border-width: 10px 0 10px 10px;
      border-color: transparent transparent transparent var(--node-main-color);;
      right: 0;
      top: 50%;
      transform: translateY(-50%);
    }

    .node_inactive {
      --node-main-color:  #04a904!important;
    }
</style>
