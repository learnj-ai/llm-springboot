FROM registry.access.redhat.com/ubi9/openjdk-25:latest

# Set working directory
WORKDIR /app

# Copy the JAR file
COPY target/*.jar app.jar

# Expose application port
EXPOSE 8086

# Run the application (--enable-preview required for Java 25 preview APIs used in the workshop)
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
