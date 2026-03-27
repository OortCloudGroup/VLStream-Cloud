#!/usr/bin/env sh

# 将文件夹移动到部署项目front的html相对应的文件夹下，实现git自动部署

# 确保脚本抛出遇到的错误
set -e

# 获取传入的参数（项目名称）
echo $1

PROJECTNAME=$1

# 替换.env.production的第一行的=后面 为传入的参数，兼容mac系统
if [[ "$(uname)" == "Darwin" ]]; then
  sed -i '' "1s/^.*$/NODE_BUILDTARGET=$1/" .env.production
else
  sed -i "1s/^.*$/NODE_BUILDTARGET=$1/" .env.production
fi

echo $PROJECT_DIR

npm run build
