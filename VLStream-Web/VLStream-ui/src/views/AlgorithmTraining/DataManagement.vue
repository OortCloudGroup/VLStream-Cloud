<template>
  <div class="data-page" v-loading="loading">
    <header class="page-heading">
      <div>
        <el-button v-if="project" link @click="back">← 返回项目</el-button>
        <h2>{{ project ? project.annotationName : '数据和样本管理' }}</h2>
        <p>{{ project ? `${project.projectCode || '历史项目'} · ${project.projectType || 'general'} · ${typeName(project.annotationType)}` : '管理原始素材、审核样本质量，并保留每一次数据集调整。' }}</p>
      </div>
      <div class="actions">
        <template v-if="project">
          <el-button @click="openProject(project)">项目设置</el-button>
          <el-button @click="annotate">进入标注</el-button>
          <el-button type="primary" @click="openImport">导入样本</el-button>
        </template>
        <el-button v-else type="primary" @click="openProject()">新建项目</el-button>
      </div>
    </header>

    <template v-if="!project">
      <div class="filters"><el-input v-model="projectQuery.keyword" clearable placeholder="项目名称或编号" @keyup.enter="loadProjects" @clear="loadProjects" /><el-button @click="loadProjects">搜索</el-button></div>
      <el-table :data="projects" stripe empty-text="暂无项目，点击右上角新建项目开始导入数据">
        <el-table-column prop="annotationName" label="项目名称" min-width="180"><template #default="{ row }"><el-link type="primary" @click="enter(row)">{{ row.annotationName }}</el-link></template></el-table-column>
        <el-table-column prop="projectCode" label="项目编号" min-width="150" />
        <el-table-column prop="projectType" label="项目类型" width="130" />
        <el-table-column label="标注配置" width="160"><template #default="{ row }">{{ typeName(row.annotationType) }}</template></el-table-column>
        <el-table-column prop="remark" label="项目说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160"><template #default="{ row }"><el-button link type="primary" @click="enter(row)">管理数据</el-button><el-button link @click="openProject(row)">编辑</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="projectQuery.page" :page-size="20" :total="projectTotal" layout="total, prev, pager, next" @current-change="loadProjects" />
    </template>

    <template v-else>
      <div class="stat-grid">
        <div v-for="item in summaryCards" :key="item.label" class="stat-card"><span>{{ item.label }}</span><strong>{{ item.value }}</strong></div>
      </div>
      <el-tabs v-model="tab">
        <el-tab-pane label="样本管理" name="samples">
          <div class="filters sample-filters">
            <el-input v-model="query.keyword" clearable placeholder="样本名称" @keyup.enter="search" />
            <el-input v-model="query.tag" clearable placeholder="标签或标注类别（精确匹配）" @keyup.enter="search" />
            <el-select v-model="query.source" clearable placeholder="来源"><el-option v-for="source in stats.sources || []" :key="source" :label="source" :value="source" /></el-select>
            <el-select v-model="query.mediaType" clearable placeholder="文件类型"><el-option label="图片" value="image" /><el-option label="视频" value="video" /></el-select>
            <el-select v-model="query.qualityStatus" clearable placeholder="质量状态"><el-option v-for="(label, value) in qualityNames" :key="value" :label="label" :value="value" /></el-select>
            <el-select v-model="query.annotationStatus" clearable placeholder="标注状态"><el-option label="已标注" value="annotated" /><el-option label="未标注" value="unannotated" /></el-select>
            <el-select v-model="query.datasetSplit" clearable placeholder="所属数据集"><el-option v-for="(label, value) in splitNames" :key="value" :label="label" :value="value" /></el-select>
            <el-button type="primary" @click="search">查询</el-button><el-button @click="resetSearch">重置</el-button>
          </div>
          <div class="sample-toolbar actions">
            <span>已选 {{ selected.length }} 项</span>
            <el-button :disabled="!selected.length || busy" @click="checkSelected">质量检查</el-button>
            <el-button :disabled="busy" @click="startReview">随机抽检</el-button>
            <el-dropdown :disabled="!selected.length || busy" @command="batchAction"><el-button :disabled="!selected.length || busy">批量操作 ▾</el-button><template #dropdown><el-dropdown-menu>
              <el-dropdown-item command="accepted">标记合格</el-dropdown-item><el-dropdown-item command="low">标记低质量</el-dropdown-item>
              <el-dropdown-item command="excluded">剔除异常样本</el-dropdown-item><el-dropdown-item command="pending">恢复待审核</el-dropdown-item>
              <el-dropdown-item command="delete" divided>删除样本</el-dropdown-item>
            </el-dropdown-menu></template></el-dropdown>
            <el-button :disabled="busy" @click="downloadSamples">{{ selected.length ? '导出已选' : '导出筛选结果' }}</el-button>
          </div>
          <el-table :data="samples" row-key="id" stripe @selection-change="selected = $event" empty-text="暂无匹配样本，可调整筛选条件或导入图片、视频">
            <el-table-column type="selection" width="45" />
            <el-table-column label="样本" min-width="230"><template #default="{ row }"><div class="sample-name">
              <el-image v-if="row.mediaType === 'image' && row.previewUrl" :src="row.previewUrl" fit="cover" @click="showSample(row)"><template #error><span class="media-icon">图片</span></template></el-image>
              <span v-else class="media-icon">{{ row.mediaType === 'video' ? '视频' : '图片' }}</span>
              <div><el-link type="primary" @click="showSample(row)">{{ row.imageName }}</el-link><small>{{ fileSize(row.fileSize) }} <template v-if="row.mediaWidth"> · {{ row.mediaWidth }}×{{ row.mediaHeight }}</template></small></div>
            </div></template></el-table-column>
            <el-table-column prop="sampleSource" label="来源" width="110" show-overflow-tooltip />
            <el-table-column label="标签 / 类别" min-width="160"><template #default="{ row }"><el-tag v-for="label in [...new Set([...(row.tags || []), ...(row.labels || [])])]" :key="label" size="small" class="tag">{{ label }}</el-tag><span v-if="!row.tags?.length && !row.labels?.length">—</span></template></el-table-column>
            <el-table-column label="质量" min-width="150"><template #default="{ row }"><el-tag :type="qualityColor(row.qualityStatus)">{{ qualityNames[row.qualityStatus] || '待审核' }}</el-tag><small class="issue" :title="row.qualityIssues">{{ row.qualityIssues || '尚无检查异常' }}</small></template></el-table-column>
            <el-table-column label="标注" width="100"><template #default="{ row }">{{ row.annotationCount ? `${row.annotationCount} 个标注` : '未标注' }}</template></el-table-column>
            <el-table-column label="数据集" width="100"><template #default="{ row }">{{ splitNames[row.datasetSplit] || '未划分' }}</template></el-table-column>
            <el-table-column label="操作" width="130" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="showSample(row)">查看 / 编辑</el-button></template></el-table-column>
          </el-table>
          <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[20, 50, 100]" :total="sampleTotal" layout="total, sizes, prev, pager, next" @current-change="run(loadSamples)" @size-change="search" />
        </el-tab-pane>
        <el-tab-pane label="数据集划分与版本" name="versions">
          <div class="dataset-intro"><div><h3>训练集与验证集</h3><p>仅划分已有标注、未标记低质量或剔除的图片。视频保留为原始素材；相同文件内容不会分入两个集合。</p></div><div class="actions">
            <el-button type="primary" :disabled="busy" @click="openSplit">划分数据集</el-button><el-button :disabled="busy" @click="versionDialog = true">保存当前版本</el-button>
            <el-button :disabled="busy" @click="publish">生成训练目录</el-button>
          </div></div>
          <el-table :data="stats.distribution || []" empty-text="尚无标注类别，请先在标注页面维护标签和标注">
            <el-table-column prop="name" label="类别" /><el-table-column prop="total" label="样本数" /><el-table-column prop="train" label="训练集" /><el-table-column prop="val" label="验证集" />
          </el-table>
          <p class="hint">多类别图片会在对应类别分别计数；分层划分按首个类别分组，小类别的实际比例可能有取整差异。</p>
          <h3>版本记录</h3>
          <el-table :data="versions" stripe empty-text="暂无版本，可保存当前状态或划分数据集自动生成版本">
            <el-table-column label="版本" width="80"><template #default="{ row }">V{{ row.versionNumber }}</template></el-table-column>
            <el-table-column prop="versionName" label="名称" min-width="160" /><el-table-column prop="sampleCount" label="样本" width="80" />
            <el-table-column prop="trainCount" label="训练" width="80" /><el-table-column prop="validationCount" label="验证" width="80" />
            <el-table-column prop="createTime" label="创建时间" width="170" /><el-table-column prop="description" label="说明" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" width="245"><template #default="{ row }"><el-button link type="primary" @click="viewVersion(row)">查看</el-button><el-button link @click="openCompare(row)">对比</el-button><el-button link @click="downloadVersion(row)">导出</el-button><el-button link type="warning" :disabled="busy" @click="restore(row)">回退</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </template>

    <el-dialog v-model="projectDialog" :title="projectForm.id ? '编辑项目' : '新建项目'" width="620px" :close-on-click-modal="false">
      <el-form label-width="100px" @submit.prevent>
        <el-form-item label="项目名称" required><el-input v-model="projectForm.annotationName" maxlength="100" /></el-form-item>
        <el-form-item label="项目编号" required><el-input v-model="projectForm.projectCode" maxlength="64" placeholder="字母、数字、下划线或短横线，租户内唯一" /></el-form-item>
        <el-form-item label="项目类型" required><el-select v-model="projectForm.projectType" allow-create filterable><el-option v-for="value in ['general', '安防', '工业', '交通', '零售']" :key="value" :value="value" :label="value === 'general' ? '通用' : value" /></el-select></el-form-item>
        <el-form-item label="标注类型" required><el-select v-model="projectForm.annotationType"><el-option v-for="(label, value) in annotationTypes" :key="value" :label="label" :value="value" /></el-select></el-form-item>
        <el-form-item v-if="projectForm.createTime" label="创建时间">{{ projectForm.createTime }}</el-form-item>
        <el-form-item label="项目说明"><el-input v-model="projectForm.remark" type="textarea" :rows="3" maxlength="1000" show-word-limit /></el-form-item>
        <el-form-item label="标注规则"><el-input v-model="projectForm.annotationRules" type="textarea" :rows="3" maxlength="4000" placeholder="说明应标注的对象、边界和排除规则" /></el-form-item>
      </el-form><template #footer><el-button @click="projectDialog = false">取消</el-button><el-button type="primary" :loading="busy" @click="saveProject">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="importDialog" title="批量导入样本" width="720px" :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy">
      <el-radio-group v-model="importMode" :disabled="busy" @change="importFiles = []; importResults = []"><el-radio-button label="files">图片 / 视频</el-radio-button><el-radio-button label="archive">ZIP 归档</el-radio-button></el-radio-group>
      <p class="hint">{{ importMode === 'files' ? '支持 JPG、PNG、BMP（25 MB 内）、MP4、MOV（100 MB 内），每批最多 100 个文件。相同内容自动去重，逐文件显示结果。' : '支持本页导出的 ZIP（含标签与标注）或仅含原始图片/视频的 ZIP。压缩包 500 MB 内，解压后 1 GB 内，最多 500 个样本。YOLO ZIP 请从“进入标注”导入。' }}</p>
      <el-input v-if="importMode === 'files'" v-model="importSource" placeholder="样本来源，例如：产线 A / 设备采集" maxlength="128" class="source-input" />
      <el-upload v-model:file-list="importFiles" drag :auto-upload="false" :multiple="importMode === 'files'" :limit="importMode === 'files' ? 100 : 1" :disabled="busy" :accept="importMode === 'files' ? '.jpg,.jpeg,.png,.bmp,.mp4,.mov' : '.zip'" @exceed="warn('已超过本批文件数量限制')"><div>拖入文件，或点击选择文件</div></el-upload>
      <el-progress v-if="busy && importFiles.length" :percentage="importProgress" />
      <el-table v-if="importResults.length" :data="importResults" max-height="220"><el-table-column prop="filename" label="文件" /><el-table-column label="结果"><template #default="{ row }"><span :class="row.success ? '' : 'error-text'">{{ row.success ? (row.duplicate ? '已存在，跳过重复文件' : '导入成功') : row.message }}</span></template></el-table-column></el-table>
      <template #footer><el-button :disabled="busy" @click="importDialog = false">关闭</el-button><el-button type="primary" :loading="busy" :disabled="!importFiles.length" @click="upload">开始导入</el-button></template>
    </el-dialog>

    <el-dialog v-model="sampleDialog" :title="reviewItems.length ? `人工抽检 ${reviewIndex + 1} / ${reviewItems.length}` : '样本详情与编辑'" width="960px" destroy-on-close :close-on-click-modal="false">
      <template v-if="activeSample"><div class="sample-detail"><div class="preview">
        <video v-if="activeSample.mediaType === 'video' && activeSample.previewUrl" :src="activeSample.previewUrl" controls preload="metadata" />
        <el-image v-else-if="activeSample.previewUrl" :src="activeSample.previewUrl" fit="contain" :preview-src-list="[activeSample.previewUrl]" preview-teleported />
        <el-empty v-else :description="activeSample.previewError || '文件暂时无法预览'" />
      </div><div class="sample-fields"><el-form label-position="top">
        <el-form-item label="样本名称"><el-input v-model="sampleForm.imageName" maxlength="200" /></el-form-item>
        <el-form-item label="来源"><el-input v-model="sampleForm.sampleSource" maxlength="128" /></el-form-item>
        <el-form-item label="样本标签"><el-select v-model="sampleForm.tags" multiple filterable allow-create default-first-option :reserve-keyword="false"><el-option v-for="label in sampleForm.tags" :key="label" :value="label" /></el-select></el-form-item>
      </el-form><p>状态：<el-tag :type="qualityColor(activeSample.qualityStatus)">{{ qualityNames[activeSample.qualityStatus] }}</el-tag> · {{ activeSample.annotationCount }} 个标注</p>
      <p class="hint">样本 ID：{{ activeSample.id }}<br />检查提示：{{ activeSample.qualityIssues || '无' }}<br />清晰度参考值：{{ activeSample.focusScore ?? '未检查' }}<br />检查时间：{{ activeSample.qualityCheckedAt || '未检查' }}</p>
      <el-input v-model="reviewNote" type="textarea" :rows="2" maxlength="1000" placeholder="人工抽检结论 / 低质量原因" />
      <div class="actions review-actions"><el-button size="small" :disabled="busy" @click="reviewAction('accepted')">合格</el-button><el-button size="small" type="warning" :disabled="busy" @click="reviewAction('low')">低质量</el-button><el-button size="small" type="danger" :disabled="busy" @click="reviewAction('excluded')">剔除</el-button><el-button size="small" :disabled="busy" @click="reviewAction('pending')">待审核</el-button></div>
      </div></div>
      <el-collapse v-if="activeSample.instances?.length"><el-collapse-item title="查看标注内容"><pre>{{ JSON.stringify(activeSample.instances.map(item => ({ labelId: item.labelId, type: item.annotationType, data: item.annotationData })), null, 2) }}</pre></el-collapse-item></el-collapse>
      </template><template #footer><el-button v-if="reviewItems.length" :disabled="reviewIndex === 0 || busy" @click="reviewMove(-1)">上一条</el-button><el-button v-if="reviewItems.length" :disabled="reviewIndex >= reviewItems.length - 1 || busy" @click="reviewMove(1)">下一条</el-button><el-button v-if="activeSample?.mediaType === 'image'" @click="annotate">编辑标注</el-button><el-button :loading="busy" type="primary" @click="saveSample">保存信息</el-button></template>
    </el-dialog>

    <el-dialog v-model="splitDialog" title="划分训练集和验证集" width="620px" :close-on-click-modal="false">
      <el-alert type="info" :closable="false" title="每张图片只进入一个集合。保存后自动生成版本；已有划分会先自动备份。" />
      <el-form label-width="105px" class="dialog-form"><el-form-item label="划分方式"><el-select v-model="splitForm.mode"><el-option label="按比例随机划分" value="random" /><el-option label="按类别分层划分" value="stratified" /><el-option label="手动指定验证样本" value="manual" /></el-select></el-form-item>
        <template v-if="splitForm.mode !== 'manual'"><el-form-item label="训练集比例"><el-slider v-model="splitForm.trainPercent" :min="1" :max="99" show-input /><span>验证集 {{ 100 - splitForm.trainPercent }}%</span></el-form-item><el-form-item label="随机种子"><el-input-number v-model="splitForm.seed" :min="0" :max="2147483647" /></el-form-item></template>
        <el-form-item v-else label="验证样本 ID"><el-input v-model="manualIds" type="textarea" :rows="4" placeholder="输入样本 ID，逗号或换行分隔；其余合格的已标注图片进入训练集" /><el-button link type="primary" @click="manualIds = selected.map(item => item.id).join('\n')">使用样本列表中已选的 {{ selected.length }} 项</el-button></el-form-item>
        <el-form-item label="版本名称"><el-input v-model="splitForm.versionName" maxlength="100" /></el-form-item>
      </el-form><template #footer><el-button @click="splitDialog = false">取消</el-button><el-button type="primary" :loading="busy" @click="saveSplit">保存划分</el-button></template>
    </el-dialog>
    <el-dialog v-model="versionDialog" title="保存数据集版本" width="520px"><el-form label-width="85px"><el-form-item label="版本名称"><el-input v-model="versionForm.name" maxlength="100" /></el-form-item><el-form-item label="说明"><el-input v-model="versionForm.description" type="textarea" maxlength="1000" /></el-form-item></el-form><p class="hint">保留当前样本、标签、标注、质量状态与训练/验证划分。</p><template #footer><el-button type="primary" :loading="busy" @click="saveVersion">保存版本</el-button></template></el-dialog>
    <el-dialog v-model="versionDetailDialog" title="版本详情" width="850px"><template v-if="versionDetail"><h3>V{{ versionDetail.version.versionNumber }} · {{ versionDetail.version.versionName }}</h3><p>{{ versionDetail.version.description }}</p><p>样本 {{ versionDetail.version.sampleCount }} · 训练 {{ versionDetail.version.trainCount }} · 验证 {{ versionDetail.version.validationCount }}</p><el-table :data="versionDetail.statistics.distribution"><el-table-column prop="name" label="类别" /><el-table-column prop="total" label="样本" /><el-table-column prop="train" label="训练" /><el-table-column prop="val" label="验证" /></el-table><el-table :data="versionDetail.samples" max-height="350"><el-table-column prop="id" label="样本 ID" min-width="190" /><el-table-column prop="name" label="名称" /><el-table-column label="划分"><template #default="{ row }">{{ splitNames[row.split] }}</template></el-table-column><el-table-column label="质量"><template #default="{ row }">{{ qualityNames[row.quality] }}</template></el-table-column></el-table></template></el-dialog>
    <el-dialog v-model="compareDialog" title="版本对比" width="860px"><div class="actions"><span>V{{ compareFrom?.versionNumber }} →</span><el-select v-model="compareTo" @change="run(loadComparison)"><el-option label="当前工作数据" value="current" /><el-option v-for="item in versions.filter(item => item.id !== compareFrom?.id)" :key="item.id" :value="item.id" :label="`V${item.versionNumber} · ${item.versionName}`" /></el-select></div><template v-if="comparison"><p>新增 {{ comparison.added.length }} · 移除 {{ comparison.removed.length }} · 修改 {{ comparison.changed.length }} · 标注规则{{ comparison.configurationChanged ? '已变化' : '未变化' }} · 类别配置{{ comparison.labelsChanged ? '已变化' : '未变化' }}</p><el-table :data="comparisonRows" max-height="450" empty-text="样本和标注内容一致"><el-table-column prop="kind" label="变化" width="80" /><el-table-column prop="name" label="样本" /><el-table-column prop="id" label="ID" min-width="180" /><el-table-column label="变化内容"><template #default="{ row }">{{ row.fields?.join('、') || '样本成员变化' }}</template></el-table-column></el-table></template></el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/dataManagement'

