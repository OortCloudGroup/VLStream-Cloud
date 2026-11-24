<template>
  <div class="smart-page" v-loading="loading">
    <header class="smart-heading">
      <div>
        <el-button v-if="detail" link @click="leave">{{ $tp('← 返回任务列表') }}</el-button>
        <h2>{{ detail ? detail.task.taskName : $tp(llmOnly ? '大模型标注' : '智能标注') }}</h2>
        <p>{{ $tp(llmOnly
          ? '使用已授权的视觉模型，自动确认合格结果，其余结果交由人工复核。'
          : '自动生成分类、目标框或像素掩膜，确认后加入数据集。') }}</p>
      </div>
      <div class="actions">
        <el-button @click="refresh">{{ $tp('刷新') }}</el-button>
        <el-button v-if="!detail" type="primary" @click="openCreate">{{ $tp('创建任务') }}</el-button>
        <el-button v-else @click="openDataset">{{ $tp('查看数据集') }}</el-button>
      </div>
    </header>
    <template v-if="!detail">
      <div v-if="!llmOnly" class="mode-cards">
        <div><h3>{{ $tp('主动学习') }}</h3><p>{{ $tp('少量人工标注 → 训练标注模型 → 优先检查难例 → 继续下一轮') }}</p></div>
        <div><h3>{{ $tp('指定模型') }}</h3><p>{{ $tp('选择已有模型 → 批量预标注 → 修正并确认结果') }}</p></div>
      </div>
      <el-alert :title="$tp('预标注结果独立保存；确认后才计入已标注图片。不会覆盖已有人工标注。')" type="info" :closable="false" />
      <el-table :data="tasks" :empty-text="$tp(taskListEmptyHint)">
        <el-table-column prop="taskName" :label="$tp('任务名称')" min-width="160" /><el-table-column v-if="!llmOnly" :label="$tp('模式')" width="110"><template #default="{ row }">{{ modeName(row.mode) }}</template></el-table-column>
        <el-table-column :label="$tp('标注类型')" width="110"><template #default="{ row }">{{ $tp(typeNames[row.annotationType]) }}</template></el-table-column>
        <el-table-column prop="modelName" :label="$tp('模型来源')" min-width="170" /><el-table-column :label="$tp('轮次')" width="75"><template #default="{ row }">{{ row.roundNumber }}</template></el-table-column>
        <el-table-column :label="$tp('状态')" width="120"><template #default="{ row }"><el-tag :type="stateType(row.taskState)">{{ stateName(row.taskState, row.mode) }}</el-tag></template></el-table-column>
        <el-table-column prop="createTime" :label="$tp('创建时间')" width="170" /><el-table-column :label="$tp('操作')" width="120"><template #default="{ row }"><el-button link type="primary" @click="enter(row.id)">{{ $tp('查看任务') }}</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="taskPage" :page-size="20" :total="taskTotal"
        :layout="unfilteredTaskPage ? 'prev, pager, next' : 'total, prev, pager, next'" @current-change="refresh" />
    </template>
    <template v-else>
      <div class="task-summary">
        <el-tag :type="stateType(detail.task.taskState)">{{ stateName(detail.task.taskState, detail.task.mode) }}</el-tag>
        <span>{{ modeName(detail.task.mode) }} · {{ llmTask ? detail.task.modelName : $tp('轮次 {round}', { round: detail.task.roundNumber }) }}</span>
        <span>{{ $tp('已预测 {count} 张', { count: detail.round.predictedCount }) }}</span>
        <span v-if="llmTask">{{ $tp('失败 {count} 张', { count: detail.round.llmFailedCount || 0 }) }}</span>
        <span>{{ $tp('已确认 {count} 张', { count: detail.counts.ACCEPTED || 0 }) }}</span>
        <span>{{ $tp(pilotReview ? '试标候选 {count} 张' : '待确认 {count} 张', { count: detail.counts.PENDING || 0 }) }}</span>
        <span v-if="llmTask && detail.pendingSpotChecks">{{ $tp('待抽检 {count} 张', { count: detail.pendingSpotChecks }) }}</span>
        <span>{{ $tp('已跳过 {count} 张', { count: detail.counts.SKIPPED || 0 }) }}</span>
      </div>
      <el-alert v-if="detail.task.errorMessage" :title="detail.task.errorMessage" type="error" :closable="false" />
      <el-alert v-if="active && detail.execution?.message" :title="detail.execution.message" type="info" :closable="false" />
      <el-alert v-if="detail.task.bulkError" :title="detail.task.bulkError" type="warning" :closable="false" />
      <div v-if="confirming || detail.task.bulkTotal" class="bulk-summary">
        <span>{{ $tp(detail.task.taskState === 'AUTO_CONFIRMING'
          ? '自动确认候选：{processed} / {total}，已保存 {accepted} 张，另有 {conflicts} 张需人工复核'
          : '批量确认：{processed} / {total}，采用 {accepted} 张，{conflicts} 张需人工检查',
          { processed: detail.task.bulkProcessed || 0, total: detail.task.bulkTotal || 0, accepted: detail.task.bulkAccepted || 0, conflicts: detail.task.bulkConflicts || 0 }) }}</span>
        <el-progress v-if="confirming" :percentage="Math.min(100, Math.round((detail.task.bulkProcessed || 0) * 100 / Math.max(1, detail.task.bulkTotal || 1)))" />
      </div>
      <div v-if="(active || llmTask) && !confirming && detail.round.progressTotal" class="bulk-summary"><span>{{ $tp(pilotStage ? '试标进度' : llmTask ? '已处理图片' : detail.round.progressStage === 'TRAINING' ? '本轮模型训练' : '生成预标注') }}：{{ detail.round.progressCurrent || 0 }} / {{ detail.round.progressTotal }}</span><el-progress :percentage="Math.min(100, Math.round((detail.round.progressCurrent || 0) * 100 / detail.round.progressTotal))" /></div>
      <el-alert v-if="active" :title="queueMessage" type="info" :closable="false" />
      <el-alert v-if="pilotReview" :title="$tp('请检查试标图片里的漏标、错标和定位偏差。试标结果暂存在任务中。')" type="info" :closable="false" />
      <div v-if="!['COMPLETED', 'CANCELLED', 'CANCEL_REQUESTED'].includes(detail.task.taskState)" class="actions task-actions">
        <template v-if="pilotReview">
          <el-button type="primary" :disabled="busy || !detail.canContinueLlm" @click="continuePilot">{{ $tp('继续批量标注') }}</el-button>
          <el-button :disabled="busy" @click="openPilotRerun">{{ $tp('调整要求再试标') }}</el-button>
        </template>
        <el-button v-if="llmTask && ['FAILED', 'REVIEW', 'PILOT_REVIEW'].includes(detail.task.taskState) && detail.round.llmFailedCount > 0" :disabled="busy" @click="retryLlm">{{ $tp('重试失败图片') }}</el-button>
        <el-button v-if="!llmTask && detail.task.taskState === 'FAILED'" :disabled="busy" type="primary" @click="perform(retrySmartTask)">{{ $tp('重试本轮') }}</el-button>
        <template v-if="detail.task.taskState === 'REVIEW'">
          <el-button v-if="detail.task.mode === 'active' && detail.canUseRoundModel !== false" :disabled="busy || detail.pendingHard > 0 || detail.task.roundNumber >= detail.maxRounds || !detail.counts.ACCEPTED" @click="next">{{ $tp('继续下一轮') }}</el-button>
          <el-button type="primary" :disabled="busy || detail.pendingSpotChecks > 0" @click="finish">{{ $tp('完成任务') }}</el-button>
          <span v-if="detail.pendingSpotChecks > 0" class="hint">{{ $tp('完成任务前请处理待抽检图片。') }}</span>
        </template>
        <el-button :disabled="busy" type="danger" plain @click="cancel">{{ $tp('取消任务') }}</el-button>
      </div>
      <template v-if="showPredictions(detail)">
        <div class="filters">
          <el-select v-model="query.state" :aria-label="$tp('状态')" @change="changePredictionFilter">
            <el-option :label="$tp(pilotReview ? '试标候选' : '待确认')" value="PENDING" />
            <el-option v-if="!pilotReview" :label="$tp('已确认')" value="ACCEPTED" />
            <el-option v-if="!pilotReview" :label="$tp('已跳过')" value="SKIPPED" />
            <el-option v-if="llmTask" :label="$tp('预标注失败')" value="FAILED" />
            <el-option :label="$tp('全部结果')" value="" />
          </el-select>
          <el-checkbox v-if="llmTask && detail.task.taskState === 'REVIEW' && (detail.pendingSpotChecks || query.reviewKind)"
            :model-value="query.reviewKind === 'SPOT_CHECK'" @change="filterSpotChecks">{{ $tp('仅看待抽检') }}</el-checkbox>
          <el-checkbox v-if="detail.task.mode === 'active'" v-model="query.hardOnly" @change="changePredictionFilter">{{ $tp('仅看本轮难例（待处理 {count}）', { count: detail.pendingHard }) }}</el-checkbox>
          <template v-if="detail.task.taskState === 'REVIEW' && !query.reviewKind">
            <el-button :disabled="busy || !confirmable.length" @click="batchConfirm">{{ $tp('确认本页可用结果（{count}）', { count: confirmable.length }) }}</el-button>
            <el-button type="primary" :disabled="busy || !canConfirmRound" @click="confirmRoundPredictions">{{ $tp('确认本轮已通过标注') }}</el-button>
          </template>
        </div>
        <p v-if="!pilotReview" class="hint">{{ $tp(llmTask
          ? '低分、缺分、抽检或需要修正的结果须逐图复核。分割结果可在编辑器中补齐遗漏区域并修正边界。'
          : '难例按预测置信度排序，优先检查漏检、低置信度和类别不匹配的图片。模型类别未匹配时，需手动选择标签。') }}</p>
        <div class="prediction-grid">
          <article v-for="row in predictions" :key="row.candidate.id" class="prediction-card" :class="{ 'is-actionable': canReview(row) }" @click="openReview(row)">
            <div class="image">
              <img v-if="row.previewSrc && !row.previewFailed" :src="row.previewSrc" :alt="row.imageName" loading="lazy" @error="row.previewFailed = true" />
              <span v-else-if="row.previewFailed" class="preview-error">{{ $tp('图片读取失败') }}</span>
              <el-tag v-if="!llmTask && row.candidate.hardExample" class="hard-tag" type="warning" size="small">{{ $tp('难例') }}</el-tag>
              <el-tag v-else-if="row.candidate.llmReviewKind === 'SPOT_CHECK'" class="hard-tag" type="warning" size="small">{{ $tp('高分抽检') }}</el-tag>
              <el-tag v-else-if="row.candidate.llmReviewKind === 'PILOT'" class="hard-tag" type="info" size="small">{{ $tp('试标样本') }}</el-tag>
            </div>
            <div class="prediction-description">
              <strong>{{ row.imageName }}</strong>
              <span>{{ boxCount(row) }} {{ $tp(countUnits[detail.task.annotationType]) }} · {{ $tp(pilotReview && row.candidate.reviewState === 'PENDING' ? '可预览' : reviewStates[row.candidate.reviewState]) }}</span>
              <small v-if="row.predictionQuality?.suppressedCount">{{ $tp('已合并 {count} 个同类重叠框', { count: row.predictionQuality.suppressedCount }) }}</small>
              <small v-if="row.predictionQuality?.classConflict" role="status">{{ $tp('类别冲突，需逐图检查后确认') }}</small>
              <small v-if="row.candidate.reviewError">{{ row.candidate.reviewError }}</small>
              <el-button v-if="canReview(row)" link type="primary" @click.stop="openReview(row)">{{ $tp(pilotReview ? '查看试标' : '检查并确认') }}</el-button>
            </div>
          </article>
        </div>
        <el-empty v-if="!predictionsLoading && !predictions.length" :description="$tp('当前筛选下没有结果')" /><el-pagination v-model:current-page="query.page" :page-size="20" :total="predictionTotal" layout="total, prev, pager, next" @current-change="loadPredictions" />
      </template>
      <details class="round-history"><summary>{{ $tp('轮次与模型记录') }}</summary><el-table :data="detail.rounds"><el-table-column prop="roundNumber" :label="$tp('轮次')" width="70" /><el-table-column prop="versionId" :label="$tp('输入数据集版本 ID')" width="200" /><el-table-column prop="predictedCount" :label="$tp('预测图片')" width="100" /><el-table-column v-if="llmTask" prop="llmFailedCount" :label="$tp('失败图片')" width="100" /><el-table-column v-else prop="modelSha256" :label="$tp('模型校验值')" min-width="220" /><el-table-column :label="$tp('状态')" min-width="110"><template #default="{ row }">{{ stateName(row.roundState, detail.task.mode) }}</template></el-table-column></el-table></details>
    </template>

    <el-dialog v-model="creating" :title="$tp(llmOnly ? '创建大模型标注任务' : '创建智能标注任务')" width="min(660px, calc(100vw - 32px))" :close-on-click-modal="false">
      <el-form label-width="110px">
        <el-form-item v-if="!llmOnly" :label="$tp('标注方式')">
          <el-radio-group v-model="form.mode" :disabled="busy" @change="changeMode">
            <el-radio value="active">{{ $tp('主动学习') }}</el-radio>
            <el-radio value="model">{{ $tp('指定模型') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$tp('数据集')">
          <el-select v-model="form.datasetId" filterable remote :remote-method="searchDatasets" :loading="datasetsLoading"
            :placeholder="$tp('搜索数据集')" :no-data-text="$tp('暂无数据集')"
            :aria-describedby="datasetIssue ? 'llm-dataset-issue' : undefined" :disabled="busy" style="width:100%" @change="changeDataset">
            <template #label="{ label }">{{ options?.datasetName ? datasetLabel(options.datasetName, options.annotationType) : label }}</template>
            <el-option v-for="dataset in datasets" :key="dataset.id" :label="datasetLabel(dataset.annotationName, dataset.annotationType)"
              :value="String(dataset.id)" :disabled="datasetDisabled(dataset)">
              <span class="dataset-option">
                <span class="dataset-option-name">{{ datasetLabel(dataset.annotationName, dataset.annotationType) }}</span>
                <span v-if="datasetReason(dataset)" class="dataset-option-status">{{ $tp(datasetReason(dataset)) }}</span>
              </span>
            </el-option>
          </el-select>
          <p v-if="activeLearning" class="hint">
            {{ $tp(datasetNeedsGeneration
              ? '请在数据集管理中确认该数据集已生成有效训练包，再重新选择。'
              : '首次主动学习前，请先导入待标注图片并完成人工示例标注，再到数据集管理生成训练包。') }}
          </p>
          <p v-else-if="datasetIssue" id="llm-dataset-issue" class="hint" role="status">{{ $tp(datasetIssue) }}</p>
        </el-form-item>
        <el-form-item v-if="form.mode !== 'llm'" :label="$tp('GPU 设备')"><div style="width:100%"><TrainingComputeSelector v-model="form.computeNodeId" :nodes="computeNodes" :loading="computeLoading" :disabled="busy" :node-label="node => node.name || String(node.id)" @reload="loadCompute" /><el-button link :disabled="computeLoading" @click="loadCompute">{{ $tp('刷新') }}</el-button><p v-if="form.computeNodeId && !selectedCompute?.schedulable" class="hint">{{ $tp('请先在算力市场完成开机与环境检查') }}</p></div></el-form-item>
        <el-form-item :label="$tp('任务名称')"><el-input v-model="form.taskName" maxlength="80" :disabled="busy" /></el-form-item>
        <template v-if="form.mode === 'llm'">
          <el-form-item :label="$tp('大模型提供商')">
            <div class="llm-field">
              <el-select v-model="form.providerId" :aria-label="$tp('大模型提供商')" :loading="llmProvidersLoading" :disabled="busy" :placeholder="$tp('选择已授权的提供商')" @change="changeLlmProvider">
                <el-option v-for="provider in llmProviders" :key="provider.id" :label="provider.name" :value="String(provider.id)" />
              </el-select>
              <el-button link :disabled="busy || llmProvidersLoading" @click="loadLlmProviders">{{ $tp('刷新') }}</el-button>
              <p v-if="llmProviderError" class="hint llm-field-message" role="status">{{ llmProviderError }}</p>
              <p v-else-if="!llmProvidersLoading && !llmProviders.length" class="hint llm-field-message">{{ $tp('暂无可用提供商，请在大模型管理中授权或配置。') }}</p>
              <p v-else-if="llmProviderName" class="hint llm-field-message">{{ $tp('创建任务后，未标注图片将发送给 {provider} 生成候选。', { provider: llmProviderName }) }}</p>
            </div>
          </el-form-item>
          <el-form-item :label="$tp('视觉模型')">
            <div class="llm-field">
              <el-select v-model="form.modelName" filterable :loading="llmModelsLoading" :disabled="busy || !form.providerId" :placeholder="$tp('请选择模型')">
                <el-option v-for="model in llmModels" :key="model" :label="model" :value="model" />
              </el-select>
              <p v-if="llmModelError" class="hint llm-field-message" role="status">{{ llmModelError }}</p>
            </div>
          </el-form-item>
          <el-form-item :label="$tp('标注要求')">
            <el-input v-model="form.prompt" type="textarea" :rows="3" :maxlength="2000" show-word-limit :disabled="busy" :placeholder="$tp('可选。描述标签的判断标准，例如遮挡时只标可见部分。')" />
          </el-form-item>
        </template>
        <el-form-item v-else :label="$tp('模型来源')"><el-select v-model="sourceKey" :placeholder="$tp('请选择模型')" style="width:100%" filterable><el-option v-for="source in sources" :key="`${source.type}:${source.id}`" :label="sourceLabel(source)" :value="`${source.type}:${source.id}`" /></el-select></el-form-item>
        <el-form-item :label="$tp('预测阈值')">
          <div style="width:100%">
            <el-slider v-model="confidenceThreshold" :min="0.05" :max="0.95" :step="0.05" show-input :disabled="busy" />
            <p class="hint">{{ $tp(form.mode === 'llm'
              ? '校验通过且各项分数均高于阈值时自动确认，其余逐图复核。'
              : '图片通过校验，且模型返回的每项预测（包括合并前的重叠框）置信度都超过阈值时，系统会自动保存为正式标注。空预测和未达标的图片留待人工确认。') }}</p>
          </div>
        </el-form-item>
        <template v-if="llmQualitySupported">
          <el-form-item :label="$tp('小批试标')">
            <div class="llm-field">
              <el-switch v-model="form.llmPilotEnabled" :aria-label="$tp('小批试标')" :disabled="busy" />
              <template v-if="form.llmPilotEnabled">
                <el-input-number v-model="form.llmPilotSize" :aria-label="$tp('试标图片数量')" :min="1" :max="50" :precision="0" :disabled="busy" />
                <span>{{ $tp('张') }}</span>
              </template>
              <p v-if="form.llmPilotEnabled" class="hint llm-field-message">{{ $tp('先生成少量候选供检查，图片不足时试标全部。你可以调整标注要求、重新试标或继续批量标注。') }}</p>
            </div>
          </el-form-item>
          <el-form-item :label="$tp('高分抽检')">
            <div class="llm-field">
              <el-input-number v-model="form.llmReviewPercent" :aria-label="$tp('高分抽检比例')" :min="0" :max="100" :precision="0" :disabled="busy" />
              <span>%</span>
              <p class="hint llm-field-message">{{ $tp('随机抽查本可自动通过的图片，逐张确认后保存。设为 0 可关闭。') }}</p>
            </div>
          </el-form-item>
        </template>
        <template v-if="form.mode === 'active'"><el-form-item :label="$tp('每轮训练次数')"><el-input-number v-model="form.epochs" :min="1" :max="100" /></el-form-item><el-form-item :label="$tp('每轮难例数量')"><el-input-number v-model="form.reviewSize" :min="1" :max="500" /></el-form-item></template>
      </el-form>
      <template v-if="options"><el-alert :title="$tp('{value0}：当前有 {value1} 张未标注图片。{value2}', { value0: $tp(typeNames[options.annotationType]), value1: options.unlabeled, value2: modeHint })" :type="ready ? 'info' : 'warning'" :closable="false" /><div class="label-counts"><el-tag v-for="label in options.labelCounts" :key="label.name">{{ label.name }}：{{ label.count }} {{ $tp(options.countUnit) }}</el-tag></div></template>
      <template #footer><el-button :disabled="busy" @click="creating = false">{{ $tp('取消') }}</el-button><el-button type="primary" :loading="busy" :disabled="!ready" @click="create">{{ $tp(llmQualitySupported && form.llmPilotEnabled ? '开始试标' : '创建并入队') }}</el-button></template>
    </el-dialog>
    <el-dialog v-model="rerunningPilot" :title="$tp('调整要求再试标')" width="min(540px, calc(100vw - 32px))" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item :label="$tp('标注要求')">
          <el-input v-model="pilotPrompt" type="textarea" :rows="4" :maxlength="2000" show-word-limit :disabled="busy" />
        </el-form-item>
      </el-form>
      <p class="hint">{{ $tp('重新处理同一批试标图片。任务会再次暂停，供你检查新结果。') }}</p>
      <template #footer>
        <el-button :disabled="busy" @click="rerunningPilot = false">{{ $tp('取消') }}</el-button>
        <el-button type="primary" :loading="busy" @click="rerunPilot">{{ $tp('重新试标') }}</el-button>
      </template>
    </el-dialog>
    <SmartAnnotationReview :item="reviewItem" :annotation-type="detail?.task.annotationType || 'object_detection'" :labels="detail?.labels || []" :saving="busy" :llm="llmTask" :read-only="pilotReview" :title="pilotReview ? '试标预览' : '确认预标注'" @close="closeReview" @save="saveReview" />
  </div>
</template>

<script setup>
import { translatePhrase as uiText } from '@/i18n'

import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDataProjects } from '@/api/dataManagement'
import { listSmartTasks, smartOptions, listSmartLlmProviders, listSmartLlmModels, createSmartTask, getSmartTask, smartPredictions, getSmartPrediction, fetchSmartPredictionImage, reviewSmartPrediction, confirmSmartBatch, confirmAllSmartPredictions, nextSmartRound, continueSmartLlm, rerunSmartLlmPilot, finishSmartTask, retrySmartTask, cancelSmartTask } from '@/api/smartAnnotation'
import SmartAnnotationReview from './components/SmartAnnotationReview.vue'
import TrainingComputeSelector from './components/TrainingComputeSelector.vue'
import { listComputeSelection } from '@/api/compute'
import { captureComputeIdentity } from '@/utils/computeIdentityRequest'

const props = defineProps({ llmOnly: Boolean })
const route = useRoute(); const router = useRouter()
const tasks = ref([]); const taskPage = ref(1); const taskTotal = ref(0); const detail = ref(null)
const unfilteredTaskPage = ref(false)
const taskListEmptyHint = computed(() => {
  if (taskTotal.value > 0) return '当前筛选下没有结果'
  return props.llmOnly ? '暂无大模型标注任务' : '暂无智能标注任务，创建任务开始预标注'
})
const loading = ref(false); const busy = ref(false); const creating = ref(false)
const datasets = ref([]); const options = ref(null); const sourceKey = ref('')
const datasetsLoading = ref(false)
const datasetNeedsGeneration = ref(false)
const datasetIssue = ref('')
const predictions = ref([]); const predictionTotal = ref(0); const reviewItem = ref(null)
const predictionsLoading = ref(false)
const rerunningPilot = ref(false)
const pilotPrompt = ref('')
const llmQualityDefaults = { llmConfidence: 0.50, llmPilotEnabled: true, llmPilotSize: 10, llmReviewPercent: 5 }
const form = reactive({
  datasetId: '', taskName: '', mode: props.llmOnly ? 'llm' : 'active',
  computeNodeId: '', confidence: 0.25, epochs: 20, reviewSize: 50,
  providerId: '', modelName: '', prompt: '', ...llmQualityDefaults
})
const llmProviders = ref([])
const llmModels = ref([])
const llmProvidersLoading = ref(false)
const llmModelsLoading = ref(false)
const llmProviderError = ref('')
const llmModelError = ref('')
const llmProviderName = computed(() => llmProviders.value.find(provider => String(provider.id) === form.providerId)?.name)
const confidenceThreshold = computed({
  get: () => form.mode === 'llm' ? form.llmConfidence : form.confidence,
  set: value => { if (form.mode === 'llm') form.llmConfidence = value; else form.confidence = value }
})
const activeLearning = computed(() => form.mode === 'active')
const computeNodes = ref([]); const computeLoading = ref(false)
const selectedCompute = computed(() => computeNodes.value.find(node => String(node.id) === form.computeNodeId))
let computeSequence = 0
const loadCompute = async () => {
  const sequence = ++computeSequence; const identity = captureComputeIdentity()
  const apply = response => { identity.assertCurrent(); if (sequence === computeSequence) computeNodes.value = response.data || [] }
  computeLoading.value = true
  try { apply(await listComputeSelection(apply)) } catch (error) { if (sequence === computeSequence) { computeNodes.value = []; handleError(error) } }
  finally { if (sequence === computeSequence) computeLoading.value = false }
}
const query = reactive({ page: 1, state: 'PENDING', hardOnly: false, reviewKind: '' })
const states = { QUEUED: '等待 GPU', PREPARING: '准备数据', RUNNING: '计算中', PILOT_REVIEW: '待查看试标', REVIEW: '待确认', CONFIRMING: '批量确认中', AUTO_CONFIRMING: '自动确认中', COMPLETED: '已完成', FAILED: '失败', CANCEL_REQUESTED: '正在取消', CANCELLED: '已取消' }
const typeNames = { image_classification: '图像分类', object_detection: '物体检测', instance_segmentation: '实例分割', semantic_segmentation: '语义分割' }
const datasetLabel = (name, type) => typeNames[type] ? `${name} · ${uiText(typeNames[type])}` : name
const countUnits = { image_classification: '个类别', object_detection: '个预测框', instance_segmentation: '个实例', semantic_segmentation: '类区域' }
const reviewStates = { PENDING: '待确认', ACCEPTED: '已确认', SKIPPED: '已跳过', FAILED: '预标注失败' }
const modeNames = { active: '主动学习', model: '指定模型', llm: '大模型标注' }
const modeName = mode => uiText(modeNames[mode] || mode)
const stateName = (state, mode) => uiText(mode === 'llm' && state === 'QUEUED' ? '等待模型调用' : states[state] || state)
const stateType = state => ({ COMPLETED: 'success', FAILED: 'danger', REVIEW: 'warning', PILOT_REVIEW: 'warning' }[state] || 'info')
const llmTask = computed(() => detail.value?.task.mode === 'llm')
const pilotStage = computed(() => llmTask.value && detail.value?.round.progressStage === 'llm_pilot')
const pilotReview = computed(() => llmTask.value && detail.value?.task.taskState === 'PILOT_REVIEW')
const confirming = computed(() => ['CONFIRMING', 'AUTO_CONFIRMING'].includes(detail.value?.task.taskState))
const active = computed(() => confirming.value || ['QUEUED', 'PREPARING', 'RUNNING', 'CANCEL_REQUESTED'].includes(detail.value?.task.taskState))
const queueMessage = computed(() => {
  if (detail.value.task.taskState !== 'QUEUED') return uiText('正在处理本轮数据。可以离开页面，后台将继续运行。')
  return uiText(llmTask.value ? '任务已入队，等待调用视觉模型。' : '已进入 GPU 队列，等待前序训练或智能标注任务结束。')
})
const sources = computed(() => (options.value?.sources || []).filter(source => form.mode === 'active' || source.type !== 'algorithm'))
const sourceLabel = source => {
  if (source.type === 'system') return `${uiText('系统基础模型')} · ${uiText(typeNames[options.value?.annotationType])}`
  if (source.type === 'algorithm') return source.name.replace(/ · 基础权重$/, ` · ${uiText('基础权重')}`)
  if (source.type === 'round') return source.name.replace(/ · 第(\d+)轮$/, (_, round) => ` · ${uiText('轮次 {round}', { round })}`)
  return source.name
}
const modeHint = computed(() => {
  if (activeLearning.value) return uiText('首次主动学习要求至少 {value0} 张未标图片，每类至少 {value1}{value2}已确认标注，最多 {value3} 轮。', {
    value0: options.value.minUnlabeledImages,
    value1: options.value.minBoxesPerLabel,
    value2: uiText(options.value.countUnit),
    value3: options.value.maxRounds
  })
  return uiText(form.mode === 'llm' ? '视觉模型使用下列数据集标签生成候选。' : '指定模型会预标注当前所有可用的未标注图片。')
})
const llmQualitySupported = computed(() => form.mode === 'llm' && options.value?.llmQualitySupported === true)
const ready = computed(() => {
  if (!form.datasetId || !form.taskName.trim() || !(options.value?.unlabeled > 0)) return false
  if (form.mode === 'llm') {
    return Boolean(llmProviderName.value && llmModels.value.includes(form.modelName) && options.value.labelCounts?.length)
      && (!llmQualitySupported.value || (
        (!form.llmPilotEnabled || (Number.isInteger(form.llmPilotSize) && form.llmPilotSize >= 1 && form.llmPilotSize <= 50))
        && Number.isInteger(form.llmReviewPercent) && form.llmReviewPercent >= 0 && form.llmReviewPercent <= 100
      ))
  }
  return !computeLoading.value && selectedCompute.value?.schedulable && sourceKey.value
    && (!activeLearning.value || (options.value.datasetGenerated === true
      && options.value.unlabeled >= options.value.minUnlabeledImages
      && options.value.labelCounts.every(label => label.count >= options.value.minBoxesPerLabel)))
})
const boxCount = row => JSON.parse(row.candidate.boxesJson || '[]').length
const confirmable = computed(() => predictions.value.filter(row => {
  if (row.predictionQuality?.classConflict || row.predictionQuality?.manualReview || row.candidate.llmReviewKind) return false
  const boxes = JSON.parse(row.candidate.boxesJson || '[]')
  return row.candidate.reviewState === 'PENDING' && boxes.length
    && boxes.every(box => box.labelId && (llmTask.value
      ? Number.isFinite(box.confidence) && box.confidence > detail.value.task.confidence
      : box.confidence >= detail.value.task.confidence))
}))
const canReview = row => row.candidate.reviewState === 'PENDING' && (pilotReview.value || detail.value?.task.taskState === 'REVIEW')
const canConfirmRound = computed(() => {
  const pending = detail.value?.counts.PENDING || 0
  return pending > (llmTask.value ? detail.value.pendingHard || 0 : 0)
})
const showPredictions = result => ['REVIEW', 'COMPLETED'].includes(result.task.taskState)
  || (result.task.mode === 'llm' && ['PILOT_REVIEW', 'FAILED'].includes(result.task.taskState))
const handleError = error => { if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || uiText('操作失败')) }
const run = async operation => { if (busy.value) return; busy.value = true; try { await operation() } catch (error) { handleError(error) } finally { busy.value = false } }
let predictionRequest
let reviewRequest
let disposed = false
const clearPredictions = () => {
  predictionRequest?.abort()
  predictionsLoading.value = false
  for (const row of predictions.value) {
    if (row.previewSrc) URL.revokeObjectURL(row.previewSrc)
  }
  predictions.value = []
}
const closeReview = () => {
  reviewRequest?.abort()
  if (reviewItem.value) URL.revokeObjectURL(reviewItem.value.previewUrl)
  reviewItem.value = null
}
const loadPredictions = async () => {
  if (disposed || !detail.value) return
  clearPredictions()
  const { id } = detail.value.task
  const controller = new AbortController()
  predictionRequest = controller
  predictionsLoading.value = true
  try {
    const result = await smartPredictions(id, { ...query }, controller.signal)
    if (controller.signal.aborted) return
    predictions.value = result.records.map(row => ({ ...row, previewSrc: '', previewFailed: false }))
    predictionTotal.value = result.total

    const rows = predictions.value
    let nextImage = 0
    const loadPreviews = async () => {
      while (!controller.signal.aborted && nextImage < rows.length) {
        const row = rows[nextImage++]
        try {
          const blob = await fetchSmartPredictionImage(id, row.candidate.id, controller.signal)
          if (!controller.signal.aborted) row.previewSrc = URL.createObjectURL(blob)
        } catch {
          if (!controller.signal.aborted) row.previewFailed = true
        }
      }
    }
    for (let worker = 0; worker < Math.min(4, rows.length); worker++) void loadPreviews()
  } catch (error) {
    if (!controller.signal.aborted) handleError(error)
  } finally {
    if (!controller.signal.aborted) predictionsLoading.value = false
  }
}
const changePredictionFilter = () => {
  if (query.state !== 'PENDING') query.reviewKind = ''
  query.page = 1
  return loadPredictions()
}
const filterSpotChecks = checked => {
  query.reviewKind = checked ? 'SPOT_CHECK' : ''
  if (checked) query.state = 'PENDING'
  return changePredictionFilter()
}
const resetPredictionQuery = result => {
  const allAccepted = !result.counts?.PENDING && result.counts?.ACCEPTED > 0
  query.page = 1
  query.reviewKind = ''
  query.state = result.task.mode === 'llm' && !result.counts?.PENDING && result.round.llmFailedCount > 0
    ? 'FAILED' : allAccepted ? 'ACCEPTED' : 'PENDING'
  query.hardOnly = !allAccepted && result.task.mode === 'active' && result.pendingHard > 0
}
const refresh = async () => {
  if (loading.value) return
  loading.value = true
  try {
    if (detail.value) {
      const { id } = detail.value.task
      const result = await getSmartTask(id)
      if (disposed || detail.value?.task.id !== id) return
      if (result.task.taskState !== detail.value.task.taskState) {
        if (detail.value.task.taskState === 'PILOT_REVIEW') {
          closeReview()
          rerunningPilot.value = false
        }
        if (showPredictions(result)) resetPredictionQuery(result)
      }
      detail.value = result
      if (showPredictions(result)) await loadPredictions()
    }
    else {
      const result = await listSmartTasks({ page: taskPage.value, datasetId: route.query.dataset, mode: props.llmOnly ? 'llm' : 'compute' })
      if (disposed) return
      tasks.value = result.records.filter(task => props.llmOnly ? task.mode === 'llm' : ['active', 'model'].includes(task.mode))
      // Keep pagination usable if an older server returns mixed pages and an unfiltered total.
      if (tasks.value.length < result.records.length) unfilteredTaskPage.value = true
      taskTotal.value = result.total
    }
  } catch (error) { handleError(error) } finally { loading.value = false }
}
const enter = id => run(async () => {
  const result = await getSmartTask(id)
  if (disposed) return
  const isLlm = result.task.mode === 'llm'
  if (isLlm !== props.llmOnly) {
    await router.replace({ path: isLlm ? '/llm-smart-annotation' : '/smart-annotation', query: { ...route.query, task: String(id) } })
    return
  }
  detail.value = result
  resetPredictionQuery(detail.value)
  await router.replace({ query: { ...route.query, task: String(id) } })
  if (showPredictions(detail.value)) await loadPredictions()
})
const leave = async () => { clearPredictions(); closeReview(); detail.value = null; await router.replace({ query: route.query.dataset ? { dataset: route.query.dataset } : {} }); await refresh() }
const openDataset = () => router.push({ path: '/data-management', query: { dataset: detail.value.task.datasetId } })
const llmDatasetIssues = {
  MISSING_LABELS: '请先添加标注标签',
  NO_UNLABELED_IMAGES: '无可标注图片',
  UNSUPPORTED_TYPE: '不支持的标注类型'
}
const datasetReason = dataset => {
  if (activeLearning.value && dataset.datasetGenerated !== true) {
    return dataset.datasetGenerated === false ? '训练包未生成或已失效' : '训练包生成状态未确认'
  }
  if (form.mode !== 'llm') return ''
  if (!typeNames[dataset.annotationType]) return llmDatasetIssues.UNSUPPORTED_TYPE
  if (dataset.llmAnnotationStatus) return llmDatasetIssues[dataset.llmAnnotationStatus] || ''
  const { totalCount, annotatedCount } = dataset
  return totalCount === 0 || (totalCount > 0 && totalCount === annotatedCount)
    ? llmDatasetIssues.NO_UNLABELED_IMAGES : ''
}
const datasetDisabled = dataset => Boolean(datasetReason(dataset))
let llmProviderSequence = 0
let llmModelSequence = 0
const llmDialogOpen = () => creating.value && form.mode === 'llm'
const changeLlmProvider = async () => {
  const sequence = ++llmModelSequence
  const provider = llmProviders.value.find(item => String(item.id) === form.providerId)
  const previousModel = form.modelName
  form.modelName = ''
  llmModels.value = []
  llmModelError.value = ''
  llmModelsLoading.value = Boolean(provider)
  if (!provider) return
  try {
    const models = await listSmartLlmModels(provider.id)
    if (sequence === llmModelSequence && llmDialogOpen()) {
      const available = [...new Set(models)]
      llmModels.value = available
      form.modelName = [previousModel, provider.modelName].find(name => available.includes(name))
        || (available.length === 1 ? available[0] : '')
      if (!available.length) llmModelError.value = uiText('该提供商没有可用于标注的视觉模型。')
    }
  } catch {
    if (sequence === llmModelSequence && llmDialogOpen()) {
      llmModelError.value = uiText('读取模型目录失败，请刷新提供商列表后重试。')
    }
  } finally {
    if (sequence === llmModelSequence) llmModelsLoading.value = false
  }
}
const loadLlmProviders = async () => {
  const sequence = ++llmProviderSequence
  llmModelSequence++
  llmModelsLoading.value = false
  llmProvidersLoading.value = true
  llmProviderError.value = ''
  try {
    const providers = await listSmartLlmProviders()
    if (sequence !== llmProviderSequence || !llmDialogOpen()) return
    llmProviders.value = providers
    if (!providers.some(item => String(item.id) === form.providerId)) {
      form.providerId = providers.length === 1 ? String(providers[0].id) : ''
    }
    await changeLlmProvider()
  } catch (error) {
    if (sequence !== llmProviderSequence || !llmDialogOpen()) return
    llmProviders.value = []
    form.providerId = ''
    await changeLlmProvider()
    llmProviderError.value = error?.message || uiText('读取提供商列表失败，请刷新后重试。')
  } finally {
    if (sequence === llmProviderSequence) llmProvidersLoading.value = false
  }
}
let datasetSequence = 0
const searchDatasets = async keyword => {
  const sequence = ++datasetSequence
  datasetsLoading.value = true
  try {
    const request = { keyword, size: 100, includeGeneratedStatus: activeLearning.value }
    if (form.mode === 'llm') request.includeLlmAnnotationStatus = true
    const result = await listDataProjects(request)
    if (sequence === datasetSequence && creating.value) {
      datasets.value = form.mode === 'llm' ? result.records : result.records.filter(dataset => typeNames[dataset.annotationType])
    }
  } catch (error) {
    if (sequence === datasetSequence && creating.value) {
      datasets.value = []
      handleError(error)
    }
  } finally {
    if (sequence === datasetSequence) datasetsLoading.value = false
  }
}

