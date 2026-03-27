<template>
  <oort-popup
    v-model="innerShow"
    position="top"
    round
    style="height: 60%; width: 100%;"
  >
    <div class="filter-panel">
      <!-- 顶部标题栏 -->
      <div class="filter-header">
        <div class="header-title">
          视频
        </div>
        <div class="header-actions">
          <img class="action-icon" :src="ssImg" alt="" @click="onSearch" />
          <img class="action-icon" :src="sxImg" alt="" />
        </div>
      </div>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <img class="search-icon" src="@/assets/img/VLStreamCloud/ss2.png" alt="" />
        <van-field
          v-model="searchKeyword"
          placeholder=""
          class="search-input"
        />
      </div>

      <!-- 顶部筛选类型 -->
      <div class="filter-tabs">
        <div
          class="filter-tab"
          :class="{ active: activeTab === 'tree' }"
          @click="activeTab = 'tree'"
        >
          设备树
          <van-icon :name="activeTab === 'tree' ? 'arrow-up' : 'arrow-down'" class="tab-icon" />
        </div>
        <!-- 分组标签页 -->
        <!-- <div
          class="filter-tab"
          :class="{ active: activeTab === 'group' }"
          @click="activeTab = 'group'"
        >
          分组
          <van-icon :name="activeTab === 'group' ? 'arrow-up' : 'arrow-down'" class="tab-icon" />
        </div> -->
        <div
          class="filter-tab"
          :class="{ active: activeTab === 'tag' }"
          @click="activeTab = 'tag'"
        >
          标签
          <van-icon :name="activeTab === 'tag' ? 'arrow-up' : 'arrow-down'" class="tab-icon" />
        </div>
      </div>

      <!-- 中间内容 -->
      <div class="filter-body">
        <!-- 设备树：三列级联 -->
        <template v-if="activeTab === 'tree'">
          <div class="tree-columns">
            <div class="tree-column">
              <div
                v-for="item in deviceTree[0]"
                :key="item"
                class="tree-item"
                :class="{ active: selectedTree[0] === item }"
                @click="selectTree(0, item)"
              >
                {{ item }}
              </div>
            </div>
            <div class="tree-column">
              <div
                v-for="item in deviceTree[1]"
                :key="item"
                class="tree-item"
                :class="{ active: selectedTree[1] === item }"
                @click="selectTree(1, item)"
              >
                {{ item }}
              </div>
            </div>
            <div class="tree-column">
              <div
                v-for="item in deviceTree[2]"
                :key="item"
                class="tree-item"
                :class="{ active: selectedTree[2] === item }"
                @click="selectTree(2, item)"
              >
                {{ item }}
              </div>
            </div>
          </div>
        </template>

        <!-- 分组：三列级联 - 暂时注释 -->
        <!-- <template v-else-if="activeTab === 'group'">
          <div class="tree-columns">
            <div class="tree-column">
              <div
                v-for="item in groupTree[0]"
                :key="item"
                class="tree-item"
                :class="{ active: selectedGroup[0] === item }"
                @click="selectGroup(0, item)"
              >
                {{ item }}
              </div>
            </div>
            <div class="tree-column">
              <div
                v-for="item in groupTree[1]"
                :key="item"
                class="tree-item"
                :class="{ active: selectedGroup[1] === item }"
                @click="selectGroup(1, item)"
              >
                {{ item }}
              </div>
            </div>
            <div class="tree-column">
              <div
                v-for="item in groupTree[2]"
                :key="item"
                class="tree-item"
                :class="{ active: selectedGroup[2] === item }"
                @click="selectGroup(2, item)"
              >
                {{ item }}
              </div>
            </div>
          </div>
        </template> -->

        <!-- 标签：平铺标签样式 -->
        <template v-else>
          <div class="tag-list">
            <div
              v-for="item in tagOptions"
              :key="item"
              class="tag-chip"
              :class="{ active: selectedTag === item }"
              @click="selectTag(item)"
            >
              {{ item }}
            </div>
          </div>
        </template>
      </div>

      <!-- 底部按钮 -->
      <div class="filter-footer">
        <van-button
          round
          type="default"
          class="reset-btn"
          @click="onReset"
        >
          重置
        </van-button>
        <van-button
          round
          type="primary"
          class="confirm-btn"
          @click="onConfirm"
        >
          确定
        </van-button>
      </div>
    </div>
  </oort-popup>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import OortPopup from '@/components/popup/oort_popup.vue'
