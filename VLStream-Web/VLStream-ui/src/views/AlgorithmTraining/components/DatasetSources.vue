<template>
  <section v-loading="loading">
    <div class="source-heading"><div><h3>{{ $tp('数据来源') }}</h3><p>{{ $tp('配置外部 S3 对象存储或公开文件下载地址，导入的数据统一保存到当前存储中。') }}</p></div><el-button type="primary" @click="edit()">{{ $tp('添加数据来源') }}</el-button></div>
    <div class="source-search"><el-input v-model="query.keyword" clearable :placeholder="$tp('搜索来源名称')" @keyup.enter="search" @clear="search" /><el-button @click="search">{{ $tp('搜索') }}</el-button></div>
    <el-table :data="rows" :empty-text="$tp('暂无数据来源，可添加外部 S3 或公开数据集链接')">
      <el-table-column prop="name" :label="$tp('来源名称')" min-width="150" /><el-table-column :label="$tp('类型')" width="140"><template #default="{ row }">{{ row.sourceType === 's3' ? $tp('S3 对象存储') : $tp('公开文件链接') }}</template></el-table-column>
      <el-table-column prop="endpoint" :label="$tp('地址')" min-width="230" show-overflow-tooltip /><el-table-column prop="bucketName" label="Bucket" width="130" /><el-table-column prop="keyPrefix" label="Prefix" width="140" show-overflow-tooltip />
      <el-table-column :label="$tp('操作')" width="225"><template #default="{ row }"><el-button link type="primary" @click="emit('use-source', row)">{{ $tp('导入') }}</el-button><el-button v-if="row.sourceType === 's3'" link @click="check(row)">{{ $tp('测试连接') }}</el-button><el-button link @click="edit(row)">{{ $tp('编辑') }}</el-button><el-button link type="danger" @click="remove(row)">{{ $tp('删除') }}</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.page" :page-size="20" :total="total" layout="total, prev, pager, next" @current-change="load" />
    <DatasetImportHistory />
    <el-dialog v-model="dialog" :title="form.id ? $tp('编辑数据来源') : $tp('添加数据来源')" width="min(680px, 94vw)" :close-on-click-modal="false">
      <el-form label-width="110px" autocomplete="off">
        <el-form-item :label="$tp('来源名称')" required><el-input v-model="form.name" maxlength="100" /></el-form-item>
        <el-form-item :label="$tp('来源类型')"><el-radio-group v-model="form.sourceType" :disabled="Boolean(form.id)"><el-radio label="s3">{{ $tp('S3 对象存储') }}</el-radio><el-radio label="public">{{ $tp('公开文件链接') }}</el-radio></el-radio-group></el-form-item>
        <el-form-item :label="form.sourceType === 's3' ? 'Endpoint' : $tp('文件地址')" required><el-input v-model="form.endpoint" :placeholder="form.sourceType === 's3' ? 'https://s3.example.com' : $tp('可直接下载的图片、视频或 ZIP 地址')" maxlength="1000" /></el-form-item>
        <template v-if="form.sourceType === 's3'">
          <el-form-item label="Region"><el-input v-model="form.region" placeholder="us-east-1" maxlength="100" /></el-form-item><el-form-item label="Bucket" required><el-input v-model="form.bucketName" maxlength="255" /></el-form-item><el-form-item label="Prefix"><el-input v-model="form.keyPrefix" :placeholder="$tp('可选，例如 datasets/cars/')" maxlength="1000" /></el-form-item>
          <el-form-item :label="$tp('访问方式')"><el-checkbox v-model="form.anonymous">{{ $tp('匿名访问公开 Bucket') }}</el-checkbox></el-form-item>
          <template v-if="!form.anonymous"><el-form-item label="Access Key"><el-input v-model="form.accessKey" type="password" show-password autocomplete="new-password" :placeholder="form.id ? $tp('留空保留原凭据') : $tp('来源的只读 Access Key')" /></el-form-item><el-form-item label="Secret Key"><el-input v-model="form.secretKey" type="password" show-password autocomplete="new-password" /></el-form-item><el-form-item label="Session Token"><el-input v-model="form.sessionToken" type="password" show-password autocomplete="new-password" :placeholder="$tp('可选，临时凭据时填写')" /></el-form-item></template>
        </template>
        <el-form-item :label="$tp('说明')"><el-input v-model="form.description" type="textarea" maxlength="1000" :placeholder="$tp('可记录数据集来源、版本、使用许可及标注格式')" /></el-form-item>
      </el-form>
      <p class="source-tip">{{ form.sourceType === 's3' ? $tp('仅使用列举和读取权限，不写入或修改外部 Bucket。凭据保存在服务端，编辑时不回显。') : $tp('支持 GitHub、Gitee、GitCode、Hugging Face 等公开文件。需提供实际图片、视频或 ZIP 文件链接；暂不支持登录资源和 Parquet/TAR。') }}</p>
      <template #footer><el-button @click="dialog = false">{{ $tp('取消') }}</el-button><el-button type="primary" :loading="saving" @click="save">{{ $tp('保存来源') }}</el-button></template>
    </el-dialog>
  </section>
</template>
<script setup>
import { translatePhrase as uiText } from '@/i18n'

import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDatasetSources, saveDatasetSource, deleteDatasetSource, browseDatasetSource } from '@/api/datasetImport'
import DatasetImportHistory from './DatasetImportHistory.vue'
const emit = defineEmits(['use-source'])
const loading = ref(false), saving = ref(false), rows = ref([]), total = ref(0), dialog = ref(false), form = reactive({}), query = reactive({ keyword: '', page: 1 })
const load = async () => { loading.value = true; try { const result = await listDatasetSources(query); rows.value = result.records; total.value = result.total } catch (error) { ElMessage.error(error.message) } finally { loading.value = false } }
const search = () => { query.page = 1; load() }
const edit = (row = {}) => { Object.keys(form).forEach(key => delete form[key]); Object.assign(form, { name: '', sourceType: 's3', endpoint: '', region: 'us-east-1', bucketName: '', keyPrefix: '', description: '', anonymous: false }, row, { accessKey: '', secretKey: '', sessionToken: '' }); if (row.id) form.anonymous = !row.credentialsConfigured; dialog.value = true }
const save = async () => { if (!form.name?.trim() || !form.endpoint?.trim()) return ElMessage.warning(uiText('填写来源名称和地址')); saving.value = true; try { await saveDatasetSource(form.id, form); dialog.value = false; form.accessKey = ''; form.secretKey = ''; form.sessionToken = ''; await load(); ElMessage.success(uiText('来源已保存')) } catch (error) { ElMessage.error(error.message) } finally { saving.value = false } }
const check = async row => { try { const result = await browseDatasetSource(row.id, row.keyPrefix); ElMessage.success(uiText('连接成功，本页读取到 {value0} 个对象', { value0: result.files.length })) } catch (error) { ElMessage.error(error.message) } }
const remove = async row => { try { await ElMessageBox.confirm(uiText('删除来源“{value0}”？已导入的数据保留。', { value0: row.name }), uiText('删除数据来源')); await deleteDatasetSource(row.id); await load() } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message) } }
onMounted(load)
</script>
<style scoped>
.source-heading { display: flex; justify-content: space-between; align-items: center; gap: 20px; margin-bottom: 18px; } h3 { margin: 8px 0; font-size: 17px; } p,.source-tip { color: #909399; font-size: 13px; line-height: 1.8; } .source-search { display: flex; gap: 10px; margin-bottom: 20px; } .source-search .el-input { width: 260px; } .el-pagination { margin-top: 16px; justify-content: flex-end; }
</style>