let optionSequence = 0
const resetOptions = () => {
  options.value = null
  sourceKey.value = ''
  datasetNeedsGeneration.value = false
  datasetIssue.value = ''
  return ++optionSequence
}
const rejectDataset = reason => {
  form.datasetId = ''
  if (form.mode === 'llm') datasetIssue.value = reason
  else datasetNeedsGeneration.value = true
}
const rejectLlmDataset = (datasetId, status) => {
  datasets.value = datasets.value.map(dataset => String(dataset.id) === datasetId
    ? { ...dataset, llmAnnotationStatus: status } : dataset)
  rejectDataset(llmDatasetIssues[status])
}
const loadOptions = async () => {
  const sequence = resetOptions()
  const datasetId = form.datasetId
  if (!datasetId) return
  const selected = datasets.value.find(dataset => String(dataset.id) === datasetId)
  const reason = selected && datasetReason(selected)
  if (reason) {
    rejectDataset(reason)
    return
  }

  try {
    const result = await smartOptions(datasetId, activeLearning.value)
    if (sequence !== optionSequence || !creating.value) return
    if (activeLearning.value && result.datasetGenerated !== true) {
      datasets.value = datasets.value.map(dataset => String(dataset.id) === datasetId
        ? { ...dataset, datasetGenerated: result.datasetGenerated } : dataset)
      rejectDataset()
      if (datasetsLoading.value) await searchDatasets('')
      return
    }
    if (form.mode === 'llm' && (result.unlabeled === 0 || result.labelCounts?.length === 0)) {
      rejectLlmDataset(datasetId, result.labelCounts?.length === 0 ? 'MISSING_LABELS' : 'NO_UNLABELED_IMAGES')
      return
    }
    options.value = result
    if (activeLearning.value) sourceKey.value = 'system:0'
    if (!form.taskName) {
      const datasetName = result.datasetName || selected?.annotationName
      form.taskName = `${datasetName || uiText('数据集')} - ${uiText(form.mode === 'llm' ? '大模型标注' : '智能标注')}`
    }
  } catch (error) {
    if (sequence === optionSequence && creating.value) {
      if (form.mode === 'llm' && error?.message === '请先在标注页面创建需要识别的标签') {
        rejectLlmDataset(datasetId, 'MISSING_LABELS')
        return
      }
      if (activeLearning.value) form.datasetId = ''
      handleError(error)
    }
  }
}

