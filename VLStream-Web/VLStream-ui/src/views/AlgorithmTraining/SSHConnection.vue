<template>
  <div class="ssh-connection-page">
    <div class="page-header">
      <h2>SSH远程连接</h2>
      <p>配置SSH连接信息，连接到远程服务器进行算法训练</p>
    </div>
    
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card header="连接配置">
          <SSHConnectionConfig @connection-ready="onConnectionReady" />
        </el-card>
      </el-col>
      
      <el-col :span="16">
        <el-card header="SSH终端">
          <div class="terminal-container">
            <SSHTerminal 
              v-if="connectionConfig"
              :connection="connectionConfig"
            />
            <div v-else class="no-connection">
              <el-empty description="请先配置SSH连接信息" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 快捷命令面板 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card header="快捷命令">
          <div class="quick-commands">
            <el-button-group>
              <el-button @click="executeQuickCommand('ls -la')">列出文件</el-button>
              <el-button @click="executeQuickCommand('pwd')">当前目录</el-button>
              <el-button @click="executeQuickCommand('df -h')">磁盘使用</el-button>
              <el-button @click="executeQuickCommand('free -h')">内存使用</el-button>
              <el-button @click="executeQuickCommand('nvidia-smi')">GPU状态</el-button>
            </el-button-group>
            
            <div class="custom-command">
              <el-input 
                v-model="customCommand"
                placeholder="输入自定义命令"
                @keyup.enter="executeQuickCommand(customCommand)"
                style="width: 300px; margin-left: 20px;"
              />
              <el-button 
                type="primary" 
                @click="executeQuickCommand(customCommand)"
                :disabled="!customCommand.trim()"
                style="margin-left: 10px;"
              >
                执行
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import SSHConnectionConfig from '@/components/SSHConnectionConfig.vue'
import SSHTerminal from '@/components/SSHTerminal.vue'

export default {
  name: 'SSHConnection',
  components: {
    SSHConnectionConfig,
    SSHTerminal
  },
  data() {
    return {
      connectionConfig: null,
      customCommand: ''
    }
  },
  methods: {
    onConnectionReady(config) {
      this.connectionConfig = config
      this.$message.success('SSH连接配置已就绪')
    },
    
    executeQuickCommand(command) {
      if (!command.trim()) return
      
      // 这里可以通过事件或ref调用终端组件的方法
      this.$message.info(`执行命令: ${command}`)
      
      // 清空自定义命令输入
      if (command === this.customCommand) {
        this.customCommand = ''
      }
    }
  }
}
</script>

<style scoped>
.ssh-connection-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.terminal-container {
  height: 500px;
}

.no-connection {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-commands {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.custom-command {
  display: flex;
  align-items: center;
}

.el-card {
  height: 100%;
}

.el-card :deep(.el-card__body) {
  height: calc(100% - 60px);
}
</style>




