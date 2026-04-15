#!/bin/bash

echo "========================================"
echo "WebRTC-streamer 配置脚本"
echo "========================================"

WEBRTC_VERSION="v0.8.4"
WEBRTC_DIR="../webrtc-streamer"
WEBRTC_PROJECT_DIR="/f/work/vls-tr/webrtc-streamer"
DOWNLOAD_URL="https://github.com/mpromonet/webrtc-streamer/releases/download/${WEBRTC_VERSION}/webrtc-streamer-${WEBRTC_VERSION}-Linux-x86_64.tar.gz"
TAR_FILE="webrtc-streamer-${WEBRTC_VERSION}-Linux-x86_64.tar.gz"

echo "正在检查WebRTC-streamer安装..."
echo

# 检查项目根目录的webrtc-streamer目录
if [ -d "$WEBRTC_PROJECT_DIR" ]; then
    echo "发现已存在的WebRTC-streamer目录: $WEBRTC_PROJECT_DIR"
    WEBRTC_DIR="$WEBRTC_PROJECT_DIR"
    cd "$WEBRTC_DIR"
    check_executable
elif [ -d "$WEBRTC_DIR" ]; then
    echo "发现已存在的WebRTC-streamer目录: $WEBRTC_DIR"
    cd "$WEBRTC_DIR"
    check_executable
else
    echo "未找到WebRTC-streamer，准备下载安装..."
    # 创建目录
    if [ ! -d "$WEBRTC_DIR" ]; then
        echo "创建目录: $WEBRTC_DIR"
        mkdir -p "$WEBRTC_DIR"
    fi
    cd "$WEBRTC_DIR"
    download_webrtc
fi

check_executable() {
    echo "检查WebRTC-streamer可执行文件..."
    
    # 检查是否存在webrtc-streamer
    if [ -f "webrtc-streamer" ]; then
        echo "找到webrtc-streamer可执行文件，跳过下载"
        create_config
        return
    fi
    
    # 检查是否存在其他可执行文件
    if ls webrtc-streamer* 1> /dev/null 2>&1; then
        echo "找到webrtc-streamer相关文件"
        # 重命名为标准名称
        for file in webrtc-streamer*; do
            if [ -x "$file" ] && [ "$file" != "webrtc-streamer" ]; then
                mv "$file" "webrtc-streamer"
                echo "重命名 $file 为 webrtc-streamer"
                break
            fi
        done
        
        if [ -f "webrtc-streamer" ]; then
            create_config
            return
        fi
    fi
    
    # 如果目录存在但没有可执行文件，尝试下载
    echo "目录存在但未找到可执行文件，尝试下载..."
    download_webrtc
}

download_webrtc() {
    echo "正在下载WebRTC-streamer..."
    echo "下载地址: $DOWNLOAD_URL"
    echo

    if command -v wget > /dev/null; then
        if ! wget -O "$TAR_FILE" "$DOWNLOAD_URL"; then
            download_failed
            return 1
        fi
    elif command -v curl > /dev/null; then
        if ! curl -L -o "$TAR_FILE" "$DOWNLOAD_URL"; then
            download_failed
            return 1
        fi
    else
        echo "错误: 需要安装wget或curl来下载文件"
        echo "请手动下载WebRTC-streamer"
        manual_download_instructions
        return 1
    fi

    if [ ! -f "$TAR_FILE" ]; then
        download_failed
        return 1
    fi

    echo "下载完成，正在解压..."
    if ! tar -xzf "$TAR_FILE"; then
        echo "解压失败"
        return 1
    fi

    # 删除压缩包
    rm -f "$TAR_FILE"

    # 设置执行权限
    chmod +x webrtc-streamer

    echo "解压完成"
    create_config
}

download_failed() {
    echo
    echo "下载失败！请手动下载WebRTC-streamer"
    manual_download_instructions
}

manual_download_instructions() {
    echo "下载地址: $DOWNLOAD_URL"
    echo "解压到: $(pwd)"
    echo
    echo "手动下载步骤："
    echo "1. 访问 https://github.com/mpromonet/webrtc-streamer/releases"
    echo "2. 下载 webrtc-streamer-${WEBRTC_VERSION}-Linux-x86_64.tar.gz"
    echo "3. 解压到当前目录: $(pwd)"
    echo "4. 重新运行此脚本"
    echo
}

