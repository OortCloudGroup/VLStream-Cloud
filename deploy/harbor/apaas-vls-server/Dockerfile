FROM hb.oortcloudsmart.com/base/alpine-java:openjdk17_cn_slim

LABEL maintainer="zhonghuixiong"

WORKDIR /root

# 更换 Alpine 源为阿里云镜像（加速下载），并安装 ffmpeg
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories \
    && apk update \
    && apk add --no-cache ffmpeg \
    && rm -rf /var/cache/apk/*

# 创建智能缓存目录（若原 doc/smartjavaai_cache 不存在，则创建空目录）
# 如需复制文件，请确保构建上下文中有该目录，并取消下行注释
RUN mkdir -p ./smartjavaai_cache

ENV PLATFORM_RECORDING_PYTHON=/opt/recording-venv/bin/python
RUN apk add --no-cache python3 py3-pip \
    && python3 -m venv /opt/recording-venv \
    && "$PLATFORM_RECORDING_PYTHON" -m pip install --no-cache-dir --upgrade 'pip>=24,<27' \
    && "$PLATFORM_RECORDING_PYTHON" -m pip install --no-cache-dir av==17.1.0 aiortc==1.15.0 aiohttp==3.14.5 \
    && "$PLATFORM_RECORDING_PYTHON" -c "import av, aiortc, aiohttp; av.CodecContext.create('libx264', 'w')"

EXPOSE 8080

# 若实际需要复制缓存文件，请取消下行注释并确保目录存在
# ADD ./doc/smartjavaai_cache ./smartjavaai_cache

COPY ./apaas-vls-server.jar app.jar

ENTRYPOINT ["java", \
    "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
    "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED", \
    "-Duser.timezone=GMT+8", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]

CMD ["--spring.profiles.active=dev"]
