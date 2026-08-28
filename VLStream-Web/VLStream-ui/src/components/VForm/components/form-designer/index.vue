<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<!--
/**
 * author: vformAdmin
 * email: vdpadmin@163.com
 * website: https://www.vform666.com
 * date: 2021.08.18
 * remark: if need to VForm , in info! !
 */
-->

<template>
  <el-container class="main-container full-height">
    <el-container>
      <el-aside class="side-panel">
        <widget-panel :designer="designer" />
      </el-aside>
      <el-container class="center-layout-container">
        <el-header class="toolbar-header">
          <toolbar-panel ref="toolbarRef" :designer="designer" :global-dsv="globalDsv" @saveForm="saveForm">
            <template v-for="(idx, slotName) in $slots" #[slotName]>
              <slot :name="slotName" />
            </template>
          </toolbar-panel>
        </el-header>
        <el-main class="form-widget-main">
          <el-scrollbar class="container-scroll-bar" :style="{height: scrollerHeight}">
            <v-form-widget ref="formRef" :designer="designer" :form-config="designer.formConfig" :global-dsv="globalDsv" />
          </el-scrollbar>
        </el-main>
      </el-container>
      <el-aside class="form-widget-aside">
        <el-button v-if="showClose" class="close_button common_btn" type="danger" @click="closeDesigner">
          关闭
        </el-button>
        <setting-panel
          :designer="designer"
          :selected-widget="designer.selectedWidget"
          :form-config="designer.formConfig"
          :global-dsv="globalDsv"
          @edit-event-handler="testEEH"
        />
      </el-aside>
    </el-container>
  </el-container>
</template>

<script>
import WidgetPanel from './widget-panel/index.vue'
import ToolbarPanel from './toolbar-panel/index.vue'
import SettingPanel from './setting-panel/index.vue'
import VFormWidget from './form-widget/index.vue'
import { createDesigner } from '~@/components/form-designer/designer.js'
import { addWindowResizeHandler, deepClone, getQueryParam, getAllContainerWidgets,
  getAllFieldWidgets, traverseAllWidgets } from '~@/utils/util'
import { MOCK_CASE_URL, VARIANT_FORM_VERSION } from '~@/utils/config'
import i18n, { changeLocale } from '~@/utils/i18n'
import bus from '@/utils/bus'
import axios from 'axios'
import { ElContainer, ElAside, ElHeader, ElMain, ElScrollbar, ElButton } from 'element-plus'

