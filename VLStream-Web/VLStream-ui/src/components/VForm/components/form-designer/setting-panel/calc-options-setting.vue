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
      公式配置
    </el-button>
    <el-dialog v-model="dialogVisible" title="公式配置" width="60%" @opened="openedDiloag">
      <div v-if="dialogVisible" class="calc_body">
        <div class="calc_body_left">
          <div class="top_tree">
            <el-tree
              style="max-width: 600px"
              :data="formTree"
              :props="widgetProps"
              node-key="id"
              :default-expanded-keys="['']"
              @node-click="handleFormNodeClick"
            />
          </div>
          <div class="bottom_tree">
            <el-tree
              style="max-width: 600px"
              :data="calcTree"
              node-key="id"
              :default-expanded-keys="['0']"
              @node-click="handleNodeClick"
            >
              <template #default="{ node, data }">
                <span class="custom-tree-node" @mouseover="changeTips({data})">
                  <span>{{ node.label }}</span>
                </span>
              </template>
            </el-tree>
          </div>
        </div>
        <div class="calc_body_right">
          <div class="calc_body_right_title">
            设置组件的值
          </div>
          <div class="calc_editor">
            <div ref="codeMirror" class="codemirror_code" />
          </div>
          <div v-if="showTipsVis" class="calc_body_right_desc">
            <span>函数说明：{{ desc }} </span>
            <span>函数示例：{{ demo }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button size="large" @click="dialogVisible = false">取 消</el-button>
          <el-button size="large" type="primary" @click="saveCalcConfig">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">

import { ref, computed, watch } from 'vue'
import { ElTree, ElButton, ElDialog } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { javascript } from '@codemirror/lang-javascript'
import { EditorView, basicSetup } from 'codemirror'
import { EditorState } from '@codemirror/state'
import { MatchDecorator, Decoration, DecorationSet, ViewPlugin } from '@codemirror/view'
import { PlaceholderWidget } from '~@/utils/placeholderWidget.js'
import { funcItems } from '~@/utils/funcItems.js'

const props = defineProps({
  modelValue: String,
  designer: Object
})

const dialogVisible = ref(false)

const widgetList = computed(() => {
  return props.designer.widgetList.map((item) => { return { ...item, ...item.options } })
})

const widgetProps = {
  // isLeaf: true
}

const formTree = ref([
  {
    label: '当前表单',
    id: '',
    children: widgetList.value
  }
])

const calcTree = ref(funcItems)

const showTipsVis = ref(false)
const desc = ref('')
const demo = ref('')
const changeTips = ({ data }) => {
  showTipsVis.value = !!data.desc
  desc.value = data.desc
  demo.value = data.demo
}

const configRuls = () => {
  dialogVisible.value = true
}

const openedDiloag = () => {
  initCodemirror()
}

const handleFormNodeClick = (data) => {
  if (data.id) {
    insertContent(data, 'field')
  }
}

const handleNodeClick = (data) => {
  if (data.type) {
    insertContent(data, 'opr')
  }
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

const insertContent = (value, type) => {
  // console.log('editor.value', editor.value)
  const state = editor.value.viewState.state
  let anchor = state.selection.main.head + value.label.length + 5 + value.id.length
  if (type === 'opr') {
    anchor += 1
  }
  editor.value.dispatch({
    changes: {
      from: state.selection.main.head,
      to: state.selection.main.head,
      insert: type === 'opr' ? `[[${value.id}.${value.label}]]()` : `[[${value.id}.${value.label}]]`
    },
    selection: { anchor: anchor }}
  )
  editor.value.focus()
}

const codeMirror = ref(null)
const editor = ref(null)
const initCodemirror = () => {
  const placeholderMatcher = new MatchDecorator({
    // regexp: /\[\[(\w+)\]\]/g,
    regexp: /\[\[(.+?)\]\]/g, // 支持中文
    decoration: match => Decoration.replace({
      widget: new PlaceholderWidget(match[1])
    })
  })
  const placeholders = ViewPlugin.fromClass(
    class {
      placeholders: DecorationSet
      constructor(view) {
        this.placeholders = placeholderMatcher.createDeco(view)
      }
      update(update) {
        this.placeholders = placeholderMatcher.updateDeco(
          update,
          this.placeholders
        )
      }
    },
    {
      decorations: (instance) => instance.placeholders,
      provide: (plugin) =>
        EditorView.atomicRanges.of((view) => {
          return view.plugin(plugin)?.placeholders || Decoration.none
        })
    }
  )

  const baseTheme = EditorView.baseTheme()

  editor.value = new EditorView({
    state: EditorState.create({
      doc: currentStr.value,
      extensions: [placeholders, baseTheme, basicSetup, javascript()]
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

<style lang="scss" scoped>

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
    height: 60%;
    overflow: auto;
  }

  .bottom_tree {
    height: 40%;
    overflow: auto;
  }

  .calc_body_right {
    width: 67%;
    height: 100%;
    border-left: 1px solid #e6e6e6;
    overflow: auto;
    position: relative;
  }

  .calc_body_right_header {
    height: 40px;
    display: flex;
    align-items: center;
    span {
      margin-left: 10px;
      font-size: 16px;
      color: #333;
    }
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
    span {
      margin: 4px;
    }
  }

  .CodeMirror {
    /*表单变量样式*/
    .cm-field {
      background: #007bff;
      padding: 3px 5px;
      border-radius: 3px;
      color: #fff;
      margin: 0 1px;
    }
    /*函数样式*/
    .cm-func {
      font-weight: bold;
      color: var(--el-color-primary);
      line-height: 14px;
      margin: 0 1px;
      padding: 0 2px;
    }
    .CodeMirror-scroll {
      width: 100%;
    }
  }

</style>
