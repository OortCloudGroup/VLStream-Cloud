
<!-- eslint-disable vue/no-v-for-template-key -->
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
  <container-item-wrapper v-if="isLoadFinished" :widget="widget">
    <div
      v-show="!widget.options.hidden"
      :key="widget.id"
      :ref="widget.id"
      class="item-container"
      :class="[customClass]"
    >
      <template v-for="(colWidget, colIdx) in widget.items" :key="colIdx">
        <div class="detail-item">
          <span>明细{{ colIdx + 1 }}
            <span v-if="errorTipsArr[colIdx]" style="color: red;">{{ errorTipsArr[colIdx] }}</span></span>
        </div>
        <ItemC
          :ref="`itemC${colIdx}`"
          :widget="colWidget"
          :parent-list="widget.items"
          :index-of-parent-list="0"
          :field="field"
          :parent-widget="widget"
          :col-height="widget.options.colHeight"
        />
      </template>
      <template v-for="(colWidget, colIdx) in copyItems" :key="colIdx">
        <div class="detail-item">
          <span>明细{{ colIdx + 2 }}
            <span v-if="errorTipsArr[colIdx + 1]" style="color: red;">{{ errorTipsArr[colIdx + 1] }}</span></span>
          <el-button v-if="!widget.options.disabled" type="text" icon="el-icon-delete" @click="deleteDetail(colIdx)" />
        </div>
        <ItemC
          :ref="`itemC${colIdx + 1}`"
          :widget="colWidget"
          :field="field"
          :parent-list="widget.items"
          :index-of-parent-list="colIdx + 1"
          :parent-widget="widget"
          :col-height="widget.options.colHeight"
        />
      </template>
      <el-button v-if="!widget.options.disabled" :disabled="widget.options.disabled" type="text" class="add-detail-btn" icon="el-icon-plus" @click="addDetail">
        <span>添加明细</span>
      </el-button>
    </div>
  </container-item-wrapper>
</template>

<script>
import emitter from '~@/utils/emitter'
import i18n from '~@/utils/i18n'
import refMixin from '../../../components/form-render/refMixin'
import ContainerItemWrapper from './container-item-wrapper'
import containerItemMixin from './containerItemMixin'
import ItemC from './itemC'

// 容器组件加入值的特性
import fieldMixin from '~@/components/form-designer/form-widget/field-widget/fieldMixin'