export default {
  name: 'VFormDesigner',
  componentName: 'VFormDesigner',
  components: {
    WidgetPanel,
    ToolbarPanel,
    SettingPanel,
    VFormWidget,
    ElContainer, ElAside, ElHeader, ElMain, ElScrollbar, ElButton
  },
  mixins: [i18n],
  provide() {
    return {
      serverFieldList: this.fieldList,
      getDesignerConfig: () => this.designerConfig,
      getBannedWidgets: () => this.bannedWidgets
    }
  },
  props: {
    /* after field API */
    fieldListApi: {
      type: Object,
      default: null
    },

    /* component array */
    bannedWidgets: {
      type: Array,
      default: () => []
    },

    designerConfig: {
      type: Object,
      default: () => {
        return {
          languageMenu: true, // whether menu
          externalLink: true, // whether GitHub、 etc.
          formTemplates: false, // whether form
          eventCollapse: false, // whether componenteventproperty
          widgetNameReadonly: false, // Update component

          clearDesignerButton: true, // whether null / empty button
          previewFormButton: true, // whether formbutton
          importJsonButton: true, // whether Import JSONbutton
          exportJsonButton: true, // whether Export JSON button
          exportCodeButton: true, // whether Export button
          generateSFCButton: false, // whether Generate SFCbutton

          toolbarMaxWidth: 450, // button ( )
          toolbarMinWidth: 300, // button ( )

          presetCssCode: '', // CSS

          resetFormJson: true // whether in Initialize form is empty
        }
      }
    },

    /* full data variable */
    globalDsv: {
      type: Object,
      default: () => ({})
    },
    showClose: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      vFormVersion: VARIANT_FORM_VERSION,
      curLangName: '',
      curLocale: '',

      vsCodeFlag: false,
      caseName: '',

      scrollerHeight: 0,

      designer: createDesigner(this),

      fieldList: []
    }
  },
  created() {
    this.vsCodeFlag = getQueryParam('vscode') === 1
    this.caseName = getQueryParam('case')
  },
  mounted() {
    this.setCustomFields
    this.initLocale()
    this.scrollerHeight = window.innerHeight - 56 - 36 + 'px'
    addWindowResizeHandler(() => {
      this.$nextTick(() => {
        this.scrollerHeight = window.innerHeight - 56 - 36 + 'px'
      })
    })

    // this.loadCase()
    // this.loadFieldListFromServer()
  },
  methods: {
    closeDesigner() {
      this.$emit('close')
    },
    // component
    setCustomFields(data) {
      bus.$emit('setCustomFields', data)
    },
    testEEH(eventName, eventParams) {
      console.log('test', eventName)
      console.log('test222222', eventParams)
    },

    loadCase() {
      if (!this.caseName) {
        return
      }

      axios.get(MOCK_CASE_URL + this.caseName + '.txt').then(res => {
        if (!!res.data.code) {
          this.$message.error(this.i18nt('designer.hint.sampleLoadedFail'))
          return
        }
        console.log('表单数据---', res.data)
        this.setFormJson(res.data)
        this.$message.success(this.i18nt('designer.hint.sampleLoadedSuccess'))
      }).catch(error => {
        this.$message.error(this.i18nt('designer.hint.sampleLoadedFail') + ':' + error)
      })
    },

    initLocale() {
      this.curLocale = localStorage.getItem('v_form_locale')
      if (!!this.vsCodeFlag) {
        this.curLocale = this.curLocale || 'en-US'
      } else {
        this.curLocale = this.curLocale || 'zh-CN'
      }
      this.curLangName = this.i18nt('application.' + this.curLocale)
      this.changeLanguage(this.curLocale)
    },

    loadFieldListFromServer() {
      if (!this.fieldListApi) {
        return
      }

      let headers = this.fieldListApi.headers || {}
      axios.get(this.fieldListApi.URL, { 'headers': headers }).then(res => {
        let labelKey = this.fieldListApi.labelKey || 'label'
        let nameKey = this.fieldListApi.nameKey || 'name'

        this.fieldList.splice(0, this.fieldList.length) // null / empty already
        res.data.forEach(fieldItem => {
          this.fieldList.push({
            label: fieldItem[labelKey],
            name: fieldItem[nameKey]
          })
        })
      }).catch(error => {
        this.$message.error(error)
      })
    },

    handleLanguageChanged(command) {
      this.changeLanguage(command)
      this.curLangName = this.i18nt('application.' + command)
    },

    changeLanguage(langName) {
      changeLocale(langName)
    },

    setFormJson(formJson) {
      let modifiedFlag = false
      if (!!formJson) {
        if (typeof formJson === 'string') {
          modifiedFlag = this.designer.loadFormJson(JSON.parse(formJson))
        } else if (formJson.constructor === Object) {
          modifiedFlag = this.designer.loadFormJson(formJson)
        }

        if (modifiedFlag) {
          this.designer.emitHistoryChange()
        }
      }
    },

    getFormJson() {
      let tempObj = {
        widgetList: deepClone(this.designer.widgetList),
        formConfig: deepClone(this.designer.formConfig)
      }
      // after need to option customName
      tempObj.widgetList.forEach(widget => {
        if (!!widget.options.customName) {
          widget.customName = widget.options.customName
        }
      })
      return tempObj
    },

    clearDesigner() {
      this.$refs.toolbarRef.clearFormWidget()
    },

    saveForm(flag = 0) {
      if (flag === 1) {
        this.$emit('saveForComponent', this.getFormJson())
      }
      if (flag === 0) {
        this.$emit('saveForm', this.getFormJson())
      }
    },

    /**
       * new form
       */
    refreshDesigner() {
      // this.designer.loadFormJson( this.getFormJson() ) //only ? ?

      let fJson = this.getFormJson()
      this.designer.clearDesigner(true) // history record
      this.designer.loadFormJson(fJson)
    },

    /**
       * form
       */
    previewForm() {
      this.$refs.toolbarRef.previewForm()
    },

    /**
       * Import formJSON
       */
    importJson() {
      this.$refs.toolbarRef.importJson()
    },

    /**
       * Export formJSON
       */
    exportJson() {
      this.$refs.toolbarRef.exportJson()
    },

    /**
       * Export Vue/HTML
       */
    exportCode() {
      this.$refs.toolbarRef.exportCode()
    },

    /**
       * Generate SFC
       */
    generateSFC() {
      this.$refs.toolbarRef.generateSFC()
    },

    /**
       * Get all fieldcomponent
       * @returns {*[]}
       */
    getFieldWidgets(widgetList = null) {
      return !!widgetList ? getAllFieldWidgets(widgetList) : getAllFieldWidgets(this.designer.widgetList)
    },

    /**
       * Get all component
       * @returns {*[]}
       */
    getContainerWidgets(widgetList = null) {
      return !!widgetList ? getAllContainerWidgets(widgetList) : getAllContainerWidgets(this.designer.widgetList)
    },

    /**
       * formjson, new componentproperty
       * @param formJson
       */
    upgradeFormJson(formJson) {
      if (!formJson.widgetList || !formJson.formConfig) {
        this.$message.error('Invalid form json!')
        return
      }

      traverseAllWidgets(formJson.widgetList, (w) => {
        this.designer.upgradeWidgetConfig(w)
      })
      this.designer.upgradeFormConfig(formJson.formConfig)

      return formJson
    },

    getWidgetRef(widgetName, showError = false) {
      return this.$refs['formRef'].getWidgetRef(widgetName, showError)
    },

    getSelectedWidgetRef() {
      return this.$refs['formRef'].getSelectedWidgetRef()
    }

    // TODO: method ! !

  }
}
</script>

