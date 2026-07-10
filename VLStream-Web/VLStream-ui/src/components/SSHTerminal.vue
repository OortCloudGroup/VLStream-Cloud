<template>
  <div class="ssh-terminal">
    <div class="terminal-header">
      <div class="connection-info">
        <span class="status-indicator" :class="{ connected: isConnected }"></span>
        <span>{{ connectionInfo }}</span>
      </div>
      <div class="terminal-actions">
        <el-button size="small" @click="clearTerminal">清空</el-button>
        <el-button size="small" @click="disconnect" v-if="isConnected">断开连接</el-button>
      </div>
    </div>
    <div class="terminal-container" ref="terminalContainer">
      <div class="terminal-output" ref="terminalOutput">
        <div v-for="(line, index) in outputLines" :key="index" class="output-line">
          {{ line }}
        </div>
      </div>
      <div class="terminal-input" v-if="isConnected">
        <span class="prompt">{{ prompt }}</span>
        <input 
          ref="commandInput"
          v-model="currentCommand"
          @keyup.enter="executeCommand"
          @keyup.up="previousCommand"
          @keyup.down="nextCommand"
          class="command-input"
          placeholder="输入命令..."
        />
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SSHTerminal',
  props: {
    connection: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      isConnected: false,
      outputLines: [],
      currentCommand: '',
      commandHistory: [],
      historyIndex: -1,
      prompt: '$',
      ws: null
    }
  },
  computed: {
    connectionInfo() {
      if (this.isConnected && this.connection.host) {
        return `${this.connection.username}@${this.connection.host}:${this.connection.port || 22}`
      }
      return '未连接'
    }
  },
  mounted() {
    this.initializeTerminal()
  },
  beforeUnmount() {
    this.disconnect()
  },
  methods: {
    initializeTerminal() {
      this.addOutputLine('SSH终端已初始化')
      this.addOutputLine('使用连接配置建立SSH连接...')
    },
    
    connect() {
      if (!this.connection.host) {
        this.addOutputLine('错误: 缺少连接配置')
        return
      }
      
      this.addOutputLine(`正在连接到 ${this.connection.host}:${this.connection.port || 22}...`)
      
      // 模拟连接过程
      setTimeout(() => {
        this.isConnected = true
        this.addOutputLine(`已连接到 ${this.connection.host}`)
        this.addOutputLine('欢迎使用SSH终端')
        this.prompt = `${this.connection.username}@${this.connection.host}:~$`
        this.$nextTick(() => {
          this.$refs.commandInput?.focus()
        })
      }, 1000)
    },
    
    disconnect() {
      if (this.ws) {
        this.ws.close()
        this.ws = null
      }
      this.isConnected = false
      this.addOutputLine('连接已断开')
      this.prompt = '$'
    },
    
    executeCommand() {
      if (!this.currentCommand.trim()) return
      
      const command = this.currentCommand.trim()
      this.addOutputLine(`${this.prompt} ${command}`)
      
      // 添加到命令历史
      this.commandHistory.unshift(command)
      if (this.commandHistory.length > 100) {
        this.commandHistory.pop()
      }
      this.historyIndex = -1
      
      // 处理特殊命令
      this.handleCommand(command)
      
      this.currentCommand = ''
    },
    
    handleCommand(command) {
      // 模拟命令执行
      if (command === 'clear') {
        this.clearTerminal()
        return
      }
      
      if (command === 'exit') {
        this.disconnect()
        return
      }
      
      if (command.startsWith('cd ')) {
        this.addOutputLine(`切换目录: ${command.substring(3)}`)
        return
      }
      
      if (command === 'ls' || command === 'ls -la') {
        this.addOutputLine('total 8')
        this.addOutputLine('drwxr-xr-x  2 user user 4096 Aug 29 14:00 .')
        this.addOutputLine('drwxr-xr-x  3 user user 4096 Aug 29 13:00 ..')
        this.addOutputLine('-rw-r--r--  1 user user  220 Aug 29 13:00 .bashrc')
        return
      }
      
      if (command === 'pwd') {
        this.addOutputLine('/home/user')
        return
      }
      
      // 默认处理
      this.addOutputLine(`执行命令: ${command}`)
      this.addOutputLine('命令执行完成')
    },
    
    addOutputLine(line) {
      this.outputLines.push(line)
      this.$nextTick(() => {
        this.scrollToBottom()
      })
    },
    
    clearTerminal() {
      this.outputLines = []
    },
    
    previousCommand() {
      if (this.historyIndex < this.commandHistory.length - 1) {
        this.historyIndex++
        this.currentCommand = this.commandHistory[this.historyIndex] || ''
      }
    },
    
    nextCommand() {
      if (this.historyIndex > 0) {
        this.historyIndex--
        this.currentCommand = this.commandHistory[this.historyIndex] || ''
      } else if (this.historyIndex === 0) {
        this.historyIndex = -1
        this.currentCommand = ''
      }
    },
    
    scrollToBottom() {
      const output = this.$refs.terminalOutput
      if (output) {
        output.scrollTop = output.scrollHeight
      }
    }
  },
  watch: {
    connection: {
      handler(newConnection) {
        if (newConnection && newConnection.host && !this.isConnected) {
          this.connect()
        }
      },
      deep: true,
      immediate: true
    }
  }
}
</script>

<style scoped>
.ssh-terminal {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #1e1e1e;
  color: #ffffff;
  font-family: 'Courier New', monospace;
}

.terminal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #2d2d2d;
  border-bottom: 1px solid #404040;
}

.connection-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff4444;
}

.status-indicator.connected {
  background: #44ff44;
}

.terminal-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.terminal-output {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.4;
}

.output-line {
  margin-bottom: 2px;
  word-wrap: break-word;
}

.terminal-input {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: #2d2d2d;
  border-top: 1px solid #404040;
}

.prompt {
  color: #44ff44;
  margin-right: 8px;
  font-weight: bold;
}

.command-input {
  flex: 1;
  background: transparent;
  border: none;
  color: #ffffff;
  font-family: inherit;
  font-size: 14px;
  outline: none;
}

.command-input::placeholder {
  color: #888888;
}

.terminal-actions .el-button {
  background: #404040;
  border-color: #404040;
  color: #ffffff;
}

.terminal-actions .el-button:hover {
  background: #505050;
  border-color: #505050;
}
</style>