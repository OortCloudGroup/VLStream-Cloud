## 目录

1. [前端多项目集合](#前端多项目集合)
1. [目录结构](#目录结构)
1. [快速上手](#快速上手)
1. [开发](#开发)
1. [发布](#发布)
1. [新建项目](#新建项目)
1. [commit 规范](#commit规范)
1. [eslint 规范](#eslint规范)
1. [图片资源命名规范](#图片资源命名规范)
1. [其他](#其他)

# 前端多项目集合

code 为多个单页面 vue 项目的合集，利用.env 文件配置启动具体的项目以及
package.json 的 script 编译项目。

## 目录结构

```js
├── dist                                 # 执行build后生成的文件夹
├── pubilc                               # 移动端H5项目的模板文件夹
├── src
│   ├── api                              # 各个项目网络请求api
│   ├── assets                           # 资源文件夹
│   │   ├── @font-face                   # 字体
│   │   ├── img                          # 各个项目的图片文件夹，文件以项目区分；包含公共的img
│   │   ├── css                          # 公共的css
│   │   ├── styles                       # 公共的styles，主要配置一些公共的css变量
│   │   ├── theme                        # 公共的主题文件夹，尚未用到
│   ├── components                       # 公共的组件,用于存放封装的一些公共组件（开发项目中，需谨慎修改，防止修改其他项目出问题）
│   ├── config                           # 各个项目的配置
│   │   ├── AppConfig.js                 # 移动端H5项目的配置，appid 密钥等
│   │   ├── index.js                     # 环境以及网络ip的配置
│   │   ├── UIConfig.js                  # UI显示以及logo等的配置
│   ├── mock                             # mock网络请求的文件夹，目前尚未用到
│   ├── pages                            # 各个项目的代码文件夹（这里展开两个 一个pc（管理员平台），一个移动端（奥陌陌首页H5）其他的类似）
移动端项目目录示例
│   │   ├── aomomo_home                  # pc 端项目文件夹展开示例
│   │   │   ├── index
│   │   │   │   ├── components           # 项目公用的组件文件夹
│   │   │   │   ├── page                 # 路由页文件夹
│   │   │   │   ├── router               # 项目路由配置文件夹
│   │   │   │   ├── index.js             # 项目入口
│   │   │   │   ├── index.vue            # 组件入口
│   │   │   ├── store                    # vuex
│   │   │   │   ├── modules              # vuex 模块文件
│   │   │   │   ├── index.js             # vuex 配置入口
│   │   │   │   ├── getters.js           # vuex getter文件
│   │   │   ├── App.css                  # 项目公用的css（当样式与全局的样式冲突时，需要修改的css 应放在这里）

│   ├── route                            # 项目公共路由文件，如h5应用的关于页面等
│   ├── util                             # 项目工具文件，如http.js, bus.js 等文件
├── tempLib                              # 临时文件夹
├── .env                                 # 开发 env 的配置文件，可 切换项目；注意；每次切换都需要重新编译运行
├── .env.production                      # build 模式下，各个项目的配置文件夹

├── .eslintignore                        # eslint 忽略文件配置
├── .eslintrc.js                         # eslint 文件配置
├── .gitignore.js                        # git 忽略文件配置
├── babel.config.js                      # babel 文件配置
├── commitlint.config.js                 # git commit 规范文件配置
├── package.json
├── vite.config.js                        # vue 3.0 配置

```

## 快速上手

执行以下命令可以快速运行项目

1. yarn install
2. yarn start
3. 访问 http://localhost:8080

## 开发

1. 在 .env 文件中找到需要开发的项目，打开注释，注意只能有一个打开的注释，若有多个，则以最后一个为准
2. yarn start
3. 访问 http://localhost:8080
4. 在 src/pages 下找到对应的项目开发

## 发布

#### 单个项目编译

- pnpm run build: 相应的项目文件夹名称 或者运行 webstorm 的配置
- 生成的文件夹 为 dist

## 新建项目

    注意 ： 项目名称保持一致

1. src 下创建项目名称的文件夹（最后复制原有的项目后做删除操作，可最大避免一些乱七八糟的问题）
2. src/config/pageConfig 下创建项目名称的文件 （最好复制原有的项目后做删除操作，可最大避免一些乱七八糟的问题）
3. .env 配置好项目名称的启动
4. package.json 文件新增 build 配置

## commit 规范

项目使用了 commitlint 来规范提交 log
以下说明 commit 的类别，只允许使用下面 7 个标识，提交时类别不能为空，且提交内容不能为空

- upd：更新某功能（不是 feat, 不是 fix）
- feat：新功能（feature）
- fix：修补 bug
- docs：文档（documentation）
- style： 格式（不影响代码运行的变动）
- refactor：重构（即不是新增功能，也不是修改 bug 的代码变动）
- revert：回滚
- test：增加测试
- chore：构建过程或辅助工具的变动

注意： 类型后台冒号空格 例: fix: 修复 bug

## eslint 规范

- webstorm 建议安装 eslint 插件，每次编辑完文件后，eslint fix 修复或者养成良好的编码习惯
  具体规范： 自行查阅配置文件或者阅读 [Airbnb JavaScript 风格指南](http://192.168.88.110:32433/oortfrontgroup/oort_h5/blob/master/REFRENCE.md)

常见规范也是本项目基本规范：

- v-for :key 必须

```vue
<service-items v-for="(item, index) in tableData" :key="index" :data="item" />
```

- 等于号 在确定值得类型情况下，尽量使用 三等号 ===

- 未用到得变量声明 要注释或者去除 包括 import ,变量， 组件

- 声明数组用 [] ,对象 {}

- 结尾逗号要去除

## 图片资源命名规范

1. 不用出现中文名称
2. 不要出现命名空格

## 其他

### 出错了怎么办

1. 看报错信息，分析报错信息，deepseek 搞一下

## 移动端 app index.vue

使用了多个 mixins ，为了提供通用的代码
devLogin pc 调试登录界面
routeAnimate 动画路由相关
backButton 移动端返回键监听的
getTokenAndGateway 获取 token 和网关

## NavHeaderBanner 移动端 banner 统一组件

[NavHeaderBanner](./src/components/navHeaderBanner/)

## 评论组件

[NavHeaderBanner](/src/components/comment_system/)

## oort-popup 组件

[oort-popup](/src/components/popup/)

## oort-field 组件

[oort-field](/src/components/field/)

## oort-image-preivew 组件

[oort-image-preivew](/src/components/imagePreview/)
注意：image 组件分为函数式和组件式

## oort-uploader 组件

[oort-uploader](/src/components/uploader/)

## 通讯录组件 一般结合 oort-popup 使用

[Contact](/src/components/contactTree/)