<style lang="scss" scoped>
  .el-container.main-container {
    background: #fff;

    :deep(aside) {  /* 防止aside样式被外部样式覆盖！！ */
      margin: 0;
      padding: 0;
      background: inherit;
    }
  }

  .el-container.full-height {
    height: 100%;
    overflow-y: hidden;
  }

  .el-container.center-layout-container {
    min-width: 680px;
    border-left: 2px dotted #EBEEF5;
    border-right: 2px dotted #EBEEF5;
  }

  .el-header.main-header {
    border-bottom: 2px dotted #EBEEF5;
    height: 48px !important;
    line-height: 48px !important;
    min-width: 800px;
  }

  div.main-title {
    font-size: 18px;
    color: #242424;
    display: flex;
    align-items: center;
    justify-items: center;

    img {
      cursor: pointer;
      width: 36px;
      height: 36px;
    }

    span.bold {
      font-size: 20px;
      font-weight: bold;
      margin: 0 6px 0 6px;
    }

    span.version-span {
      font-size: 14px;
      color: #101F1C;
      margin-left: 6px;
    }
  }

  .float-left {
    float: left;
  }

  .float-right {
    float: right;
  }

  .el-dropdown-link {
    margin-right: 12px;
    cursor: pointer;
  }

  div.external-link {
    display: flex;
    align-items: center;

    a {
      font-size: 13px;
      text-decoration: none;
      margin-right: 10px;
      color: #606266;
    }
  }

  .el-header.toolbar-header {
    font-size: 14px;
    border-bottom: 1px dotted #CCCCCC;
    display: flex;
    align-items: center;
    height: 48px !important;

  }

  .el-aside.side-panel {
    width: 260px !important;
    overflow-y: hidden;
  }

  .el-main.form-widget-main {
    padding: 0;

    position: relative;
    overflow-x: hidden;
  }

  .container-scroll-bar {
    :deep(.el-scrollbar__wrap), :deep(.el-scrollbar__view) {
      overflow-x: hidden;
    }
  }

  .form-widget-aside {
    position: relative;
  }

  .close_button {
    position: absolute;
    right: 0;
    top: 4px;
    z-index: 99;
  }

</style>