const route = useRoute(), router = useRouter()
const loading = ref(false), busy = ref(false), project = ref(null), projects = ref([]), projectTotal = ref(0), tab = ref('samples')
const projectQuery = reactive({ keyword: '', page: 1 })
const samples = ref([]), selected = ref([]), sampleTotal = ref(0), stats = ref({}), versions = ref([])
const emptyQuery = () => ({ keyword: '', tag: '', source: '', mediaType: '', qualityStatus: '', annotationStatus: '', datasetSplit: '', page: 1, size: 20 })
const query = reactive(emptyQuery())
const annotationTypes = { object_detection: '物体检测', image_classification: '图像分类', instance_segmentation: '实例分割', semantic_segmentation: '语义分割' }
const qualityNames = { pending: '待审核', accepted: '合格', low: '低质量', excluded: '已剔除' }
const splitNames = { unassigned: '未划分', train: '训练集', val: '验证集' }
const typeName = value => annotationTypes[value] || value
const qualityColor = value => ({ accepted: 'success', low: 'warning', excluded: 'danger', pending: 'info' })[value] || 'info'
const warn = text => ElMessage.warning(text)
const fileSize = bytes => !bytes ? '0 B' : bytes >= 1048576 ? `${(bytes / 1048576).toFixed(1)} MB` : `${(bytes / 1024).toFixed(1)} KB`
const summaryCards = computed(() => [{ label: '全部样本', value: stats.value.total || 0 }, { label: '图片 / 视频', value: `${stats.value.images || 0} / ${stats.value.videos || 0}` }, { label: '可划分图片', value: stats.value.eligible || 0 }, { label: '待审核', value: stats.value.quality?.pending || 0 }, { label: '训练 / 验证', value: `${stats.value.splits?.train || 0} / ${stats.value.splits?.val || 0}` }])
const run = async task => { if (busy.value) return; busy.value = true; try { return await task() } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '操作失败，请重试') } finally { busy.value = false } }
const loadProjects = async () => { loading.value = true; try { const result = await api.listDataProjects(projectQuery); projects.value = result.records; projectTotal.value = result.total } catch (error) { ElMessage.error(error.message) } finally { loading.value = false } }
const loadSamples = async () => { const result = await api.listDataSamples(project.value.id, query); samples.value = result.records; sampleTotal.value = result.total; selected.value = [] }
const refresh = async () => { const id = project.value.id; const [summary, history, current] = await Promise.all([api.dataStatistics(id), api.listDataVersions(id), api.getDataProject(id)]); stats.value = summary; versions.value = history; project.value = current; await loadSamples() }
const enter = row => run(async () => { project.value = row; Object.assign(query, emptyQuery()); await router.replace({ query: { project: row.id } }); await refresh() })
const back = async () => { project.value = null; await router.replace({ query: {} }); await loadProjects() }
const search = () => run(async () => { query.page = 1; await loadSamples() })
const resetSearch = () => { Object.assign(query, emptyQuery()); search() }
const annotate = () => router.push({ path: '/algorithm-standard', query: { annotationId: project.value.id } })

