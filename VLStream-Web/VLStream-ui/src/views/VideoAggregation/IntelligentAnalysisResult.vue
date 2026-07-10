<template>
  <div class="intelligent-analysis-result">
    <!-- 查询栏 -->
    <!--
    <div class="query-bar">
      <div class="search-form">
        <div class="search-row">
          <el-input
            v-model="searchForm.analysisName"
            placeholder="分析名称"
            clearable
            style="width: 240px;"
            class="query-input"
          />
          <el-select
            v-model="searchForm.analysisType"
            placeholder="分析类型"
            clearable
            style="width: 150px;"
          >
            <el-option label="全部" value="" />
            <el-option label="人员分析" value="人员分析" />
            <el-option label="车辆分析" value="车辆分析" />
            <el-option label="行为分析" value="行为分析" />
            <el-option label="异常检测" value="异常检测" />
          </el-select>
          <DateRangePicker
            v-model="searchForm.dateRange"
            start-placeholder="创建时间"
            end-placeholder="创建日期"
          />
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </div>
      </div>
    </div>
    -->

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 操作按钮区域 -->
      <div class="action-section">
        <div class="action-button-group">
          <!-- 导出删除按钮组 -->
          <div class="export-delete-group">
            <el-button 
              class="export-btn-custom" 
              @click="handleExport" 
              :disabled="!selectedRows.length"
            >
              <el-icon><Download /></el-icon>
              导出
            </el-button>
            <el-button 
              type="danger" 
              class="delete-btn-custom" 
              @click="handleDelete" 
              :disabled="!selectedRows.length"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </div>
        
        <!-- 高级搜索组件 -->
        <div class="search-container">
          <AdvancedSearch 
            @search="handleAdvancedSearch"
            @reset="handleAdvancedSearchReset"
            @export="handleExport"
            @upload="handleUpload"
            @template="handleDownloadTemplate"
            @batch="handleBatchOperation"
          />
        </div>
      </div>

      <!-- 表格内容 -->
      <div class="table-content">
        <el-table
          :data="currentPageData"
          stripe
          @selection-change="handleSelectionChange"
          style="width: 100%"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column type="index" label="序号" width="80" align="center" />
          <el-table-column prop="analysisName" label="分析名称" min-width="120" />
          <el-table-column prop="analysisType" label="分析类型" min-width="120" />
          <el-table-column prop="screenshot" label="抓拍截图" width="120" align="center">
            <template #default="scope">
              <div class="screenshot-container">
                <img :src="scope.row.screenshot" alt="截图" class="screenshot-image" />
                <div class="screenshot-count">+{{ scope.row.screenshotCount }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="area" label="区域" min-width="120" />
          <el-table-column prop="camera" label="摄像头" min-width="200" />
          <el-table-column prop="analysisTime" label="分析时间" min-width="180" />
          <el-table-column label="操作" width="100" fixed="right" align="right">
            <template #default="scope">
              <div class="operation-buttons">
                <PlayButton @click="handlePlay(scope.row)" />
              </div>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 分页 - 紧贴表格数据 -->
        <div class="table-pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>

    <!-- 视频播放弹窗 -->
    <el-dialog
      v-model="videoDialogVisible"
      title="分析视频播放"
      width="1400px"
      :close-on-click-modal="false"
      class="video-dialog"
    >
      <div class="video-playback-container">
        <!-- 左侧视频列表区域 -->
        <div class="video-list-section">
          <div class="section-title">视频列表</div>
          
          <!-- 视频缩略图列表 -->
          <div class="video-thumbnails">
            <div 
              v-for="(video, index) in analysisVideoList" 
              :key="index"
              class="video-thumbnail-item"
              :class="{ active: selectedVideoIndex === index }"
              @click="selectVideo(index)"
            >
              <div class="thumbnail-image">
                <img :src="video.thumbnail" alt="视频缩略图" class="thumbnail-img" />
                <div class="play-overlay">
                  <el-icon class="play-icon"><VideoPlay /></el-icon>
                </div>
              </div>
              <div class="video-time-range">{{ video.timeRange }}</div>
            </div>
          </div>
        </div>

        <!-- 右侧播放器区域 -->
        <div class="player-section">
          <div class="video-player">
            <div class="video-content">
              <!-- 模拟视频画面 -->
              <div class="video-placeholder">
                <div class="video-info">
                  <div class="device-name">{{ selectedRow?.analysisName || '路径追踪' }}</div>
                  <div class="video-time">2021年04月15日 11:18:27</div>
                </div>
              </div>
              
              <!-- 视频控制条 -->
              <div class="video-controls">
                <div class="progress-bar">
                  <div class="progress-track">
                    <div class="progress-fill" :style="{ width: '45%' }"></div>
                    <div class="progress-handle" :style="{ left: '45%' }"></div>
                  </div>
                </div>
                <div class="control-buttons">
                  <el-button class="control-btn" size="small">
                    <el-icon><VideoPlay /></el-icon>
                  </el-button>
                  <span class="time-display">00:22:14/07:34:12</span>
                  <div class="volume-control">
                    <el-icon class="volume-icon"><Microphone /></el-icon>
                    <span class="volume-text">1.0</span>
                  </div>
                  <div class="fullscreen-control">
                    <el-icon class="fullscreen-icon"><FullScreen /></el-icon>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import {
  Search,
  Refresh,
  Download,
  Delete,
  VideoPlay,
  Microphone,
  FullScreen
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DateRangePicker from '@/components/DateRangePicker.vue'
import PlayButton from '@/components/PlayButton.vue'
import AdvancedSearch from '@/components/AdvancedSearch.vue'

// 搜索表单
const searchForm = reactive({
  analysisName: '',
  analysisType: '',
  dateRange: []
})

// 视频播放弹窗相关
const videoDialogVisible = ref(false)
const selectedRow = ref(null)
const selectedVideoIndex = ref(0)

// 分析视频列表数据
const analysisVideoList = ref([
  {
    timeRange: '10:20:00 - 10:30:00',
    thumbnail: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTIwIiBoZWlnaHQ9IjkwIiB2aWV3Qm94PSIwIDAgMTIwIDkwIiBmaWxsPSJub25lIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPgo8cmVjdCB3aWR0aD0iMTIwIiBoZWlnaHQ9IjkwIiBmaWxsPSIjZjBmMGYwIi8+CjxjaXJjbGUgY3g9IjMwIiBjeT0iMzAiIHI9IjE1IiBmaWxsPSIjZTBlMGUwIi8+CjxyZWN0IHg9IjYwIiB5PSIyMCIgd2lkdGg9IjQwIiBoZWlnaHQ9IjIwIiBmaWxsPSIjZTBlMGUwIi8+CjxyZWN0IHg9IjIwIiB5PSI2MCIgd2lkdGg9IjgwIiBoZWlnaHQ9IjEwIiBmaWxsPSIjZTBlMGUwIi8+Cjx0ZXh0IHg9IjEwIiB5PSI4MCIgZm9udC1zaXplPSI4IiBmaWxsPSIjNjY2Ij7liqjnlLvliIbmnpA8L3RleHQ+Cjwvc3ZnPg=='
  },
  {
    timeRange: '10:07:00 - 10:10:00',
    thumbnail: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTIwIiBoZWlnaHQ9IjkwIiB2aWV3Qm94PSIwIDAgMTIwIDkwIiBmaWxsPSJub25lIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPgo8cmVjdCB3aWR0aD0iMTIwIiBoZWlnaHQ9IjkwIiBmaWxsPSIjZjBmMGYwIi8+CjxjaXJjbGUgY3g9IjMwIiBjeT0iMzAiIHI9IjE1IiBmaWxsPSIjZTBlMGUwIi8+CjxyZWN0IHg9IjYwIiB5PSIyMCIgd2lkdGg9IjQwIiBoZWlnaHQ9IjIwIiBmaWxsPSIjZTBlMGUwIi8+CjxyZWN0IHg9IjIwIiB5PSI2MCIgd2lkdGg9IjgwIiBoZWlnaHQ9IjEwIiBmaWxsPSIjZTBlMGUwIi8+Cjx0ZXh0IHg9IjEwIiB5PSI4MCIgZm9udC1zaXplPSI4IiBmaWxsPSIjNjY2Ij7liqjnlLvliIbmnpA8L3RleHQ+Cjwvc3ZnPg=='
  },
  {
    timeRange: '10:07:00 - 10:10:00',
    thumbnail: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTIwIiBoZWlnaHQ9IjkwIiB2aWV3Qm94PSIwIDAgMTIwIDkwIiBmaWxsPSJub25lIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPgo8cmVjdCB3aWR0aD0iMTIwIiBoZWlnaHQ9IjkwIiBmaWxsPSIjZjBmMGYwIi8+CjxjaXJjbGUgY3g9IjMwIiBjeT0iMzAiIHI9IjE1IiBmaWxsPSIjZTBlMGUwIi8+CjxyZWN0IHg9IjYwIiB5PSIyMCIgd2lkdGg9IjQwIiBoZWlnaHQ9IjIwIiBmaWxsPSIjZTBlMGUwIi8+CjxyZWN0IHg9IjIwIiB5PSI2MCIgd2lkdGg9IjgwIiBoZWlnaHQ9IjEwIiBmaWxsPSIjZTBlMGUwIi8+Cjx0ZXh0IHg9IjEwIiB5PSI4MCIgZm9udC1zaXplPSI4IiBmaWxsPSIjNjY2Ij7liqjnlLvliIbmnpA8L3RleHQ+Cjwvc3ZnPg=='
  }
])

// 表格数据
const tableData = ref([
  {
    id: 1,
    sequence: 1,
    analysisName: '路径追踪',
    analysisType: '人员分析',
    screenshot: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA4MCA2MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjYwIiBmaWxsPSIjMzMzIi8+CjxjaXJjbGUgY3g9IjI1IiBjeT0iMjUiIHI9IjgiIGZpbGw9IiNGRkYiLz4KPHJlY3QgeD0iNDUiIHk9IjIwIiB3aWR0aD0iMjAiIGhlaWdodD0iMTAiIGZpbGw9IiNGRkYiLz4KPHR5cGUgeD0iMzAiIHk9IjQ1IiBmb250LXNpemU9IjEwIiBmaWxsPSIjRkZGIj4rNTwvdGV4dD4KPC9zdmc+',
    screenshotCount: 5,
    area: '水产大厦',
    camera: '摄像头1、水产大厦2楼、水产大厦3楼、水产大厦4楼、水产大厦5楼',
    analysisTime: '2024-10-07 12:17:29'
  },
  {
    id: 2,
    sequence: 1,
    analysisName: '路径追踪',
    analysisType: '人员分析',
    screenshot: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA4MCA2MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjYwIiBmaWxsPSIjMzMzIi8+CjxjaXJjbGUgY3g9IjI1IiBjeT0iMjUiIHI9IjgiIGZpbGw9IiNGRkYiLz4KPHJlY3QgeD0iNDUiIHk9IjIwIiB3aWR0aD0iMjAiIGhlaWdodD0iMTAiIGZpbGw9IiNGRkYiLz4KPHR5cGUgeD0iMzAiIHk9IjQ1IiBmb250LXNpemU9IjEwIiBmaWxsPSIjRkZGIj4rNTwvdGV4dD4KPC9zdmc+',
    screenshotCount: 5,
    area: '水产大厦',
    camera: '摄像头1、水产大厦2楼、水产大厦3楼、水产大厦4楼、水产大厦5楼',
    analysisTime: '2024-10-07 12:17:29'
  },
  {
    id: 3,
    sequence: 1,
    analysisName: '路径追踪',
    analysisType: '人员分析',
    screenshot: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA4MCA2MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjYwIiBmaWxsPSIjMzMzIi8+CjxjaXJjbGUgY3g9IjI1IiBjeT0iMjUiIHI9IjgiIGZpbGw9IiNGRkYiLz4KPHJlY3QgeD0iNDUiIHk9IjIwIiB3aWR0aD0iMjAiIGhlaWdodD0iMTAiIGZpbGw9IiNGRkYiLz4KPHR5cGUgeD0iMzAiIHk9IjQ1IiBmb250LXNpemU9IjEwIiBmaWxsPSIjRkZGIj4rNTwvdGV4dD4KPC9zdmc+',
    screenshotCount: 5,
    area: '水产大厦',
    camera: '摄像头1、水产大厦2楼、水产大厦3楼、水产大厦4楼、水产大厦5楼',
    analysisTime: '2024-10-07 12:17:29'
  },
  {
    id: 4,
    sequence: 1,
    analysisName: '路径追踪',
    analysisType: '人员分析',
    screenshot: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA4MCA2MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjYwIiBmaWxsPSIjMzMzIi8+CjxjaXJjbGUgY3g9IjI1IiBjeT0iMjUiIHI9IjgiIGZpbGw9IiNGRkYiLz4KPHJlY3QgeD0iNDUiIHk9IjIwIiB3aWR0aD0iMjAiIGhlaWdodD0iMTAiIGZpbGw9IiNGRkYiLz4KPHR5cGUgeD0iMzAiIHk9IjQ1IiBmb250LXNpemU9IjEwIiBmaWxsPSIjRkZGIj4rNTwvdGV4dD4KPC9zdmc+',
    screenshotCount: 5,
    area: '水产大厦',
    camera: '摄像头1、水产大厦2楼、水产大厦3楼、水产大厦4楼、水产大厦5楼',
    analysisTime: '2024-10-07 12:17:29'
  },
  {
    id: 5,
    sequence: 1,
    analysisName: '路径追踪',
    analysisType: '人员分析',
    screenshot: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA4MCA2MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjYwIiBmaWxsPSIjMzMzIi8+CjxjaXJjbGUgY3g9IjI1IiBjeT0iMjUiIHI9IjgiIGZpbGw9IiNGRkYiLz4KPHJlY3QgeD0iNDUiIHk9IjIwIiB3aWR0aD0iMjAiIGhlaWdodD0iMTAiIGZpbGw9IiNGRkYiLz4KPHR5cGUgeD0iMzAiIHk9IjQ1IiBmb250LXNpemU9IjEwIiBmaWxsPSIjRkZGIj4rNTwvdGV4dD4KPC9zdmc+',
    screenshotCount: 5,
    area: '水产大厦',
    camera: '摄像头1、水产大厦2楼、水产大厦3楼、水产大厦4楼、水产大厦5楼',
    analysisTime: '2024-10-07 12:17:29'
  },
  {
    id: 6,
    sequence: 1,
    analysisName: '路径追踪',
    analysisType: '人员分析',
    screenshot: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA4MCA2MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjYwIiBmaWxsPSIjMzMzIi8+CjxjaXJjbGUgY3g9IjI1IiBjeT0iMjUiIHI9IjgiIGZpbGw9IiNGRkYiLz4KPHJlY3QgeD0iNDUiIHk9IjIwIiB3aWR0aD0iMjAiIGhlaWdodD0iMTAiIGZpbGw9IiNGRkYiLz4KPHR5cGUgeD0iMzAiIHk9IjQ1IiBmb250LXNpemU9IjEwIiBmaWxsPSIjRkZGIj4rNTwvdGV4dD4KPC9zdmc+',
    screenshotCount: 5,
    area: '水产大厦',
    camera: '摄像头1、水产大厦2楼、水产大厦3楼、水产大厦4楼、水产大厦5楼',
    analysisTime: '2024-10-07 12:17:29'
  },
  {
    id: 7,
    sequence: 1,
    analysisName: '路径追踪',
    analysisType: '人员分析',
    screenshot: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA4MCA2MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjYwIiBmaWxsPSIjMzMzIi8+CjxjaXJjbGUgY3g9IjI1IiBjeT0iMjUiIHI9IjgiIGZpbGw9IiNGRkYiLz4KPHJlY3QgeD0iNDUiIHk9IjIwIiB3aWR0aD0iMjAiIGhlaWdodD0iMTAiIGZpbGw9IiNGRkYiLz4KPHR5cGUgeD0iMzAiIHk9IjQ1IiBmb250LXNpemU9IjEwIiBmaWxsPSIjRkZGIj4rNTwvdGV4dD4KPC9zdmc+',
    screenshotCount: 5,
    area: '水产大厦',
    camera: '摄像头1、水产大厦2楼、水产大厦3楼、水产大厦4楼、水产大厦5楼',
    analysisTime: '2024-10-07 12:17:29'
  },
  {
    id: 8,
    sequence: 1,
    analysisName: '路径追踪',
    analysisType: '人员分析',
    screenshot: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA4MCA2MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjYwIiBmaWxsPSIjMzMzIi8+CjxjaXJjbGUgY3g9IjI1IiBjeT0iMjUiIHI9IjgiIGZpbGw9IiNGRkYiLz4KPHJlY3QgeD0iNDUiIHk9IjIwIiB3aWR0aD0iMjAiIGhlaWdodD0iMTAiIGZpbGw9IiNGRkYiLz4KPHR5cGUgeD0iMzAiIHk9IjQ1IiBmb250LXNpemU9IjEwIiBmaWxsPSIjRkZGIj4rNTwvdGV4dD4KPC9zdmc+',
    screenshotCount: 5,
    area: '水产大厦',
    camera: '摄像头1、水产大厦2楼、水产大厦3楼、水产大厦4楼、水产大厦5楼',
    analysisTime: '2024-10-07 12:17:29'
  },
  {
    id: 9,
    sequence: 1,
    analysisName: '路径追踪',
    analysisType: '人员分析',
    screenshot: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA4MCA2MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjYwIiBmaWxsPSIjMzMzIi8+CjxjaXJjbGUgY3g9IjI1IiBjeT0iMjUiIHI9IjgiIGZpbGw9IiNGRkYiLz4KPHJlY3QgeD0iNDUiIHk9IjIwIiB3aWR0aD0iMjAiIGhlaWdodD0iMTAiIGZpbGw9IiNGRkYiLz4KPHR5cGUgeD0iMzAiIHk9IjQ1IiBmb250LXNpemU9IjEwIiBmaWxsPSIjRkZGIj4rNTwvdGV4dD4KPC9zdmc+',
    screenshotCount: 5,
    area: '水产大厦',
    camera: '摄像头1、水产大厦2楼、水产大厦3楼、水产大厦4楼、水产大厦5楼',
    analysisTime: '2024-10-07 12:17:29'
  }
])

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const total = computed(() => tableData.value.length)

// 当前页数据
const currentPageData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return tableData.value.slice(start, end)
})

