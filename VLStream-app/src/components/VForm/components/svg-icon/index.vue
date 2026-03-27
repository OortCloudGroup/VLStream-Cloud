<template>
  <svg :class="svgClass" aria-hidden="true">
    <use :xlink:href="iconName" />
    <title v-if="!!title">{{ title }}</title>
  </svg>
</template>

<script>
import { computed } from 'vue'
export default {
  name: 'SvgIcon',
  props: {
    className: {
      type: String
    },
    iconClass: {
      required: true,
      type: String
    },
    title: {
      default: '',
      type: String
    }
  },
  setup(props) {
    const iconName = computed(() => {
      return `#icon-${props.iconClass}`
    })

    const svgClass = computed(() => {
      if (props.className) {
        return 'svg-icon ' + props.className
      } else {
        return 'svg-icon'
      }
    })

    return {
      iconName,
      svgClass
    }
  }
}
</script>

<style lang="scss" scoped>
.svg-icon {
  width: 1.1em;
  height: 1.1em;
  margin-left: 0.35em;
  margin-right: 0.35em;
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
}
</style>
