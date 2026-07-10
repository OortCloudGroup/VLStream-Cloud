<template>
  <div class="au_authority_container">
    <div class="au_authority_content">
      <!-- 可阅读者 -->
      <div class="au_authority_row">
        <div class="au_authority_label">
          可阅读者
        </div>
        <div class="au_authority_setting">
          <div
            v-if="!isReadingEditable"
            class="au_editable_text"
            @click="toggleEdit('reading')"
          >
            {{ getDisplayText('reading') }}
          </div>
          <div v-else class="au_editable_input_wrapper">
            <div class="au_input_container" @click="openUserSelect('reading')">
              <div class="au_tags_container">
                <el-tag
                  v-for="(user, index) in tempReadingUserList"
                  :key="index"
                  class="au_user_tag"
                  closable
                  @close.stop="removeUser('reading', index)"
                >
                  {{ user['user_name'] }}
                </el-tag>
                <div v-if="tempReadingUserList.length === 0" class="au_placeholder">
                  为空则只有作者和相关人可见
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 可编辑者 -->
      <div class="au_authority_row">
        <div class="au_authority_label">
          可编辑者
        </div>
        <div class="au_authority_setting">
          <div
            v-if="!isEditingEditable"
            class="au_editable_text"
            @click="toggleEdit('editing')"
          >
            {{ getDisplayText('editing') }}
          </div>
          <div v-else class="au_editable_input_wrapper">
            <div class="au_input_container" @click="openUserSelect('editing')">
              <div class="au_tags_container">
                <el-tag
                  v-for="(user, index) in tempEditingUserList"
                  :key="index"
                  class="au_user_tag"
                  closable
                  @close.stop="removeUser('editing', index)"
                >
                  {{ user['user_name'] }}
                </el-tag>
                <div v-if="tempEditingUserList.length === 0" class="au_placeholder">
                  为空则只有管理员可编辑
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 附件可下载者 -->
      <div class="au_authority_row">
        <div class="au_authority_label">
          附件可下载者
        </div>
        <div class="au_authority_setting">
          <el-radio-group v-model="downloadPermission" class="au_radio_group">
            <el-radio label="all" class="au_radio_item">
              所有人可下载
            </el-radio>
            <el-radio label="none" class="au_radio_item">
              所有人不可下载
            </el-radio>
            <el-radio label="specified" class="au_radio_item">
              指定人可下载
            </el-radio>
          </el-radio-group>
        </div>
      </div>

      <!-- 附件可打印者 -->
      <div class="au_authority_row">
        <div class="au_authority_label">
          附件可打印者
        </div>
        <div class="au_authority_setting">
          <el-radio-group v-model="printPermission" class="au_radio_group">
            <el-radio label="all" class="au_radio_item">
              所有人可打印
            </el-radio>
            <el-radio label="none" class="au_radio_item">
              所有人不可打印
            </el-radio>
            <el-radio label="specified" class="au_radio_item">
              指定人可打印
            </el-radio>
          </el-radio-group>
        </div>
      </div>

      <!-- 附件可拷贝者 -->
      <div class="au_authority_row">
        <div class="au_authority_label">
          附件可拷贝者
        </div>
        <div class="au_authority_setting">
          <el-radio-group v-model="copyPermission" class="au_radio_group">
            <el-radio label="all" class="au_radio_item">
              所有人可拷贝
            </el-radio>
            <el-radio label="none" class="au_radio_item">
              所有人不可拷贝
            </el-radio>
            <el-radio label="specified" class="au_radio_item">
              指定人可拷贝
            </el-radio>
          </el-radio-group>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div v-if="props.submit" style="text-align: right;">
      <el-button class="bigBtn" @click="emits('close')">
        取消
      </el-button>
      <el-button v-preReClick class="bigBtn" type="primary">
        保存
      </el-button>
    </div>

    <!-- 选人对话框 -->
    <el-dialog v-model="userDialogVisible" :title="dialogTitle" width="50%">
      <address-seting-dialog
        :user-list="currentUserList"
        :mode="3"
        :is-single="false"
        @saveChoose="confirmUser"
        @close="userDialogVisible = false"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import AddressSetingDialog from '@/components/personHome/addressSetingDialog.vue'

const props = defineProps({
  submit: {
    type: Boolean,
    default: true
  }
})
const emits: any = defineEmits(['handle', 'close'])

// 权限设置响应式数据
const downloadPermission = ref('all') // 附件可下载权限
const printPermission = ref('all') // 附件可打印权限
const copyPermission = ref('all') // 附件可拷贝权限

// 编辑状态管理
const isReadingEditable = ref(false) // 可阅读者编辑状态
const isEditingEditable = ref(false) // 可编辑者编辑状态

// 用户数据管理
const readingUserList = ref<any[]>([]) // 可阅读者用户列表
const editingUserList = ref<any[]>([]) // 可编辑者用户列表
const tempReadingUserList = ref<any[]>([]) // 可阅读者临时用户列表
const tempEditingUserList = ref<any[]>([]) // 可编辑者临时用户列表

