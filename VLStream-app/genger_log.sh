#!/bin/bash

# 美化输出颜色配置
BOLD=$(tput bold)
RESET=$(tput sgr0)
GREEN=$(tput setaf 2)
RED=$(tput setaf 1)
YELLOW=$(tput setaf 3)
BLUE=$(tput setaf 4)
CYAN=$(tput setaf 6)

# 调试模式开关（设为 true 启用详细输出）
DEBUG=true

LOG_FILE="vue3-h5"  # 日志文件名

log() {
  local level="$1"
  local message="$2"
  case "$level" in
    "success") echo "${BOLD}${GREEN}✅ ${message}${RESET}" ;;
    "error") echo "${BOLD}${RED}❌ ${message}${RESET}" >&2 ;;
    "warning") echo "${BOLD}${YELLOW}⚠️ ${message}${RESET}" ;;
    "info") echo "${BOLD}${BLUE}🔧 ${message}${RESET}" ;;
    "debug") $DEBUG && echo "${BOLD}${CYAN}🐞 ${message}${RESET}" ;;
  esac
}

check_dir() {
  if [ ! -d "$1" ]; then
    log error "目录不存在: $1"
    exit 1
  fi
}

gengner_log() {
  log info "开始生成提交日志文件"

  # 获取 Git 提交的日期列表
  local dates=$(git log --no-merges --since="2024-12-28 00:00:00" --date=short --pretty=format:"%ad" | sort -u -r)

  if [ -z "$dates" ]; then
    log warning "没有找到符合条件的提交记录"
    return 1
  fi

  # 初始化日志文件
  echo "## 更新日志" > $LOG_FILE || {
    log error "无法创建 $LOG_FILE 文件"
    exit 1
  }

  # 遍历日期列表
  dateLogsStr="处理日期:"
  for date in $dates; do
    # 包含merges信息，但排除以"Merge branch"开头的合并日志
    local gitlog=$(git log --pretty=format:"%s (%an)" --grep="^(\|upd\|feat\|fix\|style\|chore\|docs)" --since="$date 00:00:00" --until="$date 23:59:59" | grep -v "^Merge branch")
    dateLogsStr+="$date | "
    if [ -n "$gitlog" ]; then
      {
        echo -e "\n### $date 的更新日志\n"
        while read -r line; do
          echo "- $line"
        done <<< "$gitlog"
      } >> $LOG_FILE
    fi
  done
  log debug "$dateLogsStr"
  log success "日志文档 gitlog.md 生成成功"
  echo "${BOLD}━━━━━━━━━━━━━━━━━━━━━━${RESET}"
}

commitAndPush() {
  local DEPOLYROOT="G:/gitlab_deploy/OA-环境/oortcloud-policefront"  # 修正为合法路径格式
  
  # 首先判断当前路径 mac 和 window的路径不一致
  if [ ! -d "$DEPOLYROOT" ]; then
    # mkdir /data
    DEPOLYROOT="D:\CQL\serviceCode\oortcloud-policefront"
  fi
  
  # 最后判断是否在c: temp
  if [ ! -d "$DEPOLYROOT" ]; then
    DEPOLYROOT="C:\temp-deploy"
  fi

  # lanjian MAC path
  if [ ! -d "$DEPOLYROOT" ]; then
    DEPOLYROOT="/Users/lanjian/code/deploy/OA环境/oortcloud-policefront"
  fi

  # lwm MAC path
  if [ ! -d "$DEPOLYROOT" ]; then
    DEPOLYROOT="/Users/lwm/Desktop/oort/OA部署环境/oortcloud-policefront"
  fi

  # 如果还不存在就取默认的
  if [ ! -d "$DEPOLYROOT" ]; then
    if [ "$(uname)" == "Darwin" ]; then
      # 获取mac的用户目录名
      USERNAME=$(whoami)
      echo "当前是mac系统"
      DEPOLYROOT="/Users/$USERNAME/temp-deploy"
    else
      echo "当前是window系统"
      DEPOLYROOT="C:\temp-deploy"
    fi
  fi

  if [ ! -d "$DEPOLYROOT" ]; then
    echo "❌ 部署 文件夹不存在"
    exit 1
  fi

  log info "开始部署流程"
  check_dir "$DEPOLYROOT" 

  # 设置 git 的默认合并 为 false
  git config --local pull.merge  true 

  # 在部署目录下执行 git pull 命令 并判断是否成功
  log info "正在执行 git pull 命令..."
  if ! git -C "$DEPOLYROOT" pull -q; then
    log error "git pull 失败，请检查网络连接"
    exit 1
  fi
  log success "git pull 成功"

  # 复制日志文件
   if ! cp -v $LOG_FILE "$DEPOLYROOT/$LOG_FILE.md"; then
    log error "文件复制失败"
    exit 1
  fi

  # 删除本地日志文件
  if ! rm -v $LOG_FILE; then
    log warning "本地日志文件删除失败，继续执行..."
  fi

  # 进入部署目录
  if ! cd "$DEPOLYROOT"; then
    log error "无法进入部署目录: $DEPOLYROOT"
    exit 1
  fi

    # 到部署目录执行脚本
  # 执行 merge_md_files.sh 脚本
  log info "正在执行 merge_md_files.sh 脚本..."
  if [ -f "merge_md_files.sh" ]; then
    if ! sh merge_md_files.sh readme.md; then
      log error "执行 merge_md_files.sh 脚本失败"
      exit 1
    fi
    log success "merge_md_files.sh 脚本执行成功"
  else
    log warning "merge_md_files.sh 脚本不存在，跳过执行"
  fi

  # 生成提交信息
  local COMMIT_MSG="update service $(date +'%Y-%m-%d %H:%M:%S') 更新"
  log info "创建提交信息: ${COMMIT_MSG}"

  # 检查工作目录状态
  git add -A
  local status=$(git status --porcelain)

  if [ -z "$status" ]; then
    log warning "没有检测到文件变更，跳过提交。"
    if ! git push; then
      log error "推送失败"
      exit 1
    fi
    return 0
  fi

  # 提交变更
  if ! git commit -m "upd: $COMMIT_MSG"; then
    log error "提交失败"
    exit 1
  fi
  log success "提交成功"

  # 推送变更
  log info "正在推送更改..."
  if ! git push; then
    log error "推送失败"
    exit 1
  fi

  log success "推送成功"
  echo "${BOLD}${GREEN}🎉 部署流程完成！等待CI/CD处理...${RESET}"
  echo "${BOLD}━━━━━━━━━━━━━━━━━━━━━━${RESET}"
}

main() {
  set -euo pipefail  # 启用严格模式

  gengner_log
  commitAndPush
}

# 执行主函数
main "$@"
