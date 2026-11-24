# VLStream 通用文件上传接口对接说明

## 接入地址与权限

`VLS_API_BASE` 是 VLStream 后端的对外 API 基地址，包含实际网关前缀。下文以 `https://vls.example.com/bus/apaas-vls-server` 为例。

| 操作 | 方法和路径 | 认证 |
| --- | --- | --- |
| 上传文件 | `POST /system/oss/upload` | `Authorization: Bearer <VLS_TOKEN>` |
| 下载文件 | `GET /system/oss/public/{fileId}` | 持有链接即可访问 |
| 检查文件可用性 | `HEAD /system/oss/public/{fileId}` | 持有链接即可访问 |

### 单租户和多租户接入

| 模式 | 上传使用的令牌 | 文件登记归属 |
| --- | --- | --- |
| 单租户 | 使用当前 VLS 实例本地登录后获得的 VLS Token，也可使用该实例签发的平台换票会话 | 服务端配置的固定租户，默认 `000000` |
| 多租户 | 通过 `POST /sso/v1/exchangeToken` 用平台授权换取目标租户的 VLS Token。切换租户后使用新会话的 Token | 当前 VLS 会话绑定的租户 |

两种模式使用相同的上传字段和响应，上传时无需提交租户字段。多租户环境若已有客户端附带 `tenantId` 请求头，其值必须与当前会话一致。文件上传成功后立即公开，下载和检查链接无需登录或租户信息。

## 1. 上传文件

请求使用 `multipart/form-data`，字段名为 `file`。每次提交一个非空文件，最大 **50 MiB（52,428,800 字节）**。使用 `curl -F` 或浏览器 `FormData` 时，客户端会自动生成带 boundary 的 `Content-Type`。

```bash
VLS_API_BASE='https://vls.example.com/bus/apaas-vls-server'
VLS_TOKEN='<登录后获得的 VLS Token>'

curl --include \
  --header "Authorization: Bearer ${VLS_TOKEN}" \
  --form 'file=@./report.pdf' \
  "${VLS_API_BASE}/system/oss/upload"
```

文件已写入 MinIO 且登记成功时，HTTP 状态为 `200`，响应体的 `code` 为 `200`。

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "fileId": "ec930076-e000-4dc0-a2bd-081d84ec8b2a",
    "fileName": "report.pdf",
    "size": 24876,
    "url": "https://vls.example.com/bus/apaas-vls-server/system/oss/public/ec930076-e000-4dc0-a2bd-081d84ec8b2a"
  }
}
```

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| `data.fileId` | string | 文件唯一 ID，可保存到业务记录 |
| `data.fileName` | string | 服务端清理后的文件名，支持中文 |
| `data.size` | integer | 文件大小，单位为字节 |
| `data.url` | string | 可直接分享的完整下载地址，无自动过期时间 |

业务侧以 **HTTP 成功且 `code === 200`** 为准，并直接使用返回的 `data.url`。接口每次成功上传会创建新的文件 ID，请求超时后重传可能产生重复文件。

## 2. 下载和检查

`PUBLIC_FILE_URL` 直接取上传结果中的 `data.url`。下载请求返回二进制内容，`Content-Type` 为 `application/octet-stream`，`Content-Disposition` 提供附件名。

```bash
PUBLIC_FILE_URL='<上传响应中的 data.url>'
curl --fail --show-error --output report.pdf "${PUBLIC_FILE_URL}"
curl --head "${PUBLIC_FILE_URL}"
```

`HEAD` 成功时可从 `Content-Length` 读取文件大小，响应没有文件内容。链接可长期使用，需保留原对外地址、文件记录、对应的 OSS 配置及存储对象。

## 3. 错误处理

| 操作 | HTTP 状态或响应体 | 调用方处理 |
| --- | --- | --- |
| 上传 | HTTP `200`，`code: 401`，或 HTTP `401` | VLS 会话无效或过期，重新登录 |
| 上传 | HTTP `400` | 检查是否使用 `file` 字段提交了一个非空文件 |
| 上传 | HTTP `403` | 检查会话中的租户和身份。若传了租户请求头，值须与当前会话一致 |
| 上传 | HTTP `413` | 文件超出 50 MiB，缩小文件后重试 |
| 上传 | HTTP `200`，`code: 500` | 上传或登记失败，读取 `msg` 并按需重试 |
| 下载或检查 | HTTP `404` | 链接中的有效格式 ID 对应的文件记录或对象不存在 |
| 下载或检查 | HTTP `503` | 文件服务暂时不可用，稍后重试 |

网关可能先于应用拒绝请求，错误体格式以网关响应为准。

## 4. 部署与联调

1. 部署包含该接口的后端版本，确认 Flyway 已执行 `V1_2_0_081__public_file_upload.sql`，且当前默认 OSS 配置指向目标 MinIO。可用 `VLSTREAM_DEFAULT_OSS_CONFIG_KEY` 选择新上传使用的存储配置。
2. 确认网关转发上传、下载和检查路由，允许匿名访问公开下载和检查路由。网关请求体限额需覆盖 50 MiB 文件以及 multipart 表单开销。
3. 默认根据请求生成下载地址。若网关转发后生成的地址缺少外部域名或路径前缀，设置 `VLSTREAM_PUBLIC_FILES_BASE_URL=https://vls.example.com/bus/apaas-vls-server`。
4. 在单租户和多租户环境分别用有效会话上传一个文件，在无登录状态下打开各自返回的链接，核对文件名、大小和内容。再验证空文件、超限文件、未登录上传，以及多租户上传携带错误租户请求头的响应。