export default {
  name: 'ItemsItem',
  componentName: 'ItemsItem',
  components: {
    ItemC,
    ContainerItemWrapper
  },
  mixins: [emitter, i18n, refMixin, containerItemMixin, fieldMixin],
  inject: ['refList', 'sfRefList', 'globalModel'],
  props: {
    field: Object,
    widget: Object
  },
  data() {
    return {
      copyItems: [],
      localDefaultValue: [],
      tempObj: {},
      isLoadFinished: false,
      isRequiredFields: [],
      errorTipsArr: ['']
    }
  },
  created() {
    this.initRefList()
    // 找到值对象
    if (!this.widget.items && this.widget.items.length === 0) {
      return
    }
    this.widget.items[0].widgetList.forEach(item => {
      this.tempObj[item.id] = item.options.defaultValue
    })
    if (!this.widget.options.defaultValue || this.widget.options.defaultValue.length === 0) {
      this.localDefaultValue = JSON.parse(JSON.stringify([this.tempObj]))
    } else {
      this.localDefaultValue = JSON.parse(JSON.stringify(this.widget.options.defaultValue))
    }
    this.initItemsAndCopyItem()
    this.isLoadFinished = true
    this.initRules()
  },
  mounted() {
    this.off$('itemsItemChange') // 移除原有事件监听
    this.on$('itemsItemChange', (data) => {
      this.localDefaultValue[data[0]][data[1]] = data[2]
      this.handleChangeEvent(this.localDefaultValue)
    })
    this.off$('selfFieldValidation') // 移除原有事件监听
    this.on$('selfFieldValidation', (data) => {
      this.fieldValidation(data)
    })
    // 加入el-item的校验
  },
  beforeUnmount() {
    this.unregisterFromRefList()
  },
  methods: {
    initRules() {
      // 将items 里面的wigdes 的必填的字段 提出来，校验
      this.widget.items[0].widgetList.forEach(item => {
        if (item.options.required) {
          this.isRequiredFields.push(item.options.name)
        }
      })
    },
    doValidate() {
      // 调用一次fieldValidation，如果校验不通过，则返回false
      this.fieldValidation()
      return new Promise((resolve) => {
        resolve(this.errorTipsArr.every(item => item === ''))
      })
    },

    fieldValidation(fieldName) {
      const validateFields = () => {
        this.localDefaultValue.forEach((item, index) => {
          const isEmpty = this.isRequiredFields.some((idt) => item[idt] === '')
          this.errorTipsArr[index] = isEmpty ? (this.widget.options?.requiredHint || '必填项不能为空') : ''
        })
      }
      if (!fieldName || this.isRequiredFields.includes(fieldName[0])) {
        validateFields()
      }
    },
    setDisabled(flag = true) {
      this.$refs.itemC0[0].setDisabled(flag)
      this.copyItems.forEach((item, colIdx) => {
        this.$refs[`itemC${colIdx + 1}`][0].setDisabled(flag)
      })
      // eslint-disable-next-line vue/no-mutating-props
      this.field.options.disabled = flag
    },
    initItemsAndCopyItem() {
    // 根据 localDefaultValue 的值来创建 copyItems，并把值传递到对应的 option 的 defaultValue
      this.copyItems = []
      if (Array.isArray(this.localDefaultValue) && this.widget.items && this.widget.items.length > 0) {
        this.localDefaultValue.forEach((rowValue, index) => {
          if (index === 0) {
            this.widget.items[0].widgetList.forEach(widget => {
              // eslint-disable-next-line no-prototype-builtins
              if (rowValue.hasOwnProperty(widget.id)) {
                widget.options.defaultValue = rowValue[widget.id]
              }
            })
          } else {
            // 深拷贝一份 items[0] 作为每一行的 copyItem
            let itemCopy = JSON.parse(JSON.stringify(this.widget.items[0]))
            // 将 rowValue 的值赋值到 itemCopy 的 widgetList 的每个组件的 options.defaultValue
            if (itemCopy.widgetList && Array.isArray(itemCopy.widgetList)) {
              itemCopy.widgetList.forEach(widget => {
              // eslint-disable-next-line no-prototype-builtins
                if (rowValue.hasOwnProperty(widget.id)) {
                  widget.options.defaultValue = rowValue[widget.id]
                }
              })
            }
            this.copyItems.push(itemCopy)
            this.errorTipsArr.push('')
          }
        })
      }
    },
    deleteDetail(colIdx) {
      this.copyItems.splice(colIdx, 1)
      this.localDefaultValue.splice(colIdx, 1)
      this.errorTipsArr.splice(colIdx, 1)
    },
    addDetail() {
      if (this.widget.items.length > 0) {
        let temp = JSON.parse(JSON.stringify(this.widget.items[0]))
        this.copyItems.push(temp)
        this.localDefaultValue.push(JSON.parse(JSON.stringify(this.tempObj)))
        this.errorTipsArr.push('')
      }
    }
  }
}
</script>

  <style lang="scss" scoped>
    .item-container {
      display: flex;
      flex-direction: column;
      min-height: 50px;
      outline: 3px solid #f9f9f9;
      padding: 15px;
      .form-widget-list {
        min-height: 28px;
      }

      .add-detail-btn {
        margin: 0 auto;
        width: 50%;
        min-width: 100px;
      }
    }

    .detail-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      height: 30px;
      background-color: var(--el-color-primary-light-9);
      border-left: 2px solid var(--el-color-primary-hb2);
      margin-bottom: 10px;
      span {
        margin-left: 10px;
        font-size: 12px;
        color: #9c9c9c;
      }
    }

  </style>
