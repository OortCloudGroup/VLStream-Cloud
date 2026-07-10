<template>
  <div class="home">
    <div class="table_c flexColumn">
      <div class="searchBox">
        <div class="search_group flexRowAC">
          <div class="serchItem">
            <el-input v-model="queryParams.name" class="wh-input" placeholder="规则名称" />
          </div>
          <!-- <div class="serchItem">
            <el-select v-model="queryParams.status" clearable placeholder="状态">
              <el-option label="全部" value="" />
              <el-option label="启用" value="1" />
              <el-option label="禁用" value="0" />
            </el-select>
          </div> -->
          <div class="serchItem">
            <el-button type="primary" @click="getList">
              <el-icon><Search /></el-icon>搜索
            </el-button>
            <el-button @click="resetQuery">
              <el-icon class="searchImg">
                <RefreshRight />
              </el-icon>重置
            </el-button>
          </div>
        </div>
      </div>

      <div class="main_table">
        <div class="table_cc">
          <el-table ref="ruleTableRef" header-cell-class-name="header_cell" stripe :data="tableData">
            <el-table-column type="selection" />
            <el-table-column label="序号" type="index" width="60" align="center" />
            <el-table-column label="规则名称" align="center" prop="name" />
            <!-- <el-table-column label="结构数据类别" align="center" prop="processName" :show-overflow-tooltip="true" /> -->
            <el-table-column label="规则说明" align="center" prop="description" />
            <el-table-column label="状态" align="center">
              <template #default="scope">
                <el-tag v-if="scope.row.status === '1'" type="success">
                  启用
                </el-tag>
                <el-tag v-if="scope.row.status === '0'" type="danger">
                  禁用
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" align="center" prop="createTime" />
          </el-table>
        </div>
        <div class="paginationBox flexRowAC">
          <el-pagination
            background
            :page-sizes="[queryParams.pageSize, 20, 30, 50]"
            layout="total,prev,pager, next,sizes"
            :total="count"
            class="justifyAlign"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>
    <div class="button_bottom flexRowAC">
      <el-button @click="confirmChoose">
        取消
      </el-button>
      <el-button v-preReClick type="primary" @click="confirmChoose">
        确定
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, defineEmits } from 'vue'
import { listRule } from '@/api/processui/indexUserdefine'
import { ElButton, ElPagination, ElTable, ElTableColumn, ElTag, ElIcon, ElInput } from 'element-plus'
import { Search, RefreshRight } from '@element-plus/icons-vue'

const tableData = ref<any>([])
const count = ref<number>(0)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: undefined,
  status: 1
})
function getList() {
  listRule(queryParams).then(res => {
    if (res.code === 200) {
      tableData.value = res.data.records || []
      count.value = res.data.total || 0
    }
  })
}

function resetQuery() {
  queryParams.name = undefined
  queryParams.status = 1
  getList()
}

function handleSizeChange(pageSize) {
  queryParams.pageSize = pageSize
  getList()
}

function handleCurrentChange(pageNum) {
  queryParams.pageNum = pageNum
  getList()
}

const ruleTableRef = ref(null)
const emits = defineEmits(['confirm'])
const confirmChoose = () => {
  const ruleArr = ruleTableRef.value.getSelectionRows() || []
  emits('confirm', ruleArr.map(item => item.expression))
}

onMounted(() => {
  getList()
})

</script>

<style lang='scss' scoped>
      .searchBox {
        gap: 20px;
        padding: 24px 20px;
        display: flex;
        flex-direction: column;
        flex-wrap: wrap;

        .serchItem {
          min-width: 240px;
          height: 36px;
          margin: 0 4px;
          border-radius: 4px;
        }

        .serchItem.siRange {
          width: 350px;
        }

        :deep(.el-button.el-button--primary) {
          background: var(--el-color-primary);;
          border: none;
        }
      }

      .searchImg {
        width: 14px;
        height: 14px;
        background-size: cover;
        margin-right: 8px;
      }

      .table_cc :deep(.header_cell) {
        color: #333;
        background-color: #FAFAFA;
      }

      .home {
        height: 100%;
        overflow: auto;

        .content_top {
          margin-top: 20px;
          height: 420px;
          padding-bottom: 20px;
          gap: 20px;
        }

        .content_bottom {
          height: 556px;
          gap: 20px;
          align-items: flex-start;
        }

        .bgRadius {
          background: #FFFFFF;
          border-radius: 10px;
          padding: 20px;
        }
      }

      .table_c {
        width: 100%;
        background: #FFFFFF;
        border-radius: 10px;
      }

      .table_c_header {
        height: 60px;
        margin: 0 24px;
      }

      .table_cc {
        padding: 0 20px;
      }

      .paginationBox {
        justify-content: center;
        height: 100px;
      }

      .operateDe {
        cursor: pointer;
        color: var(--el-color-primary);;
      }

      .reSucc {
        font-size: 12px;
        color: #5CD905;
        padding: 2px 4px;
        line-height: 14px;
        color: #81D557;
        background-color: #F6FFED;
        border: 1px solid #CBEEBA;
        border-radius: 4px;
      }

      .reSucc.reFail {
        color: #FF4949;
        background-color: rgba(255, 162, 162, .2);
        border: 1px solid rgba(255, 162, 162, 1);
      }

      .reSucc.reAppr {
        color: #FA8D00;
        background-color: rgba(255, 213, 145, .2);
        border: 1px solid rgba(255, 213, 145, 1);
      }

      .exportBtn {
        cursor: pointer;
        margin-bottom: 30px;
        justify-content: center;
        width: 82px;
        height: 32px;
        background: var(--el-color-primary);;
        border-radius: 5px;
        border: 1px solid var(--el-color-primary);;
        font-size: 14px;
        color: #FFFFFF;
        box-shadow: 0px 2px 0px 0px rgba(0, 0, 0, 0.04);
      }

      .BtnImg {
        width: 14px;
        height: 14px;
        margin-right: 8px;
        background-size: cover;
      }

      .operateBox {
        gap: 20px;

        .operateRe {
          cursor: pointer;
          font-size: 14px;
          color: #FB5504;
        }
      }

      .riskBtns {
        padding: 3px 7px;
        font-size: 12px;
        border-radius: 4px;
      }

      .riskWX {
        background: #F6FFED;
        border: 1px solid rgba(183, 235, 143, 1);
        color: #52C41A;
      }

      .riskLKL {
        color: #FA8D00;
        background: #FFF7E6;
        border: 1px solid rgba(255, 213, 145, 1);
      }

      .main_table {
        display: flex;
        flex-direction: column;
      }

  .button_bottom {
    width: 98%;
    justify-content: flex-end;
  }

  </style>
