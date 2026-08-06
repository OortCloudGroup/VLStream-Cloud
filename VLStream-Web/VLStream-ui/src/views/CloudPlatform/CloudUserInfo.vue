<template>
  <div class="cloud-user-info" v-loading="loading">
    <!-- 展示模式 -->
    <div v-if="!isEditing" class="view-mode">
      <div class="profile-header">
        <el-avatar :size="88" :src="photo || defaultAvatar" class="avatar">
          {{ form.realName?.charAt(0) || 'U' }}
        </el-avatar>
        <div class="name-row">
          <span class="real-name">{{ form.realName || '未设置姓名' }}</span>
          <img v-if="form.sex === '男'" class="sex-icon" :src="maleIcon" alt="男" />
          <img v-else-if="form.sex === '女'" class="sex-icon" :src="femaleIcon" alt="女" />
          <el-icon class="edit-icon" @click="toggleEdit"><Edit /></el-icon>
        </div>
      </div>

      <el-descriptions :column="2" class="info-descriptions">
        <el-descriptions-item>
          <template #label>
            <span class="info-label">
              <img :src="userNameIcon" class="info-icon" alt="" />
              用户名
            </span>
          </template>
          {{ form.userName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item>
          <template #label>
            <span class="info-label">
              <img :src="emailIcon" class="info-icon" alt="" />
              Email
            </span>
          </template>
          {{ form.email || '-' }}
        </el-descriptions-item>
        <el-descriptions-item>
          <template #label>
            <span class="info-label">
              <img :src="phoneIcon" class="info-icon" alt="" />
              手机号
            </span>
          </template>
          {{ form.phone || '-' }}
        </el-descriptions-item>
        <el-descriptions-item>
          <template #label>
            <span class="info-label">
              <img :src="positionIcon" class="info-icon" alt="" />
              岗位
            </span>
          </template>
          {{ getPositionLabel(form.positionId) || '-' }}
        </el-descriptions-item>
        <el-descriptions-item>
          <template #label>
            <span class="info-label">
              <img :src="deptIcon" class="info-icon" alt="" />
              部门
            </span>
          </template>
          {{ getDeptLabel(form.deptId) || '-' }}
        </el-descriptions-item>
        <el-descriptions-item>
          <template #label>
            <span class="info-label">
              <img :src="postCodeIcon" class="info-icon" alt="" />
              邮政编码
            </span>
          </template>
          {{ form.postCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :span="2">
          <template #label>
            <span class="info-label">
              <img :src="addressIcon" class="info-icon" alt="" />
              地址
            </span>
          </template>
          {{ fullAddress || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :span="2">
          <template #label>
            <span class="info-label">
              <img :src="markIcon" class="info-icon" alt="" />
              简介
            </span>
          </template>
          {{ form.mark || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 编辑模式 -->
    <div v-else class="edit-mode">
      <div class="avatar-edit">
        <el-avatar :size="88" :src="photo || defaultAvatar" class="avatar">
          {{ form.realName?.charAt(0) || 'U' }}
        </el-avatar>
        <div class="avatar-actions">
          <div class="avatar-tip">只支持JPG、JPEG或PNG格式的图片文件</div>
          <div class="avatar-btns">
            <el-upload
              :headers="uploadHeaders"
              :action="uploadURL"
              accept=".jpg,.jpeg,.png,image/jpeg,image/png"
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              :on-success="handleAvatarSuccess"
              :on-error="handleAvatarError"
            >
              <el-button type="primary">更改头像</el-button>
            </el-upload>
            <el-button link type="primary" @click="handleDeleteAvatar">删除图像</el-button>
          </div>
        </div>
      </div>

      <el-form class="edit-form" label-position="top">
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input v-model="form.realName" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名">
              <el-input v-model="form.userName" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.sex" placeholder="请选择" style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
                <el-option label="未知" value="" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="简介">
              <el-input v-model="form.mark" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-select
                v-model="form.deptId"
                placeholder="请选择部门"
                style="width: 100%"
                @change="handleDeptChange"
              >
                <el-option
                  v-for="dept in deptOptions"
                  :key="dept.value"
                  :label="dept.label"
                  :value="dept.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位">
              <el-select
                v-model="form.positionId"
                placeholder="请选择岗位"
                style="width: 100%"
                :disabled="!form.deptId"
              >
                <el-option
                  v-for="position in positionOptions"
                  :key="position.value"
                  :label="position.label"
                  :value="position.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="section-title">地址</div>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="国家">
              <el-select v-model="form.country" style="width: 100%">
                <el-option label="中国" value="中国" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行政区域">
              <el-cascader
                v-model="form.cityArea"
                :options="regionOptions"
                placeholder="请选择行政区域"
                style="width: 100%"
                :props="{ expandTrigger: 'hover', value: 'label', label: 'label', children: 'children' }"
                @change="handleRegionChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="详细地址">
              <el-input v-model="form.address" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮政编码">
              <el-input v-model="form.postCode" placeholder="请输入" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 自定义信息 -->
        <template v-if="sortedCustomFields.length > 0">
          <div class="section-title">自定义信息</div>
          <el-row :gutter="24">
            <el-col
              v-for="field in sortedCustomFields"
              :key="field.field_key"
              :span="12"
            >
              <el-form-item :label="field.label" :required="!!field.is_required">
                <el-input
                  v-if="field.data_type === 'string'"
                  v-model="form.customFields[field.field_key]"
                  placeholder="请输入"
                />
                <el-input-number
                  v-else-if="field.data_type === 'integer'"
                  v-model="form.customFields[field.field_key]"
                  :precision="0"
                  :step="1"
                  style="width: 100%"
                />
                <el-input-number
                  v-else-if="field.data_type === 'float'"
                  v-model="form.customFields[field.field_key]"
                  :precision="2"
                  :step="0.01"
                  style="width: 100%"
                />
                <el-switch
                  v-else-if="field.data_type === 'boolean'"
                  v-model="form.customFields[field.field_key]"
                  :active-value="true"
                  :inactive-value="false"
                />
                <el-date-picker
                  v-else-if="field.data_type === 'date'"
                  v-model="form.customFields[field.field_key]"
                  type="date"
                  placeholder="请选择日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
                <el-date-picker
                  v-else-if="field.data_type === 'datetime'"
                  v-model="form.customFields[field.field_key]"
                  type="datetime"
                  placeholder="请选择日期时间"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  style="width: 100%"
                />
                <el-select
                  v-else-if="field.data_type === 'enum'"
                  v-model="form.customFields[field.field_key]"
                  :placeholder="`请选择${field.label}`"
                  :multiple="field.enum_type === true"
                  style="width: 100%"
                >
                  <el-option
                    v-for="option in field.options || []"
                    :key="option"
                    :label="option"
                    :value="option"
                  />
                </el-select>
                <el-input
                  v-else
                  v-model="form.customFields[field.field_key]"
                  placeholder="请输入"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <div class="form-footer">
          <el-button @click="cancelEdit">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitForm">保存修改</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import { getModelHubUserInfo, editModelHubUser } from '@/api/modelHubUser'
import { getModelHubAccessToken } from '@/utils/modelHubAuth'
import { provenceCityAreaRe } from '@/assets/json/proviceCityArea'

import userNameIcon from '@/assets/img/personInfo/userName_icon.png'
import emailIcon from '@/assets/img/personInfo/email_icon.png'
import phoneIcon from '@/assets/img/personInfo/phone_icon.png'
import positionIcon from '@/assets/img/personInfo/positionId_icon.png'
import deptIcon from '@/assets/img/personInfo/deptId_icon.png'
import postCodeIcon from '@/assets/img/personInfo/postCode_icon.png'
import addressIcon from '@/assets/img/personInfo/address_icon.png'
import markIcon from '@/assets/img/personInfo/mark_icon.png'
import maleIcon from '@/assets/img/personInfo/male_icon.png'
import femaleIcon from '@/assets/img/personInfo/female_icon.png'

/** fastdfs 上传 */
const PLATFORM_ORIGIN = import.meta.env.DEV
  ? ''
  : 'https://workup-dev.myoumuamua.com:6433'
const uploadURL = `${PLATFORM_ORIGIN}/bus/apaas-fastdfsservice/fastdfs/v1/uploadFile`

const uploadHeaders = computed(() => {
  const token = getModelHubAccessToken()
  const tenantId =
    sessionStorage.getItem('modelHubTenantId') ||
    localStorage.getItem('modelHubTenantId') ||
    ''
  return {
    accesstoken: token || '',
    requesttype: 'app',
    appid: '08e3168bd56a4e75ae3d5dee63db0657',
    secretkey: '32e3ca224aa741fbb1362d33070bca2f',
    ...(tenantId ? { tenantid: tenantId } : {})
  }
})

const defaultAvatar = ''
const loading = ref(false)
const saving = ref(false)
const isEditing = ref(false)
const photo = ref('')
const rawUserInfo = ref(null)
const deptOptions = ref([])
const positionOptions = ref([])
const customFieldsList = ref([])
const regionOptions = provenceCityAreaRe

const form = reactive({
  realName: '',
  userName: '',
  email: '',
  phone: '',
  sex: '',
  mark: '',
  deptId: '',
  positionId: '',
  country: '中国',
  province: '',
  city: '',
  area: '',
  cityArea: [],
  address: '',
  postCode: '',
  customFields: {}
})

const fullAddress = computed(() =>
  [form.province, form.city, form.area, form.address].filter(Boolean).join('')
)

/** 按 sort 排序的自定义字段 */
const sortedCustomFields = computed(() => {
  return [...customFieldsList.value].sort((a, b) => {
    const sortA = a.sort !== undefined ? a.sort : 0
    const sortB = b.sort !== undefined ? b.sort : 0
    return sortA - sortB
  })
})

const getDeptLabel = (deptId) =>
  deptOptions.value.find((d) => d.value === deptId)?.label || ''

const getPositionLabel = (positionId) =>
  positionOptions.value.find((p) => p.value === positionId)?.label || ''

const mapCustomFields = (data) => {
  if (data.custom_field && Array.isArray(data.custom_field)) {
    customFieldsList.value = data.custom_field
    form.customFields = {}
    data.custom_field.forEach((field) => {
      if (field.data_type === 'boolean') {
        form.customFields[field.field_key] =
          field.field_value !== null &&
          field.field_value !== undefined &&
          field.field_value !== ''
            ? Boolean(field.field_value)
            : false
      } else if (field.data_type === 'enum') {
        if (field.enum_type === true) {
          if (Array.isArray(field.field_value)) {
            form.customFields[field.field_key] = field.field_value
          } else if (
            field.field_value === null ||
            field.field_value === undefined ||
            field.field_value === ''
          ) {
            form.customFields[field.field_key] = []
          } else {
            form.customFields[field.field_key] = [field.field_value]
          }
        } else if (Array.isArray(field.field_value)) {
          form.customFields[field.field_key] = field.field_value[0] || ''
        } else {
          form.customFields[field.field_key] = field.field_value || ''
        }
      } else {
        form.customFields[field.field_key] = field.field_value || ''
      }
    })
  } else {
    customFieldsList.value = []
    form.customFields = {}
  }
}

const mapUserInfo = (data) => {
  rawUserInfo.value = data
  photo.value = data.photo || ''

  const deptList = Array.isArray(data.dept_list) ? data.dept_list : []
  deptOptions.value = deptList.map((dept) => ({
    value: dept.dept_id,
    label: dept.deptinfo?.dept_name || '',
    jobs: (dept.job || []).map((j) => ({ value: j.job_id, label: j.name }))
  }))

  if (deptList.length > 0) {
    const firstDept = deptList[0]
    form.deptId = firstDept.dept_id || ''
    positionOptions.value = (firstDept.job || []).map((j) => ({
      value: j.job_id,
      label: j.name
    }))
    if (firstDept.job?.length > 0) {
      form.positionId = firstDept.job[0].job_id || ''
    }
  } else {
    form.deptId = ''
    form.positionId = ''
    positionOptions.value = []
  }

  const exData = data.user_detail?.ex_data || {}
  form.userName = data.user_name || ''
  form.realName = exData.realName || data.user_name || ''
  form.email = exData.email || ''
  form.phone = exData.phone || ''
  form.sex = exData.sex || ''
  form.mark = exData.mark || ''
  form.address = exData.address || ''
  form.postCode = exData.postCode || ''
  form.country = '中国'

  const area = exData.area || ''
  let provinceLabel = ''
  let cityLabel = ''
  let areaLabel = ''
  if (Array.isArray(area)) {
    ;[provinceLabel, cityLabel, areaLabel] = area
  } else if (typeof area === 'string' && area) {
    ;[provinceLabel, cityLabel, areaLabel] = area.split(',')
  }
  form.province = provinceLabel || ''
  form.city = cityLabel || ''
  form.area = areaLabel || ''
  form.cityArea = [provinceLabel, cityLabel, areaLabel].filter(Boolean)

  mapCustomFields(data)
}

const fetchUserInfo = async () => {
  const accessToken = getModelHubAccessToken()
  if (!accessToken) {
    ElMessage.warning('未登录，无法获取用户信息')
    return
  }

  loading.value = true
  try {
    const res = await getModelHubUserInfo({
      accessToken,
      desensitize: !isEditing.value
    })
    if (res?.code === 200 && res.data) {
      mapUserInfo(res.data)
    } else {
      ElMessage.error(res?.msg || '获取用户信息失败')
    }
  } catch (error) {
    console.error('getUserInfo failed:', error)
    ElMessage.error(error?.response?.data?.msg || error?.message || '获取用户信息失败')
  } finally {
    loading.value = false
  }
}

const toggleEdit = async () => {
  isEditing.value = true
  await fetchUserInfo()
}

/** 上传前校验 */
const beforeAvatarUpload = (file) => {
  const isImage = ['image/jpeg', 'image/png', 'image/jpg'].includes(file.type)
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) {
    ElMessage.error('只能上传 JPG/JPEG/PNG 格式图片')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

/** 上传成功：更新本地头像预览，保存时一并提交 photo */
const handleAvatarSuccess = (res) => {
  if (res?.code === 200 && res?.data?.url) {
    photo.value = `${res.data.url}?t=${Date.now()}`
    ElMessage.success('头像上传成功，请点击保存修改')
  } else {
    ElMessage.error(res?.msg || '上传失败')
  }
}

const handleAvatarError = () => {
  ElMessage.error('头像上传失败')
}

const handleDeleteAvatar = () => {
  photo.value = ''
  ElMessage.success('已删除头像，请点击保存修改')
}

const cancelEdit = async () => {
  isEditing.value = false
  await fetchUserInfo()
}

const handleDeptChange = (deptId) => {
  const selectedDept = deptOptions.value.find((d) => d.value === deptId)
  if (selectedDept) {
    positionOptions.value = selectedDept.jobs || []
    form.positionId = selectedDept.jobs?.[0]?.value || ''
  } else {
    positionOptions.value = []
    form.positionId = ''
  }
}

const handleRegionChange = (value) => {
  if (Array.isArray(value) && value.length >= 1) {
    form.province = value[0] || ''
    form.city = value[1] || ''
    form.area = value[2] || ''
  } else {
    form.province = ''
    form.city = ''
    form.area = ''
  }
}

const submitForm = async () => {
  const accessToken = getModelHubAccessToken()
  if (!accessToken) {
    ElMessage.warning('登录已失效，请重新登录')
    return
  }

  const area = []
  if (form.province) area.push(form.province)
  if (form.city) area.push(form.city)
  if (form.area) area.push(form.area)

  const customFieldList = customFieldsList.value.map((field) => {
    let processedValue = form.customFields[field.field_key]

    if (field.data_type === 'boolean') {
      if (processedValue === null || processedValue === undefined || processedValue === '') {
        processedValue = false
      }
    }

    if (field.data_type === 'enum' && field.enum_type !== true && Array.isArray(processedValue)) {
      processedValue = processedValue[0] || ''
    }

    return {
      field_key: field.field_key,
      field_value: processedValue,
      data_type: field.data_type,
      label: field.label,
      is_required: field.is_required,
      sort: field.sort,
      is_show: field.is_show,
      id: field.id || '',
      enum_type: field.enum_type,
      options: field.options || [],
      group_id: field.group_id,
      entity_type: field.entity_type || 0,
      entity_id: field.entity_id || '',
      source: field.source
    }
  })

  saving.value = true
  try {
    const res = await editModelHubUser({
      accessToken,
      user_id: rawUserInfo.value?.user_id,
      dept_id: form.deptId || rawUserInfo.value?.dept_list?.[0]?.dept_id,
      ex_data: {
        phone: form.phone,
        email: form.email,
        sex: form.sex,
        mark: form.mark,
        address: form.address,
        postCode: form.postCode,
        area: area.length > 0 ? area : undefined,
        realName: form.realName
      },
      photo: photo.value,
      user_name: form.userName,
      custom_field: customFieldList
    })

    if (res?.code === 200) {
      ElMessage.success('个人信息更新成功')
      isEditing.value = false
      await fetchUserInfo()
    } else {
      ElMessage.error(res?.msg || '保存失败')
    }
  } catch (error) {
    console.error('userEdit failed:', error)
    ElMessage.error(error?.response?.data?.msg || error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style scoped>
.cloud-user-info {
  padding: 16px 8px 0;
  min-height: 360px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 28px;
}

.avatar {
  flex-shrink: 0;
  background: #e8eefc;
  color: #1a53ff;
  font-size: 28px;
  font-weight: 600;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.real-name {
  font-size: 22px;
  font-weight: 700;
  color: #1f2329;
}

.sex-icon {
  width: 18px;
  height: 18px;
}

.edit-icon {
  margin-left: 4px;
  font-size: 18px;
  color: #909399;
  cursor: pointer;
}

.edit-icon:hover {
  color: #1a53ff;
}

.info-descriptions {
  max-width: 920px;
}

.info-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #606266;
}

.info-icon {
  width: 16px;
  height: 16px;
}

.cloud-user-info :deep(.el-descriptions__label) {
  width: 100px;
}

.cloud-user-info :deep(.el-descriptions__content) {
  color: #303133;
}

.avatar-edit {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.avatar-tip {
  color: #909399;
  font-size: 13px;
  margin-bottom: 10px;
}

.avatar-btns {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-title {
  margin: 8px 0 16px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.edit-form {
  max-width: 920px;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-bottom: 8px;
}
</style>
