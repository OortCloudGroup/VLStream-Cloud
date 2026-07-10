<template>
  <el-scrollbar class="side-scroll-bar" :style="{height: scrollerHeight}">
    <div class="panel-container">
      <el-tabs v-model="firstTab" class="no-bottom-margin indent-left-margin">
        <el-tab-pane name="componentLib">
          <template #label>
            <span><svg-icon icon-class="el-set-up" /> {{ i18nt('designer.componentLib') }}</span>
          </template>

          <el-collapse v-model="activeNames" class="widget-collapse">
            <el-collapse-item name="1" :title="i18nt('designer.containerTitle')">
              <draggable
                tag="ul"
                :list="containers"
                item-key="key"
                :group="{name: 'dragGroup', pull: 'clone', put: false}"
                :clone="handleContainerWidgetClone"
                ghost-class="ghost"
                :sort="false"
                :move="checkContainerMove"
                @end="onContainerDragEnd"
              >
                <template #item="{ element: ctn }">
                  <li class="container-widget-item" :title="ctn.displayName" @dblclick="addContainerByDbClick(ctn)">
                    <span><svg-icon :icon-class="ctn.icon" class-name="color-svg-icon" />{{ i18n2t(`designer.widgetLabel.${ctn.type}`, `extension.widgetLabel.${ctn.type}`) }}</span>
                  </li>
                </template>
              </draggable>
            </el-collapse-item>

            <el-collapse-item name="2" :title="i18nt('designer.basicFieldTitle')">
              <draggable
                tag="ul"
                :list="basicFields"
                item-key="key"
                :group="{name: 'dragGroup', pull: 'clone', put: false}"
                :move="checkFieldMove"
                :clone="handleFieldWidgetClone"
                ghost-class="ghost"
                :sort="false"
              >
                <template #item="{ element: fld }">
                  <li class="field-widget-item" :title="fld.displayName" @dblclick="addFieldByDbClick(fld)">
                    <span><svg-icon :icon-class="fld.icon" class-name="color-svg-icon" />{{ i18n2t(`designer.widgetLabel.${fld.type}`, `extension.widgetLabel.${fld.type}`) }}</span>
                  </li>
                </template>
              </draggable>
            </el-collapse-item>

            <el-collapse-item name="3" :title="i18nt('designer.advancedFieldTitle')">
              <draggable
                tag="ul"
                :list="advancedFields"
                item-key="key"
                :group="{name: 'dragGroup', pull: 'clone', put: false}"
                :move="checkFieldMove"
                :clone="handleFieldWidgetClone"
                ghost-class="ghost"
                :sort="false"
              >
                <template #item="{ element: fld }">
                  <li class="field-widget-item" :title="fld.displayName" @dblclick="addFieldByDbClick(fld)">
                    <span><svg-icon :icon-class="fld.icon" class-name="color-svg-icon" />{{ i18n2t(`designer.widgetLabel.${fld.type}`, `extension.widgetLabel.${fld.type}`) }}</span>
                  </li>
                </template>
              </draggable>
            </el-collapse-item>

            <el-collapse-item name="4" :title="i18nt('designer.customFieldTitle')">
              <draggable
                tag="ul"
                :list="customFields"
                item-key="key"
                :group="{name: 'dragGroup', pull: 'clone', put: false}"
                :move="checkFieldMove"
                :clone="handleCustomWidgetClone"
                ghost-class="ghost"
                :sort="false"
              >
                <template #item="{ element: fld, index: index }">
                  <li class="field-widget-item" :title="fld.displayName" @dblclick="addFieldByDbClick(fld)">
                    <span>
                      <svg-icon icon-class="grid" class-name="color-svg-icon" />{{ fld.displayName }}</span>
                    <el-icon class="remove_confmpt" @click="removeComp(fld, index)">
                      <Close />
                    </el-icon>
                  </li>
                </template>
              </draggable>
            </el-collapse-item>
          </el-collapse>
        </el-tab-pane>
        <!-- <el-tab-pane v-if="showFormTemplates()" name="formLib" style="padding: 8px">
          <template #label>
            <span><svg-icon icon-class="el-form-template" /> {{ i18nt('designer.formLib') }}</span>
          </template>

          <template v-for="(ft, idx) in formTemplates">
            <el-card :bord-style="{ padding: '0' }" shadow="hover" class="ft-card">
              <el-popover placement="right" trigger="hover">
                <template #reference>
                  <img :src="ft.imgUrl" style="width: 200px" />
                </template>
                <img :src="ft.imgUrl" style="height: 600px;width: 720px" />
              </el-popover>
              <div class="bottom clear-fix">
                <span class="ft-title">#{{ idx+1 }} {{ ft.title }}</span>
                <el-button link type="primary" class="right-button" @click="loadFormTemplate(ft.jsonUrl)">
                  {{ i18nt('designer.hint.loadFormTemplate') }}
                </el-button>
              </div>
            </el-card>
          </template>
        </el-tab-pane> -->
      </el-tabs>
    </div>
  </el-scrollbar>
