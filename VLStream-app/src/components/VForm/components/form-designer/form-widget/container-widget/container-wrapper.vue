<!--
/**
 * author: vformAdmin
 * email: vdpadmin@163.com
 * website: https://www.vform666.com
 * date: 2021.08.18
 * remark: 如果要分发VForm源码，需在本文件顶部保留此文件头信息！！
 */
-->

<template>
  <div class="container-wrapper" :class="[customClass, widget.type, widget.options.name]">
    <slot />

    <div v-if="!!designer && designer.selectedId === widget.id && !widget.internal" class="container-action">
      <i :title="i18nt('designer.hint.selectParentWidget')" @click.stop="selectParentWidget(widget)">
        <svg-icon icon-class="el-back" />
      </i>
      <i v-if="!!parentList && parentList.length > 1" :title="i18nt('designer.hint.moveUpWidget')" @click.stop="moveUpWidget()"><svg-icon icon-class="el-move-up" /></i>
      <i v-if="!!parentList && parentList.length > 1" :title="i18nt('designer.hint.moveDownWidget')" @click.stop="moveDownWidget()"><svg-icon icon-class="el-move-down" /></i>
      <i v-if="widget.type === 'table'" :title="i18nt('designer.hint.insertRow')" @click.stop="appendTableRow(widget)"><svg-icon icon-class="el-insert-row" /></i>
      <i v-if="widget.type === 'table'" :title="i18nt('designer.hint.insertColumn')" @click.stop="appendTableCol(widget)"><svg-icon icon-class="el-insert-column" /></i>
      <i v-if="widget.type === 'grid' || widget.type === 'table'" :title="i18nt('designer.hint.cloneWidget')" @click.stop="cloneContainer(widget)"><svg-icon icon-class="el-clone" /></i>
      <i :title="i18nt('designer.hint.remove')" @click.stop="removeWidget">
        <svg-icon icon-class="el-delete" />
      </i>
    </div>

    <div v-if="!!designer && designer.selectedId === widget.id && !widget.internal" class="drag-handler">
      <i :title="i18nt('designer.hint.dragHandler')"><svg-icon icon-class="el-drag-move" /></i>
      <i>{{ i18n2t(`designer.widgetLabel.${widget.type}`, `extension.widgetLabel.${widget.type}`) }}</i>
      <i v-if="widget.options.hidden === true"><svg-icon icon-class="el-hide" /></i>
    </div>
  </div>
</template>

<script>
import { computed, toRefs } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useContainer } from '~@/components/form-designer/form-widget/container-widget/containerMixin'
import SvgIcon from '~@/components/svg-icon/index.vue'

export default {
  name: 'ContainerWrapper',
  components: {
    SvgIcon
  },
  props: {
    designer: Object,
    indexOfParentList: Number,
    parentList: Array,
    parentWidget: Object,
    widget: Object
  },
  setup(props) {
    const { i18nt, i18n2t } = useI18n()

    // const data = reactive({})
    const containerMixin = useContainer()
    const customClass = computed(() => {
      return !!props.widget.options.customClass ? props.widget.options.customClass.join(' ') : ''
    })

    return {
      i18n2t,
      i18nt,
      ...toRefs(props),
      ...containerMixin,

      customClass
    }
  }
}
</script>

<style lang="scss" scoped>
.container-wrapper {
  position: relative;

  &.selected {
    &:after {
      position: absolute;
      box-sizing: border-box;
      content: ' ';
      pointer-events: none;
      top: 0px;
      right: 0;
      bottom: 0;
      left: 0;
      border: 2px solid var(--color-primary);
      transform: scaley(1);
    }
  }

  .container-action {
    position: absolute;
    bottom: 0;
    right: 0px;
    height: 28px;
    line-height: 28px;
    background: var(--color-primary);
    z-index: 999;

    i {
      font-size: 14px;
      color: #fff;
      margin: 0 5px;
      cursor: pointer;
    }
  }

  .drag-handler {
    position: absolute;
    top: 0px;
     /*bottom: -24px;  拖拽手柄位于组件下方，有时无法正常拖动，原因未明？？ */
    left: 0px;
    height: 22px;
    line-height: 22px;
    background: var(--color-primary);
    z-index: 9;

    i {
      font-size: 14px;
      font-style: normal;
      color: #fff;
      margin: 4px;
      cursor: move;
    }
  }
}
</style>