const projectDialog = ref(false), projectForm = reactive({})
const openProject = (row = {}) => { Object.keys(projectForm).forEach(key => delete projectForm[key]); Object.assign(projectForm, { annotationName: '', projectCode: `PRJ-${Date.now()}`, projectType: 'general', annotationType: 'object_detection', remark: '', annotationRules: '' }, row); projectDialog.value = true }
const saveProject = () => run(async () => {
  if (!projectForm.annotationName?.trim() || !/^[A-Za-z0-9_-]{1,64}$/.test(projectForm.projectCode) || !projectForm.projectType?.trim()) return warn('填写名称、类型和有效项目编号')
  const saved = await api.saveDataProject(projectForm.id, projectForm); projectDialog.value = false; ElMessage.success('项目已保存')
  if (project.value) { project.value = saved; await refresh() } else await loadProjects()
})

const importDialog = ref(false), importMode = ref('files'), importSource = ref('upload'), importFiles = ref([]), importResults = ref([]), importProgress = ref(0)
const openImport = () => { importFiles.value = []; importResults.value = []; importProgress.value = 0; importDialog.value = true }
const upload = () => run(async () => {
  if (!importSource.value.trim()) return warn('请填写样本来源')
  importResults.value = []; importProgress.value = 0
  const files = importFiles.value.map(file => file.raw)
  for (let index = 0; index < files.length; index++) {
    try { const result = await api.importDataSamples(project.value.id, [files[index]], importSource.value, importMode.value === 'archive'); importResults.value.push(...result.results) }
    catch (error) { importResults.value.push({ filename: files[index].name, success: false, message: error.message }) }
    importProgress.value = Math.round((index + 1) * 100 / files.length)
  }
  await refresh()
  const failed = importResults.value.filter(item => !item.success).length
  if (failed) warn(`导入结束，${failed} 个文件失败，请查看结果后重试`); else ElMessage.success('导入完成')
})
const batchAction = action => run(async () => {
  const { value } = await ElMessageBox.prompt(action === 'delete' ? '删除选中样本及标注。已有版本仍保留，可通过版本回退恢复。请填写备注。' : '填写本次质量审核或剔除原因（可留空）', action === 'delete' ? '删除样本' : qualityNames[action], { inputType: 'textarea', confirmButtonText: '确认', cancelButtonText: '取消', inputValidator: text => !text || text.length <= 1000 || '备注不能超过 1000 字符' })
  await api.batchDataSamples(project.value.id, { ids: selected.value.map(item => item.id), action, note: value || '' }); await refresh(); ElMessage.success('批量操作完成')
})
const checkSelected = () => run(async () => { if (selected.value.length > 100) return warn('每次最多检查 100 项'); await api.checkDataSamples(project.value.id, selected.value.map(item => item.id)); await refresh(); ElMessage.success('检查完成，请查看质量提示') })
const download = async (versionId, params) => {
  const blob = await api.exportDataSamples(project.value.id, params, versionId)
  if (!(blob instanceof Blob) || blob.type.includes('json')) { const error = blob instanceof Blob ? JSON.parse(await blob.text()) : blob; throw new Error(error.msg || error.message || '导出失败') }
  const url = URL.createObjectURL(blob), link = document.createElement('a'); link.href = url; link.download = `${project.value.projectCode || 'samples'}${versionId ? `-${versionId}` : ''}.zip`; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000)
}
const downloadSamples = () => run(() => download(null, { ...query, ids: selected.value.length ? selected.value.map(item => item.id).join(',') : undefined }))
const downloadVersion = version => run(() => download(version.id))

