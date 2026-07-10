<template>
  <div>
    <div class="prop_item">
      <el-radio-group v-model="copyActiveChooseData.emptyApproType">
        <el-row>
          <el-col :span="8">
            <el-radio :value="1">
              自动通过
            </el-radio>
          </el-col>
          <el-col :span="8">
            <el-radio :value="2">
              自动驳回
            </el-radio>
          </el-col>
          <el-col :span="8">
            <el-radio :value="3">
              转交{{ flowDesignerPage?.notifyNode ? '处置' : '审批' }}管理员
            </el-radio>
          </el-col>
          <el-col :span="8">
            <el-radio :value="4">
              转交指定人员
            </el-radio>
          </el-col>
          <slot />
        </el-row>
      </el-radio-group>
    </div>
    <div v-if="copyActiveChooseData.emptyApproType === 4" class="prop_item prop_item_group">
      <div v-for="(item,index) in copyActiveChooseData.emptyApproUser" :key="index" class="prop_item_user">
        <id2HeadPic :id="item" class="prop_item_user_headpic" />
        <div class="prop_item_user_name">
          <id2Name :id="item" />
        </div>
        <el-icon class="prop_item_user_remove" color="red" size="20" @click="removeEmptyAppro(index)">
          <CircleCloseFilled />
        </el-icon>
      </div>
      <div class="prop_item prop_item_add">
        <img src="@/assets/img/processui/flownode/add.png" @click="addUser" />
      </div>
    </div>

    <el-dialog v-model="chooseUserVis" title="选择人员" width="50%">
      <address-seting-dialog
        :user-list="copyActiveChooseData.users"
        :mode="3"
        :is-single="false"
        @saveChoose="confirmUser"
        @close="chooseUserVis=false"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, inject } from 'vue'
import AddressSetingDialog from '@/components/personHome/addressSetingDialog.vue'
import id2HeadPic from '@/components/ID2HeadPic.vue'
import id2Name from '@/components/ID2Name.vue'
import { CircleCloseFilled } from '@element-plus/icons-vue'
import { ElCol, ElRow,
  ElRadioGroup, ElRadio, ElDialog, ElIcon } from 'element-plus'

const props = defineProps({
  activeChooseData: {
    type: Object,
    default: () => ({})
  }
})

// 模型-列表数据
const flowDesignerPage = inject('flowDesignerPage')
const copyActiveChooseData = ref(props.activeChooseData)
// watch 监听nodeConfig
watch(() => props.activeChooseData, () => {
  copyActiveChooseData.value = props.activeChooseData
})

const chooseUserVis = ref(false)
function addUser() {
  chooseUserVis.value = true
}

const removeEmptyAppro = (index) => {
  copyActiveChooseData.value.emptyApproUser.splice(index, 1)
  emit('update:activeChooseData', copyActiveChooseData.value)
}

const emit = defineEmits(['update:activeChooseData'])
function confirmUser(data) {
  copyActiveChooseData.value.emptyApproUser = data.user.map(item => item.user_id)
  chooseUserVis.value = false
  emit('update:activeChooseData', copyActiveChooseData.value)
}

</script>

<style lang="scss" scoped>

  .prop_item_group {
    flex-wrap: wrap;
  }

  .prop_item {
    display: flex;
    align-items: center;
    min-height: 48px;
    margin: 8px;

    &_add {
      display: flex;
      align-items: center;
      justify-content: center;
      img {
        width: 48px;
        height: 48px;
        cursor: pointer;
      }
    }

    &_user {
      background: #FFFFFF;
      width: 220px;
      min-width: 220px;
      height: 60px;
      position: relative;
      cursor: pointer;
      display: flex;
      flex-direction: row;
      align-items: center;
      margin: 4px;
      &_remove {
        display: none;
        width: 16px;
        height: 16px;
        position: absolute;
        right: 4px;
        top: 4px;
      }
      &_headpic {
        width: 48px;
        height: 48px;
        margin: 8px;
        border-radius: 100%;
      }
      &_name {
        flex: 1;
        display: flex;
        flex-direction: column;
        span:nth-child(1) {
          font-size: 14px;
          color: #333333;
          letter-spacing: 0;
          font-weight: 700;
        }
        span:nth-child(2) {
          font-size: 12px;
          color: #8D93A2;
          letter-spacing: 0;
          font-weight: 400;
        }
      }
    }
    &_user:hover {
      box-shadow: 0px 0px 6px 0px rgba(0,0,0,0.15);
      border-radius: 2px;
      .prop_item_user_remove {
        display: flex!important;
      }
    }
  }

</style>
