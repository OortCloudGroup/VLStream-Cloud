// Match UserConstants; do not silently change the deployed password policy.
export function localPasswordRules(getPassword, text) {
  return {
    password: [
      { required: true, message: text('请输入密码'), trigger: 'blur' },
      { min: 5, max: 20, message: text('密码长度必须为 5 至 20 个字符'), trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, message: text('请再次输入密码'), trigger: 'blur' },
      { validator: (_rule, value, callback) => callback(value === getPassword()
        ? undefined : new Error(text('两次输入密码不一致'))), trigger: 'blur' }
    ]
  }
}
