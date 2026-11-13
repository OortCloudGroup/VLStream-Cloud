# 镜像内置计费配置

算力计费默认启用，不依赖额外 enable 开关。

Maven 的 `process-resources` 默认把构建机上的 `ruoyi-admin/config/application.yml` 复制到 `target/classes/private/compute-billing/application.yml`，Spring Boot 打包后位于 `BOOT-INF/classes/private/compute-billing/application.yml`。默认配置通过 classpath import 加载该文件，容器无需另挂配置即可使用内置的商户和供应商凭据。

Git 忽略私有配置不等于 Maven 忽略它。本机该文件包含 `vlstream.compute.external-billing` 以及 `vlstream.compute.autodl-token`。文件不要提交仓库；发布得到的 JAR 和镜像会包含凭据，应只进入指定私有发布渠道。

在新构建机或 CI 上，先通过私有文件变量等方式放置同一路径，再执行干净构建。只从 Git 拉取代码不会取得凭据；没有此文件的构建仍可用于免费功能，不能当作已配好付费功能的正式包。切换计费环境前使用对应的私有文件并清理构建目录，避免旧产物残留。

发布前核对 JAR 中存在上述资源，且内容与本次私有源文件一致（仅比较摘要，不打印密钥）。更新镜像后也须核对实际 Pod 已使用新镜像。平台外部环境变量、配置文件或命令行参数可能覆盖内置默认值，尤其应检查是否存在显式空凭据。

本次不新增 Kubernetes Secret 或挂载要求，不自动执行付费开机。
