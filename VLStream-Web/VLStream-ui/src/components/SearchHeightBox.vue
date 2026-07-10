<!-- eslint-disable vue/no-v-model-argument -->
<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2025-3-18 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <div ref="target" class="searchHeightOut">
    <div class="searchHeight">
      <el-input
        v-model="form[props.keyword]"
        :class="{large:islarge}"
        clearable
        class="hei_input"
        :placeholder="props.placeholder"
        @focus="isfocus=true,islarge=true"
        @mouseenter="isfocus=true,islarge=true"
      >
        <template #prefix>
          <el-icon v-if="!islarge">
            <Search />
          </el-icon>
        </template>
      </el-input>
      <template v-if="islarge">
        <oort-svg-icon
          v-popover="popoverRef"
          class="icon_sel"
          :class="{act:visible}"
          name="searchHeight"
          @click.stop="isfocus=false"
        />
        <el-icon class="icon_ser" @click="searchFn">
          <Search />
        </el-icon>
      </template>
    </div>
    <el-popover
      ref="popoverRef"
      v-model:visible="visible"
      popper-class="popoverRefHeight"
      trigger="click"
      virtual-triggering
      :persistent="islarge"
    >
      <el-form class="searchBox" :model="form" :rules="rulesTemp">
        <el-form-item v-for="(item,i) in dataTemp" :key="i" class="flexRowAC" :prop="item.value">
          <div v-if="item.type!=='checkbox'&&item.type!=='serchItem'" class="serchItem flexRowAC">
            <div class="serchItem-label flexRowAC">
              <span>{{ item.label }}</span>
              <el-tooltip
                v-if="item.tips"
                :content="item.tips"
                placement="top"
              >
                <el-icon class="label-tip-icon">
                  <QuestionFilled />
                </el-icon>
              </el-tooltip>
            </div>
            <el-input
              v-if="item.type==='text'"
              v-model="form[item.value]"
              :type="item.type"
              class="wh-input"
              placeholder="请输入"
            />
            <el-select
              v-else-if="item.type==='select'"
              v-model="form[item.value]"
              :teleported="false"
              placeholder="请选择"
            >
              <el-option v-for="(dd,tt) in item.option" :key="tt" :label="dd.label" :value="dd.value" />
            </el-select>
            <el-radio-group
              v-else-if="item.type==='radio'"
              v-model="form[item.value]"
              class="datePicker"
            >
              <el-radio v-for="(dd,tt) in item.option" :key="tt" :label="dd.value">
                {{ dd.label }}
              </el-radio>
            </el-radio-group>
            <div v-else-if="item.type==='daterange'" class="datePicker">
              <el-date-picker
                v-model="form[item.value]"
                :teleported="false"
                type="daterange"
                range-separator="-"
                :start-placeholder="item?.startP||'开始时间'"
                :end-placeholder="item?.endP||'结束时间'"
                :value-format="item?.format"
                :shortcuts="shortcuts"
              />
            </div>
            <template v-else>
              <div v-if="item.type!=='checkbox'">
                <slot :name="item.type" />
              </div>
            </template>
          </div>
          <!-- 整体-->
          <template v-if="item.type==='serchItem'">
            <slot :name="item.slotName" />
          </template>
          <div v-if="item.type==='checkbox'" class="serchItem flexRowAC">
            <el-checkbox v-model="form[item.value]" class="checkbox" :label="item.label" />
          </div>
        </el-form-item>
        <div class="serchItem serBtn">
          <el-button type="primary" class="newBtn" @click="searchFn">
            <img class="searchImg" src="@/assets/img/search.png" alt="" />搜索
          </el-button>
          <el-button class="newBtn" @click="resetFn">
            <el-icon class="searchImg">
              <RefreshRight />
            </el-icon>重置
          </el-button>
        </div>
      </el-form>
    </el-popover>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { QuestionFilled } from '@element-plus/icons-vue'

const props = defineProps({
  placeholder: {
    type: String,
    default: '搜索'
  },
  keyword: {
    type: String,
    default: 'keyword',
    required: true
  },
  /* data搜索框内容 [
  { label: '登录账号', value: 'login_id', type: 'text', default: '' },
  { label: '状态', value: 'status', type: 'select', option: [{ label: '启用', value: 1 }, { label: '禁用', value: 2 }], default: undefined },
  { label: '时间', value: 'datePickerTime', type: 'daterange', startP: '注册时间', endP: '注册时间', format: 'YYYY-MM-DD HH:mm:ss', default: [] },
  { label: '包含下级', value: 'all_child', type: 'checkbox', default: false },
] */
  /* type 判断搜索框-类型 已有text、select、daterange、checkbox  */
  /* serchItem(slotName) 整体搜索嵌入*/
  /* cascader slot */
  /* rules
   [{
      validator: validatePhoneOrMobile,
      message: '请输入正确的手机号格式',
      trigger: 'blur'
    }]
    */
  data: {
    type: Array,
    default: () => []
  }
})
const emits = defineEmits(['handle'])
const popoverRef = ref()
let dataTemp = ref<any>(props.data || []) // 高级搜索框内容
let rulesTemp = reactive<any>({}) // 校验
let form = reactive<any>({})
let visible = ref<boolean>(false)
let isfocus = ref<boolean>(false)
let islarge = ref<boolean>(false)

const blurFn = async() => {
  setTimeout(() => {
    if (!visible.value) {
      islarge.value = false
      isfocus.value = false
    }
  }, 300)
}

