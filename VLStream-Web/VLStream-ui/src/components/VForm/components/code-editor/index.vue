<template>
  <div class="codemirror-container">
    <div ref="editorContainer" class="codemirror-editor" />
  </div>
</template>

<script>
import { EditorState } from '@codemirror/state'
import { EditorView, lineNumbers, highlightActiveLineGutter, highlightActiveLine, keymap } from '@codemirror/view'
import { defaultHighlightStyle, syntaxHighlighting } from '@codemirror/language'
import { history, historyKeymap, defaultKeymap, indentWithTab } from '@codemirror/commands'
import { searchKeymap } from '@codemirror/search'
import { autocompletion, completionKeymap } from '@codemirror/autocomplete'
import { rectangularSelection } from '@codemirror/view'
import { javascript } from '@codemirror/lang-javascript'
import { json } from '@codemirror/lang-json'
import { css } from '@codemirror/lang-css'
import { oneDark } from '@codemirror/theme-one-dark'

export default {
  name: 'CodeEditor',
  props: {
    modelValue: {
      type: String
      // required: true
    },
    readonly: {
      type: Boolean,
      default: false
    },
    mode: {
      type: String,
      default: 'javascript'
    },
    userWorker: { // 是否开启语法检查，默认开启
      type: Boolean,
      default: true
    }
  },
  emits: ['update:modelValue'],
  data() {
    return {
      editor: null,
      codeValue: this.modelValue || '',
      currentLanguage: this.mode
    }
  },
  watch: {
    //
  },
  mounted() {
    this.initEditor()
  },
  beforeUnmount() {
    if (this.editor) {
      this.editor.destroy()
    }
  },
  methods: {
    getLanguageExtension() {
      switch (this.currentLanguage) {
        case 'json':
          return json()
        case 'css':
          return css()
        case 'javascript':
        default:
          return javascript()
      }
    },
    getBasicSetup() {
      // 手动配置 basicSetup 的功能
      return [
        lineNumbers(),
        highlightActiveLineGutter(),
        highlightActiveLine(),
        history(),
        syntaxHighlighting(defaultHighlightStyle),
        autocompletion(),
        rectangularSelection()
      ]
    },
    getKeymap() {
      // 组合所有快捷键
      return [
        ...defaultKeymap,
        ...searchKeymap,
        ...historyKeymap,
        ...completionKeymap,
        indentWithTab
      ]
    },
    initEditor() {
      const initialState = EditorState.create({
        doc: this.codeValue,
        extensions: [
          ...this.getBasicSetup(),
          this.getLanguageExtension(),
          oneDark,
          EditorView.updateListener.of((update) => {
            if (update.docChanged) {
              this.$emit('update:modelValue', update.state.doc.toString())
            }
          }),
          EditorView.editable.of(!this.readonly),
          EditorState.tabSize.of(2),
          keymap.of(this.getKeymap())
        ]
      })

      this.editor = new EditorView({
        state: initialState,
        parent: this.$refs.editorContainer
      })
    },
    setJsonMode() {
      this.currentLanguage = 'json'
      this.updateLanguage()
    },
    setCssMode() {
      this.currentLanguage = 'css'
      this.updateLanguage()
    },
    updateLanguage() {
      if (this.editor) {
        this.editor.dispatch({
          effects: EditorView.reconfigure.of([
            ...this.getBasicSetup(),
            this.getLanguageExtension(),
            oneDark,
            EditorView.updateListener.of((update) => {
              if (update.docChanged) {
                this.$emit('update:modelValue', update.state.doc.toString())
              }
            }),
            EditorView.editable.of(!this.readonly),
            EditorState.tabSize.of(2),
            keymap.of(this.getKeymap())
          ])
        })
      }
    },
    getEditorAnnotations() {
      // CodeMirror 6 中的诊断信息通过插件获取
      // 返回空数组保持兼容性
      return []
    },
    setValue(newValue) {
      if (this.editor) {
        const currentValue = this.editor.state.doc.toString()
        if (currentValue !== newValue) {
          this.editor.dispatch({
            changes: {
              from: 0,
              to: currentValue.length,
              insert: newValue
            }
          })
        }
      }
    }
  }
}
</script>

<style lang="scss" scoped>
  .codemirror-container {
    width: 100%;
    height: 100%;
  }

  .codemirror-editor {
    width: 100%;
    height: 100%;
    min-height: 300px;
    font-size: 12px;
    border: 1px solid #ccc;
  }

  :deep(.cm-editor) {
    height: 100% !important;
    font-family: 'Monaco', 'Courier New', monospace;
    font-size: 12px;
  }

  :deep(.cm-gutters) {
    background-color: #f5f5f5;
    border-right: 1px solid #ddd;
  }

  :deep(.cm-activeLineGutter) {
    background-color: #e8f4f8;
  }

  :deep(.cm-cursor) {
    border-left: 1px solid #333;
  }

  :deep(.cm-content) {
    padding: 8px 0;
  }

  :deep(.cm-line) {
    padding: 0 8px;
  }
</style>
