<template>
  <div class="open-service-center">
    <div class="document-toolbar">
      <div class="document-title">default.md</div>
      <el-button size="small" type="primary" :loading="loading" @click="loadMarkdown">
        Reload
      </el-button>
    </div>
    <div class="document-body">
      <div v-if="loading" class="document-state">Loading...</div>
      <div v-else-if="error" class="document-state error">{{ error }}</div>
      <div v-else class="document-content" v-html="renderedMarkdown"></div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import MarkdownIt from 'markdown-it'

const markdownParser = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true
})

const markdown = ref('')
const renderedMarkdown = computed(() => markdownParser.render(markdown.value || ''))
const loading = ref(false)
const error = ref('')

const loadMarkdown = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await fetch('http://oort.oortcloudsmart.com:21410/bus/vls-ui/default.md', { cache: 'no-cache' })
    if (!response.ok) {
      throw new Error(`Failed to load default.md (${response.status})`)
    }
    markdown.value = await response.text()
  } catch (err) {
    error.value = err?.message || 'Failed to load default.md'
  } finally {
    loading.value = false
  }
}

onMounted(loadMarkdown)
</script>

<style scoped>
.open-service-center {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;
  background: #f5f7fa;
  box-sizing: border-box;
}

.document-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.document-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
}

.document-body {
  flex: 1;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
  overflow: auto;
}

.document-content {
  color: #303133;
  font-size: 14px;
  line-height: 1.7;
}

.document-content :deep(h1),
.document-content :deep(h2),
.document-content :deep(h3),
.document-content :deep(h4) {
  margin: 16px 0 8px;
  font-weight: 600;
  color: #1f2d3d;
}

.document-content :deep(p) {
  margin: 8px 0;
}

.document-content :deep(a) {
  color: #409eff;
  text-decoration: none;
}

.document-content :deep(a:hover) {
  text-decoration: underline;
}

.document-content :deep(pre) {
  background: #1f2428;
  color: #e6edf3;
  padding: 12px;
  border-radius: 6px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.6;
}

.document-content :deep(code) {
  background: #f2f4f6;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: "Consolas", "Monaco", "Courier New", monospace;
  font-size: 12px;
}

.document-content :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}

.document-content :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 12px;
  border-left: 4px solid #dcdfe6;
  background: #f7f8fa;
  color: #606266;
}

.document-content :deep(ul),
.document-content :deep(ol) {
  margin: 8px 0 8px 20px;
}

.document-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: 13px;
}

.document-content :deep(th),
.document-content :deep(td) {
  border: 1px solid #ebeef5;
  padding: 6px 8px;
  text-align: left;
}

.document-content :deep(th) {
  background: #f5f7fa;
}

.document-state {
  font-size: 14px;
  color: #909399;
}

.document-state.error {
  color: #f56c6c;
}
</style>
