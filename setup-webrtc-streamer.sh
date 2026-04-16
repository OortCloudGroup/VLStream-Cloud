#!/bin/bash

echo "========================================"
echo "WebRTC-streamer Configuration Script"
echo "========================================"

WEBRTC_VERSION="v0.8.4"
WEBRTC_DIR="../webrtc-streamer"
WEBRTC_PROJECT_DIR="/f/work/vls-tr/webrtc-streamer"
DOWNLOAD_URL="https://github.com/mpromonet/webrtc-streamer/releases/download/${WEBRTC_VERSION}/webrtc-streamer-${WEBRTC_VERSION}-Linux-x86_64.tar.gz"
TAR_FILE="webrtc-streamer-${WEBRTC_VERSION}-Linux-x86_64.tar.gz"

echo "Checking WebRTC-streamer installation..."
echo


# Check webrtc-streamer directory in project root
if [ -d "$WEBRTC_PROJECT_DIR" ]; then
    echo "Found existing WebRTC-streamer directory: $WEBRTC_PROJECT_DIR"
    WEBRTC_DIR="$WEBRTC_PROJECT_DIR"
    cd "$WEBRTC_DIR"
    check_executable
elif [ -d "$WEBRTC_DIR" ]; then
    echo "Found existing WebRTC-streamer directory: $WEBRTC_DIR"
    cd "$WEBRTC_DIR"
    check_executable
else
    echo "WebRTC-streamer not found, preparing to download and install..."
    # Create directory
    if [ ! -d "$WEBRTC_DIR" ]; then
        echo "Creating directory: $WEBRTC_DIR"
        mkdir -p "$WEBRTC_DIR"
    fi
    cd "$WEBRTC_DIR"
    download_webrtc
fi

check_executable() {
    echo "Checking WebRTC-streamer executable..."
    
    # Check if webrtc-streamer exists
    if [ -f "webrtc-streamer" ]; then
        echo "Found webrtc-streamer executable, skipping download"
        create_config
        return
    fi
    
    # Check if other executable files exist
    if ls webrtc-streamer* 1> /dev/null 2>&1; then
        echo "Found webrtc-streamer related files"
        # Rename to standard name
        for file in webrtc-streamer*; do
            if [ -x "$file" ] && [ "$file" != "webrtc-streamer" ]; then
                mv "$file" "webrtc-streamer"
                echo "Renamed $file to webrtc-streamer"
                break
            fi
        done
        
        if [ -f "webrtc-streamer" ]; then
            create_config
            return
        fi
    fi
    
    # If directory exists but no executable, try to download
    echo "Directory exists but no executable found, attempting to download..."
    download_webrtc
}

download_webrtc() {
    echo "Downloading WebRTC-streamer..."
    echo "Download URL: $DOWNLOAD_URL"
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
        echo "Error: Need to install wget or curl to download files"
        echo "Please download WebRTC-streamer manually"
        manual_download_instructions
        return 1
    fi

    if [ ! -f "$TAR_FILE" ]; then
        download_failed
        return 1
    fi

    echo "Download completed, extracting..."
    if ! tar -xzf "$TAR_FILE"; then
        echo "Extraction failed"
        return 1
    fi

    # Delete tar file
    rm -f "$TAR_FILE"

    # Set executable permission
    chmod +x webrtc-streamer

    echo "Extraction completed"
    create_config
}

download_failed() {
    echo
    echo "Download failed! Please download WebRTC-streamer manually"
    manual_download_instructions
}

manual_download_instructions() {
    echo "Download URL: $DOWNLOAD_URL"
    echo "Extract to: $(pwd)"
    echo
    echo "Manual download steps:"
    echo "1. Visit https://github.com/mpromonet/webrtc-streamer/releases"
    echo "2. Download webrtc-streamer-${WEBRTC_VERSION}-Linux-x86_64.tar.gz"
    echo "3. Extract to current directory: $(pwd)"
    echo "4. Rerun this script"
    echo
}

