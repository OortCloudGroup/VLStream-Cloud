import { currentLocale, translatePhrase } from '@/i18n'
import { platformHelpSources, platformHelpEnglish } from '@/i18n/gbHelpPhrases'

const platformMessage = (key, params = {}) => {
  const message = currentLocale.value === 'zh-CN' ? platformHelpSources[key] : platformHelpEnglish[key]
  return message.replace(/\{(\w+)\}/g, (placeholder, name) => params[name] ?? placeholder)
}

const platformFieldDescriptions = {
  name: 'nameDescription', serverGBId: 'idDescription', serverGBDomain: 'domainDescription',
  serverIp: 'ipDescription', serverPort: 'portDescription', deviceGBId: 'localIdDescription',
  deviceIp: 'localIpDescription', devicePort: 'localPortDescription', username: 'usernameDescription',
  password: 'passwordDescription', expires: 'expiresDescription', keepTimeout: 'heartbeatDescription',
  sendStreamIp: 'mediaDescription', transport: 'transportDescription', characterSet: 'charsetDescription'
}

const platformFieldSources = [
  { key: 'name' },
  { key: 'serverGBId', example: '34020000002000000001' },
  { key: 'serverGBDomain', example: '3402000000' },
  { key: 'serverIp', example: '192.0.2.20' },
  { key: 'serverPort', example: '5060' },
  { key: 'deviceGBId', example: '34020000002000000002' },
  { key: 'deviceIp', example: '192.0.2.10' },
  { key: 'devicePort', example: '8116' },
  { key: 'username' },
  { key: 'password' },
  { key: 'expires', example: '3600' },
  { key: 'keepTimeout', example: '60' },
  { key: 'sendStreamIp', example: '192.0.2.10' },
  { key: 'transport', example: 'UDP' },
  { key: 'characterSet', example: 'GB2312' }
]
export const platformFields = platformFieldSources.map(field => ({
  key: field.key,
  get label() { return platformMessage(field.key) },
  get description() { return platformMessage(platformFieldDescriptions[field.key]) },
  get example() {
    if (field.key === 'name') return platformMessage('exampleName')
    if (field.key === 'username' || field.key === 'password') return platformMessage('assigned')
    return field.example
  }
}))

// Read these getters during render so an already-open dialog updates immediately on language changes.
export const platformExample = Object.defineProperties({}, Object.fromEntries(
  platformFields.map(field => [field.key, {
    enumerable: true,
    get() { return platformMessage('example', { value: field.example }) }
  }])
))
export const serviceFields = [
  { get label() { return translatePhrase("编号") }, example: '34020000002000000002', get description() { return translatePhrase("填写到设备的 SIP 服务器编号；设备自身的国标编号需另外配置。") } },
  { get label() { return translatePhrase("域") }, example: '3402000000', get description() { return translatePhrase("填写到设备的 SIP 服务器域，使用本弹窗实际显示值。") } },
  { label: 'IP', example: '192.0.2.10', get description() { return translatePhrase("填写设备能访问的本平台地址；多网卡只选择正确的一个 IP。") } },
  { get label() { return translatePhrase("端口") }, example: '8116 / UDP', get description() { return translatePhrase("填写实际 SIP 服务端口与传输方式，并检查防火墙和端口映射。") } },
  { get label() { return translatePhrase("密码") }, example: '使用本平台配置的 SIP 密码', get description() { return translatePhrase("填入设备的国标注册认证密码字段，不是 Web 登录密码。") } }
]
