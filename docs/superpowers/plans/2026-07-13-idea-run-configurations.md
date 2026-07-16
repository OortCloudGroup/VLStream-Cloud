# IDEA 前后端启动配置 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 IDEA 对当前 Maven 工程的识别，并提供前端、后端及前后端一键启动配置。

**Architecture:** 根 `.idea` 只负责导入当前后端 Maven 根工程；根 `.run` 保存三个可共享运行配置。一个 PowerShell 回归脚本从磁盘读取 XML 和项目入口，验证所有引用形成完整闭环。

**Tech Stack:** IntelliJ IDEA Run Configuration XML、Maven、Spring Boot 2.7、npm/Vite、PowerShell 7

---

### Task 1: 建立配置回归测试

**Files:**
- Create: `codex/Test-IdeaRunConfigurations.ps1`
- Test: `codex/Test-IdeaRunConfigurations.ps1`

- [ ] **Step 1: 编写失败测试**

创建脚本，核心结构如下；脚本解析 `.idea/misc.xml`、`.idea/compiler.xml` 和根 `.run/*.run.xml`，检查 Maven 路径、JDK 8、后端主类、前端 npm 脚本及 Compound 引用：

```powershell
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$script:failureCount = 0

function Assert-Condition {
    param([bool]$Condition, [string]$Message)
    if ($Condition) { Write-Output "PASS: $Message"; return }
    Write-Output "FAIL: $Message"
    $script:failureCount++
}
# 方法说明：统一记录配置断言结果，并通过 failureCount 汇总退出码。

$expectedPom = '$PROJECT_DIR$/VLStream-Cloud-Backend-Server/vls-stream/pom.xml'
$miscPath = Join-Path $projectRoot '.idea/misc.xml'
[xml]$misc = Get-Content -Raw $miscPath
$mavenPath = $misc.project.component |
    Where-Object name -eq 'MavenProjectsManager' |
    ForEach-Object { $_.option.list.option.value }
Assert-Condition ($mavenPath -eq $expectedPom) 'Maven imports vls-stream/pom.xml'
Assert-Condition (-not ($mavenPath -match 'vls-server')) 'Maven has no deleted vls-server reference'
Assert-Condition (Test-Path (Join-Path $projectRoot 'VLStream-Cloud-Backend-Server/vls-stream/pom.xml')) 'Maven pom exists'

$jdk = $misc.project.component | Where-Object name -eq 'ProjectRootManager'
Assert-Condition ($jdk.'project-jdk-name' -eq '1.8') 'IDEA project SDK is JDK 8'
Assert-Condition ($jdk.languageLevel -eq 'JDK_1_8') 'IDEA language level is Java 8'

$compilerText = Get-Content -Raw (Join-Path $projectRoot '.idea/compiler.xml')
Assert-Condition (-not ($compilerText -match 'module name="vls-server"')) 'Compiler has no deleted module override'

$backendPath = Join-Path $projectRoot '.run/VLStream Backend.run.xml'
$frontendPath = Join-Path $projectRoot '.run/VLStream Frontend.run.xml'
$fullStackPath = Join-Path $projectRoot '.run/VLStream Full Stack.run.xml'
Assert-Condition (Test-Path $backendPath) 'Backend run configuration exists'
Assert-Condition (Test-Path $frontendPath) 'Frontend run configuration exists'
Assert-Condition (Test-Path $fullStackPath) 'Full-stack run configuration exists'

if (Test-Path $backendPath) {
    [xml]$backend = Get-Content -Raw $backendPath
    $config = $backend.component.configuration
    Assert-Condition ($config.type -eq 'SpringBootApplicationConfigurationType') 'Backend uses Spring Boot configuration'
    Assert-Condition ($config.module.name -eq 'ruoyi-admin') 'Backend uses ruoyi-admin module'
    Assert-Condition (($config.option | Where-Object name -eq 'SPRING_BOOT_MAIN_CLASS').value -eq 'com.ruoyi.RuoYiApplication') 'Backend main class is correct'
    Assert-Condition (($config.option | Where-Object name -eq 'WORKING_DIRECTORY').value -eq '$PROJECT_DIR$/VLStream-Cloud-Backend-Server/vls-stream') 'Backend working directory is correct'
}

if (Test-Path $frontendPath) {
    [xml]$frontend = Get-Content -Raw $frontendPath
    $config = $frontend.component.configuration
    Assert-Condition ($config.type -eq 'js.build_tools.npm') 'Frontend uses npm configuration'
    Assert-Condition ($config.'package-json'.value -eq '$PROJECT_DIR$/VLStream-Web/VLStream-ui/package.json') 'Frontend package.json is correct'
    Assert-Condition ($config.command.value -eq 'run') 'Frontend npm command is run'
    Assert-Condition ($config.scripts.script.value -eq 'dev') 'Frontend npm script is dev'
}

if (Test-Path $fullStackPath) {
    [xml]$fullStack = Get-Content -Raw $fullStackPath
    $targets = $fullStack.component.configuration.toRun
    Assert-Condition (($targets | Where-Object name -eq 'VLStream Backend').type -eq 'SpringBootApplicationConfigurationType') 'Compound references backend'
    Assert-Condition (($targets | Where-Object name -eq 'VLStream Frontend').type -eq 'js.build_tools.npm') 'Compound references frontend'
}

$package = Get-Content -Raw (Join-Path $projectRoot 'VLStream-Web/VLStream-ui/package.json') | ConvertFrom-Json
Assert-Condition ($package.scripts.dev -eq 'vite') 'package.json exposes vite dev script'
Assert-Condition (Test-Path (Join-Path $projectRoot 'VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/java/com/ruoyi/RuoYiApplication.java')) 'Backend main class source exists'

if ($script:failureCount -gt 0) { exit 1 }
exit 0
```