create_config() {
    echo "正在创建配置文件..."
    echo "当前目录: $(pwd)"

    # 创建配置文件
    cat > config.json << EOF
{
  "webrtc-streamer": {
    "http": "0.0.0.0:8000",
    "verbose": "2",
    "stunserver": "stun.l.google.com:19302",
    "turnserver": "",
    "recordpath": "./records",
    "publishfilter": ".*",
    "webroot": "./html"
  }
}
EOF

    # 创建启动脚本
    cat > start.sh << 'EOF'
#!/bin/bash
echo "========================================"
echo "启动WebRTC-streamer服务"
echo "========================================"
echo "服务地址: http://localhost:8000"
echo "管理界面: http://localhost:8000/webrtcstreamer.html"
echo "按Ctrl+C停止服务"
echo "========================================"
echo
./webrtc-streamer -H 0.0.0.0:8000 -S stun.l.google.com:19302 -v 2
echo
echo "服务已停止"
EOF

    # 创建后台启动脚本
    cat > start-background.sh << 'EOF'
#!/bin/bash
echo "启动WebRTC-streamer后台服务..."
nohup ./webrtc-streamer -H 0.0.0.0:8000 -S stun.l.google.com:19302 -v 2 > webrtc-streamer.log 2>&1 &
echo $! > webrtc-streamer.pid
echo "WebRTC-streamer服务已在后台启动"
echo "服务地址: http://localhost:8000"
echo "管理界面: http://localhost:8000/webrtcstreamer.html"
echo "日志文件: webrtc-streamer.log"
echo "PID文件: webrtc-streamer.pid"
echo "运行 ./stop.sh 停止服务"
EOF

    # 创建测试脚本
    cat > test.sh << 'EOF'
#!/bin/bash
echo "测试WebRTC-streamer服务..."
echo

if curl -s -m 5 http://localhost:8000/api/getIceServers > /dev/null; then
    echo "[成功] WebRTC-streamer服务运行正常"
    echo "服务地址: http://localhost:8000"
    echo "管理界面: http://localhost:8000/webrtcstreamer.html"
else
    echo "[失败] WebRTC-streamer服务未运行或不可访问"
    echo "请先运行 ./start.sh 启动服务"
fi
echo
EOF

    # 创建停止脚本
    cat > stop.sh << 'EOF'
#!/bin/bash
echo "停止WebRTC-streamer服务..."

# 先尝试通过PID文件停止
if [ -f "webrtc-streamer.pid" ]; then
    PID=$(cat webrtc-streamer.pid)
    if kill -0 $PID 2>/dev/null; then
        kill $PID
        echo "[成功] WebRTC-streamer服务已停止 (PID: $PID)"
        rm -f webrtc-streamer.pid
    else
        echo "[信息] PID文件中的进程不存在，清理PID文件"
        rm -f webrtc-streamer.pid
    fi
else
    # 通过进程名停止
    if pkill -f webrtc-streamer; then
        echo "[成功] WebRTC-streamer服务已停止"
    else
        echo "[信息] WebRTC-streamer服务未运行"
    fi
fi
echo
EOF

    # 创建README
    cat > README.md << 'EOF'
# WebRTC-streamer 使用说明

## 文件说明
- `webrtc-streamer`: WebRTC-streamer主程序
- `config.json`: 配置文件
- `start.sh`: 启动服务（前台运行）
- `start-background.sh`: 启动服务（后台运行）
- `test.sh`: 测试服务状态
- `stop.sh`: 停止服务

## 使用步骤
1. 运行 `./start.sh` 启动服务（前台运行，可看到日志）
2. 或运行 `./start-background.sh` 启动服务（后台运行）
3. 运行 `./test.sh` 测试服务是否正常
4. 运行 `./stop.sh` 停止服务

## 访问地址
- 服务地址: http://localhost:8000
- 管理界面: http://localhost:8000/webrtcstreamer.html
- API文档: http://localhost:8000/help

## 配置说明
- 服务端口: 8000
- STUN服务器: stun.l.google.com:19302
- 日志级别: 2 (详细)
- 录制路径: ./records

## 后台运行
使用 `start-background.sh` 后台运行时：
- 日志输出到 `webrtc-streamer.log`
- 进程ID保存到 `webrtc-streamer.pid`
- 使用 `stop.sh` 停止服务
EOF

    # 设置脚本权限
    chmod +x start.sh start-background.sh test.sh stop.sh

    echo
    echo "========================================"
    echo "配置完成！"
    echo "========================================"
    echo
    echo "安装位置: $(pwd)"
    echo
    echo "文件检查:"
    if [ -f "webrtc-streamer" ]; then
        echo "  [√] webrtc-streamer - 主程序"
    else
        echo "  [×] webrtc-streamer - 主程序 (缺失)"
    fi
    if [ -f "config.json" ]; then
        echo "  [√] config.json - 配置文件"
    else
        echo "  [×] config.json - 配置文件 (缺失)"
    fi
    if [ -f "start.sh" ]; then
        echo "  [√] start.sh - 启动脚本"
    else
        echo "  [×] start.sh - 启动脚本 (缺失)"
    fi
    echo
    echo "使用说明:"
    echo "  1. ./start.sh 启动服务"
    echo "  2. ./test.sh 测试服务"
    echo "  3. ./stop.sh 停止服务"
    echo
    echo "访问地址:"
    echo "  - 服务: http://localhost:8000"
    echo "  - 管理: http://localhost:8000/webrtcstreamer.html"
    echo
    echo "现在启动服务吗？(y/N)"
    read -r choice
    if [[ "$choice" == "y" || "$choice" == "Y" ]]; then
        echo
        echo "启动WebRTC-streamer服务..."
        ./start.sh &
    fi

    echo
    echo "配置完成！请查看 README.md 了解详细使用方法。"
}

# 主程序开始执行
if [ -d "$WEBRTC_PROJECT_DIR" ]; then
    echo "发现已存在的WebRTC-streamer目录: $WEBRTC_PROJECT_DIR"
    WEBRTC_DIR="$WEBRTC_PROJECT_DIR"
    cd "$WEBRTC_DIR"
    check_executable
elif [ -d "$WEBRTC_DIR" ]; then
    echo "发现已存在的WebRTC-streamer目录: $WEBRTC_DIR"
    cd "$WEBRTC_DIR"
    check_executable
else
    echo "未找到WebRTC-streamer，准备下载安装..."
    # 创建目录
    if [ ! -d "$WEBRTC_DIR" ]; then
        echo "创建目录: $WEBRTC_DIR"
        mkdir -p "$WEBRTC_DIR"
    fi
    cd "$WEBRTC_DIR"
    download_webrtc
fi 