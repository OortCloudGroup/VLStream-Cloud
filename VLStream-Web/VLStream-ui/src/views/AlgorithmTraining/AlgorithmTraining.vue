<template>
  <div class="page-container">
    <!-- 列表视图 -->
    <div v-if="!showTrainingConfig && !showVersionConfig && !showValidationView" class="list-view">
      <!-- 介绍内容区 -->
      <div class="intro-section">
        <div class="intro-header">
          <h2 class="intro-title">零代码训练介绍</h2>
          <CollapseToggle 
            :is-expanded="isIntroExpanded" 
            @toggle="handleIntroToggle"
          />
        </div>

        <div v-show="isIntroExpanded" class="intro-content">
          <p class="intro-text">
            面向应用开发者提供AutoDL训练练模式及高阶训练模式，AutoDL训练模式自动化程度高，高级训练模式支持训练参数灵活性高，两种训练模式可以灵活选择。对训练完成的任务可通过完整评估报告、校验来验证效果，满足业务应用需求的任务可发布为模型进行后续部署操作。
          </p>

          <!-- 流程步骤 -->
          <div class="process-steps">
            <div class="step-item">
              <div class="step-icon step-1">
                <img :src="trainIcon" alt="发起训练" class="step-img" />
              </div>
              <h3>1.发起训练</h3>
              <p>选择训练方式并发起成功训练参数配置</p>
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
            </div>

            <div class="step-item">
              <div class="step-icon step-2">
                <img :src="selectDatasetIcon" alt="选择标注集" class="step-img" />
              </div>
              <h3>2.选择标注集</h3>
              <p>选择算法标注集，自动或手动配置智能脑训练策略</p>
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
            </div>

            <div class="step-item">
              <div class="step-icon step-3">
                <img :src="evaluateIcon" alt="评估校验" class="step-img" />
              </div>
              <h3>3.评估校验</h3>
              <p>算法训练完成后，通过评估报告，校验验证性能效果</p>
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
            </div>

            <div class="step-item">
              <div class="step-icon step-4">
                <img :src="publishModelIcon" alt="发布为模型" class="step-img" />
              </div>
              <h3>4.发布为模型</h3>
              <p>效果满足业务需求的任务可发布为模型，进行部署操作</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 主内容区域 -->
      <div class="main-content">
        <!-- 工具栏 -->
        <div class="toolbar">
          <div class="toolbar-left">
            <ActionButtonGroup
              :selected-count="selectedRows.length"
              @add="handleAdd"
              @edit="handleEdit"
              @delete="handleDelete"
            />
