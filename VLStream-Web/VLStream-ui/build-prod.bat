@echo off
echo 开始构建生产环境版本...

REM 清理之前的构建
if exist dist rmdir /s /q dist

REM 构建生产版本
npm run build

echo 构建完成！
echo 请将 dist 目录中的文件部署到 nginx 的 /usr/share/nginx/html/ 目录
echo 并确保 nginx 配置正确指向该目录
pause 