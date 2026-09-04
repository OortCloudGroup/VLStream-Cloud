<template>
  <section class="catalog-shell">
    <header class="catalog-header">
      <el-tabs v-model="section" class="catalog-tabs">
        <el-tab-pane label="算法库" name="library" />
        <el-tab-pane label="算法分类" name="categories" />
      </el-tabs>
      <a class="model-hub" href="https://vls.oortcloudsmart.com/zh/ModelHub/ModelHub" target="_blank" rel="noopener noreferrer">
        <el-icon><TopRight /></el-icon> Model Hub
      </a>
    </header>

    <div class="catalog-body" :class="{ 'flat-layout': section === 'library' && viewMode === 'flat' }">
      <aside v-if="section !== 'library' || viewMode !== 'flat'" class="category-aside">
        <h3>所有分类</h3>
        <el-input v-model="treeKeyword" clearable placeholder="搜索分类" :prefix-icon="Search" />
        <el-tree ref="treeRef" :data="treeData" node-key="id" default-expand-all highlight-current
                 :expand-on-click-node="false" :filter-node-method="filterTree" @node-click="selectCategory">
          <template #default="{ data }">
            <span class="tree-node">
              <span class="node-label"><el-icon><Folder /></el-icon>{{ data.name }}</span>
              <span v-if="section === 'categories' && data.id !== ROOT_ID" class="node-actions" @click.stop>
                <el-button link :icon="Plus" @click="openCreate(data.id)" />
                <el-dropdown trigger="click" @command="command => handleNodeCommand(command, data)">
                  <el-button link :icon="MoreFilled" />
                  <template #dropdown><el-dropdown-menu>
                    <el-dropdown-item command="edit"><el-icon><Edit /></el-icon>编辑</el-dropdown-item>
                    <el-dropdown-item command="settings"><el-icon><Setting /></el-icon>设置</el-dropdown-item>
                    <el-dropdown-item command="delete" class="danger-item"><el-icon><Delete /></el-icon>删除</el-dropdown-item>
                  </el-dropdown-menu></template>
                </el-dropdown>
              </span>
            </span>
          </template>
        </el-tree>
      </aside>

      <main class="category-main">
        <nav v-if="section === 'library' && viewMode === 'flat'" class="flat-navigation">
          <div v-for="group in flatGroups" :key="group.id" class="flat-row">
            <span class="flat-parent" @click="selectFlat(group.id)">{{ group.name }}</span>
            <span v-for="child in flatChildren(group)" :key="child.id" class="flat-child" @click="selectFlat(child.id)">/　{{ child.name }}</span>
          </div>
        </nav>

        <div class="toolbar">
          <div class="toolbar-primary">
            <el-button v-if="section === 'library'" type="primary" round :icon="Plus" @click="emit('add', activeCategoryId)">新增算法</el-button>
            <template v-else>
              <el-button type="primary" round :icon="Plus" @click="openCreate(activeCategoryId)">添加分类</el-button>
              <el-button round :icon="Delete" :disabled="!selectedRows.length" @click="removeCategories(selectedRows.map(row => row.id))">删除</el-button>
            </template>
          </div>
          <div class="toolbar-search">
            <el-input v-model="keyword" clearable placeholder="搜索" :prefix-icon="Search" />
            <el-button link :icon="Upload" title="导入功能待定义文件格式" @click="ElMessage.info('请先定义算法导入文件格式')" />
            <el-dropdown><el-button link :icon="ArrowDown" /><template #dropdown><el-dropdown-menu>
              <el-dropdown-item @click="refreshAll">刷新</el-dropdown-item>
            </el-dropdown-menu></template></el-dropdown>
          </div>
        </div>

        <template v-if="section === 'library'">
          <div v-loading="loading" class="algorithm-grid">
            <article v-for="algorithm in algorithms" :key="algorithm.id" class="algorithm-card">
              <div class="algorithm-image" :style="{ background: fallbackColor(algorithm.id) }">
                <img v-if="algorithm.imageUrl" :src="algorithm.imageUrl" :alt="algorithm.name" />
                <el-dropdown class="card-actions" trigger="click" @command="command => handleAlgorithmCommand(command, algorithm)">
                  <button type="button"><el-icon><MoreFilled /></el-icon></button>
                  <template #dropdown><el-dropdown-menu>
                    <el-dropdown-item command="edit">编辑</el-dropdown-item>
                    <el-dropdown-item command="evaluate">算法评估</el-dropdown-item>
                    <el-dropdown-item command="deploy">下发到摄像机</el-dropdown-item>
                    <el-dropdown-item command="publish">发布到 Model Hub</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                  </el-dropdown-menu></template>
                </el-dropdown>
              </div>
              <div class="algorithm-copy" @click="emit('edit', algorithm)">
                <h4>{{ algorithm.name }} <small>- {{ typeName(algorithm.category) }}</small></h4>
                <p>{{ algorithm.description || '暂无算法描述' }}</p>
              </div>
            </article>
            <el-empty v-if="!loading && !algorithms.length" description="暂无算法" />
          </div>
          <el-pagination v-if="algorithmTotal > algorithmPageSize" v-model:current-page="algorithmCurrent"
                         :page-size="algorithmPageSize" :total="algorithmTotal" layout="prev, pager, next"
                         class="algorithm-pagination" @current-change="loadAlgorithms" />
        </template>

        <el-table v-else v-loading="loading" :data="visibleRows" row-key="id" highlight-current-row
                  @selection-change="selectedRows = $event" @row-click="row => activeCategoryId = row.id">
          <el-table-column type="selection" width="52" />
          <el-table-column type="index" label="序号" width="72" />
          <el-table-column prop="name" label="名称" min-width="180" />
          <el-table-column label="上级分类" min-width="180"><template #default="{ row }">{{ categoryName(row.parentId) }}</template></el-table-column>
          <el-table-column label="拥有下级" width="150"><template #default="{ row }">{{ childCount(row.id) }}</template></el-table-column>
          <el-table-column prop="createTime" label="创建时间" min-width="170" />
          <el-table-column label="操作" width="110" align="right"><template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click.stop="openEdit(row)" />
            <el-button link type="danger" :icon="Delete" @click.stop="removeCategories([row.id])" />
          </template></el-table-column>
        </el-table>
      </main>
    </div>

    <el-dialog v-model="editorOpen" :title="editor.id ? '编辑分类' : '添加分类'" width="480px" append-to-body>
      <el-form ref="formRef" :model="editor" :rules="rules" label-width="90px">
        <el-form-item label="分类名称" prop="name"><el-input v-model="editor.name" maxlength="50" show-word-limit /></el-form-item>
        <el-form-item label="上级分类"><el-tree-select v-model="editor.parentId" :data="parentOptions" node-key="id"
          :props="{ label: 'name', children: 'children' }" check-strictly default-expand-all style="width:100%" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="editor.sortOrder" :min="0" :max="9999" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="editor.remark" type="textarea" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="editorOpen=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveCategory">确定</el-button></template>
    </el-dialog>

    <el-dialog v-model="settingsOpen" title="设置" width="620px" append-to-body>
      <div class="view-toggle"><span :class="{active:viewDraft==='tree'}">树状视图</span><el-switch v-model="viewDraft" active-value="flat" inactive-value="tree" /><span :class="{active:viewDraft==='flat'}">平铺分类视图</span></div>
      <p class="settings-hint">树状展示和平铺分类展示示意</p>
      <div class="view-previews">
        <button type="button" :class="{active:viewDraft==='tree'}" @click="viewDraft='tree'"><TreePreview /></button>
        <button type="button" :class="{active:viewDraft==='flat'}" @click="viewDraft='flat'"><FlatPreview /></button>
      </div>
      <template v-if="viewDraft === 'flat' && settingsTarget">
        <el-form label-width="90px" class="flat-settings">
          <el-form-item label="上级分类"><el-input :model-value="settingsTarget.name" disabled /></el-form-item>
          <el-form-item label="同级展示"><el-select v-model="peerDraft" multiple filterable allow-create default-first-option
            placeholder="选择分类，或输入名称后按回车创建" style="width:100%">
            <el-option v-for="child in directChildren(settingsTarget.id)" :key="child.id" :label="child.name" :value="String(child.id)" />
          </el-select><p class="field-hint">没有可选分类时，可直接输入名称并按 Enter 创建；新分类将作为当前上级分类的下级。</p></el-form-item>
        </el-form>
      </template>
      <template #footer><el-button @click="settingsOpen=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveSettings">确定</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { ArrowDown, Delete, Edit, Folder, MoreFilled, Plus, Search, Setting, TopRight, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteAlgorithm, deleteAlgorithmCatalogCategories, getAlgorithmCatalogCategories, getAlgorithmCatalogPage,
  getAlgorithmCatalogSettings, saveAlgorithmCatalogCategory, saveAlgorithmCatalogSettings
} from '@/api/algorithmManagement'