// 选中行
const selectedRows = ref([])

// 方法
const handleSearch = () => {
  console.log('搜索:', searchForm)
  ElMessage.success('搜索完成')
}

const handleReset = () => {
  Object.assign(searchForm, {
    analysisName: '',
    analysisType: '',
    dateRange: []
  })
  ElMessage.info('已重置搜索条件')
}

// 高级搜索相关方法
const handleAdvancedSearch = (searchData) => {
  console.log('高级搜索:', searchData)
  // 这里可以根据搜索数据进行过滤
  if (searchData.keyword) {
    console.log('关键词搜索:', searchData.keyword)
  }
  if (searchData.deviceName) {
    console.log('设备名称搜索:', searchData.deviceName)
  }
  if (searchData.deviceId) {
    console.log('设备ID搜索:', searchData.deviceId)
  }
  if (searchData.selectedTags && searchData.selectedTags.length > 0) {
    console.log('标签搜索:', searchData.selectedTags)
  }
  if (searchData.dateRange && searchData.dateRange.length > 0) {
    console.log('日期范围搜索:', searchData.dateRange)
  }
  ElMessage.success('高级搜索完成')
}

const handleAdvancedSearchReset = () => {
  console.log('重置高级搜索')
  ElMessage.info('已重置高级搜索条件')
}

