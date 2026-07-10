<template>
  <el-dialog
    :model-value="modelValue"
    title="权限配置"
    width="900px"
    @open="loadGrantData"
    @close="handleClose"
  >
    <div class="grant-dialog" v-loading="loading">
      <div class="grant-title">
        <span>{{ role?.roleName || '未选择角色' }}</span>
        <small>{{ role?.roleAlias || '' }}</small>
      </div>

      <el-tabs v-model="activeTab">
        <!-- 菜单权限选项卡 -->
        <el-tab-pane label="菜单权限" name="menu">
          <el-tree ref="menuTreeRef" :data="grantTrees.menu" node-key="id" show-checkbox default-expand-all :props="treeProps" />
        </el-tab-pane>
        <!-- 数据权限选项卡 -->
        <el-tab-pane label="数据权限" name="data">
          <el-tree ref="dataScopeTreeRef" :data="grantTrees.dataScope" node-key="id" show-checkbox default-expand-all :props="treeProps" />
        </el-tab-pane>
        <!-- 接口权限选项卡 -->
        <el-tab-pane label="接口权限" name="api">
          <el-tree ref="apiScopeTreeRef" :data="grantTrees.apiScope" node-key="id" show-checkbox default-expand-all :props="treeProps" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { nextTick, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getGrantTree, getRoleTreeKeys } from '@/api/system/menu'
import { grantRole } from '@/api/system/role'
import { getPayload, normalizeTree, isSuccess } from '../utils/response'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  role: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'success'])

const loading = ref(false)
const saving = ref(false)
const activeTab = ref('menu')
const menuTreeRef = ref()
const dataScopeTreeRef = ref()
const apiScopeTreeRef = ref()

const treeProps = { label: 'label', children: 'children' }
const grantTrees = reactive({ menu: [], dataScope: [], apiScope: [] })

/**
 * 递归设置三类权限树的选中节点
 * @param {Object} keys 后端返回的包含 menu, dataScope, apiScope 选中 key 列表的负载对象
 */
const setCheckedKeys = async (keys) => {
  await nextTick()
  menuTreeRef.value?.setCheckedKeys(keys?.menu || [])
  dataScopeTreeRef.value?.setCheckedKeys(keys?.dataScope || [])
  apiScopeTreeRef.value?.setCheckedKeys(keys?.apiScope || [])
}

/**
 * 从后端异步加载完整的授权树和当前角色的已选中节点
 */
const loadGrantData = async () => {
  if (!props.role?.id) return
  loading.value = true
  try {
    const [treeResponse, keyResponse] = await Promise.all([
      getGrantTree(),
      getRoleTreeKeys(String(props.role.id))
    ])
    const treePayload = getPayload(treeResponse) || {}
    grantTrees.menu = normalizeTree(treePayload.menu || [])
    grantTrees.dataScope = normalizeTree(treePayload.dataScope || [])
    grantTrees.apiScope = normalizeTree(treePayload.apiScope || [])
    const checkedPayload = getPayload(keyResponse) || {}
    await setCheckedKeys(checkedPayload)
  } catch (error) {
    console.error('加载权限数据失败:', error)
    ElMessage.error('加载权限数据失败')
  } finally {
    loading.value = false
  }
}

/**
 * 提交保存当前选择的各类权限设置
 */
const handleSubmit = async () => {
  if (!props.role?.id) return
  saving.value = true
  try {
    const response = await grantRole({
      roleIds: String(props.role.id),
      menuIds: menuTreeRef.value?.getCheckedKeys(false).join(',') || '',
      dataScopeIds: dataScopeTreeRef.value?.getCheckedKeys(false).join(',') || '',
      apiScopeIds: apiScopeTreeRef.value?.getCheckedKeys(false).join(',') || ''
    })
    if (isSuccess(response)) {
      ElMessage.success('权限配置已保存')
      emit('success')
      handleClose()
    } else {
      ElMessage.error(response?.msg || '保存失败')
    }
  } catch (error) {
    console.error('保存权限配置失败:', error)
    ElMessage.error('保存权限配置失败')
  } finally {
    saving.value = false
  }
}

/**
 * 关闭弹窗，通知父级组件更新 v-model 状态
 */
const handleClose = () => {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.grant-dialog {
  min-height: 360px;
}

.grant-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #303133;
  font-weight: 600;
}

.grant-title small {
  color: #909399;
  font-weight: 400;
}
</style>
