@echo off
echo 🔧 修复nginx配置...

REM 创建正确的nginx配置
(
echo server {
echo     listen 21410;
echo     server_name oort.oortcloudsmart.com;
echo.    
echo     # 前端静态文件 - 修复版本
echo     location /bus/vls-ui/ {
echo         alias /path/to/your/VLStream-ui/dist/;
echo         try_files $uri $uri/ /bus/vls-ui/index.html;
echo.        
echo         # 添加CORS头
echo         add_header Access-Control-Allow-Origin *;
echo         add_header Access-Control-Allow-Methods 'GET, POST, OPTIONS';
echo         add_header Access-Control-Allow-Headers 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization';
echo.        
echo         # 处理JavaScript文件
echo         location ~* \.js$ {
echo             add_header Content-Type application/javascript;
echo         }
echo.        
echo         # 处理CSS文件
echo         location ~* \.css$ {
echo             add_header Content-Type text/css;
echo         }
echo     }
echo.    
echo     # 代理后端网关API (vls-server)
echo     location /bus/vls-server/ {
echo         proxy_pass http://oort.oortcloudsmart.com:18080/;
echo         proxy_set_header Host $host;
echo         proxy_set_header X-Real-IP $remote_addr;
echo         proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
echo         proxy_set_header X-Forwarded-Proto $scheme;
echo.        
echo         # 添加CORS头
echo         add_header Access-Control-Allow-Origin *;
echo         add_header Access-Control-Allow-Methods 'GET, POST, OPTIONS';
echo         add_header Access-Control-Allow-Headers 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization,requesttype,appid,secretkey,accesstoken';
echo.        
echo         # 处理OPTIONS预检请求
echo         if ($request_method = 'OPTIONS') {
echo             add_header Access-Control-Allow-Origin *;
echo             add_header Access-Control-Allow-Methods 'GET, POST, OPTIONS';
echo             add_header Access-Control-Allow-Headers 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization,requesttype,appid,secretkey,accesstoken';
echo             add_header Access-Control-Max-Age 1728000;
echo             add_header Content-Type 'text/plain; charset=utf-8';
echo             add_header Content-Length 0;
echo             return 204;
echo         }
echo     }
echo.    
echo     # 代理用户平台网关API (apaas-sso)
echo     location /bus/apaas-sso/ {
echo         proxy_pass http://oort.oortcloudsmart.com:18080/;
echo         proxy_set_header Host $host;
echo         proxy_set_header X-Real-IP $remote_addr;
echo         proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
echo         proxy_set_header X-Forwarded-Proto $scheme;
echo.        
echo         # 添加CORS头
echo         add_header Access-Control-Allow-Origin *;
echo         add_header Access-Control-Allow-Methods 'GET, POST, OPTIONS';
echo         add_header Access-Control-Allow-Headers 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization,requesttype,appid,secretkey,accesstoken';
echo.        
echo         # 处理OPTIONS预检请求
echo         if ($request_method = 'OPTIONS') {
echo             add_header Access-Control-Allow-Origin *;
echo             add_header Access-Control-Allow-Methods 'GET, POST, OPTIONS';
echo             add_header Access-Control-Allow-Headers 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization,requesttype,appid,secretkey,accesstoken';
echo             add_header Access-Control-Max-Age 1728000;
echo             add_header Content-Type 'text/plain; charset=utf-8';
echo             add_header Content-Length 0;
echo             return 204;
echo         }
echo     }
echo }
) > nginx.conf.fixed

echo ✅ 已创建修复的nginx配置文件: nginx.conf.fixed
echo.
echo 📋 需要执行的步骤:
echo 1. 将nginx.conf.fixed复制到服务器
echo 2. 修改alias路径为实际的dist目录路径
echo 3. 重新加载nginx配置
echo 4. 测试应用是否正常工作
echo.
echo 🔧 关键修复点:
echo - 确保alias路径指向正确的dist目录
echo - 确保try_files指令正确配置
echo - 添加了JavaScript和CSS文件的Content-Type头
echo.
pause 