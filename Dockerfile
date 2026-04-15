FROM adoptopenjdk/openjdk8:latest

# 设置工作目录
WORKDIR /root

ADD ./docs/smartjavaai_cache ./smartjavaai_cache
# 复制应用程序的JAR文件到镜像中
COPY ./target/vls-server-1.0.0.jar app.jar

# 暴露应用程序的端口
EXPOSE 18080

# 运行应用程序
CMD ["java", "-Xmx4096m", "-jar", "app.jar"]
