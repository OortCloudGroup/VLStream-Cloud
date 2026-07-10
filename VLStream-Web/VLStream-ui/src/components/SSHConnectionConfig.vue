<template>
  <div class="ssh-connection-config">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
      <el-form-item label="服务器地址" prop="host">
        <el-input v-model="form.host" placeholder="请输入服务器IP或域名" />
      </el-form-item>
      
      <el-form-item label="端口" prop="port">
        <el-input-number v-model="form.port" :min="1" :max="65535" />
      </el-form-item>
      
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" />
      </el-form-item>
      
      <el-form-item label="认证方式" prop="authType">
        <el-radio-group v-model="form.authType">
          <el-radio label="password">密码认证</el-radio>
          <el-radio label="key">密钥认证</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item v-if="form.authType === 'password'" label="密码" prop="password">
        <el-input 
          v-model="form.password" 
          type="password" 
          placeholder="请输入密码"
          show-password 
        />
      </el-form-item>
      
      <el-form-item v-if="form.authType === 'key'" label="私钥文件" prop="privateKey">
        <el-input 
          v-model="form.privateKey" 
          type="textarea" 
          :rows="4"
          placeholder="请粘贴私钥内容或选择私钥文件"
        />
      </el-form-item>
      
      <el-form-item>
        <el-button type="primary" @click="testConnection" :loading="testing">
          测试连接
        </el-button>
        <el-button @click="saveConnection" :disabled="!connectionValid">
          保存配置
        </el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
    
    <div v-if="connectionStatus" class="connection-status">
      <el-alert 
        :title="connectionStatus.message"
        :type="connectionStatus.type"
        :closable="false"
      />
    </div>
  </div>
</template>

<script>
export default {
  name: 'SSHConnectionConfig',
  emits: ['connection-ready'],
  data() {
    return {
      form: {
        host: '',
        port: 22,
        username: '',
        authType: 'password',
        password: '',
        privateKey: ''
      },
      rules: {
        host: [
          { required: true, message: '请输入服务器地址', trigger: 'blur' }
        ],
        port: [
          { required: true, message: '请输入端口号', trigger: 'blur' },
          { type: 'number', min: 1, max: 65535, message: '端口号范围1-65535', trigger: 'blur' }
        ],
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ]
      },
      testing: false,
      connectionStatus: null,
      connectionValid: false
    }
  },
  methods: {
    async testConnection() {
      try {
        await this.$refs.formRef.validate()
        this.testing = true
        this.connectionStatus = null
        
        // 模拟连接测试
        setTimeout(() => {
          this.testing = false
          this.connectionStatus = {
            type: 'success',
            message: '连接测试成功！'
          }
          this.connectionValid = true
        }, 2000)
        
      } catch (error) {
        this.testing = false
        this.connectionStatus = {
          type: 'error',
          message: '连接测试失败，请检查配置信息'
        }
        this.connectionValid = false
      }
    },
    
    saveConnection() {
      if (!this.connectionValid) {
        this.$message.warning('请先测试连接')
        return
      }
      
      // 保存连接配置到本地存储
      const connectionConfig = { ...this.form }
      localStorage.setItem('ssh_connection_config', JSON.stringify(connectionConfig))
      
      // 触发连接就绪事件
      this.$emit('connection-ready', connectionConfig)
      
      this.$message.success('连接配置已保存')
    },
    
    resetForm() {
      this.$refs.formRef.resetFields()
      this.connectionStatus = null
      this.connectionValid = false
    },
    
    loadSavedConnection() {
      const saved = localStorage.getItem('ssh_connection_config')
      if (saved) {
        try {
          this.form = { ...this.form, ...JSON.parse(saved) }
        } catch (error) {
          console.error('加载保存的连接配置失败:', error)
        }
      }
    }
  },
  
  mounted() {
    this.loadSavedConnection()
  }
}
</script>

<style scoped>
.ssh-connection-config {
  padding: 20px;
}

.connection-status {
  margin-top: 20px;
}

.el-form-item {
  margin-bottom: 20px;
}
</style>