create_config() {
    echo "Creating configuration files..."
    echo "Current directory: $(pwd)"

    # Create configuration file
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

    # Create start script
    cat > start.sh << 'EOF'
#!/bin/bash
echo "========================================"
echo "Starting WebRTC-streamer service"
echo "========================================"
echo "Service address: http://localhost:8000"
echo "Management interface: http://localhost:8000/webrtcstreamer.html"
echo "Press Ctrl+C to stop service"
echo "========================================"
echo
./webrtc-streamer -H 0.0.0.0:8000 -S stun.l.google.com:19302 -v 2
echo
echo "Service stopped"
EOF

    # Create background start script
    cat > start-background.sh << 'EOF'
#!/bin/bash
echo "Starting WebRTC-streamer background service..."
nohup ./webrtc-streamer -H 0.0.0.0:8000 -S stun.l.google.com:19302 -v 2 > webrtc-streamer.log 2>&1 &
echo $! > webrtc-streamer.pid
echo "WebRTC-streamer service started in background"
echo "Service address: http://localhost:8000"
echo "Management interface: http://localhost:8000/webrtcstreamer.html"
echo "Log file: webrtc-streamer.log"
echo "PID file: webrtc-streamer.pid"
echo "Run ./stop.sh to stop service"
EOF

    # Create test script
    cat > test.sh << 'EOF'
#!/bin/bash
echo "Testing WebRTC-streamer service..."
echo


if curl -s -m 5 http://localhost:8000/api/getIceServers > /dev/null; then
    echo "[Success] WebRTC-streamer service is running normally"
    echo "Service address: http://localhost:8000"
    echo "Management interface: http://localhost:8000/webrtcstreamer.html"
else
    echo "[Failure] WebRTC-streamer service is not running or inaccessible"
    echo "Please run ./start.sh to start service first"
fi
echo
EOF

    # Create stop script
    cat > stop.sh << 'EOF'
#!/bin/bash
echo "Stopping WebRTC-streamer service..."

# Try to stop via PID file first
if [ -f "webrtc-streamer.pid" ]; then
    PID=$(cat webrtc-streamer.pid)
    if kill -0 $PID 2>/dev/null; then
        kill $PID
        echo "[Success] WebRTC-streamer service stopped (PID: $PID)"
        rm -f webrtc-streamer.pid
    else
        echo "[Info] Process in PID file does not exist, cleaning PID file"
        rm -f webrtc-streamer.pid
    fi
else
    # Stop via process name
    if pkill -f webrtc-streamer; then
        echo "[Success] WebRTC-streamer service stopped"
    else
        echo "[Info] WebRTC-streamer service is not running"
    fi
fi
echo
EOF

    # Create README
    cat > README.md << 'EOF'
# WebRTC-streamer Usage Instructions

## File Description
- `webrtc-streamer`: WebRTC-streamer main program
- `config.json`: Configuration file
- `start.sh`: Start service (foreground)
- `start-background.sh`: Start service (background)
- `test.sh`: Test service status
- `stop.sh`: Stop service

## Usage Steps
1. Run `./start.sh` to start service (foreground, logs visible)
2. Or run `./start-background.sh` to start service (background)
3. Run `./test.sh` to test if service is normal
4. Run `./stop.sh` to stop service

## Access Addresses
- Service address: http://localhost:8000
- Management interface: http://localhost:8000/webrtcstreamer.html
- API documentation: http://localhost:8000/help

## Configuration Description
- Service port: 8000
- STUN server: stun.l.google.com:19302
- Log level: 2 (detailed)
- Recording path: ./records

## Background Running
When using `start-background.sh` to run in background:
- Logs output to `webrtc-streamer.log`
- Process ID saved to `webrtc-streamer.pid`
- Use `stop.sh` to stop service
EOF

    # Set script permissions
    chmod +x start.sh start-background.sh test.sh stop.sh

    echo
    echo "========================================"
    echo "Configuration completed!"
    echo "========================================"
    echo
    echo "Installation location: $(pwd)"
    echo
    echo "File check:"
    if [ -f "webrtc-streamer" ]; then
        echo "  [√] webrtc-streamer - Main program"
    else
        echo "  [×] webrtc-streamer - Main program (missing)"
    fi
    if [ -f "config.json" ]; then
        echo "  [√] config.json - Configuration file"
    else
        echo "  [×] config.json - Configuration file (missing)"
    fi
    if [ -f "start.sh" ]; then
        echo "  [√] start.sh - Start script"
    else
        echo "  [×] start.sh - Start script (missing)"
    fi
    echo
    echo "Usage instructions:"
    echo "  1. ./start.sh to start service"
    echo "  2. ./test.sh to test service"
    echo "  3. ./stop.sh to stop service"
    echo
    echo "Access addresses:"
    echo "  - Service: http://localhost:8000"
    echo "  - Management: http://localhost:8000/webrtcstreamer.html"
    echo
    echo "Start service now? (y/N)"
    read -r choice
    if [[ "$choice" == "y" || "$choice" == "Y" ]]; then
        echo
        echo "Starting WebRTC-streamer service..."
        ./start.sh &
    fi

    echo
    echo "Configuration completed! Please check README.md for detailed usage instructions."
}

# Main program execution
if [ -d "$WEBRTC_PROJECT_DIR" ]; then
    echo "Found existing WebRTC-streamer directory: $WEBRTC_PROJECT_DIR"
    WEBRTC_DIR="$WEBRTC_PROJECT_DIR"
    cd "$WEBRTC_DIR"
    check_executable
elif [ -d "$WEBRTC_DIR" ]; then
    echo "Found existing WebRTC-streamer directory: $WEBRTC_DIR"
    cd "$WEBRTC_DIR"
    check_executable
else
    echo "WebRTC-streamer not found, preparing to download and install..."
    # Create directory
    if [ ! -d "$WEBRTC_DIR" ]; then
        echo "Creating directory: $WEBRTC_DIR"
        mkdir -p "$WEBRTC_DIR"
    fi
    cd "$WEBRTC_DIR"
    download_webrtc
fi 