const sampleDialog = ref(false), activeSample = ref(null), sampleForm = reactive({}), reviewNote = ref(''), reviewItems = ref([]), reviewIndex = ref(0)
const setSample = sample => { activeSample.value = sample; Object.assign(sampleForm, { imageName: sample.imageName, sampleSource: sample.sampleSource || 'upload', tags: [...(sample.tags || [])] }); reviewNote.value = sample.qualityNote || ''; sampleDialog.value = true }
const showSample = row => run(async () => { reviewItems.value = []; setSample(await api.getDataSample(project.value.id, row.id)) })
const saveSample = () => run(async () => { if (!sampleForm.imageName?.trim() || !sampleForm.sampleSource?.trim()) return warn('名称和来源不能为空'); await api.editDataSample(project.value.id, activeSample.value.id, sampleForm); await refresh(); ElMessage.success('样本信息已保存') })
const startReview = () => run(async () => { reviewItems.value = await api.reviewDataSamples(project.value.id, 10); reviewIndex.value = 0; if (!reviewItems.value.length) return warn('没有可抽检样本'); setSample(reviewItems.value[0]) })
const reviewMove = delta => run(async () => { reviewIndex.value += delta; setSample(await api.getDataSample(project.value.id, reviewItems.value[reviewIndex.value].id)) })
const reviewAction = action => run(async () => { await api.batchDataSamples(project.value.id, { ids: [activeSample.value.id], action, note: reviewNote.value }); activeSample.value = await api.getDataSample(project.value.id, activeSample.value.id); await refresh(); ElMessage.success('审核结论已保存') })

