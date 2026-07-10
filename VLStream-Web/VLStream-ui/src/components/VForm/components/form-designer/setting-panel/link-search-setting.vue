<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-12-10 09:30:08
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <div>
    <el-button :icon="Plus" @click="configRuls">
      设置
    </el-button>
    <el-dialog v-model="dialogVisible" title="查询类型" width="60%" @opened="openedDiloag">
      <div v-if="dialogVisible" class="calc_body">
        <div class="calc_body_right">
          <div class="calc_body_right_title">
            查询添加源码
          </div>
          <div class="calc_editor">
            <div ref="codeMirror" class="codemirror_code" />
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button size="large" @click="mockRun">模拟运行</el-button>
          <el-button size="large" type="primary" @click="saveCalcConfig">保 存</el-button>
          <el-button size="large" @click="dialogVisible = false">取 消</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">

import { ref, watch } from 'vue'
import { ElButton, ElDialog } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { javascript } from '@codemirror/lang-javascript'
import { EditorView, basicSetup } from 'codemirror'
import { EditorState } from '@codemirror/state'

const props = defineProps({
  modelValue: String,
  designer: Object
})

const dialogVisible = ref(false)

const configRuls = () => {
  dialogVisible.value = true
}

const openedDiloag = () => {
  initCodemirror()
}

const mockRun = () => {
  currentStr.value = editor.value?.viewState.state.doc.toString()
  const mockFunc = new Function(currentStr.value)
  const result = mockFunc.call(this)
  alert(JSON.stringify(result))
}

const currentStr = ref(props.modelValue)
watch(() => props.modelValue, (newVal) => {
  currentStr.value = newVal
})

const emits = defineEmits(['update:modelValue'])
const saveCalcConfig = () => {
  currentStr.value = editor.value?.viewState.state.doc.toString()
  emits('update:modelValue', currentStr.value)
  dialogVisible.value = false
}

const codeMirror = ref(null)
const editor = ref(null)
const initCodemirror = () => {
  const baseTheme = EditorView.baseTheme()

  editor.value = new EditorView({
    state: EditorState.create({
      doc: currentStr.value,
      extensions: [baseTheme, basicSetup, javascript()]
    }),
    parent: codeMirror.value
  })
}

</script>

<style>
  .cm-editor {
    height: 100%;
  }

  .cm-line {
    padding: 8px;
  }
</style>

<style scoped>

  .item_width {
    width: 180px;
  }

  .calc_body {
    width: 100%;
    height: 60vh;
    display: flex;
    flex-direction: row;
  }

  .calc_body_left {
    width: 33%;
    height: 100%;
    border-right: 1px solid #e6e6e6;
    overflow: auto;
  }

  .top_tree {
    height: 40%;
  }

  .bottom_tree {
    height: 60%;
  }

  .calc_body_right {
    width: 100%;
    height: 100%;
    border-left: 1px solid #e6e6e6;
    overflow: auto;
    position: relative;
  }

  .calc_body_right_header {
    height: 40px;
    display: flex;
    align-items: center;
  }

  .calc_body_right_header span {
      margin-left: 10px;
      font-size: 16px;
      color: #333;
  }

  .calc_editor {
    width: 100%;
    height: calc(100% - 40px);
  }

  .codemirror_code {
    width: 100%;
    height: 100%;
    border: 1px solid #e6e6e6;
  }

  .calc_body_right_desc {
    position: absolute;
    margin: 4px;
    width: 100%;
    height: 100px;
    left: 0px;
    bottom: 0px;
    z-index: 99;
    background: #f5f5f5;
    padding: 10px;
    border-radius: 8px;
    margin: 10px 40px;
    display: flex;
    flex-direction: column;
  }

  .calc_body_right_desc span {
    margin: 4px;
  }

  .CodeMirror .cm-field {
    background: #007bff;
    padding: 3px 5px;
    border-radius: 3px;
    color: #fff;
    margin: 0 1px;
  }
  .CodeMirror .cm-func {
    font-weight: bold;
    color: var(--el-color-primary);
    line-height: 14px;
    margin: 0 1px;
    padding: 0 2px;
  }
  .CodeMirror .CodeMirror-scroll {
    width: 100%;
  }

</style>