const emit = defineEmits(['add', 'edit', 'evaluate', 'deploy', 'publish'])
const ROOT_ID = 'root'
const section = ref('library')
const rows = ref([])
const algorithms = ref([])
const algorithmCurrent = ref(1)
const algorithmPageSize = 24
const algorithmTotal = ref(0)
const loading = ref(false)
const saving = ref(false)
// Repository IDs are Java Long values and must never pass through JS Number.
const activeCategoryId = ref('0')
const keyword = ref('')
const treeKeyword = ref('')
const algorithmType = ref('')
const viewMode = ref('flat')
const treeRef = ref()
const selectedRows = ref([])
const editorOpen = ref(false)
const settingsOpen = ref(false)
const settingsTarget = ref(null)
const viewDraft = ref('flat')
const peerDraft = ref([])
const formRef = ref()
const editor = reactive({ id: null, name: '', parentId: '0', sortOrder: 0, remark: '' })
const rules = { name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }] }
const algorithmTypes = [
  { label: '全部', value: '' }, { label: '目标检测算法', value: 'detect' }, { label: '实例分割算法', value: 'segment' },
  { label: '图像分类算法', value: 'classify' }, { label: '关键点检测算法', value: 'pose' }, { label: '旋转目标检测算法', value: 'obb' }
]
const colors = ['#2f80ed', '#8755c4', '#f05467', '#1595aa', '#ff8200']
const normalizedRows = computed(() => rows.value.map(row => ({ ...row, id: String(row.id), parentId: String(row.parentId || '0') })))
const directChildren = id => normalizedRows.value.filter(row => row.parentId === String(id))
const nest = parentId => directChildren(parentId).map(row => ({ ...row, children: nest(row.id) }))
const treeData = computed(() => [{ id: ROOT_ID, name: '所有算法', children: nest(0) }])
const parentOptions = computed(() => [{ id: '0', name: '所有算法', children: nest('0') }])
const flatGroups = computed(() => directChildren('0'))
const visibleRows = computed(() => {
  const scopedRows = activeCategoryId.value !== '0' ? descendants(activeCategoryId.value) : normalizedRows.value
  const query = keyword.value.trim().toLowerCase()
  return query ? scopedRows.filter(row => row.name.toLowerCase().includes(query)) : scopedRows
})