import { getVlsTagManagement } from '@/api/VLStreamCloud/tag'
import { getTagTree } from '@/api/VLStreamCloud/tagManagement'
import ssImg from '@/assets/img/VLStreamCloud/ss.png'
import sxImg from '@/assets/img/VLStreamCloud/sx.png'

const props = defineProps<{
  show: boolean
  defaultTab?: 'tree' | 'group' | 'tag'
}>()

const emit = defineEmits(['update:show', 'confirm'])

const innerShow = ref(props.show)
const searchKeyword = ref('')

watch(
  () => props.show,
  val => {
    innerShow.value = val
  }
)

watch(innerShow, val => {
  emit('update:show', val)
})

const activeTab = ref<'tree' | 'group' | 'tag'>(props.defaultTab || 'tree')

// 当外部 defaultTab 变化时，更新内部激活 Tab
watch(
  () => props.defaultTab,
  (val) => {
    if (val) {
      activeTab.value = val
    }
  }
)

// 设备树原始数据
const tagTreeData = ref<any[]>([])

// 设备树三列数据（动态计算）
const deviceTree = ref<[string[], string[], string[]]>([[], [], []])

// 当前选中的树节点
const selectedTree = ref<(string | null)[]>([null, null, null])

// 从后端获取标签树数据
const fetchTagTree = async() => {
  try {
    const res: any = await getTagTree()
    if (res && res.code === 200 && Array.isArray(res.data)) {
      tagTreeData.value = res.data
      // 初始化第一列：显示第一级的 tagName
      const firstLevel = res.data.map((item: any) => item.tagName || '').filter((name: string) => !!name)
      deviceTree.value[0] = firstLevel
      deviceTree.value[1] = []
      deviceTree.value[2] = []
    }
  } catch (e) {
    deviceTree.value = [[], [], []]
  }
}

// 选择树节点（级联更新）
const selectTree = (columnIndex: number, value: string) => {
  selectedTree.value[columnIndex] = value

  // 重置后续列的选择和数据
  for (let i = columnIndex + 1; i < 3; i++) {
    selectedTree.value[i] = null
    deviceTree.value[i] = []
  }

  if (columnIndex === 0) {
    // 选择第一列：更新第二列
    const firstLevelItem = tagTreeData.value.find((item: any) => item.tagName === value)
    if (firstLevelItem && Array.isArray(firstLevelItem.children)) {
      const secondLevel = firstLevelItem.children.map((item: any) => item.tagName || '').filter((name: string) => !!name)
      deviceTree.value[1] = secondLevel
    } else {
      deviceTree.value[1] = []
    }
    deviceTree.value[2] = []
  } else if (columnIndex === 1) {
    // 选择第二列：更新第三列
    const firstLevelItem = tagTreeData.value.find((item: any) => item.tagName === selectedTree.value[0])
    if (firstLevelItem && Array.isArray(firstLevelItem.children)) {
      const secondLevelItem = firstLevelItem.children.find((item: any) => item.tagName === value)
      if (secondLevelItem && Array.isArray(secondLevelItem.children)) {
        const thirdLevel = secondLevelItem.children.map((item: any) => item.tagName || '').filter((name: string) => !!name)
        deviceTree.value[2] = thirdLevel
      } else {
        deviceTree.value[2] = []
      }
    } else {
      deviceTree.value[2] = []
    }
  }
}

// 分组相关代码
// const groupTree = [
//   ['全部', '香蜜湖', '梅林', '下沙/上沙', '华强南'],
//   ['全部', '香蜜湖', '梅林', '下沙/上沙', '华强南'],
//   ['全部', '香蜜湖', '梅林', '下沙/上沙', '华强南']
// ]

// const selectedGroup = ref<(string | null)[]>(['香蜜湖', '香蜜湖', '香蜜湖'])

// 标签模式下展示的标签列表
const tagOptions = ref<string[]>([])

// 当前选中的标签
const selectedTag = ref<string | null>(null)

