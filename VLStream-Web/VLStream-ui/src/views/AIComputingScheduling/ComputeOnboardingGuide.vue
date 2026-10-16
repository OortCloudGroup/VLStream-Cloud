<template>
  <article class="compute-guide" v-html="guideHtml" />
</template>

<script setup>
import MarkdownIt from 'markdown-it'
import guide from '../../../../../docs/NEW_GPU_ONBOARDING.md?raw'

// Only the versioned repository guide is rendered; raw HTML is disabled.
const markdown = new MarkdownIt({ html: false, linkify: false })
const guideHtml = markdown.render(guide)
</script>

<style scoped>
.compute-guide { max-height: 65vh; overflow: auto; line-height: 1.8; overflow-wrap: anywhere; }
.compute-guide :deep(h1) { font-size: 21px; }
.compute-guide :deep(h2) { font-size: 18px; margin-top: 28px; }
.compute-guide :deep(h3) { font-size: 16px; }
.compute-guide :deep(pre) { padding: 14px; background: var(--el-fill-color-lighter); white-space: pre-wrap; overflow-wrap: anywhere; }
.compute-guide :deep(table) { width: 100%; border-collapse: collapse; font-size: 13px; }
.compute-guide :deep(th), .compute-guide :deep(td) { border: 1px solid var(--el-border-color); padding: 8px; text-align: start; }
.compute-guide :deep(a) { color: var(--el-color-primary); }
</style>
