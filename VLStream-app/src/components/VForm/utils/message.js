import { showNotify, setNotifyDefaultOptions, showConfirmDialog } from 'vant'
export const installMessage = function(app) {
  setNotifyDefaultOptions({
    'z-index': 6000
  })
  const message = function(options) {
    options.type = options.type.replace('info', 'primary').replace('error', 'warning')
    showNotify(options)
  };

  ['success', 'warning', 'primary', 'info', 'error'].forEach(type => {
    message[type] = options => {
      if (typeof options === 'string') {
        options = {
          message: options
        }
      }
      options.type = type.replace('info', 'primary').replace('error', 'warning')
      return message(options)
    }
  })

  app.config.globalProperties.$message = message

  app.config.globalProperties.$showConfirmDialog = showConfirmDialog
}