- [ ] **Step 2: 运行测试并确认因旧 Maven 路径和缺少根运行配置而失败**

Run:

```powershell
rtk pwsh -NoProfile -File codex/Test-IdeaRunConfigurations.ps1
```

Expected: `FAIL`，至少报告旧 `vls-server/pom.xml` 引用或缺少 `.run` 配置。

### Task 2: 修正 IDEA Maven 导入

**Files:**
- Modify: `.idea/misc.xml`
- Modify: `.idea/compiler.xml`
- Test: `codex/Test-IdeaRunConfigurations.ps1`

- [ ] **Step 1: 将 Maven 根工程改为当前路径**

把 `originalFiles` 中的值改为：

```xml
<option value="$PROJECT_DIR$/VLStream-Cloud-Backend-Server/vls-stream/pom.xml" />
```

- [ ] **Step 2: 清除已删除模块的编译器覆盖项**

保留默认注解处理配置，删除 `.idea/compiler.xml` 中仅针对旧 `vls-server` 模块的 profile 和 `JavacSettings`。

- [ ] **Step 3: 运行回归测试并确认 Maven/JDK 检查通过，运行配置检查仍失败**

Run:

```powershell
rtk pwsh -NoProfile -File codex/Test-IdeaRunConfigurations.ps1
```

Expected: Maven 路径与 JDK 检查为 `PASS`；缺少根 `.run` 配置仍导致总结果 `FAIL`。

### Task 3: 添加三个共享运行配置

**Files:**
- Create: `.run/VLStream Backend.run.xml`
- Create: `.run/VLStream Frontend.run.xml`
- Create: `.run/VLStream Full Stack.run.xml`
- Test: `codex/Test-IdeaRunConfigurations.ps1`

- [ ] **Step 1: 创建 Spring Boot 后端配置**

配置类型为 `SpringBootApplicationConfigurationType`，模块为 `ruoyi-admin`，主类为 `com.ruoyi.RuoYiApplication`，工作目录为 `$PROJECT_DIR$/VLStream-Cloud-Backend-Server/vls-stream`，启动前执行 Make。

```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="VLStream Backend" type="SpringBootApplicationConfigurationType" factoryName="Spring Boot">
    <module name="ruoyi-admin" />
    <option name="SPRING_BOOT_MAIN_CLASS" value="com.ruoyi.RuoYiApplication" />
    <option name="WORKING_DIRECTORY" value="$PROJECT_DIR$/VLStream-Cloud-Backend-Server/vls-stream" />
    <method v="2">
      <option name="Make" enabled="true" />
    </method>
  </configuration>
</component>
```

- [ ] **Step 2: 创建 npm 前端配置**

配置类型为 `js.build_tools.npm`，引用 `$PROJECT_DIR$/VLStream-Web/VLStream-ui/package.json`，执行 `run dev`，Node 解释器使用项目默认值。

```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="VLStream Frontend" type="js.build_tools.npm">
    <package-json value="$PROJECT_DIR$/VLStream-Web/VLStream-ui/package.json" />
    <command value="run" />
    <scripts>
      <script value="dev" />
    </scripts>
    <node-interpreter value="project" />
    <envs />
    <method v="2" />
  </configuration>
</component>
```

- [ ] **Step 3: 创建 Compound 一键启动配置**

配置类型为 `CompoundRunConfigurationType`，分别引用 `VLStream Backend` 和 `VLStream Frontend`，不复制子配置参数。

```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="VLStream Full Stack" type="CompoundRunConfigurationType">
    <toRun name="VLStream Backend" type="SpringBootApplicationConfigurationType" />
    <toRun name="VLStream Frontend" type="js.build_tools.npm" />
    <method v="2" />
  </configuration>
</component>
```

- [ ] **Step 4: 运行回归测试并确认全部通过**

Run:

```powershell
rtk pwsh -NoProfile -File codex/Test-IdeaRunConfigurations.ps1
```

Expected: 所有断言为 `PASS`，退出码为 0。

### Task 4: 验证项目入口可由配置依赖的工具启动

**Files:**
- Test output: `codex/idea-run-verification/`

- [ ] **Step 1: 根据根 pom.xml 选择 JDK 8**

读取 `<java.version>1.8</java.version>` 后，将 `JAVA_HOME` 临时设为 `C:\Users\oort\.jdks\corretto-1.8.0_442`，并确认 `java -version` 和 `mvn -version` 都使用 JDK 8。

- [ ] **Step 2: 验证前端 Vite 入口**

在 `VLStream-Web/VLStream-ui` 执行 `npm run dev -- --host 127.0.0.1`，将输出保存到 `codex/idea-run-verification`；检测到 Vite ready 或监听地址后终止测试进程。

- [ ] **Step 3: 验证后端 Maven/主类入口**

在 `VLStream-Cloud-Backend-Server/vls-stream` 执行 `mvn -ntp -pl ruoyi-admin -am -DskipTests compile`。该命令只验证 IDEA 后端配置所依赖的 Maven 模块、主类和编译链；若失败，记录并区分依赖仓库/源码编译错误，不将其误报为运行配置错误。

- [ ] **Step 4: 最终检查**

运行配置回归脚本、`git diff --check` 和 XML 解析检查，确认没有修改业务代码或无关用户文件。