// 搜索
const searchFn = (val) => {
  let reset = false
  if (val === true) reset = true
  emits('handle', form, reset)
  setTimeout(() => {
    visible.value = false
  }, 300)
}

// 重置
const resetFn = () => {
  props.data?.forEach((item: any) => {
    form[item.value] = item.default
  })
  form[props.keyword] = ''
  searchFn(true)
}

// 高级搜索框内容
dataTemp.value.forEach(item => {
  form[item.value] = item.default
  rulesTemp[item.value] = item.rules // rules验证
})

watch(() => visible.value, (value) => {
  if (value === false && isfocus.value === false) {
    islarge.value = value
  }
})

// 点击不收起来
import { onClickOutside } from '@vueuse/core'
import { useTemplateRef } from 'vue'

const target = useTemplateRef<HTMLElement>('target')
onClickOutside(target, () => blurFn())

const shortcuts = [
  {
    text: '昨天',
    value: () => {
      const now = new Date()
      const start = new Date(now)
      start.setDate(now.getDate() - 1)
      start.setHours(0, 0, 0, 0)

      const end = new Date(start)
      end.setHours(23, 59, 59, 999)
      return [start, end]
    }
  },
  {
    text: '本周',
    value: () => {
      const now = new Date()
      const start = new Date(now)
      // 调整为周一开始（中国习惯）
      const day = start.getDay() || 7 // 周日转换为7
      start.setDate(start.getDate() - day + 1)
      start.setHours(0, 0, 0, 0)

      const end = new Date(start)
      end.setDate(start.getDate() + 6)
      end.setHours(23, 59, 59, 999)
      return [start, end]
    }
  },
  {
    text: '本月',
    value: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), now.getMonth(), 1)
      start.setHours(0, 0, 0, 0)

      const end = new Date(now.getFullYear(), now.getMonth() + 1, 0)
      end.setHours(23, 59, 59, 999)
      return [start, end]
    }
  },
  {
    text: '上月',
    value: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), now.getMonth() - 1, 1)
      start.setHours(0, 0, 0, 0)

      const end = new Date(now.getFullYear(), now.getMonth(), 0)
      end.setHours(23, 59, 59, 999)
      return [start, end]
    }
  },
  {
    text: '本季度',
    value: () => {
      const now = new Date()
      const quarter = Math.floor(now.getMonth() / 3)
      const start = new Date(now.getFullYear(), quarter * 3, 1)
      start.setHours(0, 0, 0, 0)

      const end = new Date(now.getFullYear(), (quarter + 1) * 3, 0)
      end.setHours(23, 59, 59, 999)
      return [start, end]
    }
  },
  {
    text: '上个季度',
    value: () => {
      const now = new Date()
      let year = now.getFullYear()
      let quarter = Math.floor(now.getMonth() / 3) - 1

      if (quarter < 0) {
        quarter = 3
        year -= 1
      }

      const start = new Date(year, quarter * 3, 1)
      start.setHours(0, 0, 0, 0)

      const end = new Date(year, (quarter + 1) * 3, 0)
      end.setHours(23, 59, 59, 999)
      return [start, end]
    }
  }
]

watch(() => props.data, (val) => {
  if (val && val.length) {
    dataTemp.value = val
    // 高级搜索框内容
    dataTemp.value.forEach(item => {
      form[item.value] = item.default
      rulesTemp[item.value] = item.rules // rules验证
    })
  }
})
</script>

<style lang="scss" scoped>
/*高级搜索框*/
.searchHeight {
  position: relative;
  height: 36px;

  :deep(.hei_input) {
    height: 36px;
    border-radius: 170px;
    width: 280px;
    transition: width 0.3s ease-in-out;

    .el-input__wrapper {
      width: 100%;
      border-radius: 170px;
    }

    &.large {
      width: 540px;
      position: absolute;
      right: 0;
      .el-input__wrapper {
        padding: 0 100px 0 16px;
      }
    }
  }

  .icon_ser {
    cursor: pointer;
    border-radius: 0px 170px 170px 0px;
    background: #F5F5F5;
    width: 55px;
    height: 34px;
    position: absolute;
    right: .5px;
    top: 1px;

    &:hover {
      background: var(--el-color-primary-hb);
      color: var(--el-color-primary);
    }
  }

  .icon_sel {
    cursor: pointer;
    fill: #999 !important;
    width: 24px;
    height: 24px;
    position: absolute;
    right: 62px;
    top: 6px;

    &:hover,&.act{
      background: var(--el-color-primary-hb);
      fill: var(--el-color-primary) !important;
    }
  }
}

/*搜索框-popper*/
.searchBox {
  width: 540px;
  width: 100%;
  gap: 20px;
  padding: 8px;
  flex-wrap: wrap;
  position: relative;

  .serchItem {
    width: 100%;
    min-height: 48px;
    padding-bottom: 0px!important;
    justify-content: space-between;
    position: relative;
  }

  .serBtn {
    padding-bottom: 0;
    text-align: right;

    .searchImg {
      width: 14px;
      height: 14px;
      background-size: cover;
      margin-right: 8px;
    }
  }

  .el-checkbox.checkbox{
    position: absolute;
    left: 20px;
    bottom: 20px;
  }
}

.serchItem-label {
  display: flex;
  align-items: center;
  gap: 4px;

  .label-tip-icon {
    cursor: pointer;
    font-size: 14px;
    color: #999;
    margin-bottom: 10px;
  }
}
</style>
