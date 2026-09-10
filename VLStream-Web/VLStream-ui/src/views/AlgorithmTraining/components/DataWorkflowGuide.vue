<template>
  <section class="workflow-guide" aria-labelledby="data-workflow-title">
    <div class="workflow-heading">
      <h3 id="data-workflow-title">使用流程</h3>
      <el-button link type="primary" :aria-expanded="expanded" aria-controls="data-workflow-content" @click="expanded = !expanded">
        {{ expanded ? '收起流程介绍' : '展开流程介绍' }}
        <el-icon><ArrowUp v-if="expanded" /><ArrowDown v-else /></el-icon>
      </el-button>
    </div>
    <div v-if="expanded" id="data-workflow-content">
      <p class="workflow-description">从原始素材到可用数据集，在数据集中完成数据导入和标注，再划分训练集与验证集，通过版本记录保留每次调整。</p>
      <ol class="workflow-steps">
        <li v-for="step in visibleSteps" :key="step.title" class="workflow-step">
          <div class="step-visual">
            <span class="step-icon"><el-icon :size="30"><component :is="step.icon" /></el-icon></span>
            <span class="step-number">{{ String(visibleSteps.indexOf(step) + 1).padStart(2, '0') }}</span>
            <el-icon v-if="visibleSteps.indexOf(step) < visibleSteps.length - 1" class="step-arrow"><ArrowRight /></el-icon>
          </div>
          <h4>{{ step.title }}</h4>
          <p>{{ step.description }}</p>
          <button type="button" class="step-result" :disabled="disabled" @click="emit('navigate', steps.indexOf(step))">{{ step.result }} <el-icon><ArrowRight /></el-icon></button>
        </li>
      </ol>
      <div class="workflow-tip"><el-icon><InfoFilled /></el-icon><span>{{ hasProject ? '在「样本管理」中导入和查看素材，在「数据集划分与版本」中管理数据集版本。' : '开始使用：新建数据集，或点击下方数据集的「管理数据」，按流程完成数据准备。' }}</span></div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ArrowDown, ArrowRight, ArrowUp, EditPen, Files, FolderOpened, InfoFilled, UploadFilled } from '@element-plus/icons-vue'

const props = defineProps({ canAnnotate: { type: Boolean, default: true }, hasProject: { type: Boolean, default: false }, disabled: { type: Boolean, default: false } })
const emit = defineEmits(['navigate'])
const expanded = ref(true)
const steps = [
  { title: '创建数据集', icon: FolderOpened, description: '填写数据集名称、编号和说明，选择标注类型，明确样本用途与标注规则。', result: '统一管理数据与标注配置' },
  { title: '导入数据', icon: UploadFilled, description: '批量导入图片、视频或 ZIP，记录数据来源，检查文件格式和完整性。', result: '形成可检索的原始样本' },
  { title: '数据标注', icon: EditPen, description: '视频可按时间间隔切图并加入数据集；在标注页面维护类别，为图片添加标注。', result: '形成带类别与标注的图片样本' },
  { title: '数据集与版本', icon: Files, description: '按比例或手动划分训练集和验证集，查看类别分布，保存、对比和回退版本。', result: '查看数据集与版本' }
]
const visibleSteps = computed(() => steps.filter((_, index) => index !== 2 || props.canAnnotate))
</script>

<style scoped>
.workflow-guide { margin: 0 0 24px; padding: 18px 22px 0; border: 1px solid #e8edf5; border-radius: 8px; background: #fff; }
.workflow-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding-bottom: 14px; }
.workflow-heading h3 { margin: 0; font-size: 15px; font-weight: 600; color: #25324b; }
.workflow-heading .el-button { flex-shrink: 0; font-size: 12px; }
.workflow-heading .el-icon { margin-left: 6px; }
.workflow-description { margin: 0; color: #758094; font-size: 13px; line-height: 1.8; }
.workflow-steps { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 28px; list-style: none; margin: 24px 0 20px; padding: 0; }
.workflow-step { min-width: 0; }
.step-visual { position: relative; display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.step-icon { display: flex; align-items: center; justify-content: center; width: 48px; height: 48px; border-radius: 10px; color: var(--el-color-primary, #1e50ff); background: #f1f5ff; }
.step-number { color: #c5cddd; font-size: 23px; font-weight: 500; letter-spacing: 1px; }
.step-arrow { position: absolute; right: -21px; color: #c8d2e3; font-size: 16px; }
.workflow-step h4 { margin: 0 0 10px; font-size: 14px; font-weight: 500; color: #25324b; }
.workflow-step p { margin: 0 0 12px; min-height: 72px; color: #758094; font-size: 12px; line-height: 1.9; }
.step-result { display: inline-flex; align-items: center; gap: 4px; padding: 0; border: 0; background: transparent; text-align: left; font-family: inherit; color: var(--el-color-primary, #1e50ff); font-size: 12px; line-height: 1.8; cursor: pointer; }
.step-result:hover { text-decoration: underline; }
.step-result:focus-visible { outline: 2px solid var(--el-color-primary, #1e50ff); outline-offset: 4px; border-radius: 2px; }
.step-result:disabled { opacity: .5; cursor: not-allowed; }
.step-result .el-icon { flex-shrink: 0; }
.workflow-tip { display: flex; gap: 8px; align-items: flex-start; margin: 0 -22px; padding: 11px 22px; background: #f8faff; border-radius: 0 0 8px 8px; color: #697b99; font-size: 12px; line-height: 1.8; }
.workflow-tip .el-icon { flex-shrink: 0; margin-top: 4px; color: #96a9c8; }
@media (max-width: 1100px) { .workflow-steps { grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 22px; } .step-arrow { display: none; } .workflow-step p { min-height: 0; } }
@media (max-width: 650px) { .workflow-steps { grid-template-columns: 1fr; gap: 20px; } .workflow-step { display: grid; grid-template-columns: 70px 1fr; column-gap: 12px; } .step-visual { grid-row: span 3; flex-direction: column; gap: 6px; margin: 0; } .workflow-step p { margin-bottom: 6px; } }
</style>