// 从后端获取标签列表（parentId = 1）
const fetchTags = async() => {
  const params = { parentId: 1 }
  const res: any = await getVlsTagManagement(params)
  if (res && res.code === 200 && res.data && Array.isArray(res.data.records)) {
    const names = res.data.records
      .map((item: any) => String(item.tagName || '').trim())
      .filter((name: string) => !!name)
    tagOptions.value = names
    // 若当前未选中标签且有返回值，默认选中第一个
    if (!selectedTag.value && names.length > 0) {
      selectedTag.value = names[0]
    }
  }
}

onMounted(() => {
  fetchTags()
  fetchTagTree()
})

// 分组选择函数
// const selectGroup = (columnIndex: number, value: string) => {
//   selectedGroup.value[columnIndex] = value
// }

const selectTag = (value: string) => {
  selectedTag.value = value
}

const onReset = () => {
  selectedTree.value = [null, null, null]
  // 重置设备树显示：只显示第一列
  if (tagTreeData.value.length > 0) {
    const firstLevel = tagTreeData.value.map((item: any) => item.tagName || '').filter((name: string) => !!name)
    deviceTree.value[0] = firstLevel
    deviceTree.value[1] = []
    deviceTree.value[2] = []
  }
  // selectedGroup.value = ['全部', null, null]
  searchKeyword.value = ''
  selectedTag.value = null
}

const onSearch = () => {
  // 搜索功能
}

const onConfirm = () => {
  let tagName: string | null = null
  let filterType: 'tree' | 'tag' | null = null
  let filterValue: string | null = null

  if (activeTab.value === 'tag' && selectedTag.value) {
    filterType = 'tag'
    filterValue = selectedTag.value
    tagName = selectedTag.value
  } else if (activeTab.value === 'tree') {
    filterType = 'tree'
    const lastSelected = selectedTree.value.filter((v: string | null) => v !== null).pop()
    if (lastSelected) {
      filterValue = lastSelected
      tagName = lastSelected
    }
  }

  emit('confirm', {
    tree: selectedTree.value,
    // group: selectedGroup.value,
    tag: selectedTag.value,
    keyword: searchKeyword.value,
    tagName: tagName,
    filterType: filterType,
    filterValue: filterValue
  })
  innerShow.value = false
}
</script>

<style scoped lang="scss">
.filter-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 12px;
}

.header-title {
  line-height: 29px;
  font-weight: 700;
  color: #333333;
  font-size: 18px;
}

.header-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}

.action-icon {
  width: 24px;
  height: 24px;
  cursor: pointer;
}

.search-bar {
  display: flex;
  align-items: center;
  margin: 0 16px 12px;
  padding: 8px 12px;
  background: #f9faff;
  border-radius: 20px;
}

.search-icon {
  width: 24px;
  height: 24px;
}

.search-input {
  flex: 1;
  padding: 0;
  background: transparent;
  border: none;

  :deep(.van-field__control) {
    font-size: 14px;
    color: rgba(31, 41, 55, 1);
  }
}

.filter-tabs {
  display: flex;
  padding: 8px 16px 6px;
  font-size: 14px;
  color: rgba(55, 65, 81, 1);
  border-bottom: 1px solid rgba(229, 231, 235, 1);
}

.filter-tab {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.filter-tab.active {
  color: #2563eb;
}

.tab-icon {
  font-size: 14px;
}

.filter-body {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 8px 0 12px;
}

.tree-columns {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
}

.tree-column {
  padding: 4px 0;
  border-right: 1px solid rgba(243, 244, 246, 1);
}

.tree-column:last-child {
  border-right: none;
}

.tree-item {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 16px;
  font-size: 14px;
  color: rgba(31, 41, 55, 1);
}

.tree-item.active {
  color:#3f70ff;
  background: rgba(37, 99, 235, 0.04);
}

.placeholder {
  padding: 24px 16px;
  text-align: center;
  font-size: 14px;
  color: rgba(156, 163, 175, 1);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 4px 16px;
}

.tag-chip {
  padding: 4px 24px;
  border-radius: 4px;
  background:#f9faff;
  color:#333333;
  font-size:14px;
}

.tag-chip.active {
  color:#2f69f8;
  background:rgba(47, 105, 248, 0.12);
}

.filter-footer {
  display: flex;
  padding: 8px 16px 12px;
  border-top: 1px solid rgba(229, 231, 235, 1);
  background: #fff;
}

.reset-btn,
.confirm-btn {
  flex: 1;
  height: 40px;
}

.reset-btn {
  margin-right: 10px;
}
</style>

