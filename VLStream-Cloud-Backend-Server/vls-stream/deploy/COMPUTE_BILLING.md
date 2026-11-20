# 私有算力计费配置

算力计费能力不依赖额外的 enable 开关，但付费供应商接入必须具备经过授权的商户和供应商配置。没有这些私有配置的构建不能视为已配置好付费功能。

## 私有构建

Maven 的 process-resources 会把构建机上的 ruoyi-admin/config/application.yml 复制到 target/classes/private/compute-billing/application.yml，最终进入 JAR 的 BOOT-INF/classes/private/compute-billing/application.yml。该文件包含 vlstream.compute.external-billing 和 vlstream.compute.autodl-token 配置，不要提交到仓库，也不要放入公开构建上下文。

私有构建得到的 JAR 和镜像会包含凭据，只能进入授权的私有分发渠道。切换计费环境前使用对应的私有文件并清理构建目录，避免旧产物残留。发布前仅比较文件摘要，不打印或上传密钥内容。

## 公开 GitHub Release 镜像

公开 GitHub Actions 构建会拒绝存在 ruoyi-admin/config/application.yml 的工作区，公开 Docker 构建上下文也会排除该文件。公开 JAR 和镜像不会内置商户或供应商凭据，因此不能仅凭镜像已发布就认定付费接入已经配置。

如需让公开镜像使用付费接入，请通过受控的只读运行时配置提供该文件。应用默认导入路径是 /app/ruoyi-admin/config/application.yml；可用仅本机保存的 Compose 覆盖文件挂载：

services:
  backend:
    volumes:
      - /secure/path/compute-billing.yml:/app/ruoyi-admin/config/application.yml:ro

请将宿主机路径替换为权限受限的实际路径。不要把覆盖文件或私有 YAML 放入公开部署包、镜像、日志或代码仓库。也可以使用等效的受保护 Spring 配置来源，并确认环境变量或命令行参数没有覆盖掉凭据。

自动开机、供应商网络、租户授权和付费能力还受各自运行配置约束；本说明不表示这些能力已在目标环境验收。
