<template>
  <div>
    <div class="chooose_item chooose_item_group">
      <div v-for="(item,index) in user" :key="index" class="chooose_item_user">
        <ID2HeadPic :id="item" class="chooose_item_user_headpic" />
        <div class="chooose_item_user_name">
          <ID2Name :id="item" :value-key="'user_name'" />
          <!-- <span>{{ item.oort_depname }}</span> -->
        </div>
        <el-icon class="chooose_item_user_remove" color="red" size="20" @click="removeUser(index)">
          <CircleCloseFilled />
        </el-icon>
      </div>
      <div v-if="user.length === 0 || isMulti" class="chooose_item chooose_item_add_litte">
        <img src="@/assets/img/processui/flownode/add.png" @click="addUser" />
      </div>
    </div>

    <el-dialog v-model="chooseUserVis" title="选择人员" width="50%">
      <address-seting-dialog
        :user-list="user"
        :mode="3"
        :is-single="!isMulti"
        @saveChoose="confirmUser"
        @close="chooseUserVis=false"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ID2HeadPic from '@/components/ID2HeadPic.vue'
import ID2Name from '@/components/ID2Name.vue'
import AddressSetingDialog from '@/components/personHome/addressSetingDialog.vue'
import { ElDialog, ElIcon } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  isMulti: {
    type: Boolean,
    default: false
  }
})
const user = ref(props.modelValue ? [props.modelValue] : [])
if (props.modelValue.includes(',')) {
  user.value = props.modelValue.split(',')
}
const chooseUserVis = ref(false)
function addUser() {
  chooseUserVis.value = true
}

function removeUser(index) {
  user.value.splice(index, 1)
  if (props.isMulti) {
    emits('update:modelValue', user.value.map(itm => itm.user_id).join(','))
  } else {
    emits('update:modelValue', user.value.length === 0 ? '' : user.value[0])
  }
}

const refreshList = (data) => {
  if (props.isMulti) {
    user.value = data.user.map(itm => itm.user_id)
    emits('update:modelValue', data.user.map(itm => itm.user_id).join(','))
    chooseUserVis.value = false
  } else {
    user.value = [data.user.length === 0 ? '' : data.user[0].user_id]
    emits('update:modelValue', data.user.length === 0 ? '' : data.user[0].user_id)
    chooseUserVis.value = false
  }
}

const emits = defineEmits(['update:modelValue'])
function confirmUser(data) {
  refreshList(data)
}

</script>

<style lang="scss" scoped>

  .chooose_item_group {
    flex-wrap: wrap;
  }

  .chooose_item {
    display: flex;
    align-items: center;
    min-height: 24px;
    margin: 8px;

    &_add_litte {
      display: flex;
      align-items: center;
      justify-content: center;
      img {
        width: 24px;
        height: 24px;
        cursor: pointer;
      }
    }

    &_user {
      background: #FFFFFF;
      width: 160px;
      min-width: 160px;
      height: 32px;
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
        width: 24px;
        height: 24px;
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
      .chooose_item_user_remove {
        display: flex!important;
      }
    }
  }

</style>