const changeDataset = () => {
  return loadOptions()
}
const changeMode = async () => {
  datasets.value = []
  const sequence = resetOptions()
  const resourceRequest = form.mode === 'llm' ? loadLlmProviders() : loadCompute()
  await searchDatasets('')
  if (sequence === optionSequence && creating.value) await loadOptions()
  await resourceRequest
}

const openCreate = async () => {
  if (form.mode === 'llm') Object.assign(form, llmQualityDefaults)
  creating.value = true
  if (route.query.dataset) form.datasetId = String(route.query.dataset)
  await changeMode()
}

const create = () => run(async () => {
  if (!creating.value || !ready.value) return
  const { datasetId, taskName, mode } = form
  const common = { datasetId, taskName: taskName.trim(), mode }
  let payload
  if (mode === 'llm') {
    payload = { ...common, providerId: form.providerId, modelName: form.modelName, prompt: form.prompt.trim(),
      confidence: form.llmConfidence }
    if (llmQualitySupported.value) {
      payload.llmPilotSize = form.llmPilotEnabled ? form.llmPilotSize : 0
      payload.llmReviewPercent = form.llmReviewPercent
    }
  } else {
    await loadCompute()
    if (!selectedCompute.value?.schedulable) throw new Error(uiText('所选 GPU 设备尚未就绪，请刷新后重试'))
    if (!creating.value || !ready.value) return
    const [sourceType, sourceId] = sourceKey.value.split(':')
    payload = { ...common, computeNodeId: form.computeNodeId, confidence: form.confidence,
      epochs: form.epochs, reviewSize: form.reviewSize, sourceType, sourceId }
  }
  const result = await createSmartTask(payload)
  creating.value = false
  detail.value = await getSmartTask(result.id)
  resetPredictionQuery(detail.value)
  await router.replace({ query: { ...route.query, task: String(result.id) } })
  if (showPredictions(detail.value)) await loadPredictions()
  ElMessage.success(uiText('智能标注任务已入队'))
})
const openReview = row => run(async () => {
  if (!canReview(row)) return
  const { id } = detail.value.task
  const candidateId = row.candidate.id
  const controller = new AbortController()
  reviewRequest = controller
  try {
    const [result, blob] = await Promise.all([
      getSmartPrediction(id, candidateId, controller.signal),
      fetchSmartPredictionImage(id, candidateId, controller.signal)
    ])
    if (!controller.signal.aborted) reviewItem.value = { ...result, previewUrl: URL.createObjectURL(blob) }
  } catch (error) {
    if (controller.signal.aborted) return
    controller.abort()
    throw error
  }
})
const saveReview = request => run(async () => {
  if (detail.value?.task.taskState !== 'REVIEW' || !reviewItem.value) return
  await reviewSmartPrediction(detail.value.task.id, reviewItem.value.candidate.id, request)
  closeReview()
  await refresh()
  ElMessage.success(request.skip ? uiText('已跳过') : uiText('标注已确认并保存'))
})
const batchConfirm = () => run(async () => { await ElMessageBox.confirm(uiText('将本页 {value0} 张图片当前显示的预测结果保存为正式标注。请先检查预测效果。', { value0: confirmable.value.length }), uiText('批量确认'), { type: 'warning' }); await confirmSmartBatch(detail.value.task.id, confirmable.value.map(row => row.candidate.id)); await refresh() })
const confirmRoundPredictions = () => run(async () => {
  await ElMessageBox.confirm(
    uiText('将按审核页显示的结果校验本轮 {value0} 张待确认图片。通过校验的图片将保存为正式标注，其余图片可逐张审核。', { value0: detail.value.counts.PENDING || 0 }),
    uiText('确认本轮已通过标注'),
    { type: 'warning', confirmButtonText: uiText('保存通过校验的标注') }
  )
  await confirmAllSmartPredictions(detail.value.task.id)
  await refresh()
})
const perform = operation => run(async () => { await operation(detail.value.task.id); await refresh() })
const continuePilot = () => run(async () => {
  if (!pilotReview.value || !detail.value.canContinueLlm) return
  await continueSmartLlm(detail.value.task.id)
  clearPredictions()
  await refresh()
})
const openPilotRerun = () => {
  pilotPrompt.value = detail.value.task.llmPrompt || ''
  rerunningPilot.value = true
}
const rerunPilot = () => run(async () => {
  if (!pilotReview.value) return
  await rerunSmartLlmPilot(detail.value.task.id, { prompt: pilotPrompt.value.trim() })
  rerunningPilot.value = false
  clearPredictions()
  await refresh()
})
const retryLlm = () => run(async () => {
  await ElMessageBox.confirm(uiText('重试会重新调用当前任务的视觉模型，可能产生调用费用。已保存的标注会保留。'), uiText('重试失败图片'), { type: 'warning' })
  await retrySmartTask(detail.value.task.id)
  await refresh()
})
const next = () => run(async () => { await ElMessageBox.confirm(uiText('使用当前已确认标注训练下一轮模型，并重新筛选剩余图片。未确认结果不会用于训练。'), uiText('继续主动学习')); await nextSmartRound(detail.value.task.id); clearPredictions(); await refresh() })
const finish = () => run(async () => { await ElMessageBox.confirm(uiText('完成任务并保存数据集版本。未确认结果会保留在任务记录中，不会自动写入正式标注。'), uiText('完成智能标注')); await finishSmartTask(detail.value.task.id); await refresh() })
const cancel = () => run(async () => {
  await ElMessageBox.confirm(uiText(llmTask.value
    ? '取消会停止提交后续图片。已发出的模型请求可能仍会完成并产生费用，已确认标注会保留。'
    : '取消当前智能标注任务，已确认标注保留。运行中的本任务进程将停止，等待节点确认后释放使用权。'), uiText('取消智能标注'), { type: 'warning' })
  await cancelSmartTask(detail.value.task.id)
  await refresh()
})
let timer
onMounted(() => {
  timer = setInterval(() => { if (active.value && !busy.value && !reviewItem.value) refresh() }, 5000)
  if (route.query.task) void enter(String(route.query.task))
  else void refresh()
})
onBeforeUnmount(() => { disposed = true; computeSequence++; llmProviderSequence++; llmModelSequence++; clearInterval(timer); clearPredictions(); closeReview() })
</script>