</template>

<script>
import { containers as CONS, basicFields as BFS, advancedFields as AFS, customTemplate } from './widgetsConfig'
// import { formTemplates } from './templatesConfig'
import { addWindowResizeHandler, generateId } from '~@/utils/util'
import i18n from '~@/utils/i18n'
import axios from 'axios'
import SvgIcon from '~@/components/svg-icon/index'
import { ElMessageBox, ElTabs, ElTabPane, ElCollapse, ElScrollbar, ElCollapseItem, ElIcon, ElMessage } from 'element-plus'
import bus from '@/utils/bus'
import { deleteForm } from '@/api/processui/index.ts'

export default {
  name: 'FieldPanel',
  components: {
    ElIcon,
    ElTabs,
    ElTabPane,
    ElCollapse,
    ElScrollbar,
    ElCollapseItem,
    SvgIcon
  },
  mixins: [i18n],
  inject: ['getBannedWidgets', 'getDesignerConfig'],
  props: {
    designer: Object
  },
  data() {
    return {
      designerConfig: this.getDesignerConfig(),

      firstTab: 'componentLib',

      scrollerHeight: 0,

      activeNames: ['1', '2', '3', '4'],

      containers: [],
      basicFields: [],
      advancedFields: [],
      customFields: [],

      formTemplates: null
    }
  },
  computed: {
    //
  },
  created() {
    this.loadWidgets()
    bus.$on('setCustomFields', this.setCustomFields)
  },
  mounted() {
    // this.loadWidgets()

    this.scrollerHeight = window.innerHeight - 56 + 'px'
    addWindowResizeHandler(() => {
      this.$nextTick(() => {
        this.scrollerHeight = window.innerHeight - 56 + 'px'
        // console.log(this.scrollerHeight)
      })
    })
  },
  unmounted() {
    bus.$off('setCustomFields', this.setCustomFields)
  },
  methods: {
    isBanned(wName) {
      return this.getBannedWidgets().indexOf(wName) > -1
    },

    showFormTemplates() {
      if (this.designerConfig['formTemplates'] === undefined) {
        return true
      }

      return !!this.designerConfig['formTemplates']
    },

    loadWidgets() {
      console.log('loadWidgets', CONS)
      this.containers = CONS.map(con => {
        return {
          key: generateId(),
          ...con,
          displayName: this.i18n2t(`designer.widgetLabel.${con.type}`, `extension.widgetLabel.${con.type}`)
        }
      }).filter(con => {
        return !con.internal && !this.isBanned(con.type)
      })

      this.basicFields = BFS.map(fld => {
        return {
          key: generateId(),
          ...fld,
          displayName: this.i18n2t(`designer.widgetLabel.${fld.type}`, `extension.widgetLabel.${fld.type}`)
        }
      }).filter(fld => {
        return !this.isBanned(fld.type)
      })

      this.advancedFields = AFS.map(fld => {
        return {
          key: generateId(),
          ...fld,
          displayName: this.i18n2t(`designer.widgetLabel.${fld.type}`, `extension.widgetLabel.${fld.type}`)
        }
      }).filter(fld => {
        return !this.isBanned(fld.type)
      })

      // this.customFields = CFS.map(fld => {
      //   return {
      //     key: generateId(),
      //     ...fld,
      //     displayName: this.i18n2t(`designer.widgetLabel.${fld.type}`, `extension.widgetLabel.${fld.type}`)
      //   }
      // }).filter(fld => {
      //   return !this.isBanned(fld.type)
      // })
    },

    // 将组件列表转换成一个个自定义的组件，这里使用grid包裹起来先
    setCustomFields(list) {
      this.customFields = list.map(item => {
        if (item.content) {
          try {
            item.content = JSON.parse(item.content)
          } catch (error) {
            console.warn('error_setCustomFields', error)
          }
        }
        let temp = JSON.parse(JSON.stringify(customTemplate))
        temp.cols[0].widgetList = item.content.widgetList
        return {
          key: generateId(),
          ...temp,
          displayName: item.formName,
          formId: item.formId
        }
      }).filter(fld => {
        return !this.isBanned(fld.type)
      })
    },

    handleContainerWidgetClone(origin) {
      return this.designer.copyNewContainerWidget(origin)
    },

    handleFieldWidgetClone(origin) {
      return this.designer.copyNewFieldWidget(origin)
    },

    handleCustomWidgetClone(origin) {
      return this.designer.copyNewCustomWidget(origin)
    },

    checkContainerMove(evt) {
      return this.designer.checkWidgetMove(evt)
    },

    checkFieldMove(evt) {
      return this.designer.checkFieldMove(evt)
    },

    onContainerDragEnd() {
      // console.log('Drag end of container: ')
      // console.log(evt)
    },

    addContainerByDbClick(container) {
      this.designer.addContainerByDbClick(container)
    },

    addFieldByDbClick(widget) {
      this.designer.addFieldByDbClick(widget)
    },

    removeComp(row, index) {
      ElMessageBox.confirm(
        '确定删除该自定义组件?',
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: '提示'
        }
      ).then(() => {
        deleteForm({ formId: row.formId }).then((res) => {
          if (res.code === 200) {
            ElMessage({
              type: 'success',
              message: '操作成功'
            })
            this.customFields.splice(index, 1)
          } else {
            ElMessage.error(res.msg)
          }
        })
      })
    },

    loadFormTemplate(jsonUrl) {
      ElMessageBox.confirm(this.i18nt('designer.hint.loadFormTemplateHint'), this.i18nt('render.hint.prompt'), {
        confirmButtonText: this.i18nt('render.hint.confirm'),
        cancelButtonText: this.i18nt('render.hint.cancel')
      }).then(() => {
        axios.get(jsonUrl).then(res => {
          let modifiedFlag = false
          if (typeof res.data === 'string') {
            modifiedFlag = this.designer.loadFormJson(JSON.parse(res.data))
          } else if (res.data.constructor === Object) {
            modifiedFlag = this.designer.loadFormJson(res.data)
          }
          if (modifiedFlag) {
            this.designer.emitHistoryChange()
          }

          this.$message.success(this.i18nt('designer.hint.loadFormTemplateSuccess'))
        }).catch(error => {
          this.$message.error(this.i18nt('designer.hint.loadFormTemplateFailed') + ':' + error)
        })
      }).catch(error => {
        console.error(error)
      })
    }

  }

}
</script>