watch(treeKeyword, value => treeRef.value?.filter(value))
watch(keyword, () => { algorithmCurrent.value = 1; if (section.value === 'library') loadAlgorithms() })
watch(section, value => {
  selectedRows.value = []
  algorithmCurrent.value = 1
  const hadKeyword = Boolean(keyword.value)
  keyword.value = ''
  if (value === 'library' && !hadKeyword) loadAlgorithms()
})

const filterTree = (value, data) => !value || data.name.toLowerCase().includes(value.toLowerCase())
const categoryName = id => String(id || '0') === '0' ? '所有算法' : normalizedRows.value.find(row => row.id === String(id))?.name || '-'
const childCount = id => directChildren(id).length
const descendants = id => {
  const ids = new Set([String(id)])
  let changed
  do { changed = false; normalizedRows.value.forEach(row => { if (ids.has(row.parentId) && !ids.has(row.id)) { ids.add(row.id); changed = true } }) } while (changed)
  return normalizedRows.value.filter(row => ids.has(row.id))
}
const flatChildren = group => {
  const selected = parseIds(group.flatCategoryIds)
  const children = directChildren(group.id)
  return selected.length ? children.filter(child => selected.includes(String(child.id))) : children
}
const parseIds = value => { try { return Array.isArray(value) ? value.map(String) : JSON.parse(value || '[]').map(String) } catch { return [] } }
const fallbackColor = id => colors[Number(String(id).slice(-2)) % colors.length]
const typeName = value => algorithmTypes.find(item => item.value === value)?.label || '算法'

