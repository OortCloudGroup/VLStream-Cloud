FROM adoptopenjdk/openjdk8:latest

# Set working directory
WORKDIR /root

ADD ./docs/smartjavaai_cache ./smartjavaai_cache
# Copy application JAR file to image
COPY ./target/vls-server-1.0.0.jar app.jar

# Expose application port
EXPOSE 18080

# Run application
CMD ["java", "-Xmx4096m", "-jar", "app.jar"]