const splitDialog = ref(false), manualIds = ref(''), splitForm = reactive({ mode: 'random', trainPercent: 80, seed: 42, versionName: '数据集划分' })
const openSplit = () => { splitDialog.value = true }
const saveSplit = () => run(async () => { if (!splitForm.versionName.trim()) return warn('填写版本名称'); const ids = manualIds.value.split(/[\s,，]+/).filter(Boolean); if (splitForm.mode === 'manual' && (!ids.length || ids.some(id => !/^\d+$/.test(id)))) return warn('请填写有效的验证样本 ID'); await api.splitDataSamples(project.value.id, { ...splitForm, validationIds: ids }); splitDialog.value = false; await refresh(); ElMessage.success('已保存划分并生成版本') })
const versionDialog = ref(false), versionForm = reactive({ name: '', description: '' })
const saveVersion = () => run(async () => { if (!versionForm.name.trim()) return warn('填写版本名称'); await api.saveDataVersion(project.value.id, versionForm); versionDialog.value = false; await refresh(); ElMessage.success('版本已保存') })
const publish = () => run(async () => { await ElMessageBox.confirm('将当前划分生成独立的训练目录。需要对象存储和 GPU 服务器可用，未划分时会默认按 80%/20% 划分。', '生成训练数据集', { confirmButtonText: '生成', cancelButtonText: '取消' }); const result = await api.publishDataTraining(project.value.id); if (result.code !== 200 || result.success === false) throw new Error(result.msg || '生成失败'); await refresh(); ElMessage.success('训练目录已生成，可在算法训练中选择此数据集') })
const restore = row => run(async () => { await ElMessageBox.confirm(`将恢复 V${row.versionNumber} 的样本、标注、质量状态和划分。当前状态会先自动保存为新版本，已有训练任务不会修改。`, '回退数据集', { type: 'warning', confirmButtonText: '备份并回退', cancelButtonText: '取消' }); await api.restoreDataVersion(project.value.id, row.id); await refresh(); ElMessage.success('已恢复，并保留回退前的版本') })
const versionDetailDialog = ref(false), versionDetail = ref(null)
const viewVersion = row => run(async () => { versionDetail.value = await api.getDataVersion(project.value.id, row.id); versionDetailDialog.value = true })
const compareDialog = ref(false), compareFrom = ref(null), compareTo = ref('current'), comparison = ref(null)
const loadComparison = async () => { comparison.value = await api.compareDataVersion(project.value.id, compareFrom.value.id, compareTo.value === 'current' ? undefined : compareTo.value) }
const openCompare = row => run(async () => { compareFrom.value = row; compareTo.value = 'current'; comparison.value = null; await loadComparison(); compareDialog.value = true })
const comparisonRows = computed(() => comparison.value ? [...comparison.value.added.map(item => ({ ...item, kind: '新增' })), ...comparison.value.removed.map(item => ({ ...item, kind: '移除' })), ...comparison.value.changed.map(item => ({ ...item, kind: '修改' }))] : [])

