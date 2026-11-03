<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <el-dialog
    :model-value="modelValue"
    :title="$tp('权限配置')"
    width="50%"
    @open="loadGrantData"
    @close="handleClose"
  >
    <div class="grant-dialog" v-loading="loading">
      <div class="grant-title">
        <span>{{ role?.roleName || $tp('未选择角色') }}</span>
        <small>{{ role?.roleAlias || '' }}</small>
      </div>

      <el-tabs v-model="activeTab">
        <!-- menu item -->
        <el-tab-pane :label="$tp('菜单权限')" name="menu">
          <el-alert v-if="String(role?.id) === '1' && !menuIsSystemAdmin" :title="$tp('只有超级管理员可以修改该角色的菜单授权')" type="info" :closable="false" />
          <el-alert v-if="hiddenGrantedMenuIds.length" :title="$tp('部分权限不在当前账号可配置范围内，保存时将保留')" type="info" :closable="false" />
          <el-tree ref="menuTreeRef" :data="grantTrees.menu" node-key="id" show-checkbox default-expand-all :props="treeProps" @check="handleMenuCheck" />
        </el-tab-pane>
        <!-- data item -->
        <el-tab-pane :label="$tp('数据权限')" name="data">
          <el-alert :title="$tp('当前版本暂不支持在此配置数据权限')" type="info" :closable="false" />
          <el-tree ref="dataScopeTreeRef" :data="grantTrees.dataScope" node-key="id" show-checkbox default-expand-all :props="treeProps" />
        </el-tab-pane>
        <!-- interface item -->
        <el-tab-pane :label="$tp('接口权限')" name="api">
          <el-alert :title="$tp('当前版本暂不支持在此配置接口权限')" type="info" :closable="false" />
          <el-tree ref="apiScopeTreeRef" :data="grantTrees.apiScope" node-key="id" show-checkbox default-expand-all :props="treeProps" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <template #footer>
      <el-button @click="handleClose" class="common_btn">{{ $tp('取消') }}</el-button>
      <el-button type="primary" :loading="saving" :disabled="loading || !loadSucceeded || (String(role?.id) === '1' && !menuIsSystemAdmin)" @click="handleSubmit" class="common_btn">{{ $tp('保存') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { translatePhrase as uiText } from '@/i18n'

import { nextTick, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getGrantTree } from '@/api/system/menu'
import { getRoleMenuGrants, grantRole } from '@/api/system/role'
import { getPayload, normalizeTree, isSuccess } from '../utils/response'
import { hiddenGrantedMenuIds as findHiddenGrantedMenuIds, selectedMenuIds, orderedVisibleGrants, changedVisibleGrants, preserveGrantableHiddenGrants } from '@/utils/menuGrant.mjs'
import { menuIsSystemAdmin } from '@/utils/menuAccessState'

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
const hiddenGrantedMenuIds = ref([])
const originalMenuIds = ref([])
const originalVisibleIds = ref([])
const initialSelection = ref([])
const grantableMenuIds = ref(new Set())
const loadSucceeded = ref(false)
const touchedMenuIds = new Set()

const treeProps = { label: 'label', children: 'children' }
const grantTrees = reactive({ menu: [], dataScope: [], apiScope: [] })

/**
 * Set in node
 * @param {Object} keys after menu, dataScope, apiScope in key object
 */
const setCheckedKeys = async (ids) => {
  touchedMenuIds.clear()
  await nextTick()
  menuTreeRef.value?.setCheckedKeys([])
  originalVisibleIds.value = orderedVisibleGrants(grantTrees.menu, ids)
  originalVisibleIds.value.forEach(id => menuTreeRef.value?.setChecked(id, true, false))
  await nextTick()
  initialSelection.value = selectedMenuIds(menuTreeRef.value?.getCheckedKeys(false), menuTreeRef.value?.getHalfCheckedKeys())
}

const handleMenuCheck = (data) => {
  let node = menuTreeRef.value?.getNode(data.id)
  while (node && node.level > 0) {
    touchedMenuIds.add(String(node.data.id))
    node = node.parent
  }
}

/**
 * from after Load and current role already in node
 */
const loadGrantData = async () => {
  if (!props.role?.id) return
  const roleId = String(props.role.id)
  loadSucceeded.value = false
  loading.value = true
  try {
    const [treeResponse, keyResponse] = await Promise.all([
      getGrantTree(),
      getRoleMenuGrants(roleId)
    ])
    if (String(props.role?.id) !== roleId) return
    const checkedPayload = getPayload(keyResponse) || {}
    const validIds = ids => Array.isArray(ids) && ids.every(id => typeof id === 'string' || Number.isSafeInteger(id))
    if (!validIds(checkedPayload.menuIds) || !validIds(checkedPayload.grantableMenuIds)) {
      throw new Error('角色菜单授权数据无效')
    }
    const treePayload = getPayload(treeResponse) || {}
    grantableMenuIds.value = new Set(checkedPayload.grantableMenuIds.map(String))
    const markUnavailable = nodes => nodes.map(node => ({
      ...node,
      disabled: !grantableMenuIds.value.has(String(node.id)),
      children: markUnavailable(node.children || [])
    }))
    grantTrees.menu = markUnavailable(normalizeTree(treePayload.menu || []))
    grantTrees.dataScope = normalizeTree(treePayload.dataScope || [])
    grantTrees.apiScope = normalizeTree(treePayload.apiScope || [])
    originalMenuIds.value = checkedPayload.menuIds.map(String)
    hiddenGrantedMenuIds.value = originalMenuIds.value.filter(id => !grantableMenuIds.value.has(id))
      .concat(findHiddenGrantedMenuIds(grantTrees.menu, originalMenuIds.value))
    await setCheckedKeys(originalMenuIds.value)
    loadSucceeded.value = true
  } catch (error) {
    console.error('加载权限数据失败:', error)
    ElMessage.error(uiText('加载权限数据失败'))
  } finally {
    loading.value = false
  }
}

/**
 * current Set
 */
const handleSubmit = async () => {
  if (!props.role?.id || !loadSucceeded.value) return
  saving.value = true
  try {
    const currentSelection = selectedMenuIds(menuTreeRef.value?.getCheckedKeys(false), menuTreeRef.value?.getHalfCheckedKeys())
    const selected = changedVisibleGrants(originalVisibleIds.value, initialSelection.value, currentSelection, [...touchedMenuIds])
    const menuIds = preserveGrantableHiddenGrants(grantTrees.menu, originalMenuIds.value, [...grantableMenuIds.value], selected)
    const response = await grantRole({
      roleIds: String(props.role.id),
      menuIds,
      expectedMenuIds: originalMenuIds.value
    })
    if (isSuccess(response)) {
      ElMessage.success(uiText('权限配置已保存'))
      emit('success')
      handleClose()
    } else {
      ElMessage.error(response?.msg || uiText('保存失败'))
    }
  } catch (error) {
    console.error('保存权限配置失败:', error)
    ElMessage.error(uiText('保存权限配置失败'))
  } finally {
    saving.value = false
  }
}

/**
 * dialog, notification component new v-model
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