const handleUpload = () => {
  console.log('上传文件')
  ElMessage.success('上传功能')
}

const handleDownloadTemplate = () => {
  console.log('下载模板')
  ElMessage.success('下载模板')
}

const handleBatchOperation = () => {
  console.log('批量操作')
  ElMessage.success('批量操作')
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleExport = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要导出的记录')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要导出选中的 ${selectedRows.value.length} 条记录吗？`,
      '确认导出',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    ElMessage.success(`导出 ${selectedRows.value.length} 条记录成功`)
    // 这里可以添加实际的导出逻辑
  } catch {
    ElMessage.info('已取消导出')
  }
}

const handleDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的记录')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 条记录吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 删除逻辑
    selectedRows.value.forEach(row => {
      const index = tableData.value.findIndex(item => item.id === row.id)
      if (index !== -1) {
        tableData.value.splice(index, 1)
      }
    })
    
    selectedRows.value = []
    ElMessage.success('删除成功')
  } catch {
    ElMessage.info('已取消删除')
  }
}

const handlePlay = (row) => {
  console.log('播放分析视频:', row.analysisName)
  selectedRow.value = row
  videoDialogVisible.value = true
  ElMessage.success(`开始播放 ${row.analysisName} 的分析视频`)
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handleCurrentChange = (val) => {
  currentPage.value = val
}

// 视频播放相关方法
const selectVideo = (index) => {
  selectedVideoIndex.value = index
  console.log('选择视频:', analysisVideoList.value[index])
}
</script>

<style scoped>
.intelligent-analysis-result {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  padding: 20px;
  gap: 0;
}

/* 查询栏 - 查询栏背景颜色：#F0F2F5 */
.query-bar {
  background: #F0F2F5;
  border-radius: 8px 8px 0 0;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.search-form {
  width: 100%;
}

.search-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.query-input {
  width: 240px !important;
  flex: none;
}

/* 主要内容区域 - 容器无缝连接，零间隙 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

/* 操作按钮区域 */
.action-section {
  background: white;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-button-group {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-container {
  display: flex;
  align-items: center;
}

/* 导出删除按钮组合 */
.export-delete-group {
  display: flex;
  align-items: center;
  margin: 0;
  padding: 0;
}

.export-delete-group .el-button + .el-button {
  margin-left: 0 !important;
}

/* 导出按钮自定义样式 */
.export-btn-custom {
  height: 36px !important;
  border-radius: 18px 0 0 18px !important;
  border-right: none !important;
  padding: 0 16px !important;
  font-size: 14px;
  font-weight: 500;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
  margin-right: 0 !important;
  border-color: #d9d9d9 !important;
}

.export-btn-custom:hover,
.export-btn-custom:focus {
  border-right: none !important;
  border-color: #1A53FF !important;
}

.export-btn-custom:disabled {
  border-right: none !important;
  border-color: #e4e7ed !important;
}

/* 删除按钮自定义样式 */
.delete-btn-custom {
  height: 36px !important;
  border-radius: 0 18px 18px 0 !important;
  border-left: none !important;
  padding: 0 16px !important;
  font-size: 14px;
  font-weight: 500;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
  background: white !important;
  color: #f56c6c !important;
  border-color: #d9d9d9 !important;
  margin-left: 0 !important;
}

.delete-btn-custom:hover,
.delete-btn-custom:focus {
  border-left: none !important;
  background: white !important;
  color: #f56c6c !important;
  border-color: #f56c6c !important;
}

.delete-btn-custom:active {
  background: white !important;
  color: #f56c6c !important;
  border-color: #f56c6c !important;
  border-left: none !important;
}

.delete-btn-custom:disabled {
  border-left: none !important;
  background: #f5f5f5 !important;
  color: #c0c4cc !important;
  border-color: #e4e7ed !important;
}

/* 表格内容 */
.table-content {
  flex: 1;
  background: white;
  border-radius: 0 0 8px 8px;
  padding: 20px;
  overflow: hidden;
}

/* 操作列按钮样式 */
.operation-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
}

/* 分页容器样式 */
.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 表格样式调整 */
:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #fafafa;
  font-weight: 600;
}

:deep(.el-table td) {
  padding: 12px 0;
}

/* 截图容器样式 */
.screenshot-container {
  position: relative;
  display: inline-block;
}

.screenshot-image {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.screenshot-count {
  position: absolute;
  bottom: 2px;
  right: 2px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 2px 6px;
  border-radius: 2px;
  font-size: 12px;
  line-height: 1;
}

/* 主色调替换 - #1A53FF */
:deep(.el-button--primary) {
  background-color: #1A53FF;
  border-color: #1A53FF;
}

:deep(.el-button--primary:hover) {
  background-color: #4A72FF;
  border-color: #4A72FF;
}

:deep(.el-pagination .el-pagination__sizes .el-select .el-input .el-input__inner:focus),
:deep(.el-pagination .el-pagination__jump .el-input .el-input__inner:focus) {
  border-color: #1A53FF;
}

:deep(.el-pagination .btn-next:hover),
:deep(.el-pagination .btn-prev:hover),
:deep(.el-pagination .el-pager li:hover) {
  color: #1A53FF;
}

:deep(.el-pagination .el-pager li.active) {
  color: #1A53FF;
  font-weight: 700;
}

/* 视频播放弹窗样式 */
.video-dialog {
  :deep(.el-dialog__body) {
    padding: 0;
  }
}

.video-playback-container {
  display: flex;
  height: 680px;
  background: #f5f7fa;
  padding: 20px;
  gap: 20px;
}

/* 左侧视频列表区域 */
.video-list-section {
  width: 300px;
  background: white;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  padding-bottom: 10px;
  border-bottom: 1px solid #e9ecef;
}

/* 视频缩略图列表 */
.video-thumbnails {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 15px;
  max-height: 500px;
  overflow-y: auto;
}

.video-thumbnail-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.video-thumbnail-item.active {
  border-color: #1A53FF;
  background: #f0f9ff;
  box-shadow: 0 2px 8px rgba(26, 83, 255, 0.2);
}

.video-thumbnail-item:hover:not(.active) {
  border-color: #c0c4cc;
  background: #f8f9fa;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.thumbnail-image {
  width: 100%;
  height: 120px;
  position: relative;
  border-radius: 6px;
  overflow: hidden;
}

.thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.play-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  opacity: 0;
  transition: opacity 0.3s;
}

.video-thumbnail-item:hover .play-overlay {
  opacity: 1;
}

.play-icon {
  font-size: 18px;
}

.video-time-range {
  font-size: 14px;
  color: #666;
  text-align: center;
  font-weight: 500;
}

/* 播放器区域 */
.player-section {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.video-player {
  flex: 1;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.video-content {
  width: 100%;
  height: 100%;
  position: relative;
}

.video-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  padding: 20px;
  position: relative;
  background-image: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" fill="%23f0f0f0"/><circle cx="30" cy="30" r="20" fill="%23e0e0e0"/><rect x="60" y="20" width="30" height="20" fill="%23e0e0e0"/></svg>');
  background-size: cover;
  background-position: center;
}

.video-info {
  color: white;
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 2;
}

.device-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}

.video-time {
  font-size: 14px;
  opacity: 0.8;
}

/* 视频控制条 */
.video-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.8));
  padding: 20px;
  color: white;
}

.progress-bar {
  margin-bottom: 12px;
}

.progress-track {
  height: 4px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
  position: relative;
  cursor: pointer;
}

.progress-fill {
  height: 100%;
  background: #1A53FF;
  border-radius: 2px;
  transition: width 0.3s;
}

.progress-handle {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 12px;
  height: 12px;
  background: #1A53FF;
  border-radius: 50%;
  cursor: pointer;
  transition: left 0.3s;
}

.control-buttons {
  display: flex;
  align-items: center;
  gap: 16px;
}

.control-btn {
  background: rgba(255, 255, 255, 0.2) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  color: white !important;
  padding: 8px 12px;
  border-radius: 4px;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.3) !important;
}

.time-display {
  font-size: 14px;
  color: white;
}

.volume-control, .fullscreen-control {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.volume-icon, .fullscreen-icon {
  font-size: 16px;
}

.volume-text {
  font-size: 14px;
}
</style>