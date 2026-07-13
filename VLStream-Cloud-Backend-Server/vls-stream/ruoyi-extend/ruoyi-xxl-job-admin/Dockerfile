FROM hb.oortcloudsmart.com/base/java:1.8.432-7-debian-12-r3

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/xxljob/logs

WORKDIR /ruoyi/xxljob

ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 9100

ADD ./ruoyi-xxl-job-admin.jar ./app.jar


ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
