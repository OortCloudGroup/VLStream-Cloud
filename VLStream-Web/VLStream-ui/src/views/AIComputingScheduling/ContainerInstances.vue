<template>
  <div class="container-instances">
    <!-- 监控页面 -->
    <!-- 创建容器实例页面 -->
    <div v-if="showCreateView" class="create-view">
      <!-- 面包屑导航 -->
      <div class="breadcrumb-section">
        <el-breadcrumb separator=">">
          <el-breadcrumb-item @click="backToList" class="breadcrumb-link">容器实例</el-breadcrumb-item>
          <el-breadcrumb-item>新增容器实例</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <!-- 创建表单内容 -->
      <div class="create-content">
        <!-- 实例名称 -->
        <div class="form-section">
          <div class="form-label">实例名称</div>
          <el-input 
            v-model="createInstanceForm.name" 
            placeholder="请输入" 
            class="form-input"
          />
        </div>

        <!-- 资源类型 -->
        <div class="form-section">
          <div class="form-label">资源类型 <span class="required">*</span></div>
          <div class="resource-types">
            <!-- 资源类型标题行 -->
            <div class="resource-header-row">
              <!-- 预留勾选框空间 -->
              <div class="radio-space"></div>
              <div class="spec-columns">
                <div class="spec-column">
                  <div class="spec-header">资源类型</div>
                </div>
                <div class="spec-column">
                  <div class="spec-header">计算资源</div>
                </div>
                <div class="spec-column">
                  <div class="spec-header">GPU</div>
                </div>
                <div class="spec-column">
                  <div class="spec-header">磁盘配置</div>
                </div>
      </div>
    </div>

            <!-- 资源数据行 -->
            <div class="resource-data-rows">
              <div class="resource-data-row" v-for="(resource, index) in resourceOptions" :key="index">
                <el-radio v-model="createInstanceForm.resourceType" :value="`aerte${index + 1}`">
                  <div class="spec-columns">
                    <div class="spec-column">
                      <div class="spec-value">奥尔特云</div>
        </div>
                    <div class="spec-column">
                      <div class="spec-value">Intel 4核 | 40G</div>
                    </div>
                    <div class="spec-column">
                      <div class="spec-value">奥尔特云 64G*1</div>
                    </div>
                    <div class="spec-column">
                      <div class="spec-configs">
                        <div>系统盘：40GB</div>
                        <div>数据盘：——</div>
                      </div>
                    </div>
                  </div>
                </el-radio>
              </div>
            </div>
        </div>
      </div>
      
        <!-- 资源规格表格 -->
        <div class="form-section">
          <div class="form-label">资源规格：</div>
          <div class="spec-table">
            <table>
              <thead>
                <tr>
                  <th>CPU型号</th>
                  <th>vCPU</th>
                  <th>内存</th>
                  <th>GPU</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>Intel</td>
                  <td>4核</td>
                  <td>40G</td>
                  <td>奥尔特云 64G*1</td>
                </tr>
              </tbody>
            </table>
        </div>
        </div>

        <!-- 实例数量 -->
        <div class="form-section">
          <div class="form-label">实例数量 <span class="required">*</span></div>
          <div class="quantity-control">
            <el-button @click="decreaseQuantity" class="quantity-btn">-</el-button>
            <span class="quantity-value">{{ createInstanceForm.quantity }}</span>
            <el-button @click="increaseQuantity" class="quantity-btn">+</el-button>
        </div>
      </div>
      
                <!-- 镜像 -->
        <div class="form-section">
          <div class="form-label">镜像 <span class="required">*</span></div>
          <div class="image-types">
            <div class="image-options">
              <div class="image-option">
                <el-radio v-model="createInstanceForm.imageType" value="basic">
                  基础镜像
                </el-radio>
              </div>
              <div class="image-option">
                <el-radio v-model="createInstanceForm.imageType" value="app">
                  应用镜像
                </el-radio>
              </div>
              <div class="image-option">
                <el-radio v-model="createInstanceForm.imageType" value="custom">
                  自定义镜像
                </el-radio>
              </div>
              <div class="image-option">
                <el-radio v-model="createInstanceForm.imageType" value="address">
                  镜像地址
                </el-radio>
              </div>
            </div>
            <div class="image-content">
              <div class="image-selectors">
                <el-select v-model="createInstanceForm.selectedImage" placeholder="请选择" class="image-select">
                  <el-option label="ubuntu 20.04" value="ubuntu:20.04"></el-option>
                  <el-option label="centos 7" value="centos:7"></el-option>
                  <el-option label="pytorch 1.8" value="pytorch:1.8"></el-option>
                </el-select>
                <div class="arrow-icon">></div>
                <el-select v-model="createInstanceForm.selectedVersion" placeholder="请选择" class="image-select">
                  <el-option label="latest" value="latest"></el-option>
                  <el-option label="v1.0" value="v1.0"></el-option>
                  <el-option label="v2.0" value="v2.0"></el-option>
                </el-select>
              </div>
            </div>
          </div>
        </div>
      
        <!-- 操作按钮 -->
        <div class="form-actions">
          <el-button type="primary" @click="confirmCreate" class="confirm-btn">确定</el-button>
          <el-button @click="cancelCreate" class="cancel-btn">取消</el-button>
        </div>
        </div>
      </div>

    <!-- 监控页面 -->
    <div v-else-if="showMonitoringView" class="monitoring-view">
      <!-- 面包屑导航 -->
      <div class="breadcrumb-section">
        <el-breadcrumb separator=">">
          <el-breadcrumb-item @click="backToList" class="breadcrumb-link">容器实例</el-breadcrumb-item>
          <el-breadcrumb-item>监控</el-breadcrumb-item>
        </el-breadcrumb>
    </div>

      <!-- 监控内容 -->
      <div class="monitoring-content">
        <!-- 数据盘使用情况 -->
        <div class="disk-usage-section">
          <div class="usage-card">
            <div class="usage-header">
              <h3>数据盘</h3>
              <span class="usage-percentage">32%</span>
            </div>
            <div class="usage-details">138G/500G</div>
            <el-progress :percentage="32" :show-text="false" />
          </div>
        </div>

        <!-- GPU用量和GPU列表水平布局 -->
        <div class="gpu-main-section">
          <!-- GPU用量和图表容器 -->
          <div class="gpu-chart-container">
            <!-- GPU用量标题行 -->
            <div class="gpu-title-row">
              <div class="title-left">
                <h3>GPU用量</h3>
                <el-button type="primary" text>
                  <i class="el-icon-download"></i>
                  导出
                </el-button>
              </div>
              <div class="title-right">
                <div class="time-filters">
                  <span 
                    v-for="filter in timeFilters" 
                    :key="filter.value"
                    class="time-filter"
                    :class="{ active: activeTimeFilter === filter.value }"
                    @click="activeTimeFilter = filter.value"
                  >
                    {{ filter.label }}
                  </span>
                </div>
              </div>
            </div>
            
            <!-- GPU使用量图表 -->
            <div class="chart-content">
              <div class="chart-placeholder">
                <svg width="100%" height="300" viewBox="0 0 800 300">
                  <!-- 网格线 -->
                  <defs>
                    <pattern id="grid" width="80" height="30" patternUnits="userSpaceOnUse">
                      <path d="M 80 0 L 0 0 0 30" fill="none" stroke="#f0f0f0" stroke-width="1"/>
                    </pattern>
                  </defs>
                  <rect width="100%" height="100%" fill="url(#grid)" />
                  
                  <!-- Y轴线 -->
                  <line x1="50" y1="30" x2="50" y2="270" stroke="#e8e8e8" stroke-width="1"/>
                  
                  <!-- X轴线 -->
                  <line x1="50" y1="270" x2="750" y2="270" stroke="#e8e8e8" stroke-width="1"/>
                  
                  <!-- 趋势线 -->
                  <polyline
                    fill="none"
                    stroke="#1A53FF"
                    stroke-width="3"
                    points="80,180 150,140 220,200 290,120 360,160 430,100 500,180 570,130 640,170 710,110"
                  />
                  
                  <!-- 数据点 -->
                  <circle cx="80" cy="180" r="4" fill="#1A53FF"/>
                  <circle cx="150" cy="140" r="4" fill="#1A53FF"/>
                  <circle cx="220" cy="200" r="4" fill="#1A53FF"/>
                  <circle cx="290" cy="120" r="4" fill="#1A53FF"/>
                  <circle cx="360" cy="160" r="4" fill="#1A53FF"/>
                  <circle cx="430" cy="100" r="4" fill="#1A53FF"/>
                  <circle cx="500" cy="180" r="4" fill="#1A53FF"/>
                  <circle cx="570" cy="130" r="4" fill="#1A53FF"/>
                  <circle cx="640" cy="170" r="4" fill="#1A53FF"/>
                  <circle cx="710" cy="110" r="4" fill="#1A53FF"/>
                  
                  <!-- Y轴标签 -->
                  <text x="35" y="50" fill="#909399" font-size="12" text-anchor="end">100%</text>
                  <text x="35" y="90" fill="#909399" font-size="12" text-anchor="end">80%</text>
                  <text x="35" y="130" fill="#909399" font-size="12" text-anchor="end">60%</text>
                  <text x="35" y="170" fill="#909399" font-size="12" text-anchor="end">40%</text>
                  <text x="35" y="210" fill="#909399" font-size="12" text-anchor="end">20%</text>
                  <text x="35" y="250" fill="#909399" font-size="12" text-anchor="end">0%</text>
                  
                  <!-- X轴标签 -->
                  <text x="80" y="290" fill="#909399" font-size="12" text-anchor="middle">8-17</text>
                  <text x="180" y="290" fill="#909399" font-size="12" text-anchor="middle">8-18</text>
                  <text x="280" y="290" fill="#909399" font-size="12" text-anchor="middle">8-19</text>
                  <text x="380" y="290" fill="#909399" font-size="12" text-anchor="middle">8-20</text>
                  <text x="480" y="290" fill="#909399" font-size="12" text-anchor="middle">8-21</text>
                  <text x="580" y="290" fill="#909399" font-size="12" text-anchor="middle">8-22</text>
                  <text x="680" y="290" fill="#909399" font-size="12" text-anchor="middle">8-23</text>
                </svg>
              </div>
            </div>
          </div>
          
          <!-- GPU信息列表 -->
          <div class="gpu-list-section">
            <div class="gpu-info-table">
              <table>
                <thead>
                  <tr>
                    <th>GPU</th>
                    <th>最大</th>
                    <th>平均</th>
                    <th>当前</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>
                      <span class="gpu-status">●</span>
                      hs10-5
                    </td>
                    <td>76%</td>
                    <td>53%</td>
                    <td>48%</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情页面 -->
    <div v-else-if="showDetailsView" class="details-view">
      <!-- 面包屑导航 -->
      <div class="breadcrumb-section">
        <el-breadcrumb separator=">">
          <el-breadcrumb-item @click="backToList" class="breadcrumb-link">容器实例</el-breadcrumb-item>
          <el-breadcrumb-item>{{ detailsTitle }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <!-- 详情内容 -->
      <div class="details-content">
        <!-- 基本信息 -->
        <div class="form-section">
          <div class="form-label">实例名称</div>
          <div class="form-value">{{ detailsForm.name || '--' }}</div>
        </div>

        <div class="form-section">
          <div class="form-label">实例ID</div>
          <div class="form-value">{{ detailsForm.id || '--' }}</div>
        </div>

        <div class="form-section">
          <div class="form-label">状态</div>
          <div class="form-value">
            <el-tag :type="getStatusTagType(detailsForm.status)" size="small">
              <el-icon class="status-icon">
                <component :is="getStatusIcon(detailsForm.status)" />
              </el-icon>
              {{ getStatusText(detailsForm.status) }}
            </el-tag>
          </div>
        </div>

        <!-- 镜像信息 -->
        <div class="form-section">
          <div class="form-label">镜像地址</div>
          <div class="form-value">{{ detailsForm.image || '--' }}</div>
        </div>

        <div class="form-section">
          <div class="form-label">算法名称</div>
          <div class="form-value">{{ detailsForm.algorithmName || '--' }}</div>
        </div>

        <!-- 资源配置 -->
        <div class="form-section">
          <div class="form-label">CPU限制</div>
          <div class="form-value">{{ detailsForm.cpuLimit || '--' }}</div>
        </div>

        <div class="form-section">
          <div class="form-label">内存限制</div>
          <div class="form-value">{{ detailsForm.memoryLimit || '--' }}</div>
        </div>

        <div class="form-section">
          <div class="form-label">GPU限制</div>
          <div class="form-value">{{ detailsForm.gpuLimit || '--' }}</div>
        </div>

        <!-- 网络配置 -->
        <div class="form-section">
          <div class="form-label">端口映射</div>
          <div class="form-value">{{ detailsForm.portMappings || '--' }}</div>
        </div>

        <!-- 环境配置 -->
        <div class="form-section">
          <div class="form-label">环境变量</div>
          <div class="form-value">
            <pre>{{ detailsForm.envVariables || '--' }}</pre>
          </div>
        </div>

        <!-- 时间信息 -->
        <div class="form-section">
          <div class="form-label">创建时间</div>
          <div class="form-value">{{ detailsForm.createTime || '--' }}</div>
        </div>

        <div class="form-section">
          <div class="form-label">更新时间</div>
          <div class="form-value">{{ detailsForm.updateTime || '--' }}</div>
        </div>

        <!-- 描述信息 -->
        <div class="form-section">
          <div class="form-label">描述</div>
          <div class="form-value">{{ detailsForm.description || '--' }}</div>
        </div>

        <!-- 操作按钮 -->
        <div class="form-actions">
          <el-button @click="backToList">返回列表</el-button>
          <el-button type="primary" @click="editContainer(detailsForm)">编辑实例</el-button>
        </div>
      </div>
    </div>

    <!-- 列表页面 -->
    <div v-else-if="!showCreateView && !showMonitoringView && !showDetailsView" class="list-view">
      <!-- 搜索筛选区域 - 已注释，使用高级搜索组件替代 -->
      <!--
      <div class="search-section">
      <div class="search-form">
        <el-input
          v-model="searchKeyword"
          placeholder="实例名称/ID"
          clearable
          class="search-input"
        >
        </el-input>
        
        <DateRangePicker
          v-model="createTimeRange"
          start-placeholder="创建时间"
          end-placeholder="创建时间"
          width="500px"
          value-format="YYYY-MM-DD"
          format="YYYY-MM-DD"
        />
        
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="resetFilter">
          重置
        </el-button>
      </div>
    </div>
    -->

    <!-- 操作栏 -->
    <div class="action-section">
      <el-button type="primary" @click="showCreateView = true" class="add-btn-custom">
        <el-icon><Plus /></el-icon>
        创建容器实例
      </el-button>
      
      <!-- 高级搜索组件 -->
      <div class="advanced-search-group">
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

    <!-- 容器实例列表 -->
    <div class="list-section">
      <el-table 
        :data="filteredInstances" 
        :loading="loading"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="实例名称/ID" min-width="220">
          <template #default="{ row }">
            <div class="instance-info">
              <div class="instance-name">{{ row.name }}</div>
              <div class="instance-id">ID: {{ row.id }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              <el-icon class="status-icon">
                <component :is="getStatusIcon(row.status)" />
              </el-icon>
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="计算配置" min-width="200">
          <template #default="{ row }">
            <div class="compute-config">
              <div class="config-item">CPU: {{ row.cpuLimit || '未设置' }}</div>
              <div class="config-item">内存: {{ row.memoryLimit || '未设置' }}</div>
              <div class="config-item">GPU: {{ row.gpuLimit || '未设置' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="镜像信息" min-width="200">
          <template #default="{ row }">
            <div class="image-info">
              <div class="image-name">{{ row.image || '未设置' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="端口映射" min-width="120">
          <template #default="{ row }">
            <div class="port-info">
              {{ row.portMappings ? JSON.parse(row.portMappings || '{}').port || '-' : '-' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">
            {{ row.createTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="快捷访问" min-width="250">
          <template #default="{ row }">
            <div class="quick-access">
              <el-button 
                type="primary" 
                text 
                class="access-button"
                :disabled="row.status !== 'running'"
                @click="openJupyter(row)"
              >
                jupyter
              </el-button>
              <el-button 
                type="primary" 
                text 
                class="access-button"
                :disabled="row.status !== 'running'"
                @click="openWebConnection(row)"
              >
                web 连接
              </el-button>
              <el-button 
                type="primary" 
                text 
                class="access-button"
                :disabled="row.status !== 'running'"
                @click="openTensorBoard(row)"
              >
                TensorBoard
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button 
                v-if="row.status === 'stopped'"
                type="primary" 
                text 
                size="small" 
                @click="startContainer(row)"
              >
                启动
              </el-button>
              <el-button 
                v-if="row.status === 'running'"
                type="warning" 
                text 
                size="small" 
                @click="stopContainer(row)"
              >
                停止
              </el-button>
              <el-button 
                v-if="row.status === 'running'"
                type="info" 
                text 
                size="small" 
                @click="restartContainer(row)"
              >
                重启
              </el-button>
              <el-button type="primary" text size="small" @click="saveImage(row)">保存镜像</el-button>
              <el-button type="primary" text size="small" @click="viewDetails(row)">实例详情</el-button>
              <el-dropdown trigger="click">
                <el-button type="primary" text size="small">
                  更多
                  <el-icon><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="viewMonitoring(row)">监控</el-dropdown-item>
                    <el-dropdown-item @click="viewLogs(row)">查看日志</el-dropdown-item>
                    <el-dropdown-item @click="deleteContainer(row)" class="delete-item">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination-section">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="totalInstances"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 创建容器对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建容器实例"
      width="600px"
      @close="resetCreateForm"
    >
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="120px">
        <el-form-item label="容器名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入容器名称" />
        </el-form-item>
        
        <el-form-item label="算法镜像" prop="image">
          <el-select v-model="createForm.image" placeholder="请选择算法镜像" style="width: 100%">
            <el-option label="vlstream/face-recognition:v2.1.0" value="vlstream/face-recognition:v2.1.0" />
            <el-option label="vlstream/license-plate:v1.8.3" value="vlstream/license-plate:v1.8.3" />
            <el-option label="vlstream/behavior-analysis:v1.5.2" value="vlstream/behavior-analysis:v1.5.2" />
            <el-option label="vlstream/object-detection:v3.0.1" value="vlstream/object-detection:v3.0.1" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="CPU配置" prop="cpu">
          <el-select v-model="createForm.cpu" placeholder="请选择CPU配置" style="width: 100%">
            <el-option label="1 核心" value="1 core" />
            <el-option label="2 核心" value="2 cores" />
            <el-option label="4 核心" value="4 cores" />
            <el-option label="8 核心" value="8 cores" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="内存配置" prop="memory">
          <el-select v-model="createForm.memory" placeholder="请选择内存配置" style="width: 100%">
            <el-option label="2GB" value="2GB" />
            <el-option label="4GB" value="4GB" />
            <el-option label="8GB" value="8GB" />
            <el-option label="16GB" value="16GB" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="GPU配置" prop="gpu">
          <el-select v-model="createForm.gpu" placeholder="请选择GPU配置" style="width: 100%">
            <el-option label="NVIDIA GTX 1660" value="NVIDIA GTX 1660" />
            <el-option label="NVIDIA RTX 3080" value="NVIDIA RTX 3080" />
            <el-option label="NVIDIA RTX 4090" value="NVIDIA RTX 4090" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="端口映射" prop="port">
          <el-input v-model="createForm.port" placeholder="请输入端口号，如8080" />
        </el-form-item>
        
        <el-form-item label="环境变量">
          <el-input
            v-model="createForm.envVars"
            type="textarea"
            :rows="3"
            placeholder="请输入环境变量，格式：KEY1=VALUE1&#10;KEY2=VALUE2"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createContainer">创建</el-button>
      </template>
    </el-dialog>

    <!-- 日志查看对话框 -->
    <el-dialog
      v-model="showLogsDialog"
      :title="`${currentContainer?.name} - 运行日志`"
      width="80%"
    >
      <div class="logs-container">
        <div class="logs-header">
          <el-button size="small" @click="refreshLogs">
            <el-icon><Refresh /></el-icon>
            刷新日志
          </el-button>
          <el-button size="small" @click="clearLogs">
            <el-icon><Delete /></el-icon>
            清空显示
          </el-button>
        </div>
        <div class="logs-content">
          <div 
            v-for="(log, index) in displayLogs" 
            :key="index"
            class="log-entry"
            :class="getLogLevelClass(log.level)"
          >
            <span class="log-time">{{ log.time }}</span>
            <span class="log-level">{{ log.level }}</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Refresh, Search, Box, Cpu, Monitor, VideoCamera, 
  Delete, View, Warning, Check, Loading, ArrowDown
} from '@element-plus/icons-vue'
import DateRangePicker from '@/components/DateRangePicker.vue'
import AdvancedSearch from '@/components/AdvancedSearch.vue'
import {
  getContainerInstancePage,
  getContainerInstanceById,
  createContainerInstance,
  updateContainerInstance,
  deleteContainerInstance,
  batchDeleteContainerInstances,
  startContainerInstance,
  stopContainerInstance,
  restartContainerInstance,
  getContainerInstanceStatistics,
  checkContainerInstanceName
} from '@/api/containerInstance'

// 响应式数据
const searchKeyword = ref('')
const createTimeRange = ref(null)
const currentPage = ref(1)
const pageSize = ref(20)
const showMonitoringView = ref(false)
const showCreateView = ref(false)
const showDetailsView = ref(false)
const activeTimeFilter = ref('实时')

// 时间过滤器选项
const timeFilters = ref([
  { label: '实时', value: '实时' },
  { label: '近1小时', value: '近1小时' },
  { label: '近6小时', value: '近6小时' },
  { label: '近1天', value: '近1天' },
  { label: '近3天', value: '近3天' },
  { label: '近7天', value: '近7天' },
  { label: '近30天', value: '近30天' }
])

// 对话框控制
const showCreateDialog = ref(false)
const showLogsDialog = ref(false)
const currentContainer = ref(null)

// 资源统计
const resourceStats = ref({
  cpuUsage: 65,
  memoryUsage: 72,
  gpuUsage: 45
})

// 容器数据
const containers = ref([])
const displayLogs = ref([])
const loading = ref(false)
const selectedRows = ref([])
const detailsTitle = ref('容器详情')
const detailsForm = ref({})

// 创建表单
const createForm = ref({
  name: '',
  image: '',
  cpuLimit: '',
  memoryLimit: '',
  gpuLimit: '',
  algorithmId: null,
  description: '',
  envVariables: '',
  portMappings: ''
})

// 创建实例表单
const createInstanceForm = ref({
  name: '',
  resourceType: 'aerte1',
  quantity: 1,
  imageType: 'basic',
  selectedImage: '',
  selectedVersion: ''
})

// 资源选项数据
const resourceOptions = ref([
  { id: 1, name: '奥尔特云' },
  { id: 2, name: '奥尔特云' },
  { id: 3, name: '奥尔特云' },
  { id: 4, name: '奥尔特云' },
  { id: 5, name: '奥尔特云' }
])

// 表单验证规则
const createRules = {
  name: [{ required: true, message: '请输入容器名称', trigger: 'blur' }],
  image: [{ required: true, message: '请选择算法镜像', trigger: 'change' }],
  cpu: [{ required: true, message: '请选择CPU配置', trigger: 'change' }],
  memory: [{ required: true, message: '请选择内存配置', trigger: 'change' }],
  gpu: [{ required: true, message: '请选择GPU配置', trigger: 'change' }],
  port: [{ required: true, message: '请输入端口号', trigger: 'blur' }]
}

// 计算属性
const filteredInstances = computed(() => {
  // 由于使用API分页，直接返回containers
  return containers.value
})

const runningInstances = computed(() => {
  return containers.value.filter(c => c.status === 'running').length
})

const totalInstances = ref(0) // 改为ref，从API获取总数

// API方法
const loadContainerInstances = async () => {
  try {
    loading.value = true
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      name: searchKeyword.value || undefined,
      startTime: createTimeRange.value?.[0] || undefined,
      endTime: createTimeRange.value?.[1] || undefined
    }
    
    const response = await getContainerInstancePage(params)
    if (response.code === 200) {
      containers.value = response.data.records || []
      totalInstances.value = response.data.total || 0
    } else {
      ElMessage.error(response.message || '获取容器实例列表失败')
    }
  } catch (error) {
    console.error('加载容器实例失败:', error)
    ElMessage.error('获取容器实例列表失败')
  } finally {
    loading.value = false
  }
}

// 方法
const getStatusTagType = (status) => {
  const typeMap = {
    'running': 'success',
    'starting': 'warning',
    'stopped': 'info',
    'stopping': 'warning',
    'error': 'danger'
  }
  return typeMap[status] || 'info'
}

const getStatusIcon = (status) => {
  const iconMap = {
    'running': 'Check',
    'starting': 'Loading',
    'stopped': 'Warning',
    'stopping': 'Loading',
    'error': 'Delete'
  }
  return iconMap[status] || 'Warning'
}

const getStatusText = (status) => {
  const textMap = {
    'running': '运行中',
    'starting': '启动中',
    'stopped': '已停止',
    'stopping': '停止中',
    'error': '错误'
  }
  return textMap[status] || status
}

const getLogLevelClass = (level) => {
  const classMap = {
    'ERROR': 'log-error',
    'WARNING': 'log-warning',
    'INFO': 'log-info'
  }
  return classMap[level] || 'log-info'
}

const handleSearch = () => {
  currentPage.value = 1
  loadContainerInstances()
}

const resetFilter = () => {
  searchKeyword.value = ''
  createTimeRange.value = null
  currentPage.value = 1
  loadContainerInstances()
}

const refreshInstances = () => {
  loadContainerInstances()
  ElMessage.success('实例列表已刷新')
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadContainerInstances()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadContainerInstances()
}

const startContainer = async (container) => {
  try {
    const response = await startContainerInstance(container.id)
    if (response.code === 200) {
      ElMessage.success(`容器 ${container.name} 启动成功`)
      await loadContainerInstances() // 刷新列表
    } else {
      ElMessage.error(response.message || '启动容器失败')
    }
  } catch (error) {
    console.error('启动容器失败:', error)
    ElMessage.error('启动容器失败')
  }
}

const stopContainer = async (container) => {
  try {
    await ElMessageBox.confirm('确定要停止此容器吗？', '确认操作')
    const response = await stopContainerInstance(container.id)
    if (response.code === 200) {
      ElMessage.success(`容器 ${container.name} 已停止`)
      await loadContainerInstances() // 刷新列表
    } else {
      ElMessage.error(response.message || '停止容器失败')
    }
  } catch (error) {
    if (error === 'cancel') {
      ElMessage.info('操作已取消')
    } else {
      console.error('停止容器失败:', error)
      ElMessage.error('停止容器失败')
    }
  }
}

const restartContainer = async (container) => {
  try {
    const response = await restartContainerInstance(container.id)
    if (response.code === 200) {
      ElMessage.success(`容器 ${container.name} 重启成功`)
      await loadContainerInstances() // 刷新列表
    } else {
      ElMessage.error(response.message || '重启容器失败')
    }
  } catch (error) {
    console.error('重启容器失败:', error)
    ElMessage.error('重启容器失败')
  }
}

const viewLogs = (container) => {
  currentContainer.value = container
  // 模拟日志数据，实际应该从API获取
  displayLogs.value = [
    { time: new Date().toLocaleString(), level: 'INFO', message: '容器运行正常' },
    { time: new Date().toLocaleString(), level: 'INFO', message: '服务启动完成' }
  ]
  showLogsDialog.value = true
}

const viewDetails = async (container) => {
  try {
    const response = await getContainerInstanceById(container.id)
    if (response.code === 200) {
      detailsForm.value = response.data
      detailsTitle.value = `容器详情 - ${container.name}`
      showDetailsView.value = true
    } else {
      ElMessage.error(response.message || '获取容器详情失败')
    }
  } catch (error) {
    console.error('获取容器详情失败:', error)
    ElMessage.error('获取容器详情失败')
  }
}

const saveImage = (container) => {
  ElMessage.info(`保存镜像: ${container.name}`)
  // 这里可以实现保存镜像的逻辑
}

const viewMonitoring = (container) => {
  currentContainer.value = container
  showMonitoringView.value = true
}

const backToList = () => {
  showMonitoringView.value = false
  showCreateView.value = false
  showDetailsView.value = false
  currentContainer.value = null
  detailsForm.value = {}
}

const increaseQuantity = () => {
  createInstanceForm.value.quantity++
}

const decreaseQuantity = () => {
  if (createInstanceForm.value.quantity > 1) {
    createInstanceForm.value.quantity--
  }
}

const confirmCreate = async () => {
  try {
    // 验证表单
    if (!createInstanceForm.value.name.trim()) {
      ElMessage.warning('请输入实例名称')
      return
    }

    const createData = {
      name: createInstanceForm.value.name,
      image: createInstanceForm.value.selectedImage || 'ubuntu:20.04',
      cpuLimit: '4 cores',
      memoryLimit: '40GB',
      gpuLimit: '奥尔特云 64G*1',
      description: '通过页面创建的容器实例',
      envVariables: '{}',
      portMappings: '{}'
    }

    const response = await createContainerInstance(createData)
    if (response.code === 200) {
      ElMessage.success('容器实例创建成功')
      showCreateView.value = false
      resetCreateInstanceForm()
      await loadContainerInstances() // 刷新列表
    } else {
      ElMessage.error(response.message || '创建容器实例失败')
    }
  } catch (error) {
    console.error('创建容器实例失败:', error)
    ElMessage.error('创建容器实例失败')
  }
}

const cancelCreate = () => {
  showCreateView.value = false
  resetCreateInstanceForm()
}

const resetCreateInstanceForm = () => {
  createInstanceForm.value = {
    name: '',
    resourceType: 'aerte1',
    quantity: 1,
    imageType: 'basic',
    selectedImage: '',
    selectedVersion: ''
  }
}

const deleteContainer = async (container) => {
  try {
    await ElMessageBox.confirm('确定要删除此容器吗？此操作不可恢复。', '确认删除')
    const response = await deleteContainerInstance(container.id)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      await loadContainerInstances() // 刷新列表
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error === 'cancel') {
      ElMessage.info('已取消删除')
    } else {
      console.error('删除容器失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const refreshLogs = () => {
  if (currentContainer.value) {
    // 这里应该调用API获取最新日志
    displayLogs.value = [
      ...displayLogs.value,
      { time: new Date().toLocaleString(), level: 'INFO', message: '日志已刷新' }
    ]
    ElMessage.success('日志已刷新')
  }
}

const clearLogs = () => {
  displayLogs.value = []
}

const resetCreateForm = () => {
  createForm.value = {
    name: '',
    image: '',
    cpuLimit: '',
    memoryLimit: '',
    gpuLimit: '',
    algorithmId: null,
    description: '',
    envVariables: '',
    portMappings: ''
  }
}

const createContainer = async () => {
  try {
    // 验证表单
    if (!createForm.value.name.trim()) {
      ElMessage.warning('请输入容器名称')
      return
    }

    const response = await createContainerInstance(createForm.value)
    if (response.code === 200) {
      ElMessage.success('容器实例创建成功')
      showCreateDialog.value = false
      resetCreateForm()
      await loadContainerInstances() // 刷新列表
    } else {
      ElMessage.error(response.message || '创建容器实例失败')
    }
  } catch (error) {
    console.error('创建容器实例失败:', error)
    ElMessage.error('创建容器实例失败')
  }
}

const editContainer = (container) => {
  ElMessage.info(`编辑容器实例: ${container.name}`)
  // 这里可以实现编辑容器的逻辑
  // 例如，将当前的detailsForm设置为container，并打开创建对话框
  Object.assign(createForm.value, container)
  showCreateDialog.value = true
}

const openJupyter = (container) => {
  ElMessage.info(`打开Jupyter: ${container.name}`)
  // 这里可以实现打开Jupyter的逻辑
}

const openWebConnection = (container) => {
  ElMessage.info(`打开Web连接: ${container.name}`)
  // 这里可以实现打开Web连接的逻辑
}

const openTensorBoard = (container) => {
  ElMessage.info(`打开TensorBoard: ${container.name}`)
  // 这里可以实现打开TensorBoard的逻辑
}

// 高级搜索相关方法
const handleAdvancedSearch = (searchData) => {
  console.log('高级搜索:', searchData)
  
  // 更新搜索关键词
  if (searchData.keyword) {
    searchKeyword.value = searchData.keyword
  }
  if (searchData.instanceName) {
    searchKeyword.value = searchData.instanceName
  }
  if (searchData.instanceId) {
    searchKeyword.value = searchData.instanceId
  }
  if (searchData.dateRange && searchData.dateRange.length > 0) {
    createTimeRange.value = searchData.dateRange
  }
  
  handleSearch()
}

const handleAdvancedSearchReset = () => {
  console.log('重置高级搜索')
  searchKeyword.value = ''
  createTimeRange.value = []
  resetFilter()
}

const handleExport = () => {
  console.log('导出数据')
  ElMessage.success('导出数据')
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

onMounted(() => {
  // 加载容器实例数据  
  // loadContainerInstances()
})
</script>

<style scoped>
.container-instances {
  margin: 0 20px;
}

/* 搜索区域 */
.search-section {
  background: #F5F5F5;
  padding: 20px;
  border-radius: 8px 8px 0 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.search-form {
  display: flex;
  gap: 15px;
  align-items: center;
}

.search-input {
  width: 200px;
}

/* 操作栏 */
.action-section {
  background: white;
  padding: 20px;
  border-radius: 0;
  border-top: 1px solid #e8e8e8;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 高级搜索组件区域 */
.advanced-search-group {
  display: flex;
  align-items: center;
}

/* 新增按钮样式 */
.add-btn-custom {
  height: 36px;
  background: #1A53FF;
  border: 1px solid #1A53FF;
  border-radius: 18px;
  color: white;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 0 16px;
}

.add-btn-custom:hover {
  background: #4169E1;
  border-color: #4169E1;
}

.add-btn-custom:focus {
  background: #1A53FF;
  border-color: #1A53FF;
}

.add-btn-custom:active {
  background: #0D2DB8;
  border-color: #0D2DB8;
}

/* 列表区域 */
.list-section {
  background: white;
  padding: 20px;
  border-radius: 0 0 8px 8px;
  border-top: 1px solid #e8e8e8;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.list-section .el-table {
  width: 100%;
}

/* 表格操作列固定右侧 */
:deep(.el-table__fixed-right) {
  border-left: 1px solid #e8e8e8;
}

/* 实例信息样式 */
.instance-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.instance-name {
  font-weight: 500;
  color: #303133;
}

.instance-id {
  font-size: 12px;
  color: #909399;
}

.status-icon {
  margin-right: 4px;
}

/* 计算配置样式 */
.compute-config {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.config-item {
  font-size: 12px;
  color: #606266;
}

/* 磁盘配置样式 */
.disk-config {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.disk-item {
  font-size: 12px;
  color: #606266;
}

/* 快捷访问样式 */
.quick-access {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.access-button {
  font-size: 12px;
  padding: 0 8px;
  height: auto;
  min-height: auto;
}

/* 操作按钮样式 */
.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-buttons .el-button {
  font-size: 12px;
  padding: 0 8px;
  height: auto;
  min-height: auto;
}

/* 删除项红色样式 */
:deep(.delete-item) {
  color: #f56c6c !important;
}

:deep(.delete-item:hover) {
  color: #f56c6c !important;
  background-color: #fef0f0 !important;
}

/* 分页区域 */
.pagination-section {
  background: white;
  padding: 20px;
  border-radius: 0 0 8px 8px;
  border-top: 1px solid #e8e8e8;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: center;
}

/* 日志相关样式 */
.logs-container {
  max-height: 500px;
}

.logs-header {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.logs-content {
  max-height: 400px;
  overflow-y: auto;
  background: #1e1e1e;
  padding: 16px;
  border-radius: 4px;
  font-family: monospace;
}

.log-entry {
  display: flex;
  margin-bottom: 8px;
  font-size: 12px;
}

.log-time {
  color: #999;
  margin-right: 8px;
  min-width: 140px;
}

.log-level {
  margin-right: 8px;
  min-width: 60px;
}

.log-message {
  flex: 1;
}

.log-error {
  color: #f56c6c;
}

.log-warning {
  color: #e6a23c;
}

.log-info {
  color: #67c23a;
}

/* 监控页面样式 */
.monitoring-view {
  margin: 0 20px;
}

.breadcrumb-section {
  background: white;
  padding: 16px 20px;
  border-radius: 8px 8px 0 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.breadcrumb-link {
  color: #409eff;
  cursor: pointer;
}

.breadcrumb-link:hover {
  text-decoration: underline;
}

.monitoring-content {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin-top: 0;
}

.disk-usage-section {
  width: 709px;
}

.usage-card {
  background: white;
  padding: 20px;
  border-radius: 8px 8px 0 0;
  border-top: 1px solid #e8e8e8;
  border-right: 1px solid #e8e8e8;
  border-bottom: 1px solid #e8e8e8;
  border-left: 1px solid #e8e8e8;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.usage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.usage-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  line-height: 1.2;
}

.usage-percentage {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.usage-details {
  color: #909399;
  font-size: 14px;
  margin-bottom: 16px;
}

.gpu-main-section {
  display: flex;
  gap: 0;
}

.gpu-chart-container {
  width: 1000px;
  background: white;
  border-radius: 8px 0 0 8px;
  border: 1px solid #e8e8e8;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.gpu-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e8e8e8;
}

.title-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.title-left h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  line-height: 1.2;
}

.title-right {
  display: flex;
  align-items: center;
}

.chart-content {
  padding: 20px;
}

.gpu-list-section {
  width: 610px;
  background: white;
  border-radius: 0 8px 8px 0;
  border: 1px solid #e8e8e8;
  border-left: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.time-filters {
  display: flex;
  gap: 15px;
}

.time-filter {
  padding: 4px 8px;
  cursor: pointer;
  color: #909399;
  font-size: 12px;
  border-radius: 4px;
  transition: all 0.3s;
}

.time-filter:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.time-filter.active {
  background-color: #409eff;
  color: white;
}

.gpu-info-table {
  margin: 0;
}

.gpu-info-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  min-width: 280px;
}

.gpu-info-table th,
.gpu-info-table td {
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1px solid #e8e8e8;
}

.gpu-info-table th {
  background-color: #f5f7fa;
  color: #909399;
  font-weight: normal;
}

.gpu-info-table td {
  color: #606266;
}

.gpu-status {
  color: #67c23a;
  margin-right: 8px;
}

.chart-placeholder {
  width: 100%;
  height: 300px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  background: #ffffff;
}

/* 创建页面样式 */
.create-view {
  margin: 0 20px;
}

.create-view .breadcrumb-section {
  width: 1120px;
}

.create-content {
  width: 1120px;
  background: white;
  padding: 20px;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.form-section {
  margin-bottom: 24px;
}

.form-label {
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
  display: block;
}

.required {
  color: #f56c6c;
}

.form-input {
  width: 300px;
}

.resource-types {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  background: white;
}

.resource-header-row {
  background: #FAFAFA;
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
}

.radio-space {
  width: 32px;
  margin-right: 12px;
  flex-shrink: 0;
}

.resource-header-row .spec-columns {
  flex: 1;
  grid-template-columns: 120px 180px 180px 1fr;
  gap: 20px;
  align-items: start;
}

.resource-data-rows {
  background: white;
}

.resource-data-row {
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
  transition: all 0.3s;
}

.resource-data-row:last-child {
  border-bottom: none;
}

.resource-data-row:hover {
  background: #f8f9ff;
}

.resource-data-row :deep(.el-radio) {
  width: 100%;
  display: flex;
  align-items: center;
}

.resource-data-row :deep(.el-radio__input) {
  margin-right: 12px;
  flex-shrink: 0;
}

.resource-data-row :deep(.el-radio__label) {
  width: 100%;
  padding-left: 0;
  display: flex;
  align-items: center;
}

.resource-option {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  background: white;
  transition: all 0.3s;
}

.resource-option.selected {
  border-color: #1A53FF;
  background: #f8f9ff;
}

.resource-option:hover {
  border-color: #1A53FF;
}

.resource-option-fixed {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  background: #FAFAFA;
}

.resource-header {
  margin-bottom: 12px;
}

.spec-columns {
  display: grid;
  grid-template-columns: 120px 180px 180px 1fr;
  gap: 20px;
  align-items: start;
}

.spec-column {
  display: flex;
  flex-direction: column;
  gap: 4px;
  justify-content: center;
  align-items: flex-start;
}

.spec-header {
  font-size: 12px;
  color: #909399;
  text-align: left;
  width: 100%;
}

.spec-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  display: flex;
  align-items: center;
  min-height: 24px;
  width: 100%;
  justify-content: flex-start;
}

.spec-configs {
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-items: flex-start;
  width: 100%;
}

.spec-configs div {
  font-size: 12px;
  color: #606266;
}

.spec-table {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
}

.spec-table table {
  width: 100%;
  border-collapse: collapse;
}

.spec-table th,
.spec-table td {
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1px solid #e8e8e8;
}

.spec-table th {
  background: #f5f7fa;
  color: #909399;
  font-weight: normal;
  font-size: 12px;
}

.spec-table td {
  color: #606266;
  font-size: 14px;
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: 0;
}

.quantity-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid #dcdfe6;
  background: white;
  color: #606266;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quantity-btn:first-child {
  border-radius: 4px 0 0 4px;
}

.quantity-btn:last-child {
  border-radius: 0 4px 4px 0;
}

.quantity-value {
  width: 50px;
  height: 32px;
  border: 1px solid #dcdfe6;
  border-left: none;
  border-right: none;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #303133;
}

.image-types {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

.image-options {
  display: flex;
  background: #f8f9fa;
  border-bottom: 1px solid #e8e8e8;
  padding: 16px 20px;
  gap: 30px;
}

.image-option {
  display: flex;
  align-items: center;
}

.image-option :deep(.el-radio) {
  font-size: 14px;
  color: #606266;
}

.image-option :deep(.el-radio__label) {
  font-size: 14px;
  color: #606266;
}

.image-content {
  padding: 16px;
  background: white;
}

.image-selectors {
  display: flex;
  align-items: center;
  gap: 12px;
}

.image-select {
  width: 200px;
}

.arrow-icon {
  font-size: 16px;
  color: #909399;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e8e8e8;
}

.confirm-btn {
  width: 120px;
  height: 40px;
  background: #1A53FF;
  border: 1px solid #1A53FF;
  border-radius: 4px;
  color: white;
  font-size: 14px;
}

.confirm-btn:hover {
  background: #4169E1;
  border-color: #4169E1;
}

.cancel-btn {
  width: 120px;
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  color: #606266;
  font-size: 14px;
  background: white;
}

.cancel-btn:hover {
  color: #1A53FF;
  border-color: #1A53FF;
}

/* 详情页面样式 */
.details-view {
  margin: 0 20px;
}

.details-view .breadcrumb-section {
  width: 1120px;
}

.details-content {
  width: 1120px;
  background: white;
  padding: 20px;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.details-content .form-section {
  margin-bottom: 24px;
}

.details-content .form-label {
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
  display: block;
}

.details-content .form-value {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap; /* 保留换行 */
  word-break: break-all; /* 允许在单词内换行 */
}

.details-content .form-value pre {
  background-color: #f4f4f4;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
  overflow-x: auto; /* 允许水平滚动 */
  font-size: 12px;
  color: #333;
  line-height: 1.5;
}
</style> 