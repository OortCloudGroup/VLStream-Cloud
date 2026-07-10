#!/bin/bash

# 修复nginx配置脚本
echo "🔧 修复nginx配置..."

# 创建正确的nginx配置
cat > nginx.conf.fixed << 'EOF'
server {
    listen 21410;
    server_name oort.oortcloudsmart.com;
    
    # 前端静态文件 - 修复版本
    location /bus/vls-ui/ {
        alias /path/to/your/VLStream-ui/dist/;
        try_files $uri $uri/ /bus/vls-ui/index.html;
        
        # 添加CORS头
        add_header Access-Control-Allow-Origin *;
        add_header Access-Control-Allow-Methods 'GET, POST, OPTIONS';
        add_header Access-Control-Allow-Headers 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization';
        
        # 处理JavaScript文件
        location ~* \.js$ {
            add_header Content-Type application/javascript;
        }
        
        # 处理CSS文件
        location ~* \.css$ {
            add_header Content-Type text/css;
        }
    }
    
    # 代理后端网关API (vls-server)
    location /bus/vls-server/ {
        proxy_pass http://oort.oortcloudsmart.com:18080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 添加CORS头
        add_header Access-Control-Allow-Origin *;
        add_header Access-Control-Allow-Methods 'GET, POST, OPTIONS';
        add_header Access-Control-Allow-Headers 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization,requesttype,appid,secretkey,accesstoken';
        
        # 处理OPTIONS预检请求
        if ($request_method = 'OPTIONS') {
            add_header Access-Control-Allow-Origin *;
            add_header Access-Control-Allow-Methods 'GET, POST, OPTIONS';
            add_header Access-Control-Allow-Headers 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization,requesttype,appid,secretkey,accesstoken';
            add_header Access-Control-Max-Age 1728000;
            add_header Content-Type 'text/plain; charset=utf-8';
            add_header Content-Length 0;
            return 204;
        }
    }
    
    # 代理用户平台网关API (apaas-sso)
    location /bus/apaas-sso/ {
        proxy_pass http://oort.oortcloudsmart.com:18080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 添加CORS头
        add_header Access-Control-Allow-Origin *;
        add_header Access-Control-Allow-Methods 'GET, POST, OPTIONS';
        add_header Access-Control-Allow-Headers 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization,requesttype,appid,secretkey,accesstoken';
        
        # 处理OPTIONS预检请求
        if ($request_method = 'OPTIONS') {
            add_header Access-Control-Allow-Origin *;
            add_header Access-Control-Allow-Methods 'GET, POST, OPTIONS';
            add_header Access-Control-Allow-Headers 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization,requesttype,appid,secretkey,accesstoken';
            add_header Access-Control-Max-Age 1728000;
            add_header Content-Type 'text/plain; charset=utf-8';
            add_header Content-Length 0;
            return 204;
        }
    }
}
EOF

echo "✅ 已创建修复的nginx配置文件: nginx.conf.fixed"
echo ""
echo "📋 需要执行的步骤:"
echo "1. 将nginx.conf.fixed复制到服务器"
echo "2. 修改alias路径为实际的dist目录路径"
echo "3. 重新加载nginx配置"
echo "4. 测试应用是否正常工作"
echo ""
echo "🔧 关键修复点:"
echo "- 确保alias路径指向正确的dist目录"
echo "- 确保try_files指令正确配置"
echo "- 添加了JavaScript和CSS文件的Content-Type头" 