onMounted(async () => { if (route.query.project && /^\d+$/.test(String(route.query.project))) await run(async () => { project.value = await api.getDataProject(route.query.project); await refresh() }); else await loadProjects() })
</script>

<style scoped>
.data-page { padding: 24px; min-height: 100%; background: #fff; color: #303133; overflow: auto; }
.page-heading, .dataset-intro { display: flex; justify-content: space-between; align-items: center; gap: 20px; margin-bottom: 20px; }
h2 { font-size: 22px; margin: 8px 0; } h3 { font-size: 16px; margin: 20px 0 12px; }
.page-heading p, .dataset-intro p, .hint { color: #7c8594; font-size: 13px; line-height: 1.7; }
.actions, .filters { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
.actions .el-button + .el-button { margin-left: 0; }
.filters { margin-bottom: 18px; } .filters .el-input { width: 230px; } .sample-filters .el-input { width: 200px; } .filters .el-select { width: 135px; }
.stat-grid { display: grid; grid-template-columns: repeat(5, minmax(120px, 1fr)); gap: 14px; margin-bottom: 20px; }
.stat-card { padding: 17px 20px; border: 1px solid #e8ecf2; background: #f9fafc; border-radius: 8px; }
.stat-card span { display: block; color: #7c8594; font-size: 13px; } .stat-card strong { display: block; margin-top: 10px; font-size: 25px; font-weight: 600; }
.sample-toolbar { padding: 10px 0 16px; } .sample-toolbar > span { color: #909399; font-size: 13px; }
.sample-name { display: flex; align-items: center; gap: 12px; } .sample-name .el-image, .media-icon { width: 48px; height: 44px; flex-shrink: 0; border-radius: 5px; }
.media-icon { display: inline-flex; align-items: center; justify-content: center; background: #edf2fa; color: #667da2; font-size: 12px; }
small { display: block; color: #909399; margin-top: 4px; } .issue { overflow: hidden; max-width: 190px; text-overflow: ellipsis; white-space: nowrap; }
.tag { margin: 2px 4px 2px 0; } .el-pagination { margin-top: 20px; justify-content: flex-end; }
.source-input { margin-bottom: 15px; } .error-text { color: #d9534f; }
.sample-detail { display: grid; grid-template-columns: 1.2fr 1fr; gap: 24px; } .preview { min-height: 320px; background: #f3f5f8; display: flex; align-items: center; justify-content: center; border-radius: 8px; }
.preview .el-image, .preview video { width: 100%; max-height: 440px; } .sample-fields .el-select { width: 100%; } .review-actions { margin-top: 14px; }
.dialog-form { margin-top: 24px; } pre { white-space: pre-wrap; max-height: 200px; overflow: auto; font-size: 12px; }
@media (max-width: 1000px) { .stat-grid { grid-template-columns: repeat(3, 1fr); } .page-heading, .dataset-intro { align-items: flex-start; flex-direction: column; } }
</style>