<style scoped>
.preview-error{position:absolute;inset:0;display:grid;place-items:center;color:#d1e2f4;font-size:13px}
.smart-page{padding:24px;max-width:1600px;margin:auto}.smart-heading,.actions,.task-summary,.filters{display:flex;align-items:center;gap:14px;flex-wrap:wrap}.smart-heading{justify-content:space-between;margin-bottom:24px}.smart-heading h2{margin:8px 0}.smart-heading p,.hint{color:var(--el-text-color-secondary);font-size:13px}.mode-cards{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:20px}.mode-cards>div{padding:18px 22px;border:1px solid var(--el-border-color);border-radius:10px;background:var(--el-bg-color)}.mode-cards h3{margin-top:0}.mode-cards p{line-height:1.7;color:var(--el-text-color-secondary)}.el-table{margin-top:20px}.el-pagination{margin-top:20px;justify-content:flex-end}.task-summary{padding:18px;border:1px solid var(--el-border-color);border-radius:10px;margin-bottom:20px}.task-actions{margin:20px 0}.filters .el-select{width:160px}.prediction-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(210px,1fr));gap:18px}.prediction-card{overflow:hidden;border:1px solid var(--el-border-color);border-radius:9px;cursor:pointer;background:var(--el-bg-color)}.prediction-card .image{height:170px;position:relative;background:#142333}.prediction-card img{width:100%;height:100%;object-fit:contain}.hard-tag{position:absolute;left:8px;top:8px}.prediction-description{padding:12px;display:flex;flex-direction:column;gap:8px}.prediction-description strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.prediction-description span{font-size:12px;color:var(--el-text-color-secondary)}.round-history{margin-top:28px}.label-counts{display:flex;gap:8px;flex-wrap:wrap;margin-top:12px}@media(max-width:700px){.smart-page{padding:14px}.mode-cards{grid-template-columns:1fr}.prediction-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
.dataset-option {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}
.dataset-option-name {
  overflow: hidden;
  text-overflow: ellipsis;
}
.dataset-option-status {
  flex-shrink: 0;
  font-size: 12px;
}
.prediction-card {
  cursor: default;
}
.prediction-card.is-actionable {
  cursor: pointer;
}
.llm-field {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 0 10px;
  width: 100%;
}
.llm-field .el-select {
  flex: 1 1 210px;
  min-width: 0;
}
.llm-field-message {
  flex: 0 0 100%;
  line-height: 1.5;
  margin: 6px 0 0;
}
</style>