async function refreshAll() {
  loading.value = true
  try {
    const [categoryResponse, settingResponse] = await Promise.all([getAlgorithmCatalogCategories(), getAlgorithmCatalogSettings()])
    rows.value = categoryResponse.data || []
    viewMode.value = settingResponse.data || 'flat'
    if (section.value === 'library') await loadAlgorithms()
  } finally { loading.value = false }
}
async function loadAlgorithms() {
  loading.value = true
  try {
    const response = await getAlgorithmCatalogPage({ categoryId: activeCategoryId.value === '0' ? undefined : activeCategoryId.value, keyword: keyword.value || undefined, type: algorithmType.value || undefined, current: algorithmCurrent.value, size: algorithmPageSize })
    algorithms.value = response.data?.records || []
    algorithmTotal.value = response.data?.total || 0
  } finally { loading.value = false }
}
function selectCategory(data) {
  activeCategoryId.value = data.id === ROOT_ID ? '0' : String(data.id)
  algorithmCurrent.value = 1
  if (section.value === 'library') loadAlgorithms()
}
function selectFlat(id) { activeCategoryId.value = String(id); algorithmCurrent.value = 1; loadAlgorithms() }
function resetEditor(parentId) { Object.assign(editor, { id: null, name: '', parentId: String(parentId || '0'), sortOrder: 0, remark: '' }) }
function openCreate(parentId) { resetEditor(parentId === ROOT_ID ? '0' : parentId); editorOpen.value = true }
function openEdit(row) { Object.assign(editor, { id: String(row.id), name: row.name, parentId: String(row.parentId || '0'), sortOrder: row.sortOrder || 0, remark: row.remark || '' }); editorOpen.value = true }
async function saveCategory() {
  await formRef.value.validate(); saving.value = true
  try { await saveAlgorithmCatalogCategory({ ...editor }); editorOpen.value = false; ElMessage.success('分类已保存'); await refreshAll() }
  finally { saving.value = false }
}
async function removeCategories(ids) {
  try { await ElMessageBox.confirm('删除后不可恢复，确定删除所选分类吗？', '删除分类', { type: 'warning' }) }
  catch { return }
  await deleteAlgorithmCatalogCategories(ids); activeCategoryId.value = '0'; ElMessage.success('分类已删除'); await refreshAll()
}
function openSettings(row) { settingsTarget.value = row; viewDraft.value = viewMode.value; peerDraft.value = parseIds(row.flatCategoryIds); settingsOpen.value = true }
async function saveSettings() {
  saving.value = true
  try {
    const targetId = String(settingsTarget.value?.id || '')
    let children = directChildren(targetId)
    const candidates = [...new Set(peerDraft.value.map(value => String(value).trim()))].filter(Boolean)
    let categoryCreated = false
    for (const candidate of candidates) {
      if (!candidate) continue
      const existing = children.find(child => child.id === candidate || child.name.toLowerCase() === candidate.toLowerCase())
      if (!existing) {
        await saveAlgorithmCatalogCategory({ name: candidate, parentId: targetId, sortOrder: children.length })
        categoryCreated = true
      }
    }
    if (categoryCreated) {
      const categoryResponse = await getAlgorithmCatalogCategories()
      rows.value = categoryResponse.data || []
      children = directChildren(targetId)
    }
    const resolvedPeerIds = candidates.map(candidate => children.find(child => child.id === candidate || child.name.toLowerCase() === candidate.toLowerCase())?.id).filter(Boolean)
    if (resolvedPeerIds.length !== candidates.length) throw new Error('部分分类创建后未能读取，请刷新后重试')
    await saveAlgorithmCatalogSettings({ viewMode: viewDraft.value, repositoryId: targetId, peerIds: resolvedPeerIds })
    viewMode.value = viewDraft.value
    settingsOpen.value = false
    ElMessage.success('设置已保存')
    await refreshAll()
  }
  finally { saving.value = false }
}
function handleNodeCommand(command, row) { if (command === 'edit') openEdit(row); if (command === 'settings') openSettings(row); if (command === 'delete') removeCategories([row.id]) }
async function handleAlgorithmCommand(command, algorithm) {
  if (command === 'edit') emit('edit', algorithm)
  if (command === 'evaluate') emit('evaluate', algorithm)
  if (command === 'deploy') emit('deploy', algorithm)
  if (command === 'publish') emit('publish', algorithm)
  if (command === 'delete') {
    try { await ElMessageBox.confirm(`确定删除算法“${algorithm.name}”吗？`, '删除算法', { type: 'warning' }) }
    catch { return }
    await deleteAlgorithm(algorithm.id); ElMessage.success('算法已删除'); await loadAlgorithms()
  }
}
const TreePreview = { render: () => h('div', { class: 'mini tree-mini' }, ['▾  所有算法', h('br'), '　▾  基础算法', h('br'), '　　　回归分析', h('br'), '　　　居正运算']) }
const FlatPreview = { render: () => h('div', { class: 'mini flat-mini' }, ['基础算法　/ 回归分析　/ 居正运算', h('br'), '智慧水利　/ 水位测量预测　/ 水量统计']) }
onMounted(refreshAll)
defineExpose({ refreshAll })
</script>