<style lang="scss" scoped>
  .color-svg-icon {
    -webkit-font-smoothing: antialiased;
    color: #7c7d82;
  }

  .side-scroll-bar {
    :deep(.el-scrollbar__wrap) {
      overflow-x: hidden;
    }
  }

  div.panel-container {
    padding-bottom: 10px;
  }

  .no-bottom-margin :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  .indent-left-margin {
    :deep(.el-tabs__nav) {
      margin-left: 20px;
    }
  }

  .el-collapse-item :deep(ul) > li {
    list-style: none;
  }

  .widget-collapse {
    border-top-width: 0;

    :deep(.el-collapse-item__header) {
      margin-left: 8px;
      font-style: italic;
      font-weight: bold;
    }

    :deep(.el-collapse-item__content) {
      padding-bottom: 6px;

      ul {
        padding-left: 10px;  /* 重置IE11默认样式 */
        margin: 0;  /* 重置IE11默认样式 */
        margin-block-start: 0;
        margin-block-end: 0.25em;
        padding-inline-start: 10px;

        &:after {
          content: "";
          display: block;
          clear: both;
        }

        .container-widget-item, .field-widget-item {
          position: relative;
          display: inline-block;
          height: 32px;
          line-height: 32px;
          width: 45%;
          float: left;
          margin: 2px 6px 6px 0;
          cursor: move;
          white-space: nowrap;
          text-overflow: ellipsis;
          overflow: hidden;
          background: #fff;
          border: 0px solid #e8e9eb;
          border-radius: 4px;
          padding: 0 8px;
        }

        .remove_confmpt {
          display: none;
          cursor: pointer;
          position: absolute;
          top: 0px;
          right: 0px;
        }

        .container-widget-item:hover, .field-widget-item:hover {
          background: #F1F2F3;
          border-color:var(--el-color-primary);

          .color-svg-icon {
            color:var(--el-color-primary);
          }

          .remove_confmpt {
            display: flex;
          }
        }

        .drag-handler {
          position: absolute;
          top: 0;
          left: 160px;
          background-color: #dddddd;
          border-radius: 5px;
          padding-right: 5px;
          font-size: 11px;
          color: #666666;
        }
      }
    }
  }

  .el-card.ft-card {
    border: 1px solid #8896B3;
  }

  .ft-card {
    margin-bottom: 10px;

    .bottom {
      margin-top: 10px;
      line-height: 12px;
    }

    /*
    .image-zoom {
      height: 500px;
      width: 620px
    }
    */

    .ft-title {
      font-size: 13px;
      font-weight: bold;
    }

    .right-button {
      padding: 0;
      float: right;
    }

    .clear-fix:before, .clear-fix:after {
      display: table;
      content: "";
    }

    .clear-fix:after {
      clear: both;
    }

  }

</style>