// 对话框状态
const userDialogVisible = ref(false) // 用户选择对话框显示状态
const dialogTitle = ref('选择可阅读者') // 对话框标题
const currentUserList = ref<any[]>([]) // 当前对话框显示的用户列表
const currentEditType = ref<'reading' | 'editing' | null>(null) // 当前编辑类型

// 权限项配置
const permissionConfig = {
  reading: {
    userList: readingUserList,
    tempUserList: tempReadingUserList,
    isEditable: isReadingEditable,
    defaultText: '除了作者和相关人员,其他人不能阅读',
    placeholder: '为空则只有作者和相关人可见',
    dialogTitle: '选择可阅读者'
  },
  editing: {
    userList: editingUserList,
    tempUserList: tempEditingUserList,
    isEditable: isEditingEditable,
    defaultText: '管理员',
    placeholder: '为空则只有管理员可编辑',
    dialogTitle: '选择可编辑者'
  }
} as const

// 通用方法

/**
 * 切换编辑状态
 * @param type 权限类型：'reading' | 'editing'
 */
const toggleEdit = async(type: keyof typeof permissionConfig) => {
  const config = permissionConfig[type]
  config.isEditable.value = !config.isEditable.value
  if (config.isEditable.value) {
    config.tempUserList.value = [...config.userList.value]
    await nextTick()
  }
}

/**
 * 打开用户选择对话框
 * @param type 权限类型：'reading' | 'editing'
 */
const openUserSelect = (type: keyof typeof permissionConfig) => {
  const config = permissionConfig[type]
  currentEditType.value = type
  userDialogVisible.value = true
  dialogTitle.value = config.dialogTitle
  currentUserList.value = config.userList.value
}

/**
 * 确认用户选择
 * @param data 选中的用户数据
 */
const confirmUser = (data: any) => {
  if (!currentEditType.value) return
  const config = permissionConfig[currentEditType.value]
  config.tempUserList.value = data.user || []
  userDialogVisible.value = false
  currentEditType.value = null
}

/**
 * 删除用户
 * @param type 权限类型：'reading' | 'editing'
 * @param index 要删除的用户索引
 */
const removeUser = (type: keyof typeof permissionConfig, index: number) => {
  const config = permissionConfig[type]
  config.tempUserList.value.splice(index, 1)
}

/**
 * 获取显示文本
 * @param type 权限类型：'reading' | 'editing'
 * @returns 显示文本
 */
const getDisplayText = (type: keyof typeof permissionConfig) => {
  const config = permissionConfig[type]
  const userList = config.userList.value
  if (userList.length === 0) {
    return config.defaultText
  }
  return userList.map((user: any) => user.user_name).join(',')
}
</script>

<style lang="scss" scoped>
.au_authority_container {
  padding: 20px;
  min-height: 500px;
  background-color: #fff;

  .au_authority_content {
    .au_authority_row {
      display: flex;
      align-items: center;
      padding: 10px 0;

      .au_authority_label {
        width: 120px;
        font-size: 14px;
        color: #333;
        font-weight: 500;
      }

      .au_authority_setting {
        flex: 1;
        font-size: 14px;
        color: #666;

        .au_editable_text {
          cursor: pointer;
          padding: 4px 8px;
          border-radius: 4px;
          transition: background-color 0.2s ease;

          &:hover {
            background-color: #f5f5f5;
          }
        }

        .au_editable_input_wrapper {
          display: flex;
          width: 100%;
          max-width: 400px;

          .au_input_container {
            flex: 1;
            display: flex;
            align-items: center;
            min-height: 32px;
            padding: 8px 12px;
            border: 1px solid #dcdfe6;
            border-radius: 4px;
            background-color: #fff;
            transition: border-color 0.2s ease;
            cursor: pointer;

            &:hover {
              border-color: var(--el-color-primary);
            }

            &:focus-within {
              border-color: var(--el-color-primary);
            }

            .au_tags_container {
              flex: 1;
              display: flex;
              flex-wrap: wrap;
              gap: 8px;
              align-items: center;
              min-height: 16px;

              .au_user_tag {
                margin: 0;
              }

              .au_placeholder {
                color: #c0c4cc;
                font-size: 14px;
                line-height: 1;
              }
            }
          }
        }

        .au_radio_group {
          .au_radio_item {
            margin-right: 24px;

            &:last-child {
              margin-right: 0;
            }

            :deep(.el-radio__label) {
              font-size: 14px;
              color: #333;
            }

            :deep(.el-radio__input.is-checked .el-radio__inner) {
              background-color: var(--el-color-primary);
              border-color:var(--el-color-primary);
            }

            :deep(.el-radio__input.is-checked + .el-radio__label) {
              color: var(--el-color-primary);
            }
          }
        }
      }
    }
  }
}
</style>