<!--            <el-button type="success" size="small" @click="testUpdateStatus" style="margin-left: 10px;">-->
<!--              测试状态更新-->
<!--            </el-button>-->
          </div>
          
          <div class="toolbar-right">
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
        
        <!-- 表格区域 -->
        <div class="table-section">
      <el-table 
        :data="currentPageData" 
        stripe 
        style="width: 100%"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        @row-click="handleRowClick"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="modelId" label="模型ID" width="80" align="center" />
        <el-table-column prop="algorithmName" label="算法名称" min-width="160" />
        <el-table-column prop="trainAlgorithm" label="训练算法" min-width="200" show-overflow-tooltip />
        <el-table-column prop="trainStatus" label="训练状态" width="120" align="center">
          <template #default="scope">
            <el-tag
              :type="getStatusType(scope.row.trainStatus)"
              size="small"
              class="status-tag">
              <span class="status-dot" :class="getStatusClass(scope.row.trainStatus)"></span>
              {{ getTrainStatusText(scope.row.trainStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="modelEffect" label="模型效果" width="200" align="center">
          <template #default="scope">
            <div v-if="scope.row.trainStatus === '训练完成'" class="model-metrics">
              <div class="metric-item">
                <span class="metric-label">mapValue:</span>
                <span class="metric-value">{{ scope.row.mapValue }}</span>
                <el-icon class="info-icon"><QuestionFilled /></el-icon>
              </div>
              <div class="metric-item">
                <span class="metric-label">精确度:</span>
                <span class="metric-value">{{ scope.row.precision }}</span>
                <el-icon class="info-icon"><QuestionFilled /></el-icon>
              </div>
              <div class="metric-item">
                <span class="metric-label">召回率:</span>
                <span class="metric-value">{{ scope.row.recall }}</span>
                <el-icon class="info-icon"><QuestionFilled /></el-icon>
              </div>
              <el-button type="text" size="small" class="detail-link">完整评估结果</el-button>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="targetModel" label="对应模型" min-width="120" align="center">
          <template #default="scope">
            <span v-if="scope.row.targetModel">{{ scope.row.targetModel }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right" align="right">
          <template #default="scope">
            <div class="table-action-buttons">
              <!-- 空状态：训练和更多 -->
              <template v-if="!scope.row.trainStatus || scope.row.trainStatus === '等待'">
                <el-button 
                  type="primary"
                  text
                  size="small"
                  @click="handleTrain(scope.row)"
                >
                  训练
                </el-button>
                <el-dropdown @command="(command) => handleMoreAction(command, scope.row)">
                  <el-button type="primary" text size="small">
                    更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="version">查看版本配置</el-dropdown-item>
                      <el-dropdown-item command="history">历史版本</el-dropdown-item>
                      <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>

              <!-- 训练中状态：终止训练和更多 -->
              <template v-else-if="scope.row.trainStatus === '训练中'">
                <el-button 
                  type="danger"
                  text
                  size="small"
                  @click="handleStopTraining(scope.row)"
                >
                  终止训练
                </el-button>
                <el-dropdown @command="(command) => handleMoreAction(command, scope.row)">
                  <el-button type="primary" text size="small">
                    更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="version">查看版本配置</el-dropdown-item>
                      <el-dropdown-item command="history">历史版本</el-dropdown-item>
                      <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>

              <!-- 训练完成状态：下载模型、重新训练、发布为模型、校验和更多 -->
              <template v-else-if="scope.row.trainStatus === '训练完成'">
                <div class="completed-actions">
                  <el-button
                    type="success"
                    text
                    size="small"
                    @click="handleDownloadModel(scope.row)"
                    :icon="Download"
                  >
                    下载模型
                  </el-button>
                  <el-button
                    type="warning"
                    text
                    size="small"
                    @click="handleRetrainModel(scope.row)"
                    :icon="Refresh"
                  >
                    重新训练
                  </el-button>
                  <el-button
                    type="primary"
                    text
                    size="small"
                    @click="handlePublishModel(scope.row)"
                  >
                    发布为模型
                  </el-button>
                  <el-button
                    type="info"
                    text
                    size="small"
                    @click="handleValidateModel(scope.row)"
                  >
                    校验
                  </el-button>
                  <el-dropdown @command="(command) => handleMoreAction(command, scope.row)">
                    <el-button type="primary" text size="small">
                      更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="version">查看版本配置</el-dropdown-item>
                        <el-dropdown-item command="history">历史版本</el-dropdown-item>
                        <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>

              <!-- 训练失败状态：重新训练和更多 -->
              <template v-else-if="scope.row.trainStatus === '训练失败'">
                <el-button 
                  type="primary"
                  text
                  size="small"
                  @click="handleTrain(scope.row)"
                >
                  重新训练
                </el-button>
                <el-dropdown @command="(command) => handleMoreAction(command, scope.row)">
                  <el-button type="primary" text size="small">
                    更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="version">查看版本配置</el-dropdown-item>
                      <el-dropdown-item command="history">历史版本</el-dropdown-item>
                      <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>

              <!-- 其他状态：更多 -->
              <template v-else>
                <el-dropdown @command="(command) => handleMoreAction(command, scope.row)">
                  <el-button type="primary" text size="small">
                    更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="version">查看版本配置</el-dropdown-item>
                      <el-dropdown-item command="history">历史版本</el-dropdown-item>
                      <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
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
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
        </div>
      </div>

    <!-- 版本配置视图 -->
    <div v-if="showVersionConfig" class="version-config-view">
      <!-- 导航栏 -->
      <div class="content-header"></div>

      <div class="version-config-container">
        <div class="version-content-block">
          <!-- 面包屑导航 -->
          <div class="breadcrumb">
            <span class="breadcrumb-item" @click="handleBackFromVersion">算法训练</span>
            <span class="breadcrumb-separator">></span>
            <span class="breadcrumb-item active">查看版本配置</span>
          </div>

          <!-- 基础信息 -->
                    <!-- Basic Info -->
          <div class="basic-info-section">
            <h3 class="section-title">基础信息</h3>
            <div class="info-grid">
              <div class="info-row">
                <div class="info-item">
                  <span class="info-label">训练名称：</span>
                  <span class="info-value">{{ currentVersionItem?.algorithmName || currentVersionItem?.originalData?.taskName || '-' }}</span>
                </div>
              </div>
              <div class="info-row">
                <div class="info-item">
                  <span class="info-label">训练算法：</span>
                  <span class="info-value">{{ currentVersionItem?.originalData?.algorithmName || currentVersionItem?.trainAlgorithm || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">训练状态：</span>
                  <span class="info-value">{{ getTrainStatusText(currentVersionItem?.trainStatus)}}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- History Models -->
          <div class="data-details-section">
            <h3 class="section-title">历史版本</h3>
            <el-table
              :data="modelHistoryList"
              v-loading="modelHistoryLoading"
              class="history-table"
            >
              <el-table-column type="index" label="序号" width="70" />
              <el-table-column prop="modelName" label="模型名称" min-width="140" />
              <el-table-column prop="version" label="版本" width="80" />
              <el-table-column prop="modelFormatDesc" label="格式" width="90" />
              <el-table-column prop="statusDesc" label="状态" width="90" />
              <el-table-column prop="formattedAccuracy" label="Accuracy" width="110" />
              <el-table-column prop="modelPath" label="模型路径" min-width="220" show-overflow-tooltip />
              <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
              <el-table-column prop="downloadCount" label="下载次数" width="110" />
              <el-table-column prop="createTime" label="创建时间" width="170" />
            </el-table>

            <div class="history-pagination">
              <el-pagination
                v-model:current-page="modelHistoryCurrentPage"
                v-model:page-size="modelHistoryPageSize"
                :page-sizes="[5, 10, 20, 50]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="modelHistoryTotal"
                @size-change="handleHistorySizeChange"
                @current-change="handleHistoryCurrentChange"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 校验模型视图 -->
    <div v-if="showValidationView" class="validation-view">
      <!-- 导航栏 -->
      <div class="content-header"></div>

      <div class="validation-config-container">
        <div class="validation-content-block">
          <!-- 面包屑导航 -->
          <div class="breadcrumb">
            <span class="breadcrumb-item" @click="handleBackFromValidation">算法训练</span>
            <span class="breadcrumb-separator">></span>
            <span class="breadcrumb-item active">校验模型</span>
          </div>

          <!-- 模型选择区域 -->
          <div class="model-selection-section">
            <div class="model-info-inputs">
              <el-select 
                v-model="validationModelName" 
                class="model-name-select"
                placeholder="选择模型"
              >
                <el-option
                  v-for="item in modelNameOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-select 
                v-model="validationVersion" 
                class="model-version-select"
                placeholder="选择版本"
              >
                <el-option
                  v-for="item in versionOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </div>
          </div>

          <!-- 图片上传和阈值配置区域 -->
          <div class="upload-threshold-section">
            <!-- 图片上传区域 -->
            <div class="image-upload-area">
              <div 
                class="upload-area"
                @click="handleImageUpload"
                @drop="handleImageDrop"
                @dragover.prevent
                @dragenter.prevent
              >
                <div class="upload-icon">
                  <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
                    <rect x="8" y="16" width="48" height="36" rx="4" stroke="#d9d9d9" stroke-width="2"/>
                    <circle cx="20" cy="28" r="4" stroke="#d9d9d9" stroke-width="2"/>
                    <path d="M40 44L32 36L24 44H40Z" stroke="#d9d9d9" stroke-width="2"/>
                  </svg>
                </div>
                <el-button type="primary" class="upload-btn">
                  点击添加图片
                </el-button>
                <div class="upload-tips">
                  <p class="drag-tip">或拖拽图片至此处</p>
                  <p class="format-tip">图片类型为jpg、png、bmp、jpeg，图片大小限制在4M内</p>
                </div>
              </div>
            </div>

            <!-- 阈值配置区域 -->
            <div class="threshold-config-area">
              <div class="threshold-section">
                <div class="threshold-header">
                  <span class="threshold-label">调整阈值</span>
                  <span class="threshold-value">{{ confidenceThreshold }}</span>
                </div>
                <div class="threshold-slider">
                  <el-slider
                    v-model="confidenceThreshold"
                    :min="0"
                    :max="100"
                    :step="1"
                    show-stops
                  />
                </div>
                <div class="threshold-info">
                  <div class="threshold-tag">
                    <span class="tag-label">预测标签</span>
                  </div>
                  <div class="confidence-display">
                    置信度>{{ confidenceThreshold }}.00%
                  </div>
                </div>
              </div>

              <!-- 预测标签结果显示 -->
              <div class="prediction-result">
                <span class="no-result-text">没有满足检测的目标结果</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 训练配置视图 -->
    <div v-if="showTrainingConfig" class="training-config-view">
      <!-- 导航栏 -->
      <div class="content-header">
        <div class="breadcrumb">
          <span class="breadcrumb-item" @click="handleBackToList">算法训练</span>
          <span class="breadcrumb-separator">></span>
          <span class="breadcrumb-item active">训练</span>
        </div>
      </div>

      <div class="training-config-container">
        <!-- 左侧配置区域 -->
        <div class="config-sidebar">
          <!-- 训练方式 -->
          <div class="config-section">
            <h3 class="section-title">训练方式</h3>
            <div class="training-mode-tabs">
              <div 
                class="mode-tab" 
                :class="{ active: trainingMode === 'auto' }"
                @click="trainingMode = 'auto'"
              >
                AutoDL模式
              </div>
              <div 
                class="mode-tab" 
                :class="{ active: trainingMode === 'advanced' }"
                @click="trainingMode = 'advanced'"
              >
                高级调参模式
              </div>
            </div>
          </div>

          <!-- 训练配置 -->
          <div class="config-section">
            <h3 class="section-title">训练配置</h3>
            
            <!-- 导出类型 -->
<!--            <div class="config-item">-->
<!--              <label class="config-label">导出类型-->
<!--                <el-icon class="help-icon"><QuestionFilled /></el-icon>-->
<!--              </label>-->
<!--              <div class="radio-options">-->
<!--                <el-radio v-model="exportType" label="仅导出模型">-->
<!--                  仅导出模型-->
<!--                  <el-icon class="help-icon"><QuestionFilled /></el-icon>-->
<!--                </el-radio>-->
<!--                <el-radio v-model="exportType" label="导出模型文件与离线SDK">-->
<!--                  导出模型文件与离线SDK-->
<!--                  <el-icon class="help-icon"><QuestionFilled /></el-icon>-->
<!--                </el-radio>-->
<!--              </div>-->
<!--            </div>-->

            <!-- 选择算法 -->
<!--            <div class="config-item">-->
<!--              <label class="config-label">选择算法 -->
<!--                <el-icon class="help-icon"><QuestionFilled /></el-icon>-->
<!--              </label>-->
<!--              <div class="algorithm-options">-->
<!--                <el-radio v-model="selectedAlgorithm" label="超简作图">-->
<!--                  超简作图-->
<!--                  <el-icon class="help-icon"><QuestionFilled /></el-icon>-->
<!--                </el-radio>-->
<!--                <el-radio v-model="selectedAlgorithm" label="富标格">-->
<!--                  富标格-->
<!--                  <el-icon class="help-icon"><QuestionFilled /></el-icon>-->
<!--                </el-radio>-->
<!--                <el-radio v-model="selectedAlgorithm" label="高精版">-->
<!--                  高精版-->
<!--                  <el-icon class="help-icon"><QuestionFilled /></el-icon>-->
<!--                </el-radio>-->
<!--              </div>-->
<!--            </div>-->

            <!-- 高级训练配置 -->
            <div class="config-item">
              <div class="switch-item">
                <span class="switch-label">高级训练配置</span>
                
              </div>
              <el-switch v-model="advancedConfig" />
              <div class="config-tip">
                以下高级配置最佳设置一般情况下不需要修改，如以需要任何类型调整，请根据实际情况调整
              </div>
            </div>

            <!-- 备入调分辨率 -->
            <div class="config-item">
              <label class="config-label">备入调分辨率 
                <el-icon class="help-icon"><QuestionFilled /></el-icon>
              </label>
              <el-select v-model="resolution" class="config-select">
                <el-option label="Auto" value="auto" />
              </el-select>
            </div>

            <!-- 训练完成后同步发布为模型 -->
            <div class="config-item">
              <label class="config-label">训练完成后同步发布为模型 
                <el-icon class="help-icon"><QuestionFilled /></el-icon>
              </label>
              <div class="publish-options">
                <el-radio v-model="autoPublish" label="yes">是</el-radio>
                <el-radio v-model="autoPublish" label="no">否</el-radio>
              </div>
            </div>
          </div>

          <!-- 添加数据集 -->
          <div class="config-section">
            <h3 class="section-title">添加数据集</h3>
            
            <div class="config-item">
<!--              <label class="config-label">添加标注集 *</label>-->
              <el-button type="text" class="add-dataset-btn" @click="openDatasetSelector">
                <el-icon><Plus /></el-icon>
                请选择
              </el-button>
              
              <!-- 数据集列表 -->
              <div class="dataset-table" v-if="selectedDatasets.length > 0">
                <div class="dataset-header">
                  <span>数据集</span>
                  <span>类型</span>
                  <span>进度</span>
                  <span>操作</span>
                </div>
                <div
                  v-for="dataset in selectedDatasets"
                  :key="dataset.value"
                  class="dataset-row">
                  <span>{{ dataset.annotationName }}</span>
                  <span>{{ dataset.typeLabel }}</span>
                  <span>{{ dataset.progress }}%</span>
                  <el-button
                    type="text"
                    class="remove-btn"
                    @click="removeDataset(dataset.value)">
                    移除
                  </el-button>
                </div>
              </div>

              <!-- 空状态提示 -->
              <div v-else class="empty-dataset-hint">
                <el-text type="info">请点击上方"请选择"按钮添加数据集</el-text>
              </div>
            </div>

            <!-- 自定义验证集 -->
<!--            <div class="config-item">-->
<!--              <div class="switch-item">-->
<!--                <span class="switch-label">自定义验证集 -->
<!--                  <el-icon class="help-icon"><QuestionFilled /></el-icon>-->
<!--                </span>-->
<!--                -->
<!--              </div>-->
<!--              <el-switch v-model="customValidation" />-->
<!--               <label class="config-label">选择验证集</label>-->
<!--              <el-button type="text" class="add-dataset-btn">-->
<!--                <el-icon><Plus /></el-icon>-->
<!--                请选择-->
<!--              </el-button>-->
<!--              <div class="config-hint">验证集标签应与训练集完全一致。为获得效果更好的模型，验证集数据需更贴近实际应用场景</div>-->
<!--            </div>-->
<!--            <div class="switch-item">-->
<!--                <span class="switch-label">自定义测试集 -->
<!--                  <el-icon class="help-icon"><QuestionFilled /></el-icon>-->
<!--                </span>-->
<!--                -->
<!--            </div>-->
<!--              <el-switch v-model="customValidation" />-->
<!--            <div class="config-item">-->
<!--              <label class="config-label">选择验证集</label>-->
<!--              <el-button type="text" class="add-dataset-btn">-->
<!--                <el-icon><Plus /></el-icon>-->
<!--                请选择-->
<!--              </el-button>-->
<!--              <div class="config-hint">测试集标签应该是训练集的子集或全集</div>-->
<!--            </div>-->

<!--            &lt;!&ndash; 数据增强策略 &ndash;&gt;-->
<!--            <div class="config-item">-->
<!--              <label class="config-label">数据增强策略 *</label>-->
<!--              <div class="strategy-options">-->
<!--                <el-radio v-model="dataStrategy" label="default">默认配置</el-radio>-->
<!--                <el-radio v-model="dataStrategy" label="manual">手动配置</el-radio>-->
<!--              </div>-->
<!--            </div>-->
          </div>

          <!-- 训练环境 -->
          <div class="config-section">
            <h3 class="section-title">训练环境</h3>
            <el-button type="text" class="detail-link">了解详情</el-button>
            
            <div class="environment-info">
              <div class="env-item">
                <span class="env-label">名称</span>
                <span class="env-value">规格</span>
              </div>
              <div class="env-item">
                <span class="env-label">CPU</span>
                <span class="env-value">Intel (R) Xeon (R) CPU E5-2698</span>
              </div>
            </div>
          </div>

          <!-- 开始训练按钮 -->
          <div class="training-actions">
            <el-button
              type="primary"
              @click="handleStartTraining"
              class="start-training-btn"
              :disabled="selectedDatasets.length === 0"
              :loading="isTraining"
            >
              {{ isTraining ? '训练中...' : '开始训练' }}
            </el-button>
            <div v-if="selectedDatasets.length === 0" class="training-hint">
              <el-text type="warning" size="small">请先选择数据集</el-text>
            </div>
          </div>
        </div>

        <!-- 右侧SSH终端区域 -->
        <div class="ssh-terminal-container">
          <div class="terminal-header">
            <h3 class="terminal-title">训练日志</h3>
            <div class="terminal-controls">
              <el-button size="small" @click="clearTerminal">清空</el-button>
            </div>
          </div>
          <div class="terminal-content" ref="terminalRef">
            <div class="terminal-output" v-html="terminalOutput"></div>
            <div class="terminal-input-line">
              <span class="terminal-prompt">提示</span>
              <div class="terminal-input disabled-hint">终端仅展示后台日志，命令输入已关闭</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 发布为模型弹窗 -->
  <el-dialog
    v-model="showDeployDialog"
    title="发布至模型仓库"
    width="600px"
    :before-close="handleDeployCancel"
  >
    <el-form :model="deployForm" label-width="100px" style="max-width: 500px">
      <!-- 模型名称 -->
      <el-form-item label="模型名称" required>
        <el-input 
          v-model="deployForm.modelName" 
          placeholder="请输入"
          style="width: 100%"
        />
      </el-form-item>

      <!-- 模型类型 -->
      <el-form-item label="模型类型">
        <el-input 
          v-model="deployForm.modelType" 
          disabled
          style="width: 100%"
        />
      </el-form-item>

      <!-- 发布仓库 -->
      <el-form-item label="发布仓库" required>
        <el-select 
          v-model="deployForm.repository" 
          placeholder="选择仓库"
          style="width: 100%"
          suffix-icon="ArrowRight"
        >
          <el-option
            v-for="item in repositoryOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <!-- 模型版本 -->
      <el-form-item label="模型版本">
        <el-input 
          v-model="deployForm.modelVersion" 
          style="width: 100%"
        />
      </el-form-item>

      <!-- 版本描述 -->
      <el-form-item label="版本描述">
        <el-input 
          v-model="deployForm.versionDescription" 
          type="textarea"
          :rows="4"
          placeholder="请输入..."
          style="width: 100%"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleDeployCancel">取消</el-button>
        <el-button type="primary" @click="handleDeployConfirm">确认</el-button>
      </div>
    </template>
  </el-dialog>



  <!-- 新增训练任务弹窗 -->
  <el-dialog
    v-model="showAddDialog"
    title="新增训练任务"
    width="600px"
    :before-close="handleCloseDialog"
  >
    <el-form
      ref="addFormRef"
      :model="addForm"
      :rules="addFormRules"
      label-width="100px"
      label-position="left"
    >
      <el-form-item label="任务名称" prop="taskName" required>
        <el-input
          v-model="addForm.taskName"
          placeholder="请输入任务名称"
          clearable
        />
      </el-form-item>

      <el-form-item label="选择算法" prop="algorithmId" required>
        <el-select
          v-model="addForm.algorithmId"
          placeholder="请选择算法"
          style="width: 100%"
          :loading="algorithmLoading"
          filterable
          clearable>
          <el-option
            v-for="algorithm in algorithmOptions"
            :key="algorithm.value"
            :label="algorithm.label"
            :value="algorithm.value"
          >
            <div class="algorithm-option">
              <div class="algorithm-name">{{ algorithm.name }}</div>
              <div class="algorithm-meta">
                <span class="algorithm-version">版本：{{ algorithm.version || '-' }}</span>
                <span class="algorithm-type">{{ algorithm.type || '算法' }}</span>
              </div>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      
      <el-form-item label="选择数据集" prop="datasetId" required>
        <el-select
          v-model="addForm.datasetId"
          placeholder="请选择数据集"
          style="width: 100%"
          :loading="datasetLoading"
          filterable
          clearable
        >
          <el-option
            v-for="dataset in datasetOptions"
            :key="dataset.value"
            :label="dataset.label"
            :value="dataset.value"
          >
            <div class="dataset-option">
              <div class="dataset-name">{{ dataset.annotationName }}</div>
              <div class="dataset-info">
                <el-tag size="small" :type="getStatusTagType(dataset.status)">
                  {{ dataset.statusLabel }}
                </el-tag>
                <span class="dataset-type">{{ dataset.typeLabel }}</span>
                <span class="dataset-progress">{{ dataset.progress }}%</span>
              </div>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      
      <el-form-item label="训练轮数" prop="epochTotal" required>
        <el-input-number
          v-model="addForm.epochTotal"
          :min="1"
          :max="1000"
          placeholder="请输入训练轮数"
          style="width: 100%"
        />
      </el-form-item>
      
      <el-form-item label="配置参数" prop="configParams">
        <el-input
          v-model="addForm.configParams"
          type="textarea"
          :rows="4"
          placeholder="请输入JSON格式的配置参数（可选）"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
      
      <el-form-item label="任务描述" prop="description">
        <el-input
          v-model="addForm.description"
          type="textarea"
          :rows="3"
          placeholder="请输入任务描述（可选）"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCloseDialog">取消</el-button>
        <el-button type="primary" @click="handleConfirmAdd">确定</el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 编辑训练任务弹窗 -->
  <el-dialog
    v-model="showEditDialog"
    title="编辑训练任务"
    width="600px"
    :before-close="handleCloseEditDialog"
  >
    <el-form
      ref="editFormRef"
      :model="editForm"
      :rules="editFormRules"
      label-width="100px"
      label-position="left"
    >
      <el-form-item label="任务名称" prop="taskName" required>
        <el-input
          v-model="editForm.taskName"
          placeholder="请输入任务名称"
          clearable
        />
      </el-form-item>
      
      <el-form-item label="选择算法" prop="algorithmId" required>
        <el-select
          v-model="editForm.algorithmId"
          placeholder="请选择算法"
          style="width: 100%"
          :loading="algorithmLoading"
          filterable
          clearable
        >
          <el-option
            v-for="algorithm in algorithmOptions"
            :key="algorithm.value"
            :label="algorithm.label"
            :value="algorithm.value"
          >
            <div class="algorithm-option">
              <div class="algorithm-name">{{ algorithm.name }}</div>
              <div class="algorithm-meta">
                <span class="algorithm-version">版本：{{ algorithm.version || '-' }}</span>
                <span class="algorithm-type">{{ algorithm.type || '算法' }}</span>
              </div>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      
      <el-form-item label="选择数据集" prop="datasetId" required>
        <el-select
          v-model="editForm.datasetId"
          placeholder="请选择数据集"
          style="width: 100%"
          :loading="datasetLoading"
          filterable
          clearable
        >
          <el-option
            v-for="dataset in datasetOptions"
            :key="dataset.value"
            :label="dataset.label"
            :value="dataset.value"
          >
            <div class="dataset-option">
              <div class="dataset-name">{{ dataset.annotationName }}</div>
              <div class="dataset-info">
                <el-tag size="small" :type="getStatusTagType(dataset.status)">
                  {{ dataset.statusLabel }}
                </el-tag>
                <span class="dataset-type">{{ dataset.typeLabel }}</span>
                <span class="dataset-progress">{{ dataset.progress }}%</span>
              </div>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      
      <el-form-item label="训练轮数" prop="epochTotal" required>
        <el-input-number
          v-model="editForm.epochTotal"
          :min="1"
          :max="1000"
          placeholder="请输入训练轮数"
          style="width: 100%"
        />
      </el-form-item>
      
      <el-form-item label="配置参数" prop="configParams">
        <el-input
          v-model="editForm.configParams"
          type="textarea"
          :rows="4"
          placeholder="请输入JSON格式的配置参数（可选）"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
      
      <el-form-item label="任务描述" prop="description">
        <el-input
          v-model="editForm.description"
          type="textarea"
          :rows="3"
          placeholder="请输入任务描述（可选）"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCloseEditDialog">取消</el-button>
        <el-button type="primary" @click="handleConfirmEdit">确定</el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 数据集选择弹窗 -->
  <el-dialog
    v-model="showDatasetSelector"
    title="选择数据集"
    width="800px"
    :before-close="handleCloseDatasetSelector"
  >
    <div class="dataset-selector-content">
      <!-- 搜索框 -->
      <div class="dataset-search">
        <el-input
          v-model="datasetSearchKeyword"
          placeholder="搜索数据集名称..."
          :prefix-icon="Search"
          clearable
          @input="handleDatasetSearch"
        />
      </div>

      <!-- 数据集列表 -->
      <div class="dataset-list" v-loading="datasetLoading">
        <div
          v-for="dataset in filteredDatasetOptions"
          :key="dataset.value"
          class="dataset-item"
          :class="{ selected: selectedDatasetIds.includes(dataset.value) }"
          @click="toggleDatasetSelection(dataset)"
        >
          <div class="dataset-checkbox">
            <el-checkbox
              :model-value="selectedDatasetIds.includes(dataset.value)"
              @change="toggleDatasetSelection(dataset)"
            />
          </div>
          <div class="dataset-content">
            <div class="dataset-header">
              <h4 class="dataset-name">{{ dataset.annotationName }}</h4>
              <el-tag size="small" :type="getStatusTagType(dataset.status)">
                {{ dataset.statusLabel }}
              </el-tag>
            </div>
            <div class="dataset-details">
              <span class="dataset-type">{{ dataset.typeLabel }}</span>
              <span class="dataset-progress">进度: {{ dataset.progress }}%</span>
              <span class="dataset-count">{{ dataset.annotatedCount }}/{{ dataset.totalCount }}</span>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="filteredDatasetOptions.length === 0 && !datasetLoading" class="empty-state">
          <el-empty description="暂无数据集" />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCloseDatasetSelector">取消</el-button>
        <el-button type="primary" @click="handleConfirmDatasetSelection">
          确定 ({{ selectedDatasetIds.length }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import {computed, h, nextTick, onMounted, onUnmounted, ref} from 'vue'
import {ElMessage, ElMessageBox, ElRadio, ElRadioGroup} from 'element-plus'
import {ArrowDown, Download, Plus, QuestionFilled, Refresh, Search} from '@element-plus/icons-vue'
import ActionButtonGroup from '@/components/ActionButtonGroup.vue'
import {
  batchDeleteTraining,
  convertModel,
  createTraining,
  deleteTraining,
  getTrainingLogs,
  getTrainingPage,
  getTrainingStatus,
  startTrainingWithParams,
  stopTraining,
  updateTraining
} from '@/api/algorithmTraining.js'
import {createModel, getModelPage} from '@/api/algorithmModel.js'
import {getAlgorithmAnnotationPage} from '@/api/algorithmAnnotation.js'
import {getAlgorithmPage} from '@/api/algorithmManagement.js'
import CollapseToggle from '@/components/CollapseToggle.vue'
import AdvancedSearch from '@/components/AdvancedSearch.vue'

// 导入步骤图标
import trainIcon from '@/assets/start-training@3x.png'
import selectDatasetIcon from '@/assets/select-dataset@3x.png'
import evaluateIcon from '@/assets/validation@3x.png'
import publishModelIcon from '@/assets/publish-model@3x.png'
import request, {getBaseURL} from "@/utils/request";

// 搜索表单
const searchForm = ref({
  trainName: '',
  dateRange: []
})

// 表格数据
const tableData = ref([])
const loading = ref(false)

// 分页相关
const currentPage = ref(1)
const pageSize = ref(5)
const total = ref(0)

// 计算当前页数据
const currentPageData = computed(() => {
  return tableData.value
})

// 选中行
const selectedRow = ref(null)
const selectedRows = ref([])

// 训练配置视图的状态控制
const showTrainingConfig = ref(false)
const currentTrainingItem = ref(null)

// 版本配置视图的状态控制
const showVersionConfig = ref(false)
const currentVersionItem = ref(null)
const modelHistoryList = ref([])
const modelHistoryLoading = ref(false)
const modelHistoryCurrentPage = ref(1)
const modelHistoryPageSize = ref(10)
const modelHistoryTotal = ref(0)
const historyTrainingId = ref(null)

// 校验模型视图的状态控制
const showValidationView = ref(false)
const currentValidationItem = ref(null)

// 编辑视图的状态控制 - 已移除，改用弹窗模式

// 发布为模型弹窗的状态控制
const showDeployDialog = ref(false)
const currentDeployItem = ref(null)

// 发布为模型表单数据
const deployForm = ref({
  modelName: '',
  modelType: '物体检测',
  repository: '',
  versionDescription: ''
})

// 校验相关数据
const validationModelName = ref('螺丝螺母识别')
const validationVersion = ref('V1')
const confidenceThreshold = ref(30)
const uploadedImage = ref(null)
const detectionResults = ref([])

// 下拉选项数据
const modelNameOptions = ref([
  { label: '螺丝螺母识别', value: '螺丝螺母识别' },
  { label: '螺栓检测模型', value: '螺栓检测模型' },
  { label: '工件识别模型', value: '工件识别模型' }
])

// 新增弹窗相关
const showAddDialog = ref(false)
const addFormRef = ref(null)
const addForm = ref({
  taskName: '',
  algorithmId: '',
  datasetId: '',
  epochTotal: 10,
  configParams: '',
  description: ''
})

// 编辑弹窗相关
const showEditDialog = ref(false)
const editFormRef = ref(null)
const editForm = ref({
  taskName: '',
  algorithmId: '',
  datasetId: '',
  epochTotal: 10,
  configParams: '',
  description: ''
})
const editingTrainingItem = ref(null)

// SSH终端相关数据
const sshConnected = ref(true)
const terminalOutput = ref('')
const currentCommand = ref('')
const terminalRef = ref(null)
const logPollingTimer = ref(null)
const statusPollingTimer = ref(null)
const lastLogContent = ref('')
const lastLogCount = ref(0)
const lastModelPath = ref('')
const lastLogPath = ref('')

// 表单验证规则
const addFormRules = {
  taskName: [
    { required: true, message: '请输入任务名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  algorithmId: [
    { required: true, message: '请选择算法', trigger: 'change' }
  ],
  datasetId: [
    { required: true, message: '请选择数据集', trigger: 'change' }
  ],
  epochTotal: [
    { required: true, message: '请输入训练轮数', trigger: 'blur' },
    { type: 'number', min: 1, max: 1000, message: '训练轮数在 1 到 1000 之间', trigger: 'blur' }
  ]
}

const editFormRules = {
  taskName: [
    { required: true, message: '请输入任务名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  algorithmId: [
    { required: true, message: '请选择算法', trigger: 'change' }
  ],
  datasetId: [
    { required: true, message: '请选择数据集', trigger: 'change' }
  ],
  epochTotal: [
    { required: true, message: '请输入训练轮数', trigger: 'blur' },
    { type: 'number', min: 1, max: 1000, message: '训练轮数在 1 到 1000 之间', trigger: 'blur' }
  ]
}

const versionOptions = ref([
  { label: 'V1', value: 'V1' },
  { label: 'V2', value: 'V2' },
  { label: 'V3', value: 'V3' }
])

// 发布仓库选项数据
const repositoryOptions = ref([
  { label: '私有仓库', value: 'private' },
  { label: '公共仓库', value: 'public' },
  { label: '项目仓库', value: 'project' }
])

// 训练配置相关数据
const trainingMode = ref('auto')
const advancedConfig = ref(true)
const resolution = ref('auto')
const epochMode = ref('auto')
const autoPublish = ref('yes')
const autoPublishTriggered = ref(new Set())
const customValidation = ref(true)
const dataStrategy = ref('default')

// 训练类型选项/映射
const TRAIN_TYPE_OPTIONS = [
  { label: '目标检测算法', value: 'detect' },
  { label: '实例分割算法', value: 'segment' },
  { label: '图像分类算法', value: 'classify' },
  { label: '关键点检测算法', value: 'pose' },
  { label: '旋转目标检测算法', value: 'obb' }
]

const TRAIN_TYPE_LABELS = {
  detect: '目标检测算法',
  segment: '实例分割算法',
  classify: '图像分类算法',
  pose: '关键点检测算法',
  obb: '旋转目标检测算法'
}

// 介绍内容折叠状态
const isIntroExpanded = ref(true)

// 训练配置参数
const epochTotal = ref(10)
const batchSize = ref(16)
const trainImgSize = ref(640)

// 获取状态类型
const getStatusType = (status) => {
  switch (status) {
    case 'completed':
      return 'success'
    case 'training':
      return 'warning'
    case 'pending':
      return 'info'
    case 'failed':
      return 'danger'
    default:
      return 'info'
  }
}

// 获取状态样式类
const getStatusClass = (status) => {
  switch (status) {
    case 'completed':
      return 'status-success'
    case 'training':
      return 'status-warning'
    case 'pending':
      return 'status-info'
    case 'failed':
      return 'status-danger'
    default:
      return 'status-info'
  }
}

// 事件处理
const handleAdd = async () => {
  showAddDialog.value = true
  // 重置表单
  addForm.value = {
    taskName: '',
    algorithmId: '',
    datasetId: '',
    epochTotal: 10,
    configParams: '',
    description: ''
  }
  // 加载算法与数据集选项
  await Promise.all([loadAlgorithmOptions(), loadDatasetOptions()])
}

const handleConfirmAdd = async () => {
  if (!addFormRef.value) return
  try {
    await addFormRef.value.validate()
    let parsedConfig = null
    if (addForm.value.configParams) {
      try {
        parsedConfig = JSON.parse(addForm.value.configParams)
      } catch (e) {
        ElMessage.error('配置参数需为合法的 JSON 格式')
        return
      }
    }

    const formData = {
      taskName: addForm.value.taskName,
      algorithmId: Number(addForm.value.algorithmId),
      datasetId: addForm.value.datasetId ? Number(addForm.value.datasetId) : null,
      epochTotal: addForm.value.epochTotal,
      configParams: parsedConfig,
      description: addForm.value.description
    }

    await createTraining(formData)
    ElMessage.success('训练任务创建成功')
    showAddDialog.value = false
    selectedRows.value = []
    addFormRef.value.resetFields()
    loadTrainingData()
  } catch (error) {
    console.error('新增训练任务失败:', error)
    ElMessage.error('新增训练任务失败：' + (error?.message || '未知错误'))
  }
}

const handleEdit = () => {
  if (selectedRows.value.length === 1) {
    const training = selectedRows.value[0]
    handleEditRow(training)
  }
}

const handleDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的训练任务')
    return
  }

  try {
    if (selectedRows.value.length === 1) {
      await ElMessageBox.confirm(`确认要删除训练任务"${selectedRows.value[0].algorithmName}"吗？`, '确认删除', {
        type: 'warning'
      })
      await deleteTraining(selectedRows.value[0].originalData.id)
      ElMessage.success('删除成功')
    } else {
      await ElMessageBox.confirm(`确认要批量删除${selectedRows.value.length}个训练任务吗？`, '确认批量删除', {
        type: 'warning'
      })
      const ids = selectedRows.value.map(row => row.originalData.id)
      await batchDeleteTraining(ids)
      ElMessage.success(`批量删除${selectedRows.value.length}个任务成功`)
    }
    
    selectedRows.value = []
    loadTrainingData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  }
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleRowClick = (row) => {
  selectedRow.value = row
}

const handleTrain = (row) => {
  currentTrainingItem.value = row
  stopAllPolling()
  lastLogCount.value = 0
  lastLogContent.value = ''
  lastModelPath.value = ''
  lastLogPath.value = ''
  terminalOutput.value = ''

  // 清空数据集选择 - 新训练应该从空白配置开始
  selectedDatasets.value = []
  selectedDatasetIds.value = []

  // 重置其他训练配置为默认值
  dataStrategy.value = 'default'
  resolution.value = 'auto'
  epochMode.value = 'auto'
  autoPublish.value = 'yes'
  customValidation.value = true
  epochTotal.value = row?.originalData?.epochTotal
  batchSize.value = row?.originalData?.batchSize || 16
  trainImgSize.value = row?.originalData?.imgSize || 640

  showTrainingConfig.value = true

  // 已在训练中的任务，立即开始日志/状态轮询
  const taskId = row.originalData?.id || row.id
  const rawStatus = row.originalData?.trainStatus || row.trainStatus
  if (rawStatus === 'training' || rawStatus === '训练中') {
    startPollingForTask(taskId, true)
  }
}

// 构建训练参数
const buildTrainingPayload = async () => {
  if (!currentTrainingItem.value) {
    throw new Error('请先选择训练任务')
  }
  if (!selectedDatasets.value.length) {
    throw new Error('请至少选择一个数据集')
  }
  const dataset = selectedDatasets.value[0]
  const trainingData = currentTrainingItem.value.originalData || currentTrainingItem.value
  const taskId = trainingData.id || currentTrainingItem.value.id
  if (!taskId) {
    throw new Error('无法获取训练任务 ID')
  }
  if (!dataset.datasetPath) {
    throw new Error('当前数据集缺少路径配置，请检查后端数据')
  }

  if (!algorithmOptions.value.length) {
    await loadAlgorithmOptions()
  }
  const algorithm = algorithmOptions.value.find(item => item.value === trainingData.algorithmId)
  const modelFilePath = algorithm?.ptModelFilePath || trainingData.ptModelFilePath
  const trainType = trainingData.trainType
  const epochs = Number(trainingData.epochTotal)
  const imgsz = Number(trainImgSize.value || 640)
  const batch = Number(batchSize.value || 16)

  return {
    taskId,
    dataset,
    modelFilePath,
    trainType,
    params: {
      trainType,
      datasetPath: dataset.datasetPath,
      data: dataset.datasetPath,
      datasetId: dataset.value,
      modelFilePath: modelFilePath || '',
      model: modelFilePath || '',
      epochs,
      imgsz,
      batchSize: batch,
      batch
    }
  }
}

// 开始训练
const handleStartTraining = async () => {
  try {
    stopAllPolling()
    lastModelPath.value = ''
    lastLogPath.value = ''
    isTraining.value = true
    const { taskId, trainType, modelFilePath, params } = await buildTrainingPayload()
    autoPublishTriggered.value.delete(taskId)

    if (!modelFilePath) {
      isTraining.value = false
      ElMessage.error('未找到基础模型路径，请先在算法配置中设置模型文件')
      appendLogLines('[WARN] 未找到模型文件路径，无法启动训练')
      return
    }

    const commandText = buildTrainCommand({
      task: trainType,
      datasetPath: params.datasetPath,
      modelPath: modelFilePath,
      epochs: params.epochs,
      imgsz: params.imgsz,
      batchSize: params.batchSize
    })

    appendLogLines(`[INFO] 准备启动训练任务 #${taskId}`)
    // appendLogLines(`[INFO] 使用数据集: ${dataset.annotationName || dataset.label || params.datasetPath}`)
    appendLogLines(`[CMD] ${commandText}`)
    appendLogLines('[INFO] 正在向后端提交训练请求...')

    await startTrainingWithParams(taskId, params)

    appendLogLines('[INFO] 训练命令已下发，开始监听日志与状态...')
    startPollingForTask(taskId, false)
    await loadTrainingData()
  } catch (error) {
    console.error('启动训练失败:', error)
    appendLogLines(`[ERROR] 训练启动失败: ${error?.message || error}`)
    ElMessage.error('训练启动失败：' + (error?.message || '未知错误'))
    isTraining.value = false
  }
}

// 返回列表
const handleBackToList = () => {
  stopAllPolling()
  isTraining.value = false
  showTrainingConfig.value = false
  currentTrainingItem.value = null
  selectedDatasets.value = []
  selectedDatasetIds.value = []
  terminalOutput.value = ''
  loadTrainingData()
}

const handleStopTraining = async (row) => {
  try {
    await ElMessageBox.confirm(`确认要终止训练任务"${row.algorithmName}"吗？`, '确认终止', {
      type: 'warning'
    })
    
    await stopTraining(row.originalData.id)
    ElMessage.success('训练任务已终止')
    appendLogLines('[INFO] 训练已停止')
    stopAllPolling()
    isTraining.value = false
    await loadTrainingData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('终止训练失败：' + error.message)
    }
  }
}

const handleEditRow = async (row) => {
  editingTrainingItem.value = row
  editForm.value = {
    taskName: row.algorithmName,
    trainType: row.trainType,
    algorithmId: row.originalData.algorithmId || '',
    datasetId: row.originalData.datasetId || '',
    epochTotal: row.originalData.epochTotal,
    configParams: JSON.stringify(row.originalData.configParams, null, 2) || '',
    description: row.originalData.description || ''
  }
  showEditDialog.value = true
  // 加载算法与数据集选项
  await Promise.all([loadAlgorithmOptions(), loadDatasetOptions()])
}

const handleDeleteRow = async (row) => {
  try {
    await ElMessageBox.confirm(`确认要删除训练任务"${row.algorithmName}"吗？`, '确认删除', {
      type: 'warning'
    })
    
    await deleteTraining(row.originalData.id)
    ElMessage.success('训练任务已删除')
    loadTrainingData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除训练任务失败：' + error.message)
    }
  }
}

const resolveHistoryTrainingId = (item) => item?.originalData?.id ?? item?.id

const normalizeHistoryRecord = (record) => {
  const data = record || {}
  const accuracyValue = data.formattedAccuracy
  const fallbackAccuracy = data.accuracy !== undefined && data.accuracy !== null
    ? `${Number(data.accuracy).toFixed(2)}%`
    : '-'

  return {
    ...data,
    modelName: data.modelName || data.name || '-',
    version: data.version ?? '-',
    modelFormatDesc: data.modelFormatDesc || data.modelFormat || '-',
    statusDesc: data.statusDesc || data.status || '-',
    formattedAccuracy: accuracyValue || fallbackAccuracy,
    modelPath: data.modelPath || '-',
    description: data.description || '-',
    downloadCount: data.downloadCount ?? 0,
    deployCount: data.deployCount ?? 0,
    createTime: data.createTime || '-',
    updatedTime: data.updatedTime || '-'
  }
}

const loadModelHistory = async (trainingId) => {
  const resolvedTrainingId = trainingId ?? historyTrainingId.value
  if (!resolvedTrainingId) {
    modelHistoryList.value = []
    modelHistoryTotal.value = 0
    return
  }

  historyTrainingId.value = resolvedTrainingId
  modelHistoryLoading.value = true
  try {
    const response = await getModelPage({
      current: modelHistoryCurrentPage.value,
      size: modelHistoryPageSize.value,
      trainingId: resolvedTrainingId
    })

    if (response?.code === 200) {
      const records = response?.data?.records || []
      modelHistoryList.value = records.map(normalizeHistoryRecord)
      modelHistoryTotal.value = response?.data?.total || 0
    } else {
      modelHistoryList.value = []
      modelHistoryTotal.value = 0
      ElMessage.error(response?.message || 'Failed to load history')
    }
  } catch (error) {
    console.error('Failed to load model history:', error)
    modelHistoryList.value = []
    modelHistoryTotal.value = 0
    ElMessage.error('Failed to load history')
  } finally {
    modelHistoryLoading.value = false
  }
}

const openHistoryView = async (item) => {
  currentVersionItem.value = item
  showVersionConfig.value = true
  modelHistoryCurrentPage.value = 1

  const trainingId = resolveHistoryTrainingId(item)
  await loadModelHistory(trainingId)
}

const handleHistorySizeChange = (size) => {
  modelHistoryPageSize.value = size
  modelHistoryCurrentPage.value = 1
  loadModelHistory()
}

const handleHistoryCurrentChange = (page) => {
  modelHistoryCurrentPage.value = page
  loadModelHistory()
}

const handleMoreAction = async (command, data) => {
  switch (command) {
    case 'deploy':
      currentDeployItem.value = data
      deployForm.value.modelName = data.algorithmName || ''
      showDeployDialog.value = true
      break
    case 'validate':
      currentValidationItem.value = data
      showValidationView.value = true
      break
    case 'history':
      await openHistoryView(data)
      break
    case 'version':
      await openHistoryView(data)
      break
    case 'delete':
      handleDeleteTraining(data)
      break
  }
}

// 下载模型
const promptDownloadModelType = async () => {
  let chosenType = 'pt'

  const TypeSelector = {
    name: 'DownloadModelTypeSelector',
    setup() {
      const localType = ref(chosenType)
      const updateType = (value) => {
        localType.value = value
        chosenType = value
      }

      return () => h('div', [
        h(
          ElRadioGroup,
          {
            modelValue: localType.value,
            'onUpdate:modelValue': updateType
          },
          () => [
            h(ElRadio, { label: 'pt' }, () => 'pt'),
            h(ElRadio, { label: 'onnx' }, () => 'onnx'),
            h(ElRadio, { label: 'rknn' }, () => 'rknn')
          ]
        )
      ])
    }
  }

  try {
    await ElMessageBox({
      title: '下载模型',
      message: h(TypeSelector),
      showCancelButton: true,
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      closeOnClickModal: false
    })
    return chosenType
  } catch (error) {
    return null
  }
}

const handleDownloadModel = async (row) => {
  try {
    const trainingId = row?.originalData?.id ?? row?.id
    if (!trainingId) {
      ElMessage.warning('训练ID')
      return
    }

    const downloadType = await promptDownloadModelType()
    if (!downloadType) {
      return
    }

    const taskName = row?.originalData?.taskName || row?.algorithmName || 'model'
    const modelFileName = `${taskName}.${downloadType}`

    const blob = await request({
      url: '/vlsAlgorithmTraining/download-model',
      method: 'get',
      params: { id: trainingId, type: downloadType },
      responseType: 'blob'
    })

    if (blob instanceof Blob && blob.type && blob.type.includes('application/json')) {
      const text = await blob.text()
      let message = '下载模型'
      try {
        const payload = JSON.parse(text)
        message = payload?.message || payload?.msg || message
      } catch (parseError) {
        message = text || message
      }
      throw new Error(message)
    }

    const blobUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = modelFileName
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(blobUrl)

    ElMessage.success(`下载模型成功: ${modelFileName}`)

    if (sshConnected.value) {
      terminalOutput.value += `<div class="terminal-line"><span class="terminal-success">Downloading model: ${modelFileName}</span></div>`
      terminalOutput.value += `<div class="terminal-line"><span class="terminal-info">Model type: ${downloadType}</span></div>`
      terminalOutput.value += `<div class="terminal-line"><span class="terminal-info">Training ID: ${trainingId}</span></div>`
      scrollToBottom()
    }
  } catch (error) {
    console.error('模型下载错误:', error)
    ElMessage.error('模型下载错误：' + error.message)
  }
}

const handleRetrainModel = async (row) => {
  try {
    console.log('重新训练模型 - 原始数据:', row)

    // 获取原始训练配置
    const originalData = row.originalData
    if (!originalData) {
      ElMessage.error('无法获取原始训练配置')
      return
    }

    // 确认重新训练
    await ElMessageBox.confirm(
      `确认要重新训练任务"${originalData.taskName}"吗？这将打开训练配置页面，您可以查看和修改配置后再开始训练。`,
      '重新训练',
      {
        type: 'info',
        confirmButtonText: '打开训练配置',
        cancelButtonText: '取消'
      }
    )

    // 设置当前训练项目
    currentTrainingItem.value = {
      id: originalData.id,
      name: originalData.taskName,
      algorithmName: originalData.taskName,
      trainType: originalData.trainType,
      originalData: originalData
    }

    // 恢复原始训练配置
    await restoreTrainingConfig(originalData)

    // 切换到训练配置页面
    showTrainingConfig.value = true

    // 显示重新训练信息
    ElMessage.success(`已打开训练配置页面，基于任务"${originalData.taskName}"的原有配置`)

    // 在SSH终端显示重新训练信息
    if (sshConnected.value) {
      terminalOutput.value += `<div class="terminal-line"><span class="terminal-info">[INFO]</span> 🔄 重新训练任务: ${originalData.taskName}</div>`
      terminalOutput.value += `<div class="terminal-line"><span class="terminal-info">[INFO]</span> 📋 已加载原始训练配置</div>`
      if (originalData.datasetId) {
        terminalOutput.value += `<div class="terminal-line"><span class="terminal-info">[INFO]</span> 📊 已恢复数据集选择</div>`
      }
      terminalOutput.value += `<div class="terminal-line"><span class="terminal-success">[SUCCESS]</span> ✅ 配置页面已打开，可以查看和修改配置</div>`
      scrollToBottom()
    }

  } catch (error) {
    if (error !== 'cancel') {
      console.error('重新训练失败:', error)
      ElMessage.error('重新训练失败：' + error.message)
    }
  }
}

// 恢复训练配置
const restoreTrainingConfig = async (originalData) => {
  try {
    console.log('恢复训练配置:', originalData)

    // 1. 恢复数据集选择
    if (originalData.datasetId) {
      // 加载数据集选项（如果还没有加载）
      if (datasetOptions.value.length === 0) {
        await loadDatasetOptions()
      }

      // 查找对应的数据集
      const dataset = datasetOptions.value.find(d => d.value === originalData.datasetId)
      if (dataset) {
        selectedDatasets.value = [dataset]
        selectedDatasetIds.value = [dataset.value]
        console.log('已恢复数据集选择:', dataset.label)
      } else {
        console.warn('未找到对应的数据集:', originalData.datasetId)
        selectedDatasets.value = []
        selectedDatasetIds.value = []
      }
    } else {
      selectedDatasets.value = []
      selectedDatasetIds.value = []
    }

    // 2. 恢复训练参数配置
    if (originalData.configParams) {
      try {
        const config = typeof originalData.configParams === 'string'
          ? JSON.parse(originalData.configParams)
          : originalData.configParams

        console.log('恢复训练参数配置:', config)

        // 恢复各种配置参数
        if (config.dataStrategy) dataStrategy.value = config.dataStrategy
        if (config.resolution) resolution.value = config.resolution
        if (config.epochMode) epochMode.value = config.epochMode
        if (config.autoPublish !== undefined) autoPublish.value = config.autoPublish
        if (config.customValidation !== undefined) customValidation.value = config.customValidation
        if (config.epochs) epochTotal.value = config.epochs
        if (config.batchSize) batchSize.value = config.batchSize

      } catch (parseError) {
        console.error('解析训练配置失败:', parseError)
        // 使用默认配置
        resetToDefaultConfig()
      }
    } else {
      console.log('没有保存的训练配置，使用默认配置')
      resetToDefaultConfig()
    }

  } catch (error) {
    console.error('恢复训练配置失败:', error)
    ElMessage.error('恢复训练配置失败：' + error.message)
    // 使用默认配置
    resetToDefaultConfig()
  }
}

// 重置为默认配置
const resetToDefaultConfig = () => {
  dataStrategy.value = 'default'
  resolution.value = 'auto'
  epochMode.value = 'auto'
  autoPublish.value = 'yes'
  customValidation.value = true
}

// 保存训练配置到数据库
const saveTrainingConfig = async (dataset) => {
  try {
    if (!currentTrainingItem.value) {
      console.warn('没有当前训练项目，跳过配置保存')
      return
    }

    // 构建训练配置对象
    const trainingConfig = {
      datasetId: dataset ? dataset.value : null,
      datasetName: dataset ? dataset.annotationName : null,
      dataStrategy: dataStrategy.value,
      resolution: resolution.value,
      epochMode: epochMode.value,
      autoPublish: autoPublish.value,
      customValidation: customValidation.value,
      epochs: epochTotal.value,
      batchSize: batchSize.value,
      savedAt: new Date().toISOString()
    }

    console.log('保存训练配置:', trainingConfig)

    // 调用后端API保存配置
    const response = await fetch(getBaseURL() + `/vlsAlgorithmTraining/${currentTrainingItem.value.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        datasetId: trainingConfig.datasetId,
        configParams: JSON.stringify(trainingConfig)
      })
    })

    if (response.ok) {
      console.log('训练配置已保存到数据库')

      // 在SSH终端显示保存信息
      if (sshConnected.value) {
        terminalOutput.value += `<div class="terminal-line"><span class="terminal-success">[SUCCESS]</span> 💾 训练配置已保存到数据库</div>`
        terminalOutput.value += `<div class="terminal-line"><span class="terminal-info">[INFO]</span> 📊 数据集: ${trainingConfig.datasetName || '未选择'}</div>`
        terminalOutput.value += `<div class="terminal-line"><span class="terminal-info">[INFO]</span> 🔧 配置参数已记录，可用于重新训练</div>`
        scrollToBottom()
      }
    } else {
      console.error('保存训练配置失败:', response.statusText)
    }

  } catch (error) {
    console.error('保存训练配置失败:', error)
    ElMessage.warning('保存训练配置失败，但训练将继续进行')
  }
}

// 发布为模型
const handlePublishModel = async (row) => {
  try {
    await ElMessageBox.confirm(`确认要将训练任务"${row.algorithmName}"发布为模型吗？`, '确认发布', {
      type: 'warning'
    })
    
    // 构建模型数据
    const modelData = {
      modelName: row.algorithmName,
      algorithmId: row.originalData.algorithmId,
      trainingId: row.originalData.id,
      modelFormat: 'pt',  // 默认模型格式
      accuracy: row.originalData.accuracy || 0.0,  // 添加准确率
    }
    
    await createModel(modelData)
    ElMessage.success('模型发布成功')
    await loadTrainingData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('发布模型失败：' + error.message)
    }
  }
}

// 校验模型
const handleValidateModel = (row) => {
  try {
    // 设置当前校验项
    currentValidationItem.value = row
    showValidationView.value = true
    ElMessage.info('进入模型校验页面')
  } catch (error) {
    ElMessage.error('进入校验页面失败：' + error.message)
  }
}

// 删除训练任务
const handleDeleteTraining = async (row) => {
  try {
    await ElMessageBox.confirm(`确认要删除训练任务"${row.algorithmName}"吗？`, '确认删除', {
      type: 'warning'
    })
    
    await deleteTraining(row.originalData.id)
    ElMessage.success('训练任务已删除')
    await loadTrainingData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除训练任务失败：' + error.message)
    }
  }
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  loadTrainingData()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadTrainingData()
}

// 加载训练数据
const loadTrainingData = async () => {
  try {
    loading.value = true
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      taskName: searchForm.value.trainName,
    }
    
    // 处理日期范围
    if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
      params.createdTimeBegin = searchForm.value.dateRange[0]
      params.createdTimeEnd = searchForm.value.dateRange[1]
    }
    
    const response = await getTrainingPage(params)
    if (response.data) {
      tableData.value = response.data.records.map(item => ({
        id: item.id,
        modelId: item.id,
        algorithmName: item.taskName,
        trainType: item.trainType,
        targetModel: item.targetModel,
        trainAlgorithm: item.algorithmName || '-',
        trainStatus: getTrainStatusText(item.trainStatus),
        mAP: item.mapValue ? `${(item.mapValue * 100).toFixed(1)}%` : null,
        precision: item.precisionValue ? `${(item.precisionValue * 100).toFixed(1)}%` : null,
        recall: item.recallValue ? `${(item.recallValue * 100).toFixed(1)}%` : null,
        correspondingModel: null,
        originalData: item
      }))
      total.value = response.data.total
    }
  } catch (error) {
    ElMessage.error('加载训练数据失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 训练状态转换
const getTrainStatusText = (status) => {
  const statusMap = {
    'pending': '等待',
    'training': '训练中',
    'completed': '训练完成',
    'failed': '训练失败',
    'stop': '停止'
  }
  return statusMap[status] || status
}

// 生成训练命令字符串（仅用于展示）
const buildTrainCommand = ({ task, datasetPath, modelPath, epochs, imgsz, batchSize: batch }) => {
  const safeModel = modelPath || '<默认模型>'
  const safeData = datasetPath || '<未配置数据集>'
  return `yolo ${task} train data=${safeData} model=${safeModel} epochs=${epochs} imgsz=${imgsz} batch=${batch}`
}

const appendTerminalInfo = (message) => {
  terminalOutput.value += `<div class="terminal-line"><span class="terminal-info">[INFO]</span> ${message}</div>`
  scrollToBottom()
}

const updateTrainingMeta = (item, taskId, statusValue, modelPath, logPath) => {
  if (!item) return
  const itemId = item.originalData?.id ?? item.id
  if (itemId !== taskId) return

  if (statusValue) {
    item.trainStatus = getTrainStatusText(statusValue)
    if (item.originalData) {
      item.originalData.trainStatus = statusValue
    }
  }
  if (item.originalData) {
    if (modelPath) item.originalData.modelOutputPath = modelPath
    if (logPath) item.originalData.logPath = logPath
  }
}

const appendTrainingPaths = (modelPath, logPath) => {
  if (modelPath && modelPath !== lastModelPath.value) {
    lastModelPath.value = modelPath
    appendTerminalInfo(`\u6a21\u578b\u8def\u5f84: ${modelPath}`)
  }
  if (logPath && logPath !== lastLogPath.value) {
    lastLogPath.value = logPath
    appendTerminalInfo(`\u65e5\u5fd7\u8def\u5f84: ${logPath}`)
  }
}

const detectStatusFromLogs = (logContent) => {
  if (!logContent) return null
  const text = Array.isArray(logContent) ? logContent.join('\n') : String(logContent)
  if (!text.trim()) return null
  if (/(?:\u8bad\u7ec3\u5b8c\u6210|training completed)/i.test(text)) return 'completed'
  if (/(?:\u8bad\u7ec3\u5931\u8d25|training failed)/i.test(text)) return 'failed'
  if (/(?:\u5df2\u505c\u6b62|stopped)/i.test(text)) return 'stopped'
  return null
}

const isFinishedStatus = (statusValue) => {
  if (!statusValue) return false
  const normalized = String(statusValue)
  return ['completed', 'failed', 'stopped', '\u8bad\u7ec3\u5b8c\u6210', '\u8bad\u7ec3\u5931\u8d25', '\u8bad\u7ec3\u7ec8\u6b62'].includes(normalized)
}

const isCompletedStatus = (statusValue) => {
  if (!statusValue) return false
  const normalized = String(statusValue)
  return ['completed', '\u8bad\u7ec3\u5b8c\u6210'].includes(normalized)
}

const resolveAutoPublishItem = (taskId) => {
  const tableItem = tableData.value.find(item => item.id === taskId)
  if (tableItem?.originalData) return tableItem
  const currentId = currentTrainingItem.value?.originalData?.id ?? currentTrainingItem.value?.id
  if (currentTrainingItem.value && currentId === taskId) {
    return currentTrainingItem.value
  }
  return null
}

const triggerAutoPublish = async (taskId, statusValue, displayStatus) => {
  if (autoPublish.value !== 'yes') return
  if (!isCompletedStatus(statusValue) && !isCompletedStatus(displayStatus)) return
  if (autoPublishTriggered.value.has(taskId)) return

  const item = resolveAutoPublishItem(taskId)
  if (!item?.originalData) return

  const trainingData = item.originalData
  if (!trainingData.modelOutputPath) {
    appendTerminalInfo('Auto publish skipped: model path missing.')
    return
  }

  autoPublishTriggered.value.add(taskId)
  currentDeployItem.value = item
  deployForm.value.modelName = item.algorithmName || trainingData.taskName || deployForm.value.modelName || 'model'
  deployForm.value.modelVersion = trainingData.version || deployForm.value.modelVersion
  await handleDeployModel()
}


// 训练日志与状态轮询
const appendLogLines = (logData) => {
  if (logData === undefined || logData === null) return
  const raw = Array.isArray(logData) ? logData.join('\n') : String(logData)
  if (!raw.trim()) return
  let content = raw.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  content = content.replace(/\u001b\[[0-9;?]*[ -/]*[@-~]/g, '')
  if (!content.trim()) return

  let newContent = content
  if (lastLogContent.value && content.startsWith(lastLogContent.value)) {
    newContent = content.slice(lastLogContent.value.length)
  }

  lastLogContent.value = content
  const lines = newContent.split('\n')
  if (lines.length && lines[lines.length - 1] === '') {
    lines.pop()
  }
  lastLogCount.value = lines.length
  if (!lines.length) return

  lines.forEach(line => {
    terminalOutput.value += `<div class=\"terminal-line\">${line}</div>`
  })
  scrollToBottom()
}

const stopLogPolling = () => {
  if (logPollingTimer.value) {
    clearInterval(logPollingTimer.value)
    logPollingTimer.value = null
  }
}

const stopStatusPolling = () => {
  if (statusPollingTimer.value) {
    clearInterval(statusPollingTimer.value)
    statusPollingTimer.value = null
  }
}

const stopAllPolling = () => {
  stopLogPolling()
  stopStatusPolling()
}

const startLogPolling = (taskId) => {
  stopLogPolling()
  lastLogContent.value = ''
  lastLogCount.value = 0
  const fetchOnce = async () => {
    try {
      const res = await getTrainingLogs(taskId)
      const payload = res?.data ?? res
      const data = payload?.data ?? payload
      const logs = data?.logContent ?? data?.data ?? data
      const statusValue = data?.status ?? data?.trainStatus ?? payload?.status ?? payload?.trainStatus
      const completedFlag = data?.completed ?? payload?.completed
      const modelPath = data?.modelOutputPath ?? data?.modelPath ?? data?.modelFilePath ?? payload?.modelOutputPath ?? payload?.modelPath ?? payload?.modelFilePath
      const logPath = data?.logPath ?? data?.logFilePath ?? data?.logFile ?? payload?.logPath ?? payload?.logFilePath ?? payload?.logFile
      appendLogLines(logs)

      const detectedStatus = detectStatusFromLogs(logs)
      let resolvedStatus = statusValue
      if (completedFlag === true) {
        resolvedStatus = 'completed'
      }
      if (detectedStatus && (!resolvedStatus || !isFinishedStatus(resolvedStatus))) {
        resolvedStatus = detectedStatus
      }

      updateTrainingMeta(currentTrainingItem.value, taskId, resolvedStatus, modelPath, logPath)
      const tableItem = tableData.value.find(item => item.id === taskId)
      if (tableItem && tableItem !== currentTrainingItem.value) {
        updateTrainingMeta(tableItem, taskId, resolvedStatus, modelPath, logPath)
      }

      appendTrainingPaths(modelPath, logPath)

      if (isFinishedStatus(resolvedStatus)) {
        await convertModel(taskId)
        isTraining.value = false
        stopAllPolling()
        await loadTrainingData()
        const displayStatus = getTrainStatusText(resolvedStatus)
        appendTerminalInfo(`\u8bad\u7ec3\u72b6\u6001: ${displayStatus}`)
        await triggerAutoPublish(taskId, resolvedStatus, displayStatus)
        const refreshedItem = tableData.value.find(item => item.id === taskId)
        if (refreshedItem?.originalData) {
          appendTrainingPaths(
            refreshedItem.originalData.modelOutputPath,
            refreshedItem.originalData.logPath || refreshedItem.originalData.logFilePath
          )
        }
      }

      // 日志接口返回了完成状态时，提前停止轮询
    } catch (error) {
      console.error('拉取训练日志失败:', error)
    }
  }
  fetchOnce()
  logPollingTimer.value = setInterval(fetchOnce, 2000)
}

const startStatusPolling = (taskId) => {
  stopStatusPolling()
  statusPollingTimer.value = setInterval(async () => {
    try {
      const res = await getTrainingStatus(taskId)
      const payload = res?.data ?? res
      let statusValue = payload?.status ?? payload?.trainStatus ?? payload?.completed ?? payload
      if (statusValue === true) {
        statusValue = 'completed'
      }
      if (!statusValue) return

      const displayStatus = getTrainStatusText(statusValue)
      const tableItem = tableData.value.find(item => item.id === taskId)
      if (tableItem) {
        tableItem.trainStatus = displayStatus
      }

      if (isFinishedStatus(statusValue) || isFinishedStatus(displayStatus)) {
        isTraining.value = false
        stopAllPolling()
        await loadTrainingData()
        appendLogLines(`[INFO] Training status: ${displayStatus}`)
        await triggerAutoPublish(taskId, statusValue, displayStatus)
      }
    } catch (error) {
      console.error('Failed to fetch training status:', error)
    }
  }, 3000)
}

// 启动日志+状态轮询的统一入口
const startPollingForTask = (taskId, tip = true) => {
  if (!taskId) return
  if (tip) {
    appendLogLines(`[INFO] 开始监听训练任务 ${taskId} 的日志...`)
  }
  startLogPolling(taskId)
  startStatusPolling(taskId)
}

// 搜索和重置处理
const handleSearch = () => {
  currentPage.value = 1
  loadTrainingData()
}

const handleReset = () => {
  searchForm.value = {
    trainName: '',
    dateRange: []
  }
  currentPage.value = 1
  loadTrainingData()
}

// 返回列表页面 - 已移除，改用弹窗模式

// 提交表单 - 已移除，改用弹窗模式

// 弹窗相关方法
const handleCloseDialog = () => {
  showAddDialog.value = false
  // 重置表单
  if (addFormRef.value) {
    addFormRef.value.resetFields()
  }
}

const handleCloseEditDialog = () => {
  showEditDialog.value = false
  // 重置表单
  if (editFormRef.value) {
    editFormRef.value.resetFields()
  }
  editingTrainingItem.value = null
}

const handleConfirmEdit = async () => {
  if (!editFormRef.value) return
  
  try {
    await editFormRef.value.validate()
    
    const formData = {
      id: editingTrainingItem.value.originalData.id,
      taskName: editForm.value.taskName,
      algorithmId: Number(editForm.value.algorithmId),
      datasetId: editForm.value.datasetId ? Number(editForm.value.datasetId) : null,
      epochTotal: editForm.value.epochTotal,
      configParams: editForm.value.configParams ? JSON.parse(editForm.value.configParams) : null,
      description: editForm.value.description
    }
    
    await updateTraining(formData)
    ElMessage.success('训练任务更新成功')
    
    showEditDialog.value = false
    editingTrainingItem.value = null
    selectedRows.value = []
    loadTrainingData()
  } catch (error) {
    console.error('更新失败:', error)
    ElMessage.error('更新失败：' + error.message)
  }
}

// SSH 终端相关：仅用于展示后台日志，保持接口兼容
const connectSSH = () => {
  sshConnected.value = true
  appendLogLines('[INFO] 终端已连接，开始展示后台实时日志')
}

const disconnectSSH = () => {
  sshConnected.value = false
  appendLogLines('[INFO] 终端连接已关闭')
}

const clearTerminal = () => {
  terminalOutput.value = ''
}

const executeCommand = () => {
  currentCommand.value = ''
  appendLogLines('[WARN] 训练命令由后台触发，前端终端仅用于展示日志')
}

// 训练命令执行已移除，保持接口兼容
const executeCommandInternal = async () => {
  appendLogLines('[WARN] 训练命令执行已移除，终端仅展示后台日志')
}

// 滚动到最新日志
const scrollToBottom = () => {
  nextTick(() => {
    if (terminalRef.value) {
      terminalRef.value.scrollTop = terminalRef.value.scrollHeight
    }
  })
}

// 发布为模型
const handleDeployModel = async () => {
  try {
    if (!deployForm.value.modelName) {
      ElMessage.error('请输入模型名称')
      return
    }
    
    const trainingData = currentDeployItem.value.originalData
    const modelData = {
      modelName: deployForm.value.modelName,
      algorithmId: trainingData.algorithmId,
      trainingId: trainingData.id,
      version: deployForm.value.modelVersion,
      modelFormat: 'pt', // 默认格式
      accuracy: trainingData.accuracy
    }
    
    await createModel(modelData)
    ElMessage.success('模型发布成功')
    showDeployDialog.value = false
    
    // 重置表单
    deployForm.value = {
      modelName: '',
      modelType: '物体检测',
      repository: '',
      versionDescription: ''
    }
  } catch (error) {
    ElMessage.error('发布模型失败：' + error.message)
  }
}

// 高级搜索相关方法
const handleAdvancedSearch = (searchData) => {
  console.log('高级搜索:', searchData)
  
  // 更新搜索表单
  if (searchData.keyword) {
    searchForm.trainName = searchData.keyword
  }
  if (searchData.trainName) {
    searchForm.trainName = searchData.trainName
  }
  if (searchData.dateRange && searchData.dateRange.length > 0) {
    searchForm.dateRange = searchData.dateRange
  }
  
  handleSearch()
}

const handleAdvancedSearchReset = () => {
  console.log('重置高级搜索')
  searchForm.trainName = ''
  searchForm.dateRange = []
  handleReset()
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

// 页面挂载时加载数据
onMounted(() => {
  loadTrainingData()
  loadAlgorithmOptions()
})

onUnmounted(() => {
  stopAllPolling()
})

// 版本配置页面返回方法
const handleBackFromVersion = () => {
  showVersionConfig.value = false
  currentVersionItem.value = null
  modelHistoryLoading.value = false
  modelHistoryList.value = []
  modelHistoryCurrentPage.value = 1
  modelHistoryTotal.value = 0
  historyTrainingId.value = null
}

// 校验页面返回方法
const handleBackFromValidation = () => {
  showValidationView.value = false
  currentValidationItem.value = null
  uploadedImage.value = null
  detectionResults.value = []
}

// 图片上传相关方法
const handleImageUpload = () => {
  ElMessage.info('点击选择图片')
}

const handleImageDrop = (event) => {
  event.preventDefault()
  const files = event.dataTransfer.files
  if (files.length > 0) {
    const file = files[0]
    if (validateImageFile(file)) {
      uploadedImage.value = file
      ElMessage.success('图片上传成功')
    }
  }
}

const validateImageFile = (file) => {
  const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/bmp']
  const maxSize = 4 * 1024 * 1024 // 4MB
  
  if (!validTypes.includes(file.type)) {
    ElMessage.error('图片格式不支持，请上传jpg、png、bmp、jpeg格式的图片')
    return false
  }
  
  if (file.size > maxSize) {
    ElMessage.error('图片大小不能超过4M')
    return false
  }
  
  return true
}

// 发布为模型相关方法
const handleDeployConfirm = () => {
  handleDeployModel()
}

const handleDeployCancel = () => {
  showDeployDialog.value = false
  
  // 重置表单
  deployForm.value = {
    modelName: '',
    modelType: '物体检测',
    repository: '',
    versionDescription: ''
  }
}

// 介绍内容折叠/展开处理
const handleIntroToggle = () => {
  isIntroExpanded.value = !isIntroExpanded.value
}

// 数据集相关变量
const datasetOptions = ref([])
const datasetLoading = ref(false)

// 算法列表（基础预置算法库）
const algorithmOptions = ref([])
const algorithmLoading = ref(false)

// 数据集选择弹窗相关
const showDatasetSelector = ref(false)
const datasetSearchKeyword = ref('')
const selectedDatasetIds = ref([])
const selectedDatasets = ref([])
const isTraining = ref(false)
const filteredDatasetOptions = computed(() => {
  if (!datasetSearchKeyword.value.trim()) {
    return datasetOptions.value
  }
  return datasetOptions.value.filter(dataset =>
    dataset.annotationName.toLowerCase().includes(datasetSearchKeyword.value.toLowerCase())
  )
})

// 标注类型常量
const ANNOTATION_TYPES = {
  OBJECT_DETECTION: 'object_detection',
  IMAGE_CLASSIFICATION: 'image_classification',
  INSTANCE_SEGMENTATION: 'instance_segmentation',
  SEMANTIC_SEGMENTATION: 'semantic_segmentation'
}

// 标注类型显示映射
const ANNOTATION_TYPE_LABELS = {
  [ANNOTATION_TYPES.OBJECT_DETECTION]: '物体检测',
  [ANNOTATION_TYPES.IMAGE_CLASSIFICATION]: '图像分类',
  [ANNOTATION_TYPES.INSTANCE_SEGMENTATION]: '实例分割',
  [ANNOTATION_TYPES.SEMANTIC_SEGMENTATION]: '语义分割'
}

// 标注状态常量
const ANNOTATION_STATUS = {
  NONE: 'none',
  PARTIAL: 'partial',
  COMPLETED: 'completed'
}

// 标注状态显示映射
const ANNOTATION_STATUS_LABELS = {
  [ANNOTATION_STATUS.NONE]: '未标注',
  [ANNOTATION_STATUS.PARTIAL]: '标注中',
  [ANNOTATION_STATUS.COMPLETED]: '已完成'
}

// 获取进度百分比
const getProgressPercentage = (annotatedCount, totalCount) => {
  if (totalCount === 0) return 0
  return Math.round((annotatedCount / totalCount) * 100)
}

// 获取状态对应的标签类型
const getStatusTagType = (status) => {
  switch(status) {
    case ANNOTATION_STATUS.NONE:
      return 'info'
    case ANNOTATION_STATUS.PARTIAL:
      return 'warning'
    case ANNOTATION_STATUS.COMPLETED:
      return 'success'
    default:
      return 'info'
  }
}

// 加载数据集选项
const loadDatasetOptions = async () => {
  datasetLoading.value = true
  try {
    const params = {
      current: 1,
      size: 1000 // 获取所有数据集，不限制状态
    }
    
    const response = await getAlgorithmAnnotationPage(params)
    if (response.code === 200) {
      datasetOptions.value = response.data.records.map(item => ({
        value: item.id,
        label: `${item.annotationName} (${item.annotatedCount}/${item.totalCount})`,
        annotationName: item.annotationName,
        annotationType: item.annotationType,
        typeLabel: ANNOTATION_TYPE_LABELS[item.annotationType],
        annotatedCount: item.annotatedCount,
        totalCount: item.totalCount,
        progress: getProgressPercentage(item.annotatedCount, item.totalCount),
        datasetPath: item.datasetPath,
        status: item.annotationStatus,
        statusLabel: ANNOTATION_STATUS_LABELS[item.annotationStatus]
      }))
    }
  } catch (error) {
    console.error('加载数据集失败:', error)
    ElMessage.error('加载数据集失败')
  } finally {
    datasetLoading.value = false
  }
}

// 加载基础预置算法列表
const loadAlgorithmOptions = async () => {
  algorithmLoading.value = true
  try {
    const params = {
      current: 1,
      size: 500
    }
    const response = await getAlgorithmPage(params)
    const records = response?.data?.records || response?.records || []
    algorithmOptions.value = records.map(item => ({
      value: item.id,
      label: `${item.name}${item.version ? ` (${item.version})` : ''}`,
      name: item.name,
      type: item.type || item.category || '',
      ptModelFilePath: item.ptModelFilePath
    }))
  } catch (error) {
    console.error('加载算法列表失败:', error)
    ElMessage.error('加载算法列表失败')
  } finally {
    algorithmLoading.value = false
  }
}

// 数据集选择相关方法
const handleDatasetSearch = () => {
  // 搜索逻辑已在计算属性中处理
}

const toggleDatasetSelection = (dataset) => {
  const index = selectedDatasetIds.value.indexOf(dataset.value)
  if (index > -1) {
    selectedDatasetIds.value.splice(index, 1)
    const datasetIndex = selectedDatasets.value.findIndex(d => d.value === dataset.value)
    if (datasetIndex > -1) {
      selectedDatasets.value.splice(datasetIndex, 1)
    }
  } else {
    selectedDatasetIds.value.push(dataset.value)
    selectedDatasets.value.push(dataset)
  }
}

const handleCloseDatasetSelector = () => {
  showDatasetSelector.value = false
  // 重置搜索关键词
  datasetSearchKeyword.value = ''
}

const handleConfirmDatasetSelection = () => {
  if (selectedDatasetIds.value.length === 0) {
    ElMessage.warning('请至少选择一个数据集')
    return
  }

  // 更新数据集表格显示
  updateDatasetTable()

  // 关闭弹窗
  showDatasetSelector.value = false
  datasetSearchKeyword.value = ''

  ElMessage.success(`已选择 ${selectedDatasetIds.value.length} 个数据集`)
}

const updateDatasetTable = () => {
  // 这里可以更新训练配置页面中的数据集表格显示
  console.log('选中的数据集:', selectedDatasets.value)
}

const removeDataset = (datasetId) => {
  const index = selectedDatasetIds.value.indexOf(datasetId)
  if (index > -1) {
    selectedDatasetIds.value.splice(index, 1)
  }

  const datasetIndex = selectedDatasets.value.findIndex(d => d.value === datasetId)
  if (datasetIndex > -1) {
    const removedDataset = selectedDatasets.value.splice(datasetIndex, 1)[0]
    ElMessage.success(`已移除数据集: ${removedDataset.annotationName}`)
  }
}

// 打开数据集选择器时加载数据
const openDatasetSelector = async () => {
  showDatasetSelector.value = true
  if (datasetOptions.value.length === 0) {
    await loadDatasetOptions()
  }
}
</script>

<style scoped>
.page-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 0;
  background-color: #f5f7fa;
  padding: 20px;
}

.list-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* 主内容区域 */
.main-content {
  background: white;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  flex: 1;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

/* 搜索区域 */
.search-section {
  background: #F5F5F5;
  border-radius: 8px 8px 0 0;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.search-form {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.search-input {
  width: 200px;
}

/* 页面标题 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.page-header h1 {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
  margin: 0;
}

.collapse-btn {
  color: #409eff;
  font-size: 14px;
  padding: 0;
}

/* 介绍区域 */
.intro-section {
  background: white;
  border-radius: 0;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-top: 1px solid #e8e8e8;
  margin-bottom: 24px;
}

.intro-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.intro-title {
  font-size: 24px;
  font-weight: 600;
  color: #262626;
  margin: 0;
}

.intro-content {
  font-size: 16px;
  color: #666;
  line-height: 1.6;
  margin: 0 0 32px 0;
}

.intro-text {
  margin: 0 0 32px 0;
  font-size: 16px;
}

/* 流程步骤 */
.process-steps {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 40px;
}

.step-item {
  flex: 1;
  text-align: center;
  position: relative;
}

.step-icon {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: white;
  position: relative;
}

.step-1, .step-2, .step-3, .step-4 {
  background: transparent;
}

.step-img {
  width: 68px;
  height: 68px;
  object-fit: contain;
}

.step-item h3 {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 8px 0;
}

.step-item p {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  margin: 0;
}

.arrow-icon {
  position: absolute;
  right: -25px;
  top: 30px;
  color: #d9d9d9;
  font-size: 20px;
  z-index: 1;
}

.step-item:last-child .arrow-icon {
  display: none;
}

/* 工具栏区域 */
.toolbar-section {
  background: white;
  border-radius: 0;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-top: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

/* 新增按钮自定义样式 */
.add-btn-custom {
  width: 82px !important;
  height: 36px !important;
  border-radius: 18px !important;
  background: #1A53FF !important;
  border-color: #1A53FF !important;
  padding: 0 !important;
  font-size: 14px;
  font-weight: 500;
  color: white !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
}

.add-btn-custom:hover,
.add-btn-custom:focus {
  background: #1A53FF !important;
  border-color: #1A53FF !important;
  color: white !important;
  opacity: 0.8;
}

.add-btn-custom:active {
  background: #1A53FF !important;
  border-color: #1A53FF !important;
  color: white !important;
  opacity: 0.9;
}

/* 编辑删除按钮组合 */
.edit-delete-group {
  display: flex;
  align-items: center;
  margin: 0;
  padding: 0;
}

.edit-delete-group .el-button + .el-button {
  margin-left: 0 !important;
}

/* 编辑按钮自定义样式 */
.edit-btn-custom {
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

.edit-btn-custom:hover,
.edit-btn-custom:focus {
  border-right: none !important;
  border-color: #409eff !important;
}

.edit-btn-custom:disabled {
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

/* 表格区域 */
.table-section {
  background: white;
  border-radius: 0;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  border-top: 1px solid #e8e8e8;
}

:deep(.el-table) {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  width: 100%;
}

:deep(.el-table__fixed-right) {
  border-left: 1px solid #ebeef5;
}

:deep(.el-table th) {
  background-color: #fafafa;
  font-weight: 600;
  color: #262626;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-table .el-table__row:hover > td) {
  background-color: #f5f7fa;
}

:deep(.el-table td) {
  border-bottom: 1px solid #ebeef5;
}

/* 状态标签 */
.status-tag {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-success {
  background-color: #67c23a;
}

.status-warning {
  background-color: #e6a23c;
}

.status-info {
  background-color: #909399;
}

.status-danger {
  background-color: #f56c6c;
}

/* 模型效果 */
.model-metrics {
  text-align: left;
}

.metric-item {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
  font-size: 12px;
}

.metric-label {
  color: #666;
}

.metric-value {
  color: #262626;
  font-weight: 500;
}

.info-icon {
  color: #ccc;
  font-size: 12px;
  cursor: pointer;
}

.detail-link {
  color: #409eff;
  font-size: 12px;
  padding: 0;
  margin-top: 4px;
}

/* 操作按钮强制一行展示 */
.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  flex-wrap: nowrap;
}

/* 训练完成状态的操作按钮 */
.completed-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.completed-actions .el-button {
  font-size: 12px;
  padding: 4px 8px;
  min-width: auto;
}

/* 下载模型按钮 - 绿色 */
.completed-actions .el-button--success.is-text {
  color: #67c23a !important;
}

.completed-actions .el-button--success.is-text:hover {
  color: #529b2e !important;
  background-color: rgba(103, 194, 58, 0.1) !important;
}

/* 发布为模型按钮 - 蓝色 */
.completed-actions .el-button--primary.is-text {
  color: #409eff !important;
}

.completed-actions .el-button--primary.is-text:hover {
  color: #337ecc !important;
  background-color: rgba(64, 158, 255, 0.1) !important;
}

/* 校验按钮 - 橙色 */
.completed-actions .el-button--warning.is-text {
  color: #e6a23c !important;
}

.completed-actions .el-button--warning.is-text:hover {
  color: #b88230 !important;
  background-color: rgba(230, 162, 60, 0.1) !important;
}

/* 分页区域 */
.pagination-section {
  display: flex;
  justify-content: center;
  background: white;
  border-radius: 0 0 8px 8px;
  padding: 16px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-top: 1px solid #e8e8e8;
  flex-shrink: 0;
}

/* 训练配置视图 */
.training-config-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

/* 导航栏样式 */
.content-header {
  background: white;
  border-bottom: 1px solid #e8e8e8;
  margin-bottom: 0;
  padding: 16px 20px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.breadcrumb-item {
  color: #409eff;
  cursor: pointer;
  transition: color 0.3s;
}

.breadcrumb-item:hover {
  color: #337ecc;
}

.breadcrumb-item.active {
  color: #262626;
  cursor: default;
}

.breadcrumb-separator {
  color: #999;
}

/* 训练配置容器 */
.training-config-container {
  display: flex;
  gap: 20px;
  flex: 1;
  padding: 0 20px 40px 0;
  align-items: flex-start;
}

/* 配置区域 */
.config-sidebar {
  width: 100%;
  max-width: 720px;
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow-y: visible;
  height: fit-content;
  min-height: 600px;
}

.config-section {
  margin-bottom: 32px;
}

.config-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 20px 0;
}

/* 训练方式标签 */
.training-mode-tabs {
  display: flex;
  width: 320px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

.mode-tab {
  flex: 1;
  padding: 12px 24px;
  text-align: center;
  cursor: pointer;
  background: #f8f9fa;
  color: #666;
  transition: all 0.3s;
  font-size: 14px;
  white-space: nowrap;
}

.mode-tab.active {
  background: #409eff;
  color: white;
}

/* 配置项 */
.config-item {
  margin-bottom: 24px;
}

.config-label {
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 12px;
}

.help-icon {
  margin-left: 6px;
  color: #999;
  cursor: help;
  font-size: 14px;
}

.radio-options,
.algorithm-options,
.epoch-options,
.publish-options,
.strategy-options {
  display: flex;
  flex-direction: row;
  gap: 24px;
  flex-wrap: wrap;
  align-items: center;
}

.radio-hint {
  font-size: 12px;
  color: #999;
  margin-left: 24px;
}

.switch-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.switch-label {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #262626;
}

.config-tip,
.config-hint {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
  margin-top: 8px;
}

.config-select {
  width: 200px;
}

.add-dataset-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #409eff;
  padding: 0;
  margin-bottom: 16px;
}

/* 数据集表格 */
.dataset-table {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
  font-size: 14px;
}

.dataset-header {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  background: #f8f9fa;
  padding: 12px 16px;
  font-weight: 500;
  color: #666;
  border-bottom: 1px solid #e8e8e8;
}

.dataset-row {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  padding: 12px 16px;
  align-items: center;
  color: #262626;
}

.remove-btn {
  color: #409eff;
  padding: 0;
}

/* 训练环境 */
.detail-link {
  color: #409eff;
  padding: 0;
  margin-bottom: 16px;
}

.environment-info {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 16px;
}

.env-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
}

.env-item:last-child {
  margin-bottom: 0;
}

.env-label {
  color: #666;
  font-weight: 500;
}

.env-value {
  color: #262626;
}

/* 训练操作按钮区域 */
.training-actions {
  margin-top: 32px;
  padding: 20px 0;
  border-top: 1px solid #f0f0f0;
  text-align: left;
}

.start-training-btn {
  width: 120px;
  height: 40px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 6px;
}

/* 版本配置视图 */
.version-config-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* 整体内容区域 */
.version-config-view .content-header {
  background: transparent;
  border: none;
  margin: 0;
  padding: 0;
}

.version-config-container {
  flex: 1;
  padding: 20px;
  overflow: hidden;
}

/* 完整的白色内容区域 */
.version-content-block {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 1670px;
  overflow: hidden;
}

/* 导航栏在内容块内部 */
.version-config-view .breadcrumb {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  margin: 0;
}

/* 基础信息区域 */
.basic-info-section {
  background: white;
  border-radius: 0;
  padding: 20px;
  margin-bottom: 0;
  border-top: none;
  box-shadow: none;
}

.info-grid {
  margin-top: 20px;
}

.info-row {
  display: flex;
  margin-bottom: 20px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-right: 40px;
}

.info-item:last-child {
  margin-right: 0;
}

.info-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: #262626;
  line-height: 1.5;
}

/* 数据详情区域 */
.data-details-section {
  background: white;
  border-radius: 0;
  padding: 20px;
  border-top: 1px solid #f0f0f0;
  box-shadow: none;
}

.dataset-title {
  font-size: 14px;
  color: #262626;
  margin: 20px 0 16px 0;
  font-weight: 500;
}

.dataset-table {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
}

.dataset-table-header {
  display: grid;
  grid-template-columns: 80px 1fr 2fr;
  background: #f8f9fa;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  border-bottom: 1px solid #e8e8e8;
}

.dataset-table-row {
  display: grid;
  grid-template-columns: 80px 1fr 2fr;
  padding: 12px 16px;
  font-size: 14px;
  color: #262626;
  border-bottom: 1px solid #f0f0f0;
}

.dataset-table-row:last-child {
  border-bottom: none;
}

.history-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.col-no,
.col-name,
.col-effect {
  display: flex;
  align-items: center;
}

/* 校验模型视图 */
.validation-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.validation-view .content-header {
  background: transparent;
  border: none;
  margin: 0;
  padding: 0;
}

.validation-config-container {
  flex: 1;
  padding: 20px;
  overflow: hidden;
}

.validation-content-block {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 1670px;
  overflow: hidden;
  padding-bottom: 40px;
}

.validation-view .breadcrumb {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  margin: 0;
}

/* 模型选择区域 */
.model-selection-section {
  padding: 20px 20px 0;
}

.model-info-inputs {
  display: flex;
  gap: 16px;
  max-width: 500px;
}

.model-name-select,
.model-version-select {
  flex: 1;
}

.model-name-select :deep(.el-select__wrapper) {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  box-shadow: none;
}

.model-version-select :deep(.el-select__wrapper) {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  box-shadow: none;
}

.model-name-select :deep(.el-select__wrapper:hover),
.model-version-select :deep(.el-select__wrapper:hover) {
  border-color: #409eff;
}

.model-name-select :deep(.el-select__wrapper.is-focused),
.model-version-select :deep(.el-select__wrapper.is-focused) {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

/* 图片上传和阈值配置水平对齐区域 */
.upload-threshold-section {
  display: flex;
  gap: 40px;
  padding: 20px;
  border-top: 1px solid #f0f0f0;
  align-items: flex-start;
  width: 100%;
}

.image-upload-area {
  flex: 2;
  min-width: 600px;
  max-width: 800px;
}

.threshold-config-area {
  flex: 1;
  min-width: 350px;
  max-width: 450px;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.threshold-section {
  width: 100%;
  margin-bottom: 16px;
}

.threshold-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.threshold-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.threshold-value {
  font-size: 16px;
  color: #262626;
  font-weight: 600;
}

.threshold-slider {
  margin-bottom: 20px;
}

.threshold-slider :deep(.el-slider__runway) {
  background-color: #e8e8e8;
}

.threshold-slider :deep(.el-slider__bar) {
  background-color: #409eff;
}

.threshold-slider :deep(.el-slider__button) {
  border: 2px solid #409eff;
  background-color: white;
}

.threshold-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}

.threshold-tag {
  display: flex;
  align-items: center;
}

.tag-label {
  font-size: 14px;
  color: #666;
}

.confidence-display {
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

/* 图片上传区域样式 */
.upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-area:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.upload-icon {
  margin-bottom: 20px;
}

.upload-btn {
  margin-bottom: 16px;
  background: #409eff;
  border-color: #409eff;
  padding: 10px 24px;
  border-radius: 6px;
}

.upload-tips {
  text-align: center;
}

.drag-tip {
  font-size: 14px;
  color: #666;
  margin: 0 0 8px 0;
}

.format-tip {
  font-size: 12px;
  color: #999;
  margin: 0;
  line-height: 1.4;
}

/* 预测标签结果显示 */
.prediction-result {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  background: white;
  min-height: 200px;
  text-align: center;
}

.no-result-text {
  font-size: 14px;
  color: #999;
  font-style: italic;
}

/* 发布为模型弹窗样式 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-dialog__header) {
  padding: 20px 24px 12px;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

:deep(.el-dialog__body) {
  padding: 20px 24px;
}

:deep(.el-form-item__label) {
  color: #262626;
  font-weight: 500;
}

:deep(.el-form-item.is-required .el-form-item__label::before) {
  content: '*';
  color: #f56c6c;
  margin-right: 4px;
}

:deep(.el-input__wrapper) {
  border-radius: 6px;
}

:deep(.el-select .el-input__wrapper) {
  border-radius: 6px;
}

:deep(.el-textarea__inner) {
  border-radius: 6px;
  font-family: inherit;
}

/* 编辑视图样式 */
.edit-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
  background-color: #f5f7fa;
  padding: 16px;
  overflow: hidden;
}

.breadcrumb-nav {
  background: #F0F2F5;
  border-radius: 8px 8px 0 0;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.breadcrumb-item {
  color: #606266;
  cursor: pointer;
  font-size: 14px;
}

.breadcrumb-item:hover {
  color: #1A53FF;
}

.breadcrumb-item.active {
  color: #303133;
  font-weight: 500;
}

.breadcrumb-separator {
  color: #c0c4cc;
  margin: 0 8px;
}

.edit-content {
  flex: 1;
  background: white;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.training-form {
  max-width: 600px;
  flex: 1;
  overflow-y: auto;
  padding-right: 10px;
}

/* 优化表单字段间距 */
.training-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.training-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #303133;
}

.training-form :deep(.el-input),
.training-form :deep(.el-select),
.training-form :deep(.el-input-number) {
  width: 480px;
}

/* 新增任务状态提示样式 */
.status-hint {
  margin-bottom: 16px;
  flex-shrink: 0;
}

.status-hint :deep(.el-alert) {
  border-radius: 8px;
}

.status-hint :deep(.el-alert__title) {
  font-weight: 500;
  color: #1A53FF;
}

.status-hint :deep(.el-alert__description) {
  color: #606266;
  line-height: 1.5;
}

.form-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

/* 表格操作按钮样式 */
.table-action-buttons {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

/* 确保操作按钮为纯文字样式 */
.table-action-buttons :deep(.el-button--primary.is-text) {
  color: #1A53FF !important;
  background: transparent !important;
  border: none !important;
  padding: 2px 8px !important;
}

.table-action-buttons :deep(.el-button--primary.is-text:hover) {
  color: #3d70ff !important;
  background: transparent !important;
  border: none !important;
}

.table-action-buttons :deep(.el-button--danger.is-text) {
  color: #f56c6c !important;
  background: transparent !important;
  border: none !important;
  padding: 2px 8px !important;
}

.table-action-buttons :deep(.el-button--danger.is-text:hover) {
  color: #f78989 !important;
  background: transparent !important;
  border: none !important;
}

/* 下拉菜单样式 */
.table-action-buttons :deep(.el-dropdown) {
  margin-left: 8px;
}

.table-action-buttons :deep(.el-dropdown .el-button) {
  color: #1A53FF !important;
  background: transparent !important;
  border: none !important;
  padding: 2px 8px !important;
}

.table-action-buttons :deep(.el-dropdown .el-button:hover) {
  color: #3d70ff !important;
  background: transparent !important;
  border: none !important;
}

/* 新增训练任务弹窗样式 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 弹窗表单样式 */
:deep(.el-dialog__body) {
  padding: 20px 24px;
}

:deep(.el-dialog__header) {
  padding: 20px 24px 0;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.el-dialog__footer) {
  padding: 0 24px 20px;
}

/* 弹窗表单字段样式 */
:deep(.el-form-item__label) {
  font-weight: 500;
  color: #303133;
}

:deep(.el-input),
:deep(.el-select),
:deep(.el-input-number) {
  width: 100%;
}

:deep(.el-textarea__inner) {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
}

/* 数据集选项样式 */
.dataset-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 0;
}

.dataset-name {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

.dataset-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.dataset-type {
  color: #606266;
}

.dataset-progress {
  color: #409eff;
  font-weight: 500;
}

.algorithm-option {
  display: flex;
  flex-direction: column;
}

.algorithm-name {
  font-weight: 600;
  color: #262626;
}

.algorithm-meta {
  font-size: 12px;
  color: #8c8c8c;
  display: flex;
  gap: 12px;
  margin-top: 2px;
}

/* 数据集选择器样式 */
:deep(.el-select-dropdown__item) {
  padding: 8px 12px;
}

:deep(.el-select-dropdown__item.selected) {
  background-color: #f0f9ff;
  color: #1A53FF;
}

:deep(.el-select-dropdown__item:hover) {
  background-color: #f5f7fa;
}

/* SSH终端样式 */
.ssh-terminal-container {
  position: sticky;
  top: 20px;
  width: 65%;
  height: 1000px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
  z-index: 10;
}

.terminal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
  border-radius: 8px 8px 0 0;
}

.terminal-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.terminal-controls {
  display: flex;
  gap: 8px;
}

.terminal-content {
  flex: 1;
  background: #1e1e1e;
  color: #ffffff;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.4;
  padding: 15px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.terminal-output {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 10px;
}

.terminal-line {
  margin-bottom: 4px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.4;
  display: block;
}

.terminal-prompt {
  color: #00ff00;
  font-weight: bold;
}

.terminal-info {
  color: #17a2b8;
  font-weight: bold;
}

.terminal-success {
  color: #28a745;
  font-weight: bold;
}

.terminal-warning {
  color: #ffc107;
  font-weight: bold;
}

.terminal-error {
  color: #dc3545;
  font-weight: bold;
}

.terminal-response {
  color: #e9ecef;
  margin-left: 0;
}

.terminal-input-line {
  display: flex;
  align-items: center;
  margin-top: auto;
}

.terminal-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #ffffff;
  font-family: inherit;
  font-size: inherit;
  margin-left: 5px;
}

.terminal-input::placeholder {
  color: #6c757d;
}

.disabled-hint {
  flex: 1;
  color: #c0c4cc;
}

/* 终端颜色样式 - 使用 :deep() 穿透到 v-html 内容 */
.terminal-output :deep(.terminal-dir) {
  color: #5dade2 !important;
  font-weight: bold;
}

.terminal-output :deep(.terminal-file) {
  color: #f8c471 !important;
}

.terminal-output :deep(.terminal-path) {
  color: #85c1e9 !important;
  font-weight: bold;
}

.terminal-output :deep(.terminal-user) {
  color: #58d68d !important;
  font-weight: bold;
}

.terminal-output :deep(.terminal-host) {
  color: #f7dc6f !important;
  font-weight: bold;
}

.terminal-output :deep(.terminal-interface) {
  color: #bb8fce !important;
  font-weight: bold;
}

.terminal-output :deep(.terminal-ip) {
  color: #7fb3d3 !important;
  font-weight: bold;
}

.terminal-output :deep(.terminal-gpu-header) {
  color: #76d7c4 !important;
}

.terminal-output :deep(.terminal-gpu-name) {
  color: #f39c12 !important;
  font-weight: bold;
}

.terminal-output :deep(.terminal-temp) {
  color: #e74c3c !important;
  font-weight: bold;
}

.terminal-output :deep(.terminal-power) {
  color: #f1c40f !important;
}

.terminal-output :deep(.terminal-memory) {
  color: #9b59b6 !important;
  font-weight: bold;
}

.terminal-output :deep(.terminal-util) {
  color: #2ecc71 !important;
  font-weight: bold;
}

.terminal-output :deep(.terminal-python) {
  color: #3498db !important;
}

.terminal-output :deep(.terminal-package) {
  color: #e67e22 !important;
}

.terminal-output :deep(.terminal-env) {
  color: #27ae60 !important;
  font-weight: bold;
}

.terminal-output :deep(.terminal-comment) {
  color: #95a5a6 !important;
  font-style: italic;
}

.terminal-output :deep(.terminal-config) {
  color: #f39c12 !important;
}

.terminal-output :deep(.terminal-process) {
  color: #ecf0f1 !important;
}

.terminal-output :deep(.terminal-header) {
  color: #bdc3c7 !important;
  font-weight: bold;
}

/* YOLO训练相关颜色 */
.terminal-output :deep(.terminal-training) {
  color: #f39c12 !important;
  font-weight: bold;
}

.terminal-output :deep(.terminal-epoch) {
  color: #3498db !important;
  font-weight: bold;
}

.terminal-output :deep(.terminal-progress) {
  color: #2ecc71 !important;
}

.terminal-output :deep(.terminal-metrics) {
  color: #ecf0f1 !important;
  font-family: 'Courier New', monospace;
}

.terminal-output :deep(.terminal-validation) {
  color: #9b59b6 !important;
  font-weight: bold;
}

.terminal-output :deep(.terminal-prediction) {
  color: #e67e22 !important;
  font-weight: bold;
}

.terminal-output :deep(.terminal-help) {
  color: #95a5a6 !important;
}

/* YOLO输出专用样式 */
.terminal-output :deep(.terminal-yolo-output) {
  display: block;
  line-height: 1.6;
  margin: 8px 0;
}

.terminal-output :deep(.terminal-yolo-output br) {
  display: block;
  margin: 4px 0;
  content: "";
}

.terminal-output :deep(.terminal-epoch-block) {
  display: block;
  margin: 8px 0;
  padding: 4px 0;
  border-left: 3px solid #3498db;
  padding-left: 8px;
}

.terminal-output :deep(.terminal-metrics-table) {
  display: block;
  margin: 4px 0;
  font-family: 'Courier New', monospace;
  background-color: rgba(52, 152, 219, 0.1);
  padding: 4px 8px;
  border-radius: 4px;
}

.terminal-output :deep(.terminal-metrics-header) {
  display: block;
  color: #bdc3c7 !important;
  font-weight: bold;
  margin-bottom: 2px;
}

.terminal-output :deep(.terminal-metrics-row) {
  display: block;
  color: #ecf0f1 !important;
}

/* 下载链接样式 */
.terminal-output :deep(.terminal-download) {
  color: #27ae60 !important;
  font-weight: bold;
}

.terminal-output :deep(.download-link) {
  color: #3498db !important;
  text-decoration: underline;
  cursor: pointer;
}

.terminal-output :deep(.download-link:hover) {
  color: #2980b9 !important;
}

/* 数据集选择弹窗样式 */
.dataset-selector-content {
  max-height: 500px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.dataset-search {
  margin-bottom: 16px;
}

.dataset-list {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  max-height: 400px;
}

.dataset-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.3s;
}

.dataset-item:hover {
  background-color: #f5f7fa;
}

.dataset-item.selected {
  background-color: #ecf5ff;
  border-color: #409eff;
}

.dataset-item:last-child {
  border-bottom: none;
}

.dataset-checkbox {
  margin-right: 12px;
}

.dataset-content {
  flex: 1;
}

.dataset-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.dataset-name {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.dataset-details {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #606266;
}

.dataset-type {
  color: #909399;
}

.dataset-progress {
  color: #409eff;
  font-weight: 500;
}

.dataset-count {
  color: #67c23a;
}

.empty-dataset-hint {
  text-align: center;
  padding: 20px;
  color: #909399;
  background-color: #f8f9fa;
  border-radius: 6px;
  border: 1px dashed #e4e7ed;
}

/* 训练按钮相关样式 */
.training-hint {
  margin-top: 8px;
  text-align: center;
}

.start-training-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
