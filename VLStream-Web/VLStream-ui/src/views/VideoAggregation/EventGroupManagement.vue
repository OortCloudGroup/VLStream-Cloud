<template>
  <div class="event-group-management tenant_Page draHeaPB">
    <div class="page-header">
      <div>
        <h2>{{ pageTitle }}</h2>
        <p>此处维护的数据会同步用于主动安全事件的{{ pageTitle.replace('管理', '') }}筛选。</p>
      </div>
      <el-button type="primary" @click="openCreate()">新建{{ entityName }}</el-button>
    </div>

    <div class="content-layout">
      <section class="tree-panel" v-loading="loading">
        <el-input v-model="treeKeyword" clearable placeholder="搜索" />
        <el-tree
          class="group-tree"
          node-key="uid"
          :data="filteredTree"
          :props="treeProps"
          :expand-on-click-node="false"
          :default-expand-all="true"
          highlight-current
          @node-click="selectNode"
        >
          <template #default="{ data }">
            <span class="tree-node">
              <span>{{ data.name }}</span>
              <span class="tree-actions" @click.stop>
                <el-button link type="primary" size="small" @click="openCreate(data)">新增</el-button>
                <el-button link type="primary" size="small" @click="openEdit(data)">编辑</el-button>
                <el-button link type="danger" size="small" @click="removeGroup(data)">删除</el-button>
              </span>
            </span>
          </template>
        </el-tree>
        <el-empty v-if="!loading && treeData.length === 0" description="暂无数据" :image-size="80" />
      </section>

      <section class="table-panel">
        <div class="toolbar">
          <el-input v-model="keyword" clearable placeholder="搜索名称或备注" class="search-input" />
          <el-button @click="reload">刷新</el-button>
        </div>
        <el-table v-loading="loading" :data="filteredRows" row-key="uid" stripe>
          <el-table-column type="index" label="序号" width="70" />
          <el-table-column prop="name" :label="`${entityName}名称`" min-width="180" show-overflow-tooltip />
          <el-table-column label="层级" width="100">
            <template #default="{ row }">{{ row.level + 1 }} 级</template>
          </el-table-column>
          <el-table-column prop="parentName" label="上级" min-width="150" show-overflow-tooltip />
          <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
          <el-table-column prop="created_at" label="创建时间" width="180" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="removeGroup(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.uid ? `编辑${entityName}` : `新建${entityName}`" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="82px">
        <el-form-item label="上级" v-if="form.puid">
          <el-input :model-value="form.parentName" disabled />
        </el-form-item>
        <el-form-item :label="`${entityName}名称`" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" maxlength="255" show-word-limit :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppConfig from '@/config/AppConfig'
import { event_group_delete_v2, event_group_save_v2, event_group_tree } from '@/api/smartCity/events'

const route = useRoute()
const groupType = computed(() => Number(route.meta.groupType || 3))
const entityName = computed(() => ({ 1: '区域', 2: '分组', 3: '标签' }[groupType.value] || '标签'))
const pageTitle = computed(() => `${entityName.value}管理`)
const treeProps = { label: 'name', children: 'children' }
const treeData = ref([])
const flatRows = ref([])
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const treeKeyword = ref('')
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ uid: '', puid: '', parentName: '', name: '', remark: '' })
const rules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }] }

const filteredRows = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return flatRows.value
  return flatRows.value.filter(item => `${item.name}${item.remark}${item.parentName}`.toLowerCase().includes(value))
})

const filterTree = (nodes) => nodes.reduce((result, node) => {
  const children = filterTree(node.children || [])
  const matched = !treeKeyword.value || node.name.includes(treeKeyword.value)
  if (matched || children.length) result.push({ ...node, children })
  return result
}, [])
const filteredTree = computed(() => filterTree(treeData.value))

const responseList = (response) => response?.data?.list || response?.list || []

const fetchTree = async () => {
  const response = await event_group_tree({
    app_id: AppConfig.events.appID,
    group_type: groupType.value
  })
  if (response?.code && response.code !== 200) {
    throw new Error(response.msg || '加载数据失败')
  }
  return responseList(response)
}

const flattenTree = (nodes, parentName = '', level = 0) => nodes.flatMap(node => [
  { ...node, parentName, level },
  ...flattenTree(node.children || [], node.name, level + 1)
])

const reload = async () => {
  loading.value = true
  try {
    treeData.value = await fetchTree()
    flatRows.value = flattenTree(treeData.value)
  } catch (error) {
    ElMessage.error(error?.message || `加载${entityName.value}失败`)
  } finally {
    loading.value = false
  }
}

const selectNode = (node) => {
  keyword.value = node.name
}

const resetForm = () => {
  form.uid = ''
  form.puid = ''
  form.parentName = ''
  form.name = ''
  form.remark = ''
}

const openCreate = (parent = null) => {
  resetForm()
  if (parent) {
    form.puid = parent.uid
    form.parentName = parent.name
  }
  dialogVisible.value = true
}

const openEdit = (node) => {
  form.uid = node.uid
  form.puid = node.puid
  form.parentName = node.parentName || ''
  form.name = node.name
  form.remark = node.remark || ''
  dialogVisible.value = true
}

const save = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const response = await event_group_save_v2({
      app_id: AppConfig.events.appID,
      group_type: groupType.value,
      uid: form.uid || undefined,
      puid: form.puid || undefined,
      name: form.name.trim(),
      remark: form.remark.trim()
    })
    if (response?.code && response.code !== 200) throw new Error(response.msg || '保存失败')
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await reload()
  } catch (error) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const removeGroup = async (node) => {
  try {
    await ElMessageBox.confirm(`确定删除“${node.name}”吗？有子级时无法删除。`, '删除确认', { type: 'warning' })
    const response = await event_group_delete_v2({ app_id: AppConfig.events.appID, uid: node.uid })
    if (response?.code && response.code !== 200) throw new Error(response.msg || '删除失败')
    ElMessage.success('删除成功')
    await reload()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '删除失败')
  }
}

watch(groupType, reload)
onMounted(reload)
</script>

<style scoped>
.event-group-management { padding: 24px; background: #fff; min-height: calc(100vh - 120px); }
.page-header, .toolbar, .tree-node, .tree-actions { display: flex; align-items: center; }
.page-header { justify-content: space-between; margin-bottom: 20px; }
.page-header h2 { margin: 0 0 8px; font-size: 20px; color: #303133; }
.page-header p { margin: 0; color: #909399; font-size: 14px; }
.content-layout { display: grid; grid-template-columns: 300px minmax(0, 1fr); gap: 20px; }
.tree-panel { min-height: 560px; padding: 16px; border: 1px solid #ebeef5; border-radius: 4px; }
.group-tree { margin-top: 14px; }
.tree-node { width: 100%; justify-content: space-between; gap: 8px; overflow: hidden; }
.tree-node > span:first-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tree-actions { flex-shrink: 0; }
.table-panel { min-width: 0; }
.toolbar { justify-content: flex-end; gap: 12px; margin-bottom: 16px; }
.search-input { width: 260px; }
@media (max-width: 900px) { .content-layout { grid-template-columns: 1fr; } }
</style>