<style scoped>
.catalog-shell{height:100%;min-height:0;display:flex;flex-direction:column;overflow:hidden;background:#fff;border-radius:8px;color:#30343b}.catalog-header{height:56px;flex-shrink:0;border-bottom:1px solid #e6e9ef;display:flex;align-items:flex-end;justify-content:space-between;padding:0 20px}.catalog-tabs{width:220px}.catalog-tabs :deep(.el-tabs__header){margin:0}.model-hub{height:55px;display:flex;gap:6px;align-items:center;color:#287dff;text-decoration:none}.catalog-body{display:grid;grid-template-columns:280px minmax(0,1fr);flex:1;min-height:0;overflow:hidden}.catalog-body.flat-layout{grid-template-columns:minmax(0,1fr)}.category-aside{min-height:0;padding:20px;border-right:1px solid #eef0f4;overflow-y:auto}.category-aside h3{font-size:16px;margin:0 0 16px;padding-left:9px;border-left:3px solid #287dff}.category-aside .el-input{margin-bottom:14px}.tree-node{width:100%;display:flex;align-items:center;justify-content:space-between;min-width:0}.node-label{display:flex;align-items:center;gap:8px;overflow:hidden;text-overflow:ellipsis}.node-label .el-icon{color:#6ea2f4}.node-actions{display:flex;opacity:0}.tree-node:hover .node-actions{opacity:1}.node-actions .el-button{margin:0;padding:2px}.category-main{min-width:0;min-height:0;padding:20px;overflow-y:auto}.toolbar{display:flex;justify-content:space-between;gap:20px;margin-bottom:18px}.toolbar-primary{flex-shrink:0}.toolbar-search{display:flex;align-items:center;gap:8px}.toolbar-search .el-input{width:340px}.flat-navigation{display:flex;flex-direction:column;gap:14px;margin:-2px 0 18px;padding-bottom:4px}.flat-row{display:flex;align-items:center;flex-wrap:wrap;gap:8px;min-height:24px}.flat-parent,.flat-child{cursor:pointer}.flat-parent{flex-shrink:0;font-weight:600;border-left:3px solid #287dff;padding-left:8px}.flat-child{color:#727983}.type-pills{display:flex;gap:22px;border-bottom:1px solid #edf0f5;margin-bottom:20px}.type-pills .el-button{padding:10px 0 12px;color:#7a808b;border-radius:0}.type-pills .active{color:#287dff;border-bottom:2px solid #287dff}.algorithm-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(235px,1fr));gap:20px;align-items:start}.algorithm-card{border:1px solid #e6e9ef;border-radius:8px;overflow:hidden;box-shadow:0 2px 7px rgb(33 55 85 / 7%);background:#fff}.algorithm-image{height:148px;position:relative}.algorithm-image img{width:100%;height:100%;object-fit:cover}.card-actions{position:absolute;right:10px;top:10px}.card-actions button{border:0;width:28px;height:28px;border-radius:50%;background:#fff;color:#717783;cursor:pointer}.algorithm-copy{padding:12px;cursor:pointer}.algorithm-copy h4{margin:0 0 8px;font-size:15px}.algorithm-copy small{font-size:12px;color:#8e949e;font-weight:400}.algorithm-copy p{height:36px;margin:0;color:#737a84;font-size:13px;line-height:18px;overflow:hidden}.view-toggle{display:flex;align-items:center;gap:12px}.view-toggle span{color:#8a919d}.view-toggle span.active{color:#287dff}.settings-hint{color:#a1a7b1;font-size:13px}.view-previews{display:grid;grid-template-columns:1fr 1fr;gap:18px}.view-previews button{height:145px;background:white;border:2px solid transparent;border-radius:7px;padding:12px;text-align:left}.view-previews button.active{border-color:#287dff}.mini{height:100%;border-radius:5px;background:#f5f7fb;color:#aab4c6;line-height:25px;padding:12px}.flat-settings{margin-top:20px}.field-hint{width:100%;margin:6px 0 0;color:#9299a3;font-size:12px;line-height:18px}.danger-item{color:#f56c6c}@media(max-width:1000px){.catalog-body:not(.flat-layout){grid-template-columns:220px 1fr}.toolbar{flex-direction:column}.toolbar-search .el-input{width:min(340px,80%)}}
.flat-navigation{gap:10px;margin-bottom:14px;padding-bottom:3px;font-size:13px}
.flat-row{column-gap:8px;row-gap:5px;min-height:22px}
.algorithm-grid{grid-template-columns:repeat(auto-fill,minmax(215px,1fr))}
.algorithm-pagination{display:flex;justify-content:flex-end;margin-top:20px}
</style>
