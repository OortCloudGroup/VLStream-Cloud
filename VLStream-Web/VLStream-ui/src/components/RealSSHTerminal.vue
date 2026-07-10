<template>
  <div class="real-ssh-terminal">
    <div class="terminal-wrapper">
      <div id="terminal" ref="terminal"></div>
    </div>
  </div>
</template>

<script>
// 注意：这里需要安装 xterm 相关包
// npm install xterm xterm-addon-fit xterm-addon-web-links

export default {
  name: 'RealSSHTerminal',
  props: {
    connection: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      terminal: null,
      fitAddon: null,
      ws: null
    }
  },
  mounted() {
    this.initTerminal()
  },
  beforeUnmount() {
    this.cleanup()
  },
  methods: {
    async initTerminal() {
      try {
        // 动态导入 xterm（如果已安装）
        const { Terminal } = await import('xterm')
        const { FitAddon } = await import('xterm-addon-fit')
        const { WebLinksAddon } = await import('xterm-addon-web-links')
        
        this.terminal = new Terminal({
          cursorBlink: true,
          fontSize: 14,
          fontFamily: 'Courier New, monospace',
          theme: {
            background: '#1e1e1e',
            foreground: '#ffffff',
            cursor: '#ffffff',
            selection: '#ffffff40'
          }
        })
        
        this.fitAddon = new FitAddon()
        this.terminal.loadAddon(this.fitAddon)
        this.terminal.loadAddon(new WebLinksAddon())
        
        this.terminal.open(this.$refs.terminal)
        this.fitAddon.fit()
        
        // 监听终端输入
        this.terminal.onData(data => {
          if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({
              type: 'input',
              data: data
            }))
          }
        })
        
        // 连接WebSocket
        this.connectWebSocket()
        
      } catch (error) {
        console.error('初始化终端失败:', error)
        // 如果xterm未安装，显示简单的文本终端
        this.initSimpleTerminal()
      }
    },
    
    initSimpleTerminal() {
      this.$refs.terminal.innerHTML = `
        <div style="background: #1e1e1e; color: #ffffff; padding: 20px; font-family: monospace;">
          <div>SSH终端 (简化版)</div>
          <div>连接信息: ${this.connection.username}@${this.connection.host}:${this.connection.port}</div>
          <div>注意: 完整的SSH终端功能需要安装 xterm 依赖包</div>
          <div>运行: npm install xterm xterm-addon-fit xterm-addon-web-links</div>
        </div>
      `
    },
    
    connectWebSocket() {
      const wsUrl = `ws://localhost:8080/ssh-terminal`
      this.ws = new WebSocket(wsUrl)
      
      this.ws.onopen = () => {
        console.log('WebSocket连接已建立')
        // 发送连接信息
        this.ws.send(JSON.stringify({
          type: 'connect',
          connection: this.connection
        }))
      }
      
      this.ws.onmessage = (event) => {
        const message = JSON.parse(event.data)
        if (message.type === 'output' && this.terminal) {
          this.terminal.write(message.data)
        }
      }
      
      this.ws.onclose = () => {
        console.log('WebSocket连接已关闭')
      }
      
      this.ws.onerror = (error) => {
        console.error('WebSocket错误:', error)
      }
    },
    
    cleanup() {
      if (this.ws) {
        this.ws.close()
      }
      if (this.terminal) {
        this.terminal.dispose()
      }
    }
  },
  watch: {
    connection: {
      handler() {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
          this.ws.send(JSON.stringify({
            type: 'connect',
            connection: this.connection
          }))
        }
      },
      deep: true
    }
  }
}
</script>

<style scoped>
.real-ssh-terminal {
  height: 100%;
  width: 100%;
}

.terminal-wrapper {
  height: 100%;
  width: 100%;
}

#terminal {
  height: 100%;
  width: 100%;
}

/* xterm样式 */
:deep(.xterm) {
  height: 100% !important;
}

:deep(.xterm-viewport) {
  overflow-y: auto;
}
</style>